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

package org.flowable.engine.test.bpmn.multiinstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.examples.bpmn.servicetask.ValueBean;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class MultiInstanceTest extends PluggableFlowableTestCase {

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testSequentialUserTasks() {
        checkSequentialUserTasks("miSequentialUserTasks");
    }

    @Deployment
    public void testSequentialUserTasksCustomExtensions() {
        checkSequentialUserTasks("miSequentialUserTasksCustomExtensions");
    }

    private void checkSequentialUserTasks(String processDefinitionKey) {
        String procId = runtimeService.startProcessInstanceByKey(processDefinitionKey, CollectionUtil.singletonMap("nrOfLoops", 3)).getId();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("My Task", task.getName());
        assertEquals("kermit_0", task.getAssignee());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        assertEquals("My Task", task.getName());
        assertEquals("kermit_1", task.getAssignee());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        assertEquals("My Task", task.getName());
        assertEquals("kermit_2", task.getAssignee());
        taskService.complete(task.getId());

        assertNull(taskService.createTaskQuery().singleResult());
        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testSequentialUserTasksHistory() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 4)).getId();
        for (int i = 0; i < 4; i++) {
            taskService.complete(taskService.createTaskQuery().singleResult().getId());
        }
        assertProcessEnded(procId);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().list();
            assertEquals(4, historicTaskInstances.size());
            for (HistoricTaskInstance ht : historicTaskInstances) {
                assertNotNull(ht.getAssignee());
                assertNotNull(ht.getStartTime());
                assertNotNull(ht.getEndTime());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
            assertEquals(4, historicActivityInstances.size());
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertNotNull(hai.getActivityId());
                assertNotNull(hai.getActivityName());
                assertNotNull(hai.getStartTime());
                assertNotNull(hai.getEndTime());
                assertNotNull(hai.getAssignee());
            }

        }
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testSequentialUserTasksWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 3)).getId();

        // Complete 1 tasks
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());
        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testSequentialUserTasksCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 10)).getId();

        // 10 tasks are to be created, but completionCondition stops them at 5
        for (int i = 0; i < 5; i++) {
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            taskService.complete(task.getId());
        }
        assertNull(taskService.createTaskQuery().singleResult());
        assertProcessEnded(procId);
    }

    @Deployment
    public void testNestedSequentialUserTasks() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialUserTasks").getId();

        for (int i = 0; i < 3; i++) {
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskAssignee("kermit").singleResult();
            assertEquals("My Task", task.getName());
            taskService.complete(task.getId());
        }

        assertProcessEnded(procId);
    }

    @Deployment
    public void testParallelUserTasks() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasks").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(3, tasks.size());
        assertEquals("My Task 0", tasks.get(0).getName());
        assertEquals("My Task 1", tasks.get(1).getName());
        assertEquals("My Task 2", tasks.get(2).getName());

        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        taskService.complete(tasks.get(2).getId());
        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelUserTasks.bpmn20.xml" })
    public void testParallelUserTasksHistory() {
        runtimeService.startProcessInstanceByKey("miParallelUserTasks");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(3, tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals("My Task " + i, tasks.get(i).getName());
            taskService.complete(tasks.get(i).getId());
        }

        // Validate history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().asc().list();
            for (int i = 0; i < historicTaskInstances.size(); i++) {
                HistoricTaskInstance hi = historicTaskInstances.get(i);
                assertNotNull(hi.getStartTime());
                assertNotNull(hi.getEndTime());
                assertEquals("My Task " + i, hi.getName());
                assertEquals("kermit_" + i, hi.getAssignee());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
            assertEquals(3, historicActivityInstances.size());
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertNotNull(hai.getStartTime());
                assertNotNull(hai.getEndTime());
                assertNotNull(hai.getAssignee());
                assertEquals("userTask", hai.getActivityType());
            }
        }
    }

    @Deployment
    public void testParallelUserTasksWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksWithTimer").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        taskService.complete(tasks.get(0).getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());
        assertProcessEnded(procId);
    }

    @Deployment
    public void testParallelUserTasksCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksCompletionCondition").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(5, tasks.size());

        // Completing 3 tasks gives 50% of tasks completed, which triggers
        // completionCondition
        for (int i = 0; i < 3; i++) {
            assertEquals(5 - i, taskService.createTaskQuery().count());
            taskService.complete(tasks.get(i).getId());
        }
        assertProcessEnded(procId);
    }

    @Deployment
    public void testParallelUserTasksBasedOnCollection() {
        List<String> assigneeList = Arrays.asList("kermit", "gonzo", "mispiggy", "fozzie", "bubba");
        String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksBasedOnCollection", CollectionUtil.singletonMap("assigneeList", assigneeList)).getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
        assertEquals(5, tasks.size());
        assertEquals("bubba", tasks.get(0).getAssignee());
        assertEquals("fozzie", tasks.get(1).getAssignee());
        assertEquals("gonzo", tasks.get(2).getAssignee());
        assertEquals("kermit", tasks.get(3).getAssignee());
        assertEquals("mispiggy", tasks.get(4).getAssignee());

        // Completing 3 tasks will trigger completioncondition
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        taskService.complete(tasks.get(2).getId());
        assertEquals(0, taskService.createTaskQuery().count());
        assertProcessEnded(procId);
    }

    @Deployment
    public void testParallelUserTasksCustomCollectionStringExtension() {
    	checkParallelUserTasksCustomCollection("miParallelUserTasksCollection");
    }

    private void checkParallelUserTasksCustomCollection(String processDefinitionKey) {
    	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

        List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("wfuser1").list();
        assertEquals(1, tasks.size());
        assertEquals("My Task 0", tasks.get(0).getName());

        tasks = taskService.createTaskQuery().taskCandidateUser("wfuser2").list();
        assertEquals(1, tasks.size());
        assertEquals("My Task 1", tasks.get(0).getName());

        // should be 2 tasks total
        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("My Task 0", tasks.get(0).getName());
        assertEquals("My Task 1", tasks.get(1).getName());

        // Completing 3 tasks will trigger completion condition
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        assertEquals(0, taskService.createTaskQuery().count());
        assertProcessEnded(processInstance.getProcessInstanceId());
    }

    @Deployment
    public void testParallelUserTasksCustomCollectionStringExtensionDelegateExpression() {
    	checkParallelUserTasksCustomCollectionDelegateExpression("miParallelUserTasksCollection");
    }

    private void checkParallelUserTasksCustomCollectionDelegateExpression(String processDefinitionKey) {
    	// Add bean temporary to process engine

    	Map<Object, Object> originalBeans = processEngineConfiguration.getExpressionManager().getBeans();

    	try {

    		Map<Object, Object> newBeans = new HashMap<>();
    		newBeans.put("collectionHandler", new JSONCollectionHandler());
    		processEngineConfiguration.getExpressionManager().setBeans(newBeans);

    		checkParallelUserTasksCustomCollection(processDefinitionKey);
    	} finally {

    		// Put beans back
    		processEngineConfiguration.getExpressionManager().setBeans(originalBeans);

    	}

    }

    @Deployment
    public void testParallelUserTasksCustomCollectionExpressionExtension() {
    	checkParallelUserTasksCustomCollection("miParallelUserTasksCollection");
    }

    @Deployment
    public void testParallelUserTasksCustomCollectionExpressionExtensionDelegateExpression() {
    	checkParallelUserTasksCustomCollectionDelegateExpression("miParallelUserTasksCollection");
    }

    @Deployment
    public void testParallelUserTasksCustomExtensionsCollection() {
    	checkParallelUserTasksCustomCollection("miParallelUserTasksCollection");
    }

    @Deployment
    public void testParallelUserTasksCustomExtensionsCollectionDelegateExpression() {
    	checkParallelUserTasksCustomCollectionDelegateExpression("miParallelUserTasksCollection");
    }

    @Deployment
    public void testParallelUserTasksCustomExtensions() {
        checkParallelUserTasksCustomExtensions("miParallelUserTasks");
    }

    @Deployment
    public void testParallelUserTasksCustomExtensionsLoopIndexVariable() {
        checkParallelUserTasksCustomExtensions("miParallelUserTasksLoopVariable");
    }

    private void checkParallelUserTasksCustomExtensions(String processDefinitionKey) {
        Map<String, Object> vars = new HashMap<>();
        List<String> assigneeList = Arrays.asList("kermit", "gonzo", "fozzie");
        vars.put("assigneeList", assigneeList);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, vars);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(3, tasks.size());
        assertEquals("My Task 0", tasks.get(0).getName());
        assertEquals("My Task 1", tasks.get(1).getName());
        assertEquals("My Task 2", tasks.get(2).getName());

        tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
        assertEquals("fozzie", tasks.get(0).getAssignee());
        assertEquals("gonzo", tasks.get(1).getAssignee());
        assertEquals("kermit", tasks.get(2).getAssignee());

        // Completing 3 tasks will trigger completion condition
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        taskService.complete(tasks.get(2).getId());
        assertEquals(0, taskService.createTaskQuery().count());
        assertProcessEnded(processInstance.getProcessInstanceId());
    }

    @Deployment
    public void testParallelUserTasksExecutionAndTaskListeners() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
        List<Task> tasks = taskService.createTaskQuery().list();
        for (Task task : tasks) {
            taskService.complete(task.getId());
        }

        Execution waitState = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertNotNull(waitState);

        assertEquals(3, runtimeService.getVariable(processInstance.getId(), "taskListenerCounter"));
        assertEquals(3, runtimeService.getVariable(processInstance.getId(), "executionListenerCounter"));

        runtimeService.trigger(waitState.getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Deployment
    public void testExecutionListener() {
        Map<String, Object> vars = new HashMap<>();
        List<String> countersigns = new ArrayList<>();
        countersigns.add("zjl0");
        countersigns.add("zjl1");
        countersigns.add("zjl3");
        vars.put("countersignAssigneeList", countersigns);
        vars.put("approveResult", "notpass");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("approve-process", vars);
        assertNotNull(processInstance);
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 5000, 200);

        List<Task> tasks = taskService.createTaskQuery().list();
        for (Task task : tasks){
            assertEquals(task.getAssignee(), taskService.getVariable(task.getId(), "csAssignee"));
            taskService.setVariableLocal(task.getId(), "csApproveResult", "pass");
            taskService.complete(task.getId());
            
            HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                            processEngineConfiguration.getManagementService(), 5000, 200);
        }
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Deployment
    public void testSequentialExecutionListener() {
        Map<String, Object> vars = new HashMap<>();
        List<String> countersigns = new ArrayList<>();
        countersigns.add("zjl0");
        countersigns.add("zjl1");
        countersigns.add("zjl3");
        vars.put("countersignAssigneeList", countersigns);
        vars.put("approveResult", "notpass");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("approve-process", vars);
        assertNotNull(processInstance);
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 5000, 200);

        Task task = taskService.createTaskQuery().singleResult();
        assertEquals(task.getAssignee(), taskService.getVariable(task.getId(), "csAssignee"));
        taskService.setVariableLocal(task.getId(), "csApproveResult", "pass");
        taskService.complete(task.getId());
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 5000, 200);
        
        task = taskService.createTaskQuery().singleResult();
        assertEquals(task.getAssignee(), taskService.getVariable(task.getId(), "csAssignee"));
        taskService.setVariableLocal(task.getId(), "csApproveResult", "pass");
        taskService.complete(task.getId());
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 5000, 200);
        
        task = taskService.createTaskQuery().singleResult();
        assertEquals(task.getAssignee(), taskService.getVariable(task.getId(), "csAssignee"));
        taskService.setVariableLocal(task.getId(), "csApproveResult", "pass");
        taskService.complete(task.getId());
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 5000, 200);
        
        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testNestedParallelUserTasks() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelUserTasks").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
        for (org.flowable.task.api.Task task : tasks) {
            assertEquals("My Task", task.getName());
            taskService.complete(task.getId());
        }

        assertProcessEnded(procId);
    }

    @Deployment
    public void testSequentialScriptTasks() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sum", 0);
        vars.put("nrOfLoops", 5);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialScriptTask", vars);
        int sum = (Integer) runtimeService.getVariable(processInstance.getId(), "sum");
        assertEquals(10, sum);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialScriptTasks.bpmn20.xml" })
    public void testSequentialScriptTasksHistory() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sum", 0);
        vars.put("nrOfLoops", 7);
        runtimeService.startProcessInstanceByKey("miSequentialScriptTask", vars);

        // Validate history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").orderByActivityId().asc().list();
            assertEquals(7, historicInstances.size());
            for (int i = 0; i < 7; i++) {
                HistoricActivityInstance hai = historicInstances.get(i);
                assertEquals("scriptTask", hai.getActivityType());
                assertNotNull(hai.getStartTime());
                assertNotNull(hai.getEndTime());
            }
        }
    }

    @Deployment
    public void testSequentialScriptTasksCompletionCondition() {
        runtimeService.startProcessInstanceByKey("miSequentialScriptTaskCompletionCondition").getId();
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertEquals(2, executions.size());
        Execution processInstanceExecution = null;
        Execution waitStateExecution = null;
        for (Execution execution : executions) {
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                processInstanceExecution = execution;
            } else {
                waitStateExecution = execution;
            }
        }
        assertNotNull(processInstanceExecution);
        assertNotNull(waitStateExecution);
        int sum = (Integer) runtimeService.getVariable(waitStateExecution.getId(), "sum");
        assertEquals(5, sum);
    }

    @Deployment
    public void testParallelScriptTasks() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sum", 0);
        vars.put("nrOfLoops", 10);
        runtimeService.startProcessInstanceByKey("miParallelScriptTask", vars);
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertEquals(2, executions.size());
        Execution processInstanceExecution = null;
        Execution waitStateExecution = null;
        for (Execution execution : executions) {
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                processInstanceExecution = execution;
            } else {
                waitStateExecution = execution;
            }
        }
        assertNotNull(processInstanceExecution);
        assertNotNull(waitStateExecution);
        int sum = (Integer) runtimeService.getVariable(waitStateExecution.getId(), "sum");
        assertEquals(45, sum);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelScriptTasks.bpmn20.xml" })
    public void testParallelScriptTasksHistory() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sum", 0);
        vars.put("nrOfLoops", 4);
        runtimeService.startProcessInstanceByKey("miParallelScriptTask", vars);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").list();
            assertEquals(4, historicActivityInstances.size());
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertNotNull(hai.getStartTime());
                assertNotNull(hai.getEndTime());
            }
        }
    }

    @Deployment
    public void testParallelScriptTasksCompletionCondition() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelScriptTaskCompletionCondition");
        Execution waitStateExecution = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertNotNull(waitStateExecution);
        int sum = (Integer) runtimeService.getVariable(processInstance.getId(), "sum");
        assertEquals(2, sum);
        runtimeService.trigger(waitStateExecution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelScriptTasksCompletionCondition.bpmn20.xml" })
    public void testParallelScriptTasksCompletionConditionHistory() {
        runtimeService.startProcessInstanceByKey("miParallelScriptTaskCompletionCondition");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").list();
            assertEquals(2, historicActivityInstances.size());
        }
    }

    @Deployment
    public void testSequentialSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocess").getId();

        TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = query.list();
            assertEquals(2, tasks.size());

            assertEquals("task one", tasks.get(0).getName());
            assertEquals("task two", tasks.get(1).getName());

            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());

            if (i != 3) {
                List<String> activities = runtimeService.getActiveActivityIds(procId);
                assertNotNull(activities);
                assertEquals(3, activities.size());
            }
        }

        assertProcessEnded(procId);
    }

    @Deployment
    public void testSequentialSubProcessEndEvent() {
        // ACT-1185: end-event in subprocess causes inactivated execution
        String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocess").getId();

        TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = query.list();
            assertEquals(1, tasks.size());

            assertEquals("task one", tasks.get(0).getName());

            taskService.complete(tasks.get(0).getId());

            // Last run, the execution no longer exists
            if (i != 3) {
                List<String> activities = runtimeService.getActiveActivityIds(procId);
                assertNotNull(activities);
                assertEquals(2, activities.size());
            }
        }

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialSubProcess.bpmn20.xml" })
    public void testSequentialSubProcessHistory() {
        runtimeService.startProcessInstanceByKey("miSequentialSubprocess");
        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
        }

        // Validate history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> onlySubProcessInstances = historyService.createHistoricActivityInstanceQuery().activityType("subProcess").list();
            assertEquals(4, onlySubProcessInstances.size());

            List<HistoricActivityInstance> historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("subProcess").list();
            assertEquals(4, historicInstances.size());
            for (HistoricActivityInstance hai : historicInstances) {
                assertNotNull(hai.getStartTime());
                assertNotNull(hai.getEndTime());
            }

            historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
            assertEquals(8, historicInstances.size());
            for (HistoricActivityInstance hai : historicInstances) {
                assertNotNull(hai.getStartTime());
                assertNotNull(hai.getEndTime());
            }
        }
    }

    @Deployment
    public void testSequentialSubProcessWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessWithTimer").getId();

        // Complete one subprocess
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Deployment
    public void testSequentialSubProcessCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessCompletionCondition").getId();

        TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
        for (int i = 0; i < 3; i++) {
            List<org.flowable.task.api.Task> tasks = query.list();
            assertEquals(2, tasks.size());

            assertEquals("task one", tasks.get(0).getName());
            assertEquals("task two", tasks.get(1).getName());

            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
        }

        assertProcessEnded(procId);
    }

    @Deployment
    public void testNestedSequentialSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialSubProcess").getId();

        for (int i = 0; i < 3; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
        }

        assertProcessEnded(procId);
    }

    @Deployment
    public void testNestedSequentialSubProcessWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialSubProcessWithTimer").getId();

        for (int i = 0; i < 2; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
        }

        // Complete one task, to make it a bit more trickier
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
        taskService.complete(tasks.get(0).getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Deployment
    public void testParallelSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocess").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(4, tasks.size());

        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelSubProcess.bpmn20.xml" })
    public void testParallelSubProcessHistory() {
        runtimeService.startProcessInstanceByKey("miParallelSubprocess");
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.complete(task.getId());
        }

        // Validate history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("miSubProcess").list();
            assertEquals(2, historicActivityInstances.size());
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertNotNull(hai.getStartTime());
                assertNotNull(hai.getEndTime());
            }
        }
    }

    @Deployment
    public void testParallelSubProcessWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessWithTimer").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(6, tasks.size());

        // Complete two tasks
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Deployment
    public void testParallelSubProcessCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessCompletionCondition").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(4, tasks.size());

        List<org.flowable.task.api.Task> subProcessTasks1 = taskService.createTaskQuery().taskDefinitionKey("subProcessTask1").list();
        assertEquals(2, subProcessTasks1.size());

        List<org.flowable.task.api.Task> subProcessTasks2 = taskService.createTaskQuery().taskDefinitionKey("subProcessTask2").list();
        assertEquals(2, subProcessTasks2.size());

        Execution taskExecution = runtimeService.createExecutionQuery().executionId(subProcessTasks1.get(0).getExecutionId()).singleResult();
        String parentExecutionId = taskExecution.getParentId();

        org.flowable.task.api.Task subProcessTask2 = null;
        for (org.flowable.task.api.Task task : subProcessTasks2) {
            Execution toFindExecution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
            if (toFindExecution.getParentId().equals(parentExecutionId)) {
                subProcessTask2 = task;
                break;
            }
        }

        assertNotNull(subProcessTask2);
        taskService.complete(tasks.get(0).getId());
        taskService.complete(subProcessTask2.getId());

        assertProcessEnded(procId);
    }

    @Deployment
    public void testParallelSubProcessAllAutomatic() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessAllAutomatics", CollectionUtil.singletonMap("nrOfLoops", 5)).getId();

        for (int i = 0; i < 5; i++) {
            List<Execution> waitSubExecutions = runtimeService.createExecutionQuery().activityId("subProcessWait").list();
            assertTrue(waitSubExecutions.size() > 0);
            runtimeService.trigger(waitSubExecutions.get(0).getId());
        }

        List<Execution> waitSubExecutions = runtimeService.createExecutionQuery().activityId("subProcessWait").list();
        assertEquals(0, waitSubExecutions.size());

        Execution waitState = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertEquals(10, runtimeService.getVariable(waitState.getId(), "sum"));

        runtimeService.trigger(waitState.getId());
        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelSubProcessAllAutomatic.bpmn20.xml" })
    public void testParallelSubProcessAllAutomaticCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessAllAutomatics", CollectionUtil.singletonMap("nrOfLoops", 10)).getId();

        for (int i = 0; i < 6; i++) {
            List<Execution> waitSubExecutions = runtimeService.createExecutionQuery().activityId("subProcessWait").list();
            assertTrue(waitSubExecutions.size() > 0);
            runtimeService.trigger(waitSubExecutions.get(0).getId());
        }

        List<Execution> waitSubExecutions = runtimeService.createExecutionQuery().activityId("subProcessWait").list();
        assertEquals(0, waitSubExecutions.size());

        Execution waitState = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertEquals(12, runtimeService.getVariable(procId, "sum"));

        runtimeService.trigger(waitState.getId());
        assertProcessEnded(procId);
    }

    @Deployment
    public void testNestedParallelSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelSubProcess").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(8, tasks.size());

        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }
        assertProcessEnded(procId);
    }

    @Deployment
    public void testNestedParallelSubProcessWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelSubProcess").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(12, tasks.size());

        for (int i = 0; i < 3; i++) {
            taskService.complete(tasks.get(i).getId());
        }

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testCallActivityLocalVariables.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testCallActivityLocalVariables() {
        Map<String, Object> variables = Collections.singletonMap("name", (Object) "test");
        String procId = runtimeService.startProcessInstanceByKey("miSubProcessLocalVariables", variables).getId();

        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertEquals(2, tasks.size());
            assertEquals("task one", tasks.get(0).getName());
            assertEquals("task two", tasks.get(1).getName());
            
            Map<String, Object> taskVariables = Collections.singletonMap("output", (Object) ("run" + i + 1));
            taskService.complete(tasks.get(0).getId(), taskVariables);
            taskService.complete(tasks.get(1).getId());
            
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(procId).singleResult();
            assertNotNull(task);
            assertEquals("task", task.getTaskDefinitionKey());
            
            ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().processInstanceId(procId).activityId("task").singleResult();
            assertEquals("run" + i + 1, runtimeService.getVariableLocal(execution.getId(), "output"));
            assertNull(runtimeService.getVariable(procId, "output"));
            
            taskService.complete(task.getId());
        }

        assertProcessEnded(procId);
    }
    
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testCallActivityNormalVariables.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
        public void testCallActivityNormalVariables() {
        Map<String, Object> variables = Collections.singletonMap("name", (Object) "test");
        String procId = runtimeService.startProcessInstanceByKey("miSubProcessNormalVariables", variables).getId();
        
        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertEquals(2, tasks.size());
            assertEquals("task one", tasks.get(0).getName());
            assertEquals("task two", tasks.get(1).getName());
            
            Map<String, Object> taskVariables = Collections.singletonMap("output", (Object) ("run" + i + 1));
            taskService.complete(tasks.get(0).getId(), taskVariables);
            taskService.complete(tasks.get(1).getId());
            
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(procId).singleResult();
            assertNotNull(task);
            assertEquals("task", task.getTaskDefinitionKey());
            
            ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().processInstanceId(procId).activityId("task").singleResult();
            assertNull(runtimeService.getVariableLocal(execution.getId(), "output"));
            assertEquals("run" + i + 1, runtimeService.getVariable(procId, "output"));
            
            taskService.complete(task.getId());
        }
        
        assertProcessEnded(procId);
    }
    
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivity.bpmn20.xml",
    "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
public void testSequentialCallActivity() {
String procId = runtimeService.startProcessInstanceByKey("miSequentialCallActivity").getId();

for (int i = 0; i < 3; i++) {
    List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertEquals(2, tasks.size());
    assertEquals("task one", tasks.get(0).getName());
    assertEquals("task two", tasks.get(1).getName());
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
}

assertProcessEnded(procId);
}

    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivityWithList.bpmn20.xml")
    public void testSequentialCallActivityWithList() {
        ArrayList<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("list", list);

        String procId = runtimeService.startProcessInstanceByKey("parentProcess", variables).getId();

        org.flowable.task.api.Task task1 = taskService.createTaskQuery().processVariableValueEquals("element", "one").singleResult();
        org.flowable.task.api.Task task2 = taskService.createTaskQuery().processVariableValueEquals("element", "two").singleResult();

        assertNotNull(task1);
        assertNotNull(task2);

        HashMap<String, Object> subVariables = new HashMap<>();
        subVariables.put("x", "y");

        taskService.complete(task1.getId(), subVariables);
        taskService.complete(task2.getId(), subVariables);

        org.flowable.task.api.Task task3 = taskService.createTaskQuery().processDefinitionKey("midProcess").singleResult();
        assertNotNull(task3);
        taskService.complete(task3.getId(), null);

        org.flowable.task.api.Task task4 = taskService.createTaskQuery().processDefinitionKey("parentProcess").singleResult();
        assertNotNull(task4);
        taskService.complete(task4.getId(), null);

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivityWithTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testSequentialCallActivityWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialCallActivityWithTimer").getId();

        // Complete first subprocess
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("task one", tasks.get(0).getName());
        assertEquals("task two", tasks.get(1).getName());
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testParallelCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelCallActivity").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(12, tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            taskService.complete(tasks.get(i).getId());
        }

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testParallelCallActivityHistory() {
        runtimeService.startProcessInstanceByKey("miParallelCallActivity");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(12, tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            taskService.complete(tasks.get(i).getId());
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // Validate historic processes
            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
            assertEquals(7, historicProcessInstances.size()); // 6 subprocesses
                                                              // + main process
            for (HistoricProcessInstance hpi : historicProcessInstances) {
                assertNotNull(hpi.getStartTime());
                assertNotNull(hpi.getEndTime());
            }

            // Validate historic tasks
            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().list();
            assertEquals(12, historicTaskInstances.size());
            for (HistoricTaskInstance hti : historicTaskInstances) {
                assertNotNull(hti.getStartTime());
                assertNotNull(hti.getEndTime());
                assertNotNull(hti.getAssignee());
                assertNull(hti.getDeleteReason());
            }

            // Validate historic activities
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("callActivity").list();
            assertEquals(6, historicActivityInstances.size());
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertNotNull(hai.getStartTime());
                assertNotNull(hai.getEndTime());
            }
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivityWithTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testParallelCallActivityWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelCallActivity").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(6, tasks.size());
        for (int i = 0; i < 2; i++) {
            taskService.complete(tasks.get(i).getId());
        }

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedSequentialCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedSequentialCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialCallActivity").getId();

        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertEquals(2, tasks.size());
            assertEquals("task one", tasks.get(0).getName());
            assertEquals("task two", tasks.get(1).getName());
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
        }

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedSequentialCallActivityWithTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedSequentialCallActivityWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialCallActivityWithTimer").getId();

        // first instance
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("task one", tasks.get(0).getName());
        assertEquals("task two", tasks.get(1).getName());
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        // one task of second instance
        tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());
        taskService.complete(tasks.get(0).getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedParallelCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivity").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(14, tasks.size());
        for (int i = 0; i < 14; i++) {
            taskService.complete(tasks.get(i).getId());
        }

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivityWithTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedParallelCallActivityWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivityWithTimer").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(4, tasks.size());
        for (int i = 0; i < 3; i++) {
            taskService.complete(tasks.get(i).getId());
        }

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertEquals("taskAfterTimer", taskAfterTimer.getTaskDefinitionKey());
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivityCompletionCondition.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedParallelCallActivityCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivityCompletionCondition").getId();

        assertEquals(8, taskService.createTaskQuery().count());

        for (int i = 0; i < 2; i++) {
            ProcessInstance nextSubProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("externalSubProcess").listPage(0, 1).get(0);
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(nextSubProcessInstance.getId()).list();
            assertEquals(2, tasks.size());
            for (org.flowable.task.api.Task task : tasks) {
                taskService.complete(task.getId());
            }
        }

        assertProcessEnded(procId);
    }

    // ACT-764
    @Deployment
    public void testSequentialServiceTaskWithClass() {
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("multiInstanceServiceTask", CollectionUtil.singletonMap("result", 5));
        Integer result = (Integer) runtimeService.getVariable(procInst.getId(), "result");
        assertEquals(160, result.intValue());

        Execution waitExecution = runtimeService.createExecutionQuery().processInstanceId(procInst.getId()).activityId("wait").singleResult();
        runtimeService.trigger(waitExecution.getId());
        assertProcessEnded(procInst.getId());
    }

    @Deployment
    public void testSequentialServiceTaskWithClassAndCollection() {
        Collection<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6);
        Map<String, Object> vars = new HashMap<>();
        vars.put("result", 1);
        vars.put("items", items);

        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("multiInstanceServiceTask", vars);
        Integer result = (Integer) runtimeService.getVariable(procInst.getId(), "result");
        assertEquals(720, result.intValue());

        Execution waitExecution = runtimeService.createExecutionQuery().processInstanceId(procInst.getId()).activityId("wait").singleResult();
        runtimeService.trigger(waitExecution.getId());
        assertProcessEnded(procInst.getId());
    }

    // ACT-901
    @Deployment
    public void testAct901() {

        Date startTime = processEngineConfiguration.getClock().getCurrentTime();

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("multiInstanceSubProcess");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();

        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + 61000L)); // timer is set to one minute
        List<Job> timers = managementService.createTimerJobQuery().list();
        assertEquals(5, timers.size());

        // Execute all timers one by one (single thread vs thread pool of job
        // executor, which leads to optimisticlockingexceptions!)
        for (Job timer : timers) {
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
        }

        // All tasks should be canceled
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();
        assertEquals(0, tasks.size());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.callActivityWithBoundaryErrorEvent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.throwingErrorEventSubProcess.bpmn20.xml" })
    public void testMultiInstanceCallActivityWithErrorBoundaryEvent() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("assignees", Arrays.asList("kermit", "gonzo"));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());

        // finish first call activity with error
        variableMap = new HashMap<>();
        variableMap.put("done", false);
        taskService.complete(tasks.get(0).getId(), variableMap);

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());

        taskService.complete(tasks.get(0).getId());

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").list();
        assertEquals(0, processInstances.size());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.callActivityWithBoundaryErrorEventSequential.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.throwingErrorEventSubProcess.bpmn20.xml" })
    public void testSequentialMultiInstanceCallActivityWithErrorBoundaryEvent() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("assignees", Arrays.asList("kermit", "gonzo"));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());

        // finish first call activity with error
        variableMap = new HashMap<>();
        variableMap.put("done", false);
        taskService.complete(tasks.get(0).getId(), variableMap);

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());

        taskService.complete(tasks.get(0).getId());

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").list();
        assertEquals(0, processInstances.size());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testMultiInstanceParallelReceiveTask() {
        runtimeService.startProcessInstanceByKey("multi-instance-receive");
        List<Execution> executions = runtimeService.createExecutionQuery().activityId("theReceiveTask").list();
        assertEquals(4, executions.size());

        // Complete all four of the executions
        for (Execution execution : executions) {
            runtimeService.trigger(execution.getId());
        }

        // There is one task after the task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment
    public void testMultiInstanceParalelReceiveTaskWithTimer() {
        Date startTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startTime);

        runtimeService.startProcessInstanceByKey("multiInstanceReceiveWithTimer");
        List<Execution> executions = runtimeService.createExecutionQuery().activityId("theReceiveTask").list();
        assertEquals(3, executions.size());

        // Signal only one execution. Then the timer will fire
        runtimeService.trigger(executions.get(1).getId());
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + 60000L));
        waitForJobExecutorToProcessAllJobs(10000L, 1000L);

        // The process should now be in the task after the timer
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Task after timer", task.getName());

        // Completing it should end the process
        taskService.complete(task.getId());
        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment
    public void testMultiInstanceSequentialReceiveTask() {
        runtimeService.startProcessInstanceByKey("multi-instance-receive");
        Execution execution = runtimeService.createExecutionQuery().activityId("theReceiveTask").singleResult();
        assertNotNull(execution);

        // Complete all four of the executions
        while (execution != null) {
            runtimeService.trigger(execution.getId());
            execution = runtimeService.createExecutionQuery().activityId("theReceiveTask").singleResult();
        }

        // There is one task after the task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedMultiInstanceTasks.bpmn20.xml" })
    public void testNestedMultiInstanceTasks() {
        List<String> processes = Arrays.asList("process A", "process B");
        List<String> assignees = Arrays.asList("kermit", "gonzo");
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("subProcesses", processes);
        variableMap.put("assignees", assignees);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miNestedMultiInstanceTasks", variableMap);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(processes.size() * assignees.size(), tasks.size());

        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("miNestedMultiInstanceTasks").list();
        assertEquals(0, processInstances.size());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialSubprocessEmptyCollection.bpmn20.xml" })
    public void testSequentialSubprocessEmptyCollection() {
        Collection<String> collection = Collections.emptyList();
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSequentialSubProcessEmptyCollection", variableMap);
        assertNotNull(processInstance);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNull(task);
        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialEmptyCollection.bpmn20.xml" })
    public void testSequentialEmptyCollection() {
        Collection<String> collection = Collections.emptyList();
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSequentialEmptyCollection", variableMap);
        assertNotNull(processInstance);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNull(task);
        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialEmptyCollection.bpmn20.xml" })
    public void testSequentialEmptyCollectionWithNonEmptyCollection() {
        Collection<String> collection = Collections.singleton("Test");
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSequentialEmptyCollection", variableMap);
        assertNotNull(processInstance);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelEmptyCollection.bpmn20.xml" })
    public void testParalellEmptyCollection() throws Exception {
        Collection<String> collection = Collections.emptyList();
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testParalellEmptyCollection", variableMap);
        assertNotNull(processInstance);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNull(task);
        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelEmptyCollection.bpmn20.xml" })
    public void testParalellEmptyCollectionWithNonEmptyCollection() {
        Collection<String> collection = Collections.singleton("Test");
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testParalellEmptyCollection", variableMap);
        assertNotNull(processInstance);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testInfiniteLoopWithDelegateExpressionFix() {

        // Add bean temporary to process engine

        Map<Object, Object> originalBeans = processEngineConfiguration.getExpressionManager().getBeans();

        try {

            Map<Object, Object> newBeans = new HashMap<>();
            newBeans.put("SampleTask", new TestSampleServiceTask());
            processEngineConfiguration.getExpressionManager().setBeans(newBeans);

            Map<String, Object> params = new HashMap<>();
            params.put("sampleValues", Arrays.asList("eins", "zwei", "drei"));
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("infiniteLoopTest", params);
            assertNotNull(processInstance);

        } finally {

            // Put beans back
            processEngineConfiguration.getExpressionManager().setBeans(originalBeans);

        }
    }

    @Deployment
    public void testEmptyCollectionOnParallelUserTask() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("messages", Collections.EMPTY_LIST);
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelUserTaskMi", vars);

            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).finished().count());
        }
    }

    @Deployment
    public void testZeroLoopCardinalityOnParallelUserTask() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelUserTaskMi");
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).finished().count());
        }
    }

    @Deployment
    public void testEmptyCollectionOnSequentialEmbeddedSubprocess() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("messages", Collections.EMPTY_LIST);
            runtimeService.startProcessInstanceByKey("sequentialMiSubprocess", vars);

            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().finished().count());
        }
    }

    @Deployment
    public void testEmptyCollectionOnParallelEmbeddedSubprocess() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("messages", Collections.EMPTY_LIST);
            runtimeService.startProcessInstanceByKey("parallelMiSubprocess", vars);
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().finished().count());
        }
    }

    @Deployment
    public void testExecutionListenersOnMultiInstanceSubprocess() {
        resetTestCounts();
        Map<String, Object> variableMap = new HashMap<>();
        List<String> assignees = new ArrayList<>();
        assignees.add("john");
        assignees.add("jane");
        assignees.add("matt");
        variableMap.put("assignees", assignees);
        runtimeService.startProcessInstanceByKey("MultiInstanceTest", variableMap);

        assertEquals(3, TestStartExecutionListener.countWithLoopCounter.get());
        assertEquals(3, TestEndExecutionListener.countWithLoopCounter.get());

        assertEquals(1, TestStartExecutionListener.countWithoutLoopCounter.get());
        assertEquals(1, TestEndExecutionListener.countWithoutLoopCounter.get());
    }

    @Deployment
    public void testExecutionListenersOnMultiInstanceUserTask() {
        resetTestCounts();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExecutionListenersOnMultiInstanceUserTask");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertEquals(4, TestTaskCompletionListener.count.get());

        assertEquals(4, TestStartExecutionListener.countWithLoopCounter.get());
        assertEquals(4, TestEndExecutionListener.countWithLoopCounter.get());

        assertEquals(1, TestStartExecutionListener.countWithoutLoopCounter.get());
        assertEquals(1, TestEndExecutionListener.countWithoutLoopCounter.get());
    }

    @Deployment
    public void testParallelAfterSequentialMultiInstance() {

        // Used to throw a nullpointer exception

        runtimeService.startProcessInstanceByKey("multiInstance");
        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment
    public void testEndTimeOnMiSubprocess() {

        if (!processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            return;
        }

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceSubProcessParallelTasks");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        assertEquals("User Task 1", tasks.get(0).getName());
        assertEquals("User Task 1", tasks.get(1).getName());
        
        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

        // End time should not be set for the subprocess
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
        assertEquals(2, historicActivityInstances.size());
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            assertNotNull(historicActivityInstance.getStartTime());
            assertNull(historicActivityInstance.getEndTime());
        }

        // Complete one of the user tasks. This should not trigger setting of end time of the subprocess, but due to a bug it did exactly that
        taskService.complete(tasks.get(0).getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
        
        historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
        assertEquals(2, historicActivityInstances.size());
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            assertNull(historicActivityInstance.getEndTime());
        }

        taskService.complete(tasks.get(1).getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
        
        historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
        assertEquals(2, historicActivityInstances.size());
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            assertNull(historicActivityInstance.getEndTime());
        }

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("User Task 3").list();
        assertEquals(2, tasks.size());
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            
            historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
            assertEquals(2, historicActivityInstances.size());
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertNull(historicActivityInstance.getEndTime());
            }
        }

        // Finishing the tasks should also set the end time
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }
        
        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

        historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
        assertEquals(2, historicActivityInstances.size());
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            assertNotNull(historicActivityInstance.getEndTime());
        }
    }

    @Deployment
    public void testChangingCollection() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("multi_users", Collections.singletonList("testuser"));
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("test_multi", vars);
        assertNotNull(instance);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("multi", task.getTaskDefinitionKey());
        vars.put("multi_users", new ArrayList<String>()); // <-- Problem here.
        taskService.complete(task.getId(), vars);
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().list();
        assertEquals(0, instances.size());
    }

    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.simpleMultiInstanceWithCollectionVariable.bpmn20.xml")
    public void testCollectionVariableMissing() {
        try {
            runtimeService.startProcessInstanceByKey("simple_multi");
            fail("Should have failed with missing collection variable");
        } catch (FlowableIllegalArgumentException e) {
            assertEquals("Variable 'elements' is not found", e.getMessage());
        }
    }

    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.simpleMultiInstanceWithCollectionVariable.bpmn20.xml")
    public void testCollectionVariableIsNotACollection() {
        Map<String, Object> vars = new HashMap<>();
        ValueBean valueBean = new ValueBean("test");
        vars.put("elements", valueBean);
        try {
            runtimeService.startProcessInstanceByKey("simple_multi", vars);
            fail("Should have failed with collection variable not a collection");
        } catch (FlowableIllegalArgumentException e) {
            assertEquals("Variable 'elements':" + valueBean + " is not a Collection", e.getMessage());
        }
    }

    protected void resetTestCounts() {
        TestStartExecutionListener.countWithLoopCounter.set(0);
        TestStartExecutionListener.countWithoutLoopCounter.set(0);
        TestEndExecutionListener.countWithLoopCounter.set(0);
        TestEndExecutionListener.countWithoutLoopCounter.set(0);
        TestTaskCompletionListener.count.set(0);
    }

    public static class TestStartExecutionListener implements ExecutionListener {

        public static AtomicInteger countWithLoopCounter = new AtomicInteger(0);
        public static AtomicInteger countWithoutLoopCounter = new AtomicInteger(0);

        @Override
        public void notify(DelegateExecution execution) {
            Integer loopCounter = (Integer) execution.getVariable("loopCounter");
            if (loopCounter != null) {
                countWithLoopCounter.incrementAndGet();
            } else {
                countWithoutLoopCounter.incrementAndGet();
            }
        }

    }

    public static class TestEndExecutionListener implements ExecutionListener {

        public static AtomicInteger countWithLoopCounter = new AtomicInteger(0);
        public static AtomicInteger countWithoutLoopCounter = new AtomicInteger(0);

        @Override
        public void notify(DelegateExecution execution) {
            Integer loopCounter = (Integer) execution.getVariable("loopCounter");
            if (loopCounter != null) {
                countWithLoopCounter.incrementAndGet();
            } else {
                countWithoutLoopCounter.incrementAndGet();
            }
        }

    }

    public static class TestTaskCompletionListener implements TaskListener {

        public static AtomicInteger count = new AtomicInteger(0);

        @Override
        public void notify(DelegateTask delegateTask) {
            count.incrementAndGet();
        }

    }

}
