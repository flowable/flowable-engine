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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCompletedEvent;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityEvent;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
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
import org.flowable.job.api.JobInfo;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class MultiInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testSequentialUserTasks() {
        checkSequentialUserTasks("miSequentialUserTasks");
    }

    @Test
    @Deployment
    public void testSequentialUserTasksCustomExtensions() {
        checkSequentialUserTasks("miSequentialUserTasksCustomExtensions");
    }

    private void checkSequentialUserTasks(String processDefinitionKey) {
        String procId = runtimeService.startProcessInstanceByKey(processDefinitionKey, CollectionUtil.singletonMap("nrOfLoops", 3)).getId();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("My Task");
        assertThat(task.getAssignee()).isEqualTo("kermit_0");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("My Task");
        assertThat(task.getAssignee()).isEqualTo("kermit_1");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("My Task");
        assertThat(task.getAssignee()).isEqualTo("kermit_2");
        taskService.complete(task.getId());

        assertThat(taskService.createTaskQuery().singleResult()).isNull();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testSequentialUserTasksHistory() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 4)).getId();
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(procId).list()).hasSize(1);
        }
        
        for (int i = 0; i < 4; i++) {
            String taskId = taskService.createTaskQuery().singleResult().getId();
            taskService.complete(taskId);
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNotNull();
            }
        }
        assertProcessEnded(procId);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().list();
            assertThat(historicTaskInstances).hasSize(4);
            for (HistoricTaskInstance ht : historicTaskInstances) {
                assertThat(ht.getAssignee()).isNotNull();
                assertThat(ht.getStartTime()).isNotNull();
                assertThat(ht.getEndTime()).isNotNull();
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
            assertThat(historicActivityInstances).hasSize(4);
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertThat(hai.getActivityId()).isNotNull();
                assertThat(hai.getActivityName()).isNotNull();
                assertThat(hai.getStartTime()).isNotNull();
                assertThat(hai.getEndTime()).isNotNull();
                assertThat(hai.getAssignee()).isNotNull();
            }
        }
    }

    @Test
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
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testSequentialUserTasksCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 10)).getId();

        // 10 tasks are to be created, but completionCondition stops them at 5
        for (int i = 0; i < 5; i++) {
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            taskService.complete(task.getId());
        }
        assertThat(taskService.createTaskQuery().singleResult()).isNull();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testNestedSequentialUserTasks() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialUserTasks").getId();

        for (int i = 0; i < 3; i++) {
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskAssignee("kermit").singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            taskService.complete(task.getId());
        }

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testParallelUserTasks() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasks").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("My Task 0", "My Task 1", "My Task 2");

        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        taskService.complete(tasks.get(2).getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelUserTasks.bpmn20.xml" })
    public void testParallelUserTasksHistory() {
        runtimeService.startProcessInstanceByKey("miParallelUserTasks");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks).hasSize(3);
        for (int i = 0; i < tasks.size(); i++) {
            assertThat(tasks.get(i).getName()).isEqualTo("My Task " + i);
            taskService.complete(tasks.get(i).getId());
        }

        // Validate history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().asc().list();
            for (int i = 0; i < historicTaskInstances.size(); i++) {
                HistoricTaskInstance hi = historicTaskInstances.get(i);
                assertThat(hi.getStartTime()).isNotNull();
                assertThat(hi.getEndTime()).isNotNull();
                assertThat(hi.getName()).isEqualTo("My Task " + i);
                assertThat(hi.getAssignee()).isEqualTo("kermit_" + i);
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
            assertThat(historicActivityInstances).hasSize(3);
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertThat(hai.getStartTime()).isNotNull();
                assertThat(hai.getEndTime()).isNotNull();
                assertThat(hai.getAssignee()).isNotNull();
                assertThat(hai.getActivityType()).isEqualTo("userTask");
            }
        }
    }

    @Test
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
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testParallelUserTasksCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksCompletionCondition").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(5);

        // Completing 3 tasks gives 50% of tasks completed, which triggers
        // completionCondition
        for (int i = 0; i < 3; i++) {
            assertThat(taskService.createTaskQuery().count()).isEqualTo(5 - i);
            taskService.complete(tasks.get(i).getId());
        }
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testParallelUserTasksBasedOnCollection() {
        List<String> assigneeList = Arrays.asList("kermit", "gonzo", "mispiggy", "fozzie", "bubba");
        String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksBasedOnCollection", CollectionUtil.singletonMap("assigneeList", assigneeList)).getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
        assertThat(tasks)
                .extracting(Task::getAssignee)
                .containsExactly("bubba", "fozzie", "gonzo", "kermit", "mispiggy");

        // Completing 3 tasks will trigger completioncondition
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        taskService.complete(tasks.get(2).getId());
        assertThat(taskService.createTaskQuery().count()).isZero();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testParallelUserTasksCustomCollectionStringExtension() {
    	checkParallelUserTasksCustomCollection("miParallelUserTasksCollection");
    }

    private void checkParallelUserTasksCustomCollection(String processDefinitionKey) {
    	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

        List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("wfuser1").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("My Task 0");

        tasks = taskService.createTaskQuery().taskCandidateUser("wfuser2").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("My Task 1");

        // should be 2 tasks total
        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("My Task 0", "My Task 1");

        // Completing 3 tasks will trigger completion condition
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        assertThat(taskService.createTaskQuery().count()).isZero();
        assertProcessEnded(processInstance.getProcessInstanceId());
    }

    @Test
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

    @Test
    @Deployment
    public void testParallelUserTasksCustomCollectionExpressionExtension() {
    	checkParallelUserTasksCustomCollection("miParallelUserTasksCollection");
    }

    @Test
    @Deployment
    public void testParallelUserTasksCustomCollectionExpressionExtensionDelegateExpression() {
    	checkParallelUserTasksCustomCollectionDelegateExpression("miParallelUserTasksCollection");
    }

    @Test
    @Deployment
    public void testParallelUserTasksCustomExtensionsCollection() {
    	checkParallelUserTasksCustomCollection("miParallelUserTasksCollection");
    }

    @Test
    @Deployment
    public void testParallelUserTasksCustomExtensionsCollectionDelegateExpression() {
    	checkParallelUserTasksCustomCollectionDelegateExpression("miParallelUserTasksCollection");
    }

    @Test
    @Deployment
    public void testParallelUserTasksCustomExtensions() {
        checkParallelUserTasksCustomExtensions("miParallelUserTasks");
    }

    @Test
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
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("My Task 0", "My Task 1", "My Task 2");

        tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
        assertThat(tasks)
                .extracting(Task::getAssignee)
                .containsExactly("fozzie", "gonzo", "kermit");

        // Completing 3 tasks will trigger completion condition
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        taskService.complete(tasks.get(2).getId());
        assertThat(taskService.createTaskQuery().count()).isZero();
        assertProcessEnded(processInstance.getProcessInstanceId());
    }

    @Test
    @Deployment
    public void testParallelUserTasksExecutionAndTaskListeners() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
        List<Task> tasks = taskService.createTaskQuery().list();
        for (Task task : tasks) {
            taskService.complete(task.getId());
        }

        Execution waitState = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(waitState).isNotNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "taskListenerCounter")).isEqualTo(3);
        assertThat(runtimeService.getVariable(processInstance.getId(), "executionListenerCounter")).isEqualTo(3);

        runtimeService.trigger(waitState.getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
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
        assertThat(processInstance).isNotNull();
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 7000, 200);

        List<Task> tasks = taskService.createTaskQuery().list();
        for (Task task : tasks){
            assertThat(taskService.getVariable(task.getId(), "csAssignee")).isEqualTo(task.getAssignee());
            taskService.setVariableLocal(task.getId(), "csApproveResult", "pass");
            taskService.complete(task.getId());
            
            HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                            processEngineConfiguration.getManagementService(), 7000, 200);
        }
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
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
        assertThat(processInstance).isNotNull();
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 7000, 200);

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(taskService.getVariable(task.getId(), "csAssignee")).isEqualTo(task.getAssignee());
        taskService.setVariableLocal(task.getId(), "csApproveResult", "pass");
        taskService.complete(task.getId());
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 7000, 200);
        
        task = taskService.createTaskQuery().singleResult();
        assertThat(taskService.getVariable(task.getId(), "csAssignee")).isEqualTo(task.getAssignee());
        taskService.setVariableLocal(task.getId(), "csApproveResult", "pass");
        taskService.complete(task.getId());
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 7000, 200);
        
        task = taskService.createTaskQuery().singleResult();
        assertThat(taskService.getVariable(task.getId(), "csAssignee")).isEqualTo(task.getAssignee());
        taskService.setVariableLocal(task.getId(), "csApproveResult", "pass");
        taskService.complete(task.getId());
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, 
                        processEngineConfiguration.getManagementService(), 7000, 200);
        
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testNestedParallelUserTasks() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelUserTasks").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
        for (org.flowable.task.api.Task task : tasks) {
            assertThat(task.getName()).isEqualTo("My Task");
            taskService.complete(task.getId());
        }

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testSequentialScriptTasks() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sum", 0);
        vars.put("nrOfLoops", 5);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialScriptTask", vars);
        int sum = (Integer) runtimeService.getVariable(processInstance.getId(), "sum");
        assertThat(sum).isEqualTo(10);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialScriptTasks.bpmn20.xml" })
    public void testSequentialScriptTasksHistory() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sum", 0);
        vars.put("nrOfLoops", 7);
        runtimeService.startProcessInstanceByKey("miSequentialScriptTask", vars);

        // Validate history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").orderByActivityId().asc().list();
            assertThat(historicInstances).hasSize(7);
            for (int i = 0; i < 7; i++) {
                HistoricActivityInstance hai = historicInstances.get(i);
                assertActivityInstancesAreSame(hai, runtimeService.createActivityInstanceQuery().activityInstanceId(hai.getId()).singleResult());
                assertThat(hai.getActivityType()).isEqualTo("scriptTask");
                assertThat(hai.getStartTime()).isNotNull();
                assertThat(hai.getEndTime()).isNotNull();
            }
        }
    }

    @Test
    @Deployment
    public void testSequentialScriptTasksCompletionCondition() {
        runtimeService.startProcessInstanceByKey("miSequentialScriptTaskCompletionCondition").getId();
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2);
        Execution processInstanceExecution = null;
        Execution waitStateExecution = null;
        for (Execution execution : executions) {
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                processInstanceExecution = execution;
            } else {
                waitStateExecution = execution;
            }
        }
        assertThat(processInstanceExecution).isNotNull();
        assertThat(waitStateExecution).isNotNull();
        int sum = (Integer) runtimeService.getVariable(waitStateExecution.getId(), "sum");
        assertThat(sum).isEqualTo(5);
    }

    @Test
    @Deployment
    public void testParallelScriptTasks() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sum", 0);
        vars.put("nrOfLoops", 10);
        runtimeService.startProcessInstanceByKey("miParallelScriptTask", vars);
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2);
        Execution processInstanceExecution = null;
        Execution waitStateExecution = null;
        for (Execution execution : executions) {
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                processInstanceExecution = execution;
            } else {
                waitStateExecution = execution;
            }
        }
        assertThat(processInstanceExecution).isNotNull();
        assertThat(waitStateExecution).isNotNull();
        int sum = (Integer) runtimeService.getVariable(waitStateExecution.getId(), "sum");
        assertThat(sum).isEqualTo(45);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelScriptTasks.bpmn20.xml" })
    public void testParallelScriptTasksHistory() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sum", 0);
        vars.put("nrOfLoops", 4);
        runtimeService.startProcessInstanceByKey("miParallelScriptTask", vars);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").list();
            assertThat(historicActivityInstances).hasSize(4);
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertActivityInstancesAreSame(hai, runtimeService.createActivityInstanceQuery().activityInstanceId(hai.getId()).singleResult());
                assertThat(hai.getStartTime()).isNotNull();
                assertThat(hai.getEndTime()).isNotNull();
            }
        }
    }

    @Test
    @Deployment
    public void testParallelAsyncScriptTasks() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("miParallelAsyncScriptTask")
                    .variable("nrOfLoops", 10)
                    .start();
            List<Job> jobs = managementService.createJobQuery().list();
            // There are 10 jobs for each async execution
            assertThat(jobs).hasSize(10);
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricVariableInstance varInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName("nrOfCompletedInstances")
                        .singleResult();
                assertThat(varInstance).isNotNull();
                assertThat(varInstance.getValue()).isEqualTo(0);
    
                varInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName("nrOfActiveInstances")
                        .singleResult();
                assertThat(varInstance).isNotNull();
                assertThat(varInstance.getValue()).isEqualTo(10);
            }
    
            // When a job fails it is moved to the timer jobs, so it can be executed later
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000L, 200);
            jobs = managementService.createJobQuery().list();
            assertThat(jobs).isEmpty();
            List<Job> timerJobs = managementService.createTimerJobQuery().list();
            assertThat(timerJobs).isEmpty();
    
            List<Job> deadLetterJobs = managementService.createDeadLetterJobQuery().list();
            assertThat(deadLetterJobs).isEmpty();
    
    
            List<Execution> executions = runtimeService.createExecutionQuery().list();
            assertThat(executions).hasSize(2);
            Execution processInstanceExecution = null;
            Execution waitStateExecution = null;
            for (Execution execution : executions) {
                if (execution.getId().equals(execution.getProcessInstanceId())) {
                    processInstanceExecution = execution;
                } else {
                    waitStateExecution = execution;
                }
            }
            assertThat(processInstanceExecution).isNotNull();
            assertThat(waitStateExecution).isNotNull();
    
            Map<String, VariableInstance> variableInstances = runtimeService.getVariableInstances(processInstanceExecution.getProcessInstanceId());
            assertThat(variableInstances).containsOnlyKeys("nrOfLoops");
            VariableInstance nrOfLoops = variableInstances.get("nrOfLoops");
            assertThat(nrOfLoops.getValue()).isEqualTo(10);
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricVariableInstance varInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceExecution.getProcessInstanceId())
                        .variableName("nrOfCompletedInstances")
                        .singleResult();
                assertThat(varInstance).isNotNull();
                assertThat(varInstance.getValue()).isEqualTo(10);
    
                varInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceExecution.getProcessInstanceId())
                        .variableName("nrOfActiveInstances")
                        .singleResult();
                assertThat(varInstance).isNotNull();
                assertThat(varInstance.getValue()).isEqualTo(0);
            }
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelAsyncScriptTasks.bpmn20.xml")
    public void testParallelAsyncScriptTasksWithoutAsyncLeave() {
        boolean originalAsyncLeave = processEngineConfiguration.isParallelMultiInstanceAsyncLeave();
        processEngineConfiguration.setParallelMultiInstanceAsyncLeave(false);
        try {

            runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("miParallelAsyncScriptTask")
                    .variable("nrOfLoops", 10)
                    .start();
            List<Job> jobs = managementService.createJobQuery().list();
            // There are 10 jobs for each async execution
            assertThat(jobs).hasSize(10);

            // When a job fails it is moved to the timer jobs, so it can be executed later
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(20000, 200);
            jobs = managementService.createJobQuery().list();
            assertThat(jobs).isEmpty();
            List<Job> timerJobs = managementService.createTimerJobQuery().list();
            assertThat(timerJobs)
                    .isNotEmpty()
                    .extracting(JobInfo::getExceptionMessage)
                    .allSatisfy(message -> {
                        assertThat(message)
                                .contains("was updated by another transaction concurrently");
                    });

            List<String> timerJobsExceptionStacktraces = timerJobs.stream()
                    .map(JobInfo::getId)
                    .map(managementService::getTimerJobExceptionStacktrace)
                    .collect(Collectors.toList());
            assertThat(timerJobsExceptionStacktraces)
                    .allSatisfy(stacktrace -> {
                        assertThat(stacktrace).contains("FlowableOptimisticLockingException");
                    });

            List<Job> deadLetterJobs = managementService.createDeadLetterJobQuery().list();
            assertThat(deadLetterJobs).isEmpty();

            List<Execution> executions = runtimeService.createExecutionQuery().list();
            assertThat(executions).hasSizeGreaterThan(timerJobs.size());
            Execution waitStateExecution = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
            assertThat(waitStateExecution).isNull();
        } finally {
            processEngineConfiguration.setParallelMultiInstanceAsyncLeave(originalAsyncLeave);
        }
    }

    @Test
    @Deployment
    public void testParallelScriptTasksCompletionCondition() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelScriptTaskCompletionCondition");
        Execution waitStateExecution = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(waitStateExecution).isNotNull();
        int sum = (Integer) runtimeService.getVariable(processInstance.getId(), "sum");
        assertThat(sum).isEqualTo(2);
        runtimeService.trigger(waitStateExecution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelScriptTasksCompletionCondition.bpmn20.xml" })
    public void testParallelScriptTasksCompletionConditionHistory() {
        runtimeService.startProcessInstanceByKey("miParallelScriptTaskCompletionCondition");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("scriptTask").list();
            assertThat(historicActivityInstances).hasSize(2);
        }
    }

    @Test
    @Deployment
    public void testSequentialSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocess").getId();

        TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = query.list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("task one", "task two");

            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());

            if (i != 3) {
                List<String> activities = runtimeService.getActiveActivityIds(procId);
                assertThat(activities).hasSize(3);
            }
        }

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testSequentialSubProcessEndEvent() {
        // ACT-1185: end-event in subprocess causes inactivated execution
        String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocess").getId();

        TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = query.list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("task one");

            taskService.complete(tasks.get(0).getId());

            // Last run, the execution no longer exists
            if (i != 3) {
                List<String> activities = runtimeService.getActiveActivityIds(procId);
                assertThat(activities).hasSize(2);
            }
        }

        assertProcessEnded(procId);
    }

    @Test
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
            assertThat(onlySubProcessInstances).hasSize(4);

            List<HistoricActivityInstance> historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("subProcess").list();
            assertThat(historicInstances).hasSize(4);
            for (HistoricActivityInstance hai : historicInstances) {
                assertThat(hai.getStartTime()).isNotNull();
                assertThat(hai.getEndTime()).isNotNull();
            }

            historicInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
            assertThat(historicInstances).hasSize(8);
            for (HistoricActivityInstance hai : historicInstances) {
                assertThat(hai.getStartTime()).isNotNull();
                assertThat(hai.getEndTime()).isNotNull();
            }
        }
    }

    @Test
    @Deployment
    public void testSequentialSubProcessWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessWithTimer").getId();

        // Complete one subprocess
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testSequentialSubProcessCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessCompletionCondition").getId();

        TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
        for (int i = 0; i < 3; i++) {
            List<org.flowable.task.api.Task> tasks = query.list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("task one", "task two");

            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
        }

        assertProcessEnded(procId);
    }

    @Test
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

    @Test
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
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testParallelSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocess").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks).hasSize(4);

        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelSubProcess.bpmn20.xml" })
    public void testParallelSubProcessHistory() {
        runtimeService.startProcessInstanceByKey("miParallelSubprocess");
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.complete(task.getId());
        }

        // Validate history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("miSubProcess").list();
            assertThat(historicActivityInstances).hasSize(2);
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertThat(hai.getStartTime()).isNotNull();
                assertThat(hai.getEndTime()).isNotNull();
            }
        }
    }

    @Test
    @Deployment
    public void testParallelSubProcessWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessWithTimer").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(6);

        // Complete two tasks
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testParallelSubProcessCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessCompletionCondition").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(4);

        List<org.flowable.task.api.Task> subProcessTasks1 = taskService.createTaskQuery().taskDefinitionKey("subProcessTask1").list();
        assertThat(subProcessTasks1).hasSize(2);

        List<org.flowable.task.api.Task> subProcessTasks2 = taskService.createTaskQuery().taskDefinitionKey("subProcessTask2").list();
        assertThat(subProcessTasks2).hasSize(2);

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

        assertThat(subProcessTask2).isNotNull();
        taskService.complete(tasks.get(0).getId());
        taskService.complete(subProcessTask2.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testParallelSubProcessAllAutomatic() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessAllAutomatics", CollectionUtil.singletonMap("nrOfLoops", 5)).getId();

        for (int i = 0; i < 5; i++) {
            List<Execution> waitSubExecutions = runtimeService.createExecutionQuery().activityId("subProcessWait").list();
            assertThat(waitSubExecutions).isNotEmpty();
            runtimeService.trigger(waitSubExecutions.get(0).getId());
        }

        List<Execution> waitSubExecutions = runtimeService.createExecutionQuery().activityId("subProcessWait").list();
        assertThat(waitSubExecutions).isEmpty();

        Execution waitState = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(runtimeService.getVariable(waitState.getId(), "sum")).isEqualTo(10);

        runtimeService.trigger(waitState.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelSubProcessAllAutomatic.bpmn20.xml" })
    public void testParallelSubProcessAllAutomaticCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelSubprocessAllAutomatics", CollectionUtil.singletonMap("nrOfLoops", 10)).getId();

        for (int i = 0; i < 6; i++) {
            List<Execution> waitSubExecutions = runtimeService.createExecutionQuery().activityId("subProcessWait").list();
            assertThat(waitSubExecutions).isNotEmpty();
            runtimeService.trigger(waitSubExecutions.get(0).getId());
        }

        List<Execution> waitSubExecutions = runtimeService.createExecutionQuery().activityId("subProcessWait").list();
        assertThat(waitSubExecutions).isEmpty();

        Execution waitState = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(runtimeService.getVariable(procId, "sum")).isEqualTo(12);

        runtimeService.trigger(waitState.getId());
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testNestedParallelSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelSubProcess").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(8);

        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testNestedParallelSubProcessWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelSubProcess").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(12);

        for (int i = 0; i < 3; i++) {
            taskService.complete(tasks.get(i).getId());
        }

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testCallActivityLocalVariables.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testCallActivityLocalVariables() {
        Map<String, Object> variables = Collections.singletonMap("name", (Object) "test");
        String procId = runtimeService.startProcessInstanceByKey("miSubProcessLocalVariables", variables).getId();

        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("task one", "task two");

            Map<String, Object> taskVariables = Collections.singletonMap("output", (Object) ("run" + i + 1));
            taskService.complete(tasks.get(0).getId(), taskVariables);
            taskService.complete(tasks.get(1).getId());
            
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(procId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
            
            ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().processInstanceId(procId).activityId("task").singleResult();
            assertThat(runtimeService.getVariableLocal(execution.getId(), "output")).isEqualTo("run" + i + 1);
            assertThat(runtimeService.getVariable(procId, "output")).isNull();
            
            taskService.complete(task.getId());
        }

        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testCallActivityNormalVariables.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
        public void testCallActivityNormalVariables() {
        Map<String, Object> variables = Collections.singletonMap("name", (Object) "test");
        String procId = runtimeService.startProcessInstanceByKey("miSubProcessNormalVariables", variables).getId();
        
        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("task one", "task two");

            Map<String, Object> taskVariables = Collections.singletonMap("output", (Object) ("run" + i + 1));
            taskService.complete(tasks.get(0).getId(), taskVariables);
            taskService.complete(tasks.get(1).getId());
            
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(procId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
            
            ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().processInstanceId(procId).activityId("task").singleResult();
            assertThat(runtimeService.getVariableLocal(execution.getId(), "output")).isNull();
            assertThat(runtimeService.getVariable(procId, "output")).isEqualTo("run" + i + 1);
            
            taskService.complete(task.getId());
        }
        
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivity.bpmn20.xml",
    "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testSequentialCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialCallActivity").getId();

        for (int i = 0; i < 3; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("task one", "task two");
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
        }

        assertProcessEnded(procId);
    }

    @Test
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

        assertThat(task1).isNotNull();
        assertThat(task2).isNotNull();

        HashMap<String, Object> subVariables = new HashMap<>();
        subVariables.put("x", "y");

        taskService.complete(task1.getId(), subVariables);
        taskService.complete(task2.getId(), subVariables);

        org.flowable.task.api.Task task3 = taskService.createTaskQuery().processDefinitionKey("midProcess").singleResult();
        assertThat(task3).isNotNull();
        taskService.complete(task3.getId(), null);

        org.flowable.task.api.Task task4 = taskService.createTaskQuery().processDefinitionKey("parentProcess").singleResult();
        assertThat(task4).isNotNull();
        taskService.complete(task4.getId(), null);

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivityWithTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testSequentialCallActivityWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miSequentialCallActivityWithTimer").getId();

        // Complete first subprocess
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("task one", "task two");
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testParallelCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelCallActivity").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(12);
        for (int i = 0; i < tasks.size(); i++) {
            taskService.complete(tasks.get(i).getId());
        }

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testParallelCallActivityHistory() {
        runtimeService.startProcessInstanceByKey("miParallelCallActivity");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(12);
        for (int i = 0; i < tasks.size(); i++) {
            taskService.complete(tasks.get(i).getId());
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // Validate historic processes
            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
            assertThat(historicProcessInstances).hasSize(7); // 6 subprocesses
                                                              // + main process
            for (HistoricProcessInstance hpi : historicProcessInstances) {
                assertThat(hpi.getStartTime()).isNotNull();
                assertThat(hpi.getEndTime()).isNotNull();
            }

            // Validate historic tasks
            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().list();
            assertThat(historicTaskInstances).hasSize(12);
            for (HistoricTaskInstance hti : historicTaskInstances) {
                assertThat(hti.getStartTime()).isNotNull();
                assertThat(hti.getEndTime()).isNotNull();
                assertThat(hti.getAssignee()).isNotNull();
                assertThat(hti.getDeleteReason()).isNull();
            }

            // Validate historic activities
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("callActivity").list();
            assertThat(historicActivityInstances).hasSize(6);
            for (HistoricActivityInstance hai : historicActivityInstances) {
                assertThat(hai.getStartTime()).isNotNull();
                assertThat(hai.getEndTime()).isNotNull();
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivityWithTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testParallelCallActivityWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miParallelCallActivity").getId();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(6);
        for (int i = 0; i < 2; i++) {
            taskService.complete(tasks.get(i).getId());
        }

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedSequentialCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedSequentialCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialCallActivity").getId();

        for (int i = 0; i < 4; i++) {
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("task one", "task two");
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
        }

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedSequentialCallActivityWithTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedSequentialCallActivityWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedSequentialCallActivityWithTimer").getId();

        // first instance
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("task one", "task two");
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        // one task of second instance
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);
        taskService.complete(tasks.get(0).getId());

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedParallelCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivity").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(14);
        for (int i = 0; i < 14; i++) {
            taskService.complete(tasks.get(i).getId());
        }

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivityWithTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedParallelCallActivityWithTimer() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivityWithTimer").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(4);
        for (int i = 0; i < 3; i++) {
            taskService.complete(tasks.get(i).getId());
        }

        // Fire timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().singleResult();
        assertThat(taskAfterTimer.getTaskDefinitionKey()).isEqualTo("taskAfterTimer");
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivityCompletionCondition.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testNestedParallelCallActivityCompletionCondition() {
        String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivityCompletionCondition").getId();

        assertThat(taskService.createTaskQuery().count()).isEqualTo(8);

        for (int i = 0; i < 2; i++) {
            ProcessInstance nextSubProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("externalSubProcess").listPage(0, 1).get(0);
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(nextSubProcessInstance.getId()).list();
            assertThat(tasks).hasSize(2);
            for (org.flowable.task.api.Task task : tasks) {
                taskService.complete(task.getId());
            }
        }

        assertProcessEnded(procId);
    }

    // ACT-764
    @Test
    @Deployment
    public void testSequentialServiceTaskWithClass() {
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("multiInstanceServiceTask", CollectionUtil.singletonMap("result", 5));
        Integer result = (Integer) runtimeService.getVariable(procInst.getId(), "result");
        assertThat(result.intValue()).isEqualTo(160);

        Execution waitExecution = runtimeService.createExecutionQuery().processInstanceId(procInst.getId()).activityId("wait").singleResult();
        runtimeService.trigger(waitExecution.getId());
        assertProcessEnded(procInst.getId());
    }

    @Test
    @Deployment
    public void testSequentialServiceTaskWithClassAndCollection() {
        Collection<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6);
        Map<String, Object> vars = new HashMap<>();
        vars.put("result", 1);
        vars.put("items", items);

        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("multiInstanceServiceTask", vars);
        Integer result = (Integer) runtimeService.getVariable(procInst.getId(), "result");
        assertThat(result.intValue()).isEqualTo(720);

        Execution waitExecution = runtimeService.createExecutionQuery().processInstanceId(procInst.getId()).activityId("wait").singleResult();
        runtimeService.trigger(waitExecution.getId());
        assertProcessEnded(procInst.getId());
    }

    // ACT-901
    @Test
    @Deployment
    public void testAct901() {

        Date startTime = processEngineConfiguration.getClock().getCurrentTime();

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("multiInstanceSubProcess");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();

        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + 61000L)); // timer is set to one minute
        List<Job> timers = managementService.createTimerJobQuery().list();
        assertThat(timers).hasSize(5);

        // Execute all timers one by one (single thread vs thread pool of job
        // executor, which leads to optimisticlockingexceptions!)
        for (Job timer : timers) {
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
        }

        // All tasks should be canceled
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();
        assertThat(tasks).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.callActivityWithBoundaryErrorEvent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.throwingErrorEventSubProcess.bpmn20.xml" })
    public void testMultiInstanceCallActivityWithErrorBoundaryEvent() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("assignees", Arrays.asList("kermit", "gonzo"));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);

        // finish first call activity with error
        variableMap = new HashMap<>();
        variableMap.put("done", false);
        taskService.complete(tasks.get(0).getId(), variableMap);

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);

        taskService.complete(tasks.get(0).getId());

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").list();
        assertThat(processInstances).isEmpty();
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.callActivityWithBoundaryErrorEventSequential.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.throwingErrorEventSubProcess.bpmn20.xml" })
    public void testSequentialMultiInstanceCallActivityWithErrorBoundaryEvent() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("assignees", Arrays.asList("kermit", "gonzo"));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);

        // finish first call activity with error
        variableMap = new HashMap<>();
        variableMap.put("done", false);
        taskService.complete(tasks.get(0).getId(), variableMap);

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);

        taskService.complete(tasks.get(0).getId());

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").list();
        assertThat(processInstances).isEmpty();
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testMultiInstanceParallelReceiveTask() {
        runtimeService.startProcessInstanceByKey("multi-instance-receive");
        List<Execution> executions = runtimeService.createExecutionQuery().activityId("theReceiveTask").list();
        assertThat(executions).hasSize(4);

        // Complete all four of the executions
        for (Execution execution : executions) {
            runtimeService.trigger(execution.getId());
        }

        // There is one task after the task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testMultiInstanceParalelReceiveTaskWithTimer() {
        Date startTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startTime);

        runtimeService.startProcessInstanceByKey("multiInstanceReceiveWithTimer");
        List<Execution> executions = runtimeService.createExecutionQuery().activityId("theReceiveTask").list();
        assertThat(executions).hasSize(3);

        // Signal only one execution. Then the timer will fire
        runtimeService.trigger(executions.get(1).getId());
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + 60000L));
        waitForJobExecutorToProcessAllJobs(10000L, 1000L);

        // The process should now be in the task after the timer
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task after timer");

        // Completing it should end the process
        taskService.complete(task.getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testMultiInstanceSequentialReceiveTask() {
        runtimeService.startProcessInstanceByKey("multi-instance-receive");
        Execution execution = runtimeService.createExecutionQuery().activityId("theReceiveTask").singleResult();
        assertThat(execution).isNotNull();

        // Complete all four of the executions
        while (execution != null) {
            runtimeService.trigger(execution.getId());
            execution = runtimeService.createExecutionQuery().activityId("theReceiveTask").singleResult();
        }

        // There is one task after the task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedMultiInstanceTasks.bpmn20.xml" })
    public void testNestedMultiInstanceTasks() {
        List<String> processes = Arrays.asList("process A", "process B");
        List<String> assignees = Arrays.asList("kermit", "gonzo");
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("subProcesses", processes);
        variableMap.put("assignees", assignees);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miNestedMultiInstanceTasks", variableMap);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(processes.size() * assignees.size());

        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId(), Collections.singletonMap("assignee", "1"));
        }

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("miNestedMultiInstanceTasks").list();
        assertThat(processInstances).isEmpty();
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialSubprocessEmptyCollection.bpmn20.xml" })
    public void testSequentialSubprocessEmptyCollection() {
        Collection<String> collection = Collections.emptyList();
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSequentialSubProcessEmptyCollection", variableMap);
        assertThat(processInstance).isNotNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNull();
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialEmptyCollection.bpmn20.xml" })
    public void testSequentialEmptyCollection() {
        Collection<String> collection = Collections.emptyList();
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSequentialEmptyCollection", variableMap);
        assertThat(processInstance).isNotNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNull();
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialEmptyCollection.bpmn20.xml" })
    public void testSequentialEmptyCollectionWithNonEmptyCollection() {
        Collection<String> collection = Collections.singleton("Test");
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSequentialEmptyCollection", variableMap);
        assertThat(processInstance).isNotNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelEmptyCollection.bpmn20.xml" })
    public void testParalellEmptyCollection() throws Exception {
        Collection<String> collection = Collections.emptyList();
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testParalellEmptyCollection", variableMap);
        assertThat(processInstance).isNotNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNull();
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelEmptyCollection.bpmn20.xml" })
    public void testParalellEmptyCollectionWithNonEmptyCollection() {
        Collection<String> collection = Collections.singleton("Test");
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("collection", collection);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testParalellEmptyCollection", variableMap);
        assertThat(processInstance).isNotNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
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
            assertThat(processInstance).isNotNull();

        } finally {

            // Put beans back
            processEngineConfiguration.getExpressionManager().setBeans(originalBeans);

        }
    }

    @Test
    @Deployment
    public void testEmptyCollectionOnParallelUserTask() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("messages", Collections.EMPTY_LIST);
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelUserTaskMi", vars);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).finished().count()).isEqualTo(1L);
        }
    }

    @Test
    @Deployment
    public void testZeroLoopCardinalityOnParallelUserTask() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelUserTaskMi");
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).finished().count()).isEqualTo(1L);
        }
    }

    @Test
    @Deployment
    public void testEmptyCollectionOnSequentialEmbeddedSubprocess() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("messages", Collections.EMPTY_LIST);
            runtimeService.startProcessInstanceByKey("sequentialMiSubprocess", vars);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(1L);
        }
    }

    @Test
    @Deployment
    public void testEmptyCollectionOnParallelEmbeddedSubprocess() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("messages", Collections.EMPTY_LIST);
            runtimeService.startProcessInstanceByKey("parallelMiSubprocess", vars);
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(1L);
        }
    }

    @Test
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

        assertThat(TestStartExecutionListener.countWithLoopCounter.get()).isEqualTo(3);
        assertThat(TestEndExecutionListener.countWithLoopCounter.get()).isEqualTo(3);

        assertThat(TestStartExecutionListener.countWithoutLoopCounter.get()).isEqualTo(1);
        assertThat(TestEndExecutionListener.countWithoutLoopCounter.get()).isEqualTo(1);
    }
    
    @Deployment(resources = {
        "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testExecutionListenersOnMultiInstanceSubprocess.bpmn20.xml" })
    @Test
    public void testDispatchFlowableMultiInstanceActivityCompletedEventLoopVariables() {
        MultiInstanceUserActivityEventListener testListener = new MultiInstanceUserActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(testListener);
        
        try {
            Map<String, Object> variableMap = new HashMap<>();
            List<String> assignees = new ArrayList<>();
            assignees.add("john");
            assignees.add("jane");
            assignees.add("matt");
            variableMap.put("assignees", assignees);
            runtimeService.startProcessInstanceByKey("MultiInstanceTest", variableMap);
            
            List<FlowableMultiInstanceActivityCompletedEvent> multiInstanceEvents = testListener.getEventsReceived().stream()
                    .filter(event -> event instanceof FlowableMultiInstanceActivityCompletedEvent)
                    .map(event -> (FlowableMultiInstanceActivityCompletedEvent) event).collect(Collectors.toList());
            assertThat(multiInstanceEvents.size()).isEqualTo(1);
            
            Integer numberOfActiveInstances = multiInstanceEvents.stream()
                    .map(FlowableMultiInstanceActivityCompletedEvent::getNumberOfActiveInstances).findFirst().get();
            Integer numberOfCompletedInstances = multiInstanceEvents.stream()
                    .map(FlowableMultiInstanceActivityCompletedEvent::getNumberOfCompletedInstances).findFirst().get();
            Integer numberOfInstances = multiInstanceEvents.stream().map(FlowableMultiInstanceActivityCompletedEvent::getNumberOfInstances)
                    .findFirst().get();
            
            assertThat(numberOfActiveInstances).isEqualTo(0);
            assertThat(numberOfCompletedInstances).isEqualTo(3);
            assertThat(numberOfInstances).isEqualTo(3);
            
        } finally {
            testListener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(testListener);
        }
    }

    @Test
    @Deployment
    public void testExecutionListenersOnMultiInstanceUserTask() {
        resetTestCounts();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExecutionListenersOnMultiInstanceUserTask");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertThat(TestTaskCompletionListener.count.get()).isEqualTo(4);

        assertThat(TestStartExecutionListener.countWithLoopCounter.get()).isEqualTo(4);
        assertThat(TestEndExecutionListener.countWithLoopCounter.get()).isEqualTo(4);

        assertThat(TestStartExecutionListener.countWithoutLoopCounter.get()).isEqualTo(1);
        assertThat(TestEndExecutionListener.countWithoutLoopCounter.get()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testParallelAfterSequentialMultiInstance() {

        // Used to throw a nullpointer exception

        runtimeService.startProcessInstanceByKey("multiInstance");
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testEndTimeOnMiSubprocess() {

        if (!processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            return;
        }

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceSubProcessParallelTasks");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("User Task 1", "User Task 1");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // End time should not be set for the subprocess
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
        assertThat(historicActivityInstances).hasSize(2);
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());
            assertThat(historicActivityInstance.getStartTime()).isNotNull();
            assertThat(historicActivityInstance.getEndTime()).isNull();
        }

        // Complete one of the user tasks. This should not trigger setting of end time of the subprocess, but due to a bug it did exactly that
        taskService.complete(tasks.get(0).getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
        assertThat(historicActivityInstances).hasSize(2);
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());
            assertThat(historicActivityInstance.getEndTime()).isNull();
        }

        taskService.complete(tasks.get(1).getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
        assertThat(historicActivityInstances).hasSize(2);
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());
            assertThat(historicActivityInstance.getEndTime()).isNull();
        }

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("User Task 3").list();
        assertThat(tasks).hasSize(2);
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            
            historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
            assertThat(historicActivityInstances).hasSize(2);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());
                assertThat(historicActivityInstance.getEndTime()).isNull();
            }
        }

        // Finishing the tasks should also set the end time
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        historicActivityInstances = historyService.createHistoricActivityInstanceQuery().activityId("subprocess1").list();
        assertThat(historicActivityInstances).hasSize(2);
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            assertThat(historicActivityInstance.getEndTime()).isNotNull();
        }
    }

    @Test
    @Deployment
    public void testChangingCollection() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("multi_users", Collections.singletonList("testuser"));
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("test_multi", vars);
        assertThat(instance).isNotNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("multi");
        vars.put("multi_users", new ArrayList<String>()); // <-- Problem here.
        taskService.complete(task.getId(), vars);
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().list();
        assertThat(instances).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.simpleMultiInstanceWithCollectionVariable.bpmn20.xml")
    public void testCollectionVariableMissing() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("simple_multi"))
                .as("Should have failed with missing collection variable")
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Variable 'elements' was not found");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.simpleMultiInstanceWithCollectionVariable.bpmn20.xml")
    public void testCollectionVariableIsNotACollection() {
        Map<String, Object> vars = new HashMap<>();
        ValueBean valueBean = new ValueBean("test");
        vars.put("elements", valueBean);
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("simple_multi", vars))
                .as("Should have failed with collection variable not a collection")
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Variable 'elements':" + valueBean + " is not a Collection");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testZeroLoopCardinalityOnParallelUserTaskWithEventSubscription.bpmn20.xml" })
    public void testZeroLoopCardinalityOnParallelUserTaskWithEventSubscription() {
        String procId = runtimeService.startProcessInstanceByKey("parallelUserTaskMi_withEventSubscription").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testZeroLoopCardinalityOnParallelSubprocessWithEventSubscription.bpmn20.xml" })
    public void testZeroLoopCardinalityOnParallelSubprocessWithEventSubscription() {
        String procId = runtimeService.startProcessInstanceByKey("parallelSubprocessMi_withEventSubscription").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testLoopCounterVariableForSequentialMultiInstance() {
        List<String> myCollection = Arrays.asList("a", "b", "c", "d");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .variable("myCollection", myCollection)
            .processDefinitionKey("loopCounterTest")
            .start();

        for (int i = 0; i < myCollection.size(); i++) {
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

            Integer loopCounter = (Integer) runtimeService.getVariableLocal(task.getExecutionId(), "loopCounter");
            assertThat(loopCounter).isEqualTo(i);

            String myElement = (String) runtimeService.getVariableLocal(task.getExecutionId(), "myElement");
            assertThat(myElement).isEqualTo(myCollection.get(i));

            taskService.complete(task.getId());
        }
    }

    @Test
    @Deployment
    public void testLoopCounterVariableForParallelMultiInstance() {
        List<String> myCollection = Arrays.asList("a", "b", "c", "d");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .variable("myCollection", myCollection)
            .processDefinitionKey("loopCounterTest")
            .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(4);

        List<Integer> loopCounters = new ArrayList<>();
        for (Task task : tasks) {
            loopCounters.add((Integer) runtimeService.getVariableLocal(task.getExecutionId(), "loopCounter"));

            String myElement = (String) runtimeService.getVariableLocal(task.getExecutionId(), "myElement");
            assertThat(myElement).isNotNull();
        }

        assertThat(loopCounters).containsOnly(0, 1 , 2, 3);
    }

    @Test
    @Deployment(resources = {
        "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testMapLoopCounterIntoAsyncParallelMultiInstanceCallActivity.bpmn20.xml"
    })
    public void testMapLoopCounterIntoAsyncParallelMultiInstanceCallActivity() {
        List<String> myCollection = Arrays.asList("a", "b", "c", "d");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .variable("myCollection", myCollection)
            .processDefinitionKey("loopCounterTest")
            .start();

        List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSameSizeAs(myCollection);
        for (Job job : jobs) {
            managementService.executeJob(job.getId());
        }

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(processInstances).hasSameSizeAs(myCollection);

        List<Integer> loopCounters = new ArrayList<>();
        for (ProcessInstance instance : processInstances) {
            loopCounters.add(((Integer) runtimeService.getVariable(instance.getId(), "copiedLoopCounter")));
        }
        assertThat(loopCounters).containsOnly(0, 1 , 2, 3);
    }

    @Test
    @Deployment(resources = {
        "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testMapLoopCounterIntoAsyncSequentialMultiInstanceCallActivity.bpmn20.xml"
    })
    public void testMapLoopCounterIntoAsyncSequentialMultiInstanceCallActivity() {
        List<String> myCollection = Arrays.asList("a", "b", "c", "d");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .variable("myCollection", myCollection)
            .processDefinitionKey("loopCounterTest")
            .start();

        for (int i = 0; i < myCollection.size(); i++) {
            Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
            managementService.executeJob(job.getId());

            ProcessInstance calledProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();

            Integer copiedLoopCounter = (Integer) runtimeService.getVariable(calledProcessInstance.getId(), "copiedLoopCounter");
            assertThat(i).isEqualTo(copiedLoopCounter);

            Task task = taskService.createTaskQuery().processInstanceId(calledProcessInstance.getId()).singleResult();
            taskService.complete(task.getId());
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
    
    class MultiInstanceUserActivityEventListener extends AbstractFlowableEngineEventListener {

        private List<FlowableEvent> eventsReceived;

        public MultiInstanceUserActivityEventListener() {
            super(new HashSet<>(Arrays.asList(
                    FlowableEngineEventType.ACTIVITY_STARTED,
                    FlowableEngineEventType.ACTIVITY_COMPLETED,
                    FlowableEngineEventType.ACTIVITY_CANCELLED,
                    FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED,
                    FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED,
                    FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED_WITH_CONDITION,
                    FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_CANCELLED,
                    FlowableEngineEventType.TASK_CREATED,
                    FlowableEngineEventType.TASK_COMPLETED,
                    FlowableEngineEventType.PROCESS_STARTED,
                    FlowableEngineEventType.PROCESS_COMPLETED,
                    FlowableEngineEventType.PROCESS_CANCELLED,
                    FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT
            )));
            eventsReceived = new ArrayList<>();
        }

        public List<FlowableEvent> getEventsReceived() {
            return eventsReceived;
        }

        public void clearEventsReceived() {
            eventsReceived.clear();
        }

        @Override
        protected void activityStarted(FlowableActivityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void activityCompleted(FlowableActivityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void activityCancelled(FlowableActivityCancelledEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void taskCreated(FlowableEngineEntityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void taskCompleted(FlowableEngineEntityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void processStarted(FlowableProcessStartedEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void processCompleted(FlowableEngineEntityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void processCompletedWithTerminateEnd(FlowableEngineEntityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void processCancelled(FlowableCancelledEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void multiInstanceActivityStarted(FlowableMultiInstanceActivityEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void multiInstanceActivityCompleted(FlowableMultiInstanceActivityCompletedEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void multiInstanceActivityCompletedWithCondition(FlowableMultiInstanceActivityCompletedEvent event) {
            eventsReceived.add(event);
        }

        @Override
        protected void multiInstanceActivityCancelled(FlowableMultiInstanceActivityCancelledEvent event) {
            eventsReceived.add(event);
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }

}
