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
package org.flowable.dmn.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class DmnDefinition extends NamedElement {

    protected String expressionLanguage;
    protected String typeLanguage;
    protected String namespace;
    protected List<InputData> inputData = new ArrayList<>();
    protected List<ItemDefinition> itemDefinitions = new ArrayList<>();
    protected List<Decision> decisions = new ArrayList<>();
    protected List<DecisionService> decisionServices = new ArrayList<>();
    protected Map<String, DmnDiDiagram> diDiagrams = new LinkedHashMap<>();
    protected Map<String, Map<String, GraphicInfo>> locationMap = new LinkedHashMap<>();
    protected Map<String, GraphicInfo> labelLocationMap = new LinkedHashMap<>();
    protected Map<String, Map<String, List<GraphicInfo>>> flowLocationMap = new LinkedHashMap<>();
    protected Map<String, Map<String, List<GraphicInfo>>> decisionServiceDividerLocationMap = new LinkedHashMap<>();

    public String getExpressionLanguage() {
        return expressionLanguage;
    }

    public void setExpressionLanguage(String expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
    }

    public String getTypeLanguage() {
        return typeLanguage;
    }

    public void setTypeLanguage(String typeLanguage) {
        this.typeLanguage = typeLanguage;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<InputData> getInputData() {
        return inputData;
    }
    public void setInputData(List<InputData> inputData) {
        this.inputData = inputData;
    }
    public void addInputData(InputData inputData) {
        this.inputData.add(inputData);
    }
    public void setItemDefinitions(List<ItemDefinition> itemDefinitions) {
        this.itemDefinitions = itemDefinitions;
    }
    public List<ItemDefinition> getItemDefinitions() {
        return itemDefinitions;
    }

    public void addItemDefinition(ItemDefinition itemDefinition) {
        this.itemDefinitions.add(itemDefinition);
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public void addDecision(Decision decision) {
        this.decisions.add(decision);
    }

    public Decision getDecisionById(String id) {
        for (Decision decision : decisions) {
            if (id.equals(decision.getId())) {
                return decision;
            }
        }
        return null;
    }

    public List<DecisionService> getDecisionServices() {
        return decisionServices;
    }

    public void addDecisionService(DecisionService decisionService) {
        this.decisionServices.add(decisionService);
    }

    public DecisionService getDecisionServiceById(String id) {
        for (DecisionService decisionService : decisionServices) {
            if (id.equals((decisionService.getId()))) {
                return decisionService;
            }
        }
        return null;
    }

    public DmnDiDiagram getDiDiagram(String diagramId) {
        return diDiagrams.get(diagramId);
    }

    public Map<String, DmnDiDiagram> getDiDiagrams() {
        return diDiagrams;
    }
    public void addDiDiagram(DmnDiDiagram diDiagram) {
        this.diDiagrams.put(diDiagram.getId(), diDiagram);
    }

    public void addGraphicInfo(String diagramId, String key, GraphicInfo graphicInfo) {
        locationMap.computeIfAbsent(diagramId, k -> new LinkedHashMap<>());
        locationMap.get(diagramId).put(key, graphicInfo);
    }

    public Map<String, Map<String, GraphicInfo>> getGraphicInfo() {
        return locationMap;
    }

    public Map<String, GraphicInfo> getGraphicInfo(String diagramId) {
        return locationMap.get(diagramId);
    }

    public GraphicInfo getGraphicInfo(String diagramId, String key) {
        return locationMap.get(diagramId).get(key);
    }

    public void removeGraphicInfo(String diagramId) {
        locationMap.remove(diagramId);
    }

    public Map<String, List<GraphicInfo>> getFlowLocationGraphicInfo(String diagramId) {
        return flowLocationMap.get(diagramId);
    }

    public Map<String, Map<String, List<GraphicInfo>>> getFlowLocationGraphicInfo() {
        return flowLocationMap;
    }

    public Map<String, Map<String, List<GraphicInfo>>> getDecisionServiceDividerGraphicInfo() {
        return decisionServiceDividerLocationMap;
    }

    public Map<String, List<GraphicInfo>> getDecisionServiceDividerGraphicInfo(String diagramId) {
        return decisionServiceDividerLocationMap.get(diagramId);
    }

    public void removeFlowGraphicInfoList(String diagramId) {
        flowLocationMap.remove(diagramId);
    }

    public Map<String, Map<String, GraphicInfo>> getLocationMap() {
        return locationMap;
    }

    public Map<String, Map<String, List<GraphicInfo>>> getFlowLocationMap() {
        return flowLocationMap;
    }

    public GraphicInfo getLabelGraphicInfo(String diagramId) {
        return labelLocationMap.get(diagramId);
    }

    public void addLabelGraphicInfo(String key, GraphicInfo graphicInfo) {
        labelLocationMap.put(key, graphicInfo);
    }

    public void removeLabelGraphicInfo(String key) {
        labelLocationMap.remove(key);
    }

    public Map<String, GraphicInfo> getLabelLocationMap() {
        return labelLocationMap;
    }

    public void addFlowGraphicInfoList(String diagramId, String key, List<GraphicInfo> graphicInfoList) {
        flowLocationMap.computeIfAbsent(diagramId, k -> new LinkedHashMap<>());
        flowLocationMap.get(diagramId).put(key, graphicInfoList);
    }

    public Map<String, Map<String, List<GraphicInfo>>> getDecisionServiceDividerLocationMap() {
        return decisionServiceDividerLocationMap;
    }


    public void addDecisionServiceDividerGraphicInfoList(String diagramId, String key, List<GraphicInfo> graphicInfoList) {
        decisionServiceDividerLocationMap.computeIfAbsent(diagramId, k -> new LinkedHashMap<>());
        decisionServiceDividerLocationMap.get(diagramId).put(key, graphicInfoList);
    }

}
