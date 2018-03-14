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

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class CalculationsTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/calculations.dmn")
    public void floatAndDoubleOutputEntry() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        Long inputVariable1 = 1L;
        Double inputDouble1 = 0.1D;
        Float inputFloat1 = 0.1F;

        processVariablesInput.put("inputVariable1", inputVariable1);
        processVariablesInput.put("inputDouble1", inputDouble1);
        processVariablesInput.put("inputFloat1", inputFloat1);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decision")
            .variables(processVariablesInput)
            .executeWithAuditTrail();

        Assert.assertFalse(result.isFailed());
        Assert.assertEquals(1D, result.getRuleExecutions().get(1).getConclusionResults().get(0).getResult());
        Assert.assertEquals(1D, result.getRuleExecutions().get(1).getConclusionResults().get(1).getResult());
    }
}
