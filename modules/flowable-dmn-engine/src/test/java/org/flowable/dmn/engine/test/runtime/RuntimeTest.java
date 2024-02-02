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
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yvo Swillens
 */
public class RuntimeTest extends AbstractFlowableDmnTest {

    public ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void multipleConclusions() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", 10)
                .executeWithSingleResult();
        assertThat(result.get("output1").getClass()).isSameAs(String.class);
        assertThat(result).containsEntry("output1", "test3");
        assertThat(result.get("output2").getClass()).isSameAs(Double.class);
        assertThat(result).containsEntry("output2", 3D);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/dates_1.dmn")
    public void staticDates() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", localDate.toDate())
                .executeWithSingleResult();
        assertThat(result.get("output1").getClass()).isSameAs(String.class);
        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/dates_2.dmn")
    public void dynamicDatesAdd() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", localDate.toDate())
                .executeWithSingleResult();
        assertThat(result.get("output1").getClass()).isSameAs(String.class);
        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/dates_3.dmn")
    public void dynamicDatesSubtract() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", localDate.toDate())
                .executeWithSingleResult();
        assertThat(result.get("output1").getClass()).isSameAs(String.class);
        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/dates_5.dmn")
    public void datesEquals() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", localDate.toDate())
                .executeWithSingleResult();
        assertThat(result.get("output1").getClass()).isSameAs(String.class);
        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/dates_5.dmn")
    public void localDatesEquals() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", localDate)
                .executeWithSingleResult();
        assertThat(result.get("output1").getClass()).isSameAs(String.class);
        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/strings_1.dmn")
    public void stringOnInput() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "testString")
                .executeWithSingleResult();
        assertThat(result).isNotNull();
        assertThat(result.get("output1").getClass()).isSameAs(String.class);
        assertThat(result).containsEntry("output1", "test1");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/strings_2.dmn")
    public void emptyStrings() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "");
        processVariablesInput.put("input2", "This is a sentence containing foobar words.");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithSingleResult();
        assertThat(result.get("output1").getClass()).isSameAs(String.class);
        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/empty_outcome.dmn")
    public void emptyOutcome() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "NOT TEST");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
            .decisionKey("EmptyOutcome")
            .variables(processVariablesInput)
            .executeWithSingleResult();

        assertThat(result).isNull();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/empty_outcomes.dmn")
    public void emptyOneEmptyOutcome() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", 11D);

        List<Map<String, Object>> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .execute();

        assertThat(result).hasSize(3);
        assertThat(result.get(0))
                .hasSize(1)
                .containsEntry("output1", 11d);
        assertThat(result.get(1))
                .hasSize(1)
                .containsEntry("output2", 11d);
        assertThat(result.get(2))
                .hasSize(1)
                .containsEntry("output3", 11d);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_1.dmn")
    public void conlusionExpressionDouble() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "blablatest")
                .executeWithSingleResult();
        assertThat(result.get("output1").getClass()).isSameAs(Double.class);
        assertThat(result).containsEntry("output1", 5D);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void conclusionExpressionCastException() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "blablatest")
                .executeWithAuditTrail();
        assertThat(result.getRuleExecutions().get(2).getConclusionResults().iterator().next().getException()).isNotNull();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void failedStateMissingInputVariable() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .executeWithAuditTrail();
        assertThat(result.isFailed()).isTrue();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_3.dmn")
    public void missingInputVariableBoolean() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(new HashMap<>())
                .executeWithAuditTrail();
        assertThat(result.isFailed()).isFalse();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_4.dmn")
    public void failedStateUnknownFunctionOutcomeExpression() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "blablatest")
                .executeWithAuditTrail();
        assertThat(result.isFailed()).isTrue();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_5.dmn")
    public void outcomeVariableReference() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "blablatest");
        processVariablesInput.put("referenceVar1", 10D);
        processVariablesInput.put("referenceVar2", 20D);

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithSingleResult();

        assertThat(result).containsEntry("output1", 200D);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void failedStateCouldNotCreateOutcome() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "blablatest")
                .executeWithAuditTrail();
        assertThat(result.isFailed()).isTrue();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/empty_expressions.dmn")
    public void emptyExpressions() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "testblabla")
                .executeWithAuditTrail();
        assertThat(result.isFailed()).isFalse();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/dates_4.dmn")
    public void inputNull() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", null)
                .executeWithSingleResult();
        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/reservered_word.dmn")
    public void reservedWord() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("date", localDate.toDate())
                .executeWithSingleResult();
        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/empty_tokens.dmn")
    public void emptyTokens() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("input1", "AAA");
        processVariablesInput.put("input2", "BBB");

        List<Map<String, Object>> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .execute();

        assertThat(result)
                .extracting("output1")
                .containsExactly("THIRD", "FIRST", "SECOND");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/risk_rating_spec_example.dmn")
    public void riskRating() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("age", 17);
        processVariablesInput.put("riskcategory", "HIGH");
        processVariablesInput.put("debtreview", true);

        List<Map<String, Object>> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("RiskRatingDecisionTable")
                .variables(processVariablesInput)
                .execute();

        assertThat(result)
                .extracting("routing", "reason", "reviewlevel")
                .containsExactly(
                        tuple("DECLINE", "Applicant too young", "NONE"),
                        tuple("REFER", "Applicant under debt review", "LEVEL 2"),
                        tuple("REFER", "High risk application", "LEVEL 1"),
                        tuple("ACCEPT", "Acceptable", "NONE")
                );
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/risk_rating_spec_example_DMN12.dmn")
    public void riskRatingDMN12() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("age", 17);
        processVariablesInput.put("riskcategory", "HIGH");
        processVariablesInput.put("debtreview", true);

        List<Map<String, Object>> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("RiskRatingDecisionTable")
                .variables(processVariablesInput)
                .execute();

        assertThat(result)
                .extracting("routing", "reason", "reviewlevel")
                .containsExactly(
                        tuple("DECLINE", "Applicant too young", "NONE"),
                        tuple("REFER", "Applicant under debt review", "LEVEL 2"),
                        tuple("REFER", "High risk application", "LEVEL 1"),
                        tuple("ACCEPT", "Acceptable", "NONE")
                );
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/numbers_1.dmn")
    public void testNumbers1() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("count", 101L);
        processVariablesInput.put("price", 100L);
        processVariablesInput.put("status", "");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("ad")
                .variables(processVariablesInput)
                .executeWithSingleResult();

        assertThat(result)
                .containsOnly(
                        entry("total", 500D),
                        entry("discount", 0D)
                );
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/simple.dmn")
    public void testEqualsStringImplicitOperator() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("inputVariable1", 1D);
        processVariablesInput.put("inputVariable2", "test2");

        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result).isNotNull();
        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(0).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(0).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(3).getConditionResults().get(0).getResult()).isEqualTo(true);
    }


    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/json.dmn")
    public void testJsonNumbers1() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        ObjectNode inputNode = objectMapper.createObjectNode();
        inputNode.put("value", 5L);

        processVariablesInput.put("inputVariable1", inputNode);

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithSingleResult();

        assertThat(result).containsEntry("outputVariable1", "result2");
    }
}
