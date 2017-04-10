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

        Assert.assertEquals(0, result.getResultVariables().size());
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

        Assert.assertEquals(2, result.getResultVariables().size());
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

        Assert.assertEquals(1, result.getResultVariables().size());
        Assert.assertEquals("eq 10", result.getResultVariables().get("outputVariable1"));
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

        Assert.assertEquals(0, result.getResultVariables().size());
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

        Assert.assertEquals(1, result.getResultVariables().size());
        Assert.assertEquals("lt 20", result.getResultVariables().get("outputVariable1"));
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

        Assert.assertEquals(2, result.getResultVariables().size());
        Assert.assertEquals("gt 10", result.getResultVariables().get("outputVariable1"));
        Assert.assertEquals("result2", result.getResultVariables().get("outputVariable2"));
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

        Assert.assertEquals(1, result.getResultVariables().size());

        List ruleResults = (List) result.getResultVariables().get("outputVariable1");

        Assert.assertEquals(2, ruleResults.size());

        Assert.assertEquals("result2", ruleResults.get(0));
        Assert.assertEquals("result4", ruleResults.get(1));

        List auditResults = (List) result.getAuditTrail().getOutputVariables().get("outputVariable1");

        Assert.assertEquals(2, auditResults.size());

        Assert.assertEquals("result2", auditResults.get(0));
        Assert.assertEquals("result4", auditResults.get(1));

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

        Assert.assertEquals(1, result.getResultVariables().size());
        Assert.assertEquals("OUTPUT2", result.getResultVariables().get("outputVariable1"));

        Assert.assertEquals(1, result.getAuditTrail().getOutputVariables().size());
        Assert.assertEquals("OUTPUT2", result.getAuditTrail().getOutputVariables().get("outputVariable1"));

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicyFailed() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(0, result.getResultVariables().size());
        Assert.assertEquals(0, result.getAuditTrail().getOutputVariables().size());

        Assert.assertTrue(result.getAuditTrail().isFailed());
        Assert.assertNotNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void priorityHitPolicyTypeConversion() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(1, result.getResultVariables().size());
        Assert.assertEquals("20", result.getResultVariables().get("outputVariable1"));

        Assert.assertEquals(1, result.getAuditTrail().getOutputVariables().size());
        Assert.assertEquals("20", result.getAuditTrail().getOutputVariables().get("outputVariable1"));

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

        Assert.assertEquals(1, result.getResultVariables().size());
        Assert.assertArrayEquals(new String[]{"OUTPUT2", "OUTPUT3", "OUTPUT1"}, ((List) result.getResultVariables().get("outputVariable1")).toArray());

        Assert.assertEquals(1, result.getAuditTrail().getOutputVariables().size());
        Assert.assertArrayEquals(new String[]{"OUTPUT2", "OUTPUT3", "OUTPUT1"}, ((List) result.getAuditTrail().getOutputVariables().get("outputVariable1")).toArray());

        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }

    @Test
    @DmnDeploymentAnnotation
    public void outputOrderHitPolicyCompound() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 5);

        RuleEngineExecutionResult result = dmnRuleService.executeDecisionByKey("decision1", inputVariables);

        Assert.assertEquals(2, result.getResultVariables().size());
        Assert.assertArrayEquals(new String[]{"OUTPUT2", "OUTPUT3", "OUTPUT1"}, ((List) result.getResultVariables().get("outputVariable1")).toArray());
        Assert.assertArrayEquals(new Double[]{20D, 30D, 10D}, ((List) result.getResultVariables().get("outputVariable2")).toArray());

        Assert.assertEquals(2, result.getAuditTrail().getOutputVariables().size());
        Assert.assertArrayEquals(new String[]{"OUTPUT2", "OUTPUT3", "OUTPUT1"}, ((List) result.getAuditTrail().getOutputVariables().get("outputVariable1")).toArray());
        Assert.assertArrayEquals(new Double[]{20D, 30D, 10D},  ((List) result.getAuditTrail().getOutputVariables().get("outputVariable2")).toArray());


        Assert.assertFalse(result.getAuditTrail().isFailed());
        Assert.assertNull(result.getAuditTrail().getExceptionMessage());
    }
}
