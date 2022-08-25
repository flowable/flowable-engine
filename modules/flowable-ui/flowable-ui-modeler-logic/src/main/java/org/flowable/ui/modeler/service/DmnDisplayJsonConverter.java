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
package org.flowable.ui.modeler.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.editor.converter.DmnJsonConverter;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.GraphicInfo;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class DmnDisplayJsonConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnDisplayJsonConverter.class);

    protected DmnJsonConverter dmnJsonConverter = new DmnJsonConverter();
    protected ObjectMapper objectMapper = new ObjectMapper();

    public void processDefinitionElements(AbstractModel decisionServiceModel, ObjectNode displayNode, GraphicInfo diagramInfo) {
        DmnDefinition pojoModel = null;
        if (!StringUtils.isEmpty(decisionServiceModel.getModelEditorJson())) {
            try {
                JsonNode modelNode = objectMapper.readTree(decisionServiceModel.getModelEditorJson());
                pojoModel = dmnJsonConverter.convertToDmn(modelNode);
            } catch (Exception e) {
                LOGGER.error("Error transforming json to pojo {}", decisionServiceModel.getId(), e);
            }
        }

        if (pojoModel == null || pojoModel.getLocationMap().isEmpty()) {
            return;
        }

        ArrayNode elementArray = objectMapper.createArrayNode();
        ArrayNode flowArray = objectMapper.createArrayNode();

        diagramInfo.setX(9999);
        diagramInfo.setY(1000);

        for (DecisionService decisionService : pojoModel.getDecisionServices()) {
            ObjectNode elementNode = objectMapper.createObjectNode();
            elementNode.put("id", decisionService.getId());
            elementNode.put("name", decisionService.getName());
            elementNode.put("type", "DecisionService");

            GraphicInfo graphicInfo = pojoModel.getGraphicInfo(decisionService.getId());
            if (graphicInfo != null) {
                fillGraphicInfo(elementNode, graphicInfo, true);
                fillDiagramInfo(graphicInfo, diagramInfo);
            }

            elementArray.add(elementNode);

            processDecisionServiceDivider(decisionService.getId(), pojoModel, elementNode);

            // add information requirements
            // should associations be used for this?
            Map<String, List<GraphicInfo>> flowInfo = pojoModel.getFlowLocationMap();
            for (Map.Entry<String, List<GraphicInfo>> entry : flowInfo.entrySet()) {
                if (CollectionUtils.isNotEmpty(entry.getValue())) {
                    ObjectNode flowNode = objectMapper.createObjectNode();
                    flowNode.put("id", entry.getKey());
                    flowNode.put("type", "InformationRequirement");
                    ArrayNode waypointArray = objectMapper.createArrayNode();
                    for (GraphicInfo flowGraphicInfo : entry.getValue()) {
                        ObjectNode pointNode = objectMapper.createObjectNode();
                        fillGraphicInfo(pointNode, flowGraphicInfo, false);
                        waypointArray.add(pointNode);
                        //                        fillDiagramInfo(graphicInfo, diagramInfo);
                    }
                    flowNode.set("waypoints", waypointArray);

                    flowArray.add(flowNode);
                }
            }
        }

        processDecisions(pojoModel.getDecisions(), pojoModel, elementArray, diagramInfo);

        displayNode.set("elements", elementArray);
        displayNode.set("flows", flowArray);

        displayNode.put("diagramBeginX", diagramInfo.getX());
        displayNode.put("diagramBeginY", diagramInfo.getY());
        displayNode.put("diagramWidth", diagramInfo.getWidth());
        displayNode.put("diagramHeight", diagramInfo.getHeight());
    }

    protected void processDecisions(List<Decision> decisions, DmnDefinition model, ArrayNode elementArray, GraphicInfo diagramInfo) {

        for (Decision decision : decisions) {
            ObjectNode elementNode = objectMapper.createObjectNode();
            elementNode.put("id", decision.getId());
            elementNode.put("name", decision.getName());
            elementNode.put("type", "Decision");

            GraphicInfo graphicInfo = model.getGraphicInfo(decision.getId());
            if (graphicInfo != null) {
                fillGraphicInfo(elementNode, graphicInfo, true);
                fillDiagramInfo(graphicInfo, diagramInfo);
            }

            elementArray.add(elementNode);
        }
    }

    protected void processDecisionServiceDivider(String decisionServiceId, DmnDefinition dmnDefinition, ObjectNode decisionServiceNode) {
        ObjectNode dividerNode = objectMapper.createObjectNode();
        dividerNode.put("type", "DecisionServiceDivider");
        decisionServiceNode.set("divider", dividerNode);

        fillWaypoints(decisionServiceId, dmnDefinition, dividerNode);
    }

    protected void fillGraphicInfo(ObjectNode elementNode, GraphicInfo graphicInfo, boolean includeWidthAndHeight) {
        commonFillGraphicInfo(elementNode, graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.getHeight(), includeWidthAndHeight);
    }

    protected void commonFillGraphicInfo(ObjectNode elementNode, double x, double y, double width, double height, boolean includeWidthAndHeight) {
        elementNode.put("x", x);
        elementNode.put("y", y);
        if (includeWidthAndHeight) {
            elementNode.put("width", width);
            elementNode.put("height", height);
        }
    }

    protected void fillDiagramInfo(GraphicInfo graphicInfo, GraphicInfo diagramInfo) {
        double rightX = graphicInfo.getX() + graphicInfo.getWidth();
        double bottomY = graphicInfo.getY() + graphicInfo.getHeight();
        double middleX = graphicInfo.getX() + (graphicInfo.getWidth() / 2);
        if (middleX < diagramInfo.getX()) {
            diagramInfo.setX(middleX);
        }
        if (graphicInfo.getY() < diagramInfo.getY()) {
            diagramInfo.setY(graphicInfo.getY());
        }
        if (rightX > diagramInfo.getWidth()) {
            diagramInfo.setWidth(rightX);
        }
        if (bottomY > diagramInfo.getHeight()) {
            diagramInfo.setHeight(bottomY);
        }
    }

    protected void fillWaypoints(String id, DmnDefinition model, ObjectNode elementNode) {
        List<GraphicInfo> flowInfo = model.getDecisionServiceDividerGraphicInfo(id);
        ArrayNode waypointArray = objectMapper.createArrayNode();
        for (GraphicInfo graphicInfo : flowInfo) {
            ObjectNode pointNode = objectMapper.createObjectNode();
            fillGraphicInfo(pointNode, graphicInfo, false);
            waypointArray.add(pointNode);
        }
        elementNode.set("waypoints", waypointArray);
    }
}
