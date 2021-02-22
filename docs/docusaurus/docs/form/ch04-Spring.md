---
id: ch04-Spring
title: Spring integration
---

While you can definitely use Flowable Form without Spring, we’ve provided some very nice integration features that are explained in this chapter.

## FormEngineFactoryBean

The FormEngine can be configured as a regular Spring bean. The starting point of the integration is the class org.flowable.form.spring.FormEngineFactoryBean. This bean takes a Form engine configuration and creates the Form engine. This means that the creation and configuration of properties for Spring is the same as documented in the [configuration section](form/ch02-Configuration.md#creating-a-form-engine). For Spring integration, the configuration and engine beans will look like this:

    <bean id="formEngineConfiguration" class="org.flowable.form.spring.SpringFormEngineConfiguration">
        ...
    </bean>

    <bean id="formEngine" class="org.flowable.form.spring.FormEngineFactoryBean">
      <property name="formEngineConfiguration" ref="formEngineConfiguration" />
    </bean>

Note that the formEngineConfiguration bean now uses the org.flowable.form.spring.SpringFormEngineConfiguration class.

## Automatic resource deployment

Spring integration also has a special feature for deploying resources. In the Form engine configuration, you can specify a set of resources. When the Form engine is created, all those resources will be scanned and deployed. There is filtering in place that prevents duplicate deployments. Only when the resources actually have changed will new deployments be deployed to the Flowable Form DB. This makes sense in a lot of use cases, where the Spring container is rebooted often (for example, testing).

Here’s an example:

    <bean id="formEngineConfiguration" class="org.flowable.spring.SpringFormEngineConfiguration">
      ...
      <property name="deploymentResources"
        value="classpath*:/org/flowable/spring/test/autodeployment/autodeploy/*.form" />
    </bean>

    <bean id="formEngine" class="org.flowable.form.spring.FormEngineFactoryBean">
      <property name="formEngineConfiguration" ref="formEngineConfiguration" />
    </bean>

By default, the configuration above will group all of the resources matching the filtering into a single deployment to the Flowable Form engine. The duplicate filtering to prevent re-deployment of unchanged resources applies to the whole deployment. In some cases, this may not be what you want. For instance, if you deploy a set of Form resources this way and only a single Form definition in those resources has changed, the deployment as a whole will be considered new and all of the process definitions in that deployment will be re-deployed, resulting in new versions of each of the Form definitions, even though only one was actually changed.

To be able to customize the way deployments are determined, you can specify an additional property in the SpringFormEngineConfiguration, deploymentMode. This property defines the way deployments will be determined from the set of resources that match the filter. There are 3 values that are supported by default for this property:

-   default: Group all resources into a single deployment and apply duplicate filtering to that deployment. This is the default value and it will be used if you don’t specify a value.

-   single-resource: Create a separate deployment for each individual resource and apply duplicate filtering to that deployment. This is the value you would use to have each Form definition be deployed separately and only create a new Form definition version if it has changed.

-   resource-parent-folder: Create a separate deployment for resources that share the same parent folder and apply duplicate filtering to that deployment. This value can be used to create separate deployments for most resources, but still be able to group some by placing them in a shared folder. Here’s an example of how to specify the single-resource configuration for deploymentMode:

<!-- -->

    <bean id="formEngineConfiguration"
        class="org.flowable.form.spring.SpringFormEngineConfiguration">
      ...
      <property name="deploymentResources" value="classpath*:/flowable/*.form" />
      <property name="deploymentMode" value="single-resource" />
    </bean>

In addition to using the values listed above for deploymentMode, you may require customized behavior towards determining deployments. If so, you can create a subclass of SpringFormEngineConfiguration and override the getAutoDeploymentStrategy(String deploymentMode) method. This method determines which deployment strategy is used for a certain value of the deploymentMode configuration.

## Unit testing

When integrating with Spring, forms can be tested very easily using the standard [Flowable testing facilities](form/ch03-API.md#unit-testing).
The following examples show how a form is tested in a typical Spring-based JUnit 4 and 5 tests:

**JUnit 5 test.**

    @ExtendWith(FlowableFormSpringExtension.class)
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = SpringJunitJupiterTest.TestConfiguration.class)
    public class SpringJunit4Test {

        @Autowired
        private FormEngine formEngine;

        @Autowired
        private FormService formService;

        @Test
        @FormDeploymentAnnotation
        public void simpleFormInstanceTest() {
            FormInstance result = formService.getFormInstanceModelById(
                "f7689f79-f1cc-11e6-8549-acde48001122", null);

            Assertions.assertNotNull(result));
        }
    }

Using the FlowableFormSpringExtension allows the usage of the Deployment annotation.

**JUnit 4 test.**

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration("classpath:org/flowable/spring/test/junit4/springTypicalUsageTest-context.xml")
    public class SpringJunit4Test {

        @Autowired
        private FormEngine formEngine;

        @Autowired
        private FormService formService;

        @Autowired
        @Rule
        public FlowableFormRule flowableSpringRule;

        @Test
        @FormDeploymentAnnotation
        public void simpleFormInstanceTest() {
            FormInstance result = formService.getFormInstanceModelById(
                "f7689f79-f1cc-11e6-8549-acde48001122", null);

            Assert.assertNotNull(result));
        }
    }

Note that for this to work, you need to define a *org.flowable.form.engine.test.FlowableFormRule* bean in the Spring configuration (which is injected by auto-wiring in the example above).

    <bean id="flowableFormRule" class="org.flowable.form.engine.test.FlowableFormRule">
        <property name="formEngine" ref="formEngine"/>
    </bean>
