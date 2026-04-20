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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.PlanItemLocalizationManager;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class HistoricPlanItemInstanceQueryTest extends FlowableCmmnTestCase {

    protected String deploymentId;
    protected String caseDefinitionId;

    @BeforeEach
    public void deployCaseDefinition() {
        deploymentId = addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/history/HistoricPlanItemInstanceQueryTest.testQuery.cmmn")
                .deploy());
        caseDefinitionId = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult()
                .getId();
    }

    @Test
    public void testByCaseDefinitionId() {
        startInstances(5);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list()).hasSize(20);
        }
    }

    @Test
    public void testByCaseInstanceId() {
        List<String> caseInstanceIds = startInstances(3);
        for (String caseInstanceId : caseInstanceIds) {
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstanceId).list()).hasSize(4);
            }
        }
    }

    @Test
    public void testByCaseInstanceIds() {
        List<String> caseInstanceIds = startInstances(3);
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceIds(Set.of(caseInstanceIds.get(0),caseInstanceIds.get(1))).list()).hasSize(8);
        }
    }

    @Test
    public void testByStageInstanceId() {
        startInstances(1);
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
                .planItemInstanceName("Stage one")
                .singleResult();
            assertThat(planItemInstance).isNotNull();
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceStageInstanceId(planItemInstance.getId()).count()).isEqualTo(2);
        }
    }

    @Test
    public void testByPlanItemInstanceId() {
        startInstances(1);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
            for (HistoricPlanItemInstance planItemInstance : planItemInstances) {
                assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).count()).isEqualTo(1);
            }
        }
    }

    @Test
    public void testByElementId() {
        startInstances(4);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceElementId("planItem3").list()).hasSize(4);
        }
    }

    @Test
    public void testByName() {
        startInstances(9);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceName("B").list()).hasSize(9);
        }
    }

    @Test
    public void testByState() {
        startInstances(1);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.AVAILABLE).list()).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).list()).hasSize(1);
        }
    }

    @Test
    public void testByPlanItemDefinitionType() {
        startInstances(3);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK).list()).hasSize(6);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE).list()).hasSize(6);
        }
    }

    @Test
    public void testByPlanItemDefinitionTypes() {
        startInstances(2);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.HUMAN_TASK)).list()).hasSize(8);
        }
    }

    @Test
    public void testByStateAndType() {
        startInstances(3);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list())
                .hasSize(3);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ENABLED)
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list())
                .hasSize(3);
        }
    }

    @Test
    public void testByAssignee() {
        startInstances(2);

        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list();
        assertThat(planItemInstances).hasSize(4);

        List<Task> tasks = cmmnTaskService.createTaskQuery().list();
        for (Task task : tasks) {
            cmmnTaskService.setAssignee(task.getId(), "gonzo");
        }

        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceAssignee("gonzo")
                .list();
        assertThat(planItemInstances).hasSize(2);

        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceAssignee("johnDoe")
                .list();
        assertThat(planItemInstances).hasSize(0);

    }

    @Test
    public void testByCompletedBy() {
        startInstances(3);

        List<Task> tasks = cmmnTaskService.createTaskQuery().list();
        for (Task task : tasks) {
            cmmnTaskService.setAssignee(task.getId(), "gonzo");
            cmmnTaskService.complete(task.getId(), "kermit");
        }

        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceAssignee("gonzo")
                .list();
        assertThat(planItemInstances).hasSize(3);

        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceCompletedBy("kermit")
                .list();
        assertThat(planItemInstances).hasSize(3);

        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceCompletedBy("johnDoe")
                .list();
        assertThat(planItemInstances).hasSize(0);

    }

    @Test
    public void testOrderBy() {
        startInstances(4);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByName().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByName().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByCreateTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByCreateTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastAvailableTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastAvailableTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastEnabledTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastEnabledTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastDisabledTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastDisabledTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastStartedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastStartedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastSuspendedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastSuspendedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByCompletedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByCompletedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByOccurredTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByOccurredTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByTerminatedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByTerminatedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByExitTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByExitTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByEndedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByEndedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastUpdatedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastUpdatedTime().desc().list()).hasSize(16);
        }
    }

    @Test
    public void testLocalization() {
        startInstances(1);

        cmmnEngineConfiguration.setPlanItemLocalizationManager(new PlanItemLocalizationManager() {
            @Override
            public void localize(PlanItemInstance planItemInstance, String locale, boolean withLocalizationFallback) {

            }

            @Override
            public void localize(HistoricPlanItemInstance historicPlanItemInstance, String locale, boolean withLocalizationFallback) {
                if ("pt".equals(locale)) {
                    historicPlanItemInstance.setLocalizedName("Plano traduzido");
                }
            }
        });

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getName)
                    .containsExactlyInAnyOrder(
                            "Stage one",
                            "Stage two",
                            "A",
                            "B"
                    );
    
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().locale("pt").list())
                    .extracting(HistoricPlanItemInstance::getName)
                    .containsExactlyInAnyOrder(
                            "Plano traduzido",
                            "Plano traduzido",
                            "Plano traduzido",
                            "Plano traduzido"
                    );
        }
    }

    @Test
    public void testIncludeLocalVariables() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testQuery")
                .variable("caseVar","caseVarValur")
                .name("With string value")
                .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();

        cmmnRuntimeService.setLocalVariable(planItemInstances.get(0).getId(), "localVar", "someValue");

        Task task = cmmnTaskService.createTaskQuery()
                .includeCaseVariables()
                .includeTaskLocalVariables()
                .singleResult();

        cmmnTaskService.complete(task.getId());
        HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceId(planItemInstances.get(0).getId()).singleResult();
        assertThat(planItemInstance.getPlanItemInstanceLocalVariables()).isEmpty();

        planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(planItemInstances.get(0).getId()).includeLocalVariables()
                .singleResult();
        assertThat(planItemInstance.getPlanItemInstanceLocalVariables()).isNotNull();

        assertThat(planItemInstance.getPlanItemInstanceLocalVariables()).containsOnly(
                entry("localVar", "someValue")
        );
    }

    private List<String> startInstances(int numberOfInstances) {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < numberOfInstances; i++) {
            caseInstanceIds.add(cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testQuery").start().getId());
        }
        return caseInstanceIds;
    }

}
