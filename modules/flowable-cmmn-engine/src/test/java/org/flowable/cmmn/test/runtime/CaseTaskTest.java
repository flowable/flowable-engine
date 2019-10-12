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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Joram Barrez
 */
public class CaseTaskTest extends FlowableCmmnTestCase {

    protected String oneTaskCaseDeploymentId;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        oneTaskCaseDeploymentId = cmmnRepositoryService.createDeployment().
                tenantId("flowable").
                addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn").
                addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn").
                deploy().getId();

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
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").tenantId("flowable").start();
            this.expectedException.expect(FlowableObjectNotFoundException.class);
            this.expectedException.expectMessage("Case definition was not found by key 'oneTaskCase' and tenant 'flowable'");
            assertBlockingCaseTaskFlow(caseInstance);
        } finally {
            cmmnRepositoryService.deleteDeployment(parentCaseDeploymentId, true);
        }
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
        assertEquals(PlanItemInstanceState.ACTIVE, caseTaskPlanItemInstance.getState());

        // Verify child case instance
        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(caseInstance.getId()).singleResult();
        PlanItemInstance humanTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(childCaseInstance.getId())
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, humanTaskPlanItemInstance.getState());

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstance historicChildCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(childCaseInstance.getId()).singleResult();
            assertEquals(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemDefinitionType(PlanItemDefinitionType.CASE_TASK).singleResult().getId(), historicChildCaseInstance.getCallbackId());
            assertEquals(CallbackTypes.PLAN_ITEM_CHILD_CASE, historicChildCaseInstance.getCallbackType());
        }

        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(childCaseInstance.getId())
            .planItemDefinitionType(PlanItemDefinitionType.STAGE)
            .singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stagePlanItemInstance.getState());

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
            assertEquals("The Task", taskBeforeSubTask.getName());
            Task childTask = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertEquals(taskBeforeSubTask.getId(), childTask.getId());

            cmmnTaskService.complete(taskBeforeSubTask.getId());

            Task taskInSubTask = cmmnTaskService.createTaskQuery().singleResult();
            assertEquals("Sub task", taskInSubTask.getName());
            childTask = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertEquals(taskInSubTask.getId(), childTask.getId());

            List<HistoricTaskInstance> childTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceIdWithChildren(caseInstance.getId()).list();
            assertEquals(2, childTasks.size());
            List<String> taskIds = new ArrayList<>();
            for (HistoricTaskInstance task : childTasks) {
                taskIds.add(task.getId());
            }
            assertTrue(taskIds.contains(taskBeforeSubTask.getId()));
            assertTrue(taskIds.contains(taskInSubTask.getId()));

            cmmnTaskService.complete(taskInSubTask.getId());

            Task taskAfterSubTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertEquals("The Task2", taskAfterSubTask.getName());
            childTask = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertEquals(taskAfterSubTask.getId(), childTask.getId());

            childTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceIdWithChildren(caseInstance.getId()).list();
            assertEquals(3, childTasks.size());
            taskIds = new ArrayList<>();
            for (HistoricTaskInstance task : childTasks) {
                taskIds.add(task.getId());
            }
            assertTrue(taskIds.contains(taskBeforeSubTask.getId()));
            assertTrue(taskIds.contains(taskInSubTask.getId()));
            assertTrue(taskIds.contains(taskAfterSubTask.getId()));

        } finally {
            cmmnRepositoryService.deleteDeployment(oneHumanTaskDeploymentId, true);
        }

    }

    protected void assertBlockingCaseTaskFlow(CaseInstance caseInstance) {
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);

        List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
        assertEquals(0, entityLinks.size());

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(2, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(2, planItemInstances.size());
        assertEquals("The Case", planItemInstances.get(0).getName());
        assertEquals("The Task", planItemInstances.get(1).getName());
        assertEquals(planItemInstance.getTenantId(), planItemInstances.get(1).getTenantId());

        entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
        assertEquals(1, entityLinks.size());
        EntityLink entityLink = entityLinks.get(0);

        checkEntityLink(entityLink, caseInstance, planItemInstances.get(1).getCaseInstanceId());

        List<EntityLink> entityLinkParentsForCaseInstance = cmmnRuntimeService.getEntityLinkParentsForCaseInstance(entityLink.getReferenceScopeId());
        assertEquals(1, entityLinkParentsForCaseInstance.size());

        checkEntityLink(entityLinkParentsForCaseInstance.get(0), caseInstance, planItemInstances.get(1).getCaseInstanceId());

        // Triggering the task from the child case instance should complete the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("Task Two", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());

        List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
        assertEquals(1, historicEntityLinks.size());

        checkHistoricEntityLink(historicEntityLinks.get(0), caseInstance, planItemInstances.get(1).getCaseInstanceId());

        List<HistoricEntityLink> historicEntityLinkParentForCaseInstance = cmmnHistoryService
            .getHistoricEntityLinkParentsForCaseInstance(entityLink.getReferenceScopeId());
        assertEquals(1, historicEntityLinkParentForCaseInstance.size());

        checkHistoricEntityLink(historicEntityLinkParentForCaseInstance.get(0), caseInstance, planItemInstances.get(1).getCaseInstanceId());
    }

    private void checkEntityLink(EntityLink entityLink, CaseInstance caseInstance, String referenceScopeId) {
        assertEquals(EntityLinkType.CHILD, entityLink.getLinkType());
        assertNotNull(entityLink.getCreateTime());
        assertEquals(caseInstance.getId(), entityLink.getScopeId());
        assertEquals(ScopeTypes.CMMN, entityLink.getScopeType());
        assertNull(entityLink.getScopeDefinitionId());
        assertEquals(referenceScopeId, entityLink.getReferenceScopeId());
        assertEquals(ScopeTypes.CMMN, entityLink.getReferenceScopeType());
        assertNull(entityLink.getReferenceScopeDefinitionId());
        assertEquals(HierarchyType.ROOT, entityLink.getHierarchyType());
    }

    private void checkHistoricEntityLink(HistoricEntityLink historicEntityLink, CaseInstance caseInstance, String referenceScopeId) {
        assertEquals(EntityLinkType.CHILD, historicEntityLink.getLinkType());
        assertNotNull(historicEntityLink.getCreateTime());
        assertEquals(caseInstance.getId(), historicEntityLink.getScopeId());
        assertEquals(ScopeTypes.CMMN, historicEntityLink.getScopeType());
        assertNull(historicEntityLink.getScopeDefinitionId());
        assertEquals(referenceScopeId, historicEntityLink.getReferenceScopeId());
        assertEquals(ScopeTypes.CMMN, historicEntityLink.getReferenceScopeType());
        assertNull(historicEntityLink.getReferenceScopeDefinitionId());
        assertEquals(HierarchyType.ROOT, historicEntityLink.getHierarchyType());
    }

    // Same as testBasicBlocking(), but now with a non-blocking case task
    @Test
    @CmmnDeployment
    public void testBasicNonBlocking() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);

        // Triggering the task should start the case instance (which is non-blocking -> directly go to task two)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertEquals(2, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(2, planItemInstances.size());
        assertEquals("Task Two", planItemInstances.get(0).getName());
        assertEquals("The Task", planItemInstances.get(1).getName());

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("The Task", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testRuntimeServiceTriggerCasePlanItemInstance() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(2, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(3, planItemInstances.size());
        assertEquals("Task One", planItemInstances.get(0).getName());
        assertEquals("The Case", planItemInstances.get(1).getName());
        assertEquals("The Task", planItemInstances.get(2).getName());

        // Triggering the planitem of the case should terminate the case and go to task two
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(2, planItemInstances.size());
        assertEquals("Task One", planItemInstances.get(0).getName());
        assertEquals("Task Two", planItemInstances.get(1).getName());
    }

    @Test
    @CmmnDeployment
    public void testRuntimeServiceTriggerNonBlockingCasePlanItem() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(2, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .singleResult();
        assertEquals("Task One", planItemInstance.getName());

        // Triggering the task plan item completes the parent case, but the child case remains
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .singleResult();
        assertEquals("The Task", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithNonBlockingCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("Task One", planItemInstance.getName());

        // Terminating the parent case instance should not terminate the child (it's non-blocking)
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        // Terminate child
        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(caseInstance.getId()).singleResult();
        assertNotNull(childCaseInstance);
        HistoricCaseInstance historicChildCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceParentId(caseInstance.getId()).singleResult();
        assertNotNull(historicChildCaseInstance);

        cmmnRuntimeService.terminateCaseInstance(childCaseInstance.getId());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
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
        assertEquals("myCase", planItemInstance.getName());

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
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(4, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(4, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());

    }

    @Test
    @CmmnDeployment
    public void testFallbackToDefaultTenant() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("myCase").
            tenantId("flowable").
            overrideCaseDefinitionTenantId("flowable").
            fallbackToDefaultTenant().
            start();

        assertBlockingCaseTaskFlow(caseInstance);
        assertEquals("flowable", caseInstance.getTenantId());
    }

    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/runtime/CaseTaskTest.testGlobalFallbackToDefaultTenant.cmmn", tenantId="defaultFlowable")
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
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("myCase").
                tenantId("flowable").
                overrideCaseDefinitionTenantId("flowable").
                fallbackToDefaultTenant().
                start();

            assertBlockingCaseTaskFlow(caseInstance);
            assertEquals("flowable", caseInstance.getTenantId());

        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
            cmmnRepositoryService.deleteDeployment(tenantDeploymentId, true);
        }
    }

    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/runtime/CaseTaskTest.testGlobalFallbackToDefaultTenant.cmmn", tenantId="defaultFlowable")
    public void testGlobalFallbackToDefaultTenantNoDefinition() {
        DefaultTenantProvider originalDefaultTenantProvider = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        cmmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("myCase").
                tenantId("flowable").
                overrideCaseDefinitionTenantId("flowable").
                fallbackToDefaultTenant().
                start();

            this.expectedException.expect(FlowableObjectNotFoundException.class);
            assertBlockingCaseTaskFlow(caseInstance);

        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
        }
    }

    @Test
    @CmmnDeployment
    public void testFallbackToDefaultTenantFalse() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("myCase").
            tenantId("flowable").
            overrideCaseDefinitionTenantId("flowable").
            fallbackToDefaultTenant().
            start();

        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("Case definition was not found by key 'oneTaskCase' and tenant 'flowable'");
        assertBlockingCaseTaskFlow(caseInstance);
    }

    @Test
    public void testEntityLinksAreDeleted() {
        String deploymentId = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn").deploy().getId();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        try {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertEquals("Sub task", task.getName());

            assertEquals(1, cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId()).size());
            assertEquals(1, cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId()).size());

            cmmnTaskService.complete(task.getId());

            assertEquals(1, cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId()).size());
        } finally {
            cmmnRepositoryService.deleteDeployment(deploymentId, true);
        }

        assertEquals(0, cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId()).size());
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

        assertEquals(subCase.getId(), cmmnRuntimeService.getVariable(caseInstance.getId(), "caseIdVariable"));
    }

}
