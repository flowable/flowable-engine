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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.variable.api.event.FlowableVariableEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Dennis Federico
 */
public class ChangeStateTest extends PluggableFlowableTestCase {

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
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityBackwardForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("secondTask", "firstTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentExecutionBackwardForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "firstTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityForwardForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("firstTask", "secondTask")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentExecutionForwardForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "secondTask")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();

        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("firstTask", "secondTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentExecutionWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "secondTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityToActivityWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("secondTask", "firstTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentExecutionToActivityWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "firstTask")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimers.bpmn20.xml" })
    public void testSetCurrentActivityWithTimerToActivityWithTimerSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcessWithTimers");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job timerJob1 = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();
        assertNotNull(timerJob1);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("firstTask", "secondTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob2);
        assertTrue(!timerJob1.getExecutionId().equals(timerJob2.getExecutionId()));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("firstTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        entityEvent = (FlowableEngineEntityEvent) event;
        timer = (Job) entityEvent.getEntity();
        assertEquals("secondTimerEvent", getJobActivityId(timer));

        assertTrue(!iterator.hasNext());

        Job job = managementService.moveTimerToExecutableJob(timerJob2.getId());
        managementService.executeJob(job.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("thirdTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityOutOfSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "taskBefore")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "taskBefore")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityIntoSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionIntoSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityIntoSubProcessWithModeledDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess")
            .singleResult();
        assertNotNull(runtimeService.getVariableLocal(subProcessExecution.getId(), "name", String.class));
        DataObject nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionIntoSubProcessWithModeledDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess")
            .singleResult();
        assertNotNull(runtimeService.getVariableLocal(subProcessExecution.getId(), "name", String.class));

        DataObject nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityOutOfSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "taskBefore")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "taskBefore")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityToTaskInSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentExecutionToTaskInSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityToTaskInSubProcessAndExecuteTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityOutOfSubProcessTaskWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "subTask2")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask2", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfSubProcessTaskWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask2")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask2", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToTaskWithTimerInSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask2", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionToTaskWithTimerInSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();

        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask2", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToTaskWithTimerInSubProcessAndExecuteTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        assertTrue(!iterator.hasNext());

        Job executableTimerJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableTimerJob.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityIntoNestedSubProcessExecutionFromRoot() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "nestedSubTask")
            .changeState();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(3);
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSubProcess", "nestedSubTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.oneTaskNestedSubProcessWithObject.bpmn20.xml" })
    public void testSetCurrentActivityIntoNestedSubProcessExecutionFromRootWithDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "nestedSubTask")
            .changeState();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(3);
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSubProcess", "nestedSubTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        Execution nestedSubProcess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("nestedSubProcess").singleResult();
        assertNotNull(runtimeService.getVariableLocal(nestedSubProcess.getId(), "name", String.class));
        DataObject nameDataObject = runtimeService.getDataObjectLocal(nestedSubProcess.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent) event).getVariableValue());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionIntoNestedSubProcessExecutionFromRoot() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "nestedSubTask")
            .changeState();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(3);
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSubProcess", "nestedSubTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.oneTaskNestedSubProcessWithObject.bpmn20.xml" })
    public void testSetCurrentExecutionIntoNestedSubProcessExecutionFromRootWithDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "nestedSubTask")
            .changeState();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(3);
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSubProcess", "nestedSubTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        Execution nestedSubProcess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("nestedSubProcess").singleResult();
        assertNotNull(runtimeService.getVariableLocal(nestedSubProcess.getId(), "name", String.class));
        DataObject nameDataObject = runtimeService.getDataObjectLocal(nestedSubProcess.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent) event).getVariableValue());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityIntoNestedSubProcessExecutionFromOuter() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "nestedSubTask")
            .changeState();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(3);
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSubProcess", "nestedSubTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.oneTaskNestedSubProcessWithObject.bpmn20.xml" })
    public void testSetCurrentActivityIntoNestedSubProcessExecutionFromOuterWithDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "nestedSubTask")
            .changeState();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(3);
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSubProcess", "nestedSubTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        Execution nestedSubProcess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("nestedSubProcess").singleResult();
        assertNotNull(runtimeService.getVariableLocal(nestedSubProcess.getId(), "name", String.class));
        DataObject nameDataObject = runtimeService.getDataObjectLocal(nestedSubProcess.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent) event).getVariableValue());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionIntoNestedSubProcessExecutionFromOuter() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "nestedSubTask")
            .changeState();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(3);
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSubProcess", "nestedSubTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.oneTaskNestedSubProcessWithObject.bpmn20.xml" })
    public void testSetCurrentExecutionIntoNestedSubProcessExecutionFromOuterWithDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(2);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "nestedSubTask")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(3);
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "nestedSubProcess", "nestedSubTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        Execution nestedSubProcess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("nestedSubProcess").singleResult();
        assertNotNull(runtimeService.getVariableLocal(nestedSubProcess.getId(), "name", String.class));
        DataObject nameDataObject = runtimeService.getDataObjectLocal(nestedSubProcess.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent) event).getVariableValue());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityOutOfNestedSubProcessExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("nestedSubTask", "subTaskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTaskAfter", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfNestedSubProcessExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTaskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTaskAfter", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityOutOfNestedSubProcessExecutionIntoContainingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("nestedSubTask", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfNestedSubProcessExecutionIntoContainingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/taskTwoSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivityFromSubProcessToAnotherSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoSubProcesses");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subtask", "subtask2")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask2", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/taskTwoSubProcesses.bpmn20.xml" })
    public void testSetCurrentExecutionFromSubProcessToAnotherSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoSubProcesses");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subtask2")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask2", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityForSubProcessWithVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "taskBefore")
            .processVariable("processVar1", "test")
            .processVariable("processVar2", 10)
            .localVariable("taskBefore", "localVar1", "test2")
            .localVariable("taskBefore", "localVar2", 20)
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        Map<String, Object> processVariables = runtimeService.getVariables(processInstance.getId());
        assertEquals("test", processVariables.get("processVar1"));
        assertEquals(10, processVariables.get("processVar2"));
        assertNull(processVariables.get("localVar1"));
        assertNull(processVariables.get("localVar2"));

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("taskBefore").singleResult();
        Map<String, Object> localVariables = runtimeService.getVariablesLocal(execution.getId());
        assertEquals("test2", localVariables.get("localVar1"));
        assertEquals(20, localVariables.get("localVar2"));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("processVar2", ((FlowableVariableEvent) event).getVariableName());
        assertEquals(10, ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("processVar1", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("test", ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("localVar2", ((FlowableVariableEvent) event).getVariableName());
        assertEquals(20, ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("localVar1", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("test2", ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionForSubProcessWithVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "taskBefore")
            .processVariable("processVar1", "test")
            .processVariable("processVar2", 10)
            .localVariable("taskBefore", "localVar1", "test2")
            .localVariable("taskBefore", "localVar2", 20)
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        Map<String, Object> processVariables = runtimeService.getVariables(processInstance.getId());
        assertEquals("test", processVariables.get("processVar1"));
        assertEquals(10, processVariables.get("processVar2"));
        assertNull(processVariables.get("localVar1"));
        assertNull(processVariables.get("localVar2"));

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("taskBefore").singleResult();
        Map<String, Object> localVariables = runtimeService.getVariablesLocal(execution.getId());
        assertEquals("test2", localVariables.get("localVar1"));
        assertEquals(20, localVariables.get("localVar2"));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("processVar2", ((FlowableVariableEvent) event).getVariableName());
        assertEquals(10, ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("processVar1", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("test", ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("localVar2", ((FlowableVariableEvent) event).getVariableName());
        assertEquals(20, ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("localVar1", ((FlowableVariableEvent) event).getVariableName());
        assertEquals("test2", ((FlowableVariableEvent) event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityInUnstartedSubProcessWithModeledDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess").singleResult();
        assertNotNull(runtimeService.getVariableLocal(subProcessExecution.getId(), "name", String.class));

        DataObject nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityInUnstartedSubProcessWithLocalVariableOnSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .localVariable("subProcess", "name", "Joe")
            .changeState();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess").singleResult();
        assertNotNull(runtimeService.getVariableLocal(subProcessExecution.getId(), "name", String.class));

        DataObject nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("Joe", nameDataObject.getValue());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
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

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInParentProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertEquals("theTask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(subProcessInstance.getId())
            .moveActivityIdToParentActivityId("theTask", "secondTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        assertEquals(0, runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity")
            .changeState();

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        assertEquals(0, taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).count());

        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count());
        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().count());

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertEquals("theTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceSequential.bpmn20.xml")
    public void testSetCurrentActivityOfSequentialMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("sequentialMultiInstance")
            .variable("nrOfLoops", 5)
            .start();

        List<Execution> seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, seqExecutions.size());
        List<Task> activeSeqTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(1, activeSeqTasks.size());

        //First in the loop
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(0, taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        //Second in the loop
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(1, taskService.getVariable(task.getId(), "loopCounter"));

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("seqTasks", "nextTask")
            .changeState();

        seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, seqExecutions.size());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        assertNull(taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceSequential.bpmn20.xml")
    public void testSetCurrentParentExecutionOfSequentialMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("sequentialMultiInstance")
            .variable("nrOfLoops", 5)
            .start();

        List<Execution> seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, seqExecutions.size());
        List<Task> activeSeqTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(1, activeSeqTasks.size());

        //First in the loop
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(0, taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        //Second in the loop
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(1, taskService.getVariable(task.getId(), "loopCounter"));

        //move the parent execution - otherwise the parent multi instance execution remains, although active==false.
        String parentExecutionId = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult().getParentId();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(parentExecutionId, "nextTask")
            .changeState();

        seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, seqExecutions.size());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        assertNull(taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallel.bpmn20.xml")
    public void testSetCurrentActivityOfParallelMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstance")
            .variable("nrOfLoops", 3)
            .start();

        List<Execution> parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        List<Task> activeParallelTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(3, activeParallelTasks.size());

        //Complete one of the tasks
        taskService.complete(activeParallelTasks.get(1).getId());
        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        activeParallelTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(2, activeParallelTasks.size());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelTasks", "nextTask")
            .changeState();

        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, parallelExecutions.size());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallel.bpmn20.xml")
    public void testSetCurrentParentExecutionOfParallelMultiInstanceTask() {
        ProcessInstance parallelTasksProcInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstance")
            .variable("nrOfLoops", 3)
            .start();

        List<Execution> parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(parallelTasksProcInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        List<Task> activeParallelTasks = taskService.createTaskQuery().processInstanceId(parallelTasksProcInstance.getId()).active().list();
        assertEquals(3, activeParallelTasks.size());

        //Complete one of the tasks
        taskService.complete(activeParallelTasks.get(1).getId());
        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(parallelTasksProcInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        activeParallelTasks = taskService.createTaskQuery().processInstanceId(parallelTasksProcInstance.getId()).active().list();
        assertEquals(2, activeParallelTasks.size());

        //Fetch the parent execution of the multi instance task execution
        String parentExecutionId = runtimeService.createExecutionQuery().executionId(activeParallelTasks.get(0).getExecutionId()).singleResult().getParentId();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(parentExecutionId, "nextTask")
            .changeState();

        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(parallelTasksProcInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, parallelExecutions.size());
        Task task = taskService.createTaskQuery().processInstanceId(parallelTasksProcInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(parallelTasksProcInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentExecutionWithinMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Move one of the executions within the multiInstance subProcess
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(subTask1Executions.get(0).getId(), "subTask2")
            .changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Complete one of the parallel subProcesses "subTask2"
        Task task = taskService.createTaskQuery().executionId(subTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(0, subTask2Executions.size());

        //Move the other two executions, one by one
        ChangeActivityStateBuilder changeActivityStateBuilder = runtimeService.createChangeActivityStateBuilder();
        subTask1Executions.forEach(e -> changeActivityStateBuilder.moveExecutionToActivityId(e.getId(), "subTask2"));
        changeActivityStateBuilder.changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(0, subTask1Executions.size());
        subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(2, subTask2Executions.size());

        //Complete the rest of the SubProcesses
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(2, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentActivityWithinMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Move one of the executions within the multiInstance subProcess
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask1", "subTask2")
            .changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(0, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the parallel subProcesses "subTask2"
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        Task task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentExecutionWithinNestedMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Move one of the executions within of the nested multiInstance subProcesses
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(nestedSubTask1Executions.get(0).getId(), "nestedSubTask2")
            .moveExecutionToActivityId(nestedSubTask1Executions.get(3).getId(), "nestedSubTask2")
            .moveExecutionToActivityId(nestedSubTask1Executions.get(6).getId(), "nestedSubTask2")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the outer subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move the rest of the nestedSubTask1 executions
        ChangeActivityStateBuilder changeActivityStateBuilder = runtimeService.createChangeActivityStateBuilder();
        nestedSubTask1Executions.forEach(e -> changeActivityStateBuilder.moveExecutionToActivityId(e.getId(), "nestedSubTask2"));
        changeActivityStateBuilder.changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(0, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(8, nestedSubTask2Executions.size());

        //Complete all the nestedSubTask2
        tasks = taskService.createTaskQuery().taskDefinitionKey("nestedSubTask2").list();
        assertEquals(8, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentActivityWithinNestedMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete one task for each nestedSubProcess
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Moving the nestedSubTask1 activity should move all its executions
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("nestedSubTask1", "nestedSubTask2")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(0, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(9, nestedSubTask2Executions.size());

        //Complete all the nestedSubTask2
        tasks = taskService.createTaskQuery().taskDefinitionKey("nestedSubTask2").list();
        assertEquals(9, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        Task task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentMultiInstanceSubProcessParentExecutionWithinProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Complete one of the Tasks
        Task task = taskService.createTaskQuery().executionId(subTask1Executions.get(1).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Move the parallelSubProcess via the parentExecution Ids
        String ParallelSubProcessParentExecutionId = runtimeService.createExecutionQuery()
            .processInstanceId(processInstance.getId())
            .activityId("parallelSubProcess")
            .list()
            .stream()
            .findFirst()
            .map(Execution::getParentId)
            .get();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(ParallelSubProcessParentExecutionId, "lastTask")
            .changeState();

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentMultiInstanceSubProcessParentActivityWithinProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Complete one of the Tasks
        Task task = taskService.createTaskQuery().executionId(subTask1Executions.get(1).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Move the parallelSubProcess
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelSubProcess", "lastTask")
            .changeState();

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentMultiInstanceNestedSubProcessParentExecutionWithinSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete some of the Nested tasks
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the nested subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move each nested multiInstance parent
        Stream<String> parallelNestedSubProcessesParentIds = runtimeService.createExecutionQuery()
            .processInstanceId(processInstance.getId())
            .activityId("parallelNestedSubProcess")
            .list()
            .stream()
            .map(Execution::getParentId)
            .distinct();

        parallelNestedSubProcessesParentIds.forEach(parentId -> {
            runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(parentId, "subTask2")
                .changeState();
        });

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentMultiInstanceNestedSubProcessParentActivityWithinSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete some of the Nested tasks
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the nested subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move the activity nested multiInstance parent
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelNestedSubProcess", "subTask2")
            .changeState();

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelGatewayInsideMultiInstanceSubProcess.bpmn20.xml" })
    public void testSetCurrentActivitiesUsingParallelGatewayNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGatewayInsideMultiInstanceSubProcess");

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> preForkTaskExecutions = runtimeService.createExecutionQuery().activityId("preForkTask").list();
        assertEquals(3, preForkTaskExecutions.size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        assertEquals("preForkTask", tasks.get(0).getTaskDefinitionKey());

        //Move a task before the fork within the multiInstance subProcess
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("preForkTask", Arrays.asList("forkTask1", "forkTask2"))
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(10, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> forkTask1Executions = runtimeService.createExecutionQuery().activityId("forkTask1").list();
        assertEquals(3, forkTask1Executions.size());
        List<Execution> forkTask2Executions = runtimeService.createExecutionQuery().activityId("forkTask2").list();
        assertEquals(3, forkTask2Executions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(6, tasks.size());
        Map<String, List<Task>> taskGroups = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(2, taskGroups.keySet().size());
        assertEquals(new HashSet<>(Arrays.asList("forkTask1", "forkTask2")), taskGroups.keySet());
        assertEquals(3, taskGroups.get("forkTask1").size());
        assertEquals(3, taskGroups.get("forkTask2").size());

        //Move the parallel gateway task forward
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(Arrays.asList("forkTask1", "forkTask2"), "parallelJoin")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> postForkTaskExecutions = runtimeService.createExecutionQuery().activityId("postForkTask").list();
        assertEquals(3, postForkTaskExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Complete one of the tasks
        taskService.complete(tasks.get(1).getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        postForkTaskExecutions = runtimeService.createExecutionQuery().activityId("postForkTask").list();
        assertEquals(2, postForkTaskExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Finish the rest since we cannot move out of a multiInstance subProcess
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelGatewayInsideMultiInstanceSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionsUsingParallelGatewayNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGatewayInsideMultiInstanceSubProcess");

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> preForkTaskExecutions = runtimeService.createExecutionQuery().activityId("preForkTask").list();
        assertEquals(3, preForkTaskExecutions.size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        assertEquals("preForkTask", tasks.get(0).getTaskDefinitionKey());

        //Move a task before the fork within the multiInstance subProcess
        preForkTaskExecutions.forEach(e -> runtimeService.createChangeActivityStateBuilder()
            .moveSingleExecutionToActivityIds(e.getId(), Arrays.asList("forkTask1", "forkTask2"))
            .changeState());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(10, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> forkTask1Executions = runtimeService.createExecutionQuery().activityId("forkTask1").list();
        assertEquals(3, forkTask1Executions.size());
        List<Execution> forkTask2Executions = runtimeService.createExecutionQuery().activityId("forkTask2").list();
        assertEquals(3, forkTask2Executions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(6, tasks.size());
        Map<String, List<Task>> taskGroups = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(2, taskGroups.keySet().size());
        assertEquals(new HashSet<>(Arrays.asList("forkTask1", "forkTask2")), taskGroups.keySet());
        assertEquals(3, taskGroups.get("forkTask1").size());
        assertEquals(3, taskGroups.get("forkTask2").size());

        //Move the parallel gateway task forward
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(
                Stream.concat(forkTask1Executions.stream().map(Execution::getId), forkTask2Executions.stream().map(Execution::getId)).collect(Collectors.toList()), "postForkTask")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> postForkTaskExecutions = runtimeService.createExecutionQuery().activityId("postForkTask").list();
        assertEquals(3, postForkTaskExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Complete one of the tasks
        taskService.complete(tasks.get(1).getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        postForkTaskExecutions = runtimeService.createExecutionQuery().activityId("postForkTask").list();
        assertEquals(2, postForkTaskExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Finish the rest since we cannot move out of a multiInstance subProcess
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayNestedInsideMultiInstanceSubProcess.bpmn20.xml" })
    public void testCompleteSetCurrentActivitiesUsingInclusiveGatewayNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideMultiInstanceSubProcess");

        //1x MI subProc root, 3x parallel MI subProc, 9x Task executions (3 tasks per Gw path)
        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(13, childExecutions.size());
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(3, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));

        //Move all activities
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(Arrays.asList("taskInclusive1", "taskInclusive2", "taskInclusive3"), "inclusiveJoin")
            .changeState();

        //Still 3 subProcesses running, all of them past the gateway fork/join
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(7, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(3, classifiedExecutions.get("postForkTask").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Finish the remaining subProcesses tasks
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Only one execution and task remaining
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, childExecutions.size());
        assertEquals("lastTask", childExecutions.get(0).getActivityId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        //Complete the process
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayNestedInsideMultiInstanceSubProcess.bpmn20.xml" })
    public void testCompleteSetCurrentExecutionsUsingInclusiveGatewayNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideMultiInstanceSubProcess");

        //1x MI subProc root, 3x parallel MI subProc, 9x Task executions (3 tasks per Gw path)
        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(13, childExecutions.size());
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(3, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));

        //Finish one activity in two MI subProcesses
        taskService.complete(classifiedTasks.get("taskInclusive1").get(1).getId());
        taskService.complete(classifiedTasks.get("taskInclusive2").get(2).getId());

        //1x MI subProc root, 3x parallel MI subProc, 7x Gw Task executions, 2x Gw join executions
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(13, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(2, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(2, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(2, classifiedExecutions.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(2, classifiedExecutions.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        //TEST 1 (move all)... change state of "all" executions in a gateway at once
        //Move the executions of the gateway that still contains 3 tasks in execution
        Stream<Execution> tempStream = Stream.concat(classifiedExecutions.get("taskInclusive1").stream(), classifiedExecutions.get("taskInclusive2").stream());
        Map<String, List<Execution>> taskExecutionsByParent = Stream.concat(tempStream, classifiedExecutions.get("taskInclusive3").stream())
            .collect(Collectors.groupingBy(Execution::getParentId));

        List<String> ids = taskExecutionsByParent.values().stream()
            .filter(l -> l.size() == 3)
            .findFirst().orElseGet(ArrayList::new)
            .stream().map(Execution::getId)
            .collect(Collectors.toList());

        //Move into the synchronizing gateway
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(ids, "inclusiveJoin")
            .changeState();

        //There'll be still 3 subProcesses running, 2 with "gateways" still in execution, one with task3 & task1 & join, one with task3, task2 and join
        // the 3rd subProcess should be past the gateway fork/join
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(11, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(1, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(1, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(2, classifiedExecutions.get("taskInclusive3").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(1, classifiedExecutions.get("postForkTask").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(4, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(1, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(1, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(2, classifiedTasks.get("taskInclusive3").size());
        assertTrue(classifiedTasks.containsKey("postForkTask"));
        assertEquals(1, classifiedTasks.get("postForkTask").size());

        //TEST 2 (complete last execution)... complete the last execution of a gateway were a task execution was already moved into the synchronizing join
        ids = classifiedExecutions.get("taskInclusive1").stream().map(Execution::getId).collect(Collectors.toList());
        //Move into the synchronizing gateway
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(ids, "inclusiveJoin")
            .changeState();

        //Complete remaining task3, the next inline test needs the task to be completed too
        for (Task t : tasks) {
            if (t.getTaskDefinitionKey().equals("taskInclusive3")) {
                taskService.complete(t.getId());
            }
        }

        //Still 3 subProcesses running, two of them past the gateway fork/join and the remaining one with a task2 pending
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(9, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(2, classifiedExecutions.get("postForkTask").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(1, classifiedExecutions.get("taskInclusive2").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(2, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("postForkTask"));
        assertEquals(2, classifiedTasks.get("postForkTask").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(1, classifiedTasks.get("taskInclusive2").size());

        //TEST 3 (move last execution)... move the remaining execution of a gateway with previously completed executions into the synchronizing join
        ids = classifiedExecutions.get("taskInclusive2").stream().map(Execution::getId).collect(Collectors.toList());
        //Move into the synchronizing gateway
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(ids, "inclusiveJoin")
            .changeState();

        //Still 3 subProcesses running, all of them past the gateway fork/join
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(7, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(3, classifiedExecutions.get("postForkTask").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Finish the remaining subProcesses tasks
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Only one execution and task remaining
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, childExecutions.size());
        assertEquals("lastTask", childExecutions.get(0).getActivityId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        //Complete the process
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml" })
    public void testSetCurrentActivityToIntermediateSignalCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertNotNull(classifiedExecutions.get("beforeCatchEvent"));
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertTrue(classifiedEventSubscriptions.isEmpty());

        //Move to catchEvent
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("beforeCatchEvent", "intermediateCatchEvent")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        assertEquals("signal", classifiedEventSubscriptions.get("intermediateCatchEvent").get(0).getEventType());

        //Trigger the event
        runtimeService.signalEventReceived("someSignal");

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the process
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml" })
    public void testSetCurrentExecutionToIntermediateSignalCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertNotNull(classifiedExecutions.get("beforeCatchEvent"));
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertTrue(classifiedEventSubscriptions.isEmpty());

        //Move to catchEvent
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(classifiedExecutions.get("beforeCatchEvent").get(0).getId(), "intermediateCatchEvent")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        assertEquals("signal", classifiedEventSubscriptions.get("intermediateCatchEvent").get(0).getEventType());

        //Trigger the event
        runtimeService.signalEventReceived("someSignal");

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the process
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml" })
    public void testSetCurrentActivityFromIntermediateSignalCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        assertEquals("beforeCatchEvent", task.getTaskDefinitionKey());

        //Complete initial task
        taskService.complete(task.getId());

        //Process is waiting for event invocation
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());

        //Move back to the initial task
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("intermediateCatchEvent", "beforeCatchEvent")
            .changeState();

        //Process is in the initial state, no subscriptions exists
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("beforeCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.size());
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the task once more
        taskService.complete(tasks.get(0).getId());

        //Process is waiting for signal again
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        assertEquals("signal", classifiedEventSubscriptions.get("intermediateCatchEvent").get(0).getEventType());

        //Move forward from the event catch
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("intermediateCatchEvent", "afterCatchEvent")
            .changeState();

        //Process is on the last task
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the process
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml" })
    public void testSetCurrentExecutionFromIntermediateSignalCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        assertEquals("beforeCatchEvent", task.getTaskDefinitionKey());

        //Complete initial task
        taskService.complete(task.getId());

        //Process is waiting for event invocation
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());

        //Move back to the initial task
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(classifiedExecutions.get("intermediateCatchEvent").get(0).getId(), "beforeCatchEvent")
            .changeState();

        //Process is in the initial state, no subscriptions exists
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("beforeCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.size());
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the task once more
        taskService.complete(tasks.get(0).getId());

        //Process is waiting for signal again
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        assertEquals("signal", classifiedEventSubscriptions.get("intermediateCatchEvent").get(0).getEventType());

        //Move forward from the event catch
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(classifiedExecutions.get("intermediateCatchEvent").get(0).getId(), "afterCatchEvent")
            .changeState();

        //Process is on the last task
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the process
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml" })
    public void testSetCurrentActivityToIntermediateMessageCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertNotNull(classifiedExecutions.get("beforeCatchEvent"));
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertTrue(classifiedEventSubscriptions.isEmpty());

        //Move to catchEvent
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("beforeCatchEvent", "intermediateCatchEvent")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        assertEquals("message", classifiedEventSubscriptions.get("intermediateCatchEvent").get(0).getEventType());

        //Trigger the event
        String messageCatchingExecutionId = classifiedExecutions.get("intermediateCatchEvent").get(0).getId();
        runtimeService.messageEventReceived("someMessage", messageCatchingExecutionId);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the process
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml" })
    public void testSetCurrentExecutionToIntermediateMessageCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertNotNull(classifiedExecutions.get("beforeCatchEvent"));
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertTrue(classifiedEventSubscriptions.isEmpty());

        //Move to catchEvent
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(classifiedExecutions.get("beforeCatchEvent").get(0).getId(), "intermediateCatchEvent")
            .changeState();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        assertEquals("message", classifiedEventSubscriptions.get("intermediateCatchEvent").get(0).getEventType());

        //Trigger the event
        String messageCatchingExecutionId = classifiedExecutions.get("intermediateCatchEvent").get(0).getId();
        runtimeService.messageEventReceived("someMessage", messageCatchingExecutionId);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the process
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml" })
    public void testSetCurrentActivityFromIntermediateMessageCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        assertEquals("beforeCatchEvent", task.getTaskDefinitionKey());

        //Complete initial task
        taskService.complete(task.getId());

        //Process is waiting for event invocation
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());

        //Move back to the initial task
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("intermediateCatchEvent", "beforeCatchEvent")
            .changeState();

        //Process is in the initial state, no subscriptions exists
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("beforeCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.size());
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the task once more
        taskService.complete(tasks.get(0).getId());

        //Process is waiting for signal again
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        assertEquals("message", classifiedEventSubscriptions.get("intermediateCatchEvent").get(0).getEventType());

        //Move forward from the event catch
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("intermediateCatchEvent", "afterCatchEvent")
            .changeState();

        //Process is on the last task
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the process
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml" })
    public void testSetCurrentExecutionFromIntermediateMessageCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        assertEquals("beforeCatchEvent", task.getTaskDefinitionKey());

        //Complete initial task
        taskService.complete(task.getId());

        //Process is waiting for event invocation
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());

        //Move back to the initial task
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(classifiedExecutions.get("intermediateCatchEvent").get(0).getId(), "beforeCatchEvent")
            .changeState();

        //Process is in the initial state, no subscriptions exists
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("beforeCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.size());
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the task once more
        taskService.complete(tasks.get(0).getId());

        //Process is waiting for signal again
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        assertEquals("message", classifiedEventSubscriptions.get("intermediateCatchEvent").get(0).getEventType());

        //Move forward from the event catch
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(classifiedExecutions.get("intermediateCatchEvent").get(0).getId(), "afterCatchEvent")
            .changeState();

        //Process is on the last task
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        //Complete the process
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    protected void checkInitialStateForMultipleProcessesWithSimpleEventCatch(Map<String, List<Execution>> executionsByProcessInstance) {
        executionsByProcessInstance.forEach((processId, executions) -> {
            Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
            assertEquals(1, classifiedExecutions.size());
            assertNotNull(classifiedExecutions.get("beforeCatchEvent"));
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(processId).list();
            Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
            assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
            List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
            Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
            assertTrue(classifiedEventSubscriptions.isEmpty());
        });
    }

    protected void checkWaitStateForMultipleProcessesWithSimpleEventCatch(Map<String, List<Execution>> executionsByProcessInstance) {
        executionsByProcessInstance.forEach((processId, executions) -> {
            Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
            assertEquals(1, classifiedExecutions.size());
            assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(processId).list();
            assertTrue(tasks.isEmpty());
            List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
            Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
            assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());
        });
    }

    protected void checkFinalStateForMultipleProcessesWithSimpleEventCatch(Map<String, List<Execution>> executionsByProcessInstance) {
        executionsByProcessInstance.forEach((processId, executions) -> {
            Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
            assertEquals(1, classifiedExecutions.size());
            assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(processId).list();
            Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
            assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
            List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
            assertTrue(eventSubscriptions.isEmpty());
        });
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml" })
    public void testSetCurrentActivityToIntermediateCatchEventForMultipleProcessesTriggerSimultaneously() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        List<Execution> allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        Map<String, List<Execution>> executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkInitialStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Move both processes to the eventCatch
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance1.getId())
            .moveActivityIdTo("beforeCatchEvent", "intermediateCatchEvent")
            .changeState();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance2.getId())
            .moveActivityIdTo("beforeCatchEvent", "intermediateCatchEvent")
            .changeState();

        allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkWaitStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Trigger signal
        runtimeService.signalEventReceived("someSignal");

        //Both processes should be on the final task execution
        allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkFinalStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Complete the remaining tasks for both processes
        taskService.createTaskQuery().list().forEach(this::completeTask);
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml" })
    public void testSetCurrentExecutionToIntermediateCatchEventForMultipleProcessesTriggerSimultaneously() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        List<Execution> allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        Map<String, List<Execution>> executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkInitialStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Move both processes to the eventCatch
        ChangeActivityStateBuilder changeActivityStateBuilder = runtimeService.createChangeActivityStateBuilder();
        allExecutions.stream()
            .filter(e -> e.getActivityId().equals("beforeCatchEvent"))
            .map(Execution::getId)
            .forEach(id -> changeActivityStateBuilder.moveExecutionToActivityId(id, "intermediateCatchEvent"));
        changeActivityStateBuilder.changeState();

        allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkWaitStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Trigger signal
        runtimeService.signalEventReceived("someSignal");

        //Both processes should be on the final task execution
        allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkFinalStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Complete the remaining tasks for both processes
        taskService.createTaskQuery().list().forEach(this::completeTask);
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml" })
    public void testSetCurrentActivityToIntermediateCatchEventForMultipleProcessesTriggerDiffered() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        List<Execution> allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        Map<String, List<Execution>> executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkInitialStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Move one process to the eventCatch
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance1.getId())
            .moveActivityIdTo("beforeCatchEvent", "intermediateCatchEvent")
            .changeState();

        allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        //ProcessInstance1 waiting for event
        String processId = processInstance1.getId();
        List<Execution> executions = executionsByProcessInstance.get(processId);
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processId).list();
        assertTrue(tasks.isEmpty());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());

        //processInstance2 Execution still on initial state
        processId = processInstance2.getId();
        executions = executionsByProcessInstance.get(processId);
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertNotNull(classifiedExecutions.get("beforeCatchEvent"));
        tasks = taskService.createTaskQuery().processInstanceId(processId).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertTrue(classifiedEventSubscriptions.isEmpty());

        //Trigger signal
        runtimeService.signalEventReceived("someSignal");

        //Move the second process to the eventCatch
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance2.getId())
            .moveActivityIdTo("beforeCatchEvent", "intermediateCatchEvent")
            .changeState();

        //ProcessInstance1 is on the postEvent task
        processId = processInstance1.getId();
        executions = runtimeService.createExecutionQuery().processInstanceId(processId).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processId).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
        assertTrue(eventSubscriptions.isEmpty());

        //ProcessInstance2 is waiting for the event
        processId = processInstance2.getId();
        executions = runtimeService.createExecutionQuery().processInstanceId(processId).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processId).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());

        //Fire the event once more
        runtimeService.signalEventReceived("someSignal");

        //Both process should be on the postEvent task execution
        allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkFinalStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Complete the remaining tasks for both processes
        taskService.createTaskQuery().list().forEach(this::completeTask);
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml" })
    public void testSetCurrentExecutionToIntermediateCatchEventForMultipleProcessesTriggerDiffered() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("changeStateForSimpleIntermediateEvent");

        List<Execution> allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        Map<String, List<Execution>> executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkInitialStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Move one execution to the event catch
        String executionId = executionsByProcessInstance.get(processInstance1.getId()).stream()
            .filter(e -> e.getActivityId().equals("beforeCatchEvent"))
            .findFirst()
            .map(Execution::getId)
            .get();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(executionId, "intermediateCatchEvent")
            .changeState();

        allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        //ProcessInstance1 waiting for event
        String processId = processInstance1.getId();
        List<Execution> executions = executionsByProcessInstance.get(processId);
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processId).list();
        assertTrue(tasks.isEmpty());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
        Map<String, List<EventSubscription>> classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());

        //processInstance2 Execution still on initial state
        processId = processInstance2.getId();
        executions = executionsByProcessInstance.get(processId);
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertNotNull(classifiedExecutions.get("beforeCatchEvent"));
        tasks = taskService.createTaskQuery().processInstanceId(processId).list();
        Map<String, List<Task>> classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("beforeCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertTrue(classifiedEventSubscriptions.isEmpty());

        //Trigger signal
        runtimeService.signalEventReceived("someSignal");

        //Move the second process to the eventCatch
        executionId = executionsByProcessInstance.get(processInstance2.getId()).stream()
            .filter(e -> e.getActivityId().equals("beforeCatchEvent"))
            .findFirst()
            .map(Execution::getId)
            .get();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(executionId, "intermediateCatchEvent")
            .changeState();

        //ProcessInstance1 is on the postEvent task
        processId = processInstance1.getId();
        executions = runtimeService.createExecutionQuery().processInstanceId(processId).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("afterCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processId).list();
        classifiedTasks = groupListContentBy(tasks, Task::getTaskDefinitionKey);
        assertEquals(1, classifiedTasks.get("afterCatchEvent").size());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
        assertTrue(eventSubscriptions.isEmpty());

        //ProcessInstance2 is waiting for the event
        processId = processInstance2.getId();
        executions = runtimeService.createExecutionQuery().processInstanceId(processId).onlyChildExecutions().list();
        classifiedExecutions = groupListContentBy(executions, Execution::getActivityId);
        assertEquals(1, classifiedExecutions.size());
        assertEquals(1, classifiedExecutions.get("intermediateCatchEvent").size());
        tasks = taskService.createTaskQuery().processInstanceId(processId).list();
        assertTrue(tasks.isEmpty());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processId).list();
        classifiedEventSubscriptions = groupListContentBy(eventSubscriptions, EventSubscription::getActivityId);
        assertEquals(1, classifiedEventSubscriptions.get("intermediateCatchEvent").size());

        //Fire the event once more
        runtimeService.signalEventReceived("someSignal");

        //Both process should be on the postEvent task execution
        allExecutions = runtimeService.createExecutionQuery().onlyChildExecutions().list();
        executionsByProcessInstance = groupListContentBy(allExecutions, Execution::getProcessInstanceId);
        assertEquals(2, executionsByProcessInstance.size());

        checkFinalStateForMultipleProcessesWithSimpleEventCatch(executionsByProcessInstance);

        //Complete the remaining tasks for both processes
        taskService.createTaskQuery().list().forEach(this::completeTask);
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());
    }
}
