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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.impl.dynamic.MoveExecutionEntityContainer;
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
                    List<ExecutionEntity> activityExecutions = dynamicStateManager.resolveActiveExecutions(changeActivityStateBuilder.getProcessInstanceId(), activityId, commandContext);
                    if (!activityExecutions.isEmpty()) {
                        ExecutionEntity execution = activityExecutions.get(0);

                        //Check if the activity is inside a multiInstance subProcess
                        boolean insideMultiInstance = false;
                        FlowElementsContainer parentContainer = execution.getCurrentFlowElement().getParentContainer();
                        while (!(parentContainer instanceof Process)) {
                            MultiInstanceLoopCharacteristics loopCharacteristics = ((Activity) parentContainer).getLoopCharacteristics();
                            if (loopCharacteristics != null && !loopCharacteristics.isSequential()) {

                                insideMultiInstance = true;
                                break;
                            }
                            parentContainer = ((Activity) parentContainer).getParentContainer();
                        }
                        //If inside a multiInstance, we create one container for each execution
                        if (insideMultiInstance) {
                            Stream<ExecutionEntity> executionsStream = activityExecutions.stream();
                            //If the source activity is already a multiInstance, we need to move only the parents (filter)
                            if (execution.isMultiInstanceRoot()) {
                                executionsStream = executionsStream.filter(ExecutionEntity::isMultiInstanceRoot);
                            }
                            executionsStream.forEach(e -> moveExecutionEntityContainerList.add(createMoveExecutionContainer(activityContainer, Collections.singletonList(e))));
                        } else {
                            executions.add(execution);
                        }
                    }
                }

                if (!executions.isEmpty()) {
                    moveExecutionEntityContainerList.add(createMoveExecutionContainer(activityContainer, executions));
                }
            }
        }

        dynamicStateManager.moveExecutionState(moveExecutionEntityContainerList, changeActivityStateBuilder.getProcessVariables(),
                        changeActivityStateBuilder.getLocalVariables(), commandContext);

        return null;
    }

    protected static MoveExecutionEntityContainer createMoveExecutionContainer(MoveActivityIdContainer activityContainer, List<ExecutionEntity> executions) {
        MoveExecutionEntityContainer moveExecutionEntityContainer = new MoveExecutionEntityContainer(executions, activityContainer.getMoveToActivityIds());

        if (activityContainer.isMoveToParentProcess()) {
            ExecutionEntity processInstanceExecution = executions.get(0).getProcessInstance();
            ExecutionEntity superExecution = processInstanceExecution.getSuperExecution();
            if (superExecution == null) {
                throw new FlowableException("No parent process found for execution with activity id " + executions.get(0).getCurrentActivityId());
            }

            moveExecutionEntityContainer.setMoveToParentProcess(true);
            moveExecutionEntityContainer.setSuperExecution(superExecution);

        } else if (activityContainer.isMoveToSubProcessInstance()) {
            moveExecutionEntityContainer.setMoveToSubProcessInstance(true);
            moveExecutionEntityContainer.setCallActivityId(activityContainer.getCallActivityId());
        }
        return moveExecutionEntityContainer;
    }
}
