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
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.net.HttpHeaders;

/**
 * @author Matthias De Bie
 * <p>
 * TODO: update user before logging in
 * TODO: make a config option to automatically redirect to sso in stead of idm
 */
@RestController
@RequestMapping("/app")
public class IdmSSOResource {

    @Autowired
    protected UserService userService;

    @Autowired
    protected PrivilegeService privilegeService;

    @Autowired
    protected SSOHandler ssoHandler;

    @Autowired
    protected RememberMeServices rememberMeServices;

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
                loginWithSso(request, response, userInfo.getUser());

            } catch (NotFoundException e) {
                // userInfo not found: create new account
                createSsoAccount(request, response, ssoUserInfo);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

    private static Logger LOGGER = LoggerFactory.getLogger(IdmSSOResource.class);

    private void loginWithSso(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        String redirectTo = request.getRequestURL().toString().replace("/app/sso/return", "");

        if (request.getCookies() != null) {
            Optional<Cookie> optional = Arrays.stream(request.getCookies())
                .filter(cookie -> "redirectUrl".equals(cookie.getName()))
                .findFirst();

            if (optional.isPresent()) {
                redirectTo = URLDecoder.decode(optional.get().getValue());
            }
        }

        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", redirectTo);
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
            // userInfo.getTenant()   !! This depends on PR 1444 !!
        );

        if (userInfo.getPrivileges() != null) {

            for (Privilege priv : privilegeService.findPrivileges()) {
                if (userInfo.getPrivileges().contains(priv.getName())) {
                    LOGGER.debug("privilege added: {}", priv.getName());
                    privilegeService.addUserPrivilege(priv.getId(), userInfo.getId());
                }
            }

            try {
                loginWithSso(request, response, userService.getUserInformation(userInfo.getId()).getUser());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            response.setContentType(MediaType.TEXT_HTML_VALUE);

            try {
                OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);

                osw.append("<div style='display: flex; justify-content: center; align-items: center; width: 100%; height: 100vh; padding: 0; margin: 0;'>");
                osw.append("<h1>Please wait for an admin to approve your account.</h1>");
                osw.append("</div>");

                osw.flush();
                osw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
