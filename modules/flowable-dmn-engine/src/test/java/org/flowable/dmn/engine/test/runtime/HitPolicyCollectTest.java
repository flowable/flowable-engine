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
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.jupiter.api.Test;

/**
 * @author Yvo Swillens
 */
class HitPolicyCollectTest extends BaseFlowableDmnTest {

    @Test
    @DmnDeployment
    public void collectHitPolicyNoAggregator() {
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
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HitPolicyCollectTest.collectHitPolicyNoAggregator.dmn")
    public void collectHitPolicyNoAggregatorNoMatch() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 50)
                .execute();

        assertThat(result).isEmpty();
    }

    @Test
    @DmnDeployment
    public void collectHitPolicyNoAggregatorCompound() {
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
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(result)
                .containsExactly(entry("outputVariable1", 90D));
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HitPolicyCollectTest.collectHitPolicySUM.dmn")
    public void collectHitPolicySUMNoMatch() {
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 50)
                .executeWithSingleResult();

        assertThat(result).isNull();
    }

    @Test
    @DmnDeployment
    public void collectHitPolicySUM_force_DMN11() {
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
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 50)
                .executeWithSingleResult();

        assertThat(result).isNull();
    }
}
