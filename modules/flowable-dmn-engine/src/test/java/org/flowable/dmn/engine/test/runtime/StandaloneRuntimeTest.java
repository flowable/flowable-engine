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

import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeploymentAnnotation;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class StandaloneRuntimeTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeploymentAnnotation
    public void ruleUsageExample() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);
        inputVariables.put("inputVariable2", "test2");

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals("result2", result.getResultVariables().get("outputVariable1"));
    }

    @Test
    @DmnDeploymentAnnotation
    public void anyHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(2, result.getResultVariables().size());
        Assert.assertEquals(10D, result.getResultVariables().get("outputVariable1"));
        Assert.assertEquals("result1", result.getResultVariables().get("outputVariable2"));
        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void anyHitPolicyViolated() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(2, result.getResultVariables().size());
        Assert.assertEquals(10D, result.getResultVariables().get("outputVariable1"));
        Assert.assertEquals("result1", result.getResultVariables().get("outputVariable2"));
        Assert.assertTrue(result.getAuditTrail().isFailed());
        Assert.assertNotNull(result.getAuditTrail().getExceptionMessage());
    }
}
