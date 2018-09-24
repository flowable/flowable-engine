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
        moveExecutionIdList.add(new MoveExecutionIdContainer(executionId, activityId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveExecutionsToSingleActivityId(List<String> executionIds, String activityId) {
        moveExecutionIdList.add(new MoveExecutionIdContainer(executionIds, activityId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveSingleExecutionToActivityIds(String executionId, List<String> activityIds) {
        moveExecutionIdList.add(new MoveExecutionIdContainer(executionId, activityIds));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveActivityIdTo(String currentActivityId, String newActivityId) {
        moveActivityIdList.add(new MoveActivityIdContainer(currentActivityId, newActivityId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveActivityIdsToSingleActivityId(List<String> activityIds, String activityId) {
        moveActivityIdList.add(new MoveActivityIdContainer(activityIds, activityId));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveSingleActivityIdToActivityIds(String currentActivityId, List<String> newActivityIds) {
        moveActivityIdList.add(new MoveActivityIdContainer(currentActivityId, newActivityIds));
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveActivityIdToParentActivityId(String currentActivityId, String newActivityId) {
        MoveActivityIdContainer moveActivityIdContainer = new MoveActivityIdContainer(currentActivityId, newActivityId);
        moveActivityIdContainer.setMoveToParentProcess(true);
        moveActivityIdList.add(moveActivityIdContainer);
        return this;
    }

    @Override
    public ChangeActivityStateBuilder moveActivityIdToSubProcessInstanceActivityId(String currentActivityId, String newActivityId, String callActivityId) {
        MoveActivityIdContainer moveActivityIdContainer = new MoveActivityIdContainer(currentActivityId, newActivityId);
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
