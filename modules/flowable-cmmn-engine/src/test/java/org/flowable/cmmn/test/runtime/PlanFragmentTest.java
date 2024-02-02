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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanFragmentTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testPlanFragmentsHaveNoRuntimeImpact() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanFragments").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getName)
            .containsOnly("A", "B", "C", "Stage one", "D", "E", "F", "Stage two", "G", "Stage three", "H");

        assertPlanItemInstanceState(planItemInstances, "A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "B", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "C", PlanItemInstanceState.AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage one", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "D", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "E", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "F", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage two", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "G", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage three", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "H", PlanItemInstanceState.ACTIVE);
    }

    @Test
    @CmmnDeployment
    public void testPlanFragmentsHaveNoRuntimeImpact2() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanFragments2").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getName)
            .containsOnly("A", "Stage1", "C", "D");

        assertPlanItemInstanceState(planItemInstances, "A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage1", PlanItemInstanceState.AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "C", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "D", PlanItemInstanceState.ACTIVE);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("A").singleResult().getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getName)
            .containsOnly("B", "Stage1", "C", "D");

        assertPlanItemInstanceState(planItemInstances, "Stage1", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "B", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "C", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "D", PlanItemInstanceState.ACTIVE);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("B").singleResult().getId());
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("C").singleResult().getId());
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("D").singleResult().getId());

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testEmptyPlanFragments() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanFragments3").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getName)
            .containsOnly("A", "Stage1", "C");

        assertPlanItemInstanceState(planItemInstances, "A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage1", PlanItemInstanceState.AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "C", PlanItemInstanceState.ACTIVE);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("A").singleResult().getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getName)
            .containsOnly("B", "Stage1", "C");

        assertPlanItemInstanceState(planItemInstances, "Stage1", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "B", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "C", PlanItemInstanceState.ACTIVE);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("B").singleResult().getId());
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("C").singleResult().getId());

        assertCaseInstanceEnded(caseInstance);
    }


}
