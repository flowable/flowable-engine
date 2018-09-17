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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.impl.util.TimerUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dennis
 */
public abstract class AbstractDynamicStateManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractDynamicStateManager.class);

    public static ExecutionEntity resolveActiveExecution(String executionId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity execution = executionEntityManager.findById(executionId);

        if (execution == null) {
            throw new FlowableException("Execution could not be found with id " + executionId);
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            throw new FlowableException("Flowable 5 process definitions are not supported");
        }

        return execution;
    }

    public static List<ExecutionEntity> resolveActiveExecutions(String processInstanceId, String activityId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processExecution = executionEntityManager.findById(processInstanceId);

        if (processExecution == null) {
            throw new FlowableException("Execution could not be found with id " + processInstanceId);
        }

        if (!processExecution.isProcessInstanceType()) {
            throw new FlowableException("Execution is not a process instance type execution for id " + processInstanceId);
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, processExecution.getProcessDefinitionId())) {
            throw new FlowableException("Flowable 5 process definitions are not supported");
        }

        List<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processExecution.getId());

        //For multi instance executions, the parent should have the lower start time, we make sure to order the collection since it may not be guaranteed in the query result
        List<ExecutionEntity> executions = childExecutions.stream()
            .filter(e -> e.getCurrentActivityId() != null)
            .filter(e -> e.getCurrentActivityId().equals(activityId))
            .sorted(ExecutionEntity.EXECUTION_ENTITY_START_TIME_ASC_COMPARATOR)
            .collect(Collectors.toList());

        if (executions.isEmpty()) {
            throw new FlowableException("Active execution could not be found with activity id " + activityId);
        }
        return executions;
    }

    public List<MoveExecutionEntityContainer> resolveMoveExecutionEntityContainers(ChangeActivityStateBuilderImpl changeActivityStateBuilder, CommandContext commandContext) {
        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = new ArrayList<>();
        if (changeActivityStateBuilder.getMoveExecutionIdList().size() > 0) {
            for (ChangeActivityStateBuilderImpl.MoveExecutionIdContainer executionContainer : changeActivityStateBuilder.getMoveExecutionIdList()) {
                //Executions belonging to the same parent should move together - i.e multipleExecution to single activity
                Map<String, List<ExecutionEntity>> executionsByParent = new HashMap<>();
                for (String executionId : executionContainer.getExecutionIds()) {
                    ExecutionEntity execution = resolveActiveExecution(executionId, commandContext);
                    addExecutionToExecutionListsByParentIdMap(executionsByParent, execution.getParentId(), execution);
                }
                executionsByParent.values().forEach(l -> moveExecutionEntityContainerList.add(new MoveExecutionEntityContainer(l, executionContainer.getMoveToActivityIds())));
            }
        }

        if (changeActivityStateBuilder.getMoveActivityIdList().size() > 0) {
            for (ChangeActivityStateBuilderImpl.MoveActivityIdContainer activityContainer : changeActivityStateBuilder.getMoveActivityIdList()) {
                Map<String, List<ExecutionEntity>> activitiesExecutionsByMultiInstanceParentId = new HashMap<>();
                List<ExecutionEntity> activitiesExecutionsNotInMultiInstanceParent = new ArrayList<>();

                for (String activityId : activityContainer.getActivityIds()) {
                    List<ExecutionEntity> activityExecutions = resolveActiveExecutions(changeActivityStateBuilder.getProcessInstanceId(), activityId, commandContext);
                    if (!activityExecutions.isEmpty()) {
                        ExecutionEntity execution = activityExecutions.get(0);
                        //If inside a multiInstance, we create one container for each execution
                        if (isExecutionInsideMultiInstance(execution)) {
                            //We group by the parentId (executions belonging to the same parent execution instance
                            // i.e. gateways nested in MultiInstance subprocesses, need to be in the same move container)
                            Stream<ExecutionEntity> executionEntitiesStream = activityExecutions.stream();

                            //If the source activity is already a multiInstance, we need to move only the parents (filter)
                            if (execution.isMultiInstanceRoot()) {
                                executionEntitiesStream = executionEntitiesStream.filter(ExecutionEntity::isMultiInstanceRoot);
                            }

                            executionEntitiesStream.forEach(e -> {
                                String parentId = e.isMultiInstanceRoot() ? e.getId() : e.getParentId();
                                addExecutionToExecutionListsByParentIdMap(activitiesExecutionsByMultiInstanceParentId, parentId, e);
                            });
                        } else {
                            activitiesExecutionsNotInMultiInstanceParent.add(execution);
                        }
                    }
                }

                //Create a move container for each execution group (executionList)
                Stream.concat(activitiesExecutionsByMultiInstanceParentId.values().stream(), Stream.of(activitiesExecutionsNotInMultiInstanceParent))
                    .filter(executions -> executions != null && !executions.isEmpty())
                    .forEach(executions -> moveExecutionEntityContainerList.add(createMoveExecutionEntityContainer(activityContainer, executions)));
            }
        }
        return moveExecutionEntityContainerList;

    }

    private static void addExecutionToExecutionListsByParentIdMap(Map<String, List<ExecutionEntity>> executionListsByParentIdMap, String parentId, ExecutionEntity executionEntity) {
        List<ExecutionEntity> executionEntities = executionListsByParentIdMap.get(parentId);
        if (executionEntities == null) {
            executionEntities = new ArrayList<>();
            executionListsByParentIdMap.put(parentId, executionEntities);
        }
        executionEntities.add(executionEntity);
    }

    protected static boolean isExecutionInsideMultiInstance(ExecutionEntity execution) {
        FlowElementsContainer parentContainer = execution.getCurrentFlowElement().getParentContainer();
        while (!(parentContainer instanceof Process)) {
            MultiInstanceLoopCharacteristics loopCharacteristics = ((Activity) parentContainer).getLoopCharacteristics();
            if (loopCharacteristics != null) {
                return true;
            }
            parentContainer = ((Activity) parentContainer).getParentContainer();
        }
        return false;
    }

    protected static MoveExecutionEntityContainer createMoveExecutionEntityContainer(ChangeActivityStateBuilderImpl.MoveActivityIdContainer activityContainer, List<ExecutionEntity> executions) {
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

    protected void doMoveExecutionState(List<MoveExecutionEntityContainer> moveExecutionEntityContainerList, Map<String, Object> processVariables, Map<String, Map<String, Object>> localVariables, Optional<String> migrateToProcessDefinitionId, CommandContext commandContext) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        for (MoveExecutionEntityContainer moveExecutionContainer : moveExecutionEntityContainerList) {
            // Prepare/complete each moveExecutionEntityContainer to make the move for each target activity (get next flowElement, and other flags)
            for (String activityId : moveExecutionContainer.getMoveToActivityIds()) {
                prepareMoveExecutionEntityContainer(moveExecutionContainer, activityId, migrateToProcessDefinitionId, commandContext);
            }

            // Action the moves (changeState)
            if (moveExecutionContainer.isMoveToParentProcess()) {
                String processInstanceId = moveExecutionContainer.getExecutions().get(0).getProcessInstanceId();
                String deleteReason = "Change activity to parent process activity ids: " + printFlowElementIds(moveExecutionContainer.getMoveToFlowElements());
                safeDeleteSubProcessInstance(processInstanceId, moveExecutionContainer.getExecutions(), deleteReason, commandContext);
            }

            List<ExecutionEntity> currentExecutions;
            if (moveExecutionContainer.isMoveToParentProcess()) {
                currentExecutions = Collections.singletonList(moveExecutionContainer.getSuperExecution());
            } else {
                currentExecutions = moveExecutionContainer.getExecutions();
            }

            Collection<FlowElement> moveToFlowElements;
            if (moveExecutionContainer.isMoveToSubProcessInstance()) {
                moveToFlowElements = Collections.singletonList(moveExecutionContainer.getCallActivity());
            } else {
                moveToFlowElements = moveExecutionContainer.getMoveToFlowElements();
            }

            String flowElementIdsLine = printFlowElementIds(moveToFlowElements);
            Collection<String> executionIdsNotToDelete = new HashSet<>();
            for (ExecutionEntity execution : currentExecutions) {
                executionIdsNotToDelete.add(execution.getId());

                executionEntityManager.deleteChildExecutions(execution, "Change parent activity to " + flowElementIdsLine, true);
                if (!moveExecutionContainer.isDirectExecutionMigration()) {
                    executionEntityManager.deleteExecutionAndRelatedData(execution, "Change activity to " + flowElementIdsLine, true, execution.getCurrentFlowElement());
                }

                // Make sure we are not moving the root execution
                if (execution.getParentId() == null) {
                    throw new FlowableException("Execution has no parent execution " + execution.getParentId());
                }

                // Delete the parent executions for each current execution when the move to activity id has the same subProcess scope
                ExecutionEntity continueParentExecution = deleteParentExecutions(execution.getParentId(), moveToFlowElements, executionIdsNotToDelete, commandContext);
                moveExecutionContainer.addContinueParentExecution(execution.getId(), continueParentExecution);
            }

            List<ExecutionEntity> newChildExecutions = createEmbeddedSubProcessExecutions(moveToFlowElements, currentExecutions, moveExecutionContainer, migrateToProcessDefinitionId, commandContext);

            ExecutionEntity defaultContinueParentExecution;
            if (moveExecutionContainer.isMoveToSubProcessInstance()) {
                CallActivity callActivity = moveExecutionContainer.getCallActivity();
                Process subProcess = moveExecutionContainer.getSubProcessModel().getProcessById(callActivity.getCalledElement());
                ExecutionEntity subProcessChildExecution = createSubProcessInstance(callActivity, moveExecutionContainer.getSubProcessDefinition(), newChildExecutions.get(0), subProcess.getInitialFlowElement(), commandContext);
                List<ExecutionEntity> currentSubProcessExecutions = Collections.singletonList(subProcessChildExecution);

                MoveExecutionEntityContainer subProcessMoveExecutionEntityContainer = new MoveExecutionEntityContainer(currentSubProcessExecutions, moveExecutionContainer.getMoveToActivityIds());
                subProcessMoveExecutionEntityContainer.addMoveToFlowElement(callActivity.getId(), callActivity);

                ExecutionEntity continueParentExecution = deleteParentExecutions(subProcessChildExecution.getParentId(), moveExecutionContainer.getMoveToFlowElements(), commandContext);
                subProcessMoveExecutionEntityContainer.addContinueParentExecution(subProcessChildExecution.getId(), continueParentExecution);

                executionEntityManager.deleteExecutionAndRelatedData(subProcessChildExecution, "Change activity to " + printFlowElementIds(moveToFlowElements));

                newChildExecutions = createEmbeddedSubProcessExecutions(moveExecutionContainer.getMoveToFlowElements(), currentSubProcessExecutions, subProcessMoveExecutionEntityContainer, migrateToProcessDefinitionId, commandContext);

                defaultContinueParentExecution = newChildExecutions.get(0);

            } else {

                // The default parent execution is retrieved from the match with the first source execution
                defaultContinueParentExecution = moveExecutionContainer.getContinueParentExecution(currentExecutions.get(0).getId());
            }

            if (processVariables != null && processVariables.size() > 0) {
                ExecutionEntity processInstanceExecution = defaultContinueParentExecution.getProcessInstance();
                processInstanceExecution.setVariables(processVariables);
            }

            if (localVariables != null && localVariables.size() > 0) {
                Iterator<ExecutionEntity> newChildExecutionsIter = newChildExecutions.iterator();
                while (newChildExecutionsIter.hasNext()) {
                    ExecutionEntity execution = newChildExecutionsIter.next();
                    while (execution != null) {
                        if (execution.getActivityId() != null) {
                            Map<String, Object> localVars = localVariables.get(execution.getActivityId());
                            if (localVars != null) {
                                execution.setVariablesLocal(localVars);
                            }
                        }
                        execution = execution.getParent();
                    }
                }
            }

            //TODO WIP ... Its always the same type of activity
            if (!moveExecutionContainer.isDirectExecutionMigration()) {
                for (ExecutionEntity newChildExecution : newChildExecutions) {
                    CommandContextUtil.getAgenda().planContinueProcessOperation(newChildExecution);
                }
            }
        }
    }

    protected void prepareMoveExecutionEntityContainer(MoveExecutionEntityContainer moveExecutionContainer, String activityId, Optional<String> migrateToProcessDefinitionId, CommandContext commandContext) {

        FlowElement currentFlowElement = null;
        FlowElement newFlowElement;
        String currentActivityId = null;
        if (moveExecutionContainer.isMoveToParentProcess()) {
            String parentProcessDefinitionId = moveExecutionContainer.getSuperExecution().getProcessDefinitionId();
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentProcessDefinitionId);
            if (migrateToProcessDefinitionId.isPresent()) {
                currentActivityId = moveExecutionContainer.getExecutions().get(0).getCurrentActivityId();
                currentFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, currentActivityId);
                bpmnModel = ProcessDefinitionUtil.getBpmnModel(migrateToProcessDefinitionId.get());
            }
            newFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, activityId);
        } else if (moveExecutionContainer.isMoveToSubProcessInstance()) {
            //The subProcess model is defined in the callActivity of the current processDefinition or the migrateProcessDefinition if defined
            ExecutionEntity firstExecution = moveExecutionContainer.getExecutions().get(0);
            String processDefinitionIdOfCallActivity = firstExecution.getProcessDefinitionId();
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(firstExecution.getProcessDefinitionId());
            if (migrateToProcessDefinitionId.isPresent()) {
                currentActivityId = firstExecution.getCurrentActivityId();
                currentFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, currentActivityId);
                bpmnModel = ProcessDefinitionUtil.getBpmnModel(migrateToProcessDefinitionId.get());
            }

            FlowElement callActivityElement = bpmnModel.getFlowElement(moveExecutionContainer.getCallActivityId());
            if (callActivityElement == null) {
                throw new FlowableException("Call activity could not be found in process definition for id " + activityId);
            }

            CallActivity callActivity = (CallActivity) callActivityElement;
            moveExecutionContainer.setCallActivity(callActivity);

            String calledProcessDefinitionKey = callActivity.getCalledElement();
            ProcessDefinition callActivityProcessDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionIdOfCallActivity);
            String deploymentId = callActivityProcessDefinition.getDeploymentId();
            String tenantId = callActivityProcessDefinition.getTenantId();

            ProcessDefinition subProcessDefinition = null;
            if (callActivity.isSameDeployment()) {
                ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
                if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                    subProcessDefinition = processDefinitionEntityManager.findProcessDefinitionByDeploymentAndKey(deploymentId, calledProcessDefinitionKey);
                } else {
                    subProcessDefinition = processDefinitionEntityManager.findProcessDefinitionByDeploymentAndKeyAndTenantId(deploymentId, calledProcessDefinitionKey, tenantId);
                }
            }

            if (subProcessDefinition == null) {
                if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                    subProcessDefinition = CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager().findDeployedLatestProcessDefinitionByKey(calledProcessDefinitionKey);
                } else {
                    subProcessDefinition = CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager().findDeployedLatestProcessDefinitionByKeyAndTenantId(calledProcessDefinitionKey, tenantId);
                }
            }

            BpmnModel subProcessModel = ProcessDefinitionUtil.getBpmnModel(subProcessDefinition.getId());
            moveExecutionContainer.setSubProcessDefinition(subProcessDefinition);
            moveExecutionContainer.setSubProcessModel(subProcessModel);

            newFlowElement = resolveFlowElementFromBpmnModel(subProcessModel, activityId);
        } else {
            // Get first execution to get process definition id
            ExecutionEntity firstExecution = moveExecutionContainer.getExecutions().get(0);
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(firstExecution.getProcessDefinitionId());
            if (migrateToProcessDefinitionId.isPresent()) {
                currentActivityId = moveExecutionContainer.getExecutions().get(0).getCurrentActivityId();
                currentFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, currentActivityId);
                bpmnModel = ProcessDefinitionUtil.getBpmnModel(migrateToProcessDefinitionId.get());
            }
            newFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, activityId);
        }

        moveExecutionContainer.addMoveToFlowElement(activityId, newFlowElement);

        if (migrateToProcessDefinitionId.isPresent() && isDirectMigration(currentFlowElement, newFlowElement)) {
            moveExecutionContainer.setDirectExecutionMigration(true);
        }
    }

    protected static FlowElement resolveFlowElementFromBpmnModel(BpmnModel bpmnModel, String activityId) {
        FlowElement flowElement = bpmnModel.getFlowElement(activityId);
        if (flowElement == null) {
            throw new FlowableException("Activity could not be found in process definition for id " + activityId);
        }
        return flowElement;
    }

    //TODO WIP --- This should be only on ProcessInstanceMigration side
    protected boolean isDirectMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        return currentFlowElement instanceof UserTask && newFlowElement instanceof UserTask ||
            currentFlowElement instanceof ReceiveTask && newFlowElement instanceof ReceiveTask;
    }

    protected static void safeDeleteSubProcessInstance(String processInstanceId, List<ExecutionEntity> executionsPool, String deleteReason, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        //Confirm that all the subProcessExecutions are in the executionsPool
        List<ExecutionEntity> subProcessExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);
        HashSet<String> executionIdsToMove = executionsPool.stream().map(ExecutionEntity::getId).collect(Collectors.toCollection(HashSet::new));
        Optional<ExecutionEntity> notIncludedExecution = subProcessExecutions.stream().filter(e -> !executionIdsToMove.contains(e.getId())).findAny();
        if (notIncludedExecution.isPresent()) {
            throw new FlowableException("Execution of sub process instance is not moved " + notIncludedExecution.get().getId());
        }

        // delete the sub process instance
        executionEntityManager.deleteProcessInstance(processInstanceId, deleteReason, true);
    }

    protected ExecutionEntity deleteParentExecutions(String parentExecutionId, Collection<FlowElement> moveToFlowElements, CommandContext commandContext) {
        return deleteParentExecutions(parentExecutionId, moveToFlowElements, null, commandContext);
    }

    protected ExecutionEntity deleteParentExecutions(String parentExecutionId, Collection<FlowElement> moveToFlowElements, Collection<String> executionIdsNotToDelete, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        ExecutionEntity parentExecution = executionEntityManager.findById(parentExecutionId);
        if (parentExecution.getCurrentFlowElement() instanceof SubProcess) {
            SubProcess parentSubProcess = (SubProcess) parentExecution.getCurrentFlowElement();
            if (!isSubProcessUsedInNewFlowElements(parentSubProcess.getId(), moveToFlowElements)) {
                ExecutionEntity toDeleteParentExecution = resolveParentExecutionToDelete(parentExecution, moveToFlowElements);
                ExecutionEntity finalDeleteExecution = null;
                if (toDeleteParentExecution != null) {
                    finalDeleteExecution = toDeleteParentExecution;
                } else {
                    finalDeleteExecution = parentExecution;
                }

                parentExecution = finalDeleteExecution.getParent();

                String flowElementIdsLine = printFlowElementIds(moveToFlowElements);
                executionEntityManager.deleteChildExecutions(finalDeleteExecution, executionIdsNotToDelete, null, "Change activity to " + flowElementIdsLine, true, null);
                executionEntityManager.deleteExecutionAndRelatedData(finalDeleteExecution, "Change activity to " + flowElementIdsLine, true, finalDeleteExecution.getCurrentFlowElement());
            }
        }

        return parentExecution;
    }

    protected boolean isSubProcessUsedInNewFlowElements(String subProcessId, Collection<FlowElement> moveToFlowElements) {

        Optional<SubProcess> isUsed = moveToFlowElements.stream()
            .map(FlowElement::getSubProcess)
            .filter(Objects::nonNull)
            .filter(elementSubProcess -> elementSubProcess.getId().equals(subProcessId))
            .findAny();

        return isUsed.isPresent();
    }

    protected ExecutionEntity resolveParentExecutionToDelete(ExecutionEntity execution, Collection<FlowElement> moveToFlowElements) {
        ExecutionEntity parentExecution = execution.getParent();

        if (parentExecution.isProcessInstanceType()) {
            return null;
        }

        if (!isSubProcessUsedInNewFlowElements(parentExecution.getActivityId(), moveToFlowElements)) {
            ExecutionEntity subProcessParentExecution = resolveParentExecutionToDelete(parentExecution, moveToFlowElements);
            if (subProcessParentExecution != null) {
                return subProcessParentExecution;
            } else {
                return parentExecution;
            }
        }

        return null;
    }

    protected List<ExecutionEntity> createEmbeddedSubProcessExecutions(Collection<FlowElement> moveToFlowElements, List<ExecutionEntity> currentExecutions, MoveExecutionEntityContainer moveExecutionContainer, Optional<String> migrateToProcessDefinitionId, CommandContext commandContext) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        // Resolve the sub process elements that need to be created for each move to flow element
        HashMap<String, SubProcess> subProcessesToCreate = new HashMap<>();
        for (FlowElement flowElement : moveToFlowElements) {
            SubProcess subProcess = flowElement.getSubProcess();
            while (subProcess != null) {
                if (!isParentSubProcessOfAnyExecution(subProcess.getId(), currentExecutions)) {
                    subProcessesToCreate.put(subProcess.getId(), subProcess);
                }
                subProcess = subProcess.getSubProcess();
            }
        }

        // The default parent execution is retrieved from the match with the first source execution
        ExecutionEntity defaultContinueParentExecution = moveExecutionContainer.getContinueParentExecution(currentExecutions.get(0).getId());

        //Build the subProcess hierarchy
        HashMap<String, ExecutionEntity> createdSubProcess = new HashMap<>();
        for (SubProcess subProcess : subProcessesToCreate.values()) {
            if (!createdSubProcess.containsKey(subProcess.getId())) {
                ExecutionEntity embeddedSubProcess = createEmbeddedSubProcessHierarchy(subProcess, defaultContinueParentExecution, createdSubProcess, subProcessesToCreate, commandContext);
                createdSubProcess.put(subProcess.getId(), embeddedSubProcess);
            }
        }

        //Adds the execution (leaf) to the subProcess
        List<ExecutionEntity> newChildExecutions = new ArrayList<>();
        for (FlowElement newFlowElement : moveToFlowElements) {
            ExecutionEntity parentExecution;
            if (newFlowElement.getSubProcess() != null && createdSubProcess.containsKey(newFlowElement.getSubProcess().getId())) {
                parentExecution = createdSubProcess.get(newFlowElement.getSubProcess().getId());
            } else {
                parentExecution = defaultContinueParentExecution;
            }

            ExecutionEntity newChildExecution;
            if (moveExecutionContainer.isDirectExecutionMigration()) {
                newChildExecution = migrateExecutionEntity(parentExecution, currentExecutions.get(0), newFlowElement, commandContext);
            } else {
                newChildExecution = executionEntityManager.createChildExecution(parentExecution);
                newChildExecution.setCurrentFlowElement(newFlowElement);
            }

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
                    //TODO WIP can be parentExecution insteas?
                    ExecutionEntity childExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
                    childExecution.setCurrentFlowElement(newFlowElement);
                    newChildExecutions.add(childExecution);
                });
            }
        }

        return newChildExecutions;
    }

    protected boolean isParentSubProcessOfAnyExecution(String subProcessId, List<ExecutionEntity> currentExecutions) {
        for (ExecutionEntity execution : currentExecutions) {
            FlowElement executionElement = execution.getCurrentFlowElement();

            while (executionElement.getSubProcess() != null) {
                String execElemSubProcessId = executionElement.getSubProcess().getId();
                if (execElemSubProcessId != null && execElemSubProcessId.equals(subProcessId)) {
                    return true;
                }
                executionElement = executionElement.getSubProcess();
            }
        }
        return false;
    }

    protected List<FlowElement> getFlowElementsInSubProcess(SubProcess subProcess, Collection<FlowElement> flowElements) {
        return flowElements.stream()
            .filter(e -> e.getSubProcess() != null)
            .filter(e -> e.getSubProcess().getId().equals(subProcess.getId()))
            .collect(Collectors.toList());

    }

    protected static ExecutionEntity createEmbeddedSubProcessHierarchy(SubProcess subProcess, ExecutionEntity defaultParentExecution, HashMap<String, ExecutionEntity> createdSubProcessesCache, HashMap<String, SubProcess> subProcessesToCreate, CommandContext commandContext) {
        if (createdSubProcessesCache.containsKey(subProcess.getId())) {
            return createdSubProcessesCache.get(subProcess.getId());
        }
        //Create the parent, if needed
        ExecutionEntity parentSubProcess = defaultParentExecution;
        if (subProcess.getSubProcess() != null) {
            parentSubProcess = createEmbeddedSubProcessHierarchy(subProcess.getSubProcess(), defaultParentExecution, createdSubProcessesCache, subProcessesToCreate, commandContext);
            createdSubProcessesCache.put(subProcess.getSubProcess().getId(), parentSubProcess);
        }
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        ExecutionEntity subProcessExecution = executionEntityManager.createChildExecution(parentSubProcess);
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

        if (subProcess instanceof EventSubProcess) {
            processEventSubProcessForChangeState((EventSubProcess) subProcess, subProcessExecution, commandContext);
        }

        ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
        //Process containing child Event SubProcesses not contained in this creation hierarchy
        List<EventSubProcess> childEventSubProcesses = subProcess.findAllSubFlowElementInFlowMapOfType(EventSubProcess.class);
        childEventSubProcesses.stream()
            .filter(childEventSubProcess -> !subProcessesToCreate.containsKey(childEventSubProcess.getId()))
            .forEach(childEventSubProcess -> processInstanceHelper.processEventSubProcess(subProcessExecution, childEventSubProcess, commandContext));

        return subProcessExecution;
    }

    protected static Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
        Map<String, Object> variablesMap = new HashMap<>();
        // convert data objects to process variables
        if (dataObjects != null) {
            variablesMap = new HashMap<>(dataObjects.size());
            for (ValuedDataObject dataObject : dataObjects) {
                variablesMap.put(dataObject.getName(), dataObject.getValue());
            }
        }
        return variablesMap;
    }

    protected static void executeBoundaryEvents(Collection<BoundaryEvent> boundaryEvents, ExecutionEntity execution) {

        // The parent execution becomes a scope, and a child execution is created for each of the boundary events
        for (BoundaryEvent boundaryEvent : boundaryEvents) {

            if (CollectionUtil.isEmpty(boundaryEvent.getEventDefinitions())
                || (boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition)) {
                continue;
            }

            // A Child execution of the current execution is created to represent the boundary event being active
            ExecutionEntity childExecutionEntity = CommandContextUtil.getExecutionEntityManager().createChildExecution(execution);
            childExecutionEntity.setParentId(execution.getId());
            childExecutionEntity.setCurrentFlowElement(boundaryEvent);
            childExecutionEntity.setScope(false);

            ActivityBehavior boundaryEventBehavior = ((ActivityBehavior) boundaryEvent.getBehavior());
            LOGGER.debug("Executing boundary event activityBehavior {} with execution {}", boundaryEventBehavior.getClass(), childExecutionEntity.getId());
            boundaryEventBehavior.execute(childExecutionEntity);
        }
    }

    protected ExecutionEntity createSubProcessInstance(CallActivity callActivity, ProcessDefinition subProcessDefinition, ExecutionEntity parentExecution, FlowElement initialFlowElement, CommandContext commandContext) {

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        Process subProcess = ProcessDefinitionUtil.getProcess(subProcessDefinition.getId());
        if (subProcess == null) {
            throw new FlowableException("Cannot start a sub process instance. Process model " + subProcessDefinition.getName() + " (id = " + subProcessDefinition.getId() + ") could not be found");
        }

        String businessKey = null;

        if (!StringUtils.isEmpty(callActivity.getBusinessKey())) {
            Expression expression = expressionManager.createExpression(callActivity.getBusinessKey());
            businessKey = expression.getValue(parentExecution).toString();

        } else if (callActivity.isInheritBusinessKey()) {
            ExecutionEntity processInstance = executionEntityManager.findById(parentExecution.getProcessInstanceId());
            businessKey = processInstance.getBusinessKey();
        }

        ExecutionEntity subProcessInstance = executionEntityManager.createSubprocessInstance(subProcessDefinition, parentExecution, businessKey, initialFlowElement.getId());
        CommandContextUtil.getHistoryManager(commandContext).recordSubProcessInstanceStart(parentExecution, subProcessInstance);

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher.isEnabled()) {
            CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, subProcessInstance));
        }

        // process template-defined data objects
        subProcessInstance.setVariables(processDataObjects(subProcess.getDataObjects()));

        Map<String, Object> variables = new HashMap<>();

        if (callActivity.isInheritVariables()) {
            Map<String, Object> executionVariables = parentExecution.getVariables();
            for (Map.Entry<String, Object> entry : executionVariables.entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }
        }

        // copy process variables
        for (IOParameter ioParameter : callActivity.getInParameters()) {
            Object value = null;
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(ioParameter.getSourceExpression().trim());
                value = expression.getValue(parentExecution);

            } else {
                value = parentExecution.getVariable(ioParameter.getSource());
            }
            variables.put(ioParameter.getTarget(), value);
        }

        if (!variables.isEmpty()) {
            subProcessInstance.setVariables(variables);
        }

        if (eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, subProcessInstance));
        }

        // Create the first execution that will visit all the process definition elements
        ExecutionEntity subProcessInitialExecution = executionEntityManager.createChildExecution(subProcessInstance);
        subProcessInitialExecution.setCurrentFlowElement(initialFlowElement);

        CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(subProcessInitialExecution);

        return subProcessInitialExecution;
    }

    //TODO WIP ... SHOULD BE ONLY IN ProcessInstanceMigration
    protected ExecutionEntity migrateExecutionEntity(ExecutionEntity parentExecutionEntity, ExecutionEntity childExecution, FlowElement newFlowElement, CommandContext commandContext) {

        TaskService taskService = CommandContextUtil.getTaskService(commandContext);
        HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
        HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager = CommandContextUtil.getHistoricActivityInstanceEntityManager(commandContext);
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        childExecution.setParent(parentExecutionEntity);
        // manage the bidirectional parent-child relation
        parentExecutionEntity.addChildExecution(childExecution);
        childExecution.setProcessDefinitionId(parentExecutionEntity.getProcessDefinitionId());

        //Additional changes if the new activity Id doesn't match
        String oldActivityId = childExecution.getCurrentActivityId();
        if (childExecution.getCurrentActivityId() != newFlowElement.getId()) {
            ((ExecutionEntityImpl) childExecution).setActivityId(newFlowElement.getId());
        }

        // If we are moving a UserTask we need to update its processDefinition references
        if (newFlowElement instanceof UserTask) {
            TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery().executionId(childExecution.getId()).singleResult();
            task.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            task.setTaskDefinitionKey(newFlowElement.getId());
            task.setName(newFlowElement.getName());

            //Sync historic
            List<HistoricActivityInstanceEntity> historicActivityInstances = historicActivityInstanceEntityManager.findHistoricActivityInstancesByExecutionAndActivityId(childExecution.getId(), oldActivityId);
            for (HistoricActivityInstanceEntity historicActivityInstance : historicActivityInstances) {
                historicActivityInstance.setProcessDefinitionId(childExecution.getProcessDefinitionId());
                historicActivityInstance.setActivityId(childExecution.getActivityId());
                historicActivityInstance.setActivityName(newFlowElement.getName());
            }

            historicTaskService.recordTaskInfoChange(task);
        }

        // Boundary Events - only applies to Activities and up to this point we have a UserTask or ReceiveTask execution, both are Activities
        List<BoundaryEvent> boundaryEvents = ((Activity) newFlowElement).getBoundaryEvents();
        if (boundaryEvents != null && !boundaryEvents.isEmpty()) {
            List<ExecutionEntity> boundaryEventsExecutions = createBoundaryEvents(boundaryEvents, childExecution, commandContext);
            executeBoundaryEvents(boundaryEvents, boundaryEventsExecutions);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Child execution {} updated with parent {}", childExecution, parentExecutionEntity.getId());
        }
        return childExecution;
    }

    // TODO WIP - This method should be inside ExecutionEntityManager - temporarily made private and static until its moved there
    private static List<ExecutionEntity> createBoundaryEvents(List<BoundaryEvent> boundaryEvents, ExecutionEntity execution, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        List<ExecutionEntity> boundaryEventExecutions = new ArrayList<>(boundaryEvents.size());

        // The parent execution becomes a scope, and a child execution is created for each of the boundary events
        for (BoundaryEvent boundaryEvent : boundaryEvents) {

            if (CollectionUtil.isEmpty(boundaryEvent.getEventDefinitions())
                || (boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition)) {
                continue;
            }

            // A Child execution of the current execution is created to represent the boundary event being active
            ExecutionEntity childExecutionEntity = executionEntityManager.createChildExecution(execution);
            childExecutionEntity.setParentId(execution.getId());
            childExecutionEntity.setCurrentFlowElement(boundaryEvent);
            childExecutionEntity.setScope(false);
            boundaryEventExecutions.add(childExecutionEntity);
        }

        return boundaryEventExecutions;
    }

    // TODO WIP - This method should be somewhere else - it works as Utility method - temporarily made private and static
    private static void executeBoundaryEvents(List<BoundaryEvent> boundaryEvents, List<ExecutionEntity> boundaryEventExecutions) {
        if (!CollectionUtil.isEmpty(boundaryEventExecutions)) {
            Iterator<BoundaryEvent> boundaryEventsIterator = boundaryEvents.iterator();
            Iterator<ExecutionEntity> boundaryEventExecutionsIterator = boundaryEventExecutions.iterator();

            while (boundaryEventExecutionsIterator.hasNext() && boundaryEventExecutionsIterator.hasNext()) {
                BoundaryEvent boundaryEvent = boundaryEventsIterator.next();
                ExecutionEntity boundaryEventExecution = boundaryEventExecutionsIterator.next();
                ActivityBehavior boundaryEventBehavior = ((ActivityBehavior) boundaryEvent.getBehavior());
                LOGGER.debug("Executing boundary event activityBehavior {} with execution {}", boundaryEventBehavior.getClass(), boundaryEventExecution.getId());
                boundaryEventBehavior.execute(boundaryEventExecution);
            }
        }
    }

    // TODO WIP - Partly fetched from ProcessInstanceHelper - temporarily made private and static until cleanup
    private static void processEventSubProcessForChangeState(EventSubProcess eventSubProcess, ExecutionEntity subProcessExecution, CommandContext commandContext) {

        EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager(commandContext);
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        TimerJobService timerJobService = CommandContextUtil.getTimerJobService(commandContext);
        List<StartEvent> allStartEvents = eventSubProcess.findAllSubFlowElementInFlowMapOfType(StartEvent.class);

        for (StartEvent startEvent : allStartEvents) {
            if (!startEvent.getEventDefinitions().isEmpty()) {

                EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                List<EventSubscriptionEntity> eventSubscriptions = null;
                List<TimerJobEntity> timerJobs = null;
                if (eventDefinition instanceof SignalEventDefinition) {
                    eventSubscriptions = eventSubscriptionEntityManager.findEventSubscriptionsByProcessInstanceAndActivityId(subProcessExecution.getProcessInstanceId(), startEvent.getId(), SignalEventSubscriptionEntity.EVENT_TYPE);
                } else if (eventDefinition instanceof MessageEventDefinition) {
                    eventSubscriptions = eventSubscriptionEntityManager.findEventSubscriptionsByProcessInstanceAndActivityId(subProcessExecution.getProcessInstanceId(), startEvent.getId(), MessageEventSubscriptionEntity.EVENT_TYPE);
                } else if (eventDefinition instanceof TimerEventDefinition) {
                    timerJobs = timerJobService.findTimerJobsByProcessInstanceId(subProcessExecution.getProcessInstanceId())
                        .stream().filter(getTimerJobPredicateForActivityId(startEvent.getId()))
                        .collect(Collectors.toList());
                }

                boolean nonInterruptingAsInterrupting = false;
                if (!startEvent.isInterrupting()) {
                    //TODO WIP - For non interrupting, should we get the execution it the next parent scope or from the process? also do we care about the type (subProcess, task, etc)?
                    nonInterruptingAsInterrupting = isLastExecutionFromParentScope(subProcessExecution.getParentId(), commandContext);
                }

                // If its an interrupting eventSubProcess we don't register a subscription or startEvent executions and we make sure that they are removed if existed
                if (startEvent.isInterrupting() || nonInterruptingAsInterrupting) { //Current eventSubProcess plus its startEvent
                    ExecutionEntity startEventExecution = null;
                    if (eventSubscriptions != null && !eventSubscriptions.isEmpty()) {
                        for (EventSubscriptionEntity subscriptionEntity : eventSubscriptions) {
                            if (startEventExecution == null) {
                                startEventExecution = subscriptionEntity.getExecution();
                            }
                            eventSubscriptionEntityManager.delete(subscriptionEntity);
                        }
                    }
                    if (timerJobs != null && !timerJobs.isEmpty()) {
                        for (TimerJobEntity timerJobEntity : timerJobs) {
                            if (startEventExecution == null) {
                                Collection<ExecutionEntity> inactiveExecutionsByProcessInstanceId = executionEntityManager.findInactiveExecutionsByProcessInstanceId(subProcessExecution.getProcessInstanceId());
                                startEventExecution = inactiveExecutionsByProcessInstanceId.stream().filter(execution -> execution.getId().equals(timerJobEntity.getExecutionId())).findFirst().orElse(null);
                            }
                            timerJobService.deleteTimerJob(timerJobEntity);
                        }
                    }
                    if (startEventExecution != null) {
                        executionEntityManager.deleteExecutionAndRelatedData(startEventExecution, DeleteReason.EVENT_SUBPROCESS_INTERRUPTING + "(" + startEvent.getId() + ")");
                    }
                } else {
                    // For non-interrupting, we register a subscription and startEvent execution if they don't exist already
                    if (eventDefinition instanceof MessageEventDefinition && (eventSubscriptions == null || eventSubscriptions.isEmpty())) {
                        MessageEventDefinition messageEventDefinition = (MessageEventDefinition) eventDefinition;
                        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(subProcessExecution.getProcessDefinitionId());
                        if (bpmnModel.containsMessageId(messageEventDefinition.getMessageRef())) {
                            messageEventDefinition.setMessageRef(bpmnModel.getMessage(messageEventDefinition.getMessageRef()).getName());
                        }

                        ExecutionEntity messageExecution = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(subProcessExecution.getParent());
                        messageExecution.setCurrentFlowElement(startEvent);
                        messageExecution.setEventScope(true);
                        messageExecution.setActive(false);
                        MessageEventSubscriptionEntity messageSubscription = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insertMessageEvent(messageEventDefinition.getMessageRef(), messageExecution);
                        CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher()
                            .dispatchEvent(FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageSubscription.getActivityId(),
                                messageSubscription.getEventName(), null, messageSubscription.getExecution().getId(),
                                messageSubscription.getProcessInstanceId(), messageSubscription.getProcessDefinitionId()));

                    }
                    if (eventDefinition instanceof SignalEventDefinition && (eventSubscriptions == null || eventSubscriptions.isEmpty())) {
                        SignalEventDefinition signalEventDefinition = (SignalEventDefinition) eventDefinition;
                        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(subProcessExecution.getProcessDefinitionId());
                        Signal signal = null;
                        if (bpmnModel.containsSignalId(signalEventDefinition.getSignalRef())) {
                            signal = bpmnModel.getSignal(signalEventDefinition.getSignalRef());
                            signalEventDefinition.setSignalRef(signal.getName());
                        }

                        ExecutionEntity signalExecution = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(subProcessExecution.getParent());
                        signalExecution.setCurrentFlowElement(startEvent);
                        signalExecution.setEventScope(true);
                        signalExecution.setActive(false);
                        SignalEventSubscriptionEntity signalSubscription = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insertSignalEvent(signalEventDefinition.getSignalRef(), signal, signalExecution);
                        CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher()
                            .dispatchEvent(FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalSubscription.getActivityId(),
                                signalSubscription.getEventName(), null, signalSubscription.getExecution().getId(),
                                signalSubscription.getProcessInstanceId(), signalSubscription.getProcessDefinitionId()));

                    }

                    if (eventDefinition instanceof TimerEventDefinition && (timerJobs == null || timerJobs.isEmpty())) {
                        TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;

                        ExecutionEntity timerExecution = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(subProcessExecution.getParent());
                        timerExecution.setCurrentFlowElement(startEvent);
                        timerExecution.setEventScope(true);
                        timerExecution.setActive(false);

                        TimerJobEntity timerJob = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition, false, timerExecution, TriggerTimerEventJobHandler.TYPE,
                            TimerEventHandler.createConfiguration(startEvent.getId(), timerEventDefinition.getEndDate(), timerEventDefinition.getCalendarName()));

                        if (timerJob != null) {
                            CommandContextUtil.getTimerJobService().scheduleTimerJob(timerJob);
                        }
                    }
                }
            }
        }
    }

    // TODO WIP - This method should be in a TimerJob related class (domain, manager, etc)
    public static Predicate<TimerJobEntity> getTimerJobPredicateForActivityId(String activityId) {
        return new Predicate<TimerJobEntity>() {

            @Override
            public boolean test(TimerJobEntity timerJobEntity) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    Map<String, Object> jobConfigurationMap = objectMapper.readValue(timerJobEntity.getJobHandlerConfiguration(), new TypeReference<Map<String, Object>>() {

                    });
                    String timerActivityId = (String) jobConfigurationMap.get("activityId");
                    return activityId.equals(timerActivityId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    protected static boolean isLastExecutionFromParentScope(String parentId, CommandContext commandContext) {
        //Go backward from the parent scope looking for active executions
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
//
//        ExecutionEntity byId = executionEntityManager.findById(parentId);
//        byId.getParentId()

        List<ExecutionEntity> childExecutionsByParentExecutionId = executionEntityManager.findChildExecutionsByParentExecutionId(parentId);
        int childExecutionsAtParentScope = childExecutionsByParentExecutionId.size();
        return childExecutionsAtParentScope <= 2;
    }

    private static String printFlowElementIds(Collection<FlowElement> flowElements) {
        return flowElements.stream().map(FlowElement::getId).collect(Collectors.joining(","));
    }
}