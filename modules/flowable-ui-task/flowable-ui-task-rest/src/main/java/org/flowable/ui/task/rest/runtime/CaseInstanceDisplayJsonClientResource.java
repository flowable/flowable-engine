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
package org.flowable.ui.task.rest.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
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
import org.flowable.idm.api.User;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.common.service.exception.NotPermittedException;
import org.flowable.ui.task.service.editor.mapper.InfoMapper;
import org.flowable.ui.task.service.runtime.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class CaseInstanceDisplayJsonClientResource {

    @Autowired
    protected CmmnRepositoryService cmmnRepositoryService;

    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    protected CmmnHistoryService cmmnHistoryService;

    @Autowired
    protected PermissionService permissionService;

    protected ObjectMapper objectMapper = new ObjectMapper();
    protected List<String> eventElementTypes = new ArrayList<>();
    protected Map<String, InfoMapper> propertyMappers = new HashMap<>();

    public CaseInstanceDisplayJsonClientResource() {

    }

    @GetMapping(value = "/rest/case-instances/{caseInstanceId}/model-json", produces = "application/json")
    public JsonNode getModelJSON(@PathVariable String caseInstanceId) {

        User currentUser = SecurityUtils.getCurrentUserObject();
        if (!permissionService.hasReadPermissionOnCase(currentUser, caseInstanceId)) {
            throw new NotPermittedException();
        }

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();
        if (caseInstance == null) {
            throw new BadRequestException("No case instance found with id " + caseInstanceId);
        }

        CmmnModel pojoModel = cmmnRepositoryService.getCmmnModel(caseInstance.getCaseDefinitionId());

        if (pojoModel == null || pojoModel.getLocationMap().isEmpty()) {
            throw new InternalServerErrorException("Case definition could not be found with id " + caseInstance.getCaseDefinitionId());
        }

        // Fetch case instance plan items
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).includeEnded().list();

        Set<String> completedPlanItemInstances = new HashSet<>();
        Set<String> activePlanItemInstances = new HashSet<>();
        Set<String> availablePlanItemInstances = new HashSet<>();
        if (CollectionUtils.isNotEmpty(planItemInstances)) {
            for (PlanItemInstance planItemInstance : planItemInstances) {
                if (planItemInstance.getCompletedTime() != null || planItemInstance.getTerminatedTime() != null || planItemInstance.getOccurredTime() != null) {
                    completedPlanItemInstances.add(planItemInstance.getPlanItemDefinitionId());

                } else if (PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
                    activePlanItemInstances.add(planItemInstance.getPlanItemDefinitionId());

                } else if (PlanItemInstanceState.AVAILABLE.equals(planItemInstance.getState())) {
                    availablePlanItemInstances.add(planItemInstance.getPlanItemDefinitionId());
                }
            }
        }

        ObjectNode displayNode = processCaseElements(pojoModel, completedPlanItemInstances, activePlanItemInstances, availablePlanItemInstances);

        ArrayNode completedActivities = displayNode.putArray("completedActivities");
        for (String completed : completedPlanItemInstances) {
            completedActivities.add(completed);
        }

        ArrayNode currentActivities = displayNode.putArray("currentActivities");
        for (String current : activePlanItemInstances) {
            currentActivities.add(current);
        }

        ArrayNode availableActivities = displayNode.putArray("availableActivities");
        for (String available : availablePlanItemInstances) {
            availableActivities.add(available);
        }

        return displayNode;
    }

    @GetMapping(value = "/rest/case-definitions/{caseDefinitionId}/model-json", produces = "application/json")
    public JsonNode getModelJSONForCaseDefinition(@PathVariable String caseDefinitionId) {

        CmmnModel pojoModel = cmmnRepositoryService.getCmmnModel(caseDefinitionId);

        if (pojoModel == null || pojoModel.getLocationMap().isEmpty()) {
            throw new InternalServerErrorException("Case definition could not be found with id " + caseDefinitionId);
        }

        return processCaseElements(pojoModel, null, null, null);
    }

    @GetMapping(value = "/rest/case-instances/history/{caseInstanceId}/model-json", produces = "application/json")
    public JsonNode getModelHistoryJSON(@PathVariable String caseInstanceId) {

        User currentUser = SecurityUtils.getCurrentUserObject();
        if (!permissionService.hasReadPermissionOnCase(currentUser, caseInstanceId)) {
            throw new NotPermittedException();
        }

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();
        if (caseInstance == null) {
            throw new BadRequestException("No case instance found with id " + caseInstanceId);
        }

        CmmnModel pojoModel = cmmnRepositoryService.getCmmnModel(caseInstance.getCaseDefinitionId());

        if (pojoModel == null || pojoModel.getLocationMap().isEmpty()) {
            throw new InternalServerErrorException("Case definition could not be found with id " + caseInstance.getCaseDefinitionId());
        }

        // Fetch case instance plan items
        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstanceId).list();

        Set<String> completedPlanItemInstances = new HashSet<>();
        Set<String> activePlanItemInstances = new HashSet<>();
        Set<String> availablePlanItemInstances = new HashSet<>();
        if (CollectionUtils.isNotEmpty(planItemInstances)) {
            for (HistoricPlanItemInstance planItemInstance : planItemInstances) {
                if (planItemInstance.getCompletedTime() != null || planItemInstance.getTerminatedTime() != null || planItemInstance.getOccurredTime() != null) {
                    completedPlanItemInstances.add(planItemInstance.getPlanItemDefinitionId());

                } else if (PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
                    activePlanItemInstances.add(planItemInstance.getPlanItemDefinitionId());

                } else if (PlanItemInstanceState.AVAILABLE.equals(planItemInstance.getState())) {
                    availablePlanItemInstances.add(planItemInstance.getPlanItemDefinitionId());
                }
            }
        }

        ObjectNode displayNode = processCaseElements(pojoModel, completedPlanItemInstances, activePlanItemInstances, availablePlanItemInstances);
        return displayNode;
    }

    protected ObjectNode processCaseElements(CmmnModel pojoModel, Set<String> completedElements, Set<String> activeElements, Set<String> availableElements) {
        ObjectNode displayNode = objectMapper.createObjectNode();
        GraphicInfo diagramInfo = new GraphicInfo();

        ArrayNode elementArray = objectMapper.createArrayNode();
        ArrayNode associationArray = objectMapper.createArrayNode();

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

            processElements(caseObject.getPlanModel().getPlanItems(), pojoModel, elementArray, associationArray,
                            completedElements, activeElements, availableElements, diagramInfo);
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

                associationArray.add(elementNode);
            }
        }

        displayNode.set("elements", elementArray);
        displayNode.set("flows", associationArray);

        displayNode.put("diagramBeginX", diagramInfo.getX());
        displayNode.put("diagramBeginY", diagramInfo.getY());
        displayNode.put("diagramWidth", diagramInfo.getWidth());
        displayNode.put("diagramHeight", diagramInfo.getHeight());
        return displayNode;
    }

    protected void processElements(List<PlanItem> planItemList, CmmnModel model, ArrayNode elementArray, ArrayNode flowArray,
                    Set<String> completedElements, Set<String> activeElements, Set<String> availableElements, GraphicInfo diagramInfo) {

        for (PlanItem planItem : planItemList) {
            ObjectNode elementNode = objectMapper.createObjectNode();
            elementNode.put("id", planItem.getId());
            elementNode.put("name", planItem.getName());

            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
            String className = planItemDefinition.getClass().getSimpleName();
            elementNode.put("type", className);

            if (completedElements != null) {
                elementNode.put("completed", completedElements.contains(planItemDefinition.getId()));
            }

            if (activeElements != null) {
                elementNode.put("current", activeElements.contains(planItemDefinition.getId()));
            }

            if (availableElements != null) {
                elementNode.put("available", availableElements.contains(planItemDefinition.getId()));
            }

            GraphicInfo graphicInfo = model.getGraphicInfo(planItem.getId());
            if (graphicInfo != null) {
                fillGraphicInfo(elementNode, graphicInfo, true);
                fillDiagramInfo(graphicInfo, diagramInfo);
            }

            if (planItemDefinition instanceof ServiceTask) {
                ServiceTask serviceTask = (ServiceTask) planItemDefinition;
                if (ServiceTask.MAIL_TASK.equals(serviceTask.getType())) {
                    elementNode.put("taskType", "mail");

                } else if (HttpServiceTask.HTTP_TASK.equals(serviceTask.getType())) {
                    elementNode.put("taskType", "http");
                }
            }

            elementArray.add(elementNode);

            processCriteria(planItem.getEntryCriteria(), "EntryCriterion", model, elementArray);
            processCriteria(planItem.getExitCriteria(), "ExitCriterion", model, elementArray);

            if (planItemDefinition instanceof Stage) {
                Stage stage = (Stage) planItemDefinition;

                processElements(stage.getPlanItems(), model, elementArray, flowArray, completedElements,
                                activeElements, availableElements, diagramInfo);
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
