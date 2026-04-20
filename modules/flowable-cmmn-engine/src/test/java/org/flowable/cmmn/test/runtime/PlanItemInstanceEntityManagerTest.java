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

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests methods not directly exposed on a service
 *
 * @author Joram Barrez
 */
public class PlanItemInstanceEntityManagerTest extends FlowableCmmnTestCase {

    protected String caseDefinitionId;

    @BeforeEach
    public void deployCaseDefinition() {
        String deploymentId = addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/PlanItemInstanceQueryTest.testPlanItemInstanceQuery.cmmn")
                .deploy());
        caseDefinitionId = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(deploymentId)
                .caseDefinitionKey("testPlanItemInstanceQuery")
                .singleResult()
                .getId();
    }

    @Test
    public void testFindByCaseInstanceIdAndTypeAndState() {
        List<String> caseInstanceIds = startInstances(3);
        String caseInstanceId = caseInstanceIds.get(1);

        List<PlanItemInstanceEntity> planItemInstances = ((CmmnManagementServiceImpl) cmmnManagementService).executeCommand(
                commandContext -> CommandContextUtil.getPlanItemInstanceEntityManager().findByCaseInstanceIdAndTypeAndState(caseInstanceId,
                        List.of(PlanItemDefinitionType.HUMAN_TASK), List.of(PlanItemInstanceState.ENABLED), false));

        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances.get(0).getName()).isEqualTo("B");
        
        planItemInstances = ((CmmnManagementServiceImpl) cmmnManagementService).executeCommand(
                commandContext -> CommandContextUtil.getPlanItemInstanceEntityManager().findByCaseInstanceIdAndTypeAndState(caseInstanceId,
                        null, List.of(PlanItemInstanceState.ENABLED), false));

        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances.get(0).getName()).isEqualTo("B");
        
        planItemInstances = ((CmmnManagementServiceImpl) cmmnManagementService).executeCommand(
                commandContext -> CommandContextUtil.getPlanItemInstanceEntityManager().findByCaseInstanceIdAndTypeAndState(caseInstanceId,
                        null, null, false));

        assertThat(planItemInstances).hasSize(4);
        List<String> names = new ArrayList<>();
        for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
            names.add(planItemInstance.getName());
        }
        
        assertThat(names).contains("A");
        assertThat(names).contains("B");
        assertThat(names).contains("Stage one");
        assertThat(names).contains("Stage two");
    }

    private List<String> startInstances(int numberOfInstances) {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < numberOfInstances; i++) {
            caseInstanceIds.add(cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanItemInstanceQuery").start().getId());
        }
        return caseInstanceIds;
    }

}
