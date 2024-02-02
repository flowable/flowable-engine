---
id: ch04-Spring
title: Spring integration
---

While you can definitely use the Flowable Event Registry without Spring, we’ve provided some very nice integration features that are explained in this chapter.

## EventRegistryFactoryBean

The EventRegistryEngine can be configured as a regular Spring bean. The starting point of the integration is the class org.flowable.eventregistry.spring.EventRegistryFactoryBean. This bean takes an Event Registry engine configuration and creates the Event Registry engine. This means that the creation and configuration of properties for Spring is the same as documented in the [configuration section](eventregistry/ch02-Configuration.md#creating-an-event-registry-engine). For Spring integration, the configuration and engine beans will look like this:

    <bean id="eventEngineConfiguration" class="org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration">
        ...
    </bean>

    <bean id="eventRegistryEngine" class="org.flowable.eventregistry.spring.EventRegistryFactoryBean">
      <property name="eventEngineConfiguration" ref="eventEngineConfiguration" />
    </bean>

Note that the eventEngineConfiguration bean now uses the org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration class.

## Automatic resource deployment

Spring integration also has a special feature for deploying resources. In the Event Registry engine configuration, you can specify a set of resources. When the Event Registry engine is created, all these resources will be scanned and deployed. There is filtering in place that prevents duplicate deployments. Only when the resources have actually changed will new deployments be deployed to the Flowable Event Registry DB. This makes sense in a lot of use case, where the Spring container is rebooted often (for example, testing).

Here’s an example:

    <bean id="eventEngineConfiguration" class="org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration">
      ...
      <property name="deploymentResources"
          value="classpath*:/org/flowable/test/autodeployment/autodeploy/event*.event" />
    </bean>

    <bean id="eventRegistryEngine" class="org.flowable.eventregistry.spring.EventRegistryFactoryBean">
      <property name="eventEngineConfiguration" ref="eventEngineConfiguration" />
    </bean>

By default, the configuration above will group all of the resources matching the filtering into a single deployment to the Flowable Event Registry engine. The duplicate filtering to prevent re-deployment of unchanged resources applies to the whole deployment. In some cases, this may not be what you want. For instance, if you deploy a set of Event Registry resources this way, and only a single
Event or Channel definition in those resources has changed, the deployment as a whole will be considered new and all of the process definitions in that deployment will be re-deployed, resulting in new versions of each of the Event and Channel definitions, even though only one was actually changed.

To be able to customize the way deployments are determined, you can specify an additional property in the SpringEventRegistryEngineConfiguration, deploymentMode. This property defines the way deployments will be determined from the set of resources that match the filter. There are three values that are supported by default for this property:

-   default: Group all resources into a single deployment and apply duplicate filtering to that deployment. This is the default value and it will be used if you don’t specify a value.

-   single-resource: Create a separate deployment for each individual resource and apply duplicate filtering to that deployment. This is the value you would use to have each Event and Channel definition be deployed separately and only create a new Event and Channel definition version if it has changed.

-   resource-parent-folder: Create a separate deployment for resources that share the same parent folder and apply duplicate filtering to that deployment. This value can be used to create separate deployments for most resources, but still be able to group some by placing them in a shared folder. Here’s an example of how to specify the single-resource configuration for deploymentMode:

<!-- -->

    <bean id="eventEngineConfiguration"
        class="org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration">
      ...
      <property name="deploymentResources" value="classpath*:/flowable/*.event" />
      <property name="deploymentMode" value="single-resource" />
    </bean>

In addition to using the values listed above for deploymentMode, you may want customized behavior towards determining deployments. If so, you can create a subclass of SpringEventRegistryEngineConfiguration and override the getAutoDeploymentStrategy(String deploymentMode) method. This method determines which deployment strategy is used for a certain value of the deploymentMode configuration.

## Unit testing

When integrating with Spring, decisions can be tested very easily using the standard [Flowable testing facilities](eventregistry/ch03-API.md#unit-testing).
The following examples show how a decision is tested in typical Spring-based JUnit 5 tests:

**JUnit 5 test.**

    @ExtendWith(FlowableEventSpringExtension.class)
    @SpringJUnitConfig(classes = EventRegistryJmsConfiguration.class)
    public class SpringEventRegistryChangeDetectorTest {

      @Autowired
      private EventRegistryEngine eventRegistryEngine;

      @Test
      public void testChangeDetectionRunnableCreatedWhenNotExplicitelyInjected() {
        assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionManager())
            .isInstanceOf(DefaultEventRegistryChangeDetectionManager.class);

        EventRegistryChangeDetectionExecutor eventRegistryChangeDetectionExecutor = eventRegistryEngine
            .getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionExecutor();
        assertThat(eventRegistryChangeDetectionExecutor).isInstanceOf(DefaultSpringEventRegistryChangeDetectionExecutor.class);

        DefaultSpringEventRegistryChangeDetectionExecutor executor = (DefaultSpringEventRegistryChangeDetectionExecutor) 
            eventRegistryChangeDetectionExecutor;
        assertThat(executor.getTaskScheduler()).isInstanceOf(ThreadPoolTaskScheduler.class);
      }
    }
