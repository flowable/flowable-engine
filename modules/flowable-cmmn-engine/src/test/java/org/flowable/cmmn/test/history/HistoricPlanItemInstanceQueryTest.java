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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class HistoricPlanItemInstanceQueryTest extends FlowableCmmnTestCase {

    protected String deploymentId;
    protected String caseDefinitionId;

    @Before
    public void deployCaseDefinition() {
        this.deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/history/HistoricPlanItemInstanceQueryTest.testQuery.cmmn")
                .deploy()
                .getId();
        caseDefinitionId = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult()
                .getId();
    }

    @After
    public void deleteDeployment() {
        cmmnRepositoryService.deleteDeployment(deploymentId, true);
    }

    @Test
    public void testByCaseDefinitionId() {
        startInstances(5);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list()).hasSize(20);
    }

    @Test
    public void testByCaseInstanceId() {
        List<String> caseInstanceIds = startInstances(3);
        for (String caseInstanceId : caseInstanceIds) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstanceId).list()).hasSize(4);
        }
    }

    @Test
    public void testByStageInstanceId() {
        startInstances(1);
        HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
                .planItemInstanceName("Stage one")
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceStageInstanceId(planItemInstance.getId()).count()).isEqualTo(2);
    }

    @Test
    public void testByPlanItemInstanceId() {
        startInstances(1);
        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        for (HistoricPlanItemInstance planItemInstance : planItemInstances) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).count()).isEqualTo(1L);
        }
    }

    @Test
    public void testByElementId() {
        startInstances(4);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceElementId("planItem3").list()).hasSize(4);
    }

    @Test
    public void testByName() {
        startInstances(9);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceName("B").list()).hasSize(9);
    }

    @Test
    public void testByState() {
        startInstances(1);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.AVAILABLE).list()).hasSize(1);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).list()).hasSize(1);
    }

    @Test
    public void testByPlanItemDefinitionType() {
        startInstances(3);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK).list().size())
                .isEqualTo(6);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE).list().size())
                .isEqualTo(6);
    }

    @Test
    public void testByPlanItemDefinitionTypes() {
        startInstances(2);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.HUMAN_TASK)).list()).hasSize(8);
    }

    @Test
    public void testByStateAndType() {
        startInstances(3);
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

    @Test
    public void testOrderBy() {
        startInstances(4);

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

    private List<String> startInstances(int numberOfInstances) {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < numberOfInstances; i++) {
            caseInstanceIds.add(cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testQuery").start().getId());
        }
        return caseInstanceIds;
    }

}
