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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Special Flowable UI Specific {@link LoginUrlAuthenticationEntryPoint} that allows adding a redirect on auth success dynamically
 * to the redirect URL.
 *
 * @author Filip Hrisafov
 */
public class FlowableLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    protected String redirectUrlOnAuthSuccess;

    public FlowableLoginUrlAuthenticationEntryPoint(String idmAppUrl, String redirectOnAuthSuccess) {
        super(idmAppUrl);
        this.redirectUrlOnAuthSuccess = redirectOnAuthSuccess;
    }

    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        String baseRedirectUrl = getLoginFormUrl() + "idm/#/login?redirectOnAuthSuccess=true&redirectUrl=";
        if (redirectUrlOnAuthSuccess != null) {
            return baseRedirectUrl + redirectUrlOnAuthSuccess;

        } else {
            return baseRedirectUrl + request.getRequestURL();
        }
    }
}
