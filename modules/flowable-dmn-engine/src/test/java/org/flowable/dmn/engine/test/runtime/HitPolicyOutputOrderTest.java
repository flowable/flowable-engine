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

import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class HitPolicyOutputOrderTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment
    public void outputOrderHitPolicy() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly("OUTPUT2", "OUTPUT3", "OUTPUT1");
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyNumberAsString() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly(20D, 30D, 10D);
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyNumber() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly(20D, 30D, 10D);
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyDate() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly(new DateTime("2000-01-01").toDate(), new DateTime("2020-01-01").toDate(), new DateTime("2010-01-01").toDate());
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyBoolean() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyNoOutputValuesStrictModeDisabled() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        dmnEngine.getDmnEngineConfiguration().setStrictMode(false);

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult())
                .extracting("outputVariable1")
                .containsExactly("OUTPUT1", "OUTPUT2", "OUTPUT3");

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getExceptionMessage()).isNull();
        assertThat(result.getValidationMessage()).isNotNull();

        dmnEngine.getDmnEngineConfiguration().setStrictMode(true);
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyNoOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult()).isEmpty();

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getExceptionMessage()).isNotNull();
        assertThat(result.getValidationMessage()).isNull();
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyCompound() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly("DECLINE", "REFER", "REFER", "ACCEPT");

        assertThat(result)
                .extracting("outputVariable2")
                .containsExactly("NONE", "LEVEL 2", "LEVEL 1", "NONE");
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyCompoundOtherTypes() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly("DECLINE", "REFER", "REFER", "ACCEPT");

        assertThat(result)
                .extracting("outputVariable2")
                .containsExactly(10D, 30D, 20D, 10D);
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyCompoundFirstOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly("DECLINE", "REFER", "REFER", "ACCEPT");

        assertThat(result)
                .extracting("outputVariable2")
                .containsExactly("NONE", "LEVEL 1", "LEVEL 2", "NONE");
    }

    @Test
    @DmnDeployment
    public void outputOrderHitPolicyCompoundSecondOutputValues() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();

        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();

        assertThat(result)
                .extracting("outputVariable1")
                .containsExactly("REFER", "REFER", "ACCEPT", "DECLINE");

        assertThat(result)
                .extracting("outputVariable2")
                .containsExactly("LEVEL 2", "LEVEL 1", "NONE", "NONE");
    }
}
