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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.dynamic.AbstractDynamicStateManager;
import org.flowable.engine.impl.dynamic.MoveExecutionEntityContainer;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationManagerImpl extends AbstractDynamicStateManager implements ProcessInstanceMigrationManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceMigrationManager.class);

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstancesOfProcessDefinition(String procDefKey, String procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {

        ProcessInstanceMigrationValidationResult result = new ProcessInstanceMigrationValidationResult();
        //Must first resolve the Id of the processDefinition
        ProcessDefinition processDefinition = resolveProcessDefinition(procDefKey, procDefVer, procDefTenantId, commandContext);
        if (processDefinition != null) {
            ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = validateMigrateProcessInstancesOfProcessDefinition(processDefinition.getId(), document, commandContext);
            result.addValidationResult(processInstanceMigrationValidationResult);
        } else {
            result.addValidationMessage("Cannot find the process definition to migrate from");
        }
        return result;
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {

        ProcessInstanceMigrationValidationResult result = new ProcessInstanceMigrationValidationResult();
        //Check that the processDefinition exists and get its associated BpmnModel
        ProcessDefinition processDefinition = resolveProcessDefinition(document, commandContext);
        if (processDefinition == null) {
            result.addValidationMessage("Cannot find the process definition to migrate to " + printProcessDefinitionIdentifierMessage(document));
        } else {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            if (bpmnModel == null) {
                result.addValidationMessage("Cannot find the Bpmn model of the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
            } else {
                ProcessInstanceQueryImpl processInstanceQueryByProcessDefinitionId = new ProcessInstanceQueryImpl().processDefinitionId(processDefinitionId);
                ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
                List<ProcessInstance> processInstances = executionEntityManager.findProcessInstanceByQueryCriteria(processInstanceQueryByProcessDefinitionId);

                for (ProcessInstance processInstance : processInstances) {
                    ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = doValidateProcessInstanceMigration(processInstance.getId(), processDefinition.getTenantId(), bpmnModel,
                        document.getActivityMigrationMappings(), commandContext);
                    result.addValidationResult(processInstanceMigrationValidationResult);
                }
            }
        }
        return result;
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {

        ProcessInstanceMigrationValidationResult result = new ProcessInstanceMigrationValidationResult();
        //Check that the processDefinition exists and get its associated BpmnModel
        ProcessDefinition processDefinition = resolveProcessDefinition(document, commandContext);
        if (processDefinition == null) {
            result.addValidationMessage(("Cannot find the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document)));
        } else {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            if (bpmnModel == null) {
                result.addValidationMessage("Cannot find the Bpmn model of the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
            } else {
                ProcessInstanceMigrationValidationResult validationResult = doValidateProcessInstanceMigration(processInstanceId, processDefinition.getTenantId(), bpmnModel, document.getActivityMigrationMappings(), commandContext);
                result.addValidationResult(validationResult);
            }
        }
        return result;
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

        doMigrateProcessInstance(processInstanceId, processDefinition, bpmnModel, document, commandContext);
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String procDefKey, String procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
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
            doMigrateProcessInstance(processInstance.getId(), processDefinition, bpmnModel, document, commandContext);
        }

    }

    protected void doMigrateProcessInstance(String processInstanceId, ProcessDefinition procDefToMigrateTo, BpmnModel bpmnModel, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
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

        List<ExecutionEntity> executions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);
        LOGGER.debug("Preparing ActivityChangeState builder for '" + executions.size() + "' activity executions");
        for (ExecutionEntity execution : executions) {
            LOGGER.debug("Checking execution - activityId:'" + execution.getCurrentActivityId() + "' id:'" + execution.getId());
            if (execution.getCurrentActivityId() != null) {
                //SubProcess dont need to be Mapped
                if (execution.getCurrentFlowElement() instanceof SubProcess) {
                    LOGGER.debug("activityId:'" + execution.getCurrentActivityId() + "' is a SubProcess - No mapping required");
                    continue;
                }
                //If there's no specific mapping, we check if the new process definition contains it already
                if (document.getActivityMigrationMappings().containsKey(execution.getCurrentActivityId())) {
                    String toActivityId = document.getActivityMigrationMappings().get(execution.getCurrentActivityId());
                    LOGGER.debug("Mapping for activity '" + execution.getCurrentActivityId() + "' -> '" + toActivityId);
                    changeActivityStateBuilder.moveExecutionToActivityId(execution.getId(), toActivityId);
                } else if (isActivityIdInProcessDefinitionModel(execution.getCurrentActivityId(), bpmnModel)) {
                    LOGGER.debug("Auto mapping activity '" + execution.getCurrentActivityId());
                    changeActivityStateBuilder.moveExecutionToActivityId(execution.getId(), execution.getCurrentActivityId());
                } else {
                    throw new FlowableException("Migration Activity mapping missing for activity definition Id:'" + execution.getProcessDefinitionKey() + "'");
                }
            }
        }
        LOGGER.debug("Updating Process definition reference of root execution with id:'" + processExecution.getId() + "' to '" + procDefToMigrateTo.getId() + "'");
        processExecution.setProcessDefinitionId(procDefToMigrateTo.getId());

        LOGGER.debug("Migrating activity executions");
        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = resolveMoveExecutionEntityContainers(changeActivityStateBuilder, commandContext);
        doMoveExecutionState(moveExecutionEntityContainerList, changeActivityStateBuilder.getProcessVariables(), changeActivityStateBuilder.getLocalVariables(), Optional.ofNullable(procDefToMigrateTo.getId()), commandContext);

        LOGGER.debug("Updating Process definition reference in history");
        changeProcessDefinitionReferenceOfHistory(commandContext, processExecution, procDefToMigrateTo);

        LOGGER.debug("Process migration ended for process instance with Id:'" + processInstanceId + "'");
    }

    protected static boolean isSameTenant(String tenantId1, String tenantId2) {

        if (tenantId1 != null && tenantId2 != null) {
            return tenantId1.equals(tenantId2);
        } else if (tenantId1 == null && tenantId2 == null) {
            return true;
        }
        return false;
    }

    protected static void changeProcessDefinitionReferenceOfHistory(CommandContext commandContext, ProcessInstance processInstance, ProcessDefinition processDefinition) {
        HistoryManager historyManager = CommandContextUtil.getHistoryManager(commandContext);
        historyManager.updateProcessDefinitionIdInHistory((ProcessDefinitionEntity) processDefinition, (ExecutionEntity) processInstance);
    }

    protected static ProcessInstanceMigrationValidationResult doValidateProcessInstanceMigration(String processInstanceId, String tenantId, BpmnModel bpmnModel, Map<String, String> activityMappings, CommandContext commandContext) {

        ProcessInstanceMigrationValidationResult result = new ProcessInstanceMigrationValidationResult();

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        //Check that the processInstance exists
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceId);
        if (processInstanceExecution == null) {
            return result.addValidationMessage("Cannot find process instance with id:'" + processInstanceId + "'");
        }

        //Check processExecution and processDefinition tenant
        if (!isSameTenant(processInstanceExecution.getTenantId(), tenantId)) {
            return result.addValidationMessage("Tenant mismatch between Process Instance ('" + processInstanceExecution.getTenantId() + "') and Process Definition ('" + tenantId + "') to migrate to");
        }

        ProcessInstanceMigrationValidationResult mappingValidationResult = doValidateActivityMappings(processInstanceId, activityMappings, bpmnModel, commandContext);
        return result.addValidationResult(mappingValidationResult);
    }

    protected static ProcessInstanceMigrationValidationResult doValidateActivityMappings(String processInstanceId, Map<String, String> activityMappings, BpmnModel bpmnModel, CommandContext commandContext) {

        ProcessInstanceMigrationValidationResult result = new ProcessInstanceMigrationValidationResult();

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        //For each "running" activity of the processInstance, check that there's a mapping defined or if it can be found in the new definition (auto-mapped by activity id)
        List<ExecutionEntity> executions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);
        for (ExecutionEntity execution : executions) {
            if (execution.getCurrentActivityId() != null) {
                //SubProcesses don't need to be mapped
                if (execution.getCurrentFlowElement() instanceof SubProcess) {
                    continue;
                }
                if (!activityMappings.containsKey(execution.getCurrentActivityId()) && !isActivityIdInProcessDefinitionModel(execution.getCurrentActivityId(), bpmnModel)) {
                    result.addValidationMessage("Process instance (id:'" + processInstanceId + "') has a running Activity (id:'" + execution.getCurrentActivityId() + "') that is not mapped for migration");
                }
            }
        }
        return result;
    }

    protected static ProcessDefinition resolveProcessDefinition(ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        if (document.getMigrateToProcessDefinitionId() != null) {
            ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
            return processDefinitionEntityManager.findById(document.getMigrateToProcessDefinitionId());
        } else {
            String nullableTenantId = document.getMigrateToProcessDefinitionTenantId();
            return resolveProcessDefinition(document.getMigrateToProcessDefinitionKey(), document.getMigrateToProcessDefinitionVersion(), document.getMigrateToProcessDefinitionTenantId(), commandContext);
        }
    }

    protected static ProcessDefinition resolveProcessDefinition(String processDefinitionKey, String processDefinitionVersion, String processDefinitionTenantId, CommandContext commandContext) {
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
        return processDefinitionEntityManager.findProcessDefinitionByKeyAndVersionAndTenantId(processDefinitionKey, Integer.valueOf(processDefinitionVersion), processDefinitionTenantId);
    }

    protected static boolean isActivityIdInProcessDefinitionModel(String activityId, BpmnModel bpmnModel) {
        return bpmnModel.getFlowElement(activityId) != null;
    }

    protected static String printProcessDefinitionIdentifierMessage(ProcessInstanceMigrationDocument document) {
        String id = document.getMigrateToProcessDefinitionId();
        String key = document.getMigrateToProcessDefinitionKey();
        String version = document.getMigrateToProcessDefinitionVersion();
        String tenantId = document.getMigrateToProcessDefinitionTenantId();

        return "process definition identified by [id:'" + id + "'] or [key:'" + key + "', version:'" + version + "', tenantId:'" + tenantId + "']";
    }

    @Override
    protected boolean isSubProcessUsedInNewFlowElements(String subProcessId, Collection<FlowElement> moveToFlowElements) {

        //TODO WIP ... recreates all subProcesses
        return false;
    }

    @Override
    protected List<ExecutionEntity> createEmbeddedSubProcessExecutions(Collection<FlowElement> moveToFlowElements, List<ExecutionEntity> currentExecutions, MoveExecutionEntityContainer moveExecutionContainer, Optional<String> migrateToProcessDefinitionId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        // TODO WIP ... in a migration al subProcesses would be created new
        // Resolve the sub process elements that need to be created for each move to flow element
        Map<String, List<SubProcess>> subProcessesToCreateByActivityToMove = new HashMap<>();
        for (FlowElement flowElement : moveToFlowElements) {
            List<SubProcess> subProcesses = new ArrayList<>();
            SubProcess subProcess = flowElement.getSubProcess();
            while (subProcess != null) {
                subProcesses.add(subProcess);
                subProcess = subProcess.getSubProcess();
            }
            if (!subProcesses.isEmpty()) {
                //TODO WIP ... Optimizable ?? use a set instead?
                if (subProcessesToCreateByActivityToMove.containsKey(flowElement.getId())) {
                    subProcessesToCreateByActivityToMove.get(flowElement.getId()).addAll(subProcesses);
                } else {
                    subProcessesToCreateByActivityToMove.put(flowElement.getId(), subProcesses);
                }
            }
        }

        // The default parent execution is retrieved from the match with the first source execution
        ExecutionEntity defaultContinueParentExecution = moveExecutionContainer.getContinueParentExecution(currentExecutions.get(0).getId());

        HashMap<String, ExecutionEntity> createdSubProcesses = new HashMap<>();
        for (FlowElement flowElement : moveToFlowElements) {
            List<SubProcess> subProcessesToCreate = subProcessesToCreateByActivityToMove.getOrDefault(flowElement.getId(), Collections.emptyList());
            for (SubProcess subProcess : subProcessesToCreate) {
                if (!createdSubProcesses.containsKey(subProcess.getId())) {
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

                    //TODO WIP ... WE KEEP A LIST OF SUBPROCESS START ELEMENTS OR THE ACTUA SUB-PROCESS AS PARENT IF IT IS A DIRECT MIGRATION
                    if (moveExecutionContainer.isDirectExecutionMigration() && subProcess.getId().equals(flowElement.getSubProcess().getId())) {
                        //Create start element
                        createdSubProcesses.put(subProcess.getId(), subProcessExecution);
                    } else {
                        ExecutionEntity startSubProcessExecution = executionEntityManager.createChildExecution(subProcessExecution);
                        startSubProcessExecution.setCurrentFlowElement(startElement);
                        createdSubProcesses.put(subProcess.getId(), startSubProcessExecution);
                    }
                }
            }
        }

        //TODO ... WIP Separate parentProcessCreation from actual execution
        List<ExecutionEntity> newChildExecutions = new ArrayList<>();
        for (FlowElement newFlowElement : moveToFlowElements) {
            ExecutionEntity newChildExecution = null;
            // Check if a sub process child execution was created for this move to flow element, otherwise use the default continue parent execution
            if (subProcessesToCreateByActivityToMove.containsKey(newFlowElement.getId())) {
                if (moveExecutionContainer.isDirectExecutionMigration()) {

                    ExecutionEntity subProcessExecution = createdSubProcesses.get(newFlowElement.getSubProcess().getId());
                    newChildExecution = migrateExecutionEntity(subProcessExecution, currentExecutions.get(0), newFlowElement, commandContext);
                } else {
                    newChildExecution = createdSubProcesses.get(newFlowElement.getSubProcess().getId());
                    //                    newChildExecution = moveExecutionContainer.getNewSubProcessChildExecution(subProcessesToCreateByActivityToMove.get(newFlowElement.getId()).get(0).getId());
                }
            } else {
                if (moveExecutionContainer.isDirectExecutionMigration()) {
                    newChildExecution = migrateExecutionEntity(defaultContinueParentExecution, currentExecutions.get(0), newFlowElement, commandContext);
                } else {
                    newChildExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
                }
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

    private void migrateEmbeddedSubProcessExecution(String subProcessActivityId, String processInstanceId, String processDefinitioId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        List<ExecutionEntity> subProcessExecutions = executionEntityManager.findExecutionsByQueryCriteria(new ExecutionQueryImpl().processInstanceId(processInstanceId).activityId(subProcessActivityId));
        subProcessExecutions.forEach(e -> e.setProcessDefinitionId(processDefinitioId));
    }

    protected ExecutionEntity migrateExecutionEntity(ExecutionEntity parentExecutionEntity, ExecutionEntity childExecution, FlowElement newFlowElement, CommandContext commandContext) {

        //        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        TaskService taskService = CommandContextUtil.getTaskService(commandContext);
        HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
        HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager = CommandContextUtil.getHistoricActivityInstanceEntityManager(commandContext);

        childExecution.setParent(parentExecutionEntity);
        // manage the bidirectional parent-child relation
        parentExecutionEntity.addChildExecution(childExecution);
        childExecution.setProcessDefinitionId(parentExecutionEntity.getProcessDefinitionId());
        childExecution.setProcessInstanceId(parentExecutionEntity.getProcessInstanceId() != null ? parentExecutionEntity.getProcessInstanceId() : parentExecutionEntity.getId());
        childExecution.setRootProcessInstanceId(parentExecutionEntity.getRootProcessInstanceId());

        // Inherits the 'count' feature from the parent
        if (parentExecutionEntity instanceof CountingExecutionEntity) {
            CountingExecutionEntity countingParentExecutionEntity = (CountingExecutionEntity) parentExecutionEntity;
            ((CountingExecutionEntity) childExecution).setCountEnabled(countingParentExecutionEntity.isCountEnabled());
        }

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

        //TODO ... check persistance state May not be needed
        //executionEntityManager.update(childExecution, false);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Child execution {} updated with parent {}", childExecution, parentExecutionEntity.getId());
        }
        return childExecution;
    }

}

