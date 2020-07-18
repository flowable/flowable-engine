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
package org.flowable.ui.common.properties;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Common Rest properties for the UI Apps.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.rest.app")
public class FlowableRestAppProperties {

    /**
     * Configures the way user credentials are verified when doing a REST API call:
     * 'any-user' : the user needs to exist and the password need to match. Any user is allowed to do the call (this is the pre 6.3.0 behavior)
     * 'verify-privilege' : the user needs to exist, the password needs to match and the user needs to have the 'rest-api' privilege
     * If nothing set, defaults to 'verify-privilege'
     */
    private String authenticationMode = "verify-privilege";

    public String getAuthenticationMode() {
        return authenticationMode;
    }

    public void setAuthenticationMode(String authenticationMode) {
        this.authenticationMode = authenticationMode;
    }

    public boolean isVerifyRestApiPrivilege() {
        if (StringUtils.hasText(authenticationMode)) {
            return Objects.equals("verify-privilege", authenticationMode);
        }

        return true;
    }
}
