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
package org.activiti.app.security;

import java.lang.reflect.Method;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.idm.api.IdmIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

/**
 * Custom implementation of Spring Security's RememberMeServices.
 * <p/>
 * Persistent tokens are used by Spring Security to automatically log in users.
 * <p/>
 * This is a specific implementation of Spring Security's remember-me authentication, but it is much more powerful than the standard implementations:
 * <ul>
 * <li>It allows a user to see the list of his currently opened sessions, and invalidate them</li>
 * <li>It stores more information, such as the IP address and the user agent, for audit purposes
 * <li>
 * <li>When a user logs out, only his current session is invalidated, and not all of his sessions</li>
 * </ul>
 * <p/>
 * This is inspired by:
 * <ul>
 * <li><a href="http://jaspan.com/improved_persistent_login_cookie_best_practice">Improved Persistent Login Cookie Best Practice</a></li>
 * <li><a href="https://github.com/blog/1661-modeling-your-app-s-user-session">Github's "Modeling your App's User Session"</a></li></li>
 * </ul>
 * <p/>
 * The main algorithm comes from Spring Security's PersistentTokenBasedRememberMeServices, but this class couldn't be cleanly extended.
 * <p/>
 */
@Service
public class CustomPersistentRememberMeServices extends AbstractRememberMeServices {

  private final Logger log = LoggerFactory.getLogger(CustomPersistentRememberMeServices.class);

  public static final String COOKIE_NAME = "ACTIVITI_REMEMBER_ME";

  @Autowired
  private CustomUserDetailService customUserDetailService;

  @Autowired
  private IdmIdentityService identityService;

  private final int tokenMaxAgeInSeconds;
  private final long tokenMaxAgeInMilliseconds;
  private final long tokenRefreshDurationInMilliseconds;

  @Autowired
  public CustomPersistentRememberMeServices(Environment env, org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
    super(env.getProperty("security.rememberme.key"), userDetailsService);

    setAlwaysRemember(true);

    Integer tokenMaxAgeSeconds = env.getProperty("security.cookie.max-age", Integer.class);
    if (tokenMaxAgeSeconds != null) {
      log.info("Cookie max-age set to " + tokenMaxAgeSeconds + " seconds");
    } else {
      tokenMaxAgeSeconds = 2678400; // Default: 31 days
    }
    tokenMaxAgeInSeconds = tokenMaxAgeSeconds;
    tokenMaxAgeInMilliseconds = tokenMaxAgeSeconds.longValue() * 1000L;

    Integer tokenRefreshSeconds = env.getProperty("security.cookie.refresh-age", Integer.class);
    if (tokenRefreshSeconds != null) {
      log.info("Cookie refresh age set to " + tokenRefreshSeconds + " seconds");
    } else {
      tokenRefreshSeconds = 86400; // Default : 1 day
    }
    tokenRefreshDurationInMilliseconds = tokenRefreshSeconds.longValue() * 1000L;

    setCookieName(COOKIE_NAME);
  }

  @Override
  protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
    String userEmail = successfulAuthentication.getName();

    log.debug("Creating new persistent login for user {}", userEmail);
    ActivitiAppUser activitiAppUser = (ActivitiAppUser) successfulAuthentication.getPrincipal();

    addCookie(request, response);
  }

  @Override
  @Transactional
  protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {

    System.out.println("processAutoLoginCookie " + cookieTokens);

    return customUserDetailService.loadByUserId("admin");
  }

  /**
   * When logout occurs, only invalidate the current token, and not all user sessions.
   * <p/>
   * The standard Spring Security implementations are too basic: they invalidate all tokens for the current user, so when he logs out from one browser, all his other sessions are destroyed.
   */
  @Override
  @Transactional
  public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    super.logout(request, response, authentication);
  }

  private void addCookie(HttpServletRequest request, HttpServletResponse response) {
    setCookie(new String[] { "1111", "2222" }, tokenMaxAgeInSeconds, request, response);
  }

  @Override
  protected void setCookie(String[] tokens, int maxAge, HttpServletRequest request, HttpServletResponse response) {
    String cookieValue = encodeCookie(tokens);
    Cookie cookie = new Cookie(getCookieName(), cookieValue);
    cookie.setMaxAge(maxAge);
    cookie.setPath("/");

    cookie.setSecure(request.isSecure());

    Method setHttpOnlyMethod = ReflectionUtils.findMethod(Cookie.class, "setHttpOnly", boolean.class);
    if (setHttpOnlyMethod != null) {
      ReflectionUtils.invokeMethod(setHttpOnlyMethod, cookie, Boolean.TRUE);
    } else if (logger.isDebugEnabled()) {
      logger.debug("Note: Cookie will not be marked as HttpOnly because you are not using Servlet 3.0 (Cookie#setHttpOnly(boolean) was not found).");
    }

    response.addCookie(cookie);
  }
}
