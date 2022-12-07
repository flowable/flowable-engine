---
id: ch14-Applications
title: Flowable applications
---

Flowable provides the `flowable-rest.war` which contains the Flowable REST API. More about this can be read in the [REST API](bpmn/ch15-REST.md) chapter.

The application is a Spring Boot 3.0 based application, which means that the WAR file is actually executable and can be run as a normal standalone application.
See [The Executable Jar Format](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html#build-tool-plugins-maven-packaging) in the Spring Boot reference documentation.


## Flowable REST application installation

As mentioned before, the application can be deployed on a Tomcat server, and to get started this is probably the easiest approach when additional configuration settings are used. For this installation guide we’ll describe the installation of the application in a Tomcat server.

1.  Download a recent stable version of [Apache Tomcat](http://tomcat.apache.org). It has to be Jakarta Servlet 6 compliant

2.  Download the latest stable [Flowable 6 version](http://www.flowable.org/downloads.html).

3.  Copy the flowable-rest.war file from the Flowable distribution *wars* folder to the Tomcat webapps folder.

4.  Startup the Tomcat server by running the bin/startup.sh (Mac OS and Linux) or bin/startup.bat (Windows) script.

5.  Open a web browser and go to [<http://localhost:8080/flowable-rest/docs>](http://localhost:8080/flowable-rest/docs).

The Flowable REST application should now be running with an H2 in-memory database and the Swagger Docs should be shown in your web browser:

The Flowable REST application should now be running with an H2 in-memory database

Usually, you will want to change the default H2 in-memory database configuration to a MySQL or Postgres (or other persistent database) configuration.
You can do this by changing the application.properties file in the *WEB-INF/classes/* directory of the application.
However, it is easier to use the Spring Boot [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).
An example configuration can be found on [Github](https://github.com/flowable/flowable-engine/blob/main/modules/flowable-app-rest/src/main/resources/flowable-default.properties)
To change the default configuration to MySQL the following changes are needed to the properties file:

    spring.datasource.url=jdbc:mysql://127.0.0.1:3306/flowable?characterEncoding=UTF-8
    spring.datasource.username=flowable
    spring.datasource.password=flowable

This configuration will expect a flowable database to be present in the MySQL server and the REST app will automatically generate the necessary database tables. For Postgres the following changes are necessary:

    spring.datasource.url=jdbc:postgresql://localhost:5432/flowable
    spring.datasource.username=flowable
    spring.datasource.password=flowable

In addition to changing the configuration, make sure the database driver is available on the classpath. Again, you could do this for the web application by adding the driver JAR file to the WEB-INF/lib folder, but you can also copy the JAR file to the Tomcat lib folder. For MySQL and Postgres the database drivers can be downloaded from:

-   MySQL: [<https://dev.mysql.com/downloads/connector/j>](https://dev.mysql.com/downloads/connector/j)

-   Postgres: [<https://jdbc.postgresql.org/>](https://jdbc.postgresql.org/)

When running the REST as a standalone application the database driver can be added by using the `loader.path` property.

    java -Dloader.path=/location/to/your/driverfolder -jar flowable-rest.war

See the [`PropertiesLauncher` Features](https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html#executable-jar-property-launcher-features) in the Spring Boot reference documentation for more information.

## Flowable REST Application Configuration

As the Flowable REST app is a Spring Boot application, you can use all the properties Spring Boot provides.
In order to provide custom configuration for the application have a look at the [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) section of the Spring Boot documentation.

    You can also use YAML based properties.

<table>
<caption>REST App Properties</caption>
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

## Flowable LDAP configuration


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

When the `flowable.idm.ldap.enabled` property is set to true, the REST app will expect the other LDAP properties to have been filled-in.
In this example configuration the server configuration + LDAP queries for the Apache Directory Server are provided.
For other LDAP servers, like Active Directory, other configuration values are needed.

When LDAP is configured, authentication and group retrieval for a user will be done through the LDAP server. Only privileges will still be retrieved from the Flowable identity tables. So make sure each LDAP user has the correct privileges defined in the IDM app.

If the application is booted with LDAP configuration the bootstrap logic will check if there are already privileges present in the Flowable identity tables.
If there are no privileges (only when booting the first time), the 4 default privileges will be created and the `flowable.rest.app.admin.user-id` property value (from application.properties or configured in the environment) will be used as the user id to get all privileges.
So make sure that the `flowable.rest.app.admin.user-id` property value is set to a valid LDAP user, otherwise nobody will be able to use the Flowable REST app.

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

In order to make this class an auto configuration class a file named `org.springframework.boot.autoconfigure.AutoConfiguration.imports` should be created in the `META-INF/spring` folder of your jar.
In this file you should add

    com.your.own.package.configuration.YourOwnCustomConfiguration

In order to use this approach you would need to include your jar in the `WEB-INF/lib` folder of the exploded war.
Placing this jar in the lib folder of the servlet container (e.g. Tomcat) is not going to work due to the way Spring proxies the `@Configuration` classes.

### Component scan

Another way to provide custom Spring beans to the Flowable engine is to put them under a certain package and have the Flowable application component scan that package.
Based on the application used, this package is different:

-   `org.flowable.rest.app` for the `flowable-rest.war`

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
