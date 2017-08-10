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
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flowable.cmmn.engine.repository.CaseDefinition;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.runtime.MilestoneInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class RuntimeServiceTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testStartSimplePassthroughCase() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceById(caseDefinition.getId());
        assertNotNull(caseInstance);
        assertEquals(caseDefinition.getId(), caseInstance.getCaseDefinitionId());
        assertEquals(CaseInstanceState.ACTIVE, caseInstance.getState());
        assertNotNull(caseInstance.getStartTime());
     
        List<MilestoneInstance> milestoneInstances = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .orderByMilestoneName().asc()
                .list();
        assertEquals(2, milestoneInstances.size());
        assertEquals("PlanItem Milestone One", milestoneInstances.get(0).getName());
        assertEquals("PlanItem Milestone Two", milestoneInstances.get(1).getName());
    }
    
}
