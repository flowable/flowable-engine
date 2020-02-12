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
package org.flowable.ui.idm.rest.app;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.idm.model.SSOAuthentication;
import org.flowable.ui.idm.model.SSOUserInfo;
import org.flowable.ui.idm.model.UserInformation;
import org.flowable.ui.idm.service.PrivilegeService;
import org.flowable.ui.idm.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.net.HttpHeaders;

/**
 * @author Matthias De Bie
 * @author Ruben De Swaef
 */
@RestController
@RequestMapping("/app")
public class IdmSSOResource {

    private static Logger LOGGER = LoggerFactory.getLogger(IdmSSOResource.class);

    @Autowired
    protected UserService userService;

    @Autowired
    protected PrivilegeService privilegeService;

    @Autowired
    protected SSOHandler ssoHandler;

    @Autowired
    protected RememberMeServices rememberMeServices;

    @Value("${flowable.common.app.idm-url:#{null}}")
    private String redirectIdmUrl;

    @RequestMapping(value = "/sso/external", method = RequestMethod.GET)
    public void getSsoExternalUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String urlBase = request.getRequestURL().toString().replace("/sso/external", "/sso/return");

        if (urlBase.startsWith("http://") && "https".equals(request.getHeader(HttpHeaders.X_FORWARDED_PROTO))) {
            urlBase = urlBase.replace("http://", "https://");
        }

        if (ssoHandler.isActive()) {
            response.getWriter().write(
                ssoHandler.getExternalUrl(urlBase)
            );
            response.flushBuffer();
        }
    }

    @RequestMapping(value = "/sso/return", method = RequestMethod.GET)
    public void onSsoReturnGet(HttpServletRequest request, HttpServletResponse response) {
        handleSSOReturn(request, response, null);
    }

    @RequestMapping(value = "/sso/return", method = RequestMethod.POST)
    public void onSsoReturn(@RequestBody MultiValueMap<String, String> body, HttpServletRequest request, HttpServletResponse response) {
        handleSSOReturn(request, response, body);
    }

    private void handleSSOReturn(HttpServletRequest request, HttpServletResponse response, MultiValueMap<String, String> body) {
        SSOUserInfo ssoUserInfo = ssoHandler.handleSsoReturn(request, response, body);

        if (ssoUserInfo != null) {
            try {
                UserInformation userInfo = userService.getUserInformation(ssoUserInfo.getId());
                loginWithSso(request, response, userInfo.getUser(), ssoUserInfo);

            } catch (NotFoundException e) {
                // userInfo not found: create new account
                createSsoAccount(request, response, ssoUserInfo);

            } catch (IOException e) {
                LOGGER.error("Could not use an account with this userinformation.");
                e.printStackTrace();
            }
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

    private void loginWithSso(HttpServletRequest request, HttpServletResponse response, User user, SSOUserInfo userInfo) throws IOException {
        // If there is nog redirect cookie found, redirect to the IDM
        String redirectTo = redirectIdmUrl;

        if (request.getCookies() != null) {
            Optional<Cookie> optional = Arrays.stream(request.getCookies())
                .filter(cookie -> "redirectUrl".equals(cookie.getName()))
                .findFirst();

            if (optional.isPresent()) {
                redirectTo = URLDecoder.decode(optional.get().getValue(), "UTF-8");
            }
        }

        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", redirectTo);
        if(userInfo != null){
            userService.updateUserDetails(
                userInfo.getId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getEmail()
                // userInfo.getTenant()   !! This depends on PR 1444/1687, merged in 6.5.0
            );
        }
        rememberMeServices.loginSuccess(request, response, new SSOAuthentication(user));
        response.getOutputStream().flush();
    }

    private void createSsoAccount(HttpServletRequest request, HttpServletResponse response, SSOUserInfo userInfo) {
        userService.createNewUser(
            userInfo.getId(),
            userInfo.getFirstName(),
            userInfo.getLastName(),
            userInfo.getEmail(),
            UUID.randomUUID().toString()
            // userInfo.getTenant()   !! This depends on PR 1444/1687, merged in 6.5.0
        );

        if (userInfo.getPrivileges() != null) {

            for (Privilege priv : privilegeService.findPrivileges()) {
                if (userInfo.getPrivileges().contains(priv.getName())) {
                    LOGGER.debug("privilege added: {}", priv.getName());
                    privilegeService.addUserPrivilege(priv.getId(), userInfo.getId());
                }
            }

            try {
                loginWithSso(request, response, userService.getUserInformation(userInfo.getId()).getUser(), null);
            } catch (IOException e) {
                LOGGER.error("Could not login with SSO after creating an account.");
                e.printStackTrace();
            }
        } else {
            // User has no access privileges
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
