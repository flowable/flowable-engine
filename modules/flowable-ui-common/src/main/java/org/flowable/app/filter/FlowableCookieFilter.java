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
package org.flowable.app.filter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.app.model.common.RemoteToken;
import org.flowable.app.model.common.RemoteUser;
import org.flowable.app.security.CookieConstants;
import org.flowable.app.security.FlowableAppUser;
import org.flowable.app.service.idm.RemoteIdmService;
import org.flowable.engine.common.api.FlowableException;
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

public class FlowableCookieFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableCookieFilter.class);

    protected static final String DELIMITER = ":";

    @Autowired
    protected Environment env;

    @Autowired
    protected RemoteIdmService remoteIdmService;

    @Autowired(required = false)
    protected FlowableCookieFilterCallback filterCallback;

    protected String idmAppUrl;
    protected String redirectUrlOnAuthSuccess;

    protected Collection<String> requiredPrivileges;

    // Caching the persistent tokens and users to avoid hitting the database too often
    // (eg when doing multiple requests at the same time)
    protected LoadingCache<String, RemoteToken> tokenCache;

    protected LoadingCache<String, FlowableAppUser> userCache;

    @PostConstruct
    protected void initCaches() {
        initIdmAppRedirectUrl();
        initTokenCache();
        initUserCache();
    }

    protected void initIdmAppRedirectUrl() {
        idmAppUrl = env.getProperty("idm.app.redirect.url");
        if (idmAppUrl == null || idmAppUrl.isEmpty()) {
            idmAppUrl = env.getRequiredProperty("idm.app.url");
        }
        if (!idmAppUrl.endsWith("/")) {
            idmAppUrl += "/";
        }
        
        redirectUrlOnAuthSuccess = env.getProperty("app.redirect.url.on.authsuccess");
    }

    protected void initTokenCache() {
        Long maxSize = env.getProperty("cache.login-tokens.max.size", Long.class, 2048l);
        Long maxAge = env.getProperty("cache.login-tokens.max.age", Long.class, 30l);
        tokenCache = CacheBuilder.newBuilder().maximumSize(maxSize).expireAfterWrite(maxAge, TimeUnit.SECONDS).recordStats()
                .build(new CacheLoader<String, RemoteToken>() {

                    @Override
                    public RemoteToken load(final String tokenId) throws Exception {
                        RemoteToken token = remoteIdmService.getToken(tokenId);
                        if (token != null) {
                            return token;
                        } else {
                            throw new FlowableException("token not found " + tokenId);
                        }
                    }

                });
    }

    protected void initUserCache() {
        Long userMaxSize = env.getProperty("cache.login-users.max.size", Long.class, 2048l);
        Long userMaxAge = env.getProperty("cache.login-users.max.age", Long.class, 30l);
        userCache = CacheBuilder.newBuilder().maximumSize(userMaxSize).expireAfterWrite(userMaxAge, TimeUnit.SECONDS).recordStats()
                .build(new CacheLoader<String, FlowableAppUser>() {

                    @Override
                    public FlowableAppUser load(final String userId) throws Exception {
                        RemoteUser user = remoteIdmService.getUser(userId);
                        if (user == null) {
                            throw new FlowableException("user not found " + userId);
                        }

                        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
                        for (String privilege : user.getPrivileges()) {
                            grantedAuthorities.add(new SimpleGrantedAuthority(privilege));
                        }

                        // put account into security context (for controllers to use)
                        FlowableAppUser appUser = new FlowableAppUser(user, user.getId(), grantedAuthorities);
                        return appUser;
                    }

                });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!skipAuthenticationCheck(request)) {
            RemoteToken token = getValidToken(request);
            if (token != null) {
                onValidTokenFound(request, response, token);
                if (filterCallback != null) {
                    filterCallback.onValidTokenFound(request, response, token);
                }
            } else {
                redirectOrSendNotPermitted(request, response, null);
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
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (CookieConstants.COOKIE_NAME.equals(cookie.getName())) {
                    String[] tokens = decodeCookie(cookie.getValue());

                    try {
                        RemoteToken token = tokenCache.get(tokens[0]);
                        if (!token.getValue().equals(tokens[1])) {
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
                        LOGGER.trace("Could not get token", e);
                        return null;
                    }
                }
            }
        }

        return null;
    }

    protected void onValidTokenFound(HttpServletRequest request, HttpServletResponse response, RemoteToken token) {
        try {
            FlowableAppUser appUser = userCache.get(token.getUserId());
            validateRequiredPriviliges(request, response, appUser);
            SecurityContextHolder.getContext().setAuthentication(new RememberMeAuthenticationToken(token.getId(),
                    appUser, appUser.getAuthorities()));

        } catch (Exception e) {
            LOGGER.trace("Could not set necessary threadlocals for token", e);
            redirectOrSendNotPermitted(request, response, token.getUserId());
        }
    }

    protected void validateRequiredPriviliges(HttpServletRequest request, HttpServletResponse response, FlowableAppUser user) {

        if (user == null) {
            return;
        }

        String pathInfo = request.getPathInfo();
        if (isRootPath(request)
                || !pathInfo.startsWith("/rest")) { // rest calls handled by Spring Security conf

            if (requiredPrivileges != null && requiredPrivileges.size() > 0) {

                if (user.getAuthorities() == null || user.getAuthorities().size() == 0) {
                    redirectOrSendNotPermitted(request, response, user.getUserObject().getId());
                } else {
                    int matchingPrivileges = 0;
                    for (GrantedAuthority authority : user.getAuthorities()) {
                        if (requiredPrivileges.contains(authority.getAuthority())) {
                            matchingPrivileges++;
                        }
                    }

                    if (matchingPrivileges != requiredPrivileges.size()) {
                        redirectOrSendNotPermitted(request, response, user.getUserObject().getId());
                    }
                }
            }

        }
    }

    protected void redirectOrSendNotPermitted(HttpServletRequest request, HttpServletResponse response, String userId) {
        if (isRootPath(request)) {
            redirectToLogin(request, response, userId);
        } else {
            sendNotPermitted(request, response);
        }
    }

    protected void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String userId) {
        try {
            if (userId != null) {
                userCache.invalidate(userId);
            }
            
            String baseRedirectUrl = idmAppUrl + "#/login?redirectOnAuthSuccess=true&redirectUrl=";
            if (redirectUrlOnAuthSuccess != null) {
                response.sendRedirect(baseRedirectUrl + redirectUrlOnAuthSuccess);
                
            } else {
                response.sendRedirect(baseRedirectUrl + request.getRequestURL());
            }
            
        } catch (IOException e) {
            LOGGER.warn("Could not redirect to {}", idmAppUrl, e);
        }
    }

    protected void sendNotPermitted(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    protected boolean isRootPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null || "".equals(pathInfo) || "/".equals(pathInfo);
    }

    protected boolean skipAuthenticationCheck(HttpServletRequest request) {
        return request.getRequestURI().endsWith(".css") ||
                request.getRequestURI().endsWith(".js") ||
                request.getRequestURI().endsWith(".html") ||
                request.getRequestURI().endsWith(".map") ||
                request.getRequestURI().endsWith(".woff") ||
                request.getRequestURI().endsWith(".png") ||
                request.getRequestURI().endsWith(".jpg") ||
                request.getRequestURI().endsWith(".jpeg") ||
                request.getRequestURI().endsWith(".tif") ||
                request.getRequestURI().endsWith(".tiff");
    }

    protected String[] decodeCookie(String cookieValue) throws InvalidCookieException {
        for (int j = 0; j < cookieValue.length() % 4; j++) {
            cookieValue = cookieValue + "=";
        }

        String cookieAsPlainText = null;
        try {
            cookieAsPlainText = new String(Base64.getDecoder().decode(cookieValue.getBytes()));
        } catch (IllegalArgumentException e) {
            throw new InvalidCookieException("Cookie token was not Base64 encoded; value was '" + cookieValue + "'");
        }

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

    public Collection<String> getRequiredPrivileges() {
        return requiredPrivileges;
    }

    public void setRequiredPrivileges(Collection<String> requiredPrivileges) {
        this.requiredPrivileges = requiredPrivileges;
    }

}
