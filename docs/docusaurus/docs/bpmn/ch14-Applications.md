---
id: ch14-Applications
title: Flowable applications
---

Flowable provides a web UI application to demonstrate and leverage the functionality provided by the Flowable project.  This application contains four main app components:

-   Flowable IDM: an Identity Management app that provides single sign-on authentication functionality for all the Flowable UI applications, and, for users with the IDM administrative privilege, it also provides functionality to manage users, groups and privileges.

-   Flowable Modeler: an app that allows users with modeler privileges to model processes, forms, decision tables and application definitions.

-   Flowable Task: a runtime task app that provides functionality to start process instances, edit task forms, complete tasks and query on tasks and process instances.

-   Flowable Admin: an administrative app that allows users with admin privilege to query the BPMN, DMN, form and content engines and provides several options to change process instances, tasks, jobs and so on. The admin app connects to the engines through the REST API that is deployed together with the Flowable UI application and the Flowable REST application.

This UI application is provided as a single WAR file that can be dropped in any web server or started with its embedded server.  Spring Boot starters are available for each separate component app.

The application and components are Spring Boot 2.0 based, which means that that the WAR file is actually executable and can be run as a normal standalone application.
See [The Executable Jar Format](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html#build-tool-plugins-maven-packaging) in the Spring Boot reference documentation.

Flowable also provides the `flowable-rest.war` which contains the Flowable REST API. More about this can be read in the [REST API](bpmn/ch15-REST.md) chapter.

## Flowable UI application installation

As mentioned before, the UI application can be deployed on a Tomcat server, and to get started this is probably the easiest approach when additional configuration settings are used. For this installation guide we’ll describe the installation of the application in a Tomcat server.

1.  Download a recent stable version of [Apache Tomcat](http://tomcat.apache.org).

2.  Download the latest stable [Flowable 6 version](http://www.flowable.org/downloads.html).

3.  Copy the flowable-ui.war file from the Flowable distribution *wars* folder to the Tomcat webapps folder.

4.  Startup the Tomcat server by running the bin/startup.sh (Mac OS and Linux) or bin/startup.bat (Windows) script.

5.  Open a web browser and go to [<http://localhost:8080/flowable-ui>](http://localhost:8080/flowable-ui).

The Flowable UI application should now be running with an H2 in-memory database and the following login screen should be shown in your web browser:

![flowable idm login screen](assets/bpmn/flowable_idm_login_screen.png)

By default, the Flowable IDM component will create an admin user that has privileges to all the Flowable UI apps. You can login with admin/test and the browser should go to the Flowable landing page:

![flowable landing screen](assets/bpmn/flowable_landing_screen.png)

Usually, you will want to change the default H2 in-memory database configuration to a MySQL or Postgres (or other persistent database) configuration.
You can do this by changing the application.properties file in the *WEB-INF/classes/* directory of the application.
However, it is easier to use the Spring Boot [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).
An example configuration can be found on [Github](https://github.com/flowable/flowable-engine/blob/master/modules/flowable-ui-task/flowable-ui-task-app/src/main/resources/flowable-default.properties)
To change the default configuration to MySQL the following changes are needed to the properties file:

    spring.datasource.driver-class-name=com.mysql.jdbc.Driver
    spring.datasource.url=jdbc:mysql://127.0.0.1:3306/flowable?characterEncoding=UTF-8
    spring.datasource.username=flowable
    spring.datasource.password=flowable

This configuration will expect a flowable database to be present in the MySQL server and the UI apps will automatically generate the necessary database tables. For Postgres the following changes are necessary:

    spring.datasource.driver-class-name=org.postgresql.Driver
    spring.datasource.url=jdbc:postgresql://localhost:5432/flowable
    spring.datasource.username=flowable
    spring.datasource.password=flowable

In addition to changing the configuration, make sure the database driver is available on the classpath. Again, you could do this for the web application by adding the driver JAR file to the WEB-INF/lib folder, but you can also copy the JAR file to the Tomcat lib folder. For MySQL and Postgres the database drivers can be downloaded from:

-   MySQL: [<https://dev.mysql.com/downloads/connector/j>](https://dev.mysql.com/downloads/connector/j)

-   Postgres: [<https://jdbc.postgresql.org/>](https://jdbc.postgresql.org/)

When running the UI as a standalone application the database driver can be added by using the `loader.path` property.

    java -Dloader.path=/location/to/your/driverfolder -jar flowable-ui.war

See the [`PropertiesLauncher` Features](https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html#executable-jar-property-launcher-features) in the Spring Boot reference documentation for more information.

## Flowable UI Application Configuration

As the Flowable UI app is a Spring Boot application, you can use all the properties Spring Boot provides.
In order to provide custom configuration for the application have a look at the [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) section of the Spring Boot documentation.

    You can also use YAML based properties.

<table>
<caption>Common UI App Properties</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Old Property</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>flowable.common.app.idm-url</p></td>
<td><p>idm.app.url</p></td>
<td><p>-</p></td>
<td><p>The URL to the IDM application, used for the user info and token info REST GET calls. It’s also used as a fallback for the redirect url to the login page in the UI apps. Required for standalone applications.</p></td>
</tr>
<tr class="even">
<td><p>flowable.common.app.idm-redirect-url</p></td>
<td><p>idm.app.redirect.url</p></td>
<td><p>-</p></td>
<td><p>The redirect URL to the IDM application, used for the login redirect when the cookie isn’t set or is invalid.Required for standalone applications.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.common.app.redirect-on-auth-success</p></td>
<td><p>app.redirect.url.on.authsuccess</p></td>
<td><p>-</p></td>
<td><p>The URL to which the redirect should occur after a successful authentication. Required for standalone applications.</p></td>
</tr>
<tr class="even">
<td><p>flowable.common.app.role-prefix</p></td>
<td><p>-</p></td>
<td><p>ROLE_</p></td>
<td><p>The default role prefix that needs to be used by Spring Security.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.common.app.tenant-id</p></td>
<td><p>-</p></td>
<td><p>-</p></td>
<td><p>The static tenant id used for the DefaultTenantProvider. The modeler app uses this to determine under which tenant id to store and publish models.
When not provided, empty or only contains whitespace it defaults to the user’s tenant id if available otherwise it uses no tenant id.</p></td>
</tr>
<tr class="even">
<td><p>flowable.common.app.cache-login-tokens.max-age</p></td>
<td><p>cache.login-tokens.max.age</p></td>
<td><p>30</p></td>
<td><p>The max age in seconds after which the entry should be invalidated.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.common.app.cache-login-tokens.max-size</p></td>
<td><p>cache.login-tokens.max.size</p></td>
<td><p>2048</p></td>
<td><p>The maximum number of entries that the cache should contain.</p></td>
</tr>
<tr class="even">
<td><p>flowable.common.app.cache-login-users.max-age</p></td>
<td><p>cache.login-users.max.age</p></td>
<td><p>30</p></td>
<td><p>The max age in seconds after which the entry should be invalidated.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.common.app.cache-login-users.max-size</p></td>
<td><p>cache.login-users.max.size</p></td>
<td><p>2048</p></td>
<td><p>The maximum number of entries that the cache should contain.</p></td>
</tr>
<tr class="even">
<td><p>flowable.common.app.cache-users.max-age</p></td>
<td><p>cache.users.max.age</p></td>
<td><p>30</p></td>
<td><p>The max age in seconds after which the entry should be invalidated.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.common.app.cache-users.max-size</p></td>
<td><p>cache.users.max.size</p></td>
<td><p>2048</p></td>
<td><p>The maximum number of entries that the cache should contain.</p></td>
</tr>
<tr class="even">
<td><p>flowable.common.app.idm-admin.password</p></td>
<td><p>idm.admin.password</p></td>
<td><p>-</p></td>
<td><p>The password used for executing the REST calls (with basic auth) to the IDM REST services. Default is test.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.common.app.idm-admin.user</p></td>
<td><p>idm.admin.user</p></td>
<td><p>admin</p></td>
<td><p>The username used for executing the REST calls (with basic auth) to the IDM REST services. Default is admin</p></td>
</tr>
<tr class="even">
<td><p>flowable.rest.app.authentication-mode</p></td>
<td><p>rest.authentication.mode</p></td>
<td><p>verify-privilege</p></td>
<td><p>Configures the way user credentials are verified when doing a REST API call:
'any-user' : the user needs to exist and the password need to match. Any user is allowed to do the call (this is the pre 6.3.0 behavior)
'verify-privilege' : the user needs to exist, the password needs to match and the user needs to have the 'rest-api' privilege
If nothing set, defaults to 'verify-privilege'</p></td>
</tr>
</tbody>
</table>

Some of the old properties have been moved to be managed by the Flowable Spring Boot starter (or Spring Boot itself)

<table>
<caption>Old properties managed by the Flowable Spring Boot Starter</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Old Property</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>flowable.async-executor-activate</p></td>
<td><p>engine.process.asyncexecutor.activate</p></td>
<td><p>true</p></td>
<td><p>Whether the async executor should be activated.</p></td>
</tr>
<tr class="even">
<td><p>flowable.database-schema-update</p></td>
<td><p>engine.process.schema.update</p></td>
<td><p>true</p></td>
<td><p>The strategy that should be used for the database schema.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.history-level</p></td>
<td><p>engine.process.history.level</p></td>
<td><p>-</p></td>
<td><p>The history level that needs to be used.</p></td>
</tr>
<tr class="even">
<td><p>flowable.process.servlet.name</p></td>
<td><p>flowable.rest-api-servlet-name</p></td>
<td><p>Flowable BPMN Rest API</p></td>
<td><p>The name of the Process servlet.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.process.servlet.path</p></td>
<td><p>flowable.rest-api-mapping</p></td>
<td><p>/process-api</p></td>
<td><p>The context path for the Process rest servlet.</p></td>
</tr>
<tr class="even">
<td><p>flowable.content.storage.create-root</p></td>
<td><p>contentstorage.fs.create-root</p></td>
<td><p>true</p></td>
<td><p>If the root folder doesn’t exist, should it be created?</p></td>
</tr>
<tr class="odd">
<td><p>flowable.content.storage.root-folder</p></td>
<td><p>contentstorage.fs.root-folder</p></td>
<td><p>-</p></td>
<td><p>Root folder location where content files will be stored, for example, task attachments or form file uploads.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.enabled</p></td>
<td><p>flowable.db-identity-used</p></td>
<td><p>true</p></td>
<td><p>Whether the idm engine needs to be started.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.password-encoder</p></td>
<td><p>security.passwordencoder</p></td>
<td><p>-</p></td>
<td><p>The type of the password encoder that needs to be used.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.base-dn</p></td>
<td><p>ldap.basedn</p></td>
<td><p>-</p></td>
<td><p>The base 'distinguished name' (DN) from which the searches for users and groups are started. Use 'user-base-dn' or 'group-base-dn' when needing to differentiate between user and group base DN.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.enabled</p></td>
<td><p>ldap.enabled</p></td>
<td><p>false</p></td>
<td><p>Whether to enable LDAP IDM Service.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.password</p></td>
<td><p>ldap.password</p></td>
<td><p>-</p></td>
<td><p>The password that is used to connect to the LDAP system.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.port</p></td>
<td><p>ldap.port</p></td>
<td><p>-1</p></td>
<td><p>The port on which the LDAP system is running.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.server</p></td>
<td><p>ldap.server</p></td>
<td><p>-</p></td>
<td><p>The server host on which the LDAP system can be reached. For example 'ldap://localhost'.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.user</p></td>
<td><p>ldap.user</p></td>
<td><p>-</p></td>
<td><p>The user id that is used to connect to the LDAP system.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.attribute.email</p></td>
<td><p>ldap.attribute.email</p></td>
<td><p>-</p></td>
<td><p>Name of the attribute that matches the user email.
This property is used when looking for an 'org.flowable.idm.api.User' object and the mapping between the LDAP object and the Flowable 'org.flowable.idm.api.User' object is done.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.attribute.first-name</p></td>
<td><p>ldap.attribute.firstname</p></td>
<td><p>-</p></td>
<td><p>Name of the attribute that matches the user first name.
This property is used when looking for a 'org.flowable.idm.api.User' object and the mapping between the LDAP object and the Flowable 'org.flowable.idm.api.User' object is done.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.attribute.group-id</p></td>
<td><p>ldap.attribute.groupid</p></td>
<td><p>-</p></td>
<td><p>Name of the attribute that matches the group id.
This property is used when looking for a 'org.flowable.idm.api.Group' object and the mapping between the LDAP object and the Flowable 'org.flowable.idm.api.Group' object is done.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.attribute.group-name</p></td>
<td><p>ldap.attribute.groupname</p></td>
<td><p>-</p></td>
<td><p>Name of the attribute that matches the group name.
This property is used when looking for a 'org.flowable.idm.api.Group' object and the mapping between the LDAP object and the Flowable 'org.flowable.idm.api.Group' object is done.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.attribute.last-name</p></td>
<td><p>ldap.attribute.lastname</p></td>
<td><p>-</p></td>
<td><p>Name of the attribute that matches the user last name.
This property is used when looking for a 'org.flowable.idm.api.User' object and the mapping between the LDAP object and the Flowable 'org.flowable.idm.api.User' object is done.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.attribute.user-id</p></td>
<td><p>ldap.attribute.userid</p></td>
<td><p>-</p></td>
<td><p>Name of the attribute that matches the user id.
This property is used when looking for a 'org.flowable.idm.api.User' object and the mapping between the LDAP object and the Flowable 'org.flowable.idm.api.User' object is done. This property is optional and is only needed if searching for 'org.flowable.idm.api.User' objects using the Flowable API.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.cache.group-size</p></td>
<td><p>ldap.cache.groupsize</p></td>
<td><p>-1</p></td>
<td><p>Allows to set the size of the 'org.flowable.ldap.LDAPGroupCache'.
This is an LRU cache that caches groups for users and thus avoids hitting the LDAP system each time the groups of a user needs to be known.
The cache will not be instantiated if the value is less then zero. By default set to -1, so no caching is done.
Note that the group cache is instantiated on the 'org.flowable.ldap.LDAPIdentityServiceImpl'.
As such, if you have a custom implementation of the 'org.flowable.ldap.LDAPIdentityServiceImpl', do not forget to add the group cache functionality.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.query.all-groups</p></td>
<td><p>ldap.query.groupall</p></td>
<td><p>-</p></td>
<td><p>The query that is executed when searching for all groups.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.query.all-users</p></td>
<td><p>ldap.query.userall</p></td>
<td><p>-</p></td>
<td><p>The query that is executed when searching for all users.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.query.groups-for-user</p></td>
<td><p>ldap.query.groupsforuser</p></td>
<td><p>-</p></td>
<td><p>The query that is executed when searching for the groups of a specific user.
For example: <code>(&amp;(objectClass=groupOfUniqueNames)(uniqueMember={0}))</code>
Here, all the objects in LDAP with the class 'groupOfUniqueNames' and where the provided DN is a 'uniqueMember' are returned.
As shown in the example, the user id is injected by the typical {@link java.text.MessageFormat}, ie by using <em>{0}</em>
If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different
<code>org.flowable.ldap.LDAPQueryBuilder</code>, which allows for more customization than only the query.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.ldap.query.user-by-full-name-like</p></td>
<td><p>ldap.query.userbyname</p></td>
<td><p>-</p></td>
<td><p>The query that is executed when searching for a user by full name.
For example: <code>(&amp;(objectClass=inetOrgPerson)(|({0}={1})({2}={3})))</code>
Here, all the objects in LDAP with the class 'inetOrgPerson' and who have the matching first name or last name will be returned
Several things will be injected in the expression: {0} : the first name attribute {1} : the search text {2} : the last name attribute {3} : the search text
If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different
'org.flowable.ldap.LDAPQueryBuilder', which allows for more customization than only the query.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.ldap.query.user-by-id</p></td>
<td><p>ldap.query.userbyid</p></td>
<td><p>-</p></td>
<td><p>The query that is executed when searching for a user by userId.
For example: <code>(&amp;(objectClass=inetOrgPerson)(uid={0}))</code>
Here, all the objects in LDAP with the class 'inetOrgPerson' and who have the matching 'uid' attribute value will be returned.
As shown in the example, the user id is injected by the typical {@link java.text.MessageFormat}, ie by using <em>{0}</em>
If setting the query alone is insufficient for your specific LDAP setup, you can alternatively plug in a different
'org.flowable.ldap.LDAPQueryBuilder', which allows for more customization than only the query.</p></td>
</tr>
<tr class="even">
<td><p>flowable.mail.server.host</p></td>
<td><p>email.host</p></td>
<td><p>localhost</p></td>
<td><p>The host of the mail server.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.mail.server.password</p></td>
<td><p>email.password</p></td>
<td><p>-</p></td>
<td><p>The password for the mail server authentication.</p></td>
</tr>
<tr class="even">
<td><p>flowable.mail.server.port</p></td>
<td><p>email.port</p></td>
<td><p>1025</p></td>
<td><p>The port of the mail server.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.mail.server.ssl-port</p></td>
<td><p>email.ssl-port</p></td>
<td><p>1465</p></td>
<td><p>The SSL port of the mail server.</p></td>
</tr>
<tr class="even">
<td><p>flowable.mail.server.use-ssl</p></td>
<td><p>email.use-ssl</p></td>
<td><p>false</p></td>
<td><p>Sets whether SSL/TLS encryption should be enabled for the SMTP transport upon connection (SMTPS/POPS).</p></td>
</tr>
<tr class="odd">
<td><p>flowable.mail.server.use-tls</p></td>
<td><p>email.use-tls</p></td>
<td><p>false</p></td>
<td><p>Set or disable the STARTTLS encryption.</p></td>
</tr>
<tr class="even">
<td><p>flowable.mail.server.username</p></td>
<td><p>email.username</p></td>
<td><p>-</p></td>
<td><p>The username that needs to be used for the mail server authentication.
If empty no authentication would be used.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.process.definition-cache-limit</p></td>
<td><p>flowable.process-definitions.cache.max</p></td>
<td><p>-1</p></td>
<td><p>The maximum amount of process definitions available in the process definition cache.
Per default it is -1 (all process definitions).</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Old properties managed by Spring Boot</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Old Property</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>spring.datasource.driver-class-name</p></td>
<td><p>datasource.driver</p></td>
<td><p>-</p></td>
<td><p>Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.</p></td>
</tr>
<tr class="even">
<td><p>spring.datasource.jndi-name</p></td>
<td><p>datasource.jndi.name</p></td>
<td><p>-</p></td>
<td><p>JNDI location of the datasource. Class, url, username &amp; password are ignored when
set.</p></td>
</tr>
<tr class="odd">
<td><p>spring.datasource.password</p></td>
<td><p>datasource.password</p></td>
<td><p>-</p></td>
<td><p>Login password of the database.</p></td>
</tr>
<tr class="even">
<td><p>spring.datasource.url</p></td>
<td><p>datasource.url</p></td>
<td><p>-</p></td>
<td><p>JDBC URL of the database.</p></td>
</tr>
<tr class="odd">
<td><p>spring.datasource.username</p></td>
<td><p>datasource.username</p></td>
<td><p>-</p></td>
<td><p>Login username of the database.</p></td>
</tr>
<tr class="even">
<td><p>spring.datasource.hikari.connection-test-query</p></td>
<td><p>datasource.preferred-test-query</p></td>
<td><p>-</p></td>
<td><p>The SQL query to be executed to test the validity of connections.</p></td>
</tr>
<tr class="odd">
<td><p>spring.datasource.hikari.connection-timeout</p></td>
<td><p>datasource.connection.timeout</p></td>
<td><p>-</p></td>
<td><p>The maximum number of milliseconds that a client will wait for a connection from the pool. If this time is exceeded without a connection becoming available, a SQLException will be thrown when getting a connection.</p></td>
</tr>
<tr class="even">
<td><p>spring.datasource.hikari.idle-timeout</p></td>
<td><p>datasource.connection.idletimeout</p></td>
<td><p>-</p></td>
<td><p>The maximum amount of time (in milliseconds) that a connection is allowed to sit idle in the pool.
Whether a connection is retired as idle or not is subject to a maximum variation of +30 seconds, and average variation of +15 seconds.
A connection will never be retired as idle before this timeout.
A value of 0 means that idle connections are never removed from the pool.</p></td>
</tr>
<tr class="odd">
<td><p>spring.datasource.hikari.max-lifetime</p></td>
<td><p>datasource.connection.maxlifetime</p></td>
<td><p>-</p></td>
<td><p>This property controls the maximum lifetime of a connection in the pool. When a connection reaches this
timeout, even if recently used, it will be retired from the pool. An in-use connection will never be
retired, only when it is idle will it be removed.</p></td>
</tr>
<tr class="even">
<td><p>spring.datasource.hikari.maximum-pool-size</p></td>
<td><p>datasource.connection.maxpoolsize</p></td>
<td><p>-</p></td>
<td><p>The property controls the maximum size that the pool is allowed to reach, including both idle and in-use
connections. Basically this value will determine the maximum number of actual connections to the database
backend.
When the pool reaches this size, and no idle connections are available, calls to getConnection() will
block for up to connectionTimeout milliseconds before timing out.</p></td>
</tr>
<tr class="odd">
<td><p>spring.datasource.hikari.minimum-idle</p></td>
<td><p>datasource.connection.minidle</p></td>
<td><p>-</p></td>
<td><p>The property controls the minimum number of idle connections that HikariCP tries to maintain in the pool,
including both idle and in-use connections. If the idle connections dip below this value, HikariCP will
make a best effort to restore them quickly and efficiently.</p></td>
</tr>
<tr class="even">
<td><p>spring.servlet.multipart.max-file-size</p></td>
<td><p>file.upload.max.size</p></td>
<td><p>10MB</p></td>
<td><p>Max file size. Values can use the suffixes "MB" or "KB" to indicate megabytes or kilobytes, respectively.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Not used old properties</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Old property</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>datasource.jndi.resource-ref</p></td>
<td><p>Spring Boot does not support configuring JNDI resourceRef. Use entire resource reference in the name.</p></td>
</tr>
<tr class="even">
<td><p>email.use-credentials</p></td>
<td><p>In case you don’t want to use credentials, set the password and user to empty.</p></td>
</tr>
</tbody>
</table>

## Flowable IDM app

The Flowable IDM app is used for authentication and authorization. The Flowable IDM is a simple identity management component that provides a central place to define users, groups and privileges.

The IDM component boots the IDM engine at startup and will create the identity tables as defined in the IDM engine in the datasource defined in the properties configuration.

When the Flowable application is deployed and started, it will check if there’s a user available in the ACT\_ID\_USER table, and if not it will use the `flowable.common.app.idm-admin.user` property to create a new default admin user in this table.
It will also add all available privileges in the Flowable project to the newly created admin user:

-   access-idm: provides the privilege to manage users, groups and privileges

-   access-admin: allows the user to login to the Flowable Admin app, manage the Flowable engines and access the Actuator endpoints of the application

-   access-modeler: enables access to the Flowable Modeler app

-   access-task: provides the privilege to login to the Flowable Task app

-   access-rest-api: allows the user to do call the REST API. Otherwise a 403 (forbidden) http status will be returned. Note that *flowable.rest.app.authentication-mode* needs to be set to *verify-privilege*, which is the default.

When opening the IDM app for the first time the following user overview screen is shown:

![flowable idm startup screen](assets/bpmn/flowable_idm_startup_screen.png)

In this screen users can be added, removed and updated. The groups section can be used to create, delete and update groups. In the group details view you can also add and remove users to and from the group. The privilege screen allows you to add and remove privileges from users and groups:

![flowable idm privilege screen](assets/bpmn/flowable_idm_privilege_screen.png)

There’s no option to define new privileges yet, but you can add and remove users and groups for the existing four privileges.

Following are the IDM UI app specific properties.

<table>
<caption>IDM UI App Properties</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Old Property</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>flowable.idm.app.bootstrap</p></td>
<td><p>idm.bootstrap.enabled</p></td>
<td><p>true</p></td>
<td><p>Whether the IDM App needs to be bootstrapped.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.app.rest-enabled</p></td>
<td><p>rest.idm-app.enabled</p></td>
<td><p>true</p></td>
<td><p>Enables the REST API (this is not the REST api used by the UI, but an api that’s available over basic auth authentication).</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.app.admin.email</p></td>
<td><p>admin.email</p></td>
<td><p>-</p></td>
<td><p>The email of the admin user.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.app.admin.first-name</p></td>
<td><p>admin.firstname</p></td>
<td><p>-</p></td>
<td><p>The first name of the admin user.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.app.admin.last-name</p></td>
<td><p>admin.lastname</p></td>
<td><p>-</p></td>
<td><p>The last name of the admin user.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.app.admin.password</p></td>
<td><p>admin.password</p></td>
<td><p>-</p></td>
<td><p>The password for the admin user.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.app.admin.user-id</p></td>
<td><p>admin.userid</p></td>
<td><p>-</p></td>
<td><p>The id of the admin user.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.app.security.remember-me-key</p></td>
<td><p>security.rememberme.key</p></td>
<td><p>testKey</p></td>
<td><p>The hash key that is used by Spring Security to hash the password values in the applications. Make sure that you change the value of this property.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.app.security.user-validity-period</p></td>
<td><p>cache.users.recheck.period</p></td>
<td><p>30000</p></td>
<td><p>How long should a user be cached before invalidating it in the cache for the cacheable CustomUserDetailsService.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.app.security.cookie.domain</p></td>
<td><p>security.cookie.domain</p></td>
<td><p>-</p></td>
<td><p>The domain for the cookie.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.idm.app.security.cookie.max-age</p></td>
<td><p>security.cookie.max-age</p></td>
<td><p>2678400</p></td>
<td><p>The max age of the security cookie in seconds. Default is 31 days.</p></td>
</tr>
<tr class="even">
<td><p>flowable.idm.app.security.cookie.refresh-age</p></td>
<td><p>security.cookie.refresh-age</p></td>
<td><p>86400</p></td>
<td><p>The refresh age of the cookie in seconds. Default is 1 day.</p></td>
</tr>
</tbody>
</table>

In addition to the default identity tables, the IDM component can also be configured to use an LDAP server.
To connect to a LDAP server, additional properties in the application.properties file (or any other way of configuring the application) are needed:

    #
    # LDAP
    #
    flowable.idm.ldap.enabled=true
    flowable.idm.ldap.server=ldap://localhost
    flowable.idm.ldap.port=10389
    flowable.idm.ldap.user=uid=admin, ou=system
    flowable.idm.ldap.password=secret
    flowable.idm.ldap.base-dn=o=flowable
    flowable.idm.ldap.query.user-by-id=(&(objectClass=inetOrgPerson)(uid={0}))
    flowable.idm.ldap.query.user-by-full-name-like=(&(objectClass=inetOrgPerson)(|({0}=*{1}*)({2}=*{3}*)))
    flowable.idm.ldap.query.all-users=(objectClass=inetOrgPerson)
    flowable.idm.ldap.query.groups-for-user=(&(objectClass=groupOfUniqueNames)(uniqueMember={0}))
    flowable.idm.ldap.query.all-groups=(objectClass=groupOfUniqueNames)
    flowable.idm.ldap.query.group-by-id=(&(objectClass=groupOfUniqueNames)(uniqueId={0}))
    flowable.idm.ldap.attribute.user-id=uid
    flowable.idm.ldap.attribute.first-name=cn
    flowable.idm.ldap.attribute.last-name=sn
    flowable.idm.ldap.attribute.group-id=cn
    flowable.idm.ldap.attribute.group-name=cn
    flowable.idm.ldap.cache.group-size=10000
    flowable.idm.ldap.cache.group-expiration=180000

When the `flowable.idm.ldap.enabled` property is set to true, the IDM app will expect the other LDAP properties to have been filled-in.
In this example configuration the server configuration + LDAP queries for the Apache Directory Server are provided.
For other LDAP servers, like Active Directory, other configuration values are needed.

When LDAP is configured, authentication and group retrieval for a user will be done through the LDAP server. Only privileges will still be retrieved from the Flowable identity tables. So make sure each LDAP user has the correct privileges defined in the IDM app.

If the IDM component is booted with LDAP configuration the bootstrap logic will check if there are already privileges present in the Flowable identity tables.
If there are no privileges (only when booting the first time), the 4 default privileges will be created and the `flowable.idm.app.admin.user-id` property value (from application.properties or configured in the environment) will be used as the user id to get all 4 privileges.
So make sure that the `flowable.idm.app.admin.user-id` property value is set to a valid LDAP user, otherwise nobody will be able to login to any of the Flowable UI apps.

## Flowable Modeler app

The Flowable Modeler app can be used to model BPMN processes, DMN decision table, Form definitions and create app definitions. When you open the Modeler app, you will see the process overview screen. From here you can start creating new BPMN process models by clicking on the Create Process or Import Process button.

![flowable modeler createmodel popup](assets/bpmn/flowable_modeler_createmodel_popup.png)

When creating a process model (but also any other model), it’s important to think carefully about the model key value. The model key is a unique identifier for the model across the full model repository. If you choose a model key that already exists in the model repository, an error message is shown and the model is not saved.

After creating the model with the popup, the BPMN modeling canvas is shown. All BPMN elements supported by the Flowable engine are available to be used in the design of a process model.

![flowable modeler design screen](assets/bpmn/flowable_modeler_design_screen.png)

The BPMN editor is divided into 4 parts:

-   Palette: the palette of BPMN elements available to design a process model

-   Toolbar: actions to change the model canvas, such as zooming, layout and saving a model

-   Model canvas: the modeling canvas on which to drag and drop BPMN elements and design the process model

-   Properties panel: the properties for the main process model if no element is select and otherwise the properties of the selected BPMN element

For a User task element there’s a *Referenced form* property in the properties panel. If you select this property, a popup is opened where you can select a form definition from the repository or create a new form. When creating a new form, a similar create dialog to the process model create dialog is presented. After filling in the name and form model key, the form editor is opened.

![flowable modeler formdesign screen](assets/bpmn/flowable_modeler_formdesign_screen.png)

Form fields can be dragged from the form palette on to the form canvas. In this example, a name textfield, two date fields and a remarks multiline textfield are added to the form canvas. When editing a form field, the label, id, required status and placeholder can be filled in.

![flowable modeler editfield popup](assets/bpmn/flowable_modeler_editfield_popup.png)

The id field is an important value, because a process variable will be created with the form field value using the id property value. When filling in the label property, the id property is automatically filled. If needed, you can also provide the id property value yourself by checking the override id checkbox.

After saving the form model and closing the form editor, you are automatically navigated back to the process model (when the form editor was opened via the BPMN editor). When selecting the User task element again and clicking on the *Referenced form* property you will see that the newly created form definition is now attached to the User task. When clicking on the *Form* tab in the header of the Modeler app, all form definitions available in the model repository are shown.

![flowable modeler formoverview screen](assets/bpmn/flowable_modeler_formoverview_screen.png)

You can preview every form definition by opening the details view of a form definition. In the details view, the form name, key and description can be edited and the history of form models is available. You can also duplicate the form definition to create a new form definition with the same form fields.

Now let’s open the vacation request process model in the BPMN editor again and add a Script task to the process model, that will calculate the number of days between the vacation start and end dates. Click on the *Script format* property and fill in a value of *groovy* to instruct the Flowable engine to use the Groovy scripting engine. Now click on the *Script* property and fill in the script that calculates the number of days.

![flowable modeler script popup](assets/bpmn/flowable_modeler_script_popup.png)

Now we have a *amountOfVacationDays* process variable we can add a Decision task to the process model. A decision task can be used to execute a DMN decision table in the Flowable DMN engine. Through the *Decision table reference* property, a new decision table model can be created and the DMN editor is opened.

![flowable modeler dmneditor screen](assets/bpmn/flowable_modeler_dmneditor_screen.png)

The DMN editor provides a table editor with input columns, where input conditions can be defined with the process variables available in the process context, and output columns, where output variable values can be defined. In this very simple example there’s one input column using the *amountOfVacationDays* variable that checks if it’s less than 10 or higher or equal to 10. When the amount of days is less than 10, an output variable *managerApprovalNeeded* is returned with value false, and otherwise a value of true is returned. You can define multiple input columns and have multiple input conditions per rule. It’s also possible to leave an input column empty, which means that it’s evaluated to true for that part of the rule. You can define one or multiple output variables.

Another important part of the DMN decision table definition is the hit policy. Currently, Flowable supports the First and Any hit policy. With the First hit policy, when the first rule is found that evaluates to true the DMN execution will stop and its output variables are returned. For the Any hit policy, all rules will be executed and the output variables for the last rule that evaluates to true are returned.

When the DMN editor is saved and closed, the Modeler app navigates back to the BPMN editor and the newly created DMN decision table is now attached to the Decision task. The decision task will be generated in the BPMN XML like;

    <serviceTask id="decisionTask" name="Is manager approval needed?" flowable:type="dmn">
        <extensionElements>
            <flowable:field name="decisionTableReferenceKey">
                <flowable:string><![CDATA[managerApprovalNeeded]]></flowable:string>
            </flowable:field>
        </extensionElements>
    </serviceTask>

With the *managerApprovalNeeded* variable available in the process instance context, we can now create an exclusive gateway with a sequence flow condition that evaluates the calculated value of the DMN Engine.

![flowable modeler sequenceflowcondition popup](assets/bpmn/flowable_modeler_sequenceflowcondition_popup.png)

The full BPMN process model now looks like this:

![flowable modeler vacationrequest screen](assets/bpmn/flowable_modeler_vacationrequest_screen.png)

With the process model completed, we can now create an app definition that combines one or more process models with all their associated models (for example, decision tables and form definitions) into a single artifact. An app definition can be exported as a BAR file (zip format) that can be deployed on the Flowable engine. When creating a vacation request app definition, the app editor will look something like the screen below.

![flowable modeler appeditor screen](assets/bpmn/flowable_modeler_appeditor_screen.png)

In the app editor, an icon and a theme color can be selected that will be used in the Flowable Task app to show the process app in the dashboard. The important step is to add the vacation request process model, and by selecting the process model, automatically include any form definitions and DMN decision tables.

![flowable modeler modelselection popup](assets/bpmn/flowable_modeler_modelselection_popup.png)

A process model can be selected by clicking on the model thumbnail. When one or more models are selected, you can close the popup, save the app definition and close it. When navigating to the details view of the newly created vacation request app definition, the following details screen is shown:

![flowable modeler appdetails screen](assets/bpmn/flowable_modeler_appdetails_screen.png)

From this view, you can download the app definition in two different formats. The first download button (with the arrow pointing downwards) can be used to
download the app definition with the JSON model files for each included model. This makes it easy to share app definitions between different Flowable UI applications. The second download button (with the arrow point to upper right) will provide a BAR file of the app definition models, which can be deployed on the Flowable engine. In the BAR file, only the deployable artifacts are included, such as the BPMN 2.0 XML file and the DMN XML file, and not the JSON model files. All files in a BAR file deployed on a Flowable engine are stored in the database, so therefore only the deployable files are included.

From the app definition details view, you can also *Publish* the app definition directly to the Flowable engine. 
The Flowable Modeler can only deploy if it is part of the single UI app (i.e. the engines are present during the runtime).
Once you click on the *Publish* button, the app definition is now deployed as a BAR file to the Flowable UI runtime engines.

This are the Modeler UI App specific properties.

<table>
<caption>Modeler UI App Properties</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Old Property</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>flowable.modeler.app.data-source-prefix</p></td>
<td><p>datasource.prefix</p></td>
<td><p>-</p></td>
<td><p>The prefix for the database tables.</p></td>
</tr>
<tr class="even">
<td><p>flowable.modeler.app.rest-enabled</p></td>
<td><p>rest.modeler-app.enabled</p></td>
<td><p>true</p></td>
<td><p>Enables the REST API (this is not the REST api used by the UI, but an api that’s available over basic auth authentication).</p></td>
</tr>
</tbody>
</table>

## Flowable Task app

The Flowable Task app is the UI to the runtime engines of the Flowable project and includes the Flowable BPMN, DMN, Form and Content engines by default. With the Flowable Task app, new process instances can be started, tasks can be completed, task forms can be rendered and so on. In the previous section, the vacation request app definition was deployed on the Flowable Task app REST API, and through that deployed on the Flowable engine. If you look in the Flowable database, you can see a new deployment entry has been added to the ACT\_RE\_DEPLOYMENT table for the BPMN Engine. Also, new entries haven been created in the ACT\_DMN\_DEPLOYMENT and ACT\_FO\_FORM\_DEPLOYMENT tables for the DMN and Form engines.

On the dashboard in the Task app, you can see a vacation request app in addition to the default Task app, and any other apps that have been deployed to the Flowable engine already.

![flowable task dashboard screen](assets/bpmn/flowable_task_dashboard_screen.png)

When clicking on the vacation request app, the task list for the logged-in user is shown (which is probably empty for now).

![flowable task tasklist screen](assets/bpmn/flowable_task_tasklist_screen.png)

When clicking on the *Processes* tab you can choose to start a new process instance by clicking on the *Start a process* button. The list of available process definitions within the context of this app definition is now displayed. In the general Task app this works in a similar way, but in the Task app, all process definitions deployed on the Flowable engine are shown. After selecting the vacation request process definition, the *Start process* button can be clicked to start a new vacation request process instance.

The Flowable Task app automatically navigates to the process instance details view. You can see the *Provide vacation information* task is active and, for example, comments can be added and the process instance state can be shown diagrammatically using the *Show diagram* button.

![flowable task processdetails screen](assets/bpmn/flowable_task_processdetails_screen.png)

When navigating to the task list, you can also see the *Provide vacation information* task listed there as well. The task details are shown in this view, with the vacation info form being rendered. You can also switch to the details view by clicking on the *Show details* button. In the details view, comments can be added, users can involved in the task and attachments can be added to the task. You can also change the due date and the assignee of a task.

![flowable task taskdetails screen](assets/bpmn/flowable_task_taskdetails_screen.png)

Let’s fill in the form and complete the task. First, select a start date and end date that have more than 10 days in between, so we can validate that a
*Manager approval* task is being generated. After filling in the vacation info form and clicking the *Complete* button, the Flowable task app navigates directly to the *Manager approval* task view. When you also complete this task (without a task form), the process instance is completed.

When navigating to the *Processes* tab and clicking on the *Showing running processes* section, you can select an option to show completed process instances. The list of completed process instances is now shown and when clicking on the just completed vacation request process you can see the two completed tasks.

![flowable task processhistory screen](assets/bpmn/flowable_task_processhistory_screen.png)

The completed form of each task is stored in the ACT\_FO\_FORM\_INSTANCE table of the Flowable Form engine. So it’s possible to look at the values of each completed form when you navigate to the completed task.

![flowable task completedform screen](assets/bpmn/flowable_task_completedform_screen.png)

Make sure to switch back to showing running processes instead of the completed ones, otherwise you won’t see newly started process instances. You can also filter tasks in the task list view. There are options to search on the name of a task, the task state, only tasks for a specific process definition and change the assignment filter.

![flowable task taskfilter screen](assets/bpmn/flowable_task_taskfilter_screen.png)

By default, the assignment filter is set to *Tasks where I am involved*. This doesn’t show the tasks where you are a candidate, such as tasks that are available to a specific candidate group before they are assigned to a specific person. To show candidate tasks you can select the *Tasks where I am one of the candidates* assignment filter option.

This are the Task UI App specific properties.

<table>
<caption>Task UI App Properties</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Old Property</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>flowable.experimental.debugger.enabled</p></td>
<td><p>debugger.enabled</p></td>
<td><p>false</p></td>
<td><p>Whether the process debugger should be enabled.</p></td>
</tr>
<tr class="even">
<td><p>flowable.task.app.rest-enabled</p></td>
<td><p>rest.task-app.enabled</p></td>
<td><p>true</p></td>
<td><p>Enables the REST API (this is not the REST api used by the UI, but an api that’s available over basic auth authentication).</p></td>
</tr>
<tr class="odd">
<td><p>flowable.form-field-validation-enabled</p></td>
<td></td>
<td><p>false</p></td>
<td><p>Enable form field validation after form submission on the engine side.</p></td>
</tr>
</tbody>
</table>

## Flowable Admin app

The fourth UI component the Flowable project provides is the Flowable Admin app. This provides ways to, for example, query deployments in the BPMN, DMN and Form Engines, but also shows the active state of a process instance with its active tasks and process variables. It also provides actions to assign a task to a different assignee and to complete an active task. The Flowable Admin app uses the REST API to communicate with the Flowable engines. By default, it is configured to connect to the Flowable UI REST API, but you can easily change this to use the Flowable REST app REST API instead. When going to the Admin app, the configuration screen is shown (which is also available by clicking on the arrow at the top right near the Flowable logo).

![flowable admin configuration screen](assets/bpmn/flowable_admin_configuration_screen.png)

For each engine, the REST endpoint can be configured with the basic authentication values. The configuration is done per engine, because it’s possible to, for example, deploy the DMN Engine on a separate server from the BPMN Engine.

When the configuration is defined with the correct values, the *Process Engine* can be selected to administer the Flowable BPMN engine. By default, the deployments of the Flowable BPMN engine are shown.

![flowable admin deployments screen](assets/bpmn/flowable_admin_deployments_screen.png)

You can filter the deployments based on name and tenant identifier. In this view, it’s also possible to deploy a new BPMN XML file or BAR file to the Flowable engine. When clicking on one of the deployments, the deployment details view is shown.

![flowable admin deploymentdetails screen](assets/bpmn/flowable_admin_deploymentdetails_screen.png)

More details of a deployment are shown here and also the process definitions that are part of this deployment on which you click to get more details. It’s also possible to delete a deployment here. When you want to delete a deployed app definition, this is also the way to delete the app definition from the Flowable Task app dashboard. When clicking on one of the process definitions, the process definition details view is shown.

![flowable admin processdefinitiondetails screen](assets/bpmn/flowable_admin_processdefinitiondetails_screen.png)

In the process definition details view, the first page of process instances is shown, together with optional decision table definitions and form definitions that are used in the process definition. For the vacation request process definition, there’s one connected decision table and one connected form definition. Clicking on the decision table definition navigates the Flowable Admin app to the DMN engine. You can always navigate back to the Process engine by clicking on the *Parent Deployment ID* link.

In addition to the deployments and definitions, you can also query on process instances, tasks, jobs and event subscriptions in the Process engine. The views all work in a similar way to what’s already been described.

This are the Admin UI App specific properties

<table>
<caption>Admin UI App Properties</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Old Property</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>flowable.admin.app.data-source-prefix</p></td>
<td><p>datasource.prefix</p></td>
<td></td>
<td><p>The prefix for the database tables.</p></td>
</tr>
<tr class="even">
<td><p>flowable.admin.app.security.encryption.credentials-i-v-spec</p></td>
<td><p>security.encryption.credentials-i-v-spec</p></td>
<td><p>-</p></td>
<td><p>The string that needs to be used to create an IvParameterSpec object using it’s the bytes.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.admin.app.security.encryption.credentials-secret-spec</p></td>
<td><p>security.encryption.credentials-secret-spec</p></td>
<td><p>-</p></td>
<td><p>The string that needs to be used to create a SecretKeySpec using it’s bytes.</p></td>
</tr>
<tr class="even">
<td><p>flowable.admin.app.security.preemptive-basic-authentication</p></td>
<td></td>
<td><p>false</p></td>
<td><p>Perform a preemptive basic authentication when issuing requests to the flowable REST API.
<strong>NB:</strong> This is an experimental property and might be removed without notice.</p></td>
</tr>
</tbody>
</table>

In addition to these properties, the Flowable admin app has a few more properties. The full
content of the properties file can be viewed on {sc-flowable-ui-admin}/flowable-ui-admin-app/src/main/resources/application.properties\[Github\].
The additional properties are mainly used for defining the initial values for the REST endpoints for the different engines.
The Admin app uses the initial values to make a connection to the Flowable engines, but the values can be overridden in the Admin app configuration view and these values are stored in the *ACT\\\_ADM\\\_SERVER\\\_CONFIG* table.
An example of the BPMN Engine REST properties is shown below:

    flowable.admin.app.server-config.process.name=Flowable Process app
    flowable.admin.app.server-config.process.description=Flowable Process REST config
    flowable.admin.app.server-config.process.server-address=http://localhost
    flowable.admin.app.server-config.process.port=8080
    flowable.admin.app.server-config.process.context-root=flowable-ui
    flowable.admin.app.server-config.process.rest-root=process-api
    flowable.admin.app.server-config.process.user-name=admin
    flowable.admin.app.server-config.process.password=test

These values can be used when multiple Flowable UI applications (with all the Flowable engines included) are managed by the Flowable Admin app.

<table>
<caption>Admin UI App Properties managed by Spring Boot</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Old property</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>message.reloading.enabled</p></td>
<td><p>Using Spring Boot MessageSourceAutoConfiguration. Set the duration with <code>spring.messages.cache-duration</code>.</p></td>
</tr>
</tbody>
</table>

## Internationalization

The Flowable UI application supports internationalization (i18n). The project maintains the English translations. It is however possible to provide your own translation files in order to support other languages.

The [Angular Translate]($$https://github.com/angular-translate/angular-translate) library tries to load a specific translation file based on the browser’s locale located in the *i18n* folder (present in each UI module). When a matching translation file cannot be loaded the framework will fallback to the English translation.

Mapping multiple browser locale keys to specific translations additional configuration can be provided (located in the Angular app config);

    // Initialize angular-translate
    $translateProvider.useStaticFilesLoader({
        prefix: './i18n/',
        suffix: '.json'
    })
    /*
        This can be used to map multiple browser language keys to a
        angular translate language key.
    */
    // .registerAvailableLanguageKeys(['en'], {
    //     'en-*': 'en'
    // })
    .useCookieStorage()
    .useSanitizeValueStrategy('sanitizeParameters')
    .uniformLanguageTag('bcp47')
    .determinePreferredLanguage();

For example; your browser is configured for English (United States) and provides the language key *en-US*. Without the mapping Angular Translate will try to fetch the corresponding translation file *en-US.json*. (If this is not available it will fallback to 'en' and load the *en.json* translation file)

By uncommenting the *.registerAvailableLanguageKeys* block you can map *en-US* (and all other *en* language keys) to the *en.json* language file.

## Production-ready endpoints

The [Production ready endpoints](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html) from Spring Boot are present for the application.
To have an overview of all the available Endpoints have a look at the [Actuator Web API Documentation](https://docs.spring.io/spring-boot/docs/current/actuator-api/html/).

This properties are set per default:

    # Expose all actuator endpoints to the web
    # They are exposed, but only authenticated users can see /info and /health abd users with access-admin can see the others
    management.endpoints.web.exposure.include=*
    # Full health details should only be displayed when a user is authorized
    management.endpoint.health.show-details=when_authorized
    # Only users with role access-admin can access full health details
    management.endpoint.health.roles=access-admin

The security is configured in such way that the `info` and `health` endpoint are exposed to all authenticated users.
Full details of the `health` endpoint can only be seen by users with the privilege `access-admin`.
In case you want to change that you need to configure `management.endpoint.health.show-details`.
All the rest of the endpoints are accessing only to users with the `access-admin` privilege.

## Custom bean deployment

There are multiple ways of providing custom beans to the Flowable application.

### Using Spring Boot auto configuration

The Flowable application is a Spring Boot 2 application.
This means that normal Spring Boot auto configuration can be used to make the beans to Flowable.
This can be done in the following manner

    package com.your.own.package.configuration;

    @Configuration
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE) // Makes sure that this configuration will be processed last by Spring Boot
    @ConditionalOnBean(type = "org.flowable.engine.ProcessEngine") // The configuration will only be used when the ProcessEngine bean is present
    public class YourOwnConfiguration {

        @Configuration
        @ComponentScan ("com.your.own.package.beans")
        public static class ComponentScanConfiguration {
            // This class is needed in order to conditionally perform the component scan (i.e. when the ProcessEngine bean is present)
            // It is an optional class, in case you don't need component scanning then you don't need to do this
        }

        @Bean
        public CustomBean customBean() {
            // create your bean
        }

        @Bean
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customProcessEngineConfigurationConfigurer() {
            return engineConfiguration -> {
                // You can use this to add extra configuration to the process engine
            }
        }
    }

Note that when using Spring Boot the configuration class can be under your own package and not under some Flowable package.

In order to make this class an auto configuration class a file named `spring.factories` should be created in the `META-INF` folder of your jar.
In this file you should add

    org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
      com.your.own.package.configuration.YourOwnCustomConfiguration

In order to use this approach you would need to include your jar in the `WEB-INF/lib` folder of the exploded war.
Placing this jar in the lib folder of the servlet container (e.g. Tomcat) is not going to work due to the way Spring proxies the `@Configuration` classes.

### Component scan

Another way to provide custom Spring beans to the Flowable engine is to put them under a certain package and have the Flowable application component scan that package.
Based on the application used, this package is different:

-   `org.flowable.rest.app` for the `flowable-rest.war`

-   `org.flowable.ui.application` for the `flowable-ui.war`

The custom beans can be located in a single JAR and this jar should be present on the classpath when the application is starting up.
Depending where there JAR is placed, the lib folder of the servlet container (e.g. Tomcat) or the `WEB-INF/lib` folder of the exploded war, there are different possibilities.

When using the lib folder of the servlet container then the created classes should be self contained, i.e. they should only use classes from within the jar.
You can use any of the Spring `@Component` annotations (with the exception of `@Configuration`).
The reason for not being able to use `@Configuration` classes is the fact that each configuration class is proxied by Spring with the help of the `ConfigurationClassPostProcessor`.
However, the classloader loading the `@Configuration` class does not have access to the needed classes by Spring.

When including the jar in the `WEB-INF/lib` folder of the exploded war then `@Configuration` classes and dependencies to other jars is possible.

### Creating your own Spring Boot application

This approach is the most flexible and most powerful approach of all.
In order to follow this approach have a look at the [Getting Started with Spring Boot](bpmn/ch05a-Spring-Boot.md#getting-started) section of this documentation.
