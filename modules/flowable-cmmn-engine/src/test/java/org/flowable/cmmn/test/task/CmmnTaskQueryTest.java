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
package org.flowable.cmmn.test.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnTaskQueryTest extends FlowableCmmnTestCase {

    private static final int NR_CASE_INSTANCES = 5;

    @Before
    public void createCaseInstance() {
        deployOneHumanTaskCaseModel();

        for (int i = 0; i < NR_CASE_INSTANCES; i++) {
            cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").variable("index", i).start();
        }
    }

    @Test
    public void testNoParams() {
        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(NR_CASE_INSTANCES);
        assertThat(cmmnTaskService.createTaskQuery().list()).hasSize(NR_CASE_INSTANCES);
    }

    @Test
    public void testQueryByCaseInstanceId() {
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        assertThat(caseInstances).hasSize(5);
        for (CaseInstance caseInstance : caseInstances) {
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
        }
    }

    @Test
    public void testQueryByPlanItemInstanceId() {
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        assertThat(caseInstances).hasSize(5);
        for (CaseInstance caseInstance : caseInstances) {
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive()
                    .caseInstanceId(caseInstance.getId()).list();
            assertThat(planItemInstances).hasSize(1);
            assertThat(cmmnTaskService.createTaskQuery().planItemInstanceId(planItemInstances.get(0).getId())).isNotNull();
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).planItemInstanceId(planItemInstances.get(0).getId())).
                    isNotNull();
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).caseDefinitionId(caseInstance.getCaseDefinitionId())
                    .planItemInstanceId(planItemInstances.get(0).getId()))
                    .isNotNull();
        }
    }

    @Test
    public void testQueryByCaseDefinitionId() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().caseDefinitionId(caseDefinition.getId()).list()).hasSize(NR_CASE_INSTANCES);
    }

    @Test
    public void testQueryByCaseDefinitionKey() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().caseDefinitionKey(caseDefinition.getKey()).list()).hasSize(NR_CASE_INSTANCES);
    }

    @Test
    public void testQueryByCaseDefinitionKeyLike() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().caseDefinitionKeyLike("oneTask%").list()).hasSize(NR_CASE_INSTANCES);
    }

    public void testQueryByCaseDefinitionKeyLikeIgnoreCase() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().caseDefinitionKeyLikeIgnoreCase("onetask%").list()).hasSize(NR_CASE_INSTANCES);
    }

    @Test
    public void testQueryByCaseDefinitionKeyIn() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().caseDefinitionKeyIn(Arrays.asList(caseDefinition.getKey(),"dummyKey")).list())
                .hasSize(NR_CASE_INSTANCES);
    }

    @Test
    public void testQueryByCmmnDeploymentId() {
        CmmnDeployment deployment = cmmnRepositoryService.createDeploymentQuery().singleResult();
        assertThat(deployment).isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().cmmnDeploymentId(deployment.getId()).list()).hasSize(NR_CASE_INSTANCES);
    }

    @Test
    public void testQueryByAssignee() {
        assertThat(cmmnTaskService.createTaskQuery().taskAssignee("johnDoe").list()).hasSize(NR_CASE_INSTANCES);
        
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").list();
        assertThat(caseInstances.size()).isEqualTo(5);
        
        String caseInstanceId = caseInstances.get(0).getId();
        
        Task task = cmmnTaskService.createTaskQuery().taskAssignee("johnDoe").caseInstanceId(caseInstanceId).singleResult();
        assertThat(task.getAssignee()).isEqualTo("johnDoe");
        assertThat(task.getScopeId()).isEqualTo(caseInstanceId);
        assertThat(task.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(task.getScopeDefinitionId()).isEqualTo(caseInstances.get(0).getCaseDefinitionId());
        
        task = cmmnTaskService.createTaskQuery().taskAssignee("johnDoe").caseInstanceId(caseInstanceId).includeTaskLocalVariables().singleResult();
        assertThat(task.getAssignee()).isEqualTo("johnDoe");
        assertThat(task.getScopeId()).isEqualTo(caseInstanceId);
        assertThat(task.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(task.getScopeDefinitionId()).isEqualTo(caseInstances.get(0).getCaseDefinitionId());
        
        task = cmmnTaskService.createTaskQuery().taskAssignee("johnDoe").caseInstanceId(caseInstanceId).includeProcessVariables().singleResult();
        assertThat(task.getAssignee()).isEqualTo("johnDoe");
        assertThat(task.getScopeId()).isEqualTo(caseInstanceId);
        assertThat(task.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(task.getScopeDefinitionId()).isEqualTo(caseInstances.get(0).getCaseDefinitionId());
    }

    @Test
    public void testQueryByVariableValueEquals() {
        for (int i = 0; i < NR_CASE_INSTANCES; i++) {
            assertThat(cmmnTaskService.createTaskQuery().taskVariableValueEquals(i)).isNotNull();
        }
    }

    @Test
    public void queryByCaseInstanceIdIncludeIdentityLinks() {
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        assertThat(caseInstances).hasSize(5);
        for (CaseInstance caseInstance : caseInstances) {
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).includeIdentityLinks().list()).hasSize(1);
        }
    }

    @Test
    public void queryHistoricTaskQueryByCaseInstanceIdIncludeIdentityLinks() {
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        assertThat(caseInstances).hasSize(5);
        for (CaseInstance caseInstance : caseInstances) {
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).includeIdentityLinks().list()).hasSize(1);
        }
    }

    @Test
    public void testHistoricTaskQueryByCaseDefinitionKey(){
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseDefinitionKey(caseDefinition.getKey()).list()).hasSize(NR_CASE_INSTANCES);
    }

    @Test
    public void testHistoricTaskQueryByCaseDefinitionKeyLike() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseDefinitionKeyLike("oneTask%").list()).hasSize(NR_CASE_INSTANCES);
    }

    public void testHistoricTaskQueryByCaseDefinitionKeyLikeIgnoreCase() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseDefinitionKeyLikeIgnoreCase("onetask%").list()).hasSize(NR_CASE_INSTANCES);
    }

    @Test
    public void testHistoricTaskQueryByCaseDefinitionKeyIn() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseDefinitionKeyIn(Arrays.asList(caseDefinition.getKey(),"dummyKey")).list())
                .hasSize(NR_CASE_INSTANCES);
    }
}
