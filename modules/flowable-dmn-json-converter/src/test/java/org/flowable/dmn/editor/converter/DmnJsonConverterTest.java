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
package org.flowable.dmn.editor.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.filter.Filters.filter;
import static org.assertj.core.data.Offset.offset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DecisionTableOrientation;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.dmn.model.UnaryTests;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yvo Swillens
 */
public class DmnJsonConverterTest {

    private static final String JSON_RESOURCE_1 = "org/flowable/dmn/editor/converter/decisiontable_1.json";
    private static final String JSON_RESOURCE_2 = "org/flowable/dmn/editor/converter/decisiontable_no_rules.json";
    private static final String JSON_RESOURCE_3 = "org/flowable/dmn/editor/converter/decisiontable_2.json";
    private static final String JSON_RESOURCE_4 = "org/flowable/dmn/editor/converter/decisiontable_empty_expressions.json";
    private static final String JSON_RESOURCE_5 = "org/flowable/dmn/editor/converter/decisiontable_order.json";
    private static final String JSON_RESOURCE_6 = "org/flowable/dmn/editor/converter/decisiontable_entries.json";
    private static final String JSON_RESOURCE_7 = "org/flowable/dmn/editor/converter/decisiontable_dates.json";
    private static final String JSON_RESOURCE_8 = "org/flowable/dmn/editor/converter/decisiontable_empty_operator.json";
    private static final String JSON_RESOURCE_9 = "org/flowable/dmn/editor/converter/decisiontable_complex_output_expression_regression.json";
    private static final String JSON_RESOURCE_10 = "org/flowable/dmn/editor/converter/decisiontable_regression_model_v1.json";
    private static final String JSON_RESOURCE_11 = "org/flowable/dmn/editor/converter/decisiontable_regression_model_v1_no_type.json";
    private static final String JSON_RESOURCE_12 = "org/flowable/dmn/editor/converter/decisiontable_regression_model_v1_no_type2.json";
    private static final String JSON_RESOURCE_13 = "org/flowable/dmn/editor/converter/decisiontable_regression_model_v1_no_type3.json";
    private static final String JSON_RESOURCE_14 = "org/flowable/dmn/editor/converter/decisiontable_regression_model_v1_no_type4.json";
    private static final String JSON_RESOURCE_15 = "org/flowable/dmn/editor/converter/decisiontable_aggregation.json";
    private static final String JSON_RESOURCE_16 = "org/flowable/dmn/editor/converter/decisiontable_special_characters.json";
    private static final String JSON_RESOURCE_17 = "org/flowable/dmn/editor/converter/decisiontable_custom_input_expression.json";
    private static final String JSON_RESOURCE_18 = "org/flowable/dmn/editor/converter/decisiontable_collections_collection_input.json";
    private static final String JSON_RESOURCE_19 = "org/flowable/dmn/editor/converter/decisiontable_collections_collection_compare.json";
    private static final String JSON_RESOURCE_20 = "org/flowable/dmn/editor/converter/decisiontable_complex_output_expression.json";
    private static final String JSON_RESOURCE_21 = "org/flowable/dmn/editor/converter/decisiontable_forceDMN11.json";
    private static final String JSON_RESOURCE_22 = "org/flowable/dmn/editor/converter/decisiontable_empty_outcomes.json";
    private static final String JSON_RESOURCE_23 = "org/flowable/dmn/editor/converter/decisionservice_1.json";
    private static final String JSON_RESOURCE_24 = "org/flowable/dmn/editor/converter/decisionservice_1_no_info_reqs.json";
    private static final String JSON_RESOURCE_25 = "org/flowable/dmn/editor/converter/decisionservice_1_no_decisions.json";
    private static final String JSON_RESOURCE_26 = "org/flowable/dmn/editor/converter/decisionservice_2.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void testConvertJsonToDmnOK() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_1);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertThat(dmnDefinition).isNotNull();
        assertThat(dmnDefinition.getNamespace()).isEqualTo(DmnJsonConverter.MODEL_NAMESPACE);
        assertThat(dmnDefinition.getTypeLanguage()).isEqualTo(DmnJsonConverter.URI_JSON);
        assertThat(dmnDefinition.getId()).isEqualTo("definition_abc");
        assertThat(dmnDefinition.getName()).isEqualTo("decisionTableRule1");

        assertThat(dmnDefinition.getDecisions()).hasSize(1);

        Decision decision = dmnDefinition.getDecisions().get(0);
        assertThat(decision).isNotNull();
        assertThat(decision.getId()).isEqualTo("decTable1");

        DecisionTable decisionTable = (DecisionTable) decision.getExpression();
        assertThat(decisionTable).isNotNull();
        assertThat(decisionTable.getHitPolicy()).isEqualTo(HitPolicy.ANY);
        assertThat(decisionTable.getPreferredOrientation()).isEqualTo(DecisionTableOrientation.RULE_AS_ROW);

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertThat(inputClauses).hasSize(2);

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertThat(outputClauses).hasSize(1);

        // Condition 1
        InputClause condition1 = inputClauses.get(0);
        assertThat(condition1.getInputExpression()).isNotNull();

        LiteralExpression inputExpression11 = condition1.getInputExpression();
        assertThat(inputExpression11).isNotNull();
        assertThat(inputExpression11.getLabel()).isEqualTo("Order Size");
        assertThat(inputExpression11.getTypeRef()).isEqualTo("number");

        // Condition 2
        InputClause condition2 = inputClauses.get(1);
        assertThat(condition2.getInputExpression()).isNotNull();

        LiteralExpression inputExpression21 = condition2.getInputExpression();
        assertThat(inputExpression21).isNotNull();
        assertThat(inputExpression21.getLabel()).isEqualTo("Registered On");
        assertThat(inputExpression21.getTypeRef()).isEqualTo("date");

        // Conclusion 1
        OutputClause conclusion1 = outputClauses.get(0);
        assertThat(conclusion1).isNotNull();

        assertThat(conclusion1.getLabel()).isEqualTo("Has discount");
        assertThat(conclusion1.getTypeRef()).isEqualTo("boolean");
        assertThat(conclusion1.getName()).isEqualTo("newVariable1");

        // Rule 1
        assertThat(decisionTable.getRules()).hasSize(2);

        List<DecisionRule> rules = decisionTable.getRules();

        assertThat(rules.get(0).getInputEntries()).hasSize(2);

        // input expression 1
        RuleInputClauseContainer ruleClauseContainer11 = rules.get(0).getInputEntries().get(0);
        UnaryTests inputEntry11 = ruleClauseContainer11.getInputEntry();
        assertThat(inputEntry11).isNotNull();
        assertThat(inputEntry11.getText()).isEqualTo("< 10");
        assertThat(ruleClauseContainer11.getInputClause()).isSameAs(condition1);

        // input expression 2
        RuleInputClauseContainer ruleClauseContainer12 = rules.get(0).getInputEntries().get(1);
        UnaryTests inputEntry12 = ruleClauseContainer12.getInputEntry();
        assertThat(inputEntry12).isNotNull();
        assertThat(inputEntry12.getText()).isEqualTo("<= date:toDate('1977-09-18')");
        assertThat(ruleClauseContainer12.getInputClause()).isSameAs(condition2);

        // output expression 1
        assertThat(rules.get(0).getOutputEntries()).hasSize(1);
        RuleOutputClauseContainer ruleClauseContainer13 = rules.get(0).getOutputEntries().get(0);
        LiteralExpression outputEntry13 = ruleClauseContainer13.getOutputEntry();
        assertThat(outputEntry13).isNotNull();
        assertThat(outputEntry13.getText()).isEqualTo("false");
        assertThat(ruleClauseContainer13.getOutputClause()).isSameAs(conclusion1);

        // input expression 1
        RuleInputClauseContainer ruleClauseContainer21 = rules.get(1).getInputEntries().get(0);
        UnaryTests inputEntry21 = ruleClauseContainer21.getInputEntry();
        assertThat(inputEntry21).isNotNull();
        assertThat(inputEntry21.getText()).isEqualTo("> 10");
        assertThat(ruleClauseContainer21.getInputClause()).isSameAs(condition1);

        // input expression 2
        RuleInputClauseContainer ruleClauseContainer22 = rules.get(1).getInputEntries().get(1);
        UnaryTests inputEntry22 = ruleClauseContainer22.getInputEntry();
        assertThat(inputEntry22).isNotNull();
        assertThat(inputEntry22.getText()).isEqualTo("> date:toDate('1977-09-18')");
        assertThat(ruleClauseContainer22.getInputClause()).isSameAs(condition2);

        // output expression 1
        assertThat(rules.get(1).getOutputEntries()).hasSize(1);
        RuleOutputClauseContainer ruleClauseContainer23 = rules.get(1).getOutputEntries().get(0);
        LiteralExpression outputEntry23 = ruleClauseContainer23.getOutputEntry();
        assertThat(outputEntry23).isNotNull();
        assertThat(outputEntry23.getText()).isEqualTo("true");
        assertThat(ruleClauseContainer23.getOutputClause()).isSameAs(conclusion1);

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnNoRules() {

        JsonNode testJsonResource = parseJson(JSON_RESOURCE_2);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertThat(dmnDefinition).isNotNull();
        assertThat(dmnDefinition.getNamespace()).isEqualTo(DmnJsonConverter.MODEL_NAMESPACE);
        assertThat(dmnDefinition.getId()).isEqualTo("definition_abc");
        assertThat(dmnDefinition.getName()).isEqualTo("decisionTableRule1");
        assertThat(dmnDefinition.getTypeLanguage()).isEqualTo(DmnJsonConverter.URI_JSON);

        assertThat(dmnDefinition.getDecisions()).hasSize(1);

        Decision decision = dmnDefinition.getDecisions().get(0);
        assertThat(decision).isNotNull();
        assertThat(decision.getId()).isEqualTo("decTable1");

        DecisionTable decisionTable = (DecisionTable) decision.getExpression();
        assertThat(decisionTable).isNotNull();

        assertThat(decisionTable.getHitPolicy()).isEqualTo(HitPolicy.ANY);
        assertThat(decisionTable.getPreferredOrientation()).isEqualTo(DecisionTableOrientation.RULE_AS_ROW);

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertThat(inputClauses).hasSize(2);

        LiteralExpression inputExpression11 = inputClauses.get(0).getInputExpression();
        assertThat(inputExpression11).isNotNull();
        assertThat(inputExpression11.getLabel()).isEqualTo("Order Size");
        assertThat(inputExpression11.getTypeRef()).isEqualTo("number");
        assertThat(inputExpression11.getText()).isEqualTo("ordersize");

        LiteralExpression inputExpression12 = inputClauses.get(1).getInputExpression();
        assertThat(inputExpression12).isNotNull();
        assertThat(inputExpression12.getLabel()).isEqualTo("Registered On");
        assertThat(inputExpression12.getTypeRef()).isEqualTo("date");
        assertThat(inputExpression12.getText()).isEqualTo("registered");

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertThat(outputClauses).hasSize(1);

        // Condition 1
        OutputClause outputClause1 = outputClauses.get(0);
        assertThat(outputClause1).isNotNull();
        assertThat(outputClause1.getLabel()).isEqualTo("Has discount");
        assertThat(outputClause1.getName()).isEqualTo("newVariable1");
        assertThat(outputClause1.getTypeRef()).isEqualTo("boolean");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmn2OK() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_3);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertThat(dmnDefinition).isNotNull();

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnEmptyExpressions() {

        JsonNode testJsonResource = parseJson(JSON_RESOURCE_4);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertThat(dmnDefinition).isNotNull();

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnConditionOrder() {
        // Test that editor json, which contains the rules in the incorrect order in
        // the rule object,
        // is converted to a dmn model where the rule columns are in the same order
        // as the input/output clauses
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_5);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertThat(dmnDefinition).isNotNull();

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        List<DecisionRule> rules = decisionTable.getRules();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getOutputEntries()).hasSize(3);
        assertThat(rules.get(0).getOutputEntries().get(0).getOutputClause().getName()).isEqualTo("boolvarfield");
        assertThat(rules.get(0).getOutputEntries().get(1).getOutputClause().getName()).isEqualTo("datevarfield");
        assertThat(rules.get(0).getOutputEntries().get(2).getOutputClause().getName()).isEqualTo("stringvarfield");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnEntries() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_6);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getHitPolicy().getValue()).isEqualTo("OUTPUT ORDER");

        assertThat(decisionTable.getInputs().get(0).getInputValues().getText()).isEqualTo("\"AAA\",\"BBB\"");
        assertThat(decisionTable.getInputs().get(0).getInputValues().getTextValues().get(0)).isEqualTo("AAA");
        assertThat(decisionTable.getInputs().get(0).getInputValues().getTextValues().get(1)).isEqualTo("BBB");

        assertThat(decisionTable.getInputs().get(2).getInputValues().getText()).isEqualTo("20,10,30");
        assertThat(decisionTable.getInputs().get(2).getInputValues().getTextValues().get(0)).isEqualTo("20");
        assertThat(decisionTable.getInputs().get(2).getInputValues().getTextValues().get(1)).isEqualTo("10");
        assertThat(decisionTable.getInputs().get(2).getInputValues().getTextValues().get(2)).isEqualTo("30");

        assertThat(decisionTable.getOutputs().get(0).getOutputValues().getText()).isEqualTo("\"THIRD\",\"FIRST\",\"SECOND\"");
        assertThat(decisionTable.getOutputs().get(0).getOutputValues().getTextValues().get(0)).isEqualTo("THIRD");
        assertThat(decisionTable.getOutputs().get(0).getOutputValues().getTextValues().get(1)).isEqualTo("FIRST");
        assertThat(decisionTable.getOutputs().get(0).getOutputValues().getTextValues().get(2)).isEqualTo("SECOND");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnDates() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_7);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("== date:toDate('14-06-2017')");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("!= date:toDate('16-06-2017')");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnEmptyOperator() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_8);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("date:toDate('2017-06-01')");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputClause()).isNotNull();
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(1).getInputClause()).isNotNull();

        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("date:toDate('2017-06-02')");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputClause()).isNotNull();
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(1).getInputClause()).isNotNull();

        assertThat(decisionTable.getRules().get(0).getOutputEntries().get(0).getOutputEntry().getText()).isEqualTo("date:toDate('2017-06-03')");
        assertThat(decisionTable.getRules().get(1).getOutputEntries().get(0).getOutputEntry().getText()).isEmpty();
        assertThat(decisionTable.getRules().get(0).getOutputEntries().get(0).getOutputClause()).isNotNull();
        assertThat(decisionTable.getRules().get(1).getOutputEntries().get(0).getOutputClause()).isNotNull();

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnComplexOutputExpressionRegression() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_9);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();
        assertThat(decisionTable.getRules().get(0).getOutputEntries().get(0).getOutputEntry().getText()).isEqualTo("refVar1 * refVar2");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnComplexOutputExpression() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_20);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();
        assertThat(decisionTable.getRules().get(0).getOutputEntries().get(0).getOutputEntry().getText()).isEqualTo("${refVar1 * refVar2}");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_10);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();
        assertThat(decisionTable.getInputs()).hasSize(4);
        assertThat(decisionTable.getOutputs()).hasSize(4);
        assertThat(decisionTable.getRules().get(0).getInputEntries()).hasSize(4);
        assertThat(decisionTable.getRules().get(0).getOutputEntries()).hasSize(4);

        DecisionRule rule1 = decisionTable.getRules().get(0);
        DecisionRule rule2 = decisionTable.getRules().get(1);

        assertThat(rule1.getInputEntries().get(0).getInputEntry().getText()).isEqualTo("== \"TEST\"");
        assertThat(rule1.getInputEntries().get(1).getInputEntry().getText()).isEqualTo("== 100");
        assertThat(rule1.getInputEntries().get(2).getInputEntry().getText()).isEqualTo("== true");
        assertThat(rule1.getInputEntries().get(3).getInputEntry().getText()).isEqualTo("== date:toDate('2017-06-01')");

        assertThat(rule1.getOutputEntries().get(0).getOutputEntry().getText()).isEqualTo("\"WAS TEST\"");
        assertThat(rule1.getOutputEntries().get(1).getOutputEntry().getText()).isEqualTo("100");
        assertThat(rule1.getOutputEntries().get(2).getOutputEntry().getText()).isEqualTo("true");
        assertThat(rule1.getOutputEntries().get(3).getOutputEntry().getText()).isEqualTo("date:toDate('2017-06-01')");

        assertThat(rule2.getInputEntries().get(0).getInputEntry().getText()).isEqualTo("!= \"TEST\"");
        assertThat(rule2.getInputEntries().get(1).getInputEntry().getText()).isEqualTo("!= 100");
        assertThat(rule2.getInputEntries().get(2).getInputEntry().getText()).isEqualTo("== false");
        assertThat(rule2.getInputEntries().get(3).getInputEntry().getText()).isEqualTo("!= date:toDate('2017-06-01')");

        assertThat(rule2.getOutputEntries().get(0).getOutputEntry().getText()).isEqualTo("\"WASN'T TEST\"");
        assertThat(rule2.getOutputEntries().get(1).getOutputEntry().getText()).isEqualTo("1");
        assertThat(rule2.getOutputEntries().get(2).getOutputEntry().getText()).isEqualTo("false");
        assertThat(rule2.getOutputEntries().get(3).getOutputEntry().getText()).isEqualTo("date:toDate('2016-06-01')");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1NoType() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_11);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getInputs().get(0).getInputExpression().getTypeRef()).isEqualTo("string");
        assertThat(decisionTable.getInputs().get(1).getInputExpression().getTypeRef()).isEqualTo("number");
        assertThat(decisionTable.getInputs().get(2).getInputExpression().getTypeRef()).isEqualTo("boolean");
        assertThat(decisionTable.getInputs().get(3).getInputExpression().getTypeRef()).isEqualTo("date");
        assertThat(decisionTable.getOutputs().get(0).getTypeRef()).isEqualTo("string");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1NoType2() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_12);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getInputs().get(0).getInputExpression().getTypeRef()).isEqualTo("string");
        assertThat(decisionTable.getInputs().get(1).getInputExpression().getTypeRef()).isEqualTo("number");
        assertThat(decisionTable.getInputs().get(2).getInputExpression().getTypeRef()).isEqualTo("boolean");
        assertThat(decisionTable.getInputs().get(3).getInputExpression().getTypeRef()).isEqualTo("date");
        assertThat(decisionTable.getOutputs().get(0).getTypeRef()).isEqualTo("string");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1NoType3() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_13);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getInputs().get(0).getInputExpression().getTypeRef()).isEqualTo("string");
        assertThat(decisionTable.getOutputs().get(0).getTypeRef()).isEqualTo("string");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnRegressionModelv1NoType4() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_14);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getInputs().get(0).getInputExpression().getTypeRef()).isEqualTo("number");
        assertThat(decisionTable.getOutputs().get(0).getTypeRef()).isEqualTo("boolean");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnCollectOperator() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_15);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getAggregation().getValue()).isEqualTo("SUM");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnStringSpecialCharacters() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_16);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("== \"TEST\"");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("== \"TEST\"");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnCustomExpressions() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_17);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${inputVar4 != null}");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("#{inputVar4 > date:now()}");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
    }

    @Test
    public void testConvertJsonToDmnCollectionsCollectionInput() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_18);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:noneOf(collection1, \"testValue\")}");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(collection1, \"testValue\")}");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(collection1, 'testVar1,testVar2')}");
        assertThat(decisionTable.getRules().get(3).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(collection1, '\"testValue1\",\"testValue2\"')}");
        assertThat(decisionTable.getRules().get(4).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(collection1, '10,20')}");
        assertThat(decisionTable.getRules().get(5).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(collection1, 10)}");
        assertThat(decisionTable.getRules().get(6).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(7).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:noneOf(collection1, \"testValue\")}");
        assertThat(decisionTable.getRules().get(8).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(9).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:anyOf(collection1, \"testValue\")}");
        assertThat(decisionTable.getRules().get(10).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(11).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("== \"testValue\"");
        assertThat(decisionTable.getRules().get(12).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("== testCollection");
        assertThat(decisionTable.getRules().get(13).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("!= \"testValue\"");
        assertThat(decisionTable.getRules().get(14).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(collection1, '\"test,Value1\",\"test,Value2\"')}");

        // extension elements
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getExtensionElements().get("operator").get(0).getElementText()).isEqualTo("NONE OF");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getExtensionElements().get("expression").get(0).getElementText()).isEqualTo("\"testValue\"");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();
        String inputExpression1 = modelerJson.get("inputExpressions").get(0).get("id").asText();
        assertThat(modelerJson.get("rules").get(0).get(inputExpression1 + "_operator").asText()).isEqualTo("NONE OF");
        assertThat(modelerJson.get("rules").get(0).get(inputExpression1 + "_expression").asText()).isEqualTo("\"testValue\"");
        assertThat(modelerJson.get("rules").get(1).get(inputExpression1 + "_operator").asText()).isEqualTo("ALL OF");
        assertThat(modelerJson.get("rules").get(1).get(inputExpression1 + "_expression").asText()).isEqualTo("\"testValue\"");
        assertThat(modelerJson.get("rules").get(2).get(inputExpression1 + "_operator").asText()).isEqualTo("ALL OF");
        assertThat(modelerJson.get("rules").get(2).get(inputExpression1 + "_expression").asText()).isEqualTo("testVar1, testVar2");
    }

    @Test
    public void testConvertJsonToDmnCollectionsCollectionCompare() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_19);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:noneOf(\"testValue\", input1)}");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(\"testValue\", input1)}");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf('testVar1,testVar2', input1)}");
        assertThat(decisionTable.getRules().get(3).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf('\"testValue1\",\"testValue2\"', input1)}");
        assertThat(decisionTable.getRules().get(4).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf('10,20', input1)}");
        assertThat(decisionTable.getRules().get(5).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(10, input1)}");
        assertThat(decisionTable.getRules().get(6).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(7).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:noneOf(\"testValue\", input1)}");
        assertThat(decisionTable.getRules().get(8).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(9).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(\"testValue\", input1)}");
        assertThat(decisionTable.getRules().get(10).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(11).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf('\"test,Value1\",\"test,Value2\"', input1)}");

        // extension elements
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getExtensionElements().get("operator").get(0).getElementText()).isEqualTo("IS NOT IN");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getExtensionElements().get("expression").get(0).getElementText()).isEqualTo("\"testValue\"");

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);
        assertThat(modelerJson).isNotNull();

        String inputExpression1 = modelerJson.get("inputExpressions").get(0).get("id").asText();
        assertThat(modelerJson.get("rules").get(0).get(inputExpression1 + "_operator").asText()).isEqualTo("IS NOT IN");
        assertThat(modelerJson.get("rules").get(0).get(inputExpression1 + "_expression").asText()).isEqualTo("\"testValue\"");
        assertThat(modelerJson.get("rules").get(1).get(inputExpression1 + "_operator").asText()).isEqualTo("IS IN");
        assertThat(modelerJson.get("rules").get(1).get(inputExpression1 + "_expression").asText()).isEqualTo("\"testValue\"");
        assertThat(modelerJson.get("rules").get(2).get(inputExpression1 + "_operator").asText()).isEqualTo("IS IN");
        assertThat(modelerJson.get("rules").get(2).get(inputExpression1 + "_expression").asText()).isEqualTo("testVar1, testVar2");
    }

    @Test
    public void testConvertJsonToDMNForceDMN11Enabled() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_21);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        Decision decision = dmnDefinition.getDecisions().get(0);

        assertThat(decision.isForceDMN11()).isTrue();
    }

    @Test
    public void testConvertJsonToDMNForceDMN11Disabled() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_1);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        Decision decision = dmnDefinition.getDecisions().get(0);

        assertThat(decision.isForceDMN11()).isFalse();
    }

    @Test
    public void testConvertDecisionServiceJsonToDMN() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_23);
        String testDecJsonResource1 = readJsonToString(JSON_RESOURCE_1);
        String testDecJsonResource2 = readJsonToString(JSON_RESOURCE_5);
        String testDecJsonResource3 = readJsonToString(JSON_RESOURCE_7);
        String testDecJsonResource4 = readJsonToString(JSON_RESOURCE_9);
        String testDecJsonResource5 = readJsonToString(JSON_RESOURCE_10);

        DmnJsonConverterContext converterContext = new StandaloneTestDmnConverterContext();
        Map<String, String> decisionTableMap = converterContext.getDecisionTableKeyToJsonStringMap();
        decisionTableMap.put("decTable1", testDecJsonResource1);
        decisionTableMap.put("decTable2", testDecJsonResource2);
        decisionTableMap.put("decTable3", testDecJsonResource3);
        decisionTableMap.put("ComplexExpression", testDecJsonResource4);
        decisionTableMap.put("decTable4", testDecJsonResource5);

        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", converterContext);

        assertThat(dmnDefinition.getDecisionServices()).hasSize(1);

        DecisionService decisionService = dmnDefinition.getDecisionServices().get(0);

        assertThat(decisionService.getOutputDecisions()).hasSize(2);
        assertThat(decisionService.getEncapsulatedDecisions()).hasSize(2);

        assertThat(dmnDefinition.getDecisions()).hasSize(4);

        assertThat(decisionService.getOutputDecisions())
            .extracting(DmnElementReference::getParsedId)
            .containsOnly("decision1", "decision2");

        decisionService.getOutputDecisions().forEach(outputDecisionRef -> {
            assertThat(dmnDefinition.getDecisionById(outputDecisionRef.getParsedId())).isNotNull();
            assertThat(dmnDefinition.getGraphicInfo(outputDecisionRef.getParsedId())).isNotNull();
        });

        assertThat(decisionService.getEncapsulatedDecisions())
            .extracting(DmnElementReference::getParsedId)
            .containsOnly("decision3", "decision4");

        decisionService.getEncapsulatedDecisions().forEach(encapsulatedDecisionRef -> {
            assertThat(dmnDefinition.getDecisionById(encapsulatedDecisionRef.getParsedId())).isNotNull();
            assertThat(dmnDefinition.getGraphicInfo(encapsulatedDecisionRef.getParsedId())).isNotNull();
        });

        dmnDefinition.getDecisions().forEach(decision -> assertThat(decision.getExpression()).isNotNull());

        JsonNode decisionServiceJson = new DmnJsonConverter().convertToJson(dmnDefinition);

        assertThat(decisionServiceJson).isNotNull();
    }

    @Test
    public void testConvertDecisionServiceJsonToDMNNoDecisionTableMap() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_23);

        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc");

        assertThat(dmnDefinition.getDecisionServices()).hasSize(1);

        DecisionService decisionService = dmnDefinition.getDecisionServices().get(0);

        assertThat(decisionService.getOutputDecisions()).hasSize(2);
        assertThat(decisionService.getEncapsulatedDecisions()).hasSize(2);

        assertThat(dmnDefinition.getDecisions()).hasSize(4);

        decisionService.getOutputDecisions().forEach(outputDecisionRef -> {
            assertThat(dmnDefinition.getDecisionById(outputDecisionRef.getParsedId())).isNotNull();
            assertThat(dmnDefinition.getGraphicInfo(outputDecisionRef.getParsedId())).isNotNull();
        });
        decisionService.getEncapsulatedDecisions().forEach(encapsulatedDecisionRef -> {
            assertThat(dmnDefinition.getDecisionById(encapsulatedDecisionRef.getParsedId())).isNotNull();
            assertThat(dmnDefinition.getGraphicInfo(encapsulatedDecisionRef.getParsedId())).isNotNull();
        });

        dmnDefinition.getDecisions().forEach(decision -> assertThat(decision.getExpression()).isNull());
    }

    @Test
    public void testConvertDecisionServiceJsonToDMNNoInformationRequirements() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_24);

        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource);

        assertThat(dmnDefinition.getDecisionServices()).hasSize(1);

        DecisionService decisionService = dmnDefinition.getDecisionServices().get(0);

        assertThat(decisionService.getOutputDecisions()).hasSize(2);
        assertThat(decisionService.getEncapsulatedDecisions()).hasSize(2);

        assertThat(dmnDefinition.getDecisions()).hasSize(4);

        decisionService.getOutputDecisions().forEach(outputDecisionRef -> {
            assertThat(dmnDefinition.getDecisionById(outputDecisionRef.getParsedId())).isNotNull();
            assertThat(dmnDefinition.getGraphicInfo(outputDecisionRef.getParsedId())).isNotNull();
        });
        decisionService.getEncapsulatedDecisions().forEach(encapsulatedDecisionRef -> {
            assertThat(dmnDefinition.getDecisionById(encapsulatedDecisionRef.getParsedId())).isNotNull();
            assertThat(dmnDefinition.getGraphicInfo(encapsulatedDecisionRef.getParsedId())).isNotNull();
        });

        JsonNode decisionServiceJson = new DmnJsonConverter().convertToJson(dmnDefinition);

        assertThat(decisionServiceJson).isNotNull();
    }

    @Test
    public void testConvertDecisionServiceJsonToDMNNoDecisions() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_25);

        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource);

        assertThat(dmnDefinition.getDecisionServices()).hasSize(1);

        DecisionService decisionService = dmnDefinition.getDecisionServices().get(0);

        decisionService.getOutputDecisions().forEach(outputDecisionRef -> {
            assertThat(dmnDefinition.getDecisionById(outputDecisionRef.getParsedId())).isNotNull();
            assertThat(dmnDefinition.getGraphicInfo(outputDecisionRef.getParsedId())).isNotNull();
        });
        decisionService.getEncapsulatedDecisions().forEach(encapsulatedDecisionRef -> {
            assertThat(dmnDefinition.getDecisionById(encapsulatedDecisionRef.getParsedId())).isNotNull();
            assertThat(dmnDefinition.getGraphicInfo(encapsulatedDecisionRef.getParsedId())).isNotNull();
        });

        JsonNode decisionServiceJson = new DmnJsonConverter().convertToJson(dmnDefinition);

        assertThat(decisionServiceJson).isNotNull();
    }

    @Test
    public void testConvertDRDtoJson() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_23);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource);

        ObjectNode modelerJson = new DmnJsonConverter().convertToJson(dmnDefinition);

        // main
        assertThat(modelerJson.get("bounds").get("lowerRight").get("x").asInt())
                .isEqualTo(testJsonResource.get("bounds").get("lowerRight").get("x").asInt());
        assertThat(modelerJson.get("bounds").get("lowerRight").get("y").asInt())
                .isEqualTo(testJsonResource.get("bounds").get("lowerRight").get("y").asInt());
        assertThat(modelerJson.get("bounds").get("upperLeft").get("x").asInt())
                .isEqualTo(testJsonResource.get("bounds").get("upperLeft").get("x").asInt());
        assertThat(modelerJson.get("bounds").get("upperLeft").get("y").asInt())
                .isEqualTo(testJsonResource.get("bounds").get("upperLeft").get("y").asInt());

        // decision service
        assertThat(modelerJson.get("childShapes").get(0).get("bounds").get("lowerRight").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("bounds").get("lowerRight").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("bounds").get("lowerRight").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("bounds").get("lowerRight").get("y").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("bounds").get("upperLeft").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("bounds").get("upperLeft").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("bounds").get("upperLeft").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("bounds").get("upperLeft").get("y").asInt());

        // output decisions section
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("lowerRight").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("lowerRight").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("lowerRight").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("lowerRight").get("y").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("upperLeft").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("upperLeft").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("upperLeft").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("upperLeft").get("y").asInt());

        // first output decision
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("lowerRight").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("lowerRight").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("lowerRight").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("lowerRight").get("y").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("upperLeft").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("upperLeft").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("upperLeft").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(0).get("bounds").get("upperLeft").get("y").asInt());

        // second output decision
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("lowerRight").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("lowerRight").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("lowerRight").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("lowerRight").get("y").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("upperLeft").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("upperLeft").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("upperLeft").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("upperLeft").get("y").asInt());

        // encapsulated decisions sections
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("lowerRight").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("lowerRight").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("lowerRight").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("lowerRight").get("y").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("upperLeft").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("upperLeft").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("upperLeft").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("bounds").get("upperLeft").get("y").asInt());

        // first encapsulated decision
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(0).get("bounds").get("lowerRight").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(0).get("bounds").get("lowerRight").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(0).get("bounds").get("lowerRight").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(0).get("bounds").get("lowerRight").get("y").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(0).get("bounds").get("upperLeft").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(0).get("bounds").get("upperLeft").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(0).get("bounds").get("upperLeft").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(0).get("bounds").get("upperLeft").get("y").asInt());

        // second encapsulated decision
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(1).get("bounds").get("lowerRight").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(1).get("bounds").get("lowerRight").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(1).get("bounds").get("lowerRight").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(1).get("bounds").get("lowerRight").get("y").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(1).get("bounds").get("upperLeft").get("x").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(1).get("bounds").get("upperLeft").get("x").asInt());
        assertThat(modelerJson.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(1).get("bounds").get("upperLeft").get("y").asInt())
                .isEqualTo(testJsonResource.get("childShapes").get(0).get("childShapes").get(1).get("childShapes").get(1).get("bounds").get("upperLeft").get("y").asInt());

        // first information requirement
        assertThat(modelerJson.get("childShapes").get(1).get("bounds").get("lowerRight").get("x").asInt())
                .isCloseTo(testJsonResource.get("childShapes").get(1).get("bounds").get("lowerRight").get("x").asInt(), offset(1));
        assertThat(modelerJson.get("childShapes").get(1).get("bounds").get("lowerRight").get("y").asInt())
                .isCloseTo(testJsonResource.get("childShapes").get(1).get("bounds").get("lowerRight").get("y").asInt(), offset(1));
        assertThat(modelerJson.get("childShapes").get(1).get("bounds").get("upperLeft").get("x").asInt())
                .isCloseTo(testJsonResource.get("childShapes").get(1).get("bounds").get("upperLeft").get("x").asInt(), offset(1));
        assertThat(modelerJson.get("childShapes").get(1).get("bounds").get("upperLeft").get("y").asInt())
                .isCloseTo(testJsonResource.get("childShapes").get(1).get("bounds").get("upperLeft").get("y").asInt(), offset(1));

        assertThat(modelerJson.get("childShapes").get(1).get("dockers").get(0).get("x").asInt())
                .isCloseTo(testJsonResource.get("childShapes").get(1).get("dockers").get(0).get("x").asInt(), offset(1));
        assertThat(modelerJson.get("childShapes").get(1).get("dockers").get(0).get("y").asInt())
                .isCloseTo(testJsonResource.get("childShapes").get(1).get("dockers").get(0).get("y").asInt(), offset(1));
        assertThat(modelerJson.get("childShapes").get(1).get("dockers").get(0).get("x").asInt())
                .isCloseTo(testJsonResource.get("childShapes").get(1).get("dockers").get(1).get("x").asInt(), offset(1));
        assertThat(modelerJson.get("childShapes").get(1).get("dockers").get(0).get("y").asInt())
                .isCloseTo(testJsonResource.get("childShapes").get(1).get("dockers").get(1).get("y").asInt(), offset(1));
    }

    @Test
    public void testConvertJsonToDMNEmptyOutcomes() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_22);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();


        assertThat(decisionTable.getRules().get(0).getOutputEntries()).hasSize(2);
        assertThat(decisionTable.getRules().get(1).getOutputEntries()).hasSize(2);
    }
    @Test
    public void testConvertJsonToDMNMultipleRequiredDecisions() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_26);
        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());

        assertThat(filter(dmnDefinition.getDecisions()).with("id").equalsTo("decision1").get())
                .flatExtracting(Decision::getRequiredDecisions)
                .extracting(informationRequirement -> informationRequirement.getRequiredDecision().getHref())
                .containsExactlyInAnyOrder("#decision3", "#decision2");
    }

    /* Helper methods */
    protected String readJsonToString(String resource) {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resource)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not read " + resource + " : " + e.getMessage());
            return null;
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
