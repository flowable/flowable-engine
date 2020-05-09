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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CaseTaskTest extends FlowableCmmnTestCase {

    protected String oneTaskCaseDeploymentId;

    @Before
    public void deployOneTaskCaseDefinition() {
        oneTaskCaseDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn").deploy().getId();
    }

    @After
    public void deleteOneTaskCaseDefinition() {
        cmmnRepositoryService.deleteDeployment(oneTaskCaseDeploymentId, true);
    }

    @Test
    @CmmnDeployment
    public void testBasicBlocking() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertBlockingCaseTaskFlow(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testCaseReferenceExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("myVar", "oneTaskCase")
                .start();
        assertBlockingCaseTaskFlow(caseInstance);
    }

    @Test
    public void testBasicBlockingWithTenant() {
        cmmnRepositoryService.deleteDeployment(oneTaskCaseDeploymentId, true);
        oneTaskCaseDeploymentId = cmmnRepositoryService.createDeployment()
                .tenantId("flowable")
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .deploy().getId();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").tenantId("flowable").start();
        assertBlockingCaseTaskFlow(caseInstance);
    }

    @Test
    public void testBasicBlockingWithTenantAndGlobalDeployment() {
        cmmnRepositoryService.deleteDeployment(oneTaskCaseDeploymentId, true);
        oneTaskCaseDeploymentId = cmmnRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn").
                deploy().getId();
        String parentCaseDeploymentId = cmmnRepositoryService.createDeployment().
                tenantId("flowable").
                addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn").
                deploy().getId();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").tenantId("flowable").start();
        assertThatThrownBy(() -> assertBlockingCaseTaskFlow(caseInstance))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Case definition was not found by key 'oneTaskCase' and tenant 'flowable'");
        cmmnRepositoryService.deleteDeployment(parentCaseDeploymentId, true);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/CaseTaskTest.testSimpleBlockingSubCase.cmmn",
            "org/flowable/cmmn/test/runtime/CaseTaskTest.testSimpleBlockingSubCaseChildCase.cmmn"
    })
    public void testSimpleBlockingSubCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("endEndCase").start();

        // Verify case task plan item instance
        PlanItemInstance caseTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.CASE_TASK)
                .singleResult();
        assertThat(caseTaskPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        // Verify child case instance
        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(caseInstance.getId()).singleResult();
        PlanItemInstance humanTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(childCaseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .singleResult();
        assertThat(humanTaskPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstance historicChildCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(childCaseInstance.getId())
                    .singleResult();
            assertThat(historicChildCaseInstance.getCallbackId()).isEqualTo(cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).planItemDefinitionType(PlanItemDefinitionType.CASE_TASK).singleResult().getId());
            assertThat(historicChildCaseInstance.getCallbackType()).isEqualTo(CallbackTypes.PLAN_ITEM_CHILD_CASE);
        }

        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(childCaseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.STAGE)
                .singleResult();
        assertThat(stagePlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        // Completing the task should complete both case instances
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(childCaseInstance.getId()).singleResult().getId());
        assertCaseInstanceEnded(childCaseInstance);
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testBasicSubHumanTask() {
        String oneHumanTaskDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn").deploy().getId();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
            Task taskBeforeSubTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(taskBeforeSubTask.getName()).isEqualTo("Task One");
            Task childTask = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(childTask.getId()).isEqualTo(taskBeforeSubTask.getId());

            cmmnTaskService.complete(taskBeforeSubTask.getId());

            Task taskInSubTask = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(taskInSubTask.getName()).isEqualTo("Sub task");
            childTask = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(childTask.getId()).isEqualTo(taskInSubTask.getId());

            List<HistoricTaskInstance> childTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceIdWithChildren(caseInstance.getId())
                    .list();
            assertThat(childTasks)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(taskBeforeSubTask.getId(), taskInSubTask.getId());

            cmmnTaskService.complete(taskInSubTask.getId());

            Task taskAfterSubTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(taskAfterSubTask.getName()).isEqualTo("Task Two");
            childTask = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(childTask.getId()).isEqualTo(taskAfterSubTask.getId());

            childTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceIdWithChildren(caseInstance.getId()).list();
            assertThat(childTasks)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(taskBeforeSubTask.getId(), taskInSubTask.getId(), taskAfterSubTask.getId());

        } finally {
            cmmnRepositoryService.deleteDeployment(oneHumanTaskDeploymentId, true);
        }

    }

    protected void assertBlockingCaseTaskFlow(CaseInstance caseInstance) {
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(1);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
        assertThat(entityLinks).isEmpty();

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(0);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("The Case", "The Task");
        assertThat(planItemInstances.get(1).getTenantId()).isEqualTo(planItemInstance.getTenantId());

        entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
        assertThat(entityLinks).hasSize(1);
        EntityLink entityLink = entityLinks.get(0);

        checkEntityLink(entityLink, caseInstance, planItemInstances.get(1).getCaseInstanceId());

        List<EntityLink> entityLinkParentsForCaseInstance = cmmnRuntimeService.getEntityLinkParentsForCaseInstance(entityLink.getReferenceScopeId());
        assertThat(entityLinkParentsForCaseInstance).hasSize(1);

        checkEntityLink(entityLinkParentsForCaseInstance.get(0), caseInstance, planItemInstances.get(1).getCaseInstanceId());

        // Triggering the task from the child case instance should complete the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Task Two");
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(2);

        List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
        assertThat(historicEntityLinks).hasSize(1);

        checkHistoricEntityLink(historicEntityLinks.get(0), caseInstance, planItemInstances.get(1).getCaseInstanceId());

        List<HistoricEntityLink> historicEntityLinkParentForCaseInstance = cmmnHistoryService
                .getHistoricEntityLinkParentsForCaseInstance(entityLink.getReferenceScopeId());
        assertThat(historicEntityLinkParentForCaseInstance).hasSize(1);

        checkHistoricEntityLink(historicEntityLinkParentForCaseInstance.get(0), caseInstance, planItemInstances.get(1).getCaseInstanceId());
    }

    private void checkEntityLink(EntityLink entityLink, CaseInstance caseInstance, String referenceScopeId) {
        assertThat(entityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
        assertThat(entityLink.getCreateTime()).isNotNull();
        assertThat(entityLink.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(entityLink.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(entityLink.getScopeDefinitionId()).isNull();
        assertThat(entityLink.getReferenceScopeId()).isEqualTo(referenceScopeId);
        assertThat(entityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(entityLink.getReferenceScopeDefinitionId()).isNull();
        assertThat(entityLink.getHierarchyType()).isEqualTo(HierarchyType.ROOT);
    }

    private void checkHistoricEntityLink(HistoricEntityLink historicEntityLink, CaseInstance caseInstance, String referenceScopeId) {
        assertThat(historicEntityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
        assertThat(historicEntityLink.getCreateTime()).isNotNull();
        assertThat(historicEntityLink.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(historicEntityLink.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(historicEntityLink.getScopeDefinitionId()).isNull();
        assertThat(historicEntityLink.getReferenceScopeId()).isEqualTo(referenceScopeId);
        assertThat(historicEntityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(historicEntityLink.getReferenceScopeDefinitionId()).isNull();
        assertThat(historicEntityLink.getHierarchyType()).isEqualTo(HierarchyType.ROOT);
    }

    // Same as testBasicBlocking(), but now with a non-blocking case task
    @Test
    @CmmnDeployment
    public void testBasicNonBlocking() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(1);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        // Triggering the task should start the case instance (which is non-blocking -> directly go to task two)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(0);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task Two", "The Task");

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("The Task");
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(2);
    }

    @Test
    @CmmnDeployment
    public void testRuntimeServiceTriggerCasePlanItemInstance() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(2);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task One", "The Case", "The Task");

        // Triggering the planitem of the case should terminate the case and go to task two
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(1);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task One", "Task Two");
    }

    @Test
    @CmmnDeployment
    public void testRuntimeServiceTriggerNonBlockingCasePlanItem() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(2);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Task One");

        // Triggering the task plan item completes the parent case, but the child case remains
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(1);

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("The Task");
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(0);
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithNonBlockingCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(2);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Task One");

        // Terminating the parent case instance should not terminate the child (it's non-blocking)
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(1);

        // Terminate child
        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(caseInstance.getId()).singleResult();
        assertThat(childCaseInstance).isNotNull();
        HistoricCaseInstance historicChildCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceParentId(caseInstance.getId())
                .singleResult();
        assertThat(historicChildCaseInstance).isNotNull();

        cmmnRuntimeService.terminateCaseInstance(childCaseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(0);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/CaseTaskTest.terminateAvailableCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn"
    })
    public void testTerminateAvailableCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("terminateAvailableCaseTask").start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.CASE_TASK)
                .planItemInstanceStateAvailable().singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("myCase");

        // When the event listener now occurs, the stage should be exited, also exiting the case task plan item
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithNestedCaseTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(4);

        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(4);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(0);

    }

    @Test
    @CmmnDeployment
    public void testFallbackToDefaultTenant() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .tenantId("flowable")
                .overrideCaseDefinitionTenantId("flowable")
                .fallbackToDefaultTenant()
                .start();

        assertBlockingCaseTaskFlow(caseInstance);
        assertThat(caseInstance.getTenantId()).isEqualTo("flowable");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseTaskTest.testGlobalFallbackToDefaultTenant.cmmn", tenantId = "defaultFlowable")
    public void testGlobalFallbackToDefaultTenant() {
        String tenantDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .tenantId("defaultFlowable")
                .deploy()
                .getId();

        DefaultTenantProvider originalDefaultTenantProvider = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        cmmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .tenantId("flowable")
                    .overrideCaseDefinitionTenantId("flowable")
                    .fallbackToDefaultTenant()
                    .start();

            assertBlockingCaseTaskFlow(caseInstance);
            assertThat(caseInstance.getTenantId()).isEqualTo("flowable");

        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
            cmmnRepositoryService.deleteDeployment(tenantDeploymentId, true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseTaskTest.testGlobalFallbackToDefaultTenant.cmmn", tenantId = "defaultFlowable")
    public void testGlobalFallbackToDefaultTenantNoDefinition() {
        DefaultTenantProvider originalDefaultTenantProvider = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        cmmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .tenantId("flowable")
                    .overrideCaseDefinitionTenantId("flowable")
                    .fallbackToDefaultTenant()
                    .start();
            assertThatThrownBy(() -> assertBlockingCaseTaskFlow(caseInstance))
                    .isInstanceOf(FlowableObjectNotFoundException.class);
        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
        }
    }

    @Test
    @CmmnDeployment
    public void testFallbackToDefaultTenantFalse() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .tenantId("flowable")
                .overrideCaseDefinitionTenantId("flowable")
                .fallbackToDefaultTenant()
                .start();
        assertThatThrownBy(() -> assertBlockingCaseTaskFlow(caseInstance))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Case definition was not found by key 'oneTaskCase' and tenant 'flowable'");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/CaseTaskTest.testSameDeployment.cmmn",
            "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn"
    })
    public void testSameDeployment() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        CaseInstance childCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceParentId(caseInstance.getId())
                .singleResult();

        assertThat(childCase).isNotNull();

        PlanItemInstance childPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(childCase.getId())
                .singleResult();

        assertThat(childPlanItemInstance).isNotNull();
        assertThat(childPlanItemInstance.getName()).isEqualTo("The Task");

        String v2Deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCaseV2.cmmn")
                .deploy()
                .getId();

        try {
            // Starting after V2 deployment should use the same deployment task
            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .start();

            planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .singleResult();
            assertThat(planItemInstance).isNotNull();

            // Triggering the task should start the child case instance
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

            childCase = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceParentId(caseInstance.getId())
                    .singleResult();

            assertThat(childCase).isNotNull();

            childPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(childCase.getId())
                    .singleResult();

            assertThat(childPlanItemInstance).isNotNull();
            assertThat(childPlanItemInstance.getName()).isEqualTo("The Task");
        } finally {
            cmmnRepositoryService.deleteDeployment(v2Deployment, true);
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/CaseTaskTest.testSameDeployment.cmmn",
            "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn"
    }, tenantId = "flowable")
    public void testSameDeploymentDifferentTenants() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .tenantId("flowable")
                .start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        CaseInstance childCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceParentId(caseInstance.getId())
                .singleResult();

        assertThat(childCase).isNotNull();
        assertThat(childCase.getTenantId()).isEqualTo("flowable");

        PlanItemInstance childPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(childCase.getId())
                .singleResult();

        assertThat(childPlanItemInstance).isNotNull();
        assertThat(childPlanItemInstance.getName()).isEqualTo("The Task");

        String v2Deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCaseV2.cmmn")
                .tenantId("flowable")
                .deploy()
                .getId();

        try {
            // Starting after V2 deployment should use the same deployment task
            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .tenantId("flowable")
                    .start();

            planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .singleResult();
            assertThat(planItemInstance).isNotNull();

            // Triggering the task should start the child case instance
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

            childCase = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceParentId(caseInstance.getId())
                    .singleResult();

            assertThat(childCase).isNotNull();
            assertThat(childCase.getTenantId()).isEqualTo("flowable");

            childPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(childCase.getId())
                    .singleResult();

            assertThat(childPlanItemInstance).isNotNull();
            assertThat(childPlanItemInstance.getName()).isEqualTo("The Task");
        } finally {
            cmmnRepositoryService.deleteDeployment(v2Deployment, true);
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/CaseTaskTest.testSameDeploymentGlobal.cmmn",
            "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn"
    })
    public void testGlobalSameDeployment() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        CaseInstance childCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceParentId(caseInstance.getId())
                .singleResult();

        assertThat(childCase).isNotNull();

        PlanItemInstance childPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(childCase.getId())
                .singleResult();

        assertThat(childPlanItemInstance).isNotNull();
        assertThat(childPlanItemInstance.getName()).isEqualTo("The Task");

        String v2Deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCaseV2.cmmn")
                .deploy()
                .getId();

        try {
            // Starting after V2 deployment should not use the same deployment task
            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .start();

            planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .singleResult();
            assertThat(planItemInstance).isNotNull();

            // Triggering the task should start the child case instance
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

            childCase = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceParentId(caseInstance.getId())
                    .singleResult();

            assertThat(childCase).isNotNull();

            childPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(childCase.getId())
                    .singleResult();

            assertThat(childPlanItemInstance).isNotNull();
            assertThat(childPlanItemInstance.getName()).isEqualTo("The Task V2");
        } finally {
            cmmnRepositoryService.deleteDeployment(v2Deployment, true);
        }
    }

    @Test
    @CmmnDeployment
    public void testSameDeploymentFalse() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        CaseInstance childCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceParentId(caseInstance.getId())
                .singleResult();

        assertThat(childCase).isNotNull();

        PlanItemInstance childPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(childCase.getId())
                .singleResult();

        assertThat(childPlanItemInstance).isNotNull();
        assertThat(childPlanItemInstance.getName()).isEqualTo("The Task");

        String v2Deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCaseV2.cmmn")
                .deploy()
                .getId();

        try {
            // Starting after V2 deployment should not use the same deployment task
            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .start();

            planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .singleResult();
            assertThat(planItemInstance).isNotNull();

            // Triggering the task should start the child case instance
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

            childCase = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceParentId(caseInstance.getId())
                    .singleResult();

            assertThat(childCase).isNotNull();

            childPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(childCase.getId())
                    .singleResult();

            assertThat(childPlanItemInstance).isNotNull();
            assertThat(childPlanItemInstance.getName()).isEqualTo("The Task V2");
        } finally {
            cmmnRepositoryService.deleteDeployment(v2Deployment, true);
        }

    }

    @Test
    public void testEntityLinksAreDeleted() {
        String deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn").deploy().getId();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        try {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("Sub task");

            assertThat(cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId())).hasSize(1);
            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).hasSize(1);

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).hasSize(1);
        } finally {
            cmmnRepositoryService.deleteDeployment(deploymentId, true);
        }

        assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testIOParameters() {
        String innerCaseDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
                .deploy()
                .getId();
        String inVariableContent = "Variable Test Content In";
        String outVariableContent = "Variable Test Content Out";
        CaseInstance outerCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("outerCase")
                .variable("testContentOuterTaskIn", inVariableContent)
                .start();

        CaseInstance innerCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("oneHumanTaskCase")
                .includeCaseVariables()
                .singleResult();

        assertThat(innerCaseInstance.getCaseVariables())
                .isNotNull()
                .containsExactlyEntriesOf(Collections.singletonMap("testContentInnerTaskIn", inVariableContent));

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(innerCaseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId(), Collections.singletonMap("testContentInnerTaskOut", outVariableContent));

        Map<String, Object> outerCaseVariables = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(outerCaseInstance.getId())
                .includeCaseVariables()
                .singleResult()
                .getCaseVariables();

        assertThat(outerCaseVariables)
                .isNotNull()
                .containsEntry("testContentOuterTaskOut", outVariableContent);
        cmmnRepositoryService.deleteDeployment(innerCaseDeploymentId, true);
    }

    @Test
    @CmmnDeployment
    public void testWithSpecifiedBusinessKey() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        CaseInstance subCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("oneTaskCase")
                .singleResult();

        assertThat(subCase)
                .isNotNull()
                .extracting(CaseInstance::getBusinessKey)
                .isEqualTo("myBusinessKey");

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(subCase.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
    }

    @Test
    @CmmnDeployment
    public void testWithSpecifiedBusinessKeyAndInheritBusinessKey() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("dummyBusinessKey")
                .start();

        CaseInstance subCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("oneTaskCase")
                .singleResult();

        assertThat(subCase)
                .isNotNull()
                .extracting(CaseInstance::getBusinessKey)
                .isEqualTo("myBusinessKey");

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(subCase.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
    }

    @Test
    @CmmnDeployment
    public void testWithInheritBusinessKey() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("myBusinessKey")
                .start();

        CaseInstance subCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("oneTaskCase")
                .singleResult();

        assertThat(subCase)
                .isNotNull()
                .extracting(CaseInstance::getBusinessKey)
                .isEqualTo("myBusinessKey");

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(subCase.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseTaskTest.testWithInheritBusinessKey.cmmn")
    public void testWithInheritBusinessKeyButWithoutBusinessKey() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        CaseInstance subCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("oneTaskCase")
                .singleResult();

        assertThat(subCase)
                .isNotNull()
                .extracting(CaseInstance::getBusinessKey)
                .isNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(subCase.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
    }

    @Test
    @CmmnDeployment
    public void testIdVariableName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIdVariableName")
                .start();

        CaseInstance subCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("oneTaskCase")
                .singleResult();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "caseIdVariable")).isEqualTo(subCase.getId());
    }

    @Test
    @CmmnDeployment
    public void testIdVariableNameExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIdVariableName")
                .variable("idVariableName", "test")
                .start();

        CaseInstance subCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("oneTaskCase")
                .singleResult();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "test")).isEqualTo(subCase.getId());
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/CaseTaskTest.testThreeLevelRootCase.cmmn",
            "org/flowable/cmmn/test/runtime/CaseTaskTest.testThreeLevelLevel1Case.cmmn",
            "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn"
    })
    public void testThreeLevelCase() {
        CaseInstance rootCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("rootCase")
                .start();

        String rootCaseId = rootCase.getId();

        String level1CaseId = (String) rootCase.getCaseVariables().get("caseIdVariable");
        String oneHumanTaskCaseId = (String) cmmnRuntimeService.getVariable(level1CaseId, "caseIdVariable");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(rootCaseId).singleResult();
        assertThat(task.getScopeId()).isEqualTo(oneHumanTaskCaseId);

        List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(rootCaseId);

        assertThat(entityLinks)
                .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                        EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(rootCaseId, ScopeTypes.CMMN, HierarchyType.ROOT, level1CaseId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                        tuple(rootCaseId, ScopeTypes.CMMN, HierarchyType.ROOT, oneHumanTaskCaseId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                        tuple(rootCaseId, ScopeTypes.CMMN, HierarchyType.ROOT, task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                );

        assertThat(entityLinks)
                .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                .containsOnly(
                        tuple(rootCaseId, ScopeTypes.CMMN)
                );

        entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(level1CaseId);

        assertThat(entityLinks)
                .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                        EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(level1CaseId, ScopeTypes.CMMN, HierarchyType.PARENT, oneHumanTaskCaseId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                        tuple(level1CaseId, ScopeTypes.CMMN, HierarchyType.GRAND_PARENT, task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                );

        assertThat(entityLinks)
                .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                .containsOnly(
                        tuple(rootCaseId, ScopeTypes.CMMN)
                );

        entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(oneHumanTaskCaseId);

        assertThat(entityLinks)
                .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                        EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(oneHumanTaskCaseId, ScopeTypes.CMMN, HierarchyType.PARENT, task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                );
        
        List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(rootCaseId);

        assertThat(historicEntityLinks)
                .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                        HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(rootCaseId, ScopeTypes.CMMN, HierarchyType.ROOT, level1CaseId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                        tuple(rootCaseId, ScopeTypes.CMMN, HierarchyType.ROOT, oneHumanTaskCaseId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                        tuple(rootCaseId, ScopeTypes.CMMN, HierarchyType.ROOT, task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                );

        assertThat(historicEntityLinks)
                .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                .containsOnly(
                        tuple(rootCaseId, ScopeTypes.CMMN)
                );

        historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(level1CaseId);

        assertThat(historicEntityLinks)
                .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                        HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(level1CaseId, ScopeTypes.CMMN, HierarchyType.PARENT, oneHumanTaskCaseId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                        tuple(level1CaseId, ScopeTypes.CMMN, HierarchyType.GRAND_PARENT, task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                );

        assertThat(historicEntityLinks)
                .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                .containsOnly(
                        tuple(rootCaseId, ScopeTypes.CMMN)
                );

        historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(oneHumanTaskCaseId);

        assertThat(historicEntityLinks)
                .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                        HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(oneHumanTaskCaseId, ScopeTypes.CMMN, HierarchyType.PARENT, task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                );

        assertThat(historicEntityLinks)
                .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                .containsOnly(
                        tuple(rootCaseId, ScopeTypes.CMMN)
                );
    }

}
