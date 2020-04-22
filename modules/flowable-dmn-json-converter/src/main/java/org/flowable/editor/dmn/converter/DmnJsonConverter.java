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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DecisionTableOrientation;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.DmnExtensionAttribute;
import org.flowable.dmn.model.DmnExtensionElement;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.InformationRequirement;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.dmn.model.UnaryTests;
import org.flowable.editor.constants.DmnJsonConstants;
import org.flowable.editor.constants.DmnStencilConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yvo Swillens
 */
public class DmnJsonConverter implements DmnJsonConstants, DmnStencilConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DmnJsonConverter.class);

    public static final String MODEL_NAMESPACE = "http://flowable.org/dmn";
    public static final String URI_JSON = "http://www.ecma-international.org/ecma-404/";
    public static final String MODEL_VERSION = "3";

    protected ObjectMapper objectMapper = new ObjectMapper();

    public DmnDefinition convertToDmn(JsonNode modelNode, String modelId, int modelVersion, Date lastUpdated) {
        return convertToDmn(modelNode, modelId, Collections.EMPTY_MAP);
    }

    public DmnDefinition convertToDmn(JsonNode modelNode, String modelId) {
        return convertToDmn(modelNode, modelId, Collections.EMPTY_MAP);

    }

    public DmnDefinition convertToDmn(JsonNode modelNode, String modelId, Map<String, String> decisionEditorJsonMap) {
        DmnDefinition definition = new DmnDefinition();

        definition.setId("definition_" + modelId);
        definition.setName(DmnJsonConverterUtil.getPropertyValueAsString("name", modelNode));
        definition.setNamespace(MODEL_NAMESPACE);
        definition.setTypeLanguage(URI_JSON);

        if (DmnJsonConverterUtil.isDRD(modelNode)) {
            processDRD(modelNode, definition, decisionEditorJsonMap);
        } else {
            processDecisionTableDecision(modelNode, definition);
        }

        return definition;
    }

    public ObjectNode convertToJson(DmnDefinition definition) {

        ObjectNode modelNode = objectMapper.createObjectNode();

        Decision firstDecision = definition.getDecisions().get(0);
        DecisionTable decisionTable = (DecisionTable) firstDecision.getExpression();

        modelNode.put("id", definition.getId());
        modelNode.put("key", firstDecision.getId());
        modelNode.put("name", definition.getName());
        modelNode.put("version", MODEL_VERSION);
        modelNode.put("description", definition.getDescription());
        modelNode.put("hitIndicator", decisionTable.getHitPolicy().name());

        if (decisionTable.getAggregation() != null) {
            modelNode.put("collectOperator", decisionTable.getAggregation().name());
        }

        if (firstDecision.isForceDMN11()) {
            modelNode.put("forceDMN11", true);
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

    protected void processDRD(JsonNode modelNode, DmnDefinition definition, Map<String, String> decisionEditorJsonMap) {
        Map<String, JsonNode> shapeMap = new HashMap<>();
        Map<String, JsonNode> sourceRefMap = new HashMap<>();
        Map<String, JsonNode> targetRefMap = new HashMap<>();
        Map<String, List<JsonNode>> sourceAndTargetMap = new HashMap<>();

        preProcessShapes(modelNode, shapeMap, sourceRefMap);
        preProcessFlows(modelNode, shapeMap, sourceRefMap, sourceAndTargetMap, targetRefMap);

        ArrayNode shapesArrayNode = (ArrayNode) modelNode.get(EDITOR_CHILD_SHAPES);

        if (shapesArrayNode == null || shapesArrayNode.size() == 0) {
            return;
        }

        // first create the (expanded) decision service
        shapesArrayNode.forEach(shapeNode -> {
            String stencilId = DmnJsonConverterUtil.getStencilId(shapeNode);
            if (STENCIL_EXPANDED_DECISION_SERVICE.equals(stencilId)) {
                DecisionService decisionService = new DecisionService();
                definition.addDecisionService(decisionService);

                String decisionServiceId = DmnJsonConverterUtil.getElementId(shapeNode);
                decisionService.setId(decisionServiceId);
                String decisionServiceName = DmnJsonConverterUtil.getValueAsString("name", shapeNode);
                if (StringUtils.isNotEmpty(decisionServiceName)) {
                    decisionService.setName(DmnJsonConverterUtil.getValueAsString("name", shapeNode));
                } else {
                    decisionService.setName(decisionServiceId);
                }

                ArrayNode decisionServiceShapesArrayNode = (ArrayNode) shapeNode.get(EDITOR_CHILD_SHAPES);
                if (decisionServiceShapesArrayNode == null || decisionServiceShapesArrayNode.size() == 0) {
                    return;
                }

                decisionServiceShapesArrayNode.forEach(decisionServiceChildNode -> {
                    String decisionServiceChildNodeStencilId = DmnJsonConverterUtil.getStencilId(decisionServiceChildNode);
                    if (STENCIL_OUTPUT_DECISIONS.equals(decisionServiceChildNodeStencilId)) {
                        processDRDDecision(definition, decisionServiceChildNode, decisionEditorJsonMap, sourceRefMap, targetRefMap, decisionService.getOutputDecisions());
                    } else if (STENCIL_ENCAPSULATED_DECISIONS.equals(decisionServiceChildNodeStencilId)) {
                        processDRDDecision(definition, decisionServiceChildNode, decisionEditorJsonMap, sourceRefMap, targetRefMap, decisionService.getEncapsulatedDecisions());
                    }
                });
            }
        });
    }

    protected void processDRDDecision(DmnDefinition definition, JsonNode decisionServiceChildNode, Map<String, String> decisionEditorJsonMap,
        Map<String, JsonNode> sourceRefMap, Map<String, JsonNode> targetRefMap, List<DmnElementReference> decisionServiceDecisisions) {

        ArrayNode decisionsArrayNode = (ArrayNode) decisionServiceChildNode.get(EDITOR_CHILD_SHAPES);
        if (decisionsArrayNode == null || decisionsArrayNode.size() == 0) {
            return;
        }

        decisionsArrayNode.forEach(decisionChildNode -> {
            Decision decision = new Decision();
            decision.setDmnDefinition(definition);
            decision.setId(DmnJsonConverterUtil.getElementId(decisionChildNode));
            decision.setName(DmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, decisionChildNode));

            JsonNode decisionTableReferenceNode = DmnJsonConverterUtil.getProperty(PROPERTY_DECISION_TABLE_REFERENCE, decisionChildNode);
            if (decisionTableReferenceNode != null && decisionTableReferenceNode.has("key") && !decisionTableReferenceNode.get("key").isNull()) {

                String decisionTableKey = decisionTableReferenceNode.get("key").asText();
                if (decisionEditorJsonMap != null) {
                    String decisionTableEditorJson = decisionEditorJsonMap.get(decisionTableKey);
                    if (StringUtils.isNotEmpty(decisionTableEditorJson)) {
                        try {
                            JsonNode decisionTableNode = objectMapper.readTree(decisionTableEditorJson);

                            DecisionTable decisionTable = new DecisionTable();
                            decision.setExpression(decisionTable);

                            processDecisionTable(decisionTableNode, decisionTable);
                        } catch (Exception ex) {
                            LOGGER.error("Error while parsing decision table editor JSON: " + decisionTableEditorJson);
                        }
                    } else {
                        LOGGER.warn("Could not find decision table for key: " + decisionTableKey);
                    }
                }
            }

            if (targetRefMap.containsKey(decisionChildNode.get("resourceId").asText())) {
                JsonNode informationRequirementNode = targetRefMap.get(decisionChildNode.get("resourceId").asText());

                InformationRequirement informationRequirement = new InformationRequirement();
                informationRequirement.setId(DmnJsonConverterUtil.getElementId(informationRequirementNode));
                informationRequirement.setName(DmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, informationRequirementNode));

                JsonNode requiredDecisionNode = sourceRefMap.get(DmnJsonConverterUtil.getElementId(informationRequirementNode));

                DmnElementReference requiredDecisionReference = createDmnElementReference(requiredDecisionNode);

                informationRequirement.setRequiredDecision(requiredDecisionReference);
                decision.addRequiredDecision(informationRequirement);
            }

            decisionServiceDecisisions.add(createDmnElementReference(decisionChildNode));
            definition.addDecision(decision);
        });
    }

    protected DmnElementReference createDmnElementReference(JsonNode node) {
        DmnElementReference dmnElementReference = new DmnElementReference();
        String decisionHref = "#" + DmnJsonConverterUtil.getElementId(node);
        dmnElementReference.setHref(decisionHref);

        return dmnElementReference;
    }

    protected DmnElementReference createDmnElementReference(DmnElement element) {
        DmnElementReference dmnElementReference = new DmnElementReference();
        String decisionHref = "#" + element.getId();
        dmnElementReference.setHref(decisionHref);

        return dmnElementReference;
    }

    protected void preProcessShapes(JsonNode objectNode, Map<String, JsonNode> shapeMap, Map<String, JsonNode> sourceRefMap) {
        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {
                String stencilId = DmnJsonConverterUtil.getStencilId(jsonChildNode);

                if (!STENCIL_INFORMATION_REQUIREMENT.equals(stencilId)) {
                    String childShapeId = jsonChildNode.get(EDITOR_SHAPE_ID).asText();

                    shapeMap.put(childShapeId, jsonChildNode);

                    ArrayNode outgoingNode = (ArrayNode) jsonChildNode.get("outgoing");
                    if (outgoingNode != null && outgoingNode.size() > 0) {
                        for (JsonNode outgoingChildNode : outgoingNode) {
                            JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                            if (resourceNode != null) {
                                sourceRefMap.put(resourceNode.asText(), jsonChildNode);
                            }
                        }
                    }

                    preProcessShapes(jsonChildNode, shapeMap, sourceRefMap);
                }
            }
        }
    }

    protected void preProcessFlows(JsonNode objectNode, Map<String, JsonNode> shapeMap, Map<String, JsonNode> sourceRefMap,
        Map<String, List<JsonNode>> sourceAndTargetMap, Map<String, JsonNode> targetRefMap) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {
                String stencilId = DmnJsonConverterUtil.getStencilId(jsonChildNode);
                if (!STENCIL_INFORMATION_REQUIREMENT.equals(stencilId)) {
                    preProcessFlows(jsonChildNode, shapeMap, sourceRefMap, sourceAndTargetMap, targetRefMap);
                } else {
                    String childEdgeId = DmnJsonConverterUtil.getElementId(jsonChildNode);

                    JsonNode targetNode = jsonChildNode.get("target");
                    if (targetNode != null && !targetNode.isNull()) {
                        String targetRefId = targetNode.get(EDITOR_SHAPE_ID).asText();
                        List<JsonNode> sourceAndTargetList = new ArrayList<>();
                        sourceAndTargetList.add(sourceRefMap.get(jsonChildNode.get(EDITOR_SHAPE_ID).asText()));
                        sourceAndTargetList.add(shapeMap.get(targetRefId));
                        sourceAndTargetMap.put(childEdgeId, sourceAndTargetList);

                        targetRefMap.put(targetRefId, jsonChildNode);
                    }
                }
            }
        }
    }

    protected void processDecisionTableDecision(JsonNode modelNode, DmnDefinition definition) {
        // check and migrate model
        modelNode = DmnJsonConverterUtil.migrateModel(modelNode, objectMapper);

        definition.setName(DmnJsonConverterUtil.getValueAsString("name", modelNode));

        // decision
        //
        Decision decision = new Decision();
        decision.setId(DmnJsonConverterUtil.getValueAsString("key", modelNode));
        decision.setName(DmnJsonConverterUtil.getValueAsString("name", modelNode));
        decision.setDescription(DmnJsonConverterUtil.getValueAsString("description", modelNode));

        if (modelNode.has("forceDMN11") && "true".equals(DmnJsonConverterUtil.getValueAsString("forceDMN11", modelNode))) {
            decision.setForceDMN11(true);
        }

        definition.addDecision(decision);

        // decision table
        //
        DecisionTable decisionTable = new DecisionTable();

        decision.setExpression(decisionTable);

        // inputs
        processDecisionTable(modelNode, decisionTable);
    }

    protected void processDecisionTable(JsonNode modelNode, DecisionTable decisionTable) {
        if (decisionTable == null) {
            return;
        }

        decisionTable.setId(DmnJsonConverterUtil.getUniqueElementId("decisionTable"));

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

        Map<String, InputClause> ruleInputContainerMap = new LinkedHashMap<>();
        Map<String, OutputClause> ruleOutputContainerMap = new LinkedHashMap<>();
        Map<String, String> newOldIdMap = new HashMap<>();
        List<String> complexExpressionIds = new ArrayList<>();

        processInputExpressions(modelNode, ruleInputContainerMap, newOldIdMap, decisionTable);

        processOutputExpressions(modelNode, ruleOutputContainerMap, complexExpressionIds, newOldIdMap, decisionTable);

        processRules(modelNode, ruleInputContainerMap, ruleOutputContainerMap, complexExpressionIds, newOldIdMap, decisionTable);

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

    protected void processInputExpressions(JsonNode modelNode, Map<String, InputClause> ruleInputContainerMap,
        Map<String, String> newOldIdMap, DecisionTable decisionTable) {

        // input expressions
        JsonNode inputExpressions = modelNode.get("inputExpressions");

        if (inputExpressions != null && !inputExpressions.isNull()) {

            for (JsonNode inputExpressionNode : inputExpressions) {

                InputClause inputClause = new InputClause();
                inputClause.setLabel(DmnJsonConverterUtil.getValueAsString("label", inputExpressionNode));

                String oldInputExpressionId = DmnJsonConverterUtil.getValueAsString("id", inputExpressionNode);
                String inputExpressionId = DmnJsonConverterUtil.getUniqueElementId();

                newOldIdMap.put(inputExpressionId, oldInputExpressionId);

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
        List<String> complexExpressionIds, Map<String, String> newOldIdMap, DecisionTable decisionTable) {

        // output expressions
        JsonNode outputExpressions = modelNode.get("outputExpressions");

        if (outputExpressions != null && !outputExpressions.isNull()) {

            for (JsonNode outputExpressionNode : outputExpressions) {

                OutputClause outputClause = new OutputClause();

                String oldOutputExpressionId = DmnJsonConverterUtil.getValueAsString("id", outputExpressionNode);
                String outputExpressionId = DmnJsonConverterUtil.getUniqueElementId();
                String outputClauseId = "outputExpression_" + outputExpressionId;
                outputClause.setId(outputClauseId);

                newOldIdMap.put(outputExpressionId, oldOutputExpressionId);

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
        List<String> complexExpressionIds, Map<String, String> newOldIdMap, DecisionTable decisionTable) {
        // rules
        JsonNode rules = modelNode.get("rules");

        if (rules != null && !rules.isNull()) {

            int ruleCounter = 1;

            for (JsonNode ruleNode : rules) {
                // Make sure the rules are added in the same order that they are defined
                // in the input/output clauses
                DecisionRule rule = new DecisionRule();
                for (String id : ruleInputContainerMap.keySet()) {
                    String oldInputExpressionId = newOldIdMap.get(id);
                    String operatorId = oldInputExpressionId + "_operator";
                    String expressionId = oldInputExpressionId + "_expression";

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

                        String formattedCollectionExpression;
                        if ("collection".equals(ruleInputClauseContainer.getInputClause().getInputExpression().getTypeRef())) {
                            formattedCollectionExpression = DmnJsonConverterUtil
                                .formatCollectionExpression(operatorValue, inputExpressionVariable, expressionValue);
                        } else {
                            formattedCollectionExpression = DmnJsonConverterUtil
                                .formatCollectionExpression(operatorValue, expressionValue, inputExpressionVariable);
                        }

                        inputEntry.setText(formattedCollectionExpression);

                        // extensions
                        addExtensionElement("operator", operatorValue, inputEntry);
                        addExtensionElement("expression", expressionValue, inputEntry);

                    } else if ("collection".equals(ruleInputClauseContainer.getInputClause().getInputExpression().getTypeRef())
                        && StringUtils.isNotEmpty(expressionValue)) {

                        String inputExpressionVariable = ruleInputClauseContainer.getInputClause().getInputExpression().getText();
                        String formattedCollectionExpression = DmnJsonConverterUtil
                            .formatCollectionExpression(operatorValue, inputExpressionVariable, expressionValue);

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
                    String oldOutputExpressionId = newOldIdMap.get(id);
                    RuleOutputClauseContainer ruleOutputClauseContainer = new RuleOutputClauseContainer();
                    ruleOutputClauseContainer.setOutputClause(ruleOutputContainerMap.get(id));

                    LiteralExpression outputEntry = new LiteralExpression();
                    outputEntry.setId("outputEntry_" + id + "_" + ruleCounter);

                    if (ruleNode.has(oldOutputExpressionId)) {
                        JsonNode expressionValueNode = ruleNode.get(oldOutputExpressionId);
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

    protected void addExtensionAttribute(String name, String value, DmnElement element) {
        DmnExtensionAttribute extensionAttribute = new DmnExtensionAttribute();
        extensionAttribute.setNamespace(MODEL_NAMESPACE);
        extensionAttribute.setNamespacePrefix("flowable");
        extensionAttribute.setName(name);
        extensionAttribute.setValue(value);

        element.addAttribute(extensionAttribute);
    }
}
