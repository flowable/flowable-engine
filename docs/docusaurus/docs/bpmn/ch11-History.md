---
id: ch11-History
title: History
---

History is the component that captures what happened during process execution and stores it permanently. In contrast to the runtime data, the history data will remain present in the DB also after process instances have completed.

There are 6 history entities:

-   HistoricProcessInstances containing information about current and past process instances.

-   HistoricVariableInstances containing the latest value of a process variable or task variable.

-   HistoricActivityInstances containing information about a single execution of an activity (node in the process).

-   HistoricTaskInstances containing information about current and past (completed and deleted) task instances.

-   HistoricIdentityLinks containing information about current and past identity links on tasks and process instances.

-   HistoricDetails containing various kinds of information related to either a historic process instances, an activity instance or a task instance.

Since the DB contains historic entities for past as well as ongoing instances, you might want to consider querying these tables in order to minimize access to the runtime process instance data and that way keeping the runtime execution performant.

## Querying history

In the API, it’s possible to query all 6 of the History entities. The HistoryService exposes the methods createHistoricProcessInstanceQuery(), createHistoricVariableInstanceQuery(), createHistoricActivityInstanceQuery(), getHistoricIdentityLinksForTask(), getHistoricIdentityLinksForProcessInstance(), createHistoricDetailQuery() and createHistoricTaskInstanceQuery().

Below are a couple of examples that show some of the possibilities of the query API for history. Full description of the possibilities can be found in the [javadocs](http://www.flowable.org/docs/javadocs/index.html), in the org.flowable.engine.history package.

### HistoricProcessInstanceQuery

Get 10 HistoricProcessInstances that are finished and which took the most time to complete (the longest duration) of all finished processes with definition 'XXX'.

    historyService.createHistoricProcessInstanceQuery()
      .finished()
      .processDefinitionId("XXX")
      .orderByProcessInstanceDuration().desc()
      .listPage(0, 10);

### HistoricVariableInstanceQuery

Get all HistoricVariableInstances from a finished process instance with id 'xxx' ordered by variable name.

    historyService.createHistoricVariableInstanceQuery()
      .processInstanceId("XXX")
      .orderByVariableName.desc()
      .list();

### HistoricActivityInstanceQuery

Get the last HistoricActivityInstance of type 'serviceTask' that has been finished in any process that uses the processDefinition with id XXX.

    historyService.createHistoricActivityInstanceQuery()
      .activityType("serviceTask")
      .processDefinitionId("XXX")
      .finished()
      .orderByHistoricActivityInstanceEndTime().desc()
      .listPage(0, 1);

### HistoricDetailQuery

The next example, gets all variable-updates that have been done in process with id 123. Only HistoricVariableUpdates will be returned by this query. Note that it’s possible that a certain variable name has multiple HistoricVariableUpdate entries, for each time the variable was updated in the process. You can use orderByTime (the time the variable update was done) or orderByVariableRevision (revision of runtime variable at the time of updating) to find out in what order they occurred.

    historyService.createHistoricDetailQuery()
      .variableUpdates()
      .processInstanceId("123")
      .orderByVariableName().asc()
      .list()

This example gets all [form-properties](bpmn/ch08-Forms.md#form-properties) that were submitted in any task or when starting the process with id "123". Only HistoricFormProperties will be returned by this query.

    historyService.createHistoricDetailQuery()
      .formProperties()
      .processInstanceId("123")
      .orderByVariableName().asc()
      .list()

The last example gets all variable updates that were performed on the task with id "123". This returns all HistoricVariableUpdates for variables that were set on the task (task local variables), and NOT on the process instance.

    historyService.createHistoricDetailQuery()
      .variableUpdates()
      .taskId("123")
      .orderByVariableName().asc()
      .list()

Task local variables can be set using the TaskService or on a DelegateTask, inside TaskListener:

    taskService.setVariableLocal("123", "myVariable", "Variable value");

    public void notify(DelegateTask delegateTask) {
      delegateTask.setVariableLocal("myVariable", "Variable value");
    }

### HistoricTaskInstanceQuery

Get 10 HistoricTaskInstances that are finished and which took the most time to complete (the longest duration) of all tasks.

    historyService.createHistoricTaskInstanceQuery()
      .finished()
      .orderByHistoricTaskInstanceDuration().desc()
      .listPage(0, 10);

Get HistoricTaskInstances that are deleted with a delete reason that contains "invalid", which were last assigned to user 'kermit'.

    historyService.createHistoricTaskInstanceQuery()
      .finished()
      .taskDeleteReasonLike("%invalid%")
      .taskAssignee("kermit")
      .listPage(0, 10);

## History configuration

The history level can be configured programmatically, using the enum org.flowable.engine.impl.history.HistoryLevel (or *HISTORY* constants defined on ProcessEngineConfiguration for versions prior to 5.11):

    ProcessEngine processEngine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResourceDefault()
      .setHistory(HistoryLevel.AUDIT.getKey())
      .buildProcessEngine();

The level can also be configured in flowable.cfg.xml or in a spring-context:

    <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration">
      <property name="history" value="audit" />
      ...
    </bean>

Following history levels can be configured:

-   none: skips all history archiving. This is the most performant for runtime process execution, but no historical information will be available.

-   activity: archives all process instances and activity instances. At the end of the process instance, the latest values of the top level process instance variables will be copied to historic variable instances. No details will be archived.

-   audit: This is the default. It archives all process instances, activity instances, keeps variable values continuously in sync and all form properties that are submitted so that all user interaction through forms is traceable and can be audited.

-   full: This is the highest level of history archiving and hence the slowest. This level stores all information as in the audit level plus all other possible details, mostly this are process variable updates.

**In older releases, the history level was stored in the database (table ACT\_GE\_PROPERTY, property with name historyLevel). Starting from 5.11, this value is not used anymore and is ignored/deleted from the database. The history can now be changed between 2 boots of the engine, without an exception being thrown in case the level changed from the previous engine-boot.**

## Async History configuration

Async History has been introduced with Flowable 6.1.0 and allows historic data to be persisted asynchronously using a history job executor.

    <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration">
      <property name="asyncHistoryEnabled" value="true" />
      <property name="asyncHistoryExecutorNumberOfRetries" value="10" />
      <property name="asyncHistoryExecutorActivate" value="true" />
      ...
    </bean>

With the asyncHistoryExecutorActivate property, the history job executor can be started automatically when booting the Process Engine. This would be only set to false for test cases (or if Async History is not enabled of course).
The asyncHistoryExecutorNumberOfRetries property configures the number of retries for an Async History job. This property is a bit different than that for a normal async job, because a history job may need more cycles before it can be handled successfully. For example, a historic task first has to be created in the ACT\_HI\_TASK\_ table before the assignee can be updated by another history job. The default value for this property is set to 10 in the Process Engine configuration. When the number of retries has been reached, the history job will be ignored (and not written to a deadletter job table).

In addition to these properties, the asyncHistoryExecutor property can be used to configure an AsyncExecutor in a similar way that you can do for the normal async job executor.

When the history data is not to be persisted in the default history tables, but for example, is required in a NoSQL database (such as Elasticsearch, MongoDb, Cassandra and so on), or something completely different is to be done with it, the handler that is responsible for handling the job can be overridden:

-   Using the historyJobHandlers property, which is a map of all the custom history job handlers

-   Or, configure the customHistoryJobHandlers list with all instances will be added to the historyJobHandlers map at boot time.

Alternatively, it is also possible to use a Message Queue and configure the engine in such a way that a message will be sent when a new history job is available. This way, the historical data can be processed on different servers to where the engines are run. It’s also possible to configure the engine and Message Queue using JTA (when using JMS) and not store the historical data in a job, but send it all data to a Message Queue that participates in a global transaction.

See [the Flowable Async History Examples](https://github.com/flowable/flowable-examples/tree/master/async-history) for various examples on how to configure the Async History, including the default way, using a JMS queue, using JTA or using a Message Queue and a Spring Boot application that acts as a message listener.

## History for audit purposes

When [configuring](bpmn/ch11-History.md#history-configuration) at least audit level for configuration. Then all properties submitted through methods FormService.submitStartFormData(String processDefinitionId, Map&lt;String, String&gt; properties) and FormService.submitTaskFormData(String taskId, Map&lt;String, String&gt; properties) are recorded.

Form properties can be retrieved with the query API like this:

    historyService
          .createHistoricDetailQuery()
          .formProperties()
          ...
          .list();

In that case only historic details of type HistoricFormProperty are returned.

If you’ve set the authenticated user before calling the submit methods with IdentityService.setAuthenticatedUserId(String) then that authenticated user who submitted the form will be accessible in the history as well with HistoricProcessInstance.getStartUserId() for start forms and HistoricActivityInstance.getAssignee() for task forms.

## History Cleaning

By default history data is stored forever, this can cause the history tables to grow very large and impact the performance of the HistoryService.  History Cleaning has been introduced with 6.5.0 and allows the deletion of HistoricProcessInstances and their associated data.  Once process data no longer needs to be retained it can be deleted to reduce the history database's size. 

### Automatic History Cleaning Configuration

Automatic cleanup of HistoricProcessInstances is disabled by default but can be enabled and configured programmatically.  Once enabled the default is to run a cleanup job at 1 AM to delete all HistoricProcessInstances and associated data that have ended 365 days prior or older. 

    ProcessEngine processEngine = ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResourceDefault()
        .setEnableHistoryCleaning(true)
        .setHistoryCleaningTimeCycleConfig("0 0 1 * * ?")
        .setCleanInstancesEndedAfterNumberOfDays(365)
        .buildProcessEngine();
 
Spring properties set in an application.properties or externalized configuration are also available:
 
         flowable.enable-history-cleaning=true
         flowable.history-cleaning-after-days=365
         flowable.history-cleaning-cycle=0 0 1 * * ?
 
Additionally, History Cleanup can also be configured in flowable.cfg.xml or in a spring-context:
 
     <bean id="processEngineConfiguration" class="org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration">
       <property name="enableHistoryCleaning" value="true"/>
       <property name="historyCleaningTimeCycleConfig" value="0 0 1 * * ?"/>
       <property name="cleanInstancesEndedAfterNumberOfDays" value="365"/>
       ...
     </bean>

### Manually Deleting History

Manually cleaning history can accomplished by executing methods on the HistoryService query builders.

Delete all HistoricProcessInstances and their related data that are older than one year.

     Calendar cal = new GregorianCalendar();
     cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
     historyService.createHistoricProcessInstanceQuery()
       .finishedBefore(cal.getTime())
       .deleteWithRelatedData();

Delete just HistoricProcessInstances older than one year.

    Calendar cal = new GregorianCalendar();
    cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
    historyService.createHistoricProcessInstanceQuery()
      .finishedBefore(cal.getTime())
      .delete();
      
Delete just HistoricActivityInstances older than one year.

    Calendar cal = new GregorianCalendar();
    cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
    historyService.createHistoricActivityInstanceQuery()
      .finishedBefore(cal.getTime())
      .delete();
          
Delete the task and activity data for deleted HistoricProcessInstances.
    
    historyService.deleteTaskAndActivityDataOfRemovedHistoricProcessInstances();
