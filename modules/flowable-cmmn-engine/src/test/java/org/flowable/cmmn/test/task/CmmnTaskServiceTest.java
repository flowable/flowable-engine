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
package org.flowable.cmmn.test.task;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.engine.common.api.scope.ScopeTypes;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.TaskServiceConfiguration;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * @author Joram Barrez
 */
public class CmmnTaskServiceTest extends FlowableCmmnTestCase {
    private Optional<Task> task = Optional.empty();

    @After
    public void tearDown() {
        task.ifPresent(taskInstance -> this.cmmnTaskService.deleteTask(taskInstance.getId(), true));
    }

    public void testCreateTaskWithBuilder() {
        task = Optional.of(
                this.cmmnTaskService.createTaskBuilder().
                        name("testName").
                        description("testDescription").
                        priority(35).
                        owner("testOwner").
                        assignee("testAssignee").
                        dueDate(new Date(0)).
                        category("testCategory").
                        parentTaskId("testParentTaskId").
                        tenantId("testTenantId").
                        formKey("testFormKey").
                        taskDefinitionId("testDefintionId").
                        taskDefinitionKey("testDefinitionKey").
                        create()
        );
        Task updatedTask = cmmnTaskService.createTaskQuery().taskId(task.map(TaskInfo::getId).orElse("NON-EXISTING-TASK")).singleResult();
        assertThat(updatedTask, notNullValue());
        assertThat(updatedTask.getName(), is("testName"));
        assertThat(updatedTask.getDescription(), is("testDescription"));
        assertThat(updatedTask.getPriority(), is(35));
        assertThat(updatedTask.getOwner(), is("testOwner"));
        assertThat(updatedTask.getAssignee(), is("testAssignee"));
        assertThat(updatedTask.getDueDate(), is(new Date(0)));
        assertThat(updatedTask.getCategory(), is("testCategory"));
        assertThat(updatedTask.getParentTaskId(), is("testParentTaskId"));
        assertThat(updatedTask.getTenantId(), is("testTenantId"));
        assertThat(updatedTask.getFormKey(), is("testFormKey"));
        assertThat(updatedTask.getTaskDefinitionId(), is("testDefintionId"));
        assertThat(updatedTask.getTaskDefinitionKey(), is("testDefinitionKey"));
    }

    public void testCreateTaskWithBuilderAndPostprocessor() {
        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) this.cmmnEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        UnaryOperator<TaskInfo> previousTaskBuilderPostProcessor = taskServiceConfiguration.getTaskBuilderPostProcessor();
        taskServiceConfiguration.setTaskBuilderPostProcessor(TestTaskInfoWrapper::new);
        task = Optional.of(
                cmmnTaskService.createTaskBuilder().
                        name("testName").
                        create()
        );
        assertTrue(task.isPresent());
        Task updatedTask = cmmnTaskService.createTaskQuery().taskId(task.get().getId()).includeIdentityLinks().singleResult();
        assertThat(updatedTask, notNullValue());
        assertThat(updatedTask.getName(), is("testName"));
        assertThat(updatedTask.getIdentityLinks().size(), is(2));

        cmmnTaskService.deleteUserIdentityLink(task.get().getId(), "testUser", IdentityLinkType.CANDIDATE);
        cmmnTaskService.deleteGroupIdentityLink(task.get().getId(), "testGroup", IdentityLinkType.CANDIDATE);
        taskServiceConfiguration.setTaskBuilderPostProcessor(previousTaskBuilderPostProcessor);
    }

    @Test
    @CmmnDeployment
    public void testOneHumanTaskCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("The Task", task.getName());
        assertEquals("This is a test documentation", task.getDescription());
        assertEquals("johnDoe", task.getAssignee());
        
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertNull(historicTaskInstance.getEndTime());
        }
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
        
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertEquals("The Task", historicTaskInstance.getName());
            assertEquals("This is a test documentation", historicTaskInstance.getDescription());
            assertNotNull(historicTaskInstance.getEndTime());
        }
    }
    
    @Test
    @CmmnDeployment
    public void testOneHumanTaskExpressionCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneHumanTaskCase")
                        .variable("var1", "A")
                        .variable("var2", "YES")
                        .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("The Task A", task.getName());
        assertEquals("This is a test YES", task.getDescription());
        assertEquals("johnDoe", task.getAssignee());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
        
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertEquals("The Task A", historicTaskInstance.getName());
            assertEquals("This is a test YES", historicTaskInstance.getDescription());
            assertNotNull(historicTaskInstance.getEndTime());
        }
    }
    
    @Test
    @CmmnDeployment
    public void testTriggerOneHumanTaskCaseProgrammatically() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().singleResult();
        assertEquals(planItemInstance.getId(), task.getSubScopeId());
        assertEquals(planItemInstance.getCaseInstanceId(), task.getScopeId());
        assertEquals(planItemInstance.getCaseDefinitionId(), task.getScopeDefinitionId());
        assertEquals(ScopeTypes.CMMN, task.getScopeType());
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(0, cmmnTaskService.createTaskQuery().count());
        assertCaseInstanceEnded(caseInstance);
    }

    private class TestTaskInfoWrapper implements TaskInfo {
        private TaskInfo taskInfo;

        public TestTaskInfoWrapper(TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
        }

        @Override
        public String getId() {
            return taskInfo.getId();
        }

        @Override
        public String getName() {
            return taskInfo.getName();
        }

        @Override
        public String getDescription() {
            return taskInfo.getDescription();
        }

        @Override
        public int getPriority() {
            return taskInfo.getPriority();
        }

        @Override
        public String getOwner() {
            return taskInfo.getOwner();
        }

        @Override
        public String getAssignee() {
            return taskInfo.getAssignee();
        }

        @Override
        public String getProcessInstanceId() {
            return taskInfo.getProcessInstanceId();
        }

        @Override
        public String getExecutionId() {
            return taskInfo.getExecutionId();
        }

        @Override
        public String getTaskDefinitionId() {
            return taskInfo.getTaskDefinitionId();
        }

        @Override
        public String getProcessDefinitionId() {
            return taskInfo.getProcessDefinitionId();
        }

        @Override
        public String getScopeId() {
            return taskInfo.getScopeId();
        }

        @Override
        public String getSubScopeId() {
            return taskInfo.getSubScopeId();
        }

        @Override
        public String getScopeType() {
            return taskInfo.getScopeType();
        }

        @Override
        public String getScopeDefinitionId() {
            return taskInfo.getScopeDefinitionId();
        }

        @Override
        public Date getCreateTime() {
            return taskInfo.getCreateTime();
        }

        @Override
        public String getTaskDefinitionKey() {
            return taskInfo.getTaskDefinitionKey();
        }

        @Override
        public Date getDueDate() {
            return taskInfo.getDueDate();
        }

        @Override
        public String getCategory() {
            return taskInfo.getCategory();
        }

        @Override
        public String getParentTaskId() {
            return taskInfo.getParentTaskId();
        }

        @Override
        public String getTenantId() {
            return taskInfo.getTenantId();
        }

        @Override
        public String getFormKey() {
            return taskInfo.getFormKey();
        }

        @Override
        public Map<String, Object> getTaskLocalVariables() {
            return taskInfo.getTaskLocalVariables();
        }

        @Override
        public Map<String, Object> getProcessVariables() {
            return taskInfo.getProcessVariables();
        }

        @Override
        public List<? extends IdentityLinkInfo> getIdentityLinks() {
            IdentityLinkEntityImpl identityLinkEntityCandidateUser = new IdentityLinkEntityImpl();
            identityLinkEntityCandidateUser.setUserId("testUser");
            identityLinkEntityCandidateUser.setType(IdentityLinkType.CANDIDATE);
            IdentityLinkEntityImpl identityLinkEntityCandidateGroup = new IdentityLinkEntityImpl();
            identityLinkEntityCandidateGroup.setGroupId("testGroup");
            identityLinkEntityCandidateGroup.setType(IdentityLinkType.CANDIDATE);

            return Arrays.asList(
                    identityLinkEntityCandidateUser,
                    identityLinkEntityCandidateGroup
            );
        }

        @Override
        public Date getClaimTime() {
            return taskInfo.getClaimTime();
        }
    }
}
