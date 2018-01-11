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

package org.flowable.engine.impl.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl.MoveActivityIdContainer;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl.MoveExecutionIdContainer;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class ChangeActivityStateCmd implements Command<Void> {

    protected ChangeActivityStateBuilderImpl changeActivityStateBuilder;

    public ChangeActivityStateCmd(ChangeActivityStateBuilderImpl changeActivityStateBuilder) {
        this.changeActivityStateBuilder = changeActivityStateBuilder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (changeActivityStateBuilder.getMoveExecutionIdList().size() == 0 && changeActivityStateBuilder.getMoveActivityIdList().size() == 0) {
            throw new FlowableIllegalArgumentException("No move execution or activity ids provided");
            
        } else if (changeActivityStateBuilder.getMoveActivityIdList().size() > 0 && changeActivityStateBuilder.getProcessInstanceId() == null) {
            throw new FlowableIllegalArgumentException("Process instance id is required");
        }
        
        DynamicStateManager dynamicStateManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicStateManager();

        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = new ArrayList<>();
        if (changeActivityStateBuilder.getMoveExecutionIdList().size() > 0) {
            for (MoveExecutionIdContainer executionContainer : changeActivityStateBuilder.getMoveExecutionIdList()) {
                List<ExecutionEntity> executions = new ArrayList<>();
                for (String executionId : executionContainer.getExecutionIds()) {
                    ExecutionEntity execution = dynamicStateManager.resolveActiveExecution(executionId, commandContext);
                    executions.add(execution);
                }
                
                moveExecutionEntityContainerList.add(new MoveExecutionEntityContainer(executions, executionContainer.getMoveToActivityIds()));
            }
        }
            
        if (changeActivityStateBuilder.getMoveActivityIdList().size() > 0) {
            for (MoveActivityIdContainer activityContainer : changeActivityStateBuilder.getMoveActivityIdList()) {
                List<ExecutionEntity> executions = new ArrayList<>();
                for (String activityId : activityContainer.getActivityIds()) {
                    ExecutionEntity execution = dynamicStateManager.resolveActiveExecution(changeActivityStateBuilder.getProcessInstanceId(), activityId, commandContext);
                    executions.add(execution);
                }
                
                moveExecutionEntityContainerList.add(new MoveExecutionEntityContainer(executions, activityContainer.getMoveToActivityIds()));
            }
        }

        dynamicStateManager.moveExecutionState(moveExecutionEntityContainerList, changeActivityStateBuilder.getProcessVariables(), 
                        changeActivityStateBuilder.getLocalVariables(), commandContext);

        return null;
    }
    
    public class MoveExecutionEntityContainer {
        
        protected List<ExecutionEntity> executions;
        protected List<String> moveToActivityIds;
        protected Map<String, ExecutionEntity> continueParentExecutionMap = new HashMap<>();
        protected Map<String, FlowElement> moveToFlowElementMap = new HashMap<>();
        protected Map<String, List<SubProcess>> subProcessesToCreateMap = new HashMap<>();
        protected Map<String, ExecutionEntity> newSubProcessChildExecutionMap = new HashMap<>();
        
        public MoveExecutionEntityContainer(List<ExecutionEntity> executions, List<String> moveToActivityIds) {
            this.executions = executions;
            this.moveToActivityIds = moveToActivityIds;
        }
        
        public List<ExecutionEntity> getExecutions() {
            return executions;    
        }
        
        public List<String> getMoveToActivityIds() {
            return moveToActivityIds;
        }
        
        public void addContinueParentExecution(String executionId, ExecutionEntity continueParentExecution) {
            continueParentExecutionMap.put(executionId, continueParentExecution);
        }
        
        public ExecutionEntity getContinueParentExecution(String executionId) {
            return continueParentExecutionMap.get(executionId);
        }
        
        public void addMoveToFlowElement(String activityId, FlowElement flowElement) {
            moveToFlowElementMap.put(activityId, flowElement);
        }
        
        public FlowElement getMoveToFlowElement(String activityId) {
            return moveToFlowElementMap.get(activityId);
        }
        
        public Collection<FlowElement> getMoveToFlowElements() {
            return moveToFlowElementMap.values();
        }
        
        public void addSubProcessToCreate(String activityId, SubProcess subProcess) {
            List<SubProcess> subProcesses = null;
            if (subProcessesToCreateMap.containsKey(activityId)) {
                subProcesses = subProcessesToCreateMap.get(activityId);
            } else {
                subProcesses = new ArrayList<>();
            }
            
            subProcesses.add(0, subProcess);
            subProcessesToCreateMap.put(activityId, subProcesses);
        }
        
        public Map<String, List<SubProcess>> getSubProcessesToCreateMap() {
            return subProcessesToCreateMap;
        }
        
        public void addNewSubProcessChildExecution(String subProcessId, ExecutionEntity childExecution) {
            newSubProcessChildExecutionMap.put(subProcessId, childExecution);
        }
        
        public ExecutionEntity getNewSubProcessChildExecution(String subProcessId) {
            return newSubProcessChildExecutionMap.get(subProcessId);
        }
    }

}
