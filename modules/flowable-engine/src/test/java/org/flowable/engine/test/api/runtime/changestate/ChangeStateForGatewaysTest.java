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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Dennis Federico
 */
public class ChangeStateForGatewaysTest extends PluggableFlowableTestCase {

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
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("task1");
        newActivityIds.add("task2");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("taskBefore", newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));
        assertFalse(executionsByActivity.containsKey("parallelJoin"));

        //Complete one task1
        Optional<Task> task1 = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("task1")).findFirst();
        if (task1.isPresent()) {
            taskService.complete(task1.get().getId());
        }

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("task1", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("task2", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertFalse(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));
        assertTrue(executionsByActivity.containsKey("parallelJoin"));

        assertFalse(((ExecutionEntity) executionsByActivity.get("parallelJoin").get(0)).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetMultipleActivitiesToSingleActivityAfterParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("task1");
        currentActivityIds.add("task2");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskAfter")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("task1", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("task2", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskAfter", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetMultipleActivitiesIntoSynchronizingParallelGateway() {

        //Move all parallelGateway activities to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(Arrays.asList("task1", "task2"), "parallelJoin")
            .changeState();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertEquals("taskAfter", execution.getActivityId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetMultipleGatewayActivitiesAndSynchronizingParallelGatewayAfterGateway() {

        //Move all parallelGateway activities to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        //Complete task1
        for (Task t : tasks) {
            if (t.getTaskDefinitionKey().equals("task1")) {
                taskService.complete(t.getId());
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("task2"));
        assertEquals(1, classifiedExecutions.get("task2").size());
        assertNotNull(classifiedExecutions.get("parallelJoin"));
        assertEquals(1, classifiedExecutions.get("parallelJoin").size());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(Arrays.asList("task2", "parallelJoin"), "taskAfter")
            .changeState();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertEquals("taskAfter", execution.getActivityId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetActivityIntoSynchronizingParallelGatewayFirst() {

        //Move one task to the synchronizing gateway, then complete the remaining task
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("task1", "parallelJoin")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNotNull(executionsByActivity.get("parallelJoin"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task2", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetActivityIntoSynchronizingParallelGatewayLast() {

        //Complete one task and then move the last remaining task to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        //Complete task1
        Optional<Task> task1 = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("task1")).findFirst();
        if (task1.isPresent()) {
            taskService.complete(task1.get().getId());
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNotNull(executionsByActivity.get("parallelJoin"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task2", task.getTaskDefinitionKey());

        //Move task2
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("task2", "parallelJoin")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetCurrentExecutionToMultipleActivitiesForParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        Execution taskBeforeExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("task1");
        newActivityIds.add("task2");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleExecutionToActivityIds(taskBeforeExecution.getId(), newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNotNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNull(executionsByActivity.get("parallelJoin"));

        //Complete one task1
        Optional<Task> task1 = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("task1")).findFirst();
        if (task1.isPresent()) {
            taskService.complete(task1.get().getId());
        }

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertFalse(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));
        assertTrue(executionsByActivity.containsKey("parallelJoin"));

        assertFalse(((ExecutionEntity) executionsByActivity.get("parallelJoin").get(0)).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetMultipleExecutionsToSingleActivityAfterParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        List<String> currentExecutionIds = new ArrayList<>();
        currentExecutionIds.add(executions.get(0).getId());
        currentExecutionIds.add(executions.get(1).getId());
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(currentExecutionIds, "taskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetMultipleExecutionsIntoSynchronizingParallelGateway() {

        //Move all gateway executions to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<String> executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(executionIds, "parallelJoin")
            .changeState();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertEquals("taskAfter", execution.getActivityId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetMultipleGatewayExecutionsAndSynchronizingParallelGatewayAfterGateway() {

        //Move all gateway executions to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        //Complete task1
        for (Task t : tasks) {
            if (t.getTaskDefinitionKey().equals("task1")) {
                taskService.complete(t.getId());
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("task2"));
        assertEquals(1, classifiedExecutions.get("task2").size());
        assertNotNull(classifiedExecutions.get("parallelJoin"));
        assertEquals(1, classifiedExecutions.get("parallelJoin").size());

        List<String> executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(executionIds, "taskAfter")
            .changeState();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertEquals("taskAfter", execution.getActivityId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetExecutionIntoSynchronizingParallelGatewayFirst() {

        //Move one task to the synchronizing gateway, then complete the remaining task
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        String executionId = executions.stream().filter(e -> "task1".equals(e.getActivityId())).findFirst().map(Execution::getId).get();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(executionId, "parallelJoin")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNotNull(executionsByActivity.get("parallelJoin"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task2", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetExecutionIntoSynchronizingParallelGatewayLast() {

        //Complete one task and then move the last remaining task to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        //Complete task1
        Optional<Task> task1 = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("task1")).findFirst();
        if (task1.isPresent()) {
            taskService.complete(task1.get().getId());
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNotNull(executionsByActivity.get("parallelJoin"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task2", task.getTaskDefinitionKey());

        //Move task2 execution
        String executionId = executions.stream().filter(e -> "task2".equals(e.getActivityId())).findFirst().map(Execution::getId).get();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(executionId, "parallelJoin")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForInclusiveGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("task1");
        newActivityIds.add("task2");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("taskBefore", newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));
        assertFalse(executionsByActivity.containsKey("gwJoin"));

        //Complete one task1
        Optional<Task> task1 = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("task1")).findFirst();
        if (task1.isPresent()) {
            taskService.complete(task1.get().getId());

        }

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertFalse(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));
        assertTrue(executionsByActivity.containsKey("gwJoin"));

        assertFalse(((ExecutionEntity) executionsByActivity.get("gwJoin").get(0)).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetMultipleActivitiesToSingleActivityAfterInclusiveGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("task1");
        currentActivityIds.add("task2");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetMultipleActivitiesIntoSynchronizingInclusiveGateway() {

        //Move all parallelGateway activities to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(Arrays.asList("task1", "task2"), "gwJoin")
            .changeState();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertEquals("taskAfter", execution.getActivityId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetMultipleGatewayActivitiesAndSynchronizingInclusiveGatewayAfterGateway() {

        //Move all parallelGateway activities to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        //Complete task1
        for (Task t : tasks) {
            if (t.getTaskDefinitionKey().equals("task1")) {
                taskService.complete(t.getId());
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("task2"));
        assertEquals(1, classifiedExecutions.get("task2").size());
        assertNotNull(classifiedExecutions.get("gwJoin"));
        assertEquals(1, classifiedExecutions.get("gwJoin").size());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(Arrays.asList("task2", "gwJoin"), "taskAfter")
            .changeState();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertEquals("taskAfter", execution.getActivityId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetActivityIntoSynchronizingParallelInclusiveFirst() {

        //Move one task to the synchronizing gateway, then complete the remaining task
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("task1", "gwJoin")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNotNull(executionsByActivity.get("gwJoin"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertEquals("task2", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetActivityIntoSynchronizingParallelInclusiveLast() {

        //Complete one task and then move the last remaining task to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        //Complete task1
        Optional<Task> task1 = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("task1")).findFirst();
        if (task1.isPresent()) {
            taskService.complete(task1.get().getId());
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNotNull(executionsByActivity.get("gwJoin"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task2", task.getTaskDefinitionKey());

        //Move task2
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("task2", "gwJoin")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetCurrentExecutionToMultipleActivitiesForInclusiveGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        Execution taskBeforeExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("task1");
        newActivityIds.add("task2");
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleExecutionToActivityIds(taskBeforeExecution.getId(), newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNotNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNull(executionsByActivity.get("gwJoin"));

        //Complete one task1
        Optional<Task> task1 = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("task1")).findFirst();
        if (task1.isPresent()) {
            taskService.complete(task1.get().getId());
        }

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertFalse(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));
        assertTrue(executionsByActivity.containsKey("gwJoin"));

        assertFalse(((ExecutionEntity) executionsByActivity.get("gwJoin").get(0)).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetMultipleExecutionsToSingleActivityAfterInclusiveGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        List<String> currentExecutionIds = new ArrayList<>();
        currentExecutionIds.add(executions.get(0).getId());
        currentExecutionIds.add(executions.get(1).getId());
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(currentExecutionIds, "taskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetMultipleExecutionsIntoSynchronizingInclusiveGateway() {

        //Move all gateway executions to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<String> executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(executionIds, "gwJoin")
            .changeState();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertEquals("taskAfter", execution.getActivityId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetMultipleGatewayExecutionsAndSynchronizingInclusiveGatewayAfterGateway() {

        //Move all gateway executions to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertTrue(executionsByActivity.containsKey("task1"));
        assertTrue(executionsByActivity.containsKey("task2"));

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        //Complete task1
        for (Task t : tasks) {
            if (t.getTaskDefinitionKey().equals("task1")) {
                taskService.complete(t.getId());
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("task2"));
        assertEquals(1, classifiedExecutions.get("task2").size());
        assertNotNull(classifiedExecutions.get("gwJoin"));
        assertEquals(1, classifiedExecutions.get("gwJoin").size());

        List<String> executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(executionIds, "taskAfter")
            .changeState();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertEquals("taskAfter", execution.getActivityId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetExecutionIntoSynchronizingInclusiveGatewayFirst() {

        //Move one task to the synchronizing gateway, then complete the remaining task
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        String executionId = executions.stream().filter(e -> "task1".equals(e.getActivityId())).findFirst().map(Execution::getId).get();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(executionId, "gwJoin")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNotNull(executionsByActivity.get("gwJoin"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task2", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayForkJoin.bpmn20.xml" })
    public void testSetExecutionIntoSynchronizingInclusiveGatewayLast() {

        //Complete one task and then move the last remaining task to the synchronizing gateway
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startInclusiveGwProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        //Complete task1
        Optional<Task> task1 = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("task1")).findFirst();
        if (task1.isPresent()) {
            taskService.complete(task1.get().getId());
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        Map<String, List<Execution>> executionsByActivity = groupListContentBy(executions, Execution::getActivityId);
        assertNull(executionsByActivity.get("task1"));
        assertNotNull(executionsByActivity.get("task2"));
        assertNotNull(executionsByActivity.get("gwJoin"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task2", task.getTaskDefinitionKey());

        //Move task2 execution
        String executionId = executions.stream().filter(e -> "task2".equals(e.getActivityId())).findFirst().map(Execution::getId).get();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(executionId, "gwJoin")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForParallelSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("subtask");
        newActivityIds.add("subtask2");
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("taskBefore", newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        Optional<Execution> parallelJoinExecution = executions.stream().filter(e -> e.getActivityId().equals("parallelJoin")).findFirst();
        assertFalse(parallelJoinExecution.isPresent());

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executions.size());

        parallelJoinExecution = executions.stream().filter(e -> e.getActivityId().equals("parallelJoin")).findFirst();
        assertTrue(parallelJoinExecution.isPresent());
        assertFalse(((ExecutionEntity) parallelJoinExecution.get()).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelSubProcesses.bpmn20.xml" })
    public void testSetMultipleActivitiesToSingleActivityAfterParallelSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("subtask");
        currentActivityIds.add("subtask2");
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelSubProcessesMultipleTasks.bpmn20.xml" })
    public void testMoveCurrentActivityInParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess1")
            .singleResult();
        String subProcessExecutionId = subProcessExecution.getId();
        runtimeService.setVariableLocal(subProcessExecutionId, "subProcessVar", "test");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subtask", "subtask2")
            .changeState();

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        subProcessExecution = runtimeService.createExecutionQuery().executionId(subProcessExecutionId).singleResult();
        assertNotNull(subProcessExecution);
        assertEquals("test", runtimeService.getVariableLocal(subProcessExecutionId, "subProcessVar"));

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executions.size());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/multipleParallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForInclusiveAndParallelSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess", Collections.singletonMap("var1", "test2"));
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("taskInclusive3");
        newActivityIds.add("subtask");
        newActivityIds.add("subtask3");
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("taskBefore", newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(5, executions.size());

        Optional<Execution> parallelJoinExecution = executions.stream().filter(e -> e.getActivityId().equals("parallelJoin")).findFirst();
        assertFalse(parallelJoinExecution.isPresent());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask").singleResult();
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask2").singleResult();
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        parallelJoinExecution = executions.stream().filter(e -> e.getActivityId().equals("parallelJoin")).findFirst();
        assertTrue(parallelJoinExecution.isPresent());
        assertFalse(((ExecutionEntity) parallelJoinExecution.get()).isActive());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask3").singleResult();
        taskService.complete(task.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive3").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/multipleParallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivitiesToSingleActivityForInclusiveAndParallelSubProcesses() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("var1", "test2");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess", variableMap);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive1").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());

        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive3").count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask").count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask3").count());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("taskInclusive3");
        currentActivityIds.add("subtask");
        currentActivityIds.add("subtask3");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskAfter")
            .changeState();

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/multipleParallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivitiesToSingleActivityInInclusiveGateway() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("var1", "test2");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess", variableMap);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive1").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());

        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive3").count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask").count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask3").count());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("subtask");
        currentActivityIds.add("subtask3");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskInclusive1")
            .changeState();

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive3").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive1").singleResult();
        taskService.complete(task.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(5, executions.size());

        Optional<Execution> inclusiveJoinExecution = executions.stream().filter(e -> e.getActivityId().equals("inclusiveJoin")).findFirst();
        assertTrue(inclusiveJoinExecution.isPresent());
        assertFalse(((ExecutionEntity) inclusiveJoinExecution.get()).isActive());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask3").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask2").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

}
