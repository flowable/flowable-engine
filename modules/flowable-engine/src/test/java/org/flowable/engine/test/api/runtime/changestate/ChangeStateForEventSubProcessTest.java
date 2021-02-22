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

package org.flowable.engine.test.api.runtime.changestate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ChangeStateForEventSubProcessTest extends PluggableFlowableTestCase {

    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @AfterEach
    protected void tearDown() {
        processEngine.getRuntimeService().removeEventListener(changeStateEventListener);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleMessageEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityFromMessageEventSubProcessStart() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertThat(executionsByActivity).hasSize(2);
        assertThat(executionsByActivity.get("processTask")).hasSize(1);
        assertThat(executionsByActivity.get("eventSubProcessStart")).hasSize(1);

        List<Task> tasks = taskService.createTaskQuery().list();
        Map<String, List<Task>> tasksByKey = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(tasksByKey).hasSize(1);
        assertThat(tasksByKey.get("processTask")).hasSize(1);

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription.getActivityId()).isEqualTo("eventSubProcessStart");
        assertThat(eventSubscription.getEventType()).isEqualTo("message");
        assertThat(eventSubscription.getEventName()).isEqualTo("eventMessage");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("eventSubProcessStart", "eventSubProcessTask")
                .changeState();

        tasks = taskService.createTaskQuery().list();
        tasksByKey = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(tasksByKey).hasSize(2);
        assertThat(tasksByKey.get("processTask")).hasSize(1);
        assertThat(tasksByKey.get("eventSubProcessTask")).hasSize(1);

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        taskService.complete(tasksByKey.get("eventSubProcessTask").get(0).getId());
        taskService.complete(tasksByKey.get("processTask").get(0).getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleMessageEventSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionFromMessageEventSubProcessStart() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertThat(executionsByActivity).hasSize(2);
        assertThat(executionsByActivity.get("processTask")).hasSize(1);
        assertThat(executionsByActivity.get("eventSubProcessStart")).hasSize(1);

        List<Task> tasks = taskService.createTaskQuery().list();
        Map<String, List<Task>> tasksByKey = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(tasksByKey).hasSize(1);
        assertThat(tasksByKey.get("processTask")).hasSize(1);

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription.getActivityId()).isEqualTo("eventSubProcessStart");
        assertThat(eventSubscription.getEventType()).isEqualTo("message");
        assertThat(eventSubscription.getEventName()).isEqualTo("eventMessage");

        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(executionsByActivity.get("eventSubProcessStart").get(0).getId(), "eventSubProcessTask")
                .changeState();

        tasks = taskService.createTaskQuery().list();
        tasksByKey = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(tasksByKey).hasSize(2);
        assertThat(tasksByKey.get("processTask")).hasSize(1);
        assertThat(tasksByKey.get("eventSubProcessTask")).hasSize(1);

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        taskService.complete(tasksByKey.get("eventSubProcessTask").get(0).getId());
        taskService.complete(tasksByKey.get("processTask").get(0).getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleSignalEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityFromSignalEventSubProcessStart() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertThat(executionsByActivity).hasSize(2);
        assertThat(executionsByActivity.get("processTask")).hasSize(1);
        assertThat(executionsByActivity.get("eventSubProcessStart")).hasSize(1);

        List<Task> tasks = taskService.createTaskQuery().list();
        Map<String, List<Task>> tasksByKey = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(tasksByKey).hasSize(1);
        assertThat(tasksByKey.get("processTask")).hasSize(1);

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription.getActivityId()).isEqualTo("eventSubProcessStart");
        assertThat(eventSubscription.getEventType()).isEqualTo("signal");
        assertThat(eventSubscription.getEventName()).isEqualTo("eventSignal");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("eventSubProcessStart", "eventSubProcessTask")
                .changeState();

        tasks = taskService.createTaskQuery().list();
        tasksByKey = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(tasksByKey).hasSize(2);
        assertThat(tasksByKey.get("processTask")).hasSize(1);
        assertThat(tasksByKey.get("eventSubProcessTask")).hasSize(1);

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        taskService.complete(tasksByKey.get("eventSubProcessTask").get(0).getId());
        taskService.complete(tasksByKey.get("processTask").get(0).getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleSignalEventSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionFromSignalEventSubProcessStart() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertThat(executionsByActivity).hasSize(2);
        assertThat(executionsByActivity.get("processTask")).hasSize(1);
        assertThat(executionsByActivity.get("eventSubProcessStart")).hasSize(1);

        List<Task> tasks = taskService.createTaskQuery().list();
        Map<String, List<Task>> tasksByKey = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(tasksByKey).hasSize(1);
        assertThat(tasksByKey.get("processTask")).hasSize(1);

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription.getActivityId()).isEqualTo("eventSubProcessStart");
        assertThat(eventSubscription.getEventType()).isEqualTo("signal");
        assertThat(eventSubscription.getEventName()).isEqualTo("eventSignal");

        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(executionsByActivity.get("eventSubProcessStart").get(0).getId(), "eventSubProcessTask")
                .changeState();

        tasks = taskService.createTaskQuery().list();
        tasksByKey = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(tasksByKey).hasSize(2);
        assertThat(tasksByKey.get("processTask")).hasSize(1);
        assertThat(tasksByKey.get("eventSubProcessTask")).hasSize(1);

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        taskService.complete(tasksByKey.get("eventSubProcessTask").get(0).getId());
        taskService.complete(tasksByKey.get("processTask").get(0).getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleSignalEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideSignalEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleMessageEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideMessageEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .contains("processTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "eventMessage");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleTimerEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideTimerEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("processTask");

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleNonInterruptingSignalEventSubProcess.bpmn20.xml" })
    public void testSetCurrentOnlyActivityToActivityInsideNonInterruptingSignalEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("processTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).
                extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        //SINCE THE LAST EXECUTION ON THE PARENT SCOPE WAS MOVED, THERE SHOULD BE NO EVENT SUBSCRIPTION
        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleNonInterruptingMessageEventSubProcess.bpmn20.xml" })
    public void testSetCurrentOnlyActivityToActivityInsideNonInterruptingMessageEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("processTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "eventMessage");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        //SINCE THE LAST EXECUTION ON THE PARENT SCOPE WAS MOVED, THERE SHOULD BE NO EVENT SUBSCRIPTION//SINCE THE LAST EXECUTION ON THE PARENT SCOPE WAS MOVED, THERE SHOULD BE NO EVENT SUBSCRIPTION
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleNonInterruptingTimerEventSubProcess.bpmn20.xml" })
    public void testSetCurrentOnlyActivityToActivityInsideNonInterruptingTimerEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("processTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("processTask");

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        //SINCE THE LAST EXECUTION ON THE PARENT SCOPE WAS MOVED, THERE SHOULD BE NO EVENT SUBSCRIPTION
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelSignalEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideSignalEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TAES");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcessStart");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "parallelTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelMessageEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideMessageEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TAES");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcessStart");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "myMessage");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "parallelTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelTimerEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideTimerEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TAES");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcessStart");

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId())
                .list().stream()
                .filter(j -> "spawnParallelTask".equals(getJobActivityId(j)))
                .findFirst().get();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "parallelTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs)
                .extracting(this::getJobActivityId)
                .containsExactly("eventSubProcessStart");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNonInterruptingSignalEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNonInterruptingSignalEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TAES");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcessStart");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "parallelTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcess", "eventSubProcessStart", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("eventSubProcessTask", "processTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        //Fire the signal
        runtimeService.signalEventReceived("eventSignal");

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcess", "eventSubProcess", "eventSubProcessStart",
                        "eventSubProcessTask", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("eventSubProcessTask", "eventSubProcessTask", "processTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNonInterruptingMessageEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNonInterruptingMessageEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TAES");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcessStart");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "myMessage");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "parallelTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcess", "eventSubProcessTask", "eventSubProcessStart");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("eventSubProcessTask", "processTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "myMessage");

        //Trigger the event
        Execution messageSubscriptionExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId())
                .messageEventSubscriptionName("myMessage").singleResult();
        runtimeService.messageEventReceived("myMessage", messageSubscriptionExecution.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcess", "eventSubProcess", "eventSubProcessStart",
                        "eventSubProcessTask", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("eventSubProcessTask", "eventSubProcessTask", "processTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "myMessage");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNonInterruptingTimerEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNonInterruptingTimerEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TAES");

        List<Execution> executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcessStart");

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs)
                .extracting(this::getJobActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "eventSubProcessStart");

        //Spawn the parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId())
                .list().stream()
                .filter(j -> "spawnParallelTask".equals(getJobActivityId(j)))
                .findFirst().get();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs)
                .extracting(this::getJobActivityId)
                .containsExactly("eventSubProcessStart");

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "parallelTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcess", "eventSubProcessStart", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("eventSubProcessTask", "processTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job)
                .extracting(this::getJobActivityId)
                .isEqualTo("eventSubProcessStart");

        //Trigger the eventSubProcess timer
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("spawnParallelTask", "processTask", "eventSubProcess", "eventSubProcess", "eventSubProcessStart",
                        "eventSubProcessTask", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("eventSubProcessTask", "eventSubProcessTask", "processTask");

        jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs)
                .extracting(this::getJobActivityId)
                .containsExactly("eventSubProcessStart");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedSignalEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityAtParentSubProcessOfNestedSignalEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("processTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("processTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "subProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        //Trigger the event
        runtimeService.signalEventReceived("eventSignal");

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessStart", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedMessageEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityAtParentSubProcessOfNestedMessageEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("processTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("processTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "subProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "eventMessage");

        //Trigger the event
        runtimeService.messageEventReceived("eventMessage", eventSubscription.getExecutionId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessStart", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedTimerEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityAtParentSubProcessOfNestedTimerEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("processTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("processTask");

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("processTask", "subProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job)
                .extracting(this::getJobActivityId)
                .isEqualTo("eventSubProcessStart");

        //Fire the timer
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessStart", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedSignalEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideNestedSignalEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Move the current task inside the subProcess
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subProcessTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedMessageEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideNestedMessageEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Move the current task inside the subProcess
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).
                extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "eventMessage");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subProcessTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedTimerEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideNestedTimerEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Move the current task inside the subProcess
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job)
                .extracting(this::getJobActivityId)
                .isEqualTo("eventSubProcessStart");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subProcessTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedNonInterruptingSignalEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideNestedNonInterruptingSignalEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Move the current task inside the subProcess
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "signal", "eventSignal");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subProcessTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        //SINCE THE LAST EXECUTION ON THE PARENT SCOPE WAS MOVED, THERE SHOULD BE NO EVENT SUBSCRIPTION
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedNonInterruptingMessageEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideNestedNonInterruptingMessageEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Move the current task inside the subProcess
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("eventSubProcessStart", "message", "eventMessage");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subProcessTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        //SINCE THE LAST EXECUTION ON THE PARENT SCOPE WAS MOVED, THERE SHOULD BE NO EVENT SUBSCRIPTION
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.nestedNonInterruptingTimerEventSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToActivityInsideNestedNonInterruptingTimerEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Move the current task inside the subProcess
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "eventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessTask");

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job)
                .extracting(this::getJobActivityId)
                .isEqualTo("eventSubProcessStart");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subProcessTask", "eventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "eventSubProcess", "eventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("eventSubProcessTask");

        //SINCE THE LAST EXECUTION ON THE PARENT SCOPE WAS MOVED, THERE SHOULD BE NO EVENT SUBSCRIPTION
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedSignalEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedSignalEventSubProcessWithActivityAtRootScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "parallelTask", "spawnParallelTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "signalEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "signalEventSubProcess", "signalEventSubProcessTask", "processTask", "spawnParallelTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("signalEventSubProcessTask", "processTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedSignalEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedSignalEventSubProcessWithActivityAtParentScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Move the current task inside the subProcess
        completeTask(currentTask);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("parallelTask", "subProcess", "subProcessTask", "signalEventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("signalEventSubProcessStart", "signal", "eventSignal");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "signalEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "signalEventSubProcess", "signalEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("signalEventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedMessageEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedMessageEventSubProcessWithActivityAtRootScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "parallelTask", "spawnParallelTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "messageEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "messageEventSubProcess", "messageEventSubProcessTask", "processTask", "spawnParallelTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("messageEventSubProcessTask", "processTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedMessageEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedMessageEventSubProcessWithActivityAtParentScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Move the current task inside the subProcess
        completeTask(currentTask);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("parallelTask", "subProcess", "subProcessTask", "messageEventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("messageEventSubProcessStart", "message", "eventMessage");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "messageEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "messageEventSubProcess", "messageEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("messageEventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedTimerEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedTimerEventSubProcessWithActivityAtRootScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "parallelTask", "spawnParallelTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        //The TimerEvent SubProcess stat time is not registered
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "timerEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "timerEventSubProcess", "timerEventSubProcessTask", "processTask", "spawnParallelTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("timerEventSubProcessTask", "processTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedTimerEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedTimerEventSubProcessWithActivityAtParentScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Move the current task inside the subProcess
        completeTask(currentTask);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("parallelTask", "subProcess", "subProcessTask", "timerEventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "parallelTask");

        //The TimerEvent SubProcess stat time is not registered
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job)
                .extracting(this::getJobActivityId)
                .isEqualTo("timerEventSubProcessStart");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "timerEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "timerEventSubProcess", "timerEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("timerEventSubProcessTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedNonInterruptingSignalEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedNonInterruptingSignalEventSubProcessWithActivityAtRootScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "parallelTask", "spawnParallelTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "signalEventSubProcessTask")
                .changeState();

        //Behaves like interrupting eventSubProcess since there are other executions at the parentScope
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "signalEventSubProcess", "signalEventSubProcessTask", "processTask", "spawnParallelTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("signalEventSubProcessTask", "processTask");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedNonInterruptingSignalEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedNonInterruptingSignalEventSubProcessWithActivityAtParentScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Move the current task inside the subProcess
        completeTask(currentTask);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("parallelTask", "subProcess", "subProcessTask", "signalEventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("signalEventSubProcessStart", "signal", "eventSignal");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "signalEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "signalEventSubProcessStart", "signalEventSubProcess", "signalEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "signalEventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("signalEventSubProcessStart", "signal", "eventSignal");

        //We can trigger the event again
        runtimeService.signalEventReceived("eventSignal");

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "signalEventSubProcessStart", "signalEventSubProcess", "signalEventSubProcessTask",
                        "signalEventSubProcess", "signalEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "signalEventSubProcessTask", "signalEventSubProcessTask");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedNonInterruptingMessageEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedNonInterruptingMessageEventSubProcessWithActivityAtRootScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "parallelTask", "spawnParallelTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "messageEventSubProcessTask")
                .changeState();

        //Behaves like interrupting eventSubProcess since there are other executions at the parentScope
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "messageEventSubProcess", "messageEventSubProcessTask", "processTask", "spawnParallelTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("messageEventSubProcessTask", "processTask");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedNonInterruptingMessageEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedNonInterruptingMessageEventSubProcessWithActivityAtParentScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Move the current task inside the subProcess
        completeTask(currentTask);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("parallelTask", "subProcess", "subProcessTask", "messageEventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("messageEventSubProcessStart", "message", "eventMessage");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "messageEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "messageEventSubProcessStart", "messageEventSubProcess",
                        "messageEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "messageEventSubProcessTask");

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType, EventSubscription::getEventName)
                .containsExactly("messageEventSubProcessStart", "message", "eventMessage");

        //We can trigger the event again
        Execution messageSubscriptionExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId())
                .messageEventSubscriptionName("eventMessage").singleResult();
        runtimeService.messageEventReceived("eventMessage", messageSubscriptionExecution.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "messageEventSubProcessStart", "messageEventSubProcess",
                        "messageEventSubProcessTask", "messageEventSubProcess", "messageEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "messageEventSubProcessTask", "messageEventSubProcessTask");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedNonInterruptingTimerEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedNonInterruptingTimerEventSubProcessWithActivityAtRootScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("processTask", "parallelTask", "spawnParallelTask");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "timerEventSubProcessTask")
                .changeState();

        //Behaves like interrupting eventSubProcess since there are other executions at the parentScope
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "timerEventSubProcess", "timerEventSubProcessTask", "processTask", "spawnParallelTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("timerEventSubProcessTask", "processTask");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.TimerParallelNestedNonInterruptingTimerEventSubProcess.bpmn20.xml" })
    public void testSetParallelActivityToActivityInsideNestedNonInterruptingTimerEventSubProcessWithActivityAtParentScope() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForEventSubProcess");

        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        //Spawn a parallel task
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Move the current task inside the subProcess
        completeTask(currentTask);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("parallelTask", "subProcess", "subProcessTask", "timerEventSubProcessStart");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "parallelTask");

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs)
                .extracting(this::getJobActivityId)
                .contains("timerEventSubProcessStart");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("parallelTask", "timerEventSubProcessTask")
                .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "timerEventSubProcessStart", "timerEventSubProcess", "timerEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "timerEventSubProcessTask");

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job)
                .extracting(this::getJobActivityId)
                .isEqualTo("timerEventSubProcessStart");

        //We can trigger the event again
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subProcessTask", "timerEventSubProcessStart", "timerEventSubProcess", "timerEventSubProcessTask",
                        "timerEventSubProcess", "timerEventSubProcessTask");

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subProcessTask", "timerEventSubProcessTask", "timerEventSubProcessTask");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

}
