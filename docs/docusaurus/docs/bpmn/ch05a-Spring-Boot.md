---
id: ch05a-Spring-Boot
title: Spring Boot
---

Spring Boot is an application framework which, according to [its website](http://projects.spring.io/spring-boot/), *makes it easy to create stand-alone, production-grade Spring based Applications that you can "just run". It takes an opinionated view of the Spring platform and third-party libraries so you can get started with minimum fuss. Most Spring Boot applications need very little Spring configuration*.

For more information on Spring Boot, see [<http://projects.spring.io/spring-boot/>](http://projects.spring.io/spring-boot/)

The Spring Boot - Flowable integration has been developed together with Spring committers.

## Compatibility

Flowable supports both Spring Boot 2.0 and 1.5 with the same starters. The base support is for Spring Boot 2.0, which means that the actuator endpoints are only supported on 2.0.
The Flowable starters are also puling spring boot starter transitively, which means that users will have to define the 1.5 version of the spring boot starters in their own build files.

## Getting started

Spring Boot is all about convention over configuration.
To get started, you would need to create a Spring Boot project.
The easiest way to do this would be to create a project via [start.spring.io](https://start.spring.io/).
For example create a project with web and h2 dependencies.
Then in the created project simply add the *flowable-spring-boot-starter* or *flowable-spring-boot-starter-rest* dependency.
In case you don’t need all the engines see the other [Flowable starters](bpmn/ch05a-Spring-Boot.md#flowable-starters).
For example for Maven:

    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-spring-boot-starter</artifactId>
        <version>${flowable.version}</version>
    </dependency>

That’s all that’s needed. This dependency will transitively add the correct Flowable dependencies to the classpath.
You can now run your Spring Boot application:

    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;

    @SpringBootApplication(proxyBeanMethods = false)
    public class MyApplication {

        public static void main(String[] args) {
            SpringApplication.run(MyApplication.class, args);
        }

    }

You will see an output like this:

      .   ____          _            __ _ _
     /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
    ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
     \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
      '  |____| .__|_| |_|_| |_\__, | / / / /
     =========|_|==============|___/=/_/_/_/
     :: Spring Boot ::        (v2.0.0.RELEASE)

    MyApplication                            : Starting MyApplication on ...
    MyApplication                            : No active profile set, falling back to default profiles: default
    ConfigServletWebServerApplicationContext : Refreshing org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext@4fdfa676: startup date [Wed Mar 28 12:04:00 CEST 2018]; root of context hierarchy
    o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
    o.apache.catalina.core.StandardService   : Starting service [Tomcat]
    org.apache.catalina.core.StandardEngine  : Starting Servlet Engine: Apache Tomcat/8.5.28
    o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
    o.s.web.context.ContextLoader            : Root WebApplicationContext: initialization completed in 3085 ms
    o.s.b.w.servlet.ServletRegistrationBean  : Servlet dispatcherServlet mapped to [/]
    o.s.b.w.servlet.ServletRegistrationBean  : Servlet Flowable IDM Rest API mapped to [/idm-api/*]
    o.s.b.w.servlet.ServletRegistrationBean  : Servlet Flowable Form Rest API mapped to [/form-api/*]
    o.s.b.w.servlet.ServletRegistrationBean  : Servlet Flowable DMN Rest API mapped to [/dmn-api/*]
    o.s.b.w.servlet.ServletRegistrationBean  : Servlet Flowable Content Rest API mapped to [/content-api/*]
    o.s.b.w.servlet.ServletRegistrationBean  : Servlet Flowable CMMN Rest API mapped to [/cmmn-api/*]
    o.s.b.w.servlet.ServletRegistrationBean  : Servlet Flowable BPMN Rest API mapped to [/process-api/*]
    o.s.b.w.servlet.FilterRegistrationBean   : Mapping filter: 'characterEncodingFilter' to: [/*]
    o.s.b.w.servlet.FilterRegistrationBean   : Mapping filter: 'hiddenHttpMethodFilter' to: [/*]
    o.s.b.w.servlet.FilterRegistrationBean   : Mapping filter: 'httpPutFormContentFilter' to: [/*]
    o.s.b.w.servlet.FilterRegistrationBean   : Mapping filter: 'requestContextFilter' to: [/*]
    uration$$EnhancerBySpringCGLIB$$3d0c70ac : No deployment resources were found for autodeployment
    uration$$EnhancerBySpringCGLIB$$8131eb1a : No deployment resources were found for autodeployment
    o.f.e.i.c.ProcessEngineConfigurationImpl : Found 5 Engine Configurators in total:
    o.f.e.i.c.ProcessEngineConfigurationImpl : class org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator (priority:100000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : class org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator (priority:200000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : class org.flowable.form.spring.configurator.SpringFormEngineConfigurator (priority:300000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : class org.flowable.content.spring.configurator.SpringContentEngineConfigurator (priority:400000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : class org.flowable.cmmn.spring.configurator.SpringCmmnEngineConfigurator (priority:500000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing beforeInit() of class org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator (priority:100000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing beforeInit() of class org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator (priority:200000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing beforeInit() of class org.flowable.form.spring.configurator.SpringFormEngineConfigurator (priority:300000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing beforeInit() of class org.flowable.content.spring.configurator.SpringContentEngineConfigurator (priority:400000)
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing beforeInit() of class org.flowable.cmmn.spring.configurator.SpringCmmnEngineConfigurator (priority:500000)
    com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
    com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing configure() of class org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator (priority:100000)
    .d.AbstractSqlScriptBasedDbSchemaManager : performing create on identity with resource org/flowable/idm/db/create/flowable.h2.create.identity.sql
    o.f.idm.engine.impl.IdmEngineImpl        : IdmEngine default created
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing configure() of class org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator (priority:200000)
    o.f.dmn.engine.impl.DmnEngineImpl        : DmnEngine default created
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing configure() of class org.flowable.form.spring.configurator.SpringFormEngineConfigurator (priority:300000)
    o.f.form.engine.impl.FormEngineImpl      : FormEngine default created
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing configure() of class org.flowable.content.spring.configurator.SpringContentEngineConfigurator (priority:400000)
    o.f.c.engine.ContentEngineConfiguration  : Content file system root : ...
    o.f.c.engine.impl.ContentEngineImpl      : ContentEngine default created
    o.f.e.i.c.ProcessEngineConfigurationImpl : Executing configure() of class org.flowable.cmmn.spring.configurator.SpringCmmnEngineConfigurator (priority:500000)
    o.f.cmmn.engine.CmmnEngineConfiguration  : Found 1 Engine Configurators in total:
    o.f.cmmn.engine.CmmnEngineConfiguration  : class org.flowable.idm.engine.configurator.IdmEngineConfigurator (priority:100000)
    o.f.cmmn.engine.CmmnEngineConfiguration  : Executing beforeInit() of class org.flowable.idm.engine.configurator.IdmEngineConfigurator (priority:100000)
    o.f.cmmn.engine.CmmnEngineConfiguration  : Executing configure() of class org.flowable.idm.engine.configurator.IdmEngineConfigurator (priority:100000)
    o.f.idm.engine.impl.IdmEngineImpl        : IdmEngine default created
    o.f.cmmn.engine.impl.CmmnEngineImpl      : CmmnEngine default created
    o.f.engine.impl.ProcessEngineImpl        : ProcessEngine default created
    o.f.j.s.i.a.AbstractAsyncExecutor        : Starting up the async job executor [org.flowable.spring.job.service.SpringAsyncExecutor].
    o.f.j.s.i.a.AcquireAsyncJobsDueRunnable  : starting to acquire async jobs due
    o.f.j.s.i.a.AcquireTimerJobsRunnable     : starting to acquire async jobs due
    o.f.j.s.i.a.ResetExpiredJobsRunnable     : starting to reset expired jobs
    o.f.e.impl.cmd.ValidateV5EntitiesCmd     : Total of v5 deployments found: 0
    s.w.s.m.m.a.RequestMappingHandlerAdapter : Looking for @ControllerAdvice: org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext@4fdfa676: startup date [Wed Mar 28 12:04:00 CEST 2018]; root of context hierarchy
    s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/error]}" onto public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String, java.lang.Object>> org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController.error(javax.servlet.http.HttpServletRequest)
    s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/error],produces=[text/html]}" onto public org.springframework.web.servlet.ModelAndView org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController.errorHtml(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)
    o.s.w.s.handler.SimpleUrlHandlerMapping  : Mapped URL path [/webjars/**] onto handler of type [class org.springframework.web.servlet.resource.ResourceHttpRequestHandler]
    o.s.w.s.handler.SimpleUrlHandlerMapping  : Mapped URL path [/**] onto handler of type [class org.springframework.web.servlet.resource.ResourceHttpRequestHandler]
    o.s.w.s.handler.SimpleUrlHandlerMapping  : Mapped URL path [/**/favicon.ico] onto handler of type [class org.springframework.web.servlet.resource.ResourceHttpRequestHandler]
    o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
    o.s.j.e.a.AnnotationMBeanExporter        : Bean with name 'dataSource' has been autodetected for JMX exposure
    o.s.j.e.a.AnnotationMBeanExporter        : Located MBean 'dataSource': registering with JMX server as MBean [com.zaxxer.hikari:name=dataSource,type=HikariDataSource]
    o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase -20
    o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase 0
    o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase 20
    o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
    flowable.Application                     : Started Application in 18.235 seconds (JVM running for 19.661)

So, by just adding the dependency to the classpath and using the *@SpringBootApplication* annotation a lot has happened behind the scenes:

-   An in-memory datasource is created automatically (because the H2 driver is on the classpath) and passed to the Flowable process engine configuration

-   A Flowable ProcessEngine, CmmnEngine, DmnEngine, FormEngine, ContentEngine and IdmEngine beans have been created and exposed

-   All the Flowable services are exposed as Spring beans

-   The Spring Job Executor is created

Also:

-   Any BPMN 2.0 process definitions in the *processes* folder will be automatically deployed. Create a folder *processes* and add a dummy process definition (named *one-task-process.bpmn20.xml*) to this folder. The content of this file is shown below.

-   Any CMMN 1.1 case definitions in the *cases* folder will be automatically deployed.

-   Any DMN 1.1 dmn definitions in the *dmn* folder will be automatically deployed.

-   Any Form definitions in the *forms* folder will be automatically deployed.

The XML content of the process definition is shown below. Notice that, for the moment, we are hard-coding an assignee called "kermit" to the user task. We’ll come back to this later.

    <?xml version="1.0" encoding="UTF-8"?>
    <definitions
            xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
            xmlns:flowable="http://flowable.org/bpmn"
            targetNamespace="Examples">

        <process id="oneTaskProcess" name="The One Task Process">
            <startEvent id="theStart" />
            <sequenceFlow id="flow1" sourceRef="theStart" targetRef="theTask" />
            <userTask id="theTask" name="my task" flowable:assignee="kermit" />
            <sequenceFlow id="flow2" sourceRef="theTask" targetRef="theEnd" />
            <endEvent id="theEnd" />
        </process>

    </definitions>

Also, add following code lines to test if the deployment actually worked. The *CommandLineRunner* is a special kind of Spring bean that is executed when the application boots:

    @SpringBootApplication(proxyBeanMethods = false)
    public class MyApplication {

        public static void main(String[] args) {
            SpringApplication.run(MyApplication.class, args);
        }

        @Bean
        public CommandLineRunner init(final RepositoryService repositoryService,
                                      final RuntimeService runtimeService,
                                      final TaskService taskService) {

            return new CommandLineRunner() {
                @Override
                public void run(String... strings) throws Exception {
                    System.out.println("Number of process definitions : "
                        + repositoryService.createProcessDefinitionQuery().count());
                    System.out.println("Number of tasks : " + taskService.createTaskQuery().count());
                    runtimeService.startProcessInstanceByKey("oneTaskProcess");
                    System.out.println("Number of tasks after process start: "
                        + taskService.createTaskQuery().count());
                }
            };
        }
    }

The output expected will be:

    Number of process definitions : 1
    Number of tasks : 0
    Number of tasks after process start : 1

## Changing the database and connection pool

As stated above, Spring Boot is about convention over configuration. By default, by having only H2 on the classpath, it created an in-memory datasource and passed that to the Flowable process engine configuration.

To change the datasource, simply add the database driver dependencies and provide the URL to the database.
For example, to switch to a MySQL database:

    spring.datasource.url=jdbc:mysql://127.0.0.1:3306/flowable-spring-boot?characterEncoding=UTF-8
    spring.datasource.username=flowable
    spring.datasource.password=flowable

Remove H2 from the Maven dependencies and add the MySQL driver to the classpath:

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>5.1.45</version>
    </dependency>

When the app is now booted up, you’ll see it uses MySQL as database (and the HikariCP connection pooling framework):

    org.flowable.engine.impl.db.DbSqlSession   : performing create on engine with resource org/flowable/db/create/flowable.mysql.create.engine.sql
    org.flowable.engine.impl.db.DbSqlSession   : performing create on history with resource org/flowable/db/create/flowable.mysql.create.history.sql
    org.flowable.engine.impl.db.DbSqlSession   : performing create on identity with resource org/flowable/db/create/flowable.mysql.create.identity.sql

When you reboot the application multiple times, you’ll see the number of tasks go up (the H2 in-memory database does not survive a shutdown, MySQL does).

For more information about how to configure the datasource have a look in [Configure a DataSource](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-sql.html#boot-features-configure-datasource) in the Spring Boot reference guide.

## REST support

Often, a REST API is used on top of the embedded Flowable engine (interacting with the different services in a company). Spring Boot makes this really easy. Add following dependency to the classpath:

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>${spring.boot.version}</version>
    </dependency>

Create a new class, a Spring service and create two methods: one to start our process and one to get a task list for a given assignee. We simply wrap Flowable calls here, but in real-life scenarios this will be more complex.

    @Service
    public class MyService {

        @Autowired
        private RuntimeService runtimeService;

        @Autowired
        private TaskService taskService;

        @Transactional
        public void startProcess() {
            runtimeService.startProcessInstanceByKey("oneTaskProcess");
        }

        @Transactional
        public List<Task> getTasks(String assignee) {
            return taskService.createTaskQuery().taskAssignee(assignee).list();
        }

    }

We can now create a REST endpoint by annotating a class with *@RestController*. Here, we simply delegate to the service defined above.

    @RestController
    public class MyRestController {

        @Autowired
        private MyService myService;

        @PostMapping(value="/process")
        public void startProcessInstance() {
            myService.startProcess();
        }

        @RequestMapping(value="/tasks", method= RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
        public List<TaskRepresentation> getTasks(@RequestParam String assignee) {
            List<Task> tasks = myService.getTasks(assignee);
            List<TaskRepresentation> dtos = new ArrayList<TaskRepresentation>();
            for (Task task : tasks) {
                dtos.add(new TaskRepresentation(task.getId(), task.getName()));
            }
            return dtos;
        }

        static class TaskRepresentation {

            private String id;
            private String name;

            public TaskRepresentation(String id, String name) {
                this.id = id;
                this.name = name;
            }

            public String getId() {
                return id;
            }
            public void setId(String id) {
                this.id = id;
            }
            public String getName() {
                return name;
            }
            public void setName(String name) {
                this.name = name;
            }

        }

    }

Both the *@Service* and the *@RestController* will be found by the automatic component scan for a Spring Boot application.
Run the application class again. We can now interact with the REST API, for example, by using cURL:

    curl http://localhost:8080/tasks?assignee=kermit
    []

    curl -X POST  http://localhost:8080/process
    curl http://localhost:8080/tasks?assignee=kermit
    [{"id":"10004","name":"my task"}]

As can be seen, we are referring to the assignee, "kermit", which was hard-coded into the process definition XML earlier. We’ll change this later on to allow the assignee to be set when the workflow instance is started.

## JPA support

To add JPA support for Flowable in Spring Boot, add following dependency:

    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-spring-boot-starter</artifactId>
        <version>${flowable.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <version>${spring-boot.version</version>
    </dependency>

This will add in the Spring configuration and beans for using JPA. By default, the JPA provider will be Hibernate.

Let’s create a simple Entity class:

    @Entity
    class Person {

        @Id
        @GeneratedValue
        private Long id;

        private String username;

        private String firstName;

        private String lastName;

        private Date birthDate;

        public Person() {
        }

        public Person(String username, String firstName, String lastName, Date birthDate) {
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.birthDate = birthDate;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public Date getBirthDate() {
            return birthDate;
        }

        public void setBirthDate(Date birthDate) {
            this.birthDate = birthDate;
        }
    }

By default, when not using an in-memory database, the tables won’t be created automatically. Create a file *application.properties* on the classpath and add following property:

    spring.jpa.hibernate.ddl-auto=update

Add following class:

    @Repository
    public interface PersonRepository extends JpaRepository<Person, Long> {

        Person findByUsername(String username);
    }

This is a Spring repository, which offers CRUD out of the box. We add the method to find a Person by username. Spring will automatically implement this based on conventions (typically, the property names used).

We now enhance our service further:

-   by adding *@Transactional* to the class. Note that by adding the JPA dependency above, the DataSourceTransactionManager which we were using before is now automatically swapped out by a JpaTransactionManager.

-   The *startProcess* now gets an assignee username passed in, which is used to look up the Person, and put the Person JPA object as a process variable in the process instance.

-   A method to create Dummy users is added. This is used in the CommandLineRunner to populate the database.

<!-- -->

    @Service
    @Transactional
    public class MyService {

        @Autowired
        private RuntimeService runtimeService;

        @Autowired
        private TaskService taskService;

        @Autowired
        private PersonRepository personRepository;

        public void startProcess(String assignee) {

            Person person = personRepository.findByUsername(assignee);

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("person", person);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
        }

        public List<Task> getTasks(String assignee) {
            return taskService.createTaskQuery().taskAssignee(assignee).list();
        }

        public void createDemoUsers() {
            if (personRepository.findAll().size() == 0) {
                personRepository.save(new Person("jbarrez", "Joram", "Barrez", new Date()));
                personRepository.save(new Person("trademakers", "Tijs", "Rademakers", new Date()));
            }
        }

    }

The CommandLineRunner now looks like:

    @Bean
    public CommandLineRunner init(final MyService myService) {

        return new CommandLineRunner() {
            public void run(String... strings) throws Exception {
                myService.createDemoUsers();
            }
        };
    }

The RestController is also modified slightly to incorporate the changes above (only showing new methods) and the HTTP POST now has a body that contains the assignee username:

    @RestController
    public class MyRestController {

        @Autowired
        private MyService myService;

        @PostMapping(value="/process")
        public void startProcessInstance(@RequestBody StartProcessRepresentation startProcessRepresentation) {
            myService.startProcess(startProcessRepresentation.getAssignee());
        }

       ...

        static class StartProcessRepresentation {

            private String assignee;

            public String getAssignee() {
                return assignee;
            }

            public void setAssignee(String assignee) {
                this.assignee = assignee;
            }
        }

And finally, to try out the Spring-JPA-Flowable integration, we assign the task using the ID of the Person JPA object in the process definition:

    <userTask id="theTask" name="my task" flowable:assignee="${person.id}"/>

This replaces the hard-coded recipient, "kermit", which we had initially set.

We can now start a new process instance, providing the user name in the POST body:

    curl -H "Content-Type: application/json" -d '{"assignee" : "jbarrez"}' http://localhost:8080/process

And the task list is now fetched using the person ID:

    curl http://localhost:8080/tasks?assignee=1

    [{"id":"12505","name":"my task"}]

## Flowable Actuator Endpoints

Flowable provides a Spring Boot Actuator Endpoint that exposes information for the Processes that are running.
By default, the `flowable` endpoint is mapped to `/actuator/flowable`.
Spring Boot by default only exposes a few endpoints to the web (e.g.: In `spring-boot-starter-actuator:2.5.4` , Spring Boot by default only exposes the `health` endpoint to the web. For more information about the endpoints exposed to the web by default, see [https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.exposing](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.exposing) ). In order to use the `flowable` endpoint through the web, you need to add `management.endpoints.web.exposure.include=flowable` to your `application.properties` (Note: The `org.flowable.spring.boot.EndpointAutoConfiguration` class does not use the `@ConditionalOnAvailableEndpoint` annotation to check whether the `flowable` endpoint is both enabled and exposed like the `HealthEndpointAutoConfiguration` class. So the only thing you need to do to use the `flowable` endpoint through the web is to add `management.endpoints.web.exposure.include=flowable` or `management.endpoints.web.exposure.include=*` to your configuration file.).

In order to make enable Actuator endpoints you need to add a dependency on Actuator, e.g. by using {sc-flowable-starter}/flowable-spring-boot-starter-actuator/pom.xml\[flowable-spring-boot-starter-actuator\].

`curl http://localhost:8080/actuator/flowable`

    {
      "completedTaskCountToday": 0,
      "deployedProcessDefinitions": [
        "oneTaskProcess (v1)"
      ],
      "processDefinitionCount": 1,
      "cachedProcessDefinitionCount": 0,
      "runningProcessInstanceCount": {
        "oneTaskProcess (v1)": 0
      },
      "completedTaskCount": 2,
      "completedActivities": 3,
      "completedProcessInstanceCount": {
        "oneTaskProcess (v1)": 0
      },
      "openTaskCount": 0
    }

For more information about Spring Boot Actuator see [Production Ready Endpoint](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html) in the Spring Boot reference documentation.

## Flowable Info Contributor

Flowable also provides a Spring Boot `InfoContributor` which looks like:

`curl http://localhost:8080/actuator/info`

    {
      "flowable": {
        "version": "6.6.0"
      }
    }

## Flowable Application Properties

The Flowable auto configuration is leveraging the Spring Boot properties and configuration mechanism.
See [Properties and Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-properties-and-configuration.html) in the Spring Boot reference guide.

Here is a list of configuration properties that the Flowable Spring Boot support consumes.

    # ===================================================================
    # Common Flowable Spring Boot Properties
    #
    # This sample file is provided as a guideline. Do NOT copy it in its
    # entirety to your own application.            ^^^
    # ===================================================================

    # Core (Process) https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/FlowableProperties.java
    flowable.check-process-definitions=true # Whether process definitions need to be auto deployed.
    flowable.custom-mybatis-mappers= # The FQN of custom Mybatis mappers that need to be added to the engine.
    flowable.custom-mybatis-x-m-l-mappers= # The location of the custom Mybatis XML Mappers that need to be added to the engine.
    flowable.database-schema= # In some situations you want to set the schema to use for table checks / generation if the database metadata doesn't return that correctly.
    flowable.database-schema-update=true # The strategy that should be used for the database schema.
    flowable.db-history-used=true # Whether db history should be used.
    flowable.deployment-name=SpringBootAutoDeployment # The name of the auto deployment.
    flowable.history-level=audit # The history level that needs to be used.
    flowable.process-definition-location-prefix=classpath*:/processes/ # The folder in which processes need to be searched for auto deployment.
    flowable.process-definition-location-suffixes=**.bpmn20.xml,**.bpmn # The suffixes (extensions) of the files that needs to be deployed from the 'processDefinitionLocationPrefix' location.

    # Process https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/process/FlowableProcessProperties.java
    flowable.process.definition-cache-limit=-1 # The maximum amount of process definitions available in the process definition cache. Per default it is -1 (all process definitions).
    flowable.process.enable-safe-xml=true # Enables extra checks on the BPMN xml that is parsed. See https://www.flowable.org/docs/userguide/index.html#advanced.safe.bpmn.xml. Unfortunately, this feature is not available on some platforms (JDK 6, JBoss), hence you need to disable if your platform does not allow the use of StaxSource during XML parsing.
    flowable.process.servlet.load-on-startup=-1 # Load on startup of the Process dispatcher servlet.
    flowable.process.servlet.name=Flowable BPMN Rest API # The name of the Process servlet.
    flowable.process.servlet.path=/process-api # The context path for the Process rest servlet.

    # Process Async Executor
    flowable.process.async-executor-activate=true # Whether the async executor should be activated.
    flowable.process.async.executor.async-job-lock-time=PT5M # The amount of time an async job is locked when acquired by the async executor. During this period of time, no other async executor will try to acquire and lock this job.
    flowable.process.async.executor.default-async-job-acquire-wait-time=PT10S # The time the async job acquisition thread will wait to execute the next acquirement query. This happens when no new async jobs were found or when less async jobs have been fetched. Default value = 10 seconds.
    flowable.process.async.executor.default-queue-size-full-wait-time=PT5S # The time the async job (both timer and async continuations) acquisition thread will wait when the queue is full to execute the next query.
    flowable.process.async.executor.default-timer-job-acquire-wait-time=PT10S # The time the timer job acquisition thread will wait to execute the next acquirement query. This happens when no new timer jobs were found or when less async jobs have been fetched. Default value = 10 seconds.
    flowable.process.async.executor.max-async-jobs-due-per-acquisition=1 # ???
    flowable.process.async.executor.timer-lock-time=PT5M # The amount of time a timer job is locked when acquired by the async executor. During this period of time, no other async executor will try to acquire and lock this job.


    # CMMN https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/cmmn/FlowableCmmnProperties.java
    flowable.cmmn.deploy-resources=true # Whether to perform deployment of resources, default is 'true'.
    flowable.cmmn.deployment-name=SpringBootAutoDeployment # The name of the deployment for the CMMN resources.
    flowable.cmmn.enable-safe-xml=true # Enables extra checks on the DMN xml that is parsed. See https://www.flowable.org/docs/userguide/index.html#advanced.safe.bpmn.xml. Unfortunately, this feature is not available on some platforms (JDK 6, JBoss), hence you need to disable if your platform does not allow the use of StaxSource during XML parsing.
    flowable.cmmn.enabled=true # Whether the CMMN engine needs to be started.
    flowable.cmmn.resource-location=classpath*:/cases/ # The location where the CMMN resources are located.
    flowable.cmmn.resource-suffixes=**.cmmn,**.cmmn11,**.cmmn.xml,**.cmmn11.xml # The suffixes for the resources that need to be scanned.
    flowable.cmmn.servlet.load-on-startup=-1 # Load on startup of the CMMN dispatcher servlet.
    flowable.cmmn.servlet.name=Flowable CMMN Rest API # The name of the CMMN servlet.
    flowable.cmmn.servlet.path=/cmmn-api # The context path for the CMMN rest servlet.

    # CMMN Async Executor
    flowable.cmmn.async-executor-activate=true # Whether the async executor should be activated.
    flowable.cmmn.async.executor.async-job-lock-time=PT5M # The amount of time an async job is locked when acquired by the async executor. During this period of time, no other async executor will try to acquire and lock this job.
    flowable.cmmn.async.executor.default-async-job-acquire-wait-time=PT10S # The time the async job acquisition thread will wait to execute the next acquirement query. This happens when no new async jobs were found or when less async jobs have been fetched. Default value = 10 seconds.
    flowable.cmmn.async.executor.default-queue-size-full-wait-time=PT5S # The time the async job (both timer and async continuations) acquisition thread will wait when the queue is full to execute the next query.
    flowable.cmmn.async.executor.default-timer-job-acquire-wait-time=PT10S # The time the timer job acquisition thread will wait to execute the next acquirement query. This happens when no new timer jobs were found or when less async jobs have been fetched. Default value = 10 seconds.
    flowable.cmmn.async.executor.max-async-jobs-due-per-acquisition=1 # ???
    flowable.cmmn.async.executor.timer-lock-time=PT5M # The amount of time a timer job is locked when acquired by the async executor. During this period of time, no other async executor will try to acquire and lock this job.

    # Content https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/content/FlowableContentProperties.java
    flowable.content.enabled=true # Whether the content engine needs to be started.
    flowable.content.servlet.load-on-startup=-1 # Load on startup of the Content dispatcher servlet.
    flowable.content.servlet.name=Flowable Content Rest API # The name of the Content servlet.
    flowable.content.servlet.path=/content-api # The context path for the Content rest servlet.
    flowable.content.storage.create-root=true # If the root folder doesn't exist, should it be created?
    flowable.content.storage.root-folder= # Root folder location where content files will be stored, for example, task attachments or form file uploads.

    # DMN https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/dmn/FlowableDmnProperties.java
    flowable.dmn.deploy-resources=true # Whether to perform deployment of resources, default is 'true'.
    flowable.dmn.deployment-name=SpringBootAutoDeployment # The name of the deployment for the dmn resources.
    flowable.dmn.enable-safe-xml=true # Enables extra checks on the DMN xml that is parsed. See https://www.flowable.org/docs/userguide/index.html#advanced.safe.bpmn.xml. Unfortunately, this feature is not available on some platforms (JDK 6, JBoss), hence you need to disable if your platform does not allow the use of StaxSource during XML parsing.
    flowable.dmn.enabled=true # Whether the dmn engine needs to be started.
    flowable.dmn.history-enabled=true # Whether the history for the DMN engine should be enabled.
    flowable.dmn.resource-location=classpath*:/dmn/ # The location where the dmn resources are located.
    flowable.dmn.resource-suffixes=**.dmn,**.dmn.xml,**.dmn11,**.dmn11.xml # The suffixes for the resources that need to be scanned.
    flowable.dmn.servlet.load-on-startup=-1 # Load on startup of the DMN dispatcher servlet.
    flowable.dmn.servlet.name=Flowable DMN Rest API # The name of the DMN servlet.
    flowable.dmn.servlet.path=/dmn-api # The context path for the DMN rest servlet.
    flowable.dmn.strict-mode=true # Set this to false if you want to ignore the decision table hit policy validity checks to result in an failed decision table state. A result is that intermediate results created up to the point the validation error occurs are returned.

    # Form https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/form/FlowableFormProperties.java
    flowable.form.deploy-resources=true # Whether to perform deployment of resources, default is true.
    flowable.form.deployment-name=SpringBootAutoDeployment # The name of the deployment for the form resources.
    flowable.form.enabled=true # Whether the form engine needs to be started.
    flowable.form.resource-location=classpath*:/forms/ # The location where the form resources are located.
    flowable.form.resource-suffixes=**.form # The suffixes for the resources that need to be scanned.
    flowable.form.servlet.load-on-startup=-1 # Load on startup of the Form dispatcher servlet.
    flowable.form.servlet.name=Flowable Form Rest API # The name of the Form servlet.
    flowable.form.servlet.path=/form-api # The context path for the Form rest servlet.

    # IDM https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/idm/FlowableIdmProperties.java
    flowable.idm.enabled=true # Whether the idm engine needs to be started.
    flowable.idm.password-encoder= # The type of the password encoder that needs to be used.
    flowable.idm.servlet.load-on-startup=-1 # Load on startup of the IDM dispatcher servlet.
    flowable.idm.servlet.name=Flowable IDM Rest API # The name of the IDM servlet.
    flowable.idm.servlet.path=/idm-api # The context path for the IDM rest servlet.

    # IDM Ldap https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/ldap/FlowableLdapProperties.java
    flowable.idm.ldap.attribute.email= # Name of the attribute that matches the user email.
    flowable.idm.ldap.attribute.first-name= # Name of the attribute that matches the user first name.
    flowable.idm.ldap.attribute.group-id= # Name of the attribute that matches the group id.
    flowable.idm.ldap.attribute.group-name= # Name of the attribute that matches the group name.
    flowable.idm.ldap.attribute.group-type= # Name of the attribute that matches the group type.
    flowable.idm.ldap.attribute.last-name= # Name of the attribute that matches the user last name.
    flowable.idm.ldap.attribute.user-id= # Name of the attribute that matches the user id.
    flowable.idm.ldap.base-dn= # The base 'distinguished name' (DN) from which the searches for users and groups are started.
    flowable.idm.ldap.cache.group-size=-1 # Allows to set the size of the {@link org.flowable.ldap.LDAPGroupCache}. This is an LRU cache that caches groups for users and thus avoids hitting the LDAP system each time the groups of a user needs to be known.
    flowable.idm.ldap.custom-connection-parameters= # Allows to set all LDAP connection parameters which do not have a dedicated setter. See for example http://docs.oracle.com/javase/tutorial/jndi/ldap/jndi.html for custom properties. Such properties are for example to configure connection pooling, specific security settings, etc.
    flowable.idm.ldap.enabled=false # Whether to enable LDAP IDM Service.
    flowable.idm.ldap.group-base-dn= # The base 'distinguished name' (DN) from which the searches for groups are started.
    flowable.idm.ldap.initial-context-factory=com.sun.jndi.ldap.LdapCtxFactory # The class name for the initial context factory.
    flowable.idm.ldap.password= # The password that is used to connect to the LDAP system.
    flowable.idm.ldap.port=-1 # The port on which the LDAP system is running.
    flowable.idm.ldap.query.all-groups= # The query that is executed when searching for all groups.
    flowable.idm.ldap.query.all-users= # The query that is executed when searching for all users.
    flowable.idm.ldap.query.groups-for-user= # The query that is executed when searching for the groups of a specific user.
    flowable.idm.ldap.query.user-by-full-name-like= # The query that is executed when searching for a user by full name.
    flowable.idm.ldap.query.user-by-id= # The query that is executed when searching for a user by userId.
    flowable.idm.ldap.query.group-by-id= # The query that is executed when searching for a specific group by groupId.
    flowable.idm.ldap.search-time-limit=0 # The timeout (in milliseconds) that is used when doing a search in LDAP. By default set to '0', which means 'wait forever'.
    flowable.idm.ldap.security-authentication=simple # The value that is used for the 'java.naming.security.authentication' property used to connect to the LDAP system.
    flowable.idm.ldap.server= # The server host on which the LDAP system can be reached. For example 'ldap://localhost'.
    flowable.idm.ldap.user= # The user id that is used to connect to the LDAP system.
    flowable.idm.ldap.user-base-dn= # The base 'distinguished name' (DN) from which the searches for users are started.

    # Flowable Mail https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/FlowableMailProperties.java
    flowable.mail.server.default-from=flowable@localhost # The default from address that needs to be used when sending emails.
    flowable.mail.server.force-to= # The force to address(es) that would be used when sending out emails. IMPORTANT: If this is set then all emails will be send to defined address(es) instead of the address configured in the MailActivity.
    flowable.mail.server.host=localhost # The host of the mail server.
    flowable.mail.server.password= # The password for the mail server authentication.
    flowable.mail.server.port=1025 # The port of the mail server.
    flowable.mail.server.ssl-port=1465 # The SSL port of the mail server.
    flowable.mail.server.use-ssl=false # Sets whether SSL/TLS encryption should be enabled for the SMTP transport upon connection (SMTPS/POPS).
    flowable.mail.server.use-tls=false # Set or disable the STARTTLS encryption.
    flowable.mail.server.username= # The username that needs to be used for the mail server authentication. If empty no authentication would be used.

    # Flowable Http https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/FlowableHttpProperties.java
    flowable.http.user-system-properties=false # Whether to use system properties (e.g. http.proxyPort).
    flowable.http.connect-timeout=5s # Connect timeout for the http client
    flowable.http.socket-timeout=5s # Socket timeout for the http client
    flowable.http.connection-request-timeout=5s # Connection Request Timeout for the http client
    flowable.http.request-retry-limit=3 # Request retry limit for the http client
    flowable.http.disable-cert-verify=false # Whether to disable certificate validation for the http client

    # Flowable REST
    flowable.rest.app.cors.enabled=true # Whether to enable CORS requests at all. If false, the other properties have no effect
    flowable.rest.app.cors.allow-credentials=true # Whether to include credentials in a CORS request
    flowable.rest.app.cors.allowed-origins=* # Comma-separated URLs to accept CORS requests from
    flowable.rest.app.cors.allowed-headers=* # Comma-separated HTTP headers to allow in a CORS request
    flowable.rest.app.cors.allowed-methods=DELETE,GET,PATCH,POST,PUT # Comma-separated HTTP verbs to allow in a CORS request
    flowable.rest.app.cors.exposed-headers=* # Comma-separated list of headers to expose in CORS response

    # Actuator
    management.endpoint.flowable.cache.time-to-live=0ms # Maximum time that a response can be cached.
    management.endpoint.flowable.enabled=true # Whether to enable the flowable endpoint.


<table>
<caption>Deprecated properties</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Property name</th>
<th>Old Property</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>flowable.process.servlet.name</p></td>
<td><p>flowable.rest-api-servlet-name</p></td>
<td><p>Flowable BPMN Rest API</p></td>
<td><p>The name of the Process servlet.</p></td>
</tr>
<tr class="even">
<td><p>flowable.process.servlet.path</p></td>
<td><p>flowable.rest-api-mapping</p></td>
<td><p>/process-api</p></td>
<td><p>The context path for the Process rest servlet.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.mail.server.host</p></td>
<td><p>flowable.mail-server-host</p></td>
<td><p>localhost</p></td>
<td><p>The host of the mail server.</p></td>
</tr>
<tr class="even">
<td><p>flowable.mail.server.password</p></td>
<td><p>flowable.mail-server-password</p></td>
<td><p>-</p></td>
<td><p>The password for the mail server authentication.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.mail.server.port</p></td>
<td><p>flowable.mail-server-port</p></td>
<td><p>1025</p></td>
<td><p>The port of the mail server.</p></td>
</tr>
<tr class="even">
<td><p>flowable.mail.server.ssl-port</p></td>
<td><p>flowable.mail-server-ssl-port</p></td>
<td><p>1465</p></td>
<td><p>The SSL port of the mail server.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.mail.server.use-ssl</p></td>
<td><p>flowable.mail-server-use-ssl</p></td>
<td><p>false</p></td>
<td><p>Sets whether SSL/TLS encryption should be enabled for the SMTP transport upon connection (SMTPS/POPS).</p></td>
</tr>
<tr class="even">
<td><p>flowable.mail.server.use-tls</p></td>
<td><p>flowable.mail-server-use-tls</p></td>
<td><p>false</p></td>
<td><p>Set or disable the STARTTLS encryption.</p></td>
</tr>
<tr class="odd">
<td><p>flowable.mail.server.username</p></td>
<td><p>flowable.mail-server-user-name</p></td>
<td><p>-</p></td>
<td><p>The username that needs to be used for the mail server authentication.
If empty no authentication would be used.</p></td>
</tr>
<tr class="even">
<td><p>flowable.process.definition-cache-limit</p></td>
<td><p>flowable.process-definitions.cache.max</p></td>
<td><p>-1</p></td>
<td><p>The maximum amount of process definitions available in the process definition cache.
Per default it is -1 (all process definitions)</p></td>
</tr>
</tbody>
</table>

## Flowable Auto-configuration classes

Here is a list of all auto-configuration classes provided by Flowable, with links to documentation and source code.
Remember to also look at the conditions report in your application for more details of which features are switched on.
(To do so, start the app with --debug or -Ddebug or, in an Actuator application, use the conditions endpoint).

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>Configuration Class</th>
</tr>
</thead>
<tbody>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/content/ContentEngineAutoConfiguration.java">ContentEngineAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/content/ContentEngineServicesAutoConfiguration.java">ContentEngineServicesAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/cmmn/CmmnEngineAutoConfiguration.java">CmmnEngineAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/cmmn/CmmnEngineServicesAutoConfiguration.java">CmmnEngineServicesAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/dmn/DmnEngineAutoConfiguration.java">DmnEngineAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/dmn/DmnEngineServicesAutoConfiguration.java">DmnEngineServicesAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/EndpointAutoConfiguration.java">EndpointAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/actuate/info/FlowableInfoAutoConfiguration.java">FlowableInfoAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/ldap/FlowableLdapAutoConfiguration.java">FlowableLdapAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/FlowableTransactionAutoConfiguration.java">FlowableTransactionAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/form/FormEngineAutoConfiguration.java">FormEngineAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/form/FormEngineServicesAutoConfiguration.java">FormEngineServicesAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/idm/IdmEngineAutoConfiguration.java">IdmEngineAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/idm/IdmEngineServicesAutoConfiguration.java">IdmEngineServicesAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/ProcessEngineAutoConfiguration.java">ProcessEngineAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/RestApiAutoConfiguration.java">RestApiAutoConfiguration</a></p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/SecurityAutoConfiguration.java">SecurityAutoConfiguration</a></p></td>
</tr>
</tbody>
</table>

## Flowable Starters

Here is a list of the flowable spring boot starters.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Starter</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter-cmmn/pom.xml">flowable-spring-boot-starter-cmmn</a></p></td>
<td><p>Contains the dependencies for booting the CMMN Engine in Standalone mode</p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter-cmmn-rest/pom.xml">flowable-spring-boot-starter-cmmn-rest</a></p></td>
<td><p>Contains the dependencies for booting the CMMN Engine in Standalone mode and starts its REST API</p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter-dmn/pom.xml">flowable-spring-boot-starter-dmn</a></p></td>
<td><p>Contains the dependencies for booting the DMN Engine in Standalone mode</p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter-dmn-rest/pom.xml">flowable-spring-boot-starter-dmn-rest</a></p></td>
<td><p>Contains the dependencies for booting the DMN Engine in Standalone mode and starts its REST API</p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter-process/pom.xml">flowable-spring-boot-starter-process</a></p></td>
<td><p>Contains the dependencies for booting the Process Engine in Standalone mode</p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter-process-rest/pom.xml">flowable-spring-boot-starter-process-rest</a></p></td>
<td><p>Contains the dependencies for booting the Process Engine in Standalone mode and starts its REST API</p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter/pom.xml">flowable-spring-boot-starter</a></p></td>
<td><p>Contains the dependencies for booting all Flowable Engines (Process, CMMN, DMN, Form, Content and IDM)</p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter-rest/pom.xml">flowable-spring-boot-starter-rest</a></p></td>
<td><p>Contains the dependencies for booting all Flowable Engines and their respective REST API</p></td>
</tr>
<tr>
<td><p><a href="https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-starter-actuator/pom.xml">flowable-spring-boot-starter-actuator</a></p></td>
<td><p>Contains the required dependencies for Spring Boot</p></td>
</tr>
</tbody>
</table>

## Using Liquibase

The Flowable engines are using Liquibase to manage the versioning of it’s tables.
This means that the `LiquibaseAutoConfiguration` from Spring Boot would automatically kick in.
However, if you are not using Liquibase then the application will not start and would throw an exception.
For this reason Flowable is setting `spring.liquibase.enabled` to `false`, which means that if you need to use Liquibase you have to explicitly enable it.

## Further Reading

Obviously, there is a lot about Spring Boot that hasn’t been touched upon yet, like very easy JTA integration or building a WAR file that can be run on major application servers. And there is a lot more to the Spring Boot integration:

-   Actuator support

-   Spring Integration support

-   Rest API integration: boot up the Flowable Rest API embedded within the Spring application

-   Spring Security support

## Advanced Configuration

### Customizing Engine Configuration

It’s possible to get a hold of the engine configuration by implementing the *org.flowable.spring.boot.EngineConfigurationConfigurer&lt;T&gt;* interface.
Where *T* is the Spring Type of the particular Engine Configuration.
This can be useful for advanced configuration settings or simply because a property has not been exposed (yet).
For example:

    public class MyConfigurer implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

        public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
            // advanced configuration
        }

    }

By exposing an instance of this class as an *@Bean* in the Spring Boot configuration, the instance will be called before the process engine is fully created.

> **Tip**
>
> You can provide a custom implementation of a Flowable Service by using this. See https://github.com/flowable/flowable-engine/tree/master/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/ldap/FlowableLdapAutoConfiguration.java\[FlowableLdapAutoConfiguration\]

### Combining starters

In case you need only a combination of the engines then you can add only the required dependencies.
For example to use the Process, CMMN, Form and IDM engine and use LDAP you need to add the following dependencies:

    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-spring-boot-starter-process</artifactId>
        <version>${flowable.version}</version>
    </dependency>
    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-spring-boot-starter-cmmn</artifactId>
        <version>${flowable.version}</version>
    </dependency>
    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-content-spring-configurator</artifactId>
        <version>${flowable.version}</version>
    </dependency>
    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-form-spring-configurator</artifactId>
        <version>${flowable.version}</version>
    </dependency>
    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-ldap</artifactId>
        <version>${flowable.version}</version>
    </dependency>

### Configuring Async Executors

The Process and CMMN engines have dedicated `AsyncExecutor`(s) and they can be configured with the `flowable.{engine}.async.executor` property group.
Where `engine` is either `process` or `cmmn`.

The `AsyncExecutor`(s) per default share the same Spring `TaskExecutor` and `SpringRejectedJobsHandler`.
In case you want to provide a dedicated executor for each of the engines you need define a qualified bean with `@Process` and `@Cmmn`.

You can configure custom executors in the following way:

    @Configuration
    public class MyConfiguration {

        @Process 
        @Bean
        public TaskExecutor processTaskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }

        @Cmmn 
        @Bean
        public TaskExecutor cmmnTaskExecutor() {
            return new SyncTaskExecutor();
        }
    }

-   The Async Executor for the Process Engine would use a `SimpleAsyncTaskExecutor`

-   The Async Executor for the CMMN Engine would use a `SyncTaskExecutor`

> **Important**
>
> If you define a custom `TaskExecutor` bean the Flowable creation of the bean is not triggered.
> Which means that if you define a bean qualified with `@Process` you have to define one with `@Cmmn` or `@Primary`, otherwise the Cmmn Async Executor will use the one for the Process
