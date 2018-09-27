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
package org.flowable.ui.idm.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for the IDM UI App.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.idm.app")
public class FlowableIdmAppProperties {

    /**
     * Enables the REST API (this is not the REST api used by the UI, but an api that's available over basic auth authentication).
     */
    private boolean restEnabled = true;

    /**
     * Whether the IDM App needs to be bootstrapped.
     */
    private boolean bootstrap = true;

    /**
     * The information for the admin user for bootstrapping the application.
     */
    @NestedConfigurationProperty
    private final Admin admin = new Admin();

    /**
     * The security configuration for the IDM UI App.
     */
    @NestedConfigurationProperty
    private final Security security = new Security();

    public boolean isRestEnabled() {
        return restEnabled;
    }

    public void setRestEnabled(boolean restEnabled) {
        this.restEnabled = restEnabled;
    }

    public boolean isBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(boolean bootstrap) {
        this.bootstrap = bootstrap;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Security getSecurity() {
        return security;
    }

    public static class Admin {
        //TODO maybe we need to move the bootstrap directly in the starter
        /**
         * The id of the admin user.
         */
        private String userId;

        /**
         * The password for the admin user.
         */
        private String password;

        /**
         * The first name of the admin user.
         */
        private String firstName;

        /**
         * The last name of the admin user.
         */
        private String lastName;

        /**
         * The email of the admin user.
         */
        private String email;

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * Security properties for the IDM UI App.
     */
    public static class Security {

        /**
         * The hash key that is used by Spring Security to hash the password values in the applications. Make sure that you change the value of this property.
         */
        private String rememberMeKey = "testKey";

        /**
         * The configuration for the security cookie.
         */
        @NestedConfigurationProperty
        private final Cookie cookie = new Cookie();

        /**
         * How long should a user be cached before invalidating it in the cache for the cacheable CustomUserDetailsService.
         */
        private long userValidityPeriod = 30000L;

        public String getRememberMeKey() {
            return rememberMeKey;
        }

        public void setRememberMeKey(String rememberMeKey) {
            this.rememberMeKey = rememberMeKey;
        }

        public Cookie getCookie() {
            return cookie;
        }

        public long getUserValidityPeriod() {
            return userValidityPeriod;
        }

        public void setUserValidityPeriod(long userValidityPeriod) {
            this.userValidityPeriod = userValidityPeriod;
        }
    }

    /**
     * The configuration for the security remember me cookie.
     */
    public static class Cookie {

        /**
         * The max age of the security cookie in seconds. Default is 31 days.
         */
        //TODO use duration with Boot 2
        private int maxAge = 2678400; //31 days

        /**
         * The domain for the cookie.
         */
        private String domain;

        /**
         * The refresh age of the token in seconds. Default is 1 day.
         */
        private int refreshAge = 86400; // Default : 1 day;

        public int getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(int maxAge) {
            this.maxAge = maxAge;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public int getRefreshAge() {
            return refreshAge;
        }

        public void setRefreshAge(int refreshAge) {
            this.refreshAge = refreshAge;
        }
    }
}
