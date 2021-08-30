---
id: ch06-cmmn
title: CMMN 1.1
---

## What is CMMN?

The Case Management Model and Notation (CMMN) is a standard notation and formal specification by the [Object Management Group](http://www.omg.org/spec/CMMN/) for representing case models.

Flowable contains:

-   A CMMN 1.1 modeler to create CMMN 1.1 case models

-   A Java engine that can import and execute CMMN 1.1 case models

-   A demonstration UI that executes the case models, allowing users to see and complete human tasks (and their forms)

## Basic concepts and terminology

The following figure shows a simple CMMN 1.1 diagram:

![cmmn basic concepts](assets/cmmn/cmmn-basic-concepts.png)

A **case model** is always visualized as some sort of *folder* that contains all the case elements. Every case model contains a **plan model** onto which items will be *planned*.

The elements of a plan model are called **plan items**. Each *plan item* has a **plan item definition** that gives its type and possible configuration options at runtime. For example, in the figure above, there are three **human task** plan items and one **milestone**. Other examples of plan items are *process tasks, case tasks and stages*.

After having deployed a case model to the Flowable CMMN engine, it’s possible to start **case instances** based on this case model. The plan items defined in the case model similarly have **plan item instance** runtime representations that are exposed by, and can be queried using, the Flowable API. Plan item instances have a state lifecycle that is defined in the CMMN 1.1 specification and is core to how the engine works. Please check out section 8.4.2 of the CMMN 1.1 specification for all the details.

Plan items can have *sentries*: a plan item is said to have *entry criteria* when a sentry "guards" its activation. These criteria specify conditions that must be satisfied to *trigger the sentry*. For example, in the figure above, the "Milestone One" plan item is *available* after a case instance is started, but it is *activated* (in CMMN 1.1 specification terminology: it moves from the *available* state to the *active* state) when both human task A and B are completed. Note that sentries can have complex expression in their *if part*, which are not visualized, allowing for much more complex functionality. Also note that there can be multiple sentries, although only one needs to be satisfied to trigger a state transition.

Plan items and the plan model can also have sentries with *exit criteria*, which specify conditions that trigger an *exit* from that particular plan item. In the figure above, the whole plan model is exited (as are all the child elements that are active at that moment), when human task C completes.

CMMN 1.1 defines a standard XML format in an XSD that is part of the specification. For information, the example in the figure above is represented in XML as shown below.

Some observations:

-   The four plan items above are in the XML and they reference their definition with a *definitionRef*. The actual definitions are at the bottom of the *casePlanModel* element

-   The plan items have criteria (entry or exit) that reference a *sentry* (not the other way around)

-   The XML also contains information on how the diagram is visualized (x and y coordinates, widths and heights, and so on), which are omitted below. These elements are important when exchanging case models with other CMMN 1.1 modeling tools to preserve the correct visual representation

<!-- -->

    <?xml version="1.0" encoding="UTF-8"?>
    <definitions xmlns="http://www.omg.org/spec/CMMN/20151109/MODEL"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xmlns:flowable="http://flowable.org/cmmn"
                 xmlns:cmmndi="http://www.omg.org/spec/CMMN/20151109/CMMNDI"
                 xmlns:dc="http://www.omg.org/spec/CMMN/20151109/DC"
                 xmlns:di="http://www.omg.org/spec/CMMN/20151109/DI"
                 targetNamespace="http://www.flowable.org/casedef">
      <case id="simpleExample" name="Simple Example">
        <casePlanModel id="casePlanModel" name="My Case">
          <planItem id="planItem1" name="Human task A"
                definitionRef="sid-88199E7C-7655-439C-810B-8849FC52D3EB"></planItem>
          <planItem id="planItem2" name="Milestone One"
                definitionRef="sid-8BF8A774-A8A7-4F1A-95CF-1E0D61EE5A47">
            <entryCriterion id="sid-62CC4A6D-B29B-4129-93EA-460253C45CDF"
                sentryRef="sentry1"></entryCriterion>
          </planItem>
          <planItem id="planItem3" name="Human task B"
                definitionRef="sid-A1FB8733-0DBC-4B38-9830-CBC4D0C4B802"></planItem>
          <planItem id="planItem4" name="Human task C"
                definitionRef="sid-D3970AFC-7391-4BA7-95BA-51C64D2F41E9"></planItem>
          <sentry id="sentry1">
            <planItemOnPart id="sentryOnPart1" sourceRef="planItem1">
              <standardEvent>complete</standardEvent>
            </planItemOnPart>
            <planItemOnPart id="sentryOnPart2" sourceRef="planItem3">
              <standardEvent>complete</standardEvent>
            </planItemOnPart>
          </sentry>
          <sentry id="sentry2">
            <planItemOnPart id="sentryOnPart3" sourceRef="planItem4">
              <standardEvent>complete</standardEvent>
            </planItemOnPart>
          </sentry>
          <humanTask id="sid-88199E7C-7655-439C-810B-8849FC52D3EB"
            name="Human task A"></humanTask>
          <milestone id="sid-8BF8A774-A8A7-4F1A-95CF-1E0D61EE5A47"
            name="Milestone One"></milestone>
          <humanTask id="sid-A1FB8733-0DBC-4B38-9830-CBC4D0C4B802"
            name="Human task B"></humanTask>
          <humanTask id="sid-D3970AFC-7391-4BA7-95BA-51C64D2F41E9"
            name="Human task C"></humanTask>
          <exitCriterion id="sid-422626DB-9B40-49D8-955E-641AB96A5BFA"
            sentryRef="sentry2"></exitCriterion>
        </casePlanModel>
      </case>
      <cmmndi:CMMNDI>
        <cmmndi:CMMNDiagram id="CMMNDiagram_simpleExample">
            ...
        </cmmndi:CMMNDiagram>
      </cmmndi:CMMNDI>
    </definitions>

## Programmatic example

In this section we’re going to build a simple case model and execute it programmatically through the Java APIs of the Flowable CMMN engine in a simple command line example.

The case model we’ll build is a simplified *employee onboarding* case with two stages: a phase before and phase after the potential employee has started. In the first stage, someone from the HR department will complete the tasks, while in the second stage it is the employee completing them. Also, at any point in time, the potential employee can reject the job and stop the whole case instance.

Note that only stages and human tasks are used. In a real case model, there will most likely other plan item types too, such as milestones, nested stages, automated tasks, and so on.

![cmmn.programmatic.example](assets/cmmn/cmmn.programmatic.example.png)

The XML for this case model is the following:

    <?xml version="1.0" encoding="UTF-8"?>
    <definitions xmlns="http://www.omg.org/spec/CMMN/20151109/MODEL"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xmlns:flowable="http://flowable.org/cmmn"
                 xmlns:cmmndi="http://www.omg.org/spec/CMMN/20151109/CMMNDI"
                 xmlns:dc="http://www.omg.org/spec/CMMN/20151109/DC"
                 xmlns:di="http://www.omg.org/spec/CMMN/20151109/DI"
                 targetNamespace="http://www.flowable.org/casedef">
      <case id="employeeOnboarding" name="Simple Example">
        <casePlanModel id="casePlanModel" name="My Case">
          <planItem id="planItem5" name="Prior to starting"
                definitionRef="sid-025D29E8-BA9B-403D-A684-8C5B52185642"></planItem>
          <planItem id="planItem8" name="After starting"
                definitionRef="sid-8459EF32-4F4C-4E9B-A6E9-87FDC2299044">
            <entryCriterion id="sid-50B5F12D-FE75-4D05-9148-86574EE6C073"
                sentryRef="sentry2"></entryCriterion>
          </planItem>
          <planItem id="planItem9" name="Reject job"
                definitionRef="sid-134E885A-3D58-417E-81E2-66A3E12334F9"></planItem>
          <sentry id="sentry2">
            <planItemOnPart id="sentryOnPart4" sourceRef="planItem5">
              <standardEvent>complete</standardEvent>
            </planItemOnPart>
          </sentry>
          <sentry id="sentry3">
            <planItemOnPart id="sentryOnPart5" sourceRef="planItem9">
              <standardEvent>complete</standardEvent>
            </planItemOnPart>
          </sentry>
          <stage id="sid-025D29E8-BA9B-403D-A684-8C5B52185642" name="Prior to starting">
            <planItem id="planItem1" name="Create email address"
                    definitionRef="sid-EA434DDD-E1BE-4AC1-8520-B19ACE8782D2"></planItem>
            <planItem id="planItem2" name="Allocate office"
                    definitionRef="sid-505BA223-131A-4EF0-ABAD-485AEB0F2C96"></planItem>
            <planItem id="planItem3" name="Send joining letter to candidate"
                    definitionRef="sid-D28DBAD5-0F5F-45F4-8553-3381199AC45F">
              <entryCriterion id="sid-4D88C79D-8E31-4246-9541-A4F6A5720AC8"
                sentryRef="sentry1"></entryCriterion>
            </planItem>
            <planItem id="planItem4" name="Agree start date"
                    definitionRef="sid-97A72C46-C0AD-477F-86DD-85EF643BB97D"></planItem>
            <sentry id="sentry1">
              <planItemOnPart id="sentryOnPart1" sourceRef="planItem1">
                <standardEvent>complete</standardEvent>
              </planItemOnPart>
              <planItemOnPart id="sentryOnPart2" sourceRef="planItem2">
                <standardEvent>complete</standardEvent>
              </planItemOnPart>
              <planItemOnPart id="sentryOnPart3" sourceRef="planItem4">
                <standardEvent>complete</standardEvent>
              </planItemOnPart>
            </sentry>
            <humanTask id="sid-EA434DDD-E1BE-4AC1-8520-B19ACE8782D2"
                name="Create email address"
                flowable:candidateGroups="hr"></humanTask>
            <humanTask id="sid-505BA223-131A-4EF0-ABAD-485AEB0F2C96"
                name="Allocate office"
                flowable:candidateGroups="hr"></humanTask>
            <humanTask id="sid-D28DBAD5-0F5F-45F4-8553-3381199AC45F"
                name="Send joining letter to candidate"
                flowable:candidateGroups="hr"></humanTask>
            <humanTask id="sid-97A72C46-C0AD-477F-86DD-85EF643BB97D"
                name="Agree start date"
                flowable:candidateGroups="hr"></humanTask>
          </stage>
          <stage id="sid-8459EF32-4F4C-4E9B-A6E9-87FDC2299044"
            name="After starting">
            <planItem id="planItem6" name="New starter training"
                    definitionRef="sid-DF7B9582-11A6-40B4-B7E5-EC7AC6029387"></planItem>
            <planItem id="planItem7" name="Fill in paperwork"
                    definitionRef="sid-7BF2B421-7FA0-479D-A8BD-C22EBD09F599"></planItem>
            <humanTask id="sid-DF7B9582-11A6-40B4-B7E5-EC7AC6029387"
                name="New starter training"
                flowable:assignee="${potentialEmployee}"></humanTask>
            <humanTask id="sid-7BF2B421-7FA0-479D-A8BD-C22EBD09F599"
                name="Fill in paperwork"
                flowable:assignee="${potentialEmployee}"></humanTask>
          </stage>
          <humanTask id="sid-134E885A-3D58-417E-81E2-66A3E12334F9" name="Reject job"
            flowable:assignee="${potentialEmployee}"></humanTask>
          <exitCriterion id="sid-18277F30-E146-4B3E-B3C9-3F1E187EC7A8"
            sentryRef="sentry3"></exitCriterion>
        </casePlanModel>
      </case>
    </definitions>

First of all, create a new project and add the *flowable-cmmn-engine* dependency (here shown for Maven). The H2 dependency is also added, as H2 will be used as embedded database later on.

    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-cmmn-engine</artifactId>
        <version>${flowable.version}</version>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <version>${h2.version}</version>
    </dependency>

The Flowable CMMN API is designed to be consistent with the other Flowable APIs and concepts. As such, people that know the BPMN or DMN APIs will have no problem finding their way around. As with the other engines, the first line of code is creating a CmmnEngine. Here, the default in-memory configuration is used, which uses H2 as the database:

    public class Main {
      public static void main(String[] args) {
        CmmnEngine cmmnEngine
            = new StandaloneInMemCmmnEngineConfiguration().buildCmmnEngine();
      }
    }

Note that the *CmmnEngineConfiguration* exposes many configuration options for tweaking various settings of the CMMN engine.

Put the XML from above in a file, for example *my-case.cmmn* (or .cmmn.xml). For Maven, it should be placed in the *src/main/resources* folder.

To make the engine aware of the case model, it first needs to be *deployed*. This is done through the *CmmnRepositoryService*:

    CmmnRepositoryService cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
    CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
        .addClasspathResource("my-case.cmmn")
        .deploy();

Deploying the XML will return a **CmmnDeployment**. A deployment can contain many case models and artifacts. The specific case model definition above is stored as a **CaseDefinition**. This can be verified by doing a *CaseDefinitionQuery*:

    List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().list();
    System.out.println("Found " + caseDefinitions.size() + " case definitions");

Having a **CaseDefinition** in the engine, it’s now possible to start a **CaseInstance** for this case model definition. Either the result from the query is used and passed into the following snippet of code, or the *key* of the case definition is used directly (as done below).

Note that we’re also passing data, an identifier to the *potentialEmployee* as a variable when starting the **CaseInstance**. This variable will later be used in the human tasks to assign the task to the correct person (see the *assignee="${potentialEmployee}"* attribute on *human tasks*).

    CmmnRuntimeService cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
    CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
        .caseDefinitionKey("employeeOnboarding")
        .variable("potentialEmployee", "johnDoe")
        .start();

After the **CaseInstance** is started, the engine will determine which of the plan items of the model should be activated:

-   The first stage has no entry criteria, so it’s activated

-   The child human tasks of the first stage have no entry criteria, so three of them are expected to be active

The plan items are represented at runtime by **PlanItemInstances** and can be queried through the *CmmnRuntimeService*:

    List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
        .caseInstanceId(caseInstance.getId())
        .orderByName().asc()
        .list();

    for (PlanItemInstance planItemInstance : planItemInstances) {
        System.out.println(planItemInstance.getName());
    }

which prints out

    After starting
    Agree start date
    Allocate office
    Create email address
    Prior to starting
    Reject job
    Send joining letter to candidate

Some things might be unexpected here:

-   The stages are *also* plan items and thus have a representation as **PlanItemInstance**. Note that that child plan item instances will have the stage as parent when calling *.getStageInstanceId()*.

-   The *Send joining letter to candidate* is returned in the result. The reason is that, in accordance with the CMMN 1.1 specification, this plan item instance is in the *available* state, but not yet in the *active* state.

Indeed, when the code above is changed to

    for (PlanItemInstance planItemInstance : planItemInstances) {
        System.out.println(planItemInstance.getName()
            + ", state=" + planItemInstance.getState()
            + ", parent stage=" + planItemInstance.getStageInstanceId());
    }

The output now becomes:

    After starting, state=available, parent stage=null
    Agree start date, state=active, parent stage=fe37ac97-b016-11e7-b3ad-acde48001122
    Allocate office, state=active, parent stage=fe37ac97-b016-11e7-b3ad-acde48001122
    Create email address, state=active, parent stage=fe37ac97-b016-11e7-b3ad-acde48001122
    Prior to starting, state=active, parent stage=null
    Reject job, state=active, parent stage=fe37ac97-b016-11e7-b3ad-acde48001122
    Send joining letter to candidate, state=available, parent stage=fe37ac97-b016-11e7-b3ad-acde48001122

To only show the active plan item instances, the query can be adapted by adding *planItemInstanceStateActive()*:

     List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
        .caseInstanceId(caseInstance.getId())
        .planItemInstanceStateActive()
        .orderByName().asc()
        .list();

The output is now

    Agree start date
    Allocate office
    Create email address
    Prior to starting
    Reject job

Of course, the **PlanItemInstance** is the low level representation, but each plan item also has a *plan item definition* that defines what type it is. In this case, we only have *human tasks*. It is possible to interact with the *CaseInstance* via its *plan item instances*, for example by triggering them programmatically (for example, *CmmnRuntimeService.triggerPlanItemInstance(String planItemInstanceId)*). However, most likely the interaction will happen through the results of the actual plan item definition: here, the human tasks.

Querying for tasks is done in the exact same way as for the BPMN engine (in fact, the task service is a shared component and tasks created in BPMN or CMMN can be queried through both engines):

    CmmnTaskService cmmnTaskService = cmmnEngine.getCmmnTaskService();
    List<Task> hrTasks = cmmnTaskService.createTaskQuery()
        .taskCandidateGroup("hr")
        .caseInstanceId(caseInstance.getId())
        .orderByTaskName().asc()
        .list();
    for (Task task : hrTasks) {
        System.out.println("Task for HR : " + task.getName());
    }

    List<Task> employeeTasks = cmmnTaskService.createTaskQuery()
        .taskAssignee("johndoe")
        .orderByTaskName().asc()
        .list();
    for (Task task : employeeTasks) {
        System.out.println("Task for employee: " + task);
    }

Which outputs:

    Task for HR : Agree start date
    Task for HR : Allocate office
    Task for HR : Create email address

    Task for employee: Reject job

When the three tasks of HR are completed, the 'Send joining letter to candidate' task should be available:

    for (Task task : hrTasks) {
        cmmnTaskService.complete(task.getId());
    }

    hrTasks = cmmnTaskService.createTaskQuery()
        .taskCandidateGroup("hr")
        .caseInstanceId(caseInstance.getId())
        .orderByTaskName().asc()
        .list();

    for (Task task : hrTasks) {
        System.out.println("Task for HR : " + task.getName());
    }

And indeed, the expected task is now created:

    Task for HR : Send joining letter to candidate

Completing this task will now move the case instance into the second stage, as the sentry for the first stage is satisfied. The 'Reject job' tasks is automatically completed by the system and the two tasks for the employee are created:

    Task for employee: Fill in paperwork
    Task for employee: New starter training
    Task for employee: Reject job

Completing all the tasks will end the case instance:

    List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).listPage(0, 1);
    while (!tasks.isEmpty()) {
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery()
            .caseInstanceId(caseInstance.getId())
            .listPage(0, 1);
    }

While executing case instances, the engine also stores historic information, which can be queried via a query API:

    CmmnHistoryService cmmnHistoryService = cmmnEngine.getCmmnHistoryService();
    HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
        .caseInstanceId(caseInstance.getId())
        .singleResult();

    System.out.println("Case instance execution took "
        + (historicCaseInstance.getEndTime().getTime() - historicCaseInstance.getStartTime().getTime()) + " ms");

    List<HistoricTaskInstance> historicTaskInstances = cmmnHistoryService.createHistoricTaskInstanceQuery()
        .caseInstanceId(caseInstance.getId())
        .orderByTaskCreateTime().asc()
        .list();

    for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
        System.out.println("Task completed: " + historicTaskInstance.getName());
    }

Which outputs:

    Case instance execution took 149 ms
    Task completed: Reject job
    Task completed: Agree start date
    Task completed: Allocate office
    Task completed: Create email address
    Task completed: Send joining letter to candidate
    Task completed: New starter training
    Task completed: Fill in paperwork

Historic data related to the case execution is collected for special constructs, such as Tasks (as seen above), milestones, cases, variables and plan items in general.
This data is persisted at the same time as the runtime data, but it is not deleted when case instances end.
Access to the historic data is provided as query APIs by the *CmmnHistoryService*

Of course, this is but a small part of the available APIs and constructs available in the Flowable CMMN Engine. Please check the other sections for more detailed information

## CMMN 1.1 Constructs

This chapter covers the CMMN 1.1 constructs supported by Flowable, as well as extensions to the CMMN 1.1 standard.

The following constructs, with the exception of sentries and item control, as for the CMMN specification are considered plan items.
Historic data of their instances execution can be queried through the *CmmnHistoryService* using *org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery*.

### Stage

A stage is used to group plan items together. It is typically used to define "phases" in a case instance.

A stage is a plan item itself, and thus can have entry and exit criteria. Plan items contained within a stage are only available when the parent stage moves to the *active* state. Stages can be nested in other stages.

A stage is visualized as a rectangle with angled corners:

![cmmn.stage](assets/cmmn/cmmn.stage.png)

### Task

A "manual" task, meaning the task will happen external to the engine.

Properties:

-   **name**: expression that will be resolved at runtime as the name of the manual task

-   **blocking**: a boolean value determining whether the task blocks

-   **blockingExpression**: an expression that evaluates to a boolean indicating whether the tasks blocks

If a task is non-blocking, the engine will simply complete it automatically when executing it. If a task is blocking, a *PlanItemInstance* for this task will remain in the *active* state until it is programmatically triggered by the *CmmnRuntimeService.triggerPlanItemInstance(String planItemInstanceId)* method.

A task is visualized as a rounded rectangle:

![cmmn.task](assets/cmmn/cmmn.task.png)

### Human task

A human task is used to model work that needs to be done by a human, typically through a form. When the engine arrives at a human task, a new entry is created in the task list of any users or groups assigned to that task.

A human task is a plan item, which means that beyond a human task entry also a *PlanItemInstance* is created and it can be queried via the *PlanItemInstanceQuery*.

Human tasks can be queried through the *org.flowable.task.api.TaskQuery* API. Historic task data can be queried through the *org.flowable.task.api.history.HistoricTaskInstanceQuery*.

Properties:

-   **name**: expression that will be resolved at runtime as the name of the human task

-   **blocking**: a boolean value determining whether the task blocks

-   **blockingExpression**: an expression that evaluates to a boolean indicating whether the tasks blocks

-   **assignee** : an expression (can be a static text value) that is used to determine to whom the human task is assigned

-   **owner** : an expression (can be a static text value) that is used to determine who is the owner of the human task

-   **candidateUsers** : an expression (can be a static text value) that resolves to a comma-separated list of Strings that is used to determine which users are candidate for this human task

-   **candidateGroups** : an expression (can be a static text value) that resolves to a comma-separated list of Strings that is used to determine to which groups the task is assigned

-   **form key**: an expression that determines a key when using forms. Can be retrieved via the API afterwards

-   **Due date** an expression that resolves to java.util.Date or a ISO-8601 date string

-   **Priority**: an expression that resolves to an integer. Can be used in the TaskQuery API to filter tasks

A human task is visualized as a rounded rectangle with a user icon in the top left corner:

![cmmn.humantask](assets/cmmn/cmmn.humantask.png)

### Java Service task

A service task is used to execute custom logic.

Custom logic is placed in a class that implements the *org.flowable.cmmn.api.delegate.PlanItemJavaDelegate* interface.

    public class MyJavaDelegate implements PlanItemJavaDelegate {

        public void execute(DelegatePlanItemInstance planItemInstance) {
            String value = (String) planItemInstance.getVariable("someVariable");
            ...
        }

    }

For lower-level implementations that cannot be covered by using the *PlanItemJavaDelegate* approach, the *CmmnActivityBehavior* can be used (similar to *JavaDelegate* vs *ActivityBehavior* in the BPMN engine).

Properties:

-   **name**: name of the service task

-   **class**: the Java class implementing the custom logic

-   **class fields**: parameters to pass when calling the custom logic

-   **Delegate expression**: an expression that resolves to a class implementing the *PlanItemJavaDelegate* interface

A service task is visualized as a rounded rectangle with a cog icon in the top left corner:

![cmmn.servicetask](assets/cmmn/cmmn.servicetask.png)

### External Worker Task

#### Description

The External Worker Task allows you to create jobs that should be acquired and executed by External Workers.
An External Worker can acquire jobs over the Java API or REST API.
This is similar to an async Service Task.
The difference is that instead of Flowable executing the logic,
an External Worker, which can be implemented in any language, queries Flowable for jobs, executes them and sends the result to Flowable.
Note that the External Worker task is **not** an 'official' task of CMMN spec (and doesn’t have a dedicated icon as a consequence).

#### Defining an External Worker Task

The External Worker task is implemented as a dedicated [Task](cmmn/ch06-cmmn.md#task) and is defined by setting *'external-worker'* for the *type* of the task.

```xml
<task id="externalWorkerOrder" flowable:type="external-worker">
```

The External Worker task is configured by setting the `topic` (can be an EL expression) which the External Worker uses to query for jobs to execute.

#### Example usage

The following XML snippet shows an example of using the External Worker Task.
The External Worker is a wait state.
When the execution reaches the task it will create an External Worker Job, which can be acquired by an External Worker.
Once the External Worker is done with the job and notifies Flowable of the completion the execution of the case will continue.


```xml
<task id="externalWorkerOrder" flowable:type="external-worker" flowable:topic="orderService" />
```

#### Acquiring External Worker Job

External Worker Jobs are acquired via the `CmmnManagementService#createExternalWorkerJobAcquireBuilder` by using a `ExternalWorkerJobAcquireBuilder`

```java
List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(30))
                .acquireAndLock(5, "orderWorker-1");
```

By using the above Java snippet External Worker jobs can be acquired.
With the snippet we did the following:

* Query for External Worker Jobs with the topic *orderService*.
* Acquire and lock the jobs for 30 minutes waiting for the completion signal from the External Worker.
* Acquire maximum of 5 jobs
* The owner of the jobs is the worker with id *orderWorker-1*

An `AcquiredExternalWorkerJob` also has access to the Case variables.
When the External Worker Task is exclusive, acquiring the job will lock the Case Instance.

#### Completing an External Worker Job

External Worker Jobs are completed via the `CmmnManagementService#createCmmnExternalWorkerTransitionBuilder(String, String)` by using a `CmmnExternalWorkerTransitionBuilder`

```java
cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(acquiredJob.getId(), "orderWorker-1")
                .variable("orderStatus", "COMPLETED")
                .complete();
```

A Job can be completed only by the worker that acquired it. Otherwise a `FlowableIllegalArgumentException` will be thrown.

Using the snippet above the task is completed and the case execution will continue.
The continuation of the execution is done asynchronously in a new transaction.
This means that completing an external worker task will only create an asynchronous (new) job to execute the completion (and the current thread returns after doing that).
Any steps in the model that follow after the external worker task will be executed in that transaction, similar to a regular async service task.

It is also possible to use `CmmnExternalWorkerTransitionBuilder#terminate()` to transition the external worker job.

#### Error handling for an External Worker Job

The `ExternalWorkerJobFailureBuilder` is used to fail a job (schedule it for a new execution in the future)

In order to fail a job the following can be used:


```java
cmmnManagementService.createExternalWorkerJobFailureBuilder(acquiredJob.getId(), "orderWorker-1")
                .errorMessage("Failed to run job. Database not accessible")
                .errorDetails("Some complex and long error details")
                .retries(4)
                .retryTimeout(Duration.ofHours(1))
                .fail();
```

With this snippet the following will be done:

* The error message and error details will be set on the job
* The retry count for the job will be set to 4
* The job will be available for acquiring after 1 hour

The Job can only be failed by the worker that acquired it.
If no retries have been set, flowable will automatically decrease the number of retries for a job by 1.
When the number of retries is 0 the job will be moved to the DeadLetter table job and will no longer be available for acquiring.

#### Querying External Worker Jobs

External Worker Jobs are queried by using the `ExternalWorkerJobQuery` by creating it via `CmmnManagementService#createExternalWorkerJobQuery`.

### Decision task

A *Decision task* calls out to a DMN decision table and stores the resulting variable in the case instance.

Properties:

-   **Decision table reference**: the referenced DMN decision table that needs to be invoked.

It is also possible to throw an error when no rule is hit during the evaluation of the DMN decision table by setting the '*Throw error if no rules were hit*' property.

A decision task is visualized as a *task* with a table icon in the top left corner:

![cmmn.decisiontask](assets/cmmn/cmmn.decisiontask.png)

### Http Task

The Http task is an out-of-the-box implementation of a *service task*. It is used when a REST service needs to be called over HTTP.

The Http task has various options to customize the request and response. See the BPMN http task documentation for details on all the configuration options.

A http task is visualized as a *task* with a rocket icon in the top left corner:

![cmmn.httptask](assets/cmmn/cmmn.httptask.png)

### Script Task

A task of type "script", similar to its equivalent in BPMN, the Script Task executes a script when the plan item instance becomes active.

Properties:

-   **name**: task attribute to indicate the name of the task

-   **type**: task attribute whose value must be "script" to indicate the type of task

-   **scriptFormat**: extended attribute that indicate the language of the script (for example, javascript, groovy)

-   **script**: the script to execute, defined as a string in a field element named "script"

-   **autoStoreVariables**: optional task attribute flag (default: false) that indicates whether or not variables defined in the script will be stored in the Plan Item Instance context (see note below)

-   **resultVariableName**: optional task attribute that when present will store a variable with the specified name in the Plan Item instance context with the script evaluation result (see note below)

A script task is visualized as a *task* with a script icon in the top left corner:

![cmmn.scripttask](assets/cmmn/cmmn.scripttask.png)

    <planItem id="scriptPlanItem" name="Script Plan Item" definitionRef="myScriptTask" />
    <task name="My Script Task Item" flowable:type="script" flowable:scriptFormat="JavaScript">
        <documentation>Optional documentation</documentation>
        <extensionElements>
            <flowable:field name="script">
                <string>
                    sum = 0;
                    for ( i in inputArray ) {
                        sum += i;
                    }
                </string>
            </flowable:field>
        </extensionElements>
    </task>

**Note**: The value of the **scriptFormat** attribute must be a name that is compatible with the [JSR-223](http://jcp.org/en/jsr/detail?id=223) (scripting for the Java platform). By default, JavaScript is included in every JDK and as such doesn’t need any additional JAR files. If you want to use another (JSR-223 compatible) scripting engine, it is sufficient to add the corresponding JAR to the classpath and use the appropriate name. For example, the Flowable unit tests often use Groovy because the syntax is similar to that of Java.

Do note that the Groovy scripting engine is bundled with the groovy-jsr223 JAR. As such, one must add the following dependency:

    <dependency>
        <groupId>org.codehaus.groovy</groupId>
        <artifactId>groovy-jsr223</artifactId>
        <version>2.x.x<version>
    </dependency>

All case variables that are accessible through the PlanItem instance that arrives in the script task can be used within the script. In the example below, the script variable *'inputArray'* is in fact a case variable (an array of integers).

    <flowable:field name="script">
        <string>
        sum = 0
        for ( i in inputArray ) {
          sum += i
        }
        </string>
    </flowable:field>

**Note**: It’s also possible to set plan item instance variables in a script, simply by calling *planItemInstance.setVariable("variableName", variableValue)*. By default, no variables are stored automatically. It’s possible to automatically store any variable defined in the script (for example, *sum* in the example above) by setting the property autoStoreVariables on the scriptTask to true. However, **the best practice is not to do this and use an explicit planItemInstance.setVariable() call**, as with some recent versions of the JDK, auto storing of variables does not work for some scripting languages. See [this link](http://www.jorambarrez.be/blog/2013/03/25/bug-on-jdk-1-7-0_17-when-using-scripttask-in-activiti/) for more details.

    <task name="Script Task" flowable:type="script" flowable:scriptFormat="groovy" flowable:autoStoreVariables="false">

The default for this parameter is false, meaning that if the parameter is omitted from the script task definition, all the declared variables will only exist during the duration of the script.

Here’s an example of how to set a variable in a script:

    <flowable:field name="script">
        <string>
        def scriptVar = "test123"
        planItemInstance.setVariable("myVar", scriptVar)
        </string>
    </flowable:field>

The following names are reserved and **cannot be used** as variable names: **out, out:print, lang:import, context, elcontext**.

**Note** The return value of a script task can be assigned to an already existing or new plan item instance variable by specifying its name as a literal value for the *'flowable:resultVariable'* attribute of a script task definition. Any existing value for a specific plan item instance variable will be overwritten by the result value of the script execution. When a result variable name is not specified, the script result value gets ignored.

    <task name="Script Task" flowable:type="script" flowable:scriptFormat="groovy" flowable:resultVariable="myVar">
        <flowable:field name="script">
            <string>#{echo}</string>
        </flowable:field>
    </task>

In the above example, the result of the script execution (the value of the resolved expression *'\#{echo}'*) is set to the process variable named *'myVar'* after the script completes.

### Milestone

A milestone is used to mark arriving at a certain point in the case instance. At runtime, they are represented as **MilestoneInstances** and they can be queried through the **MilestoneInstanceQuery** via the *CmmnRuntimeService*. There is also a historical counterpart via the *CmmnHistoryService*.

A milestone is a plan item, which means that as well as a milestone entry, a *PlanItemInstance* is created also, which can be queried via the *PlanItemInstanceQuery*.

Properties:

-   **name**: an expression or static text that determines the name of the milestone

A milestone is visualized as a rounded rectangle (more rounded than a task):

![cmmn.milestone](assets/cmmn/cmmn.milestone.png)

### Case task

A case task is used to start a child case within the context of another case. The *CaseInstanceQuery* has *parent* options to find these cases.

When the case task is blocking, the *PlanItemInstance* will be *active* until the child case has completely finished. If the case task is non-blocking, the child case is started and the plan item instance automatically completes. When the child case instance ends there is no impact on the parent case.

Properties:

-   **name**: an expression or static text that determines the name

-   **blocking**: a boolean value determining whether the task blocks

-   **blockingExpression**: an expression that evaluates to a boolean indicating whether the tasks blocks

-   **Case reference**: the key of the case definition that is used to start the child case instance. Can be an expression

A case task is visualized as a rounded rectangle with a case icon in the top left corner:

![cmmn.casetask](assets/cmmn/cmmn.casetask.png)

### Process task

A process task is used to start a process instance within the context of a case.

When the process task is blocking, the *PlanItemInstance* will be *active* until the process instance has completely finished. If the process task is non-blocking, the process instance is started and the plan item instance automatically completes. When the process instance ends there is no impact on the parent case.

Properties:

-   **name**: an expression or static text that determines the name

-   **blocking**: a boolean value determining whether the task blocks

-   **blockingExpression**: an expression that evaluates to a boolean indicating whether the tasks blocks

-   **Process reference**: the key of the process definition that is used to start the process instance. Can be an expression

A process task is visualized as a rounded rectangle with an arrow icon in the top left corner:

![cmmn.processtask](assets/cmmn/cmmn.processtask.png)

A process task can be configured to have in- and out parameters, which take the form of *source/sourceExpression* and *target/targetExpression*.

The in parameters are resolved within context of the case instance.

-   The *source* value will be the case instance variable which value will be mapped to a process variable

-   Alternatively, the *sourceExpression* allows to create an arbitrary value, where the expression is resolved against the case instance.

-   The *target* will be the name of the process variable to which the source value is mapped.

-   Alternatively, the *targetExpression* will resolve to a **string** value that is used as variable name in the process instance. The expression is resolved within case instance context.

The out parameters are resolved within context of the process instance.

-   The *source* value will be the process instance variable which value will be mapped to a case variable

-   Alternatively, the *sourceExpression* allows to create an arbitrary value, where the expression is resolved against the process instance.

-   The *target* will be the name of the case variable to which the source value is mapped.

-   Alternatively, the *targetExpression* will resolve to a **string** value that is used as variable name in the case instance. The expression is resolved within process instance context.

### Criteria

#### Entry criterion (entry sentry)

Entry criteria form a sentry for a given plan item instance. They consist of two parts:

-   One or more parts that depend on other plan items: these define dependencies on state transitions of other plan items. For example, one human task can depend on the state transition 'complete' of three other human tasks to become active itself

-   One optional *if part* or *condition*: this is an expression that allows the definition of a complex condition

A sentry is satisfied when all its criteria are resolved to *true*. When a criterion evaluates to true, this is stored and remembered for future evaluations. Note that entry criteria of all plan item instances in the *available* state are evaluated whenever something changes in the case instance.
Multiple sentries are possible on a plan item. However, when one is satisfied, the plan item moves from state *available* to *active*.

See [the section on sentry evaluation](cmmn/ch06-cmmn.md#sentry-evaluation) for more information.

An entry criterion is visualized as a diamond shape (white color inside) on the border of a plan item:

![cmmn.entrycriteria](assets/cmmn/cmmn.entrycriteria.png)

#### Exit criterion (exit sentry)

Exit criteria form a sentry for a given plan item instance. They consist of two parts:

-   One or more parts that depend on other plan items: these define dependencies on state transitions of other plan items. For example, one human task can depend on reaching a certain milestone to be automatically terminated

-   One optional *if part* or *condition*: this is an expression that allows a complex condition to be defined

A sentry is satisfied when all its criteria are resolved to *true*. When a criterion evaluates to true, this is stored and remembered for future evaluations. Note that exit criteria of all plan item instances in the *active* state are evaluated whenever something changes in the case instance.
Multiple sentries are possible on a plan item. However, when one is satisfied, the plan item moves from state *active* to *exit*.

See [the section on sentry evaluation](cmmn/ch06-cmmn.md#sentry-evaluation) for more information.

An exit criterion is visualized as a diamond shape (white color inside) on the border of a plan item:

![cmmn.exitcriteria](assets/cmmn/cmmn.exitcriteria.png)

Beyond the specification, Flowable supports additional attributes on an exit sentry which adds more flexibility and options on how a plan item is terminated when the exit sentry triggers.

##### exitType

This attribute can be used for exit sentries on plan items, not stages or the case plan model though, and helps define how to exit the plan item.
It particularly makes sense in combination with repetition. A possible use case might be that you want to terminate active instances of a repetitive plan item, but maybe later on it becomes available again as the conditions change in the case. With the exit type other than the default, this is possible as the plan item is not terminated for good, but only active or active and enabled instances.

Possible values are:

-   **default**: The default exit type works as the spec says, it will terminate (exit) the plan item and all not yet finished instances as well.

-   **activeInstances**: If this exit type is chosen, the exit sentry only terminates active instances, but leaves enabled, available ones in place, so they can become active later on.

-   **activeAndEnabledInstances**: In addition to the previous one, this exit type also terminates enabled instances (e.g. ready for manual activation), but leaves available ones in place.

Example of an extended exit sentry on a human task:

    <planItem id="planItem1" name="Task 1" definitionRef="humanTask1">
        <itemControl>
            <repetitionRule></repetitionRule>
        </itemControl>
        <exitCriterion id="exitCriterion1" sentryRef="sentry1" flowable:exitType="activeAndEnabledInstances"></exitCriterion>
    </planItem>

##### exitEventType

This attribute can be used for exit sentries on stages or the case plan model as it offers an alternative exit than terminate. Imagine a stage where you don’t want to put autocompletion on, but rather have a user listener becoming available whenever the stage is completable and let the user decide when the stage should actually complete by triggering an exit sentry on the stage.
Using this combination according to spec will exit the stage and leave it in terminated state and triggering the exit event for further processing.
Maybe not what you want. With the *exitEventType* you can specify how the stage is exiting other than the default behavior.

Possible values are:

-   **exit** This is the default behavior compliant to the spec. It will terminate the stage and all of its children and leave it *terminated* state, using *exit* as the event type being triggered.

-   **complete** This value can be used to terminate the stage, but leave it in *completed* state (instead of *terminated*) and trigger the *complete* event, instead of the *exit* one. Basically, this behavior is exactly the same as if the stage would have been auto-completed. The engine will throw an exception, if the stage is not completable at the moment the exit sentry with this exit event type is triggered.

-   **forceComplete** This value is similar to the *complete* one, but does not check the stage to be completable upfront, but forces it to complete, even if there are still active child plan items at the moment the exit sentry is triggered. They will be terminated first, then the stage completes with *complete* event and be left in *completed* state.

Full example on how to use the exit event type attribute in combination with a user listener to manually complete the stage.
It contains two important parts: the *flowable:exitEventType="complete"* attribute on the exit criterion and the *flowable:availableCondition="${cmmn:isStageCompletable()}"* on the user event listener which makes the listener only available, if the stage is currently completable, otherwise it is unavailable.
Once the user listener triggers, the exit sentry is executed and will complete the stage, not terminate it and leave it in *completed* state, triggering the *complete* event, not the *exit* event.

![cmmn exit sentry on stage](assets/cmmn/cmmn-exit-sentry-on-stage.png)

Here you find the CMMN model in XML:

    <?xml version="1.0" encoding="UTF-8"?>
    <definitions xmlns="http://www.omg.org/spec/CMMN/20151109/MODEL"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xmlns:flowable="http://flowable.org/cmmn"
                 xmlns:cmmndi="http://www.omg.org/spec/CMMN/20151109/CMMNDI"
                 xmlns:dc="http://www.omg.org/spec/CMMN/20151109/DC"
                 xmlns:di="http://www.omg.org/spec/CMMN/20151109/DI"
                 xmlns:design="http://flowable.org/design"
                 targetNamespace="http://flowable.org/cmmn">
      <case id="stageWithUserListenerForCompletion" name="Stage with user listener for completion">
        <casePlanModel id="casePlanModel1" name="Case plan model">
          <planItem id="planItem4" definitionRef="expandedStage1">
            <exitCriterion id="exitCriterion1" flowable:sentryRef="sentry1" flowable:exitEventType="complete"></exitCriterion>
          </planItem>
          <sentry id="sentry1">
            <planItemOnPart id="sentryOnPart1" sourceRef="planItem3">
              <standardEvent>occur</standardEvent>
            </planItemOnPart>
          </sentry>
          <stage id="expandedStage1" name="Stage A">
            <planItem id="planItem1" name="Task A" definitionRef="humanTask1"></planItem>
            <planItem id="planItem2" name="Task B" definitionRef="humanTask2">
              <itemControl>
                <repetitionRule></repetitionRule>
                <manualActivationRule></manualActivationRule>
              </itemControl>
            </planItem>
            <planItem id="planItem3" name="Complete stage" definitionRef="userEventListener1"></planItem>
            <humanTask id="humanTask1" name="Task A"></humanTask>
            <humanTask id="humanTask2" name="Task B"></humanTask>
            <userEventListener id="userEventListener1" name="Complete stage" flowable:availableCondition="${cmmn:isStageCompletable()}"></userEventListener>
          </stage>
        </casePlanModel>
      </case>
    </definitions>

### Event Listeners

#### Timer Event Listener

A timer event listener is used when the passing of time needs to be captured in a case model.

A timer event listener is not a task and has a simpler plan item lifecycle compared to a *task*: the timer will simply move from *available* to *completed* when the event (in this case, the time passing) occurs.

Properties:

-   **Timer expression**: an expression that defines when the timer should occur. The following options are possible:

    -   An expression resolving to a java.util.Date or org.joda.time.DateTime instance (for example, \_${someBean.calculateNextDate(someCaseInstanceVariable)})

    -   An ISO8601 date

    -   An ISO8601 duration String (for example, *PT5H*, indicating the timer should fire in 5 hours from instantiation)

    -   AN ISO8601 repetition String (for example, R5/PT2H, indicating the timer should fire 5 times, each time waiting 2 hours)

    -   A String containing a cron expression

-   **Start trigger plan item/event**: reference to a plan item in the case model that triggers the start of the timer event listener

Note that setting a *start trigger* for the timer event listener does not have a visual indicator in the case model, unlike entry/exit criteria on sentries.

A timer event listener is visualized as circle with a clock icon inside:

![cmmn.timereventlistener](assets/cmmn/cmmn.timereventlistener.png)

#### User Event Listener

A user event listener can be used when needing to capture a user interaction that directly influences a case state,
instead of indirectly via impacting variables or information in the case.
A typical use case for a user event listener are buttons in a UI that a user can click to drive the state of the case instance.
When the event is triggered an *Occur* event is thrown to which sentries can listener to.
Like timer event listeners, it has a much simpler lifecycle that a *task*.

![cmmn.usereventlistener](assets/cmmn/cmmn.usereventlistener.png)

User event listeners can be queried using the *org.flowable.cmmn.api.runtime.UserEventListenerInstanceQuery*. Such a query can be created by calling the *cmmnRuntimeService.createUserEventListenerInstanceQuery()* method. Note that a user event listener is also a plan item instance, which means it can also be queried through the *org.flowable.cmmn.api.runtime.PlanItemInstanceQuery* API.

A user event listener can be completed by calling the *cmmnRuntimeService.completeUserEventListenerInstance(id)* method.

#### Generic Event Listener

A generic event listener is used to typically model a programmatic interaction (e.g. a external system that calls out to change something in a case instance).

![cmmn.generic event listener](assets/cmmn/cmmn.generic-event-listener.png)

The API to retrieve and complete these event listeners is on the *CmmnRuntimeService*:

    GenericEventListenerInstanceQuery createGenericEventListenerInstanceQuery();
    void completeGenericEventListenerInstance(String genericEventListenerInstanceId);

Similar to *user event listeners*, this API is a wrapper on top of the *PlanItemInstance* queries and operations. This means that the data can also be retrieved through the regular *PlanItemInstanceQuery*

Note that generic event listeners are not part of the CMMN specification, but are a Flowable-specific addition.

#### Automatic removal of event listeners

The engine will automatically detect when event listeners (user or timer) are not useful anymore.
Take for example the following case definition:

![cmmn.user event listener removal 1](assets/cmmn/cmmn.user-event-listener-removal-1.png)

Here, the *First stage* contains two human tasks (A and B) and it can be exited by a user when the *Stop first stage* user event is triggered.
However, when both tasks A and B are completed, the stage will also complete. If now the user event listener would be triggered, there is nothing that listens to this event anymore.
The engine will detect this and terminate the user event automatically.

The same mechanism also works for event listeners that are referenced by entry sentries:

![cmmn.user event listener removal 2](assets/cmmn/cmmn.user-event-listener-removal-2.png)

In this case, in the case that *EventListenerA* would be triggered, *EventListenerB* is terminated (as nothing is listening to its occurrence anymore).

Or, when timer and user event listeners are mixed, the one that is triggered first will also cause the removal of others (when they are not referenced somewhere else):

![cmmn.user event listener removal 3](assets/cmmn/cmmn.user-event-listener-removal-3.png)

Here, the timer will be removed in case the user event is triggered first (and vice versa).

The detection also takes in account plan items that have not yet been created. Take for example the following case definition:

![cmmn.user event listener removal 4](assets/cmmn/cmmn.user-event-listener-removal-4.png)

Here, human task *C* is not yet created when a case instance is started for this case definition. The user event listener will not be removed as long that *C* has a parent stage that is in a non-terminal state, as this means that the event could still be listened to in the future.

#### Available condition

All types of event listeners can be configured to have a **available condition**: an expressions that will guard the available state of the event listener. To explain the use case, take the following case definition:

![cmmn.create condition](assets/cmmn/cmmn.create-condition.png)

When the case instance is started, Stage 1 (as it has no entry criteria) will be moving immediately from *available* to *active*. Similar story for human task A. Human task B will move from *available* to *enabled* as it’s manually activated.

Normally, also the event listener would become *available*. The life cycle of event listeners is simpler than that of plan items such as human tasks: an event listener stays in the *available* state until the event happens. There’s no *active* state like for other plan items.
This means that a user could trigger it after start and the stage would be exited.

In some use case however, the event listener shouldn’t be *available* for the user to interact with (or a timer shouldn’t start, when using a timer event listener) unless a certain condition is true.

In the example above, we want to only create it when the stage doesn’t have any active children (or required) anymore. Setting the **availableCondition** to **${cmmn:isStageCompletable()}** will allow the event listener to be created which makes it move immediately to *available*. Concretely in this model, when human task A is completed Stage 1 becomes *completable* (as human task B is manually activated and non-required). This makes the *availableCondition* of the event listener *true* and the event listener is now available for a user to decide to exit the stage.

Note: this is a Flowable specific addition to the CMMN specification. Without this addition, the event listener would have to be nested within a substage which is protected with entry criteria that listens to the completion of task A.

Note: if this were an autocompletable stage, the engine would complete the stage automatically when A completes.

### Item control: Repetition Rule

Plan items on the case model can have a *repetition rule*: an expression that can be used to indicate a certain plan item needs to be repeated.
When no expression is set, but the repetition is enabled (for example, the checkbox is checked in the Flowable Modeler) or the expression is empty, a *true* value is assumed by default.

An optional *repetition counter variable* can be set, which holds the index (one-based) of the instance. If not set, the default variable name is *repetitionCounter*.

If the plan item does not have any entry criteria, the repetition rule expression is evaluated when the plan item is completed or terminated. If the expression resolved to *true*, a new instance is created. For example, a human task with a repetition rule expression *${repetitionCounter &lt; 3}*, will create three sequential human tasks.

If the plan item has entry criteria, the behavior is different. The repetition rule is not evaluated on completion or termination, but when a sentry of the plan item is satisfied. If both the sentry is satisfied and the repetition rule evaluates to true, a new instance is created.

Take, for example, the following timer event listener followed by a human task. The sentry has one entry criterion for the *occur* event of the timer event listener. Note that enabling and setting the repetition rule on the task has a visual indicator at the bottom of the rectangle.

![cmmn.repeatingtimereventlistener](assets/cmmn/cmmn.repeatingtimereventlistener.png)

If the timer event listener is repeating (for example, *R/PT1H*), the *occur* event will be fired every hour. When the repetition rule expression of the human task evaluates to true, a new human task instance will be created each hour.

Note that Flowable allows to have repeating user and generic event listeners. This is contrary to the CMMN specification (which disallows it), but we believe it is needed for having a more flexible way of using event listeners (for example to model a case where a user might multiple times trigger an action that leads to the creation of tasks).

#### Repetition Rule: Max instance count attribute

There is an extended attribute in the Flowable CMMN model for the repetition rule to take more control about the number of concurrently active plan item instances.
Let’s assume you have a plan item with repetition and an entry sentry with a condition. According to the specification, this would create an endless number of plan item instances as long as the condition is true, which might not be the desired behavior.
With the *maxInstanceCount* attribute on the repetition rule, you can define, if there might be an unlimited number of instances (default according to the spec) or if there should be only one instance at a time or any particular maximum instance count.
If you have repetition and set the *maxInstanceCount* to *unlimited*, you need to control the condition in a way that it only creates as many instances as you want or combine it with an on-part (trigger) to only create a new instances whenever that trigger is fired.

Example with a user task having repetition in combination with an entry sentry and a condition and making sure there is only one instance created at a time:

    ...
    <planItem id="planItem1" name="Task 1" definitionRef="humanTask1">
      <itemControl>
        <repetitionRule flowable:maxInstanceCount="1"></repetitionRule>
      </itemControl>
      <entryCriterion id="entryCriterion1" flowable:sentryRef="sentry1"></entryCriterion>
    </planItem>
    <sentry id="sentry1">
      <ifPart>
        <condition><![CDATA[${vars:getOrDefault('enableTaskA', false)}]]></condition>
      </ifPart>
    </sentry>
    ...

Example with a user task having repetition and a repetition condition to control the number of instances being created (in this example 5 tasks will be created):

    <planItem id="planItem1" name="Task 1" definitionRef="humanTask1">
      <itemControl>
        <repetitionRule flowable:counterVariable="repetitionCounter" flowable:maxInstanceCount="unlimited">
          <condition><![CDATA[${vars:getOrDefault('repetitionCounter', 0) <= 5}]]></condition>
        </repetitionRule>
      </itemControl>
    </planItem>

#### Repetition Rule: Repetition collection variable

Similar to the multi instance type in BPMN, you can also use the repetition rule in CMMN in combination with items in a collection.
There are extended attributes for the repetition rule to make use of a collection of elements to create plan item instances for.

Here is the list of the available, extended attributes to control the repetition rule out of a collection (list) of items:

-   **collectionVariable**, set this attribute to the name of the variable with the collection or an expression resolving to a collection

-   **elementVariable**, if the collection variable is set, you can optionally set an element variable to be used at runtime to hold the element as a local variable of the plan item instance

-   **elementIndexVariable**, if the collection variable is set, you can optionally set an element index variable to be used at runtime to hold the index (0 based) as a local variable of the plan item instance

Depending on how the collection gets used with repetition, the time when it is evaluated might be a bit different.

If there is an on-part (an event triggering the plan item) combined with the collection variable, it gets evaluated whenever that on-part is triggered and if the collection is null or empty at that time, no plan item instances are created.

If there is an on-part in combination with an if-part and a collection based repetition, the behavior is the same, however, the if-part might have deferred event trigger handling, which means the plan item is waiting for the if-part to be satisfied, before the collection is checked for the repetition.
Or in other words: the collection needs to be present at the time the if-part is satisfied and the on-part triggers (or was triggered before), then the collection gets evaluated and according its elements, new plan item instances are being started.

If there is no on-part and no if-part, the collection variable gets evaluated every on every evaluation cycle and as soon as it is not null, it is used to create new plan item instances, even if it is empty.
This is done once and the plan item is then terminated. When using a repetition on collection that way, make sure the collection variable becomes available exactly at the moment you want to evaluate it for repetition, otherwise, combine it with an if-part.

If there is an if-part (but no on-part) in combination with a collection variable for repetition, evaluation waits until the if-part is satisfied and afterwards, the collection variable is evaluated to be non-null.
When the if-part is satisfied and the collection is not null (but might be empty), the collection is used for repetition, then the plan item terminates.

Here is an example of a repetition rule combined with a collection variable:

    <planItem id="planItem1" name="Task (${vars:getOrDefault('item', 'na')} - ${vars:getOrDefault('itemIndex', 'na')})" definitionRef="humanTask1">
        <itemControl>
            <repetitionRule flowable:counterVariable="repetitionCounter" flowable:collectionVariable="myCollection" flowable:elementVariable="item" flowable:elementIndexVariable="itemIndex"></repetitionRule>
        </itemControl>
    </planItem>

The example uses a collection variable named *myCollection* and has an element and even element index variable specified. They both get used for the plan item instance name used in its name expression.
As there is no entry sentry, the collection gets evaluated in each evaluation cycle and as soon as *myCollection* is no longer null, it is used for repetition.

### Item control: Manual Activation Rule

Plan items on the case model can have a *manual activation rule*: an expression that can be used to indicate a certain plan item needs to be *manually activated by an end-user*.
When no expression is set, but the manual activation is enabled (for example, the checkbox is checked in the Flowable Modeler) or the expression is empty, a *true* value is assumed by default.

Stages and all task types can be marked for manual activation. Visually, the task or stage will get a 'play' icon (small triangle pointing to the right) to indicate an end-user will have to manually activate it:

![cmmn.manual activation](assets/cmmn/cmmn.manual-activation.png)

Normally, when a sentry for a plan item is satisfied (or the plan item doesn’t have any sentries) the plan item instance is automatically moved to the *ACTIVE* state. When a manual activation is set though, and it evaluates to true, the plan item instance now becomes *ENABLED* instead of *ACTIVE*. As the name implies, the idea behind this is that end-users manually have to activate the plan item instance. A typical use case is showing a list of buttons of potential plan item instances that can currently be started by the end user.

To start an enabled plan item instance, the *startPlanItemInstance* method of the *CmmnRuntimeService* can be used:

    List<PlanItemInstance> enabledPlanItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
        .caseInstanceId(caseInstance.getId())
        .planItemInstanceStateEnabled()
        .list();

    // ...

    cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());

Note that the behavior of a task is only executed when the plan item instance moves into the *ACTIVE* state. For example, for a human task, the user task will only be created after calling the *startPlanItemInstance* method.

Plan item instances that are enabled can be moved into the *DISABLED* state:

    cmmnRuntimeService.disablePlanItemInstance(planItemInstance.getId());

Disabled plan item instances can be enabled again:

    cmmnRuntimeService.enablePlanItemInstance(planItemInstance.getId());

Note that with regards to determining stage or case instance termination, the *DISABLED* state is seen as a 'terminal' state. This means that the case instance will terminate when only disabled plan item instances would remain.

### Item control: Required Rule

Plan items on the case model can have a *required rule*: an expression that can be used to indicate a certain plan item is *required by the enclosing stage (or plan model)*. This can be used to indicate which plan items of the case model are required to be executed and which are optional.

When no expression is set, but the required rule is enabled (for example, the checkbox is checked in the Flowable Modeler) or the expression is empty, a *true* value is assumed by default.

The *required rule* works in conjunction with the *autoComplete* attribute on the parent stage:

-   If *autoComplete* resolves to *false* for the stage, which is also the default when nothing is set, **all** child plan item instances must be in an end state (completed, terminated, and so on) for the stage plan item instance to be completed by the engine

-   If *autoComplete* resolves to *true* for the stage, all child plan item instances for **which the required rule evaluates to true** need to be in an end state. If there are also no other active child plan item instances, the stage completes automatically

A *stage plan item instance* has a **completable** property that can be used to see whether or not the conditions for completion are satisfied.
Take, for example, the following simple stage and assume that the sentry for the *required task* evaluates to true and the other one to false. This means that the left plan item instance will be active while the right one will be in the *available* state.

![cmmn.completeable stage](assets/cmmn/cmmn.completeable-stage.png)

Calling *cmmnRuntimeService.completeStagePlanItemInstance(String stagePlanItemInstanceId)* will not be possible for the stage (an exception will be thrown) as it has one active child plan item instance. When this user task on the left is completed, the *completeStagePlanItemInstance* can now be called, as no child plan item instances are currently active. However, by itself, the stage will not automatically complete as the right user task is in the available state.

If the previous stage is changed to be **autocompletable** (this is visualised by a black rectangle at the bottom of the stage) and the plan item on the left is changed to be required (this is visualised using an exclamation mark), the behavior will be different:

![cmmn.completeable stage02](assets/cmmn/cmmn.completeable-stage02.png)

-   If the left plan item instance is active (sentry is true) and the right is not (sentry is false). In this case, when the left user task is completed, the stage instance will auto complete as it has no active child plan item instances and all required plan item instances are in an end state

-   If both the left and right user tasks are active (sentries are true)

    -   When the left user task is completed, the stage will not autocomplete as there is still a child plan item instance active

    -   When the right user task is completed, the stage will not autocomplete as the required left child plan item instance is not in an end state

-   If the left plan item instance is not active and the right is active. In this case, when the right user task is completed the stage will not autocomplete, as the required left user task is not in an end state. It will need to become active and be completed to complete the stage.

Note that the manual activation rule works independently of the required rule. For example, given the following stage:

![cmmn.completeable stage03](assets/cmmn/cmmn.completeable-stage03.png)

Here, user task D is required and user task B is manually activated.

-   If D is completed, the stage will automatically complete, as B is not required and it is not active

-   If B would be required too, it would need to be manually started (using *cmmnRuntimeService.startPlanItemInstance(String planItemInstanceId)*) before the stage would automatically complete, even if D would be completed before the manual start of B

### Item control: Completion Neutral Rule

Plan items on the case model can have a *completion neutral rule*: an expression that can be used to indicate a certain plan item is *neutral with regards to the completion of its parent stage (or plan model)*. This can be used to indicate which plan items of the case model are required to be executed and which are optional, as a more flexible alternative in some use cases to using the *required rule* and *autoComplete* .

Note that the *Completion Neutral Rule* is not a CMMN 1.1 Standard, but a Flowable-specific addition.

Following the specification, a stage with a plan item in state **AVAILABLE** does not complete unless its *autoComplete* attribute is set *true* and the plan item is not required. For example, a plan item that has an unsatisfied sentry remains in **AVAILABLE** until the sentry is satisfied. This means that the parent stage would not complete, unless the plan item is marked as *not required* and the stage is set to *autoComplete*. The downside is that once a stage is marked as to autoComplete, all child plan items need to have a configuration for the *required* rule, which is in some use cases tedious and lots of work.

The *Completion Neutral Rule*, contrary to the autoComplete-required mechanism, works from "bottom-up": a plan item can be marked individually to be *neutral wrt the completion of its parent* without having to mark any other plan item.

The *Required Rule* takes precedence when plan items with both rules both evaluate to *true*.

To summarize:

-   a plan item configured to be *"completion neutral"* will allow a stage to complete automatically if it’s in **AVAILABLE** state (e.g. waiting for an entry criterion sentry),meaning that such a plan item is neutral with respect to its parent stage completion evaluation.

-   a stage will remain **ACTIVE** on any of these conditions:

    1.  It has at least one plan item in **ACTIVE** state

    2.  It has at least one plan item with *requiredRule* in **AVAILABLE** or **ENABLE** state

    3.  It is not marked as *autoComplete* and has at least one plan item in **ENABLED** state (irrespective of its *requiredRule*)

    4.  It is not marked as *autoComplete* and has at least one plan item in **AVAILABLE** state that is **not** *completionNeutral*

-   a stage will **COMPLETE** if:

    1.  It contains no plan items or all child plan items are in a *Terminal* or *Semi-terminal* state (CLOSED, COMPLETED, DISABLED, FAILED)

    2.  It is not marked as *autoComplete* and all remaining child plan items are in **AVAILABLE** state and are *completionNeutral* and not *required*

    3.  It is *autoComplete* and all remaining plan items are *not required* in **ENABLED** or **AVAILABLE** state (regardless of its completion neutrality, as required rule gets precedence)

### Item control: Parent Completion Rule

In addition to the *completion neutral rule* (which is going to be deprecated), the *parent completion rule* offers way more flexibility whenever it comes down to evaluating a stage or the case plan model to be completable.
There is the *auto complete* possibility to automatically complete a stage whenever all the required and active work is done, but sometimes, you want a more fine-grained way on how existing plan item instances get treated around the evaluation of its parent being completable.
With the parent completion rule, you can define the behavior of a plan item on its parent completion evaluation.

Here is a list of types currently supported for the parent completion rule:

-   **default**: Use this value if the default behavior is required according the CMMN specification.

-   **ignore**: With this value, the plan item is fully ignored when it is evaluated for its parent completion state. This is particularly useful, if you have plan items (e.g. a case page) to be ignored completely as they don’t have an impact on the case execution or stage completion evaluation.

-   **ignoreIfAvailable**: With this value, the plan item only gets ignored, if it is in *available* state, but will prevent the stage from being completed, if it is *active* or *enabled*.

-   **ignoreIfAvailableOrEnabled**: This value includes the *enabled* (waiting for manual activation) state to be ignored, only an *active* instance will prevent the stage from being completed.

-   **ignoreAfterFirstCompletion**: This value is interesting for instance if you have a user task which has repetition and you want to make sure, it was completed at least once, but afterwards, it must not prevent its parent from being completed, even if it is *active*.

-   **ignoreAfterFirstCompletionIfAvailableOrEnabled**: In contrast to the previous one, use this value, if you want a plan item being ignored after its first completion, if it is in *available* or *enabled* state, but is currently not *active*.

Here is an example on how to use the **parent completion rule** for a plan item. This example uses it in combination with repetition, the required rule and even manual activation.
So it will prevent it’s parent from being completed, if it was not started and completed at least once, but no longer, if it was completed once and is not in *active* state afterwards.

    <planItem id="planItem1" name="Task A" definitionRef="humanTask1">
        <itemControl>
            <extensionElements>
                <flowable:parentCompletionRule type="ignoreAfterFirstCompletionIfAvailableOrEnabled" />
            </extensionElements>
            <repetitionRule></repetitionRule>
            <requiredRule></requiredRule>
            <manualActivationRule></manualActivationRule>
        </itemControl>
    </planItem>

## Sentry evaluation

Sentries play a big role in any case definition as they offer a powerful way of configuring in a declarative way when certain plan item instances activate or when they are automatically stopped.
As such, one of the most important parts of the Flowable CMMN engine core logic is to evaluate the sentries to see what state changes happen in a case instance.

### When are sentries evaluated?

Sentries are evaluated whenever state changes happen in the case instance or new events happen. Concretely this means:

-   When a case instance is started.

-   When a wait state plan item such as a human task is triggered to continue.

-   When variables related to the case instance change (added, updated or deleted).

-   When the state of a plan item instance is changed (e.g. terminated through RuntimeService, a manual plan item instance is started, etc.).

-   When manually triggered through the RuntimeService\#evaluateCriteria method.

The engine will continue to plan new evaluations of all currently active sentries as long as changes keep happening.
For example, suppose the completion of a human task satisfies the exit sentry of another human task. The state change of the second human task will again schedule a new evaluation of all active sentries with this new information. When no changes have happened during the last evaluation, the engine deems the state stable and evaluation is stopped.

### Concepts

Sentries consist of two parts:

-   One or more *onParts* that reference lifecycle events from other plan items

-   Zero or one ifPart with a condition

Take for example the following case definition:

![cmmn.sentry eval 01](assets/cmmn/cmmn.sentry-eval-01.png)

Assume (not shown in the diagram here)

-   The entry sentry on task C listens to the *complete* event from task A and B.

-   The exit sentry listens to the *occur* event of the user event listener *'Stop C'*

-   The entry sentry has a condition expression set to *${var:eq(myVar, 'hello world')}*

In this simple example, the *entry sentry* has two onParts and one ifPart. The *exit sentry* only has onPart.

When the case instance is started, human tasks A and B are created (as they have no entry sentry) and move immediately to state *active*. C is not *active*, but *available* as the sentry has not yet been satisfied. The user event listener *'Stop C'* is also *available* from the start and it can thus be triggered.

When both task A and B have been completed and the variable *myVar* is set to *'hello world'*, the entry sentry is satisfied and fires. The plan item instance behind C is moved to the *active* state and as a side-effect the human task C is created (it can now be queried through the *TaskService* for example).
When *'Stop C'* is triggered (through the *CmmnRuntimeService\#completeUserEventListenerInstance* method, the exit sentry for C is satisfied and C is terminated.

If *'Stop C'* would be triggered before C moves to *active*, its plan item instance would be terminated and the entry sentry won’t be listening anymore to anything.

### Default behavior

When the case instance is started

    CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
        .caseDefinitionKey("myCase")
        .start();

the condition on the entry sentry is immediately evaluated, as a regular evaluation cycle happens on case instance start.

Note that, if an expression for the condition like *${myVar == 'hello world'}* would be used this would not work. The engine would throw a *PropertyNotFound* exception as it doesn’t know the *myVar* variable.

To solve this:

-   pass a variable value for *myVar* on case instance start

-   do a null check in the expression, like *${planItemInstance.getVariable('myVar') != null && planItemInstance.getVariable('myVar') == 'hello world'}*

-   or (and probably easiest), check [expression functions](cmmn/ch03-API.md#expression-functions) to use a function such as *${var:eq(myVar, 'hello world')}* which takes in account the fact the variable might not exist.

**The default evaluation logic has "memory", which means that when a part of a sentry is satisfied the engine will store and "remember" this in subsequent evaluations.**

This means that, from the moment a part (an onPart or ifPart of the sentry) is satisfied, that particular part is not evaluated anymore in next evaluations and it is deemed true.

In the example above, this is needed as task A will typically be completed at another point in time than task B. For example if task A is completed, the part of the sentry on task C that says "i’m listening to the complete event of task A" is now satisfied and this fact is remembered for the future. If now B completes, this is also stored. If now the *myVar* variable gets the right value, the ifPart also fires and the whole sentry fires and task C gets activated. Of course, it could also be that the variable value is satisfied first and the tasks after. The point is that it doesn’t matter as the engine will remember the parts that were satisfied in the past.

This behavior "with memory" is the default behavior of the engine and is covered by setting the *triggerMode* of a sentry to **default**. This is automatically set in the Flowable Modeler when adding a new plan item. When no value has been set (for example when importing a case model from another tool), the *triggerMode* is assumed to be *default*.

### Trigger mode "onEvent"

The default behavior (see previous section) will remember which parts have been satisfied previously. This is the most used and safest approach (and also what is typically expected when reasoning about sentries).

There is an alternative mode of sentry triggering that is called **"onEvent"**. In this mode, the engine will have memory with regards to parts of the sentry and will **not remember** any part that was satisfied in the past. This is sometimes needed in advanced use cases. Take for example the following example:

![cmmn.sentry eval 02](assets/cmmn/cmmn.sentry-eval-02.png)

Here, the case model has a stage with with three substages. All substage are repeating. Substage B and C have an entry sentry for the completion of stage B. Also (not visually shown), both sentries have a condition that depends on a variable.

In advanced use cases, it could be wanted or needed that that the sentry parts (and especially the ifPart containing the condition) are evaluated only *when the lifecycle event of the dependent plan item* happens. In this case, this is the *complete* event of *Stage A*. For these use cases, the *triggerMode* of the sentry can be set to *onEvent*. As the name implies, this means that the sentry evaluation only happens when a referenced event happens and no memory of past things are taken into account.

Concretely, in the example here, the condition of the entry sentries will be evaluated **only** when Stage A completed (and on no other moment). This is very different from the general evaluation rules. In this particular example, it does make managing the variables easier as the conditions are only evaluated on one precise moment and there needs to be no fear of some sentry part being fired due to a variable having a value at a certain point in time. Especially as in the example here all substages are repeating, this would be a lot of work to do. This is a powerful mechanism, but meant for advanced modelers that have an intrinsic knowledge of the case model and the semantics of this triggerMode.

Do note that the engine deems all events to happen simultaneously when it comes to evaluating sentries. Take the following case definition:

![cmmn.sentry eval 03](assets/cmmn/cmmn.sentry-eval-03.png)

Assume that all sentries use the *triggerMode onEvent* setting. If task A is completed, this exits task B. Task C will now exit too. So, even though there are two distinct lifecycle events (A being completed and B being exited) and one might assume that *onEvent* literally means that there are two distinct evaluations happening where the memory of the other part of the exit sentry on task C is forgotten, the engine is smart enough to see that they are part of the same evaluation cycle and task C will be exited too.

Technically spoken: there is *some* memory for the *onEvent* sentry, more specifically for evaluations that happen during the same API call (or transaction, lower-level spoken).

**Important: onEvent is a powerful mechanism and should only be used when the semantics are well understood. It’s possible to create a case model that gets stuck due to not having the correct sentry configuration if the use case is not carefully examined.**

(For example, suppose a sentry has an onPart listening to the completion of a plan item and an ifPart with a condition. If the plan item completes - thus triggering the onPart - but a variable used in the condition is missing for some reason …​ the ifPart would never fire and the case instance might get stuck in an unwanted state).
