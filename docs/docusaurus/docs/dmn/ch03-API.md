---
id: ch03-API
title: The Flowable DMN API
---

## The DMN Engine API and services

The DMN engine API is the most common way of interacting with Flowable DMN. The central starting point is the DmnEngine, which can be created in several ways as described in the [configuration section](dmn/ch02-Configuration.md#creating-a-dmn-engine). From the DmnEngine, you can obtain the various other services.
DmnEngine and the services objects are thread safe. So you can keep a reference to one of those for a whole server.

    DmnEngine dmnEngine = DmnEngines.getDefaultDmnEngine();
    DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();
    DmnRepositoryService dmnRepositoryService = dmnEngine.getDmnRepositoryService();
    DmnManagementService dmnManagementService = dmnEngine.getDmnManagementService();

DmnEngines.getDefaultDmnEngine() will initialize and build a DMN engine the first time it is called and afterwards always return the same DMN engine. Proper creation and closing of all DMN engines can be done with DMNEngines.init() and DMNEngines.destroy().

The DmnEngines class will scan for all flowable.dmn.cfg.xml and flowable-dmn-context.xml files. For all flowable.dmn.cfg.xml files, the DMN engine will be built in the typical Flowable way: DmnEngineConfiguration.createDmnEngineConfigurationFromInputStream(inputStream).buildDmnEngine(). For all flowable-dmn-context.xml files, the DMN engine will be built in the Spring way: First the Spring application context is created and then the DMN engine is obtained from that application context.

All services are stateless. This means that you can easily run Flowable DMN on multiple nodes in a cluster, each going to the same database, without having to worry about which machine actually executed previous calls. Any call to any service is idempotent, regardless of where it is executed.

The **DmnRepositoryService** is probably the first service needed when working with the Flowable DMN engine. This service offers operations for managing and manipulating deployments and DMN definitions. A DMN definition is the root concept of a DMN model (the main concepts of DMN are explained in the &lt;dmn-introduction, DMN introduction section&gt;). It contains the definition of the decision (and its decision table).
A deployment is the unit of packaging within the Flowable DMN engine. A deployment can contain multiple DMN XML files. Deploying a deployment means it is uploaded to the engine, where all DMN definitions are inspected and parsed before being stored in the database. From that point on, the deployment is known to the system and any decision included in the deployment can now be executed.

Furthermore, this service allows you to:

-   Query deployments, DMN definitions and decision tables known to the engine.

-   Retrieve a POJO version of the DMN definition or decision table that can be used to introspect using Java rather than XML.

The **DmnRuleService** provides methods for executing a decision. By providing parameters and input data, the evaluation of a decision can be started.

The **DmnManagementService** is typically not needed when coding custom application using Flowable DMN. It allows you to retrieve information about the engine version, database tables and table metadata.

For more detailed information on the service operations and the DMN engine API, see [the javadocs](http://www.flowable.org/docs/javadocs/index.html).

## Exception strategy

The base exception in Flowable is the org.flowable.engine.FlowableException, an unchecked exception. This exception can be thrown at all times by the API, but 'expected' exceptions that happen in specific methods are documented in [the javadocs](http://www.flowable.org/docs/javadocs/index.html). For example, an extract from DmnRuleService:

    /**
      * Execute a decision identified by it's key.
      *
      * @param  decisionKey      the decision key, cannot be null
      * @param  inputVariables   map with input variables
      * @return                  the {@link RuleEngineExecutionResult} for this execution
      * @throws FlowableObjectNotFoundException
      *            when the decision with given key does not exist.
      * @throws FlowableException
      *           when an error occurs while executing the decision.
      */
    RuleEngineExecutionResult executeDecisionByKey(String decisionKey, Map<String, Object> inputVariables);

In the example above, when a key is passed for which no decisions exist, an exception will be thrown. Also, as the javadoc **explicitly states that decisionKey cannot be null, a FlowableIllegalArgumentException will be thrown when null is passed**.

Even though we want to avoid a big exception hierarchy, the following subclasses are thrown in specific cases. All other errors that occur during process-execution or API-invocation that don’t fit into the possible exceptions below are thrown as regular FlowableExceptions.

-   FlowableOptimisticLockingException: Thrown when an optimistic lock occurs in the data store caused by concurrent access of the same data entry.

-   FlowableClassLoadingException: Thrown when a class requested to load was not found or when an error occurred while loading it.

-   FlowableObjectNotFoundException: Thrown when an object that is requested or acted on does not exist.

-   FlowableIllegalArgumentException: An exception indicating that an illegal argument has been supplied in a Flowable DMN API call, an illegal value was configured in the engine’s configuration or an illegal value has been supplied.

## Query API

There are two ways of querying data from the engine: the query API and native queries. The Query API allows to program completely typesafe queries with a fluent API. You can add various conditions to your queries (all of which are applied together as a logical AND) and precisely one ordering. The following code shows an example:

    List<DmnDeployment> dmnDeployments = dmnRepositoryService.createDeploymentQuery()
        .deploymentNameLike("deployment%")
        .orderByDeployTime()
        .list();

Sometimes you need more powerful queries, for example, queries using an OR operator or restrictions you cannot express using the Query API. For these cases, we introduced native queries, which allow you to write your own SQL queries. The return type is defined by the Query object you use and the data is mapped into the correct objects, such as Deployment, ProcessInstance, Execution, and so on. As the query will be fired at the database, you have to use table and column names as they are defined in the database; this requires some knowledge about the internal data structure and it’s recommended you use native queries with care. The table names can be retrieved through the API to keep the dependency as small as possible.

    long count = dmnRepositoryService.createNativeDeploymentQuery()
        .sql("SELECT count(*) FROM " + dmnManagementService.getTableName(DmnDeploymentEntity.class) + " D1, "
            + dmnManagementService.getTableName(DecisionTableEntity.class) + " D2 "
            + "WHERE D1.ID_ = D2.DEPLOYMENT_ID_ "
            + "AND D1.ID_ = #{deploymentId}")
        .parameter("deploymentId", deployment.getId())
        .count();

## Unit testing

As Flowable DMN is an embeddable Java engine, writing unit tests for DMN definitions is as simple as writing regular unit tests.

Flowable supports the JUnit version 4 and 5 style of unit testing.

In the JUnit 5 style one needs to use the org.flowable.dmn.engine.test.FlowableDmnTest annotation
or register the org.flowable.dmn.engine.test.FlowableDmnExtension manually.
The FlowableDmnTest annotation is just a meta annotation and the does the registration of the FlowableDmnExtension
(i.e. it does @ExtendWith(FlowableDmnExtension.class)).
This will make the DmnEngine and the services available as parameters into the test and lifecycle methods
(@BeforeAll, @BeforeEach, @AfterEach, @AfterAll).
Before each test the dmnEngine will be initialized by default with the flowable.dmn.cfg.xml resource on the classpath.
In order to specify a different configuration file the org.flowable.dmn.engine.test.DmnConfigurationResource
annotation needs to be used (see second example).
Dmn engines are cached statically over multiple unit tests when the configuration resource is the same.

By using FlowableDmnExtension, you can annotate test methods with org.flowable.dmn.engine.test.DmnDeployment
or org.flowable.dmn.engine.test.DmnDeploymentAnnotation.
If both @DmnDeployment and @DmnDeploymentAnnotation are used then the @DmnDeployment
takes precedence and @DmnDeploymentAnnotation will be ignored.
When a test method is annotated with @DmnDeployment,
before each test the dmn files defined in DmnDeployment\#resources will be deployed.
In case there are no resources defined, a resource file of the form testClassName.testMethod.dmn
in the same package as the test class, will be deployed.
At the end of the test, the deployment will be deleted, including all related dmn definitions, executions, and so on.
See the DmnDeployment class for more information.

Taking all that in account, a JUnit 5 test looks as follows:

**JUnit 5 test with default resource.**

    @FlowableDmnTest
    class MyDecisionTableTest {

      @Test
      @DmnDeploymentAnnotation
      void simpleDmnTest(DmnEngine dmnEngine) {
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> executionResult = ruleService.createExecuteDecisionBuilder()
                .decisionKey("extensionUsage")
                .variable("inputVariable1", 2)
                .variable("inputVariable2", "test2")
                .executeWithSingleResult();

        Assertions.assertThat(executionResult).containsEntry("output1", "test1");
      }
    }

    With JUnit 5 you can also inject the id of the deployment (with +org.flowable.dmn.engine.test.DmnDeploymentId+_) into your test and lifecycle methods.

**JUnit 5 test with custom resource.**

    @FlowableDmnTest
    @DmnConfigurationResource("flowable.custom.dmn.cfg.xml")
    class MyDecisionTableTest {

      @Test
      @DmnDeploymentAnnotation
      void simpleDmnTest(DmnEngine dmnEngine) {
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> executionResult = ruleService.createExecuteDecisionBuilder()
                .decisionKey("extensionUsage")
                .variable("inputVariable1", 2)
                .variable("inputVariable2", "test2")
                .executeWithSingleResult();

        Assertions.assertThat(executionResult).containsEntry("output1", "test1");
      }
    }

When writing JUnit 4 unit tests, the org.flowable.dmn.engine.test.FlowableDmnRule Rule can be used. Through this rule, the DMN engine and services are available through getters. Including this Rule will enable the use of the org.flowable.dmn.engine.test.DmnDeploymentAnnotation annotation (see above for an explanation of its use and configuration) and it will look for the default configuration file on the classpath. DMN engines are statically cached over multiple unit tests when using the same configuration resource.
It’s also possible to provide a custom engine configuration to the rule.

The following code snippet shows an example of using the JUnit 4 style of testing and the usage of the FlowableDmnRule (and passing an optional custom configuration):

**JUnit 4 test.**

    public class MyDecisionTableTest {

      @Rule
      public FlowableDmnRule flowableDmnRule = new FlowableDmnRule("custom1.flowable.dmn.cfg.xml");

      @Test
      @DmnDeploymentAnnotation
      public void ruleUsageExample() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> executionResult = ruleService.createExecuteDecisionBuilder()
                .decisionKey("extensionUsage")
                .variable("inputVariable1", 2)
                .variable("inputVariable2", "test2")
                .executeWithSingleResult();

        Assertions.assertThat(executionResult).containsEntry("output1", "test1");
      }
    }

## The DMN engine in a web application

The DmnEngine is a thread-safe class and can easily be shared among multiple threads. In a web application, this means it is possible to create the DMN engine once when the container boots and shut down the engine when the container goes down.

The following code snippet shows how you can write a simple ServletContextListener to initialize and destroy process engines in a plain Servlet environment:

    public class DmnEnginesServletContextListener implements ServletContextListener {

      public void contextInitialized(ServletContextEvent servletContextEvent) {
        DmnEngines.init();
      }

      public void contextDestroyed(ServletContextEvent servletContextEvent) {
        DmnEngines.destroy();
      }

    }

The contextInitialized method will delegate to DmnEngines.init(). This will look for flowable.dmn.cfg.xml resource files on the classpath, and create a DmnEngine for the given configurations (for example, multiple JARs with a configuration file). If you have multiple such resource files on the classpath, make sure they all have different names. When the DMN engine is needed, it can be fetched using:

    DmnEngines.getDefaultDmnEngine()

or:

    DmnEngines.getDmnEngine("myName");

Of course, it’s also possible to use any of the variants of creating a DMN engine,
as described in the [configuration section](dmn/ch02-Configuration.md#creating-a-dmn-engine).

The contextDestroyed method of the context-listener delegates to DmnEngines.destroy(). That will properly close all initialized DMN engines.
