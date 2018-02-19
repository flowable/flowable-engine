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

package org.flowable.engine.test.api.runtime;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.common.impl.util.CollectionUtil;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class RuntimeServiceTest extends PluggableFlowableTestCase {

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceWithVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("basicType", new DummySerializable());
        runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        org.flowable.task.api.Task task = taskService.createTaskQuery().includeProcessVariables().singleResult();
        assertNotNull(task.getProcessVariables());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceWithLongStringVariable() {
        Map<String, Object> vars = new HashMap<>();
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 4001; i++) {
            longString.append("c");
        }
        vars.put("longString", longString.toString());
        runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        org.flowable.task.api.Task task = taskService.createTaskQuery().includeProcessVariables().singleResult();
        assertNotNull(task.getProcessVariables());
        assertEquals(longString.toString(), task.getProcessVariables().get("longString"));
    }

    public void testStartProcessInstanceByKeyNullKey() {
        try {
            runtimeService.startProcessInstanceByKey(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException e) {
            // Expected exception
        }
    }

    public void testStartProcessInstanceByKeyUnexistingKey() {
        try {
            runtimeService.startProcessInstanceByKey("unexistingkey");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("no processes deployed with key", ae.getMessage());
            assertEquals(ProcessDefinition.class, ae.getObjectClass());
        }
    }

    public void testStartProcessInstanceByIdNullId() {
        try {
            runtimeService.startProcessInstanceById(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException e) {
            // Expected exception
        }
    }

    public void testStartProcessInstanceByIdUnexistingId() {
        try {
            runtimeService.startProcessInstanceById("unexistingId");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("no deployed process definition found with id", ae.getMessage());
            assertEquals(ProcessDefinition.class, ae.getObjectClass());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceByIdNullVariables() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", (Map<String, Object>) null);
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceWithBusinessKey() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        // by key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "123");
        assertNotNull(processInstance);
        assertEquals("123", processInstance.getBusinessKey());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());

        // by key with variables
        processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "456", CollectionUtil.singletonMap("var", "value"));
        assertNotNull(processInstance);
        assertEquals(2, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals("value", runtimeService.getVariable(processInstance.getId(), "var"));

        // by id
        processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), "789");
        assertNotNull(processInstance);
        assertEquals(3, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());

        // by id with variables
        processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), "101123", CollectionUtil.singletonMap("var", "value2"));
        assertNotNull(processInstance);
        assertEquals(4, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals("value2", runtimeService.getVariable(processInstance.getId(), "var"));
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceByProcessInstanceBuilder() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        // by key
        ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess").businessKey("123").start();
        assertNotNull(processInstance);
        assertEquals("123", processInstance.getBusinessKey());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());

        processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        // by key, with processInstance name with variables
        processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess").businessKey("456").variable("var", "value").name("processName1").start();
        assertNotNull(processInstance);
        assertEquals(2, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals("processName1", processInstance.getName());
        assertEquals("456", processInstance.getBusinessKey());
        assertEquals("value", runtimeService.getVariable(processInstance.getId(), "var"));

        processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

        // by id
        processInstance = processInstanceBuilder.processDefinitionId(processDefinition.getId()).businessKey("789").start();
        assertNotNull(processInstance);
        assertEquals(3, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals("789", processInstance.getBusinessKey());

        processInstanceBuilder = runtimeService.createProcessInstanceBuilder();
        // by id with variables
        processInstance = processInstanceBuilder.processDefinitionId(processDefinition.getId()).businessKey("101123").variable("var", "value2").start();
        assertNotNull(processInstance);
        assertEquals(4, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals("value2", runtimeService.getVariable(processInstance.getId(), "var"));
        assertEquals("101123", processInstance.getBusinessKey());

        processInstanceBuilder = runtimeService.createProcessInstanceBuilder();
        // by id and processInstance name
        processInstance = processInstanceBuilder.processDefinitionId(processDefinition.getId()).businessKey("101124").name("processName2").start();
        assertNotNull(processInstance);
        assertEquals(5, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals("processName2", processInstance.getName());
        assertEquals("101124", processInstance.getBusinessKey());
    }

    public void testStartProcessInstanceByProcessInstanceBuilderWithTenantId() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml").
                tenantId("flowable").
                deploy();
        try {
            ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();

            ProcessInstance processInstance = processInstanceBuilder.processDefinitionKey("oneTaskProcess").businessKey("123").
                    tenantId("flowable").start();
            assertNotNull(processInstance);
            assertEquals("123", processInstance.getBusinessKey());
            assertEquals("flowable", processInstance.getTenantId());

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTenantId(), is("flowable"));
        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNonUniqueBusinessKey() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "123");

        // Behaviour changed: https://activiti.atlassian.net/browse/ACT-1860
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "123");
        assertEquals(2, runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("123").count());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceFormWithoutFormKey() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("basicType", new DummySerializable());

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        runtimeService.startProcessInstanceWithForm(processDefinition.getId(), null, vars, null);
        org.flowable.task.api.Task task = taskService.createTaskQuery().includeProcessVariables().singleResult();
        assertNotNull(task.getProcessVariables());
    }

    // some databases might react strange on having multiple times null for the
    // business key
    // when the unique constraint is {processDefinitionId, businessKey}
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testMultipleNullBusinessKeys() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNull(processInstance.getBusinessKey());

        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertEquals(3, runtimeService.createProcessInstanceQuery().count());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testDeleteProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());

        String deleteReason = "testing instance deletion";
        runtimeService.deleteProcessInstance(processInstance.getId(), deleteReason);
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());

        // test that the delete reason of the process instance shows up as
        // delete reason of the task in history ACT-848
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

            assertEquals(deleteReason, historicTaskInstance.getDeleteReason());

            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

            assertNotNull(historicInstance);
            assertEquals(deleteReason, historicInstance.getDeleteReason());
            assertNotNull(historicInstance.getEndTime());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testDeleteProcessInstanceNullReason() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());

        // Deleting without a reason should be possible
        runtimeService.deleteProcessInstance(processInstance.getId(), null);
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();

            assertNotNull(historicInstance);
            assertEquals(DeleteReason.PROCESS_INSTANCE_DELETED, historicInstance.getDeleteReason());
        }
    }

    public void testDeleteProcessInstanceUnexistingId() {
        try {
            runtimeService.deleteProcessInstance("enexistingInstanceId", null);
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("No process instance found for id", ae.getMessage());
            assertEquals(ProcessInstance.class, ae.getObjectClass());
        }
    }

    public void testDeleteProcessInstanceNullId() {
        try {
            runtimeService.deleteProcessInstance(null, "test null id delete");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("processInstanceId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testFindActiveActivityIds() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<String> activities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertNotNull(activities);
        assertEquals(1, activities.size());
    }

    public void testFindActiveActivityIdsUnexistingExecututionId() {
        try {
            runtimeService.getActiveActivityIds("unexistingExecutionId");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
            assertEquals(Execution.class, ae.getObjectClass());
        }
    }

    public void testFindActiveActivityIdsNullExecututionId() {
        try {
            runtimeService.getActiveActivityIds(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    /**
     * Testcase to reproduce ACT-950 (https://jira.codehaus.org/browse/ACT-950)
     */
    @Deployment
    public void testFindActiveActivityIdProcessWithErrorEventAndSubProcess() {
        ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("errorEventSubprocess");

        List<String> activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertEquals(5, activeActivities.size());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());

        org.flowable.task.api.Task parallelUserTask = null;
        for (org.flowable.task.api.Task task : tasks) {
            if (!task.getName().equals("ParallelUserTask") && !task.getName().equals("MainUserTask")) {
                fail("Expected: <ParallelUserTask> or <MainUserTask> but was <" + task.getName() + ">.");
            }
            if (task.getName().equals("ParallelUserTask")) {
                parallelUserTask = task;
            }
        }
        assertNotNull(parallelUserTask);

        taskService.complete(parallelUserTask.getId());

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subprocess1WaitBeforeError").singleResult();
        runtimeService.trigger(execution.getId());

        activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertEquals(4, activeActivities.size());

        tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());

        org.flowable.task.api.Task beforeErrorUserTask = null;
        for (org.flowable.task.api.Task task : tasks) {
            if (!task.getName().equals("BeforeError") && !task.getName().equals("MainUserTask")) {
                fail("Expected: <BeforeError> or <MainUserTask> but was <" + task.getName() + ">.");
            }
            if (task.getName().equals("BeforeError")) {
                beforeErrorUserTask = task;
            }
        }
        assertNotNull(beforeErrorUserTask);

        taskService.complete(beforeErrorUserTask.getId());

        activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertEquals(2, activeActivities.size());

        tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());

        org.flowable.task.api.Task afterErrorUserTask = null;
        for (org.flowable.task.api.Task task : tasks) {
            if (!task.getName().equals("AfterError") && !task.getName().equals("MainUserTask")) {
                fail("Expected: <AfterError> or <MainUserTask> but was <" + task.getName() + ">.");
            }
            if (task.getName().equals("AfterError")) {
                afterErrorUserTask = task;
            }
        }
        assertNotNull(afterErrorUserTask);

        taskService.complete(afterErrorUserTask.getId());

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals("MainUserTask", tasks.get(0).getName());

        activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
        assertEquals(1, activeActivities.size());
        assertEquals("MainUserTask", activeActivities.get(0));

        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    public void testSignalUnexistingExecututionId() {
        try {
            runtimeService.trigger("unexistingExecutionId");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
            assertEquals(Execution.class, ae.getObjectClass());
        }
    }

    public void testSignalNullExecutionId() {
        try {
            runtimeService.trigger(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    @Deployment
    public void testSignalWithProcessVariables() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSignalWithProcessVariables");
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("variable", "value");

        // signal the execution while passing in the variables
        Execution execution = runtimeService.createExecutionQuery().activityId("receiveMessage").singleResult();
        runtimeService.trigger(execution.getId(), processVariables);

        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertEquals(processVariables, variables);

    }

    public void testGetVariablesUnexistingExecutionId() {
        try {
            runtimeService.getVariables("unexistingExecutionId");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
            assertEquals(Execution.class, ae.getObjectClass());
        }
    }

    public void testGetVariablesNullExecutionId() {
        try {
            runtimeService.getVariables(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    public void testGetVariableUnexistingExecutionId() {
        try {
            runtimeService.getVariables("unexistingExecutionId");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
            assertEquals(Execution.class, ae.getObjectClass());
        }
    }

    public void testGetVariableNullExecutionId() {
        try {
            runtimeService.getVariables(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableUnexistingVariableName() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Object variableValue = runtimeService.getVariable(processInstance.getId(), "unexistingVariable");
        assertNull(variableValue);
    }

    public void testSetVariableUnexistingExecutionId() {
        try {
            runtimeService.setVariable("unexistingExecutionId", "variableName", "value");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
            assertEquals(Execution.class, ae.getObjectClass());
        }
    }

    public void testSetVariableNullExecutionId() {
        try {
            runtimeService.setVariable(null, "variableName", "variableValue");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetVariableNullVariableName() {
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            runtimeService.setVariable(processInstance.getId(), null, "variableValue");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("variableName is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariables(processInstance.getId(), vars);

        assertEquals("value1", runtimeService.getVariable(processInstance.getId(), "variable1"));
        assertEquals("value2", runtimeService.getVariable(processInstance.getId(), "variable2"));
    }

    @SuppressWarnings("unchecked")
    public void testSetVariablesUnexistingExecutionId() {
        try {
            runtimeService.setVariables("unexistingexecution", Collections.EMPTY_MAP);
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("execution unexistingexecution doesn't exist", ae.getMessage());
            assertEquals(Execution.class, ae.getObjectClass());
        }
    }

    @SuppressWarnings("unchecked")
    public void testSetVariablesNullExecutionId() {
        try {
            runtimeService.setVariables(null, Collections.EMPTY_MAP);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    private void checkHistoricVariableUpdateEntity(String variableName, String processInstanceId) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            boolean deletedVariableUpdateFound = false;

            List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().processInstanceId(processInstanceId).list();
            for (HistoricDetail currentHistoricDetail : resultSet) {
                assertTrue(currentHistoricDetail instanceof HistoricDetailVariableInstanceUpdateEntity);
                HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = (HistoricDetailVariableInstanceUpdateEntity) currentHistoricDetail;

                if (historicVariableUpdate.getName().equals(variableName)) {
                    if (historicVariableUpdate.getValue() == null) {
                        if (deletedVariableUpdateFound) {
                            fail("Mismatch: A HistoricVariableUpdateEntity with a null value already found");
                        } else {
                            deletedVariableUpdateFound = true;
                        }
                    }
                }
            }

            assertTrue(deletedVariableUpdateFound);
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariables(processInstance.getId(), vars);

        runtimeService.removeVariable(processInstance.getId(), "variable1");

        assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
        assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable1"));
        assertEquals("value2", runtimeService.getVariable(processInstance.getId(), "variable2"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneSubProcess.bpmn20.xml" })
    public void testRemoveVariableInParentScope() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        runtimeService.removeVariable(currentTask.getExecutionId(), "variable1");

        assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
        assertEquals("value2", runtimeService.getVariable(processInstance.getId(), "variable2"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    public void testRemoveVariableNullExecutionId() {
        try {
            runtimeService.removeVariable(null, "variable");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariableLocal() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        runtimeService.removeVariableLocal(processInstance.getId(), "variable1");

        assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
        assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable1"));
        assertEquals("value2", runtimeService.getVariable(processInstance.getId(), "variable2"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneSubProcess.bpmn20.xml" })
    public void testRemoveVariableLocalWithParentScope() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();
        runtimeService.setVariableLocal(currentTask.getExecutionId(), "localVariable", "local value");

        assertEquals("local value", runtimeService.getVariableLocal(currentTask.getExecutionId(), "localVariable"));

        runtimeService.removeVariableLocal(currentTask.getExecutionId(), "localVariable");

        assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "localVariable"));
        assertNull(runtimeService.getVariableLocal(currentTask.getExecutionId(), "localVariable"));

        assertEquals("value1", runtimeService.getVariable(processInstance.getId(), "variable1"));
        assertEquals("value2", runtimeService.getVariable(processInstance.getId(), "variable2"));

        assertEquals("value1", runtimeService.getVariable(currentTask.getExecutionId(), "variable1"));
        assertEquals("value2", runtimeService.getVariable(currentTask.getExecutionId(), "variable2"));

        checkHistoricVariableUpdateEntity("localVariable", processInstance.getId());
    }

    public void testRemoveLocalVariableNullExecutionId() {
        try {
            runtimeService.removeVariableLocal(null, "variable");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        runtimeService.setVariable(processInstance.getId(), "variable3", "value3");

        runtimeService.removeVariables(processInstance.getId(), vars.keySet());

        assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
        assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable1"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "variable2"));
        assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable2"));

        assertEquals("value3", runtimeService.getVariable(processInstance.getId(), "variable3"));
        assertEquals("value3", runtimeService.getVariableLocal(processInstance.getId(), "variable3"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneSubProcess.bpmn20.xml" })
    public void testRemoveVariablesWithParentScope() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
        runtimeService.setVariable(processInstance.getId(), "variable3", "value3");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        runtimeService.removeVariables(currentTask.getExecutionId(), vars.keySet());

        assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
        assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable1"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "variable2"));
        assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable2"));

        assertEquals("value3", runtimeService.getVariable(processInstance.getId(), "variable3"));
        assertEquals("value3", runtimeService.getVariableLocal(processInstance.getId(), "variable3"));

        assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable1"));
        assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable2"));

        assertEquals("value3", runtimeService.getVariable(currentTask.getExecutionId(), "variable3"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
    }

    @SuppressWarnings("unchecked")
    public void testRemoveVariablesNullExecutionId() {
        try {
            runtimeService.removeVariables(null, Collections.EMPTY_LIST);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneSubProcess.bpmn20.xml" })
    public void testRemoveVariablesLocalWithParentScope() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("variable1", "value1");
        vars.put("variable2", "value2");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();
        Map<String, Object> varsToDelete = new HashMap<>();
        varsToDelete.put("variable3", "value3");
        varsToDelete.put("variable4", "value4");
        varsToDelete.put("variable5", "value5");
        runtimeService.setVariablesLocal(currentTask.getExecutionId(), varsToDelete);
        runtimeService.setVariableLocal(currentTask.getExecutionId(), "variable6", "value6");

        assertEquals("value3", runtimeService.getVariable(currentTask.getExecutionId(), "variable3"));
        assertEquals("value3", runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable3"));
        assertEquals("value4", runtimeService.getVariable(currentTask.getExecutionId(), "variable4"));
        assertEquals("value4", runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable4"));
        assertEquals("value5", runtimeService.getVariable(currentTask.getExecutionId(), "variable5"));
        assertEquals("value5", runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable5"));
        assertEquals("value6", runtimeService.getVariable(currentTask.getExecutionId(), "variable6"));
        assertEquals("value6", runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable6"));

        runtimeService.removeVariablesLocal(currentTask.getExecutionId(), varsToDelete.keySet());

        assertEquals("value1", runtimeService.getVariable(currentTask.getExecutionId(), "variable1"));
        assertEquals("value2", runtimeService.getVariable(currentTask.getExecutionId(), "variable2"));

        assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable3"));
        assertNull(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable3"));
        assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable4"));
        assertNull(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable4"));
        assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable5"));
        assertNull(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable5"));

        assertEquals("value6", runtimeService.getVariable(currentTask.getExecutionId(), "variable6"));
        assertEquals("value6", runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable6"));

        checkHistoricVariableUpdateEntity("variable3", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable4", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable5", processInstance.getId());
    }

    @SuppressWarnings("unchecked")
    public void testRemoveVariablesLocalNullExecutionId() {
        try {
            runtimeService.removeVariablesLocal(null, Collections.EMPTY_LIST);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("executionId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchAlertSignal.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchPanicSignal.bpmn20.xml" })
    public void testSignalEventReceived() {

        startSignalCatchProcesses();
        // 15, because the signal catch is a scope
        assertEquals(15, runtimeService.createExecutionQuery().count());
        runtimeService.signalEventReceived("alert");
        assertEquals(9, runtimeService.createExecutionQuery().count());
        runtimeService.signalEventReceived("panic");
        assertEquals(0, runtimeService.createExecutionQuery().count());

        // //// test signalEventReceived(String, String)
        startSignalCatchProcesses();

        // signal the executions one at a time:
        for (int executions = 3; executions > 0; executions--) {
            List<Execution> page = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").listPage(0, 1);
            runtimeService.signalEventReceived("alert", page.get(0).getId());

            assertEquals(executions - 1, runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").count());
        }

        for (int executions = 3; executions > 0; executions--) {
            List<Execution> page = runtimeService.createExecutionQuery().signalEventSubscriptionName("panic").listPage(0, 1);
            runtimeService.signalEventReceived("panic", page.get(0).getId());

            assertEquals(executions - 1, runtimeService.createExecutionQuery().signalEventSubscriptionName("panic").count());
        }

    }

    @Deployment(resources = { "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchAlertMessage.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchPanicMessage.bpmn20.xml" })
    public void testMessageEventReceived() {

        startMessageCatchProcesses();
        // 12, because the signal catch is a scope
        assertEquals(12, runtimeService.createExecutionQuery().count());

        // signal the executions one at a time:
        for (int executions = 3; executions > 0; executions--) {
            List<Execution> page = runtimeService.createExecutionQuery().messageEventSubscriptionName("alert").listPage(0, 1);
            runtimeService.messageEventReceived("alert", page.get(0).getId());

            assertEquals(executions - 1, runtimeService.createExecutionQuery().messageEventSubscriptionName("alert").count());
        }

        for (int executions = 3; executions > 0; executions--) {
            List<Execution> page = runtimeService.createExecutionQuery().messageEventSubscriptionName("panic").listPage(0, 1);
            runtimeService.messageEventReceived("panic", page.get(0).getId());

            assertEquals(executions - 1, runtimeService.createExecutionQuery().messageEventSubscriptionName("panic").count());
        }

    }

    public void testSignalEventReceivedNonExistingExecution() {
        try {
            runtimeService.signalEventReceived("alert", "nonexistingExecution");
            fail("exception expected");
        } catch (FlowableObjectNotFoundException ae) {
            // this is good
            assertEquals(Execution.class, ae.getObjectClass());
        }
    }

    public void testMessageEventReceivedNonExistingExecution() {
        try {
            runtimeService.messageEventReceived("alert", "nonexistingExecution");
            fail("exception expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertEquals(Execution.class, ae.getObjectClass());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/runtime/RuntimeServiceTest.catchAlertSignal.bpmn20.xml" })
    public void testExecutionWaitingForDifferentSignal() {
        runtimeService.startProcessInstanceByKey("catchAlertSignal");
        Execution execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        try {
            runtimeService.signalEventReceived("bogusSignal", execution.getId());
            fail("exception expected");
        } catch (FlowableException e) {
            // this is good
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetProcessInstanceName() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        assertNull(processInstance.getName());

        // Set the name
        runtimeService.setProcessInstanceName(processInstance.getId(), "New name");
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(processInstance);
        assertEquals("New name", processInstance.getName());

        // Set the name to null
        runtimeService.setProcessInstanceName(processInstance.getId(), null);
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(processInstance);
        assertNull(processInstance.getName());

        // Set name for unexisting process instance, should fail
        try {
            runtimeService.setProcessInstanceName("unexisting", null);
            fail("Exception expected");
        } catch (FlowableObjectNotFoundException aonfe) {
            assertEquals(ProcessInstance.class, aonfe.getObjectClass());
        }

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(processInstance);
        assertNull(processInstance.getName());

        // Set name for suspended process instance, should fail
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        try {
            runtimeService.setProcessInstanceName(processInstance.getId(), null);
            fail("Exception expected");
        } catch (FlowableException ae) {
            assertEquals("process instance " + processInstance.getId() + " is suspended, cannot set name", ae.getMessage());
        }

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(processInstance);
        assertNull(processInstance.getName());
    }

    private void startSignalCatchProcesses() {
        for (int i = 0; i < 3; i++) {
            runtimeService.startProcessInstanceByKey("catchAlertSignal");
            runtimeService.startProcessInstanceByKey("catchPanicSignal");
        }
    }

    private void startMessageCatchProcesses() {
        for (int i = 0; i < 3; i++) {
            runtimeService.startProcessInstanceByKey("catchAlertMessage");
            runtimeService.startProcessInstanceByKey("catchPanicMessage");
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableUnexistingVariableNameWithCast() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        String variableValue = runtimeService.getVariable(processInstance.getId(), "unexistingVariable", String.class);
        assertNull(variableValue);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableExistingVariableNameWithCast() {
        Map<String, Object> params = new HashMap<>();
        params.put("var1", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", params);
        Boolean variableValue = runtimeService.getVariable(processInstance.getId(), "var1", Boolean.class);
        assertTrue(variableValue);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableExistingVariableNameWithInvalidCast() {
        Map<String, Object> params = new HashMap<>();
        params.put("var1", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", params);
        catchException(runtimeService).getVariable(processInstance.getId(), "var1", String.class);
        Exception e = caughtException();
        assertNotNull(e);
        assertTrue(e instanceof ClassCastException);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalUnexistingVariableNameWithCast() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        String variableValue = runtimeService.getVariableLocal(processInstance.getId(), "var1", String.class);
        assertNull(variableValue);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalExistingVariableNameWithCast() {
        Map<String, Object> params = new HashMap<>();
        params.put("var1", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", params);
        Boolean variableValue = runtimeService.getVariableLocal(processInstance.getId(), "var1", Boolean.class);
        assertTrue(variableValue);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalExistingVariableNameWithInvalidCast() {
        Map<String, Object> params = new HashMap<>();
        params.put("var1", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", params);
        catchException(runtimeService).getVariableLocal(processInstance.getId(), "var1", String.class);
        Exception e = caughtException();
        assertNotNull(e);
        assertTrue(e instanceof ClassCastException);
    }

    // Test for https://activiti.atlassian.net/browse/ACT-2186
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricVariableRemovedWhenRuntimeVariableIsRemoved() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("var1", "Hello");
            vars.put("var2", "World");
            vars.put("var3", "!");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

            // Verify runtime
            assertEquals(3, runtimeService.getVariables(processInstance.getId()).size());
            assertEquals(3, runtimeService.getVariables(processInstance.getId(), Arrays.asList("var1", "var2", "var3")).size());
            assertNotNull(runtimeService.getVariable(processInstance.getId(), "var2"));

            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            // Verify history
            assertEquals(3, historyService.createHistoricVariableInstanceQuery().list().size());
            assertNotNull(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("var2").singleResult());

            // Remove one variable
            runtimeService.removeVariable(processInstance.getId(), "var2");

            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            // Verify runtime
            assertEquals(2, runtimeService.getVariables(processInstance.getId()).size());
            assertEquals(2, runtimeService.getVariables(processInstance.getId(), Arrays.asList("var1", "var2", "var3")).size());
            assertNull(runtimeService.getVariable(processInstance.getId(), "var2"));

            // Verify history
            assertEquals(2, historyService.createHistoricVariableInstanceQuery().list().size());
            assertNull(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("var2").singleResult());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartTimeProcessInstance() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date noon = calendar.getTime();

        processEngineConfiguration.getClock().setCurrentTime(noon);
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertEquals(noon, processInstance.getStartTime());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testAuthenticatedStartUserProcessInstance() {
        final String authenticatedUser = "user1";
        identityService.setAuthenticatedUserId(authenticatedUser);
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertEquals(authenticatedUser, processInstance.getStartUserId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNoAuthenticatedStartUserProcessInstance() {
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertNull(processInstance.getStartUserId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("secondTask", "firstTask")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("secondTask", "firstTask")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityForSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subtask", "taskBefore")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityForSubProcessExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(task.getExecutionId(), "taskBefore")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityForSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subtask", "taskBefore")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityToTaskInSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("taskBefore", "subtask")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityToTaskInSubProcessAndExecuteTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("taskBefore", "subtask")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityForSubProcessWithTaskTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subtask", "taskBefore")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToTaskInSubProcessWithTaskTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("taskBefore", "subtask")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToTaskInSubProcessAndExecuteTaskTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("taskBefore", "subtask")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask2", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityForNestedSubProcessExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubtask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(task.getExecutionId(), "subtaskAfter")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtaskAfter", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityForNestedSubProcessExecution2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubtask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(task.getExecutionId(), "taskAfter")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/taskTwoSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivityForTwoSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoSubProcesses");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(task.getExecutionId(), "subtask2")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityForSubProcessWithVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("subtask", "taskBefore")
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

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
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

        Execution parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNull(parallelJoinExecution);

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNotNull(parallelJoinExecution);
        assertTrue(!((ExecutionEntity) parallelJoinExecution).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

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

    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetCurrentExecutionToMultipleActivitiesForParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
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

        Execution parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNull(parallelJoinExecution);

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNotNull(parallelJoinExecution);
        assertTrue(!((ExecutionEntity) parallelJoinExecution).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

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
                .processInstanceId(processInstance.getId())
                .moveExecutionsToSingleActivityId(currentExecutionIds, "taskAfter")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

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

        Execution parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNull(parallelJoinExecution);

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executions.size());

        parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNotNull(parallelJoinExecution);
        assertTrue(!((ExecutionEntity) parallelJoinExecution).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

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

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess1").singleResult();
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

    @Deployment(resources = { "org/flowable/engine/test/api/multipleParallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForInclusiveAndParallelSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess", Collections.singletonMap("var1", (Object) "test2"));
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

        Execution parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNull(parallelJoinExecution);

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

        parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNotNull(parallelJoinExecution);
        assertTrue(!((ExecutionEntity) parallelJoinExecution).isActive());

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

        Execution inclusiveJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("inclusiveJoin")) {
                inclusiveJoinExecution = execution;
                break;
            }
        }

        assertNotNull(inclusiveJoinExecution);
        assertTrue(!((ExecutionEntity) inclusiveJoinExecution).isActive());

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
}
