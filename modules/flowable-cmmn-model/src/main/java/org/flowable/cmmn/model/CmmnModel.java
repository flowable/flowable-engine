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
package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public class CmmnModel {

    protected String id;
    protected String name;
    protected String targetNamespace;
    protected String expressionLanguage;
    protected String exporter;
    protected String exporterVersion;
    protected String author;
    protected Date creationDate;

    protected List<Case> cases = new ArrayList<>();

    protected List<Process> processes = new ArrayList<>();

    protected List<Decision> decisions = new ArrayList<>();

    protected List<Association> associations = new ArrayList<>();

    protected Map<String, Criterion> criterionMap = new LinkedHashMap<>();
    protected Map<String, String> criterionTechnicalIdMap = new HashMap<>();

    protected Map<String, GraphicInfo> locationMap = new LinkedHashMap<>();
    protected Map<String, GraphicInfo> labelLocationMap = new LinkedHashMap<>();
    protected Map<String, List<GraphicInfo>> flowLocationMap = new LinkedHashMap<>();

    protected Map<String, String> namespaceMap = new LinkedHashMap<>();

    public void addCase(Case caze) {
        cases.add(caze);
    }

    public Case getPrimaryCase() {
        return cases.get(0);
    }

    public Case getCaseById(String id) {
        for (Case caze : cases) {
            if (id.equals(caze.getId())) {
                return caze;
            }
        }
        return null;
    }

    public void addProcess(Process process) {
        processes.add(process);
    }

    public Process getProcessById(String id) {
        for (Process process : processes) {
            if (id.equals(process.getId())) {
                return process;
            }
        }
        return null;
    }

    public void addDecision(Decision decision) {
        decisions.add(decision);
    }

    public Decision getDecisionById(String id) {
        for (Decision decision : decisions) {
            if (id.equals(decision.getId())) {
                return decision;
            }
        }
        return null;
    }

    public Collection<Decision> getDecisions() {
        return decisions;
    }

    public PlanItemDefinition findPlanItemDefinition(String id) {
        PlanItemDefinition foundPlanItemDefinition = null;
        for (Case caseModel : cases) {
            foundPlanItemDefinition = caseModel.getPlanModel().findPlanItemDefinition(id);
            if (foundPlanItemDefinition != null) {
                break;
            }
        }

        if (foundPlanItemDefinition == null) {
            for (Case caseModel : cases) {
                for (Stage stage : caseModel.getPlanModel().findPlanItemDefinitionsOfType(Stage.class, true)) {
                    foundPlanItemDefinition = stage.findPlanItemDefinition(id);
                    if (foundPlanItemDefinition != null) {
                        break;
                    }
                }
                if (foundPlanItemDefinition != null) {
                    break;
                }
            }
        }

        return foundPlanItemDefinition;
    }

    public PlanItem findPlanItem(String id) {
        PlanItem foundPlanItem = null;
        for (Case caseModel : cases) {
            foundPlanItem = caseModel.getPlanModel().findPlanItemInPlanFragmentOrUpwards(id);
            if (foundPlanItem != null) {
                break;
            }
        }

        if (foundPlanItem == null) {
            for (Case caseModel : cases) {
                for (Stage stage : caseModel.getPlanModel().findPlanItemDefinitionsOfType(Stage.class, true)) {
                    foundPlanItem = stage.findPlanItemInPlanFragmentOrUpwards(id);
                    if (foundPlanItem != null) {
                        break;
                    }
                }
                if (foundPlanItem != null) {
                    break;
                }
            }
        }

        return foundPlanItem;
    }

    public void addAssociation(Association association) {
        associations.add(association);
    }

    public void addCriterion(String key, Criterion criterion) {
        criterionMap.put(key, criterion);
    }

    public Criterion getCriterion(String key) {
        return criterionMap.get(key);
    }

    public void addCriterionTechnicalId(String technicalId, String id) {
        criterionTechnicalIdMap.put(technicalId, id);
    }

    public String getCriterionId(String technicalId) {
        return criterionTechnicalIdMap.get(technicalId);
    }

    public void addGraphicInfo(String key, GraphicInfo graphicInfo) {
        locationMap.put(key, graphicInfo);
    }

    public GraphicInfo getGraphicInfo(String key) {
        return locationMap.get(key);
    }

    public void removeGraphicInfo(String key) {
        locationMap.remove(key);
    }

    public List<GraphicInfo> getFlowLocationGraphicInfo(String key) {
        return flowLocationMap.get(key);
    }

    public void removeFlowGraphicInfoList(String key) {
        flowLocationMap.remove(key);
    }

    public Map<String, GraphicInfo> getLocationMap() {
        return locationMap;
    }

    public Map<String, List<GraphicInfo>> getFlowLocationMap() {
        return flowLocationMap;
    }

    public GraphicInfo getLabelGraphicInfo(String key) {
        return labelLocationMap.get(key);
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

    public void addFlowGraphicInfoList(String key, List<GraphicInfo> graphicInfoList) {
        flowLocationMap.put(key, graphicInfoList);
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getTargetNamespace() {
        return targetNamespace;
    }
    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }
    public String getExpressionLanguage() {
        return expressionLanguage;
    }
    public void setExpressionLanguage(String expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
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
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public List<Case> getCases() {
        return cases;
    }
    public void setCases(List<Case> cases) {
        this.cases = cases;
    }
    public List<Process> getProcesses() {
        return processes;
    }
    public void setProcesses(List<Process> processes) {
        this.processes = processes;
    }
    public List<Association> getAssociations() {
        return associations;
    }
    public void setAssociations(List<Association> associations) {
        this.associations = associations;
    }
    public void addNamespace(String prefix, String uri) {
        namespaceMap.put(prefix, uri);
    }
    public boolean containsNamespacePrefix(String prefix) {
        return namespaceMap.containsKey(prefix);
    }
    public String getNamespace(String prefix) {
        return namespaceMap.get(prefix);
    }
    public Map<String, String> getNamespaces() {
        return namespaceMap;
    }
}
