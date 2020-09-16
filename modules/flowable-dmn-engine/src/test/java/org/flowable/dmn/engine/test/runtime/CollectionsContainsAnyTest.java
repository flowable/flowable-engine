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

import java.util.Arrays;
import java.util.HashMap;
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
public class CollectionsContainsAnyTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_ANY.dmn")
    public void testContainsAnyTrue() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        List inputVariable1 = Arrays.asList("test1", "test2", "test3");
        List inputVariable2 = Arrays.asList(5L, 10L, 20L, 50L);
        List inputVariable3 = Arrays.asList("test2", "test5");
        List inputVariable4 = Arrays.asList("test6", "test5");


        processVariablesInput.put("collection1", inputVariable1);
        processVariablesInput.put("collection2", inputVariable2);
        processVariablesInput.put("collection3", inputVariable3);
        processVariablesInput.put("collection4", inputVariable4);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getRuleExecutions().get(1).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(2).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(5).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(6).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(8).isValid()).isTrue();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_ANY.dmn")
    public void testContainsAnyFalse() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        List inputVariable1 = Arrays.asList("test1", "test2", "test3");
        List inputVariable2 = Arrays.asList(5L, 10L, 20L, 50L);
        List inputVariable3 = Arrays.asList("test2", "test5");
        List inputVariable4 = Arrays.asList("test6", "test5");

        processVariablesInput.put("collection1", inputVariable1);
        processVariablesInput.put("collection2", inputVariable2);
        processVariablesInput.put("collection3", inputVariable3);
        processVariablesInput.put("collection4", inputVariable4);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getRuleExecutions().get(3).isValid()).isFalse();
        assertThat(result.getRuleExecutions().get(4).isValid()).isFalse();
        assertThat(result.getRuleExecutions().get(7).isValid()).isFalse();
        assertThat(result.getRuleExecutions().get(9).isValid()).isFalse();
    }
}
