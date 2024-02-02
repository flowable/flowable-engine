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

import java.util.List;
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
public class HitPolicyCollectTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment
    public void collectHitPolicyNoAggregator() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly("OUTPUT1", "OUTPUT2", "OUTPUT3");
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyNoAggregatorCompound() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).keySet()).hasSize(2);

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly("OUTPUT1", "OUTPUT2", "OUTPUT3");

        assertThat(result)
                .extracting("outputVariable2")
                .containsExactly(1D, 2D, 3D);
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyWithAggregatorMultipleOutputs() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult()).isEmpty();
        assertThat(result.isFailed()).isTrue();
        assertThat(result.getExceptionMessage()).isNotNull();
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyWithAggregatorWrongOutputType() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult()).isEmpty();
        assertThat(result.isFailed()).isTrue();
        assertThat(result.getExceptionMessage()).isNotNull();
    }

    @Test
    @DmnDeployment
    public void collectHitPolicySUM() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsExactly(entry("outputVariable1", 90D));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicySUM_force_DMN11() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsExactly(entry("outputVariable1", 60D));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyMIN() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsExactly(entry("outputVariable1", 10D));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyMAX() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsExactly(entry("outputVariable1", 30D));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyCOUNT() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsExactly(entry("outputVariable1", 4D));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyCOUNT_force_DMN11() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsExactly(entry("outputVariable1", 3D));
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyCOUNTNoResults() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 50)
                .executeWithSingleResult();

        assertThat(result).isNull();
    }
}
