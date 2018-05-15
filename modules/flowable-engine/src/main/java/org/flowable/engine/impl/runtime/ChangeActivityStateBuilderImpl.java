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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    protected Map<String, Object> processVariables;
    protected Map<String, Map<String, Object>> localVariables;

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

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

    public Map<String, Map<String, Object>> getLocalVariables() {
        return localVariables;
    }
    
    public class MoveExecutionIdContainer {
        
        protected List<String> executionIds;
        protected String singleExecutionId;
        protected String moveToActivityId;
        protected List<String> moveToActivityIds;
        
        public MoveExecutionIdContainer(String singleExecutionId, String moveToActivityId) {
            this.singleExecutionId = singleExecutionId;
            this.moveToActivityId = moveToActivityId;
        }
        
        public MoveExecutionIdContainer(List<String> executionIds, String moveToActivityId) {
            this.executionIds = executionIds;
            this.moveToActivityId = moveToActivityId;
        }
        
        public MoveExecutionIdContainer(String singleExecutionId, List<String> moveToActivityIds) {
            this.singleExecutionId = singleExecutionId;
            this.moveToActivityIds = moveToActivityIds;
        }
        
        public List<String> getExecutionIds() {
            if (singleExecutionId != null) {
                return Collections.singletonList(singleExecutionId);
            } else if (executionIds != null) {
                return executionIds;
            } else {
                return new ArrayList<>();
            }    
        }
        
        public List<String> getMoveToActivityIds() {
            if (moveToActivityId != null) {
                return Collections.singletonList(moveToActivityId);
            } else if (moveToActivityIds != null) {
                return moveToActivityIds;
            } else {
                return new ArrayList<>();
            }
        }
    }
    
    public class MoveActivityIdContainer {
        
        protected List<String> activityIds;
        protected String singleActivityId;
        protected String moveToActivityId;
        protected List<String> moveToActivityIds;
        protected boolean moveToParentProcess;
        protected boolean moveToSubProcessInstance;
        protected String callActivityId;
        
        public MoveActivityIdContainer(String singleActivityId, String moveToActivityId) {
            this.singleActivityId = singleActivityId;
            this.moveToActivityId = moveToActivityId;
        }
        
        public MoveActivityIdContainer(List<String> activityIds, String moveToActivityId) {
            this.activityIds = activityIds;
            this.moveToActivityId = moveToActivityId;
        }
        
        public MoveActivityIdContainer(String singleActivityId, List<String> moveToActivityIds) {
            this.singleActivityId = singleActivityId;
            this.moveToActivityIds = moveToActivityIds;
        }
        
        public List<String> getActivityIds() {
            if (singleActivityId != null) {
                return Collections.singletonList(singleActivityId);
            } else if (activityIds != null) {
                return activityIds;
            } else {
                return new ArrayList<>();
            }
        }

        public List<String> getMoveToActivityIds() {
            if (moveToActivityId != null) {
                return Collections.singletonList(moveToActivityId);
            } else if (moveToActivityIds != null) {
                return moveToActivityIds;
            } else {
                return new ArrayList<>();
            }
        }

        public boolean isMoveToParentProcess() {
            return moveToParentProcess;
        }

        public void setMoveToParentProcess(boolean moveToParentProcess) {
            this.moveToParentProcess = moveToParentProcess;
        }

        public boolean isMoveToSubProcessInstance() {
            return moveToSubProcessInstance;
        }

        public void setMoveToSubProcessInstance(boolean moveToSubProcessInstance) {
            this.moveToSubProcessInstance = moveToSubProcessInstance;
        }

        public String getCallActivityId() {
            return callActivityId;
        }

        public void setCallActivityId(String callActivityId) {
            this.callActivityId = callActivityId;
        }
    }
}
