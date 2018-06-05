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
package org.flowable.editor.dmn.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DecisionTableOrientation;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.dmn.model.UnaryTests;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class DmnJsonConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnJsonConverterTest.class);

    private static final String JSON_RESOURCE_1 = "org/flowable/editor/dmn/converter/decisiontable_1.json";
    private static final String JSON_RESOURCE_2 = "org/flowable/editor/dmn/converter/decisiontable_no_rules.json";
    private static final String JSON_RESOURCE_3 = "org/flowable/editor/dmn/converter/decisiontable_2.json";
    private static final String JSON_RESOURCE_4 = "org/flowable/editor/dmn/converter/decisiontable_empty_expressions.json";
    private static final String JSON_RESOURCE_5 = "org/flowable/editor/dmn/converter/decisiontable_order.json";
    private static final String JSON_RESOURCE_6 = "org/flowable/editor/dmn/converter/decisiontable_entries.json";
    private static final String JSON_RESOURCE_7 = "org/flowable/editor/dmn/converter/decisiontable_dates.json";
    private static final String JSON_RESOURCE_8 = "org/flowable/editor/dmn/converter/decisiontable_empty_operator.json";
    private static final String JSON_RESOURCE_9 = "org/flowable/editor/dmn/converter/decisiontable_complex_output_expression_regression.json";
    private static final String JSON_RESOURCE_10 = "org/flowable/editor/dmn/converter/decisiontable_regression_model_v1.json";
    private static final String JSON_RESOURCE_11 = "org/flowable/editor/dmn/converter/decisiontable_regression_model_v1_no_type.json";
    private static final String JSON_RESOURCE_12 = "org/flowable/editor/dmn/converter/decisiontable_regression_model_v1_no_type2.json";
    private static final String JSON_RESOURCE_13 = "org/flowable/editor/dmn/converter/decisiontable_regression_model_v1_no_type3.json";
    private static final String JSON_RESOURCE_14 = "org/flowable/editor/dmn/converter/decisiontable_regression_model_v1_no_type4.json";
    private static final String JSON_RESOURCE_15 = "org/flowable/editor/dmn/converter/decisiontable_aggregation.json";
    private static final String JSON_RESOURCE_16 = "org/flowable/editor/dmn/converter/decisiontable_special_characters.json";
    private static final String JSON_RESOURCE_17 = "org/flowable/editor/dmn/converter/decisiontable_custom_input_expression.json";
    private static final String JSON_RESOURCE_18 = "org/flowable/editor/dmn/converter/decisiontable_collections_collection_input.json";
    private static final String JSON_RESOURCE_19 = "org/flowable/editor/dmn/converter/decisiontable_collections_collection_compare.json";
    private static final String JSON_RESOURCE_20 = "org/flowable/editor/dmn/converter/decisiontable_complex_output_expression.json";


    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void testConvertJsonToDmnOK() {

        JsonNode testJsonResource = parseJson(JSON_RESOURCE_1);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertNotNull(dmnDefinition);
        assertEquals(DmnJsonConverter.MODEL_NAMESPACE, dmnDefinition.getNamespace());
        assertEquals(DmnJsonConverter.URI_JSON, dmnDefinition.getTypeLanguage());
        assertEquals("definition_abc", dmnDefinition.getId());
        assertEquals("decisionTableRule1", dmnDefinition.getName());

        assertNotNull(dmnDefinition.getDecisions());
        assertEquals(1, dmnDefinition.getDecisions().size());

        Decision decision = dmnDefinition.getDecisions().get(0);
        assertNotNull(decision);
        assertEquals("decTable1", decision.getId());

        DecisionTable decisionTable = (DecisionTable) decision.getExpression();
        assertNotNull(decisionTable);
        assertEquals("decisionTable_11", decisionTable.getId());
        assertEquals(HitPolicy.ANY, decisionTable.getHitPolicy());
        assertEquals(DecisionTableOrientation.RULE_AS_ROW, decisionTable.getPreferredOrientation());

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertNotNull(inputClauses);
        assertEquals(2, inputClauses.size());

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertNotNull(outputClauses);
        assertEquals(1, outputClauses.size());

        // Condition 1
        InputClause condition1 = inputClauses.get(0);
        assertNotNull(condition1.getInputExpression());

        LiteralExpression inputExpression11 = condition1.getInputExpression();
        assertNotNull(inputExpression11);
        assertEquals("Order Size", inputExpression11.getLabel());
        assertEquals("inputExpression_input1", inputExpression11.getId());
        assertEquals("number", inputExpression11.getTypeRef());

        // Condition 2
        InputClause condition2 = inputClauses.get(1);
        assertNotNull(condition2.getInputExpression());

        LiteralExpression inputExpression21 = condition2.getInputExpression();
        assertNotNull(inputExpression21);
        assertEquals("Registered On", inputExpression21.getLabel());
        assertEquals("inputExpression_input2", inputExpression21.getId());
        assertEquals("date", inputExpression21.getTypeRef());

        // Conclusion 1
        OutputClause conclusion1 = outputClauses.get(0);
        assertNotNull(conclusion1);

        assertEquals("Has discount", conclusion1.getLabel());
        assertEquals("outputExpression_output1", conclusion1.getId());
        assertEquals("boolean", conclusion1.getTypeRef());
        assertEquals("newVariable1", conclusion1.getName());

        // Rule 1
        assertNotNull(decisionTable.getRules());
        assertEquals(2, decisionTable.getRules().size());

        List<DecisionRule> rules = decisionTable.getRules();

        assertEquals(2, rules.get(0).getInputEntries().size());

        // input expression 1
        RuleInputClauseContainer ruleClauseContainer11 = rules.get(0).getInputEntries().get(0);
        UnaryTests inputEntry11 = ruleClauseContainer11.getInputEntry();
        assertNotNull(inputEntry11);
        assertEquals("< 10", inputEntry11.getText());
        assertSame(condition1, ruleClauseContainer11.getInputClause());

        // input expression 2
        RuleInputClauseContainer ruleClauseContainer12 = rules.get(0).getInputEntries().get(1);
        UnaryTests inputEntry12 = ruleClauseContainer12.getInputEntry();
        assertNotNull(inputEntry12);
        assertEquals("<= date:toDate('1977-09-18')", inputEntry12.getText());
        assertSame(condition2, ruleClauseContainer12.getInputClause());

        // output expression 1
        assertEquals(1, rules.get(0).getOutputEntries().size());
        RuleOutputClauseContainer ruleClauseContainer13 = rules.get(0).getOutputEntries().get(0);
        LiteralExpression outputEntry13 = ruleClauseContainer13.getOutputEntry();
        assertNotNull(outputEntry13);
        assertEquals("false", outputEntry13.getText());
        assertSame(conclusion1, ruleClauseContainer13.getOutputClause());

        // input expression 1
        RuleInputClauseContainer ruleClauseContainer21 = rules.get(1).getInputEntries().get(0);
        UnaryTests inputEntry21 = ruleClauseContainer21.getInputEntry();
        assertNotNull(inputEntry21);
        assertEquals("> 10", inputEntry21.getText());
        assertSame(condition1, ruleClauseContainer21.getInputClause());

        // input expression 2
        RuleInputClauseContainer ruleClauseContainer22 = rules.get(1).getInputEntries().get(1);
        UnaryTests inputEntry22 = ruleClauseContainer22.getInputEntry();
        assertNotNull(inputEntry22);
        assertEquals("> date:toDate('1977-09-18')", inputEntry22.getText());
        assertSame(condition2, ruleClauseContainer22.getInputClause());

        // output expression 1
        assertEquals(1, rules.get(1).getOutputEntries().size());
        RuleOutputClauseContainer ruleClauseContainer23 = rules.get(1).getOutputEntries().get(0);
        LiteralExpression outputEntry23 = ruleClauseContainer23.getOutputEntry();
        assertNotNull(outputEntry23);
        assertEquals("true", outputEntry23.getText());
        assertSame(conclusion1, ruleClauseContainer23.getOutputClause());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnNoRules() {

        JsonNode testJsonResource = parseJson(JSON_RESOURCE_2);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertNotNull(dmnDefinition);
        assertEquals(DmnJsonConverter.MODEL_NAMESPACE, dmnDefinition.getNamespace());
        assertEquals("definition_abc", dmnDefinition.getId());
        assertEquals("decisionTableRule1", dmnDefinition.getName());
        assertEquals(DmnJsonConverter.URI_JSON, dmnDefinition.getTypeLanguage());

        assertNotNull(dmnDefinition.getDecisions());
        assertEquals(1, dmnDefinition.getDecisions().size());

        Decision decision = dmnDefinition.getDecisions().get(0);
        assertNotNull(decision);
        assertEquals("decTable1", decision.getId());

        DecisionTable decisionTable = (DecisionTable) decision.getExpression();
        assertNotNull(decisionTable);

        assertEquals("decisionTable_11", decisionTable.getId());
        assertEquals(HitPolicy.ANY, decisionTable.getHitPolicy());
        assertEquals(DecisionTableOrientation.RULE_AS_ROW, decisionTable.getPreferredOrientation());

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertNotNull(inputClauses);
        assertEquals(2, inputClauses.size());

        LiteralExpression inputExpression11 = inputClauses.get(0).getInputExpression();
        assertNotNull(inputExpression11);
        assertEquals("Order Size", inputExpression11.getLabel());
        assertEquals("inputExpression_1", inputExpression11.getId());
        assertEquals("number", inputExpression11.getTypeRef());
        assertEquals("ordersize", inputExpression11.getText());

        LiteralExpression inputExpression12 = inputClauses.get(1).getInputExpression();
        assertNotNull(inputExpression12);
        assertEquals("Registered On", inputExpression12.getLabel());
        assertEquals("inputExpression_2", inputExpression12.getId());
        assertEquals("date", inputExpression12.getTypeRef());
        assertEquals("registered", inputExpression12.getText());

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertNotNull(outputClauses);
        assertEquals(1, outputClauses.size());

        // Condition 1
        OutputClause outputClause1 = outputClauses.get(0);
        assertNotNull(outputClause1);
        assertEquals("Has discount", outputClause1.getLabel());
        assertEquals("outputExpression_3", outputClause1.getId());
        assertEquals("newVariable1", outputClause1.getName());
        assertEquals("boolean", outputClause1.getTypeRef());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmn2OK() {

        JsonNode testJsonResource = parseJson(JSON_RESOURCE_3);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertNotNull(dmnDefinition);

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnEmptyExpressions() {

        JsonNode testJsonResource = parseJson(JSON_RESOURCE_4);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertNotNull(dmnDefinition);

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("-", decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-", decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-", decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnConditionOrder() {
        // Test that editor json, which contains the rules in the incorrect order in
        // the rule object,
        // is converted to a dmn model where the rule columns are in the same order
        // as the input/output clauses
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_5);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertNotNull(dmnDefinition);

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        List<DecisionRule> rules = decisionTable.getRules();
        assertNotNull(rules);
        assertEquals(1, rules.size());
        assertNotNull(rules.get(0).getOutputEntries());
        assertEquals(3, rules.get(0).getOutputEntries().size());
        assertEquals("outputExpression_14", rules.get(0).getOutputEntries().get(0).getOutputClause().getId());
        assertEquals("outputExpression_13", rules.get(0).getOutputEntries().get(1).getOutputClause().getId());
        assertEquals("outputExpression_15", rules.get(0).getOutputEntries().get(2).getOutputClause().getId());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnEntries() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_6);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("OUTPUT ORDER", decisionTable.getHitPolicy().getValue());

        assertEquals("\"AAA\",\"BBB\"", decisionTable.getInputs().get(0).getInputValues().getText());
        assertEquals("AAA", decisionTable.getInputs().get(0).getInputValues().getTextValues().get(0));
        assertEquals("BBB", decisionTable.getInputs().get(0).getInputValues().getTextValues().get(1));

        assertEquals("\"THIRD\",\"FIRST\",\"SECOND\"", decisionTable.getOutputs().get(0).getOutputValues().getText());
        assertEquals("THIRD", decisionTable.getOutputs().get(0).getOutputValues().getTextValues().get(0));
        assertEquals("FIRST", decisionTable.getOutputs().get(0).getOutputValues().getTextValues().get(1));
        assertEquals("SECOND", decisionTable.getOutputs().get(0).getOutputValues().getTextValues().get(2));

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnDates() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_7);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("== date:toDate('14-06-2017')",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("!= date:toDate('16-06-2017')",  decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnEmptyOperator() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_8);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();
        assertEquals("date:toDate('2017-06-01')", decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-", decisionTable.getRules().get(0).getInputEntries().get(1).getInputEntry().getText());
        assertNotNull(decisionTable.getRules().get(0).getInputEntries().get(0).getInputClause());
        assertNotNull(decisionTable.getRules().get(0).getInputEntries().get(1).getInputClause());

        assertEquals("date:toDate('2017-06-02')", decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-", decisionTable.getRules().get(1).getInputEntries().get(1).getInputEntry().getText());
        assertNotNull(decisionTable.getRules().get(1).getInputEntries().get(0).getInputClause());
        assertNotNull(decisionTable.getRules().get(1).getInputEntries().get(1).getInputClause());

        assertEquals("date:toDate('2017-06-03')", decisionTable.getRules().get(0).getOutputEntries().get(0).getOutputEntry().getText());
        assertEquals("", decisionTable.getRules().get(1).getOutputEntries().get(0).getOutputEntry().getText());
        assertNotNull(decisionTable.getRules().get(0).getOutputEntries().get(0).getOutputClause());
        assertNotNull(decisionTable.getRules().get(1).getOutputEntries().get(0).getOutputClause());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnComplexOutputExpressionRegression() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_9);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();
        assertEquals("refVar1 * refVar2", decisionTable.getRules().get(0).getOutputEntries().get(0).getOutputEntry().getText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnComplexOutputExpression() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_20);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();
        assertEquals("${refVar1 * refVar2}", decisionTable.getRules().get(0).getOutputEntries().get(0).getOutputEntry().getText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_10);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();
        assertEquals(4, decisionTable.getInputs().size());
        assertEquals(4, decisionTable.getOutputs().size());
        assertEquals(4, decisionTable.getRules().get(0).getInputEntries().size());
        assertEquals(4, decisionTable.getRules().get(0).getOutputEntries().size());

        DecisionRule rule1 = decisionTable.getRules().get(0);
        DecisionRule rule2 = decisionTable.getRules().get(1);

        assertEquals("== \"TEST\"", rule1.getInputEntries().get(0).getInputEntry().getText());
        assertEquals("== 100", rule1.getInputEntries().get(1).getInputEntry().getText());
        assertEquals("== true", rule1.getInputEntries().get(2).getInputEntry().getText());
        assertEquals("== date:toDate('2017-06-01')", rule1.getInputEntries().get(3).getInputEntry().getText());

        assertEquals("\"WAS TEST\"", rule1.getOutputEntries().get(0).getOutputEntry().getText());
        assertEquals("100", rule1.getOutputEntries().get(1).getOutputEntry().getText());
        assertEquals("true", rule1.getOutputEntries().get(2).getOutputEntry().getText());
        assertEquals("date:toDate('2017-06-01')", rule1.getOutputEntries().get(3).getOutputEntry().getText());

        assertEquals("!= \"TEST\"", rule2.getInputEntries().get(0).getInputEntry().getText());
        assertEquals("!= 100", rule2.getInputEntries().get(1).getInputEntry().getText());
        assertEquals("== false", rule2.getInputEntries().get(2).getInputEntry().getText());
        assertEquals("!= date:toDate('2017-06-01')", rule2.getInputEntries().get(3).getInputEntry().getText());

        assertEquals("\"WASN'T TEST\"", rule2.getOutputEntries().get(0).getOutputEntry().getText());
        assertEquals("1", rule2.getOutputEntries().get(1).getOutputEntry().getText());
        assertEquals("false", rule2.getOutputEntries().get(2).getOutputEntry().getText());
        assertEquals("date:toDate('2016-06-01')", rule2.getOutputEntries().get(3).getOutputEntry().getText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1NoType() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_11);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("string", decisionTable.getInputs().get(0).getInputExpression().getTypeRef());
        assertEquals("number", decisionTable.getInputs().get(1).getInputExpression().getTypeRef());
        assertEquals("boolean", decisionTable.getInputs().get(2).getInputExpression().getTypeRef());
        assertEquals("date", decisionTable.getInputs().get(3).getInputExpression().getTypeRef());
        assertEquals("string", decisionTable.getOutputs().get(0).getTypeRef());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1NoType2() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_12);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("string", decisionTable.getInputs().get(0).getInputExpression().getTypeRef());
        assertEquals("number", decisionTable.getInputs().get(1).getInputExpression().getTypeRef());
        assertEquals("boolean", decisionTable.getInputs().get(2).getInputExpression().getTypeRef());
        assertEquals("date", decisionTable.getInputs().get(3).getInputExpression().getTypeRef());
        assertEquals("string", decisionTable.getOutputs().get(0).getTypeRef());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1NoType3() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_13);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("string", decisionTable.getInputs().get(0).getInputExpression().getTypeRef());
        assertEquals("string", decisionTable.getOutputs().get(0).getTypeRef());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1NoType4() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_14);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("number", decisionTable.getInputs().get(0).getInputExpression().getTypeRef());
        assertEquals("boolean", decisionTable.getOutputs().get(0).getTypeRef());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnCollectOperator() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_15);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("SUM", decisionTable.getAggregation().getValue());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnStringSpecialCharacters() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_16);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("== \"TEST\"",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("== \"TEST\"",  decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnCustomExpressions() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_17);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("${inputVar4 != null}",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("#{inputVar4 > date:now()}",  decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
    }

    @Test
    public void testConvertJsonToDmnCollectionsCollectionInput() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_18);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("${collection:notIn(collection1, \"testValue\")}",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(collection1, \"testValue\")}",  decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(collection1, 'testVar1,testVar2')}",  decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(collection1, '\"testValue1\",\"testValue2\"')}",  decisionTable.getRules().get(3).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(collection1, '10,20')}",  decisionTable.getRules().get(4).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(collection1, 10)}",  decisionTable.getRules().get(5).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-",  decisionTable.getRules().get(6).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:notIn(collection1, \"testValue\")}",  decisionTable.getRules().get(7).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-",  decisionTable.getRules().get(8).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:any(collection1, \"testValue\")}",  decisionTable.getRules().get(9).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-",  decisionTable.getRules().get(10).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("== \"testValue\"",  decisionTable.getRules().get(11).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("== testCollection",  decisionTable.getRules().get(12).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("!= \"testValue\"",  decisionTable.getRules().get(13).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(collection1, '\"test,Value1\",\"test,Value2\"')}",  decisionTable.getRules().get(14).getInputEntries().get(0).getInputEntry().getText());
        
        assertEquals("${collection:contains(collection1, \"testValue\")}",  decisionTable.getRules().get(15).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:notContains(collection1, \"testValue\")}",  decisionTable.getRules().get(16).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:containsAny(collection1, '\"testValue1\",\"testValue2\"')}",  decisionTable.getRules().get(17).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:notContainsAny(collection1, '\"testValue1\",\"testValue2\"')}",  decisionTable.getRules().get(18).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:contains(collection1, '\"testValue1\",\"testValue2\"')}",  decisionTable.getRules().get(19).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:notContains(collection1, '\"testValue1\",\"testValue2\"')}",  decisionTable.getRules().get(20).getInputEntries().get(0).getInputEntry().getText());

        // extension elements
        assertEquals("NOT IN",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getExtensionElements().get("operator").get(0).getElementText());
        assertEquals("\"testValue\"",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getExtensionElements().get("expression").get(0).getElementText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
        assertEquals("NOT IN", modelerJson.get("rules").get(0).get("inputExpression_1_operator").asText());
        assertEquals("\"testValue\"", modelerJson.get("rules").get(0).get("inputExpression_1_expression").asText());
        assertEquals("IN", modelerJson.get("rules").get(1).get("inputExpression_1_operator").asText());
        assertEquals("\"testValue\"", modelerJson.get("rules").get(1).get("inputExpression_1_expression").asText());
        assertEquals("IN", modelerJson.get("rules").get(2).get("inputExpression_1_operator").asText());
        assertEquals("testVar1, testVar2", modelerJson.get("rules").get(2).get("inputExpression_1_expression").asText());
    }

    @Test
    public void testConvertJsonToDmnCollectionsCollectionCompare() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_19);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("${collection:notIn(input1, \"testValue\")}",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(input1, \"testValue\")}",  decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(input1, 'testVar1,testVar2')}",  decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(input1, '\"testValue1\",\"testValue2\"')}",  decisionTable.getRules().get(3).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(input1, '10,20')}",  decisionTable.getRules().get(4).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(input1, 10)}",  decisionTable.getRules().get(5).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-",  decisionTable.getRules().get(6).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:notIn(input1, \"testValue\")}",  decisionTable.getRules().get(7).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-",  decisionTable.getRules().get(8).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:any(input1, \"testValue\")}",  decisionTable.getRules().get(9).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("-",  decisionTable.getRules().get(10).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:in(input1, '\"test,Value1\",\"test,Value2\"')}",  decisionTable.getRules().get(11).getInputEntries().get(0).getInputEntry().getText());

        // extension elements
        assertEquals("NOT IN",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getExtensionElements().get("operator").get(0).getElementText());
        assertEquals("\"testValue\"",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getExtensionElements().get("expression").get(0).getElementText());

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertNotNull(modelerJson);
        assertEquals("NOT IN", modelerJson.get("rules").get(0).get("inputExpression_1_operator").asText());
        assertEquals("\"testValue\"", modelerJson.get("rules").get(0).get("inputExpression_1_expression").asText());
        assertEquals("IN", modelerJson.get("rules").get(1).get("inputExpression_1_operator").asText());
        assertEquals("\"testValue\"", modelerJson.get("rules").get(1).get("inputExpression_1_expression").asText());
        assertEquals("IN", modelerJson.get("rules").get(2).get("inputExpression_1_operator").asText());
        assertEquals("testVar1, testVar2", modelerJson.get("rules").get(2).get("inputExpression_1_expression").asText());
    }

    /* Helper methods */
    protected String readJsonToString(String resource) {
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(resource);
            return IOUtils.toString(is);
        } catch (IOException e) {
            fail("Could not read " + resource + " : " + e.getMessage());
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    protected JsonNode parseJson(String resource) {
        String jsonString = readJsonToString(resource);
        try {
            return OBJECT_MAPPER.readTree(jsonString);
        } catch (IOException e) {
            fail("Could not parse " + resource + " : " + e.getMessage());
        }
        return null;
    }
}
