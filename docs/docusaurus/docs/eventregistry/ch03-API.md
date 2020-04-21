---
id: ch03-API
title: The Flowable Event Registry API
---

## The Event Registry Engine API and services

The Event Registry engine API is the most common way of interacting with the Flowable Event Registry. The central starting point is the EventRegistryEngine, which can be created in several ways as described in the [configuration section](eventregistry/ch02-Configuration.md#creating-an-event-registry-engine). From the EventRegistryEngine, you can obtain the various other services.
EventRegistryEngine and the services objects are thread safe. So you can keep a reference to one of those for a whole server.

    EventRegistryEngine eventRegistryEngine = EventRegistryEngines.getDefaultEventRegistryEngine();
    EventRegistry eventRegistry = eventRegistryEngine.getEventRegistry();
    EventRepositoryService eventRepositoryService = eventRegistryEngine.getEventRepositoryService();
    EventManagementService eventManagementService = eventRegistryEngine.getEventManagementService();

EventRegistryEngines.getDefaultEventRegistryEngine() will initialize and build an Event Registry engine the first time it is called and afterwards always return the same Event Registry engine. Proper creation and closing of all Event Registry engines can be done with EventRegistryEngines.init() and EventRegistryEngines.destroy().

The EventRegistryEngines class will scan for all flowable.eventregistry.cfg.xml and flowable-eventregistry-context.xml files. For all flowable.eventregistry.cfg.xml files, the Event Registry engine will be built in the typical Flowable way: EventRegistryEngineConfiguration.createEventRegistryEngineConfigurationFromInputStream(inputStream).buildEventRegistryEngine(). For all flowable-eventregistry-context.xml files, the Event Registry engine will be built in the Spring way: First the Spring application context is created and then the Event Registry engine is obtained from that application context.

All services are stateless. This means that you can easily run the Flowable Event Registry on multiple nodes in a cluster, each going to the same database, without having to worry about which machine actually executed previous calls. Any call to any service is idempotent, regardless of where it is executed.

The **EventRepositoryService** is probably the first service needed when working with the Flowable Event Registry engine. This service offers operations for managing and manipulating deployments and Event and Channel definitions. 
An event definition is used to configure the event payload and correlation parameters and defines the relation to a specific channel definition key.
A channel definition configures the source or target destination of an incoming or outgoing event. For example, a channel definition can define a JMS listener to a JMS queue name **orderQueue**. 
A deployment is the unit of packaging within the Flowable Event Registry engine. A deployment can contain multiple Event and Channel definition JSON files. Deploying a deployment means it is uploaded to the engine, where all Event and Channel definitions are inspected and parsed before being stored in the database. From that point on, the deployment is known to the system and any event and channel definition included in the deployment can now be used.

Furthermore, this service allows you to:

-   Query deployments, Event and Channel definitions known to the engine.

-   Retrieve a POJO version of the Event and Channel definition that can be used to introspect using Java rather than JSON.

The **EventRegistry** provides methods to receive and send event instances. It also provides methods to add and remove event consumers.

The **EventManagementService** is typically not needed when coding custom application using the Flowable Event Registry. It allows you to retrieve information about the engine version, database tables and table metadata.

For more detailed information on the service operations and the Event Registry engine API, see [the javadocs](http://www.flowable.org/docs/javadocs/index.html).

## Exception strategy

The base exception in Flowable is the org.flowable.engine.FlowableException, an unchecked exception. This exception can be thrown at all times by the API, but 'expected' exceptions that happen in specific methods are documented in [the javadocs](http://www.flowable.org/docs/javadocs/index.html).

    /**
      * Retrieves the Java representation of an event definition.
      *
      * @param  eventDefinitionKey      the event definition key, cannot be null
      * @return the event model Java representation.
      */
    EventModel getEventModelByKey(String eventDefinitionKey);

In the example above, when a key is passed for which no event definitions exist, an exception will be thrown. Also, as the javadoc **explicitly states that eventDefinitionKey cannot be null, a FlowableIllegalArgumentException will be thrown when null is passed**.

Even though we want to avoid a big exception hierarchy, the following subclasses are thrown in specific cases. All other errors that occur during execution or API invocation that don’t fit into the possible exceptions below are thrown as regular FlowableExceptions.

-   FlowableOptimisticLockingException: Thrown when an optimistic lock occurs in the data store caused by concurrent access of the same data entry.

-   FlowableClassLoadingException: Thrown when a class requested to load was not found or when an error occurred while loading it.

-   FlowableObjectNotFoundException: Thrown when an object that is requested or acted on does not exist.

-   FlowableIllegalArgumentException: An exception indicating that an illegal argument has been supplied in a Flowable Event Registry API call, an illegal value was configured in the engine’s configuration or an illegal value has been supplied.

## Query API

The Query API allows to program completely typesafe queries with a fluent API. You can add various conditions to your queries (all of which are applied together as a logical AND) and precisely one ordering. The following code shows an example:

    List<EventDeployment> eventDeployments = eventRepositoryService.createDeploymentQuery()
        .deploymentNameLike("deployment%")
        .orderByDeployTime()
        .list();


## Unit testing

As the Flowable Event Registry is an embeddable Java engine, writing unit tests for Event and Channel definitions is as simple as writing regular unit tests.

Flowable supports the JUnit version 5 style of unit testing.

In the JUnit 5 style one needs to register the org.flowable.eventregistry.test.FlowableEventExtension.
Registering the FlowableEventExtension can be done with @ExtendWith(FlowableEventExtension.class).
This will make the EventRegistryEngine and the services available as parameters into the test and lifecycle methods
(@BeforeAll, @BeforeEach, @AfterEach, @AfterAll).
Before each test the eventRegistryEngine will be initialized by default with the flowable.eventregistry.cfg.xml resource on the classpath.
In order to specify a different configuration file the org.flowable.eventregistry.test.EventConfigurationResource
annotation needs to be used (see second example).
Event Registry engines are cached statically over multiple unit tests when the configuration resource is the same.

By using EventRegistryEngine, you can annotate test methods with org.flowable.eventregistry.test.EventDeploymentAnnotation.
When a test method is annotated with @EventDeploymentAnnotation, before each test the event definition files defined in EventDeploymentAnnotation\#resources will be deployed.
In case there are no resources defined, a resource file of the form testClassName.testMethod.event in the same package as the test class, will be deployed.
At the end of the test, the deployment will be deleted, including all related event and channel definitions.
See the EventDeploymentAnnotation class for more information.

In addition, by using EventRegistryEngine, you can annotate test methods with org.flowable.eventregistry.test.ChannelDeploymentAnnotation.
When a test method is annotated with @ChannelDeploymentAnnotation, before each test the channel definition files defined in ChannelDeploymentAnnotation\#resources will be deployed.
In case there are no resources defined, a resource file of the form testClassName.testMethod.channel in the same package as the test class, will be deployed.
At the end of the test, the deployment will be deleted, including all related event and channel definitions.
See the EventDeploymentAnnotation class for more information.

Taking all that in account, a JUnit 5 test looks as follows:

**JUnit 5 test with default resource.**

    @ExtendWith(FlowableEventExtension.class)
    class MyEventDefinitionTest {

      @Test
      @EventDeploymentAnnotation
      void simpleEventDefinitionTest(EventRegistryEngine eventRegistryEngine) {
          EventRepositoryService repositoryService = eventRegistryEngine.getEventRepositoryService();
          EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
          assertNotNull(eventDefinition);
          assertEquals("myEvent", eventDefinition.getKey());
          assertEquals(1, eventDefinition.getVersion());
      }
    }

**JUnit 5 test with custom resource.**

    @ExtendWith(FlowableEventExtension.class)
    @EventConfigurationResource("flowable.custom.eventregistry.cfg.xml")
    class MyEventDefinitionTest {

      @Test
      @EventDeploymentAnnotation
      void simpleEventDefinitionTest(EventRegistryEngine eventRegistryEngine) {
          EventRepositoryService repositoryService = eventRegistryEngine.getEventRepositoryService();
          EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery()
                .eventDefinitionKey("myEvent")
                .latestVersion()
                .singleResult();
          assertNotNull(eventDefinition);
          assertEquals("myEvent", eventDefinition.getKey());
          assertEquals(1, eventDefinition.getVersion());
      }
    }

## The Event Registry engine in a web application

The EventRegistryEngine is a thread-safe class and can easily be shared among multiple threads. In a web application, this means it is possible to create the Event Registry engine once when the container boots and shut down the engine when the container goes down.

The following code snippet shows how you can write a simple ServletContextListener to initialize and destroy process engines in a plain Servlet environment:

    public class EventRegistryEnginesServletContextListener implements ServletContextListener {

      public void contextInitialized(ServletContextEvent servletContextEvent) {
        EventRegistryEngines.init();
      }

      public void contextDestroyed(ServletContextEvent servletContextEvent) {
        EventRegistryEngines.destroy();
      }

    }

The contextInitialized method will delegate to EventRegistryEngines.init(). This will look for flowable.eventregistry.cfg.xml resource files on the classpath, and create a EventRegistryEngine for the given configurations (for example, multiple JARs with a configuration file). If you have multiple such resource files on the classpath, make sure they all have different names. When the Event Registry engine is needed, it can be fetched using:

    EventRegistryEngines.getDefaultEventRegistryEngine()

or:

    EventRegistryEngines.getEventRegistryEngine("myName");

Of course, it’s also possible to use any of the variants of creating an Event Registry engine,
as described in the [configuration section](eventregistry/ch02-Configuration.md#creating-an-event-registry-engine).

The contextDestroyed method of the context-listener delegates to EventRegistryEngines.destroy(). That will properly close all initialized Event Registry engines.
