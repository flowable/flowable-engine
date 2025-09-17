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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.dynamic.MoveExecutionEntityContainer.FlowElementMoveEntry;
import org.flowable.engine.impl.event.EventDefinitionExpressionUtil;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.runtime.EnableActivityIdContainer;
import org.flowable.engine.impl.runtime.MoveActivityIdContainer;
import org.flowable.engine.impl.runtime.MoveExecutionIdContainer;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.EntityLinkUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.IOParameterUtil;
import org.flowable.engine.impl.util.JobUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.engine.impl.util.TimerUtil;
import org.flowable.engine.interceptor.MigrationContext;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Dennis Federico
 */
public abstract class AbstractDynamicStateManager {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    //-- Move container preparation section start
    public List<MoveExecutionEntityContainer> resolveMoveExecutionEntityContainers(ChangeActivityStateBuilderImpl changeActivityStateBuilder, 
    		Map<String, Object> variables, CommandContext commandContext) {
    	
        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = new ArrayList<>();
        if (changeActivityStateBuilder.getMoveExecutionIdList().size() > 0) {
            for (MoveExecutionIdContainer executionContainer : changeActivityStateBuilder.getMoveExecutionIdList()) {
                
                Map<String, List<ExecutionEntity>> executionsByParent = new HashMap<>();
                Map<String, List<ExecutionEntity>> miExecutionsByParent = new HashMap<>();
                for (String executionId : executionContainer.getExecutionIds()) {
                    ExecutionEntity execution = resolveActiveExecution(executionId, commandContext);
                    List<ExecutionEntity> executionEntities;
                    if (!execution.isMultiInstanceRoot()) {
                    	executionEntities = executionsByParent.computeIfAbsent(execution.getParentId(), k -> new ArrayList<>());
                    } else {
                    	executionEntities = miExecutionsByParent.computeIfAbsent(execution.getParentId(), k -> new ArrayList<>());
                    }
                    executionEntities.add(execution);
                }
                
                miExecutionsByParent.values().forEach(executions -> {
                    MoveExecutionEntityContainer moveExecutionEntityContainer = new MoveExecutionEntityContainer(executions, executionContainer.getMoveToActivityIds());
                    if (executionsByParent.containsKey(executions.get(0).getId())) {
                        moveExecutionEntityContainer.setMultiInstanceExecutionWithChildExecutions(true);
                    }
                    
                    if (executions.get(0).getVariablesLocal() != null && !executions.get(0).getVariablesLocal().isEmpty()) {
                        moveExecutionEntityContainer.addLocalVariableMap(executions.get(0).getActivityId(), executions.get(0).getVariablesLocal());
                    }
                    if (executionContainer.getNewAssigneeId() != null) {
                        moveExecutionEntityContainer.setNewAssigneeId(executionContainer.getNewAssigneeId());
                    }
                    if (executionContainer.getNewOwnerId() != null) {
                        moveExecutionEntityContainer.setNewOwnerId(executionContainer.getNewOwnerId());
                    }
                    
                    moveExecutionEntityContainerList.add(moveExecutionEntityContainer);
                });
                
                executionsByParent.values().forEach(executions -> {
                    if (!miExecutionsByParent.isEmpty() && executions.size() > 1 && (executions.get(0).getCurrentFlowElement() instanceof Task || executions.get(0).getCurrentFlowElement() instanceof CallActivity)) {
                        for (ExecutionEntity execution : executions) {
                            List<ExecutionEntity> miExecutionList = new ArrayList<>();
                            miExecutionList.add(execution);
                            MoveExecutionEntityContainer moveExecutionEntityContainer = new MoveExecutionEntityContainer(miExecutionList, executionContainer.getMoveToActivityIds());
                        
                            if (execution.getVariablesLocal() != null && !execution.getVariablesLocal().isEmpty()) {
                                moveExecutionEntityContainer.addLocalVariableMap(execution.getActivityId(), execution.getVariablesLocal());
                            }
                            
                            if (executionContainer.getNewAssigneeId() != null) {
                                moveExecutionEntityContainer.setNewAssigneeId(executionContainer.getNewAssigneeId());
                            }
                            if (executionContainer.getNewOwnerId() != null) {
                                moveExecutionEntityContainer.setNewOwnerId(executionContainer.getNewOwnerId());
                            }
                            moveExecutionEntityContainerList.add(moveExecutionEntityContainer);
                        }
                    
                    } else {
                        MoveExecutionEntityContainer moveExecutionEntityContainer = new MoveExecutionEntityContainer(executions, executionContainer.getMoveToActivityIds());
                        for (ExecutionEntity execution : executions) {
                            if (execution.getVariablesLocal() != null && !execution.getVariablesLocal().isEmpty()) {
                                moveExecutionEntityContainer.addLocalVariableMap(execution.getActivityId(), execution.getVariablesLocal());
                            }
                        }
                        if (executionContainer.getNewAssigneeId() != null) {
                        	moveExecutionEntityContainer.setNewAssigneeId(executionContainer.getNewAssigneeId());
                        }
                        if (executionContainer.getNewOwnerId() != null) {
                        	moveExecutionEntityContainer.setNewOwnerId(executionContainer.getNewOwnerId());
                        }
                        moveExecutionEntityContainerList.add(moveExecutionEntityContainer);
                    }
                });
            }
        }

        if (changeActivityStateBuilder.getMoveActivityIdList().size() > 0) {
            for (MoveActivityIdContainer activityContainer : changeActivityStateBuilder.getMoveActivityIdList()) {
                Map<String, List<ExecutionEntity>> activitiesExecutionsByMultiInstanceParentId = new HashMap<>();
                List<ExecutionEntity> activitiesExecutionsNotInMultiInstanceParent = new ArrayList<>();

                for (String activityId : activityContainer.getActivityIds()) {
                    List<ExecutionEntity> activityExecutions = resolveActiveExecutions(changeActivityStateBuilder.getProcessInstanceId(), activityId, commandContext);
                    if (!activityExecutions.isEmpty()) {

                        // check for a multi instance root execution
                        ExecutionEntity miExecution = null;
                        boolean isInsideMultiInstance = false;
                        for (ExecutionEntity possibleMIExecution : activityExecutions) {
                            if (possibleMIExecution.isMultiInstanceRoot()) {
                                miExecution = possibleMIExecution;
                                isInsideMultiInstance = true;
                                break;
                            }

                            if (isExecutionInsideMultiInstance(possibleMIExecution)) {
                                isInsideMultiInstance = true;
                            }
                        }

                        // If inside a multiInstance, we create one container for each execution
                        if (isInsideMultiInstance) {

                            // We group by the parentId (executions belonging to the same parent execution instance
                            // i.e. gateways nested in MultiInstance subProcesses, need to be in the same move container)
                            Stream<ExecutionEntity> executionEntitiesStream = activityExecutions.stream();
                            if (miExecution != null) {
                                executionEntitiesStream = executionEntitiesStream.filter(ExecutionEntity::isMultiInstanceRoot);
                            }

                            executionEntitiesStream.forEach(childExecution -> {
                                String parentId = childExecution.isMultiInstanceRoot() ? childExecution.getId() : childExecution.getParentId();
                                List<ExecutionEntity> executionEntities = activitiesExecutionsByMultiInstanceParentId.computeIfAbsent(parentId, k -> new ArrayList<>());
                                executionEntities.add(childExecution);
                            });

                        } else {
                            ExecutionEntity execution = activityExecutions.iterator().next();
                            activitiesExecutionsNotInMultiInstanceParent.add(execution);
                        }
                    }
                }

                List<ExecutionEntity> combinedExecutions = new ArrayList<>();
                if (!activitiesExecutionsByMultiInstanceParentId.isEmpty()) {
                    for (String parentId : activitiesExecutionsByMultiInstanceParentId.keySet()) {
                        List<ExecutionEntity> miExecutions = activitiesExecutionsByMultiInstanceParentId.get(parentId);
                        if (!miExecutions.isEmpty()) {
                            if (isTopLevelMultiInstanceRoot(miExecutions.get(0))) {
                                combinedExecutions.addAll(miExecutions);
                            } else {
                                moveExecutionEntityContainerList.add(createMoveExecutionEntityContainer(activityContainer, miExecutions, commandContext));
                            }
                        }
                    }
                }
                
                if (!activitiesExecutionsNotInMultiInstanceParent.isEmpty()) {
                    combinedExecutions.addAll(activitiesExecutionsNotInMultiInstanceParent);
                }
                
                if (!combinedExecutions.isEmpty()) {
                    moveExecutionEntityContainerList.add(createMoveExecutionEntityContainer(activityContainer, combinedExecutions, commandContext));
                }
            }
        }

        return moveExecutionEntityContainerList;
    }
    
    public List<EnableActivityContainer> resolveEnableActivityContainers(ChangeActivityStateBuilderImpl changeActivityStateBuilder) {
        List<EnableActivityContainer> enableActivityContainerList = new ArrayList<>();
        if (!changeActivityStateBuilder.getEnableActivityIdList().isEmpty()) {
            for (EnableActivityIdContainer enableActivityIdContainer : changeActivityStateBuilder.getEnableActivityIdList()) {
                EnableActivityContainer enableActivityContainer = new EnableActivityContainer(enableActivityIdContainer.getActivityIds());
                enableActivityContainerList.add(enableActivityContainer);
            }
        }
        
        return enableActivityContainerList;
    }

    protected ExecutionEntity resolveActiveExecution(String executionId, CommandContext commandContext) {
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

    protected List<ExecutionEntity> resolveActiveExecutions(String processInstanceId, String activityId, CommandContext commandContext) {
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

        List<ExecutionEntity> executions = childExecutions.stream()
            .filter(e -> e.getCurrentActivityId() != null)
            .filter(e -> e.getCurrentActivityId().equals(activityId))
            .collect(Collectors.toList());

        if (executions.isEmpty()) {
            throw new FlowableException("Active execution could not be found with activity id " + activityId);
        }

        return executions;
    }

    protected MoveExecutionEntityContainer createMoveExecutionEntityContainer(MoveActivityIdContainer activityContainer, List<ExecutionEntity> executions, CommandContext commandContext) {
        MoveExecutionEntityContainer moveExecutionEntityContainer = new MoveExecutionEntityContainer(executions, activityContainer.getMoveToActivityIds());
        activityContainer.getNewAssigneeId().ifPresent(moveExecutionEntityContainer::setNewAssigneeId);
        activityContainer.getNewOwnerId().ifPresent(moveExecutionEntityContainer::setNewOwnerId);

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
            moveExecutionEntityContainer.setCallActivitySubProcessVersion(activityContainer.getCallActivitySubProcessVersion());
        }
        return moveExecutionEntityContainer;
    }

    protected void prepareMoveExecutionEntityContainer(MoveExecutionEntityContainer moveExecutionContainer, ProcessDefinition migrateToProcessDefinition, CommandContext commandContext) {
        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager();

        BpmnModel bpmnModelToMigrateTo = null;
        if (migrateToProcessDefinition != null) {
        	bpmnModelToMigrateTo = ProcessDefinitionUtil.getBpmnModel(migrateToProcessDefinition.getId());
        }
        boolean canContainerDirectMigrate = (moveExecutionContainer.getMoveToActivityIds().size() == 1) && (moveExecutionContainer.getExecutions().size() == 1);
        for (String activityId : moveExecutionContainer.getMoveToActivityIds()) {
            FlowElement currentFlowElement;
            FlowElement newFlowElement;
            String currentActivityId;
            if (moveExecutionContainer.isMoveToParentProcess()) {
                String parentProcessDefinitionId = moveExecutionContainer.getSuperExecution().getProcessDefinitionId();
                BpmnModel modelOfCallActivity = ProcessDefinitionUtil.getBpmnModel(moveExecutionContainer.getExecutions().get(0).getProcessDefinitionId());
                currentActivityId = moveExecutionContainer.getExecutions().get(0).getCurrentActivityId();
                currentFlowElement = resolveFlowElementFromBpmnModel(modelOfCallActivity, currentActivityId);
                if (bpmnModelToMigrateTo != null) {
                	newFlowElement = resolveFlowElementFromBpmnModel(bpmnModelToMigrateTo, activityId);
                } else {
                	BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentProcessDefinitionId);
                	newFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, activityId);
                }
                canContainerDirectMigrate = false;
                
            } else if (moveExecutionContainer.isMoveToSubProcessInstance()) {
                //The subProcess model is defined in the callActivity of the current processDefinition or the migrateProcessDefinition if defined
                ExecutionEntity firstExecution = moveExecutionContainer.getExecutions().get(0);
                BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(firstExecution.getProcessDefinitionId());
                currentActivityId = firstExecution.getCurrentActivityId();
                currentFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, currentActivityId);

                String processDefinitionIdOfCallActivity = null;
                if (migrateToProcessDefinition != null) {
                	processDefinitionIdOfCallActivity = migrateToProcessDefinition.getId();
                } else {
                	processDefinitionIdOfCallActivity = firstExecution.getProcessDefinitionId();
                }
                
                CallActivity callActivity = null;
                if (bpmnModelToMigrateTo != null) {
                	callActivity = (CallActivity) resolveFlowElementFromBpmnModel(bpmnModelToMigrateTo, moveExecutionContainer.getCallActivityId());
                } else {
                	callActivity = (CallActivity) resolveFlowElementFromBpmnModel(bpmnModel, moveExecutionContainer.getCallActivityId());
                }

                moveExecutionContainer.setCallActivity(callActivity);
                ProcessDefinition callActivityProcessDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionIdOfCallActivity);
                String tenantId = callActivityProcessDefinition.getTenantId();
                Integer calledProcessVersion = moveExecutionContainer.getCallActivitySubProcessVersion();
                String calledProcessDefKey = callActivity.getCalledElement();
                if (isExpression(calledProcessDefKey)) {
                    try {
                        calledProcessDefKey = expressionManager.createExpression(calledProcessDefKey).getValue(firstExecution.getProcessInstance()).toString();
                    } catch (FlowableException e) {
                        throw new FlowableException("Cannot resolve calledElement expression '" + calledProcessDefKey + "' of callActivity '" + callActivity.getId() + "'", e);
                    }
                }
                moveExecutionContainer.setSubProcessDefKey(calledProcessDefKey);
                ProcessDefinition subProcessDefinition = resolveProcessDefinition(calledProcessDefKey, calledProcessVersion, tenantId, commandContext);
                BpmnModel subProcessModel = ProcessDefinitionUtil.getBpmnModel(subProcessDefinition.getId());
                moveExecutionContainer.setSubProcessDefinition(subProcessDefinition);
                moveExecutionContainer.setSubProcessModel(subProcessModel);

                newFlowElement = resolveFlowElementFromBpmnModel(subProcessModel, activityId);
                canContainerDirectMigrate = false;
                
            } else {
                // Get first execution to get process definition id
                ExecutionEntity firstExecution = moveExecutionContainer.getExecutions().get(0);
                BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(firstExecution.getProcessDefinitionId());
                currentActivityId = firstExecution.getCurrentActivityId();
                currentFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, currentActivityId);
                if (bpmnModelToMigrateTo != null) {
                	newFlowElement = resolveFlowElementFromBpmnModel(bpmnModelToMigrateTo, activityId);
                } else {
                	newFlowElement = resolveFlowElementFromBpmnModel(bpmnModel, activityId);
                }
            }

            moveExecutionContainer.addMoveToFlowElement(activityId, currentFlowElement, newFlowElement);
            moveExecutionContainer.addCurrentActivityToNewElement(currentActivityId, currentFlowElement, newFlowElement);
            canContainerDirectMigrate = canContainerDirectMigrate && isDirectFlowElementExecutionMigration(currentFlowElement, newFlowElement);
        }

        moveExecutionContainer.setDirectExecutionMigration(canContainerDirectMigrate && migrateToProcessDefinition != null);
    }

    protected FlowElement resolveFlowElementFromBpmnModel(BpmnModel bpmnModel, String activityId) {
        FlowElement flowElement = bpmnModel.getFlowElement(activityId);
        if (flowElement == null) {
            throw new FlowableException("Cannot find activity '" + activityId + "' in process definition with id '" + bpmnModel.getMainProcess().getId() + "'");
        }
        return flowElement;
    }
    //-- Move container preparation section end

    protected void doMoveExecutionState(ProcessInstanceChangeState processInstanceChangeState, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        Map<String, List<ExecutionEntity>> activeEmbeddedSubProcesses = resolveActiveEmbeddedSubProcesses(processInstanceChangeState.getProcessInstanceId(), commandContext);
        processInstanceChangeState.setProcessInstanceActiveEmbeddedExecutions(activeEmbeddedSubProcesses);

        //Set the processInstance variables first so they are available during the change state
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceChangeState.getProcessInstanceId());
        processInstanceExecution.setVariables(processInstanceChangeState.getProcessInstanceVariables());

        for (MoveExecutionEntityContainer moveExecutionContainer : processInstanceChangeState.getMoveExecutionEntityContainers()) {
            prepareMoveExecutionEntityContainer(moveExecutionContainer, processInstanceChangeState.getProcessDefinitionToMigrateTo(), commandContext);
            // Action the moves (changeState)
            if (moveExecutionContainer.isMoveToParentProcess()) {
                String callActivityInstanceId = moveExecutionContainer.getExecutions().get(0).getProcessInstanceId();
                String deleteReason = "Change activity to parent process activity ids: " + printFlowElementIds(moveExecutionContainer.getMoveToFlowElements());
                safeDeleteSubProcessInstance(callActivityInstanceId, moveExecutionContainer.getExecutions(), deleteReason, commandContext);
            }

            List<ExecutionEntity> executionsToMove;
            if (moveExecutionContainer.isMoveToParentProcess()) {
                executionsToMove = Collections.singletonList(moveExecutionContainer.getSuperExecution());
            } else {
                executionsToMove = moveExecutionContainer.getExecutions();
            }

            Collection<FlowElementMoveEntry> moveToFlowElements;
            if (moveExecutionContainer.isMoveToSubProcessInstance()) {
                moveToFlowElements = Collections.singletonList(new FlowElementMoveEntry(moveExecutionContainer.getCallActivity(), moveExecutionContainer.getCallActivity()));
            } else {
                moveToFlowElements = moveExecutionContainer.getMoveToFlowElements();
            }

            String flowElementIdsLine = printFlowElementIds(moveToFlowElements);
            Collection<String> executionIdsNotToDelete = new HashSet<>();
            for (ExecutionEntity execution : executionsToMove) {
                executionIdsNotToDelete.add(execution.getId());

                // Don't delete called process when directly migration CallActivity
                List<String> childExecutionsToKeep = new ArrayList<>();
                if (execution.getCurrentFlowElement() instanceof CallActivity && moveExecutionContainer.isDirectExecutionMigration()) {
                    executionEntityManager.collectChildren(execution).stream()
                            .filter(executionEntity -> !Objects.equals(executionEntity.getProcessDefinitionId(), execution.getProcessDefinitionId()))
                            .map(ExecutionEntity::getId)
                            .forEach(childExecutionsToKeep::add);
                }
                
                if (execution.getCurrentFlowElement() instanceof Activity && moveExecutionContainer.isDirectExecutionMigration()) {
                    FlowElementMoveEntry moveEntry = moveExecutionContainer.getCurrentActivityToNewElement(execution.getCurrentFlowElement().getId());
                    if (moveEntry != null) {
                        if (moveEntry.getOriginalFlowElement().getId().equals(moveEntry.getNewFlowElement().getId()) &&
                                moveEntry.getOriginalFlowElement().getClass().getName().equals(moveEntry.getNewFlowElement().getClass().getName())) {
                            
                            List<BoundaryEvent> newBoundaryEvents = ((Activity) moveEntry.getNewFlowElement()).getBoundaryEvents();
                            Map<String, BoundaryEvent> newBoundaryEventMap = new HashMap<>();
                            if (newBoundaryEvents != null && !newBoundaryEvents.isEmpty()) {
                                for (BoundaryEvent newBoundaryEvent : newBoundaryEvents) {
                                    newBoundaryEventMap.put(newBoundaryEvent.getId(), newBoundaryEvent);
                                }
                            }
                            
                            List<BoundaryEvent> boundaryEvents = ((Activity) execution.getCurrentFlowElement()).getBoundaryEvents();
                            if (boundaryEvents != null && !boundaryEvents.isEmpty()) {
                                for (ExecutionEntity executionEntity : executionEntityManager.collectChildren(execution)) {
                                    if (executionEntity.getCurrentFlowElement() instanceof BoundaryEvent boundaryEvent) {
                                        if (newBoundaryEventMap.containsKey(boundaryEvent.getId()) && sameBoundaryEventDefinition(boundaryEvent, newBoundaryEventMap.get(boundaryEvent.getId()))) {
                                            
                                            boolean hasEventSubscriptions = false;
                                            if (boundaryEvent.getEventDefinitions() != null && !boundaryEvent.getEventDefinitions().isEmpty()) {
                                                EventDefinition sourceEventDef = boundaryEvent.getEventDefinitions().get(0);
                                                if (sourceEventDef instanceof SignalEventDefinition || sourceEventDef instanceof MessageEventDefinition) {
                                                    hasEventSubscriptions = true;
                                                }
                                                
                                            } else if (!boundaryEvent.getExtensionElements().isEmpty()) {
                                                List<ExtensionElement> sourceEventTypeExtensionElements = boundaryEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
                                                if (sourceEventTypeExtensionElements != null && !sourceEventTypeExtensionElements.isEmpty()) {
                                                    String sourceEventTypeValue = sourceEventTypeExtensionElements.get(0).getElementText();
                                                    if (StringUtils.isNotEmpty(sourceEventTypeValue)) {
                                                        hasEventSubscriptions = true;
                                                    }
                                                }
                                            }
                                            
                                            List<EventSubscriptionEntity> eventSubscriptions = null;
                                            if (hasEventSubscriptions) {
                                                eventSubscriptions = CommandContextUtil.getEventSubscriptionService(commandContext).findEventSubscriptionsByExecution(executionEntity.getId());
                                            }
                                            
                                            if (!hasEventSubscriptions  || (eventSubscriptions != null && !eventSubscriptions.isEmpty())) {
                                                executionEntity.setProcessDefinitionId(processInstanceChangeState.getProcessDefinitionToMigrateTo().getId());
                                                childExecutionsToKeep.add(executionEntity.getId());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                executionEntityManager.deleteChildExecutions(execution, childExecutionsToKeep, null, "Change parent activity to " + flowElementIdsLine, true, null);
                if (!moveExecutionContainer.isDirectExecutionMigration()) {
                    executionEntityManager.deleteExecutionAndRelatedData(execution, "Change activity to " + flowElementIdsLine, false, false, true, execution.getCurrentFlowElement());
                }

                // Make sure we are not moving the root execution
                if (execution.getParentId() == null) {
                    throw new FlowableException("Execution has no parent execution " + execution.getParentId());
                }

                // Delete the parent executions for each current execution when the move to activity id has the same subProcess scope
                ExecutionEntity continueParentExecution;
                if (processInstanceChangeState.getProcessDefinitionToMigrateTo() != null) {
                    continueParentExecution = deleteDirectParentExecutions(execution.getParentId(), moveToFlowElements, 
                    		executionIdsNotToDelete, processInstanceChangeState.getProcessDefinitionToMigrateTo(), moveExecutionContainer, commandContext);
                } else {
                    continueParentExecution = deleteParentExecutions(execution.getParentId(), moveToFlowElements, executionIdsNotToDelete, commandContext);
                }
                moveExecutionContainer.addContinueParentExecution(execution.getId(), continueParentExecution);
            }

            List<ExecutionEntity> newChildExecutions = createEmbeddedSubProcessAndExecutions(moveToFlowElements, executionsToMove, moveExecutionContainer, processInstanceChangeState, commandContext);

            if (moveExecutionContainer.isMoveToSubProcessInstance()) {
                CallActivity callActivity = moveExecutionContainer.getCallActivity();
                Process subProcess = moveExecutionContainer.getSubProcessModel().getProcessById(moveExecutionContainer.getSubProcessDefKey());
                ExecutionEntity callActivityInstanceExecution = createCallActivityInstance(callActivity, moveExecutionContainer.getSubProcessDefinition(), newChildExecutions.get(0), subProcess.getInitialFlowElement().getId(), commandContext);
                List<ExecutionEntity> moveExecutions = moveExecutionContainer.getExecutions();
                MoveExecutionEntityContainer subProcessMoveExecutionEntityContainer = new MoveExecutionEntityContainer(moveExecutions, moveExecutionContainer.getMoveToActivityIds());
                subProcessMoveExecutionEntityContainer.setNewAssigneeId(moveExecutionContainer.getNewAssigneeId());
                subProcessMoveExecutionEntityContainer.setNewOwnerId(moveExecutionContainer.getNewOwnerId());
                moveExecutions.forEach(executionEntity -> subProcessMoveExecutionEntityContainer.addContinueParentExecution(executionEntity.getId(), callActivityInstanceExecution));
                newChildExecutions = createEmbeddedSubProcessAndExecutions(moveExecutionContainer.getMoveToFlowElements(), moveExecutions, subProcessMoveExecutionEntityContainer, new ProcessInstanceChangeState(), commandContext);
            }

            if (!processInstanceChangeState.getLocalVariables().isEmpty()) {
                Map<String, Map<String, Object>> localVariables = processInstanceChangeState.getLocalVariables();
                Iterator<ExecutionEntity> newChildExecutionsIterator = newChildExecutions.iterator();
                while (newChildExecutionsIterator.hasNext()) {
                    //With changeState Api we can set local variables to the parents of moved executions (i.e. subProcesses created during the move)
                    //Thus we traverse up in the hierarchy from the newly created executions
                    ExecutionEntity execution = newChildExecutionsIterator.next();
                    while (execution != null) {
                        if (execution.getActivityId() != null && localVariables.containsKey(execution.getActivityId())) {
                            if (execution.isScope() || execution.getCurrentFlowElement() instanceof UserTask) {
                                execution.setVariablesLocal(localVariables.get(execution.getActivityId()));
                            } else {
                                ExecutionEntity scopedExecutionCandidate = execution;
                                while (scopedExecutionCandidate.getParent() != null) {
                                    ExecutionEntity parentExecution = scopedExecutionCandidate.getParent();
                                    if (parentExecution.isScope()) {
                                        parentExecution.setVariablesLocal(localVariables.get(execution.getActivityId()));
                                        break;
                                    }

                                    scopedExecutionCandidate = scopedExecutionCandidate.getParent();
                                }
                            }
                        }
                        execution = execution.getParent();
                    }
                }
            }

            if (!moveExecutionContainer.isDirectExecutionMigration()) {
                for (ExecutionEntity newChildExecution : newChildExecutions) {
                    if (moveExecutionContainer.getNewAssigneeId() != null && moveExecutionContainer.hasNewExecutionId(newChildExecution.getId())) {
                        MigrationContext migrationContext = new MigrationContext();
                        migrationContext.setAssignee(moveExecutionContainer.getNewAssigneeId());
                        CommandContextUtil.getAgenda(commandContext).planContinueProcessWithMigrationContextOperation(newChildExecution, migrationContext);
                        
                    } else if (moveExecutionContainer.getNewOwnerId() != null && moveExecutionContainer.hasNewExecutionId(newChildExecution.getId())) {
                        MigrationContext migrationContext = new MigrationContext();
                        migrationContext.setOwner(moveExecutionContainer.getNewOwnerId());
                        CommandContextUtil.getAgenda(commandContext).planContinueProcessWithMigrationContextOperation(newChildExecution, migrationContext);

                    } else {
                        if (newChildExecution.isMultiInstanceRoot() && moveExecutionContainer.isMultiInstanceExecutionWithChildExecutions() && 
                                (newChildExecution.getCurrentFlowElement() instanceof Task || newChildExecution.getCurrentFlowElement() instanceof CallActivity)) {
                            
                            continue;
                        }
                        
                        if (newChildExecution.getCurrentFlowElement() instanceof Task task && task.isAsynchronous()) {
                            JobService jobService = CommandContextUtil.getJobService(commandContext);
                            
                            JobEntity job = JobUtil.createJob(newChildExecution, newChildExecution.getCurrentFlowElement(), AsyncContinuationJobHandler.TYPE, CommandContextUtil.getProcessEngineConfiguration(commandContext));

                            jobService.createAsyncJob(job, task.isExclusive());
                            jobService.scheduleAsyncJob(job);
                        
                        } else {
                            CommandContextUtil.getAgenda(commandContext).planContinueProcessOperation(newChildExecution);
                        }
                    }
                }
            }
        }

        processPendingEventSubProcessesStartEvents(processInstanceChangeState, commandContext);
        
        for (EnableActivityContainer enableActivityContainer : processInstanceChangeState.getEnableActivityContainers()) {
            if (enableActivityContainer.getActivityIds() != null && !enableActivityContainer.getActivityIds().isEmpty()) {
                BpmnModel bpmnModel = null;
                ExecutionEntity parentExecution = executionEntityManager.findById(processInstanceChangeState.getProcessInstanceId());
                if (processInstanceChangeState.getProcessDefinitionToMigrateTo() != null) {
                    bpmnModel = ProcessDefinitionUtil.getBpmnModel(processInstanceChangeState.getProcessDefinitionToMigrateTo().getId());
                    
                } else {
                    bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentExecution.getProcessDefinitionId());
                }
                
                ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
                ProcessInstanceHelper processInstanceHelper = processEngineConfiguration.getProcessInstanceHelper();
                
                for (String enableActivityId : enableActivityContainer.getActivityIds()) {
                    FlowElement enableFlowElement = bpmnModel.getFlowElement(enableActivityId);
                    if (enableFlowElement == null) {
                        throw new FlowableException("could not find element for activity id " + enableActivityId);
                    }
                    
                    processInstanceHelper.processEventSubProcessStartEvent(enableFlowElement, parentExecution, processEngineConfiguration, commandContext);
                }
            }
        }
    }

    protected void processPendingEventSubProcessesStartEvents(ProcessInstanceChangeState processInstanceChangeState, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ProcessInstanceHelper processInstanceHelper = processEngineConfiguration.getProcessInstanceHelper();
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        ManagementService managementService = processEngineConfiguration.getManagementService();

        for (Map.Entry<? extends StartEvent, ExecutionEntity> pendingStartEventEntry : processInstanceChangeState.getPendingEventSubProcessesStartEvents().entrySet()) {
            StartEvent startEvent = pendingStartEventEntry.getKey();
            ExecutionEntity parentExecution = pendingStartEventEntry.getValue();
            EventDefinition eventDefinition = startEvent.getEventDefinitions().isEmpty() ? null : startEvent.getEventDefinitions().get(0);

            //Process event sub process when no subscriptions/timer jobs are found
            boolean processEventSubProcess = false;
            if (eventDefinition instanceof TimerEventDefinition) {
                processEventSubProcess = managementService.createTimerJobQuery().executionId(parentExecution.getId()).list().isEmpty();
            } else {
                processEventSubProcess = eventSubscriptionService.findEventSubscriptionsByExecution(parentExecution.getId()).isEmpty();
            }
            if (processEventSubProcess) {
                processInstanceHelper.processEventSubProcess(parentExecution, (EventSubProcess) startEvent.getSubProcess(), commandContext);
            }
        }
    }

    protected abstract Map<String, List<ExecutionEntity>> resolveActiveEmbeddedSubProcesses(String processInstanceId, CommandContext commandContext);

    protected abstract boolean isDirectFlowElementExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement);

    protected void safeDeleteSubProcessInstance(String processInstanceId, List<ExecutionEntity> executionsPool, String deleteReason, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        //Confirm that all the subProcessExecutions are in the executions pool
        List<ExecutionEntity> subProcessExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);
        
        Set<String> executionIdsToMove = new HashSet<>();
        for (ExecutionEntity executionPoolItem : executionsPool) {
            executionIdsToMove.add(executionPoolItem.getId());
        }

        for (ExecutionEntity subProcessExecution : subProcessExecutions) {
            FlowElement currentFlowElement = subProcessExecution.getCurrentFlowElement();
            if (currentFlowElement != null && currentFlowElement instanceof BoundaryEvent) {
                String parentExecutionId = subProcessExecution.getParentId();
                if (!StringUtils.isNotEmpty(parentExecutionId) || !executionIdsToMove.contains(parentExecutionId)) {
                    throw new FlowableException("Unbound boundary event execution prevents the sub process instance to be moved " + subProcessExecution.getId());
                }
            
            } else if (!executionIdsToMove.contains(subProcessExecution.getId())) {
                throw new FlowableException("Following execution of sub process instance is not moved " + subProcessExecution.getId());
            }
        }

        // delete the sub process instance
        executionEntityManager.deleteProcessInstance(processInstanceId, deleteReason, true);
    }

    protected ExecutionEntity deleteParentExecutions(String parentExecutionId, Collection<FlowElementMoveEntry> moveToFlowElements, CommandContext commandContext) {
        return deleteParentExecutions(parentExecutionId, moveToFlowElements, null, commandContext);
    }

    protected ExecutionEntity deleteParentExecutions(String parentExecutionId, Collection<FlowElementMoveEntry> moveToFlowElements, Collection<String> executionIdsNotToDelete, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        ExecutionEntity parentExecution = executionEntityManager.findById(parentExecutionId);
        if (parentExecution != null && parentExecution.getCurrentFlowElement() instanceof SubProcess parentSubProcess) {
            if (!isSubProcessAncestorOfAnyNewFlowElements(parentSubProcess.getId(), moveToFlowElements)) {
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
                executionEntityManager.deleteExecutionAndRelatedData(finalDeleteExecution, "Change activity to " + flowElementIdsLine, false, false, true, finalDeleteExecution.getCurrentFlowElement());
            }
        }

        return parentExecution;
    }

    protected ExecutionEntity deleteDirectParentExecutions(String parentExecutionId, Collection<FlowElementMoveEntry> moveToFlowElements, 
    		Collection<String> executionIdsNotToDelete, ProcessDefinition procDefToMigrateTo, 
    		MoveExecutionEntityContainer moveExecutionContainer, CommandContext commandContext) {
    	
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        ExecutionEntity parentExecution = executionEntityManager.findById(parentExecutionId);
        if (parentExecution.getCurrentFlowElement() instanceof SubProcess parentSubProcess) {
            if (!isSubProcessContainerOfAnyFlowElement(parentSubProcess.getId(), moveToFlowElements)) {
            	if (parentSubProcess.getLoopCharacteristics() == null || moveExecutionContainer.getMoveToFlowElement(parentSubProcess.getId()) != null) {
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
	                executionEntityManager.deleteExecutionAndRelatedData(finalDeleteExecution, "Change activity to " + flowElementIdsLine, false, false, true, finalDeleteExecution.getCurrentFlowElement());
            	
            	} else {
            		parentExecution.setProcessDefinitionId(procDefToMigrateTo.getId());
            		if (!parentExecution.isMultiInstanceRoot()) {
            			parentExecution.getParent().setProcessDefinitionId(procDefToMigrateTo.getId());
            		}
            	}
            }
        }

        return parentExecution;
    }

    protected boolean isSubProcessContainerOfAnyFlowElement(String subProcessId, Collection<FlowElementMoveEntry> moveToFlowElements) {
        Optional<SubProcess> isUsed = moveToFlowElements.stream()
            .map(FlowElementMoveEntry::getNewFlowElement)
            .map(FlowElement::getSubProcess)
            .filter(Objects::nonNull)
            .filter(elementSubProcess -> elementSubProcess.getId().equals(subProcessId))
            .findAny();

        return isUsed.isPresent();
    }

    protected ExecutionEntity resolveParentExecutionToDelete(ExecutionEntity execution, Collection<FlowElementMoveEntry> moveToFlowElements) {
        ExecutionEntity parentExecution = execution.getParent();

        if (parentExecution.isProcessInstanceType()) {
            return null;
        }

        if (!isSubProcessContainerOfAnyFlowElement(parentExecution.getActivityId(), moveToFlowElements)) {
            ExecutionEntity subProcessParentExecution = resolveParentExecutionToDelete(parentExecution, moveToFlowElements);
            if (subProcessParentExecution != null) {
                return subProcessParentExecution;
            } else {
                return parentExecution;
            }
        }

        return null;
    }

    protected List<ExecutionEntity> createEmbeddedSubProcessAndExecutions(Collection<FlowElementMoveEntry> moveToFlowElements, List<ExecutionEntity> movingExecutions, 
            MoveExecutionEntityContainer moveExecutionEntityContainer, ProcessInstanceChangeState processInstanceChangeState, CommandContext commandContext) {

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();

        // Resolve the sub process elements that need to be created for each move to flow element
        HashMap<String, SubProcess> subProcessesToCreate = new HashMap<>();
        for (FlowElementMoveEntry flowElementMoveEntry : moveToFlowElements) {
            FlowElement newFlowElement = flowElementMoveEntry.getNewFlowElement();
            SubProcess subProcess = newFlowElement.getSubProcess();
            //If the new flowElement is the StartEvent of and EventSubProcess, we skip the subProcess creation, the startEvent is contained in a level above
            if (isEventSubProcessStart(newFlowElement)) {
                subProcess = subProcess.getSubProcess();
            }
            while (subProcess != null) {
                if (!processInstanceChangeState.getProcessInstanceActiveEmbeddedExecutions().containsKey(subProcess.getId()) && !isSubProcessAncestorOfAnyExecution(subProcess.getId(), movingExecutions)) {
                    subProcessesToCreate.put(subProcess.getId(), subProcess);
                }
                subProcess = subProcess.getSubProcess();
            }
        }

        // The default parent execution is retrieved from the match with the first source execution
        ExecutionEntity defaultContinueParentExecution = moveExecutionEntityContainer.getContinueParentExecution(movingExecutions.get(0).getId());
        Set<String> movingExecutionIds = movingExecutions.stream().map(ExecutionEntity::getId).collect(Collectors.toSet());

        // Build the subProcess hierarchy
        for (SubProcess subProcess : subProcessesToCreate.values()) {
            if (subProcess instanceof EventSubProcess) {
                ExecutionEntity embeddedSubProcess = createEmbeddedSubProcessHierarchy(subProcess, defaultContinueParentExecution, subProcessesToCreate, movingExecutionIds, processInstanceChangeState, commandContext);
                moveExecutionEntityContainer.addCreatedEventSubProcess(subProcess.getId(), embeddedSubProcess);
            } else if (!processInstanceChangeState.getCreatedEmbeddedSubProcesses().containsKey(subProcess.getId())) {
                ExecutionEntity embeddedSubProcess = createEmbeddedSubProcessHierarchy(subProcess, defaultContinueParentExecution, subProcessesToCreate, movingExecutionIds, processInstanceChangeState, commandContext);
                processInstanceChangeState.addCreatedEmbeddedSubProcess(subProcess.getId(), embeddedSubProcess);
            }
        }

        // Adds the execution (leaf) to the subProcess
        List<ExecutionEntity> newChildExecutions = new ArrayList<>();
        for (FlowElementMoveEntry flowElementMoveEntry : moveToFlowElements) {
            FlowElement newFlowElement = flowElementMoveEntry.getNewFlowElement();
            ExecutionEntity parentExecution;
            if (newFlowElement.getSubProcess() != null && moveExecutionEntityContainer.getCreatedEventSubProcess(newFlowElement.getSubProcess().getId()) != null) {
                parentExecution = moveExecutionEntityContainer.getCreatedEventSubProcess(newFlowElement.getSubProcess().getId());
            } else  if (newFlowElement.getSubProcess() != null && processInstanceChangeState.getCreatedEmbeddedSubProcesses().containsKey(newFlowElement.getSubProcess().getId())) {
                parentExecution = processInstanceChangeState.getCreatedEmbeddedSubProcesses().get(newFlowElement.getSubProcess().getId());
            
            } else if ((newFlowElement instanceof Task || newFlowElement instanceof CallActivity) && isFlowElementMultiInstance(newFlowElement) && !movingExecutions.get(0).isMultiInstanceRoot() &&
                    processInstanceChangeState.getCreatedMultiInstanceRootExecution().containsKey(newFlowElement.getId())) {
                
                parentExecution = processInstanceChangeState.getCreatedMultiInstanceRootExecution().get(newFlowElement.getId());
                
            } else {
                parentExecution = defaultContinueParentExecution;
            }

            if (isEventSubProcessStart(newFlowElement)) {
                // EventSubProcessStarts are created later if the eventSubProcess was not created already during another move
                processInstanceChangeState.addPendingEventSubProcessStartEvent((StartEvent) newFlowElement, parentExecution);
                
            } else {
                ExecutionEntity newChildExecution;
                if (moveExecutionEntityContainer.isDirectExecutionMigration() && isDirectFlowElementExecutionMigration(flowElementMoveEntry.originalFlowElement, flowElementMoveEntry.newFlowElement)) {
                    newChildExecution = migrateExecutionEntity(parentExecution, movingExecutions.get(0), flowElementMoveEntry.getOriginalFlowElement(), newFlowElement, commandContext);
                } else {
                    newChildExecution = executionEntityManager.createChildExecution(parentExecution);
                    newChildExecution.setCurrentFlowElement(newFlowElement);
                    moveExecutionEntityContainer.addNewExecutionId(newChildExecution.getId());
                }

                if (newChildExecution != null) {

                    if (moveExecutionEntityContainer.getFlowElementLocalVariableMap().containsKey(newFlowElement.getId())) {
                        newChildExecution.setVariablesLocal(moveExecutionEntityContainer.getFlowElementLocalVariableMap().get(newFlowElement.getId()));
                    }
                    
                    if (movingExecutions.get(0).isMultiInstanceRoot() && isFlowElementMultiInstance(newFlowElement) && hasSameMultiInstanceConfig(movingExecutions.get(0).getCurrentFlowElement(), newFlowElement)) {
                        newChildExecution.setMultiInstanceRoot(true);
                        newChildExecution.setActive(false);
                        processInstanceChangeState.addCreatedMultiInstanceRootExecution(newFlowElement.getId(), newChildExecution);
                    }
                    
                    if (newFlowElement instanceof UserTask
                            && !moveExecutionEntityContainer.hasNewExecutionId(newChildExecution.getId())) {

                        if (moveExecutionEntityContainer.getNewAssigneeId() != null) {
                            handleUserTaskNewAssignee(newChildExecution, moveExecutionEntityContainer.getNewAssigneeId(), commandContext);
                        }

                        if (moveExecutionEntityContainer.getNewOwnerId() != null) {
                            handleUserTaskNewOwner(newChildExecution, moveExecutionEntityContainer.getNewOwnerId(), commandContext);
                        }

                    }

                    if (newFlowElement instanceof CallActivity callActivity && !moveExecutionEntityContainer.isDirectExecutionMigration()) {
                        
                        if (!newChildExecution.isMultiInstanceRoot()) {
                            processEngineConfiguration.getActivityInstanceEntityManager().recordActivityStart(newChildExecution);
    
                            FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
                            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                                eventDispatcher.dispatchEvent(
                                    FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_STARTED, newFlowElement.getId(), newFlowElement.getName(), newChildExecution.getId(),
                                        newChildExecution.getProcessInstanceId(), newChildExecution.getProcessDefinitionId(), newFlowElement),
                                    processEngineConfiguration.getEngineCfgKey());
                            }
                        }

                        // start boundary events of new call activity
                        List<BoundaryEvent> boundaryEvents = callActivity.getBoundaryEvents();
                        if (CollectionUtil.isNotEmpty(boundaryEvents)) {
                            executeBoundaryEvents(boundaryEvents, newChildExecution);
                        }
                    }

                    newChildExecutions.add(newChildExecution);
                }

                // Parallel gateway joins needs each incoming execution to enter the gateway naturally as it checks the number of executions to be able to progress/continue
                // If we have multiple executions going into a gateway, usually into a gateway join using xxxToSingleActivityId
                if (newFlowElement instanceof Gateway) {
                    //Skip one that was already added
                    movingExecutions.stream().skip(1).forEach(e -> {
                        ExecutionEntity childExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
                        childExecution.setCurrentFlowElement(newFlowElement);
                        newChildExecutions.add(childExecution);
                    });
                }
            }
        }

        return newChildExecutions;
    }

    protected boolean isSubProcessAncestorOfAnyExecution(String subProcessId, List<ExecutionEntity> executions) {
        for (ExecutionEntity execution : executions) {
            FlowElement executionElement = execution.getCurrentFlowElement();

            if (isSubProcessAncestor(subProcessId, executionElement)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSubProcessAncestorOfAnyNewFlowElements(String subProcessId, Collection<FlowElementMoveEntry> flowElements) {
        for (FlowElementMoveEntry flowElementMoveEntry : flowElements) {
            if (isSubProcessAncestor(subProcessId, flowElementMoveEntry.getNewFlowElement())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSubProcessAncestor(String subProcessId, FlowElement flowElement) {
        while (flowElement.getSubProcess() != null) {
            String execElemSubProcessId = flowElement.getSubProcess().getId();
            if (execElemSubProcessId != null && execElemSubProcessId.equals(subProcessId)) {
                return true;
            }
            flowElement = flowElement.getSubProcess();
        }
        return false;
    }

    protected List<FlowElement> getFlowElementsInSubProcess(SubProcess subProcess, Collection<FlowElement> flowElements) {
        return flowElements.stream()
            .filter(e -> e.getSubProcess() != null)
            .filter(e -> e.getSubProcess().getId().equals(subProcess.getId()))
            .collect(Collectors.toList());
    }

    protected ExecutionEntity createEmbeddedSubProcessHierarchy(SubProcess subProcess, ExecutionEntity defaultParentExecution, Map<String, SubProcess> subProcessesToCreate, Set<String> movingExecutionIds, ProcessInstanceChangeState processInstanceChangeState, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        if (processInstanceChangeState.getProcessInstanceActiveEmbeddedExecutions().containsKey(subProcess.getId())) {
            return processInstanceChangeState.getProcessInstanceActiveEmbeddedExecutions().get(subProcess.getId()).get(0);
        }

        if (processInstanceChangeState.getCreatedEmbeddedSubProcesses().containsKey(subProcess.getId())) {
            return processInstanceChangeState.getCreatedEmbeddedSubProcesses().get(subProcess.getId());
        }

        //Create the parent, if needed
        ExecutionEntity parentSubProcess = defaultParentExecution;
        if (subProcess.getSubProcess() != null) {
            parentSubProcess = createEmbeddedSubProcessHierarchy(subProcess.getSubProcess(), defaultParentExecution, subProcessesToCreate, movingExecutionIds, processInstanceChangeState, commandContext);
            processInstanceChangeState.getCreatedEmbeddedSubProcesses().put(subProcess.getSubProcess().getId(), parentSubProcess);
        }
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();

        ExecutionEntity subProcessExecution = executionEntityManager.createChildExecution(parentSubProcess);
        subProcessExecution.setCurrentFlowElement(subProcess);
        subProcessExecution.setScope(true);

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(
                FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_STARTED, subProcess.getId(), subProcess.getName(), subProcessExecution.getId(),
                    subProcessExecution.getProcessInstanceId(), subProcessExecution.getProcessDefinitionId(), subProcess),
                processEngineConfiguration.getEngineCfgKey());
        }

        subProcessExecution.setVariablesLocal(processDataObjects(subProcess.getDataObjects()));

        processEngineConfiguration.getActivityInstanceEntityManager().recordActivityStart(subProcessExecution);

        List<BoundaryEvent> boundaryEvents = subProcess.getBoundaryEvents();
        if (CollectionUtil.isNotEmpty(boundaryEvents)) {
            executeBoundaryEvents(boundaryEvents, subProcessExecution);
        }

        if (subProcess instanceof EventSubProcess) {
            processCreatedEventSubProcess((EventSubProcess) subProcess, subProcessExecution, movingExecutionIds, commandContext);
        }

        ProcessInstanceHelper processInstanceHelper = processEngineConfiguration.getProcessInstanceHelper();

        //Process containing child Event SubProcesses not contained in this creation hierarchy
        List<EventSubProcess> childEventSubProcesses = subProcess.findAllSubFlowElementInFlowMapOfType(EventSubProcess.class);
        childEventSubProcesses.stream()
            .filter(childEventSubProcess -> !subProcessesToCreate.containsKey(childEventSubProcess.getId()))
            .forEach(childEventSubProcess -> processInstanceHelper.processEventSubProcess(subProcessExecution, childEventSubProcess, commandContext));

        return subProcessExecution;
    }

    protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
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

    protected void executeBoundaryEvents(Collection<BoundaryEvent> boundaryEvents, ExecutionEntity execution) {

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

            CommandContextUtil.getProcessEngineConfiguration().getActivityInstanceEntityManager().recordActivityStart(childExecutionEntity);

            ActivityBehavior boundaryEventBehavior = ((ActivityBehavior) boundaryEvent.getBehavior());
            LOGGER.debug("Executing boundary event activityBehavior {} with execution {}", boundaryEventBehavior.getClass(), childExecutionEntity.getId());
            boundaryEventBehavior.execute(childExecutionEntity);
        }
    }

    protected ExecutionEntity createCallActivityInstance(CallActivity callActivity, ProcessDefinition subProcessDefinition, ExecutionEntity parentExecution, String initialActivityId, CommandContext commandContext) {

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();

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

        ExecutionEntity subProcessInstance = executionEntityManager.createSubprocessInstance(subProcessDefinition, parentExecution, businessKey, initialActivityId);
        if (processEngineConfiguration.isEnableEntityLinks()) {
            EntityLinkUtil.createEntityLinks(parentExecution.getProcessInstanceId(), parentExecution.getId(), callActivity.getId(),
                    subProcessInstance.getId(), ScopeTypes.BPMN);
        }
        CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordSubProcessInstanceStart(parentExecution, subProcessInstance);

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(
                    FlowableEngineEventType.PROCESS_CREATED, subProcessInstance), processEngineConfiguration.getEngineCfgKey());
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
        IOParameterUtil.processInParameters(callActivity.getInParameters(), parentExecution, variables::put, variables::put, expressionManager);

        if (!variables.isEmpty()) {
            subProcessInstance.setVariables(variables);
        }

        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, subProcessInstance),
                    processEngineConfiguration.getEngineCfgKey());
        }

        return subProcessInstance;
    }

    protected ExecutionEntity migrateExecutionEntity(ExecutionEntity parentExecutionEntity, ExecutionEntity childExecution, FlowElement originalElement, FlowElement newFlowElement, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        TaskService taskService = processEngineConfiguration.getTaskServiceConfiguration().getTaskService();
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        // manage the bidirectional parent-child relation
        childExecution.setProcessInstanceId(parentExecutionEntity.getProcessInstanceId());
        childExecution.setProcessInstance(parentExecutionEntity.getProcessInstance());
        childExecution.setProcessDefinitionId(parentExecutionEntity.getProcessDefinitionId());
        ExecutionEntity oldParent = childExecution.getParent();
        if (oldParent != null && !oldParent.getId().equals(parentExecutionEntity.getId())) {
            oldParent.getExecutions().remove(childExecution);
        }
        childExecution.setParent(parentExecutionEntity);
        parentExecutionEntity.addChildExecution(childExecution);

        //Additional changes if the new activity Id doesn't match
        String oldActivityId = childExecution.getCurrentActivityId();
        if (!childExecution.getCurrentActivityId().equals(newFlowElement.getId())) {
            ((ExecutionEntityImpl) childExecution).setActivityId(newFlowElement.getId());
        }

        // If we are moving a UserTask we need to update its processDefinition references
        if (newFlowElement instanceof UserTask) {
            TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration)
                    .executionId(childExecution.getId()).singleResult();
            task.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            task.setTaskDefinitionKey(newFlowElement.getId());

            // Set name
            String name = null;
            if (newFlowElement.getName() != null) {
                Object nameValue = expressionManager.createExpression(newFlowElement.getName()).getValue(childExecution);
                if (nameValue != null) {
                    name = nameValue.toString();
                }
            }
            task.setName(name);

            // Set description
            String description = null;
            if (newFlowElement.getDocumentation() != null) {
                Object descriptionValue = expressionManager.createExpression(newFlowElement.getDocumentation()).getValue(childExecution);
                if (descriptionValue != null) {
                    description = descriptionValue.toString();
                }
            }
            task.setDescription(description);
            
            // Set form key
            String newFormKey = null;
            String userTaskFormKey = ((UserTask) newFlowElement).getFormKey();
            if (userTaskFormKey != null) {
                Object formKeyValue = expressionManager.createExpression(userTaskFormKey).getValue(childExecution);
                if (formKeyValue != null) {
                    newFormKey = formKeyValue.toString();
                }
            }
            task.setFormKey(newFormKey);

            // Set newCategory
            String newCategory = null;
            String userTaskCategory = ((UserTask) newFlowElement).getCategory();
            if (userTaskCategory != null) {
                Object categoryValue = expressionManager.createExpression(userTaskCategory).getValue(childExecution);
                if (categoryValue != null) {
                    newCategory = categoryValue.toString();
                }
            }
            task.setCategory(newCategory);

            task.setProcessInstanceId(childExecution.getProcessInstanceId());

            //Sync history
            processEngineConfiguration.getActivityInstanceEntityManager().syncUserTaskExecution(childExecution, newFlowElement, oldActivityId, task);
        }

        if (newFlowElement instanceof ServiceTask && (((ServiceTask) newFlowElement).isAsynchronous() || ((ServiceTask) newFlowElement).isAsynchronousLeave())) {
            handleServiceTaskJobUpdate(childExecution, commandContext);
        } else if (newFlowElement instanceof ExternalWorkerServiceTask) {
            handleExternalWorkerServiceTaskJobUpdate(childExecution, commandContext);
        }
        
        // Boundary Events - only applies to Activities and up to this point we have a UserTask, ReceiveTask or ExternalWorkerServiceTask execution, they are all Activities
        List<BoundaryEvent> finalBoundaryEvents = null;
        if (originalElement == null || !originalElement.getId().equals(newFlowElement.getId()) ||
                !originalElement.getClass().getName().equals(newFlowElement.getClass().getName())) {
            
            finalBoundaryEvents = ((Activity) newFlowElement).getBoundaryEvents();
        
        } else {
            List<BoundaryEvent> boundaryEvents = ((Activity) newFlowElement).getBoundaryEvents();
            if (boundaryEvents != null && !boundaryEvents.isEmpty()) {
                
                Map<String, Pair<BoundaryEvent, ExecutionEntity>> originalBoundaryEventMap = new HashMap<>();
                for (ExecutionEntity executionEntity : processEngineConfiguration.getExecutionEntityManager().collectChildren(childExecution)) {
                    if (executionEntity.getCurrentFlowElement() != null && executionEntity.getCurrentFlowElement() instanceof BoundaryEvent) {
                        originalBoundaryEventMap.put(executionEntity.getCurrentFlowElement().getId(), Pair.of((BoundaryEvent) executionEntity.getCurrentFlowElement(), executionEntity));
                    }
                }
                
                finalBoundaryEvents = new ArrayList<>();
                for (BoundaryEvent boundaryEvent : boundaryEvents) {
                    if (!originalBoundaryEventMap.containsKey(boundaryEvent.getId()) || !sameBoundaryEventDefinition(boundaryEvent, originalBoundaryEventMap.get(boundaryEvent.getId()).getLeft())) {
                        finalBoundaryEvents.add(boundaryEvent);
                        continue;
                    }
                        
                    boolean hasEventSubscriptions = false;
                    if (boundaryEvent.getEventDefinitions() != null && !boundaryEvent.getEventDefinitions().isEmpty()) {
                        EventDefinition sourceEventDef = boundaryEvent.getEventDefinitions().get(0);
                        if (sourceEventDef instanceof SignalEventDefinition || sourceEventDef instanceof MessageEventDefinition) {
                            hasEventSubscriptions = true;
                        }
                        
                    } else if (!boundaryEvent.getExtensionElements().isEmpty()) {
                        List<ExtensionElement> sourceEventTypeExtensionElements = boundaryEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
                        if (sourceEventTypeExtensionElements != null && !sourceEventTypeExtensionElements.isEmpty()) {
                            String sourceEventTypeValue = sourceEventTypeExtensionElements.get(0).getElementText();
                            if (StringUtils.isNotEmpty(sourceEventTypeValue)) {
                                hasEventSubscriptions = true;
                            }
                        }
                    }
                        
                    if (hasEventSubscriptions) {
                        List<EventSubscriptionEntity> eventSubscriptions = CommandContextUtil.getEventSubscriptionService(commandContext)
                                .findEventSubscriptionsByExecution(originalBoundaryEventMap.get(boundaryEvent.getId()).getRight().getId());
                        if (eventSubscriptions == null || eventSubscriptions.isEmpty()) {
                            finalBoundaryEvents.add(boundaryEvent);
                        }
                    }
                }
            }
        }
        
        if (finalBoundaryEvents != null && !finalBoundaryEvents.isEmpty()) {
            List<ExecutionEntity> boundaryEventsExecutions = createBoundaryEvents(finalBoundaryEvents, childExecution, commandContext);
            executeBoundaryEvents(finalBoundaryEvents, boundaryEventsExecutions);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Child execution {} updated with parent {}", childExecution, parentExecutionEntity.getId());
        }
        return childExecution;
    }
    
    protected void handleServiceTaskJobUpdate(ExecutionEntity childExecution, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ManagementService managementService = processEngineConfiguration.getManagementService();

        // An existing executable job is not updated, because it's not needed and could lead to optimistic lock exceptions
        
        // Update an existing timer job
        TimerJobEntityImpl timerJob = (TimerJobEntityImpl) managementService.createTimerJobQuery()
                .executionId(childExecution.getId()).singleResult();
        if (timerJob != null) {
            timerJob.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            return;
        }

        // Update an existing dead letter job
        DeadLetterJobEntityImpl deadLetterJob = (DeadLetterJobEntityImpl) managementService.createDeadLetterJobQuery()
                .executionId(childExecution.getId()).singleResult();
        if (deadLetterJob != null) {
            deadLetterJob.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            return;
        }

        // Update an existing suspended job
        SuspendedJobEntityImpl suspendedJob = (SuspendedJobEntityImpl) managementService.createSuspendedJobQuery()
                .executionId(childExecution.getId()).singleResult();
        if (suspendedJob != null) {
            suspendedJob.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            return;
        }

        // Update an existing external worker job
        ExternalWorkerJobEntityImpl externalWorkerJob = (ExternalWorkerJobEntityImpl) managementService.createExternalWorkerJobQuery()
                .executionId(childExecution.getId()).singleResult();
        if (externalWorkerJob != null) {
            externalWorkerJob.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            return;
        }
    }

    protected void handleExternalWorkerServiceTaskJobUpdate(ExecutionEntity childExecution, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ManagementService managementService = processEngineConfiguration.getManagementService();

        // Update an existing external worker job
        ExternalWorkerJobEntityImpl externalWorkerJob = (ExternalWorkerJobEntityImpl) managementService.createExternalWorkerJobQuery()
                .executionId(childExecution.getId()).singleResult();
        if (externalWorkerJob != null) {
            externalWorkerJob.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            return;
        }

        // Update an existing dead letter job
        DeadLetterJobEntityImpl deadLetterJob = (DeadLetterJobEntityImpl) managementService.createDeadLetterJobQuery()
                .executionId(childExecution.getId()).singleResult();
        if (deadLetterJob != null) {
            deadLetterJob.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            return;
        }

        // Update an existing suspended job
        SuspendedJobEntityImpl suspendedJob = (SuspendedJobEntityImpl) managementService.createSuspendedJobQuery()
                .executionId(childExecution.getId()).singleResult();
        if (suspendedJob != null) {
            suspendedJob.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            return;
        }
    }

    protected void handleUserTaskNewAssignee(ExecutionEntity taskExecution, String newAssigneeId, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        TaskService taskService = processEngineConfiguration.getTaskServiceConfiguration().getTaskService();
        TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration)
                .executionId(taskExecution.getId()).singleResult();
        if (task != null) {
            TaskHelper.changeTaskAssignee(task, newAssigneeId);
        }
    }

    protected void handleUserTaskNewOwner(ExecutionEntity taskExecution, String newOwnerId, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        TaskService taskService = processEngineConfiguration.getTaskServiceConfiguration().getTaskService();
        TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration)
                .executionId(taskExecution.getId()).singleResult();
        if (task != null) {
            TaskHelper.changeTaskOwner(task, newOwnerId);
        }
    }

    protected boolean isEventSubProcessStart(FlowElement flowElement) {
        return flowElement instanceof StartEvent && flowElement.getSubProcess() != null && flowElement.getSubProcess() instanceof EventSubProcess;
    }

    protected List<ExecutionEntity> createBoundaryEvents(List<BoundaryEvent> boundaryEvents, ExecutionEntity execution, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        List<ExecutionEntity> boundaryEventExecutions = new ArrayList<>(boundaryEvents.size());

        // The parent execution becomes a scope, and a child execution is created for each of the boundary events
        for (BoundaryEvent boundaryEvent : boundaryEvents) {

            if (CollectionUtil.isEmpty(boundaryEvent.getEventDefinitions())) {
                
                boolean hasEventRegistryBoundaryEvent = false;
                if (!boundaryEvent.getExtensionElements().isEmpty()) {
                    List<ExtensionElement> eventTypeExtensionElements = boundaryEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
                    if (eventTypeExtensionElements != null && !eventTypeExtensionElements.isEmpty()) {
                        String eventTypeValue = eventTypeExtensionElements.get(0).getElementText();
                        if (StringUtils.isNotEmpty(eventTypeValue)) {
                            hasEventRegistryBoundaryEvent = true;
                        }
                    }
                }
                
                if (!hasEventRegistryBoundaryEvent) {
                    continue;
                }
                
            } else if (boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition) {
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

    protected void executeBoundaryEvents(List<BoundaryEvent> boundaryEvents, List<ExecutionEntity> boundaryEventExecutions) {
        if (!CollectionUtil.isEmpty(boundaryEventExecutions)) {
            Iterator<BoundaryEvent> boundaryEventsIterator = boundaryEvents.iterator();
            Iterator<ExecutionEntity> boundaryEventExecutionsIterator = boundaryEventExecutions.iterator();

            while (boundaryEventsIterator.hasNext() && boundaryEventExecutionsIterator.hasNext()) {
                BoundaryEvent boundaryEvent = boundaryEventsIterator.next();
                ExecutionEntity boundaryEventExecution = boundaryEventExecutionsIterator.next();
                ActivityBehavior boundaryEventBehavior = ((ActivityBehavior) boundaryEvent.getBehavior());
                LOGGER.debug("Executing boundary event activityBehavior {} with execution {}", boundaryEventBehavior.getClass(), boundaryEventExecution.getId());
                boundaryEventBehavior.execute(boundaryEventExecution);
            }
        }
    }

    protected boolean isExecutionInsideMultiInstance(ExecutionEntity execution) {
        return getFlowElementMultiInstanceParentId(execution.getCurrentFlowElement()) != null;
    }

    protected String getFlowElementMultiInstanceParentId(FlowElement flowElement) {
        FlowElementsContainer parentContainer = flowElement.getParentContainer();
        while (parentContainer instanceof Activity) {
            if (isFlowElementMultiInstance((Activity) parentContainer)) {
                return parentContainer.getId();
            }
            parentContainer = ((Activity) parentContainer).getParentContainer();
        }
        return null;
    }

    protected boolean isFlowElementMultiInstance(FlowElement flowElement) {
        if (flowElement instanceof Activity) {
            return ((Activity) flowElement).getLoopCharacteristics() != null;
        }
        return false;
    }
    
    protected boolean isTopLevelMultiInstanceRoot(ExecutionEntity execution) {
        boolean topLevelMultiInstanceRoot = false;
        if (execution.isMultiInstanceRoot()) {
            topLevelMultiInstanceRoot = true;
            ExecutionEntity parentExecution = execution.getParent();
            while (parentExecution != null) {
                if (parentExecution.isMultiInstanceRoot()) {
                    topLevelMultiInstanceRoot = false;
                    break;
                }
                
                parentExecution = parentExecution.getParent();
            }
        }
        
        return topLevelMultiInstanceRoot;
    }
    
    protected boolean hasSameMultiInstanceConfig(FlowElement sourceElement, FlowElement targetElement) {
        MultiInstanceLoopCharacteristics sourceMIConfig = null;
        if (sourceElement instanceof Activity) {
            sourceMIConfig = ((Activity) sourceElement).getLoopCharacteristics();
        }
        
        MultiInstanceLoopCharacteristics targetMIConfig = null;
        if (targetElement instanceof Activity) {
            targetMIConfig = ((Activity) targetElement).getLoopCharacteristics();
        }
        
        if (sourceMIConfig == null || targetMIConfig == null) {
            return false;
        }
        
        if (sourceMIConfig.isSequential() != targetMIConfig.isSequential()) {
            return false;
        }
        
        if (sourceMIConfig.getLoopCardinality() != null && !sourceMIConfig.getLoopCardinality().equals(targetMIConfig.getLoopCardinality())) {
            return false;
        }
        
        if (targetMIConfig.getLoopCardinality() != null && !targetMIConfig.getLoopCardinality().equals(sourceMIConfig.getLoopCardinality())) {
            return false;
        }
        
        return true;
    }

    protected void processCreatedEventSubProcess(EventSubProcess eventSubProcess, ExecutionEntity eventSubProcessExecution, Set<String> movingExecutionIds, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();
        TimerJobService timerJobService = processEngineConfiguration.getJobServiceConfiguration().getTimerJobService();
        List<StartEvent> allStartEvents = eventSubProcess.findAllSubFlowElementInFlowMapOfType(StartEvent.class);

        for (StartEvent startEvent : allStartEvents) {
            if (!startEvent.getEventDefinitions().isEmpty()) {

                Collection<ExecutionEntity> inactiveExecutionsByProcessInstanceId = executionEntityManager.findInactiveExecutionsByProcessInstanceId(eventSubProcessExecution.getProcessInstanceId());
                Optional<ExecutionEntity> startEventExecution = inactiveExecutionsByProcessInstanceId.stream().filter(execution -> execution.getActivityId().equals(startEvent.getId())).findFirst();

                EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                List<EventSubscriptionEntity> eventSubscriptions = null;
                if (eventDefinition instanceof SignalEventDefinition) {
                    eventSubscriptions = eventSubscriptionService.findEventSubscriptionsByProcessInstanceAndActivityId(eventSubProcessExecution.getProcessInstanceId(), startEvent.getId(), SignalEventSubscriptionEntity.EVENT_TYPE);
                } else if (eventDefinition instanceof MessageEventDefinition) {
                    eventSubscriptions = eventSubscriptionService.findEventSubscriptionsByProcessInstanceAndActivityId(eventSubProcessExecution.getProcessInstanceId(), startEvent.getId(), MessageEventSubscriptionEntity.EVENT_TYPE);
                }

                boolean isOnlyRemainingExecutionAtParentScope = isOnlyRemainingExecutionAtParentScope(eventSubProcessExecution, movingExecutionIds, commandContext);

                // If its an interrupting eventSubProcess we don't register a subscription or startEvent executions and we make sure that they are removed if existed
                //Current eventSubProcess plus its startEvent
                if (startEvent.isInterrupting() || isOnlyRemainingExecutionAtParentScope) {
                    if (eventSubscriptions != null && !eventSubscriptions.isEmpty()) {
                        eventSubscriptions.forEach(eventSubscriptionService::deleteEventSubscription);
                    }
                    if (eventDefinition instanceof TimerEventDefinition && startEventExecution.isPresent()) {
                        List<TimerJobEntity> timerJobsByExecutionId = timerJobService.findTimerJobsByExecutionId(startEventExecution.get().getId());
                        timerJobsByExecutionId.forEach(timerJobService::deleteTimerJob);
                    }
                    if (startEventExecution.isPresent()) {
                        executionEntityManager.deleteExecutionAndRelatedData(startEventExecution.get(), DeleteReason.EVENT_SUBPROCESS_INTERRUPTING + "(" + startEvent.getId() + ")", false);
                    }

                    //Remove any other child of the parentScope
                    List<ExecutionEntity> childExecutions = executionEntityManager.collectChildren(eventSubProcessExecution.getParent());
                    for (int i = childExecutions.size() - 1; i >= 0; i--) {
                        ExecutionEntity childExecutionEntity = childExecutions.get(i);
                        if (!childExecutionEntity.isEnded() && !childExecutionEntity.getId().equals(eventSubProcessExecution.getId()) && !movingExecutionIds.contains(childExecutionEntity.getId())) {
                            executionEntityManager.deleteExecutionAndRelatedData(childExecutionEntity,
                                DeleteReason.EVENT_SUBPROCESS_INTERRUPTING + "(" + startEvent.getId() + ")", false);
                        }
                    }
                } else {
                    // For non-interrupting, we register a subscription and startEvent execution if they don't exist already
                    if (eventDefinition instanceof MessageEventDefinition messageEventDefinition && (eventSubscriptions == null || eventSubscriptions.isEmpty())) {
                        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(eventSubProcessExecution.getProcessDefinitionId());
                        if (bpmnModel.containsMessageId(messageEventDefinition.getMessageRef())) {
                            messageEventDefinition.setMessageRef(bpmnModel.getMessage(messageEventDefinition.getMessageRef()).getName());
                        }

                        ExecutionEntity messageExecution = processEngineConfiguration.getExecutionEntityManager().createChildExecution(eventSubProcessExecution.getParent());
                        messageExecution.setCurrentFlowElement(startEvent);
                        messageExecution.setEventScope(true);
                        messageExecution.setActive(false);

                        String messageName = EventDefinitionExpressionUtil.determineMessageName(commandContext, messageEventDefinition, null);
                        EventSubscriptionEntity messageSubscription = (EventSubscriptionEntity) eventSubscriptionService.createEventSubscriptionBuilder()
                                        .eventType(MessageEventSubscriptionEntity.EVENT_TYPE)
                                        .eventName(messageName)
                                        .executionId(messageExecution.getId())
                                        .processInstanceId(messageExecution.getProcessInstanceId())
                                        .activityId(messageExecution.getCurrentActivityId())
                                        .processDefinitionId(messageExecution.getProcessDefinitionId())
                                        .tenantId(messageExecution.getTenantId())
                                        .create();
                        
                        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(messageSubscription);
                        messageExecution.getEventSubscriptions().add(messageSubscription);
                        
                        processEngineConfiguration.getEventDispatcher()
                            .dispatchEvent(FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageSubscription.getActivityId(),
                                    messageSubscription.getEventName(), null, messageSubscription.getExecutionId(),
                                    messageSubscription.getProcessInstanceId(), messageSubscription.getProcessDefinitionId()),
                                    processEngineConfiguration.getEngineCfgKey());

                    }
                    if (eventDefinition instanceof SignalEventDefinition signalEventDefinition && (eventSubscriptions == null || eventSubscriptions.isEmpty())) {
                        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(eventSubProcessExecution.getProcessDefinitionId());
                        Signal signal = bpmnModel.getSignal(signalEventDefinition.getSignalRef());
                        if (signal != null) {
                            signalEventDefinition.setSignalRef(signal.getName());
                        }

                        ExecutionEntity signalExecution = processEngineConfiguration.getExecutionEntityManager().createChildExecution(eventSubProcessExecution.getParent());
                        signalExecution.setCurrentFlowElement(startEvent);
                        signalExecution.setEventScope(true);
                        signalExecution.setActive(false);

                        String eventName = EventDefinitionExpressionUtil.determineSignalName(commandContext, signalEventDefinition, bpmnModel, null);

                        EventSubscriptionEntity signalSubscription = (EventSubscriptionEntity) eventSubscriptionService.createEventSubscriptionBuilder()
                                        .eventType(SignalEventSubscriptionEntity.EVENT_TYPE)
                                        .eventName(eventName)
                                        .signal(signal)
                                        .executionId(signalExecution.getId())
                                        .processInstanceId(signalExecution.getProcessInstanceId())
                                        .activityId(signalExecution.getCurrentActivityId())
                                        .processDefinitionId(signalExecution.getProcessDefinitionId())
                                        .tenantId(signalExecution.getTenantId())
                                        .create();
                                        
                        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(signalSubscription);
                        signalExecution.getEventSubscriptions().add(signalSubscription);
                        
                        processEngineConfiguration.getEventDispatcher()
                            .dispatchEvent(FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalSubscription.getActivityId(),
                                    signalSubscription.getEventName(), null, signalSubscription.getExecutionId(),
                                    signalSubscription.getProcessInstanceId(), signalSubscription.getProcessDefinitionId()),
                                    processEngineConfiguration.getEngineCfgKey());

                    }

                    if (eventDefinition instanceof TimerEventDefinition timerEventDefinition) {
                        if (!startEventExecution.isPresent()) {

                            ExecutionEntity timerExecution = processEngineConfiguration.getExecutionEntityManager().createChildExecution(eventSubProcessExecution.getParent());
                            timerExecution.setCurrentFlowElement(startEvent);
                            timerExecution.setEventScope(true);
                            timerExecution.setActive(false);
                            TimerJobEntity timerJob = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition, startEvent, false, timerExecution, TriggerTimerEventJobHandler.TYPE,
                                TimerEventHandler.createConfiguration(startEvent.getId(), timerEventDefinition.getEndDate(), timerEventDefinition.getCalendarName()));
                            if (timerJob != null) {
                                timerJobService.scheduleTimerJob(timerJob);
                            }
                        }
                    }
                }
            }
        }
    }
    
    protected boolean sameBoundaryEventDefinition(BoundaryEvent sourceEvent, BoundaryEvent targetEvent) {
        if (sourceEvent.getEventDefinitions() != null && !sourceEvent.getEventDefinitions().isEmpty() && 
                (targetEvent.getEventDefinitions() == null || targetEvent.getEventDefinitions().isEmpty())) {
            
            return false;
        
        } else if ((sourceEvent.getEventDefinitions() == null || sourceEvent.getEventDefinitions().isEmpty()) && 
                targetEvent.getEventDefinitions() != null && !targetEvent.getEventDefinitions().isEmpty()) {
            
            return false;
        }
        
        if (sourceEvent.getEventDefinitions() != null && !sourceEvent.getEventDefinitions().isEmpty()) {
            EventDefinition sourceEventDef = sourceEvent.getEventDefinitions().get(0);
            EventDefinition targetEventDef = targetEvent.getEventDefinitions().get(0);
            
            if (!sourceEventDef.getClass().getName().equals(targetEventDef.getClass().getName())) {
                return false;
            }
            
            if (sourceEventDef instanceof SignalEventDefinition signalSourceDef) {
                SignalEventDefinition signalTargetDef = (SignalEventDefinition) targetEventDef;
                
                if (StringUtils.isNotEmpty(signalSourceDef.getSignalRef()) && signalSourceDef.getSignalRef().equals(signalTargetDef.getSignalRef())) {
                    return true;
                }
                
                if (StringUtils.isNotEmpty(signalSourceDef.getSignalExpression()) && signalSourceDef.getSignalExpression().equals(signalTargetDef.getSignalRef())) {
                    return true;
                }
            
            } else if (sourceEventDef instanceof MessageEventDefinition messageSourceDef) {
                MessageEventDefinition messageTargetDef = (MessageEventDefinition) targetEventDef;
                
                if (StringUtils.isNotEmpty(messageSourceDef.getMessageRef()) && messageSourceDef.getMessageRef().equals(messageTargetDef.getMessageRef())) {
                    return true;
                }
                
                if (StringUtils.isNotEmpty(messageSourceDef.getMessageExpression()) && messageSourceDef.getMessageExpression().equals(messageTargetDef.getMessageExpression())) {
                    return true;
                }
            
            } else if (sourceEventDef instanceof TimerEventDefinition timerSourceDef) {
                TimerEventDefinition timerTargetDef = (TimerEventDefinition) targetEventDef;
                
                if (StringUtils.isNotEmpty(timerSourceDef.getTimeCycle()) && timerSourceDef.getTimeCycle().equals(timerTargetDef.getTimeCycle())) {
                    return true;
                }
                
                if (StringUtils.isNotEmpty(timerSourceDef.getTimeDate()) && timerSourceDef.getTimeDate().equals(timerTargetDef.getTimeDate())) {
                    return true;
                }
                
                if (StringUtils.isNotEmpty(timerSourceDef.getTimeDuration()) && timerSourceDef.getTimeDuration().equals(timerTargetDef.getTimeDuration())) {
                    return true;
                }
            } 
            
        } else if (!sourceEvent.getExtensionElements().isEmpty() && !targetEvent.getExtensionElements().isEmpty()) {
            List<ExtensionElement> sourceEventTypeExtensionElements = sourceEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
            List<ExtensionElement> targetEventTypeExtensionElements = targetEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
            String sourceEventTypeValue = null;
            if (sourceEventTypeExtensionElements != null && !sourceEventTypeExtensionElements.isEmpty()) {
                sourceEventTypeValue = sourceEventTypeExtensionElements.get(0).getElementText();
            }
            
            String targetEventTypeValue = null;
            if (targetEventTypeExtensionElements != null && !targetEventTypeExtensionElements.isEmpty()) {
                targetEventTypeValue = targetEventTypeExtensionElements.get(0).getElementText();
            }
            
            if (StringUtils.isNotEmpty(sourceEventTypeValue) && StringUtils.isNotEmpty(targetEventTypeValue) && 
                    sourceEventTypeValue.equals(targetEventTypeValue)) {
                
                return true;
            }
        }
        
        return false;
    }

    protected boolean isOnlyRemainingExecutionAtParentScope(ExecutionEntity executionEntity, Set<String> ignoreExecutionIds, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        List<ExecutionEntity> siblingExecutions = executionEntityManager.findChildExecutionsByParentExecutionId(executionEntity.getParentId());
        return siblingExecutions.stream()
            .filter(ExecutionEntity::isActive)
            .filter(execution -> !execution.getId().equals(executionEntity.getId()))
            .filter(execution -> !ignoreExecutionIds.contains(execution.getId()))
            .count() == 0;
    }

    protected boolean isExpression(String variableName) {
        return variableName.startsWith("${") || variableName.startsWith("#{");
    }

    protected ProcessDefinition resolveProcessDefinition(String processDefinitionKey, Integer processDefinitionVersion, String tenantId, CommandContext commandContext) {
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
        ProcessDefinition processDefinition;
        if (processDefinitionVersion != null) {
            processDefinition = processDefinitionEntityManager.findProcessDefinitionByKeyAndVersionAndTenantId(processDefinitionKey, processDefinitionVersion, tenantId);
        } else {
            if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);
            } else {
                processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
            }
        }

        if (processDefinition == null) {
            DeploymentManager deploymentManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDeploymentManager();
            if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                processDefinition = deploymentManager.findDeployedLatestProcessDefinitionByKey(processDefinitionKey);
            } else {
                processDefinition = deploymentManager.findDeployedLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
            }
        }
        return processDefinition;
    }

    private String printFlowElementIds(Collection<FlowElementMoveEntry> flowElements) {
        return flowElements.stream().map(FlowElementMoveEntry::getNewFlowElement).map(FlowElement::getId).collect(Collectors.joining(","));
    }
}
