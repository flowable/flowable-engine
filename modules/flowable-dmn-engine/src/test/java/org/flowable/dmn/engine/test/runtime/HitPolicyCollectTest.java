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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
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
public class HitPolicyCollectTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment
    public void collectHitPolicyNoAggregator() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertEquals(3, result.size());
        assertEquals("OUTPUT1", result.get(0).get("outputVariable1"));
        assertEquals("OUTPUT2", result.get(1).get("outputVariable1"));
        assertEquals("OUTPUT3", result.get(2).get("outputVariable1"));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyNoAggregatorCompound() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).keySet().size());

        assertEquals("OUTPUT1", result.get(0).get("outputVariable1"));
        assertEquals("OUTPUT2", result.get(1).get("outputVariable1"));
        assertEquals("OUTPUT3", result.get(2).get("outputVariable1"));

        assertEquals(1D, result.get(0).get("outputVariable2"));
        assertEquals(2D, result.get(1).get("outputVariable2"));
        assertEquals(3D, result.get(2).get("outputVariable2"));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyWithAggregatorMultipleOutputs() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        assertEquals(0, result.getDecisionResult().size());
        assertTrue(result.isFailed());
        assertNotNull(result.getExceptionMessage());
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyWithAggregatorWrongOutputType() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        assertEquals(0, result.getDecisionResult().size());
        assertTrue(result.isFailed());
        assertNotNull(result.getExceptionMessage());
    }

    @Test
    @DmnDeployment
    public void collectHitPolicySUM() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertEquals(1, result.keySet().size());
        assertEquals(60D, result.get("outputVariable1"));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyMIN() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertEquals(1, result.keySet().size());
        assertEquals(10D, result.get("outputVariable1"));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyMAX() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertEquals(1, result.keySet().size());
        assertEquals(30D, result.get("outputVariable1"));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyCOUNT() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertEquals(1, result.keySet().size());
        assertEquals(3D, result.get("outputVariable1"));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyCOUNTNoResults() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnRuleService dmnRuleService = dmnEngine.getDmnRuleService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 50)
                .executeWithSingleResult();

        assertNull(result);
    }
}
