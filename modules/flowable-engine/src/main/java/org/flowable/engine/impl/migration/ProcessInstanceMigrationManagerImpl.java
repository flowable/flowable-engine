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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
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
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationManagerImpl extends AbstractDynamicStateManager implements ProcessInstanceMigrationManager {

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstancesOfProcessDefinition(String procDefKey, int procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        // Must first resolve the Id of the processDefinition
        ProcessDefinition processDefinition = resolveProcessDefinition(procDefKey, procDefVer, procDefTenantId, commandContext);
        if (processDefinition != null) {
            return validateMigrateProcessInstancesOfProcessDefinition(processDefinition.getId(), document, commandContext);
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

        doValidateActivityMappings(processInstanceId, document.getActivityMigrationMappings(), newModel, document, validationResult, commandContext);
    }

    protected void doValidateActivityMappings(String processInstanceId, List<ActivityMigrationMapping> activityMappings, BpmnModel newModel, ProcessInstanceMigrationDocument document, ProcessInstanceMigrationValidationResult validationResult, CommandContext commandContext) {

        HashMap<String, ActivityMigrationMapping> activityMappingByFromActivityId = new HashMap<>();
        for (ActivityMigrationMapping activityMigrationMapping : activityMappings) {
            for (String fromActivityId : activityMigrationMapping.getFromActivityIds()) {
                activityMappingByFromActivityId.put(fromActivityId, activityMigrationMapping);
            }
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

                //Check auto-mapping
                if (!activityMappingByFromActivityId.containsKey(execution.getCurrentActivityId()) && !isActivityIdInProcessDefinitionModel(execution.getCurrentActivityId(), newModel)) {
                    //Check if the execution is inside a MultiInstance parent and if such is mapped instead
                    FlowElement currentModelFlowElement = currentModel.getFlowElement(execution.getCurrentActivityId());
                    Optional<String> flowElementMultiInstanceParentId = getFlowElementMultiInstanceParentId(currentModelFlowElement);
                    if (!flowElementMultiInstanceParentId.isPresent() || !activityMappingByFromActivityId.containsKey(flowElementMultiInstanceParentId.get())) {
                        validationResult.addValidationMessage("Process instance (id:'" + processInstanceId + "') has a running Activity (id:'" + execution.getCurrentActivityId() + "') that is not mapped for migration (Or its Multi-Instance parent)");
                        continue;
                    }
                }

                //Check explicit mapping
                if (activityMappingByFromActivityId.containsKey(execution.getCurrentActivityId())) {
                    ActivityMigrationMapping mapping = activityMappingByFromActivityId.get(execution.getCurrentActivityId());
                    BpmnModel mappingModel = newModel;

                    if (mapping.isToCallActivity()) {
                        FlowElement callActivityFlowElement = newModel.getFlowElement(mapping.getToCallActivityId());
                        if (callActivityFlowElement != null && callActivityFlowElement instanceof CallActivity) {
                            CallActivity callActivity = (CallActivity) callActivityFlowElement;
                            ProcessDefinition mappingProcDef = resolveProcessDefinition(callActivity.getCalledElement(), mapping.getCallActivityProcessDefinitionVersion(), document.getMigrateToProcessDefinitionTenantId(), commandContext);
                            mappingModel = ProcessDefinitionUtil.getBpmnModel(mappingProcDef.getId());
                        } else {
                            validationResult.addValidationMessage("There's no call activity element with id '" + mapping.getToCallActivityId() + "' in the process definition with id '" + mappingModel.getMainProcess().getId() + "'");
                            continue;
                        }
                    }

                    List<String> toActivityIds = mapping.getToActivityIds();
                    for (String targetActivityId : toActivityIds) {
                        if (!isActivityIdInProcessDefinitionModel(targetActivityId, mappingModel)) {
                            validationResult.addValidationMessage("The target mapping for " + execution.getCurrentActivityId() + " to " + targetActivityId + " can not be found in the process definition with id '" + mappingModel.getMainProcess().getId() + "'");
                        }
                    }
                }
            }
        }
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

        ChangeActivityStateBuilderImpl changeActivityStateBuilder = prepareChangeStateBuilder(processInstanceId, procDefToMigrateTo, document, commandContext);

        LOGGER.debug("Updating Process definition reference of root execution with id:'" + processExecution.getId() + "' to '" + procDefToMigrateTo.getId() + "'");
        processExecution.setProcessDefinitionId(procDefToMigrateTo.getId());

        LOGGER.debug("Migrating activity executions");
        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = resolveMoveExecutionEntityContainers(changeActivityStateBuilder, Optional.of(procDefToMigrateTo.getId()), commandContext);

        ProcessInstanceChangeState processInstanceChangeState = new ProcessInstanceChangeState()
            .setProcessInstanceId(processInstanceId)
            .setProcessDefinitionToMigrateTo(procDefToMigrateTo)
            .setMoveExecutionEntityContainers(moveExecutionEntityContainerList)
            .setProcessInstanceVariables(changeActivityStateBuilder.getProcessInstanceVariables())
            .setLocalVariables(changeActivityStateBuilder.getLocalVariables());

        doMoveExecutionState(processInstanceChangeState, commandContext);

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
            (((Task) currentFlowElement).getLoopCharacteristics() == null && !getFlowElementMultiInstanceParentId(currentFlowElement).isPresent()) &&
            (((Task) newFlowElement).getLoopCharacteristics() == null && !getFlowElementMultiInstanceParentId(newFlowElement).isPresent());
    }

    protected ChangeActivityStateBuilderImpl prepareChangeStateBuilder(String processInstanceId, ProcessDefinition procDefToMigrateTo, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
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

        //Current executions to migrate...
        Map<String, List<ExecutionEntity>> filteredExecutionsByActivityId = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId)
            .stream()
            .filter(executionEntity -> executionEntity.getCurrentActivityId() != null)
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof SubProcess))
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof BoundaryEvent))
            .collect(Collectors.groupingBy(ExecutionEntity::getCurrentActivityId));

        LOGGER.debug("Preparing ActivityChangeState builder for '" + filteredExecutionsByActivityId.size() + "' distinct activities");

        Set<String> mappedFromActivities = ProcessInstanceMigrationDocumentImpl.extractMappedFromActivities(document.getActivityMigrationMappings());

        //Partition the executions by Explicitly mapped or not
        Map<Boolean, List<String>> partitionedExecutionActivityIds = filteredExecutionsByActivityId.keySet()
            .stream()
            .collect(Collectors.partitioningBy(mappedFromActivities::contains));
        List<String> executionActivityIdsToAutoMap = partitionedExecutionActivityIds.get(false);
        List<String> executionActivityIdsToMapExplicitly = partitionedExecutionActivityIds.get(true);

        BpmnModel newModel = ProcessDefinitionUtil.getBpmnModel(procDefToMigrateTo.getId());
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceId);
        BpmnModel currentModel = ProcessDefinitionUtil.getBpmnModel(processInstanceExecution.getProcessDefinitionId());

        //Auto Mapping
        LOGGER.debug("Process AutoMapping for '" + executionActivityIdsToAutoMap.size() + "' activity executions");
        for (String activityId : executionActivityIdsToAutoMap) {
            FlowElement currentModelFlowElement = currentModel.getFlowElement(activityId);
            Optional<String> flowElementMultiInstanceParentId = getFlowElementMultiInstanceParentId(currentModelFlowElement);
            if (flowElementMultiInstanceParentId.isPresent() && mappedFromActivities.contains(flowElementMultiInstanceParentId.get())) {
                //Add the parent MI execution activity Id to be explicitly mapped...
                if (!executionActivityIdsToMapExplicitly.contains(flowElementMultiInstanceParentId.get())) {
                    executionActivityIdsToMapExplicitly.add(flowElementMultiInstanceParentId.get());
                }
                //The root executions are the ones to migrate and are explicitly mapped
                List<ExecutionEntity> miRootExecutions = (List<ExecutionEntity>) executionEntityManager.findInactiveExecutionsByActivityIdAndProcessInstanceId(flowElementMultiInstanceParentId.get(), processInstanceId);
                filteredExecutionsByActivityId.put(flowElementMultiInstanceParentId.get(), miRootExecutions);
            } else {
                LOGGER.debug("Checking execution(s) - activityId:'" + activityId);
                if (isActivityIdInProcessDefinitionModel(activityId, newModel)) {
                    LOGGER.debug("Auto mapping activity '" + activityId + "'");
                    List<ExecutionEntity> executionEntities = filteredExecutionsByActivityId.get(activityId);
                    if (executionEntities.size() > 1) {
                        List<String> executionIds = executionEntities.stream().map(ExecutionEntity::getId).collect(Collectors.toList());
                        changeActivityStateBuilder.moveExecutionsToSingleActivityId(executionIds, activityId);
                    } else {
                        changeActivityStateBuilder.moveExecutionToActivityId(executionEntities.get(0).getId(), activityId);
                    }
                } else {
                    throw new FlowableException("Migration Activity mapping missing for activity definition Id:'" + activityId + "' or its MI Parent");
                }
            }
        }

        //Explicit Mapping - Iterates over the provided mappings instead to keep the explicit migration order
        List<ActivityMigrationMapping> activityMigrationMappings = document.getActivityMigrationMappings();

        LOGGER.debug("Process explicit mapping for '" + executionActivityIdsToMapExplicitly.size() + "' activity executions");
        for (ActivityMigrationMapping activityMapping : activityMigrationMappings) {
            String fromActivityId;
            String toActivityId;
            String newAssignee;

            if (activityMapping instanceof ActivityMigrationMapping.OneToOneMapping) {
                fromActivityId = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getFromActivityId();
                toActivityId = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getToActivityId();
                newAssignee = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getWithNewAssignee();
                if (executionActivityIdsToMapExplicitly.contains(fromActivityId)) {
                    if (activityMapping.isToCallActivity()) {
                        //TODO - WIP need to handle different versions of the subProcess definition
                        changeActivityStateBuilder.moveActivityIdToSubProcessInstanceActivityId(fromActivityId, toActivityId, activityMapping.getToCallActivityId(), newAssignee);
                    } else if (activityMapping.isToParentProcess()) {
                        changeActivityStateBuilder.moveActivityIdToParentActivityId(fromActivityId, toActivityId, newAssignee);
                    } else {
                        changeActivityStateBuilder.moveActivityIdTo(fromActivityId, toActivityId, newAssignee);
                    }
                    executionActivityIdsToMapExplicitly.remove(fromActivityId);
                }
            } else if (activityMapping instanceof ActivityMigrationMapping.OneToManyMapping) {
                fromActivityId = ((ActivityMigrationMapping.OneToManyMapping) activityMapping).getFromActivityId();
                List<String> toActivityIds = activityMapping.getToActivityIds();
                if (executionActivityIdsToMapExplicitly.contains(fromActivityId)) {
                    if (activityMapping.isToCallActivity()) {
                        throw new UnsupportedOperationException("Mapping one activity to multiple activities in a subProcess is not implemented yet!!!");
                    } else if (activityMapping.isToParentProcess()) {
                        throw new UnsupportedOperationException("Mapping one activity to multiple activities in the parent process is not implemented yet!!!");
                    } else {
                        changeActivityStateBuilder.moveSingleActivityIdToActivityIds(fromActivityId, toActivityIds);
                    }
                    executionActivityIdsToMapExplicitly.remove(fromActivityId);
                }
            } else if (activityMapping instanceof ActivityMigrationMapping.ManyToOneMapping) {
                List<String> fromActivityIds = activityMapping.getFromActivityIds();
                toActivityId = ((ActivityMigrationMapping.ManyToOneMapping) activityMapping).getToActivityId();
                List<String> executionIds = new ArrayList<>();
                for (String activityId : fromActivityIds) {
                    if (executionActivityIdsToMapExplicitly.contains(activityId)) {
                        List<ExecutionEntity> executionEntities = filteredExecutionsByActivityId.get(activityId);
                        executionIds.addAll(executionEntities.stream().map(ExecutionEntity::getId).collect(Collectors.toList()));
                        executionActivityIdsToMapExplicitly.remove(activityId);
                    }
                }
                if (activityMapping.isToCallActivity()) {
                    throw new UnsupportedOperationException("Mapping multiple activities to single activity in a subProcess is not implemented yet!!!");
                } else if (activityMapping.isToParentProcess()) {
                    throw new UnsupportedOperationException("Mapping multiple activities to single activity in the parent process is not implemented yet!!!");
                } else {
                    changeActivityStateBuilder.moveExecutionsToSingleActivityId(executionIds, toActivityId, ((ActivityMigrationMapping.ManyToOneMapping) activityMapping).getWithNewAssignee());
                }

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

    protected void changeProcessDefinitionReferenceOfHistory(ProcessInstance processInstance, ProcessDefinition processDefinition, CommandContext commandContext) {
        HistoryLevel currentHistoryLevel = CommandContextUtil.getProcessEngineConfiguration(commandContext).getHistoryLevel();
        if (currentHistoryLevel.isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoryManager historyManager = CommandContextUtil.getHistoryManager(commandContext);
            historyManager.updateProcessDefinitionIdInHistory((ProcessDefinitionEntity) processDefinition, (ExecutionEntity) processInstance);
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

    protected ProcessDefinition resolveProcessDefinition(String processDefinitionKey, Integer processDefinitionVersion, String processDefinitionTenantId, CommandContext commandContext) {
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
        if (processDefinitionVersion != null) {
            return processDefinitionEntityManager.findProcessDefinitionByKeyAndVersionAndTenantId(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId);
        } else {
            if (processDefinitionTenantId != null) {
                return processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, processDefinitionTenantId);
            } else {
                return processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);
            }
        }
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
    protected boolean isSubProcessContainerOfAnyFlowElement(String subProcessId, Collection<MoveExecutionEntityContainer.FlowElementMoveEntry> moveToFlowElements) {
        //recreates all subProcesses
        return false;
    }
}

