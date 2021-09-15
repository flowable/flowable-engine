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
package org.flowable.cmmn.test.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.junit.Test;

public class CaseCompletionOnStageListenerTest extends CustomCmmnConfigurationFlowableTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/Multi_Stage_Instance_Listener_Test_Case.cmmn.xml")
    public void testStageACompletionWithoutActivatingStageB() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("multiStageInstanceListenerTestCase")
            .start();
        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).isNotNull().hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/Multi_Stage_Instance_Listener_Test_Case.cmmn.xml")
    public void testStageBActivation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("multiStageInstanceListenerTestCase")
            .start();
        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).isNotNull().hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        cmmnRuntimeService.setVariable(caseInstance.getId(), "nextStep", "stageB");
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).isNotNull().hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        assertCaseInstanceEnded(caseInstance);
    }

    @Override
    protected String getEngineName() {
        return this.getClass().getName();
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        Map<Object, Object> beans = new HashMap<>();
        beans.put("testListener", new VariableReadingLifecycleListener(cmmnEngineConfiguration.getCmmnRuntimeService()));
        cmmnEngineConfiguration.setBeans(beans);
    }

    static class VariableReadingLifecycleListener {
        protected final CmmnRuntimeService caseService;
        VariableReadingLifecycleListener(CmmnRuntimeService caseService) {
            this.caseService = caseService;
        }

        public void readVariable(PlanItemInstance planItemInstance, String var) {
            // If this method gets invoked on the cmmn runtime service, it creates a new context which will lead to the case completion at the end of the context
            caseService.getVariable(planItemInstance.getCaseInstanceId(), var);
        }

    }
}
