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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.SubProcess;
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
                    doValidateProcessInstanceMigration(processInstance.getId(), processDefinition.getTenantId(), bpmnModel,
                        document.getActivityMigrationMappings(), validationResult, commandContext);
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
                doValidateProcessInstanceMigration(processInstanceId, processDefinition.getTenantId(), bpmnModel, 
                                document.getActivityMigrationMappings(), validationResult, commandContext);
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

        doMigrateProcessInstance(processInstanceId, processDefinition, bpmnModel, document, commandContext);
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
            doMigrateProcessInstance(processInstance.getId(), processDefinition, bpmnModel, document, commandContext);
        }

    }

    protected void doMigrateProcessInstance(String processInstanceId, ProcessDefinition procDefToMigrateTo, BpmnModel bpmnModel, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        LOGGER.debug("Start migration of process instance with Id:'" + processInstanceId + "' to " + printProcessDefinitionIdentifierMessage(document));

        //Check processExecution and processDefinition tenant
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processExecution = executionEntityManager.findById(processInstanceId);

        ChangeActivityStateBuilderImpl changeActivityStateBuilder = prepareChangeStateBuilder(processInstanceId, procDefToMigrateTo, bpmnModel, document, commandContext);

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
        return currentFlowElement instanceof UserTask && newFlowElement instanceof UserTask ||
            currentFlowElement instanceof ReceiveTask && newFlowElement instanceof ReceiveTask;
    }

    protected ChangeActivityStateBuilderImpl prepareChangeStateBuilder(String processInstanceId, ProcessDefinition procDefToMigrateTo, BpmnModel bpmnModel, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
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

        //Resolve in the order of the mappings...
        Map<String, List<ExecutionEntity>> filteredExecutionsByActivityId = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId)
            .stream()
            .filter(executionEntity -> executionEntity.getCurrentActivityId() != null)
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof SubProcess))
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof BoundaryEvent))
            .collect(Collectors.groupingBy(ExecutionEntity::getCurrentActivityId));

        LOGGER.debug("Preparing ActivityChangeState builder for '" + filteredExecutionsByActivityId.size() + "' distinct activities");

        //Automatic mappings
        List<String> executionActivityIdsToAutoMap = filteredExecutionsByActivityId.keySet()
            .stream()
            .filter(activityId -> !document.getActivityMigrationMappings().containsKey(activityId))
            .collect(Collectors.toList());

        List<ExecutionEntity> executionsToAutoMap = executionActivityIdsToAutoMap.stream()
            .flatMap(activityId -> filteredExecutionsByActivityId.getOrDefault(activityId, Collections.emptyList()).stream())
            .collect(Collectors.toList());

        LOGGER.debug("Process AutoMapping for '" + executionsToAutoMap.size() + "' activity executions");
        for (ExecutionEntity execution : executionsToAutoMap) {
            LOGGER.debug("Checking execution - activityId:'" + execution.getCurrentActivityId() + "' id:'" + execution.getId());
            if (isActivityIdInProcessDefinitionModel(execution.getCurrentActivityId(), bpmnModel)) {
                LOGGER.debug("Auto mapping activity '" + execution.getCurrentActivityId() + "'");
                changeActivityStateBuilder.moveExecutionToActivityId(execution.getId(), execution.getCurrentActivityId());
            } else {
                throw new FlowableException("Migration Activity mapping missing for activity definition Id:'" + execution.getActivityId() + "'");
            }
        }

        //Explicit Mappings of the executions
        List<String> executionActivityIdsToMapExplicitly = filteredExecutionsByActivityId.keySet()
            .stream()
            .filter(activityId -> document.getActivityMigrationMappings().containsKey(activityId))
            .collect(Collectors.toList());

        List<ExecutionEntity> executionsToMapExplicitly = executionActivityIdsToMapExplicitly.stream()
            .flatMap(activityId -> filteredExecutionsByActivityId.getOrDefault(activityId, Collections.emptyList()).stream())
            .collect(Collectors.toList());

        LOGGER.debug("Process explicit mapping for '" + executionsToMapExplicitly.size() + "' activity executions");
        for (ExecutionEntity execution : executionsToMapExplicitly) {
            LOGGER.debug("Checking execution - activityId:'" + execution.getCurrentActivityId() + "' id:'" + execution.getId());
            if (document.getActivityMigrationMappings().containsKey(execution.getCurrentActivityId())) {
                String toActivityId = document.getActivityMigrationMappings().get(execution.getCurrentActivityId());
                LOGGER.debug("Mapping found for activity '" + execution.getCurrentActivityId() + "' -> '" + toActivityId);
                changeActivityStateBuilder.moveExecutionToActivityId(execution.getId(), toActivityId);
            } else {
                throw new FlowableException("Migration Activity mapping missing for activity definition Id:'" + execution.getActivityId() + "'");
            }
        }

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

    protected void doValidateProcessInstanceMigration(String processInstanceId, String tenantId, BpmnModel bpmnModel, 
                    Map<String, String> activityMappings, ProcessInstanceMigrationValidationResult validationResult, CommandContext commandContext) {

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

        doValidateActivityMappings(processInstanceId, activityMappings, bpmnModel, validationResult, commandContext);
    }

    protected void doValidateActivityMappings(String processInstanceId, Map<String, String> activityMappings, 
                    BpmnModel bpmnModel, ProcessInstanceMigrationValidationResult validationResult, CommandContext commandContext) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        //For each "running" active activity of the processInstance, check that there's a mapping defined or if it can be found in the new definition (auto-mapped by activity id)
        List<ExecutionEntity> activeExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId).stream().filter(ExecutionEntity::isActive).collect(Collectors.toList());
        for (ExecutionEntity execution : activeExecutions) {
            if (execution.getCurrentActivityId() != null) {
                //SubProcesses don't need to be mapped
                if (execution.getCurrentFlowElement() instanceof SubProcess) {
                    continue;
                }
                if (execution.getCurrentFlowElement() instanceof BoundaryEvent) {
                    continue;
                }
                
                if (!activityMappings.containsKey(execution.getCurrentActivityId()) && !isActivityIdInProcessDefinitionModel(execution.getCurrentActivityId(), bpmnModel)) {
                    validationResult.addValidationMessage("Process instance (id:'" + processInstanceId + "') has a running Activity (id:'" + execution.getCurrentActivityId() + "') that is not mapped for migration");
                }
                
                if (activityMappings.containsKey(execution.getCurrentActivityId())) {
                    String targetActivityId = activityMappings.get(execution.getCurrentActivityId());
                    if (!isActivityIdInProcessDefinitionModel(targetActivityId, bpmnModel)) {
                        validationResult.addValidationMessage("The target mapping of " + execution.getCurrentActivityId() + " to " + targetActivityId + 
                                        " can not be found in the target process definition");
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
        return processDefinitionEntityManager.findProcessDefinitionByKeyAndVersionAndTenantId(processDefinitionKey, Integer.valueOf(processDefinitionVersion), processDefinitionTenantId);
    }

    protected boolean isActivityIdInProcessDefinitionModel(String activityId, BpmnModel bpmnModel) {
        return bpmnModel.getFlowElement(activityId) != null;
    }

    protected String printProcessDefinitionIdentifierMessage(ProcessInstanceMigrationDocument document) {
        String id = document.getMigrateToProcessDefinitionId();
        String key = document.getMigrateToProcessDefinitionKey();
        int version = document.getMigrateToProcessDefinitionVersion();
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

