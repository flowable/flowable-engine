---
id: ch18-Advanced
title: Advanced
---

The following sections cover advanced use cases of Flowable, that go beyond typical execution of BPMN 2.0 processes. As such, a certain proficiency and experience with Flowable is advised to understand the topics described here.

## Async Executor

In Flowable v5, the Async executor was added in addition to the existing job executor. The Async Executor has proved to be more performant than the old job executor by many users of Flowable and in our benchmarks.

From Flowable V6, the async executor is the only one available. For V6, the async executor has been completely refactored for optimal performance and pluggability, while still being compatible with existing APIs.

### Async Executor design

Two types of jobs exist: timers (such as those belonging to a boundary event on a user task) and async continuations (belonging to a service task with the *flowable:async="true"* attribute).

**Timers** are the easiest to explain: they are persisted in the ACT\_RU\_TIMER\_JOB table with a certain due date. There is a thread in the async executor that periodically checks if there are new timers that should fire (in other words, the due date is 'before' the current time). When that happens, the timer is removed and an async job is created.

An **async job** is inserted in the database during the execution of process instance steps (which means, during some API call that was made). If the async executor is active for the current Flowable engine, the async job is actually already *locked*. This means that the job entry is inserted in the ACT\_RU\_JOB table and will have a *lock owner* and a *lock expiration time* set. A transaction listener that fires on a successful commit of the API call triggers the async executor of the same engine to execute the job (so the data is guaranteed to be in the database). To do this, the async executor has a configurable thread pool from which a thread will execute the job and continue the process asynchronously. If the Flowable engine does not have the async executor enabled, the async job is inserted in the ACT\_RU\_JOB table without being locked.

Similar to the thread that checks for new timers, the async executor has a thread that 'acquires' new async jobs. These are jobs that are present in the table that are not locked. This thread will lock these jobs for the current Flowable engine and pass it to the async executor.

The thread pool executing the jobs uses an in-memory queue from which to take jobs. When this queue is full (this is configurable), the job will be unlocked and re-inserted into its table. This way, other async executors can pick it up instead.

If an exception happens during job execution, the async job will be transformed to a timer job with a due date. Later, it will be picked up like a regular timer job and become an async job again, to be retried soon. When a job has been retried for a (configurable) number of times and continues to fail, the job is assumed to be 'dead' and moved to the ACT\_RU\_DEADLETTER\_JOB. The 'deadletter' concept is widely used in various other systems. An administrator will now need to inspect the exception for the failed job and decide what the best course of action is.

Process definitions and process instances can be suspended. Suspended jobs related to these definitions or instances are put in the ACT\_RU\_SUSPENDED\_JOB table, to make sure the query to acquire jobs has as few possible conditions in its where clause.

One thing that is clear from the above: for people familiar with the old implementations of the job/async executor, the main goal is to allow the 'acquire queries' to be as simple as possible. In the past (before V6), one table was used for all job types/states, which made the 'where' condition large as it catered for all the use cases. This problem is now solved and our benchmarks have proved that this new design delivers better performance and is more scalable.

### Async executor configuration

The async executor is a highly configurable component. It’s always recommended to look into the default settings of the async executor and validate if they match the requirements of your processes.

Alternatively, it’s possible to extend the default implementation or replace the *org.flowable.engine.impl.asyncexecutor.AsyncExecutor* interface with your own implementation.

The following properties are available on the process engine configuration through setters:

<table>
<caption>Async executor configuration options</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Name</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>asyncExecutorThreadPoolQueueSize</p></td>
<td><p>100</p></td>
<td><p>The size of the queue on which jobs to be executed are placed after being acquired, before they are actually executed by a thread from the thread pool</p></td>
</tr>
<tr class="even">
<td><p>asyncExecutorCorePoolSize</p></td>
<td><p>8</p></td>
<td><p>The minimal number of threads that are kept alive in the thread pool for job execution.</p></td>
</tr>
<tr class="odd">
<td><p>asyncExecutorMaxPoolSize</p></td>
<td><p>8</p></td>
<td><p>The maximum number of threads that are created in the thread pool for job execution.</p></td>
</tr>
<tr class="even">
<td><p>asyncExecutorThreadKeepAliveTime</p></td>
<td><p>5000</p></td>
<td><p>The time (in milliseconds) a thread used for job execution must be kept alive before it is destroyed. Having a setting &gt; 0 takes resources, but in the case of many job executions it avoids creating new threads all the time. If 0, threads will be destroyed after they’ve been used for job execution.</p></td>
</tr>
<tr class="odd">
<td><p>asyncExecutorNumberOfRetries</p></td>
<td><p>3</p></td>
<td><p>The number of times a job will be retried before it is moved to the 'deadletter' table.</p></td>
</tr>
<tr class="even">
<td><p>asyncExecutorMaxTimerJobsPerAcquisition</p></td>
<td><p>1</p></td>
<td><p>The number of timer jobs that are acquired in one query. Default value is 1, as this lowers the potential for optimistic locking exceptions. Larger values can perform better, but the chance of optimistic locking exceptions occurring between different engines becomes larger too.</p></td>
</tr>
<tr class="odd">
<td><p>asyncExecutorMaxAsyncJobsDuePerAcquisition</p></td>
<td><p>1</p></td>
<td><p>The number of async jobs that are acquired during one query. Default value is 1, as this lowers the potential for optimistic locking exceptions. Larger values can perform better, but the chance of optimistic locking exceptions occurring between different engines becomes larger too.</p></td>
</tr>
<tr class="even">
<td><p>asyncExecutorDefaultTimerJobAcquireWaitTime</p></td>
<td><p>10000</p></td>
<td><p>The time (in milliseconds) the timer acquisition thread will wait to execute the next query. This happens when no new timer jobs are found or when less timer jobs have been fetched than set in <em>asyncExecutorMaxTimerJobsPerAcquisition</em>.</p></td>
</tr>
<tr class="odd">
<td><p>asyncExecutorDefaultAsyncJobAcquireWaitTime</p></td>
<td><p>10000</p></td>
<td><p>The time (in milliseconds) the async job acquisition thread will wait to execute the next query. This happens when no new async jobs were found or when less async jobs have been fetched than set in <em>asyncExecutorMaxAsyncJobsDuePerAcquisition</em>.</p></td>
</tr>
<tr class="even">
<td><p>asyncExecutorDefaultQueueSizeFullWaitTime</p></td>
<td><p>0</p></td>
<td><p>The time (in milliseconds) the async job (both timer and async continuations) acquisition thread will wait when the internal job queue is full to execute the next query. By default set to 0 (for backwards compatibility). Setting this property to a higher value allows the async executor to hopefully clear its queue a bit.</p></td>
</tr>
<tr class="odd">
<td><p>asyncExecutorTimerLockTimeInMillis</p></td>
<td><p>5 minutes</p></td>
<td><p>The amount of time (in milliseconds) a timer job is locked when acquired by the async executor. During this period of time, no other async executor will try to acquire and lock this job.</p></td>
</tr>
<tr class="even">
<td><p>asyncExecutorAsyncJobLockTimeInMillis</p></td>
<td><p>5 minutes</p></td>
<td><p>The amount of time (in milliseconds) an async job is locked when acquired by the async executor. During this period of time, no other async executor will try to acquire and lock this job.</p></td>
</tr>
<tr class="odd">
<td><p>asyncExecutorSecondsToWaitOnShutdown</p></td>
<td><p>60</p></td>
<td><p>The time (in seconds) that is waited to gracefully shut down the thread pool used for job execution when a shutdown on the executor (or process engine) is requested.</p></td>
</tr>
<tr class="even">
<td><p>asyncExecutorResetExpiredJobsInterval</p></td>
<td><p>60 seconds</p></td>
<td><p>The amount of time (in milliseconds) that is between two consecutive checks of 'expired jobs'. Expired jobs are jobs that were locked (a lock owner + time was written by some executor, but the job was never completed). During such a check, jobs that are expired are made available again, meaning the lock owner and lock time will be removed. Other executors will now be able to pick it up. A job is deemed expired if the lock time is before the current date.</p></td>
</tr>
<tr class="odd">
<td><p>asyncExecutorResetExpiredJobsPageSize</p></td>
<td><p>3</p></td>
<td><p>The amount of jobs that are fetched at once by the 'reset expired' thread of the async executor.</p></td>
</tr>
</tbody>
</table>

### Message Queue based Async Executor

When reading the [async executor design section](bpmn/ch18-Advanced.md#async-executor-design), it becomes clear that the architecture is inspired by message queues. The async executor is designed in such a way that a message queue can easily be used to take over the job of the thread pool and the handling of async jobs.

Benchmarks have shown that using a message queue is superior, throughput-wise, to the thread pool-backed async executor. However, it does come with an extra architectural component, which of course makes setup, maintenance and monitoring more complex. For many users, the performance of the thread pool-backed async executor is more than sufficient. It is nice to know however, that there is an alternative if the required performance grows.

Currently, the only option that is supported out-of-the-box is JMS with Spring. The reason for supporting Spring before anything else is because Spring has some very nice features that ease a lot of the pain when it comes to threading and dealing with multiple message consumers. However, the integration is so simple, that it can easily be ported to any message queue implementation or protocol (Stomp, AMPQ, and so on). Feedback is appreciated for what should be the next implementation.

When a new async job is created by the engine, a message is put on a message queue (in a transaction committed transaction listener, so we’re sure the job entry is in the database) containing the job identifier. A message consumer then takes this job identifier to fetch the job, and execute the job. The async executor will not create a thread pool anymore. It will insert and query for timers from a separate thread. When a timer fires, it is moved to the async job table, which now means a message is sent to the message queue too. The 'reset expired' thread will also unlock jobs as usual, as message queues can fail too. Instead of 'unlocking' a job, a message will now be resent. The async executor will not poll for async jobs anymore.

The implementation consists of two classes:

-   An implementation of the *org.flowable.engine.impl.asyncexecutor.JobManager* interface that puts a message on a message queue instead of passing it to the thread pool.

-   A *javax.jms.MessageListener* implementation that consumes a message from the message queue, using the job identifier in the message to fetch and execute the job.

First of all, add the *flowable-jms-spring-executor* dependency to your project:

    <dependency>
      <groupId>org.flowable</groupId>
      <artifactId>flowable-jms-spring-executor</artifactId>
      <version>${flowable.version}</version>
    </dependency>

To enable the message queue based async executor, in the process engine configuration, the following needs to be done:

-   *asyncExecutorActivate* must be set to *true*, as usual

-   *asyncExecutorMessageQueueMode* needs to be set to *true*

-   The *org.flowable.spring.executor.jms.MessageBasedJobManager* must be injected as *JobManager*

Below is a complete example of a Java based configuration, using *ActiveMQ* as the message queue broker.

Some things to note:

-   The *MessageBasedJobManager* expects a *JMSTemplate* to be injected that is configured with a correct *connectionFactory*.

-   We’re using the *MessageListenerContainer* concept from Spring, as this simplifies threading and multiple consumers a lot.

<!-- -->

    @Configuration
    public class SpringJmsConfig {

      @Bean
      public DataSource dataSource() {
        // Omitted
      }

      @Bean(name = "transactionManager")
      public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
      }

      @Bean
      public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
        JobManager jobManager) {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setDatabaseSchemaUpdate(SpringProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setAsyncExecutorMessageQueueMode(true);
        configuration.setAsyncExecutorActivate(true);
        configuration.setJobManager(jobManager);
        return configuration;
      }

      @Bean
      public ProcessEngine processEngine(ProcessEngineConfiguration processEngineConfiguration) {
        return processEngineConfiguration.buildProcessEngine();
      }

      @Bean
      public MessageBasedJobManager jobManager(JmsTemplate jmsTemplate) {
        MessageBasedJobManager jobManager = new MessageBasedJobManager();
        jobManager.setJmsTemplate(jmsTemplate);
        return jobManager;
      }

      @Bean
      public ConnectionFactory connectionFactory() {
          ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
          activeMQConnectionFactory.setUseAsyncSend(true);
          activeMQConnectionFactory.setAlwaysSessionAsync(true);
          return new CachingConnectionFactory(activeMQConnectionFactory);
      }

      @Bean
      public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
          JmsTemplate jmsTemplate = new JmsTemplate();
          jmsTemplate.setDefaultDestination(new ActiveMQQueue("flowable-jobs"));
          jmsTemplate.setConnectionFactory(connectionFactory);
          return jmsTemplate;
      }

      @Bean
      public MessageListenerContainer messageListenerContainer(JobMessageListener jobMessageListener) {
          DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();
          messageListenerContainer.setConnectionFactory(connectionFactory());
          messageListenerContainer.setDestinationName("flowable-jobs");
          messageListenerContainer.setMessageListener(jobMessageListener);
          messageListenerContainer.setConcurrentConsumers(2);
          messageListenerContainer.start();
          return messageListenerContainer;
      }

      @Bean
      public JobMessageListener jobMessageListener(ProcessEngineConfiguration processEngineConfiguration) {
        JobMessageListener jobMessageListener = new JobMessageListener();
        jobMessageListener.setProcessEngineConfiguration(processEngineConfiguration);
        return jobMessageListener;
      }

    }

In the code above, the *JobMessageListener* and *MessageBasedJobManager* are the only classes from the *flowable-jms-spring-executor* module. All the other code is from Spring. As such, when wanting to port this to other queues/protocols, these classes must be ported.

## Hooking into process parsing

A BPMN 2.0 XML needs to be parsed to the Flowable internal model to be executed on the Flowable engine. This parsing happens during a deployment of the process or when a process is not found in memory, and the XML is fetched from the database.

For each of these processes, the BpmnParser class creates a new BpmnParse instance. This instance will be used as container for all things that are done during parsing. The parsing, by itself, is very simple: for each BPMN 2.0 element, there is a matching instance of the org.flowable.engine.parse.BpmnParseHandler available in the engine. As such, the parser has a map that basically maps a BPMN 2.0 element class to an instance of BpmnParseHandler. By default, Flowable has BpmnParseHandler instances to handle all supported elements and also uses it to attach execution listeners to steps of the process for creating the history.

It is possible to add custom instances of org.flowable.engine.parse.BpmnParseHandler to the Flowable engine. An often seen use case, for example, is to add execution listeners to certain steps that fire events to some queue for event processing. The history handling is done in such a way internally in Flowable. To add such custom handlers, the Flowable configuration needs to be tweaked:

    <property name="preBpmnParseHandlers">
      <list>
        <bean class="org.flowable.parsing.MyFirstBpmnParseHandler" />
      </list>
    </property>

    <property name="postBpmnParseHandlers">
      <list>
        <bean class="org.flowable.parsing.MySecondBpmnParseHandler" />
        <bean class="org.flowable.parsing.MyThirdBpmnParseHandler" />
      </list>
    </property>

The list of BpmnParseHandler instances that is configured in the preBpmnParseHandlers property are added before any of the default handlers. Likewise, the postBpmnParseHandlers are added after those. This can be important if the order of things matter for the logic contained in the custom parse handlers.

org.flowable.engine.parse.BpmnParseHandler is a simple interface:

    public interface BpmnParseHandler {

      Collection<Class>? extends BaseElement>> getHandledTypes();

      void parse(BpmnParse bpmnParse, BaseElement element);

    }

The getHandledTypes() method returns a collection of all the types handled by this parser. The possible types are a subclass of BaseElement, as directed by the generic type of the collection. You can also extend the AbstractBpmnParseHandler class and override the getHandledType() method, which only returns one Class and not a collection. This class also contains some helper methods shared by many of the default parse handlers. The BpmnParseHandler instance will be called when the parser encounters any of the types returned by this method. In the following example, whenever a process contained in some BPMN 2.0 XML is encountered, it will execute the logic in the executeParse method (which is a typecast method that replaces the regular parse method on the BpmnParseHandler interface).

    public class TestBPMNParseHandler extends AbstractBpmnParseHandler<Process> {

      protected Class<? extends BaseElement> getHandledType() {
        return Process.class;
      }

      protected void executeParse(BpmnParse bpmnParse, Process element) {
         ..
      }

    }

**Important note:** when writing custom parse handlers, do not use any of the internal classes that are used to parse the BPMN 2.0 constructs. This will cause difficult to find bugs. The safe way to implement a custom handler is to implement the *BpmnParseHandler* interface or extend the internal abstract class *org.flowable.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler*.

It is possible (but less common) to replace the default BpmnParseHandler instances that are responsible for the parsing of the BPMN 2.0 elements to the internal Flowable model. This can be done by following snippet of logic:

    <property name="customDefaultBpmnParseHandlers">
      <list>
        ...
      </list>
    </property>

A simple example could, for example, be to force all of the service tasks to be asynchronous:

    public class CustomUserTaskBpmnParseHandler extends ServiceTaskParseHandler {

      protected void executeParse(BpmnParse bpmnParse, ServiceTask serviceTask) {

        // Do the regular stuff
        super.executeParse(bpmnParse, serviceTask);

        // Make always async
        serviceTask.setAsynchronous(true);
      }

    }

## UUID ID generator for high concurrency

In some (very) high concurrency load cases, the default ID generator may cause exceptions due to not being able to fetch new ID blocks quickly enough. Every process engine has one ID generator. The default ID generator reserves a block of IDs in the database, such that no other engine will be able to use IDs from the same block. During engine operations, when the default ID generator notices that the ID block is used up, a new transaction is started to fetch a new block. In (very) limited use cases this can cause problems when there is a real high load. For most use cases the default ID generator is more than sufficient. The default org.flowable.engine.impl.db.DbIdGenerator also has a property idBlockSize which can be configured to set the size of the reserved block of IDs and to tweak the behavior of the ID fetching.

The alternative to the default ID generator is the org.flowable.engine.impl.persistence.StrongUuidGenerator, which generates a unique [UUID](http://en.wikipedia.org/wiki/Universally_unique_identifier) locally and uses that as an identifier for all entities. Since the UUID is generated without the need for database access, it copes better with very high concurrency use cases. Do note that performance may differ from the default ID generator (both positive and negative) depending on the machine.

The UUID generator can be set up in the Flowable configuration as follows:

    <property name="idGenerator">
        <bean class="org.flowable.engine.impl.persistence.StrongUuidGenerator" />
    </property>

The use of the UUID ID generator has the following extra dependency:

     <dependency>
        <groupId>com.fasterxml.uuid</groupId>
        <artifactId>java-uuid-generator</artifactId>
        <version>3.1.3</version>
    </dependency>

## Multitenancy

Multitenancy in general is a concept where the software is capable of serving multiple different organizations. Key is that the data is partitioned and no organization can see the data of other ones. In this context, such an organization (or a department, or a team or whatever, is called a *tenant*.

Note that this is fundamentally different from a multi-instance setup, where a Flowable Process engine instance is running for each organization separately (and with a different database schema). Although Flowable is lightweight, and running a Process Engine instance doesn’t take much resources, it does add complexity and more maintenance. But, for some use cases it might be the right solution.

Multitenancy in Flowable is mainly implemented around partitioning the data. It is important to note that *Flowable does not enforce multi tenancy rules*. This means it will not verify when querying and using data whether the user doing the operation belongs to the correct tenant. This should be done in the layer calling the Flowable engine. Flowable does make sure that tenant information can be stored and used when retrieving process data.

When deploying process definition to the Flowable Process engine it is possible to pass a *tenant identifier*. This is a string (e.g. a UUID, department id, etc.), limited to 256 characters which uniquely identifies the tenant:

    repositoryService.createDeployment()
                .addClassPathResource(...)
                .tenantId("myTenantId")
                .deploy();

Passing a tenant ID during a deployment has following implications:

-   All the process definitions contained in the deployment inherit the tenant identifier from this deployment.

-   All process instances started from those process definitions inherit this tenant identifier from the process definition.

-   All tasks created at runtime when executing the process instance inherit this tenant identifier from the process instance. Standalone tasks can have a tenant identifier too.

-   All executions created during process instance execution inherit this tenant identifier from the process instance.

-   Firing a signal throw event (in the process itself or through the API) can be done whilst providing a tenant identifier. The signal will only be executed in the tenant context: i.e. if there are multiple signal catch events with the same name, only the one with the correct tenant identifier will actually be called.

-   All jobs (timers and async continuations) inherit the tenant identifier from either the process definition (e.g. timer start event) or the process instance (when a job is created at runtime, e.g. an async continuation). This could potentially be used for giving priority to some tenants in a custom job executor.

-   All the historic entities (historic process instance, task and activities) inherit the tenant identifier from their runtime counterparts.

-   As a side note, models can have a tenant identifier too (models are used e.g. by the Flowable modeler to store BPMN 2.0 models).

To actually make use of the tenant identifier on the process data, all the query API’s have the capability to filter on tenant. For example (and can be replaced by the relevant query implementation of the other entities):

    runtimeService.createProcessInstanceQuery()
        .processInstanceTenantId("myTenantId")
        .processDefinitionKey("myProcessDefinitionKey")
        .variableValueEquals("myVar", "someValue")
        .list()

The query API’s also allow to filter on the tenant identifier with *like* semantics and also to filter out entities without tenant id.

**Important implementation detail:** due to database quirks (more specifically: null handling in unique constraints) the *default* tenant identifier value indicating *no tenant* is the **empty string**. The combination of (process definition key, process definition version, tenant identifier) needs to be unique (and there is a database constraint checking this). Also note that the tenant identifier shouldn’t be set to null, as this will affect the queries since certain databases (Oracle) treat empty string as a null value (that’s why the query *.withoutTenantId* does a check against the empty string or null). This means that the same process definition (with same process definition key) can be deployed for multiple tenants, each with their own versioning. This does not affect the usage when tenancy is not used.

**Do note that all of the above does not conflict with running multiple Flowable instances in a cluster.**

\[Experimental\] It is possible to change the tenant identifier by calling the *changeDeploymentTenantId(String deploymentId, String newTenantId)* method on the *repositoryService*. This will change the tenant identifier everywhere it was inherited before. This can be useful when going from a non-multitenant setup to a multitenant configuration. See the Javadoc on the method for more detailed information.

## Execute custom SQL

The Flowable API allows for interacting with the database using a high level API. For example, for retrieving data the Query API and the Native Query API are powerful in its usage. However, for some use cases they might not be flexible enough. The following section describes how a completely custom SQL statement (select, insert, update and delete are possible) can be executed against the Flowable data store, but completely within the configured Process Engine (and thus levering the transaction setup for example).

To define custom SQL statements, the Flowable engine leverages the capabilities of its underlying framework, MyBatis. More info can be read [in the MyBatis user guide](http://mybatis.github.io/mybatis-3/java-api.html).

### Annotation based Mapped Statements

The first thing to do when using Annotation based Mapped Statements, is to create a MyBatis mapper class. For example, suppose that for some use case not the whole task data is needed, but only a small subset of it. A Mapper that could do this, looks as follows:

    public interface MyTestMapper {

        @Select("SELECT ID_ as id, NAME_ as name, CREATE_TIME_ as createTime FROM ACT_RU_TASK")
        List<Map<String, Object>> selectTasks();

    }

This mapper must be provided to the Process Engine configuration as follows:

    ...
    <property name="customMybatisMappers">
      <set>
        <value>org.flowable.standalone.cfg.MyTestMapper</value>
      </set>
    </property>
    ...

Notice that this is an interface. The underlying MyBatis framework will make an instance of it that can be used at runtime. Also notice that the return value of the method is not typed, but a list of maps (which corresponds to the list of rows with column values). Typing is possible with the MyBatis mappers if wanted.

To execute the query above, the *managementService.executeCustomSql* method must be used. This method takes in a *CustomSqlExecution* instance. This is a wrapper that hides the internal bits of the engine otherwise needed to make it work.

Unfortunately, Java generics make it a bit less readable than it could have been. The two generic types below are the mapper class and the return type class. However, the actual logic is simply to call the mapper method and return its results (if applicable).

    CustomSqlExecution<MyTestMapper, List<Map<String, Object>>> customSqlExecution =
              new AbstractCustomSqlExecution<MyTestMapper, List<Map<String, Object>>>(MyTestMapper.class) {

      public List<Map<String, Object>> execute(MyTestMapper customMapper) {
        return customMapper.selectTasks();
      }

    };

    List<Map<String, Object>> results = managementService.executeCustomSql(customSqlExecution);

The Map entries in the list above will only contain *id, name and create time* in this case and not the full task object.

Any SQL is possible when using the approach above. Another more complex example:

        @Select({
            "SELECT task.ID_ as taskId, variable.LONG_ as variableValue FROM ACT_RU_VARIABLE variable",
            "inner join ACT_RU_TASK task on variable.TASK_ID_ = task.ID_",
            "where variable.NAME_ = #{variableName}"
        })
        List<Map<String, Object>> selectTaskWithSpecificVariable(String variableName);

Using this method, the task table will be joined with the variables table. Only where the variable has a certain name is retained, and the task id and the corresponding numerical value is returned.

For a working example on using Annotation based Mapped Statements check the unit test *org.flowable.standalone.cfg.CustomMybatisMapperTest* and other classes and resources in folders src/test/java/org/flowable/standalone/cfg/ and src/test/resources/org/flowable/standalone/cfg/

### XML based Mapped Statements

When using XML based Mapped Statements, statements are defined in XML files. For the use case where not the whole task data is needed, but only a small subset of it. The XML file can look as follows:

    <mapper namespace="org.flowable.standalone.cfg.TaskMapper">

      <resultMap id="customTaskResultMap" type="org.flowable.standalone.cfg.CustomTask">
        <id property="id" column="ID_" jdbcType="VARCHAR"/>
        <result property="name" column="NAME_" jdbcType="VARCHAR"/>
        <result property="createTime" column="CREATE_TIME_" jdbcType="TIMESTAMP" />
      </resultMap>

      <select id="selectCustomTaskList" resultMap="customTaskResultMap">
        select RES.ID_, RES.NAME_, RES.CREATE_TIME_ from ACT_RU_TASK RES
      </select>

    </mapper>

Results are mapped to instances of *org.flowable.standalone.cfg.CustomTask* class which can look as follows:

    public class CustomTask {

      protected String id;
      protected String name;
      protected Date createTime;

      public String getId() {
        return id;
      }
      public String getName() {
        return name;
      }
      public Date getCreateTime() {
        return createTime;
      }
    }

Mapper XML files must be provided to the Process Engine configuration as follows:

    ...
    <property name="customMybatisXMLMappers">
      <set>
        <value>org/flowable/standalone/cfg/custom-mappers/CustomTaskMapper.xml</value>
      </set>
    </property>
    ...

The statement can be executed as follows:

    List<CustomTask> tasks = managementService.executeCommand(new Command<List<CustomTask>>() {

          @SuppressWarnings("unchecked")
          @Override
          public List<CustomTask> execute(CommandContext commandContext) {
            return (List<CustomTask>) CommandContextUtil.getDbSqlSession().selectList("selectCustomTaskList");
          }
        });

For uses cases that require more complicated statements, XML Mapped Statements can be helpful. Since Flowable uses XML Mapped Statements internally, it’s possible to make use of the underlying capabilities.

Suppose that for some use case the ability to query attachments data is required based on id, name, type, userId, etc! To fulfill the use case a query class *AttachmentQuery* that extends *org.flowable.engine.impl.AbstractQuery* can be created as follows:

    public class AttachmentQuery extends AbstractQuery<AttachmentQuery, Attachment> {

      protected String attachmentId;
      protected String attachmentName;
      protected String attachmentType;
      protected String userId;

      public AttachmentQuery(ManagementService managementService) {
        super(managementService);
      }

      public AttachmentQuery attachmentId(String attachmentId){
        this.attachmentId = attachmentId;
        return this;
      }

      public AttachmentQuery attachmentName(String attachmentName){
        this.attachmentName = attachmentName;
        return this;
      }

      public AttachmentQuery attachmentType(String attachmentType){
        this.attachmentType = attachmentType;
        return this;
      }

      public AttachmentQuery userId(String userId){
        this.userId = userId;
        return this;
      }

      @Override
      public long executeCount(CommandContext commandContext) {
        return (Long) CommandContextUtil.getDbSqlSession()
                       .selectOne("selectAttachmentCountByQueryCriteria", this);
      }

      @Override
      public List<Attachment> executeList(CommandContext commandContext, Page page) {
        return CommandContextUtil.getDbSqlSession()
                .selectList("selectAttachmentByQueryCriteria", this);
      }

Note that when extending *AbstractQuery* extended classes should pass an instance of *ManagementService* to super constructor and methods *executeCount* and *executeList* need to be implemented to call the mapped statements.

The XML file containing the mapped statements can look as follows:

    <mapper namespace="org.flowable.standalone.cfg.AttachmentMapper">

      <select id="selectAttachmentCountByQueryCriteria" parameterType="org.flowable.standalone.cfg.AttachmentQuery" resultType="long">
        select count(distinct RES.ID_)
        <include refid="selectAttachmentByQueryCriteriaSql"/>
      </select>

      <select id="selectAttachmentByQueryCriteria" parameterType="org.flowable.standalone.cfg.AttachmentQuery" resultMap="org.flowable.engine.impl.persistence.entity.AttachmentEntity.attachmentResultMap">
        ${limitBefore}
        select distinct RES.* ${limitBetween}
        <include refid="selectAttachmentByQueryCriteriaSql"/>
        ${orderBy}
        ${limitAfter}
      </select>

      <sql id="selectAttachmentByQueryCriteriaSql">
      from ${prefix}ACT_HI_ATTACHMENT RES
      <where>
       <if test="attachmentId != null">
         RES.ID_ = #{attachmentId}
       </if>
       <if test="attachmentName != null">
         and RES.NAME_ = #{attachmentName}
       </if>
       <if test="attachmentType != null">
         and RES.TYPE_ = #{attachmentType}
       </if>
       <if test="userId != null">
         and RES.USER_ID_ = #{userId}
       </if>
      </where>
      </sql>
    </mapper>

Capabilities such as pagination, ordering, table name prefixing are available and can be used in the statements (since the parameterType is a subclass of *AbstractQuery*). Note that to map results the predefined *org.flowable.engine.impl.persistence.entity.AttachmentEntity.attachmentResultMap* resultMap can be used.

Finally, the *AttachmentQuery* can be used as follows:

    ....
    // Get the total number of attachments
    long count = new AttachmentQuery(managementService).count();

    // Get attachment with id 10025
    Attachment attachment = new AttachmentQuery(managementService).attachmentId("10025").singleResult();

    // Get first 10 attachments
    List<Attachment> attachments = new AttachmentQuery(managementService).listPage(0, 10);

    // Get all attachments uploaded by user kermit
    attachments = new AttachmentQuery(managementService).userId("kermit").list();
    ....

For working examples on using XML Mapped Statements check the unit test *org.flowable.standalone.cfg.CustomMybatisXMLMapperTest* and other classes and resources in folders src/test/java/org/flowable/standalone/cfg/ and src/test/resources/org/flowable/standalone/cfg/

## Advanced Process Engine configuration with a ProcessEngineConfigurator

An advanced way of hooking into the process engine configuration is through the use of a *ProcessEngineConfigurator*. The idea is that an implementation of the *org.flowable.engine.cfg.ProcessEngineConfigurator* interface is created and injected into the process engine configuration:

    <bean id="processEngineConfiguration" class="...SomeProcessEngineConfigurationClass">

        ...

        <property name="configurators">
            <list>
                <bean class="com.mycompany.MyConfigurator">
                    ...
                </bean>
            </list>
        </property>

        ...

    </bean>

There are two methods required to implement this interface. The *configure* method, which gets a *ProcessEngineConfiguration* instance as parameter. The custom configuration can be added this way, and this method will guaranteed be called **before the process engine is created, but after all default configuration has been done**. The other method is the *getPriority* method, which allows for ordering the configurators in the case where some configurators are dependent on each other.

An example of such a configurator is the [LDAP integration](bpmn/ch17-Ldap.md#ldap-integration), where the configurator is used to replace the default user and group manager classes with one that is capable of handling an LDAP user store. So basically a configurator allows to change or tweak the process engine quite heavily and is meant for very advanced use cases. Another example is to swap the process definition cache with a customized version:

    public class ProcessDefinitionCacheConfigurator extends AbstractProcessEngineConfigurator {

        public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
                MyCache myCache = new MyCache();
                processEngineConfiguration.setProcessDefinitionCache(enterpriseProcessDefinitionCache);
        }

    }

Process Engine configurators can also be auto discovered from the classpath using the [ServiceLoader](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) approach. This means that a jar with the configurator implementation must be put on the classpath, containing a file in the *META-INF/services* folder in the jar called **org.flowable.engine.cfg.ProcessEngineConfigurator**. The content of the file needs to be the fully qualified classname of the custom implementation. When the process engine is booted, the logging will show that these configurators are found:

    INFO  org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl  - Found 1 auto-discoverable Process Engine Configurators
    INFO  org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl  - Found 1 Process Engine Configurators in total:
    INFO  org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl  - class org.flowable.MyCustomConfigurator

Note that this ServiceLoader approach might not work in certain environments. It can be explicitly disabled using the *enableConfiguratorServiceLoader* property of the ProcessEngineConfiguration (true by default).

## Advanced query API: seamless switching between runtime and historic task querying

One core component of any BPM user interface is the task list. Typically, end users work on open, runtime tasks, filtering their inbox with various setting. Often also the historic tasks need to be displayed in those lists, with similar filtering. To make that code-wise easier, the *TaskQuery* and *HistoricTaskInstanceQuery* both have a shared parent interface, which contains all common operations (and most of the operations are common).

This common interface is the *org.flowable.engine.task.TaskInfoQuery* class. Both *org.flowable.engine.task.Task* and *org.flowable.engine.task.HistoricTaskInstance* have a common superclass *org.flowable.engine.task.TaskInfo* (with common properties) which is returned from e.g. the *list()* method. However, Java generics are sometimes more harming than helping: if you want to use the *TaskInfoQuery* type directly, it would look like this:

    TaskInfoQuery<? extends TaskInfoQuery<?,?>, ? extends TaskInfo> taskInfoQuery

Ugh, Right. To 'solve' this, a *org.flowable.engine.task.TaskInfoQueryWrapper* class that can be used to avoid the generics (the following code could come from REST code that returns a task list where the user can switch between open and completed tasks):

    TaskInfoQueryWrapper taskInfoQueryWrapper = null;
    if (runtimeQuery) {
        taskInfoQueryWrapper = new TaskInfoQueryWrapper(taskService.createTaskQuery());
    } else {
        taskInfoQueryWrapper = new TaskInfoQueryWrapper(historyService.createHistoricTaskInstanceQuery());
    }

    List<? extends TaskInfo> taskInfos = taskInfoQueryWrapper.getTaskInfoQuery().or()
        .taskNameLike("%k1%")
        .taskDueAfter(new Date(now.getTime() + (3 * 24L * 60L * 60L * 1000L)))
    .endOr()
    .list();

## Custom identity management by overriding standard SessionFactory

If you do not want to use a full *ProcessEngineConfigurator* implementation like in the [LDAP integration](bpmn/ch17-Ldap.md#ldap-integration), but still want to plug in your custom identity management framework, then you can also override the *IdmIdentityServiceImpl* class or implement the *IdmIdentityService* interface directly and use the implemented class for the *idmIdentityService* property in the *ProcessEngineConfiguration*. In Spring this can be easily done by adding the following to the *ProcessEngineConfiguration* bean definition:

    <bean id="processEngineConfiguration" class="...SomeProcessEngineConfigurationClass">

        ...

        <property name="idmIdentityService">
            <bean class="com.mycompany.IdmIdentityServiceBean"/>
        </property>

        ...

    </bean>

Have a look at the *LDAPIdentityServiceImpl* class implementation to have a good example of how to implement the methods of the *IdmIdentityService* interface.
You have to figure out which methods you want to implement in the custom identity service class. For example the following call:

    long potentialOwners = identityService.createUserQuery().memberOfGroup("management").count();

leads to a call on the following member of the *IdmIdentityService* interface:

    UserQuery createUserQuery();

The code for the [LDAP integration](bpmn/ch17-Ldap.md#ldap-integration) contains full examples of how to implement this. Check out the code on Github: [LDAPIdentityServiceImpl](https://github.com/flowable/flowable-engine/blob/master/modules/flowable-ldap/src/main/java/org/flowable/ldap/LDAPIdentityServiceImpl.java).

## Enable safe BPMN 2.0 xml

In most cases the BPMN 2.0 processes that are being deployed to the Flowable engine are under tight control of e.g. the development team. However, in some use cases it might be desirable to upload arbitrary BPMN 2.0 xml to the engine. In that case, take into consideration that a user with bad intentions can bring the server down as described [here](http://www.jorambarrez.be/blog/2013/02/19/uploading-a-funny-xml-can-bring-down-your-server/).

To avoid the attacks described in the link above, a property *enableSafeBpmnXml* can be set on the process engine configuration:

    <property name="enableSafeBpmnXml" value="true"/>

**By default this feature is disabled!** The reason for this is that it relies on the availability of the StaxSource class of the JDK. Unfortunately, on some platforms this class is unavailable (due to older xml parser implementation) and thus the safe BPMN 2.0 xml feature cannot be enabled.

If the platform on which Flowable runs does support it, do enable this feature.

## Event logging

An event logging mechanism has been introduced. The logging mechanism builds upon the general-purpose [event mechanism of the Flowable engine](bpmn/ch03-Configuration.md#event-handlers) and is disabled by default. The idea is that the events originating from the engine are caught, and a map containing all the event data (and some more) is created and provided to an *org.flowable.engine.impl.event.logger.EventFlusher* which will flush this data to somewhere else. By default, simple database-backed event handlers/flusher is used, which serializes the said map to JSON using Jackson and stores it in the database as an *EventLogEntryEntity* instance. The table required for this database logging is created by default (called *ACT\_EVT\_LOG*). This table can be deleted if the event logging is not used.

To enable the database logger:

    processEngineConfigurationImpl.setEnableDatabaseEventLogging(true);

or at runtime:

    databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(),
                                          processEngineConfiguration.getObjectMapper());
    runtimeService.addEventListener(databaseEventLogger);

The EventLogger class can be extended. In particular, the *createEventFlusher()* method needs to return an instance of the *org.flowable.engine.impl.event.logger.EventFlusher* interface if the default database logging is not wanted. The *managementService.getEventLogEntries(startLogNr, size);* can be used to retrieve the *EventLogEntryEntity* instances through Flowable.

It is easy to see how this table data can now be used to feed the JSON into a big data NoSQL store such as MongoDB, Elastic Search, etc. It is also easy to see that the classes used here (org.flowable.engine.impl.event.logger.EventLogger/EventFlusher and many EventHandler classes) are pluggable and can be tweaked to your own use case (eg not storing the JSON in the database, but firing it straight onto a queue or big data store).

Note that this event logging mechanism is additional to the 'traditional' history manager of Flowable. Although all the data is in the database tables,
it is not optimized for querying nor for easy retrieval. The real use case is audit trailing and feeding it into a big data store.

## Disabling bulk inserts

By default, the engine will group multiple insert statements for the same database table together in a *bulk insert*, thus improving performance. This has been tested and implemented for all supported databases.

However, it could be a specific version of a supported and tested database does not allow bulk inserts (we have for example a report for DB2 on z/OS, although DB2 in general works), the bulk insert can be disabled on the process engine configuration:

    <property name="bulkInsertEnabled" value="false" />

## Secure Scripting

By default, when using a [script task](bpmn/ch07b-BPMN-Constructs.md#script-Task), the script that is executed has similar capabilities as a Java delegate. It has full access to the JVM, can run forever (due to infinite loops) or use up a lot of memory. However, Java delegates need to be written and put on the classpath in a jar and they have a different life cycle from a process definitions. End-users generally will not write Java delegates, as this is a typical the job of a developer.

Scripts on the other hand are part of the process definition and its lifecycle is the same. Script tasks don’t need the extra step of a jar deployment, but can be executed from the moment the process definition is deployed. Sometimes, scripts for script tasks are not written by developers. Yet, this poses a problem as stated above: a script has full access to the JVM and it is possible to block many system resources when executing the script. Allowing scripts from just about anyone is thus not a good idea.

To solve this problem, the *secure scripting* feature can be enabled. Currently, this feature is implemented for *javascript* scripting only. To enable it, add the *flowable-secure-javascript* dependency to your project. When using maven:

    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-secure-javascript</artifactId>
        <version>${flowable.version}</version>
    </dependency>

Adding this dependency will transitively bring in the Rhino dependency (see [<https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino>]($https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino$$)). Rhino is a javascript engine for the JDK. It used to be included in JDK version 6 and 7 and was superseded by the Nashorn engine. However, the Rhino project continued development after it was included in the JDK. Many features (including the ones Flowable uses to implement the secure scripting) were added afterwards. At the time of writing, the Nashorn engine **does not** have the features that are needed to implement the secure scripting feature.

This does mean that there could be (typically small) differences between scripts (for example, *importPackage* works on Rhino, but *load()* has to be used on Nashorn).

Configuring the secure scripting is done through a dedicated *Configurator* object that is passed to the process engine configuration before the process engine is instantiated:

    SecureJavascriptConfigurator configurator = new SecureJavascriptConfigurator()
      .setWhiteListedClasses(new HashSet<String>(Arrays.asList("java.util.ArrayList")))
      .setMaxStackDepth(10)
      .setMaxScriptExecutionTime(3000L)
      .setMaxMemoryUsed(3145728L)
      .setNrOfInstructionsBeforeStateCheckCallback(10);

    processEngineConfig.addConfigurator(configurator);

Following settings are possible:

-   **enableClassWhiteListing**: When true, all classes will be blacklisted and all classes that want to be used will need to be whitelisted individually. This gives tight control over what is exposed to scripts. By default *false*.

-   **whiteListedClasses**: a Set of Strings corresponding with fully qualified classnames of the classes that are allowed to be used in the script. For example, to expose the *execution* object in a script, the *org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl* String needs to be added to this Set. By default *empty*.

-   **maxStackDepth**: Limits the stack depth while calling functions within a script. This can be used to avoid stack overflow exceptions that occur when recursively calling a method defined in the script. By default *-1* (disabled).

-   **maxScriptExecutionTime**: The maximum time a script is allowed to run. By default *-1* (disabled).

-   **maxMemoryUsed**: The maximum memory, in bytes, that the script is allowed to use. Note that the script engine itself takes a a certain amount of memory that is counted here too. By default *-1* (disabled).

-   **nrOfInstructionsBeforeStateCheckCallback**: The maximum script execution time and memory usage is implemented using a callback that is called every x instructions of the script. Note that these are not script instructions, but Java byte code instructions (which means one script line could be hundreds of byte code instructions). By default 100.

*Note:* the *maxMemoryUsed* setting can only be used by a JVM that supports the com.sun.management.ThreadMXBean\#getThreadAllocatedBytes() method. The Oracle JDK has this.

There is also a secure variant of the ScriptExecutionListener and ScriptTaskListener: *org.flowable.scripting.secure.listener.SecureJavascriptExecutionListener* and *org.flowable.scripting.secure.listener.SecureJavascriptTaskListener*.

It’s used as follows:

    <flowable:executionListener event="start" class="org.flowable.scripting.secure.listener.SecureJavascriptExecutionListener">
      <flowable:field name="script">
        <flowable:string>
            <![CDATA[
                execution.setVariable('test');
            ]]>
        </flowable:string>
      </flowable:field>
      <flowable:field name="language" stringValue="javascript" />
    </flowable:executionListener>

For examples that demonstrate unsecure scripts and how they are made secure by the *secure scripting* feature, please check the [unit tests on Github](https://github.com/Flowable/Flowable/tree/master/modules/flowable-secure-javascript/src/test/resources)

## Logging Sessions \[Experimental\]

Added in 6.5.0, Logging sessions allow you to collect information about process execution even if an exception causes the transaction to be rolled back.  This is enabled by providing a LoggingListener implementation to the engine configuration.  The loggingListener contains a single method called `loggingGenerated` that takes a list of Jackson ObjectNodes.  


In this simple implementation, each ObjectNode is sent to the logger:

```
class MyLoggingListener implements LoggingListener{
    static Logger logger = LoggerFactory.getLogger(MyLoggingListener.class);
    
    @Override
    public void loggingGenerated(List<ObjectNode> loggingNodes) {
        loggingNodes.forEach(jsonNodes -> logger.info(jsonNodes.toString()));
    }
}
```

During process engine configuration, an instance of the LoggingListener is passed 

```
ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
      .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
      .setJdbcUrl("jdbc:h2:mem:my-own-db;DB_CLOSE_DELAY=1000")
      .setLoggingListener(new MyLoggingListener())
      .buildProcessEngine();

```

### LoggingSession ObjectNodes
 
 The list of ObjectNodes passed to loggingGenerated method Are JSON objects, with at least the following attributes:
 - `message` - a human readable message
 - `scopeId` - a correlation ID to group all messages from the same transaction 
 - `scopeType` - the type of the scope
 
 Additional fields will also be present based on the type of event they describe:
 
```
2020-01-21 10:46:54.852  INFO 4985 --- [  restartedMain] c.e.f.MyLoggingListener                : {"message":"Variable 'initiator' created","scopeId":"a193efb3-3c6d-11ea-a01d-bed6c476b3ed","scopeType":"bpmn","variableName":"initiator","variableType":"null","variableRawValue":null,"variableValue":null,"scopeDefinitionId":"loggingSessionProcess:1:a18d38ef-3c6d-11ea-a01d-bed6c476b3ed","scopeDefinitionKey":"loggingSessionProcess","scopeDefinitionName":"Logging Session Process","__id":"a1948bf5-3c6d-11ea-a01d-bed6c476b3ed","__timeStamp":"2020-01-21T16:46:54.819Z","type":"variableCreate","__transactionId":"a1948bf5-3c6d-11ea-a01d-bed6c476b3ed","__logNumber":1}
``` 