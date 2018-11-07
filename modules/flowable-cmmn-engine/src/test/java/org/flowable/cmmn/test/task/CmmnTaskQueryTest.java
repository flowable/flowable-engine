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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
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
        
        for (int i=0; i<NR_CASE_INSTANCES; i++) {
            cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").variable("index", i).start();
        }
    }
    
    @Test
    public void testNoParams() {
        assertEquals(NR_CASE_INSTANCES, cmmnTaskService.createTaskQuery().count());
        assertEquals(NR_CASE_INSTANCES, cmmnTaskService.createTaskQuery().list().size());
    }
    
    @Test
    public void testQueryByCaseInstanceId() {
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        assertEquals(5, caseInstances.size());
        for (CaseInstance caseInstance : caseInstances) {
            assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list().size());
        }
    }
    
    @Test
    public void testQueryByPlanItemInstanceId() {
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        assertEquals(5, caseInstances.size());
        for (CaseInstance caseInstance : caseInstances) {
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().caseInstanceId(caseInstance.getId()).list();
            assertEquals(1, planItemInstances.size());
            assertNotNull(cmmnTaskService.createTaskQuery().planItemInstanceId(planItemInstances.get(0).getId()));
            assertNotNull(cmmnTaskService.createTaskQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceId(planItemInstances.get(0).getId()));
            assertNotNull(cmmnTaskService.createTaskQuery()
                    .caseInstanceId(caseInstance.getId())
                    .caseDefinitionId(caseInstance.getCaseDefinitionId())
                    .planItemInstanceId(planItemInstances.get(0).getId()));
        }
    }
    
    @Test
    public void testQueryByCaseDefinitionId() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertNotNull(caseDefinition);
        assertEquals(NR_CASE_INSTANCES, cmmnTaskService.createTaskQuery().caseDefinitionId(caseDefinition.getId()).list().size());
    }
    
    @Test
    public void testQueryByCmmnDeploymentId() {
        CmmnDeployment deployment = cmmnRepositoryService.createDeploymentQuery().singleResult();
        assertNotNull(deployment);
        assertEquals(NR_CASE_INSTANCES, cmmnTaskService.createTaskQuery().cmmnDeploymentId(deployment.getId()).list().size());
    }
    
    @Test
    public void testQueryByAssignee() {
        assertEquals(NR_CASE_INSTANCES, cmmnTaskService.createTaskQuery().taskAssignee("johnDoe").list().size());
    }
    
    @Test
    public void testQueryByVariableValueEquals() {
        for (int i=0; i<NR_CASE_INSTANCES; i++) {
            assertNotNull(cmmnTaskService.createTaskQuery().taskVariableValueEquals(i));
        }
    }

    @Test
    public void queryByCaseInstanceIdIncludeIdentityLinks() {
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        assertEquals(5, caseInstances.size());
        for (CaseInstance caseInstance : caseInstances) {
            assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).includeIdentityLinks().list().size());
        }
    }

    @Test
    public void queryHistoricTaskQueryByCaseInstanceIdIncludeIdentityLinks() {
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().list();
        assertEquals(5, caseInstances.size());
        for (CaseInstance caseInstance : caseInstances) {
            assertEquals(1, cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).includeIdentityLinks().list().size());
        }
    }

}
