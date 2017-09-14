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
    protected Map<String, PlanItemDefinition> planItemDefinitionMap = new LinkedHashMap<>();
    protected List<Criterion> exitCriteria = new ArrayList<>();

    public void addPlanItemDefinition(PlanItemDefinition planItemDefinition) {
        planItemDefinitionMap.put(planItemDefinition.getId(), planItemDefinition);
    }

    public PlanItemDefinition findPlanItemDefinition(String planItemDefinitionId) {
        if (id != null && id.equals(planItemDefinitionId)) {
            return this;
        }

        if (planItemDefinitionMap.containsKey(planItemDefinitionId)) {
            return planItemDefinitionMap.get(planItemDefinitionId);
        }

        Stage parentStage = getParentStage();
        if (parentStage != null) {
            return parentStage.findPlanItemDefinition(planItemDefinitionId);
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

    public List<Criterion> getExitCriteria() {
        return exitCriteria;
    }

    public void setExitCriteria(List<Criterion> exitCriteria) {
        this.exitCriteria = exitCriteria;
    }

}
