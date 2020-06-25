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
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class CalculationsTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/calculations2.dmn")
    public void doubleToBigDecimalScaleConversion() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        Double inputDouble1 = 0.40D;

        processVariablesInput.put("sample_input", inputDouble1);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("dmnWithExpressionAndDecimals")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getRuleExecutions()).hasSize(2);
        assertThat(result.getRuleExecutions().get(2).isValid()).isTrue();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/calculations2.dmn")
    public void floatToBigDecimalScaleConversion() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        Float inputFloat1 = 0.40F;

        processVariablesInput.put("sample_input", inputFloat1);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("dmnWithExpressionAndDecimals")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getRuleExecutions()).hasSize(2);
        assertThat(result.getRuleExecutions().get(2).isValid()).isTrue();
    }


    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/calculations.dmn")
    public void floatAndDoubleOutputEntry() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        Long inputVariable1 = 1L;
        Double inputDouble1 = 0.1D;
        Double inputDouble2 = 1.0e307;
        Float inputFloat1 = 0.1F;

        processVariablesInput.put("inputVariable1", inputVariable1);
        processVariablesInput.put("inputDouble1", inputDouble1);
        processVariablesInput.put("inputDouble2", inputDouble2);
        processVariablesInput.put("inputFloat1", inputFloat1);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getRuleExecutions().get(1).getConclusionResults().get(0).getResult()).isEqualTo(1D);
        assertThat(result.getRuleExecutions().get(1).getConclusionResults().get(1).getResult()).isEqualTo(1D);
        assertThat(result.getRuleExecutions().get(2).getConclusionResults().get(0).getResult()).isEqualTo(inputDouble2);
        assertThat(result.getRuleExecutions().get(2).getConclusionResults().get(1).getResult()).isEqualTo(10L);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/calculations_variable_update_1.dmn")
    public void outcomeVariableReferenceUpdate1() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "blablatest");
        processVariablesInput.put("referenceVar1", 10D);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.getRuleExecutions().get(1).getConclusionResults().get(0).getResult()).isEqualTo(10D);
        assertThat(result.getRuleExecutions().get(2).getConclusionResults().get(0).getResult()).isEqualTo(20D);
    }
}
