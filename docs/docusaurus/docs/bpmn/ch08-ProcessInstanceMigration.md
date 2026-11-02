---
id: ch08-ProcessInstanceMigration
title: Process Instance Migration
---

When process definitions are updated with new versions, the question arises what should be done with already running process instances that are using older versions of the process definition.
In case running process instances should be migrated to another process definition version, you can use the process instance migration features on the Flowable Engine.

## Simple example

Let’s start with a simple example to explain the basics of process instance migration in the Flowable Engine.
In this simple example we use the following use case:

-   There’s one process instance running with a process definition that has a key named simpleTasks and it consists of a start event - user task 1 - end event.

-   The running process instance has an active state of user task 1.

-   A new process definition is deployed with the same key (simpleTasks) and the process definition now consists of a start event - user task 1 - user task 2 - end event.

-   The running process instance should be migrated to the new process definition.

To test if the process instance can be migrated without issues the following code can be used:

    ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
        .migrateToProcessDefinition(version2ProcessDef.getId())
        .validateMigration(processInstanceToMigrate.getId());

    boolean isMigrationValid = validationResult.isMigrationValid();

The process instance migration builder can be used to validate and, as we will see later on, migrate one or more process instances.
In this case we test if the running process instance can be migrated to the new process definition version with 2 user tasks.
Because the user task id didn’t change between the two process definition versions, the process instance can be migrated without any additional mapping configuration.
Therefore, the migration will have a migration valid boolean value of true. This means we can run the actual migration without to be expected issues.

    processMigrationService.createProcessInstanceMigrationBuilder()
        .migrateToProcessDefinition(version2ProcessDef.getId())
        .migrate(processInstanceToMigrate.getId());

After executing the migrate method, the running process instance is migrated to the new process definition (including the historic information).
This means that when user task 1 is completed, user task 2 will be the next active state for the running process instance.

## Migration with activity migration mappings

In the simple example user task 1 was automatically mapped to the same user task in the new process definition version.
But in some cases the current activity of a running process instance doesn’t exist anymore in the new process definition, or the activity should be migrated to another activity for another reason.
For this use case, the process instance migration builder allows you to specify a list of specific activity migration mappings.

    processMigrationService.createProcessInstanceMigrationBuilder()
        .migrateToProcessDefinition(version2ProcessDef.getId())
        .addActivityMigrationMapping("userTask1Id", "userTask2Id")
        .migrate(processInstanceToMigrate.getId());

In this example running process instance with an active state of user task 1 will be migrated to a new process definition version with 2 user tasks, and the active state will be migrated to user task 2.
This means that when user task 2 is completed the process instance will be ended.

## Supported process instance migration cases

This section provides an overview of the supported cases for process instance migration.
If the case you are looking for is not yet supported have a look at next section with upcoming support.

-   automatic migration of wait states (user task, receive task, intermediate catch events) to activities with the same id in the new process definition version.

-   manual migration of wait states by specifying the target activity for a specific active state in the running process instance.

-   migrating a wait state to an activity with a boundary timer, signal or message event.

-   migrating a wait state with a boundary timer, signal or message event to an activity without a boundary event.

-   migrating a wait state to an activity in an embedded sub process or a nested embedded subprocess.

-   migrating a wait state in an embedded sub process or a nested embedded sub process to the root level of the process definition or another nested scope.

-   migrating a wait state to an activity in an (nested) event sub process, both interrupting and non-interrupting.

-   migrating multiple executions when using a parallel or inclusive gateway, to one execution outside of the gateway scope.

-   migrating from a single execution to multiple executions within a parallel or inclusive gateway.

-   migrating a wait state to an activity in the parent process.

## Upcoming process instance migration support

With this version of the Flowable Engine the first step with process instance migration support is added. In the next version the focus is on adding support for the following migration cases:

-   Support to move a collection of multi instance executions to another activity.

-   Support to move a wait state to a multi instance activity.

-   Support to move a wait state to an activity in a sub process, when one or more call activities are present in the process definition.

-   Support to add and remove variables in the process instance or local execution scope.

-   Support to define the assignment rules and other configuration options of a target user task.
