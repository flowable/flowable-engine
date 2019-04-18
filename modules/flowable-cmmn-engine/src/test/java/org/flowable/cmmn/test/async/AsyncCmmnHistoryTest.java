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
package org.flowable.cmmn.test.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class AsyncCmmnHistoryTest extends CustomCmmnConfigurationFlowableTestCase {
    
    @Override
    protected String getEngineName() {
        return "AsyncCmmnHistoryTest";
    }
    
    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setAsyncHistoryEnabled(true);
        cmmnEngineConfiguration.setAsyncExecutorActivate(false);
        cmmnEngineConfiguration.setAsyncHistoryJsonGroupingEnabled(true);
        cmmnEngineConfiguration.setAsyncHistoryJsonGroupingThreshold(1);
        cmmnEngineConfiguration.setAsyncFailedJobWaitTime(1);
        cmmnEngineConfiguration.setDefaultFailedJobWaitTime(1);
        cmmnEngineConfiguration.setAsyncHistoryExecutorNumberOfRetries(10);
        cmmnEngineConfiguration.setAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime(1000);
    }
    
    @Test
    @CmmnDeployment
    public void testCaseInstanceStartAndEnd() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("someName")
                .businessKey("someBusinessKey")
                .start();
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
        
        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
        assertEquals(caseInstance.getId(), historicCaseInstance.getId());
        assertEquals("someName", historicCaseInstance.getName());
        assertNull(historicCaseInstance.getParentId());
        assertEquals("someBusinessKey", historicCaseInstance.getBusinessKey());
        assertEquals(caseInstance.getCaseDefinitionId(), historicCaseInstance.getCaseDefinitionId());
        assertEquals(CaseInstanceState.ACTIVE, historicCaseInstance.getState());
        assertNotNull(historicCaseInstance.getStartTime());
        assertNull(historicCaseInstance.getEndTime());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        assertCaseInstanceEnded(caseInstance);
        historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
        assertEquals(caseInstance.getId(), historicCaseInstance.getId());
        assertEquals("someName", historicCaseInstance.getName());
        assertNull(historicCaseInstance.getParentId());
        assertEquals("someBusinessKey", historicCaseInstance.getBusinessKey());
        assertEquals(caseInstance.getCaseDefinitionId(), historicCaseInstance.getCaseDefinitionId());
        assertEquals(CaseInstanceState.COMPLETED, historicCaseInstance.getState());
        assertNotNull(historicCaseInstance.getStartTime());
        assertNotNull(historicCaseInstance.getEndTime());
    }
    
    @Test
    @CmmnDeployment
    public void testHistoricCaseInstanceDeleted() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("someName")
                .businessKey("someBusinessKey")
                .variable("test", "test")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
        cmmnHistoryService.deleteHistoricCaseInstance(caseInstance.getId());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
    }

    @Test
    public void testCreateTaskHistory() {
        Task task = cmmnTaskService.createTaskBuilder().id("task1").create();
        assertNull(cmmnHistoryService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult());

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertNotNull(cmmnHistoryService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult());
        assertEquals("task1", task.getId());

        cmmnTaskService.deleteTask(task.getId(), true);
    }
    
    @Test
    @CmmnDeployment
    public void testMilestoneReached() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();
        assertEquals(1, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        HistoricMilestoneInstance historicMilestoneInstance =  cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .singleResult();
        assertEquals("xyzMilestone", historicMilestoneInstance.getName());
        assertEquals("milestonePlanItem1", historicMilestoneInstance.getElementId());
        assertEquals(caseInstance.getId(), historicMilestoneInstance.getCaseInstanceId());
        assertEquals(caseInstance.getCaseDefinitionId(), historicMilestoneInstance.getCaseDefinitionId());
        assertNotNull(historicMilestoneInstance.getTimeStamp());
    }
    
    @Test
    @CmmnDeployment
    public void testIdentityLinks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "someUser", IdentityLinkType.PARTICIPANT);
        assertEquals(0, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId()).size());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(1, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId()).size());
        
        cmmnRuntimeService.deleteUserIdentityLink(caseInstance.getId(), "someUser", IdentityLinkType.PARTICIPANT);
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(0, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId()).size());
    }
    
    @Test
    @CmmnDeployment
    public void testVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        assertEquals(0, cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count());

        cmmnRuntimeService.setVariable(caseInstance.getId(), "test", "hello world");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "test2", 2);
        
        // Create
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(2, cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        
        HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test").singleResult();
        assertEquals("test", historicVariableInstance.getVariableName());
        assertEquals(caseInstance.getId(), historicVariableInstance.getScopeId());
        assertEquals(ScopeTypes.CMMN, historicVariableInstance.getScopeType());
        assertEquals("hello world", historicVariableInstance.getValue());
        assertNotNull(historicVariableInstance.getCreateTime());
        assertNotNull(historicVariableInstance.getLastUpdatedTime());
        
        historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test2").singleResult();
        assertEquals("test2", historicVariableInstance.getVariableName());
        assertEquals(caseInstance.getId(), historicVariableInstance.getScopeId());
        assertNull(historicVariableInstance.getSubScopeId());
        assertEquals(ScopeTypes.CMMN, historicVariableInstance.getScopeType());
        assertEquals(2, historicVariableInstance.getValue());
        assertNotNull(historicVariableInstance.getCreateTime());
        assertNotNull(historicVariableInstance.getLastUpdatedTime());
        
        // Update
        try {
            Thread.sleep(16); // wait time for diff in last updated time
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 
        cmmnRuntimeService.setVariable(caseInstance.getId(), "test", "hello test");
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        HistoricVariableInstance updatedHistoricVariable = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test").singleResult();
        assertEquals("test", updatedHistoricVariable.getVariableName());
        assertEquals(caseInstance.getId(), updatedHistoricVariable.getScopeId());
        assertNull(updatedHistoricVariable.getSubScopeId());
        assertEquals(ScopeTypes.CMMN, updatedHistoricVariable.getScopeType());
        assertEquals("hello test", updatedHistoricVariable.getValue());
        assertNotNull(updatedHistoricVariable.getCreateTime());
        assertNotNull(updatedHistoricVariable.getLastUpdatedTime());
        assertNotEquals(updatedHistoricVariable.getLastUpdatedTime(), historicVariableInstance.getLastUpdatedTime());
        
        // Delete
        cmmnRuntimeService.removeVariable(caseInstance.getId(), "test");
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertNull(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test").singleResult());
    }
    
    @Test
    @CmmnDeployment
    public void testHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(0, cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(1, cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Create
        HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("The Task", historicTaskInstance.getName());
        assertEquals("johnDoe", historicTaskInstance.getAssignee());
        assertEquals(caseInstance.getId(), historicTaskInstance.getScopeId());
        assertEquals(caseInstance.getCaseDefinitionId(), historicTaskInstance.getScopeDefinitionId());
        assertEquals(ScopeTypes.CMMN, historicTaskInstance.getScopeType());
        assertNotNull(historicTaskInstance.getCreateTime());
        
        // Update
        cmmnTaskService.setAssignee(historicTaskInstance.getId(), "janeDoe");
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("The Task", historicTaskInstance.getName());
        assertEquals("janeDoe", historicTaskInstance.getAssignee());
        
        cmmnTaskService.setPriority(historicTaskInstance.getId(), 99);
        waitForAsyncHistoryExecutorToProcessAllJobs();
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(99, historicTaskInstance.getPriority());
        assertNull(historicTaskInstance.getEndTime());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .orderByName().asc()
            .list();
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("The Task");
        assertThat(planItemInstances).extracting(PlanItemInstance::getCreateTime).isNotNull();
        
        // Complete
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(historicTaskInstance.getEndTime());

        List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .list();
        assertThat(historicPlanItemInstances).extracting(HistoricPlanItemInstance::getName).containsExactly("The Task");
        assertThat(historicPlanItemInstances).extracting(HistoricPlanItemInstance::getCreateTime).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testHumanTaskWithCandidateUsersAndGroups() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(cmmnTaskService.getIdentityLinksForTask(task.getId()))
            .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
            .containsExactlyInAnyOrder(
                tuple("assignee", "johnDoe", null),
                tuple("candidate", "user1", null),
                tuple("candidate", null, "group1"),
                tuple("candidate", null, "group2")
            );

        assertThatThrownBy(() -> cmmnHistoryService.getHistoricIdentityLinksForTask(task.getId()))
            .isInstanceOf(FlowableObjectNotFoundException.class)
            .hasMessageContaining("No historic task exists");

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertThat(cmmnHistoryService.getHistoricIdentityLinksForTask(task.getId()))
            .extracting(HistoricIdentityLink::getType, HistoricIdentityLink::getUserId, HistoricIdentityLink::getGroupId)
            .containsExactlyInAnyOrder(
                tuple("assignee", "johnDoe", null),
                tuple("candidate", "user1", null),
                tuple("candidate", null, "group1"),
                tuple("candidate", null, "group2")
            );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncCmmnHistoryTest.testHumanTask.cmmn")
    public void testHumanTaskWithNameDueDateAndDescription() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        task.setName("Test name");
        task.setDescription("Test description");
        cmmnTaskService.saveTask(task);

        waitForAsyncHistoryExecutorToProcessAllJobs();

        // Create
        HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance).isNotNull();
        assertThat(historicTaskInstance.getName()).isEqualTo("Test name");
        assertThat(historicTaskInstance.getDescription()).isEqualTo("Test description");
        assertThat(historicTaskInstance.getDueDate()).isNull();

        // Set due date
        Date dueDate = Date.from(Instant.now().with(ChronoField.MILLI_OF_SECOND, 0));
        cmmnTaskService.setDueDate(task.getId(), dueDate);

        waitForAsyncHistoryExecutorToProcessAllJobs();

        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);

        // Update name and description to null
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        task.setName(null);
        task.setDescription(null);
        cmmnTaskService.saveTask(task);

        // Before the history jobs it has the old data
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getName()).isEqualTo("Test name");
        assertThat(historicTaskInstance.getDescription()).isEqualTo("Test description");

        waitForAsyncHistoryExecutorToProcessAllJobs();

        // After the history jobs it has the new data
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getName()).isNull();
        assertThat(historicTaskInstance.getDescription()).isNull();

        // Update dueDate to null
        cmmnTaskService.setDueDate(task.getId(), null);

        // Before the history jobs it has the old data
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);

        waitForAsyncHistoryExecutorToProcessAllJobs();

        // After the history jobs it has the new data
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getDueDate()).isNull();
    }
    
    @Test
    @CmmnDeployment
    public void testPlanItemInstances() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimpleCaseFlow").start();
        List<PlanItemInstance> currentPlanItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertEquals(3, currentPlanItemInstances.size());
        assertEquals(0, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(3, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count());
        
        List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
        assertTrue(historicPlanItemInstances.stream().map(HistoricPlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.STAGE::equalsIgnoreCase));
        assertTrue(historicPlanItemInstances.stream().map(HistoricPlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.MILESTONE::equalsIgnoreCase));
        assertTrue(historicPlanItemInstances.stream().anyMatch(h -> "task".equalsIgnoreCase(h.getPlanItemDefinitionType()) && "planItemTaskA".equalsIgnoreCase(h.getElementId())));
        
        for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
            assertEquals(caseInstance.getId(), historicPlanItemInstance.getCaseInstanceId());
            assertEquals(caseInstance.getCaseDefinitionId(), historicPlanItemInstance.getCaseDefinitionId());
            assertNotNull(historicPlanItemInstance.getElementId());
            assertNotNull(historicPlanItemInstance.getCreateTime());
            assertNotNull(historicPlanItemInstance.getLastAvailableTime());
            assertNull(historicPlanItemInstance.getEndedTime());
            assertNull(historicPlanItemInstance.getLastDisabledTime());
            assertNull(historicPlanItemInstance.getLastSuspendedTime());
            assertNull(historicPlanItemInstance.getExitTime());
            assertNull(historicPlanItemInstance.getTerminatedTime());
            assertNull(historicPlanItemInstance.getEntryCriterionId());
            assertNull(historicPlanItemInstance.getExitCriterionId());
            
            if (historicPlanItemInstance.getElementId().equals("planItemTaskA")) {
                assertNotNull(historicPlanItemInstance.getLastEnabledTime());
            } else {
                assertNull(historicPlanItemInstance.getLastEnabledTime());
            }
        }
        
        // Disable task
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertNotNull(task);
        
        cmmnRuntimeService.disablePlanItemInstance(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        assertEquals(0, cmmnManagementService.createHistoryJobQuery().scopeType(ScopeTypes.CMMN).count());
        assertEquals(0, cmmnManagementService.createDeadLetterJobQuery().scopeType(ScopeTypes.CMMN).count());
        
        HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(task.getId()).singleResult();
        assertEquals(PlanItemInstanceState.DISABLED, historicPlanItemInstance.getState());
        assertNotNull(historicPlanItemInstance.getLastEnabledTime());
        assertNotNull(historicPlanItemInstance.getLastDisabledTime());
        
        assertNotNull(historicPlanItemInstance.getLastAvailableTime());
        assertNull(historicPlanItemInstance.getLastStartedTime());
        assertNull(historicPlanItemInstance.getEndedTime());
        assertNull(historicPlanItemInstance.getLastSuspendedTime());
        assertNull(historicPlanItemInstance.getExitTime());
        assertNull(historicPlanItemInstance.getTerminatedTime());
        assertNotNull(historicPlanItemInstance.getLastUpdatedTime());
        
        // Enable task
        cmmnRuntimeService.enablePlanItemInstance(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(task.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ENABLED, historicPlanItemInstance.getState());
        assertNotNull(historicPlanItemInstance.getLastEnabledTime());
        
        assertNotNull(historicPlanItemInstance.getLastAvailableTime());
        assertNotNull(historicPlanItemInstance.getLastDisabledTime());
        assertNull(historicPlanItemInstance.getLastStartedTime());
        assertNull(historicPlanItemInstance.getEndedTime());
        assertNull(historicPlanItemInstance.getLastSuspendedTime());
        assertNull(historicPlanItemInstance.getExitTime());
        assertNull(historicPlanItemInstance.getTerminatedTime());
        assertNotNull(historicPlanItemInstance.getLastUpdatedTime());
        
        // Manually enable
        cmmnRuntimeService.startPlanItemInstance(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(task.getId()).singleResult();
        assertNotNull(historicPlanItemInstance.getLastStartedTime());
        assertNull(historicPlanItemInstance.getEndedTime());
        
        // Complete task
        Calendar clockCal = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        clockCal.add(Calendar.HOUR, 1);
        setClockTo(clockCal.getTime());
        cmmnRuntimeService.triggerPlanItemInstance(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        HistoricPlanItemInstance completedHistoricPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(task.getId()).singleResult();
        assertNotNull(completedHistoricPlanItemInstance.getEndedTime());
        
        assertNotNull(completedHistoricPlanItemInstance.getLastEnabledTime());
        assertNotNull(completedHistoricPlanItemInstance.getLastDisabledTime());
        assertNotNull(completedHistoricPlanItemInstance.getLastAvailableTime());
        assertNotNull(completedHistoricPlanItemInstance.getLastStartedTime());
        assertNull(completedHistoricPlanItemInstance.getLastSuspendedTime());
        assertNull(completedHistoricPlanItemInstance.getExitTime());
        assertNull(completedHistoricPlanItemInstance.getTerminatedTime());
        assertNotNull(completedHistoricPlanItemInstance.getLastUpdatedTime());
        assertTrue(historicPlanItemInstance.getLastUpdatedTime().before(completedHistoricPlanItemInstance.getLastUpdatedTime()));
        
        cmmnEngineConfiguration.getClock().reset();
    }

    @Test
    @CmmnDeployment
    public void testCriterionStoredOnPlanItemInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCriterions").start();

        // Executing the tasks triggers the entry criterion
        Task taskB = cmmnTaskService.createTaskQuery().taskName("B").singleResult();
        cmmnTaskService.complete(taskB.getId());

        assertEquals(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("C").singleResult().getEntryCriterionId(), "entryA2");

        waitForAsyncHistoryExecutorToProcessAllJobs();

        HistoricPlanItemInstance planItemInstanceC = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceName("C").singleResult();
        assertEquals(planItemInstanceC.getEntryCriterionId(), "entryA2");
        assertNull(planItemInstanceC.getExitCriterionId());

        // Completing  will set the exit criterion
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        waitForAsyncHistoryExecutorToProcessAllJobs();
        planItemInstanceC = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceName("C").singleResult();
        assertEquals(planItemInstanceC.getEntryCriterionId(), "entryA2");
        assertEquals(planItemInstanceC.getExitCriterionId(), "stop");
    }

    @Test
    public void createUserTaskLogEntity() {
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder();
        
        Date todayDate = new Date();
        historicTaskLogEntryBuilder.taskId("1");
        historicTaskLogEntryBuilder.type("testType");
        historicTaskLogEntryBuilder.userId("testUserId");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.scopeId("testScopeId");
        historicTaskLogEntryBuilder.scopeType("testScopeType");
        historicTaskLogEntryBuilder.scopeDefinitionId("testDefinitionId");
        historicTaskLogEntryBuilder.subScopeId("testSubScopeId");
        historicTaskLogEntryBuilder.timeStamp(todayDate);
        historicTaskLogEntryBuilder.tenantId("testTenant");

        historicTaskLogEntryBuilder.create();

        HistoricTaskLogEntry historicTaskLogEntry = null;
        try {
            assertEquals(0l, cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId("1").count());
            waitForAsyncHistoryExecutorToProcessAllJobs();
            assertEquals(1l, cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId("1").count());

            historicTaskLogEntry = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId("1").singleResult();
            assertTrue(historicTaskLogEntry.getLogNumber() > 0);
            assertEquals("1", historicTaskLogEntry.getTaskId());
            assertEquals("testType", historicTaskLogEntry.getType());
            assertEquals("testUserId", historicTaskLogEntry.getUserId());
            assertEquals("testScopeId", historicTaskLogEntry.getScopeId());
            assertEquals("testScopeType", historicTaskLogEntry.getScopeType());
            assertEquals("testDefinitionId", historicTaskLogEntry.getScopeDefinitionId());
            assertEquals("testSubScopeId", historicTaskLogEntry.getSubScopeId());
            assertEquals("testData", historicTaskLogEntry.getData());
            assertTrue(historicTaskLogEntry.getLogNumber() > 0l);
            assertNotNull(historicTaskLogEntry.getTimeStamp());
            assertEquals("testTenant", historicTaskLogEntry.getTenantId());
        } finally {
            if (historicTaskLogEntry != null) {
                cmmnHistoryService.deleteHistoricTaskLogEntry(historicTaskLogEntry.getLogNumber());
                waitForAsyncHistoryExecutorToProcessAllJobs();
            }
        }
    }

    @Test
    public void createCmmnAsynchUserTaskLogEntries() {
        CaseInstance caseInstance = deployAndStartOneHumanTaskCaseModel();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        task.setName("newName");
        task.setPriority(0);
        cmmnTaskService.saveTask(task);
        cmmnTaskService.setAssignee(task.getId(), "newAssignee");
        cmmnTaskService.setOwner(task.getId(), "newOwner");
        cmmnTaskService.setDueDate(task.getId(), new Date());
        cmmnTaskService.addUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);
        cmmnTaskService.addGroupIdentityLink(task.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnTaskService.deleteUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);
        cmmnTaskService.deleteGroupIdentityLink(task.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnTaskService.complete(task.getId());

        assertEquals(0l, cmmnHistoryService.createHistoricTaskLogEntryQuery().count());
        assertEquals(10l, cmmnManagementService.createHistoryJobQuery().count());

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertEquals(11l, cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count());
        assertEquals(1l, cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_CREATED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_NAME_CHANGED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_PRIORITY_CHANGED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_ASSIGNEE_CHANGED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_OWNER_CHANGED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_DUEDATE_CHANGED.name()).count());
        assertEquals(2l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED.name()).count());
        assertEquals(2l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_REMOVED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_COMPLETED.name()).count());
    }

    @Test
    public void deleteAsynchUserTaskLogEntries() {
        CaseInstance caseInstance = deployAndStartOneHumanTaskCaseModel();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(0l, cmmnHistoryService.createHistoricTaskLogEntryQuery().count());
        assertEquals(1l, cmmnManagementService.createHistoryJobQuery().count());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        List<HistoricTaskLogEntry> historicTaskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertEquals(1l, historicTaskLogEntries.size());

        cmmnHistoryService.deleteHistoricTaskLogEntry(historicTaskLogEntries.get(0).getLogNumber());

        assertEquals(1l, cmmnManagementService.createHistoryJobQuery().count());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(0l, cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count());
    }

    @Test
    @CmmnDeployment
    public void createRootEntityLink() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneHumanTaskCase")
            .name("someName")
            .businessKey("someBusinessKey")
            .start();
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().count());

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().count());

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertCaseInstanceEnded(caseInstance);

        CommandExecutor commandExecutor = cmmnEngine.getCmmnEngineConfiguration().getCommandExecutor();

        List<HistoricEntityLink> entityLinksByScopeIdAndType = commandExecutor.execute(commandContext -> {
            HistoricEntityLinkService historicEntityLinkService = CommandContextUtil.getHistoricEntityLinkService(commandContext);

            return historicEntityLinkService.findHistoricEntityLinksByReferenceScopeIdAndType(task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD);
        });

        assertEquals(1, entityLinksByScopeIdAndType.size());
        assertEquals("root", entityLinksByScopeIdAndType.get(0).getHierarchyType());
    }

    @Test
    @CmmnDeployment
    public void testPlanItemInstancesStateChangesWithFixedTime() {
        Date fixTime = Date.from(Instant.now());
        cmmnEngineConfiguration.getClock().setCurrentTime(fixTime);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("allStates")
            .start();

        List<PlanItemInstance> runtimePlanItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(runtimePlanItemInstances)
            .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
            .as("planItemDefinitionId, state")
            .containsExactlyInAnyOrder(
                tuple("eventListenerAvailable", PlanItemInstanceState.AVAILABLE),
                tuple("eventListenerUnavailable", PlanItemInstanceState.UNAVAILABLE),
                tuple("serviceTaskAvailableEnabled", PlanItemInstanceState.ENABLED),
                tuple("serviceTaskAvailableAsyncActive", PlanItemInstanceState.ASYNC_ACTIVE)
            );

        Map<String, PlanItemInstance> runtimePlanItemInstancesByDefinitionId = runtimePlanItemInstances.stream()
            .collect(Collectors.toMap(PlanItemInstance::getPlanItemDefinitionId, Function.identity()));

        PlanItemInstance eventListenerAvailable = runtimePlanItemInstancesByDefinitionId.get("eventListenerAvailable");

        assertThat(eventListenerAvailable).extracting(
            PlanItemInstance::getCompletedTime,
            PlanItemInstance::getEndedTime,
            PlanItemInstance::getOccurredTime,
            PlanItemInstance::getTerminatedTime,
            PlanItemInstance::getExitTime,
            PlanItemInstance::getLastEnabledTime,
            PlanItemInstance::getLastDisabledTime,
            PlanItemInstance::getLastStartedTime,
            PlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(eventListenerAvailable).extracting(
            PlanItemInstance::getCreateTime,
            PlanItemInstance::getLastAvailableTime
        ).containsOnly(fixTime);

        PlanItemInstance eventListenerUnavailable = runtimePlanItemInstancesByDefinitionId.get("eventListenerUnavailable");

        assertThat(eventListenerUnavailable).extracting(
            PlanItemInstance::getCompletedTime,
            PlanItemInstance::getEndedTime,
            PlanItemInstance::getOccurredTime,
            PlanItemInstance::getTerminatedTime,
            PlanItemInstance::getExitTime,
            PlanItemInstance::getLastEnabledTime,
            PlanItemInstance::getLastAvailableTime,
            PlanItemInstance::getLastDisabledTime,
            PlanItemInstance::getLastStartedTime,
            PlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(eventListenerUnavailable).extracting(
            PlanItemInstance::getCreateTime
        ).isEqualTo(fixTime);

        PlanItemInstance serviceTaskAvailableEnabled = runtimePlanItemInstancesByDefinitionId.get("serviceTaskAvailableEnabled");

        assertThat(serviceTaskAvailableEnabled).extracting(
            PlanItemInstance::getCompletedTime,
            PlanItemInstance::getEndedTime,
            PlanItemInstance::getOccurredTime,
            PlanItemInstance::getTerminatedTime,
            PlanItemInstance::getExitTime,
            PlanItemInstance::getLastDisabledTime,
            PlanItemInstance::getLastStartedTime,
            PlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(serviceTaskAvailableEnabled).extracting(
            PlanItemInstance::getCreateTime,
            PlanItemInstance::getLastEnabledTime,
            PlanItemInstance::getLastAvailableTime
        ).containsOnly(fixTime);

        PlanItemInstance serviceTaskAvailableAsyncActive = runtimePlanItemInstancesByDefinitionId.get("serviceTaskAvailableAsyncActive");

        assertThat(serviceTaskAvailableAsyncActive).extracting(
            PlanItemInstance::getCompletedTime,
            PlanItemInstance::getEndedTime,
            PlanItemInstance::getOccurredTime,
            PlanItemInstance::getTerminatedTime,
            PlanItemInstance::getExitTime,
            PlanItemInstance::getLastEnabledTime,
            PlanItemInstance::getLastDisabledTime,
            PlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(serviceTaskAvailableAsyncActive).extracting(
            PlanItemInstance::getCreateTime,
            PlanItemInstance::getLastAvailableTime,
            PlanItemInstance::getLastStartedTime
        ).containsOnly(fixTime);

        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list())
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
            .isEmpty();

        waitForAsyncHistoryExecutorToProcessAllJobs();

        List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .list();

        assertThat(historicPlanItemInstances)
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
            .containsExactlyInAnyOrder(
                tuple("serviceTaskAvailableActiveCompleted", PlanItemInstanceState.COMPLETED),
                tuple("stageAvailableActiveTerminated", PlanItemInstanceState.TERMINATED),
                tuple("humanTaskAvailableActiveTerminatedAndWaitingForRepetition", PlanItemInstanceState.TERMINATED),
                tuple("eventListenerAvailable", PlanItemInstanceState.AVAILABLE),
                tuple("eventListenerUnavailable", PlanItemInstanceState.UNAVAILABLE),
                tuple("serviceTaskAvailableEnabled", PlanItemInstanceState.ENABLED),
                tuple("serviceTaskAvailableAsyncActive", PlanItemInstanceState.ASYNC_ACTIVE)
            );

        Map<String, HistoricPlanItemInstance> historicPlanItemInstancesByDefinitionId = historicPlanItemInstances.stream()
            .collect(Collectors.toMap(HistoricPlanItemInstance::getPlanItemDefinitionId, Function.identity()));

        HistoricPlanItemInstance historicEventListenerAvailable = historicPlanItemInstancesByDefinitionId.get("eventListenerAvailable");

        assertThat(historicEventListenerAvailable).extracting(
            HistoricPlanItemInstance::getCompletedTime,
            HistoricPlanItemInstance::getEndedTime,
            HistoricPlanItemInstance::getOccurredTime,
            HistoricPlanItemInstance::getTerminatedTime,
            HistoricPlanItemInstance::getExitTime,
            HistoricPlanItemInstance::getLastEnabledTime,
            HistoricPlanItemInstance::getLastDisabledTime,
            HistoricPlanItemInstance::getLastStartedTime,
            HistoricPlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(historicEventListenerAvailable).extracting(
            HistoricPlanItemInstance::getCreateTime,
            HistoricPlanItemInstance::getLastAvailableTime
        ).containsOnly(fixTime);

        HistoricPlanItemInstance historicEventListenerUnavailable = historicPlanItemInstancesByDefinitionId.get("eventListenerUnavailable");

        assertThat(historicEventListenerUnavailable).extracting(
            HistoricPlanItemInstance::getCompletedTime,
            HistoricPlanItemInstance::getEndedTime,
            HistoricPlanItemInstance::getOccurredTime,
            HistoricPlanItemInstance::getTerminatedTime,
            HistoricPlanItemInstance::getExitTime,
            HistoricPlanItemInstance::getLastEnabledTime,
            HistoricPlanItemInstance::getLastAvailableTime,
            HistoricPlanItemInstance::getLastDisabledTime,
            HistoricPlanItemInstance::getLastStartedTime,
            HistoricPlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(historicEventListenerUnavailable).extracting(
            HistoricPlanItemInstance::getCreateTime
        ).isEqualTo(fixTime);

        HistoricPlanItemInstance historicServiceTaskAvailableEnabled = historicPlanItemInstancesByDefinitionId.get("serviceTaskAvailableEnabled");

        assertThat(historicServiceTaskAvailableEnabled).extracting(
            HistoricPlanItemInstance::getCompletedTime,
            HistoricPlanItemInstance::getEndedTime,
            HistoricPlanItemInstance::getOccurredTime,
            HistoricPlanItemInstance::getTerminatedTime,
            HistoricPlanItemInstance::getExitTime,
            HistoricPlanItemInstance::getLastDisabledTime,
            HistoricPlanItemInstance::getLastStartedTime,
            HistoricPlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(historicServiceTaskAvailableEnabled).extracting(
            HistoricPlanItemInstance::getCreateTime,
            HistoricPlanItemInstance::getLastEnabledTime,
            HistoricPlanItemInstance::getLastAvailableTime
        ).containsOnly(fixTime);

        HistoricPlanItemInstance historicServiceTaskAvailableActiveCompleted = historicPlanItemInstancesByDefinitionId.get("serviceTaskAvailableActiveCompleted");

        assertThat(historicServiceTaskAvailableActiveCompleted).extracting(
            HistoricPlanItemInstance::getOccurredTime,
            HistoricPlanItemInstance::getTerminatedTime,
            HistoricPlanItemInstance::getExitTime,
            HistoricPlanItemInstance::getLastEnabledTime,
            HistoricPlanItemInstance::getLastDisabledTime,
            HistoricPlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(historicServiceTaskAvailableActiveCompleted).extracting(
            HistoricPlanItemInstance::getCreateTime,
            HistoricPlanItemInstance::getCompletedTime,
            HistoricPlanItemInstance::getEndedTime,
            HistoricPlanItemInstance::getLastAvailableTime,
            HistoricPlanItemInstance::getLastStartedTime
        ).containsOnly(fixTime);

        HistoricPlanItemInstance historicStageAvailableActiveTerminated = historicPlanItemInstancesByDefinitionId.get("stageAvailableActiveTerminated");

        assertThat(historicStageAvailableActiveTerminated).extracting(
            HistoricPlanItemInstance::getCompletedTime,
            HistoricPlanItemInstance::getOccurredTime,
            HistoricPlanItemInstance::getTerminatedTime,
            HistoricPlanItemInstance::getLastEnabledTime,
            HistoricPlanItemInstance::getLastDisabledTime,
            HistoricPlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(historicStageAvailableActiveTerminated).extracting(
            HistoricPlanItemInstance::getCreateTime,
            HistoricPlanItemInstance::getEndedTime,
            HistoricPlanItemInstance::getExitTime,
            HistoricPlanItemInstance::getLastAvailableTime,
            HistoricPlanItemInstance::getLastStartedTime
        ).containsOnly(fixTime);

        HistoricPlanItemInstance historicHumanTaskAvailableActiveTerminatedAndWaitingForRepetition = historicPlanItemInstancesByDefinitionId
            .get("humanTaskAvailableActiveTerminatedAndWaitingForRepetition");

        assertThat(historicHumanTaskAvailableActiveTerminatedAndWaitingForRepetition).extracting(
            HistoricPlanItemInstance::getCompletedTime,
            HistoricPlanItemInstance::getOccurredTime,
            HistoricPlanItemInstance::getTerminatedTime,
            HistoricPlanItemInstance::getLastEnabledTime,
            HistoricPlanItemInstance::getLastDisabledTime,
            HistoricPlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(historicHumanTaskAvailableActiveTerminatedAndWaitingForRepetition).extracting(
            HistoricPlanItemInstance::getCreateTime,
            HistoricPlanItemInstance::getEndedTime,
            HistoricPlanItemInstance::getExitTime,
            HistoricPlanItemInstance::getLastAvailableTime,
            HistoricPlanItemInstance::getLastStartedTime
        ).containsOnly(fixTime);
    }

}
