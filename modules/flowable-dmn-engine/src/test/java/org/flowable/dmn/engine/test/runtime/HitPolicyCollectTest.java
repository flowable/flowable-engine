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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeploymentAnnotation;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class HitPolicyCollectTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicyNoAggregator() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        List<Map<String, Object>> result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("OUTPUT1", result.get(0).get("outputVariable1"));
        Assert.assertEquals("OUTPUT2", result.get(1).get("outputVariable1"));
        Assert.assertEquals("OUTPUT3", result.get(2).get("outputVariable1"));
    }

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicyNoAggregatorCompound() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        List<Map<String, Object>> result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(2, result.get(0).keySet().size());

        Assert.assertEquals("OUTPUT1", result.get(0).get("outputVariable1"));
        Assert.assertEquals("OUTPUT2", result.get(1).get("outputVariable1"));
        Assert.assertEquals("OUTPUT3", result.get(2).get("outputVariable1"));

        Assert.assertEquals(1D, result.get(0).get("outputVariable2"));
        Assert.assertEquals(2D, result.get(1).get("outputVariable2"));
        Assert.assertEquals(3D, result.get(2).get("outputVariable2"));
    }

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicyWithAggregatorMultipleOutputs() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKeyWithAuditTrail("decision1", inputVariables);

        Assert.assertNull(result.getDecisionResult());
        Assert.assertTrue(result.getAuditTrail().isFailed());
        Assert.assertNotNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicyWithAggregatorWrongOutputType() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKeyWithAuditTrail("decision1", inputVariables);

        Assert.assertNull(result.getDecisionResult());
        Assert.assertTrue(result.getAuditTrail().isFailed());
        Assert.assertNotNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicySUM() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        Map<String, Object> result = dmnRuleService.executeDecisionByKeySingleResult("decision1", inputVariables);

        Assert.assertEquals(1, result.keySet().size());
        Assert.assertEquals(60D, result.get("outputVariable1"));
    }

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicyMIN() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        Map<String, Object> result = dmnRuleService.executeDecisionByKeySingleResult("decision1", inputVariables);

        Assert.assertEquals(1, result.keySet().size());
        Assert.assertEquals(10D, result.get("outputVariable1"));
    }

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicyMAX() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        Map<String, Object> result = dmnRuleService.executeDecisionByKeySingleResult("decision1", inputVariables);

        Assert.assertEquals(1, result.keySet().size());
        Assert.assertEquals(30D, result.get("outputVariable1"));
    }

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicyCOUNT() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        Map<String, Object> result = dmnRuleService.executeDecisionByKeySingleResult("decision1", inputVariables);

        Assert.assertEquals(1, result.keySet().size());
        Assert.assertEquals(3D, result.get("outputVariable1"));
    }

    @Test
    @DmnDeploymentAnnotation
    public void collectHitPolicyCOUNTNoResults() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 50);

        Map<String, Object> result = dmnRuleService.executeDecisionByKeySingleResult("decision1", inputVariables);

        Assert.assertNull(result);
    }
}
