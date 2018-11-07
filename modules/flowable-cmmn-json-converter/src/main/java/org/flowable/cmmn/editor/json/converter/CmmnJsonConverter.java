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
package org.flowable.cmmn.editor.json.converter;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.constants.EditorJsonConstants;
import org.flowable.cmmn.editor.json.converter.util.CollectionUtils;
import org.flowable.cmmn.editor.json.model.CmmnModelInfo;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.HasEntryCriteria;
import org.flowable.cmmn.model.HasExitCriteria;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.TimerEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class CmmnJsonConverter implements EditorJsonConstants, CmmnStencilConstants, ActivityProcessor {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CmmnJsonConverter.class);

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected static Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap = new HashMap<>();
    protected static Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap = new HashMap<>();

    public static final String MODELER_NAMESPACE = "http://flowable.org/modeler";
    protected static final DateFormat defaultFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    protected static final DateFormat entFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    static {

        // connectors
        AssociationJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);

        // task types
        HumanTaskJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
        ServiceTaskJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
        DecisionTaskJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
        HttpTaskJsonConverter.fillTypes(convertersToCmmnMap);
        CaseTaskJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
        ProcessTaskJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
        TimerEventListenerJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
        UserEventListenerJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
        TaskJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
        ScriptTaskJsonConverter.fillTypes(convertersToCmmnMap);

        // milestone
        MilestoneJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);

        // boundary events
        CriterionJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);

        // stage
        StageJsonConverter.fillTypes(convertersToCmmnMap, convertersToJsonMap);
    }

    private static final List<String> DI_RECTANGLES = new ArrayList<>();
    private static final List<String> DI_CIRCLES = new ArrayList<>();
    private static final List<String> DI_SENTRY = new ArrayList<>();

    static {
        DI_CIRCLES.add(STENCIL_TIMER_EVENT_LISTENER);
        DI_CIRCLES.add(STENCIL_USER_EVENT_LISTENER);

        DI_RECTANGLES.add(STENCIL_TASK);
        DI_RECTANGLES.add(STENCIL_TASK_HUMAN);
        DI_RECTANGLES.add(STENCIL_TASK_SERVICE);
        DI_RECTANGLES.add(STENCIL_TASK_DECISION);
        DI_RECTANGLES.add(STENCIL_TASK_CASE);
        DI_RECTANGLES.add(STENCIL_TASK_PROCESS);
        DI_RECTANGLES.add(STENCIL_MILESTONE);
        DI_RECTANGLES.add(STENCIL_STAGE);

        DI_SENTRY.add(STENCIL_ENTRY_CRITERION);
        DI_SENTRY.add(STENCIL_EXIT_CRITERION);
    }

    protected double lineWidth = 0.05d;

    public ObjectNode convertToJson(CmmnModel model) {
        return convertToJson(model, null, null);
    }

    public ObjectNode convertToJson(CmmnModel model, Map<String, CmmnModelInfo> formKeyMap, Map<String, CmmnModelInfo> decisionTableKeyMap) {
        ObjectNode modelNode = objectMapper.createObjectNode();
        double maxX = 0.0;
        double maxY = 0.0;
        for (GraphicInfo flowInfo : model.getLocationMap().values()) {
            if ((flowInfo.getX() + flowInfo.getWidth()) > maxX) {
                maxX = flowInfo.getX() + flowInfo.getWidth();
            }

            if ((flowInfo.getY() + flowInfo.getHeight()) > maxY) {
                maxY = flowInfo.getY() + flowInfo.getHeight();
            }
        }
        maxX += 50;
        maxY += 50;

        if (maxX < 1485) {
            maxX = 1485;
        }

        if (maxY < 700) {
            maxY = 700;
        }

        modelNode.set("bounds", CmmnJsonConverterUtil.createBoundsNode(maxX, maxY, 0, 0));

        ObjectNode stencilNode = objectMapper.createObjectNode();
        stencilNode.put("id", "CMMNDiagram");
        modelNode.set("stencil", stencilNode);

        ObjectNode stencilsetNode = objectMapper.createObjectNode();
        stencilsetNode.put("namespace", "http://b3mn.org/stencilset/cmmn1.1#");
        stencilsetNode.put("url", "../editor/stencilsets/cmmn1.1/cmmn1.1.json");
        modelNode.set("stencilset", stencilsetNode);

        ArrayNode shapesArrayNode = objectMapper.createArrayNode();

        Case caseModel = model.getPrimaryCase();

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        if (StringUtils.isNotEmpty(caseModel.getId())) {
            propertiesNode.put(PROPERTY_CASE_ID, caseModel.getId());
        }
        if (StringUtils.isNotEmpty(caseModel.getName())) {
            propertiesNode.put(PROPERTY_NAME, caseModel.getName());
        }
        if (StringUtils.isNotEmpty(caseModel.getDocumentation())) {
            propertiesNode.put(PROPERTY_DOCUMENTATION, caseModel.getDocumentation());
        }
        if (StringUtils.isNotEmpty(caseModel.getInitiatorVariableName())) {
            propertiesNode.put(PROPERTY_CASE_INITIATOR_VARIABLE_NAME,  caseModel.getInitiatorVariableName());
        }

        if (StringUtils.isNoneEmpty(model.getTargetNamespace())) {
            propertiesNode.put(PROPERTY_CASE_NAMESPACE, model.getTargetNamespace());
        }

        modelNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);

        Stage planModelStage = caseModel.getPlanModel();
        GraphicInfo planModelGraphicInfo = model.getGraphicInfo(planModelStage.getId());
        ObjectNode planModelNode = CmmnJsonConverterUtil.createChildShape(planModelStage.getId(), STENCIL_PLANMODEL, planModelGraphicInfo.getX() + planModelGraphicInfo.getWidth(),
                        planModelGraphicInfo.getY() + planModelGraphicInfo.getHeight(), planModelGraphicInfo.getX(), planModelGraphicInfo.getY());

        ObjectNode planModelPropertiesNode = objectMapper.createObjectNode();
        if (StringUtils.isNotEmpty(planModelStage.getName())) {
            planModelPropertiesNode.put(PROPERTY_NAME, planModelStage.getName());
        }
        if (StringUtils.isNotEmpty(planModelStage.getDocumentation())) {
            planModelPropertiesNode.put(PROPERTY_DOCUMENTATION, planModelStage.getDocumentation());
        }
        if (planModelStage.isAutoComplete()) {
            planModelPropertiesNode.put(PROPERTY_IS_AUTOCOMPLETE, planModelStage.isAutoComplete());
        }
        if (StringUtils.isNotEmpty(planModelStage.getAutoCompleteCondition())) {
            planModelPropertiesNode.put(PROPERTY_AUTOCOMPLETE_CONDITION, planModelStage.getAutoCompleteCondition());
        }
        if (StringUtils.isNotEmpty(planModelStage.getFormKey())) {
            planModelPropertiesNode.put(PROPERTY_FORMKEY, planModelStage.getFormKey());
        }
        planModelNode.set(EDITOR_SHAPE_PROPERTIES, planModelPropertiesNode);

        planModelNode.putArray(EDITOR_OUTGOING);
        shapesArrayNode.add(planModelNode);

        ArrayNode outgoingArrayNode = objectMapper.createArrayNode();
        for (Criterion criterion : planModelStage.getExitCriteria()) {
            GraphicInfo criterionGraphicInfo = model.getGraphicInfo(criterion.getId());
            ObjectNode criterionNode = CmmnJsonConverterUtil.createChildShape(criterion.getId(), STENCIL_EXIT_CRITERION,
                    criterionGraphicInfo.getX() + criterionGraphicInfo.getWidth(), criterionGraphicInfo.getY() + criterionGraphicInfo.getHeight(),
                    criterionGraphicInfo.getX(), criterionGraphicInfo.getY());

            shapesArrayNode.add(criterionNode);
            ObjectNode criterionPropertiesNode = objectMapper.createObjectNode();
            criterionPropertiesNode.put(PROPERTY_OVERRIDE_ID, criterion.getId());
            new CriterionJsonConverter().convertElementToJson(criterionNode, criterionPropertiesNode, this, criterion, model);
            criterionNode.set(EDITOR_SHAPE_PROPERTIES, criterionPropertiesNode);

            if (CollectionUtils.isNotEmpty(criterion.getOutgoingAssociations())) {
                ArrayNode criterionOutgoingArrayNode = objectMapper.createArrayNode();
                for (Association association : criterion.getOutgoingAssociations()) {
                    criterionOutgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getId()));
                }

                criterionNode.set("outgoing", criterionOutgoingArrayNode);
            }

            outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(criterion.getId()));
        }
        planModelNode.set("outgoing", outgoingArrayNode);

        ArrayNode planModelShapesArrayNode = objectMapper.createArrayNode();
        planModelNode.set(EDITOR_CHILD_SHAPES, planModelShapesArrayNode);

        processPlanItems(caseModel.getPlanModel(), model, planModelShapesArrayNode, formKeyMap, decisionTableKeyMap, planModelGraphicInfo.getX(), planModelGraphicInfo.getY());

        for (Association association : model.getAssociations()) {
            AssociationJsonConverter associationJsonConverter = new AssociationJsonConverter();
            associationJsonConverter.convertToJson(association, model, shapesArrayNode);
        }

        modelNode.set(EDITOR_CHILD_SHAPES, shapesArrayNode);
        return modelNode;
    }

    @Override
    public void processPlanItems(Stage stage, CmmnModel model, ArrayNode shapesArrayNode,
                                 Map<String, CmmnModelInfo> formKeyMap, Map<String, CmmnModelInfo> decisionTableKeyMap, double subProcessX, double subProcessY) {

        for (PlanItem planItem : stage.getPlanItems()) {
            processPlanItem(planItem, stage, model, shapesArrayNode, formKeyMap, decisionTableKeyMap, subProcessX, subProcessY);
        }
    }

    protected void processPlanItem(PlanItem planItem, Stage stage, CmmnModel model,
            ArrayNode shapesArrayNode, Map<String, CmmnModelInfo> formKeyMap, Map<String, CmmnModelInfo> decisionTableKeyMap, double containerX, double containerY) {

        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        Class<? extends BaseCmmnJsonConverter> converter = convertersToJsonMap.get(planItemDefinition.getClass());
        if (converter != null) {
            try {
                BaseCmmnJsonConverter converterInstance = converter.newInstance();
                if (converterInstance instanceof FormKeyAwareConverter) {
                    ((FormKeyAwareConverter) converterInstance).setFormKeyMap(formKeyMap);
                }
                if (converterInstance instanceof DecisionTableKeyAwareConverter) {
                    ((DecisionTableKeyAwareConverter) converterInstance).setDecisionTableKeyMap(decisionTableKeyMap);
                }

                converterInstance.convertToJson(planItem, this, model, stage, shapesArrayNode, containerX, containerY);

            } catch (Exception e) {
                LOGGER.error("Error converting {}", planItemDefinition, e);
            }
        }
    }

    public CmmnModel convertToCmmnModel(JsonNode modelNode) {
        return convertToCmmnModel(modelNode, null, null, null, null);
    }

    public CmmnModel convertToCmmnModel(JsonNode modelNode, Map<String, String> formKeyMap, Map<String, String> decisionTableKeyMap,
                    Map<String, String> caseModelKeyMap, Map<String, String> processModelKeyMap) {

        CmmnModel cmmnModel = new CmmnModel();
        CmmnModelIdHelper cmmnModelIdHelper = new CmmnModelIdHelper();


        cmmnModel.setTargetNamespace("http://flowable.org/cmmn"); // will be overriden later with actual value
        Map<String, JsonNode> shapeMap = new HashMap<>();
        Map<String, JsonNode> sourceRefMap = new HashMap<>();
        Map<String, JsonNode> edgeMap = new HashMap<>();
        Map<String, List<JsonNode>> sourceAndTargetMap = new HashMap<>();

        readShapeInfo(modelNode, shapeMap, sourceRefMap);
        filterAllEdges(modelNode, edgeMap, sourceAndTargetMap, shapeMap, sourceRefMap);

        ArrayNode shapesArrayNode = (ArrayNode) modelNode.get(EDITOR_CHILD_SHAPES);

        if (shapesArrayNode == null || shapesArrayNode.size() == 0) {
            return cmmnModel;
        }

        Case caseModel = new Case();
        cmmnModel.getCases().add(caseModel);
        caseModel.setId(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_CASE_ID, modelNode));
        caseModel.setName(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, modelNode));
        caseModel.setInitiatorVariableName(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_CASE_INITIATOR_VARIABLE_NAME, modelNode));
        if (StringUtils.isEmpty(caseModel.getInitiatorVariableName())) {
            caseModel.setInitiatorVariableName("initiator");
        }

        String namespace = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_CASE_NAMESPACE, modelNode);
        if (StringUtils.isNotEmpty(namespace)) {
            cmmnModel.setTargetNamespace(namespace);
        }
        caseModel.setDocumentation(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_DOCUMENTATION, modelNode));

        JsonNode planModelShape = shapesArrayNode.get(0);

        JsonNode planModelShapesArray = planModelShape.get(EDITOR_CHILD_SHAPES);
        Stage planModelStage = new Stage();
        planModelStage.setId(CmmnJsonConverterUtil.getElementId(planModelShape));
        planModelStage.setName(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, planModelShape));
        planModelStage.setDocumentation(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_DOCUMENTATION, planModelShape));
        planModelStage.setAutoComplete(CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_AUTOCOMPLETE, planModelShape));

        String autocompleteCondition = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_AUTOCOMPLETE_CONDITION, planModelShape);
        if (StringUtils.isNotEmpty(autocompleteCondition)) {
            planModelStage.setAutoCompleteCondition(autocompleteCondition);
        }
        planModelStage.setFormKey(CmmnJsonConverterUtil.getPropertyFormKey(planModelShape, formKeyMap));
        planModelStage.setPlanModel(true);

        caseModel.setPlanModel(planModelStage);

        processJsonElements(planModelShapesArray, modelNode, planModelStage, shapeMap, formKeyMap, decisionTableKeyMap,
                        caseModelKeyMap, processModelKeyMap, cmmnModel, cmmnModelIdHelper);

        List<String> planModelExitCriteriaRefs = new ArrayList<>();
        for (JsonNode shapeNode : shapesArrayNode) {
            // associations are now all on root level
            if (STENCIL_ASSOCIATION.equalsIgnoreCase(CmmnJsonConverterUtil.getStencilId(shapeNode))) {
                AssociationJsonConverter associationConverter = new AssociationJsonConverter();
                Association association = associationConverter.convertJsonToElement(shapeNode, modelNode, this, planModelStage, shapeMap, cmmnModel, cmmnModelIdHelper);
                cmmnModel.addAssociation(association);

            // exit criteria for the plan model are on the root level
            } else if (STENCIL_EXIT_CRITERION.equalsIgnoreCase(CmmnJsonConverterUtil.getStencilId(shapeNode))) {
                JsonNode resourceNode = shapeNode.get(EDITOR_SHAPE_ID);
                if (resourceNode != null) {
                    planModelExitCriteriaRefs.add(resourceNode.asText());
                    CriterionJsonConverter criterionJsonConverter = new CriterionJsonConverter();
                    criterionJsonConverter.convertJsonToElement(shapeNode, modelNode, this, planModelStage, shapeMap, cmmnModel, cmmnModelIdHelper);
                }
            }
        }

        readShapeDI(modelNode, 0, 0, cmmnModel);
        readEdgeDI(edgeMap, sourceAndTargetMap, cmmnModel);

        // post handling of process elements
        Map<String, List<Association>> associationMap = postProcessAssociations(cmmnModel);
        postProcessElements(planModelStage, planModelStage.getPlanItems(), edgeMap, associationMap, cmmnModel, cmmnModelIdHelper);

        // Create sentries for exit criteria on plan model
        createSentryParts(planModelExitCriteriaRefs, planModelStage, associationMap, cmmnModel, cmmnModelIdHelper, null, planModelStage);

        return cmmnModel;
    }

    @Override
    public void processJsonElements(JsonNode shapesArrayNode, JsonNode modelNode, BaseElement parentElement, Map<String, JsonNode> shapeMap,
                                    Map<String, String> formMap, Map<String, String> decisionTableMap, Map<String, String> caseModelKeyMap, Map<String, String> processModelKeyMap,
                                    CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {

        for (JsonNode shapeNode : shapesArrayNode) {
            String stencilId = CmmnJsonConverterUtil.getStencilId(shapeNode);
            Class<? extends BaseCmmnJsonConverter> converter = convertersToCmmnMap.get(stencilId);
            try {
                BaseCmmnJsonConverter converterInstance = converter.newInstance();
                if (converterInstance instanceof DecisionTableAwareConverter) {
                    ((DecisionTableAwareConverter) converterInstance).setDecisionTableMap(decisionTableMap);
                }

                if (converterInstance instanceof FormAwareConverter) {
                    ((FormAwareConverter) converterInstance).setFormMap(formMap);
                }

                if (converterInstance instanceof CaseModelAwareConverter) {
                    ((CaseModelAwareConverter) converterInstance).setCaseModelMap(caseModelKeyMap);
                }

                if (converterInstance instanceof ProcessModelAwareConverter) {
                    ((ProcessModelAwareConverter) converterInstance).setProcessModelMap(processModelKeyMap);
                }

                converterInstance.convertToCmmnModel(shapeNode, modelNode, this, parentElement, shapeMap, cmmnModel, cmmnModelIdHelper);
            } catch (Exception e) {
                LOGGER.error("Error converting {}", CmmnJsonConverterUtil.getStencilId(shapeNode));
            }
        }
    }

    protected Map<String, List<Association>> postProcessAssociations(CmmnModel cmmnModel) {
        Map<String, List<Association>> associationMap = new HashMap<>();
        for (Association association : cmmnModel.getAssociations()) {
            if (association.getSourceRef() == null || association.getTargetRef() == null) {
                continue;
            }

            boolean sourceIsCriterion = true;
            Criterion criterion = cmmnModel.getCriterion(association.getSourceRef());
            PlanItemDefinition planItemDefinition;
            if (criterion != null) {
                planItemDefinition = cmmnModel.findPlanItemDefinition(association.getTargetRef());

            } else {
                criterion = cmmnModel.getCriterion(association.getTargetRef());
                if (criterion == null) {
                    continue;
                }

                sourceIsCriterion = false;
                planItemDefinition = cmmnModel.findPlanItemDefinition(association.getSourceRef());
                if (planItemDefinition == null) { // exit criteria on planmodel
                    planItemDefinition = cmmnModel.findPlanItemDefinition(association.getTargetRef());
                }
            }

            if (planItemDefinition == null) {
                continue;
            }

            PlanItem planItem = cmmnModel.findPlanItem(planItemDefinition.getPlanItemRef());
            if (sourceIsCriterion) {
                association.setSourceElement(criterion);
                criterion.addOutgoingAssociation(association);
                association.setTargetElement(planItem);
                association.setTargetRef(planItem.getId());
                planItem.addIncomingAssociation(association);

            } else {
                association.setTargetElement(criterion);
                criterion.addIncomingAssociation(association);
                association.setSourceElement(planItem);
                association.setSourceRef(planItem.getId());
                planItem.addOutgoingAssociation(association);
            }

            if (!associationMap.containsKey(criterion.getId())) {
                associationMap.put(criterion.getId(), new ArrayList<Association>());
            }
            associationMap.get(criterion.getId()).add(association);
        }
        return associationMap;
    }

    protected void postProcessElements(Stage parentStage, List<PlanItem> planItems, Map<String, JsonNode> edgeMap,
            Map<String, List<Association>> associationMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {

        for (PlanItem planItem : planItems) {
            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();

            if (planItemDefinition instanceof Stage) {
                Stage stage = (Stage) planItemDefinition;
                postProcessElements(stage, stage.getPlanItems(), edgeMap, associationMap, cmmnModel, cmmnModelIdHelper);

            } else if (planItemDefinition instanceof TimerEventListener) {
                TimerEventListener timerEventListener = (TimerEventListener) planItemDefinition;

                // The modeler json has referenced the plan item definition. Swapping it with the plan item id when found.
                String startTriggerSourceRef = timerEventListener.getTimerStartTriggerSourceRef();
                if (StringUtils.isNotEmpty(startTriggerSourceRef)) {
                    PlanItemDefinition referencedPlanItemDefinition = parentStage.findPlanItemDefinition(startTriggerSourceRef);
                    timerEventListener.setTimerStartTriggerSourceRef(referencedPlanItemDefinition.getPlanItemRef());
                }
            }

             if (CollectionUtils.isNotEmpty(planItem.getCriteriaRefs())) {
                 createSentryParts(planItem.getCriteriaRefs(), parentStage, associationMap, cmmnModel, cmmnModelIdHelper, planItem, planItem);
            }

        }
    }

    protected void createSentryParts(List<String> criteriaRefs, Stage parentStage, Map<String, List<Association>> associationMap, CmmnModel cmmnModel,
            CmmnModelIdHelper cmmnModelIdHelper, HasEntryCriteria hasEntryCriteriaElement, HasExitCriteria hasExitCriteriaElement) {

        for (String criterionRef : criteriaRefs) {
            String criterionId = cmmnModel.getCriterionId(criterionRef);
            if (criterionId == null) {
                continue;
            }

            Criterion criterion = cmmnModel.getCriterion(criterionId);
            if (criterion == null) {
                continue;
            }

            // replace criterion attachedToRefId to from plan item definition id to plan item id
            PlanItemDefinition planItemDefinition = cmmnModel.findPlanItemDefinition(criterion.getAttachedToRefId());
            criterion.setAttachedToRefId(planItemDefinition.getPlanItemRef());

            parentStage.addSentry(criterion.getSentry());

            boolean associationsFound = associationMap.containsKey(criterion.getId());
            if (!associationsFound && criterion.getSentry() == null) {
                continue;
            } else {
                if (criterion.isEntryCriterion()) {
                    hasEntryCriteriaElement.addEntryCriterion(criterion);
                } else if (criterion.isExitCriterion()) {
                    hasExitCriteriaElement.addExitCriterion(criterion);
                }
            }

            if (associationsFound) {
                List<Association> associations = associationMap.get(criterion.getId());
                for (Association association : associations) {
                    PlanItem criterionPlanItem = null;
                    if (association.getSourceRef().equals(criterion.getId())) {
                        criterionPlanItem = (PlanItem) association.getTargetElement();
                    } else {
                        criterionPlanItem = (PlanItem) association.getSourceElement();
                    }

                    SentryOnPart sentryOnPart = new SentryOnPart();
                    sentryOnPart.setId("sentryOnPart" + cmmnModelIdHelper.nextSentryOnPartId());
                    sentryOnPart.setSourceRef(criterionPlanItem.getId());
                    sentryOnPart.setSource(criterionPlanItem);

                    if (StringUtils.isNotEmpty(association.getTransitionEvent())) {
                        sentryOnPart.setStandardEvent(association.getTransitionEvent());
                    } else {
                        sentryOnPart.setStandardEvent("complete");
                    }
                    criterion.getSentry().addSentryOnPart(sentryOnPart);
                }
            }

        }
    }

    protected void readShapeInfo(JsonNode objectNode, Map<String, JsonNode> shapeMap, Map<String, JsonNode> sourceRefMap) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {

                String stencilId = CmmnJsonConverterUtil.getStencilId(jsonChildNode);
                if (!STENCIL_ASSOCIATION.equals(stencilId)) {

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

                    readShapeInfo(jsonChildNode, shapeMap, sourceRefMap);
                }
            }
        }
    }

    protected void readShapeDI(JsonNode objectNode, double parentX, double parentY, CmmnModel cmmnModel) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {

                String stencilId = CmmnJsonConverterUtil.getStencilId(jsonChildNode);
                if (!STENCIL_ASSOCIATION.equals(stencilId)) {

                    GraphicInfo graphicInfo = new GraphicInfo();

                    JsonNode boundsNode = jsonChildNode.get(EDITOR_BOUNDS);
                    ObjectNode upperLeftNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_UPPER_LEFT);
                    ObjectNode lowerRightNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_LOWER_RIGHT);

                    graphicInfo.setX(upperLeftNode.get(EDITOR_BOUNDS_X).asDouble() + parentX);
                    graphicInfo.setY(upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY);
                    graphicInfo.setWidth(lowerRightNode.get(EDITOR_BOUNDS_X).asDouble() - graphicInfo.getX() + parentX);
                    graphicInfo.setHeight(lowerRightNode.get(EDITOR_BOUNDS_Y).asDouble() - graphicInfo.getY() + parentY);

                    String elementId = CmmnJsonConverterUtil.getElementId(jsonChildNode);
                    if (STENCIL_ENTRY_CRITERION.equals(stencilId) || STENCIL_EXIT_CRITERION.equals(stencilId)) {
                        cmmnModel.addGraphicInfo(elementId, graphicInfo);
                    } else {
                        PlanItemDefinition planItemDefinition = cmmnModel.findPlanItemDefinition(elementId);
                        if (!(planItemDefinition instanceof Stage) || !((Stage) planItemDefinition).isPlanModel()) {
                            PlanItem planItem = cmmnModel.findPlanItem(planItemDefinition.getPlanItemRef());
                            cmmnModel.addGraphicInfo(planItem.getId(), graphicInfo);
                        } else {
                            cmmnModel.addGraphicInfo(planItemDefinition.getId(), graphicInfo);
                        }
                    }

                    readShapeDI(jsonChildNode, graphicInfo.getX(), graphicInfo.getY(), cmmnModel);
                }
            }
        }
    }

    protected void filterAllEdges(JsonNode objectNode, Map<String, JsonNode> edgeMap, Map<String, List<JsonNode>> sourceAndTargetMap, Map<String, JsonNode> shapeMap, Map<String, JsonNode> sourceRefMap) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {

                ObjectNode childNode = (ObjectNode) jsonChildNode;
                String stencilId = CmmnJsonConverterUtil.getStencilId(childNode);
                if (STENCIL_PLANMODEL.equals(stencilId) || STENCIL_STAGE.equals(stencilId)) {

                    filterAllEdges(childNode, edgeMap, sourceAndTargetMap, shapeMap, sourceRefMap);

                } else if (STENCIL_ASSOCIATION.equals(stencilId)) {

                    String childEdgeId = CmmnJsonConverterUtil.getElementId(childNode);
                    JsonNode targetNode = childNode.get("target");
                    if (targetNode != null && !targetNode.isNull()) {
                        String targetRefId = targetNode.get(EDITOR_SHAPE_ID).asText();
                        List<JsonNode> sourceAndTargetList = new ArrayList<>();
                        sourceAndTargetList.add(sourceRefMap.get(childNode.get(EDITOR_SHAPE_ID).asText()));
                        sourceAndTargetList.add(shapeMap.get(targetRefId));
                        sourceAndTargetMap.put(childEdgeId, sourceAndTargetList);
                    }
                    edgeMap.put(childEdgeId, childNode);
                }
            }
        }
    }

    protected void readEdgeDI(Map<String, JsonNode> edgeMap, Map<String, List<JsonNode>> sourceAndTargetMap, CmmnModel cmmnModel) {

        for (String edgeId : edgeMap.keySet()) {

            JsonNode edgeNode = edgeMap.get(edgeId);
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

            String stencilId = CmmnJsonConverterUtil.getStencilId(sourceRefNode);
            String sourceId = null;
            if (STENCIL_ENTRY_CRITERION.equals(stencilId) || STENCIL_EXIT_CRITERION.equals(stencilId)) {
                sourceId = CmmnJsonConverterUtil.getElementId(sourceRefNode);
            } else {
                String elementId = CmmnJsonConverterUtil.getElementId(sourceRefNode);
                PlanItemDefinition planItemDefinition = cmmnModel.findPlanItemDefinition(elementId);
                PlanItem planItem = cmmnModel.findPlanItem(planItemDefinition.getPlanItemRef());
                sourceId = planItem.getId();
            }
            GraphicInfo sourceInfo = cmmnModel.getGraphicInfo(sourceId);

            stencilId = CmmnJsonConverterUtil.getStencilId(targetRefNode);
            String targetId = null;
            if (STENCIL_ENTRY_CRITERION.equals(stencilId) || STENCIL_EXIT_CRITERION.equals(stencilId)) {
                targetId = CmmnJsonConverterUtil.getElementId(targetRefNode);
            } else {
                String elementId = CmmnJsonConverterUtil.getElementId(targetRefNode);
                PlanItemDefinition planItemDefinition = cmmnModel.findPlanItemDefinition(elementId);
                PlanItem planItem = cmmnModel.findPlanItem(planItemDefinition.getPlanItemRef());
                targetId = planItem.getId();
            }
            GraphicInfo targetInfo = cmmnModel.getGraphicInfo(targetId);

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

            Line2D firstLine = new Line2D.Double(sourceRefLineX, sourceRefLineY,
                    nextPointInLineX, nextPointInLineY);

            String sourceRefStencilId = CmmnJsonConverterUtil.getStencilId(sourceRefNode);
            String targetRefStencilId = CmmnJsonConverterUtil.getStencilId(targetRefNode);

            List<GraphicInfo> graphicInfoList = new ArrayList<>();

            Area source2D = null;
            if (DI_CIRCLES.contains(sourceRefStencilId)) {
                source2D = createEllipse(sourceInfo, sourceDockersX, sourceDockersY);

            } else if (DI_RECTANGLES.contains(sourceRefStencilId)) {
                source2D = createRectangle(sourceInfo);

            } else if (DI_SENTRY.contains(sourceRefStencilId)) {
                source2D = createGateway(sourceInfo);
            }

            if (source2D != null) {
                Collection<Point2D> intersections = getIntersections(firstLine, source2D);
                if (intersections != null && intersections.size() > 0) {
                    Point2D intersection = intersections.iterator().next();
                    graphicInfoList.add(createGraphicInfo(intersection.getX(), intersection.getY()));
                } else {
                    graphicInfoList.add(createGraphicInfo(sourceRefLineX, sourceRefLineY));
                }
            }

            Line2D lastLine = null;

            if (dockersNode.size() > 2) {
                for (int i = 1; i < dockersNode.size() - 1; i++) {
                    double x = dockersNode.get(i).get(EDITOR_BOUNDS_X).asDouble();
                    double y = dockersNode.get(i).get(EDITOR_BOUNDS_Y).asDouble();
                    graphicInfoList.add(createGraphicInfo(x, y));
                }

                double startLastLineX = dockersNode.get(dockersNode.size() - 2).get(EDITOR_BOUNDS_X).asDouble();
                double startLastLineY = dockersNode.get(dockersNode.size() - 2).get(EDITOR_BOUNDS_Y).asDouble();

                double endLastLineX = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_X).asDouble();
                double endLastLineY = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_Y).asDouble();

                endLastLineX += targetInfo.getX();
                endLastLineY += targetInfo.getY();

                lastLine = new Line2D.Double(startLastLineX, startLastLineY, endLastLineX, endLastLineY);

            } else {
                lastLine = firstLine;
            }

            Area target2D = null;
            if (DI_CIRCLES.contains(targetRefStencilId)) {
                double targetDockersX = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_X).asDouble();
                double targetDockersY = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_Y).asDouble();

                target2D = createEllipse(targetInfo, targetDockersX, targetDockersY);

            } if (DI_RECTANGLES.contains(targetRefStencilId)) {
                target2D = createRectangle(targetInfo);

            } else if (DI_SENTRY.contains(targetRefStencilId)) {
                target2D = createGateway(targetInfo);
            }

            if (target2D != null) {
                Collection<Point2D> intersections = getIntersections(lastLine, target2D);
                if (intersections != null && intersections.size() > 0) {
                    Point2D intersection = intersections.iterator().next();
                    graphicInfoList.add(createGraphicInfo(intersection.getX(), intersection.getY()));
                } else {
                    graphicInfoList.add(createGraphicInfo(lastLine.getX2(), lastLine.getY2()));
                }
            }

            cmmnModel.addFlowGraphicInfoList(edgeId, graphicInfoList);
        }
    }


    protected Area createEllipse(GraphicInfo sourceInfo, double halfWidth, double halfHeight) {
        Area outerCircle = new Area(new Ellipse2D.Double(
                sourceInfo.getX(), sourceInfo.getY(), 2 * halfWidth, 2 * halfHeight
        ));
        Area innerCircle = new Area(new Ellipse2D.Double(
                sourceInfo.getX() + lineWidth, sourceInfo.getY() + lineWidth, 2 * (halfWidth - lineWidth), 2 * (halfHeight - lineWidth)
        ));
        outerCircle.subtract(innerCircle);
        return outerCircle;
    }

    protected Collection<java.awt.geom.Point2D> getIntersections(java.awt.geom.Line2D line, Area shape) {
        Area intersectionArea = new Area(getLineShape(line));
        intersectionArea.intersect(shape);
        if (!intersectionArea.isEmpty()) {
            Rectangle2D bounds2D = intersectionArea.getBounds2D();
            HashSet<java.awt.geom.Point2D> intersections = new HashSet<>(2);
            intersections.add(new java.awt.geom.Point2D.Double(bounds2D.getX(), bounds2D.getY()));
            return intersections;
        }
        return Collections.EMPTY_SET;
    }

    protected Shape getLineShape(java.awt.geom.Line2D line2D) {
        Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        line.moveTo(line2D.getX1(), line2D.getY1());
        line.lineTo(line2D.getX2(), line2D.getY2());
        line.lineTo(line2D.getX2() + lineWidth, line2D.getY2() + lineWidth);
        line.closePath();
        return line;
    }

    protected Area createRectangle(GraphicInfo graphicInfo) {
        Area outerRectangle = new Area(new Rectangle2D.Double(
                graphicInfo.getX(), graphicInfo.getY(),
                graphicInfo.getWidth(), graphicInfo.getHeight()
        ));
        Area innerRectangle = new Area(new Rectangle2D.Double(
                graphicInfo.getX() + lineWidth, graphicInfo.getY() + lineWidth,
                graphicInfo.getWidth() - 2 * lineWidth, graphicInfo.getHeight() - 2 * lineWidth
        ));
        outerRectangle.subtract(innerRectangle);
        return outerRectangle;
    }

    protected Area createGateway(GraphicInfo graphicInfo) {
        Area outerGatewayArea = new Area(
                createGatewayShape(graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.getHeight())
        );
        Area innerGatewayArea = new Area(
                createGatewayShape(graphicInfo.getX() + lineWidth, graphicInfo.getY() + lineWidth,
                        graphicInfo.getWidth() - 2 * lineWidth, graphicInfo.getHeight() - 2 * lineWidth)
        );
        outerGatewayArea.subtract(innerGatewayArea);
        return outerGatewayArea;
    }

    private Path2D.Double createGatewayShape(double x, double y, double width, double height) {
        double middleX = x + (width / 2);
        double middleY = y + (height / 2);

        Path2D.Double gatewayShape = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        gatewayShape.moveTo(x, middleY);
        gatewayShape.lineTo(middleX, y);
        gatewayShape.lineTo(x + width, middleY);
        gatewayShape.lineTo(middleX, y + height);
        gatewayShape.closePath();
        return gatewayShape;
    }

    protected GraphicInfo createGraphicInfo(double x, double y) {
        GraphicInfo graphicInfo = new GraphicInfo();
        graphicInfo.setX(x);
        graphicInfo.setY(y);
        return graphicInfo;
    }

    public static class CmmnModelIdHelper {

        protected int planItemIndex = 0;
        protected int criterionId = 0;
        protected int sentryIndex = 0;
        protected int sentryOnPartIndex = 0;

        public int nextPlanItemId() {
            planItemIndex++;
            return planItemIndex;
        }

        public int nextCriterionId() {
            criterionId++;
            return criterionId;
        }

        public int nextSentryId() {
            sentryIndex++;
            return sentryIndex;
        }

        public int nextSentryOnPartId() {
            sentryOnPartIndex++;
            return sentryOnPartIndex;
        }

    }
}
