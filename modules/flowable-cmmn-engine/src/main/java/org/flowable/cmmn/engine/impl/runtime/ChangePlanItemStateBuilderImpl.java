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

import org.flowable.cmmn.api.runtime.ChangePlanItemStateBuilder;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Tijs Rademakers
 */
public class ChangePlanItemStateBuilderImpl implements ChangePlanItemStateBuilder {

    protected CmmnRuntimeServiceImpl runtimeService;

    protected String caseInstanceId;
    protected List<MovePlanItemInstanceIdContainer> movePlanItemInstanceIdList = new ArrayList<>();
    protected List<MovePlanItemDefinitionIdContainer> movePlanItemDefinitionIdList = new ArrayList<>();
    protected List<String> activatePlanItemDefinitionIdList = new ArrayList<>();
    protected List<String> changePlanItemToAvailableIdList = new ArrayList<>();
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
    public ChangePlanItemStateBuilder movePlanItemInstanceToPlanItemDefinitionId(String planItemInstanceId, String planItemDefinitionId) {
        return moveExecutionToActivityId(planItemInstanceId, planItemDefinitionId, null);
    }

    public ChangePlanItemStateBuilder moveExecutionToActivityId(String planItemInstanceId, String planItemDefinitionId, String newAssigneeId) {
        movePlanItemInstanceIdList.add(new MovePlanItemInstanceIdContainer(planItemInstanceId, planItemDefinitionId, newAssigneeId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder movePlanItemInstancesToSinglePlanItemDefinitionId(List<String> planItemInstanceIds, String planItemDefinitionId) {
        return movePlanItemInstancesToSinglePlanItemDefinitionId(planItemInstanceIds, planItemDefinitionId, null);
    }

    public ChangePlanItemStateBuilder movePlanItemInstancesToSinglePlanItemDefinitionId(List<String> planItemInstanceIds, String planItemDefinitionId, String newAssigneeId) {
        movePlanItemInstanceIdList.add(new MovePlanItemInstanceIdContainer(planItemInstanceIds, planItemDefinitionId, newAssigneeId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder moveSinglePlanItemInstanceToPlanItemDefinitionIds(String executionId, List<String> activityIds) {
        return moveSinglePlanItemInstanceToPlanItemDefinitionIds(executionId, activityIds, null);
    }

    public ChangePlanItemStateBuilder moveSinglePlanItemInstanceToPlanItemDefinitionIds(String executionId, List<String> activityIds, String newAssigneeId) {
        movePlanItemInstanceIdList.add(new MovePlanItemInstanceIdContainer(executionId, activityIds, newAssigneeId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder movePlanItemDefinitionIdTo(String currentPlanItemDefinitionId, String newPlanItemDefinitionId) {
        return movePlanItemDefinitionIdTo(currentPlanItemDefinitionId, newPlanItemDefinitionId, null);
    }

    public ChangePlanItemStateBuilder movePlanItemDefinitionIdTo(String currentPlanItemDefinitionId, String newPlanItemDefinitionId, String newAssigneeId) {
        movePlanItemDefinitionIdList.add(new MovePlanItemDefinitionIdContainer(currentPlanItemDefinitionId, newPlanItemDefinitionId, newAssigneeId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder movePlanItemDefinitionIdsToSinglePlanItemDefinitionId(List<String> planItemDefinitionIds, String planItemDefinitionId) {
        return movePlanItemDefinitionIdsToSinglePlanItemDefinitionId(planItemDefinitionIds, planItemDefinitionId, null);
    }

    public ChangePlanItemStateBuilder movePlanItemDefinitionIdsToSinglePlanItemDefinitionId(List<String> planItemDefinitionIds, String planItemDefinitionId, String newAssigneeId) {
        movePlanItemDefinitionIdList.add(new MovePlanItemDefinitionIdContainer(planItemDefinitionIds, planItemDefinitionId, newAssigneeId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder moveSinglePlanItemDefinitionIdToPlanItemDefinitionIds(String currentPlanItemDefinitionId, List<String> newPlanItemDefinitionIds) {
        return moveSinglePlanItemDefinitionIdToPlanItemDefinitionIds(currentPlanItemDefinitionId, newPlanItemDefinitionIds, null);
    }

    public ChangePlanItemStateBuilder moveSinglePlanItemDefinitionIdToPlanItemDefinitionIds(String currentPlanItemDefinitionId, List<String> newPlanItemDefinitionIds, String newAssigneeId) {
        movePlanItemDefinitionIdList.add(new MovePlanItemDefinitionIdContainer(currentPlanItemDefinitionId, newPlanItemDefinitionIds, newAssigneeId));
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder activatePlanItemDefinitionId(String planItemDefinitionId) {
        activatePlanItemDefinitionIdList.add(planItemDefinitionId);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder activatePlanItemDefinitionIds(List<String> planItemDefinitionIds) {
        activatePlanItemDefinitionIdList.addAll(planItemDefinitionIds);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder changePlanItemInstanceToAvailableByPlanItemDefinitionId(String planItemDefinitionId) {
        changePlanItemToAvailableIdList.add(planItemDefinitionId);
        return this;
    }

    @Override
    public ChangePlanItemStateBuilder changePlanItemInstancesToAvailableByPlanItemDefinitionId(List<String> planItemDefinitionIds) {
        changePlanItemToAvailableIdList.addAll(planItemDefinitionIds);
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
            throw new FlowableException("CmmnRuntimeService cannot be null, Obtain your builder instance from the CmmnRuntimService to access this feature");
        }
        runtimeService.changePlanItemState(this);
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public List<MovePlanItemInstanceIdContainer> getMovePlanItemInstanceIdList() {
        return movePlanItemInstanceIdList;
    }

    public List<MovePlanItemDefinitionIdContainer> getMovePlanItemDefinitionIdList() {
        return movePlanItemDefinitionIdList;
    }

    public List<String> getActivatePlanItemDefinitionIdList() {
        return activatePlanItemDefinitionIdList;
    }

    public List<String> getChangePlanItemToAvailableIdList() {
        return changePlanItemToAvailableIdList;
    }

    public Map<String, Object> getCaseVariables() {
        return caseVariables;
    }

    public Map<String, Map<String, Object>> getChildInstanceTaskVariables() {
        return childInstanceTaskVariables;
    }
}
