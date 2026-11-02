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
package org.flowable.dmn.engine.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.jupiter.api.Test;

/**
 * @author Yvo Swillens
 */
class HitPolicyPriorityTest extends BaseFlowableDmnTest {

    @Test
    @DmnDeployment
    public void priorityHitPolicy() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsOnly(entry("outputVariable1", "OUTPUT2"));
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HitPolicyPriorityTest.priorityHitPolicy.dmn")
    public void priorityHitPolicyNoMatch() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 50)
                .executeWithSingleResult();

        assertThat(result).isNull();
    }

    @Test
    @DmnDeployment
    public void priorityHitPolicyCompound() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsOnly(
                        entry("outputVariable1", "REFER"),
                        entry("outputVariable2", "LEVEL 2")
                );
    }

    @Test
    @DmnDeployment
    public void priorityHitPolicyCompoundFirstOutputValues() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsOnly(
                        entry("outputVariable1", "REFER"),
                        entry("outputVariable2", "LEVEL 1")
                );
    }

    @Test
    @DmnDeployment
    public void priorityHitPolicyCompoundSecondOutputValues() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsOnly(
                        entry("outputVariable1", "REFER"),
                        entry("outputVariable2", "LEVEL 2")
                );
    }

    @Test
    @DmnDeployment
    public void priorityHitPolicyCompoundNoOutputValues() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult()).isEmpty();
        assertThat(result.isFailed()).isTrue();
        assertThat(result.getExceptionMessage()).isNotNull();
        assertThat(result.getValidationMessage()).isNull();
    }

    @Test
    @DmnDeployment
    public void priorityHitPolicyCompoundNoOutputValuesStrictModeDisabled() {
        dmnEngine.getDmnEngineConfiguration().setStrictMode(false);

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult()).hasSize(1);
        Map<String, Object> outputMap = result.getDecisionResult().iterator().next();
        assertThat(outputMap)
                .containsOnly(
                        entry("outputVariable1", "ACCEPT"),
                        entry("outputVariable2", "NONE")
                );

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getExceptionMessage()).isNull();
        assertThat(result.getValidationMessage()).isNotNull();

        dmnEngine.getDmnEngineConfiguration().setStrictMode(true);
    }


    @Test
    @DmnDeployment
    public void priorityHitPolicyTypeConversion() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsOnly(entry("outputVariable1", 20D));
    }
}
