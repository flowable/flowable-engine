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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class RuntimeTest extends AbstractFlowableDmnTest {

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void multipleConclusions() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", 10)
                .executeWithSingleResult();
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test3", result.get("output1"));
        Assert.assertSame(Double.class, result.get("output2").getClass());
        Assert.assertEquals(3D, result.get("output2"));
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
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
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
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
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
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
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
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
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
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/strings_1.dmn")
    public void stringOnInput() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "testString")
                .executeWithSingleResult();
        Assert.assertNotNull(result);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test1", result.get("output1"));
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
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_1.dmn")
    public void conlusionExpressionDouble() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "blablatest")
                .executeWithSingleResult();
        Assert.assertSame(Double.class, result.get("output1").getClass());
        Assert.assertEquals(5D, result.get("output1"));
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void conclusionExpressionCastException() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "blablatest")
                .executeWithAuditTrail();
        Assert.assertNotNull(result.getRuleExecutions().get(2).getConclusionResults().iterator().next().getException());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void failedStateMissingInputVariable() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .executeWithAuditTrail();
        Assert.assertEquals(true, result.isFailed());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_3.dmn")
    public void missingInputVariableBoolean() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(new HashMap<>())
                .executeWithAuditTrail();
        Assert.assertEquals(false, result.isFailed());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_4.dmn")
    public void failedStateUnknownFunctionOutcomeExpression() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "blablatest")
                .executeWithAuditTrail();
        Assert.assertEquals(true, result.isFailed());
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
        
        Assert.assertEquals(200D, result.get("output1"));
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void failedStateCouldNotCreateOutcome() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "blablatest")
                .executeWithAuditTrail();
        Assert.assertEquals(true, result.isFailed());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/empty_expressions.dmn")
    public void emptyExpressions() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", "testblabla")
                .executeWithAuditTrail();
        Assert.assertEquals(false, result.isFailed());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/dates_4.dmn")
    public void inputNull() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", null)
                .executeWithSingleResult();
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
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
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
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

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("THIRD", result.get(0).get("output1"));
        Assert.assertEquals("FIRST", result.get(1).get("output1"));
        Assert.assertEquals("SECOND", result.get(2).get("output1"));
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
        
        Map<String, Object> ruleResult1 = result.get(0);
        Map<String, Object> ruleResult2 = result.get(1);
        Map<String, Object> ruleResult3 = result.get(2);
        Map<String, Object> ruleResult4 = result.get(3);

        Assert.assertEquals("DECLINE", ruleResult1.get("routing"));
        Assert.assertEquals("Applicant too young", ruleResult1.get("reason"));
        Assert.assertEquals("NONE", ruleResult1.get("reviewlevel"));

        Assert.assertEquals("REFER", ruleResult2.get("routing"));
        Assert.assertEquals("Applicant under debt review", ruleResult2.get("reason"));
        Assert.assertEquals("LEVEL 2", ruleResult2.get("reviewlevel"));

        Assert.assertEquals("REFER", ruleResult3.get("routing"));
        Assert.assertEquals("High risk application", ruleResult3.get("reason"));
        Assert.assertEquals("LEVEL 1", ruleResult3.get("reviewlevel"));

        Assert.assertEquals("ACCEPT", ruleResult4.get("routing"));
        Assert.assertEquals("Acceptable", ruleResult4.get("reason"));
        Assert.assertEquals("NONE", ruleResult4.get("reviewlevel"));
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

        Assert.assertEquals(500D, result.get("total"));
        Assert.assertEquals(0D, result.get("discount"));
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

        Assert.assertNotNull(result);
        Assert.assertEquals(true, result.getRuleExecutions().get(1).getConditionResults().get(0).getResult());
        Assert.assertEquals(true, result.getRuleExecutions().get(2).getConditionResults().get(0).getResult());
        Assert.assertEquals(true, result.getRuleExecutions().get(3).getConditionResults().get(0).getResult());
    }
}
