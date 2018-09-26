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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
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
        assertEquals(20, cmmnHistoryService.createHistoricPlanItemInstanceQuery().list().size());
    }

    @Test
    public void testByCaseInstanceId() {
        List<String> caseInstanceIds = startInstances(3);
        for (String caseInstanceId : caseInstanceIds) {
            assertEquals(4, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstanceId).list().size());
        }
    }

    @Test
    public void testByStageInstanceId() {
        startInstances(1);
        HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
            .planItemInstanceName("Stage one")
            .singleResult();
        assertNotNull(planItemInstance);
        assertEquals(2, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceStageInstanceId(planItemInstance.getId()).count());
    }

    @Test
    public void testByPlanItemInstanceId() {
        startInstances(1);
        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        for (HistoricPlanItemInstance planItemInstance : planItemInstances) {
            assertEquals(1L, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).count());
        }
    }

    @Test
    public void testByElementId() {
        startInstances(4);
        assertEquals(4, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceElementId("planItem3").list().size());
    }

    @Test
    public void testByName() {
        startInstances(9);
        assertEquals(9, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceName("B").list().size());
    }

    @Test
    public void testByState() {
        startInstances(1);
        assertEquals(2, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).list().size());
        assertEquals(1, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.AVAILABLE).list().size());
        assertEquals(1, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).list().size());
    }

    @Test
    public void testByPlanItemDefinitionType() {
        startInstances(3);
        assertEquals(6, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK).list().size());
        assertEquals(6, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE).list().size());
    }

    @Test
    public void testByPlanItemDefinitionTypes() {
        startInstances(2);
        assertEquals(8, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.HUMAN_TASK)).list().size());
    }

    @Test
    public void testByStateAndType() {
        startInstances(3);
        assertEquals(3, cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceState(PlanItemInstanceState.ACTIVE)
            .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .list().size());

        assertEquals(3, cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceState(PlanItemInstanceState.ENABLED)
            .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .list().size());
    }

    private List<String> startInstances(int numberOfInstances) {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < numberOfInstances; i++) {
            caseInstanceIds.add(cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testQuery").start().getId());
        }
        return caseInstanceIds;
    }

}
