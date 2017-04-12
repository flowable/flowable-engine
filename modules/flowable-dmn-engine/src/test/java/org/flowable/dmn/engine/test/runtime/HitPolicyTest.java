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
import java.util.List;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class HitPolicyTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeploymentAnnotation
    public void anyHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(10D, result.getDecisionResult().getFirstOutputValue("outputVariable1"));
        Assert.assertEquals("result1", result.getDecisionResult().getFirstOutputValue("outputVariable2"));
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

        Assert.assertNull(result.getDecisionResult());
        Assert.assertTrue(result.getAuditTrail().isFailed());
        Assert.assertNotNull(result.getAuditTrail().getRuleExecutions().get(3).getConclusionResults().get(2).getException());
        Assert.assertNotNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void anyHitPolicyViolatedStrictModeDisabled() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        dmnEngine.getDmnEngineConfiguration().setStrictMode(false);

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(2, result.getDecisionResult().getRuleResults().size());
        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getRuleExecutions().get(3).getConclusionResults().get(1).getException());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());

        // re enable strict mode
        dmnEngine.getDmnEngineConfiguration().setStrictMode(true);
    }

    @Test
    @DmnDeploymentAnnotation
    public void uniqueHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 10);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals("eq 10", result.getDecisionResult().getSingleOutputValue("outputVariable1"));
        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void uniqueHitPolicyViolated() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 9);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertNull(result.getDecisionResult());
        Assert.assertTrue(result.getAuditTrail().isFailed());
        Assert.assertNotNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void uniqueHitPolicyViolatedStrictModeDisabled() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        dmnEngine.getDmnEngineConfiguration().setStrictMode(false);

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 9);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals("lt 20", result.getDecisionResult().getSingleOutputValue("outputVariable1"));
        Assert.assertEquals(10D, result.getDecisionResult().getSingleOutputValue("outputVariable2"));
        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());

        // re enable strict mode
        dmnEngine.getDmnEngineConfiguration().setStrictMode(true);
    }

    @Test
    @DmnDeploymentAnnotation
    public void firstHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 11);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals("gt 10", result.getDecisionResult().getSingleOutputValue("outputVariable1"));
        Assert.assertEquals("result2", result.getDecisionResult().getSingleOutputValue("outputVariable2"));
        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void ruleOrderHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 13);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(2, result.getDecisionResult().getRuleResults().size());

        Assert.assertEquals(2, result.getDecisionResult().getOutputValues("outputVariable1").size());

        Assert.assertEquals("result2", result.getDecisionResult().getOutputValues("outputVariable1").get(0));
        Assert.assertEquals("result4", result.getDecisionResult().getOutputValues("outputVariable1").get(1));

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals("OUTPUT2", result.getDecisionResult().getSingleOutputValue("outputVariable1"));

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicyCompound() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals("REFER", result.getDecisionResult().getSingleOutputValue("outputVariable1"));
        Assert.assertEquals("LEVEL 2", result.getDecisionResult().getSingleOutputValue("outputVariable2"));

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicyCompoundFirstOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals("REFER", result.getDecisionResult().getSingleOutputValue("outputVariable1"));
        Assert.assertEquals("LEVEL 1", result.getDecisionResult().getSingleOutputValue("outputVariable2"));

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicyCompoundSecondOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals("REFER", result.getDecisionResult().getSingleOutputValue("outputVariable1"));
        Assert.assertEquals("LEVEL 2", result.getDecisionResult().getSingleOutputValue("outputVariable2"));

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicyCompoundNoOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertNull(result.getDecisionResult());
        Assert.assertTrue(result.getAuditTrail().isFailed());
        Assert.assertNotNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicyCompoundNoOutputValuesStrictModeDisabled() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        dmnEngine.getDmnEngineConfiguration().setStrictMode(false);

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals("ACCEPT", result.getDecisionResult().getSingleOutputValue("outputVariable1"));
        Assert.assertEquals("NONE", result.getDecisionResult().getSingleOutputValue("outputVariable2"));

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());

        dmnEngine.getDmnEngineConfiguration().setStrictMode(true);
    }


    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicyTypeConversion() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getDecisionResult().getRuleResults().size());
        Assert.assertEquals(20D, result.getDecisionResult().getSingleOutputValue("outputVariable1"));

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void outputOrderHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(3, result.getDecisionResult().getRuleResults().size());
        Assert.assertArrayEquals(new String[]{"OUTPUT2", "OUTPUT3", "OUTPUT1"}, result.getDecisionResult().getOutputValues("outputVariable1").toArray());

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void outputOrderHitPolicyNoOutputValuesStrictModeDisabled() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        dmnEngine.getDmnEngineConfiguration().setStrictMode(false);

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(3, result.getDecisionResult().getRuleResults().size());
        Assert.assertArrayEquals(new String[]{"OUTPUT1", "OUTPUT2", "OUTPUT3"}, result.getDecisionResult().getOutputValues("outputVariable1").toArray());

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void outputOrderHitPolicyNoOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertNull(result.getDecisionResult());

        Assert.assertTrue(result.getAuditTrail().isFailed());
        Assert.assertNotNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void outputOrderHitPolicyCompound() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(4, result.getDecisionResult().getRuleResults().size());
        Assert.assertArrayEquals(new String[]{"DECLINE", "REFER", "REFER", "ACCEPT"}, result.getDecisionResult().getOutputValues("outputVariable1").toArray());
        Assert.assertArrayEquals(new String[]{"NONE", "LEVEL 2", "LEVEL 1", "NONE"}, result.getDecisionResult().getOutputValues("outputVariable2").toArray());

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void outputOrderHitPolicyCompoundOtherTypes() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(4, result.getDecisionResult().getRuleResults().size());
        Assert.assertArrayEquals(new String[]{"DECLINE", "REFER", "REFER", "ACCEPT"}, result.getDecisionResult().getOutputValues("outputVariable1").toArray());
        Assert.assertArrayEquals(new Double[]{10D, 30D, 20D, 10D}, result.getDecisionResult().getOutputValues("outputVariable2").toArray());

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void outputOrderHitPolicyCompoundFirstOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(4, result.getDecisionResult().getRuleResults().size());
        Assert.assertArrayEquals(new String[]{"DECLINE", "REFER", "REFER", "ACCEPT"}, result.getDecisionResult().getOutputValues("outputVariable1").toArray());
        Assert.assertArrayEquals(new String[]{"NONE", "LEVEL 1", "LEVEL 2", "NONE"}, result.getDecisionResult().getOutputValues("outputVariable2").toArray());

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void outputOrderHitPolicyCompoundSecondOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(4, result.getDecisionResult().getRuleResults().size());
        Assert.assertArrayEquals(new String[]{"REFER", "REFER", "ACCEPT", "DECLINE"}, result.getDecisionResult().getOutputValues("outputVariable1").toArray());
        Assert.assertArrayEquals(new String[]{"LEVEL 2", "LEVEL 1", "NONE", "NONE"}, result.getDecisionResult().getOutputValues("outputVariable2").toArray());

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

}
