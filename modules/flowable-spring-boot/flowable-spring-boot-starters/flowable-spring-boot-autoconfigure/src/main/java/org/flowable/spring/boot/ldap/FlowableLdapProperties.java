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
package org.flowable.spring.boot.ldap;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.flowable.ldap.LDAPConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.idm.ldap")
public class FlowableLdapProperties {

    /**
     * Whether to enable LDAP IDM Service.
     */
    private boolean enabled = false;

    /**
     * The server host on which the LDAP system can be reached. For example 'ldap://localhost'.
     */
    private String server;

    /**
     * The port on which the LDAP system is running.
     */
    private int port = -1;

    /**
     * The user id that is used to connect to the LDAP system.
     */
    private String user;

    /**
     * The password that is used to connect to the LDAP system.
     */
    private String password;

    /**
     * The class name for the initial context factory.
     */
    private String initialContextFactory = "com.sun.jndi.ldap.LdapCtxFactory";

    /**
     * The value that is used for the 'java.naming.security.authentication' property used to connect to the LDAP system.
     */
    private String securityAuthentication = "simple";

    /**
     * Allows to set all LDAP connection parameters which do not have a dedicated setter. See for example http://docs.oracle.com/javase/tutorial/jndi/ldap/jndi.html for custom properties. Such
     * properties are for example to configure connection pooling, specific security settings, etc.
     * <p>
     * All the provided parameters will be provided when creating an InitialDirContext, ie when a connection to the LDAP system is established.
     */
    private Map<String, String> customConnectionParameters = new HashMap<>();

    /**
     * The base 'distinguished name' (DN) from which the searches for users and groups are started.
     * <p>
     * Use {@link #userBaseDn} or {@link #groupBaseDn} when needing to differentiate between user and group base DN.
     */
    private String baseDn;

    /**
     * The base 'distinguished name' (DN) from which the searches for users are started.
     */
    private String userBaseDn;

    /**
     * The base 'distinguished name' (DN) from which the searches for groups are started.
     */
    private String groupBaseDn;

    /**
     * The timeout (in milliseconds) that is used when doing a search in LDAP. By default set to '0', which means 'wait forever'.
     */
    private int searchTimeLimit = 0;

    /**
     * Sets if connections to the LDAP system should be pooled and reused. Enabled by default.
     */
    private boolean connectionPooling = true;

    /**
     * Configuration for the queries performed by the IDM Service.
     */
    @NestedConfigurationProperty
    private final Query query = new Query();

    /**
     * Configuration for the attributes for the queries.
     */
    @NestedConfigurationProperty
    private final Attribute attribute = new Attribute();

    /**
     * Configuration for the LDAP caching.
     */
    @NestedConfigurationProperty
    private final Cache cache = new Cache();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

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

    public String getInitialContextFactory() {
        return initialContextFactory;
    }

    public void setInitialContextFactory(String initialContextFactory) {
        this.initialContextFactory = initialContextFactory;
    }

    public String getSecurityAuthentication() {
        return securityAuthentication;
    }

    public void setSecurityAuthentication(String securityAuthentication) {
        this.securityAuthentication = securityAuthentication;
    }

    public Map<String, String> getCustomConnectionParameters() {
        return customConnectionParameters;
    }

    public void setCustomConnectionParameters(Map<String, String> customConnectionParameters) {
        this.customConnectionParameters = customConnectionParameters;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getUserBaseDn() {
        return userBaseDn;
    }

    public void setUserBaseDn(String userBaseDn) {
        this.userBaseDn = userBaseDn;
    }

    public String getGroupBaseDn() {
        return groupBaseDn;
    }

    public void setGroupBaseDn(String groupBaseDn) {
        this.groupBaseDn = groupBaseDn;
    }

    public int getSearchTimeLimit() {
        return searchTimeLimit;
    }

    public void setSearchTimeLimit(int searchTimeLimit) {
        this.searchTimeLimit = searchTimeLimit;
    }

    public boolean isConnectionPooling() {
        return connectionPooling;
    }

    public void setConnectionPooling(boolean connectionPooling) {
        this.connectionPooling = connectionPooling;
    }

    public Query getQuery() {
        return query;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public Cache getCache() {
        return cache;
    }

    public void customize(LDAPConfiguration configuration) {
        configuration.setServer(getServer());
        configuration.setPort(getPort());
        configuration.setUser(getUser());
        configuration.setPassword(getPassword());
        configuration.setInitialContextFactory(getInitialContextFactory());
        configuration.setSecurityAuthentication(getSecurityAuthentication());
        configuration.setCustomConnectionParameters(getCustomConnectionParameters());
        configuration.setBaseDn(getBaseDn());
        configuration.setUserBaseDn(getUserBaseDn());
        configuration.setGroupBaseDn(getGroupBaseDn());
        configuration.setSearchTimeLimit(getSearchTimeLimit());
        configuration.setConnectionPooling(isConnectionPooling());
        query.customize(configuration);
        attribute.customize(configuration);
        cache.customize(configuration);
    }

    public static class Query {

        /**
         * The query that is executed when searching for a user by userId.
         * <p>
         * For example: (&amp;(objectClass=inetOrgPerson)(uid={0}))
         * <p>
         * Here, all the objects in LDAP with the class 'inetOrgPerson' and who have the matching 'uid' attribute value will be returned.
         * <p>
         * As shown in the example, the user id is injected by the typical {@link java.text.MessageFormat}, ie by using <i>{0}</i>
         * <p>
         * If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different
         * {@link org.flowable.ldap.LDAPQueryBuilder}, which allows for more customization than only the query.
         */
        private String userById;

        /**
         * The query that is executed when searching for a user by full name.
         * <p>
         * For example: (&amp;(objectClass=inetOrgPerson)(|({0}=*{1}*)({2}={3})))
         * <p>
         * Here, all the objects in LDAP with the class 'inetOrgPerson' and who have the matching first name or last name will be returned
         * <p>
         * Several things will be injected in the expression: {0} : the first name attribute {1} : the search text {2} : the last name attribute {3} : the search text
         * <p>
         * If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different
         * {@link org.flowable.ldap.LDAPQueryBuilder}, which allows for more customization than only the query.
         */
        private String userByFullNameLike;

        /**
         * The query that is executed when searching for all users.
         */
        private String allUsers;

        /**
         * The query that is executed when searching for the groups of a specific user.
         * <p>
         * For example: (&amp;(objectClass=groupOfUniqueNames)(uniqueMember={0}))
         * <p>
         * Here, all the objects in LDAP with the class 'groupOfUniqueNames' and where the provided DN is a 'uniqueMember' are returned.
         * <p>
         * As shown in the example, the user id is injected by the typical {@link java.text.MessageFormat}, ie by using <i>{0}</i>
         * <p>
         * If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different
         * {@link org.flowable.ldap.LDAPQueryBuilder}, which allows for more customization than only the query.
         */
        private String groupsForUser;

        /**
         * The query that is executed when searching for all groups.
         */
        private String allGroups;

        /**
         * The query that is executed when searching for a group by groupId.
         *
         * <p>
         *     For example: (&amp;(objectClass=organizationalRole)(cn={0}))
         * </p>
         *
         * The group id will be injected as {0}
         */
        private String groupById;

        public String getUserById() {
            return userById;
        }

        public void setUserById(String userById) {
            this.userById = userById;
        }

        public String getUserByFullNameLike() {
            return userByFullNameLike;
        }

        public void setUserByFullNameLike(String userByFullNameLike) {
            this.userByFullNameLike = userByFullNameLike;
        }

        public String getAllUsers() {
            return allUsers;
        }

        public void setAllUsers(String allUsers) {
            this.allUsers = allUsers;
        }

        public String getGroupsForUser() {
            return groupsForUser;
        }

        public void setGroupsForUser(String groupsForUser) {
            this.groupsForUser = groupsForUser;
        }

        public String getAllGroups() {
            return allGroups;
        }

        public void setAllGroups(String allGroups) {
            this.allGroups = allGroups;
        }

        public String getGroupById() {
            return groupById;
        }

        public void setGroupById(String groupById) {
            this.groupById = groupById;
        }

        public void customize(LDAPConfiguration configuration) {
            configuration.setQueryUserByUserId(getUserById());
            configuration.setQueryUserByFullNameLike(getUserByFullNameLike());
            configuration.setQueryAllUsers(getAllUsers());
            configuration.setQueryGroupsForUser(getGroupsForUser());
            configuration.setQueryAllGroups(getAllGroups());
            configuration.setQueryGroupByGroupId(getGroupById());
        }
    }

    public static class Attribute {

        /**
         * Name of the attribute that matches the user id.
         * <p>
         * This property is used when looking for a {@link org.flowable.idm.api.User} object and the mapping between the LDAP object and the Flowable
         * {@link org.flowable.idm.api.User} object is done.
         * <p>
         * This property is optional and is only needed if searching for {@link org.flowable.idm.api.User} objects using the Flowable API.
         */
        private String userId;

        /**
         * Name of the attribute that matches the user first name.
         * <p>
         * This property is used when looking for a {@link org.flowable.idm.api.User} object and the mapping between the LDAP object and the Flowable
         * {@link org.flowable.idm.api.User} object is done.
         */
        private String firstName;

        /**
         * Name of the attribute that matches the user last name.
         * <p>
         * This property is used when looking for a {@link org.flowable.idm.api.User} object and the mapping between the LDAP object and the Flowable
         * {@link org.flowable.idm.api.User} object is done.
         */
        private String lastName;

        /**
         * Name of the attribute that matches the user email.
         * <p>
         * This property is used when looking for a {@link org.flowable.idm.api.User} object and the mapping between the LDAP object and the Flowable
         * {@link org.flowable.idm.api.User} object is done.
         */
        private String email;

        /**
         * Name of the attribute that matches the group id.
         * <p>
         * This property is used when looking for a {@link org.flowable.idm.api.Group} object and the mapping between the LDAP object and the Flowable
         * {@link org.flowable.idm.api.Group} object is done.
         */
        private String groupId;

        /**
         * Name of the attribute that matches the group name.
         * <p>
         * This property is used when looking for a {@link org.flowable.idm.api.Group} object and the mapping between the LDAP object and the Flowable
         * {@link org.flowable.idm.api.Group} object is done.
         */
        private String groupName;

        /**
         * Name of the attribute that matches the group type.
         * <p>
         * This property is used when looking for a {@link org.flowable.idm.api.Group} object and the mapping between the LDAP object and the Flowable
         * {@link org.flowable.idm.api.Group} object is done.
         */
        private String groupType;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
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

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getGroupType() {
            return groupType;
        }

        public void setGroupType(String groupType) {
            this.groupType = groupType;
        }

        public void customize(LDAPConfiguration configuration) {
            configuration.setUserIdAttribute(getUserId());
            configuration.setUserFirstNameAttribute(getFirstName());
            configuration.setUserLastNameAttribute(getLastName());
            configuration.setUserEmailAttribute(getEmail());
            configuration.setGroupIdAttribute(getGroupId());
            configuration.setGroupNameAttribute(getGroupName());
            configuration.setGroupTypeAttribute(getGroupType());
        }
    }

    public static class Cache {

        /**
         * Allows to set the size of the {@link org.flowable.ldap.LDAPGroupCache}. This is an LRU cache that caches groups for users and thus avoids hitting
         * the LDAP system each time the groups of a user needs to be known.
         * <p>
         * The cache will not be instantiated if the value is less then zero. By default set to -1, so no caching is done.
         * <p>
         * Note that the group cache is instantiated on the {@link org.flowable.ldap.LDAPIdentityServiceImpl}. As such, if you have a custom implementation of
         * the {@link org.flowable.ldap.LDAPIdentityServiceImpl}, do not forget to add the group cache functionality.
         */
        private int groupSize = -1;

        /**
         * Sets the expiration time of the {@link org.flowable.ldap.LDAPGroupCache} in milliseconds. When groups for a specific user are fetched, and if the
         * group cache exists (see {@link #groupSize}), the
         * groups will be stored in this cache for the time set in this property. ie. when the groups were fetched at 00:00 and the expiration time is 30 mins, any fetch of the groups for that user after
         * 00:30 will not come from the cache, but do a fetch again from the LDAP system. Likewise, everything group fetch for that user done between 00:00 - 00:30 will come from the cache.
         * <p>
         * By default set to one hour.
         */
        //TODO once we move to Boot 2.0 we can use Duration as a parameter’
        private long groupExpiration = Duration.of(1, ChronoUnit.HOURS).toMillis();

        public int getGroupSize() {
            return groupSize;
        }

        public void setGroupSize(int groupSize) {
            this.groupSize = groupSize;
        }

        public long getGroupExpiration() {
            return groupExpiration;
        }

        public void setGroupExpiration(int groupExpiration) {
            this.groupExpiration = groupExpiration;
        }

        public void customize(LDAPConfiguration configuration) {
            configuration.setGroupCacheSize(getGroupSize());
            configuration.setGroupCacheExpirationTime(getGroupExpiration());
        }
    }
}
