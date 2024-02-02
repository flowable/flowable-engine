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

package org.flowable.engine.test.bpmn.usertask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.interceptor.CreateUserTaskAfterContext;
import org.flowable.engine.interceptor.CreateUserTaskBeforeContext;
import org.flowable.engine.interceptor.CreateUserTaskInterceptor;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class UserTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testTaskPropertiesNotNull() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getId()).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");
        assertThat(task.getDescription()).isEqualTo("Very important");
        assertThat(task.getPriority()).isGreaterThan(0);
        assertThat(task.getAssignee()).isEqualTo("kermit");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(task.getProcessDefinitionId()).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isNotNull();
        assertThat(task.getCreateTime()).isNotNull();
        
        // the next test verifies that if an execution creates a task, that no events are created during creation of the task.
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(taskService.getTaskEvents(task.getId())).isEmpty();
        }
    }

    @Test
    @Deployment
    public void testEntityLinkCreated() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getId()).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");
        assertThat(task.getDescription()).isEqualTo("Very important");
        assertThat(task.getPriority()).isGreaterThan(0);
        assertThat(task.getAssignee()).isEqualTo("kermit");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(task.getProcessDefinitionId()).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isNotNull();
        assertThat(task.getCreateTime()).isNotNull();

        CommandExecutor commandExecutor = processEngine.getProcessEngineConfiguration().getCommandExecutor();

        List<EntityLink> entityLinksByScopeIdAndType = commandExecutor.execute(commandContext -> {
            EntityLinkService entityLinkService = processEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService();

            return entityLinkService.findEntityLinksByScopeIdAndType(processInstance.getId(), ScopeTypes.BPMN, EntityLinkType.CHILD);
        });

        assertThat(entityLinksByScopeIdAndType).hasSize(1);
        assertThat(entityLinksByScopeIdAndType.get(0).getHierarchyType()).isEqualTo(HierarchyType.ROOT);
    }

    @Test
    @Deployment
    public void testQuerySortingWithParameter() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
    }

    @Test
    @Deployment
    public void testCompleteAfterParallelGateway() throws InterruptedException {
        // related to https://activiti.atlassian.net/browse/ACT-1054

        // start the process
        runtimeService.startProcessInstanceByKey("ForkProcess");
        List<org.flowable.task.api.Task> taskList = taskService.createTaskQuery().list();
        assertThat(taskList).hasSize(2);

        // make sure user task exists
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("SimpleUser").singleResult();
        assertThat(task).isNotNull();

        // attempt to complete the task and get PersistenceException pointing to
        // "referential integrity constraint violation"
        taskService.complete(task.getId());
    }

    @Test
    @Deployment
    public void testTaskCategory() {
        runtimeService.startProcessInstanceByKey("testTaskCategory");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        // Test if the property set in the model is shown in the task
        String testCategory = "My Category";
        assertThat(task.getCategory()).isEqualTo(testCategory);

        // Test if can be queried by query API
        assertThat(taskService.createTaskQuery().taskCategory(testCategory).singleResult().getName()).isEqualTo("Task with category");
        assertThat(taskService.createTaskQuery().taskCategory("Does not exist").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // Check historic task
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getCategory()).isEqualTo(testCategory);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskCategory(testCategory).singleResult().getName()).isEqualTo("Task with category");
            assertThat(historyService.createHistoricTaskInstanceQuery().taskCategory("Does not exist").count()).isZero();
        }

        // Update category
        String newCategory = "New Test Category";
        task.setCategory(newCategory);
        taskService.saveTask(task);

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getCategory()).isEqualTo(newCategory);
        assertThat(taskService.createTaskQuery().taskCategory(newCategory).singleResult().getName()).isEqualTo("Task with category");
        assertThat(taskService.createTaskQuery().taskCategory(testCategory).count()).isZero();

        // Complete task and verify history
        taskService.complete(task.getId());
            
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getCategory()).isEqualTo(newCategory);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskCategory(newCategory).singleResult().getName()).isEqualTo("Task with category");
            assertThat(historyService.createHistoricTaskInstanceQuery().taskCategory(testCategory).count()).isZero();
        }
    }

    // See https://activiti.atlassian.net/browse/ACT-4041
    @Test
    public void testTaskFormKeyWhenUsingIncludeVariables() {
        deployOneTaskTestProcess();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // Set variables
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        Map<String, Object> vars = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            vars.put("var" + i, i * 2);
        }
        taskService.setVariables(task.getId(), vars);

        // Set form key
        task = taskService.createTaskQuery().singleResult();
        task.setFormKey("test123");
        taskService.saveTask(task);

        // Verify query and check form key
        task = taskService.createTaskQuery().includeProcessVariables().singleResult();
        assertThat(task.getProcessVariables()).hasSameSizeAs(vars);

        assertThat(task.getFormKey()).isEqualTo("test123");
    }
    
    @Test
    @Deployment
    public void testEmptyAssignmentExpression() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("assignee", null);
        variableMap.put("candidateUsers", null);
        variableMap.put("candidateGroups", null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variableMap);
        assertThat(processInstance).isNotNull();
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getAssignee()).isNull();
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        assertThat(identityLinks).isEmpty();
        
        variableMap = new HashMap<>();
        variableMap.put("assignee", "");
        variableMap.put("candidateUsers", "");
        variableMap.put("candidateGroups", "");
        processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variableMap);
        assertThat(processInstance).isNotNull();
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getAssignee()).isNull();
        identityLinks = taskService.getIdentityLinksForTask(task.getId());
        assertThat(identityLinks).isEmpty();
    }
    
    @Test
    @Deployment
    public void testNonStringProperties() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("taskName", 1);
        vars.put("taskDescription", 2);
        vars.put("taskCategory", 3);
        vars.put("taskFormKey", 4);
        vars.put("taskAssignee", 5);
        vars.put("taskOwner", 6);
        vars.put("taskCandidateGroups", 7);
        vars.put("taskCandidateUsers", 8);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonStringProperties", vars);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("1");
        assertThat(task.getDescription()).isEqualTo("2");
        assertThat(task.getCategory()).isEqualTo("3");
        assertThat(task.getFormKey()).isEqualTo("4");
        assertThat(task.getAssignee()).isEqualTo("5");
        assertThat(task.getOwner()).isEqualTo("6");
        
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        assertThat(identityLinks).hasSize(4);
        int candidateIdentityLinkCount = 0;
        for (IdentityLink identityLink : identityLinks) {
            if (identityLink.getType().equals(IdentityLinkType.CANDIDATE)) {
                candidateIdentityLinkCount++;
                if (identityLink.getGroupId() != null) {
                    assertThat(identityLink.getGroupId()).isEqualTo("7");
                } else {
                    assertThat(identityLink.getUserId()).isEqualTo("8");
                }
            }
        }
        assertThat(candidateIdentityLinkCount).isEqualTo(2);
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/usertask/UserTaskTest.testTaskPropertiesNotNull.bpmn20.xml")
    public void testCreateUserTaskInterceptor() throws Exception {
        TestCreateUserTaskInterceptor testCreateUserTaskInterceptor = new TestCreateUserTaskInterceptor();
        processEngineConfiguration.setCreateUserTaskInterceptor(testCreateUserTaskInterceptor);
        
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getId()).isNotNull();
            assertThat(task.getName()).isEqualTo("my task");
            assertThat(task.getDescription()).isEqualTo("Very important");
            assertThat(task.getCategory()).isEqualTo("testCategory");
            
            assertThat(testCreateUserTaskInterceptor.getBeforeCreateUserTaskCounter()).isEqualTo(1);
            assertThat(testCreateUserTaskInterceptor.getAfterCreateUserTaskCounter()).isEqualTo(1);
            
        } finally {
            processEngineConfiguration.setCreateUserTaskInterceptor(null);
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/usertask/UserTaskTest.userTaskIdVariableName.bpmn20.xml")
    public void testUserTaskIdVariableName() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskIdVariableName");

        // Normal string
        Task firstTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("task1").singleResult();
        assertThat(firstTask).isNotNull();

        String actualTaskId = firstTask.getId();
        String myTaskId = (String)runtimeService.getVariable(processInstance.getId(), "myTaskId");
        assertThat(myTaskId).isEqualTo(actualTaskId);

        // Expression
        Task secondTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("task2").singleResult();
        assertThat(secondTask).isNotNull();

        actualTaskId = secondTask.getId();
        String myExpressionTaskId = (String)runtimeService.getVariable(processInstance.getId(), "myExpressionTaskId");
        assertThat(myExpressionTaskId).isEqualTo(actualTaskId);
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/usertask/UserTaskTest.userTaskCompleterVariableName.bpmn20.xml")
    public void testUserTaskCompleterVariableName() throws Exception {
        Authentication.setAuthenticatedUserId("JohnDoe");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskCompleterVariableName");

        // Normal string
        Task firstTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("task1").singleResult();
        assertThat(firstTask).isNotNull();
        taskService.complete(firstTask.getId());
        String completerTask1 = runtimeService.getVariable(processInstance.getProcessInstanceId(), "completerTask1", String.class);
        assertThat(completerTask1).isEqualTo("JohnDoe");

        // Expression
        Task secondTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("task2").singleResult();
        assertThat(secondTask).isNotNull();
        taskService.complete(secondTask.getId());
        String completerTask2 = runtimeService.getVariable(processInstance.getProcessInstanceId(), "completerTask2", String.class);
        assertThat(completerTask2).isEqualTo("JohnDoe");

        // No user
        Authentication.setAuthenticatedUserId(null);
        Task thirdTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("task3").singleResult();
        assertThat(thirdTask).isNotNull();
        taskService.complete(thirdTask.getId());
        String completerTask3 = runtimeService.getVariable(processInstance.getProcessInstanceId(), "completerTask3", String.class);
        assertThat(completerTask3).isEqualTo(null);
    }

    /**
     * Test for Issue <a href="https://github.com/flowable/flowable-engine/issues/3467">#3467</a>
     */
    @Deployment
    @Test
    void testIdentityLinkNotDuplicate() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("employee", "John Doe");
        variables.put("nrOfHolidays", 10);
        variables.put("description", "I want to go to Hawaii");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holidayRequest", variables);
        Task task = taskService.createTaskQuery().taskCandidateGroup("managers").singleResult();
        List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(task.getId());

        assertThat(identityLinksForTask).singleElement().satisfies(e -> assertThat(e.getType()).isEqualTo("candidate"));
        // Identity link variables are added in TestListener.java. Only one expected
        assertThat(processInstance.getProcessVariables()).describedAs("Expected only one identity link")
                .containsOnlyKeys("identityLinks_0", "employee", "nrOfHolidays", "description");
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testFillTaskLifecycleValues() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getId()).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");
        assertThat(task.getState()).isEqualTo(Task.CREATED);
        assertThat(task.getInProgressStartTime()).isNull();
        assertThat(task.getInProgressStartedBy()).isNull();
        assertThat(task.getClaimTime()).isNull();
        assertThat(task.getClaimedBy()).isNull();
        assertThat(task.getSuspendedTime()).isNull();
        assertThat(task.getSuspendedBy()).isNull();
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getState()).isEqualTo(Task.CREATED);
            assertThat(historicTaskInstance.getCreateTime()).isNotNull();
        }
        
        taskService.claim(task.getId(), "kermit");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getState()).isEqualTo(Task.CLAIMED);
        assertThat(task.getClaimTime()).isNotNull();
        assertThat(task.getClaimedBy()).isEqualTo("kermit");
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getState()).isEqualTo(Task.CLAIMED);
            assertThat(historicTaskInstance.getClaimTime()).isNotNull();
            assertThat(historicTaskInstance.getClaimedBy()).isEqualTo("kermit");
        }
        
        taskService.startProgress(task.getId(), "fozzie");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getState()).isEqualTo(Task.IN_PROGRESS);
        assertThat(task.getInProgressStartTime()).isNotNull();
        assertThat(task.getInProgressStartedBy()).isEqualTo("fozzie");
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getState()).isEqualTo(Task.IN_PROGRESS);
            assertThat(historicTaskInstance.getInProgressStartTime()).isNotNull();
            assertThat(historicTaskInstance.getInProgressStartedBy()).isEqualTo("fozzie");
        }
        
        taskService.suspendTask(task.getId(), "gonzo");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getState()).isEqualTo(Task.SUSPENDED);
        assertThat(task.getSuspendedTime()).isNotNull();
        assertThat(task.getSuspendedBy()).isEqualTo("gonzo");
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getState()).isEqualTo(Task.SUSPENDED);
            assertThat(historicTaskInstance.getSuspendedTime()).isNotNull();
            assertThat(historicTaskInstance.getSuspendedBy()).isEqualTo("gonzo");
        }
        
        taskService.activateTask(task.getId(), "kermit");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getState()).isEqualTo(Task.IN_PROGRESS);
        assertThat(task.getSuspendedTime()).isNull();
        assertThat(task.getSuspendedBy()).isNull();
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getState()).isEqualTo(Task.IN_PROGRESS);
            assertThat(historicTaskInstance.getClaimTime()).isNotNull();
            assertThat(historicTaskInstance.getClaimedBy()).isEqualTo("kermit");
            assertThat(historicTaskInstance.getInProgressStartTime()).isNotNull();
            assertThat(historicTaskInstance.getInProgressStartedBy()).isEqualTo("fozzie");
        }
        
        taskService.complete(task.getId(), "kermit");
        assertProcessEnded(processInstance.getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getState()).isEqualTo(Task.COMPLETED);
            assertThat(historicTaskInstance.getEndTime()).isNotNull();
            assertThat(historicTaskInstance.getCompletedBy()).isEqualTo("kermit");
        }
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testTaskWithTimerJob() {
        Map<String, JobHandler> existingJobHandlers = processEngineConfiguration.getJobHandlers();
        Map<String, JobHandler> updatedJobHandlers = new HashMap<>(existingJobHandlers);
        TestBpmnTaskTimerJobHandler testTimerJobHandler = new TestBpmnTaskTimerJobHandler();
        updatedJobHandlers.put(testTimerJobHandler.getType(), testTimerJobHandler);
        processEngineConfiguration.setJobHandlers(updatedJobHandlers);
        processEngineConfiguration.getJobServiceConfiguration().setJobHandlers(updatedJobHandlers);
        
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    
            final org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getId()).isNotNull();
            assertThat(task.getName()).isEqualTo("my task");
            assertThat(task.getPriority()).isEqualTo(50);
            
            managementService.executeCommand(new Command<Void>() {
                
                @Override
                public Void execute(CommandContext commandContext) {
                    TimerJobService timerJobService = processEngineConfiguration.getJobServiceConfiguration().getTimerJobService();
                    TimerJobEntity timerJob = timerJobService.createTimerJob();
                    timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
                    timerJob.setJobHandlerType(testTimerJobHandler.getType());
                    timerJob.setProcessInstanceId(processInstance.getId());
                    timerJob.setSubScopeId(task.getId());
                    
                    Calendar calendar = processEngineConfiguration.getClock().getCurrentCalendar();
                    calendar.add(Calendar.MINUTE, -60);
                    timerJob.setDuedate(calendar.getTime());
                    
                    timerJobService.scheduleTimerJob(timerJob);
                    
                    return null;
                }
            });
            
            JobTestHelper.waitForJobExecutorToProcessAllJobsAndTimerJobs(processEngineConfiguration, managementService, 5000, 200);
            
            Task updatedTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(updatedTask.getPriority()).isEqualTo(100);
            
        } finally {
            processEngineConfiguration.setJobHandlers(existingJobHandlers);
            processEngineConfiguration.getJobServiceConfiguration().setJobHandlers(existingJobHandlers);
        }
    }

    @Test
    @Deployment
    public void testTaskNameWithExpression() {
        ProcessInstance processWithoutVariable = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        ProcessInstance processWithVariable = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("testVar", "Task name")
                .start();

        Task task = taskService.createTaskQuery().processInstanceId(processWithoutVariable.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("${testVar}");

        task = taskService.createTaskQuery().processInstanceId(processWithVariable.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Task name");
    }

    protected class TestCreateUserTaskInterceptor implements CreateUserTaskInterceptor {
        
        protected int beforeCreateUserTaskCounter = 0;
        protected int afterCreateUserTaskCounter = 0;
        
        @Override
        public void beforeCreateUserTask(CreateUserTaskBeforeContext context) {
            beforeCreateUserTaskCounter++;
            context.setCategory("testCategory");
        }

        @Override
        public void afterCreateUserTask(CreateUserTaskAfterContext context) {
            afterCreateUserTaskCounter++;
        }

        public int getBeforeCreateUserTaskCounter() {
            return beforeCreateUserTaskCounter;
        }

        public int getAfterCreateUserTaskCounter() {
            return afterCreateUserTaskCounter;
        }
    }
}
