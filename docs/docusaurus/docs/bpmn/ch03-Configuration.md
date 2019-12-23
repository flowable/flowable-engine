---
id: ch03-Configuration
title: Configuration
---

## Creating a ProcessEngine

The Flowable process engine is configured through an XML file called flowable.cfg.xml. Note that this is **not** applicable if you’re using [the Spring style of building a process engine](bpmn/ch05-Spring.md#spring-integration).

The easiest way to obtain a ProcessEngine is to use the org.flowable.engine.ProcessEngines class:

    ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine()

This will look for a flowable.cfg.xml file on the classpath and construct an engine based on the configuration in that file. The following snippet shows an example configuration. The following sections will give a detailed overview of the configuration properties.

    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

      <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration">

        <property name="jdbcUrl" value="jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000" />
        <property name="jdbcDriver" value="org.h2.Driver" />
        <property name="jdbcUsername" value="sa" />
        <property name="jdbcPassword" value="" />

        <property name="databaseSchemaUpdate" value="true" />

        <property name="asyncExecutorActivate" value="false" />

        <property name="mailServerHost" value="mail.my-corp.com" />
        <property name="mailServerPort" value="5025" />
      </bean>

    </beans>

Note that the configuration XML is in fact a Spring configuration. **This does not mean that Flowable can only be used in a Spring environment!** We are simply leveraging the parsing and dependency injection capabilities of Spring internally for building up the engine.

The ProcessEngineConfiguration object can also be created programmatically using the configuration file. It is also possible to use a different bean id (for example, see line 3).

    ProcessEngineConfiguration.
      createProcessEngineConfigurationFromResourceDefault();
      createProcessEngineConfigurationFromResource(String resource);
      createProcessEngineConfigurationFromResource(String resource, String beanName);
      createProcessEngineConfigurationFromInputStream(InputStream inputStream);
      createProcessEngineConfigurationFromInputStream(InputStream inputStream, String beanName);

It is also possible not to use a configuration file, and create a configuration based on
defaults (see [the different supported classes](#configurationClasses) for more information).

    ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
    ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();

All these ProcessEngineConfiguration.createXXX() methods return a ProcessEngineConfiguration that can be tweaked further if needed. After calling the buildProcessEngine() operation, a ProcessEngine is created:

    ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
      .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
      .setJdbcUrl("jdbc:h2:mem:my-own-db;DB_CLOSE_DELAY=1000")
      .setAsyncExecutorActivate(false)
      .buildProcessEngine();
      
## ProcessEngineConfiguration bean

The flowable.cfg.xml must contain a bean that has the id 'processEngineConfiguration'.

     <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration">

<a name="configurationClasses"/>
This bean is then used to construct the ProcessEngine. There are multiple classes available that can be used to define the processEngineConfiguration. These classes represent different environments, and set defaults accordingly. It’s best practice to select the class that best matches your environment, to minimize the number of properties needed to configure the engine. The following classes are currently available:

-   **org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration**: the process engine is used in a standalone way. Flowable will take care of all transactions. By default, the database will only be checked when the engine boots (and an exception is thrown if there is no Flowable schema or the schema version is incorrect).

-   **org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration**: this is a convenience class for unit testing purposes. Flowable will take care of all transactions. An H2 in-memory database is used by default. The database will be created and dropped when the engine boots and shuts down. When using this, no additional configuration is probably needed (except when using, for example, the job executor or mail capabilities).

-   **org.flowable.spring.SpringProcessEngineConfiguration**: To be used when the process engine is used in a Spring environment. See [the Spring integration section](bpmn/ch05-Spring.md#spring-integration) for more information.

-   **org.flowable.engine.impl.cfg.JtaProcessEngineConfiguration**: To be used when the engine runs in standalone mode, with JTA transactions.

## Database configuration

There are two ways to configure the database that the Flowable engine will use. The first option is to define the JDBC properties of the database:

-   **jdbcUrl**: JDBC URL of the database.

-   **jdbcDriver**: implementation of the driver for the specific database type.

-   **jdbcUsername**: username to connect to the database.

-   **jdbcPassword**: password to connect to the database.

The data source that is constructed based on the provided JDBC properties will have the default [MyBatis](http://www.mybatis.org/) connection pool settings. The following attributes can optionally be set to tweak that connection pool (taken from the MyBatis documentation):

-   **jdbcMaxActiveConnections**: The number of active connections that the connection pool at maximum at any time can contain. Default is 10.

-   **jdbcMaxIdleConnections**: The number of idle connections that the connection pool at maximum at any time can contain.

-   **jdbcMaxCheckoutTime**: The amount of time in milliseconds a connection can be 'checked out' from the connection pool before it is forcefully returned. Default is 20000 (20 seconds).

-   **jdbcMaxWaitTime**: This is a low level setting that gives the pool a chance to print a log status and re-attempt the acquisition of a connection in the case that it is taking unusually long (to avoid failing silently forever if the pool is misconfigured) Default is 20000 (20 seconds).

Example database configuration:

    <property name="jdbcUrl" value="jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000" />
    <property name="jdbcDriver" value="org.h2.Driver" />
    <property name="jdbcUsername" value="sa" />
    <property name="jdbcPassword" value="" />

Our benchmarks have shown that the MyBatis connection pool is not the most efficient or resilient when dealing with a lot of concurrent requests. As such, it is advised to use a javax.sql.DataSource implementation and inject it into the process engine configuration (For example HikariCP, Tomcat JDBC Connection Pool, etc.):

    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" >
      <property name="driverClassName" value="com.mysql.jdbc.Driver" />
      <property name="url" value="jdbc:mysql://localhost:3306/flowable" />
      <property name="username" value="flowable" />
      <property name="password" value="flowable" />
      <property name="defaultAutoCommit" value="false" />
    </bean>

    <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration">

      <property name="dataSource" ref="dataSource" />
      ...

Note that Flowable does not ship with a library that allows you to define such a data source. So you need to make sure that the libraries are on your classpath.

The following properties can be set, regardless of whether you are using the JDBC or data source approach:

-   **databaseType**: it’s normally not necessary to specify this property, as it is automatically detected from the database connection metadata. Should only be specified when automatic detection fails. Possible values: {h2, mysql, oracle, postgres, mssql, db2}. This setting will determine which create/drop scripts and queries will be used. See [the 'supported databases' section](bpmn/ch03-Configuration.md#supported-databases) for an overview of which types are supported.

-   **databaseSchemaUpdate**: sets the strategy to handle the database schema on process engine boot and shutdown.

    -   false (default): Checks the version of the DB schema against the library when the process engine is being created and throws an exception if the versions don’t match.

    -   true: Upon building the process engine, a check is performed and an update of the schema is performed if it is necessary. If the schema doesn’t exist, it is created.

    -   create-drop: Creates the schema when the process engine is being created and drops the schema when the process engine is being closed.

## JNDI Datasource Configuration

By default, the database configuration for Flowable is contained within the db.properties files in the WEB-INF/classes of each web application. This isn’t always ideal because it
requires users to either modify the db.properties in the Flowable source and recompile the WAR file, or explode the WAR and modify the db.properties on every deployment.

By using JNDI (Java Naming and Directory Interface) to obtain the database connection, the connection is fully managed by the Servlet Container and the configuration can be managed outside the WAR deployment. This also allows more control over the connection parameters than what is provided by the db.properties file.

### Configuration

Configuration of the JNDI data source will differ depending on what servlet container application you are using. The instructions below will work for Tomcat, but for other container applications, please refer to the documentation for your container app.

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

To configure a JNDI data source, use following properties in the properties file for the Flowable UI:

-   spring.datasource.jndi-name=: the JNDI name of the data source.

-   datasource.jndi.resourceRef: Set whether the lookup occurs in a J2EE container, for example, the prefix "java:comp/env/" needs to be added if the JNDI name doesn’t already contain it. Default is "true".

### Custom properties

System properties can also be used in the flowable.cfg.xml by using them in the format `${propertyName:defaultValue}`.

    <property name="jdbcUrl" value="${jdbc.url:jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000}" />
    <property name="jdbcDriver" value="${jdbc.driver:org.h2.Driver}" />
    <property name="jdbcUsername" value="${jdbc.username:sa}" />
    <property name="jdbcPassword" value="${jdbc.password:}" />

Using this configuration if the property `jdbc.url` is available then it would be used for the `jdbcUrl` of the `ProcessEngineConfiguration`.
Otherwise the value after the first `:` would be used.

It is also possible to define locations from where properties can be picked up from the system by using a bean of type org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.

Example configuration with custom location for properties

    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

      <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
        <property name="location" value="file:/opt/conf/flowable.properties" />
      </bean>

      <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration">

        <property name="jdbcUrl" value="${jdbc.url:jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000}" />
        <property name="jdbcDriver" value="${jdbc.driver:org.h2.Driver}" />
        <property name="jdbcUsername" value="${jdbc.username:sa}" />
        <property name="jdbcPassword" value="${jdbc.password:}" />

        <property name="databaseSchemaUpdate" value="true" />

        <property name="asyncExecutorActivate" value="false" />

        <property name="mailServerHost" value="mail.my-corp.com" />
        <property name="mailServerPort" value="5025" />
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
<th>Flowable database type</th>
<th>Example JDBC URL</th>
<th>Notes</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>h2</p></td>
<td><p>jdbc:h2:tcp://localhost/flowable</p></td>
<td><p>Default configured database</p></td>
</tr>
<tr class="even">
<td><p>mysql</p></td>
<td><p>jdbc:mysql://localhost:3306/flowable?autoReconnect=true</p></td>
<td><p>Tested using mysql-connector-java database driver</p></td>
</tr>
<tr class="odd">
<td><p>oracle</p></td>
<td><p>jdbc:oracle:thin:@localhost:1521:xe</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>postgres</p></td>
<td><p>jdbc:postgresql://localhost:5432/flowable</p></td>
<td></td>
</tr>
<tr class="odd">
<td><p>db2</p></td>
<td><p>jdbc:db2://localhost:50000/flowable</p></td>
<td></td>
</tr>
<tr class="even">
<td><p>mssql</p></td>
<td><p>jdbc:sqlserver://localhost:1433;databaseName=flowable (jdbc.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver) <em>OR</em> jdbc:jtds:sqlserver://localhost:1433/flowable (jdbc.driver=net.sourceforge.jtds.jdbc.Driver)</p></td>
<td><p>Tested using Microsoft JDBC Driver 4.0 (sqljdbc4.jar) and JTDS Driver</p></td>
</tr>
</tbody>
</table>

## Creating the database tables

The easiest way to create the database tables for your database is to:

-   Add the flowable-engine JARs to your classpath

-   Add a suitable database driver

-   Add a Flowable configuration file (*flowable.cfg.xml*) to your classpath, pointing to your database (see [database configuration section](bpmn/ch03-Configuration.md#database-configuration))

-   Execute the main method of the *DbSchemaCreate* class

However, often only database administrators can execute DDL statements on a database. On a production system, this is also the wisest of choices. The SQL DDL statements can be found on the Flowable downloads page or inside the Flowable distribution folder, in the database subdirectory. The scripts are also in the engine JAR (*flowable-engine-x.jar*), in the package *org/flowable/db/create* (the *drop* folder contains the drop statements). The SQL files are of the form

    flowable.{db}.{create|drop}.{type}.sql

Where *db* is any of the [supported databases](bpmn/ch03-Configuration.md#supported-databases) and *type* is:

-   **engine:** the tables needed for engine execution. Required.

-   **history:** the tables that contain the history and audit information. Optional: not needed when history level is set to *none*. Note that this will also disable some features (such as commenting on tasks) which store the data in the history database.

**Note for MySQL users:** MySQL versions lower than 5.6.4 have no support for timestamps or dates with millisecond precision. To make things even worse, some versions will throw an exception when trying to create such a column, but other versions don’t. When doing auto-creation/upgrade, the engine will change the DDL when executing it. When using the DDL file approach, both a regular version and a special file with *mysql55* in it are available (this applies on anything lower than 5.6.4). This latter file will have column types with no millisecond precision.

Concretely, the following applies for MySQL versions:

-   **&lt;5.6:** No millisecond precision available. DDL files available (look for files containing *mysql55*). Auto creation/update will work out of the box.

-   **5.6.0 - 5.6.3:** No millisecond precision available. Auto creation/update will NOT work. It is advised to upgrade to a newer database version anyway. DDL files for *mysql 5.5* could be used if really needed.

-   **5.6.4+:** Millisecond precision available. DDL files available (default file containing *mysql*). Auto creation/update works out of the box.

Do note that in the case of upgrading the MySQL database later on and the Flowable tables are already created/upgraded, the column type change will have to be done manually!

## Database table names explained

The database names of Flowable all start with **ACT\_**. The second part is a two-character identification of the use case of the table. This use case will also roughly match the service API.

-   **ACT\_RE\_**\*: 'RE' stands for repository. Tables with this prefix contain 'static' information such as process definitions and process resources (images, rules, etc.).

-   **ACT\_RU\_**\*: 'RU' stands for runtime. These are the runtime tables that contain the runtime data of process instances, user tasks, variables, jobs, and so on. Flowable only stores the runtime data during process instance execution and removes the records when a process instance ends. This keeps the runtime tables small and fast.

-   **ACT\_HI\_**\*: 'HI' stands for history. These are the tables that contain historic data, such as past process instances, variables, tasks, and so on.

-   **ACT\_GE\_**\*: general data, which is used for various use cases.

## Database upgrade

Make sure you make a backup of your database (using your database backup capabilities) before you run an upgrade.

By default, a version check will be performed each time a process engine is created. This typically happens once at boot time of your application or of the Flowable webapps. If the Flowable library notices a difference between the library version and the version of the Flowable database tables, then an exception is thrown.

To upgrade, you have to start by putting the following configuration property in your flowable.cfg.xml configuration file:

    <beans >

      <bean id="processEngineConfiguration"
          class="org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration">
        <!-- ... -->
        <property name="databaseSchemaUpdate" value="true" />
        <!-- ... -->
      </bean>

    </beans>

**Also, include a suitable database driver for your database to the classpath.** Upgrade the Flowable libraries in your application. Or start up a new version of Flowable and point it to a database that contains data from an older version. With databaseSchemaUpdate set to true, Flowable will automatically upgrade the DB schema to the newest version the first time when it notices that libraries and DB schema are out of sync.

**As an alternative, you can also run the upgrade DDL statements.** It’s also possible to run the upgrade database scripts available on the Flowable downloads page.

## Job Executor (from version 6.0.0 onwards)

The async executor of Flowable v5 is the only available job executor in Flowable V6, as it is a more performant and more database friendly way of executing asynchronous jobs in the Flowable engine.
The old job executor of Flowable 5 is no longer available in V6. More information can be found in the advanced section of the user guide.

Additionally, if running under Java EE 7, JSR-236 compliant ManagedAsyncJobExecutor can be used for letting the container manage the threads. In order to enable them, the thread factory should be passed in the configuration as follows:

    <bean id="threadFactory" class="org.springframework.jndi.JndiObjectFactoryBean">
       <property name="jndiName" value="java:jboss/ee/concurrency/factory/default" />
    </bean>

    <bean id="customJobExecutor" class="org.flowable.engine.impl.jobexecutor.ManagedAsyncJobExecutor">
       <!-- ... -->
       <property name="threadFactory" ref="threadFactory" />
       <!-- ... -->
    </bean>

The managed implementations fall back to their default counterparts if the thread factory is not specified.

## Job executor activation

The AsyncExecutor is a component that manages a thread pool to fire timers and other asynchronous tasks. Other implementations are possible (for example using a message queue, see the advanced section of the user guide).

By default, the AsyncExecutor is not activated and not started. With the following configuration the async executor can be started together with the Flowable Engine.

    <property name="asyncExecutorActivate" value="true" />

The property asyncExecutorActivate instructs the Flowable engine to start the Async executor at startup.

## Mail server configuration

Configuring a mail server is optional. Flowable supports sending e-mails in business processes. To actually send an e-mail, a valid SMTP mail server configuration is required. See the [e-mail task](bpmn/ch07b-BPMN-Constructs.md#mail-server-configuration) for the configuration options.

## History configuration

Customizing the configuration of history storage is optional. This allows you to tweak settings that influence the [history capabilities](bpmn/ch11-History.md#history) of the engine. See [history configuration](bpmn/ch11-History.md#history-configuration) for more details.

    <property name="history" value="audit" />

## Async history configuration

\[Experimental\] Since Flowable 6.1.0 the async history feature has been added. When async history is enabled, the historic data will be persisted by a history job executor, instead of synchronous persistence as part of the runtime execution persistence.
See [async history configuration](bpmn/ch11-History.md#async-history-configuration) for more details.

    <property name="asyncHistoryEnabled" value="true" />

## Exposing configuration beans in expressions and scripts

By default, all beans that you specify in the flowable.cfg.xml configuration or in your own Spring configuration file are available to expressions and scripts. If you want to limit the visibility of beans in your configuration file, you can configure a property called beans in your process engine configuration. The beans property in ProcessEngineConfiguration is a map. When you specify that property, only beans specified in that map will be visible to expressions and scripts. The exposed beans will be exposed with the names as you specify in the map.

## Deployment cache configuration

All process definitions are cached (after they’re parsed) to avoid hitting the database every time a process definition is needed and because process definition data doesn’t change. By default, there is no limit on this cache. To limit the process definition cache, add following property:

    <property name="processDefinitionCacheLimit" value="10" />

Setting this property will swap the default hashmap cache with a LRU cache that has the provided hard limit. Of course, the 'best' value for this property depends on the total amount of process definitions stored and the number of process definitions actually used at runtime by all the runtime process instances.

You can also inject your own cache implementation. This must be a bean that implements the org.flowable.engine.impl.persistence.deploy.DeploymentCache interface:

    <property name="processDefinitionCache">
      <bean class="org.flowable.MyCache" />
    </property>

There is a similar property called knowledgeBaseCacheLimit and knowledgeBaseCache for configuring the rules cache. This is only needed when you use the rules task in your processes.

## Logging

All logging (flowable, spring, mybatis, …​) is routed through SLF4J and allows for selecting the logging-implementation of your choice.

**By default no SFL4J-binding JAR is present in the flowable-engine dependencies, this should be added in your project in order to use the logging framework of your choice.** If no implementation JAR is added, SLF4J will use a NOP-logger, not logging anything at all, other than a warning that nothing will be logged. For more info on these bindings [<http://www.slf4j.org/codes.html#StaticLoggerBinder>](http://www.slf4j.org/codes.html#StaticLoggerBinder).

With Maven, add for example a dependency like this (here using log4j), note that you still need to add a version:

    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-log4j12</artifactId>
    </dependency>

The flowable-ui and flowable-rest webapps are configured to use Log4j-binding. Log4j is also used when running the tests for all the flowable-\* modules.

**Important note when using a container with commons-logging in the classpath:** In order to route the spring-logging through SLF4J, a bridge is used (see [<http://www.slf4j.org/legacy.html#jclOverSLF4J>](http://www.slf4j.org/legacy.html#jclOverSLF4J)). If your container provides a commons-logging implementation, please follow directions on this page: [<http://www.slf4j.org/codes.html#release>](http://www.slf4j.org/codes.html#release) to ensure stability.

Example when using Maven (version omitted):

    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>jcl-over-slf4j</artifactId>
    </dependency>

## Mapped Diagnostic Contexts

Flowable supports the Mapped Diagnostic Contexts feature of SLF4j. This basic information is passed to the underlying logger along with what is going to be logged:

-   processDefinition Id as mdcProcessDefinitionID

-   processInstance Id as mdcProcessInstanceID

-   execution Id as mdcExecutionId

None of this information is logged by default. The logger can be configured to show them in your desired format, extra to the usual logged messages. For example in
Log4j the following sample layout definition causes the logger to show the above mentioned information:

    log4j.appender.consoleAppender.layout.ConversionPattern=ProcessDefinitionId=%X{mdcProcessDefinitionID}
    executionId=%X{mdcExecutionId} mdcProcessInstanceID=%X{mdcProcessInstanceID} mdcBusinessKey=%X{mdcBusinessKey} %m%n

This is useful when the logs contain information that needs to checked in real time, by means of a log analyzer, for example.

## Event handlers

The event mechanism in the Flowable engine allows you to get notified when various events occur within the engine. Take a look at [all supported event types](bpmn/ch03-Configuration.md#supported-event-types) for an overview of the events available.

It’s possible to register a listener for certain types of events as opposed to getting notified when any type of event is dispatched. You can either add engine-wide event listeners [through the configuration](bpmn/ch03-Configuration.md#configuration-and-setup), add engine-wide event listeners [at runtime using the API](bpmn/ch03-Configuration.md#adding-listeners-at-runtime) or add event-listeners to [specific process definitions in the BPMN XML](bpmn/ch03-Configuration.md#adding-listeners-to-process-definitions).

All events dispatched are a subtype of org.flowable.engine.common.api.delegate.event.FlowableEvent. The event exposes (if available) the type, executionId, processInstanceId and processDefinitionId. Certain events contain additional context related to the event that occurred, more information about additional payloads can be found in the list of [all supported event types](bpmn/ch03-Configuration.md#supported-event-types).

### Event listener implementation

The only requirement for an event-listener is to implement org.flowable.engine.delegate.event.FlowableEventListener. Below is an example implementation of a listener, which outputs all events received to the standard-out, with exception of events related to job-execution:

    public class MyEventListener implements FlowableEventListener {

      @Override
      public void onEvent(FlowableEvent event) {

        if(event.getType() == FlowableEngineEventType.JOB_EXECUTION_SUCCESS) {
          System.out.println("A job well done!");
        } else if (event.getType() == FlowableEngineEventType.JOB_EXECUTION_FAILURE) {
          System.out.println("A job has failed...");
        } else {
          System.out.println("Event received: " + event.getType());
        }
      }

      @Override
      public boolean isFailOnException() {
        // The logic in the onEvent method of this listener is not critical, exceptions
        // can be ignored if logging fails...
        return false;
      }

      @Override
      public boolean isFireOnTransactionLifecycleEvent() {
        return false;
      }

      @Override
      public String getOnTransaction() {
        return null;
      }
    }

The isFailOnException() method determines the behavior when the onEvent(..) method throws an exception when an event is dispatched. When false is returned, the exception is ignored. When true is returned, the exception is not ignored and bubbles up, effectively failing the current ongoing command. If the event was part of an API-call (or any other transactional operation, for example, job-execution), the transaction will be rolled back. If the behavior in the event-listener is not business-critical, it’s recommended to return false.

The isFireOnTransactionLifecycleEvent() method determines whether this event listener fires immediately when the event occurs or on a
transaction lifecycle event determined by getOnTransaction() method.
Supported values of the transaction life cycle event are: COMMITTED, ROLLED\_BACK, COMMITTING, ROLLINGBACK.

There are a few base implementations provided by Flowable to facilitate common use cases of event-listeners. These can be used as base-class or as an example listener implementation:

-   **org.flowable.engine.delegate.event.BaseEntityEventListener**: An event-listener base-class that can be used to listen for entity-related events for a specific type of entity or for all entities. It hides away the type-checking and offers 4 methods that should be overridden: onCreate(..), onUpdate(..) and onDelete(..) when an entity is created, updated or deleted. For all other entity-related events, the onEntityEvent(..) is called.

### Configuration and setup

If an event-listener is configured in the process engine configuration, it will be active when the process engine starts and will remain active after subsequent reboots of the engine.

The property eventListeners expects a list of org.flowable.engine.delegate.event.FlowableEventListener instances. As usual, you can either declare an inline bean definition or use a ref to an existing bean instead. The snippet below adds an event-listener to the configuration that is notified when any event is dispatched, regardless of its type:

    <bean id="processEngineConfiguration"
        class="org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration">
        ...
        <property name="eventListeners">
          <list>
             <bean class="org.flowable.engine.example.MyEventListener" />
          </list>
        </property>
    </bean>

To get notified when certain types of events get dispatched, use the typedEventListeners property, which expects a map. The key of a map-entry is a comma-separated list of event-names (or a single event-name). The value of a map-entry is a list of org.flowable.engine.delegate.event.FlowableEventListener instances. The snippet below adds an event-listener to the configuration, that is notified when a job execution was successful or failed:

    <bean id="processEngineConfiguration"
        class="org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration">
        ...
        <property name="typedEventListeners">
          <map>
            <entry key="JOB_EXECUTION_SUCCESS,JOB_EXECUTION_FAILURE" >
              <list>
                <bean class="org.flowable.engine.example.MyJobEventListener" />
              </list>
            </entry>
          </map>
        </property>
    </bean>

The order of dispatching events is determined by the order in which the listeners were added. First, all normal event-listeners are called (eventListeners property) in the order they are defined in the list. After that, all typed event listeners (typedEventListeners properties) are called, if an event of the right type is dispatched.

### Adding listeners at runtime

It’s possible to add and remove additional event-listeners to the engine by using the API (RuntimeService):

    /**
     * Adds an event-listener which will be notified of ALL events by the dispatcher.
     * @param listenerToAdd the listener to add
     */
    void addEventListener(FlowableEventListener listenerToAdd);

    /**
     * Adds an event-listener which will only be notified when an event occurs,
     * which type is in the given types.
     * @param listenerToAdd the listener to add
     * @param types types of events the listener should be notified for
     */
    void addEventListener(FlowableEventListener listenerToAdd, FlowableEventType... types);

    /**
     * Removes the given listener from this dispatcher. The listener will no longer be notified,
     * regardless of the type(s) it was registered for in the first place.
     * @param listenerToRemove listener to remove
     */
     void removeEventListener(FlowableEventListener listenerToRemove);

Please note that the listeners added at runtime **are not retained when the engine is rebooted.**

### Adding listeners to process definitions

It’s possible to add listeners to a specific process-definition. The listeners will only be called for events related to the process definition and to all events related to process instances that are started with that specific process definition. The listener implementations can be defined using a fully qualified classname, an expression that resolves to a bean that implements the listener interface or can be configured to throw a message/signal/error BPMN event.

#### Listeners executing user-defined logic

The snippet below adds 2 listeners to a process-definition. The first listener will receive events of any type, with a listener implementation based on a fully-qualified class name. The second listener is only notified when a job is successfully executed or when it failed, using a listener that has been defined in the beans property of the process engine configuration.

    <process id="testEventListeners">
      <extensionElements>
        <flowable:eventListener class="org.flowable.engine.test.MyEventListener" />
        <flowable:eventListener delegateExpression="${testEventListener}" events="JOB_EXECUTION_SUCCESS,JOB_EXECUTION_FAILURE" />
      </extensionElements>

      ...

    </process>

For events related to entities, it’s also possible to add listeners to a process-definition that get only notified when entity-events occur for a certain entity type. The snippet below shows how this can be achieved. It can be used along for ALL entity-events (first example) or for specific event types only (second example).

    <process id="testEventListeners">
      <extensionElements>
        <flowable:eventListener class="org.flowable.engine.test.MyEventListener" entityType="task" />
        <flowable:eventListener delegateExpression="${testEventListener}" events="ENTITY_CREATED" entityType="task" />
      </extensionElements>

      ...

    </process>

Supported values for the entityType are: attachment, comment, execution, identity-link, job, process-instance, process-definition, task.

#### Listeners throwing BPMN events

Another way of handling events being dispatched is to throw a BPMN event. Please bear in mind that it only makes sense to throw BPMN-events with certain kinds of Flowable event types. For example, throwing a BPMN event when the process-instance is deleted will result in an error. The snippet below shows how to throw a signal inside process-instance, throw a signal to an external process (global), throw a message-event inside the process-instance and throw an error-event inside the process-instance. Instead of using the class or delegateExpression, the attribute throwEvent is used, along with an additional attribute, specific to the type of event being thrown.

    <process id="testEventListeners">
      <extensionElements>
        <flowable:eventListener throwEvent="signal" signalName="My signal" events="TASK_ASSIGNED" />
      </extensionElements>
    </process>

    <process id="testEventListeners">
      <extensionElements>
        <flowable:eventListener throwEvent="globalSignal" signalName="My signal" events="TASK_ASSIGNED" />
      </extensionElements>
    </process>

    <process id="testEventListeners">
      <extensionElements>
        <flowable:eventListener throwEvent="message" messageName="My message" events="TASK_ASSIGNED" />
      </extensionElements>
    </process>

    <process id="testEventListeners">
      <extensionElements>
        <flowable:eventListener throwEvent="error" errorCode="123" events="TASK_ASSIGNED" />
      </extensionElements>
    </process>

If additional logic is needed to decide whether or not to throw the BPMN-event, it’s possible to extend the listener-classes provided by Flowable. By overriding the isValidEvent(FlowableEvent event) in your subclass, the BPMN-event throwing can be prevented. The classes involved are +org.flowable.engine.test.api.event.SignalThrowingEventListenerTest, org.flowable.engine.impl.bpmn.helper.MessageThrowingEventListener and org.flowable.engine.impl.bpmn.helper.ErrorThrowingEventListener.

#### Notes on listeners on a process-definition

-   Event-listeners can only be declared on the process element, as a child-element of the extensionElements. Listeners cannot be defined on individual activities in the process.

-   Expressions used in the delegateExpression do not have access to the execution-context, as other expressions (for example, in gateways) have. They can only reference beans defined in the beans property of the process engine configuration or when using Spring (and the beans property is absent) to any spring-bean that implements the listener interface.

-   When using the class attribute of a listener, there will only be a single instance of that class created. Make sure the listener implementations do not rely on member-fields or ensure safe usage from multiple threads/contexts.

-   When an illegal event-type is used in the events attribute or illegal throwEvent value is used, an exception will be thrown when the process-definition is deployed (effectively failing the deployment). When an illegal value for class or delegateExecution is supplied (either a nonexistent class, a nonexistent bean reference or a delegate not implementing listener interface), an exception will be thrown when the process is started (or when the first valid event for that process-definition is dispatched to the listener). Make sure the referenced classes are on the classpath and that the expressions resolve to a valid instance.

### Dispatching events through API

We opened up the event-dispatching mechanism through the API, to allow you to dispatch custom events to any listeners that are registered in the engine. It’s recommended (although not enforced) to only dispatch FlowableEvents with type CUSTOM. Dispatching the event can be done using the RuntimeService:

    /**
     * Dispatches the given event to any listeners that are registered.
     * @param event event to dispatch.
     *
     * @throws FlowableException if an exception occurs when dispatching the event or
     * when the {@link FlowableEventDispatcher} is disabled.
     * @throws FlowableIllegalArgumentException when the given event is not suitable for dispatching.
     */
     void dispatchEvent(FlowableEvent event);

### Supported event types

Listed below are all event types that can occur in the engine. Each type corresponds to an enum value in the org.flowable.engine.common.api.delegate.event.FlowableEventType.

<table>
<caption>Supported events</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Event name</th>
<th>Description</th>
<th>Event classes</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>ENGINE_CREATED</p></td>
<td><p>The process-engine this listener is attached to has been created and is ready for API-calls.</p></td>
<td><p>org.flowable...FlowableEvent</p></td>
</tr>
<tr class="even">
<td><p>ENGINE_CLOSED</p></td>
<td><p>The process-engine this listener is attached to has been closed. API-calls to the engine are no longer possible.</p></td>
<td><p>org.flowable...FlowableEvent</p></td>
</tr>
<tr class="odd">
<td><p>ENTITY_CREATED</p></td>
<td><p>A new entity is created. The new entity is contained in the event.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>ENTITY_INITIALIZED</p></td>
<td><p>A new entity has been created and is fully initialized. If any children are created as part of the creation of an entity, this event will be fired AFTER the create/initialisation of the child entities as opposed to the ENTITY_CREATE event.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>ENTITY_UPDATED</p></td>
<td><p>An existing entity is updated. The updated entity is contained in the event.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>ENTITY_DELETED</p></td>
<td><p>An existing entity is deleted. The deleted entity is contained in the event.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>ENTITY_SUSPENDED</p></td>
<td><p>An existing entity is suspended. The suspended entity is contained in the event. Will be dispatched for ProcessDefinitions, ProcessInstances and Tasks.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>ENTITY_ACTIVATED</p></td>
<td><p>An existing entity is activated. The activated entity is contained in the event. Will be dispatched for ProcessDefinitions, ProcessInstances and Tasks.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>JOB_EXECUTION_SUCCESS</p></td>
<td><p>A job has been executed successfully. The event contains the job that was executed.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>JOB_EXECUTION_FAILURE</p></td>
<td><p>The execution of a job has failed. The event contains the job that was executed and the exception.</p></td>
<td><p>org.flowable...FlowableEntityEvent and org.flowable...FlowableExceptionEvent</p></td>
</tr>
<tr class="odd">
<td><p>JOB_RETRIES_DECREMENTED</p></td>
<td><p>The number of job retries have been decremented due to a failed job. The event contains the job that was updated.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>TIMER_SCHEDULED</p></td>
<td><p>A timer job has been created and is scheduled for being executed at a future point in time.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>TIMER_FIRED</p></td>
<td><p>A timer has been fired. The event contains the job that was executed.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>JOB_CANCELED</p></td>
<td><p>A job has been canceled. The event contains the job that was canceled. Job can be canceled by API call, task was completed and associated boundary timer was canceled, on the new process definition deployment.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>ACTIVITY_STARTED</p></td>
<td><p>An activity is starting to execute</p></td>
<td><p>org.flowable...FlowableActivityEvent</p></td>
</tr>
<tr class="even">
<td><p>ACTIVITY_COMPLETED</p></td>
<td><p>An activity is completed successfully</p></td>
<td><p>org.flowable...FlowableActivityEvent</p></td>
</tr>
<tr class="odd">
<td><p>ACTIVITY_CANCELLED</p></td>
<td><p>An activity is going to be canceled. There can be three reasons for activity cancellation (MessageEventSubscriptionEntity, SignalEventSubscriptionEntity, TimerEntity).</p></td>
<td><p>org.flowable...FlowableActivityCancelledEvent</p></td>
</tr>
<tr class="even">
<td><p>ACTIVITY_SIGNALED</p></td>
<td><p>An activity received a signal</p></td>
<td><p>org.flowable...FlowableSignalEvent</p></td>
</tr>
<tr class="odd">
<td><p>ACTIVITY_MESSAGE_RECEIVED</p></td>
<td><p>An activity received a message. Dispatched before the activity receives the message. When received, a ACTIVITY_SIGNAL or ACTIVITY_STARTED will be dispatched for this activity, depending on the type (boundary-event or event-subprocess start-event)</p></td>
<td><p>org.flowable...FlowableMessageEvent</p></td>
</tr>
<tr class="even">
<td><p>ACTIVITY_MESSAGE_WAITING</p></td>
<td><p>An activity has created a message event subscription and is waiting to receive.</p></td>
<td><p>org.flowable...FlowableMessageEvent</p></td>
</tr>
<tr class="odd">
<td><p>ACTIVITY_MESSAGE_CANCELLED</p></td>
<td><p>An activity for which a message event subscription has been created is canceled and thus receiving the message will not trigger this particular message anymore.</p></td>
<td><p>org.flowable...FlowableMessageEvent</p></td>
</tr>
<tr class="even">
<td><p>ACTIVITY_ERROR_RECEIVED</p></td>
<td><p>An activity has received an error event. Dispatched before the actual error has been handled by the activity. The event’s activityId contains a reference to the error-handling activity. This event will be either followed by a ACTIVITY_SIGNALLED event or ACTIVITY_COMPLETE for the involved activity, if the error was delivered successfully.</p></td>
<td><p>org.flowable...FlowableErrorEvent</p></td>
</tr>
<tr class="odd">
<td><p>UNCAUGHT_BPMN_ERROR</p></td>
<td><p>An uncaught BPMN error has been thrown. The process did not have any handlers for that specific error. The event’s activityId will be empty.</p></td>
<td><p>org.flowable...FlowableErrorEvent</p></td>
</tr>
<tr class="even">
<td><p>ACTIVITY_COMPENSATE</p></td>
<td><p>An activity is about to be compensated. The event contains the id of the activity that is will be executed for compensation.</p></td>
<td><p>org.flowable...FlowableActivityEvent</p></td>
</tr>
<tr class="odd">
<td><p>MULTI_INSTANCE_ACTIVITY_STARTED</p></td>
<td><p>A multi-instance activity is starting to execute</p></td>
<td><p>org.flowable...FlowableMultiInstanceActivityEvent</p></td>
</tr>
<tr class="even">
<td><p>MULTI_INSTANCE_ACTIVITY_COMPLETED</p></td>
<td><p>A multi-instance activity completed successfully</p></td>
<td><p>org.flowable...FlowableMultiInstanceActivityEvent</p></td>
</tr>
<tr class="odd">
<td><p>MULTI_INSTANCE_ACTIVITY_CANCELLED</p></td>
<td><p>A multi-instance activity is going to be canceled. There can be three reasons for activity cancellation (MessageEventSubscriptionEntity, SignalEventSubscriptionEntity, TimerEntity).</p></td>
<td><p>org.flowable...FlowableMultiInstanceActivityCancelledEvent</p></td>
</tr>
<tr class="even">
<td><p>VARIABLE_CREATED</p></td>
<td><p>A variable has been created. The event contains the variable name, value and related execution and task (if any).</p></td>
<td><p>org.flowable...FlowableVariableEvent</p></td>
</tr>
<tr class="odd">
<td><p>VARIABLE_UPDATED</p></td>
<td><p>An existing variable has been updated. The event contains the variable name, updated value and related execution and task (if any).</p></td>
<td><p>org.flowable...FlowableVariableEvent</p></td>
</tr>
<tr class="even">
<td><p>VARIABLE_DELETED</p></td>
<td><p>An existing variable has been deleted. The event contains the variable name, last known value and related execution and task (if any).</p></td>
<td><p>org.flowable...FlowableVariableEvent</p></td>
</tr>
<tr class="odd">
<td><p>TASK_ASSIGNED</p></td>
<td><p>A task has been assigned to a user. The event contains the task</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>TASK_CREATED</p></td>
<td><p>A task has been created. This is dispatched after the ENTITY_CREATE event. If the task is part of a process, this event will be fired before the task listeners are executed.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>TASK_COMPLETED</p></td>
<td><p>A task has been completed. This is dispatched before the ENTITY_DELETE event. If the task is part of a process, this event will be fired before the process has moved on and will be followed by a ACTIVITY_COMPLETE event, targeting the activity that represents the completed task.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>TASK_OWNER_CHANGED</p></td>
<td><p>A task owner has been changed. This is dispatched before the ENTITY_UPDATE event.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>TASK_PRIORITY_CHANGED</p></td>
<td><p>A task priority has been changed. This is dispatched before the ENTITY_UPDATE event.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>TASK_DUEDATE_CHANGED</p></td>
<td><p>A task due date has been changed. This is dispatched before the ENTITY_UPDATE event.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>TASK_NAME_CHANGED</p></td>
<td><p>A task name has been changed. This is dispatched before the ENTITY_UPDATE event.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>PROCESS_CREATED</p></td>
<td><p>A process instance has been created. All basic properties have been set, but variables not yet.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>PROCESS_STARTED</p></td>
<td><p>A process instance has been started. Dispatched when starting a process instance previously created. The event PROCESS_STARTED is dispatched after the associated event ENTITY_INITIALIZED and after the variables have been set.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="even">
<td><p>PROCESS_COMPLETED</p></td>
<td><p>A process has been completed, meaning all executions have stopped for the process instance. Dispatched after the last activity ACTIVITY_COMPLETED event. Process is completed when it reaches state in which process instance does not have any transition to take.</p></td>
<td><p>org.flowable...FlowableEntityEvent</p></td>
</tr>
<tr class="odd">
<td><p>PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT</p></td>
<td><p>A process has been completed by arriving at a terminate end event.</p></td>
<td><p>org.flowable...FlowableProcessTerminatedEvent</p></td>
</tr>
<tr class="even">
<td><p>PROCESS_CANCELLED</p></td>
<td><p>A process has been canceled. Dispatched before the process instance is deleted from runtime. A process instance can for example be canceled by the API call RuntimeService.deleteProcessInstance, by an interrupting boundary event on a call activity, …​</p></td>
<td><p>org.flowable...FlowableCancelledEvent</p></td>
</tr>
<tr class="odd">
<td><p>MEMBERSHIP_CREATED</p></td>
<td><p>A user has been added to a group. The event contains the ids of the user and group involved.</p></td>
<td><p>org.flowable...FlowableMembershipEvent</p></td>
</tr>
<tr class="even">
<td><p>MEMBERSHIP_DELETED</p></td>
<td><p>A user has been removed from a group. The event contains the ids of the user and group involved.</p></td>
<td><p>org.flowable...FlowableMembershipEvent</p></td>
</tr>
<tr class="odd">
<td><p>MEMBERSHIPS_DELETED</p></td>
<td><p>All members will be removed from a group. The event is thrown before the members are removed, so they are still accessible. No individual MEMBERSHIP_DELETED events will be thrown if all members are deleted at once, for performance reasons.</p></td>
<td><p>org.flowable...FlowableMembershipEvent</p></td>
</tr>
</tbody>
</table>

All ENTITY\_\\\* events are related to entities inside the engine. The list below show an overview of what entity-events are dispatched for which entities:

-   **ENTITY\_CREATED, ENTITY\_INITIALIZED, ENTITY\_DELETED**: Attachment, Comment, Deployment, Execution, Group, IdentityLink, Job, Model, ProcessDefinition, ProcessInstance, Task, User.

-   **ENTITY\_UPDATED**: Attachment, Deployment, Execution, Group, IdentityLink, Job, Model, ProcessDefinition, ProcessInstance, Task, User.

-   **ENTITY\_SUSPENDED, ENTITY\_ACTIVATED**: ProcessDefinition, ProcessInstance/Execution, Task.

### Additional remarks

**Listeners are only notified for events dispatched from the engine they are registered with.** So if you have different engines - running against the same database - only events that originated in the engine the listener is registered to are dispatched to that listener. The events that occur in other engines are not dispatched to the listeners, regardless of whether they are running in the same JVM or not.

Certain event-types (related to entities) expose the targeted entity. Depending on the type or event, these entities cannot be updated anymore (for example, when the entity is deleted). If possible, use the EngineServices exposed by the event to interact in a safe way with the engine. Even then, you need to be cautious with updates/operations on entities that are involved in the dispatched event.

No entity-events are dispatched related to history, as they all have a runtime-counterpart that dispatch their events.
