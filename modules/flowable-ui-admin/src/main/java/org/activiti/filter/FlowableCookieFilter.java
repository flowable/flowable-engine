/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.common.api.ActivitiException;
import org.activiti.idm.api.Group;
import org.activiti.idm.api.IdmIdentityService;
import org.activiti.idm.api.Token;
import org.activiti.idm.api.User;
import org.activiti.security.ActivitiAppUser;
import org.activiti.security.AuthoritiesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class FlowableCookieFilter extends OncePerRequestFilter {
  
  private final Logger logger = LoggerFactory.getLogger(FlowableCookieFilter.class);
  
  protected static final String COOKIE_NAME = "FLOWABLE_REMEMBER_ME";
  protected static final String DELIMITER = ":";
  
  @Autowired
  protected Environment env;
  
  @Autowired
  protected IdmIdentityService idmIdentityService;
  
  // Caching the persistent tokens to avoid hitting the database too often (eg when doing multiple requests at the same time)
  // (This happens a lot, when the page consists of multiple requests)
  protected LoadingCache<String, Token> tokenCache;
  
  protected LoadingCache<String, ActivitiAppUser> userCache;
  
  @PostConstruct
  protected void initTokenCache() {
    Long maxSize = env.getProperty("cache.login-tokens.max.size", Long.class, 2048l);
    Long maxAge = env.getProperty("cache.login-tokens.max.age", Long.class, 30l);
    tokenCache = CacheBuilder.newBuilder().maximumSize(maxSize).expireAfterWrite(maxAge, TimeUnit.SECONDS).recordStats()
        .build(new CacheLoader<String, Token>() {

          public Token load(final String tokenId) throws Exception {
            Token token = idmIdentityService.createTokenQuery().tokenId(tokenId).singleResult();
            if (token != null) {
              return token;
            } else {
              throw new ActivitiException("token not found " + tokenId);
            }
          }

        });
    
    Long userMaxSize = env.getProperty("cache.login-users.max.size", Long.class, 2048l);
    Long userMaxAge = env.getProperty("cache.login-users.max.age", Long.class, 30l);
    userCache = CacheBuilder.newBuilder().maximumSize(userMaxSize).expireAfterWrite(userMaxAge, TimeUnit.SECONDS).recordStats()
        .build(new CacheLoader<String, ActivitiAppUser>() {

          public ActivitiAppUser load(final String userId) throws Exception {
            User userFromToken = idmIdentityService.createUserQuery().userId(userId).singleResult();
            
            if (userFromToken == null) {
              throw new ActivitiException("user not found " + userId);
            }
            
            // Add capabilities to user object
            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();

            // add default authority
            grantedAuthorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));

            // check if user is in super user group
            String superUserGroupName = env.getRequiredProperty("admin.group");
            for (Group group : idmIdentityService.createGroupQuery().groupMember(userFromToken.getId()).list()) {
              if (superUserGroupName.equals(group.getName())) {
                grantedAuthorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN));
              }
            }

            // put account into security context (for controllers to use)
            ActivitiAppUser appUser = new ActivitiAppUser(userFromToken, userFromToken.getId(), grantedAuthorities);
            return appUser;
          }

        });
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if (skipAuthenticationCheck(request) == false) {
      boolean authenticated = checkAuthentication(request);
      
      if (authenticated == false) {
        String idmAppUrl = env.getRequiredProperty("idm.app.url");
        if (idmAppUrl.endsWith("/") == false) {
          idmAppUrl += "/";
        }
        
        response.sendRedirect(idmAppUrl + "#/login?redirectUrl=" + request.getRequestURL());
        return;
      }
    }
    
    filterChain.doFilter(request, response);
  }
  
  protected boolean checkAuthentication(HttpServletRequest request) {
    boolean authenticated = false;
    Cookie[] cookies = request.getCookies();
    if (cookies != null){
      for (Cookie cookie : cookies) {
        if (COOKIE_NAME.equals(cookie.getName())) {
          String[] tokens = decodeCookie(cookie.getValue());
          
          try {
            Token token = tokenCache.get(tokens[0]);
            if (token.getTokenValue().equals(tokens[1]) == false) {
              // refetch the token from the database
              tokenCache.invalidate(tokens[0]);
              token = tokenCache.get(tokens[0]);
              if (token.getTokenValue().equals(tokens[1])) {
                authenticated = true;
              }
            
            } else {
              authenticated = true;
            }
            
            ActivitiAppUser appUser = userCache.get(token.getUserId());
            
            SecurityContextHolder.getContext().setAuthentication(new RememberMeAuthenticationToken(token.getId(), 
                appUser, appUser.getAuthorities()));
            
            authenticated = true;
            break;
            
          } catch (Exception e) {
            logger.trace("Could not get user for token", e);
            return false;
          }
        }
      }
    }
    
    return authenticated;
  }
  
  protected boolean skipAuthenticationCheck(HttpServletRequest request) {
    boolean skipAuthentication = false;
    if (request.getRequestURI().endsWith(".css") || request.getRequestURI().endsWith(".js") ||
        request.getRequestURI().endsWith(".html") || request.getRequestURI().endsWith(".map") ||
        request.getRequestURI().endsWith(".woff") || request.getRequestURI().endsWith(".map") ||
        request.getRequestURI().endsWith(".png") || request.getRequestURI().endsWith(".jpg")) {
      
      skipAuthentication = true;
    }
    return skipAuthentication;
  }
  
  protected String[] decodeCookie(String cookieValue) throws InvalidCookieException {
    for (int j = 0; j < cookieValue.length() % 4; j++) {
      cookieValue = cookieValue + "=";
    }

    if (!Base64.isBase64(cookieValue.getBytes())) {
      throw new InvalidCookieException("Cookie token was not Base64 encoded; value was '" + cookieValue + "'");
    }

    String cookieAsPlainText = new String(Base64.decode(cookieValue.getBytes()));

    String[] tokens = StringUtils.delimitedListToStringArray(cookieAsPlainText, DELIMITER);

    if ((tokens[0].equalsIgnoreCase("http") || tokens[0].equalsIgnoreCase("https")) && tokens[1].startsWith("//")) {
      // Assume we've accidentally split a URL (OpenID identifier)
      String[] newTokens = new String[tokens.length - 1];
      newTokens[0] = tokens[0] + ":" + tokens[1];
      System.arraycopy(tokens, 2, newTokens, 1, newTokens.length - 1);
      tokens = newTokens;
    }

    return tokens;
  }
  
}
