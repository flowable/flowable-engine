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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.Stage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class CriterionJsonConverter extends BaseCmmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap) {
        convertersToCmmnMap.put(STENCIL_ENTRY_CRITERION, CriterionJsonConverter.class);
        convertersToCmmnMap.put(STENCIL_EXIT_CRITERION, CriterionJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(Criterion.class, CriterionJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        Criterion criterion = (Criterion) baseElement;
        if (criterion.isEntryCriterion()) {
            return STENCIL_ENTRY_CRITERION;
        } else if (criterion.isExitCriterion()) {
            return STENCIL_EXIT_CRITERION;
        }
        return STENCIL_ENTRY_CRITERION;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement,
            CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {

        Criterion criterion = (Criterion) baseElement;
        ArrayNode dockersArrayNode = objectMapper.createArrayNode();
        ObjectNode dockNode = objectMapper.createObjectNode();
        GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(criterion.getId());

        GraphicInfo parentGraphicInfo = null;
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        if (criterion.getAttachedToRefId() != null) {
            if (criterion.getAttachedToRefId().equals(planModel.getId())) {
                parentGraphicInfo = cmmnModel.getGraphicInfo(planModel.getId());
            } else {
                PlanItem parentPlanItem = cmmnModel.findPlanItem(criterion.getAttachedToRefId());
                parentGraphicInfo = cmmnModel.getGraphicInfo(parentPlanItem.getId());
            }
        } else {
            parentGraphicInfo = cmmnModel.getGraphicInfo(planModel.getId());
        }

        BigDecimal parentX = new BigDecimal(parentGraphicInfo.getX());
        BigDecimal parentY = new BigDecimal(parentGraphicInfo.getY());

        BigDecimal criterionX = new BigDecimal(graphicInfo.getX());
        BigDecimal criterionWidth = new BigDecimal(graphicInfo.getWidth());
        BigDecimal criterionXMid = criterionWidth.divide(new BigDecimal(2));

        BigDecimal criterionY = new BigDecimal(graphicInfo.getY());
        BigDecimal criterionHeight = new BigDecimal(graphicInfo.getHeight());
        BigDecimal criterionYMid = criterionHeight.divide(new BigDecimal(2));

        BigDecimal xBound = criterionX.add(criterionXMid).subtract(parentX).setScale(0, RoundingMode.HALF_UP);
        BigDecimal yBound = criterionY.add(criterionYMid).subtract(parentY).setScale(0,RoundingMode.HALF_UP);

        dockNode.put(EDITOR_BOUNDS_X, xBound);
        dockNode.put(EDITOR_BOUNDS_Y, yBound);
        dockersArrayNode.add(dockNode);
        elementNode.set("dockers", dockersArrayNode);
        elementNode.set("outgoing", getOutgoingArrayNodes(criterion.getId(), cmmnModel));

        // set properties
        putProperty(propertiesNode, PROPERTY_NAME, criterion.getSentry().getName());
        putProperty(propertiesNode, PROPERTY_DOCUMENTATION, criterion.getSentry().getDocumentation());

        if (criterion.getSentry() != null && criterion.getSentry().getSentryIfPart() != null) {
            putProperty(propertiesNode, PROPERTY_IF_PART_CONDITION, criterion.getSentry().getSentryIfPart().getCondition());
        }

        if (criterion.getSentry() != null && criterion.getSentry().getTriggerMode() != null) {
            putProperty(propertiesNode, PROPERTY_TRIGGER_MODE, criterion.getSentry().getTriggerMode());
        }
    }

    protected JsonNode getOutgoingArrayNodes(String id, CmmnModel cmmnModel) {
        ArrayNode outgoingArrayNode = objectMapper.createArrayNode();
        for (Association association : cmmnModel.getAssociations()) {
            if (id.equals(association.getSourceRef())) {
                outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getId()));
            }
        }
        return outgoingArrayNode;
    }

    protected void putProperty(ObjectNode propertiesNode, String propertyName, String propertyValue) {
        if (StringUtils.isNotEmpty(propertyValue)) {
            propertiesNode.put(propertyName, propertyValue);
        }
    }

    @Override
    protected CaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper) {

        Criterion criterion = new Criterion();
        String id = CmmnJsonConverterUtil.getElementId(elementNode);
        if (StringUtils.isBlank(id)) {
            id = "criterion_" + cmmnModelIdHelper.nextCriterionId();
        }
        criterion.setId(id);
        criterion.setTechnicalId(CmmnJsonConverterUtil.getShapeId(elementNode));
        String stencilId = CmmnJsonConverterUtil.getStencilId(elementNode);
        if (STENCIL_ENTRY_CRITERION.equals(stencilId)) {
            criterion.setEntryCriterion(true);

        } else if (STENCIL_EXIT_CRITERION.equals(stencilId)) {
            criterion.setExitCriterion(true);
        }

        String attachedRefId = lookForAttachedRef(elementNode.get(EDITOR_SHAPE_ID).asText(), 
                elementNode, modelNode.get(EDITOR_CHILD_SHAPES));
        if (attachedRefId == null && criterion.isExitCriterion() && parentElement instanceof Stage) {
            // exit sentry is on parent container, plan item model sentries are handled separately
            Stage parentStage = (Stage) parentElement;
            if (!parentStage.isPlanModel()) {
                criterion.setAttachedToRefId(parentStage.getId());
            }

        } else {
            criterion.setAttachedToRefId(attachedRefId);
        }

        if (criterion.getAttachedToRefId() != null) {
            String criterionId = CmmnJsonConverterUtil.getElementId(elementNode);
            cmmnModel.addCriterion(criterionId, criterion);
            cmmnModel.addCriterionTechnicalId(criterion.getTechnicalId(), criterionId);
        }

        createSentry(elementNode, criterion, cmmnModelIdHelper);

        return criterion;
    }

    protected void createSentry(JsonNode elementNode, Criterion criterion, CmmnModelIdHelper cmmnModelIdHelper) {
        // Associate a sentry with the criterion.
        // The onparts will be added later, in the postprocessing.
        Sentry sentry = new Sentry();
        sentry.setId("sentry" + cmmnModelIdHelper.nextSentryId());
        sentry.setName(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, elementNode));
        sentry.setDocumentation(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_DOCUMENTATION, elementNode));
        sentry.setTriggerMode(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_TRIGGER_MODE, elementNode));

        String ifPartCondition = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_IF_PART_CONDITION, elementNode);
        if (StringUtils.isNotBlank(ifPartCondition)) {
            SentryIfPart sentryIfPart = new SentryIfPart();
            sentryIfPart.setCondition(ifPartCondition);
            sentry.setSentryIfPart(sentryIfPart);
        }

        criterion.setSentryRef(sentry.getId());
        criterion.setSentry(sentry);
    }

    protected String lookForAttachedRef(String criterionId, JsonNode elementNode, JsonNode childShapesNode) {
        String attachedRefId = null;

        if (childShapesNode != null) {

            for (JsonNode childNode : childShapesNode) {
                ArrayNode outgoingNode = (ArrayNode) childNode.get("outgoing");
                if (outgoingNode != null && outgoingNode.size() > 0) {
                    for (JsonNode outgoingChildNode : outgoingNode) {
                        JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                        if (resourceNode != null && criterionId.equals(resourceNode.asText())) {
                            attachedRefId = CmmnJsonConverterUtil.getElementId(childNode);
                            break;
                        }
                    }

                    if (attachedRefId != null) {
                        break;
                    }
                }

                attachedRefId = lookForAttachedRef(criterionId, null, childNode.get(EDITOR_CHILD_SHAPES));

                if (attachedRefId != null) {
                    break;
                }
            }
        }

        return attachedRefId;
    }
}
