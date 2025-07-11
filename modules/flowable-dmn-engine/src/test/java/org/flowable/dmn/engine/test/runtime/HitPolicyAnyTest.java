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

import java.util.HashMap;
import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.jupiter.api.Test;

/**
 * @author Yvo Swillens
 */
class HitPolicyAnyTest extends BaseFlowableDmnTest {

    @Test
    @DmnDeployment
    public void anyHitPolicy() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variables(inputVariables)
                .executeWithSingleResult();

        assertThat(result)
                .containsOnly(
                        entry("outputVariable1", 10D),
                        entry("outputVariable2", "result1")
                );
    }

    @Test
    @DmnDeployment
    public void anyHitPolicyViolated() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variables(inputVariables)
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult()).isEmpty();
        assertThat(result.isFailed()).isTrue();
        assertThat(result.getValidationMessage()).isNull();
        assertThat(result.getExceptionMessage()).isNotNull();
    }

    @Test
    @DmnDeployment
    public void anyHitPolicyNoValueViolated() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variables(inputVariables)
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult()).isEmpty();
        assertThat(result.isFailed()).isTrue();

        assertThat(result.getExceptionMessage()).isNotNull();
        assertThat(result.getRuleExecutions().get(1).getExceptionMessage()).isNotNull();
        assertThat(result.getRuleExecutions().get(3).getExceptionMessage()).isNotNull();

        assertThat(result.getValidationMessage()).isNull();
        assertThat(result.getRuleExecutions().get(1).getValidationMessage()).isNull();
        assertThat(result.getRuleExecutions().get(3).getValidationMessage()).isNull();
    }

    @Test
    @DmnDeployment
    public void anyHitPolicyViolatedStrictModeDisabled() {
        dmnEngine.getDmnEngineConfiguration().setStrictMode(false);

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variables(inputVariables)
                .executeWithAuditTrail();

        Map<String, Object> outputMap = result.getDecisionResult().iterator().next();
        assertThat(outputMap)
                .containsOnly(
                        entry("outputVariable1", 10D),
                        entry("outputVariable2", "result2")
                );
        assertThat(result.isFailed()).isFalse();

        assertThat(result.getExceptionMessage()).isNull();
        assertThat(result.getRuleExecutions().get(1).getExceptionMessage()).isNull();
        assertThat(result.getRuleExecutions().get(3).getExceptionMessage()).isNull();

        assertThat(result.getValidationMessage()).isNotNull();
        assertThat(result.getRuleExecutions().get(1).getValidationMessage()).isNotNull();
        assertThat(result.getRuleExecutions().get(3).getValidationMessage()).isNotNull();

        // re enable strict mode
        dmnEngine.getDmnEngineConfiguration().setStrictMode(true);
    }
}
