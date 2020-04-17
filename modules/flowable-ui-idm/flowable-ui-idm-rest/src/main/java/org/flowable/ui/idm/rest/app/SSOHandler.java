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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.ui.idm.model.SSOUserInfo;
import org.springframework.util.MultiValueMap;

/**
 * Handler for login in or registering through a custom defined SSO.
 *
 * @author mattydebie
 */
public interface SSOHandler {

    /**
     * This is purely to use when no SSO is present.
     * When overriding the SSOHandler, make #isActive() return true to show the 'login with external service' link on the login page.
     *
     * @return bool,    wheter to use the SSO or not
     */
    boolean isActive();

    /**
     * This is the method where all the custom logic is present.
     * the #IdmSSOResource calls this method and expects an SSOUserInfo to continue
     *
     * @param request   HttpServletRequest
     * @param response  HttpServletResponse
     * @param body      Map or null, depends on whether the sso returns with a POST or a GET
     * @return          SSOUserInfo
     */
    SSOUserInfo handleSsoReturn(HttpServletRequest request, HttpServletResponse response, MultiValueMap<String, String> body);

    /**
     * Method that returns the full URL to the custom implemented SSO.
     *
     * @param idmUrl    String, current url; used as redirectUrl for the SSO
     * @return          String, url to external SSO
     */
    String getExternalUrl(String idmUrl);

}
