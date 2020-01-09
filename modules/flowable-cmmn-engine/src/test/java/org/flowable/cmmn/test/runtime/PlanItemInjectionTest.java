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

import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * Testing dynamic injection of a new plan item at runtime.
 *
 * @author Micha Kiener
 */
public class PlanItemInjectionTest extends FlowableCmmnTestCase {

    protected CmmnDeploymentManager deploymentManager;


    @Test
    @CmmnDeployment
    public void testDynamicPlanItemInjection() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        // inject new plan item into Stage A
        PlanItemInstance injectedTask = cmmnRuntimeService
            .createInjectedPlanItemInstanceBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE))
            .name("Injected Task A")
            .caseDefinitionId(caseInstance.getCaseDefinitionId())
            .elementId(getPlanItemInstanceByName(planItemInstances, "Task A", ACTIVE).getElementId())
            .create();

        assertNotNull(injectedTask);
        assertEquals(ACTIVE, injectedTask.getState());

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
            .caseInstanceId(caseInstance.getId())
            .active()
            .list();

        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(2, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
        "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicPlanItemInjectionFromTemplate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("dynamicPlanItemCase")
            .latestVersion()
            .singleResult();

        CaseElement templateTask = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem1");

        // inject new plan item into Stage A
        PlanItemInstance injectedTask = cmmnRuntimeService
            .createInjectedPlanItemInstanceBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE))
            .name("Injected Task A")
            .caseDefinitionId(dynamicPlanItemCase.getId())
            .elementId(templateTask.getId())
            .create();

        assertNotNull(injectedTask);
        assertEquals(ACTIVE, injectedTask.getState());

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
            .caseInstanceId(caseInstance.getId())
            .active()
            .list();

        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(2, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
        "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionFromTemplate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("dynamicPlanItemCase")
            .latestVersion()
            .singleResult();

        CaseElement templateStage = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem5");

        // inject new plan item into Stage A
        PlanItemInstance injectedStage = cmmnRuntimeService
            .createInjectedPlanItemInstanceBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE))
            .name("Injected Stage")
            .caseDefinitionId(dynamicPlanItemCase.getId())
            .elementId(templateStage.getId())
            .create();

        assertNotNull(injectedStage);
        assertEquals(ACTIVE, injectedStage.getState());

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", AVAILABLE);
        assertTrue(getPlanItemInstanceByName(planItemInstances, "Injected Stage", ACTIVE).isStage());

        List<Task> tasks = cmmnTaskService.createTaskQuery()
            .caseInstanceId(caseInstance.getId())
            .active()
            .list();

        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(5, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", AVAILABLE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task B", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(2, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
        "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionWithSentryFromTemplate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("dynamicPlanItemCase")
            .latestVersion()
            .singleResult();

        // already make sure the condition for the injected stage entry sentry will be satisfied when the stage will be injected
        cmmnRuntimeService.setVariable(caseInstance.getId(), "injectedStageEnabled", true);

        CaseElement templateStage = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem8");

        // inject new plan item into Stage A
        PlanItemInstance injectedStage = cmmnRuntimeService
            .createInjectedPlanItemInstanceBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE))
            .name("Injected Stage")
            .caseDefinitionId(dynamicPlanItemCase.getId())
            .elementId(templateStage.getId())
            .create();

        assertNotNull(injectedStage);
        assertEquals(ACTIVE, injectedStage.getState());

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task X", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
            .caseInstanceId(caseInstance.getId())
            .active()
            .list();

        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Task X");
        assertSingleTaskExists(tasks, "Task Y");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(5, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task X", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task X", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task Y", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(2, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
        "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionWithSentryConditionFromTemplate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("dynamicPlanItemCase")
            .latestVersion()
            .singleResult();

        CaseElement templateStage = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem8");

        // inject new plan item into Stage A
        PlanItemInstance injectedStage = cmmnRuntimeService
            .createInjectedPlanItemInstanceBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE))
            .name("Injected Stage")
            .caseDefinitionId(dynamicPlanItemCase.getId())
            .elementId(templateStage.getId())
            .create();

        assertNotNull(injectedStage);
        assertEquals(AVAILABLE, injectedStage.getState());

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", AVAILABLE);
        assertTrue(getPlanItemInstanceByName(planItemInstances, "Injected Stage", AVAILABLE).isStage());

        cmmnRuntimeService.setVariable(caseInstance.getId(), "injectedStageEnabled", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task X", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
            .caseInstanceId(caseInstance.getId())
            .active()
            .list();

        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Task X");
        assertSingleTaskExists(tasks, "Task Y");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(5, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task X", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task X", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task Y", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(2, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
    }

    @Override
    public void setupServices() {
        super.setupServices();
        CmmnEngineConfiguration cmmnEngineConfiguration = CmmnTestRunner.getCmmnEngineConfiguration();
        this.deploymentManager = cmmnEngineConfiguration.getDeploymentManager();
    }

    protected CmmnModel getCmmnModel(String caseDefinitionId) {
        CaseDefinitionCacheEntry cacheEntry = deploymentManager.getCaseDefinitionCache().get(caseDefinitionId);
        if (cacheEntry != null) {
            return cacheEntry.getCmmnModel();
        }
        deploymentManager.findDeployedCaseDefinitionById(caseDefinitionId);
        return deploymentManager.getCaseDefinitionCache().get(caseDefinitionId).getCmmnModel();
    }

    protected Case getCase(String caseDefinitionId) {
        return getCmmnModel(caseDefinitionId).getPrimaryCase();
    }
}
