---
id: ch02-Configuration
title: Configuration
---

## Creating a Form Engine

*The Flowable Form engine is structured in a very similar way to the Flowable process engine. As a result parts of the documentation have a resemblance with their process engine counterpart.*

The Flowable Form engine is configured through an XML file named flowable.form.cfg.xml. Note that this is **not** applicable if you’re using [the Spring style of building a Form engine](form/ch04-Spring.md#spring-integration).

The easiest way to obtain a FormEngine, is to use the org.flowable.form.engine.FormEngines class:

    FormEngine formEngine = FormEngines.getDefaultFormEngine()

This will look for a flowable.form.cfg.xml file on the classpath and construct an engine based on the configuration in that file. The following snippet shows an example configuration. The following sections will give a detailed overview of the configuration properties.

    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

      <bean id="formEngineConfiguration" class="org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration">

        <property name="jdbcUrl" value="jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000" />
        <property name="jdbcDriver" value="org.h2.Driver" />
        <property name="jdbcUsername" value="sa" />
        <property name="jdbcPassword" value="" />

        <property name="databaseSchemaUpdate" value="true" />

      </bean>

    </beans>

Note that the configuration XML is, in fact, a Spring configuration. **This does not mean that Flowable Form can only be used in a Spring environment!** We are simply leveraging the parsing and dependency injection capabilities of Spring internally for building up the engine.

The FormEngineConfiguration object can also be created programmatically using the configuration file. It is also possible to use a different bean id (for example, see line 3).

    FormEngineConfiguration.
      createFormEngineConfigurationFromResourceDefault();
      createFormEngineConfigurationFromResource(String resource);
      createFormEngineConfigurationFromResource(String resource, String beanName);
      createFormEngineConfigurationFromInputStream(InputStream inputStream);
      createFormEngineConfigurationFromInputStream(InputStream inputStream, String beanName);

It is also possible not to use a configuration file, and create a configuration based on
defaults (see [the different supported classes](form/ch02-Configuration.md#plug-into-process-engine) for more information).

    FormEngineConfiguration.createStandaloneFormEngineConfiguration();
    FormEngineConfiguration.createStandaloneInMemFormEngineConfiguration();

All these FormEngineConfiguration.createXXX() methods return a FormEngineConfiguration that can further be tweaked if needed. After calling the buildFormEngine() operation, a FormEngine is created:

    FormEngine formEngine = FormEngineConfiguration.createStandaloneInMemFormEngineConfiguration()
      .setDatabaseSchemaUpdate(FormEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
      .setJdbcUrl("jdbc:h2:mem:my-own-db;DB_CLOSE_DELAY=1000")
      .buildFormEngine();

## FormEngineConfiguration bean

The flowable.form.cfg.xml must contain a bean that has the id 'formEngineConfiguration'.

     <bean id="formEngineConfiguration" class="org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration">

This bean is then used to construct the FormEngine. There are multiple classes available that can be used to define the formEngineConfiguration. These classes represent different environments, and set defaults accordingly. It is a best practice to select the class that matches your environment the most, to minimize the number of properties needed to configure the engine. The following classes are currently available:

-   **org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration**: the process engine is used in a standalone way. Flowable will take care of the transactions. By default, the database will only be checked when the engine boots (and an exception is thrown if there is no Flowable Form schema or the schema version is incorrect).

-   **org.flowable.form.engine.impl.cfg.StandaloneInMemFormEngineConfiguration**: this is a convenience class for unit testing purposes. Flowable Form will take care of the transactions. An H2 in-memory database is used by default. The database will be created and dropped when the engine boots and shuts down. When using this, probably no additional configuration is needed).

-   **org.flowable.form.spring.SpringFormEngineConfiguration**: To be used when the Form engine is used in a Spring environment. See [the Spring integration section](form/ch04-Spring.md#spring-integration) for more information.

## Plug into Process Engine

In addition to running in standalone mode, it’s also possible to plug the Form engine into the Process engine. This makes the Process engine aware of it and other engines. This enables, for example, deploying artifacts containing not only BPMN models, but also DMN models, to the Process engine’s deployment service API.

To make the Process engine aware of the Form engine, the **org.flowable.dmn.engine.configurator.FormEngineConfigurator** needs to be added to list of configurators in the process engine configuration. This configurator is part of the *flowable-form-engine-configurator* module.

    <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration">

        <property name="configurators">
          <list>
            <ref bean="formEngineConfigurator" />
          </list>
        </property>
        ...
    </bean>

    <bean id="formEngineConfiguration" class="org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration">
        <property name="jdbcUrl" value="jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000" />
        <property name="jdbcDriver" value="org.h2.Driver" />
        <property name="jdbcUsername" value="sa" />
        <property name="jdbcPassword" value="" />
    </bean>

    <bean id="formEngineConfigurator" class="org.flowable.form.engine.configurator.FormEngineConfigurator">
        <property name="formEngineConfiguration" ref="formEngineConfiguration" />
    </bean>

## Database configuration

There are two ways to configure the database that the Flowable Form engine will use. The first option is to define the JDBC properties of the database:

-   **jdbcUrl**: JDBC URL of the database.

-   **jdbcDriver**: implementation of the driver for the specific database type.

-   **jdbcUsername**: username to connect to the database.

-   **jdbcPassword**: password to connect to the database.

The data source that is constructed based on the provided JDBC properties will have the default [MyBatis](http://www.mybatis.org/) connection pool settings. The following attributes can optionally be set to tweak that connection pool (taken from the MyBatis documentation):

-   **jdbcMaxActiveConnections**: The maximum number of active connections that the connection pool can contain at any time. Default is 10.

-   **jdbcMaxIdleConnections**: The maximum number of idle connections that the connection pool can contain at any time.

-   **jdbcMaxCheckoutTime**: The amount of time, in milliseconds, a connection can be 'checked out' from the connection pool before it is forcefully returned. Default is 20000 (20 seconds).

-   **jdbcMaxWaitTime**: This is a low level setting that gives the pool a chance to print a log status and re-attempt the acquisition of a connection in the case that it is taking unusually long (to avoid failing silently forever if the pool is misconfigured). Default is 20000 (20 seconds).

Example database configuration:

    <property name="jdbcUrl" value="jdbc:h2:mem:flowable_dmn;DB_CLOSE_DELAY=1000" />
    <property name="jdbcDriver" value="org.h2.Driver" />
    <property name="jdbcUsername" value="sa" />
    <property name="jdbcPassword" value="" />

Our benchmarks have shown that the MyBatis connection pool is not the most efficient or resilient when dealing with a lot of concurrent requests. As such, we advise the use of a javax.sql.DataSource implementation and inject it into the process engine configuration (for example HikariCP, Tomcat JDBC Connection Pool, and so on):

    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" >
      <property name="driverClassName" value="com.mysql.jdbc.Driver" />
      <property name="url" value="jdbc:mysql://localhost:3306/flowable_dmn" />
      <property name="username" value="flowable" />
      <property name="password" value="flowable" />
      <property name="defaultAutoCommit" value="false" />
    </bean>

    <bean id="formEngineConfiguration" class="org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration">

        <property name="dataSource" ref="dataSource" />
        ...

Note that Flowable Form does not ship with a library that allows you to define such a data source. So you have to make sure that the libraries are on your classpath.

The following properties can be set, regardless of whether you are using the JDBC or data source approach:

-   **databaseType**: it’s normally not necessary to specify this property as it’s automatically identified from the database connection metadata. Should only be specified in case automatic detection fails. Possible values: {h2, mysql, oracle, postgres, mssql, db2}. This setting will determine which create/drop scripts and queries will be used. See [the 'supported databases' section](form/ch02-Configuration.md#supported-databases) for an overview of which types are supported.

-   **databaseSchemaUpdate**: allows you to set the strategy to handle the database schema on form engine boot and shutdown.

    -   false (default): Checks the version of the DB schema against the library when the form engine is being created and throws an exception if the versions don’t match.

    -   true: Upon building the form engine, a check is performed and an update of the schema is performed if it is necessary. If the schema doesn’t exist, it is created.

    -   create-drop: Creates the schema when the form engine is being created and drops the schema when the process engine is being closed.

## JNDI Datasource Configuration

By default, the database configuration for Flowable Form is contained within the db.properties files in the WEB-INF/classes of each web application. This isn’t always ideal because it
requires users to either modify the db.properties in the Flowable source and recompile the WAR file, or explode the WAR and modify the db.properties on every deployment.

By using JNDI (Java Naming and Directory Interface) to obtain the database connection, the connection is fully managed by the Servlet Container and the configuration can be managed outside the WAR deployment. This also allows more control over the connection parameters than that provided by the db.properties file.

### Configuration

Configuration of the JNDI datasource will differ depending on what servlet container application you are using. The instructions below will work for Tomcat, but for other container applications, please refer to the documentation for your container app.

If using Tomcat, the JNDI resource is configured within $CATALINA\_BASE/conf/\[enginename\]/\[hostname\]/\[warname\].xml (for the Flowable UI this will usually be $CATALINA\_BASE/conf/Catalina/localhost/flowable-app.xml). The default context is copied from the Flowable WAR file when the application is first deployed, so if it already exists, you will need to replace it. To change the JNDI resource so that the application connects to MySQL instead of H2, for example, change the file to the following:

    <?xml version="1.0" encoding="UTF-8"?>
        <Context antiJARLocking="true" path="/flowable-app">
            <Resource auth="Container"
                name="jdbc/flowableDB"
                type="javax.sql.DataSource"
                description="JDBC DataSource"
                url="jdbc:mysql://localhost:3306/flowable"
                driverClassName="com.mysql.jdbc.Driver"
                username="sa"
                password=""
                defaultAutoCommit="false"
                initialSize="5"
                maxWait="5000"
                maxActive="120"
                maxIdle="5"/>
            </Context>

### JNDI properties

To configure a JNDI Datasource, use following properties in the properties file for the Flowable UI:

-   spring.datasource.jndi-name=: the JNDI name of the Datasource.

-   datasource.jndi.resourceRef: Set whether the lookup occurs in a J2EE container, in other words, if the prefix "java:comp/env/" needs to be added if the JNDI name doesn’t already contain it. Default is "true".

### Custom properties

System properties can also be used in the flowable.form.cfg.xml by using them in the format `${propertyName:defaultValue}`.

    <property name="jdbcUrl" value="${jdbc.url:jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000}" />
    <property name="jdbcDriver" value="${jdbc.driver:org.h2.Driver}" />
    <property name="jdbcUsername" value="${jdbc.username:sa}" />
    <property name="jdbcPassword" value="${jdbc.password:}" />

Using this configuration if the property `jdbc.url` is available then it would be used for the `jdbcUrl` of the `FormEngineConfiguration`.
Otherwise the value after the first `:` would be used.

It is also possible to define locations from where properties can be picked up from the system by using a bean of type org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.

Example configuration with custom location for properties

    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

      <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
        <property name="location" value="file:/opt/conf/flowable.properties" />
      </bean>

      <bean id="formEngineConfiguration" class="org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration">

        <property name="jdbcUrl" value="${jdbc.url:jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000}" />
        <property name="jdbcDriver" value="${jdbc.driver:org.h2.Driver}" />
        <property name="jdbcUsername" value="${jdbc.username:sa}" />
        <property name="jdbcPassword" value="${jdbc.password:}" />

        <property name="databaseSchemaUpdate" value="true" />

      </bean>

    </beans>

With this configuration the properties would be first looked up in the /opt/conf/flowable.properties file.

## Supported databases

Listed below are the types (case sensitive!) that Flowable uses to refer to databases.

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Flowable Form database type</th>
<th>Example JDBC URL</th>
<th>Notes</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>h2</p></td>
<td><p>jdbc:h2:tcp://localhost/flowable_form</p></td>
<td><p>Default configured database</p></td>
</tr>
<tr class="even">
<td><p>mysql</p></td>
<td><p>jdbc:mysql://localhost:3306/flowable_form?autoReconnect=true</p></td>
<td><p>Tested using mysql-connector-java database driver</p></td>
</tr>
<tr class="odd">
<td><p>oracle</p></td>
<td><p>jdbc:oracle:thin:@localhost:1521:xe</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>postgres</p></td>
<td><p>jdbc:postgresql://localhost:5432/flowable_form</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>db2</p></td>
<td><p>jdbc:db2://localhost:50000/flowable_form</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>mssql</p></td>
<td><p>jdbc:sqlserver://localhost:1433;databaseName=flowable_form (jdbc.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver) <em>OR</em> jdbc:jtds:sqlserver://localhost:1433/flowable_form (jdbc.driver=net.sourceforge.jtds.jdbc.Driver)</p></td>
<td><p>Tested using Microsoft JDBC Driver 4.0 (sqljdbc4.jar) and JTDS Driver</p></td>
</tr>
</tbody>
</table>

## Creating the database tables

Flowable Form uses [Liquibase](http://www.liquibase.org) for tracking, managing and applying database schema changes.

The easiest way to create the database tables for your database is to:

-   Add the flowable-form-engine JARs to your classpath

-   Add a suitable database driver

-   Add a Flowable configuration file (*flowable.form.cfg.xml*) to your classpath, pointing to your database (see [database configuration section](form/ch02-Configuration.md#database-configuration))

-   Execute the main method of the *DbSchemaCreate* class

## Database table names explained

The database names of Flowable Form all start with **ACT\_FO\_**.

-   ACT\_FO\_DATABASECHANGELOG: This table is used by Liquibase to track which changesets have been run.

-   ACT\_FO\_DATABASECHANGELOGLOCK: This table is used by Liquibase to ensure only one instance of Liquibase is running at one time.

-   ACT\_FO\_FORM\_DEFINITION: This table contains the definition info of the deployed form definitions.

-   ACT\_FO\_FORM\_INSTANCE: This table contains the form instances with the values that have been filled in by a user.

-   ACT\_FO\_FORM\_DEPLOYMENT: This table contains the deployment metadata.

-   ACT\_FO\_FORM\_RESOURCE: This table contains the Form definition resource.

## Database upgrade

Make sure you make a backup of your database (using your database backup capabilities) before you run an upgrade.

By default, a version check will be performed each time a process engine is created. This typically happens once at boot time of your application or the Flowable webapps. If the Flowable library notices a difference between the library version and the version of the Flowable database tables, then an exception is thrown.

To upgrade, you have to start with putting the following configuration property in your flowable.form.cfg.xml configuration file:

    <beans >

      <bean id="formEngineConfiguration" class="org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration">
        <!-- ... -->
        <property name="databaseSchemaUpdate" value="true" />
        <!-- ... -->
      </bean>

    </beans>

**Also, include a suitable database driver for your database to the classpath.** Upgrade the Flowable Form libraries in your application. Or start up a new version of Flowable Form and point it to a database that contains an older version. With databaseSchemaUpdate set to true, Flowable Form will automatically upgrade the DB schema to the newer version the first time when it notices that libraries and DB schema are out of sync.

## Deployment cache configuration

All definitions are cached (after they’re parsed) to avoid hitting the database every time a form is needed and because form data doesn’t change. By default, there is no limit on this cache. To limit the forms cache, add following property

    <property name="formCacheLimit" value="10" />

Setting this property will swap the default hashmap cache with a LRU cache that has the provided hard limit. Of course, the 'best' value of this property depends on the total amount of forms stored and the number of forms actually used at runtime.

You can also inject your own cache implementation. This must be a bean that implements the org.flowable.form.engine.impl.persistence.deploy.DeploymentCache interface:

    <property name="formCache">
      <bean class="org.flowable.MyCache" />
    </property>

## Logging

All logging (flowable, spring, mybatis, …​) is routed through SLF4J and allows the selection of the logging-implementation of your choice.

**By default no SFL4J-binding jar is present in the flowable-dmn-engine dependencies, this should be added in your project in order to use the logging framework of your choice.** If no implementation jar is added, SLF4J will use a NOP-logger, not logging anything at all, other than a warning that nothing will be logged. For more info on these bindings [<http://www.slf4j.org/codes.html#StaticLoggerBinder>](http://www.slf4j.org/codes.html#StaticLoggerBinder).

With Maven, add for example a dependency like this (here using log4j), note that you still need to add a version:

    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-log4j12</artifactId>
    </dependency>

The flowable-ui and flowable-rest webapps are configured to use Log4j binding. Log4j is also used when running the tests for all the flowable-\* modules.

**Important note when using a container with commons-logging in the classpath:** In order to route the spring-logging through SLF4J, a bridge is used (see [<http://www.slf4j.org/legacy.html#jclOverSLF4J>](http://www.slf4j.org/legacy.html#jclOverSLF4J)). If your container provides a commons-logging implementation, please follow directions on this page: [<http://www.slf4j.org/codes.html#release>](http://www.slf4j.org/codes.html#release) to ensure stability.

Example when using Maven (version omitted):

    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>jcl-over-slf4j</artifactId>
    </dependency>
