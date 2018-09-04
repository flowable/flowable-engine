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

package org.flowable.engine.impl.dynamic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;

/**
 * @author Tijs Rademakers
 */
public class DefaultDynamicStateManager extends AbstractDynamicStateManager implements DynamicStateManager {

    @Override
    public void moveExecutionState(List<MoveExecutionEntityContainer> moveExecutionEntityContainerList, Map<String, Object> processVariables, Map<String, Map<String, Object>> localVariables, CommandContext commandContext) {
        doMoveExecutionState(moveExecutionEntityContainerList, processVariables, localVariables, Optional.empty(), commandContext);
    }

    protected List<ExecutionEntity> createEmbeddedSubProcessExecutions(Collection<FlowElement> moveToFlowElements, List<ExecutionEntity> currentExecutions, MoveExecutionEntityContainer moveExecutionContainer, Optional<String> migrateToProcessDefinitionId, CommandContext commandContext) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        // Resolve the sub process elements that need to be created for each move to flow element
        for (FlowElement flowElement : moveToFlowElements) {

            SubProcess subProcess = flowElement.getSubProcess();
            while (subProcess != null) {
                if (!hasSubProcessId(subProcess.getId(), currentExecutions)) {
                    moveExecutionContainer.addSubProcessToCreate(flowElement.getId(), subProcess);
                }
                subProcess = subProcess.getSubProcess();
            }
        }

        // The default parent execution is retrieved from the match with the first source execution
        ExecutionEntity defaultContinueParentExecution = moveExecutionContainer.getContinueParentExecution(currentExecutions.get(0).getId());

        for (String activityId : moveExecutionContainer.getSubProcessesToCreateMap().keySet()) {
            List<SubProcess> subProcessesToCreate = moveExecutionContainer.getSubProcessesToCreateMap().get(activityId);
            for (SubProcess subProcess : subProcessesToCreate) {

                // Check if sub process execution was not already created
                if (moveExecutionContainer.getNewSubProcessChildExecution(subProcess.getId()) == null) {
                    FlowElement startElement = getStartFlowElement(subProcess);

                    if (startElement == null) {
                        throw new FlowableException("No initial activity found for subprocess " + subProcess.getId());
                    }

                    ExecutionEntity subProcessExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
                    subProcessExecution.setCurrentFlowElement(subProcess);
                    subProcessExecution.setScope(true);

                    FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher();
                    if (eventDispatcher.isEnabled()) {
                        eventDispatcher.dispatchEvent(
                            FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_STARTED, subProcess.getId(), subProcess.getName(), subProcessExecution.getId(),
                                subProcessExecution.getProcessInstanceId(), subProcessExecution.getProcessDefinitionId(), subProcess));
                    }

                    subProcessExecution.setVariablesLocal(processDataObjects(subProcess.getDataObjects()));

                    CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(subProcessExecution);

                    List<BoundaryEvent> boundaryEvents = subProcess.getBoundaryEvents();
                    if (CollectionUtil.isNotEmpty(boundaryEvents)) {
                        executeBoundaryEvents(boundaryEvents, subProcessExecution);
                    }

                    ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
                    processInstanceHelper.processAvailableEventSubProcesses(subProcessExecution, subProcess, commandContext);

                    ExecutionEntity startSubProcessExecution = CommandContextUtil.getExecutionEntityManager(commandContext)
                        .createChildExecution(subProcessExecution);
                    startSubProcessExecution.setCurrentFlowElement(startElement);

                    moveExecutionContainer.addNewSubProcessChildExecution(subProcess.getId(), startSubProcessExecution);
                }
            }
        }

        List<ExecutionEntity> newChildExecutions = new ArrayList<>();
        for (FlowElement newFlowElement : moveToFlowElements) {
            ExecutionEntity newChildExecution = null;

            // Check if a sub process child execution was created for this move to flow element, otherwise use the default continue parent execution
            if (moveExecutionContainer.getSubProcessesToCreateMap().containsKey(newFlowElement.getId())) {
                newChildExecution = moveExecutionContainer.getNewSubProcessChildExecution(moveExecutionContainer.getSubProcessesToCreateMap().get(newFlowElement.getId()).get(0).getId());
            } else {
                newChildExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
            }

            newChildExecution.setCurrentFlowElement(newFlowElement);

            if (newFlowElement instanceof CallActivity) {
                CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(newChildExecution);

                FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher();
                if (eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(
                        FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_STARTED, newFlowElement.getId(), newFlowElement.getName(), newChildExecution.getId(),
                            newChildExecution.getProcessInstanceId(), newChildExecution.getProcessDefinitionId(), newFlowElement));
                }
            }

            newChildExecutions.add(newChildExecution);

            // Parallel gateway joins needs each incoming execution to enter the gateway naturally as it checks the number of executions to be able to progress/continue
            // If we have multiple executions going into a gateway, usually into a gateway join using xxxToSingleActivityId
            if (newFlowElement instanceof Gateway) {
                //Skip one that was already added
                currentExecutions.stream().skip(1).forEach(e -> {
                    ExecutionEntity childExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
                    childExecution.setCurrentFlowElement(newFlowElement);
                    newChildExecutions.add(childExecution);
                });
            }

        }

        return newChildExecutions;
    }
}
