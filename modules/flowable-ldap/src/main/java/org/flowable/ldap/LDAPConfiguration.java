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
package org.flowable.ldap;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.naming.directory.InitialDirContext;
import javax.naming.spi.InitialContextFactory;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.ldap.LDAPGroupCache.LDAPGroupCacheListener;

/**
 * A {@link org.flowable.engine.configurator.ProcessEngineConfigurator} that integrates a LDAP system with the Flowable process engine. The LDAP system will be consulted primarily for getting user information and in particular for
 * fetching groups of a user.
 * <p>
 * This class is extensible and many methods can be overridden when the default behavior is not fitting your use case.
 * <p>
 * Check the docs (specifically the setters) to see how this class can be tweaked.
 *
 * @author Joram Barrez
 */
public class LDAPConfiguration {

    /* Server connection params */

    protected String server;
    protected int port;
    protected String user;
    protected String password;
    protected String initialContextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
    protected String securityAuthentication = "simple";

    // For parameters like connection pooling settings, etc.
    protected Map<String, String> customConnectionParameters = new HashMap<>();

    // Query configuration
    protected String baseDn;
    protected String userBaseDn;
    protected String groupBaseDn;
    protected int searchTimeLimit = 0; // Default '0' == wait forever

    protected String queryUserByUserId;
    protected String queryGroupsForUser;
    protected String queryUserByFullNameLike;
    protected String queryAllUsers;
    protected String queryAllGroups;
    protected String queryGroupByGroupId;

    // Attribute names
    protected String userIdAttribute;
    protected String userFirstNameAttribute;
    protected String userLastNameAttribute;
    protected String userEmailAttribute;

    protected String groupIdAttribute;
    protected String groupNameAttribute;
    protected String groupTypeAttribute;

    // Pluggable query helper bean
    protected LDAPQueryBuilder ldapQueryBuilder = new LDAPQueryBuilder();

    // Group caching
    protected int groupCacheSize = -1;
    protected long groupCacheExpirationTime = 3600000L; // default: one hour

    // Cache listener (experimental)
    protected LDAPGroupCacheListener groupCacheListener;

    protected boolean connectionPooling = true;

    // Getters and Setters //////////////////////////////////////////////////

    public String getServer() {
        return server;
    }

    /**
     * The server on which the LDAP system can be reached. For example 'ldap://localhost:33389'.
     */
    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    /**
     * The port on which the LDAP system is running.
     */
    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    /**
     * The user id that is used to connect to the LDAP system.
     */
    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    /**
     * The password that is used to connect to the LDAP system.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getInitialContextFactory() {
        return initialContextFactory;
    }

    /**
     * The {@link InitialContextFactory} name used to connect to the LDAP system.
     * <p>
     * By default set to 'com.sun.jndi.ldap.LdapCtxFactory'.
     */
    public void setInitialContextFactory(String initialContextFactory) {
        this.initialContextFactory = initialContextFactory;
    }

    public String getSecurityAuthentication() {
        return securityAuthentication;
    }

    /**
     * The value that is used for the 'java.naming.security.authentication' property used to connect to the LDAP system.
     * <p>
     * By default set to 'simple'.
     */
    public void setSecurityAuthentication(String securityAuthentication) {
        this.securityAuthentication = securityAuthentication;
    }

    public Map<String, String> getCustomConnectionParameters() {
        return customConnectionParameters;
    }

    /**
     * Allows to set all LDAP connection parameters which do not have a dedicated setter. See for example http://docs.oracle.com/javase/tutorial/jndi/ldap/jndi.html for custom properties. Such
     * properties are for example to configure connection pooling, specific security settings, etc.
     * <p>
     * All the provided parameters will be provided when creating a {@link InitialDirContext}, ie when a connection to the LDAP system is established.
     */
    public void setCustomConnectionParameters(Map<String, String> customConnectionParameters) {
        this.customConnectionParameters = customConnectionParameters;
    }

    public String getBaseDn() {
        return baseDn;
    }

    /**
     * The base 'distinguished name' (DN) from which the searches for users and groups are started.
     * <p>
     * Use {@link #setUserBaseDn(String)} or {@link #setGroupBaseDn(String)} when needing to differentiate between user and group base DN.
     */
    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getUserBaseDn() {
        return userBaseDn;
    }

    /**
     * The base 'distinguished name' (DN) from which the searches for users are started.
     */
    public void setUserBaseDn(String userBaseDn) {
        this.userBaseDn = userBaseDn;
    }

    public String getGroupBaseDn() {
        return groupBaseDn;
    }

    /**
     * The base 'distinguished name' (DN) from which the searches for groups are started.
     */
    public void setGroupBaseDn(String groupBaseDn) {
        this.groupBaseDn = groupBaseDn;
    }

    public int getSearchTimeLimit() {
        return searchTimeLimit;
    }

    /**
     * The timeout that is used when doing a search in LDAP. By default set to '0', which means 'wait forever'.
     */
    public void setSearchTimeLimit(int searchTimeLimit) {
        this.searchTimeLimit = searchTimeLimit;
    }

    public String getQueryUserByUserId() {
        return queryUserByUserId;
    }

    /**
     * The query that is executed when searching for a user by userId.
     * <p>
     * For example: (&amp;(objectClass=inetOrgPerson)(uid={0}))
     * <p>
     * Here, all the objects in LDAP with the class 'inetOrgPerson' and who have the matching 'uid' attribute value will be returned.
     * <p>
     * As shown in the example, the user id is injected by the typical {@link MessageFormat}, ie by using <i>{0}</i>
     * <p>
     * If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different {@link LDAPQueryBuilder}, which allows for more customization than only the
     * query.
     */
    public void setQueryUserByUserId(String queryUserByUserId) {
        this.queryUserByUserId = queryUserByUserId;
    }

    public String getQueryGroupsForUser() {
        return queryGroupsForUser;
    }

    public String getQueryUserByFullNameLike() {
        return queryUserByFullNameLike;
    }

    /**
     * The query that is executed when searching for a user by full name.
     * <p>
     * For example: (&amp;(objectClass=inetOrgPerson)(|({0}=*{1}*)({2}={3})))
     * <p>
     * Here, all the objects in LDAP with the class 'inetOrgPerson' and who have the matching first name or last name will be returned
     * <p>
     * Several things will be injected in the expression: {0} : the first name attribute {1} : the search text {2} : the last name attribute {3} : the search text
     * <p>
     * If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different {@link LDAPQueryBuilder}, which allows for more customization than only the
     * query.
     */
    public void setQueryUserByFullNameLike(String queryUserByFullNameLike) {
        this.queryUserByFullNameLike = queryUserByFullNameLike;
    }

    public String getQueryAllUsers() {
        return queryAllUsers;
    }

    /**
     * The query that is executed when searching for all users.
     */
    public void setQueryAllUsers(String queryAllUsers) {
        this.queryAllUsers = queryAllUsers;
    }

    public String getQueryAllGroups() {
        return queryAllGroups;
    }

    /**
     * The query that is executed when searching for all groups.
     */
    public void setQueryAllGroups(String queryAllGroups) {
        this.queryAllGroups = queryAllGroups;
    }

    /**
     * Query that is executed when searching for one group by a specific group id
     */
    public String getQueryGroupByGroupId() {
        return queryGroupByGroupId;
    }

    /**
     * Query that is executed when searching for one group by a specific group id
     */
    public void setQueryGroupByGroupId(String queryGroupByGroupId) {
        this.queryGroupByGroupId = queryGroupByGroupId;
    }

    /**
     * The query that is executed when searching for the groups of a specific user.
     * <p>
     * For example: (&amp;(objectClass=groupOfUniqueNames)(uniqueMember={0}))
     * <p>
     * Here, all the objects in LDAP with the class 'groupOfUniqueNames' and where the provided DN is a 'uniqueMember' are returned.
     * <p>
     * As shown in the example, the user id is injected by the typical {@link MessageFormat}, ie by using <i>{0}</i>
     * <p>
     * If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different {@link LDAPQueryBuilder}, which allows for more customization than only the
     * query.
     */
    public void setQueryGroupsForUser(String queryGroupsForUser) {
        this.queryGroupsForUser = queryGroupsForUser;
    }

    public String getUserIdAttribute() {
        return userIdAttribute;
    }

    /**
     * Name of the attribute that matches the user id.
     * <p>
     * This property is used when looking for a {@link User} object and the mapping between the LDAP object and the Flowable {@link User} object is done.
     * <p>
     * This property is optional and is only needed if searching for {@link User} objects using the Flowable API.
     */
    public void setUserIdAttribute(String userIdAttribute) {
        this.userIdAttribute = userIdAttribute;
    }

    public String getUserFirstNameAttribute() {
        return userFirstNameAttribute;
    }

    /**
     * Name of the attribute that matches the user first name.
     * <p>
     * This property is used when looking for a {@link User} object and the mapping between the LDAP object and the Flowable {@link User} object is done.
     */
    public void setUserFirstNameAttribute(String userFirstNameAttribute) {
        this.userFirstNameAttribute = userFirstNameAttribute;
    }

    public String getUserLastNameAttribute() {
        return userLastNameAttribute;
    }

    /**
     * Name of the attribute that matches the user last name.
     * <p>
     * This property is used when looking for a {@link User} object and the mapping between the LDAP object and the Flowable {@link User} object is done.
     */
    public void setUserLastNameAttribute(String userLastNameAttribute) {
        this.userLastNameAttribute = userLastNameAttribute;
    }

    public String getUserEmailAttribute() {
        return userEmailAttribute;
    }

    /**
     * Name of the attribute that matches the user email.
     * <p>
     * This property is used when looking for a {@link User} object and the mapping between the LDAP object and the Flowable {@link User} object is done.
     */
    public void setUserEmailAttribute(String userEmailAttribute) {
        this.userEmailAttribute = userEmailAttribute;
    }

    public String getGroupIdAttribute() {
        return groupIdAttribute;
    }

    /**
     * Name of the attribute that matches the group id.
     * <p>
     * This property is used when looking for a {@link Group} object and the mapping between the LDAP object and the Flowable {@link Group} object is done.
     */
    public void setGroupIdAttribute(String groupIdAttribute) {
        this.groupIdAttribute = groupIdAttribute;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    /**
     * Name of the attribute that matches the group name.
     * <p>
     * This property is used when looking for a {@link Group} object and the mapping between the LDAP object and the Flowable {@link Group} object is done.
     */
    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    public String getGroupTypeAttribute() {
        return groupTypeAttribute;
    }

    /**
     * Name of the attribute that matches the group type.
     * <p>
     * This property is used when looking for a {@link Group} object and the mapping between the LDAP object and the Flowable {@link Group} object is done.
     */
    public void setGroupTypeAttribute(String groupTypeAttribute) {
        this.groupTypeAttribute = groupTypeAttribute;
    }

    /**
     * Set a custom {@link LDAPQueryBuilder} if the default implementation is not suitable. The {@link LDAPQueryBuilder} instance is used when the {@link LDAPUserManager} or {@link LDAPGroupManager}
     * does an actual query against the LDAP system.
     * <p>
     * The default implementation uses the properties as set on this instance such as {@link #setQueryGroupsForUser(String)} and {@link #setQueryUserByUserId(String)}.
     */
    public LDAPQueryBuilder getLdapQueryBuilder() {
        return ldapQueryBuilder;
    }

    public void setLdapQueryBuilder(LDAPQueryBuilder ldapQueryBuilder) {
        this.ldapQueryBuilder = ldapQueryBuilder;
    }

    public int getGroupCacheSize() {
        return groupCacheSize;
    }

    /**
     * Allows to set the size of the {@link LDAPGroupCache}. This is an LRU cache that caches groups for users and thus avoids hitting the LDAP system each time the groups of a user needs to be known.
     * <p>
     * The cache will not be instantiated if the value is less then zero. By default set to -1, so no caching is done.
     * <p>
     * Note that the group cache is instantiated on the {@link LDAPGroupManagerFactory}. As such, if you have a custom implementation of the {@link LDAPGroupManagerFactory}, do not forget to add the
     * group cache functionality.
     */
    public void setGroupCacheSize(int groupCacheSize) {
        this.groupCacheSize = groupCacheSize;
    }

    public long getGroupCacheExpirationTime() {
        return groupCacheExpirationTime;
    }

    /**
     * Sets the expiration time of the {@link LDAPGroupCache} in milliseconds. When groups for a specific user are fetched, and if the group cache exists (see {@link #setGroupCacheSize(int)}), the
     * groups will be stored in this cache for the time set in this property. ie. when the groups were fetched at 00:00 and the expiration time is 30 mins, any fetch of the groups for that user after
     * 00:30 will not come from the cache, but do a fetch again from the LDAP system. Likewise, everything group fetch for that user done between 00:00 - 00:30 will come from the cache.
     * <p>
     * By default set to one hour.
     */
    public void setGroupCacheExpirationTime(long groupCacheExpirationTime) {
        this.groupCacheExpirationTime = groupCacheExpirationTime;
    }

    public LDAPGroupCacheListener getGroupCacheListener() {
        return groupCacheListener;
    }

    public void setGroupCacheListener(LDAPGroupCacheListener groupCacheListener) {
        this.groupCacheListener = groupCacheListener;
    }

    /**
     * Sets if connections to the LDAP system should be pooled and reused.
     * <p>
     * Enabled by default.
     */
    public void setConnectionPooling(boolean connectionPooling) {
        this.connectionPooling = connectionPooling;
    }

    public boolean isConnectionPooling() {
        return connectionPooling;
    }
}
