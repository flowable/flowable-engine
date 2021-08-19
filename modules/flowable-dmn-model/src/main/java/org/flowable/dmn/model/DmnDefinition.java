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
    protected String exporter;
    protected String exporterVersion;
    protected List<InputData> inputData = new ArrayList<>();
    protected List<ItemDefinition> itemDefinitions = new ArrayList<>();
    protected List<Decision> decisions = new ArrayList<>();
    protected List<DecisionService> decisionServices = new ArrayList<>();
    protected Map<String, DmnDiDiagram> diDiagrams = new LinkedHashMap<>();
    protected Map<String, GraphicInfo> locationMap = new LinkedHashMap<>();
    protected Map<String, Map<String, GraphicInfo>> locationByDiagramIdMap = new LinkedHashMap<>();
    protected Map<String, GraphicInfo> labelLocationMap = new LinkedHashMap<>();
    protected Map<String, Map<String, GraphicInfo>> labelLocationByDiagramIdMap = new LinkedHashMap<>();
    protected Map<String, List<GraphicInfo>> flowLocationMap = new LinkedHashMap<>();
    protected Map<String, Map<String, List<GraphicInfo>>> flowLocationByDiagramIdMap = new LinkedHashMap<>();
    protected Map<String, List<GraphicInfo>> decisionServiceDividerLocationMap = new LinkedHashMap<>();
    protected Map<String, Map<String, List<GraphicInfo>>> decisionServiceDividerLocationByDiagramIdMap = new LinkedHashMap<>();

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

    public Map<String, DmnDiDiagram> getDiDiagramMap() {
        return diDiagrams;
    }
    public void addDiDiagram(DmnDiDiagram diDiagram) {
        this.diDiagrams.put(diDiagram.getId(), diDiagram);
    }

    public void addGraphicInfo(String key, GraphicInfo graphicInfo) {
        locationMap.put(key, graphicInfo);
    }

    public void addGraphicInfoByDiagramId(String diagramId, String key, GraphicInfo graphicInfo) {
        locationByDiagramIdMap.computeIfAbsent(diagramId, k -> new LinkedHashMap<>());
        locationByDiagramIdMap.get(diagramId).put(key, graphicInfo);
        locationMap.put(key, graphicInfo);
    }

    public GraphicInfo getGraphicInfo(String key) {
        return locationMap.get(key);
    }

    public GraphicInfo getGraphicInfoByDiagramId(String diagramId, String key) {
        return locationByDiagramIdMap.get(diagramId).get(key);
    }

    public Map<String, GraphicInfo> getLocationMap() {
        return locationMap;
    }

    public Map<String, Map<String, GraphicInfo>> getLocationByDiagramIdMap() {
        return locationByDiagramIdMap;
    }

    public Map<String, GraphicInfo> getLocationMapByDiagramId(String diagramId) {
        return locationByDiagramIdMap.get(diagramId);
    }

    public void removeLocationByDiagramId(String diagramId) {
        locationByDiagramIdMap.remove(diagramId);
    }

    public List<GraphicInfo> getFlowLocationGraphicInfo(String key) {
        return flowLocationMap.get(key);
    }

    public Map<String, List<GraphicInfo>> getFlowLocationMap() {
        return flowLocationMap;
    }

    public Map<String, List<GraphicInfo>> getFlowLocationMapByDiagramId(String diagramId) {
        return flowLocationByDiagramIdMap.get(diagramId);
    }

    public Map<String, Map<String, List<GraphicInfo>>> getFlowLocationByDiagramIdMap() {
        return flowLocationByDiagramIdMap;
    }

    public void removeFlowLocationByDiagramId(String diagramId) {
        flowLocationByDiagramIdMap.remove(diagramId);
    }

    public void addFlowGraphicInfoList(String key, List<GraphicInfo> graphicInfoList) {
        flowLocationMap.put(key, graphicInfoList);
    }

    public void addFlowGraphicInfoListByDiagramId(String diagramId, String key, List<GraphicInfo> graphicInfoList) {
        flowLocationByDiagramIdMap.computeIfAbsent(diagramId, k -> new LinkedHashMap<>());
        flowLocationByDiagramIdMap.get(diagramId).put(key, graphicInfoList);
        flowLocationMap.put(key, graphicInfoList);
    }

    public Map<String, Map<String, GraphicInfo>> getLabelLocationByDiagramIdMap() {
        return labelLocationByDiagramIdMap;
    }

    public Map<String, GraphicInfo> getLabelLocationByDiagramId(String diagramId) {
        return labelLocationByDiagramIdMap.get(diagramId);
    }

    public void addLabelGraphicInfo(String key, GraphicInfo graphicInfo) {
        labelLocationMap.put(key, graphicInfo);
    }

    public void addLabelGraphicInfoByDiagramId(String diagramId, String key, GraphicInfo graphicInfo) {
        labelLocationByDiagramIdMap.computeIfAbsent(diagramId, k -> new LinkedHashMap<>());
        labelLocationByDiagramIdMap.get(diagramId).put(key, graphicInfo);
        labelLocationMap.put(key, graphicInfo);
    }

    public void removeLabelGraphicInfo(String key) {
        flowLocationMap.remove(key);
    }

    public Map<String, Map<String, List<GraphicInfo>>> getDecisionServiceDividerLocationByDiagramIdMap() {
        return decisionServiceDividerLocationByDiagramIdMap;
    }

    public Map<String, List<GraphicInfo>> getDecisionServiceDividerLocationMapByDiagramId(String diagramId) {
        return decisionServiceDividerLocationByDiagramIdMap.get(diagramId);
    }

    public Map<String, List<GraphicInfo>> getDecisionServiceDividerLocationMap() {
        return decisionServiceDividerLocationMap;
    }

    public List<GraphicInfo> getDecisionServiceDividerGraphicInfo(String key) {
        return decisionServiceDividerLocationMap.get(key);
    }

    public void addDecisionServiceDividerGraphicInfoList(String key, List<GraphicInfo> graphicInfoList) {
        decisionServiceDividerLocationMap.put(key, graphicInfoList);
    }

    public void addDecisionServiceDividerGraphicInfoListByDiagramId(String diagramId, String key, List<GraphicInfo> graphicInfoList) {
        decisionServiceDividerLocationByDiagramIdMap.computeIfAbsent(diagramId, k -> new LinkedHashMap<>());
        decisionServiceDividerLocationByDiagramIdMap.get(diagramId).put(key, graphicInfoList);
        decisionServiceDividerLocationMap.put(key, graphicInfoList);
    }

    public String getExporter() {
        return exporter;
    }
    public void setExporter(String exporter) {
        this.exporter = exporter;
    }
    public String getExporterVersion() {
        return exporterVersion;
    }
    public void setExporterVersion(String exporterVersion) {
        this.exporterVersion = exporterVersion;
    }
}
