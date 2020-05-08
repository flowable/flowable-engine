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

import java.util.HashMap;
import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class HitPolicyAnyTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment
    public void anyHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variables(inputVariables)
                .executeWithSingleResult();

        assertThat(result.get("outputVariable1")).isEqualTo(10D);
        assertThat(result.get("outputVariable2")).isEqualTo("result1");
    }

    @Test
    @DmnDeployment
    public void anyHitPolicyViolated() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

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
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

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
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        dmnEngine.getDmnEngineConfiguration().setStrictMode(false);

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variables(inputVariables)
                .executeWithAuditTrail();

        Map<String, Object> outputMap = result.getDecisionResult().iterator().next();
        assertThat(outputMap.keySet()).hasSize(2);
        assertThat(outputMap.get("outputVariable1")).isEqualTo(10D);
        assertThat(outputMap.get("outputVariable2")).isEqualTo("result2");
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
