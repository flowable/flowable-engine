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

package org.flowable.engine.test.history;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.TaskQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class HistoricVariableInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/callactivity/orderProcess.bpmn20.xml", "org/flowable/examples/bpmn/callactivity/checkCreditProcess.bpmn20.xml" })
    public void testOrderProcessWithCallActivity() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // After the process has started, the 'verify credit history' task should be active
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("orderProcess");
            TaskQuery taskQuery = taskService.createTaskQuery();
            org.flowable.task.api.Task verifyCreditTask = taskQuery.singleResult();
            assertEquals("Verify credit history", verifyCreditTask.getName());

            // Verify with Query API
            ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
            assertNotNull(subProcessInstance);
            assertEquals(pi.getId(), runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId());

            // Completing the task with approval, will end the subprocess and continue the original process
            taskService.complete(verifyCreditTask.getId(), CollectionUtil.singletonMap("creditApproved", true));
            org.flowable.task.api.Task prepareAndShipTask = taskQuery.singleResult();
            assertEquals("Prepare and Ship", prepareAndShipTask.getName());
        }
    }

    @Test
    @Deployment
    public void testSimple() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
            TaskQuery taskQuery = taskService.createTaskQuery();
            org.flowable.task.api.Task userTask = taskQuery.singleResult();
            assertEquals("userTask1", userTask.getName());

            taskService.complete(userTask.getId(), CollectionUtil.singletonMap("myVar", "test789"));

            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
            assertEquals(1, variables.size());

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertEquals("test456", historicVariable.getTextValue());

            assertEquals(9, historyService.createHistoricActivityInstanceQuery().count());
            assertEquals(3, historyService.createHistoricDetailQuery().count());
        }
    }

    @Test
    @Deployment
    public void testSimpleNoWaitState() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
            assertEquals(1, variables.size());

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertEquals("test456", historicVariable.getTextValue());

            assertEquals(7, historyService.createHistoricActivityInstanceQuery().count());
            assertEquals(2, historyService.createHistoricDetailQuery().count());
        }
    }

    @Test
    @Deployment
    public void testParallel() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
            TaskQuery taskQuery = taskService.createTaskQuery();
            org.flowable.task.api.Task userTask = taskQuery.singleResult();
            assertEquals("userTask1", userTask.getName());

            taskService.complete(userTask.getId(), CollectionUtil.singletonMap("myVar", "test789"));

            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .orderByVariableName().asc()
                    .list();
            assertEquals(2, variables.size());

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertEquals("myVar", historicVariable.getName());
            assertEquals("test789", historicVariable.getTextValue());

            HistoricVariableInstanceEntity historicVariable1 = (HistoricVariableInstanceEntity) variables.get(1);
            assertEquals("myVar1", historicVariable1.getName());
            assertEquals("test456", historicVariable1.getTextValue());

            assertEquals(15, historyService.createHistoricActivityInstanceQuery().count());
            assertEquals(5, historyService.createHistoricDetailQuery().count());
        }
    }

    @Test
    @Deployment
    public void testParallelNoWaitState() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
            assertEquals(1, variables.size());

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertEquals("test456", historicVariable.getTextValue());

            assertEquals(13, historyService.createHistoricActivityInstanceQuery().count());
            assertEquals(2, historyService.createHistoricDetailQuery().count());
        }
    }

    @Test
    @Deployment
    public void testTwoSubProcessInParallelWithinSubProcess() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoSubProcessInParallelWithinSubProcess");
            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .orderByVariableName().asc().list();
            assertEquals(2, variables.size());

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertEquals("myVar", historicVariable.getName());
            assertEquals("test101112", historicVariable.getTextValue());

            HistoricVariableInstanceEntity historicVariable1 = (HistoricVariableInstanceEntity) variables.get(1);
            assertEquals("myVar1", historicVariable1.getName());
            assertEquals("test789", historicVariable1.getTextValue());

            assertEquals(32, historyService.createHistoricActivityInstanceQuery().count());
            assertEquals(7, historyService.createHistoricDetailQuery().count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/HistoricVariableInstanceTest.testCallSimpleSubProcess.bpmn20.xml", "org/flowable/engine/test/history/simpleSubProcess.bpmn20.xml" })
    public void testHistoricVariableInstanceQuery() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertEquals(4, historyService.createHistoricVariableInstanceQuery().count());
            assertEquals(4, historyService.createHistoricVariableInstanceQuery().list().size());
            assertEquals(4, historyService.createHistoricVariableInstanceQuery().orderByProcessInstanceId().asc().count());
            assertEquals(4, historyService.createHistoricVariableInstanceQuery().orderByProcessInstanceId().asc().list().size());
            assertEquals(4, historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc().count());
            assertEquals(4, historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc().list().size());

            assertEquals(2, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(2, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list().size());
            assertEquals(2, historyService.createHistoricVariableInstanceQuery().variableName("myVar").count());
            assertEquals(2, historyService.createHistoricVariableInstanceQuery().variableName("myVar").list().size());
            assertEquals(2, historyService.createHistoricVariableInstanceQuery().variableNameLike("myVar1").count());
            assertEquals(2, historyService.createHistoricVariableInstanceQuery().variableNameLike("myVar1").list().size());

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
            assertEquals(4, variables.size());

            assertEquals(1, historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test123").count());
            assertEquals(1, historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test123").list().size());
            assertEquals(1, historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test456").count());
            assertEquals(1, historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test456").list().size());
            assertEquals(1, historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test666").count());
            assertEquals(1, historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test666").list().size());
            assertEquals(1, historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test666").count());
            assertEquals(1, historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test666").list().size());

            assertEquals(14, historyService.createHistoricActivityInstanceQuery().count());
            assertEquals(5, historyService.createHistoricDetailQuery().count());
        }
    }

    @Test
    public void testHistoricVariableQuery2() {
        deployTwoTasksTestProcess();
        
        // Generate data
        Map<String, Object> startVars = new HashMap<>();
        startVars.put("startVar", "hello");
        String processInstanceId = runtimeService.startProcessInstanceByKey("twoTasksProcess", startVars).getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        for (int i = 0; i < tasks.size(); i++) {
            runtimeService.setVariableLocal(tasks.get(i).getExecutionId(), "executionVar" + i, i);
            taskService.setVariableLocal(tasks.get(i).getId(), "taskVar" + i, i);
        }
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Verify historic variable instance queries
        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).orderByVariableName().asc().list();
        assertEquals(5, historicVariableInstances.size());

        List<String> expectedVariableNames = Arrays.asList("executionVar0", "executionVar1", "startVar", "taskVar0", "taskVar1");
        for (int i = 0; i < expectedVariableNames.size(); i++) {
            assertEquals(expectedVariableNames.get(i), historicVariableInstances.get(i).getVariableName());
        }

        // by execution id
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .executionId(tasks.get(0).getExecutionId()).orderByVariableName().asc().list();
        assertEquals(2, historicVariableInstances.size());
        assertEquals("executionVar0", historicVariableInstances.get(0).getVariableName());
        assertEquals("taskVar0", historicVariableInstances.get(1).getVariableName());
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .executionId(tasks.get(1).getExecutionId()).orderByVariableName().asc().list();
        assertEquals(2, historicVariableInstances.size());
        assertEquals("executionVar1", historicVariableInstances.get(0).getVariableName());
        assertEquals("taskVar1", historicVariableInstances.get(1).getVariableName());

        // By process instance id and execution id
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).executionId(tasks.get(0).getExecutionId()).orderByVariableName().asc().list();
        assertEquals(2, historicVariableInstances.size());
        assertEquals("executionVar0", historicVariableInstances.get(0).getVariableName());
        assertEquals("taskVar0", historicVariableInstances.get(1).getVariableName());
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).executionId(tasks.get(1).getExecutionId()).orderByVariableName().asc().list();
        assertEquals(2, historicVariableInstances.size());
        assertEquals("executionVar1", historicVariableInstances.get(0).getVariableName());
        assertEquals("taskVar1", historicVariableInstances.get(1).getVariableName());

        // By task id
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .taskId(tasks.get(0).getId()).list();
        assertEquals(1, historicVariableInstances.size());
        assertEquals("taskVar0", historicVariableInstances.get(0).getVariableName());
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .taskId(tasks.get(1).getId()).list();
        assertEquals(1, historicVariableInstances.size());
        assertEquals("taskVar1", historicVariableInstances.get(0).getVariableName());

        // By task id and process instance id
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).taskId(tasks.get(0).getId()).list();
        assertEquals(1, historicVariableInstances.size());
        assertEquals("taskVar0", historicVariableInstances.get(0).getVariableName());
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).taskId(tasks.get(1).getId()).list();
        assertEquals(1, historicVariableInstances.size());
        assertEquals("taskVar1", historicVariableInstances.get(0).getVariableName());

    }

    @Test
    public void testHistoricVariableQueryByExecutionIds() {
        deployTwoTasksTestProcess();

        Set<String> processInstanceIds = new HashSet<>();
        Set<String> testProcessInstanceIds = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            // Generate data
            Map<String, Object> startVars = new HashMap<>();
            if (i == 1) {
                startVars.put("startVar2", "hello2");
            } else {
                startVars.put("startVar", "hello");
            }
            String processInstanceId = runtimeService.startProcessInstanceByKey("twoTasksProcess", startVars).getId();
            processInstanceIds.add(processInstanceId);
            if (i != 1) {
                testProcessInstanceIds.add(processInstanceId);
            }
        }
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertEquals(2, historyService.createHistoricVariableInstanceQuery().executionIds(testProcessInstanceIds).count());
        assertEquals(2, historyService.createHistoricVariableInstanceQuery().executionIds(testProcessInstanceIds).list().size());

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().executionIds(testProcessInstanceIds).list();
        assertEquals("startVar", historicVariableInstances.get(0).getVariableName());
        assertEquals("hello", historicVariableInstances.get(0).getValue());

        historicVariableInstances = historyService.createHistoricVariableInstanceQuery().executionIds(processInstanceIds).list();
        int startVarCount = 0;
        int startVar2Count = 0;
        for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
            if ("startVar".equals(historicVariableInstance.getVariableName())) {
                startVarCount++;
                assertEquals("hello", historicVariableInstance.getValue());
            
            } else if ("startVar2".equals(historicVariableInstance.getVariableName())) {
                startVar2Count++;
                assertEquals("hello2", historicVariableInstance.getValue());
            }
        }
        
        assertEquals(2, startVarCount);
        assertEquals(1, startVar2Count);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/variableScope.bpmn20.xml"
    })
    public void testHistoricVariableQueryByExecutionIdsForScope() {
        Map<String, Object> processVars = new HashMap<>();
        processVars.put("processVar", "processVar");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableScopeProcess", processVars);

        Set<String> executionIds = new HashSet<>();
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        for (Execution execution : executions) {
            if (!processInstance.getId().equals(execution.getId())) {
                executionIds.add(execution.getId());
                runtimeService.setVariableLocal(execution.getId(), "executionVar", "executionVar");
            }
        }

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (org.flowable.task.api.Task task : tasks) {
            taskService.setVariableLocal(task.getId(), "taskVar", "taskVar");
        }
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        Set<String> processInstanceIds = new HashSet<>();
        processInstanceIds.add(processInstance.getId());
        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().executionIds(processInstanceIds).list();
        assertEquals(1, historicVariableInstances.size());
        assertEquals("processVar", historicVariableInstances.get(0).getVariableName());
        assertEquals("processVar", historicVariableInstances.get(0).getValue());

        historicVariableInstances = historyService.createHistoricVariableInstanceQuery().executionIds(executionIds).excludeTaskVariables().list();
        assertEquals(2, historicVariableInstances.size());
        assertEquals("executionVar", historicVariableInstances.get(0).getVariableName());
        assertEquals("executionVar", historicVariableInstances.get(0).getValue());
        assertEquals("executionVar", historicVariableInstances.get(1).getVariableName());
        assertEquals("executionVar", historicVariableInstances.get(1).getValue());
    }

    @Test
    public void testHistoricVariableQueryByTaskIds() {
        deployTwoTasksTestProcess();
        // Generate data
        String processInstanceId = runtimeService.startProcessInstanceByKey("twoTasksProcess").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        taskService.setVariableLocal(tasks.get(0).getId(), "taskVar1", "hello1");
        taskService.setVariableLocal(tasks.get(1).getId(), "taskVar2", "hello2");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        Set<String> taskIds = new HashSet<>();
        taskIds.add(tasks.get(0).getId());
        taskIds.add(tasks.get(1).getId());
        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().taskIds(taskIds).list();
        assertEquals(2, historyService.createHistoricVariableInstanceQuery().taskIds(taskIds).count());
        assertEquals(2, historicVariableInstances.size());
        assertEquals("taskVar1", historicVariableInstances.get(0).getVariableName());
        assertEquals("hello1", historicVariableInstances.get(0).getValue());
        assertEquals("taskVar2", historicVariableInstances.get(1).getVariableName());
        assertEquals("hello2", historicVariableInstances.get(1).getValue());

        taskIds = new HashSet<>();
        taskIds.add(tasks.get(0).getId());
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery().taskIds(taskIds).list();
        assertEquals(1, historyService.createHistoricVariableInstanceQuery().taskIds(taskIds).count());
        assertEquals(1, historicVariableInstances.size());
        assertEquals("taskVar1", historicVariableInstances.get(0).getVariableName());
        assertEquals("hello1", historicVariableInstances.get(0).getValue());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/variableScope.bpmn20.xml"
    })
    public void testHistoricVariableQueryByTaskIdsForScope() {
        Map<String, Object> processVars = new HashMap<>();
        processVars.put("processVar", "processVar");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableScopeProcess", processVars);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        for (Execution execution : executions) {
            if (!processInstance.getId().equals(execution.getId())) {
                runtimeService.setVariableLocal(execution.getId(), "executionVar", "executionVar");
            }
        }

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Set<String> taskIds = new HashSet<>();
        for (org.flowable.task.api.Task task : tasks) {
            taskService.setVariableLocal(task.getId(), "taskVar", "taskVar");
            taskIds.add(task.getId());
        }
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().taskIds(taskIds).list();
        assertEquals(2, historicVariableInstances.size());
        assertEquals("taskVar", historicVariableInstances.get(0).getVariableName());
        assertEquals("taskVar", historicVariableInstances.get(0).getValue());
        assertEquals("taskVar", historicVariableInstances.get(1).getVariableName());
        assertEquals("taskVar", historicVariableInstances.get(1).getValue());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testHistoricProcessVariableOnDeletion() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            HashMap<String, Object> variables = new HashMap<>();
            variables.put("testVar", "Hallo Christian");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
            runtimeService.deleteProcessInstance(processInstance.getId(), "deleted");
            assertProcessEnded(processInstance.getId());

            // check that process variable is set even if the process is canceled and not ended normally
            assertEquals(1, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableValueEquals("testVar", "Hallo Christian").count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/history/FullHistoryTest.testVariableUpdatesAreLinkedToActivity.bpmn20.xml" })
    public void testVariableUpdatesLinkedToActivity() throws Exception {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("ProcessWithSubProcess");

            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
            Map<String, Object> variables = new HashMap<>();
            variables.put("test", "1");
            taskService.complete(task.getId(), variables);

            // now we are in the subprocess
            task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
            variables.clear();
            variables.put("test", "2");
            taskService.complete(task.getId(), variables);

            // now we are ended
            assertProcessEnded(pi.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // check history
            List<HistoricDetail> updates = historyService.createHistoricDetailQuery().processInstanceId(pi.getId()).variableUpdates().list();
            assertEquals(2, updates.size());

            Map<String, HistoricVariableUpdate> updatesMap = new HashMap<>();
            HistoricVariableUpdate update = (HistoricVariableUpdate) updates.get(0);
            updatesMap.put((String) update.getValue(), update);
            update = (HistoricVariableUpdate) updates.get(1);
            updatesMap.put((String) update.getValue(), update);

            HistoricVariableUpdate update1 = updatesMap.get("1");
            HistoricVariableUpdate update2 = updatesMap.get("2");

            assertNotNull(update1.getActivityInstanceId());
            assertNotNull(update1.getExecutionId());
            HistoricActivityInstance historicActivityInstance1 = historyService.createHistoricActivityInstanceQuery().activityInstanceId(update1.getActivityInstanceId()).singleResult();
            assertEquals("usertask1", historicActivityInstance1.getActivityId());

            // TODO https://activiti.atlassian.net/browse/ACT-1083
            assertNotNull(update2.getActivityInstanceId());
            HistoricActivityInstance historicActivityInstance2 = historyService.createHistoricActivityInstanceQuery().activityInstanceId(update2.getActivityInstanceId()).singleResult();
            assertEquals("usertask2", historicActivityInstance2.getActivityId());

            /*
             * This is OK! The variable is set on the root execution, on a execution never run through the activity, where the process instances stands when calling the set Variable. But the
             * ActivityId of this flow node is used. So the execution id's doesn't have to be equal.
             * 
             * execution id: On which execution it was set activity id: in which activity was the process instance when setting the variable
             */
            assertFalse(historicActivityInstance2.getExecutionId().equals(update2.getExecutionId()));
        }
    }

    // Test for ACT-1528, which (correctly) reported that deleting any
    // historic process instance would remove ALL historic variables.
    // Yes. Real serious bug.
    @Test
    @Deployment
    public void testHistoricProcessInstanceDeleteCascadesCorrectly() {

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {

            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "value1");
            variables.put("var2", "value2");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess", variables);
            assertNotNull(processInstance);

            variables = new HashMap<>();
            variables.put("var3", "value3");
            variables.put("var4", "value4");
            ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("myProcess", variables);
            assertNotNull(processInstance2);
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // check variables
            long count = historyService.createHistoricVariableInstanceQuery().count();
            assertEquals(4, count);

            // delete runtime execution of ONE process instance
            runtimeService.deleteProcessInstance(processInstance.getId(), "reason 1");
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            
            historyService.deleteHistoricProcessInstance(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // recheck variables
            // this is a bug: all variables was deleted after delete a history process instance
            count = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance2.getId())
                    .count();
            assertEquals(2, count);
            
            count = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .count();
            assertEquals(0, count);
        }

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/HistoricVariableInstanceTest.testSimple.bpmn20.xml")
    public void testNativeHistoricVariableInstanceQuery() {

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {

            assertEquals("ACT_HI_VARINST", managementService.getTableName(HistoricVariableInstance.class, false));
            assertEquals("ACT_HI_VARINST", managementService.getTableName(HistoricVariableInstanceEntity.class, false));

            String tableName = managementService.getTableName(HistoricVariableInstance.class);
            String baseQuerySql = "SELECT * FROM " + tableName;

            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "value1");
            variables.put("var2", "value2");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc", variables);
            assertNotNull(processInstance);
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertEquals(3, historyService.createNativeHistoricVariableInstanceQuery().sql(baseQuerySql).list().size());

            String sqlWithConditions = baseQuerySql + " where NAME_ = #{name}";
            assertEquals("test123", historyService.createNativeHistoricVariableInstanceQuery().sql(sqlWithConditions).parameter("name", "myVar").singleResult().getValue());

            sqlWithConditions = baseQuerySql + " where NAME_ like #{name}";
            assertEquals(2, historyService.createNativeHistoricVariableInstanceQuery().sql(sqlWithConditions).parameter("name", "var%").list().size());

            // paging
            assertEquals(3, historyService.createNativeHistoricVariableInstanceQuery().sql(baseQuerySql).listPage(0, 3).size());
            assertEquals(2, historyService.createNativeHistoricVariableInstanceQuery().sql(baseQuerySql).listPage(1, 3).size());
            assertEquals(2, historyService.createNativeHistoricVariableInstanceQuery().sql(sqlWithConditions).parameter("name", "var%").listPage(0, 2).size());
        }

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/HistoricVariableInstanceTest.testSimple.bpmn20.xml")
    public void testNativeHistoricDetailQuery() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            assertEquals("ACT_HI_DETAIL", managementService.getTableName(HistoricDetail.class, false));
            assertEquals("ACT_HI_DETAIL", managementService.getTableName(HistoricVariableUpdate.class, false));

            String tableName = managementService.getTableName(HistoricDetail.class);
            String baseQuerySql = "SELECT * FROM " + tableName;

            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "value1");
            variables.put("var2", "value2");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc", variables);
            assertNotNull(processInstance);
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertEquals(3, historyService.createNativeHistoricDetailQuery().sql(baseQuerySql).list().size());

            String sqlWithConditions = baseQuerySql + " where NAME_ = #{name} and TYPE_ = #{type}";
            assertNotNull(historyService.createNativeHistoricDetailQuery().sql(sqlWithConditions).parameter("name", "myVar").parameter("type", "VariableUpdate").singleResult());

            sqlWithConditions = baseQuerySql + " where NAME_ like #{name}";
            assertEquals(2, historyService.createNativeHistoricDetailQuery().sql(sqlWithConditions).parameter("name", "var%").list().size());

            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            Map<String, String> formDatas = new HashMap<>();
            formDatas.put("field1", "field value 1");
            formDatas.put("field2", "field value 2");
            formService.submitTaskFormData(task.getId(), formDatas);
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            String countSql = "select count(*) from " + tableName + " where TYPE_ = #{type} and PROC_INST_ID_ = #{pid}";
            assertEquals(2, historyService.createNativeHistoricDetailQuery().sql(countSql).parameter("type", "FormProperty").parameter("pid", processInstance.getId()).count());

            // paging
            assertEquals(3, historyService.createNativeHistoricDetailQuery().sql(baseQuerySql).listPage(0, 3).size());
            assertEquals(3, historyService.createNativeHistoricDetailQuery().sql(baseQuerySql).listPage(1, 3).size());
            sqlWithConditions = baseQuerySql + " where TYPE_ = #{type} and PROC_INST_ID_ = #{pid}";
            assertEquals(2, historyService.createNativeHistoricDetailQuery().sql(sqlWithConditions).parameter("type", "FormProperty").parameter("pid", processInstance.getId()).listPage(0, 2).size());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testChangeType() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            TaskQuery taskQuery = taskService.createTaskQuery();
            org.flowable.task.api.Task task = taskQuery.singleResult();
            assertEquals("my task", task.getName());

            // no type change
            runtimeService.setVariable(processInstance.getId(), "firstVar", "123");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertEquals("123", getHistoricVariable("firstVar").getValue());
            
            runtimeService.setVariable(processInstance.getId(), "firstVar", "456");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertEquals("456", getHistoricVariable("firstVar").getValue());
            
            runtimeService.setVariable(processInstance.getId(), "firstVar", "789");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertEquals("789", getHistoricVariable("firstVar").getValue());

            // type is changed from text to integer and back again. same result expected(?)
            runtimeService.setVariable(processInstance.getId(), "secondVar", "123");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertEquals("123", getHistoricVariable("secondVar").getValue());
            
            runtimeService.setVariable(processInstance.getId(), "secondVar", 456);
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            // there are now 2 historic variables, so the following does not work
            assertEquals(456, getHistoricVariable("secondVar").getValue());
            
            runtimeService.setVariable(processInstance.getId(), "secondVar", "789");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            // there are now 3 historic variables, so the following does not work
            assertEquals("789", getHistoricVariable("secondVar").getValue());

            taskService.complete(task.getId());

            assertProcessEnded(processInstance.getId());
        }
    }

    private HistoricVariableInstance getHistoricVariable(String variableName) {
        return historyService.createHistoricVariableInstanceQuery().variableName(variableName).singleResult();
    }

    @Test
    @Deployment
    public void testRestrictByExecutionId() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
            TaskQuery taskQuery = taskService.createTaskQuery();
            org.flowable.task.api.Task userTask = taskQuery.singleResult();
            assertEquals("userTask1", userTask.getName());

            taskService.complete(userTask.getId(), CollectionUtil.singletonMap("myVar", "test789"));

            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().executionId(processInstance.getId()).list();
            assertEquals(1, variables.size());

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertEquals("test456", historicVariable.getTextValue());

            assertEquals(9, historyService.createHistoricActivityInstanceQuery().count());
            assertEquals(3, historyService.createHistoricDetailQuery().count());
        }
    }
}
