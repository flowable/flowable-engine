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
package org.flowable.engine.impl.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;

/**
 * @author Tijs Rademakers
 */
public class ChangeActivityStateBuilderImpl implements ChangeActivityStateBuilder {

    protected RuntimeServiceImpl runtimeService;

    protected String processInstanceId;
    protected List<MoveExecutionIdContainer> moveExecutionIdList = new ArrayList<>();
    protected List<MoveActivityIdContainer> moveActivityIdList = new ArrayList<>();
    protected Map<String, Object> processVariables = new HashMap<>();
    protected Map<String, Map<String, Object>> localVariables = new HashMap<>();

    public ChangeActivityStateBuilderImpl() {
    }

    public ChangeActivityStateBuilderImpl(RuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ChangeActivityStateBuilder processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveExecutionToActivityId(String executionId, String activityId) {
        return moveExecutionToActivityId(executionId, activityId, null);
    }

    public ChangeActivityStateBuilder moveExecutionToActivityId(String executionId, String activityId, String newAssigneeId) {
        moveExecutionIdList.add(new MoveExecutionIdContainer(executionId, activityId, newAssigneeId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveExecutionsToSingleActivityId(List<String> executionIds, String activityId) {
        return moveExecutionsToSingleActivityId(executionIds, activityId, null);
    }

    public ChangeActivityStateBuilder moveExecutionsToSingleActivityId(List<String> executionIds, String activityId, String newAssigneeId) {
        moveExecutionIdList.add(new MoveExecutionIdContainer(executionIds, activityId, newAssigneeId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveSingleExecutionToActivityIds(String executionId, List<String> activityIds) {
        return moveSingleExecutionToActivityIds(executionId, activityIds, null);
    }

    public ChangeActivityStateBuilder moveSingleExecutionToActivityIds(String executionId, List<String> activityIds, String newAssigneeId) {
        moveExecutionIdList.add(new MoveExecutionIdContainer(executionId, activityIds, newAssigneeId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveActivityIdTo(String currentActivityId, String newActivityId) {
        return moveActivityIdTo(currentActivityId, newActivityId, null);
    }

    public ChangeActivityStateBuilder moveActivityIdTo(String currentActivityId, String newActivityId, String newAssigneeId) {
        moveActivityIdList.add(new MoveActivityIdContainer(currentActivityId, newActivityId, newAssigneeId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveActivityIdsToSingleActivityId(List<String> activityIds, String activityId) {
        return moveActivityIdsToSingleActivityId(activityIds, activityId, null);
    }

    public ChangeActivityStateBuilder moveActivityIdsToSingleActivityId(List<String> activityIds, String activityId, String newAssigneeId) {
        moveActivityIdList.add(new MoveActivityIdContainer(activityIds, activityId, newAssigneeId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveSingleActivityIdToActivityIds(String currentActivityId, List<String> newActivityIds) {
        return moveSingleActivityIdToActivityIds(currentActivityId, newActivityIds, null);
    }

    public ChangeActivityStateBuilder moveSingleActivityIdToActivityIds(String currentActivityId, List<String> newActivityIds, String newAssigneeId) {
        moveActivityIdList.add(new MoveActivityIdContainer(currentActivityId, newActivityIds, newAssigneeId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveActivityIdToParentActivityId(String currentActivityId, String newActivityId) {
        return moveActivityIdToParentActivityId(currentActivityId, newActivityId, null);
    }

    public ChangeActivityStateBuilder moveActivityIdToParentActivityId(String currentActivityId, String newActivityId, String newAssigneeId) {
        MoveActivityIdContainer moveActivityIdContainer = new MoveActivityIdContainer(currentActivityId, newActivityId, newAssigneeId);
        moveActivityIdContainer.setMoveToParentProcess(true);
        moveActivityIdList.add(moveActivityIdContainer);
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveActivityIdToSubProcessInstanceActivityId(String currentActivityId, String newActivityId, String callActivityId) {
        return moveActivityIdToSubProcessInstanceActivityId(currentActivityId, newActivityId, callActivityId, null);
    }

    public ChangeActivityStateBuilder moveActivityIdToSubProcessInstanceActivityId(String currentActivityId, String newActivityId, String callActivityId, String newAssigneeId) {
        MoveActivityIdContainer moveActivityIdContainer = new MoveActivityIdContainer(currentActivityId, newActivityId, newAssigneeId);
        moveActivityIdContainer.setMoveToSubProcessInstance(true);
        moveActivityIdContainer.setCallActivityId(callActivityId);
        moveActivityIdList.add(moveActivityIdContainer);
        return this;
    }

    @Override
    public ChangeActivityStateBuilder processVariable(String processVariableName, Object processVariableValue) {
        if (this.processVariables == null) {
            this.processVariables = new HashMap<>();
        }

        this.processVariables.put(processVariableName, processVariableValue);
        return this;
    }

    @Override
    public ChangeActivityStateBuilder processVariables(Map<String, Object> processVariables) {
        this.processVariables = processVariables;
        return this;
    }

    @Override
    public ChangeActivityStateBuilder localVariable(String startActivityId, String localVariableName, Object localVariableValue) {
        if (this.localVariables == null) {
            this.localVariables = new HashMap<>();
        }

        Map<String, Object> localVariableMap = null;
        if (localVariables.containsKey(startActivityId)) {
            localVariableMap = localVariables.get(startActivityId);
        } else {
            localVariableMap = new HashMap<>();
        }

        localVariableMap.put(localVariableName, localVariableValue);

        this.localVariables.put(startActivityId, localVariableMap);

        return this;
    }

    @Override
    public ChangeActivityStateBuilder localVariables(String startActivityId, Map<String, Object> localVariables) {
        if (this.localVariables == null) {
            this.localVariables = new HashMap<>();
        }

        this.localVariables.put(startActivityId, localVariables);

        return this;
    }

    @Override
    public void changeState() {
        if (runtimeService == null) {
            throw new FlowableException("RuntimeService cannot be null, Obtain your builder instance from the RuntimService to access this feature");
        }
        runtimeService.changeActivityState(this);
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public List<MoveExecutionIdContainer> getMoveExecutionIdList() {
        return moveExecutionIdList;
    }

    public List<MoveActivityIdContainer> getMoveActivityIdList() {
        return moveActivityIdList;
    }

    public Map<String, Object> getProcessInstanceVariables() {
        return processVariables;
    }

    public Map<String, Map<String, Object>> getLocalVariables() {
        return localVariables;
    }
}
