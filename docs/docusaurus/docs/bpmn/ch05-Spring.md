---
id: ch05-Spring
title: Spring integration
---

While you can definitely use Flowable without Spring, we’ve provided some very nice integration features that are explained in this chapter.

## ProcessEngineFactoryBean

The ProcessEngine can be configured as a regular Spring bean. The starting point of the integration is the class org.flowable.spring.ProcessEngineFactoryBean. That bean takes a process engine configuration and creates the process engine. This means that the creation and configuration of properties for Spring is the same as documented in the [configuration section](bpmn/ch03-Configuration.md#creating-a-processengine). For Spring integration, the configuration and engine beans will look like this:

    <bean id="processEngineConfiguration" class="org.flowable.spring.SpringProcessEngineConfiguration">
        ...
    </bean>

    <bean id="processEngine" class="org.flowable.spring.ProcessEngineFactoryBean">
      <property name="processEngineConfiguration" ref="processEngineConfiguration" />
    </bean>

Note that the processEngineConfiguration bean now uses the org.flowable.spring.SpringProcessEngineConfiguration class.

## Transactions

We’ll explain the SpringTransactionIntegrationTest found in the Spring examples of the distribution step by step. Below is the Spring configuration file that we use in this example (you can find it in SpringTransactionIntegrationTest-context.xml). The section shown below contains the dataSource, transactionManager, processEngine and the Flowable engine services.

When passing the DataSource to the SpringProcessEngineConfiguration (using property "dataSource"), Flowable uses a org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy internally, which wraps the passed DataSource. This is done to make sure the SQL connections retrieved from the DataSource and the Spring transactions play well together. This implies that it’s no longer necessary to proxy the dataSource yourself in Spring configuration, although it’s still possible to pass a TransactionAwareDataSourceProxy into the SpringProcessEngineConfiguration. In this case, no additional wrapping will occur.

**Make sure when declaring a TransactionAwareDataSourceProxy in Spring configuration yourself that you don’t use it for resources that are already aware of Spring transactions (e.g. DataSourceTransactionManager and JPATransactionManager need the un-proxied dataSource).**

    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:context="http://www.springframework.org/schema/context"
           xmlns:tx="http://www.springframework.org/schema/tx"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
                                 http://www.springframework.org/schema/beans/spring-beans.xsd
                               http://www.springframework.org/schema/context
                                 http://www.springframework.org/schema/context/spring-context-2.5.xsd
                               http://www.springframework.org/schema/tx
                                 http://www.springframework.org/schema/tx/spring-tx-3.0.xsd">

      <bean id="dataSource" class="org.springframework.jdbc.datasource.SimpleDriverDataSource">
        <property name="driverClass" value="org.h2.Driver" />
        <property name="url" value="jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000" />
        <property name="username" value="sa" />
        <property name="password" value="" />
      </bean>

      <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource" />
      </bean>

      <bean id="processEngineConfiguration" class="org.flowable.spring.SpringProcessEngineConfiguration">
        <property name="dataSource" ref="dataSource" />
        <property name="transactionManager" ref="transactionManager" />
        <property name="databaseSchemaUpdate" value="true" />
        <property name="asyncExecutorActivate" value="false" />
      </bean>

      <bean id="processEngine" class="org.flowable.spring.ProcessEngineFactoryBean">
        <property name="processEngineConfiguration" ref="processEngineConfiguration" />
      </bean>

      <bean id="repositoryService" factory-bean="processEngine" factory-method="getRepositoryService" />
      <bean id="runtimeService" factory-bean="processEngine" factory-method="getRuntimeService" />
      <bean id="taskService" factory-bean="processEngine" factory-method="getTaskService" />
      <bean id="historyService" factory-bean="processEngine" factory-method="getHistoryService" />
      <bean id="managementService" factory-bean="processEngine" factory-method="getManagementService" />

    ...

The remainder of that Spring configuration file contains the beans and configuration that we’ll use in this particular example:

    <beans>
      ...
      <tx:annotation-driven transaction-manager="transactionManager"/>

      <bean id="userBean" class="org.flowable.spring.test.UserBean">
        <property name="runtimeService" ref="runtimeService" />
      </bean>

      <bean id="printer" class="org.flowable.spring.test.Printer" />

    </beans>

First, the application context is created using any of the ways supported by Spring. In this example, you could use a classpath XML resource to configure our Spring application context:

    ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
        "org/flowable/examples/spring/SpringTransactionIntegrationTest-context.xml");

or, as it’s a test:

    @ContextConfiguration(
      "classpath:org/flowable/spring/test/transaction/SpringTransactionIntegrationTest-context.xml")

Then we can get the service beans and invoke methods on them. The ProcessEngineFactoryBean will have added an extra interceptor to the services that applies Propagation.REQUIRED transaction semantics on the Flowable service methods. So, for example, we can use the repositoryService to deploy a process like this:

    RepositoryService repositoryService =
      (RepositoryService) applicationContext.getBean("repositoryService");
    String deploymentId = repositoryService
      .createDeployment()
      .addClasspathResource("org/flowable/spring/test/hello.bpmn20.xml")
      .deploy()
      .getId();

The other way around also works. In this case, the Spring transaction will be around the userBean.hello() method and the Flowable service method invocation will join that same transaction.

    UserBean userBean = (UserBean) applicationContext.getBean("userBean");
    userBean.hello();

The UserBean looks like this. Remember, from above in the Spring bean configuration, we injected the repositoryService into the userBean.

    public class UserBean {

      /** injected by Spring */
      private RuntimeService runtimeService;

      @Transactional
      public void hello() {
        // here you can do transactional stuff in your domain model
        // and it will be combined in the same transaction as
        // the startProcessInstanceByKey to the Flowable RuntimeService
        runtimeService.startProcessInstanceByKey("helloProcess");
      }

      public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
      }
    }

## Expressions

When using the ProcessEngineFactoryBean, all [expressions](bpmn/ch04-API.md#expressions) in the BPMN processes will also 'see' all the Spring beans, by default. It’s possible to limit the beans (even none) you want to expose in expressions using a map that you can configure. The example below exposes a single bean (printer), available to use under the key "printer". **To have NO beans exposed at all, just pass an empty list as 'beans' property on the SpringProcessEngineConfiguration. When no 'beans' property is set, all Spring beans in the context will be available.**

    <bean id="processEngineConfiguration" class="org.flowable.spring.SpringProcessEngineConfiguration">
      ...
      <property name="beans">
        <map>
          <entry key="printer" value-ref="printer" />
        </map>
      </property>
    </bean>

    <bean id="printer" class="org.flowable.examples.spring.Printer" />

Now the exposed beans can be used in expressions: for example, the SpringTransactionIntegrationTest hello.bpmn20.xml shows how a method on a Spring bean can be invoked using a UEL method expression:

    <definitions id="definitions">

      <process id="helloProcess">

        <startEvent id="start" />
        <sequenceFlow id="flow1" sourceRef="start" targetRef="print" />

        <serviceTask id="print" flowable:expression="#{printer.printMessage()}" />
        <sequenceFlow id="flow2" sourceRef="print" targetRef="end" />

        <endEvent id="end" />

      </process>

    </definitions>

Where Printer looks like this:

    public class Printer {

      public void printMessage() {
        System.out.println("hello world");
      }
    }

And the Spring bean configuration (also shown above) looks like this:

    <beans>
      ...

      <bean id="printer" class="org.flowable.examples.spring.Printer" />

    </beans>

## Automatic resource deployment

Spring integration also has a special feature for deploying resources. In the process engine configuration, you can specify a set of resources. When the process engine is created, all those resources will be scanned and deployed. There is filtering in place that prevents duplicate deployments. Only when the resources have actually changed will new deployments be deployed to the Flowable DB. This makes sense in a lot of use cases, where the Spring container is rebooted frequently (for example, testing).

Here’s an example:

    <bean id="processEngineConfiguration" class="org.flowable.spring.SpringProcessEngineConfiguration">
      ...
      <property name="deploymentResources"
        value="classpath*:/org/flowable/spring/test/autodeployment/autodeploy.*.bpmn20.xml" />
    </bean>

    <bean id="processEngine" class="org.flowable.spring.ProcessEngineFactoryBean">
      <property name="processEngineConfiguration" ref="processEngineConfiguration" />
    </bean>

By default, the configuration above will group all of the resources matching the filter into a single deployment to the Flowable engine. The duplicate filtering to prevent re-deployment of unchanged resources applies to the whole deployment. In some cases, this may not be what you want. For instance, if you deploy a set of process resources this way and only a single process definition in those resources has changed, the deployment as a whole will be considered new and all of the process definitions in that deployment will be re-deployed, resulting in new versions of each of the process definitions, even though only one was actually changed.

To be able to customize the way deployments are determined, you can specify an additional property in the SpringProcessEngineConfiguration, deploymentMode. This property defines the way deployments will be determined from the set of resources that match the filter. There are 3 values that are supported by default for this property:

-   default: Group all resources into a single deployment and apply duplicate filtering to that deployment. This is the default value and it will be used if you don’t specify a value.

-   single-resource: Create a separate deployment for each individual resource and apply duplicate filtering to that deployment. This is the value you would use to have each process definition be deployed separately and only create a new process definition version if it has changed.

-   resource-parent-folder: Create a separate deployment for resources that share the same parent folder and apply duplicate filtering to that deployment. This value can be used to create separate deployments for most resources, but still be able to group some by placing them in a shared folder. Here’s an example of how to specify the single-resource configuration for deploymentMode:

<!-- -->

    <bean id="processEngineConfiguration"
        class="org.flowable.spring.SpringProcessEngineConfiguration">
      ...
      <property name="deploymentResources" value="classpath*:/flowable/*.bpmn" />
      <property name="deploymentMode" value="single-resource" />
    </bean>

In addition to using the values listed above for deploymentMode, you may require customized behavior towards determining deployments. If so, you can create a subclass of SpringProcessEngineConfiguration and override the getAutoDeploymentStrategy(String deploymentMode) method. This method determines which deployment strategy is used for a certain value of the deploymentMode configuration.

## Unit testing

When integrating with Spring, business processes can be tested very easily using the standard [Flowable testing facilities](bpmn/ch04-API.md#unit-testing).
The following examples show how a business process is tested in typical Spring-based JUnit 4 and 5 test:

**JUnit 5 test.**

    @ExtendWith(FlowableSpringExtension.class)
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = SpringJunitJupiterTest.TestConfiguration.class)
    public class MyBusinessProcessTest {

      @Autowired
      private RuntimeService runtimeService;

      @Autowired
      private TaskService taskService;

      @Test
      @Deployment
      void simpleProcessTest() {
        runtimeService.startProcessInstanceByKey("simpleProcess");
        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("My Task", task.getName());

        taskService.complete(task.getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());

      }
    }

Using the FlowableSpringExtension allows the usage of the Deployment annotation.

**JUnit 4 test.**

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration("classpath:org/flowable/spring/test/junit4/springTypicalUsageTest-context.xml")
    public class MyBusinessProcessTest {

      @Autowired
      private RuntimeService runtimeService;

      @Autowired
      private TaskService taskService;

      @Autowired
      @Rule
      public FlowableRule flowableSpringRule;

      @Test
      @Deployment
      public void simpleProcessTest() {
        runtimeService.startProcessInstanceByKey("simpleProcess");
        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("My Task", task.getName());

        taskService.complete(task.getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());

      }
    }

Note that for this to work, you need to define an *org.flowable.engine.test.Flowable* bean in the Spring configuration (which is injected by auto-wiring in the example above).

    <bean id="flowableRule" class="org.flowable.engine.test.Flowable">
      <property name="processEngine" ref="processEngine" />
    </bean>

## JPA with Hibernate 4.2.x

When using Hibernate 4.2.x JPA in service task or listener logic in the Flowable engine, an additional dependency to Spring ORM is needed. This is not needed for Hibernate 4.1.x or earlier. The following dependency should be added:

    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-orm</artifactId>
      <version>${org.springframework.version}</version>
    </dependency>
