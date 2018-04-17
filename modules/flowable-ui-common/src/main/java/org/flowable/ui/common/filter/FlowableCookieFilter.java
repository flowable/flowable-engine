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
package org.flowable.ui.common.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.ui.common.model.RemoteToken;
import org.flowable.ui.common.model.RemoteUser;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.flowable.ui.common.security.CookieConstants;
import org.flowable.ui.common.security.FlowableAppUser;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableCookieFilter.class);

    protected static final String DELIMITER = ":";

    protected final RemoteIdmService remoteIdmService;

    protected final FlowableCommonAppProperties properties;

    protected FlowableCookieFilterCallback filterCallback;

    protected String idmAppUrl;
    protected String redirectUrlOnAuthSuccess;

    protected Collection<String> requiredPrivileges;

    // Caching the persistent tokens and users to avoid hitting the database too often
    // (eg when doing multiple requests at the same time)
    protected LoadingCache<String, RemoteToken> tokenCache;

    protected LoadingCache<String, FlowableAppUser> userCache;

    public FlowableCookieFilter(RemoteIdmService remoteIdmService, FlowableCommonAppProperties properties) {
        this.remoteIdmService = remoteIdmService;
        this.properties = properties;
    }

    @PostConstruct
    protected void initCaches() {
        initIdmAppRedirectUrl();
        initTokenCache();
        initUserCache();
    }

    protected void initIdmAppRedirectUrl() {
        idmAppUrl = properties.determineIdmAppRedirectUrl();

        redirectUrlOnAuthSuccess = properties.getRedirectOnAuthSuccess();
    }

    protected void initTokenCache() {
        FlowableCommonAppProperties.Cache cache = properties.getCacheLoginTokens();
        Long maxSize = cache.getMaxSize();
        Long maxAge = cache.getMaxAge();
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
        FlowableCommonAppProperties.Cache cache = properties.getCacheLoginUsers();
        Long userMaxSize = cache.getMaxSize();
        Long userMaxAge = cache.getMaxAge();
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

        for (int i = 0; i < tokens.length; i++)
        {
            try
            {
                tokens[i] = URLDecoder.decode(tokens[i], StandardCharsets.UTF_8.toString());
            }
            catch (UnsupportedEncodingException e)
            {
                logger.error(e.getMessage(), e);
            }
        }

        return tokens;
    }

    public Collection<String> getRequiredPrivileges() {
        return requiredPrivileges;
    }

    public void setRequiredPrivileges(Collection<String> requiredPrivileges) {
        this.requiredPrivileges = requiredPrivileges;
    }

    @Autowired(required = false)
    public void setFilterCallback(FlowableCookieFilterCallback filterCallback) {
        this.filterCallback = filterCallback;
    }
}
