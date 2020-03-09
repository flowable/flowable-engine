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
package org.flowable.dmn.engine.test.runtime.drg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
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

public class DecisionServiceTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/simple_decisionservice.dmn")
    public void executeDecisionServiceDecisionExecutionOrder() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVar1", "val1");
        inputVariables.put("inputVar2", "val2");
        inputVariables.put("inputVar3", "val3");

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decisionService1")
            .variables(inputVariables)
            .executeWithAuditTrail();

        assertThat(result.getChildDecisionExecutions().keySet())
            .containsSequence("decision9", "decision7")
            .containsSequence("decision6", "decision5",
                "decision3", "decision4", "decision1", "decision2", "decision8")
            .containsExactlyInAnyOrder("decision9", "decision7", "decision6", "decision5",
                "decision3", "decision4", "decision1", "decision2", "decision8");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/related_decisionservice.dmn")
    public void executeDecisionServiceWithDecisionOutcomes() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("input1", "test");

        List<Map<String, Object>> result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decisionService1")
            .variables(inputVariables)
            .execute();

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("output1", "was test1");

        assertThat(result)
            .containsExactly(expectedResult);
    }
}
