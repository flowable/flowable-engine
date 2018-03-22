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

import java.util.List;
import java.util.Map;

import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class AssociationJsonConverter extends BaseCmmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap) {
        convertersToCmmnMap.put(STENCIL_ASSOCIATION, AssociationJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(Association.class, AssociationJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_ASSOCIATION;
    }

    public void convertToJson(BaseElement baseElement, CmmnModel model, ArrayNode shapesArrayNode) {

        Association association = (Association) baseElement;
        ObjectNode flowNode = CmmnJsonConverterUtil.createChildShape(association.getId(), STENCIL_ASSOCIATION, 172, 212, 128, 212);
        ArrayNode dockersArrayNode = objectMapper.createArrayNode();
        ObjectNode dockNode = objectMapper.createObjectNode();
        dockNode.put(EDITOR_BOUNDS_X, model.getGraphicInfo(association.getSourceRef()).getWidth() / 2.0);
        dockNode.put(EDITOR_BOUNDS_Y, model.getGraphicInfo(association.getSourceRef()).getHeight() / 2.0);
        dockersArrayNode.add(dockNode);

        List<GraphicInfo> graphicInfoList = model.getFlowLocationGraphicInfo(association.getId());
        if (graphicInfoList.size() > 2) {
            for (int i = 1; i < graphicInfoList.size() - 1; i++) {
                GraphicInfo graphicInfo = graphicInfoList.get(i);
                dockNode = objectMapper.createObjectNode();
                dockNode.put(EDITOR_BOUNDS_X, graphicInfo.getX());
                dockNode.put(EDITOR_BOUNDS_Y, graphicInfo.getY());
                dockersArrayNode.add(dockNode);
            }
        }

        PlanItem planItem = model.findPlanItem(association.getTargetRef());
        if (planItem == null) {
            Criterion criterion = model.getCriterion(association.getTargetRef());
            if (criterion == null) {
                // Invalid reference, ignoring
                return;
            }
        }

        GraphicInfo targetGraphicInfo = model.getGraphicInfo(association.getTargetRef());
        GraphicInfo flowGraphicInfo = graphicInfoList.get(graphicInfoList.size() - 1);

        double diffTopY = Math.abs(flowGraphicInfo.getY() - targetGraphicInfo.getY());
        double diffRightX = Math.abs(flowGraphicInfo.getX() - (targetGraphicInfo.getX() + targetGraphicInfo.getWidth()));
        double diffBottomY = Math.abs(flowGraphicInfo.getY() - (targetGraphicInfo.getY() + targetGraphicInfo.getHeight()));

        dockNode = objectMapper.createObjectNode();
        if (diffTopY < 5) {
            dockNode.put(EDITOR_BOUNDS_X, targetGraphicInfo.getWidth() / 2.0);
            dockNode.put(EDITOR_BOUNDS_Y, 0.0);

        } else if (diffRightX < 5) {
            dockNode.put(EDITOR_BOUNDS_X, targetGraphicInfo.getWidth());
            dockNode.put(EDITOR_BOUNDS_Y, targetGraphicInfo.getHeight() / 2.0);

        } else if (diffBottomY < 5) {
            dockNode.put(EDITOR_BOUNDS_X, targetGraphicInfo.getWidth() / 2.0);
            dockNode.put(EDITOR_BOUNDS_Y, targetGraphicInfo.getHeight());

        } else {
            dockNode.put(EDITOR_BOUNDS_X, 0.0);
            dockNode.put(EDITOR_BOUNDS_Y, targetGraphicInfo.getHeight() / 2.0);
        }
        dockersArrayNode.add(dockNode);
        flowNode.set("dockers", dockersArrayNode);
        ArrayNode outgoingArrayNode = objectMapper.createArrayNode();
        outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getTargetRef()));
        flowNode.set("outgoing", outgoingArrayNode);
        flowNode.set("target", CmmnJsonConverterUtil.createResourceNode(association.getTargetRef()));

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.put(PROPERTY_OVERRIDE_ID, association.getId());
        propertiesNode.put(PROPERTY_TRANSITION_EVENT, association.getTransitionEvent());

        flowNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);

        shapesArrayNode.add(flowNode);
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement, CmmnModel cmmnModel) {
        // nothing to do
    }

    @Override
    protected Association convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {

        Association association = new Association();

        association.setId(CmmnJsonConverterUtil.getElementId(elementNode));
        association.setTransitionEvent(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_TRANSITION_EVENT, elementNode));
        String sourceRef = CmmnJsonConverterUtil.lookForSourceRef(elementNode.get(EDITOR_SHAPE_ID).asText(), modelNode.get(EDITOR_CHILD_SHAPES));

        if (sourceRef != null) {
            association.setSourceRef(sourceRef);
            String targetId = elementNode.get("target").get(EDITOR_SHAPE_ID).asText();
            association.setTargetRef(CmmnJsonConverterUtil.getElementId(shapeMap.get(targetId)));
        }

        return association;
    }
}
