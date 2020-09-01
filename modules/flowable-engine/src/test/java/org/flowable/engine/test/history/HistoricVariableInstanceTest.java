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

import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat(verifyCreditTask.getName()).isEqualTo("Verify credit history");

            // Verify with Query API
            ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
            assertThat(subProcessInstance).isNotNull();
            assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId()).isEqualTo(pi.getId());

            // Completing the task with approval, will end the subprocess and continue the original process
            taskService.complete(verifyCreditTask.getId(), CollectionUtil.singletonMap("creditApproved", true));
            org.flowable.task.api.Task prepareAndShipTask = taskQuery.singleResult();
            assertThat(prepareAndShipTask.getName()).isEqualTo("Prepare and Ship");
        }
    }

    @Test
    @Deployment
    public void testSimple() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
            TaskQuery taskQuery = taskService.createTaskQuery();
            org.flowable.task.api.Task userTask = taskQuery.singleResult();
            assertThat(userTask.getName()).isEqualTo("userTask1");

            taskService.complete(userTask.getId(), CollectionUtil.singletonMap("myVar", "test789"));

            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
            assertThat(variables).hasSize(1);

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertThat(historicVariable.getTextValue()).isEqualTo("test456");

            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(9);
            assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(3);
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
            assertThat(variables).hasSize(1);

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertThat(historicVariable.getTextValue()).isEqualTo("test456");

            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(7);
            assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);
        }
    }

    @Test
    @Deployment
    public void testParallel() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
            TaskQuery taskQuery = taskService.createTaskQuery();
            org.flowable.task.api.Task userTask = taskQuery.singleResult();
            assertThat(userTask.getName()).isEqualTo("userTask1");

            taskService.complete(userTask.getId(), CollectionUtil.singletonMap("myVar", "test789"));

            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .orderByVariableName().asc()
                    .list();
            assertThat(variables).hasSize(2);

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertThat(historicVariable.getName()).isEqualTo("myVar");
            assertThat(historicVariable.getTextValue()).isEqualTo("test789");

            HistoricVariableInstanceEntity historicVariable1 = (HistoricVariableInstanceEntity) variables.get(1);
            assertThat(historicVariable1.getName()).isEqualTo("myVar1");
            assertThat(historicVariable1.getTextValue()).isEqualTo("test456");

            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(15);
            assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(5);
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
            assertThat(variables).hasSize(1);

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertThat(historicVariable.getTextValue()).isEqualTo("test456");

            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(13);
            assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);
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
            assertThat(variables).hasSize(2);

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertThat(historicVariable.getName()).isEqualTo("myVar");
            assertThat(historicVariable.getTextValue()).isEqualTo("test101112");

            HistoricVariableInstanceEntity historicVariable1 = (HistoricVariableInstanceEntity) variables.get(1);
            assertThat(historicVariable1.getName()).isEqualTo("myVar1");
            assertThat(historicVariable1.getTextValue()).isEqualTo("test789");

            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(32);
            assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(7);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/HistoricVariableInstanceTest.testCallSimpleSubProcess.bpmn20.xml", "org/flowable/engine/test/history/simpleSubProcess.bpmn20.xml" })
    public void testHistoricVariableInstanceQuery() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(4);
            assertThat(historyService.createHistoricVariableInstanceQuery().list()).hasSize(4);
            assertThat(historyService.createHistoricVariableInstanceQuery().orderByProcessInstanceId().asc().count()).isEqualTo(4);
            assertThat(historyService.createHistoricVariableInstanceQuery().orderByProcessInstanceId().asc().list()).hasSize(4);
            assertThat(historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc().count()).isEqualTo(4);
            assertThat(historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc().list()).hasSize(4);

            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableName("myVar").count()).isEqualTo(2);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableName("myVar").list()).hasSize(2);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableNameLike("myVar1").count()).isEqualTo(2);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableNameLike("myVar1").list()).hasSize(2);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
            assertThat(variables).hasSize(4);

            assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test123").count()).isEqualTo(1);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test123").list()).hasSize(1);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test456").count()).isEqualTo(1);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test456").list()).hasSize(1);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test666").count()).isEqualTo(1);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test666").list()).hasSize(1);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test666").count()).isEqualTo(1);
            assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test666").list()).hasSize(1);

            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(14);
            assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(5);
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
        assertThat(historicVariableInstances).hasSize(5);

        List<String> expectedVariableNames = Arrays.asList("executionVar0", "executionVar1", "startVar", "taskVar0", "taskVar1");
        for (int i = 0; i < expectedVariableNames.size(); i++) {
            assertThat(historicVariableInstances.get(i).getVariableName()).isEqualTo(expectedVariableNames.get(i));
        }

        // by execution id
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .executionId(tasks.get(0).getExecutionId()).orderByVariableName().asc().list();
        assertThat(historicVariableInstances).hasSize(2);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("executionVar0");
        assertThat(historicVariableInstances.get(1).getVariableName()).isEqualTo("taskVar0");
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .executionId(tasks.get(1).getExecutionId()).orderByVariableName().asc().list();
        assertThat(historicVariableInstances).hasSize(2);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("executionVar1");
        assertThat(historicVariableInstances.get(1).getVariableName()).isEqualTo("taskVar1");

        // By process instance id and execution id
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).executionId(tasks.get(0).getExecutionId()).orderByVariableName().asc().list();
        assertThat(historicVariableInstances).hasSize(2);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("executionVar0");
        assertThat(historicVariableInstances.get(1).getVariableName()).isEqualTo("taskVar0");
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).executionId(tasks.get(1).getExecutionId()).orderByVariableName().asc().list();
        assertThat(historicVariableInstances).hasSize(2);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("executionVar1");
        assertThat(historicVariableInstances.get(1).getVariableName()).isEqualTo("taskVar1");

        // By task id
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .taskId(tasks.get(0).getId()).list();
        assertThat(historicVariableInstances).hasSize(1);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("taskVar0");
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .taskId(tasks.get(1).getId()).list();
        assertThat(historicVariableInstances).hasSize(1);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("taskVar1");

        // By task id and process instance id
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).taskId(tasks.get(0).getId()).list();
        assertThat(historicVariableInstances).hasSize(1);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("taskVar0");
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).taskId(tasks.get(1).getId()).list();
        assertThat(historicVariableInstances).hasSize(1);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("taskVar1");

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

        assertThat(historyService.createHistoricVariableInstanceQuery().executionIds(testProcessInstanceIds).count()).isEqualTo(2);
        assertThat(historyService.createHistoricVariableInstanceQuery().executionIds(testProcessInstanceIds).list()).hasSize(2);

        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().executionIds(testProcessInstanceIds).list();
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("startVar");
        assertThat(historicVariableInstances.get(0).getValue()).isEqualTo("hello");

        historicVariableInstances = historyService.createHistoricVariableInstanceQuery().executionIds(processInstanceIds).list();
        int startVarCount = 0;
        int startVar2Count = 0;
        for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
            if ("startVar".equals(historicVariableInstance.getVariableName())) {
                startVarCount++;
                assertThat(historicVariableInstance.getValue()).isEqualTo("hello");
            
            } else if ("startVar2".equals(historicVariableInstance.getVariableName())) {
                startVar2Count++;
                assertThat(historicVariableInstance.getValue()).isEqualTo("hello2");
            }
        }
        
        assertThat(startVarCount).isEqualTo(2);
        assertThat(startVar2Count).isEqualTo(1);
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
        assertThat(historicVariableInstances).hasSize(1);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("processVar");
        assertThat(historicVariableInstances.get(0).getValue()).isEqualTo("processVar");

        historicVariableInstances = historyService.createHistoricVariableInstanceQuery().executionIds(executionIds).excludeTaskVariables().list();
        assertThat(historicVariableInstances).hasSize(2);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("executionVar");
        assertThat(historicVariableInstances.get(0).getValue()).isEqualTo("executionVar");
        assertThat(historicVariableInstances.get(1).getVariableName()).isEqualTo("executionVar");
        assertThat(historicVariableInstances.get(1).getValue()).isEqualTo("executionVar");
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
        assertThat(historyService.createHistoricVariableInstanceQuery().taskIds(taskIds).count()).isEqualTo(2);
        assertThat(historicVariableInstances).hasSize(2);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("taskVar1");
        assertThat(historicVariableInstances.get(0).getValue()).isEqualTo("hello1");
        assertThat(historicVariableInstances.get(1).getVariableName()).isEqualTo("taskVar2");
        assertThat(historicVariableInstances.get(1).getValue()).isEqualTo("hello2");

        taskIds = new HashSet<>();
        taskIds.add(tasks.get(0).getId());
        historicVariableInstances = historyService.createHistoricVariableInstanceQuery().taskIds(taskIds).list();
        assertThat(historyService.createHistoricVariableInstanceQuery().taskIds(taskIds).count()).isEqualTo(1);
        assertThat(historicVariableInstances).hasSize(1);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("taskVar1");
        assertThat(historicVariableInstances.get(0).getValue()).isEqualTo("hello1");
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
        assertThat(historicVariableInstances).hasSize(2);
        assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo("taskVar");
        assertThat(historicVariableInstances.get(0).getValue()).isEqualTo("taskVar");
        assertThat(historicVariableInstances.get(1).getVariableName()).isEqualTo("taskVar");
        assertThat(historicVariableInstances.get(1).getValue()).isEqualTo("taskVar");
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
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableValueEquals("testVar", "Hallo Christian").count()).isEqualTo(1);
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
            assertThat(updates).hasSize(2);

            Map<String, HistoricVariableUpdate> updatesMap = new HashMap<>();
            HistoricVariableUpdate update = (HistoricVariableUpdate) updates.get(0);
            updatesMap.put((String) update.getValue(), update);
            update = (HistoricVariableUpdate) updates.get(1);
            updatesMap.put((String) update.getValue(), update);

            HistoricVariableUpdate update1 = updatesMap.get("1");
            HistoricVariableUpdate update2 = updatesMap.get("2");

            assertThat(update1.getActivityInstanceId()).isNotNull();
            assertThat(update1.getExecutionId()).isNotNull();
            HistoricActivityInstance historicActivityInstance1 = historyService.createHistoricActivityInstanceQuery().activityInstanceId(update1.getActivityInstanceId()).singleResult();
            assertThat(historicActivityInstance1.getActivityId()).isEqualTo("usertask1");

            // TODO https://activiti.atlassian.net/browse/ACT-1083
            assertThat(update2.getActivityInstanceId()).isNotNull();
            HistoricActivityInstance historicActivityInstance2 = historyService.createHistoricActivityInstanceQuery().activityInstanceId(update2.getActivityInstanceId()).singleResult();
            assertThat(historicActivityInstance2.getActivityId()).isEqualTo("usertask2");

            /*
             * This is OK! The variable is set on the root execution, on a execution never run through the activity, where the process instances stands when calling the set Variable. But the
             * ActivityId of this flow node is used. So the execution id's doesn't have to be equal.
             * 
             * execution id: On which execution it was set activity id: in which activity was the process instance when setting the variable
             */
            assertThat(historicActivityInstance2).isNotEqualTo(update2.getExecutionId());
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
            assertThat(processInstance).isNotNull();

            variables = new HashMap<>();
            variables.put("var3", "value3");
            variables.put("var4", "value4");
            ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("myProcess", variables);
            assertThat(processInstance2).isNotNull();
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // check variables
            long count = historyService.createHistoricVariableInstanceQuery().count();
            assertThat(count).isEqualTo(4);

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
            assertThat(count).isEqualTo(2);
            
            count = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .count();
            assertThat(count).isZero();
        }

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/HistoricVariableInstanceTest.testSimple.bpmn20.xml")
    public void testNativeHistoricVariableInstanceQuery() {

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {

            assertThat(managementService.getTableName(HistoricVariableInstance.class, false)).isEqualTo("ACT_HI_VARINST");
            assertThat(managementService.getTableName(HistoricVariableInstanceEntity.class, false)).isEqualTo("ACT_HI_VARINST");

            String tableName = managementService.getTableName(HistoricVariableInstance.class);
            String baseQuerySql = "SELECT * FROM " + tableName;

            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "value1");
            variables.put("var2", "value2");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc", variables);
            assertThat(processInstance).isNotNull();
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertThat(historyService.createNativeHistoricVariableInstanceQuery().sql(baseQuerySql).list()).hasSize(3);

            String sqlWithConditions = baseQuerySql + " where NAME_ = #{name}";
            assertThat(historyService.createNativeHistoricVariableInstanceQuery().sql(sqlWithConditions).parameter("name", "myVar").singleResult().getValue()).isEqualTo("test123");

            sqlWithConditions = baseQuerySql + " where NAME_ like #{name}";
            assertThat(historyService.createNativeHistoricVariableInstanceQuery().sql(sqlWithConditions).parameter("name", "var%").list()).hasSize(2);

            // paging
            assertThat(historyService.createNativeHistoricVariableInstanceQuery().sql(baseQuerySql).listPage(0, 3)).hasSize(3);
            assertThat(historyService.createNativeHistoricVariableInstanceQuery().sql(baseQuerySql).listPage(1, 3)).hasSize(2);
            assertThat(historyService.createNativeHistoricVariableInstanceQuery().sql(sqlWithConditions).parameter("name", "var%").listPage(0, 2)).hasSize(2);
        }

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/HistoricVariableInstanceTest.testSimple.bpmn20.xml")
    public void testNativeHistoricDetailQuery() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            assertThat(managementService.getTableName(HistoricDetail.class, false)).isEqualTo("ACT_HI_DETAIL");
            assertThat(managementService.getTableName(HistoricVariableUpdate.class, false)).isEqualTo("ACT_HI_DETAIL");

            String tableName = managementService.getTableName(HistoricDetail.class);
            String baseQuerySql = "SELECT * FROM " + tableName;

            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "value1");
            variables.put("var2", "value2");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc", variables);
            assertThat(processInstance).isNotNull();
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertThat(historyService.createNativeHistoricDetailQuery().sql(baseQuerySql).list()).hasSize(3);

            String sqlWithConditions = baseQuerySql + " where NAME_ = #{name} and TYPE_ = #{type}";
            assertThat(historyService.createNativeHistoricDetailQuery().sql(sqlWithConditions).parameter("name", "myVar").parameter("type", "VariableUpdate").singleResult()).isNotNull();

            sqlWithConditions = baseQuerySql + " where NAME_ like #{name}";
            assertThat(historyService.createNativeHistoricDetailQuery().sql(sqlWithConditions).parameter("name", "var%").list()).hasSize(2);

            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            Map<String, String> formDatas = new HashMap<>();
            formDatas.put("field1", "field value 1");
            formDatas.put("field2", "field value 2");
            formService.submitTaskFormData(task.getId(), formDatas);
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            String countSql = "select count(*) from " + tableName + " where TYPE_ = #{type} and PROC_INST_ID_ = #{pid}";
            assertThat(historyService.createNativeHistoricDetailQuery().sql(countSql).parameter("type", "FormProperty").parameter("pid", processInstance.getId()).count()).isEqualTo(2);

            // paging
            assertThat(historyService.createNativeHistoricDetailQuery().sql(baseQuerySql).listPage(0, 3)).hasSize(3);
            assertThat(historyService.createNativeHistoricDetailQuery().sql(baseQuerySql).listPage(1, 3)).hasSize(3);
            sqlWithConditions = baseQuerySql + " where TYPE_ = #{type} and PROC_INST_ID_ = #{pid}";
            assertThat(historyService.createNativeHistoricDetailQuery().sql(sqlWithConditions).parameter("type", "FormProperty").parameter("pid", processInstance.getId()).listPage(0, 2)).hasSize(2);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml" })
    public void testChangeType() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            TaskQuery taskQuery = taskService.createTaskQuery();
            org.flowable.task.api.Task task = taskQuery.singleResult();
            assertThat(task.getName()).isEqualTo("my task");

            // no type change
            runtimeService.setVariable(processInstance.getId(), "firstVar", "123");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertThat(getHistoricVariable("firstVar").getValue()).isEqualTo("123");
            
            runtimeService.setVariable(processInstance.getId(), "firstVar", "456");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertThat(getHistoricVariable("firstVar").getValue()).isEqualTo("456");
            
            runtimeService.setVariable(processInstance.getId(), "firstVar", "789");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertThat(getHistoricVariable("firstVar").getValue()).isEqualTo("789");

            // type is changed from text to integer and back again. same result expected(?)
            runtimeService.setVariable(processInstance.getId(), "secondVar", "123");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertThat(getHistoricVariable("secondVar").getValue()).isEqualTo("123");
            
            runtimeService.setVariable(processInstance.getId(), "secondVar", 456);
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            // there are now 2 historic variables, so the following does not work
            assertThat(getHistoricVariable("secondVar").getValue()).isEqualTo(456);
            
            runtimeService.setVariable(processInstance.getId(), "secondVar", "789");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            // there are now 3 historic variables, so the following does not work
            assertThat(getHistoricVariable("secondVar").getValue()).isEqualTo("789");

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
            assertThat(userTask.getName()).isEqualTo("userTask1");

            taskService.complete(userTask.getId(), CollectionUtil.singletonMap("myVar", "test789"));

            assertProcessEnded(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().executionId(processInstance.getId()).list();
            assertThat(variables).hasSize(1);

            HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
            assertThat(historicVariable.getTextValue()).isEqualTo("test456");

            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(9);
            assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(3);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryVariableValueEqualsAndNotEquals() {
        ProcessInstance processWithStringValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        ProcessInstance processWithNullValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With null value")
                .variable("var", null)
                .start();

        ProcessInstance processWithLongValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With long value")
                .variable("var", 100L)
                .start();

        ProcessInstance processWithDoubleValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueNotEquals("var", "TEST").list())
                .extracting(HistoricVariableInstance::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        processWithNullValue.getId(),
                        processWithLongValue.getId(),
                        processWithDoubleValue.getId()
                );

        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("var", "TEST").list())
                .extracting(HistoricVariableInstance::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        processWithStringValue.getId()
                );

        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueNotEquals("var", 100L).list())
                .extracting(HistoricVariableInstance::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        processWithStringValue.getId(),
                        processWithNullValue.getId(),
                        processWithDoubleValue.getId()
                );

        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("var", 100L).list())
                .extracting(HistoricVariableInstance::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        processWithLongValue.getId()
                );

        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueNotEquals("var", 45.55).list())
                .extracting(HistoricVariableInstance::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        processWithStringValue.getId(),
                        processWithNullValue.getId(),
                        processWithLongValue.getId()
                );

        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("var", 45.55).list())
                .extracting(HistoricVariableInstance::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        processWithDoubleValue.getId()
                );

        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueNotEquals("var", "test").list())
                .extracting(HistoricVariableInstance::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        processWithStringValue.getId(),
                        processWithNullValue.getId(),
                        processWithLongValue.getId(),
                        processWithDoubleValue.getId()
                );

        assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("var", "test").list())
                .extracting(HistoricVariableInstance::getProcessInstanceId)
                .isEmpty();
    }
}
