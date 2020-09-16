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
package org.flowable.dmn.engine.test.runtime.drd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Maps;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
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

        DecisionServiceExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decisionService1")
            .variables(inputVariables)
            .executeDecisionServiceWithAuditTrail();

        assertThat(result.getChildDecisionExecutions().keySet())
            .containsSequence("decision9", "decision7")
            .containsSequence("decision6", "decision5",
                "decision3", "decision4", "decision1", "decision2", "decision8")
            .containsExactlyInAnyOrder("decision9", "decision7", "decision6", "decision5",
                "decision3", "decision4", "decision1", "decision2", "decision8");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/simple_decisionservice.dmn")
    public void evaluateDecisionServiceDecisionExecutionOrder() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVar1", "val1");
        inputVariables.put("inputVar2", "val2");
        inputVariables.put("inputVar3", "val3");

        DecisionServiceExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decisionService1")
            .variables(inputVariables)
            .executeDecisionServiceWithAuditTrail();

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

        Map<String, List<Map<String, Object>>> result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decisionService1")
            .variables(inputVariables)
            .executeDecisionService();

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("output1", "result is another test 1");

        assertThat(result)
            .containsEntry("decision1", Collections.singletonList(expectedResult))
            .hasSize(1);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/evaluateMortgageRequestService.dmn")
    public void executeEvaluateMortgageRequest() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("housePrice", 300000D);
        inputVariables.put("age", 42D);
        inputVariables.put("region", "CITY_CENTRE");
        inputVariables.put("doctorVisit", false);
        inputVariables.put("hospitalVisit", false);

        Map<String, List<Map<String, Object>>> result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("evaluateMortgageRequestService")
            .variables(inputVariables)
            .executeDecisionService();

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("approval", "APPROVED");

        assertThat(result)
            .containsEntry("evaluateMortgageRequest", Collections.singletonList(expectedResult))
            .hasSize(1);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/evaluateMortgageRequestService.dmn")
    public void evaluateMortgageRequest() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("housePrice", 300000D);
        inputVariables.put("age", 42D);
        inputVariables.put("region", "CITY_CENTRE");
        inputVariables.put("doctorVisit", false);
        inputVariables.put("hospitalVisit", false);

        Map<String, List<Map<String, Object>>> result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("evaluateMortgageRequestService")
            .variables(inputVariables)
            .executeDecisionService();

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("approval", "APPROVED");

        assertThat(result)
            .containsEntry("evaluateMortgageRequest", Collections.singletonList(expectedResult))
            .hasSize(1);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions.dmn")
    public void executeDecisionServiceMultipleOutputDecisions() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, List<Map<String, Object>>> result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .variable("input1", "test1")
            .variable("input2", "test2")
            .variable("input3", "test3")
            .variable("input4", "test4")
            .executeDecisionService();

        Map<String, Object> expectedResult1 = new HashMap<>();
        expectedResult1.put("output1", "NOT EMPTY");
        Map<String, Object> expectedResult2 = new HashMap<>();
        expectedResult2.put("output2", "NOT EMPTY");

        assertThat(result)
            .containsEntry("decision1", Collections.singletonList(expectedResult1))
            .containsEntry("decision2", Collections.singletonList(expectedResult2))
            .hasSize(2);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions2.dmn")
    public void executeDecisionServiceMultipleOutputDecisionsWithMultiOutcomes() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, List<Map<String, Object>>> result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .variable("input1", "test1")
            .variable("input2", "test2")
            .variable("input3", "test3")
            .variable("input4", "test4")
            .executeDecisionService();

        List<Map<String, Object>> expectedResultList1 = Arrays.asList(
            Maps.newHashMap("output1", "NOT EMPTY 1"),
            Maps.newHashMap("output1", "NOT EMPTY 2"),
            Maps.newHashMap("output1", "NOT EMPTY 3"));

        List<Map<String, Object>> expectedResultList2 = Arrays.asList(
            Maps.newHashMap("output2", "NOT EMPTY 1"),
            Maps.newHashMap("output2", "NOT EMPTY 2"),
            Maps.newHashMap("output2", "NOT EMPTY 3"));

        assertThat(result)
            .containsEntry("decision1", expectedResultList1)
            .containsEntry("decision2", expectedResultList2)
            .hasSize(2);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions2.dmn")
    public void executeDecisionServiceMultipleOutputDecisionsWithAuditTrail() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionServiceExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .variable("input1", "test1")
            .variable("input2", "test2")
            .variable("input3", "test3")
            .variable("input4", "test4")
            .executeDecisionServiceWithAuditTrail();

        List<Map<String, Object>> expectedResultList1 = Arrays.asList(
            Maps.newHashMap("output1", "NOT EMPTY 1"),
            Maps.newHashMap("output1", "NOT EMPTY 2"),
            Maps.newHashMap("output1", "NOT EMPTY 3"));

        List<Map<String, Object>> expectedResultList2 = Arrays.asList(
            Maps.newHashMap("output2", "NOT EMPTY 1"),
            Maps.newHashMap("output2", "NOT EMPTY 2"),
            Maps.newHashMap("output2", "NOT EMPTY 3"));

        assertThat(result.getDecisionServiceResult())
            .containsEntry("decision1", expectedResultList1)
            .containsEntry("decision2", expectedResultList2)
            .hasSize(2);
    }

    @Test()
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/simple.dmn")
    public void executeDecisionServiceWithDecision() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        assertThatThrownBy(() -> dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("decision")
            .variable("inputVariable1", 10D)
            .variable("inputVariable2", "test2")
            .executeDecisionService())
            .isInstanceOf(FlowableIllegalArgumentException.class)
            .hasMessageContaining("no decision service with id: 'decision' found in definition");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions2.dmn")
    public void executeDecisionWithDecisionService() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        assertThatThrownBy(() -> dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .execute())
            .isInstanceOf(FlowableIllegalArgumentException.class)
            .hasMessageContaining("no decision with id: 'expandedDecisionService' found in definition");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions.dmn")
    public void evaluateDecisionWithSingleResult() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .variable("input1", "test1")
            .variable("input2", "test2")
            .variable("input3", "test3")
            .variable("input4", "test4")
            .executeWithSingleResult();

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("output1", "NOT EMPTY");
        expectedResult.put("output2", "NOT EMPTY");

        assertThat(result)
            .containsAllEntriesOf(expectedResult);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions2.dmn")
    public void evaluateDecisionWithSingleResultMultipleResults() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        assertThatThrownBy(() -> dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .variable("input1", "test1")
            .variable("input2", "test2")
            .variable("input3", "test3")
            .variable("input4", "test4")
            .executeWithSingleResult())
            .isInstanceOf(FlowableException.class)
            .hasMessageContaining("more than one result in decision: decision1");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions.dmn")
    public void executeDecisionServiceWithSingleResult() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .variable("input1", "test1")
            .variable("input2", "test2")
            .variable("input3", "test3")
            .variable("input4", "test4")
            .executeDecisionServiceWithSingleResult();

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("output1", "NOT EMPTY");
        expectedResult.put("output2", "NOT EMPTY");

        assertThat(result)
            .containsAllEntriesOf(expectedResult);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions2.dmn")
    public void executeDecisionServiceWithSingleResultMultipleResults() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        assertThatThrownBy(() -> dmnRuleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .variable("input1", "test1")
            .variable("input2", "test2")
            .variable("input3", "test3")
            .variable("input4", "test4")
            .executeDecisionServiceWithSingleResult())
            .isInstanceOf(FlowableException.class)
            .hasMessageContaining("more than one result in decision: decision1");
    }
}
