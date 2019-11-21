---
id: ch07-architecture
title: Architecture
---

This chapter gives a short introduction to the internals of the CMMN engine from a high-level point of view. Of course, as the CMMN engine code is open source, the actual implementation details can be found by diving into the source code.

The CMMN engine, as shown in the diagram below, is one of the engines that is part of the Flowable engines eco-system and is designed in a similar way as the other engines. The CMMN engine builds upon CMMN specific services and the shared services: the task, variable, identity and job services, which are independent from the engines. On a lower level the entity- and datamanager layer takes care of the low-level persistence, equivalent to the other engines.

![cmmn.architecture](assets/cmmn/cmmn.architecture.png)

By using the shared services, this also means that for example tasks from both the BPMN and the CMMN engine will end up together and can be queried and managed through the same API. Similarly for the async executor: timers and async jobs use the same logic and even can be managed by a central executor.

The shared services architecture has a second benefit. When the engines are used together, as is often the case, the engines will share resources where possible, database transactions will span calls over multiple engines operations, lookup caches are common and persistency is handled independently from the engines itself.

The CMMN engine is designed with the same principles as the other engines of the Flowable project. The diagram below gives a high-level overview of the different components involved when working with the CMMN Engine:

![cmmn.api call flow](assets/cmmn/cmmn.api-call-flow.png)

From a high level point of view:

-   A **CmmnEngine** instance is created from a **CmmnEngineConfiguration**. The configuration instance can be created from a configuration file or programmatically.

-   The *CmmnEngine* gives access the Flowable CMMN API’s in the form of services: **CmmnRepositoryService**, **CmmnRuntimeService**, **CmmnTaskService**, **CmmnHistoryService**, **CmmnManagementService**. The naming of the services and their responsibilities is similar to the other engines.

-   Every API method is transformed into a **Command** instance. This *Command* instance is passed into the **CommandExecutor** which, amongst others, makes it pass a stack of **CommandInterceptors**. These interceptors have various responsibilities, including handling transactions.

-   Eventually, the *Command* typically (unless it’s a pure data modification *Command*) plans a **CmmnOperation** on the **CmmnEngineAgenda**.

-   Operations will be taken from the *CmmnEngineAgenda* until no more operations are left. Typically, an operation will plan new operations as part of its logic.

-   The logic in the operations will call out to lower-level services and/or entity managers.

A big difference between the BPMN and the CMMN engine is that the BPMN engine is generally 'local': the engine looks at the current state, checks what is ahead in the process and continues (of course this is a simplification, there are plenty of operations where this doesn’t apply, but for making a distinction conceptually, it is correct). The CMMN engine works differently: in CMMN, data plays an important role and the change of data can trigger many things in various places in the case definition. For this reason, the **EvaluateCriteriaOperation** is planned and executed often whenever changes happen. The engine does optimize these evaluations when it detects duplicates or useless evaluations.

Core to the working of the CMMN engine is the concept of a **Plan item instance**, a representation of which *plan items* currently are *live* in the case and which state they have. Quite different from BPMN, CMMN defines a strict state life cycle for the plan items. This is represented in the *CmmnRuntimeService* methods, the query API’s and the data fields part of the **PlanItemInstance** object.

The working of the agenda, the operations and how plan item instances are handled can be inspected by setting logging to debug for the agenda package. For example, when using log4j:

    log4j.logger.org.flowable.cmmn.engine.impl.agenda=DEBUG

Which leads to logging like:

    Planned [Init Plan Model] initializing plan model for case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122
    Planned [Change PlanItem state] Task A (id: planItemTaskA), new state: [available] with transition [create]
    Planned [Change PlanItem state] PlanItem Milestone One (id: planItemMileStoneOne), new state: [available] with transition [create]
    Planned [Change PlanItem state] Task B (id: planItemTaskB), new state: [available] with transition [create]
    Planned [Change PlanItem state] PlanItem Milestone Two (id: planItemMileStoneTwo), new state: [available] with transition [create]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'create' having fired for plan item planItemTaskA (Task A)
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'create' having fired for plan item planItemMileStoneOne (PlanItem Milestone One)
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'create' having fired for plan item planItemTaskB (Task B)
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'create' having fired for plan item planItemMileStoneTwo (PlanItem Milestone Two)
    Planned [Activate PlanItem] Task A (planItemTaskA)
    Planned [Change PlanItem state] Task A (id: planItemTaskA), new state: [active] with transition [start]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'start' having fired for plan item planItemTaskA (Task A)
    Planned [Change PlanItem state] Task A (id: planItemTaskA), new state: [completed] with transition [complete]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'complete' having fired for plan item planItemTaskA (Task A)
    Planned [Activate PlanItem] PlanItem Milestone One (planItemMileStoneOne)
    Planned [Change PlanItem state] PlanItem Milestone One (id: planItemMileStoneOne), new state: [active] with transition [start]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'start' having fired for plan item planItemMileStoneOne (PlanItem Milestone One)
    Planned [Change PlanItem state] PlanItem Milestone One (id: planItemMileStoneOne), new state: [completed] with transition [occur]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'occur' having fired for plan item planItemMileStoneOne (PlanItem Milestone One)
    Planned [Activate PlanItem] Task B (planItemTaskB)
    Planned [Change PlanItem state] Task B (id: planItemTaskB), new state: [active] with transition [start]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'start' having fired for plan item planItemTaskB (Task B)
    Planned [Change PlanItem state] Task B (id: planItemTaskB), new state: [completed] with transition [complete]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'complete' having fired for plan item planItemTaskB (Task B)
    Planned [Activate PlanItem] PlanItem Milestone Two (planItemMileStoneTwo)
    Planned [Change PlanItem state] PlanItem Milestone Two (id: planItemMileStoneTwo), new state: [active] with transition [start]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'start' having fired for plan item planItemMileStoneTwo (PlanItem Milestone Two)
    Planned [Change PlanItem state] PlanItem Milestone Two (id: planItemMileStoneTwo), new state: [completed] with transition [occur]
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122 with transition 'occur' having fired for plan item planItemMileStoneTwo (PlanItem Milestone Two)
    Planned [Evaluate Criteria] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122
    No active plan items found for plan model, completing case instance
    Planned [Complete case instance] case instance bfaf0e64-eaf4-11e7-b9d0-acde48001122
