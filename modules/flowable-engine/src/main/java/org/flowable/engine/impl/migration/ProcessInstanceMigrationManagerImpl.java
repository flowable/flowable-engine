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
package org.flowable.engine.impl.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.dynamic.AbstractDynamicStateManager;
import org.flowable.engine.impl.dynamic.MoveExecutionEntityContainer;
import org.flowable.engine.impl.dynamic.ProcessInstanceChangeState;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.migration.ProcessInstanceActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationManagerImpl extends AbstractDynamicStateManager implements ProcessInstanceMigrationManager {

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstancesOfProcessDefinition(String procDefKey, int procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        // Must first resolve the Id of the processDefinition
        ProcessDefinition processDefinition = resolveProcessDefinition(procDefKey, procDefVer, procDefTenantId, commandContext);
        if (processDefinition != null) {
            ProcessInstanceMigrationValidationResult validationResult = validateMigrateProcessInstancesOfProcessDefinition(processDefinition.getId(), document, commandContext);
            return validationResult;

        } else {
            ProcessInstanceMigrationValidationResult validationResult = new ProcessInstanceMigrationValidationResult();
            validationResult.addValidationMessage("Cannot find the process definition to migrate from");
            return validationResult;
        }
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessInstanceMigrationValidationResult validationResult = new ProcessInstanceMigrationValidationResult();
        //Check that the processDefinition exists and get its associated BpmnModel
        ProcessDefinition processDefinition = resolveProcessDefinition(document, commandContext);
        if (processDefinition == null) {
            validationResult.addValidationMessage("Cannot find the process definition to migrate to " + printProcessDefinitionIdentifierMessage(document));
        } else {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            if (bpmnModel == null) {
                validationResult.addValidationMessage("Cannot find the Bpmn model of the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
            } else {
                ProcessInstanceQueryImpl processInstanceQueryByProcessDefinitionId = new ProcessInstanceQueryImpl().processDefinitionId(processDefinitionId);
                ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
                List<ProcessInstance> processInstances = executionEntityManager.findProcessInstanceByQueryCriteria(processInstanceQueryByProcessDefinitionId);

                for (ProcessInstance processInstance : processInstances) {
                    doValidateProcessInstanceMigration(processInstance.getId(), processDefinition.getTenantId(), bpmnModel, document, validationResult, commandContext);
                }
            }
        }
        return validationResult;
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessInstanceMigrationValidationResult validationResult = new ProcessInstanceMigrationValidationResult();
        //Check that the processDefinition exists and get its associated BpmnModel
        ProcessDefinition processDefinition = resolveProcessDefinition(document, commandContext);
        if (processDefinition == null) {
            validationResult.addValidationMessage(("Cannot find the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document)));
        } else {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            if (bpmnModel == null) {
                validationResult.addValidationMessage("Cannot find the Bpmn model of the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
            } else {
                doValidateProcessInstanceMigration(processInstanceId, processDefinition.getTenantId(), bpmnModel, document, validationResult, commandContext);
            }
        }
        return validationResult;
    }

    @Override
    public void migrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessDefinition processDefinition = resolveProcessDefinition(document, commandContext);
        if (processDefinition == null) {
            throw new FlowableException("Cannot find the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
        }

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
        if (bpmnModel == null) {
            throw new FlowableException("Cannot find the Bpmn model of the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
        }

        doMigrateProcessInstance(processInstanceId, processDefinition, document, commandContext);
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String procDefKey, int procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessDefinition processDefinition = resolveProcessDefinition(procDefKey, procDefVer, procDefTenantId, commandContext);
        if (processDefinition != null) {
            migrateProcessInstancesOfProcessDefinition(processDefinition.getId(), document, commandContext);
        }
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessDefinition processDefinition = resolveProcessDefinition(document, commandContext);
        if (processDefinition == null) {
            throw new FlowableException("Cannot find the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
        }

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
        if (bpmnModel == null) {
            throw new FlowableException("Cannot find the Bpmn model of the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
        }

        ProcessInstanceQueryImpl processInstanceQueryByProcessDefinitionId = new ProcessInstanceQueryImpl().processDefinitionId(processDefinitionId);
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        List<ProcessInstance> processInstances = executionEntityManager.findProcessInstanceByQueryCriteria(processInstanceQueryByProcessDefinitionId);

        for (ProcessInstance processInstance : processInstances) {
            doMigrateProcessInstance(processInstance.getId(), processDefinition, document, commandContext);
        }

    }

    protected void doMigrateProcessInstance(String processInstanceId, ProcessDefinition procDefToMigrateTo, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        LOGGER.debug("Start migration of process instance with Id:'" + processInstanceId + "' to " + printProcessDefinitionIdentifierMessage(document));

        //Check processExecution and processDefinition tenant
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processExecution = executionEntityManager.findById(processInstanceId);

        //TODO WIP - ChangeActivityStateBuilder preparation could be moved inside ProcessInstanceChangeState, and make ProcessInstanceChangeState a builder with two build methods,
        //TODO WIP - one for the ChangeSate that accepts a ChangeActivityStateBuilder already and the other to be used here to prepare it, in that way, the list of executions not "migrated"
        //TODO WIP - that only need to change the processDefinition reference, can be omitted as parameter here and contained already in the ProcessInstanceChangeState
        List<ExecutionEntity> notMigratedExecutions = new ArrayList<>();
        ChangeActivityStateBuilderImpl changeActivityStateBuilder = prepareChangeStateBuilder(processInstanceId, procDefToMigrateTo, document, notMigratedExecutions, commandContext);

        LOGGER.debug("Migrating activity executions");
        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = resolveMoveExecutionEntityContainers(changeActivityStateBuilder, Optional.of(procDefToMigrateTo.getId()), commandContext);

        ProcessInstanceChangeState processInstanceChangeState = new ProcessInstanceChangeState()
            .setProcessInstanceId(processInstanceId)
            .setProcessDefinitionToMigrateTo(procDefToMigrateTo)
            .setMoveExecutionEntityContainers(moveExecutionEntityContainerList)
            .setProcessInstanceVariables(changeActivityStateBuilder.getProcessInstanceVariables())
            .setLocalVariables(changeActivityStateBuilder.getLocalVariables());

        LOGGER.debug("Updating Process definition reference of root execution with id:'" + processExecution.getId() + "' to '" + procDefToMigrateTo.getId() + "'");
        processExecution.setProcessDefinitionId(procDefToMigrateTo.getId());

        doMoveExecutionState(processInstanceChangeState, commandContext);

        if (!notMigratedExecutions.isEmpty()) {
            LOGGER.debug("Updating Process definition reference on remaining executions");
            updateProcessDefinitionReferenceOfExecutionsAndRelatedData(notMigratedExecutions, procDefToMigrateTo.getId(), commandContext);
        }

        LOGGER.debug("Updating Process definition reference in history");
        changeProcessDefinitionReferenceOfHistory(processExecution, procDefToMigrateTo, commandContext);

        LOGGER.debug("Process migration ended for process instance with Id:'" + processInstanceId + "'");
    }

    @Override
    protected Map<String, List<ExecutionEntity>> resolveActiveEmbeddedSubProcesses(String processInstanceId, CommandContext commandContext) {
        return Collections.emptyMap();
    }

    @Override
    protected boolean isDirectFlowElementExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        //Activities inside or that are MultiInstance cannot be migrated directly, as it is better to trigger the MultiInstanceBehavior using the agenda, directMigration skips the agenda
        return (currentFlowElement instanceof UserTask && newFlowElement instanceof UserTask ||
            currentFlowElement instanceof ReceiveTask && newFlowElement instanceof ReceiveTask) &&
            (((Task) currentFlowElement).getLoopCharacteristics() == null && !isFlowElementInsideMultiInstance(currentFlowElement)) &&
            (((Task) newFlowElement).getLoopCharacteristics() == null && !isFlowElementInsideMultiInstance(newFlowElement));
    }

    protected ChangeActivityStateBuilderImpl prepareChangeStateBuilder(String processInstanceId, ProcessDefinition procDefToMigrateTo, ProcessInstanceMigrationDocument document, List<ExecutionEntity> notMigratedExecutions, CommandContext commandContext) {
        LOGGER.debug("Start migration of process instance with Id:'" + processInstanceId + "' to " + printProcessDefinitionIdentifierMessage(document));

        //Check processExecution and processDefinition tenant
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processExecution = executionEntityManager.findById(processInstanceId);

        String procDefTenantId = procDefToMigrateTo.getTenantId();
        if (!isSameTenant(processExecution.getTenantId(), procDefTenantId)) {
            throw new FlowableException("Tenant mismatch between Process Instance ('" + processExecution.getTenantId() + "') and Process Definition ('" + procDefTenantId + "') to migrate to");
        }

        ChangeActivityStateBuilderImpl changeActivityStateBuilder = new ChangeActivityStateBuilderImpl();
        changeActivityStateBuilder.processInstanceId(processInstanceId);

        //Current process executions mapped by ActivityId
        Map<String, List<ExecutionEntity>> filteredExecutionsByActivityId = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId)
            .stream()
            .filter(executionEntity -> executionEntity.getCurrentActivityId() != null)
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof SubProcess && ((SubProcess) executionEntity.getCurrentFlowElement()).getLoopCharacteristics() == null))
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof BoundaryEvent))
            .collect(Collectors.groupingBy(ExecutionEntity::getCurrentActivityId));

        LOGGER.debug("Preparing ActivityChangeState builder for '" + filteredExecutionsByActivityId.size() + "' distinct activities");

        Set<String> mappedFromActivities = ProcessInstanceMigrationDocumentImpl.extractMappedFromActivities(document.getActivityMigrationMappings());

        //Partition the executions by whether or not they are explicitly mapped
        Map<Boolean, List<String>> partitionedExecutionActivityIds = filteredExecutionsByActivityId.keySet()
            .stream()
            .collect(Collectors.partitioningBy(mappedFromActivities::contains));
        List<String> executionActivityIdsToAutoMap = partitionedExecutionActivityIds.get(false);
        Set<String> executionActivityIdsToMapExplicitly = new HashSet<>(partitionedExecutionActivityIds.get(true));

        BpmnModel newModel = ProcessDefinitionUtil.getBpmnModel(procDefToMigrateTo.getId());
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceId);
        BpmnModel currentModel = ProcessDefinitionUtil.getBpmnModel(processInstanceExecution.getProcessDefinitionId());

        //Auto Mapping
        LOGGER.debug("Process AutoMapping for '" + executionActivityIdsToAutoMap.size() + "' activity executions");

        ListIterator<String> iteratorOfActivityIdsForAutoMap = executionActivityIdsToAutoMap.listIterator();
        while (iteratorOfActivityIdsForAutoMap.hasNext()) {
            String currentActivityId = iteratorOfActivityIdsForAutoMap.next();
            FlowElement currentModelFlowElement = currentModel.getFlowElement(currentActivityId);

            //If the activity is embedded in a MI container, we try to auto-map its MI parent instead...
            Optional<FlowElement> flowElementMultiInstanceParent = getFlowElementMultiInstanceParent(currentModelFlowElement);
            if (flowElementMultiInstanceParent.isPresent()) {
                String miParentActivityId = flowElementMultiInstanceParent.get().getId();
                //Check if there's no explicit mapping, activities with MI parents should not be auto-mapped (TODO WIP - add as a validator warning)
                //TODO ... miSubProcess are added to the autoMap list
                if (!mappedFromActivities.contains(currentActivityId)) {
//                    //We add the parent container to the list to check for auto mapping next
//                    if (!executionActivityIdsToAutoMap.contains(miParentActivityId)) {
//                        iteratorOfActivityIdsForAutoMap.add(miParentActivityId);
//                        iteratorOfActivityIdsForAutoMap.previous();
//                    }
                    notMigratedExecutions.addAll(filteredExecutionsByActivityId.get(currentActivityId));
                    continue;
                }
            }

            if (!mappedFromActivities.contains(currentActivityId)) {
                if (!isActivityIdInProcessDefinitionModel(currentActivityId, newModel)) {
                    throw new FlowableException("Migration Activity mapping missing for activity definition Id: '" + currentActivityId + "'");
                } else {
                    FlowElement newModelFlowElement = newModel.getFlowElement(currentActivityId);

                    //Activities cannot be "moved" inside a MI container in the new model
                    Optional<FlowElement> newFlowElementMultiInstanceParent = getFlowElementMultiInstanceParent(newModelFlowElement);
                    if (newFlowElementMultiInstanceParent.isPresent()) {
                        String miContainerId = newFlowElementMultiInstanceParent.get().getId();
                        throw new FlowableException("Cannot auto-map Activity '" + currentActivityId + "' inside a new MultiInstance container '" + miContainerId + "' in the new Model");
                    }

                    if (isMultiInstanceFlowElement(currentModelFlowElement)) {
                        //It must have the same Loop Characteristics

                        MultiInstanceLoopCharacteristics newLoopCharacteristics = ((Activity) newModelFlowElement).getLoopCharacteristics();
                        MultiInstanceLoopCharacteristics currentLoopCharacteristics = ((Activity) currentModelFlowElement).getLoopCharacteristics();
                        if (newLoopCharacteristics == null || newLoopCharacteristics.isSequential() != currentLoopCharacteristics.isSequential()) {
                            throw new FlowableException("Cannot auto-map Mi activity '" + currentActivityId + "'"
                                + " from: '" + (currentLoopCharacteristics.isSequential() ? "sequential" : "parallel") + "'"
                                + " to: '" + (newLoopCharacteristics.isSequential() ? "sequential" : "parallel") + "'");
                        }
                        notMigratedExecutions.addAll(filteredExecutionsByActivityId.get(currentActivityId));
                        //TODO WIP - should be able to flag the changeState as AutoMap to not delete re-create the execution
                    } else {
                        List<ExecutionEntity> executionEntities = filteredExecutionsByActivityId.get(currentActivityId);
                        if (executionEntities.size() > 1) {
                            List<String> executionIds = executionEntities.stream().map(ExecutionEntity::getId).collect(Collectors.toList());
                            changeActivityStateBuilder.moveExecutionsToSingleActivityId(executionIds, currentActivityId);
                        } else {
                            changeActivityStateBuilder.moveExecutionToActivityId(executionEntities.get(0).getId(), currentActivityId);
                        }
                    }
                }
            } else {
                //Case of MI containers explicitly mapped
                if (isMultiInstanceFlowElement(currentModelFlowElement) && currentModelFlowElement instanceof SubProcess) {
                    executionActivityIdsToMapExplicitly.add(currentActivityId);
                    //The root executions are the ones to migrate and should be explicitly mapped
                    List<ExecutionEntity> miRootExecutions = (List<ExecutionEntity>) executionEntityManager.findInactiveExecutionsByActivityIdAndProcessInstanceId(currentActivityId, processInstanceId);
                    filteredExecutionsByActivityId.put(currentActivityId, miRootExecutions);
                }
            }
        }

        //Explicit Mapping - Iterates over the provided mappings instead to keep the explicit migration order
        List<ProcessInstanceActivityMigrationMapping> activityMigrationMappings = document.getActivityMigrationMappings();

        LOGGER.debug("Process explicit mapping for '" + executionActivityIdsToMapExplicitly.size() + "' activity executions");
        for (ProcessInstanceActivityMigrationMapping activityMapping : activityMigrationMappings) {
            String fromActivityId;
            String toActivityId;
            String newAssignee = activityMapping.getWithNewAssignee();

            if (activityMapping instanceof ProcessInstanceActivityMigrationMapping.OneToOneMapping) {
                fromActivityId = ((ProcessInstanceActivityMigrationMapping.OneToOneMapping) activityMapping).getFromActivityId();
                toActivityId = ((ProcessInstanceActivityMigrationMapping.OneToOneMapping) activityMapping).getToActivityId();
                if (executionActivityIdsToMapExplicitly.contains(fromActivityId)) {
                    changeActivityStateBuilder.moveActivityIdTo(fromActivityId, toActivityId, newAssignee);
                    executionActivityIdsToMapExplicitly.remove(fromActivityId);
                }
            } else if (activityMapping instanceof ProcessInstanceActivityMigrationMapping.OneToManyMapping) {
                fromActivityId = ((ProcessInstanceActivityMigrationMapping.OneToManyMapping) activityMapping).getFromActivityId();
                List<String> toActivityIds = activityMapping.getToActivityIds();
                if (executionActivityIdsToMapExplicitly.contains(fromActivityId)) {
                    changeActivityStateBuilder.moveSingleActivityIdToActivityIds(fromActivityId, toActivityIds, newAssignee);
                    executionActivityIdsToMapExplicitly.remove(fromActivityId);
                }
            } else if (activityMapping instanceof ProcessInstanceActivityMigrationMapping.ManyToOneMapping) {
                List<String> fromActivityIds = activityMapping.getFromActivityIds();
                toActivityId = ((ProcessInstanceActivityMigrationMapping.ManyToOneMapping) activityMapping).getToActivityId();
                List<String> executionIds = new ArrayList<>();
                for (String activityId : fromActivityIds) {
                    if (executionActivityIdsToMapExplicitly.contains(activityId)) {
                        List<ExecutionEntity> executionEntities = filteredExecutionsByActivityId.get(activityId);
                        executionIds.addAll(executionEntities.stream().map(ExecutionEntity::getId).collect(Collectors.toList()));
                        executionActivityIdsToMapExplicitly.remove(activityId);
                    }
                }
                changeActivityStateBuilder.moveExecutionsToSingleActivityId(executionIds, toActivityId, newAssignee);
            } else {
                throw new FlowableException("Unknown Activity Mapping or not implemented yet!!!");
            }
        }

        if (!executionActivityIdsToMapExplicitly.isEmpty()) {
            throw new FlowableException("Migration Activity mapping missing for activity definition Ids:'" + Arrays.toString(executionActivityIdsToMapExplicitly.toArray()) + "'");
        }

        //Assign variables to the changeStateBuilder
        document.getActivitiesLocalVariables().forEach(changeActivityStateBuilder::localVariables);
        changeActivityStateBuilder.processVariables(document.getProcessInstanceVariables());

        return changeActivityStateBuilder;
    }

    protected boolean isSameTenant(String tenantId1, String tenantId2) {

        if (tenantId1 != null && tenantId2 != null) {
            return tenantId1.equals(tenantId2);
        } else if (tenantId1 == null && tenantId2 == null) {
            return true;
        }
        return false;
    }

    protected void updateProcessDefinitionReferenceOfExecutionsAndRelatedData(List<ExecutionEntity> executions, String processDefinitionId, CommandContext commandContext) {

        for (ExecutionEntity execution : executions) {
            execution.setProcessDefinitionId(processDefinitionId);

            List<TaskEntity> tasksByExecutionId = CommandContextUtil.getTaskService(commandContext).findTasksByExecutionId(execution.getId());
            if (tasksByExecutionId != null) {
                tasksByExecutionId.forEach(taskEntity -> taskEntity.setProcessDefinitionId(processDefinitionId));
            }

            List<TimerJobEntity> timerJobsByExecutionId = CommandContextUtil.getTimerJobService(commandContext).findTimerJobsByExecutionId(execution.getId());
            if (timerJobsByExecutionId != null) {
                timerJobsByExecutionId.forEach(timerJobEntity -> timerJobEntity.setProcessDefinitionId(processDefinitionId));
            }

            List<JobEntity> jobsByExecutionId = CommandContextUtil.getJobService(commandContext).findJobsByExecutionId(execution.getId());
            if (jobsByExecutionId != null) {
                jobsByExecutionId.forEach(jobEntity -> jobEntity.setProcessDefinitionId(processDefinitionId));
            }

            List<EventSubscriptionEntity> eventSubscriptionsByExecution = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByExecution(execution.getId());
            if (eventSubscriptionsByExecution != null) {
                eventSubscriptionsByExecution.forEach(eventSubscriptionEntity -> eventSubscriptionEntity.setProcessDefinitionId(processDefinitionId));
            }
        }
    }

    protected void changeProcessDefinitionReferenceOfHistory(ProcessInstance processInstance, ProcessDefinition processDefinition, CommandContext commandContext) {
        HistoryLevel currentHistoryLevel = CommandContextUtil.getProcessEngineConfiguration(commandContext).getHistoryLevel();
        if (currentHistoryLevel.isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoryManager historyManager = CommandContextUtil.getHistoryManager(commandContext);
            historyManager.updateProcessDefinitionIdInHistory((ProcessDefinitionEntity) processDefinition, (ExecutionEntity) processInstance);
        }
    }

    protected void doValidateProcessInstanceMigration(String processInstanceId, String tenantId, BpmnModel newModel, ProcessInstanceMigrationDocument document, ProcessInstanceMigrationValidationResult validationResult, CommandContext commandContext) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        //Check that the processInstance exists
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceId);
        if (processInstanceExecution == null) {
            validationResult.addValidationMessage("Cannot find process instance with id:'" + processInstanceId + "'");
            return;
        }

        //Check processExecution and processDefinition tenant
        if (!isSameTenant(processInstanceExecution.getTenantId(), tenantId)) {
            validationResult.addValidationMessage("Tenant mismatch between Process Instance ('" + processInstanceExecution.getTenantId() + "') and Process Definition ('" + tenantId + "') to migrate to");
            return;
        }

        doValidateActivityMappings(processInstanceId, document.getActivityMigrationMappings(), newModel, validationResult, commandContext);
    }

    protected void doValidateActivityMappings(String processInstanceId, List<ProcessInstanceActivityMigrationMapping> activityMappings, BpmnModel newModel, ProcessInstanceMigrationValidationResult validationResult, CommandContext commandContext) {

        HashMap<String, List<String>> targetActivitiesBySrcActivity = new HashMap<>();
        for (ProcessInstanceActivityMigrationMapping activityMapping : activityMappings) {
            activityMapping.getFromActivityIds().forEach(fromActivity -> targetActivitiesBySrcActivity.merge(fromActivity, activityMapping.getToActivityIds(), (currentList, newList) -> {
                currentList.addAll(newList);
                return currentList;
            }));
        }

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceId);
        BpmnModel currentModel = ProcessDefinitionUtil.getBpmnModel(processInstanceExecution.getProcessDefinitionId());

        //For each "running" active activity of the processInstance, check that there's a mapping defined or if it can be found in the new definition (auto-mapped by activity id)
        List<ExecutionEntity> activeExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId).stream()
            .filter(ExecutionEntity::isActive)
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof SubProcess))
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof BoundaryEvent))
            .collect(Collectors.toList());
        for (ExecutionEntity execution : activeExecutions) {
            if (execution.getCurrentActivityId() != null) {
                if (!targetActivitiesBySrcActivity.containsKey(execution.getCurrentActivityId()) && !isActivityIdInProcessDefinitionModel(execution.getCurrentActivityId(), newModel)) {
                    //Check if the execution is inside a MultiInstance parent and if such is mapped instead
                    FlowElement currentModelFlowElement = currentModel.getFlowElement(execution.getCurrentActivityId());
                    Optional<String> flowElementMultiInstanceParentId = getFlowElementMultiInstanceParent(currentModelFlowElement).map(FlowElement::getId);
                    if (!flowElementMultiInstanceParentId.isPresent() || !targetActivitiesBySrcActivity.containsKey(flowElementMultiInstanceParentId.get())) {
                        validationResult.addValidationMessage("Process instance (id:'" + processInstanceId + "') has a running Activity (id:'" + execution.getCurrentActivityId() + "') that is not mapped for migration (Or its Multi-Instance parent");
                    }
                }

                if (targetActivitiesBySrcActivity.containsKey(execution.getCurrentActivityId())) {
                    List<String> targetActivityIds = targetActivitiesBySrcActivity.get(execution.getCurrentActivityId());
                    for (String targetActivityId : targetActivityIds) {
                        if (!isActivityIdInProcessDefinitionModel(targetActivityId, newModel)) {
                            validationResult.addValidationMessage("The target mapping of " + execution.getCurrentActivityId() + " to " + targetActivityId + " can not be found in the target process definition");
                        }
                    }
                }
            }
        }
    }

    protected ProcessDefinition resolveProcessDefinition(ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        if (document.getMigrateToProcessDefinitionId() != null) {
            ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
            return processDefinitionEntityManager.findById(document.getMigrateToProcessDefinitionId());
        } else {
            document.getMigrateToProcessDefinitionTenantId();
            return resolveProcessDefinition(document.getMigrateToProcessDefinitionKey(), document.getMigrateToProcessDefinitionVersion(), document.getMigrateToProcessDefinitionTenantId(), commandContext);
        }
    }

    protected ProcessDefinition resolveProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, CommandContext commandContext) {
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
        return processDefinitionEntityManager.findProcessDefinitionByKeyAndVersionAndTenantId(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId);
    }

    protected boolean isActivityIdInProcessDefinitionModel(String activityId, BpmnModel bpmnModel) {
        return bpmnModel.getFlowElement(activityId) != null;
    }

    protected String printProcessDefinitionIdentifierMessage(ProcessInstanceMigrationDocument document) {
        String id = document.getMigrateToProcessDefinitionId();
        String key = document.getMigrateToProcessDefinitionKey();
        Integer version = document.getMigrateToProcessDefinitionVersion();
        String tenantId = document.getMigrateToProcessDefinitionTenantId();

        return "process definition identified by [id:'" + id + "'] or [key:'" + key + "', version:'" + version + "', tenantId:'" + tenantId + "']";
    }

    @Override
    protected boolean isSubProcessAncestorOfAnyExecution(String subProcessId, List<ExecutionEntity> currentExecutions) {
        //recreates all subProcesses
        return false;
    }

    @Override
    protected boolean isSubProcessUsedInNewFlowElements(String subProcessId, Collection<MoveExecutionEntityContainer.FlowElementMoveEntry> moveToFlowElements) {
        //recreates all subProcesses
        return false;
    }
}

