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
package org.flowable.app.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Configuration properties for the remote IDM authentication.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.app.idm")
public class FlowableRemoteIdmProperties {

    /**
     * The URL to the IDM application, used for the login redirect when the cookie isn't set or is invalid, and for the user info and token info REST GET calls.
     */
    private String url;

    /**
     * The URL to which the redirect should occur after a successful authentication.
     */
    private String redirectOnAuthSuccess;

    /**
     * The cache configuration for the login tokens.
     */
    private final Cache cacheLoginTokens = new Cache();

    /**
     * The cache configuration for the login users.
     */
    private final Cache cacheLoginUsers = new Cache();

    /**
     * The cache configuration for the users.
     */
    private final Cache cacheUsers = new Cache();

    /**
     * The information for the IDM Admin user.
     */
    private final Admin admin = new Admin();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public Admin getAdmin() {
        return admin;
    }

    public String determineIdmAppUrl() {
        String idmAppUrl = getUrl();
        Assert.hasText(idmAppUrl, "`flowable.app.idm.url` must be set");

        if (!idmAppUrl.endsWith("/")) {
            idmAppUrl += "/";
        }

        return idmAppUrl;
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
