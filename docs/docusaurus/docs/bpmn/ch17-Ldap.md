---
id: ch17-Ldap
title: LDAP integration
---

Companies often already have a user and group store in the form of an LDAP system. Flowable offers an out-of-the-box solution for easily configuring how Flowable should connect with an LDAP system.

In earlier versions, it was already possible to integrate LDAP, but since then, the configuration has been simplified a lot. However, the 'old' way of configuring LDAP still works. More specifically, the simplified configuration is just a wrapper on top of the 'old' infrastructure.

## Usage

To add the LDAP integration code to your project, simply add the following dependency to your pom.xml:

    <dependency>
      <groupId>org.flowable</groupId>
      <artifactId>flowable-ldap-configurator</artifactId>
      <version>latest.version</version>
    </dependency>

## Use cases

The LDAP integration has currently two main use cases:

-   Allow for authentication through the IdentityService. This could be useful when doing everything through the IdentityService.

-   Fetching the groups of a user. This is important when for example querying tasks to see which tasks a certain user can see (i.e. tasks with a candidate group).

## Configuration

Integrating the LDAP system with Flowable is done by injecting an instance of org.flowable.ldap.LDAPConfigurator in the idmProcessEngineConfigurator section of the process engine configuration. This class is highly extensible: methods can be easily overridden and many dependent beans are pluggable if the default implementation would not fit the use case.

This is an example configuration (note: of course, when creating the engine programmatically this is completely similar). Don’t worry about all the properties for now, we’ll look at them in detail in a next section.

    <bean id="processEngineConfiguration" class="...SomeProcessEngineConfigurationClass">
        ...
        <property name="idmProcessEngineConfigurator">
          <bean class="org.flowable.ldap.LDAPConfigurator">

            <property name="ldapConfiguration">
              <bean class="org.flowable.ldap.LDAPConfiguration">

                <!-- Server connection params -->
                <property name="server" value="ldap://localhost" />
                <property name="port" value="33389" />
                <property name="user" value="uid=admin, ou=users, o=flowable" />
                <property name="password" value="pass" />

                <!-- Query params -->
                <property name="baseDn" value="o=flowable" />
                <property name="queryUserByUserId" value="(&(objectClass=inetOrgPerson)(uid={0}))" />
                <property name="queryUserByFullNameLike" value="(&(objectClass=inetOrgPerson)(|({0}=*{1}*)({2}=*{3}*)))" />
                <property name="queryAllUsers" value="(objectClass=inetOrgPerson)" />
                <property name="queryGroupsForUser" value="(&(objectClass=groupOfUniqueNames)(uniqueMember={0}))" />
                <property name="queryAllGroups" value="(objectClass=groupOfUniqueNames)" />

                <!-- Attribute config -->
                <property name="userIdAttribute" value="uid" />
                <property name="userFirstNameAttribute" value="cn" />
                <property name="userLastNameAttribute" value="sn" />
                <property name="userEmailAttribute" value="mail" />

                <property name="groupIdAttribute" value="cn" />
                <property name="groupNameAttribute" value="cn" />

              </bean>
            </property>
          </bean>
        </property>
    </bean>

## Properties

Following properties can be set on org.flowable.ldap.LDAPConfiguration:

    .LDAP configuration properties

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Description</th>
<th>Type</th>
<th>Default value</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>server</p></td>
<td><p>The server on which the LDAP system can be reached. For example 'ldap://localhost:33389'</p></td>
<td><p>String</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>port</p></td>
<td><p>The port on which the LDAP system is running</p></td>
<td><p>int</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>user</p></td>
<td><p>The user id that is used to connect to the LDAP system</p></td>
<td><p>String</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>password</p></td>
<td><p>The password that is used to connect to the LDAP system</p></td>
<td><p>String</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>initialContextFactory</p></td>
<td><p>The InitialContextFactory name used to connect to the LDAP system</p></td>
<td><p>String</p></td>
<td><p>com.sun.jndi.ldap.LdapCtxFactory</p></td>
</tr>
<tr class="even">
<td><p>securityAuthentication</p></td>
<td><p>The value that is used for the 'java.naming.security.authentication' property used to connect to the LDAP system</p></td>
<td><p>String</p></td>
<td><p>simple</p></td>
</tr>
<tr class="odd">
<td><p>customConnectionParameters</p></td>
<td><p>Allows to set all LDAP connection parameters which do not have a dedicated setter.
See for example <a href="http://docs.oracle.com/javase/tutorial/jndi/ldap/jndi.html">http://docs.oracle.com/javase/tutorial/jndi/ldap/jndi.html</a> for custom
properties. Such properties are for example to configure connection pooling, specific
security settings, etc. All the provided parameters will be provided when creating a
connection to the LDAP system.</p></td>
<td><p>Map&lt;String, String&gt;</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>baseDn</p></td>
<td><p>The base 'distinguished name' (DN) from which the searches for users and groups are started</p></td>
<td><p>String</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>userBaseDn</p></td>
<td><p>The base 'distinguished name' (DN) from which the searches for users are started. If not provided, baseDn (see above) will be used</p></td>
<td><p>String</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>groupBaseDn</p></td>
<td><p>The base 'distinguished name' (DN) from which the searches for groups are started. If not provided, baseDn (see above) will be used</p></td>
<td><p>String</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>searchTimeLimit</p></td>
<td><p>The timeout that is used when doing a search in LDAP in milliseconds</p></td>
<td><p>long</p></td>
<td><p>one hour</p></td>
</tr>
<tr class="even">
<td><p>queryUserByUserId</p></td>
<td><p>The query that is executed when searching for a user by userId.
For example: (&amp;(objectClass=inetOrgPerson)(uid={0}))
Here, all the objects in LDAP with the class 'inetOrgPerson'
and who have the matching 'uid' attribute value will be returned.
As shown in the example, the user id is injected by using
{0}. If setting the query alone is insufficient for your specific
LDAP setup, you can alternatively plug in a different
LDAPQueryBuilder, which allows for more customization than only the query.</p></td>
<td><p>string</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>queryUserByFullNameLike</p></td>
<td><p>The query that is executed when searching for a user by full name.
For example: (&amp; (objectClass=inetOrgPerson) (</p></td>
<td><p>({0}=<strong>{1}</strong>)({2}=<strong>{3}</strong>)) )
Here, all the objects in LDAP with the class 'inetOrgPerson'
and who have the matching first name and last name values will be returned.
Note that {0} injects the firstNameAttribute (as defined above), {1} and {3} the search text
and {2} the lastNameAttribute. If setting the query alone is insufficient for your specific
LDAP setup, you can alternatively plug in a different
LDAPQueryBuilder, which allows for more customization than only the query.</p></td>
<td><p>string</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>queryAllUsers</p></td>
<td><p>The query that is executed when searching on users without a filter.
For example: (objectClass=inetOrgPerson)
Here, all the objects in LDAP with the class 'inetOrgPerson' will be returned.</p></td>
<td><p>string</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>queryGroupsForUser</p></td>
<td><p>The query that is executed when searching for the groups of a specific user.
For example: (&amp;(objectClass=groupOfUniqueNames)(uniqueMember={0}))
Here, all the objects in LDAP with the class 'groupOfUniqueNames'
and where the provided DN (matching a DN for a user) is a 'uniqueMember' are returned.
As shown in the example, the user id is injected by using {0}
If setting the query alone is insufficient for your specific
LDAP setup, you can alternatively plug in a different
LDAPQueryBuilder, which allows for more customization than only the query.</p></td>
<td><p>string</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>queryAllGroups</p></td>
<td><p>The query that is executed when searching on groups without a filter.
For example: (objectClass=groupOfUniqueNames)
Here, all the objects in LDAP with the class 'groupOfUniqueNames' will be returned.</p></td>
<td><p>string</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>userIdAttribute</p></td>
<td><p>Name of the attribute that matches the user id.
This property is used when looking for a User object
and the mapping between the LDAP object and the Flowable User object
is done.</p></td>
<td><p>string</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>userFirstNameAttribute</p></td>
<td><p>Name of the attribute that matches the user first name.
This property is used when looking for a User object
and the mapping between the LDAP object and the Flowable User object is done.</p></td>
<td><p>string</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>userLastNameAttribute</p></td>
<td><p>Name of the attribute that matches the user last name.
This property is used when looking for a User object
and the mapping between the LDAP object and the Flowable User object is done.</p></td>
<td><p>string</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>groupIdAttribute</p></td>
<td><p>Name of the attribute that matches the group id.
This property is used when looking for a Group object
and the mapping between the LDAP object and the Flowable Group object is done.</p></td>
<td><p>string</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>groupNameAttribute</p></td>
<td><p>Name of the attribute that matches the group name.
This property is used when looking for a Group object
and the mapping between the LDAP object and the Flowable Group object is done.</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>groupTypeAttribute</p></td>
<td><p>Name of the attribute that matches the group type.
This property is used when looking for a Group object
and the mapping between the LDAP object and the Flowable Group object
is done.</p></td>
<td><p>String</p></td>
</tr>
</tbody>
</table>

Following properties are when one wants to customize default behavior or introduced group caching:

<table>
<caption>Advanced properties</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Description</th>
<th>Type</th>
<th>Default value</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>ldapUserManagerFactory</p></td>
<td><p>Set a custom implementation of the LDAPUserManagerFactory if the default implementation is not suitable.</p></td>
<td><p>instance of LDAPUserManagerFactory</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>ldapGroupManagerFactory</p></td>
<td><p>Set a custom implementation of the LDAPGroupManagerFactory if the default implementation is not suitable.</p></td>
<td><p>instance of LDAPGroupManagerFactory</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>ldapMemberShipManagerFactory</p></td>
<td><p>Set a custom implementation of the LDAPMembershipManagerFactory if the default implementation is not suitable.
Note that this is very unlikely, as membership are managed in the LDAP system itself normally.</p></td>
<td><p>An instance of LDAPMembershipManagerFactory</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>ldapQueryBuilder</p></td>
<td><p>Set a custom query builder if the default implementation is not suitable.
The LDAPQueryBuilder instance is used when the LDAPUserManager or
LDAPGroupManage} does an actual query against the LDAP system.
The default implementation uses the properties as set on this instance
such as queryGroupsForUser and queryUserById</p></td>
<td><p>An instance of org.flowable.ldap.LDAPQueryBuilder</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>groupCacheSize</p></td>
<td><p>Allows to set the size of the group cache.
This is an LRU cache that caches groups for users and thus
avoids hitting the LDAP system each time the groups of
a user needs to be known.</p>
<p>The cache will not be instantiated if the value is less than zero.
By default set to -1, so no caching is done.</p></td>
<td><p>int</p></td>
<td><p>-1</p></td>
</tr>
<tr class="even">
<td><p>groupCacheExpirationTime</p></td>
<td><p>Sets the expiration time of the group cache in milliseconds.
When groups for a specific user are fetched, and if the group cache exists,
the groups will be stored in this cache for the time set in this property.
I.e. when the groups were fetched at 00:00 and the expiration time is 30 minutes,
any fetch of the groups for that user after 00:30 will not come from the cache, but do
a fetch again from the LDAP system. Likewise, everything group fetch for that user done
between 00:00 - 00:30 will come from the cache.</p></td>
<td><p>long</p></td>
<td><p>one hour</p></td>
</tr>
</tbody>
</table>

Note when using Active Directory: people have reported that for Active Directory, the 'InitialDirContext' needs to be set to Context.REFERRAL. This can be passed through the customConnectionParameters map as described above.
