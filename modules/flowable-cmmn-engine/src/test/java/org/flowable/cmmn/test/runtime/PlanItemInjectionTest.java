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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.impl.history.HistoryLevel;
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

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        // inject new plan item into Stage A
        PlanItemInstance injectedTask = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Task A")
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .elementId(getPlanItemInstanceByName(planItemInstances, "Task A", ACTIVE).getElementId())
                .createInStage(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE));

        assertThat(injectedTask).isNotNull();
        assertThat(injectedTask.getState()).isEqualTo(ACTIVE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .active()
                .list();

        assertThat(tasks).hasSize(2);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        // test the query for the derived case definition (in this unit test, it will be the same as the running one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(caseInstance.getCaseDefinitionId())
                .list();

        assertThat(derivedPlanItems).isNotNull();
        assertThat(derivedPlanItems)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Injected Task A");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn" })
    public void testDynamicPlanItemInjectionInCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        // inject new plan item into case instance
        PlanItemInstance injectedTask = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Task A")
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .elementId(getPlanItemInstanceByName(planItemInstances, "Task A", ACTIVE).getElementId())
                .createInCase(caseInstance.getId());

        assertThat(injectedTask).isNotNull();
        assertThat(injectedTask.getState()).isEqualTo(ACTIVE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .active()
                .list();

        assertThat(tasks).hasSize(2);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        // test the query for the derived case definition (in this unit test, it will be the same as the running one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(caseInstance.getCaseDefinitionId())
                .list();

        assertThat(derivedPlanItems).isNotNull();
        assertThat(derivedPlanItems)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Injected Task A");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(1);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDerivedCaseDefinitionId(caseInstance.getCaseDefinitionId())
                .list();

            assertThat(historicPlanItemInstances).isNotNull();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getName)
                .containsExactly("Injected Task A");
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicPlanItemInjectionFromTemplate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("dynamicPlanItemCase")
                .latestVersion()
                .singleResult();

        CaseElement templateTask = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem1");

        // inject new plan item into Stage A
        PlanItemInstance injectedTask = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Task A")
                .caseDefinitionId(dynamicPlanItemCase.getId())
                .elementId(templateTask.getId())
                .createInStage(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE));

        assertThat(injectedTask).isNotNull();
        assertThat(injectedTask.getState()).isEqualTo(ACTIVE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .active()
                .list();

        assertThat(tasks).hasSize(2);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        // test the query for the derived case definition (in this unit test, it will be a different one from the running case one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

        assertThat(derivedPlanItems).isNotNull();
        assertThat(derivedPlanItems)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Injected Task A");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDerivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

            assertThat(historicPlanItemInstances).isNotNull();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getName)
                .containsExactly("Injected Task A");
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicPlanItemInjectionFromTemplateInCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("dynamicPlanItemCase")
                .latestVersion()
                .singleResult();

        CaseElement templateTask = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem1");

        // inject new plan item into case instance
        PlanItemInstance injectedTask = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Task A")
                .caseDefinitionId(dynamicPlanItemCase.getId())
                .elementId(templateTask.getId())
                .createInCase(caseInstance.getId());

        assertThat(injectedTask).isNotNull();
        assertThat(injectedTask.getState()).isEqualTo(ACTIVE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .active()
                .list();

        assertThat(tasks).hasSize(2);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        // test the query for the derived case definition (in this unit test, it will be a different one from the running case one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

        assertThat(derivedPlanItems).isNotNull();
        assertThat(derivedPlanItems)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Injected Task A");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(1);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDerivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

            assertThat(historicPlanItemInstances).isNotNull();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getName)
                .containsExactly("Injected Task A");
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionFromTemplate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("dynamicPlanItemCase")
                .latestVersion()
                .singleResult();

        CaseElement templateStage = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem5");

        // inject new plan item into Stage A
        PlanItemInstance injectedStage = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Stage")
                .caseDefinitionId(dynamicPlanItemCase.getId())
                .elementId(templateStage.getId())
                .createInStage(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE));

        assertThat(injectedStage).isNotNull();
        assertThat(injectedStage.getState()).isEqualTo(ACTIVE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", AVAILABLE);
        assertThat(getPlanItemInstanceByName(planItemInstances, "Injected Stage", ACTIVE).isStage()).isTrue();

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .active()
                .list();

        assertThat(tasks).hasSize(2);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        // test the query for the derived case definition (in this unit test, it will be a different one from the running case one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

        assertThat(derivedPlanItems).hasSize(3);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", AVAILABLE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task B", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDerivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

            assertThat(historicPlanItemInstances).hasSize(3);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionFromTemplateInCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("dynamicPlanItemCase")
                .latestVersion()
                .singleResult();

        CaseElement templateStage = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem5");

        // inject new plan item into running case
        PlanItemInstance injectedStage = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Stage")
                .caseDefinitionId(dynamicPlanItemCase.getId())
                .elementId(templateStage.getId())
                .createInCase(caseInstance.getId());

        assertThat(injectedStage).isNotNull();
        assertThat(injectedStage.getState()).isEqualTo(ACTIVE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", AVAILABLE);
        assertThat(getPlanItemInstanceByName(planItemInstances, "Injected Stage", ACTIVE).isStage()).isTrue();

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .active()
                .list();

        assertThat(tasks).hasSize(2);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Injected Task A");

        // test the query for the derived case definition (in this unit test, it will be a different one from the running case one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

        assertThat(derivedPlanItems).hasSize(3);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", AVAILABLE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Task B", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Injected Task B", ACTIVE));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDerivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

            assertThat(historicPlanItemInstances).hasSize(3);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionWithSentryFromTemplate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
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
        PlanItemInstance injectedStage = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Stage")
                .caseDefinitionId(dynamicPlanItemCase.getId())
                .elementId(templateStage.getId())
                .createInStage(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE));

        assertThat(injectedStage).isNotNull();
        assertThat(injectedStage.getState()).isEqualTo(ACTIVE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
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

        assertThat(tasks).hasSize(3);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Task X");
        assertSingleTaskExists(tasks, "Task Y");

        // test the query for the derived case definition (in this unit test, it will be a different one from the running case one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

        assertThat(derivedPlanItems).hasSize(3);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task X", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task X", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task Y", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionWithSentryFromTemplateInCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
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

        // inject new plan item into running case
        PlanItemInstance injectedStage = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Stage")
                .caseDefinitionId(dynamicPlanItemCase.getId())
                .elementId(templateStage.getId())
                .createInCase(caseInstance.getId());

        assertThat(injectedStage).isNotNull();
        assertThat(injectedStage.getState()).isEqualTo(ACTIVE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
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

        assertThat(tasks).hasSize(3);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Task X");
        assertSingleTaskExists(tasks, "Task Y");

        // test the query for the derived case definition (in this unit test, it will be a different one from the running case one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

        assertThat(derivedPlanItems).hasSize(3);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task X", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task X", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task Y", ACTIVE));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDerivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

            assertThat(historicPlanItemInstances).hasSize(3);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionWithSentryConditionFromTemplate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("dynamicPlanItemCase")
                .latestVersion()
                .singleResult();

        CaseElement templateStage = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem8");

        // inject new plan item into Stage A
        PlanItemInstance injectedStage = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Stage")
                .caseDefinitionId(dynamicPlanItemCase.getId())
                .elementId(templateStage.getId())
                .createInStage(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A", ACTIVE));

        assertThat(injectedStage).isNotNull();
        assertThat(injectedStage.getState()).isEqualTo(AVAILABLE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", AVAILABLE);
        assertThat(getPlanItemInstanceByName(planItemInstances, "Injected Stage", AVAILABLE).isStage()).isTrue();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "injectedStageEnabled", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
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

        assertThat(tasks).hasSize(3);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Task X");
        assertSingleTaskExists(tasks, "Task Y");

        // test the query for the derived case definition (in this unit test, it will be a different one from the running case one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

        assertThat(derivedPlanItems).hasSize(3);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task X", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task X", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task Y", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.testDynamicPlanItemInjection.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInjectionTest.dynamicPlanItemTemplates.cmmn" })
    public void testDynamicStagePlanItemInjectionWithSentryConditionFromTemplateInCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("dynamicPlanItemInjection").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        CaseDefinition dynamicPlanItemCase = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("dynamicPlanItemCase")
                .latestVersion()
                .singleResult();

        CaseElement templateStage = getCase(dynamicPlanItemCase.getId()).getAllCaseElements().get("planItem8");

        // inject new plan item into running case
        PlanItemInstance injectedStage = dynamicCmmnService
                .createInjectedPlanItemInstanceBuilder()
                .name("Injected Stage")
                .caseDefinitionId(dynamicPlanItemCase.getId())
                .elementId(templateStage.getId())
                .createInCase(caseInstance.getId());

        assertThat(injectedStage).isNotNull();
        assertThat(injectedStage.getState()).isEqualTo(AVAILABLE);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", AVAILABLE);
        assertThat(getPlanItemInstanceByName(planItemInstances, "Injected Stage", AVAILABLE).isStage()).isTrue();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "injectedStageEnabled", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
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

        assertThat(tasks).hasSize(3);
        assertSingleTaskExists(tasks, "Task A");
        assertSingleTaskExists(tasks, "Task X");
        assertSingleTaskExists(tasks, "Task Y");

        // test the query for the derived case definition (in this unit test, it will be a different one from the running case one)
        List<PlanItemInstance> derivedPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .derivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

        assertThat(derivedPlanItems).hasSize(3);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task X", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task X", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Injected Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task Y", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task Y", ACTIVE));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDerivedCaseDefinitionId(dynamicPlanItemCase.getId())
                .list();

            assertThat(historicPlanItemInstances).hasSize(3);
        }
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
