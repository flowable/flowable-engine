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

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.editor.constants.DmnJsonConstants;
import org.flowable.dmn.editor.constants.DmnStencilConstants;
import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DecisionTableOrientation;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnDiDiagram;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.DmnExtensionAttribute;
import org.flowable.dmn.model.DmnExtensionElement;
import org.flowable.dmn.model.GraphicInfo;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.InformationRequirement;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.dmn.model.UnaryTests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public DmnDefinition convertToDmn(JsonNode modelNode) {
        return convertToDmn(modelNode, null, null);
    }

    public DmnDefinition convertToDmn(JsonNode modelNode, String modelId) {
        return convertToDmn(modelNode, modelId, null);
    }

    public DmnDefinition convertToDmn(JsonNode modelNode, String modelId, int modelVersion, Date lastUpdated) {
        return convertToDmn(modelNode, modelId, null);
    }

    public DmnDefinition convertToDmn(JsonNode modelNode, String modelId, DmnJsonConverterContext converterContext) {
        DmnDefinition definition = new DmnDefinition();

        if (modelId != null) {
            definition.setId("definition_" + modelId);
        }
        definition.setNamespace(MODEL_NAMESPACE);
        definition.setExporter("Flowable Open Source Modeler");
        definition.setExporterVersion(getClass().getPackage().getImplementationVersion());
        definition.setTypeLanguage(URI_JSON);

        if (DmnJsonConverterUtil.isDRD(modelNode)) {
            processDRD(modelNode, definition, converterContext);
        } else {
            processDecisionTableDecision(modelNode, definition);
        }

        return definition;
    }

    public ObjectNode convertToJson(DmnDefinition model) {
        // check if model has no DRD DI info
        if (model.getDiDiagramMap().isEmpty()) {
            return convertDecisionTableToJson(model);
        }

        return convertToDecisionServiceJson(model, null);
    }

    public ObjectNode convertToJson(DmnDefinition model, DmnJsonConverterContext converterContext) {
        // check if model has no DRD DI info
        if (model.getDiDiagramMap().isEmpty()) {
            return convertDecisionTableToJson(model);
        }

        return convertToDecisionServiceJson(model, converterContext);
    }

    public ObjectNode convertToDecisionServiceJson(DmnDefinition model, DmnJsonConverterContext converterContext) {
        Map<String, List<String>> sourceTargetRefMap = createDecisionSourceTargetRefMap(model.getDecisions());

        ObjectNode modelNode = objectMapper.createObjectNode();

        // get the shapes of the first diagram
        Map.Entry<String, DmnDiDiagram> entry = model.getDiDiagramMap().entrySet().iterator().next();

        DmnDiDiagram diDiagram = entry.getValue();

        double maxX = diDiagram.getGraphicInfo().getWidth();
        double maxY = diDiagram.getGraphicInfo().getHeight();

        //        if (maxX < 1485) {
        //            maxX = 1485;
        //        }
        //
        //        if (maxY < 700) {
        //            maxY = 700;
        //        }

        modelNode.set("bounds", DmnJsonConverterUtil.createBoundsNode(maxX, maxY, 0, 0));

        ObjectNode stencilNode = objectMapper.createObjectNode();
        stencilNode.put("id", "DMNDiagram");
        modelNode.set("stencil", stencilNode);

        ObjectNode stencilsetNode = objectMapper.createObjectNode();
        stencilsetNode.put("namespace", "http://b3mn.org/stencilset/dmn1.2#");
        stencilsetNode.put("url", "../editor/stencilsets/dmn1.1/dmn1.2.json");
        modelNode.set("stencilset", stencilsetNode);

        ArrayNode shapesArrayNode = objectMapper.createArrayNode();
        modelNode.set(EDITOR_CHILD_SHAPES, shapesArrayNode);

        // get first decision service
        if (model.getDecisionServices().isEmpty()) {
            return modelNode;
        }
        DecisionService decisionService = model.getDecisionServices().get(0);

        // create expanded decision service node
        ObjectNode expandedDecisionServiceNode = createExpandedDecisionServiceNode(decisionService, model);
        shapesArrayNode.add(expandedDecisionServiceNode);

        ArrayNode expandedDecisionServiceChildShapeNodes = objectMapper.createArrayNode();
        expandedDecisionServiceNode.set(EDITOR_CHILD_SHAPES, expandedDecisionServiceChildShapeNodes);

        // create output decisions section node
        ObjectNode outputDecisionsNode = createOutputDecisionsNode(decisionService, model);
        expandedDecisionServiceChildShapeNodes.add(outputDecisionsNode);

        ArrayNode outputDecisionChildShapeNodes = objectMapper.createArrayNode();
        outputDecisionsNode.set(EDITOR_CHILD_SHAPES, outputDecisionChildShapeNodes);

        // create output decision nodes
        decisionService.getOutputDecisions()
                .forEach(decisionRef -> outputDecisionChildShapeNodes
                        .add(createOutputDecisionNode(decisionRef, decisionService.getId(), sourceTargetRefMap, model, converterContext)));

        // create encapsulated decisions section node
        ObjectNode encapsulatedDecisionsNode = createEncapsulatedDecisionsNode(decisionService, model);
        expandedDecisionServiceChildShapeNodes.add(encapsulatedDecisionsNode);

        ArrayNode encapsulatedDecisionChildShapeNodes = objectMapper.createArrayNode();
        encapsulatedDecisionsNode.set(EDITOR_CHILD_SHAPES, encapsulatedDecisionChildShapeNodes);

        // create encapsulated decision nodes
        decisionService.getEncapsulatedDecisions().forEach(
                decisionRef -> encapsulatedDecisionChildShapeNodes
                        .add(createEncapsulatedDecisionNode(decisionRef, decisionService.getId(), sourceTargetRefMap, model, converterContext)));

        // create information requirement nodes
        if (model.getFlowLocationMapByDiagramId(diDiagram.getId()) != null) {
            model.getFlowLocationMapByDiagramId(diDiagram.getId())
                    .forEach((id, graphicInfoList) -> shapesArrayNode.add(createInformationRequirementNode(id, graphicInfoList, model)));
        }
        return modelNode;
    }

    protected Map<String, List<String>> createDecisionSourceTargetRefMap(List<Decision> decisions) {
        Map<String, List<String>> sourceTargetMap = new HashMap<>();
        for (Decision targetDecision : decisions) {
            for (InformationRequirement informationRequirement : targetDecision.getRequiredDecisions()) {
                if (informationRequirement.getRequiredDecision() != null) {
                    String sourceDecisionId = informationRequirement.getRequiredDecision().getParsedId();
                    sourceTargetMap.computeIfAbsent(sourceDecisionId, k -> new ArrayList<>());
                    sourceTargetMap.get(sourceDecisionId).add(informationRequirement.getId());
                }
            }
        }
        return sourceTargetMap;
    }

    protected ObjectNode createInformationRequirementNode(String resourceId, List<GraphicInfo> graphicInfoList, DmnDefinition definition) {
        return createInformationRequirementNode(resourceId, graphicInfoList, null, definition);
    }

    protected ObjectNode createInformationRequirementNode(String resourceId, List<GraphicInfo> graphicInfoList, String diagramId, DmnDefinition definition) {
        GraphicInfo lowerRightGraphicInfo = graphicInfoList.get(0);
        GraphicInfo upperLeftGraphicInfo = graphicInfoList.get(1);

        ObjectNode informationRequirementNode = DmnJsonConverterUtil.createChildShape(resourceId, DmnStencilConstants.STENCIL_INFORMATION_REQUIREMENT,
                lowerRightGraphicInfo.getX(),
                lowerRightGraphicInfo.getY(),
                upperLeftGraphicInfo.getX(),
                upperLeftGraphicInfo.getY());

        // find corresponding decision
        String targetDecisionId = null;
        String sourceDecisionId = null;
        decisionLoop:
        for (Decision decision : definition.getDecisions()) {
            for (InformationRequirement informationRequirement : decision.getRequiredDecisions()) {
                if (informationRequirement.getId().equals(resourceId)) {
                    sourceDecisionId = informationRequirement.getRequiredDecision().getParsedId();
                    targetDecisionId = decision.getId();
                    break decisionLoop;
                }
            }
        }

        if (targetDecisionId != null && sourceDecisionId != null) {
            ArrayNode outgoingNode = objectMapper.createArrayNode();
            informationRequirementNode.set(EDITOR_OUTGOING, outgoingNode);
            ObjectNode resourceNode = objectMapper.createObjectNode();
            outgoingNode.add(resourceNode);
            resourceNode.put(EDITOR_SHAPE_ID, targetDecisionId);

            ObjectNode targetNode = objectMapper.createObjectNode();
            informationRequirementNode.set(EDITOR_TARGET, targetNode);
            targetNode.put(EDITOR_SHAPE_ID, targetDecisionId);

            GraphicInfo sourceGraphicInfo = getGraphicInfo(sourceDecisionId, diagramId, definition);
            GraphicInfo targetGraphicInfo = getGraphicInfo(targetDecisionId, diagramId, definition);

            ArrayNode dockersArrayNode = objectMapper.createArrayNode();
            informationRequirementNode.set(EDITOR_DOCKERS, dockersArrayNode);

            ObjectNode dockNode1 = objectMapper.createObjectNode();
            dockNode1.put(EDITOR_BOUNDS_X, sourceGraphicInfo.getWidth() / 2.0);
            dockNode1.put(EDITOR_BOUNDS_Y, sourceGraphicInfo.getHeight() / 2.0);
            dockersArrayNode.add(dockNode1);

            ObjectNode dockNode2 = objectMapper.createObjectNode();
            dockNode2.put(EDITOR_BOUNDS_X, targetGraphicInfo.getWidth() / 2.0);
            dockNode2.put(EDITOR_BOUNDS_Y, targetGraphicInfo.getHeight() / 2.0);
            dockersArrayNode.add(dockNode2);
        }

        return informationRequirementNode;
    }

    protected ObjectNode createOutputDecisionNode(DmnElementReference decisionRef, String decisionServiceId,
            Map<String, List<String>> sourceTargetRefMap, DmnDefinition model, DmnJsonConverterContext converterContext) {
        return createOutputDecisionNode(decisionRef, decisionServiceId, null, sourceTargetRefMap, model, converterContext);
    }

    protected ObjectNode createOutputDecisionNode(DmnElementReference decisionRef, String decisionServiceId, String diagramId,
            Map<String, List<String>> sourceTargetRefMap, DmnDefinition model, DmnJsonConverterContext converterContext) {
        Decision decision = model.getDecisionById(decisionRef.getParsedId());
        GraphicInfo graphicInfo = getGraphicInfo(decision.getId(), diagramId, model);
        GraphicInfo decisionServiceGraphicInfo = getGraphicInfo(decisionServiceId, diagramId, model);

        ObjectNode decisionNode = DmnJsonConverterUtil.createChildShape(decision.getId(), DmnStencilConstants.STENCIL_DECISION,
                graphicInfo.getX() - decisionServiceGraphicInfo.getX() + graphicInfo.getWidth(),
                graphicInfo.getY() - decisionServiceGraphicInfo.getY() + graphicInfo.getHeight(), graphicInfo.getX() - decisionServiceGraphicInfo.getX(),
                graphicInfo.getY() - decisionServiceGraphicInfo.getY());

        return populateDecisionNode(decisionNode, decision, sourceTargetRefMap, converterContext);
    }

    protected ObjectNode createEncapsulatedDecisionNode(DmnElementReference decisionRef, String decisionServiceId,
            Map<String, List<String>> sourceTargetRefMap, DmnDefinition model, DmnJsonConverterContext converterContext) {
        return createEncapsulatedDecisionNode(decisionRef, decisionServiceId, null, sourceTargetRefMap, model, converterContext);
    }

    protected ObjectNode createEncapsulatedDecisionNode(DmnElementReference decisionRef, String decisionServiceId, String diagramId,
            Map<String, List<String>> sourceTargetRefMap, DmnDefinition model, DmnJsonConverterContext converterContext) {
        Decision decision = model.getDecisionById(decisionRef.getParsedId());
        GraphicInfo graphicInfo = getGraphicInfo(decision.getId(), diagramId, model);
        List<GraphicInfo> decisionServiceDividerGraphicInfoList = getDecisionServiceDividerGraphicInfos(decisionServiceId, diagramId, model);
        GraphicInfo encapsulatedDecisionsGraphicInfo = decisionServiceDividerGraphicInfoList.get(0);

        ObjectNode decisionNode = DmnJsonConverterUtil.createChildShape(decision.getId(), DmnStencilConstants.STENCIL_DECISION,
                graphicInfo.getX() - encapsulatedDecisionsGraphicInfo.getX() + graphicInfo.getWidth(),
                graphicInfo.getY() - encapsulatedDecisionsGraphicInfo.getY() + graphicInfo.getHeight(),
                graphicInfo.getX() - encapsulatedDecisionsGraphicInfo.getX(), graphicInfo.getY() - encapsulatedDecisionsGraphicInfo.getY());

        return populateDecisionNode(decisionNode, decision, sourceTargetRefMap, converterContext);
    }

    protected ObjectNode populateDecisionNode(ObjectNode decisionNode, Decision decision, Map<String, List<String>> sourceTargetRefMap,
            DmnJsonConverterContext converterContext) {
        ObjectNode propertiesNode = objectMapper.createObjectNode();
        decisionNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);
        propertiesNode.put(PROPERTY_NAME, decision.getName());

        if (converterContext != null && converterContext.getDecisionTableModelInfoForDecisionTableModelKey(decision.getId()) != null) {
            Map<String, String> modelInfo = converterContext.getDecisionTableModelInfoForDecisionTableModelKey(decision.getId());
            ObjectNode modelRefNode = propertiesNode.putObject(PROPERTY_DECISION_TABLE_REFERENCE);
            modelRefNode.put("id", modelInfo.get("id"));
            modelRefNode.put("name", modelInfo.get("name"));
            modelRefNode.put("key", decision.getId());

        } else {
            ObjectNode modelRefNode = propertiesNode.putObject(PROPERTY_DECISION_TABLE_REFERENCE);
            modelRefNode.put("key", decision.getId());
        }

        ArrayNode outgoingNodeArray = objectMapper.createArrayNode();
        decisionNode.set(EDITOR_OUTGOING, outgoingNodeArray);

        if (sourceTargetRefMap.containsKey(decision.getId())) {
            sourceTargetRefMap.get(decision.getId()).forEach(targetDecisionId -> {
                ObjectNode outgoingNode = objectMapper.createObjectNode();
                outgoingNodeArray.add(outgoingNode);
                outgoingNode.put(EDITOR_SHAPE_ID, targetDecisionId);
            });
        }

        return decisionNode;
    }

    protected ObjectNode createOutputDecisionsNode(DecisionService decisionService, DmnDefinition model) {
        return createOutputDecisionsNode(decisionService, null, model);
    }

    protected ObjectNode createOutputDecisionsNode(DecisionService decisionService, String diDiagramId, DmnDefinition model) {
        List<GraphicInfo> decisionServiceDividerGraphicInfoList = getDecisionServiceDividerGraphicInfos(decisionService.getId(), diDiagramId, model);
        GraphicInfo outputDecisionsGraphicInfo = decisionServiceDividerGraphicInfoList.get(0);

        String resourceId = decisionService.getId() + "_outputDecisions";
        ObjectNode outputDecisionsNode = DmnJsonConverterUtil
                .createChildShape(resourceId, DmnStencilConstants.STENCIL_OUTPUT_DECISIONS, outputDecisionsGraphicInfo.getWidth(),
                        outputDecisionsGraphicInfo.getHeight(), 0, 0);

        return outputDecisionsNode;
    }

    protected ObjectNode createEncapsulatedDecisionsNode(DecisionService decisionService, DmnDefinition model) {
        return createEncapsulatedDecisionsNode(decisionService, null, model);
    }

    protected ObjectNode createEncapsulatedDecisionsNode(DecisionService decisionService, String diDiagramId, DmnDefinition model) {
        List<GraphicInfo> decisionServiceDividerGraphicInfoList = getDecisionServiceDividerGraphicInfos(decisionService.getId(), diDiagramId, model);
        GraphicInfo outputDecisionsGraphicInfo = decisionServiceDividerGraphicInfoList.get(0);
        GraphicInfo encapsulatedDecisionsGraphicInfo = decisionServiceDividerGraphicInfoList.get(1);

        String resourceId = decisionService.getId() + "_encapsulatedDecisions";
        ObjectNode encapsulatedDecisionsNode = DmnJsonConverterUtil
                .createChildShape(resourceId, DmnStencilConstants.STENCIL_ENCAPSULATED_DECISIONS, encapsulatedDecisionsGraphicInfo.getWidth(),
                        encapsulatedDecisionsGraphicInfo.getHeight() + outputDecisionsGraphicInfo.getHeight(), 0, outputDecisionsGraphicInfo.getHeight());

        return encapsulatedDecisionsNode;
    }

    protected ObjectNode createExpandedDecisionServiceNode(DecisionService decisionService, DmnDefinition model) {
        return createExpandedDecisionServiceNode(decisionService, null, model);
    }

    protected ObjectNode createExpandedDecisionServiceNode(DecisionService decisionService, String diDiagramId, DmnDefinition model) {
        GraphicInfo decisionServiceGraphicInfo = getGraphicInfo(decisionService.getId(), diDiagramId, model);
        ObjectNode decisionServiceNode = DmnJsonConverterUtil.createChildShape(decisionService.getId(), DmnStencilConstants.STENCIL_EXPANDED_DECISION_SERVICE,
                decisionServiceGraphicInfo.getX() + decisionServiceGraphicInfo.getWidth(),
                decisionServiceGraphicInfo.getY() + decisionServiceGraphicInfo.getHeight(), decisionServiceGraphicInfo.getX(),
                decisionServiceGraphicInfo.getY());

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        if (StringUtils.isNotEmpty(decisionService.getId())) {
            propertiesNode.put(PROPERTY_OVERRIDE_ID, decisionService.getId());
        }
        if (StringUtils.isNotEmpty(decisionService.getName())) {
            propertiesNode.put(PROPERTY_NAME, decisionService.getName());
        }

        decisionServiceNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);

        return decisionServiceNode;
    }

    protected GraphicInfo getGraphicInfo(String dmnElementId, String diDiagramId, DmnDefinition model) {
        GraphicInfo graphicInfo = null;
        if (StringUtils.isNotEmpty(diDiagramId)) {
            graphicInfo = model.getLocationMapByDiagramId(diDiagramId).get(dmnElementId);
        } else {
            graphicInfo = model.getGraphicInfo(dmnElementId);
        }

        if (graphicInfo == null) {
            throw new FlowableException("Could not find graphic info for element with id: " + dmnElementId);
        }

        return graphicInfo;
    }

    protected List<GraphicInfo> getDecisionServiceDividerGraphicInfos(String decisionServiceId, String diDiagramId, DmnDefinition model) {
        List<GraphicInfo> graphicInfos;
        if (StringUtils.isNotEmpty(diDiagramId)) {
            graphicInfos = model.getDecisionServiceDividerLocationMapByDiagramId(diDiagramId).get(decisionServiceId);
        } else {
            graphicInfos = model.getDecisionServiceDividerGraphicInfo(decisionServiceId);
        }

        if (graphicInfos == null) {
            throw new FlowableException("Could not find decision service divider graphic info for decision service with id: " + decisionServiceId);
        }

        return graphicInfos;
    }

    public ObjectNode convertDecisionTableToJson(DmnDefinition definition) {
        Decision firstDecision = definition.getDecisions().get(0);
        return convertDecisionDecisionTableToJson(firstDecision, definition.getId(), definition.getName(), definition.getDescription());
    }

    public ObjectNode convertDecisionDecisionTableToJson(Decision decision, String id, String name, String description) {
        ObjectNode modelNode = objectMapper.createObjectNode();
        DecisionTable decisionTable = (DecisionTable) decision.getExpression();

        modelNode.put("id", id);
        modelNode.put("key", decision.getId());
        modelNode.put("name", name);
        modelNode.put("modelVersion", MODEL_VERSION);
        modelNode.put("description", description);

        // only decision table decision are supported for now
        if (decisionTable == null) {
            return modelNode;
        }

        modelNode.put("hitIndicator", decisionTable.getHitPolicy().getValue());

        if (decisionTable.getAggregation() != null) {
            modelNode.put("collectOperator", decisionTable.getAggregation().name());
        }

        if (decision.isForceDMN11()) {
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

            if (clause.getInputValues() != null) {
                ArrayNode inputValues = objectMapper.createArrayNode();
                inputExpressionNode.set("entries", inputValues);
                for (Object inputValue : clause.getInputValues().getTextValues()) {
                    inputValues.add(String.valueOf(inputValue));
                }
            }

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

            if (clause.getOutputValues() != null) {
                ArrayNode outputValues = objectMapper.createArrayNode();
                outputExpressionNode.set("entries", outputValues);
                for (Object outputValue : clause.getOutputValues().getTextValues()) {
                    outputValues.add(String.valueOf(outputValue));
                }
            }

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

    protected void processDRD(JsonNode modelNode, DmnDefinition definition, DmnJsonConverterContext converterContext) {
        Map<String, JsonNode> shapeMap = new HashMap<>();
        Map<String, JsonNode> sourceRefMap = new HashMap<>();
        Map<String, List<JsonNode>> targetRefMap = new HashMap<>();
        Map<String, JsonNode> edgeMap = new HashMap<>();
        Map<String, List<JsonNode>> sourceAndTargetMap = new HashMap<>();

        preProcessShapes(modelNode, shapeMap, sourceRefMap);
        preProcessFlows(modelNode, edgeMap, shapeMap, sourceRefMap, sourceAndTargetMap, targetRefMap);

        ArrayNode shapesArrayNode = (ArrayNode) modelNode.get(EDITOR_CHILD_SHAPES);

        if (shapesArrayNode == null || shapesArrayNode.size() == 0) {
            return;
        }

        String decisionServiceId = DmnJsonConverterUtil.getPropertyValueAsString("drd_id", modelNode);
        String decisionServiceName = DmnJsonConverterUtil.getPropertyValueAsString("name", modelNode);
        definition.setName(decisionServiceName);

        // first create the (expanded) decision service
        shapesArrayNode.forEach(shapeNode -> {
            String stencilId = DmnJsonConverterUtil.getStencilId(shapeNode);
            if (STENCIL_EXPANDED_DECISION_SERVICE.equals(stencilId)) {
                DecisionService decisionService = new DecisionService();
                definition.addDecisionService(decisionService);

                decisionService.setId(decisionServiceId);
                if (StringUtils.isNotEmpty(decisionServiceName)) {
                    decisionService.setName(decisionServiceName);
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
                        processDRDDecision(definition, decisionServiceChildNode, converterContext, sourceRefMap, targetRefMap,
                                decisionService.getOutputDecisions());
                    } else if (STENCIL_ENCAPSULATED_DECISIONS.equals(decisionServiceChildNodeStencilId)) {
                        processDRDDecision(definition, decisionServiceChildNode, converterContext, sourceRefMap, targetRefMap,
                                decisionService.getEncapsulatedDecisions());
                    }
                });
            }
        });

        readShapeDI(modelNode, 0, 0, definition);
        readEdgeDI(edgeMap, sourceAndTargetMap, definition);
    }

    protected void readShapeDI(JsonNode objectNode, double parentX, double parentY, DmnDefinition definition) {
        // for now only one DiDiagram per model is supported
        JsonNode boundsNode = objectNode.get(EDITOR_BOUNDS);
        ObjectNode upperLeftNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_UPPER_LEFT);
        ObjectNode lowerRightNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_LOWER_RIGHT);

        GraphicInfo graphicInfo = new GraphicInfo();
        graphicInfo.setX(upperLeftNode.get(EDITOR_BOUNDS_X).asDouble() + parentX);
        graphicInfo.setY(upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY);
        graphicInfo.setWidth(lowerRightNode.get(EDITOR_BOUNDS_X).asDouble() - graphicInfo.getX() + parentX);
        graphicInfo.setHeight(lowerRightNode.get(EDITOR_BOUNDS_Y).asDouble() - graphicInfo.getY() + parentY);

        DmnDiDiagram diagram = new DmnDiDiagram();
        diagram.setName(DmnJsonConverterUtil.getValueAsString("name", objectNode));
        diagram.setId("DMNDiagram_" + DmnJsonConverterUtil.getPropertyValueAsString(DmnStencilConstants.PROPERTY_DRD_ID, objectNode));
        diagram.setGraphicInfo(graphicInfo);

        definition.addDiDiagram(diagram);

        readShapeDI(objectNode, parentX, parentY, definition, diagram.getId(), null);
    }

    protected void readShapeDI(JsonNode objectNode, double parentX, double parentY, DmnDefinition definition, String currentDiagramId, String parentElementId) {
        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {
                String stencilId = DmnJsonConverterUtil.getStencilId(jsonChildNode);
                if (!STENCIL_INFORMATION_REQUIREMENT.equals(stencilId)) {
                    String currentElementId = DmnJsonConverterUtil.getElementId(jsonChildNode);

                    JsonNode boundsNode = jsonChildNode.get(EDITOR_BOUNDS);
                    ObjectNode upperLeftNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_UPPER_LEFT);
                    ObjectNode lowerRightNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_LOWER_RIGHT);

                    if (STENCIL_OUTPUT_DECISIONS.equals(stencilId)) {
                        GraphicInfo graphicInfoLeft = new GraphicInfo();
                        graphicInfoLeft.setX(upperLeftNode.get(EDITOR_BOUNDS_X).asDouble() + parentX);
                        graphicInfoLeft.setY(upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + lowerRightNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY);
                        graphicInfoLeft.setWidth(lowerRightNode.get(EDITOR_BOUNDS_X).asDouble());
                        graphicInfoLeft.setHeight(lowerRightNode.get(EDITOR_BOUNDS_Y).asDouble());

                        List<GraphicInfo> graphicInfoList = new LinkedList<>(Arrays.asList(graphicInfoLeft));

                        definition.addDecisionServiceDividerGraphicInfoListByDiagramId(currentDiagramId, parentElementId, graphicInfoList);
                        readShapeDI(jsonChildNode, upperLeftNode.get(EDITOR_BOUNDS_X).asDouble() + parentX,
                                upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY, definition, currentDiagramId, currentElementId);
                    } else if (STENCIL_ENCAPSULATED_DECISIONS.equals(stencilId)) {
                        GraphicInfo graphicInfoRight = new GraphicInfo();
                        graphicInfoRight.setX(lowerRightNode.get(EDITOR_BOUNDS_X).asDouble() + parentX);
                        graphicInfoRight.setY(upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY);
                        graphicInfoRight.setWidth(lowerRightNode.get(EDITOR_BOUNDS_X).asDouble());
                        graphicInfoRight.setHeight(lowerRightNode.get(EDITOR_BOUNDS_Y).asDouble() - upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble());

                        definition.getDecisionServiceDividerLocationMapByDiagramId(currentDiagramId).get(parentElementId).add(graphicInfoRight);
                        readShapeDI(jsonChildNode, upperLeftNode.get(EDITOR_BOUNDS_X).asDouble() + parentX,
                                upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY, definition, currentDiagramId, currentElementId);
                    } else {
                        GraphicInfo graphicInfo = new GraphicInfo();

                        graphicInfo.setX(upperLeftNode.get(EDITOR_BOUNDS_X).asDouble() + parentX);
                        graphicInfo.setY(upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY);
                        graphicInfo.setWidth(lowerRightNode.get(EDITOR_BOUNDS_X).asDouble() - upperLeftNode.get(EDITOR_BOUNDS_X).asDouble());
                        graphicInfo.setHeight(lowerRightNode.get(EDITOR_BOUNDS_Y).asDouble() - upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble());

                        if (STENCIL_EXPANDED_DECISION_SERVICE.equals(stencilId) && !definition.getDecisionServices().isEmpty()) {
                            currentElementId = definition.getDecisionServices().get(0).getId();
                        }
                        definition.addGraphicInfoByDiagramId(currentDiagramId, currentElementId, graphicInfo);

                        readShapeDI(jsonChildNode, graphicInfo.getX(), graphicInfo.getY(), definition, currentDiagramId, currentElementId);
                    }
                }
            }
        }
    }

    protected void readEdgeDI(Map<String, JsonNode> edgeMap, Map<String, List<JsonNode>> sourceAndTargetMap, DmnDefinition definition) {
        // for now only one DiDiagram per model is supported
        // get first diagram
        Map.Entry<String, DmnDiDiagram> diagramEntry = definition.getDiDiagramMap().entrySet().iterator().next();
        DmnDiDiagram diDiagram = diagramEntry.getValue();

        for (Map.Entry<String, JsonNode> entry : edgeMap.entrySet()) {
            String edgeId = entry.getKey();
            JsonNode edgeNode = entry.getValue();
            List<JsonNode> sourceAndTargetList = sourceAndTargetMap.get(edgeId);

            JsonNode sourceRefNode = null;
            JsonNode targetRefNode = null;

            if (sourceAndTargetList != null && sourceAndTargetList.size() > 1) {
                sourceRefNode = sourceAndTargetList.get(0);
                targetRefNode = sourceAndTargetList.get(1);
            }

            if (sourceRefNode == null) {
                LOGGER.info("Skipping edge {} because source ref is null", edgeId);
                continue;
            }

            if (targetRefNode == null) {
                LOGGER.info("Skipping edge {} because target ref is null", edgeId);
                continue;
            }

            JsonNode dockersNode = edgeNode.get(EDITOR_DOCKERS);
            double sourceDockersX = dockersNode.get(0).get(EDITOR_BOUNDS_X).asDouble();
            double sourceDockersY = dockersNode.get(0).get(EDITOR_BOUNDS_Y).asDouble();

            GraphicInfo sourceInfo = definition.getGraphicInfoByDiagramId(diDiagram.getId(), DmnJsonConverterUtil.getElementId(sourceRefNode));
            GraphicInfo targetInfo = definition.getGraphicInfoByDiagramId(diDiagram.getId(), DmnJsonConverterUtil.getElementId(targetRefNode));

            double sourceRefLineX = sourceInfo.getX() + sourceDockersX;
            double sourceRefLineY = sourceInfo.getY() + sourceDockersY;

            double nextPointInLineX;
            double nextPointInLineY;

            nextPointInLineX = dockersNode.get(1).get(EDITOR_BOUNDS_X).asDouble();
            nextPointInLineY = dockersNode.get(1).get(EDITOR_BOUNDS_Y).asDouble();
            if (dockersNode.size() == 2) {
                nextPointInLineX += targetInfo.getX();
                nextPointInLineY += targetInfo.getY();
            }

            java.awt.geom.Line2D firstLine = new java.awt.geom.Line2D.Double(sourceRefLineX, sourceRefLineY, nextPointInLineX, nextPointInLineY);

            //            String sourceRefStencilId = DmnJsonConverterUtil.getStencilId(sourceRefNode);
            //            String targetRefStencilId = DmnJsonConverterUtil.getStencilId(targetRefNode);

            List<GraphicInfo> graphicInfoList = new ArrayList<>();

            // rectangle area for decision
            Area source2D = DmnJsonConverterUtil.createRectangle(sourceInfo);

            if (source2D != null) {
                Collection<Point2D> intersections = DmnJsonConverterUtil.getIntersections(firstLine, source2D);
                if (intersections != null && intersections.size() > 0) {
                    java.awt.geom.Point2D intersection = intersections.iterator().next();
                    graphicInfoList.add(DmnJsonConverterUtil.createGraphicInfo(intersection.getX(), intersection.getY()));
                } else {
                    graphicInfoList.add(DmnJsonConverterUtil.createGraphicInfo(sourceRefLineX, sourceRefLineY));
                }
            }

            java.awt.geom.Line2D lastLine;

            if (dockersNode.size() > 2) {
                for (int i = 1; i < dockersNode.size() - 1; i++) {
                    double x = dockersNode.get(i).get(EDITOR_BOUNDS_X).asDouble();
                    double y = dockersNode.get(i).get(EDITOR_BOUNDS_Y).asDouble();
                    graphicInfoList.add(DmnJsonConverterUtil.createGraphicInfo(x, y));
                }

                double startLastLineX = dockersNode.get(dockersNode.size() - 2).get(EDITOR_BOUNDS_X).asDouble();
                double startLastLineY = dockersNode.get(dockersNode.size() - 2).get(EDITOR_BOUNDS_Y).asDouble();

                double endLastLineX = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_X).asDouble();
                double endLastLineY = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_Y).asDouble();

                endLastLineX += targetInfo.getX();
                endLastLineY += targetInfo.getY();

                lastLine = new java.awt.geom.Line2D.Double(startLastLineX, startLastLineY, endLastLineX, endLastLineY);

            } else {
                lastLine = firstLine;
            }

            Area target2D = DmnJsonConverterUtil.createRectangle(targetInfo);

            if (target2D != null) {
                Collection<java.awt.geom.Point2D> intersections = DmnJsonConverterUtil.getIntersections(lastLine, target2D);
                if (intersections != null && intersections.size() > 0) {
                    java.awt.geom.Point2D intersection = intersections.iterator().next();
                    graphicInfoList.add(DmnJsonConverterUtil.createGraphicInfo(intersection.getX(), intersection.getY()));
                } else {
                    graphicInfoList.add(DmnJsonConverterUtil.createGraphicInfo(lastLine.getX2(), lastLine.getY2()));
                }
            }

            definition.addFlowGraphicInfoListByDiagramId(diDiagram.getId(), edgeId, graphicInfoList);
        }
    }

    protected void processDRDDecision(DmnDefinition definition, JsonNode decisionServiceChildNode, DmnJsonConverterContext converterContext,
            Map<String, JsonNode> sourceRefMap, Map<String, List<JsonNode>> targetRefMap, List<DmnElementReference> decisionServiceDecisions) {

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
                if (converterContext != null && converterContext.getDecisionTableKeyToJsonStringMap() != null) {
                    String decisionTableEditorJson = converterContext.getDecisionTableKeyToJsonStringMap().get(decisionTableKey);
                    if (StringUtils.isNotEmpty(decisionTableEditorJson)) {
                        try {
                            JsonNode decisionTableNode = objectMapper.readTree(decisionTableEditorJson);

                            DecisionTable decisionTable = new DecisionTable();
                            decision.setExpression(decisionTable);

                            processDecisionTable(decisionTableNode, decisionTable);
                        } catch (Exception ex) {
                            LOGGER.error("Error while parsing decision table editor JSON: {}", decisionTableEditorJson);
                        }
                    } else {
                        LOGGER.warn("Could not find decision table for key: {}", decisionTableKey);
                    }
                }
            }

            if (targetRefMap.containsKey(decisionChildNode.get("resourceId").asText())) {
                List<JsonNode> informationRequirementNodes = targetRefMap.get(decisionChildNode.get("resourceId").asText());

                informationRequirementNodes.forEach(informationRequirementNode -> {
                    InformationRequirement informationRequirement = new InformationRequirement();
                    informationRequirement.setId(DmnJsonConverterUtil.getElementId(informationRequirementNode));
                    informationRequirement.setName(DmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, informationRequirementNode));

                    JsonNode requiredDecisionNode = sourceRefMap.get(DmnJsonConverterUtil.getElementId(informationRequirementNode));

                    DmnElementReference requiredDecisionReference = createDmnElementReference(requiredDecisionNode);

                    informationRequirement.setRequiredDecision(requiredDecisionReference);
                    decision.addRequiredDecision(informationRequirement);
                });
            }

            decisionServiceDecisions.add(createDmnElementReference(decisionChildNode));
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

    protected void preProcessFlows(JsonNode objectNode, Map<String, JsonNode> edgeMap, Map<String, JsonNode> shapeMap, Map<String, JsonNode> sourceRefMap,
            Map<String, List<JsonNode>> sourceAndTargetMap, Map<String, List<JsonNode>> targetRefMap) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {
                String stencilId = DmnJsonConverterUtil.getStencilId(jsonChildNode);
                if (!STENCIL_INFORMATION_REQUIREMENT.equals(stencilId)) {
                    preProcessFlows(jsonChildNode, edgeMap, shapeMap, sourceRefMap, sourceAndTargetMap, targetRefMap);
                } else {
                    String childEdgeId = DmnJsonConverterUtil.getElementId(jsonChildNode);

                    JsonNode targetNode = jsonChildNode.get("target");
                    if (targetNode != null && !targetNode.isNull()) {
                        String targetRefId = targetNode.get(EDITOR_SHAPE_ID).asText();
                        List<JsonNode> sourceAndTargetList = new ArrayList<>();
                        sourceAndTargetList.add(sourceRefMap.get(jsonChildNode.get(EDITOR_SHAPE_ID).asText()));
                        sourceAndTargetList.add(shapeMap.get(targetRefId));
                        sourceAndTargetMap.put(childEdgeId, sourceAndTargetList);

                        if (targetRefMap.containsKey(targetRefId)) {
                            LOGGER.debug("ALREADY CONTAINS");
                        }

                        targetRefMap.computeIfAbsent(targetRefId, k -> new ArrayList<>());
                        targetRefMap.get(targetRefId).add(jsonChildNode);
                    }
                    edgeMap.put(childEdgeId, jsonChildNode);
                }
            }
        }
    }

    protected void processDecisionTableDecision(JsonNode modelNode, DmnDefinition definition) {
        // check and migrate model
        DmnJsonConverterUtil.migrateModel(modelNode, objectMapper);

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

                    String inputValuesText = formatInputOutputValuesText(inputExpression.getTypeRef(), inputEntries);
                    inputValues.setText(inputValuesText);

                    // add to clause
                    inputClause.setInputValues(inputValues);
                }

                // add to map
                ruleInputContainerMap.put(inputExpressionId, inputClause);

                decisionTable.addInput(inputClause);
            }
        }
    }

    protected String formatInputOutputValuesText(String type, List<Object> inputOutputValues) {
        if ("number".equals(type) || "double".equals(type) || "boolean".equals(type)) {
            return StringUtils.join(inputOutputValues, ",");
        } else {
            return "\"" + StringUtils.join(inputOutputValues, "\",\"") + "\"";
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

                    String outputValuesText = formatInputOutputValuesText(outputClause.getTypeRef(), outputEntries);
                    outputValues.setText(outputValuesText);

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
