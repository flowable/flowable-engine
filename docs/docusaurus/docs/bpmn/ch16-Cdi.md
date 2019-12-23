---
id: ch16-Cdi
title: CDI integration
---

The flowable-cdi modules leverages both the configurability of Flowable and the extensibility of cdi. The most prominent features of flowable-cdi are:

-   Support for @BusinessProcessScoped beans (CDI beans whose lifecycle is bound to a process instance),

-   A custom El-Resolver for resolving CDI beans (including EJBs) from the process,

-   Declarative control over a process instance using annotations,

-   Flowable is hooked-up to the CDI event bus,

-   Works with both Java EE and Java SE, works with Spring,

-   Support for unit testing.

<!-- -->

    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-cdi</artifactId>
        <version>6.x</version>
    </dependency>

## Setting up flowable-cdi

Flowable-cdi can be set up in different environments. In this section we briefly walk through the configuration options.

### Looking up a Process Engine

The CDI extension needs to get access to a ProcessEngine. To achieve this, an implementation of the interface org.flowable.cdi.spi.ProcessEngineLookup is looked up at runtime. The CDI module ships with a default implementation named org.flowable.cdi.impl.LocalProcessEngineLookup, which uses the ProcessEngines-Utility class for looking up the ProcessEngine. In the default configuration ProcessEngines\#NAME\_DEFAULT is used to look up the ProcessEngine. This class might be subclassed to set a custom name. NOTE: You need a flowable.cfg.xml configuration on the classpath.

Flowable-cdi uses a java.util.ServiceLoader SPI for resolving an instance of org.flowable.cdi.spi.ProcessEngineLookup. In order to provide a custom implementation of the interface, we need to add a plain text file named META-INF/services/org.flowable.cdi.spi.ProcessEngineLookup to our deployment, in which we specify the fully qualified class name of the implementation.

> **Note**
>
> If you do not provide a custom org.flowable.cdi.spi.ProcessEngineLookup implementation, Flowable will use the default LocalProcessEngineLookup implementation. In that case, all you need to do is providing a flowable.cfg.xml file on the classpath (see next section).

### Configuring the Process Engine

Configuration depends on the selected ProcessEngineLookup-Strategy (cf. previous section). Here, we focus on the configuration options available in combination with the LocalProcessEngineLookup, which requires us to provide a Spring flowable.cfg.xml file on the classpath.

Flowable offers different ProcessEngineConfiguration implementations mostly dependent on the underlying transaction management strategy. The flowable-cdi module is not concerned with transactions, which means that potentially any transaction management strategy can be used (even the Spring transaction abstraction). As a convenience, the CDI module provides two custom ProcessEngineConfiguration implementations:

-   org.flowable.cdi.CdiJtaProcessEngineConfiguration: a subclass of the Flowable JtaProcessEngineConfiguration can be used if JTA-managed transactions should be used for Flowable

-   org.flowable.cdi.CdiStandaloneProcessEngineConfiguration: a subclass of the Flowable StandaloneProcessEngineConfiguration can be used if plain JDBC transactions should be used for Flowable. The following is an example flowable.cfg.xml file for JBoss 7:

<!-- -->

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

        <!-- lookup the JTA-Transaction manager -->
        <bean id="transactionManager" class="org.springframework.jndi.JndiObjectFactoryBean">
            <property name="jndiName" value="java:jboss/TransactionManager"></property>
            <property name="resourceRef" value="true" />
        </bean>

        <!-- process engine configuration -->
        <bean id="processEngineConfiguration"
            class="org.flowable.cdi.CdiJtaProcessEngineConfiguration">
            <!-- lookup the default Jboss datasource -->
            <property name="dataSourceJndiName" value="java:jboss/datasources/ExampleDS" />
            <property name="databaseType" value="h2" />
            <property name="transactionManager" ref="transactionManager" />
            <!-- using externally managed transactions -->
            <property name="transactionsExternallyManaged" value="true" />
            <property name="databaseSchemaUpdate" value="true" />
        </bean>
    </beans>

And this is how it would look like for Glassfish 3.1.1 (assuming a datasource named jdbc/flowable is properly configured):

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

        <!-- lookup the JTA-Transaction manager -->
        <bean id="transactionManager" class="org.springframework.jndi.JndiObjectFactoryBean">
            <property name="jndiName" value="java:appserver/TransactionManager"></property>
            <property name="resourceRef" value="true" />
        </bean>

        <!-- process engine configuration -->
        <bean id="processEngineConfiguration"
            class="org.flowable.cdi.CdiJtaProcessEngineConfiguration">
            <property name="dataSourceJndiName" value="jdbc/flowable" />
            <property name="transactionManager" ref="transactionManager" />
            <!-- using externally managed transactions -->
            <property name="transactionsExternallyManaged" value="true" />
            <property name="databaseSchemaUpdate" value="true" />
        </bean>
    </beans>

Note that the above configuration requires the "spring-context" module:

    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>4.2.5.RELEASE</version>
    </dependency>

The configuration in a Java SE environment looks exactly like the examples provided in section [Creating a ProcessEngine](bpmn/ch03-Configuration.md#creating-a-processengine), substitute CdiStandaloneProcessEngineConfiguration for StandaloneProcessEngineConfiguration.

### Deploying Processes

Processes can be deployed using the standard Flowable API (RepositoryService). In addition, flowable-cdi offers the possibility to auto-deploy processes listed in a file named processes.xml located top-level in the classpath. This is an example processes.xml file:

    <?xml version="1.0" encoding="utf-8" ?>
    <!-- list the processes to be deployed -->
    <processes>
        <process resource="diagrams/myProcess.bpmn20.xml" />
        <process resource="diagrams/myOtherProcess.bpmn20.xml" />
    </processes>

## Contextual Process Execution with CDI

In this section we briefly look at the contextual process execution model used by the Flowable CDI extension. A BPMN business process is typically a long-running interaction, comprised of both user and system tasks. At runtime, a process is split-up into a set of individual units of work, performed by users and/or application logic. In flowable-cdi, a process instance can be associated with a CDI scope, the association representing a unit of work. This is particularly useful, if a unit of work is complex, for instance if the implementation of a user task is a complex sequence of different forms and "non-process-scoped" state needs to be kept during this interaction.

In the default configuration, process instances are associated with the "broadest" active scope, starting with the conversation and falling back to the request if the conversation context is not active.

### Associating a Conversation with a Process Instance

When resolving @BusinessProcessScoped beans, or injecting process variables, we rely on an existing association between an active CDI scope and a process instance. flowable-cdi provides the org.flowable.cdi.BusinessProcess bean for controlling the association, most prominently:

-   The *startProcessBy(…​)* methods, mirroring the respective methods exposed by the Flowable RuntimeService allowing to start and subsequently associating a business process.

-   resumeProcessById(String processInstanceId), allowing to associate the process instance with the provided id.

-   resumeTaskById(String taskId), allowing to associate the task with the provided id (and by extension, the corresponding process instance).

Once a unit of work (for example a user task) is completed, the completeTask() method can be called to disassociate the conversation/request from the process instance. This signals the engine that the current task is completed and makes the process instance proceed.

Note that the BusinessProcess bean is a @Named bean, which means that the exposed methods can be invoked using expression language, for example from a JSF page. The following JSF2 snippet begins a new conversation and associates it with a user task instance, the id of which is passed as a request parameter (e.g. pageName.jsf?taskId=XX):

    <f:metadata>
        <f:viewParam name="taskId" />
        <f:event type="preRenderView" listener="#{businessProcess.startTask(taskId, true)}" />
    </f:metadata>

### Declaratively controlling the Process

Flowable-cdi allows declaratively starting process instances and completing tasks using annotations. The @org.flowable.cdi.annotation.StartProcess annotation allows to start a process instance either by "key" or by "name". Note that the process instance is started *after* the annotated method returns. Example:

    @StartProcess("authorizeBusinessTripRequest")
    public String submitRequest(BusinessTripRequest request) {
        // do some work
        return "success";
    }

Depending on the configuration of Flowable, the code of the annotated method and the starting of the process instance will be combined in the same transaction. The @org.flowable.cdi.annotation.CompleteTask-annotation works in the same way:

    @CompleteTask(endConversation=false)
    public String authorizeBusinessTrip() {
        // do some work
        return "success";
    }

The @CompleteTask annotation offers the possibility to end the current conversation. The default behavior is to end the conversation after the call to Flowable returns. Ending the conversation can be disabled, as shown in the example above.

### Referencing Beans from the Process

Flowable-cdi exposes CDI beans to Flowable El, using a custom resolver. This makes it possible to reference beans from the process:

    <userTask id="authorizeBusinessTrip" name="Authorize Business Trip"
                flowable:assignee="#{authorizingManager.account.username}" />

Where authorizingManager could be a bean provided by a producer method:

    @Inject    @ProcessVariable Object businessTripRequesterUsername;

    @Produces
    @Named
    public Employee authorizingManager() {
        TypedQuery<Employee> query = entityManager.createQuery("SELECT e FROM Employee e WHERE e.account.username='"
            + businessTripRequesterUsername + "'", Employee.class);
        Employee employee = query.getSingleResult();
        return employee.getManager();
    }

We can use the same feature to call a business method of an EJB in a service task, using the flowable:expression="myEjb.method()"-extension. Note that this requires a @Named-annotation on the MyEjb-class.

### Working with @BusinessProcessScoped beans

Using flowable-cdi, the lifecycle of a bean can be bound to a process instance. To this extent, a custom context implementation is provided, namely the BusinessProcessContext. Instances of BusinessProcessScoped beans are stored as process variables in the current process instance. BusinessProcessScoped beans need to be PassivationCapable (for example Serializable). The following is an example of a process scoped bean:

    @Named
    @BusinessProcessScoped
    public class BusinessTripRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        private String startDate;
        private String endDate;
        // ...
    }

Sometimes, we want to work with process scoped beans, in the absence of an association with a process instance, for example before starting a process. If no process instance is currently active, instances of BusinessProcessScoped beans are temporarily stored in a local scope, i.e. the Conversation or the Request, depending on the context. If this scope is later associated with a business process instance, the bean instances are flushed to the process instance.

### Injecting Process Variables

Process variables are available for injection. flowable-cdi supports

-   type-safe injection of @BusinessProcessScoped beans using @Inject \\\[additional qualifiers\\\] Type fieldName

-   unsafe injection of other process variables using the @ProcessVariable(name?) qualifier:

<!-- -->

    @Inject @ProcessVariable Object accountNumber;
    @Inject @ProcessVariable("accountNumber") Object account

In order to reference process variables using EL, there are similar options:

-   @Named @BusinessProcessScoped beans can be referenced directly,

-   other process variables can be referenced using the ProcessVariables-bean:

<!-- -->

    #{processVariables['accountNumber']}

### Receiving Process Events

Flowable can be hooked-up to the CDI event bus. This allows us to be notified of process events using standard CDI event mechanisms. In order to enable CDI event support for Flowable, enable the corresponding parse listener in the configuration:

    <property name="postBpmnParseHandlers">
        <list>
            <bean class="org.flowable.cdi.impl.event.CdiEventSupportBpmnParseHandler" />
        </list>
    </property>

Now Flowable is configured for publishing events using the CDI event bus. The following gives an overview of how process events can be received in CDI beans. In CDI, we can declaratively specify event observers using the @Observes-annotation. Event notification is type-safe. The type of
process events is org.flowable.cdi.BusinessProcessEvent.
The following is an example of a simple event observer method:

    public void onProcessEvent(@Observes BusinessProcessEvent businessProcessEvent) {
        // handle event
    }

This observer would be notified of all events. If we want to restrict the set of events the observer receives, we can add qualifier annotations:

-   @BusinessProcess: restricts the set of events to a certain process definition. Example: @Observes @BusinessProcess("billingProcess") BusinessProcessEvent evt

-   @StartActivity: restricts the set of events by a certain activity. For example: @Observes @StartActivity("shipGoods") BusinessProcessEvent evt is invoke whenever an activity with the id "shipGoods" is entered.

-   @EndActivity: restricts the set of events by a certain activity. For example: @Observes @EndActivity("shipGoods") BusinessProcessEvent evt is invoke whenever an activity with the id "shipGoods" is left.

-   @TakeTransition: restricts the set of events by a certain transition.

-   @CreateTask: restricts the set of events by a certain task’s creation.

-   @DeleteTask: restricts the set of events by a certain task’s deletion.

-   @AssignTask: restricts the set of events by a certain task’s assignment.

-   @CompleteTask: restricts the set of events by a certain task’s completion.

The qualifiers named above can be combined freely. For example, in order to receive all events generated when leaving the "shipGoods" activity in the "shipmentProcess", we could write the following observer method:

    public void beforeShippingGoods(@Observes @BusinessProcess("shippingProcess") @EndActivity("shipGoods") BusinessProcessEvent evt) {
        // handle event
    }

In the default configuration, event listeners are invoked synchronously and in the context of the same transaction. CDI transactional observers (only available in combination with JavaEE/EJB), allow to control when the event is handed to the observer method. Using transactional observers, we can for example assure that an observer is only notified if the transaction in which the event is fired succeeds:

    public void onShipmentSucceeded(@Observes(during=TransactionPhase.AFTER_SUCCESS) @BusinessProcess("shippingProcess") @EndActivity("shipGoods") BusinessProcessEvent evt) {
        // send email to customer.
    }

### Additional Features

-   The ProcessEngine as well as the services are available for injection: @Inject ProcessEngine, RepositoryService, TaskService, …​

-   The current process instance and task can be injected: @Inject ProcessInstance, Task,

-   The current business key can be injected: @Inject @BusinessKey String businessKey,

-   The current process instance id be injected: @Inject @ProcessInstanceId String pid,

## Known Limitations

Although flowable-cdi is implemented against the SPI and designed to be a "portable-extension" it is only tested using Weld.
