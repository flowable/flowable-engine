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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemDefinitionWithNewTargetIdsMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdWithDefinitionIdMapping;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.RemoveWaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.WaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.runtime.ChangePlanItemStateBuilder;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Tijs Rademakers
 */
public class ChangePlanItemStateBuilderImpl implements ChangePlanItemStateBuilder {

    protected CmmnRuntimeServiceImpl runtimeService;

    protected String caseInstanceId;
    protected Set<ActivatePlanItemDefinitionMapping> activatePlanItemDefinitions = new HashSet<>();
    protected Set<MoveToAvailablePlanItemDefinitionMapping> changeToAvailableStatePlanItemDefinitions = new HashSet<>();
    protected Set<TerminatePlanItemDefinitionMapping> terminatePlanItemDefinitions = new HashSet<>();
    protected Set<WaitingForRepetitionPlanItemDefinitionMapping> waitingForRepetitionPlanItemDefinitions = new HashSet<>();
    protected Set<RemoveWaitingForRepetitionPlanItemDefinitionMapping> removeWaitingForRepetitionPlanItemDefinitions = new HashSet<>();
    protected Set<ChangePlanItemIdMapping> changePlanItemIds = new HashSet<>();
    protected Set<ChangePlanItemIdWithDefinitionIdMapping> changePlanItemIdsWithDefinitionId = new HashSet<>();
    protected Set<ChangePlanItemDefinitionWithNewTargetIdsMapping> changePlanItemDefinitionWithNewTargetIds = new HashSet<>();
    protected Map<String, Object> caseVariables = new HashMap<>();
    protected Map<String, Map<String, Object>> childInstanceTaskVariables = new HashMap<>();

    public ChangePlanItemStateBuilderImpl() {
    }

    public ChangePlanItemStateBuilderImpl(CmmnRuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ChangePlanItemStateBuilder caseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder activatePlanItemDefinitionId(String planItemDefinitionId) {
        activatePlanItemDefinitions.add(new ActivatePlanItemDefinitionMapping(planItemDefinitionId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder activatePlanItemDefinitionIds(List<String> planItemDefinitionIds) {
        if (planItemDefinitionIds != null) {
            for (String planItemDefinitionId : planItemDefinitionIds) {
                activatePlanItemDefinitions.add(new ActivatePlanItemDefinitionMapping(planItemDefinitionId));
            }
        }
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder activatePlanItemDefinition(ActivatePlanItemDefinitionMapping planItemDefinitionMapping) {
        activatePlanItemDefinitions.add(planItemDefinitionMapping);
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder activatePlanItemDefinitions(List<ActivatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        activatePlanItemDefinitions.addAll(planItemDefinitionMappings);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder changeToAvailableStateByPlanItemDefinitionId(String planItemDefinitionId) {
        changeToAvailableStatePlanItemDefinitions.add(new MoveToAvailablePlanItemDefinitionMapping(planItemDefinitionId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder changeToAvailableStateByPlanItemDefinitionIds(List<String> planItemDefinitionIds) {
        if (planItemDefinitionIds != null) {
            for (String planItemDefinitionId : planItemDefinitionIds) {
                changeToAvailableStatePlanItemDefinitions.add(new MoveToAvailablePlanItemDefinitionMapping(planItemDefinitionId));
            }
        }
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder changeToAvailableStateByPlanItemDefinition(MoveToAvailablePlanItemDefinitionMapping planItemDefinitionMapping) {
        changeToAvailableStatePlanItemDefinitions.add(planItemDefinitionMapping);
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder terminatePlanItemDefinitionId(String planItemDefinitionId) {
        terminatePlanItemDefinitions.add(new TerminatePlanItemDefinitionMapping(planItemDefinitionId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder terminatePlanItemDefinition(TerminatePlanItemDefinitionMapping planItemDefinition) {
        terminatePlanItemDefinitions.add(planItemDefinition);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder terminatePlanItemDefinitionIds(List<String> planItemDefinitionIds) {
        if (planItemDefinitionIds != null) {
            for (String planItemDefinitionId : planItemDefinitionIds) {
                terminatePlanItemDefinitions.add(new TerminatePlanItemDefinitionMapping(planItemDefinitionId));
            }
        }
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder addWaitingForRepetitionPlanItemDefinitionId(String planItemDefinitionId) {
        waitingForRepetitionPlanItemDefinitions.add(new WaitingForRepetitionPlanItemDefinitionMapping(planItemDefinitionId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder addWaitingForRepetitionPlanItemDefinition(WaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionMapping) {
        waitingForRepetitionPlanItemDefinitions.add(planItemDefinitionMapping);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder addWaitingForRepetitionPlanItemDefinitionIds(List<String> planItemDefinitionIds) {
        if (planItemDefinitionIds != null) {
            for (String planItemDefinitionId : planItemDefinitionIds) {
                waitingForRepetitionPlanItemDefinitions.add(new WaitingForRepetitionPlanItemDefinitionMapping(planItemDefinitionId));
            }
        }
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder removeWaitingForRepetitionPlanItemDefinitionId(String planItemDefinitionId) {
        removeWaitingForRepetitionPlanItemDefinitions.add(new RemoveWaitingForRepetitionPlanItemDefinitionMapping(planItemDefinitionId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder removeWaitingForRepetitionPlanItemDefinition(RemoveWaitingForRepetitionPlanItemDefinitionMapping planItemDefinition) {
        removeWaitingForRepetitionPlanItemDefinitions.add(planItemDefinition);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder removeWaitingForRepetitionPlanItemDefinitionIds(List<String> planItemDefinitionIds) {
        if (planItemDefinitionIds != null) {
            for (String planItemDefinitionId : planItemDefinitionIds) {
                removeWaitingForRepetitionPlanItemDefinitions.add(new RemoveWaitingForRepetitionPlanItemDefinitionMapping(planItemDefinitionId));
            }
        }
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder changePlanItemId(String existingPlanItemId, String newPlanItemId) {
        changePlanItemIds.add(new ChangePlanItemIdMapping(existingPlanItemId, newPlanItemId));
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder changePlanItemIds(Map<String, String> changePlanItemIdMap) {
        if (changePlanItemIdMap != null) {
            for (String existingPlanItemId : changePlanItemIdMap.keySet()) {
                changePlanItemIds.add(new ChangePlanItemIdMapping(existingPlanItemId, changePlanItemIdMap.get(existingPlanItemId)));
            }
        }
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder changePlanItemIdWithDefinitionId(String existingPlanItemDefinitionId, String newPlanItemDefinitionId) {
        changePlanItemIdsWithDefinitionId.add(new ChangePlanItemIdWithDefinitionIdMapping(existingPlanItemDefinitionId, newPlanItemDefinitionId));
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder changePlanItemIdsWithDefinitionId(Map<String, String> changePlanItemIdWithDefinitionIdMap) {
        if (changePlanItemIdWithDefinitionIdMap != null) {
            for (String existingPlanItemDefinitionId : changePlanItemIdWithDefinitionIdMap.keySet()) {
                changePlanItemIdsWithDefinitionId.add(new ChangePlanItemIdWithDefinitionIdMapping(existingPlanItemDefinitionId, changePlanItemIdWithDefinitionIdMap.get(existingPlanItemDefinitionId)));
            }
        }
        return this;
    }
    
    @Override
    public ChangePlanItemStateBuilder changePlanItemDefinitionWithNewTargetIds(String existingPlanItemDefinitionId, String newPlanItemId, String newPlanItemDefinitionId) {
        changePlanItemDefinitionWithNewTargetIds.add(new ChangePlanItemDefinitionWithNewTargetIdsMapping(existingPlanItemDefinitionId, newPlanItemId, newPlanItemDefinitionId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder caseVariable(String caseVariableName, Object caseVariableValue) {
        this.caseVariables.put(caseVariableName, caseVariableValue);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder caseVariables(Map<String, Object> caseVariables) {
        this.caseVariables.putAll(caseVariables);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder childInstanceTaskVariable(String planItemDefinitionId, String name, Object value) {
        if (!this.childInstanceTaskVariables.containsKey(planItemDefinitionId)) {
            this.childInstanceTaskVariables.put(planItemDefinitionId, new HashMap<>());
        }
        this.childInstanceTaskVariables.get(planItemDefinitionId).put(name, value);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder childInstanceTaskVariables(String planItemDefinitionId, Map<String, Object> variables) {
        if (!this.childInstanceTaskVariables.containsKey(planItemDefinitionId)) {
            this.childInstanceTaskVariables.put(planItemDefinitionId, new HashMap<>());
        }
        this.childInstanceTaskVariables.get(planItemDefinitionId).putAll(variables);
        return this;
    }

    @Override
    public void changeState() {
        if (runtimeService == null) {
            throw new FlowableException("CmmnRuntimeService cannot be null, Obtain your builder instance from the CmmnRuntimeService to access this feature");
        }
        runtimeService.changePlanItemState(this);
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public Set<ActivatePlanItemDefinitionMapping> getActivatePlanItemDefinitions() {
        return activatePlanItemDefinitions;
    }

    public Set<MoveToAvailablePlanItemDefinitionMapping> getChangeToAvailableStatePlanItemDefinitions() {
        return changeToAvailableStatePlanItemDefinitions;
    }

    public Set<TerminatePlanItemDefinitionMapping> getTerminatePlanItemDefinitions() {
        return terminatePlanItemDefinitions;
    }

    public Set<WaitingForRepetitionPlanItemDefinitionMapping> getWaitingForRepetitionPlanItemDefinitions() {
        return waitingForRepetitionPlanItemDefinitions;
    }
    
    public Set<RemoveWaitingForRepetitionPlanItemDefinitionMapping> getRemoveWaitingForRepetitionPlanItemDefinitions() {
        return removeWaitingForRepetitionPlanItemDefinitions;
    }

    public Set<ChangePlanItemIdMapping> getChangePlanItemIds() {
        return changePlanItemIds;
    }
    
    public Set<ChangePlanItemIdWithDefinitionIdMapping> getChangePlanItemIdsWithDefinitionId() {
        return changePlanItemIdsWithDefinitionId;
    }
    
    public Set<ChangePlanItemDefinitionWithNewTargetIdsMapping> getChangePlanItemDefinitionWithNewTargetIds() {
        return changePlanItemDefinitionWithNewTargetIds;
    }

    public Map<String, Object> getCaseVariables() {
        return caseVariables;
    }

    public Map<String, Map<String, Object>> getChildInstanceTaskVariables() {
        return childInstanceTaskVariables;
    }
}
