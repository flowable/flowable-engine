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
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
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
    public ChangePlanItemStateBuilder terminatePlanItemDefinitionId(String planItemDefinitionId) {
        terminatePlanItemDefinitions.add(new TerminatePlanItemDefinitionMapping(planItemDefinitionId));
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

    public Map<String, Object> getCaseVariables() {
        return caseVariables;
    }

    public Map<String, Map<String, Object>> getChildInstanceTaskVariables() {
        return childInstanceTaskVariables;
    }
}
