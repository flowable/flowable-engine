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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HumanTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testHumanTask() {
        Authentication.setAuthenticatedUserId("JohnDoe");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .start();
        assertThat(caseInstance).isNotNull();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");
        assertThat(task.getAssignee()).isEqualTo("JohnDoe");
        String task1Id = task.getId();
        
        List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
        assertThat(entityLinks).hasSize(1);
        EntityLink entityLink = entityLinks.get(0);
        assertThat(entityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
        assertThat(entityLink.getCreateTime()).isNotNull();
        assertThat(entityLink.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(entityLink.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(entityLink.getScopeDefinitionId()).isNull();
        assertThat(entityLink.getReferenceScopeId()).isEqualTo(task.getId());
        assertThat(entityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.TASK);
        assertThat(entityLink.getReferenceScopeDefinitionId()).isNull();
        assertThat(entityLink.getHierarchyType()).isEqualTo(HierarchyType.ROOT);

        assertThat(cmmnTaskService.getIdentityLinksForTask(task1Id))
            .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
            .containsExactlyInAnyOrder(
                tuple("assignee", "JohnDoe", null)
            );

        PlanItemInstance taskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateActive()
                .singleResult();
        assertThat(taskPlanItemInstance).isNotNull();
        assertThat(taskPlanItemInstance.getReferenceId()).isEqualTo(task.getId());
        assertThat(taskPlanItemInstance.getReferenceType()).isEqualTo(ReferenceTypes.PLAN_ITEM_CHILD_HUMAN_TASK);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricPlanItemInstance historicTaskPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceId(taskPlanItemInstance.getId())
                    .singleResult();

            assertThat(historicTaskPlanItemInstance.getReferenceId()).isEqualTo(task.getId());
            assertThat(historicTaskPlanItemInstance.getReferenceType()).isEqualTo(ReferenceTypes.PLAN_ITEM_CHILD_HUMAN_TASK);
        }

        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");
        assertThat(task.getAssignee()).isNull();
        String task2Id = task.getId();

        task = cmmnTaskService.createTaskQuery().taskCandidateGroup("test").caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        task = cmmnTaskService.createTaskQuery().taskCandidateUser("test2").caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        assertThat(cmmnTaskService.getIdentityLinksForTask(task2Id))
            .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
            .containsExactlyInAnyOrder(
                tuple("candidate", "test", null),
                tuple("candidate", "test2", null),
                tuple("candidate", null, "test")
            );

        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .variableName("var1")
                    .singleResult().getValue()).isEqualTo("JohnDoe");
            
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            for (HistoricEntityLink historicEntityLink : historicEntityLinks) {
                assertThat(historicEntityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
                assertThat(historicEntityLink.getCreateTime()).isNotNull();
                assertThat(historicEntityLink.getScopeId()).isEqualTo(caseInstance.getId());
                assertThat(historicEntityLink.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(historicEntityLink.getScopeDefinitionId()).isNull();
                assertThat(historicEntityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.TASK);
                assertThat(historicEntityLink.getReferenceScopeDefinitionId()).isNull();
                assertThat(entityLink.getHierarchyType()).isEqualTo(HierarchyType.ROOT);
            }

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getReferenceScopeId)
                    .containsExactlyInAnyOrder(task1Id, task2Id);

            assertThat(cmmnHistoryService.getHistoricIdentityLinksForTask(task1Id))
                .extracting(HistoricIdentityLink::getType, HistoricIdentityLink::getUserId, HistoricIdentityLink::getGroupId)
                .containsExactlyInAnyOrder(
                    tuple("assignee", "JohnDoe", null)
                );

            assertThat(cmmnHistoryService.getHistoricIdentityLinksForTask(task2Id))
                .extracting(HistoricIdentityLink::getType, HistoricIdentityLink::getUserId, HistoricIdentityLink::getGroupId)
                .containsExactlyInAnyOrder(
                    tuple("candidate", "test", null),
                    tuple("candidate", "test2", null),
                    tuple("candidate", null, "test")
                );
        }

        Authentication.setAuthenticatedUserId(null);
    }

    @Test
    public void testCreateHumanTaskUnderTenantByKey() {
        Authentication.setAuthenticatedUserId("JohnDoe");
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment().tenantId("flowable").
                addClasspathResource("org/flowable/cmmn/test/runtime/HumanTaskTest.testHumanTask.cmmn").deploy();
        try {
            assertThat(deployment.getTenantId()).isEqualTo("flowable");

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .tenantId("flowable")
                    .start();
            assertThat(caseInstance).isNotNull();
            assertThat(caseInstance.getTenantId()).isEqualTo("flowable");

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("Task One");
            assertThat(task.getAssignee()).isEqualTo("JohnDoe");
            assertThat(task.getTenantId()).isEqualTo("flowable");

            cmmnTaskService.complete(task.getId());

            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("Task Two");
            assertThat(task.getTenantId()).isEqualTo("flowable");
            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        } finally {
            CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, deployment.getId());
            Authentication.setAuthenticatedUserId(null);
        }

    }

    @Test
    public void testCreateHumanTaskUnderTenantById() {
        Authentication.setAuthenticatedUserId("JohnDoe");
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment().tenantId("flowable").
                addClasspathResource("org/flowable/cmmn/test/runtime/HumanTaskTest.testHumanTask.cmmn").deploy();
        try {
            assertThat(deployment.getTenantId()).isEqualTo("flowable");
            CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertThat(caseDefinition.getTenantId()).isEqualTo("flowable");

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionId(caseDefinition.getId())
                    .tenantId("flowable")
                    .start();
            assertThat(caseInstance).isNotNull();
            assertThat(caseInstance.getTenantId()).isEqualTo("flowable");

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("Task One");
            assertThat(task.getAssignee()).isEqualTo("JohnDoe");
            assertThat(task.getTenantId()).isEqualTo("flowable");

            cmmnTaskService.complete(task.getId());

            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task.getTenantId()).isEqualTo("flowable");
            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        } finally {
            CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, deployment.getId());
            Authentication.setAuthenticatedUserId(null);
        }

    }

    @Test
    @CmmnDeployment
    public void testTaskCompletionExitsStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("humanTaskCompletionExits")
                .start();
        assertThat(caseInstance).isNotNull();

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C");

        // Completing A should delete B and C
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskInstance> historicTaskInstances = cmmnHistoryService.createHistoricTaskInstanceQuery().list();
            assertThat(historicTaskInstances).hasSize(3);
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertThat(historicTaskInstance.getStartTime()).isNotNull();
                assertThat(historicTaskInstance.getEndTime()).isNotNull();
                if (!"A".equals(historicTaskInstance.getName())) {
                    assertThat(historicTaskInstance.getDeleteReason()).isEqualTo("cmmn-state-transition-terminate-case");
                }
            }
        }

        // Completing C should delete B
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("humanTaskCompletionExits")
                .start();
        assertThat(caseInstance2).isNotNull();
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).orderByTaskName().asc().list();
        cmmnTaskService.complete(tasks.get(2).getId());

        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).orderByTaskName().asc().singleResult();
        assertThat(taskA).isNotNull();
        cmmnTaskService.complete(taskA.getId());
        assertCaseInstanceEnded(caseInstance2);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskInstance> historicTaskInstances = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance2.getId()).list();
            assertThat(historicTaskInstances).hasSize(3);
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertThat(historicTaskInstance.getStartTime()).isNotNull();
                assertThat(historicTaskInstance.getEndTime()).isNotNull();
                if ("B".equals(historicTaskInstance.getName())) {
                    assertThat(historicTaskInstance.getDeleteReason()).isEqualTo("cmmn-state-transition-exit");
                }
            }
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/HumanTaskTest.testHumanTask.cmmn")
    public void addCompleteAuthenticatedUserAsParticipantToParentCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("myCase")
            .start();
        assertThat(caseInstance).isNotNull();


        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");
        assertThat(task.getAssignee()).isNull();

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId())).isEmpty();

        String prevUserId = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId("JohnDoe");
        try {
            cmmnTaskService.complete(task.getId());
        } finally {
            Authentication.setAuthenticatedUserId(prevUserId);
        }

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId()))
            .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.PARTICIPANT, "JohnDoe", null),
                tuple(IdentityLinkType.PARTICIPANT, "test", null),
                tuple(IdentityLinkType.PARTICIPANT, "test2", null)
            );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/HumanTaskTest.testHumanTaskCandidatesExpression.cmmn")
    public void humanTaskWithCollectionExpressionCandidates() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("userVar", Arrays.asList("kermit", "gonzo"))
                .transientVariable("groupVar", Collections.singletonList("management"))
                .start();

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        assertThat(cmmnTaskService.getIdentityLinksForTask(task.getId()))
                .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
                .containsExactlyInAnyOrder(
                        tuple(IdentityLinkType.CANDIDATE, "kermit", null),
                        tuple(IdentityLinkType.CANDIDATE, "gonzo", null),
                        tuple(IdentityLinkType.CANDIDATE, null, "management")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/HumanTaskTest.testHumanTaskCandidatesExpression.cmmn")
    public void humanTaskWithArrayNodeExpressionCandidates() {
        ArrayNode userVar = cmmnEngineConfiguration.getObjectMapper().createArrayNode();
        userVar.add("kermit");
        ArrayNode groupVar = cmmnEngineConfiguration.getObjectMapper().createArrayNode();
        groupVar.add("management").add("sales");
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("userVar", userVar)
                .transientVariable("groupVar", groupVar)
                .start();

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        assertThat(cmmnTaskService.getIdentityLinksForTask(task.getId()))
                .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
                .containsExactlyInAnyOrder(
                        tuple(IdentityLinkType.CANDIDATE, "kermit", null),
                        tuple(IdentityLinkType.CANDIDATE, null, "management"),
                        tuple(IdentityLinkType.CANDIDATE, null, "sales")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/HumanTaskTest.testHumanTaskIdVariableName.cmmn")
    public void testHumanTaskIdVariableName() {
        Authentication.setAuthenticatedUserId("JohnDoe");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        // Normal string
        Task firstTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task1").singleResult();
        assertThat(firstTask).isNotNull();

        String actualTaskId = firstTask.getId();
        String myTaskId = (String)cmmnRuntimeService.getVariable(caseInstance.getId(), "myTaskId");
        assertThat(myTaskId).isEqualTo(actualTaskId);

        // Expression
        Task secondTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").singleResult();
        assertThat(secondTask).isNotNull();

        actualTaskId = secondTask.getId();
        String myExpressionTaskId = (String)cmmnRuntimeService.getVariable(caseInstance.getId(), "myExpressionTaskId");
        assertThat(myExpressionTaskId).isEqualTo(actualTaskId);
    }

}
