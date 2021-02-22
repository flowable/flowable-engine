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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.Assert;

/**
 * Common configuration properties that needs to be shared by all the UI apps.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.common.app")
public class FlowableCommonAppProperties {

    /**
     * The static tenant id used for the DefaultTenantProvider. The modeler app uses this to determine under which tenant id to store and publish models.
     * When not provided, empty or only contains whitespace it defaults to the user's tenant id if available otherwise it uses no tenant id.
     */
    private String tenantId;

    /**
     * The default role prefix that needs to be used by Spring Security.
     */
    private String rolePrefix = "ROLE_";

    /**
     * The URL to the IDM application, used for the user info and token info REST GET calls. It's also used as a fallback for the redirect url to the login page in the UI apps.
     */
    private String idmUrl;
    
    /**
     * The redirect URL to the IDM application, used for the login redirect when the cookie isn't set or is invalid.
     */
    private String idmRedirectUrl;

    /**
     * The URL to which the redirect should occur after a successful authentication.
     */
    private String redirectOnAuthSuccess;

    /**
     * The cache configuration for the login tokens.
     */
    @NestedConfigurationProperty
    private final Cache cacheLoginTokens = new Cache();

    /**
     * The cache configuration for the login users.
     */
    @NestedConfigurationProperty
    private final Cache cacheLoginUsers = new Cache();

    /**
     * The cache configuration for the users.
     */
    @NestedConfigurationProperty
    private final Cache cacheUsers = new Cache();

    /**
     * The information for the IDM Admin user.
     */
    @NestedConfigurationProperty
    private final Admin idmAdmin = new Admin();

    /**
     * The information for the idm engine based security
     */
    @NestedConfigurationProperty
    private final Security security = new Security();

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public String getIdmUrl() {
        return idmUrl;
    }

    public void setIdmUrl(String idmUrl) {
        this.idmUrl = idmUrl;
    }
    
    public String getIdmRedirectUrl() {
        return idmRedirectUrl;
    }

    public void setIdmRedirectUrl(String idmRedirectUrl) {
        this.idmRedirectUrl = idmRedirectUrl;
    }

    public String getRedirectOnAuthSuccess() {
        return redirectOnAuthSuccess;
    }

    public void setRedirectOnAuthSuccess(String redirectOnAuthSuccess) {
        this.redirectOnAuthSuccess = redirectOnAuthSuccess;
    }

    public Cache getCacheLoginTokens() {
        return cacheLoginTokens;
    }

    public Cache getCacheLoginUsers() {
        return cacheLoginUsers;
    }

    public Cache getCacheUsers() {
        return cacheUsers;
    }

    public Admin getIdmAdmin() {
        return idmAdmin;
    }

    public Security getSecurity() {
        return security;
    }

    public String determineIdmAppUrl() {
        String idmAppUrl = getIdmUrl();
        Assert.hasText(idmAppUrl, "`flowable.common.app.idm-url` must be set");

        if (!idmAppUrl.endsWith("/")) {
            idmAppUrl += "/";
        }

        return idmAppUrl;
    }
    
    public String determineIdmAppRedirectUrl() {
        String idmAppRedirectUrl = getIdmRedirectUrl();
        if (idmAppRedirectUrl != null && idmAppRedirectUrl.length() > 0) {
            if (!idmAppRedirectUrl.endsWith("/")) {
                idmAppRedirectUrl += "/";
            }
            
            return idmAppRedirectUrl;
        }
        
        return determineIdmAppUrl();
    }

    /**
     * The cache configuration for the for login users and token.
     */
    public static class Cache {

        /**
         * The maximum number of entries that the cache should contain.
         */
        private long maxSize = 2048L;

        /**
         * The max age in seconds after which the entry should be invalidated.
         */
        private long maxAge = 30L;

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class Admin {

        /**
         * The username used for executing the REST calls (with basic auth) to the IDM REST services. Default is admin
         */
        private String user = "admin";

        /**
         * The password used for executing the REST calls (with basic auth) to the IDM REST services. Default is test.
         */
        private String password;

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
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

        /**
         * The type of the security for the UI Apps.
         */
        private String type = "idm";

        /**
         * The OAuth2 configuration.
         */
        private final OAuth2 oAuth2 = new OAuth2();

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public OAuth2 getOAuth2() {
            return oAuth2;
        }
    }

    /**
     * Security properties for the OAuth2 configuration.
     */
    public static class OAuth2 {

        /**
         * The attribute that contains the authorities that should be mapped for the authenticated user.
         */
        private String authoritiesAttribute;

        /**
         * The attribute that contains the groups for the authenticated user.
         */
        private String groupsAttribute;

        /**
         * The default authorities that should be added to every user.
         */
        private Collection<String> defaultAuthorities;

        /**
         * The default groups that should be added to every user.
         */
        private Collection<String> defaultGroups;

        /**
         * The key of the attribute that holds the first name of the user.
         */
        private String firstNameAttribute;

        /**
         * The key of the attribute that holds the last name of the user.
         */
        private String lastNameAttribute;

        /**
         * The key of the attribute that holds the full of the user.
         */
        private String fullNameAttribute;

        /**
         * The key of the attribute that holds the email of the user.
         */
        private String emailAttribute;

        public String getAuthoritiesAttribute() {
            return authoritiesAttribute;
        }

        public void setAuthoritiesAttribute(String authoritiesAttribute) {
            this.authoritiesAttribute = authoritiesAttribute;
        }

        public String getGroupsAttribute() {
            return groupsAttribute;
        }

        public void setGroupsAttribute(String groupsAttribute) {
            this.groupsAttribute = groupsAttribute;
        }

        public Collection<String> getDefaultAuthorities() {
            return defaultAuthorities;
        }

        public void setDefaultAuthorities(Collection<String> defaultAuthorities) {
            this.defaultAuthorities = defaultAuthorities;
        }

        public Collection<String> getDefaultGroups() {
            return defaultGroups;
        }

        public void setDefaultGroups(Collection<String> defaultGroups) {
            this.defaultGroups = defaultGroups;
        }

        public String getFirstNameAttribute() {
            return firstNameAttribute;
        }

        public void setFirstNameAttribute(String firstNameAttribute) {
            this.firstNameAttribute = firstNameAttribute;
        }

        public String getLastNameAttribute() {
            return lastNameAttribute;
        }

        public void setLastNameAttribute(String lastNameAttribute) {
            this.lastNameAttribute = lastNameAttribute;
        }

        public String getFullNameAttribute() {
            return fullNameAttribute;
        }

        public void setFullNameAttribute(String fullNameAttribute) {
            this.fullNameAttribute = fullNameAttribute;
        }

        public String getEmailAttribute() {
            return emailAttribute;
        }

        public void setEmailAttribute(String emailAttribute) {
            this.emailAttribute = emailAttribute;
        }
    }

    /**
     * The configuration for the security remember me cookie.
     */
    public static class Cookie {

        /**
         * The max age of the security cookie. Default is 31 days.
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration maxAge = Duration.ofDays(31);

        /**
         * The domain for the cookie.
         */
        private String domain;

        /**
         * The refresh age of the token. Default is 1 day.
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration refreshAge = Duration.ofDays(1);

        public Duration getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Duration maxAge) {
            this.maxAge = maxAge;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public Duration getRefreshAge() {
            return refreshAge;
        }

        public void setRefreshAge(Duration refreshAge) {
            this.refreshAge = refreshAge;
        }
    }
}
