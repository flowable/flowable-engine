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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public class Stage extends PlanFragment implements HasExitCriteria {

    protected boolean isPlanModel;
    protected List<Criterion> exitCriteria = new ArrayList<>();
    protected boolean autoComplete; 
    protected String autoCompleteCondition;
    protected String formKey; // For the start form of the plan model. Null otherwise
    protected boolean sameDeployment = true;
    protected String validateFormFields;
    protected Integer displayOrder;
    protected String includeInStageOverview;
    protected Map<String, PlanItemDefinition> planItemDefinitionMap = new LinkedHashMap<>();

    public void addPlanItemDefinition(PlanItemDefinition planItemDefinition) {
        planItemDefinitionMap.put(planItemDefinition.getId(), planItemDefinition);
    }

    public PlanItemDefinition findPlanItemDefinitionInStageOrUpwards(String planItemDefinitionId) {
        if (id != null && id.equals(planItemDefinitionId)) {
            return this;
        }

        if (planItemDefinitionMap.containsKey(planItemDefinitionId)) {
            return planItemDefinitionMap.get(planItemDefinitionId);
        }

        Stage parentStage = getParentStage();
        if (parentStage != null) {
            return parentStage.findPlanItemDefinitionInStageOrUpwards(planItemDefinitionId);
        }

        return null;
    }

    public PlanItemDefinition findPlanItemDefinitionInStageOrDownwards(String planItemDefinitionId) {
        if (id != null && id.equals(planItemDefinitionId)) {
            return this;
        }

        if (planItemDefinitionMap.containsKey(planItemDefinitionId)) {
            return planItemDefinitionMap.get(planItemDefinitionId);
        }

        for (String key : planItemDefinitionMap.keySet()) {
            PlanItemDefinition planItemDefinition = planItemDefinitionMap.get(key);
            if (planItemDefinition instanceof Stage) {
                PlanItemDefinition p = ((Stage) planItemDefinition).findPlanItemDefinitionInStageOrDownwards(planItemDefinitionId);
                if (p != null) {
                    return p;
                }
            }
        }

        return null;
    }

    public <T extends PlanItemDefinition> List<T> findPlanItemDefinitionsOfType(Class<T> clazz, boolean recursive) {
        List<T> planItemDefinitions = new ArrayList<>();
        internalFindPlanItemDefinitionsOfType(clazz, this, planItemDefinitions, recursive);
        return planItemDefinitions;
    }

    @SuppressWarnings("unchecked")
    private <T extends PlanItemDefinition> void internalFindPlanItemDefinitionsOfType(Class<T> clazz, Stage stage, List<T> planItemDefinitions, boolean recursive) {
        for (PlanItemDefinition planItemDefinition : stage.getPlanItemDefinitions()) {
            if (clazz.isInstance(planItemDefinition)) {
                planItemDefinitions.add((T) planItemDefinition);
            }
            if (recursive && planItemDefinition instanceof Stage) {
                internalFindPlanItemDefinitionsOfType(clazz, (Stage) planItemDefinition, planItemDefinitions, recursive);
            }
        }
    }

    public List<PlanItemDefinition> getPlanItemDefinitions() {
        return new ArrayList<>(planItemDefinitionMap.values());
    }

    public Map<String, PlanItemDefinition> getPlanItemDefinitionMap() {
        return planItemDefinitionMap;
    }

    public void setPlanItemDefinitionMap(Map<String, PlanItemDefinition> planItemDefinitionMap) {
        this.planItemDefinitionMap = planItemDefinitionMap;
    }

    public boolean isPlanModel() {
        return isPlanModel;
    }

    public void setPlanModel(boolean isPlanModel) {
        this.isPlanModel = isPlanModel;
    }
    
    public boolean isAutoComplete() {
        return autoComplete;
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }
    
    public String getAutoCompleteCondition() {
        return autoCompleteCondition;
    }

    public void setAutoCompleteCondition(String autoCompleteCondition) {
        this.autoCompleteCondition = autoCompleteCondition;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public boolean isSameDeployment() {
        return sameDeployment;
    }

    public void setSameDeployment(boolean sameDeployment) {
        this.sameDeployment = sameDeployment;
    }

    public String getValidateFormFields() {
        return validateFormFields;
    }

    public void setValidateFormFields(String validateFormFields) {
        this.validateFormFields = validateFormFields;
    }

    @Override
    public void addExitCriterion(Criterion exitCriterion) {
        exitCriteria.add(exitCriterion);
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getIncludeInStageOverview() {
        return includeInStageOverview;
    }

    public void setIncludeInStageOverview(String includeInStageOverview) {
        this.includeInStageOverview = includeInStageOverview;
    }

    @Override
    public List<Criterion> getExitCriteria() {
        return exitCriteria;
    }

    @Override
    public void setExitCriteria(List<Criterion> exitCriteria) {
        this.exitCriteria = exitCriteria;
    }

}
