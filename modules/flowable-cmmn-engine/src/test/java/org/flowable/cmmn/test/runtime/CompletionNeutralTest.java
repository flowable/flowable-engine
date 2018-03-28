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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.model.PlanItem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Joram Barrez
 */
public class CompletionNeutralTest extends FlowableCmmnTestCase {

    @Rule
    public TestName name = new TestName();

    @Test
    @CmmnDeployment
    public void testSimpleStageCompletion() {
        //Simple use of the UserEventListener as EntryCriteria of a Task
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);

        //Check case setup
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.ACTIVE, stageOne.getState());

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        assertEquals(PlanItemInstanceState.AVAILABLE, taskB.getState());

        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertNotNull(taskC);
        assertEquals(PlanItemInstanceState.ACTIVE, taskC.getState());

        assertCaseInstanceNotEnded(caseInstance);
        cmmnRuntimeService.triggerPlanItemInstance(taskC.getId());

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNull(stageOne);
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNull(taskB);
        taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertNull(taskC);

        assertCaseInstanceNotEnded(caseInstance);

    }

}
