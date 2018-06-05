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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DecisionTableOrientation;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.DmnExtensionElement;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.dmn.model.UnaryTests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yvo Swillens
 */
public class DmnJsonConverter {

    public static final String MODEL_NAMESPACE = "http://flowable.org/dmn";
    public static final String URI_JSON = "http://www.ecma-international.org/ecma-404/";

    protected ObjectMapper objectMapper = new ObjectMapper();

    public DmnDefinition convertToDmn(JsonNode modelNode, String modelId, int modelVersion, Date lastUpdated) {

        // check and migrate model
        modelNode = DmnJsonConverterUtil.migrateModel(modelNode, objectMapper);

        DmnDefinition definition = new DmnDefinition();

        definition.setId("definition_" + modelId);
        definition.setName(DmnJsonConverterUtil.getValueAsString("name", modelNode));
        definition.setNamespace(MODEL_NAMESPACE);
        definition.setTypeLanguage(URI_JSON);

        // decision
        //
        Decision decision = new Decision();
        decision.setId(DmnJsonConverterUtil.getValueAsString("key", modelNode));
        decision.setName(DmnJsonConverterUtil.getValueAsString("name", modelNode));
        decision.setDescription(DmnJsonConverterUtil.getValueAsString("description", modelNode));

        definition.addDecision(decision);

        // decision table
        //
        DecisionTable decisionTable = new DecisionTable();
        decisionTable.setId("decisionTable_" + DmnJsonConverterUtil.getValueAsString("id", modelNode));

        if (modelNode.has("hitIndicator")) {
            decisionTable.setHitPolicy(HitPolicy.get(DmnJsonConverterUtil.getValueAsString("hitIndicator", modelNode)));
        } else {
            decisionTable.setHitPolicy(HitPolicy.FIRST);
        }

        if (modelNode.has("collectOperator")) {
            decisionTable.setAggregation(BuiltinAggregator.get(DmnJsonConverterUtil.getValueAsString("collectOperator", modelNode)));
        }

        // default orientation
        decisionTable.setPreferredOrientation(DecisionTableOrientation.RULE_AS_ROW);

        decision.setExpression(decisionTable);

        // inputs
        processDecisionTable(modelNode, definition, decisionTable);

        return definition;
    }

    public ObjectNode convertToJson(DmnDefinition definition) {

        ObjectNode modelNode = objectMapper.createObjectNode();

        Decision firstDecision = definition.getDecisions().get(0);
        DecisionTable decisionTable = (DecisionTable) firstDecision.getExpression();

        modelNode.put("id", definition.getId());
        modelNode.put("key", firstDecision.getId());
        modelNode.put("name", definition.getName());
        modelNode.put("description", definition.getDescription());
        modelNode.put("hitIndicator", decisionTable.getHitPolicy().name());

        if (decisionTable.getAggregation() != null) {
            modelNode.put("collectOperator", decisionTable.getAggregation().name());
        }

        // input expressions
        ArrayNode inputExpressionsNode = objectMapper.createArrayNode();

        for (InputClause clause : decisionTable.getInputs()) {

            LiteralExpression inputExpression = clause.getInputExpression();

            ObjectNode inputExpressionNode = objectMapper.createObjectNode();
            inputExpressionNode.put("id", inputExpression.getId());
            inputExpressionNode.put("type", inputExpression.getTypeRef());
            inputExpressionNode.put("label", clause.getLabel());
            inputExpressionNode.put("variableId", inputExpression.getText());

            inputExpressionsNode.add(inputExpressionNode);
        }

        modelNode.set("inputExpressions", inputExpressionsNode);

        // output expressions
        ArrayNode outputExpressionsNode = objectMapper.createArrayNode();

        for (OutputClause clause : decisionTable.getOutputs()) {

            ObjectNode outputExpressionNode = objectMapper.createObjectNode();
            outputExpressionNode.put("id", clause.getId());
            outputExpressionNode.put("type", clause.getTypeRef());
            outputExpressionNode.put("label", clause.getLabel());
            outputExpressionNode.put("variableId", clause.getName());

            outputExpressionsNode.add(outputExpressionNode);
        }

        modelNode.set("outputExpressions", outputExpressionsNode);

        // rules
        ArrayNode rulesNode = objectMapper.createArrayNode();
        for (DecisionRule rule : decisionTable.getRules()) {

            ObjectNode ruleNode = objectMapper.createObjectNode();

            for (RuleInputClauseContainer ruleClauseContainer : rule.getInputEntries()) {
                InputClause inputClause = ruleClauseContainer.getInputClause();
                UnaryTests inputEntry = ruleClauseContainer.getInputEntry();

                String inputExpressionId = inputClause.getInputExpression().getId();
                String operatorId = inputExpressionId + "_operator";
                String expressionId = inputExpressionId + "_expression";
                String expressionText = inputEntry.getText();
                String operatorValue = null;
                String expressionValue = null;


                if (inputEntry.getExtensionElements() != null && !inputEntry.getExtensionElements().isEmpty()) {
                    if (inputEntry.getExtensionElements().containsKey("operator")) {
                        operatorValue = inputEntry.getExtensionElements().get("operator").get(0).getElementText();
                    }
                    if (inputEntry.getExtensionElements().containsKey("expression")) {
                        expressionValue = inputEntry.getExtensionElements().get("expression").get(0).getElementText();
                    }
                } else {
                    if (StringUtils.isNotEmpty(expressionText)) {
                        if (expressionText.startsWith("${") || expressionText.startsWith("#{")) {
                            expressionValue = expressionText;
                        } else {
                            if (expressionText.indexOf(' ') != -1) {
                                operatorValue = expressionText.substring(0, expressionText.indexOf(' '));
                                expressionValue = expressionText.substring(expressionText.indexOf(' ') + 1);
                            } else { // no prefixed operator
                                expressionValue = expressionText;
                            }

                            // remove outer escape quotes
                            if (expressionValue.startsWith("\"") && expressionValue.endsWith("\"")) {
                                expressionValue = expressionValue.substring(1, expressionValue.length() - 1);
                            }

                            // if build in date function
                            if (expressionValue.startsWith("fn_date(")) {
                                expressionValue = expressionValue.substring(9, expressionValue.lastIndexOf('\''));
                            } else if (expressionValue.startsWith("date:toDate(")) {
                                expressionValue = expressionValue.substring(13, expressionValue.lastIndexOf('\''));
                            }
                        }
                    }
                }

                ruleNode.put(operatorId, operatorValue);
                ruleNode.put(expressionId, expressionValue);
            }

            for (RuleOutputClauseContainer ruleClauseContainer : rule.getOutputEntries()) {
                OutputClause outputClause = ruleClauseContainer.getOutputClause();
                LiteralExpression outputEntry = ruleClauseContainer.getOutputEntry();
                ruleNode.put(outputClause.getId(), outputEntry.getText());
            }

            rulesNode.add(ruleNode);
        }

        modelNode.set("rules", rulesNode);

        return modelNode;
    }

    protected void processDecisionTable(JsonNode modelNode, DmnDefinition definition, DecisionTable decisionTable) {

        if (definition == null || decisionTable == null) {
            return;
        }

        Map<String, InputClause> ruleInputContainerMap = new LinkedHashMap<>();
        Map<String, OutputClause> ruleOutputContainerMap = new LinkedHashMap<>();
        List<String> complexExpressionIds = new ArrayList<>();

        processInputExpressions(modelNode, ruleInputContainerMap, decisionTable);

        processOutputExpressions(modelNode, ruleOutputContainerMap, complexExpressionIds, decisionTable);

        processRules(modelNode, ruleInputContainerMap, ruleOutputContainerMap, complexExpressionIds, decisionTable);

        // regression check for empty expression types
        for (InputClause inputClause : decisionTable.getInputs()) {
            if (StringUtils.isEmpty(inputClause.getInputExpression().getTypeRef())) {
                // default to string
                inputClause.getInputExpression().setTypeRef("string");
            }
        }
        for (OutputClause outputClause : decisionTable.getOutputs()) {
            if (StringUtils.isEmpty(outputClause.getTypeRef())) {
                // default to string
                outputClause.setTypeRef("string");
            }
        }
    }

    protected void processInputExpressions(JsonNode modelNode, Map<String, InputClause> ruleInputContainerMap, DecisionTable decisionTable) {
        // input expressions
        JsonNode inputExpressions = modelNode.get("inputExpressions");

        if (inputExpressions != null && !inputExpressions.isNull()) {

            for (JsonNode inputExpressionNode : inputExpressions) {

                InputClause inputClause = new InputClause();
                inputClause.setLabel(DmnJsonConverterUtil.getValueAsString("label", inputExpressionNode));

                String inputExpressionId = DmnJsonConverterUtil.getValueAsString("id", inputExpressionNode);

                LiteralExpression inputExpression = new LiteralExpression();
                inputExpression.setId("inputExpression_" + inputExpressionId);
                inputExpression.setTypeRef(DmnJsonConverterUtil.getValueAsString("type", inputExpressionNode));
                inputExpression.setLabel(DmnJsonConverterUtil.getValueAsString("label", inputExpressionNode));
                inputExpression.setText(DmnJsonConverterUtil.getValueAsString("variableId", inputExpressionNode));

                // add to clause
                inputClause.setInputExpression(inputExpression);

                if (inputExpressionNode.get("entries") != null && !inputExpressionNode.get("entries").isNull()
                    && inputExpressionNode.get("entries").isArray() && inputExpressionNode.get("entries").size() > 0) {
                    UnaryTests inputValues = new UnaryTests();
                    List<Object> inputEntries = new ArrayList<>();
                    for (JsonNode entriesNode : inputExpressionNode.get("entries")) {
                        inputEntries.add(entriesNode.asText());
                    }
                    inputValues.setTextValues(inputEntries);

                    // add to clause
                    inputClause.setInputValues(inputValues);
                }

                // add to map
                ruleInputContainerMap.put(inputExpressionId, inputClause);

                decisionTable.addInput(inputClause);
            }
        }
    }

    protected void processOutputExpressions(JsonNode modelNode, Map<String, OutputClause> ruleOutputContainerMap,
                                            List<String> complexExpressionIds, DecisionTable decisionTable) {

        // output expressions
        JsonNode outputExpressions = modelNode.get("outputExpressions");

        if (outputExpressions != null && !outputExpressions.isNull()) {

            for (JsonNode outputExpressionNode : outputExpressions) {

                OutputClause outputClause = new OutputClause();

                String outputExpressionId = DmnJsonConverterUtil.getValueAsString("id", outputExpressionNode);
                String outputClauseId = "outputExpression_" + outputExpressionId;

                outputClause.setId(outputClauseId);
                outputClause.setLabel(DmnJsonConverterUtil.getValueAsString("label", outputExpressionNode));
                outputClause.setName(DmnJsonConverterUtil.getValueAsString("variableId", outputExpressionNode));
                outputClause.setTypeRef(DmnJsonConverterUtil.getValueAsString("type", outputExpressionNode));

                if (outputExpressionNode.get("entries") != null && !outputExpressionNode.get("entries").isNull()
                    && outputExpressionNode.get("entries").isArray() && outputExpressionNode.get("entries").size() > 0) {
                    UnaryTests outputValues = new UnaryTests();
                    List<Object> outputEntries = new ArrayList<>();
                    for (JsonNode entriesNode : outputExpressionNode.get("entries")) {
                        outputEntries.add(entriesNode.asText());
                    }
                    outputValues.setTextValues(outputEntries);

                    // add to clause
                    outputClause.setOutputValues(outputValues);
                }

                if (outputExpressionNode.get("complexExpression") != null && !outputExpressionNode.get("complexExpression").isNull()) {
                    if (outputExpressionNode.get("complexExpression").asBoolean()) {
                        complexExpressionIds.add(outputExpressionId);
                    }
                }

                // add to map
                ruleOutputContainerMap.put(outputExpressionId, outputClause);

                decisionTable.addOutput(outputClause);
            }
        }
    }

    protected void processRules(JsonNode modelNode, Map<String, InputClause> ruleInputContainerMap, Map<String, OutputClause> ruleOutputContainerMap,
                                List<String> complexExpressionIds, DecisionTable decisionTable) {
        // rules
        JsonNode rules = modelNode.get("rules");

        if (rules != null && !rules.isNull()) {

            int ruleCounter = 1;

            for (JsonNode ruleNode : rules) {
                // Make sure the rules are added in the same order that they are defined
                // in the input/output clauses
                DecisionRule rule = new DecisionRule();
                for (String id : ruleInputContainerMap.keySet()) {

                    String operatorId = id + "_operator";
                    String expressionId = id + "_expression";

                    RuleInputClauseContainer ruleInputClauseContainer = new RuleInputClauseContainer();
                    ruleInputClauseContainer.setInputClause(ruleInputContainerMap.get(id));

                    UnaryTests inputEntry = new UnaryTests();
                    inputEntry.setId("inputEntry_" + id + "_" + ruleCounter);

                    JsonNode operatorValueNode = ruleNode.get(operatorId);
                    String operatorValue = null;
                    if (operatorValueNode != null && !operatorValueNode.isNull()) {
                        operatorValue = operatorValueNode.asText();
                    }

                    JsonNode expressionValueNode = ruleNode.get(expressionId);
                    String expressionValue;
                    if (expressionValueNode == null || expressionValueNode.isNull()) {
                        expressionValue = "-";
                    } else {
                        expressionValue = expressionValueNode.asText();
                    }

                    // if expression is dash value or custom expression skip composition
                    if ("-".equals(expressionValue) || expressionValue.startsWith("${") || expressionValue.startsWith("#{")) {
                        inputEntry.setText(expressionValue);
                    } else if (DmnJsonConverterUtil.isCollectionOperator(operatorValue) && StringUtils.isNotEmpty(expressionValue)) {

                        String inputExpressionVariable = ruleInputClauseContainer.getInputClause().getInputExpression().getText();

                        String formattedCollectionExpression = DmnJsonConverterUtil.formatCollectionExpression(operatorValue, inputExpressionVariable, expressionValue);

                        inputEntry.setText(formattedCollectionExpression);

                        // extensions
                        addExtensionElement("operator", operatorValue, inputEntry);
                        addExtensionElement("expression", expressionValue, inputEntry);


                    } else if ("collection".equals(ruleInputClauseContainer.getInputClause().getInputExpression().getTypeRef())
                        && StringUtils.isNotEmpty(expressionValue)) {

                        String inputExpressionVariable = ruleInputClauseContainer.getInputClause().getInputExpression().getText();
                        String formattedCollectionExpression = DmnJsonConverterUtil.formatCollectionExpression(operatorValue, inputExpressionVariable, expressionValue);

                        inputEntry.setText(formattedCollectionExpression);

                        // extensions
                        addExtensionElement("operator", operatorValue, inputEntry);
                        addExtensionElement("expression", expressionValue, inputEntry);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        if (StringUtils.isNotEmpty(operatorValue)) {
                            stringBuilder = new StringBuilder(operatorValue);
                            stringBuilder.append(" ");
                        }

                        // add quotes for string
                        if ("string".equals(ruleInputClauseContainer.getInputClause().getInputExpression().getTypeRef())
                            && !expressionValue.startsWith("\"")
                            && !expressionValue.endsWith("\"")) { // add quotes for string (with no surrounding quotes)

                            stringBuilder.append("\"");
                            stringBuilder.append(expressionValue);
                            stringBuilder.append("\"");
                        } else if ("date".equals(ruleInputClauseContainer.getInputClause().getInputExpression().getTypeRef())
                            && StringUtils.isNotEmpty(expressionValue)) {

                            // wrap in built in toDate function
                            stringBuilder.append("date:toDate('");
                            stringBuilder.append(expressionValue);
                            stringBuilder.append("')");
                        } else {
                            stringBuilder.append(expressionValue);
                        }
                        inputEntry.setText(stringBuilder.toString());
                    }

                    ruleInputClauseContainer.setInputEntry(inputEntry);
                    rule.addInputEntry(ruleInputClauseContainer);
                }
                for (String id : ruleOutputContainerMap.keySet()) {
                    RuleOutputClauseContainer ruleOutputClauseContainer = new RuleOutputClauseContainer();
                    ruleOutputClauseContainer.setOutputClause(ruleOutputContainerMap.get(id));

                    LiteralExpression outputEntry = new LiteralExpression();
                    outputEntry.setId("outputEntry_" + id + "_" + ruleCounter);

                    if (ruleNode.has(id)) {
                        JsonNode expressionValueNode = ruleNode.get(id);
                        String expressionValue;
                        if (expressionValueNode == null || expressionValueNode.isNull()) {
                            expressionValue = "";
                        } else {
                            expressionValue = expressionValueNode.asText();
                        }

                        if (complexExpressionIds.contains(id) || expressionValue.startsWith("${") || expressionValue.startsWith("#{")) {
                            outputEntry.setText(expressionValue);
                        } else {
                            if ("string".equals(ruleOutputClauseContainer.getOutputClause().getTypeRef())
                                && !expressionValue.startsWith("\"")
                                && !expressionValue.endsWith("\"")) { // add quotes for string (with no surrounding quotes)
                                outputEntry.setText("\"" + expressionValue + "\"");
                            } else if ("date".equals(ruleOutputClauseContainer.getOutputClause().getTypeRef())
                                && StringUtils.isNotEmpty(expressionValue)) { // wrap in built in toDate function
                                outputEntry.setText("date:toDate('" + expressionValue + "')");
                            } else {
                                outputEntry.setText(expressionValue);
                            }
                        }

                    } else { // output entry not present in rule node
                        outputEntry.setText("");
                    }

                    ruleOutputClauseContainer.setOutputEntry(outputEntry);
                    rule.addOutputEntry(ruleOutputClauseContainer);
                }
                ruleCounter++;
                decisionTable.addRule(rule);
            }
        }
    }

    protected void addExtensionElement(String name, String value, DmnElement element) {
        DmnExtensionElement extensionElement = new DmnExtensionElement();
        extensionElement.setNamespace(MODEL_NAMESPACE);
        extensionElement.setNamespacePrefix("flowable");
        extensionElement.setName(name);
        extensionElement.setElementText(value);

        element.addExtensionElement(extensionElement);
    }
}
