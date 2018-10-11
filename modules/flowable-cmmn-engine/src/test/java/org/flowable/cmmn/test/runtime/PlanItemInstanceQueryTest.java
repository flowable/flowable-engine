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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceQueryTest extends FlowableCmmnTestCase {

    protected String deploymentId;
    protected String caseDefinitionId;

    @Before
    public void deployCaseDefinition() {
        this.deploymentId = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/runtime/PlanItemInstanceQueryTest.testPlanItemInstanceQuery.cmmn")
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
        assertEquals(20, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());
    }

    @Test
    public void testByCaseInstanceId() {
        List<String> caseInstanceIds = startInstances(3);
        for (String caseInstanceId : caseInstanceIds) {
            assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).list().size());
        }
    }

    @Test
    public void testByStageInstanceId() {
        startInstances(1);
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.STAGE)
            .planItemInstanceName("Stage one")
            .singleResult();
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().stageInstanceId(planItemInstance.getId()).count());
    }

    @Test
    public void testByPlanItemInstanceId() {
        startInstances(1);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).count());
        }
    }

    @Test
    public void testByElementId() {
        startInstances(4);
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItem3").list().size());
    }

    @Test
    public void testByName() {
        startInstances(9);
        assertEquals(9, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").list().size());
    }

    @Test
    public void testByState() {
        startInstances(1);
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).list().size());
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().list().size());

        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.AVAILABLE).list().size());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateAvailable().list().size());

        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).list().size());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().list().size());
    }

    @Test
    public void testByPlanItemDefinitionType() {
        startInstances(3);
        assertEquals(6, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).list().size());
        assertEquals(6, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).list().size());
    }

    @Test
    public void testByPlanItemDefinitionTypes() {
        startInstances(2);
        assertEquals(8, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.HUMAN_TASK)).list().size());
    }

    @Test
    public void testByStateAndType() {
        startInstances(3);
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemInstanceState(PlanItemInstanceState.ACTIVE)
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .list().size());

        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemInstanceState(PlanItemInstanceState.ENABLED)
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .list().size());
    }

    private List<String> startInstances(int numberOfInstances) {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < numberOfInstances; i++) {
            caseInstanceIds.add(cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanItemInstanceQuery").start().getId());
        }
        return caseInstanceIds;
    }

}
