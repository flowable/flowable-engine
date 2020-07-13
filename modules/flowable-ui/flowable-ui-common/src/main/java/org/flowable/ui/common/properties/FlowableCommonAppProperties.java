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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
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
}
