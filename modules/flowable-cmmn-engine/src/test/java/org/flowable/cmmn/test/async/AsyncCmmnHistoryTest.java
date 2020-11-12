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
import static org.assertj.core.api.Assertions.extractProperty;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        cmmnEngineConfiguration.setAsyncHistoryExecutorActivate(false);
        cmmnEngineConfiguration.setAsyncHistoryJsonGroupingEnabled(true);
        cmmnEngineConfiguration.setAsyncHistoryJsonGroupingThreshold(1);
        cmmnEngineConfiguration.setAsyncFailedJobWaitTime(100);
        cmmnEngineConfiguration.setDefaultFailedJobWaitTime(100);
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
                .callbackId("someCallbackId")
                .callbackType("someCallbackType")
                .referenceId("someReferenceId")
                .referenceType("someReferenceType")
                .start();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
        assertThat(historicCaseInstance.getId()).isEqualTo(caseInstance.getId());
        assertThat(historicCaseInstance.getName()).isEqualTo("someName");
        assertThat(historicCaseInstance.getParentId()).isNull();
        assertThat(historicCaseInstance.getBusinessKey()).isEqualTo("someBusinessKey");
        assertThat(historicCaseInstance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
        assertThat(historicCaseInstance.getCaseDefinitionKey()).isEqualTo("oneHumanTaskCase");
        assertThat(historicCaseInstance.getCaseDefinitionName()).isEqualTo("oneHumanTaskCaseName");
        assertThat(historicCaseInstance.getCaseDefinitionVersion()).isEqualTo(1);
        assertThat(historicCaseInstance.getCaseDefinitionDeploymentId()).isEqualTo(caseInstance.getCaseDefinitionDeploymentId());
        assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.ACTIVE);
        assertThat(historicCaseInstance.getCallbackId()).isEqualTo("someCallbackId");
        assertThat(historicCaseInstance.getCallbackType()).isEqualTo("someCallbackType");
        assertThat(historicCaseInstance.getReferenceId()).isEqualTo("someReferenceId");
        assertThat(historicCaseInstance.getReferenceType()).isEqualTo("someReferenceType");
        assertThat(historicCaseInstance.getStartTime()).isNotNull();
        assertThat(historicCaseInstance.getEndTime()).isNull();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertCaseInstanceEnded(caseInstance);
        historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
        assertThat(historicCaseInstance.getId()).isEqualTo(caseInstance.getId());
        assertThat(historicCaseInstance.getName()).isEqualTo("someName");
        assertThat(historicCaseInstance.getParentId()).isNull();
        assertThat(historicCaseInstance.getBusinessKey()).isEqualTo("someBusinessKey");
        assertThat(historicCaseInstance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
        assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.COMPLETED);
        assertThat(historicCaseInstance.getCallbackId()).isEqualTo("someCallbackId");
        assertThat(historicCaseInstance.getCallbackType()).isEqualTo("someCallbackType");
        assertThat(historicCaseInstance.getReferenceId()).isEqualTo("someReferenceId");
        assertThat(historicCaseInstance.getReferenceType()).isEqualTo("someReferenceType");
        assertThat(historicCaseInstance.getStartTime()).isNotNull();
        assertThat(historicCaseInstance.getEndTime()).isNotNull();
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

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        cmmnHistoryService.deleteHistoricCaseInstance(caseInstance.getId());

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isZero();
    }

    @Test
    public void testCreateTaskHistory() {
        Task task = cmmnTaskService.createTaskBuilder().id("task1").create();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult()).isNull();

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult()).isNotNull();
        assertThat(task.getId()).isEqualTo("task1");

        cmmnTaskService.deleteTask(task.getId(), true);
    }

    @Test
    @CmmnDeployment
    public void testMilestoneReached() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();
        assertThat(cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(historicMilestoneInstance.getName()).isEqualTo("xyzMilestone");
        assertThat(historicMilestoneInstance.getElementId()).isEqualTo("milestonePlanItem1");
        assertThat(historicMilestoneInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
        assertThat(historicMilestoneInstance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
        assertThat(historicMilestoneInstance.getTimeStamp()).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testIdentityLinks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "someUser", IdentityLinkType.PARTICIPANT);
        assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId())).isEmpty();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId())).hasSize(1);

        cmmnRuntimeService.deleteUserIdentityLink(caseInstance.getId(), "someUser", IdentityLinkType.PARTICIPANT);

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId())).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "test", "hello world");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "test2", 2);

        // Create
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);

        HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId())
                .variableName("test").singleResult();
        assertThat(historicVariableInstance.getVariableName()).isEqualTo("test");
        assertThat(historicVariableInstance.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(historicVariableInstance.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(historicVariableInstance.getValue()).isEqualTo("hello world");
        assertThat(historicVariableInstance.getCreateTime()).isNotNull();
        assertThat(historicVariableInstance.getLastUpdatedTime()).isNotNull();

        historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test2")
                .singleResult();
        assertThat(historicVariableInstance.getVariableName()).isEqualTo("test2");
        assertThat(historicVariableInstance.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(historicVariableInstance.getSubScopeId()).isNull();
        assertThat(historicVariableInstance.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(historicVariableInstance.getValue()).isEqualTo(2);
        assertThat(historicVariableInstance.getCreateTime()).isNotNull();
        assertThat(historicVariableInstance.getLastUpdatedTime()).isNotNull();

        // Update
        try {
            Thread.sleep(16); // wait time for diff in last updated time
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cmmnRuntimeService.setVariable(caseInstance.getId(), "test", "hello test");
        waitForAsyncHistoryExecutorToProcessAllJobs();

        HistoricVariableInstance updatedHistoricVariable = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId())
                .variableName("test").singleResult();
        assertThat(updatedHistoricVariable.getVariableName()).isEqualTo("test");
        assertThat(updatedHistoricVariable.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(updatedHistoricVariable.getSubScopeId()).isNull();
        assertThat(updatedHistoricVariable.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(updatedHistoricVariable.getValue()).isEqualTo("hello test");
        assertThat(updatedHistoricVariable.getCreateTime()).isNotNull();
        assertThat(updatedHistoricVariable.getLastUpdatedTime()).isNotNull();
        assertThat(historicVariableInstance.getLastUpdatedTime()).isNotEqualTo(updatedHistoricVariable.getLastUpdatedTime());

        // Delete
        cmmnRuntimeService.removeVariable(caseInstance.getId(), "test");
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test").singleResult()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        // Create
        HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getName()).isEqualTo("The Task");
        assertThat(historicTaskInstance.getAssignee()).isEqualTo("johnDoe");
        assertThat(historicTaskInstance.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(historicTaskInstance.getScopeDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
        assertThat(historicTaskInstance.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(historicTaskInstance.getCreateTime()).isNotNull();

        // Update
        cmmnTaskService.setAssignee(historicTaskInstance.getId(), "janeDoe");
        waitForAsyncHistoryExecutorToProcessAllJobs();

        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getName()).isEqualTo("The Task");
        assertThat(historicTaskInstance.getAssignee()).isEqualTo("janeDoe");

        cmmnTaskService.setPriority(historicTaskInstance.getId(), 99);
        waitForAsyncHistoryExecutorToProcessAllJobs();
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getPriority()).isEqualTo(99);
        assertThat(historicTaskInstance.getEndTime()).isNull();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("The Task");
        assertThat(planItemInstances).extracting(PlanItemInstance::getCreateTime).isNotNull();

        // Complete
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getEndTime()).isNotNull();

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
    public void testCasePageTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneCasePageTask").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceFormKey("testKey")
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(historicPlanItemInstance.getName()).isEqualTo("The Case Page Task");
        assertThat(historicPlanItemInstance.getFormKey()).isEqualTo("testKey");
        assertThat(historicPlanItemInstance.getExtraValue()).isEqualTo("testKey");
        assertThat(historicPlanItemInstance.getEndedTime()).isNull();

        List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForPlanItemInstance(historicPlanItemInstance.getId());
        assertThat(historicIdentityLinks).hasSize(5);

        List<HistoricIdentityLink> historicAssigneeLink = historicIdentityLinks.stream()
                .filter(identityLink -> identityLink.getType().equals(IdentityLinkType.ASSIGNEE)).collect(Collectors.toList());
        assertThat(historicAssigneeLink)
                .extracting(HistoricIdentityLink::getUserId)
                .containsExactly("johnDoe");

        List<HistoricIdentityLink> historicOwnerLink = historicIdentityLinks.stream()
                .filter(identityLink -> identityLink.getType().equals(IdentityLinkType.OWNER)).collect(Collectors.toList());
        assertThat(historicOwnerLink)
                .extracting(HistoricIdentityLink::getUserId)
                .containsExactly("janeDoe");

        List<HistoricIdentityLink> historicCandidateUserLinks = historicIdentityLinks.stream()
                .filter(identityLink -> identityLink.getType().equals(IdentityLinkType.CANDIDATE) &&
                        identityLink.getUserId() != null).collect(Collectors.toList());
        List<String> linkValues = new ArrayList<>();
        for (HistoricIdentityLink candidateLink : historicCandidateUserLinks) {
            linkValues.add(candidateLink.getUserId());
        }
        assertThat(extractProperty("userId").from(historicCandidateUserLinks))
                .containsExactlyInAnyOrder("johnDoe", "janeDoe");

        List<HistoricIdentityLink> historicGroupLink = historicIdentityLinks.stream()
                .filter(identityLink -> identityLink.getType().equals(IdentityLinkType.CANDIDATE) &&
                        identityLink.getGroupId() != null).collect(Collectors.toList());
        assertThat(historicGroupLink)
                .extracting(HistoricIdentityLink::getGroupId)
                .containsExactly("sales");

        // Complete
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getEndTime()).isNotNull();

        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceFormKey("testKey")
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(historicPlanItemInstance.getEndedTime()).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testPlanItemInstances() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimpleCaseFlow").start();
        List<PlanItemInstance> currentPlanItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(currentPlanItemInstances).hasSize(3);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(3);

        List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
        assertThat(historicPlanItemInstances.stream().map(HistoricPlanItemInstance::getPlanItemDefinitionType)
                .anyMatch(PlanItemDefinitionType.STAGE::equalsIgnoreCase)).isTrue();
        assertThat(historicPlanItemInstances.stream().map(HistoricPlanItemInstance::getPlanItemDefinitionType)
                .anyMatch(PlanItemDefinitionType.MILESTONE::equalsIgnoreCase)).isTrue();
        assertThat(historicPlanItemInstances.stream()
                .anyMatch(h -> "task".equalsIgnoreCase(h.getPlanItemDefinitionType()) && "planItemTaskA".equalsIgnoreCase(h.getElementId()))).isTrue();

        boolean showInOverviewMilestone = false;
        Date lastEnabledTimeTaskA = null;
        for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
            assertThat(historicPlanItemInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());

            assertThat(historicPlanItemInstance)
                    .extracting(
                            HistoricPlanItemInstance::getElementId,
                            HistoricPlanItemInstance::getCreateTime,
                            HistoricPlanItemInstance::getLastAvailableTime)
                    .doesNotContainNull();
            assertThat(historicPlanItemInstance)
                    .extracting(
                            HistoricPlanItemInstance::getEndedTime,
                            HistoricPlanItemInstance::getLastDisabledTime,
                            HistoricPlanItemInstance::getLastSuspendedTime,
                            HistoricPlanItemInstance::getExitTime,
                            HistoricPlanItemInstance::getTerminatedTime,
                            HistoricPlanItemInstance::getEntryCriterionId,
                            HistoricPlanItemInstance::getExitCriterionId)
                    .containsOnlyNulls();

            if ("planItemTaskA".equals(historicPlanItemInstance.getElementId())) {
                lastEnabledTimeTaskA = historicPlanItemInstance.getLastEnabledTime();
            } else if ("planItemMilestoneOne".equals(historicPlanItemInstance.getElementId())) {
                showInOverviewMilestone = historicPlanItemInstance.isShowInOverview();
            } else {
                assertThat(historicPlanItemInstance.getLastEnabledTime()).isNull();
            }
        }

        assertThat(lastEnabledTimeTaskA).isNotNull();
        assertThat(showInOverviewMilestone).isTrue();

        // Disable task
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertThat(task).isNotNull();

        cmmnRuntimeService.disablePlanItemInstance(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertThat(cmmnManagementService.createHistoryJobQuery().scopeType(ScopeTypes.CMMN).count()).isZero();
        assertThat(cmmnManagementService.createDeadLetterJobQuery().scopeType(ScopeTypes.CMMN).count()).isZero();

        HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(task.getId())
                .singleResult();
        assertThat(historicPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.DISABLED);
        assertThat(historicPlanItemInstance)
                .extracting(
                        HistoricPlanItemInstance::getLastEnabledTime,
                        HistoricPlanItemInstance::getLastDisabledTime,
                        HistoricPlanItemInstance::getLastAvailableTime,
                        HistoricPlanItemInstance::getLastUpdatedTime)
                .doesNotContainNull();
        assertThat(historicPlanItemInstance)
                .extracting(
                        HistoricPlanItemInstance::getLastStartedTime,
                        HistoricPlanItemInstance::getEndedTime,
                        HistoricPlanItemInstance::getLastSuspendedTime,
                        HistoricPlanItemInstance::getExitTime,
                        HistoricPlanItemInstance::getTerminatedTime)
                .containsOnlyNulls();

        // Enable task
        cmmnRuntimeService.enablePlanItemInstance(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(task.getId()).singleResult();
        assertThat(historicPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        assertThat(historicPlanItemInstance)
                .extracting(
                        HistoricPlanItemInstance::getLastEnabledTime,
                        HistoricPlanItemInstance::getLastDisabledTime,
                        HistoricPlanItemInstance::getLastAvailableTime,
                        HistoricPlanItemInstance::getLastUpdatedTime)
                .doesNotContainNull();
        assertThat(historicPlanItemInstance)
                .extracting(
                        HistoricPlanItemInstance::getLastStartedTime,
                        HistoricPlanItemInstance::getEndedTime,
                        HistoricPlanItemInstance::getLastSuspendedTime,
                        HistoricPlanItemInstance::getExitTime,
                        HistoricPlanItemInstance::getTerminatedTime)
                .containsOnlyNulls();

        // Manually enable
        cmmnRuntimeService.startPlanItemInstance(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();

        historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(task.getId()).singleResult();
        assertThat(historicPlanItemInstance.getLastStartedTime()).isNotNull();
        assertThat(historicPlanItemInstance.getEndedTime()).isNull();

        // Complete task
        Calendar clockCal = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        clockCal.add(Calendar.HOUR, 1);
        setClockTo(clockCal.getTime());
        cmmnRuntimeService.triggerPlanItemInstance(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();

        HistoricPlanItemInstance completedHistoricPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(task.getId())
                .singleResult();
        assertThat(completedHistoricPlanItemInstance)
                .extracting(
                        HistoricPlanItemInstance::getLastEnabledTime,
                        HistoricPlanItemInstance::getLastDisabledTime,
                        HistoricPlanItemInstance::getLastAvailableTime,
                        HistoricPlanItemInstance::getLastStartedTime,
                        HistoricPlanItemInstance::getLastUpdatedTime)
                .doesNotContainNull();
        assertThat(historicPlanItemInstance)
                .extracting(
                        HistoricPlanItemInstance::getEndedTime,
                        HistoricPlanItemInstance::getLastSuspendedTime,
                        HistoricPlanItemInstance::getExitTime,
                        HistoricPlanItemInstance::getTerminatedTime)
                .containsOnlyNulls();

        assertThat(historicPlanItemInstance.getLastUpdatedTime()).isBefore(completedHistoricPlanItemInstance.getLastUpdatedTime());

        HistoricPlanItemInstance completedMilestoneInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceElementId("planItemMilestoneOne").singleResult();
        assertThat(completedMilestoneInstance.getEndedTime()).isNotNull();
        assertThat(completedMilestoneInstance.isShowInOverview()).isTrue();

        cmmnEngineConfiguration.getClock().reset();
    }

    @Test
    @CmmnDeployment
    public void testCriterionStoredOnPlanItemInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCriterions").start();

        waitForAsyncHistoryExecutorToProcessAllJobs();

        // Executing the tasks triggers the entry criterion
        Task taskB = cmmnTaskService.createTaskQuery().taskName("B").singleResult();
        cmmnTaskService.complete(taskB.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("C").singleResult().getEntryCriterionId()).isEqualTo("entryA2");

        waitForAsyncHistoryExecutorToProcessAllJobs();

        HistoricPlanItemInstance planItemInstanceC = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceName("C").singleResult();
        assertThat(planItemInstanceC.getEntryCriterionId()).isEqualTo("entryA2");
        assertThat(planItemInstanceC.getExitCriterionId()).isNull();

        // Completing  will set the exit criterion
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        waitForAsyncHistoryExecutorToProcessAllJobs();
        planItemInstanceC = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceName("C").singleResult();
        assertThat(planItemInstanceC.getEntryCriterionId()).isEqualTo("entryA2");
        assertThat(planItemInstanceC.getExitCriterionId()).isEqualTo("stop");
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
            assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId("1").count()).isZero();
            waitForAsyncHistoryExecutorToProcessAllJobs();
            assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId("1").count()).isEqualTo(1);

            historicTaskLogEntry = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId("1").singleResult();
            assertThat(historicTaskLogEntry.getLogNumber()).isPositive();
            assertThat(historicTaskLogEntry.getTaskId()).isEqualTo("1");
            assertThat(historicTaskLogEntry.getType()).isEqualTo("testType");
            assertThat(historicTaskLogEntry.getUserId()).isEqualTo("testUserId");
            assertThat(historicTaskLogEntry.getScopeId()).isEqualTo("testScopeId");
            assertThat(historicTaskLogEntry.getScopeType()).isEqualTo("testScopeType");
            assertThat(historicTaskLogEntry.getScopeDefinitionId()).isEqualTo("testDefinitionId");
            assertThat(historicTaskLogEntry.getSubScopeId()).isEqualTo("testSubScopeId");
            assertThat(historicTaskLogEntry.getData()).isEqualTo("testData");
            assertThat(historicTaskLogEntry.getLogNumber()).isPositive();
            assertThat(historicTaskLogEntry.getTimeStamp()).isNotNull();
            assertThat(historicTaskLogEntry.getTenantId()).isEqualTo("testTenant");
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

        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().count()).isZero();
        assertThat(cmmnManagementService.createHistoryJobQuery().count()).isEqualTo(10);

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isEqualTo(11);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_CREATED.name()).count())
                .isEqualTo(1);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_NAME_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_PRIORITY_CHANGED.name())
                        .count())
                .isEqualTo(1);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_ASSIGNEE_CHANGED.name())
                        .count())
                .isEqualTo(1);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_OWNER_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_DUEDATE_CHANGED.name())
                        .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED.name())
                .count()).isEqualTo(2);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_REMOVED.name())
                        .count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_COMPLETED.name()).count())
                .isEqualTo(1);
    }

    @Test
    public void deleteAsynchUserTaskLogEntries() {
        CaseInstance caseInstance = deployAndStartOneHumanTaskCaseModel();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().count()).isZero();
        assertThat(cmmnManagementService.createHistoryJobQuery().count()).isEqualTo(1);
        waitForAsyncHistoryExecutorToProcessAllJobs();
        List<HistoricTaskLogEntry> historicTaskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(historicTaskLogEntries).hasSize(1);

        cmmnHistoryService.deleteHistoricTaskLogEntry(historicTaskLogEntries.get(0).getLogNumber());

        assertThat(cmmnManagementService.createHistoryJobQuery().count()).isEqualTo(1);
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void createRootEntityLink() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("someName")
                .businessKey("someBusinessKey")
                .start();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertCaseInstanceEnded(caseInstance);

        CommandExecutor commandExecutor = cmmnEngine.getCmmnEngineConfiguration().getCommandExecutor();

        List<HistoricEntityLink> entityLinksByScopeIdAndType = commandExecutor.execute(commandContext -> {
            HistoricEntityLinkService historicEntityLinkService = cmmnEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService();

            return historicEntityLinkService.findHistoricEntityLinksByReferenceScopeIdAndType(task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD);
        });

        assertThat(entityLinksByScopeIdAndType)
                .extracting(HistoricEntityLink::getHierarchyType)
                .containsExactly("root");
    }

    @Test
    @CmmnDeployment
    public void testPlanItemInstancesStateChangesWithFixedTime() {
        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Date fixTime = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(823));
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

        HistoricPlanItemInstance historicServiceTaskAvailableActiveCompleted = historicPlanItemInstancesByDefinitionId
                .get("serviceTaskAvailableActiveCompleted");

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

    @Test
    @CmmnDeployment
    public void testBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("businessKeyCase")
                .businessKey("someBusinessKey")
                .start();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getBusinessKey())
                .isEqualTo("someBusinessKey");
        cmmnRuntimeService.updateBusinessKey(caseInstance.getId(), "newBusinessKey");

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getBusinessKey())
                .isEqualTo("newBusinessKey");
    }

    @Test
    @CmmnDeployment
    public void testHistoryJobFailure() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneHumanTaskCase")
            .start();

        // Fetch the first history job, and programmatically change the handler type, such that it will guaranteed fail.
        HistoryJob historyJob = cmmnManagementService.createHistoryJobQuery().singleResult();
        changeHistoryJsonToBeInvalid((HistoryJobEntity) historyJob);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().list()).isEmpty();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createDeadLetterJobQuery().count()).isEqualTo(0);
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnManagementService.createHistoryJobQuery().count()).isEqualTo(0);
        // There is no historic case instance because the job data for it was invalid
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().list()).isEmpty();
        // There is a historic task because the job data for it was valid
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().list()).hasSize(1);

        Job deadLetterJob = cmmnManagementService.createDeadLetterJobQuery().singleResult();
        assertThat(deadLetterJob.getJobType()).isEqualTo(HistoryJobEntity.HISTORY_JOB_TYPE);
        assertThat(deadLetterJob.getExceptionMessage()).isNotEmpty();

        String deadLetterJobExceptionStacktrace = cmmnManagementService.getDeadLetterJobExceptionStacktrace(deadLetterJob.getId());
        assertThat(deadLetterJobExceptionStacktrace).isNotEmpty();

        cmmnManagementService.moveDeadLetterJobToHistoryJob(deadLetterJob.getId(), 3);
        assertThat(cmmnManagementService.createHistoryJobQuery().count()).isEqualTo(1);
        historyJob = cmmnManagementService.createHistoryJobQuery().singleResult();

        changeHistoryJsonToBeValid((HistoryJobEntity) historyJob);
        waitForAsyncHistoryExecutorToProcessAllJobs();
        // Once the history job is valid there will be a historic case instance
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().list()).hasSize(1);

        // The history jobs in the deadletter table have no link to the case instance, hence why a manual cleanup is needed.
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        cmmnManagementService.createHistoryJobQuery().list().forEach(j -> cmmnManagementService.deleteHistoryJob(j.getId()));
        cmmnManagementService.createDeadLetterJobQuery().list().forEach(j -> cmmnManagementService.deleteDeadLetterJob(j.getId()));

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncCmmnHistoryTest.testHistoryJobFailure.cmmn")
    public void testMoveDeadLetterJobBackToHistoryJob() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneHumanTaskCase")
            .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().list()).isEmpty();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().list()).isEmpty();

        // Fetch the first history job, and programmatically change the handler type, such that it will guaranteed fail.
        HistoryJob historyJob = cmmnManagementService.createHistoryJobQuery().singleResult();
        changeHistoryJsonToBeInvalid((HistoryJobEntity) historyJob);

        waitForAsyncHistoryExecutorToProcessAllJobs();

        // There is no historic case instance because the job data for it was invalid
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().list()).isEmpty();
        // There is a historic task because the job data for it was valid
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().list()).hasSize(1);

        assertThat(cmmnManagementService.createHistoryJobQuery().count()).isEqualTo(0);
        Job deadLetterJob = cmmnManagementService.createDeadLetterJobQuery().singleResult();

        cmmnManagementService.moveDeadLetterJobToHistoryJob(deadLetterJob.getId(), 3);
        assertThat(cmmnManagementService.createHistoryJobQuery().count()).isEqualTo(1);
        historyJob = cmmnManagementService.createHistoryJobQuery().singleResult();

        assertThat(historyJob.getCreateTime()).isNotNull();
        assertThat(historyJob.getRetries()).isEqualTo(3);
        assertThat(historyJob.getExceptionMessage()).isNotNull(); // this is consistent with regular jobs
        assertThat(historyJob.getJobHandlerConfiguration()).isNull(); // needs to have been reset

        changeHistoryJsonToBeValid((HistoryJobEntity) historyJob);
        waitForAsyncHistoryExecutorToProcessAllJobs();
        // Once the history job is valid there will be a historic case instance
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().list()).hasSize(1);

        // The history jobs in the deadletter table have no link to the case instance, hence why a manual cleanup is needed.
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        cmmnManagementService.createHistoryJobQuery().list().forEach(j -> cmmnManagementService.deleteHistoryJob(j.getId()));
        cmmnManagementService.createDeadLetterJobQuery().list().forEach(j -> cmmnManagementService.deleteDeadLetterJob(j.getId()));
    }

    protected void changeHistoryJsonToBeInvalid(HistoryJobEntity historyJob) {
        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                try {
                    HistoryJobEntity historyJobEntity = historyJob;

                    ObjectMapper objectMapper = cmmnEngineConfiguration.getObjectMapper();
                    JsonNode historyJsonNode = objectMapper.readTree(historyJobEntity.getAdvancedJobHandlerConfiguration());

                    for (JsonNode jsonNode : historyJsonNode) {
                        if (jsonNode.has("type") && "cmmn-case-instance-start".equals(jsonNode.get("type").asText())) {
                            ((ObjectNode) jsonNode).put("type", "invalidType");
                        }
                    }

                    historyJobEntity.setAdvancedJobHandlerConfiguration(objectMapper.writeValueAsString(historyJsonNode));
                } catch (JsonProcessingException e) {
                    Assert.fail();
                }
                return null;
            }
        });
    }

    protected void changeHistoryJsonToBeValid(HistoryJobEntity historyJob) {
        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                try {
                    HistoryJobEntity historyJobEntity = historyJob;

                    ObjectMapper objectMapper = cmmnEngineConfiguration.getObjectMapper();
                    JsonNode historyJsonNode = objectMapper.readTree(historyJobEntity.getAdvancedJobHandlerConfiguration());

                    for (JsonNode jsonNode : historyJsonNode) {
                        if (jsonNode.has("type") && "invalidType".equals(jsonNode.get("type").asText())) {
                            ((ObjectNode) jsonNode).put("type", "cmmn-case-instance-start");
                        }
                    }

                    historyJobEntity.setAdvancedJobHandlerConfiguration(objectMapper.writeValueAsString(historyJsonNode));
                } catch (JsonProcessingException e) {
                    Assert.fail();
                }
                return null;
            }
        });
    }

}
