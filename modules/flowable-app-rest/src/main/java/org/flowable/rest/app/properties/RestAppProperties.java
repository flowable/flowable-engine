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
package org.flowable.rest.app.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for the rest app.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.rest.app")
public class RestAppProperties {

    /**
     * Configures the way user credentials are verified when doing a REST API call:
     * 'any-user' : the user needs to exist and the password need to match. Any user is allowed to do the call (this is the pre 6.3.0 behavior)
     * 'verify-privilege' : the user needs to exist, the password needs to match and the user needs to have the 'rest-api' privilege
     * If nothing set, defaults to 'verify-privilege'
     */
    private String authenticationMode = "verify-privilege";

    /**
     * Deploys demo process definitions that allows to have some example data when using the REST APIs
     */
    private boolean createDemoDefinitions = true;

    /**
     * Enable/disable whether the docs are available on /docs
     */
    private boolean swaggerDocsEnabled = true;

    @NestedConfigurationProperty
    private final Admin admin = new Admin();

    /**
     * The default role prefix that needs to be used by Spring Security.
     */
    private String rolePrefix = "ROLE_";

    public String getAuthenticationMode() {
        return authenticationMode;
    }

    public void setAuthenticationMode(String authenticationMode) {
        this.authenticationMode = authenticationMode;
    }

    public boolean isCreateDemoDefinitions() {
        return createDemoDefinitions;
    }

    public void setCreateDemoDefinitions(boolean createDemoDefinitions) {
        this.createDemoDefinitions = createDemoDefinitions;
    }

    public boolean isSwaggerDocsEnabled() {
        return swaggerDocsEnabled;
    }

    public void setSwaggerDocsEnabled(boolean swaggerDocsEnabled) {
        this.swaggerDocsEnabled = swaggerDocsEnabled;
    }

    public Admin getAdmin() {
        return admin;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public static class Admin {

        private String userId;

        private String password;

        private String firstName;

        private String lastName;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
