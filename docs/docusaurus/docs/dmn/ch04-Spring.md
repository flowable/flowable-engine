---
id: ch04-Spring
title: Spring integration
---

While you can definitely use Flowable DMN without Spring, we’ve provided some very nice integration features that are explained in this chapter.

## DmnEngineFactoryBean

The DmnEngine can be configured as a regular Spring bean. The starting point of the integration is the class org.flowable.dmn.spring.DmnEngineFactoryBean. This bean takes a DMN engine configuration and creates the DMN engine. This means that the creation and configuration of properties for Spring is the same as documented in the [configuration section](dmn/ch02-Configuration.md#creating-a-dmn-engine). For Spring integration, the configuration and engine beans will look like this:

    <bean id="dmnEngineConfiguration" class="org.flowable.dmn.spring.SpringDmnEngineConfiguration">
        ...
    </bean>

    <bean id="dmnEngine" class="org.flowable.dmn.spring.DmnEngineFactoryBean">
      <property name="dmnEngineConfiguration" ref="dmnEngineConfiguration" />
    </bean>

Note that the dmnEngineConfiguration bean now uses the org.flowable.dmn.spring.SpringDmnEngineConfiguration class.

## Automatic resource deployment

Spring integration also has a special feature for deploying resources. In the DMN engine configuration, you can specify a set of resources. When the DMN engine is created, all these resources will be scanned and deployed. There is filtering in place that prevents duplicate deployments. Only when the resources have actually changed will new deployments be deployed to the Flowable DMN DB. This makes sense in a lot of use case, where the Spring container is rebooted often (for example, testing).

Here’s an example:

    <bean id="dmnEngineConfiguration" class="org.flowable.spring.SpringDmnEngineConfiguration">
      ...
      <property name="deploymentResources"
        value="classpath*:/org/flowable/spring/test/autodeployment/autodeploy/decision*.dmn" />
    </bean>

    <bean id="dmnEngine" class="org.flowable.dmn.spring.DmnEngineFactoryBean">
      <property name="dmnEngineConfiguration" ref="dmnEngineConfiguration" />
    </bean>

By default, the configuration above will group all of the resources matching the filtering into a single deployment to the Flowable DMN engine. The duplicate filtering to prevent re-deployment of unchanged resources applies to the whole deployment. In some cases, this may not be what you want. For instance, if you deploy a set of DMN resources this way, and only a single
DMN definition in those resources has changed, the deployment as a whole will be considered new and all of the process definitions in that deployment will be re-deployed, resulting in new versions of each of the DMN definitions, even though only one was actually changed.

To be able to customize the way deployments are determined, you can specify an additional property in the SpringDmnEngineConfiguration, deploymentMode. This property defines the way deployments will be determined from the set of resources that match the filter. There are three values that are supported by default for this property:

-   default: Group all resources into a single deployment and apply duplicate filtering to that deployment. This is the default value and it will be used if you don’t specify a value.

-   single-resource: Create a separate deployment for each individual resource and apply duplicate filtering to that deployment. This is the value you would use to have each DMN definition be deployed separately and only create a new DMN definition version if it has changed.

-   resource-parent-folder: Create a separate deployment for resources that share the same parent folder and apply duplicate filtering to that deployment. This value can be used to create separate deployments for most resources, but still be able to group some by placing them in a shared folder. Here’s an example of how to specify the single-resource configuration for deploymentMode:

<!-- -->

    <bean id="dmnEngineConfiguration"
        class="org.flowable.dmn.spring.SpringDmnEngineConfiguration">
      ...
      <property name="deploymentResources" value="classpath*:/flowable/*.dmn" />
      <property name="deploymentMode" value="single-resource" />
    </bean>

In addition to using the values listed above for deploymentMode, you may want customized behavior towards determining deployments. If so, you can create a subclass of SpringDmnEngineConfiguration and override the getAutoDeploymentStrategy(String deploymentMode) method. This method determines which deployment strategy is used for a certain value of the deploymentMode configuration.

## Unit testing

When integrating with Spring, decisions can be tested very easily using the standard [Flowable testing facilities](dmn/ch03-API.md#unit-testing).
The following examples show how a decision is tested in typical Spring-based JUnit 4 and 5 tests:

**JUnit 5 test.**

    @ExtendWith(FlowableDmnSpringExtension.class)
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = DmnSpringJunitJupiterTest.TestConfiguration.class)
    public class SpringJunit4Test {

        @Autowired
        private DmnEngine dmnEngine;

        @Autowired
        private DmnRuleService ruleService;

        @Test
        @DmnDeploymentAnnotation
        public void simpleDecisionTest() {
            Map<String, Object> executionResult = ruleService.createExecuteDecisionBuilder()
                .decisionKey("extensionUsage")
                .variable("inputVariable1", 2)
                .variable("inputVariable2", "test2")
                .executeWithSingleResult();

            Assertions.assertThat(executionResult).containsEntry("output1", "test1");
        }
    }

**JUnit 4 test.**

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration("classpath:org/flowable/spring/test/junit4/springTypicalUsageTest-context.xml")
    public class SpringJunit4Test {

        @Autowired
        private DmnEngine dmnEngine;

        @Autowired
        private DmnRuleService ruleService;

        @Autowired
        @Rule
        public FlowableDmnRule flowableSpringRule;

        @Test
        @DmnDeploymentAnnotation
        public void simpleDecisionTest() {
            Map<String, Object> executionResult = ruleService.createExecuteDecisionBuilder()
                    .decisionKey("extensionUsage")
                    .variable("inputVariable1", 2)
                    .variable("inputVariable2", "test2")
                    .executeWithSingleResult();

            Assertions.assertThat(executionResult).containsEntry("output1", "test1");
        }
    }

Note that for this to work, you need to define an *org.flowable.dmn.engine.test.FlowableDmnRule* bean in the Spring configuration (which is injected by auto-wiring in the example above).

    <bean id="flowableDmnRule" class="org.flowable.dmn.engine.test.FlowableDmnRule">
        <property name="dmnEngine" ref="dmnEngine"/>
    </bean>
