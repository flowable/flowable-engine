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
package org.flowable.ui.common.security;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.idm.api.Token;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.util.ReflectionUtils;

/**
 * Custom implementation of Spring Security's RememberMeServices.
 * <p>
 * Persistent tokens are used by Spring Security to automatically log in users.
 * </p>
 * <p>
 * This is a specific implementation of Spring Security's remember-me authentication, but it is much more powerful than the standard implementations:
 * <ul>
 * <li>It allows a user to see the list of his currently opened sessions, and invalidate them</li>
 * <li>It stores more information, such as the IP address and the user agent, for audit purposes
 * <li>
 * <li>When a user logs out, only his current session is invalidated, and not all of his sessions</li>
 * </ul>
 * <p>
 * This is inspired by:
 * <ul>
 * <li><a href="http://jaspan.com/improved_persistent_login_cookie_best_practice">Improved Persistent Login Cookie Best Practice</a></li>
 * <li><a href="https://github.com/blog/1661-modeling-your-app-s-user-session">Github's "Modeling your App's User Session"</a></li>
 * </ul>
 * <p>
 * The main algorithm comes from Spring Security's PersistentTokenBasedRememberMeServices, but this class couldn't be cleanly extended.
 */
public class CustomPersistentRememberMeServices extends AbstractRememberMeServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPersistentRememberMeServices.class);

    private final PersistentTokenService persistentTokenService;

    private final String tokenDomain;
    private final int tokenMaxAgeInSeconds;
    private final long tokenMaxAgeInMilliseconds;
    private final long tokenRefreshDurationInMilliseconds;

    public CustomPersistentRememberMeServices(FlowableCommonAppProperties properties, UserDetailsService userDetailsService,
            PersistentTokenService persistentTokenService) {
        super(properties.getSecurity().getRememberMeKey(), userDetailsService);
        this.persistentTokenService = persistentTokenService;

        setAlwaysRemember(true);

        FlowableCommonAppProperties.Cookie cookie = properties.getSecurity().getCookie();
        tokenMaxAgeInSeconds = (int) cookie.getMaxAge().getSeconds();
        LOGGER.info("Cookie max-age set to {} seconds", tokenMaxAgeInSeconds);
        tokenMaxAgeInMilliseconds = tokenMaxAgeInSeconds * 1000L;

        String domain = cookie.getDomain();
        if (domain != null) {
            LOGGER.info("Cookie domain set to {}", domain);
        }
        tokenDomain = domain;

        int tokenRefreshSeconds = (int) cookie.getRefreshAge().getSeconds();
        LOGGER.info("Cookie refresh age set to {} seconds", tokenRefreshSeconds);
        tokenRefreshDurationInMilliseconds = tokenRefreshSeconds * 1000L;

        setCookieName(CookieConstants.COOKIE_NAME);
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
        String userEmail = successfulAuthentication.getName();

        Token token = createAndInsertPersistentToken(userEmail, request.getRemoteAddr(), request.getHeader("User-Agent"));
        addCookie(token, request, response);
    }

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {

        Token token = getPersistentToken(cookieTokens);

        // Refresh token if refresh period has been passed
        if (new Date().getTime() - token.getTokenDate().getTime() > tokenRefreshDurationInMilliseconds) {

            // log.info("Refreshing persistent login token for user '{}', series '{}'", token.getUserId(), token.getSeries());
            try {

                // Refreshing: creating a new token to be used for subsequent calls

                token = persistentTokenService.createToken(token.getUserId(), request.getRequestURI(), request.getHeader("User-Agent"));
                addCookie(token, request, response);

            } catch (DataAccessException e) {
                LOGGER.error("Failed to update token: ", e);
                throw new RememberMeAuthenticationException("Autologin failed due to data access problem: " + e.getMessage());
            }

        }

        return getUserDetailsService().loadUserByUsername(token.getUserId());
    }

    /**
     * When logout occurs, only invalidate the current token, and not all user sessions.
     * <p>
     * The standard Spring Security implementations are too basic: they invalidate all tokens for the current user, so when he logs out from one browser, all his other sessions are destroyed.
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String rememberMeCookie = extractRememberMeCookie(request);
        if (rememberMeCookie != null && rememberMeCookie.length() != 0) {
            try {
                String[] cookieTokens = decodeCookie(rememberMeCookie);
                Token token = getPersistentToken(cookieTokens);
                persistentTokenService.delete(token);
            } catch (InvalidCookieException ice) {
                LOGGER.info("Invalid cookie, no persistent token could be deleted");
            } catch (RememberMeAuthenticationException rmae) {
                LOGGER.debug("No persistent token found, so no token could be deleted");
            }
        }
        super.logout(request, response, authentication);
    }

    /**
     * Validate the token and return it.
     */
    private Token getPersistentToken(String[] cookieTokens) {
        if (cookieTokens.length != 2) {
            throw new InvalidCookieException("Cookie token did not contain " + 2 + " tokens, but contained '" + Arrays.asList(cookieTokens) + "'");
        }

        final String presentedSeries = cookieTokens[0];
        final String presentedToken = cookieTokens[1];

        Token token = persistentTokenService.getToken(presentedSeries);

        if (token == null) {
            // No series match, so we can't authenticate using this cookie
            throw new RememberMeAuthenticationException("No persistent token found for series id: " + presentedSeries);
        }

        // We have a match for this user/series combination
        if (!presentedToken.equals(token.getTokenValue())) {

            // This could be caused by the opportunity window where the token just has been refreshed, but
            // has not been put into the token cache yet. Invalidate the token and refetch and it the new token value from the db is now returned.

            token = persistentTokenService.invalidateCacheEntryAndGetToken(presentedSeries, true); // Note the 'true' here, which invalidates the cache before fetching
            if (token != null && !presentedToken.equals(token.getTokenValue())) {

                // Token doesn't match series value. Delete this session and throw an exception.
                persistentTokenService.delete(token);

                throw new CookieTheftException("Invalid remember-me token (Series/token) mismatch. Implies previous cookie theft attack.");

            }
        }

        if (new Date().getTime() - token.getTokenDate().getTime() > tokenMaxAgeInMilliseconds) {
            throw new RememberMeAuthenticationException("Remember-me login has expired");
        }
        return token;
    }

    private void addCookie(Token token, HttpServletRequest request, HttpServletResponse response) {
        setCookie(new String[] { token.getId(), token.getTokenValue() }, tokenMaxAgeInSeconds, request, response);
    }

    @Override
    protected void setCookie(String[] tokens, int maxAge, HttpServletRequest request, HttpServletResponse response) {
        String cookieValue = encodeCookie(tokens);
        Cookie cookie = new Cookie(getCookieName(), cookieValue);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        if (tokenDomain != null) {
            cookie.setDomain(tokenDomain);
        }

        String xForwardedProtoHeader = request.getHeader("X-Forwarded-Proto");
        if (xForwardedProtoHeader != null) {
            cookie.setSecure("https".equals(xForwardedProtoHeader) || request.isSecure());
        } else {
            cookie.setSecure(request.isSecure());
        }

        Method setHttpOnlyMethod = ReflectionUtils.findMethod(Cookie.class, "setHttpOnly", boolean.class);
        if (setHttpOnlyMethod != null) {
            ReflectionUtils.invokeMethod(setHttpOnlyMethod, cookie, Boolean.TRUE);
        } else if (logger.isDebugEnabled()) {
            logger.debug("Note: Cookie will not be marked as HttpOnly because you are not using Servlet 3.0 (Cookie#setHttpOnly(boolean) was not found).");
        }

        response.addCookie(cookie);
    }

    public Token createAndInsertPersistentToken(String userId, String remoteAddress, String userAgent) {
        return persistentTokenService.createToken(userId, remoteAddress, userAgent);
    }
}
