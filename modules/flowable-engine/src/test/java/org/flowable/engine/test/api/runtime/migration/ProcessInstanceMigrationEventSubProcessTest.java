/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.api.runtime.changestate.ChangeStateEventListener;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationEventSubProcessTest extends PluggableFlowableTestCase {

    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @AfterEach
    protected void tearDown() {
        processEngine.getRuntimeService().removeEventListener(changeStateEventListener);
        deleteDeployments();
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideSignalEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-signal-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("eventSubProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideMessageEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-message-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("eventSubProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideTimerEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-timer-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("eventSubProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(job);

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideNonInterruptingSignalEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-signal-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("eventSubProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());

        //Since the only task in the parent scope was moved, it behaves as a interrupting eventSubProcess
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideNonInterruptingMessageEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-message-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("eventSubProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());

        //Since the only task in the parent scope was moved, it behaves as a interrupting eventSubProcess
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideNonInterruptingTimerEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-timer-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("eventSubProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());

        //Since the only task in the parent scope was moved, it behaves as a interrupting eventSubProcess
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(job);

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideSignalEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-signal-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("parallelTask", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("eventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideMessageEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-message-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("parallelTask", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("eventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideTimerEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-timer-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("parallelTask", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("eventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(job);

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNonInterruptingSignalEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-signal-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("parallelTask", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcess", "eventSubProcessTask", "eventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "eventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("eventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("eventSignal");

        //Fire the signal
        runtimeService.signalEventReceived("eventSignal");

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcess", "eventSubProcess", "eventSubProcessStart", "eventSubProcessTask", "eventSubProcessTask");
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("eventSubProcessTask", "eventSubProcessTask", "processTask");
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("eventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("eventSignal");

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNonInterruptingMessageEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-message-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("parallelTask", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcess", "eventSubProcessTask", "eventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "eventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("eventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("someMessage");

        //Trigger the event
        Execution messageSubscriptionExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).messageEventSubscriptionName("someMessage").singleResult();
        runtimeService.messageEventReceived("someMessage", messageSubscriptionExecution.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcess", "eventSubProcess", "eventSubProcessStart", "eventSubProcessTask", "eventSubProcessTask");
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("eventSubProcessTask", "eventSubProcessTask", "processTask");
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("eventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("someMessage");

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNonInterruptingTimerEventSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-timer-event-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("parallelTask", "eventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcess", "eventSubProcessTask", "eventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "eventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).extracting(this::getJobActivityId).isEqualTo("eventSubProcessStart");

        //Fire the signal
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcess", "eventSubProcess", "eventSubProcessStart", "eventSubProcessTask", "eventSubProcessTask");
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("eventSubProcessTask", "eventSubProcessTask", "processTask");
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).extracting(this::getJobActivityId).isEqualTo("eventSubProcessStart");

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideSubProcessInNewDefinitionWithNestedSignalEventSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-signal-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "subProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedSignalEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedSignalEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("someSignal");

        //Fire the signal
        runtimeService.signalEventReceived("someSignal");

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSignalEventSubProcess", "nestedSignalEventSubProcessTask", "nestedSignalEventSubProcessStart");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("nestedSignalEventSubProcessTask");
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        eventSubscriptions.isEmpty();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideSubProcessInNewDefinitionWithNestedMessageEventSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-message-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "subProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedMessageEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedMessageEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("someMessage");

        //Trigger the event
        Execution messageSubscriptionExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).messageEventSubscriptionName("someMessage").singleResult();
        runtimeService.messageEventReceived("someMessage", messageSubscriptionExecution.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedMessageEventSubProcess", "nestedMessageEventSubProcessTask", "nestedMessageEventSubProcessStart");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("nestedMessageEventSubProcessTask");
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        eventSubscriptions.isEmpty();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideSubProcessInNewDefinitionWithNestedTimerEventSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-timer-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "subProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedTimerEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).extracting(this::getJobActivityId).isEqualTo("nestedTimerEventSubProcessStart");

        //Fire the signal
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedTimerEventSubProcess", "nestedTimerEventSubProcessTask", "nestedTimerEventSubProcessStart");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("nestedTimerEventSubProcessTask");
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(job);

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideSubProcessInNewDefinitionWithNestedNonInterruptingSignalEventSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-signal-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "subProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSignalEventSubProcessStart", "subProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedSignalEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");

        //-- TODO CHECK HISTORY AND EVENTS

        //Check that the trigger works
        runtimeService.signalEventReceived("someSignal");

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSignalEventSubProcessStart", "nestedSignalEventSubProcess", "nestedSignalEventSubProcessTask", "subProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedSignalEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedSignalEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideSubProcessInNewDefinitionWithNestedNonInterruptingMessageEventSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-message-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "subProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedMessageEventSubProcessStart", "subProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedMessageEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");

        //-- TODO CHECK HISTORY AND EVENTS

        //Trigger the event
        Execution messageCatchExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).messageEventSubscriptionName("someMessage").singleResult();
        runtimeService.messageEventReceived("someMessage", messageCatchExecution.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedMessageEventSubProcess", "nestedMessageEventSubProcessStart", "nestedMessageEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedMessageEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedMessageEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSimpleActivityToActivityInsideSubProcessInNewDefinitionWithNestedNonInterruptingTimerEventSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-timer-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "subProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedTimerEventSubProcessStart", "subProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subProcessTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).extracting(this::getJobActivityId).isEqualTo("nestedTimerEventSubProcessStart");

        //-- TODO CHECK HISTORY AND EVENTS

        //Check that the trigger works
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedTimerEventSubProcessStart", "nestedTimerEventSubProcess", "subProcessTask", "nestedTimerEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedTimerEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).extracting(this::getJobActivityId).isEqualTo("nestedTimerEventSubProcessStart");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedSignalEventSubProcessInNewDefinitionAndActivityToRootScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-signal-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "beforeSubProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedSignalEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("beforeSubProcessTask", "subProcess", "nestedSignalEventSubProcess", "nestedSignalEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("beforeSubProcessTask", "nestedSignalEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedSignalEventSubProcessInNewDefinitionAndActivityToParentScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-signal-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "subProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedSignalEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSignalEventSubProcess", "nestedSignalEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("nestedSignalEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedMessageEventSubProcessInNewDefinitionAndActivityToRootScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-message-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "beforeSubProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedMessageEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("beforeSubProcessTask", "subProcess", "nestedMessageEventSubProcess", "nestedMessageEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("beforeSubProcessTask", "nestedMessageEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedMessageEventSubProcessInNewDefinitionAndActivityToParentScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-message-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "subProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedMessageEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedMessageEventSubProcess", "nestedMessageEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("nestedMessageEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedTimerEventSubProcessInNewDefinitionAndActivityToRootScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-timer-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "beforeSubProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedTimerEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("beforeSubProcessTask", "subProcess", "nestedTimerEventSubProcess", "nestedTimerEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("beforeSubProcessTask", "nestedTimerEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(job);

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedTimerEventSubProcessInNewDefinitionAndActivityToParentScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/interrupting-timer-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "subProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedTimerEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedTimerEventSubProcess", "nestedTimerEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("nestedTimerEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(job);

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedNonInterruptingSignalEventSubProcessInNewDefinitionAndActivityToRootScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-signal-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "beforeSubProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedSignalEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("beforeSubProcessTask", "subProcess", "nestedSignalEventSubProcess", "nestedSignalEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("beforeSubProcessTask", "nestedSignalEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedNonInterruptingSignalEventSubProcessInNewDefinitionAndActivityToParentScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-signal-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "subProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedSignalEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedSignalEventSubProcess", "nestedSignalEventSubProcessTask", "nestedSignalEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedSignalEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedSignalEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("someSignal");

        //Trigger the event
        runtimeService.signalEventReceived("someSignal");

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId)
            .containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedSignalEventSubProcess", "nestedSignalEventSubProcessTask", "nestedSignalEventSubProcess", "nestedSignalEventSubProcessTask", "nestedSignalEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedSignalEventSubProcessTask", "nestedSignalEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedSignalEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("someSignal");

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedNonInterruptingMessageEventSubProcessInNewDefinitionAndActivityToRootScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-message-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "beforeSubProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedMessageEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("beforeSubProcessTask", "subProcess", "nestedMessageEventSubProcess", "nestedMessageEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("beforeSubProcessTask", "nestedMessageEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedNonInterruptingMessageEventSubProcessInNewDefinitionAndActivityToParentScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-message-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "subProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedMessageEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedMessageEventSubProcess", "subProcessTask", "nestedMessageEventSubProcessTask", "nestedMessageEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedMessageEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedMessageEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("someMessage");

        //Trigger the event
        EventSubscription eventExecution = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).eventName("someMessage").singleResult();
        runtimeService.messageEventReceived("someMessage", eventExecution.getExecutionId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId)
            .containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedMessageEventSubProcess", "nestedMessageEventSubProcessTask", "nestedMessageEventSubProcess", "nestedMessageEventSubProcessTask",
                "nestedMessageEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedMessageEventSubProcessTask", "nestedMessageEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("nestedMessageEventSubProcessStart");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventName).containsExactly("someMessage");

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedNonInterruptingTimerEventSubProcessInNewDefinitionAndActivityToRootScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-timer-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "beforeSubProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedTimerEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("beforeSubProcessTask", "subProcess", "nestedTimerEventSubProcess", "nestedTimerEventSubProcessTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("beforeSubProcessTask", "nestedTimerEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        //Behaves like interrupting since theres no execution in the parentScop
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(job);

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivityToActivityInsideNestedNonInterruptingTimerEventSubProcessInNewDefinitionAndActivityToParentScope() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/non-interrupting-timer-event-subprocess-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).extracting(this::getJobActivityId).doesNotContain("nestedTimerEventSubProcessStart");

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("processTask", "subProcessTask")
            .addActivityMigrationMapping("parallelTask", "nestedTimerEventSubProcessTask")
            .migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedTimerEventSubProcess", "nestedTimerEventSubProcessTask", "nestedTimerEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedTimerEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).extracting(this::getJobActivityId).isEqualTo("nestedTimerEventSubProcessStart");

        //Fire the timer
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId)
            .containsExactlyInAnyOrder("subProcess", "subProcessTask", "nestedTimerEventSubProcess", "nestedTimerEventSubProcessTask", "nestedTimerEventSubProcess", "nestedTimerEventSubProcessTask", "nestedTimerEventSubProcessStart");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("subProcessTask", "nestedTimerEventSubProcessTask", "nestedTimerEventSubProcessTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procWithSignal.getId());

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).extracting(this::getJobActivityId).isEqualTo("nestedTimerEventSubProcessStart");

        //-- TODO CHECK HISTORY AND EVENTS

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }
}
