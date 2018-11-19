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
import java.util.function.Predicate;
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
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
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
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationManagerImpl extends AbstractDynamicStateManager implements ProcessInstanceMigrationManager {

    Predicate<ExecutionEntity> isSubProcessExecution = executionEntity -> executionEntity.getCurrentFlowElement() instanceof SubProcess;
    Predicate<ExecutionEntity> isBoundaryEventExecution = executionEntity -> executionEntity.getCurrentFlowElement() instanceof BoundaryEvent;
    Predicate<ExecutionEntity> isCallActivityExecution = executionEntity -> executionEntity.getCurrentFlowElement() instanceof CallActivity;
    Predicate<ExecutionEntity> isActiveExecution = executionEntity -> executionEntity.isActive();
    Predicate<ExecutionEntity> executionHasCurrentActivityId = executionEntity -> executionEntity.getCurrentActivityId() != null;

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

        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager();
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceId);
        BpmnModel currentModel = ProcessDefinitionUtil.getBpmnModel(processInstanceExecution.getProcessDefinitionId());

        HashMap<String, ActivityMigrationMapping> mainProcessActivityMappingByFromActivityId = new HashMap<>();
        HashMap<String, HashMap<String, ActivityMigrationMapping>> subProcessActivityMappingsByCallActivityIdAndFromActivityId = new HashMap<>();

        for (ActivityMigrationMapping activityMigrationMapping : activityMappings) {
            HashMap<String, ActivityMigrationMapping> mapToFill;
            if (activityMigrationMapping.isToParentProcess()) {
                mapToFill = subProcessActivityMappingsByCallActivityIdAndFromActivityId.computeIfAbsent(activityMigrationMapping.getFromCallActivityId(), k -> new HashMap<>());
            } else {
                mapToFill = mainProcessActivityMappingByFromActivityId;
            }
            for (String fromActivityId : activityMigrationMapping.getFromActivityIds()) {
                mapToFill.put(fromActivityId, activityMigrationMapping);
            }
        }

        List<ExecutionEntity> activeMainProcessExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);

        //For each "running" active activity of the processInstance, check that there's a mapping defined or if it can be found in the new definition (auto-mapped by activity id)
        List<ExecutionEntity> mappableMainProcessExecutions = activeMainProcessExecutions.stream()
            .filter(executionHasCurrentActivityId)
            .filter(isActiveExecution)
            .filter(isSubProcessExecution.negate())
            .filter(isBoundaryEventExecution.negate())
            .collect(Collectors.toList());

        for (ExecutionEntity execution : mappableMainProcessExecutions) {
            String executionActivityId = execution.getCurrentActivityId();
            FlowElement executionFlowElement = execution.getCurrentFlowElement();

            //Not mapped executions
            if (!mainProcessActivityMappingByFromActivityId.containsKey(executionActivityId)) {

                if (executionFlowElement instanceof CallActivity) {
                    //CallActivity... when not mapped explicitly and none of its children activities are mapped (toParentProcess), then the call activity should exist in the new model with the same callElement
                    //If the call activity is only partially mapped (not all its executing activity children are mapped toParentProcess), then call activity should still exists in the new model with the same callElement
                    if (subProcessActivityMappingsByCallActivityIdAndFromActivityId.containsKey(executionActivityId)) {
                        //Check if all the call activity executing children are mapped, if there's any unMapped execution, the call activity must exists in the new model with the same callElement
                        List<ExecutionEntity> subProcessChildExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(execution.getSubProcessInstance().getId());
                        Set<String> childSubProcessExecutionActivityIds = subProcessChildExecutions.stream().map(Execution::getActivityId).collect(Collectors.toSet());
                        Set<String> mappedSubProcessActivityIds = subProcessActivityMappingsByCallActivityIdAndFromActivityId.get(executionActivityId).keySet();
                        childSubProcessExecutionActivityIds.removeAll(mappedSubProcessActivityIds);
                        boolean childrenFullyMapped = childSubProcessExecutionActivityIds.isEmpty();

                        if (!childrenFullyMapped) {
                            FlowElement newModelFlowElement = newModel.getFlowElement(executionActivityId);
                            if (newModelFlowElement == null) {
                                validationResult.addValidationMessage(String.format("Incomplete migration mapping for call activity. The call activity '%s' does not exist in the new model. "
                                    + "Running subProcess activities '%s' should also be mapped for migration (or the call activity itself)", executionActivityId, childSubProcessExecutionActivityIds));
                            } else if (newModelFlowElement instanceof CallActivity) {
                                if (!referToSameCalledElement((CallActivity) executionFlowElement, (CallActivity) newModelFlowElement)) {
                                    validationResult.addValidationMessage(String.format("Incomplete migration mapping for call activity. The call activity '%s' called element is different in the new model. "
                                        + "Running subProcess activities '%s' should also be mapped for migration (or the call activity itself)", executionActivityId, childSubProcessExecutionActivityIds));
                                }
                                if (((CallActivity) executionFlowElement).hasMultiInstanceLoopCharacteristics() ^ ((CallActivity) newModelFlowElement).hasMultiInstanceLoopCharacteristics()) {
                                    validationResult.addValidationMessage(String.format("Incomplete migration mapping for call activity. The Call activity '%s' loop characteristics is different in new model. "
                                        + "Running subProcess activities '%s' should also be mapped for migration (or the call activity itself)", executionActivityId, childSubProcessExecutionActivityIds));
                                }
                            } else {
                                validationResult.addValidationMessage(String.format("Incomplete migration mapping for call activity. Activity '%s' is not a Call Activity in the new model."
                                    + "Running subProcess activities '%s' should also be mapped for migration (or the call activity itself)", executionActivityId, childSubProcessExecutionActivityIds));
                            }
                        }
                    } else {
                        FlowElement newModelFlowElement = newModel.getFlowElement(executionActivityId);
                        if (newModelFlowElement == null) {
                            validationResult.addValidationMessage("Call activity '" + executionActivityId + "' does not exist in the new model. It must be mapped explicitly for migration (or all its child activities)");
                        } else if (newModelFlowElement instanceof CallActivity) {
                            if (!referToSameCalledElement((CallActivity) executionFlowElement, (CallActivity) newModelFlowElement)) {
                                validationResult.addValidationMessage("Call activity '" + executionActivityId + "' has a different called element in the new model. It must be mapped explicitly for migration (or all its child activities)");
                            }
                            if (((CallActivity) executionFlowElement).hasMultiInstanceLoopCharacteristics() ^ ((CallActivity) newModelFlowElement).hasMultiInstanceLoopCharacteristics()) {
                                validationResult.addValidationMessage("Call activity '" + executionActivityId + "' has a different loop characteristics is different in new model. It must be mapped explicitly for migration (or all its child activities)");
                            }
                        } else {
                            validationResult.addValidationMessage("Call activity '" + executionActivityId + "' is not a Call Activity in the new model. It must be mapped explicitly for migration (or all its child activities)");
                        }
                    }
                    continue;
                }

                //auto-mapping -> fail if the unMapped activityId not found in the new Model ... unless its a child of a "mapped" multiInstance activity
                if (!isActivityIdInProcessDefinitionModel(executionActivityId, newModel)) {
                    //Check if the execution is inside a MultiInstance parent and if so, check that its mapped
                    FlowElement currentModelFlowElement = currentModel.getFlowElement(executionActivityId);
                    Optional<String> flowElementMultiInstanceParentId = getFlowElementMultiInstanceParentId(currentModelFlowElement);
                    if (!flowElementMultiInstanceParentId.isPresent() || !mainProcessActivityMappingByFromActivityId.containsKey(flowElementMultiInstanceParentId.get())) {
                        validationResult.addValidationMessage("Process instance (id:'" + processInstanceId + "') has a running Activity (id:'" + executionActivityId + "') that is not mapped for migration (Or its Multi-Instance parent)");
                        continue;
                    }
                }
            }

            //Check in explicit mappings
            if (mainProcessActivityMappingByFromActivityId.containsKey(executionActivityId)) {
                ActivityMigrationMapping mapping = mainProcessActivityMappingByFromActivityId.get(executionActivityId);
                BpmnModel mappingModel = newModel;
                if (mapping.isToCallActivity()) {
                    FlowElement callActivityFlowElement = newModel.getFlowElement(mapping.getToCallActivityId());
                    if (callActivityFlowElement instanceof CallActivity) {
                        CallActivity callActivity = (CallActivity) callActivityFlowElement;
                        String procDefKey = callActivity.getCalledElement();
                        if (isExpression(procDefKey)) {
                            Expression expression = expressionManager.createExpression(procDefKey);
                            try {
                                procDefKey = expression.getValue(processInstanceExecution).toString();
                            } catch (FlowableException e) {
                                procDefKey = document.getProcessInstanceVariables().getOrDefault(procDefKey.substring(2, procDefKey.length() - 1), procDefKey).toString();
                            }
                        }
                        try {
                            ProcessDefinition mappingProcDef = resolveProcessDefinition(procDefKey, mapping.getCallActivityProcessDefinitionVersion(), document.getMigrateToProcessDefinitionTenantId(), commandContext);
                            mappingModel = ProcessDefinitionUtil.getBpmnModel(mappingProcDef.getId());
                        } catch (FlowableException e) {
                            validationResult.addValidationMessage(e.getMessage() + " for call activity element with id '" + mapping.getToCallActivityId() + "' in the process definition with id '" + mappingModel.getMainProcess().getId() + "'");
                            continue;
                        }
                    } else {
                        validationResult.addValidationMessage("There's no call activity element with id '" + mapping.getToCallActivityId() + "' in the process definition with id '" + mappingModel.getMainProcess().getId() + "'");
                        continue;
                    }
                }

                if (mapping.isToParentProcess()) {
                    FlowElement callActivityFlowElement = newModel.getFlowElement(mapping.getToCallActivityId());
                    throw new UnsupportedOperationException("Not implemented yet!!!");
                }

                //Check the target flow element
                List<String> toActivityIds = mapping.getToActivityIds();
                for (String targetActivityId : toActivityIds) {
                    if (!isActivityIdInProcessDefinitionModel(targetActivityId, mappingModel)) {
                        validationResult.addValidationMessage("Invalid mapping for '" + execution.getCurrentActivityId() + "' to '" + targetActivityId + "', cannot be found in the process definition with id '" + mappingModel.getMainProcess().getId() + "'");
                        continue;
                    }
                    //We cannot move an activity inside a MultiInstance container
                    FlowElement targetFlowElement = mappingModel.getFlowElement(targetActivityId);
                    Optional<String> targetFlowElementMultiInstanceParentId = getFlowElementMultiInstanceParentId(targetFlowElement);
                    if (targetFlowElementMultiInstanceParentId.isPresent()) {
                        validationResult.addValidationMessage("Invalid mapping for '" + execution.getCurrentActivityId() + "' to '" + targetActivityId + "', cannot migrate arbitrarily inside a Multi Instance container '" + targetFlowElementMultiInstanceParentId.get() + "' inside process definition with id '" + mappingModel.getMainProcess().getId() + "'");
                        continue;
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

        List<ChangeActivityStateBuilderImpl> changeActivityStateBuilders = prepareChangeStateBuilders(processInstanceId, procDefToMigrateTo, document, commandContext);

        LOGGER.debug("Updating Process definition reference of root execution with id:'" + processExecution.getId() + "' to '" + procDefToMigrateTo.getId() + "'");
        processExecution.setProcessDefinitionId(procDefToMigrateTo.getId());

        LOGGER.debug("Resolve activity executions to migrate");
        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = new ArrayList<>();
        for (ChangeActivityStateBuilderImpl builder : changeActivityStateBuilders) {
            moveExecutionEntityContainerList.addAll(resolveMoveExecutionEntityContainers(builder, Optional.of(procDefToMigrateTo.getId()), document.getProcessInstanceVariables(), commandContext));
        }

        ProcessInstanceChangeState processInstanceChangeState = new ProcessInstanceChangeState()
            .setProcessInstanceId(processInstanceId)
            .setProcessDefinitionToMigrateTo(procDefToMigrateTo)
            .setMoveExecutionEntityContainers(moveExecutionEntityContainerList)
            .setProcessInstanceVariables(document.getProcessInstanceVariables())
            .setLocalVariables(document.getActivitiesLocalVariables());

        doMoveExecutionState(processInstanceChangeState, commandContext);

        LOGGER.debug("Updating Process definition of call unchanged call activity");
        List<ExecutionEntity> callActivities = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId).stream()
            .filter(executionEntity -> executionEntity.getCurrentFlowElement() instanceof CallActivity)
            .collect(Collectors.toList());
        callActivities.forEach(executionEntity -> executionEntity.setProcessDefinitionId(procDefToMigrateTo.getId()));

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

    protected List<ChangeActivityStateBuilderImpl> prepareChangeStateBuilders(String processInstanceId, ProcessDefinition procDefToMigrateTo, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        LOGGER.debug("Start migration of process instance with Id:'" + processInstanceId + "' to " + printProcessDefinitionIdentifierMessage(document));

        //Check processExecution and processDefinition tenant
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processExecution = executionEntityManager.findById(processInstanceId);

        String procDefTenantId = procDefToMigrateTo.getTenantId();
        if (!isSameTenant(processExecution.getTenantId(), procDefTenantId)) {
            throw new FlowableException("Tenant mismatch between Process Instance ('" + processExecution.getTenantId() + "') and Process Definition ('" + procDefTenantId + "') to migrate to");
        }

        List<ChangeActivityStateBuilderImpl> changeActivityStateBuilders = new ArrayList<>();
        ChangeActivityStateBuilderImpl mainProcessChangeActivityStateBuilder = new ChangeActivityStateBuilderImpl();
        mainProcessChangeActivityStateBuilder.processInstanceId(processInstanceId);
        changeActivityStateBuilders.add(mainProcessChangeActivityStateBuilder);

        //Current executions to migrate...
        Map<String, List<ExecutionEntity>> filteredExecutionsByActivityId = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId)
            .stream()
            .filter(executionEntity -> executionEntity.getCurrentActivityId() != null)
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof SubProcess))
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof BoundaryEvent))
            .collect(Collectors.groupingBy(ExecutionEntity::getCurrentActivityId));

        LOGGER.debug("Preparing ActivityChangeState builder for '" + filteredExecutionsByActivityId.size() + "' distinct activities");

        HashMap<String, ActivityMigrationMapping> mainProcessActivityMappingByFromActivityId = new HashMap<>();
        HashMap<String, HashMap<String, ActivityMigrationMapping>> subProcessActivityMappingsByCallActivityIdAndFromActivityId = new HashMap<>();
        for (ActivityMigrationMapping activityMigrationMapping : document.getActivityMigrationMappings()) {
            HashMap<String, ActivityMigrationMapping> mapToFill;
            if (activityMigrationMapping.isToParentProcess()) {
                mapToFill = subProcessActivityMappingsByCallActivityIdAndFromActivityId.computeIfAbsent(activityMigrationMapping.getFromCallActivityId(), k -> new HashMap<>());
            } else {
                mapToFill = mainProcessActivityMappingByFromActivityId;
            }
            for (String fromActivityId : activityMigrationMapping.getFromActivityIds()) {
                mapToFill.put(fromActivityId, activityMigrationMapping);
            }
        }

        Set<String> mappedFromActivities = mainProcessActivityMappingByFromActivityId.keySet();

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
        for (String executionActivityId : executionActivityIdsToAutoMap) {
            FlowElement currentModelFlowElement = currentModel.getFlowElement(executionActivityId);

            if (currentModelFlowElement instanceof CallActivity) {
                //Check that all or none of the call activity child activities executions are explicitly mapped
                boolean runningChildrenNotFullyMapped = false;
                if (subProcessActivityMappingsByCallActivityIdAndFromActivityId.containsKey(executionActivityId)) {
                    Set<String> mappedSubProcessActivityIds = subProcessActivityMappingsByCallActivityIdAndFromActivityId.get(executionActivityId).keySet();
                    List<ExecutionEntity> callActivityExecutions = filteredExecutionsByActivityId.get(executionActivityId).stream().filter(ExecutionEntity::isActive).collect(Collectors.toList());
                    for (ExecutionEntity callActivityExecution : callActivityExecutions) { //parallel MultiInstance call activities
                        List<ExecutionEntity> subProcessChildExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(callActivityExecution.getSubProcessInstance().getId());
                        Set<String> childSubProcessExecutionActivityIds = subProcessChildExecutions.stream().map(Execution::getActivityId).collect(Collectors.toSet());
                        childSubProcessExecutionActivityIds.removeAll(mappedSubProcessActivityIds);
                        if (!childSubProcessExecutionActivityIds.isEmpty()) {
                            runningChildrenNotFullyMapped = true;
                            break;
                        }
                    }
                }

                if (!subProcessActivityMappingsByCallActivityIdAndFromActivityId.containsKey(executionActivityId) || runningChildrenNotFullyMapped) {
                    //If there are running child activities not mapped, the call activity must be equally valid in the new model, the activityId in the new model must refer also to a callActivity with matching callElement
                    FlowElement newModelFlowElement = newModel.getFlowElement(executionActivityId);
                    if (newModelFlowElement == null) {
                        throw new FlowableException("Call activity '" + executionActivityId + "' does not exist in the new model. It must be mapped explicitly for migration (or all its child activities)");
                    }
                    if (newModelFlowElement instanceof CallActivity) {
                        if (!referToSameCalledElement((CallActivity) currentModelFlowElement, (CallActivity) newModelFlowElement)) {
                            throw new FlowableException("Call activity '" + executionActivityId + "' has a different called element in the new model. It must be mapped explicitly for migration (or all its child activities)");
                        }
                        if (((CallActivity) currentModelFlowElement).hasMultiInstanceLoopCharacteristics() ^ ((CallActivity) newModelFlowElement).hasMultiInstanceLoopCharacteristics()) {
                            throw new FlowableException("Call activity '" + executionActivityId + "' loop characteristics differs in new model. It must be mapped explicitly for migration (or all its child activities)");
                        }
                    } else {
                        throw new FlowableException("Call activity '" + executionActivityId + "' is not a Call Activity in the new model. It must be mapped explicitly for migration (or all its child activities)");
                    }
                }
                continue;
            }

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
                LOGGER.debug("Checking execution(s) - activityId:'" + executionActivityId);
                if (isActivityIdInProcessDefinitionModel(executionActivityId, newModel)) {
                    //Cannot auto-map inside a MultiInstance container
                    FlowElement newModelFlowElement = newModel.getFlowElement(executionActivityId);
                    Optional<String> newFlowElementMIParentId = getFlowElementMultiInstanceParentId(newModelFlowElement);

                    if (newFlowElementMIParentId.isPresent()) {
                        throw new FlowableException("Cannot autoMap activity migration for '" + executionActivityId + "'. Cannot migrate arbitrarily inside a Multi Instance container '" + newFlowElementMIParentId.get());
                    }

                    LOGGER.debug("Auto mapping activity '" + executionActivityId + "'");
                    List<ExecutionEntity> executionEntities = filteredExecutionsByActivityId.get(executionActivityId);
                    if (executionEntities.size() > 1) {
                        List<String> executionIds = executionEntities.stream().map(ExecutionEntity::getId).collect(Collectors.toList());
                        mainProcessChangeActivityStateBuilder.moveExecutionsToSingleActivityId(executionIds, executionActivityId);
                    } else {
                        mainProcessChangeActivityStateBuilder.moveExecutionToActivityId(executionEntities.get(0).getId(), executionActivityId);
                    }
                } else {
                    throw new FlowableException("Migration Activity mapping missing for activity definition Id:'" + executionActivityId + "' or its MI Parent");
                }
            }
        }

        //Explicit Mapping - Iterates over the provided mappings instead, to keep the explicit migration order
        List<ActivityMigrationMapping> activityMigrationMappings = document.getActivityMigrationMappings();

        LOGGER.debug("Process explicit mapping for '" + executionActivityIdsToMapExplicitly.size() + "' activity executions");
        for (
            ActivityMigrationMapping activityMapping : activityMigrationMappings) {

            if (activityMapping instanceof ActivityMigrationMapping.OneToOneMapping) {
                String fromActivityId = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getFromActivityId();
                String toActivityId = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getToActivityId();
                String newAssignee = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getWithNewAssignee();
                String fromCallActivityId = activityMapping.getFromCallActivityId();

                if (activityMapping.isToParentProcess() && !executionActivityIdsToMapExplicitly.contains(fromCallActivityId)) {
                    List<ExecutionEntity> callActivityExecutions = filteredExecutionsByActivityId.get(fromCallActivityId).stream().filter(ExecutionEntity::isActive).collect(Collectors.toList());
                    for (ExecutionEntity callActivityExecution : callActivityExecutions) {
                        ExecutionEntity subProcessInstanceExecution = executionEntityManager.findSubProcessInstanceBySuperExecutionId(callActivityExecution.getId());
                        ChangeActivityStateBuilderImpl subProcessChangeActivityStateBuilder = new ChangeActivityStateBuilderImpl();
                        subProcessChangeActivityStateBuilder.processInstanceId(subProcessInstanceExecution.getId());
                        subProcessChangeActivityStateBuilder.moveActivityIdToParentActivityId(fromActivityId, toActivityId, newAssignee);
                        changeActivityStateBuilders.add(subProcessChangeActivityStateBuilder);
                    }
                } else if (executionActivityIdsToMapExplicitly.contains(fromActivityId)) {
                    if (activityMapping.isToCallActivity()) {
                        mainProcessChangeActivityStateBuilder.moveActivityIdToSubProcessInstanceActivityId(fromActivityId, toActivityId, activityMapping.getToCallActivityId(), activityMapping.getCallActivityProcessDefinitionVersion(), newAssignee);
                    } else {
                        mainProcessChangeActivityStateBuilder.moveActivityIdTo(fromActivityId, toActivityId, newAssignee);
                    }
                    executionActivityIdsToMapExplicitly.remove(fromActivityId);
                }
            } else if (activityMapping instanceof ActivityMigrationMapping.OneToManyMapping) {
                String fromActivityId = ((ActivityMigrationMapping.OneToManyMapping) activityMapping).getFromActivityId();
                List<String> toActivityIds = activityMapping.getToActivityIds();
                String fromCallActivityId = activityMapping.getFromCallActivityId();
                if (activityMapping.isToParentProcess() && !executionActivityIdsToMapExplicitly.contains(fromCallActivityId)) {
                    List<ExecutionEntity> callActivityExecutions = filteredExecutionsByActivityId.get(fromCallActivityId).stream().filter(ExecutionEntity::isActive).collect(Collectors.toList());
                    for (ExecutionEntity callActivityExecution : callActivityExecutions) {
                        ExecutionEntity subProcessInstanceExecution = executionEntityManager.findSubProcessInstanceBySuperExecutionId(callActivityExecution.getId());
                        ChangeActivityStateBuilderImpl subProcessChangeActivityStateBuilder = new ChangeActivityStateBuilderImpl();
                        subProcessChangeActivityStateBuilder.processInstanceId(subProcessInstanceExecution.getId());
                        subProcessChangeActivityStateBuilder.moveSingleActivityIdToParentActivityIds(fromActivityId, toActivityIds);
                        changeActivityStateBuilders.add(subProcessChangeActivityStateBuilder);
                    }
                } else if (executionActivityIdsToMapExplicitly.contains(fromActivityId)) {
                    if (activityMapping.isToCallActivity()) {
                        mainProcessChangeActivityStateBuilder.moveSingleActivityIdToSubProcessInstanceActivityIds(fromActivityId, toActivityIds, activityMapping.getToCallActivityId(), activityMapping.getCallActivityProcessDefinitionVersion());
                    } else {
                        mainProcessChangeActivityStateBuilder.moveSingleActivityIdToActivityIds(fromActivityId, toActivityIds);
                    }
                    executionActivityIdsToMapExplicitly.remove(fromActivityId);
                }
            } else if (activityMapping instanceof ActivityMigrationMapping.ManyToOneMapping) {
                List<String> fromActivityIds = activityMapping.getFromActivityIds();
                String toActivityId = ((ActivityMigrationMapping.ManyToOneMapping) activityMapping).getToActivityId();
                String fromCallActivityId = activityMapping.getFromCallActivityId();
                String newAssignee = ((ActivityMigrationMapping.ManyToOneMapping) activityMapping).getWithNewAssignee();
                if (activityMapping.isToParentProcess() && !executionActivityIdsToMapExplicitly.contains(fromCallActivityId)) {
                    List<ExecutionEntity> callActivityExecutions = filteredExecutionsByActivityId.get(fromCallActivityId).stream().filter(ExecutionEntity::isActive).collect(Collectors.toList());
                    for (ExecutionEntity callActivityExecution : callActivityExecutions) {
                        ExecutionEntity subProcessInstanceExecution = executionEntityManager.findSubProcessInstanceBySuperExecutionId(callActivityExecution.getId());
                        ChangeActivityStateBuilderImpl subProcessChangeActivityStateBuilder = new ChangeActivityStateBuilderImpl();
                        subProcessChangeActivityStateBuilder.processInstanceId(subProcessInstanceExecution.getId());
                        subProcessChangeActivityStateBuilder.moveActivityIdsToParentActivityId(fromActivityIds, toActivityId, newAssignee);
                        changeActivityStateBuilders.add(subProcessChangeActivityStateBuilder);
                    }
                } else {
                    List<String> executionIds = new ArrayList<>();
                    for (String activityId : fromActivityIds) {
                        if (executionActivityIdsToMapExplicitly.contains(activityId)) {
                            List<ExecutionEntity> executionEntities = filteredExecutionsByActivityId.get(activityId);
                            executionIds.addAll(executionEntities.stream().map(ExecutionEntity::getId).collect(Collectors.toList()));
                            executionActivityIdsToMapExplicitly.remove(activityId);
                        }
                    }
                    if (activityMapping.isToCallActivity()) {
                        mainProcessChangeActivityStateBuilder.moveActivityIdsToSubProcessInstanceActivityId(fromActivityIds, toActivityId, activityMapping.getToCallActivityId(), activityMapping.getCallActivityProcessDefinitionVersion(), newAssignee);
                    } else {
                        mainProcessChangeActivityStateBuilder.moveExecutionsToSingleActivityId(executionIds, toActivityId, newAssignee);
                    }
                }
            } else {
                throw new FlowableException("Unknown Activity Mapping or not implemented yet!!!");
            }
        }

        if (!executionActivityIdsToMapExplicitly.isEmpty()) {
            throw new FlowableException("Migration Activity mapping missing for activity definition Ids:'" + Arrays.toString(executionActivityIdsToMapExplicitly.toArray()) + "'");
        }

        return changeActivityStateBuilders;
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

    protected boolean referToSameCalledElement(CallActivity callActivity1, CallActivity callActivity2) {
        String calledElement1 = callActivity1.getCalledElement();
        String calledElement2 = callActivity2.getCalledElement();

        return calledElement1.equals(calledElement2) && !isExpression(calledElement1);
    }

}

