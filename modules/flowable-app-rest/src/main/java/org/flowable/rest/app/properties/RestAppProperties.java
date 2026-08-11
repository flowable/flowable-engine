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

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for the rest app.
 *
 * @author Filip Hrisafov
 * @author Tim Stephenson
 */
@ConfigurationProperties(prefix = "flowable.rest.app")
public class RestAppProperties {

    /**
     * Configures the way user credentials are verified when doing a REST API call:
     * 'any-user' : the user needs to exist and the password need to match. Any user is allowed to do the call (this is the pre 6.3.0 behavior)
     * 'verify-privilege' : the user needs to exist, the password needs to match and the user needs to have the 'rest-api' privilege
     * 'pre-auth' : the request is trusted to have been authenticated by a reverse proxy in front of the app, and the user id is
     *              read from a request header (see {@link PreAuth}) instead of HTTP Basic. The password is not checked; privileges
     *              are still loaded from the IDM engine so authorization behaves as with 'verify-privilege'. Only use this when the
     *              app cannot be reached except through a trusted proxy that strips any client-supplied copy of the header.
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
    private final Cors cors = new Cors();

    @NestedConfigurationProperty
    private final Admin admin = new Admin();

    @NestedConfigurationProperty
    private final PreAuth preAuth = new PreAuth();

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

    public Cors getCors() {
        return cors;
    }

    public Admin getAdmin() {
        return admin;
    }

    public PreAuth getPreAuth() {
        return preAuth;
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

    /**
     * Settings for the 'pre-auth' authentication mode, where a trusted reverse proxy has already
     * authenticated the caller and passes the user id in a request header.
     */
    public static class PreAuth {

        /**
         * The request header that carries the already-authenticated user id. Defaults to
         * {@code X-Forwarded-User}, which is what most authenticating reverse proxies emit
         * (oauth2-proxy, and Databricks Apps also forwards {@code X-Forwarded-Email} /
         * {@code X-Forwarded-Preferred-Username}).
         */
        private String principalHeader = "X-Forwarded-User";

        public String getPrincipalHeader() {
            return principalHeader;
        }

        public void setPrincipalHeader(String principalHeader) {
            this.principalHeader = principalHeader;
        }
    }

    public static class Cors {
        /**
         * Enable/disable CORS filter.
         */
        private boolean enabled = false;

        /**
         * Allow/disallow CORS credentials.
         */
        private boolean allowCredentials = false;

        /**
         * Allowed CORS origins, use * for all, but not in production. Default empty.
         */
        private Set<String> allowedOrigins;

        /**
         * Allowed CORS headers, use * for all, but not in production. Default empty.
         */
        private Set<String> allowedHeaders;

        /**
         * Exposed CORS headers, use * for all, but not in production. Default empty.
         */
        private Set<String> exposedHeaders;

        /**
         * Allowed CORS methods, use * for all, but not in production. Default empty.
         */
        private Set<String> allowedMethods;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public Set<String> getAllowedOrigins() {
            return allowedOrigins == null ? Collections.emptySet() : allowedOrigins;
        }

        public void setAllowedOrigins(Set<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public Set<String> getAllowedHeaders() {
            return allowedHeaders == null ? Collections.emptySet() : allowedHeaders;
        }

        public void setAllowedHeaders(Set<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public Set<String> getExposedHeaders() {
            return exposedHeaders == null ? Collections.emptySet() : exposedHeaders;
        }

        public void setExposedHeaders(Set<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public Set<String> getAllowedMethods() {
            return allowedMethods == null ? Collections.emptySet() : allowedMethods;
        }

        public void setAllowedMethods(Set<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }
    }
}
