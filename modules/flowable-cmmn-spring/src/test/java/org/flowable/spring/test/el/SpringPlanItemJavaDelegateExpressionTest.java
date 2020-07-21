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
package org.flowable.spring.test.el;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.behavior.CmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class SpringPlanItemJavaDelegateExpressionTest {

    @Rule
    public FlowableCmmnRule cmmnRule = new FlowableCmmnRule("org/flowable/spring/test/el/SpringBeanTest-context.xml");

    @Test
    @CmmnDeployment
    public void testCmmnTriggerableActivityBehaviorDelegateExpression() {
        CmmnRuntimeService cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("myCase")
            .start();

        // The service task here acts like a wait state.
        // When the case instance is started, it will wait and be in state ACTIVE.

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        // When triggered, the plan item will complete
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId()).trigger();

        assertThat(cmmnRule.getCmmnHistoryService().createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).singleResult().getState())
            .isEqualTo(PlanItemInstanceState.COMPLETED);
    }

    @Test
    @CmmnDeployment
    public void testPlanItemJavaDelegateExpression() {
        CmmnRuntimeService cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("myCase")
            .start();

        assertThat(cmmnRule.getCmmnHistoryService().createHistoricVariableInstanceQuery()
            .caseInstanceId(caseInstance.getId()).singleResult().getValue()).isEqualTo(true);
    }

    public static class TestJavaDelegate01 implements CmmnTriggerableActivityBehavior {

        @Override
        public void execute(DelegatePlanItemInstance delegatePlanItemInstance) {
            // Do nothing, wait state
        }

        @Override
        public void trigger(DelegatePlanItemInstance planItemInstance) {
            CommandContextUtil.getAgenda().planCompletePlanItemInstanceOperation((PlanItemInstanceEntity) planItemInstance);
        }

    }

    public static class TestJavaDelegate02 implements PlanItemJavaDelegate {

        @Override
        public void execute(DelegatePlanItemInstance delegatePlanItemInstance) {
            delegatePlanItemInstance.setVariable("delegateVariable", true);
        }

    }


}
