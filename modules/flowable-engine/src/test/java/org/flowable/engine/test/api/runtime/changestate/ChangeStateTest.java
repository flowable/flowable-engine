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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
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
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

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
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent) event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent) event).getActivityId());

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
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcessV2.bpmn20.xml", "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityInParentProcessV2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(subProcessInstance.getId())
            .moveActivityIdToParentActivityId("secondTask", "secondTask")
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
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcessV2.bpmn20.xml", "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstanceV2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdToSubProcessInstanceActivityId("firstTask", "secondTask", "callActivity")
            .changeState();

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        assertEquals(0, taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).count());

        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count());
        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().count());

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstanceSpecificVersion() {

        //Deploy second version of the process definition
        ProcessDefinition procDefCallActivityV1 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcessV2.bpmn20.xml");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        try {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity")
                .changeState();
            fail("Change state should not be possible as it referring to an activity of a previous version");
        } catch (FlowableException e) {
            assertTextPresent("Cannot find activity 'theTask' in process definition for with id 'oneTaskProcess'", e.getMessage());
        }

        //Invalid "unExistent" process definition version
        try {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity", 5)
                .changeState();
            fail("Change state should not be possible as it referring to an activity of a previous version");
        } catch (FlowableException e) {
            assertTextPresent("Cannot find activity 'theTask' in process definition for with id 'oneTaskProcess'", e.getMessage());
        }

        //Change state specifying the first version
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity", 1)
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

        deleteDeployments();
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
