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

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
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
public class ChangeStateForCallActivityTest extends PluggableFlowableTestCase {

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
    @Deployment(resources = { "org/flowable/engine/test/api/variables/callActivityWithCalledElementExpression.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstanceWithCalledElementExpression() {

        //Deploy second version of the process definition
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("calledElementExpression");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        //First change state attempt fails as the calledElement expression cannot be evaluated
        try {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity")
                .changeState();
            fail("Change state should not be possible calledElement expression could not be evaluated");
        } catch (FlowableException e) {
            assertTextPresent("Cannot resolve calledElement expression '${subProcessDefId}' of callActivity 'callActivity'", e.getMessage());
        }

        //Change state specifying the variable with the value
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity", 1)
            .processVariable("subProcessDefId", "oneTaskProcess")
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
        assertEquals("lastTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        deleteDeployments();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstanceSpecificVersion() {

        //Deploy second version of the process definition
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcessV2.bpmn20.xml");

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
            assertTextPresent("Cannot find activity 'theTask' in process definition with id 'oneTaskProcess'", e.getMessage());
        }

        //Invalid "unExistent" process definition version
        try {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity", 5)
                .changeState();
            fail("Change state should not be possible as it referring to an activity of a previous version");
        } catch (FlowableException e) {
            assertTextPresent("Cannot find activity 'theTask' in process definition with id 'oneTaskProcess'", e.getMessage());
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

}
