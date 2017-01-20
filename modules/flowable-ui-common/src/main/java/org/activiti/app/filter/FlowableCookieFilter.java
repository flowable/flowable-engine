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
package org.activiti.app.filter;

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

import org.activiti.app.model.common.RemoteToken;
import org.activiti.app.model.common.RemoteUser;
import org.activiti.app.security.ActivitiAppUser;
import org.activiti.app.security.CookieConstants;
import org.activiti.app.service.idm.RemoteIdmService;
import org.activiti.engine.common.api.ActivitiException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class FlowableCookieFilter extends OncePerRequestFilter {
  
  private final Logger logger = LoggerFactory.getLogger(FlowableCookieFilter.class);
  
  protected static final String DELIMITER = ":";
  
  @Autowired
  protected Environment env;
  
  @Autowired
  protected RemoteIdmService remoteIdmService;
  
  @Autowired(required=false)
  protected FlowableCookieFilterCallback filterCallback;
  
  protected String idmAppUrl;
  
  // Caching the persistent tokens and users to avoid hitting the database too often 
  // (eg when doing multiple requests at the same time)
  protected LoadingCache<String, RemoteToken> tokenCache;
  protected LoadingCache<String, ActivitiAppUser> userCache;
  
  @PostConstruct
  protected void initCaches() {
    initIdmAppUrl();
    initTokenCache();
    initUserCache();
  }

  protected void initIdmAppUrl() {
    idmAppUrl = env.getRequiredProperty("idm.app.url");
    if (idmAppUrl.endsWith("/") == false) {
      idmAppUrl += "/";
    }
  }

  protected void initTokenCache() {
    Long maxSize = env.getProperty("cache.login-tokens.max.size", Long.class, 2048l);
    Long maxAge = env.getProperty("cache.login-tokens.max.age", Long.class, 30l);
    tokenCache = CacheBuilder.newBuilder().maximumSize(maxSize).expireAfterWrite(maxAge, TimeUnit.SECONDS).recordStats()
        .build(new CacheLoader<String, RemoteToken>() {

          public RemoteToken load(final String tokenId) throws Exception {
            RemoteToken token = remoteIdmService.getToken(tokenId); 
            if (token != null) {
              return token;
            } else {
              throw new ActivitiException("token not found " + tokenId);
            }
          }

        });
  }

  protected void initUserCache() {
    Long userMaxSize = env.getProperty("cache.login-users.max.size", Long.class, 2048l);
    Long userMaxAge = env.getProperty("cache.login-users.max.age", Long.class, 30l);
    userCache = CacheBuilder.newBuilder().maximumSize(userMaxSize).expireAfterWrite(userMaxAge, TimeUnit.SECONDS).recordStats()
        .build(new CacheLoader<String, ActivitiAppUser>() {

          public ActivitiAppUser load(final String userId) throws Exception {
            RemoteUser user = remoteIdmService.getUser(userId);
            if (user == null) {
              throw new ActivitiException("user not found " + userId);
            }
            
            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
            for (String privilege : user.getPrivileges()) {
              grantedAuthorities.add(new SimpleGrantedAuthority(privilege));
            }

            // put account into security context (for controllers to use)
            ActivitiAppUser appUser = new ActivitiAppUser(user, user.getId(), grantedAuthorities);
            return appUser;
          }

        });
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if (skipAuthenticationCheck(request) == false) {
      RemoteToken token = getValidToken(request);
      if (token != null) {
        onValidTokenFound(request, response, token);
        if (filterCallback != null) {
          filterCallback.onValidTokenFound(request, response, token);
        }
      } else {
        redirectToLogin(request, response);
        return; // no need to execute any other filters
      }
    }
    
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (filterCallback != null) {
        filterCallback.onFilterCleanup(request, response);
      }
    }
  }

  protected RemoteToken getValidToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null){
      for (Cookie cookie : cookies) {
        if (CookieConstants.COOKIE_NAME.equals(cookie.getName())) {
          String[] tokens = decodeCookie(cookie.getValue());
          
          try {
            RemoteToken token = tokenCache.get(tokens[0]);
            if (token.getValue().equals(tokens[1]) == false) {
              // refetch the token from the database
              tokenCache.invalidate(tokens[0]);
              token = tokenCache.get(tokens[0]);
              if (token.getValue().equals(tokens[1])) {
                return token;
              }
            } else {
              return token;
            }
            
            break; // We're only interested in one particular cookie
            
          } catch (Exception e) {
            logger.trace("Could not get token", e);
            return null;
          }
        }
      }
    }
    
    return null;
  }
  
  protected void onValidTokenFound(HttpServletRequest request, HttpServletResponse response, RemoteToken token) {
    try {
      ActivitiAppUser appUser = userCache.get(token.getUserId());
      
      SecurityContextHolder.getContext().setAuthentication(new RememberMeAuthenticationToken(token.getId(), 
          appUser, appUser.getAuthorities()));
      
    } catch (Exception e) {
      logger.trace("Could not set necessary threadlocals for token, e");
      redirectToLogin(request, response);
    }
  }
  
  protected void redirectToLogin(HttpServletRequest request, HttpServletResponse response)  {
    try {
      response.sendRedirect(idmAppUrl + "#/login?redirectOnAuthSuccess=true&redirectUrl=" + request.getRequestURL());
    } catch (IOException e) {
      logger.warn("Could not redirect to " + idmAppUrl, e);
    }
  }
  
  protected boolean skipAuthenticationCheck(HttpServletRequest request) {
    if (request.getRequestURI().endsWith(".css") || 
        request.getRequestURI().endsWith(".js") ||
        request.getRequestURI().endsWith(".html") || 
        request.getRequestURI().endsWith(".map") ||
        request.getRequestURI().endsWith(".woff") || 
        request.getRequestURI().endsWith(".map") ||
        request.getRequestURI().endsWith(".png") || 
        request.getRequestURI().endsWith(".jpg")) {
      return true;
    }
    return false;
  }
  
  protected String[] decodeCookie(String cookieValue) throws InvalidCookieException {
    for (int j = 0; j < cookieValue.length() % 4; j++) {
      cookieValue = cookieValue + "=";
    }

    if (!Base64.isBase64(cookieValue.getBytes())) {
      throw new InvalidCookieException("Cookie token was not Base64 encoded; value was '" + cookieValue + "'");
    }

    String cookieAsPlainText = new String(Base64.decodeBase64(cookieValue.getBytes()));

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
