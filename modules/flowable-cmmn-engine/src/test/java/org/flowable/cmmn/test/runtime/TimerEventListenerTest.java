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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.job.api.Job;
import org.flowable.variable.api.type.VariableScopeType;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class TimerEventListenerTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testTimerExpressionDuration() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        
        assertEquals(1L, cmmnManagementService.createTimerJobQuery().count());
        assertEquals(1L, cmmnManagementService.createTimerJobQuery().scopeId(caseInstance.getId()).scopeType(VariableScopeType.CMMN).count());
        assertEquals(0L, cmmnTaskService.createTaskQuery().count());
        
        Job timerJob = cmmnManagementService.createTimerJobQuery().scopeDefinitionId(caseInstance.getCaseDefinitionId()).singleResult();
        assertNotNull(timerJob);
        cmmnManagementService.moveTimerToExecutableJob(timerJob.getId());
        cmmnManagementService.executeJob(timerJob.getId());
        assertEquals(1L, cmmnTaskService.createTaskQuery().count());
    }

}
