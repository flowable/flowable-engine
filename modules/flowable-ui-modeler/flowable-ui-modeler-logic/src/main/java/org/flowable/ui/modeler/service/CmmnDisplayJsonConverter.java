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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.service.mapper.InfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class CmmnDisplayJsonConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnDisplayJsonConverter.class);

    protected CmmnJsonConverter cmmnJsonConverter = new CmmnJsonConverter();

    protected ObjectMapper objectMapper = new ObjectMapper();
    protected List<String> eventElementTypes = new ArrayList<>();
    protected Map<String, InfoMapper> propertyMappers = new HashMap<>();

    public void processCaseElements(AbstractModel caseModel, ObjectNode displayNode, GraphicInfo diagramInfo) {
        CmmnModel pojoModel = null;
        if (!StringUtils.isEmpty(caseModel.getModelEditorJson())) {
            try {
                JsonNode modelNode = objectMapper.readTree(caseModel.getModelEditorJson());
                pojoModel = cmmnJsonConverter.convertToCmmnModel(modelNode);
            } catch (Exception e) {
                LOGGER.error("Error transforming json to pojo {}", caseModel.getId(), e);
            }
        }

        if (pojoModel == null || pojoModel.getLocationMap().isEmpty())
            return;

        ArrayNode elementArray = objectMapper.createArrayNode();
        ArrayNode flowArray = objectMapper.createArrayNode();

        // in initialize with fake x and y to make sure the minimal values are set
        diagramInfo.setX(9999);
        diagramInfo.setY(1000);

        for (Case caseObject : pojoModel.getCases()) {
            ObjectNode elementNode = objectMapper.createObjectNode();
            elementNode.put("id", caseObject.getPlanModel().getId());
            elementNode.put("name", caseObject.getPlanModel().getName());

            GraphicInfo graphicInfo = pojoModel.getGraphicInfo(caseObject.getPlanModel().getId());
            if (graphicInfo != null) {
                fillGraphicInfo(elementNode, graphicInfo, true);
                fillDiagramInfo(graphicInfo, diagramInfo);
            }

            elementNode.put("type", "PlanModel");
            elementArray.add(elementNode);

            processCriteria(caseObject.getPlanModel().getExitCriteria(), "ExitCriterion", pojoModel, elementArray);

            processElements(caseObject.getPlanModel().getPlanItems(), pojoModel, elementArray, flowArray, diagramInfo);
        }

        for (Association association : pojoModel.getAssociations()) {
            ObjectNode elementNode = objectMapper.createObjectNode();
            elementNode.put("id", association.getId());
            elementNode.put("type", "Association");
            elementNode.put("sourceRef", association.getSourceRef());
            elementNode.put("targetRef", association.getTargetRef());
            List<GraphicInfo> flowInfo = pojoModel.getFlowLocationGraphicInfo(association.getId());
            if (CollectionUtils.isNotEmpty(flowInfo)) {
                ArrayNode waypointArray = objectMapper.createArrayNode();
                for (GraphicInfo graphicInfo : flowInfo) {
                    ObjectNode pointNode = objectMapper.createObjectNode();
                    fillGraphicInfo(pointNode, graphicInfo, false);
                    waypointArray.add(pointNode);
                    fillDiagramInfo(graphicInfo, diagramInfo);
                }
                elementNode.set("waypoints", waypointArray);

                flowArray.add(elementNode);
            }
        }

        displayNode.set("elements", elementArray);
        displayNode.set("flows", flowArray);

        displayNode.put("diagramBeginX", diagramInfo.getX());
        displayNode.put("diagramBeginY", diagramInfo.getY());
        displayNode.put("diagramWidth", diagramInfo.getWidth());
        displayNode.put("diagramHeight", diagramInfo.getHeight());
    }

    protected void processElements(List<PlanItem> planItemList, CmmnModel model, ArrayNode elementArray, ArrayNode flowArray, GraphicInfo diagramInfo) {

        for (PlanItem planItem : planItemList) {
            ObjectNode elementNode = objectMapper.createObjectNode();
            elementNode.put("id", planItem.getId());
            elementNode.put("name", planItem.getName());

            GraphicInfo graphicInfo = model.getGraphicInfo(planItem.getId());
            if (graphicInfo != null) {
                fillGraphicInfo(elementNode, graphicInfo, true);
                fillDiagramInfo(graphicInfo, diagramInfo);
            }

            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
            String className = planItemDefinition.getClass().getSimpleName();
            elementNode.put("type", className);

            if (planItemDefinition instanceof ServiceTask) {
                ServiceTask serviceTask = (ServiceTask) planItemDefinition;
                if (HttpServiceTask.HTTP_TASK.equals(serviceTask.getType())) {
                    elementNode.put("taskType", "http");
                } 
            }

            elementArray.add(elementNode);

            processCriteria(planItem.getEntryCriteria(), "EntryCriterion", model, elementArray);
            processCriteria(planItem.getExitCriteria(), "ExitCriterion", model, elementArray);

            if (planItemDefinition instanceof Stage) {
                Stage stage = (Stage) planItemDefinition;

                processElements(stage.getPlanItems(), model, elementArray, flowArray, diagramInfo);
            }
        }
    }

    protected void processCriteria(List<Criterion> criteria, String type, CmmnModel model, ArrayNode elementArray) {
        for (Criterion criterion : criteria) {
            ObjectNode criterionNode = objectMapper.createObjectNode();
            criterionNode.put("id", criterion.getId());
            criterionNode.put("name", criterion.getName());
            criterionNode.put("type", type);

            GraphicInfo criterionGraphicInfo = model.getGraphicInfo(criterion.getId());
            if (criterionGraphicInfo != null) {
                fillGraphicInfo(criterionNode, criterionGraphicInfo, true);
            }

            elementArray.add(criterionNode);
        }
    }

    protected void fillWaypoints(String id, CmmnModel model, ObjectNode elementNode, GraphicInfo diagramInfo) {
        List<GraphicInfo> flowInfo = model.getFlowLocationGraphicInfo(id);
        ArrayNode waypointArray = objectMapper.createArrayNode();
        for (GraphicInfo graphicInfo : flowInfo) {
            ObjectNode pointNode = objectMapper.createObjectNode();
            fillGraphicInfo(pointNode, graphicInfo, false);
            waypointArray.add(pointNode);
            fillDiagramInfo(graphicInfo, diagramInfo);
        }
        elementNode.set("waypoints", waypointArray);
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
}
