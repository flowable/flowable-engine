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

import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.RuleEngineExecutionSingleResult;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeploymentAnnotation;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Yvo Swillens
 */
public class RuntimeTest extends AbstractFlowableDmnTest {

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void executeDecision_multiple_conclusions() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("input1", 10);
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test3", result.get("output1"));
        Assert.assertSame(Double.class, result.get("output2").getClass());
        Assert.assertEquals(3D, result.get("output2"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/dates_1.dmn")
    public void executeDecision_static_dates() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        processVariablesInput.put("input1", localDate.toDate());
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/dates_2.dmn")
    public void executeDecision_dynamic_dates_add() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        processVariablesInput.put("input1", localDate.toDate());
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/dates_3.dmn")
    public void executeDecision_dynamic_dates_subtract() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        processVariablesInput.put("input1", localDate.toDate());
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/dates_5.dmn")
    public void executeDecision_dates_equals() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        processVariablesInput.put("input1", localDate.toDate());
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/dates_5.dmn")
    public void executeDecision_local_dates_equals() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        processVariablesInput.put("input1", localDate);
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/strings_1.dmn")
    public void executeDecision_String_on_input() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "testString");
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertNotNull(result);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test1", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/strings_2.dmn")
    public void executeDecision_empty_strings() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "");
        processVariablesInput.put("input2", "This is a sentence containing foobar words.");

        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_1.dmn")
    public void executeDecision_conlusion_expression_double() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "blablatest");
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(Double.class, result.get("output1").getClass());
        Assert.assertEquals(5D, result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void executeDecision_conclusion_expression_cast_exception() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "blablatest");
        RuleEngineExecutionSingleResult result = ruleService.executeDecisionByKeySingleResultWithAuditTrail("decision", processVariablesInput);
        Assert.assertNotNull(result.getAuditTrail().getRuleExecutions().get(2).getConclusionResults().get(1).getException());
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void executeDecision_failed_state_missing_input_variable() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        RuleEngineExecutionSingleResult result = ruleService.executeDecisionByKeySingleResultWithAuditTrail("decision", processVariablesInput);
        Assert.assertEquals(true, result.getAuditTrail().isFailed());
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_3.dmn")
    public void executeDecision_missing_input_variable_boolean() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        RuleEngineExecutionSingleResult result = ruleService.executeDecisionByKeySingleResultWithAuditTrail("decision", processVariablesInput);
        Assert.assertEquals(false, result.getAuditTrail().isFailed());
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_4.dmn")
    public void executeDecision_failed_state_unknown_function_outcome_expression() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "blablatest");
        RuleEngineExecutionSingleResult result = ruleService.executeDecisionByKeySingleResultWithAuditTrail("decision", processVariablesInput);
        Assert.assertEquals(true, result.getAuditTrail().isFailed());
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/outcome_expression_2.dmn")
    public void executeDecision_failed_state_could_not_create_outcome() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "blablatest");
        RuleEngineExecutionSingleResult result = ruleService.executeDecisionByKeySingleResultWithAuditTrail("decision", processVariablesInput);
        Assert.assertEquals(true, result.getAuditTrail().isFailed());
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/empty_expressions.dmn")
    public void executeDecision_empty_expressions() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        processVariablesInput.put("input1", "testblabla");
        RuleEngineExecutionSingleResult result = ruleService.executeDecisionByKeySingleResultWithAuditTrail("decision", processVariablesInput);
        Assert.assertEquals(false, result.getAuditTrail().isFailed());
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/dates_4.dmn")
    public void executeDecision_input_null() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("input1", null);
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/reservered_word.dmn")
    public void executeDecision_reserved_word() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        processVariablesInput.put("date", localDate.toDate());
        Map<String, Object> result = ruleService.executeDecisionByKeySingleResult("decision", processVariablesInput);
        Assert.assertSame(String.class, result.get("output1").getClass());
        Assert.assertEquals("test2", result.get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/empty_tokens.dmn")
    public void empty_tokens() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("input1", "AAA");
        processVariablesInput.put("input2", "BBB");

        List<Map<String, Object>> result = ruleService.executeDecisionByKey("decision", processVariablesInput);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("THIRD", result.get(0).get("output1"));
        Assert.assertEquals("FIRST", result.get(1).get("output1"));
        Assert.assertEquals("SECOND", result.get(2).get("output1"));
    }

    @Test
    @DmnDeploymentAnnotation(resources = "org/flowable/dmn/engine/test/deployment/risk_rating_spec_example.dmn")
    public void risk_rating() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        processVariablesInput.put("age", 17);
        processVariablesInput.put("riskcategory", "HIGH");
        processVariablesInput.put("debtreview", true);

        List<Map<String, Object>> result = ruleService.executeDecisionByKey("RiskRatingDecisionTable", processVariablesInput);

        Map ruleResult1 = result.get(0);
        Map ruleResult2 = result.get(1);
        Map ruleResult3 = result.get(2);
        Map ruleResult4 = result.get(3);

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

}
