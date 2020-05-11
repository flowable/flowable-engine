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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;

public class CaseInstanceChangeState {

    protected String caseInstanceId;
    protected CaseDefinition caseDefinitionToMigrateTo;
    protected Map<String, Object> caseVariables = new HashMap<>();
    protected Map<String, List<PlanItemInstanceEntity>> currentPlanItemInstances;
    protected Set<ActivatePlanItemDefinitionMapping> activatePlanItemDefinitions;
    protected Set<MoveToAvailablePlanItemDefinitionMapping> changePlanItemToAvailables;
    protected Set<TerminatePlanItemDefinitionMapping> terminatePlanItemDefinitions;
    protected Map<String, Map<String, Object>> childInstanceTaskVariables = new HashMap<>();
    protected HashMap<String, PlanItemInstanceEntity> createdStageInstances = new HashMap<>();

    public CaseInstanceChangeState() {
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public CaseInstanceChangeState setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    public CaseDefinition getCaseDefinitionToMigrateTo() {
        return caseDefinitionToMigrateTo;
    }

    public CaseInstanceChangeState setCaseDefinitionToMigrateTo(CaseDefinition caseDefinitionToMigrateTo) {
        this.caseDefinitionToMigrateTo = caseDefinitionToMigrateTo;
        return this;
    }

    public Map<String, Object> getCaseVariables() {
        return caseVariables;
    }

    public CaseInstanceChangeState setCaseVariables(Map<String, Object> caseVariables) {
        this.caseVariables = caseVariables;
        return this;
    }

    public Map<String, List<PlanItemInstanceEntity>> getCurrentPlanItemInstances() {
        return currentPlanItemInstances;
    }

    public void setCurrentPlanItemInstances(Map<String, List<PlanItemInstanceEntity>> currentPlanItemInstances) {
        this.currentPlanItemInstances = currentPlanItemInstances;
    }
    
    public PlanItemInstanceEntity getRuntimePlanItemInstance(String planItemDefinitionId) {
        if (currentPlanItemInstances != null && currentPlanItemInstances.containsKey(planItemDefinitionId)) {
            List<PlanItemInstanceEntity> currentPlanItemInstanceList = currentPlanItemInstances.get(planItemDefinitionId);
            for (PlanItemInstanceEntity planItemInstance : currentPlanItemInstanceList) {
                if (!PlanItemInstanceState.TERMINAL_STATES.contains(planItemInstance.getState())) {
                    return planItemInstance;
                }
            }
        }
        
        return null;
    }
    
    public Map<String, List<PlanItemInstanceEntity>> getActivePlanItemInstances() {
        Map<String, List<PlanItemInstanceEntity>> activePlanItemInstanceMap = new HashMap<>();
        if (currentPlanItemInstances != null) {
            for (String planItemDefinitionId : currentPlanItemInstances.keySet()) {
                List<PlanItemInstanceEntity> planItemInstances = currentPlanItemInstances.get(planItemDefinitionId);
                List<PlanItemInstanceEntity> activePlanItemInstances = null;
                for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
                    if (PlanItemInstanceState.ACTIVE_STATES.contains(planItemInstance.getState())) {
                        if (activePlanItemInstances == null) {
                            activePlanItemInstances = new ArrayList<>();
                        }
                        
                        activePlanItemInstances.add(planItemInstance);
                    }
                }
                
                if (activePlanItemInstances != null) {
                    activePlanItemInstanceMap.put(planItemDefinitionId, activePlanItemInstances);
                }
            }
        }
        
        return activePlanItemInstanceMap;
    }
    
    public Map<String, List<PlanItemInstanceEntity>> getRuntimePlanItemInstances() {
        Map<String, List<PlanItemInstanceEntity>> runtimePlanItemInstanceMap = new HashMap<>();
        if (currentPlanItemInstances != null) {
            for (String planItemDefinitionId : currentPlanItemInstances.keySet()) {
                List<PlanItemInstanceEntity> planItemInstances = currentPlanItemInstances.get(planItemDefinitionId);
                List<PlanItemInstanceEntity> runtimePlanItemInstances = null;
                for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
                    if (!PlanItemInstanceState.TERMINAL_STATES.contains(planItemInstance.getState())) {
                        if (runtimePlanItemInstances == null) {
                            runtimePlanItemInstances = new ArrayList<>();
                        }
                        
                        runtimePlanItemInstances.add(planItemInstance);
                    }
                }
                
                if (runtimePlanItemInstances != null) {
                    runtimePlanItemInstanceMap.put(planItemDefinitionId, runtimePlanItemInstances);
                }
            }
        }
        
        return runtimePlanItemInstanceMap;
    }

    public Set<ActivatePlanItemDefinitionMapping> getActivatePlanItemDefinitions() {
        return activatePlanItemDefinitions;
    }

    public CaseInstanceChangeState setActivatePlanItemDefinitions(Set<ActivatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.activatePlanItemDefinitions = planItemDefinitionMappings;
        return this;
    }
    
    public Set<MoveToAvailablePlanItemDefinitionMapping> getChangePlanItemDefinitionsToAvailable() {
        return changePlanItemToAvailables;
    }

    public CaseInstanceChangeState setChangePlanItemDefinitionsToAvailable(Set<MoveToAvailablePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.changePlanItemToAvailables = planItemDefinitionMappings;
        return this;
    }

    public Set<TerminatePlanItemDefinitionMapping> getTerminatePlanItemDefinitions() {
        return terminatePlanItemDefinitions;
    }

    public CaseInstanceChangeState setTerminatePlanItemDefinitions(Set<TerminatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.terminatePlanItemDefinitions = planItemDefinitionMappings;
        return this;
    }

    public Map<String, Map<String, Object>> getChildInstanceTaskVariables() {
        return childInstanceTaskVariables;
    }

    public CaseInstanceChangeState setChildInstanceTaskVariables(Map<String, Map<String, Object>> childInstanceTaskVariables) {
        this.childInstanceTaskVariables = childInstanceTaskVariables;
        return this;
    }

    public HashMap<String, PlanItemInstanceEntity> getCreatedStageInstances() {
        return createdStageInstances;
    }

    public CaseInstanceChangeState setCreatedStageInstances(HashMap<String, PlanItemInstanceEntity> createdStageInstances) {
        this.createdStageInstances = createdStageInstances;
        return this;
    }
    
    public void addCreatedStageInstance(String key, PlanItemInstanceEntity planItemInstance) {
        this.createdStageInstances.put(key, planItemInstance);
    }
}
