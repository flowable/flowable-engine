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

import static org.flowable.engine.impl.bpmn.helper.AbstractClassDelegate.defaultInstantiateDelegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchBuilder;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.scripting.ScriptEngineRequest;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.bpmn.helper.DelegateExpressionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.ActivityBehaviorInvocation;
import org.flowable.engine.impl.delegate.invocation.JavaDelegateInvocation;
import org.flowable.engine.impl.dynamic.AbstractDynamicStateManager;
import org.flowable.engine.impl.dynamic.EnableActivityContainer;
import org.flowable.engine.impl.dynamic.MoveExecutionEntityContainer;
import org.flowable.engine.impl.dynamic.ProcessInstanceChangeState;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.jobexecutor.ProcessInstanceMigrationJobHandler;
import org.flowable.engine.impl.jobexecutor.ProcessInstanceMigrationStatusJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.EnableActivityMapping;
import org.flowable.engine.migration.ProcessInstanceBatchMigrationResult;
import org.flowable.engine.migration.ProcessInstanceMigrationCallback;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.migration.Script;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

public class ProcessInstanceMigrationManagerImpl extends AbstractDynamicStateManager implements ProcessInstanceMigrationManager {

    Predicate<ExecutionEntity> isSubProcessExecution = executionEntity -> executionEntity.getCurrentFlowElement() instanceof SubProcess;
    Predicate<ExecutionEntity> isBoundaryEventExecution = executionEntity -> executionEntity.getCurrentFlowElement() instanceof BoundaryEvent;
    Predicate<ExecutionEntity> isCallActivityExecution = executionEntity -> executionEntity.getCurrentFlowElement() instanceof CallActivity;
    Predicate<ExecutionEntity> isActiveExecution = ExecutionEntity::isActive;
    Predicate<ExecutionEntity> executionHasCurrentActivityId = executionEntity -> executionEntity.getCurrentActivityId() != null;

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstancesOfProcessDefinition(String procDefKey, int procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessDefinition processDefinition = resolveProcessDefinition(procDefKey, procDefVer, procDefTenantId, commandContext);
        return validateMigrateProcessInstancesOfProcessDefinition(processDefinition.getId(), document, commandContext);
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessInstanceMigrationValidationResult validationResult = new ProcessInstanceMigrationValidationResult();
        ProcessDefinition processDefinition = resolveProcessDefinition(document, commandContext);
        if (processDefinition == null) {
            validationResult.addValidationMessage("Cannot find the process definition to migrate to " + printProcessDefinitionIdentifierMessage(document));
        } else {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            if (bpmnModel == null) {
                validationResult.addValidationMessage("Cannot find the Bpmn model of the process definition to migrate to, with " + printProcessDefinitionIdentifierMessage(document));
            } else {
                BpmnModel newModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
        
                ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
                ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();
                List<ProcessInstance> processInstances = executionEntityManager.findProcessInstanceByQueryCriteria(
                        new ProcessInstanceQueryImpl(commandContext, processEngineConfiguration).processDefinitionId(processDefinitionId));
        
                for (ProcessInstance processInstance : processInstances) {
                    doValidateProcessInstanceMigration(processInstance.getId(), processDefinition.getTenantId(), newModel, document, validationResult, commandContext);
                }
            }
        }
        
        return validationResult;
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessInstanceMigrationValidationResult validationResult = new ProcessInstanceMigrationValidationResult();
        // Check that the processDefinition exists and get its associated BpmnModel
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

    protected void doValidateProcessInstanceMigration(String processInstanceId, String tenantId, BpmnModel newModel, 
                    ProcessInstanceMigrationDocument document, ProcessInstanceMigrationValidationResult validationResult, CommandContext commandContext) {
        
        // Check that the processInstance exists
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceId);
        if (processInstanceExecution == null) {
            validationResult.addValidationMessage("Cannot find process instance with id:'" + processInstanceId + "'");
            return;
        }

        doValidateActivityMappings(processInstanceId, document.getActivityMigrationMappings(), newModel, document, validationResult, commandContext);
    }

    protected void doValidateActivityMappings(String processInstanceId, List<ActivityMigrationMapping> activityMappings, BpmnModel newModel, 
                    ProcessInstanceMigrationDocument document, ProcessInstanceMigrationValidationResult validationResult, CommandContext commandContext) {
        
        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager();
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processInstanceExecution = executionEntityManager.findById(processInstanceId);
        BpmnModel currentModel = ProcessDefinitionUtil.getBpmnModel(processInstanceExecution.getProcessDefinitionId());

        HashMap<String, ActivityMigrationMapping> mainProcessActivityMappingByFromActivityId = new HashMap<>();
        HashMap<String, HashMap<String, ActivityMigrationMapping>> subProcessActivityMappingsByCallActivityIdAndFromActivityId = new HashMap<>();

        for (ActivityMigrationMapping activityMigrationMapping : activityMappings) {
            splitMigrationMappingByCallActivitySubProcessScope(activityMigrationMapping, mainProcessActivityMappingByFromActivityId, subProcessActivityMappingsByCallActivityIdAndFromActivityId);
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
                                validationResult.addValidationMessage(String.format("Incomplete migration mapping for call activity. Activity '%s' is not a Call Activity in the new model. "
                                                + "Running subProcess activities '%s' should also be mapped for migration (or the call activity itself)", 
                                                executionActivityId, childSubProcessExecutionActivityIds));
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
                    String flowElementMultiInstanceParentId = getFlowElementMultiInstanceParentId(currentModelFlowElement);
                    if (flowElementMultiInstanceParentId == null || !mainProcessActivityMappingByFromActivityId.containsKey(flowElementMultiInstanceParentId)) {
                        validationResult.addValidationMessage("Process instance (id:'" + processInstanceId + "') has a running Activity (id:'" + executionActivityId + 
                        		"') that is not mapped for migration (Or its Multi-Instance parent)");
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
                    if (callActivityFlowElement instanceof CallActivity callActivity) {
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
                        validationResult.addValidationMessage("Invalid mapping for '" + execution.getCurrentActivityId() + "' to '" + 
                        		targetActivityId + "', cannot be found in the process definition with id '" + mappingModel.getMainProcess().getId() + "'");
                        continue;
                    }
                    //We cannot move an activity inside a MultiInstance container
                    FlowElement targetFlowElement = mappingModel.getFlowElement(targetActivityId);
                    String targetFlowElementMultiInstanceParentId = getFlowElementMultiInstanceParentId(targetFlowElement);
                    if (targetFlowElementMultiInstanceParentId != null) {
                        validationResult.addValidationMessage("Invalid mapping for '" + execution.getCurrentActivityId() + "' to '" + targetActivityId + "', cannot migrate arbitrarily inside a Multi Instance container '" + 
                        		targetFlowElementMultiInstanceParentId + "' inside process definition with id '" + mappingModel.getMainProcess().getId() + "'");
                        continue;
                    }
                }
            }
        }
    }

    @Override
    public Batch batchMigrateProcessInstancesOfProcessDefinition(String procDefKey, int procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessDefinition processDefinition = resolveProcessDefinition(procDefKey, procDefVer, procDefTenantId, commandContext);
        return batchMigrateProcessInstancesOfProcessDefinition(processDefinition.getId(), document, commandContext);
    }

    @Override
    public Batch batchMigrateProcessInstancesOfProcessDefinition(String sourceProcDefId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        // Check of the target definition exists before submitting the batch
        ProcessDefinition targetProcessDefinition = resolveProcessDefinition(document, commandContext);

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();
        List<ProcessInstance> processInstances = executionEntityManager.findProcessInstanceByQueryCriteria(
                new ProcessInstanceQueryImpl(commandContext, processEngineConfiguration).processDefinitionId(sourceProcDefId));

        BatchService batchService = processEngineConfiguration.getBatchServiceConfiguration().getBatchService();
        BatchBuilder batchBuilder = batchService.createBatchBuilder().batchType(Batch.PROCESS_MIGRATION_TYPE)
            .searchKey(sourceProcDefId)
            .searchKey2(targetProcessDefinition.getId())
            .status(ProcessInstanceBatchMigrationResult.STATUS_IN_PROGRESS)
            .batchDocumentJson(document.asJsonString());
        if (targetProcessDefinition.getTenantId() != null) {
            batchBuilder.tenantId(targetProcessDefinition.getTenantId());
        }
        Batch batch = batchBuilder.create();

        JobService jobService = processEngineConfiguration.getJobServiceConfiguration().getJobService();
        for (ProcessInstance processInstance : processInstances) {
            BatchPart batchPart = batchService.createBatchPart(batch, ProcessInstanceBatchMigrationResult.STATUS_WAITING, 
                            processInstance.getId(), null, ScopeTypes.BPMN);
            
            JobEntity job = jobService.createJob();
            job.setJobHandlerType(ProcessInstanceMigrationJobHandler.TYPE);
            job.setProcessInstanceId(processInstance.getId());
            job.setJobHandlerConfiguration(ProcessInstanceMigrationJobHandler.getHandlerCfgForBatchPartId(batchPart.getId()));
            job.setTenantId(processInstance.getTenantId());
            jobService.createAsyncJob(job, false);
            job.setRetries(0);
            jobService.scheduleAsyncJob(job);
        }
        
        if (!processInstances.isEmpty()) {
            TimerJobService timerJobService = processEngineConfiguration.getJobServiceConfiguration().getTimerJobService();
            TimerJobEntity timerJob = timerJobService.createTimerJob();
            timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
            timerJob.setRevision(1);
            timerJob.setRetries(0);
            timerJob.setJobHandlerType(ProcessInstanceMigrationStatusJobHandler.TYPE);
            timerJob.setJobHandlerConfiguration(ProcessInstanceMigrationJobHandler.getHandlerCfgForBatchId(batch.getId()));
            timerJob.setTenantId(batch.getTenantId());
            
            BusinessCalendar businessCalendar = processEngineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
            timerJob.setDuedate(businessCalendar.resolveDuedate(processEngineConfiguration.getBatchStatusTimeCycleConfig()));
            timerJob.setRepeat(processEngineConfiguration.getBatchStatusTimeCycleConfig());
            
            timerJobService.scheduleTimerJob(timerJob);
        }

        return batch;
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String procDefKey, int procDefVer, String procDefTenantId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessDefinition processDefinition = resolveProcessDefinition(procDefKey, procDefVer, procDefTenantId, commandContext);
        migrateProcessInstancesOfProcessDefinition(processDefinition.getId(), document, commandContext);
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ProcessDefinition processDefinition = resolveProcessDefinition(document, commandContext);
        if (processDefinition == null) {
            throw new FlowableException("Cannot find the process definition to migrate to, identified by " + printProcessDefinitionIdentifierMessage(document));
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ProcessInstanceQueryImpl processInstanceQueryByProcessDefinitionId = new ProcessInstanceQueryImpl(commandContext, processEngineConfiguration).processDefinitionId(processDefinitionId);
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();
        List<ProcessInstance> processInstances = executionEntityManager.findProcessInstanceByQueryCriteria(processInstanceQueryByProcessDefinitionId);

        for (ProcessInstance processInstance : processInstances) {
            doMigrateProcessInstance(processInstance, processDefinition, document, commandContext);
        }
    }

    @Override
    public void migrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processExecution = executionEntityManager.findById(processInstanceId);
        if (processExecution == null) {
            throw new FlowableException("Cannot find the process to migrate, with id" + processInstanceId);
        }

        ProcessDefinition procDefToMigrateTo = resolveProcessDefinition(document, commandContext);
        doMigrateProcessInstance(processExecution, procDefToMigrateTo, document, commandContext);
    }

    protected void doMigrateProcessInstance(ProcessInstance processInstance, ProcessDefinition procDefToMigrateTo, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        LOGGER.debug("Start migration of process instance with Id:'{}' to process definition identified by {}", processInstance.getId(),
            printProcessDefinitionIdentifierMessage(document));

        if (document.getPreUpgradeScript() != null) {
            LOGGER.debug("Execute pre upgrade process instance script");
            executeScript(processInstance, procDefToMigrateTo, document.getPreUpgradeScript(), commandContext);
        }

        if (document.getPreUpgradeJavaDelegate() != null) {
            LOGGER.debug("Execute pre upgrade process instance script");
            executeJavaDelegate(processInstance, procDefToMigrateTo, document.getPreUpgradeJavaDelegate(), commandContext);
        }

        if (document.getPreUpgradeJavaDelegateExpression() != null) {
            LOGGER.debug("Execute pre upgrade process instance script");
            executeExpression(processInstance, procDefToMigrateTo, document.getPreUpgradeJavaDelegateExpression(), commandContext);
        }

        ExecutionEntity processInstanceEntity = (ExecutionEntity) processInstance;
        List<ChangeActivityStateBuilderImpl> changeActivityStateBuilders = prepareChangeStateBuilders(processInstanceEntity, 
        		procDefToMigrateTo, document, commandContext);

        LOGGER.debug("Updating Process definition reference of process root execution with id:'{}' to '{}'", processInstance.getId(), procDefToMigrateTo.getId());
        processInstanceEntity.setProcessDefinitionId(procDefToMigrateTo.getId());

        LOGGER.debug("Resolve activity executions to migrate");
        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = new ArrayList<>();
        for (ChangeActivityStateBuilderImpl builder : changeActivityStateBuilders) {
        	List<MoveExecutionEntityContainer> moveExecutionEntityContainers = resolveMoveExecutionEntityContainers(
        			builder, document.getProcessInstanceVariables(), commandContext);
            moveExecutionEntityContainerList.addAll(moveExecutionEntityContainers);
        }
        
        List<EnableActivityContainer> enableActivityContainerList = new ArrayList<>();
        if (!document.getEnableActivityMappings().isEmpty()) {
            for (EnableActivityMapping enableActivityMapping : document.getEnableActivityMappings()) {
                EnableActivityContainer enableActivityContainer = new EnableActivityContainer(Collections.singletonList(enableActivityMapping.getActivityId()));
                enableActivityContainerList.add(enableActivityContainer);
            }
        }

        ProcessInstanceChangeState processInstanceChangeState = new ProcessInstanceChangeState()
            .setProcessInstanceId(processInstance.getId())
            .setProcessDefinitionToMigrateTo(procDefToMigrateTo)
            .setMoveExecutionEntityContainers(moveExecutionEntityContainerList)
            .setEnableActivityContainers(enableActivityContainerList)
            .setProcessInstanceVariables(document.getProcessInstanceVariables())
            .setLocalVariables(document.getActivitiesLocalVariables());

        doMoveExecutionState(processInstanceChangeState, commandContext);

        LOGGER.debug("Updating process definition reference in activity instances");
        CommandContextUtil.getActivityInstanceEntityManager().updateActivityInstancesProcessDefinitionId(procDefToMigrateTo.getId(), processInstance.getId());

        LOGGER.debug("Updating Process definition reference in history");
        changeProcessDefinitionReferenceOfHistory(processInstance, procDefToMigrateTo, commandContext);

        if (document.getPostUpgradeScript() != null) {
            LOGGER.debug("Execute post upgrade process instance script");
            executeScript(processInstance, procDefToMigrateTo, document.getPostUpgradeScript(), commandContext);
        }

        if (document.getPostUpgradeJavaDelegate() != null) {
            LOGGER.debug("Execute post upgrade process instance script");
            executeJavaDelegate(processInstance, procDefToMigrateTo, document.getPostUpgradeJavaDelegate(), commandContext);
        }

        if (document.getPostUpgradeJavaDelegateExpression() != null) {
            LOGGER.debug("Execute post upgrade process instance script");
            executeExpression(processInstance, procDefToMigrateTo, document.getPostUpgradeJavaDelegateExpression(), commandContext);
        }
        
        List<ProcessInstanceMigrationCallback> migrationCallbacks = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceMigrationCallbacks();
        if (migrationCallbacks != null && !migrationCallbacks.isEmpty()) {
            for (ProcessInstanceMigrationCallback processInstanceMigrationCallback : migrationCallbacks) {
                processInstanceMigrationCallback.processInstanceMigrated(processInstance, procDefToMigrateTo, document, commandContext);
            }
        }

        LOGGER.debug("Process migration ended for process instance with Id:'{}'", processInstance.getId());
    }

    @Override
    protected Map<String, List<ExecutionEntity>> resolveActiveEmbeddedSubProcesses(String processInstanceId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        List<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);

        Map<String, List<ExecutionEntity>> activeSubProcessesByActivityId = childExecutions.stream()
            .filter(ExecutionEntity::isActive)
            .filter(executionEntity -> executionEntity.getCurrentFlowElement() instanceof SubProcess)
            .filter(executionEntity -> ((SubProcess) executionEntity.getCurrentFlowElement()).hasMultiInstanceLoopCharacteristics())
            .collect(Collectors.groupingBy(ExecutionEntity::getActivityId));
        return activeSubProcessesByActivityId;
    }

    @Override
    protected boolean isDirectFlowElementExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        //Activities inside or that are MultiInstance cannot be migrated directly, as it is better to trigger the MultiInstanceBehavior using the agenda, directMigration skips the agenda

        return (isDirectCallActivityExecutionMigration(currentFlowElement, newFlowElement) ||
                isDirectUserTaskExecutionMigration(currentFlowElement, newFlowElement) ||
                isDirectAsyncServiceTaskExecutionMigration(currentFlowElement, newFlowElement) ||
                isDirectReceiveTaskExecutionMigration(currentFlowElement, newFlowElement) ||
                isDirectExternalWorkerServiceTaskExecutionMigration(currentFlowElement, newFlowElement)) &&
                (getFlowElementMultiInstanceParentId(currentFlowElement) == null && getFlowElementMultiInstanceParentId(newFlowElement) == null);
    }

    protected boolean isDirectCallActivityExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        return currentFlowElement instanceof CallActivity &&
                newFlowElement instanceof CallActivity &&
                ((CallActivity) currentFlowElement).getLoopCharacteristics() == null &&
                ((CallActivity) newFlowElement).getLoopCharacteristics() == null;
    }

    protected boolean isDirectUserTaskExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        return currentFlowElement instanceof UserTask &&
                newFlowElement instanceof UserTask &&
                ((Task) currentFlowElement).getLoopCharacteristics() == null &&
                ((Task) newFlowElement).getLoopCharacteristics() == null;
    }
    
    protected boolean isDirectAsyncServiceTaskExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        return currentFlowElement instanceof ServiceTask &&
                newFlowElement instanceof ServiceTask &&
                ((Task) currentFlowElement).getLoopCharacteristics() == null &&
                ((Task) newFlowElement).getLoopCharacteristics() == null &&
                ((((ServiceTask) currentFlowElement).isAsynchronous() &&
                ((ServiceTask) newFlowElement).isAsynchronous()) ||
                (((ServiceTask) currentFlowElement).isAsynchronousLeave() &&
                ((ServiceTask) newFlowElement).isAsynchronousLeave()));
    }

    protected boolean isDirectReceiveTaskExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        return currentFlowElement instanceof ReceiveTask &&
                newFlowElement instanceof ReceiveTask &&
                ((Task) currentFlowElement).getLoopCharacteristics() == null &&
                ((Task) newFlowElement).getLoopCharacteristics() == null;
    }

    protected boolean isDirectExternalWorkerServiceTaskExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        //The current and new external worker service task must be equal to support direct execution migration
        if (currentFlowElement instanceof ExternalWorkerServiceTask currentExternalWorkerServiceTask && newFlowElement instanceof ExternalWorkerServiceTask newExternalWorkerServiceTask) {
            return currentExternalWorkerServiceTask.getLoopCharacteristics() == null &&
                    newExternalWorkerServiceTask.getLoopCharacteristics() == null &&
                    new EqualsBuilder()
                            .append(currentExternalWorkerServiceTask.getId(), newExternalWorkerServiceTask.getId())
                            .append(currentExternalWorkerServiceTask.getName(), newExternalWorkerServiceTask.getName())
                            .append(currentExternalWorkerServiceTask.getTopic(), newExternalWorkerServiceTask.getTopic())
                            .append(currentExternalWorkerServiceTask.isExclusive(), newExternalWorkerServiceTask.isExclusive())
                            .append(currentExternalWorkerServiceTask.isAsynchronous(), newExternalWorkerServiceTask.isAsynchronous())
                            .isEquals();
        }
        return false;
    }

    protected void executeScript(ProcessInstance processInstance, ProcessDefinition procDefToMigrateTo, Script script, CommandContext commandContext) {
        ScriptingEngines scriptingEngines = CommandContextUtil.getProcessEngineConfiguration(commandContext).getScriptingEngines();

        try {
            ScriptEngineRequest.Builder builder = ScriptEngineRequest.builder()
                    .script(script.getScript())
                    .language(script.getLanguage())
                    .scopeContainer((ExecutionEntityImpl) processInstance);
            scriptingEngines.evaluate(builder.build());
        } catch (FlowableException e) {
            LOGGER.warn("Exception while executing upgrade of process instance {} : {}", processInstance.getId(), e.getMessage());
            throw e;
        }
    }

    protected void executeJavaDelegate(ProcessInstance processInstance, ProcessDefinition procDefToMigrateTo, 
            String preUpgradeJavaDelegate, CommandContext commandContext) {
        
        CommandContextUtil.getProcessEngineConfiguration(commandContext).getDelegateInterceptor()
            .handleInvocation(new JavaDelegateInvocation((JavaDelegate) defaultInstantiateDelegate(preUpgradeJavaDelegate, Collections.emptyList()),
                (ExecutionEntityImpl) processInstance));
    }

    protected void executeExpression(ProcessInstance processInstance, ProcessDefinition procDefToMigrateTo, 
            String preUpgradeJavaDelegateExpression, CommandContext commandContext) {
        
        Expression expression = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager().createExpression(preUpgradeJavaDelegateExpression);

        Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, (VariableContainer) processInstance, Collections.emptyList());
        if (delegate instanceof ActivityBehavior) {
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getDelegateInterceptor().handleInvocation(new ActivityBehaviorInvocation((ActivityBehavior) delegate, (ExecutionEntityImpl) processInstance));
        } else if (delegate instanceof JavaDelegate) {
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getDelegateInterceptor().handleInvocation(new JavaDelegateInvocation((JavaDelegate) delegate, (ExecutionEntityImpl) processInstance));
        } else {
            throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did neither resolve to an implementation of " + ActivityBehavior.class + " nor " + JavaDelegate.class);
        }
    }

    protected List<ChangeActivityStateBuilderImpl> prepareChangeStateBuilders(ExecutionEntity processInstanceExecution, ProcessDefinition procDefToMigrateTo, ProcessInstanceMigrationDocument document, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        // Check processDefinition tenant
        String procDefTenantId = procDefToMigrateTo.getTenantId();
        if (!isSameOrDefaultTenant(processInstanceExecution.getTenantId(), procDefToMigrateTo.getKey(), 
                procDefTenantId, CommandContextUtil.getProcessEngineConfiguration(commandContext))) {
            
            throw new FlowableException("Tenant mismatch between Process Instance ('" + processInstanceExecution.getTenantId() + "') and Process Definition ('" + procDefTenantId + "') to migrate to");
        }

        List<ChangeActivityStateBuilderImpl> changeActivityStateBuilders = new ArrayList<>();
        ChangeActivityStateBuilderImpl mainProcessChangeActivityStateBuilder = new ChangeActivityStateBuilderImpl();
        mainProcessChangeActivityStateBuilder.processInstanceId(processInstanceExecution.getId());
        changeActivityStateBuilders.add(mainProcessChangeActivityStateBuilder);

        // Current executions to migrate...
        Map<String, List<ExecutionEntity>> filteredExecutionsByActivityId = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceExecution.getId())
            .stream()
            .filter(executionEntity -> executionEntity.getCurrentActivityId() != null)
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof SubProcess))
            .filter(executionEntity -> !(executionEntity.getCurrentFlowElement() instanceof BoundaryEvent))
            .collect(Collectors.groupingBy(ExecutionEntity::getCurrentActivityId));

        LOGGER.debug("Preparing ActivityChangeState builder for '{}' distinct activities", filteredExecutionsByActivityId.size());

        HashMap<String, ActivityMigrationMapping> mainProcessActivityMappingByFromActivityId = new HashMap<>();
        HashMap<String, HashMap<String, ActivityMigrationMapping>> subProcessActivityMappingsByCallActivityIdAndFromActivityId = new HashMap<>();
        for (ActivityMigrationMapping activityMigrationMapping : document.getActivityMigrationMappings()) {
            splitMigrationMappingByCallActivitySubProcessScope(activityMigrationMapping, mainProcessActivityMappingByFromActivityId, subProcessActivityMappingsByCallActivityIdAndFromActivityId);
        }

        Set<String> mappedFromActivities = mainProcessActivityMappingByFromActivityId.keySet();

        // Partition the executions by Explicitly mapped or not
        Map<Boolean, List<String>> partitionedExecutionActivityIds = filteredExecutionsByActivityId.keySet()
            .stream()
            .collect(Collectors.partitioningBy(mappedFromActivities::contains));
        List<String> executionActivityIdsToAutoMap = partitionedExecutionActivityIds.get(false);
        List<String> executionActivityIdsToMapExplicitly = partitionedExecutionActivityIds.get(true);

        BpmnModel newModel = ProcessDefinitionUtil.getBpmnModel(procDefToMigrateTo.getId());
        BpmnModel currentModel = ProcessDefinitionUtil.getBpmnModel(processInstanceExecution.getProcessDefinitionId());

        // Auto Mapping
        LOGGER.debug("Process AutoMapping for '{}' activity executions", executionActivityIdsToAutoMap.size());
        for (String executionActivityId : executionActivityIdsToAutoMap) {
            FlowElement currentModelFlowElement = currentModel.getFlowElement(executionActivityId);

            if (currentModelFlowElement instanceof CallActivity) {
                // Check that all or none of the call activity child activities executions are explicitly mapped
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
            }

            String flowElementMultiInstanceParentId = getFlowElementMultiInstanceParentId(currentModelFlowElement);
            if (flowElementMultiInstanceParentId != null && mappedFromActivities.contains(flowElementMultiInstanceParentId)) {
                // Add the parent MI execution activity Id to be explicitly mapped...
                if (!executionActivityIdsToMapExplicitly.contains(flowElementMultiInstanceParentId)) {
                    executionActivityIdsToMapExplicitly.add(flowElementMultiInstanceParentId);
                }
                // The root executions are the ones to migrate and are explicitly mapped
                List<ExecutionEntity> miRootExecutions = (List<ExecutionEntity>) executionEntityManager.findInactiveExecutionsByActivityIdAndProcessInstanceId(flowElementMultiInstanceParentId, processInstanceExecution.getId());
                filteredExecutionsByActivityId.put(flowElementMultiInstanceParentId, miRootExecutions);
                
            } else {
                LOGGER.debug("Checking execution(s) - activityId:'{}", executionActivityId);
                if (isActivityIdInProcessDefinitionModel(executionActivityId, newModel)) {
                    // Cannot auto-map inside a MultiInstance container
                    FlowElement newModelFlowElement = newModel.getFlowElement(executionActivityId);
                    String newFlowElementMIParentId = getFlowElementMultiInstanceParentId(newModelFlowElement);

                    if (newFlowElementMIParentId != null) {
                    	boolean noChangesInMI = false;
                    	Activity currentMIElement = (Activity) currentModel.getFlowElement(newFlowElementMIParentId);
                    	Activity newMIElement = (Activity) newModel.getFlowElement(newFlowElementMIParentId);
                    	if (currentMIElement.getClass().getName().equals(newMIElement.getClass().getName()) &&
                    			hasSameLoopCharacteristics(currentMIElement, newMIElement)) {
                    		
                    		if (!(currentMIElement instanceof SubProcess)) {
                    			noChangesInMI = true;
                    		} else if (hasSameSubProcessContent((SubProcess) currentMIElement, (SubProcess) newMIElement)) {
                    			noChangesInMI = true;
                    		}
                    	}
                    	
                    	if (!noChangesInMI) {
                    		throw new FlowableException("Cannot autoMap activity migration for '" + executionActivityId + "'. Cannot migrate arbitrarily inside a Multi Instance container '" + newFlowElementMIParentId);
                    	}
                    }

                    LOGGER.debug("Auto mapping activity '{}'", executionActivityId);
                    List<ExecutionEntity> executionEntities = filteredExecutionsByActivityId.get(executionActivityId);
                    if (executionEntities.size() > 1) {
                        List<String> executionIds = executionEntities.stream().map(ExecutionEntity::getId).collect(Collectors.toList());
                        mainProcessChangeActivityStateBuilder.moveExecutionsToSingleActivityId(executionIds, executionActivityId);
                    } else {
                        mainProcessChangeActivityStateBuilder.moveExecutionToActivityId(executionEntities.get(0).getId(), executionActivityId);
                    }
                    
                } else {
                    if (!(currentModelFlowElement instanceof CallActivity)) {
                        throw new FlowableException("Migration Activity mapping missing for activity definition Id:'" + 
                        		executionActivityId + "' or its MI Parent");
                    }
                }
            }
        }

        //Explicit Mapping - Iterates over the provided mappings instead, to keep the explicit migration order
        List<ActivityMigrationMapping> activityMigrationMappings = document.getActivityMigrationMappings();

        LOGGER.debug("Process explicit mapping for '{}' activity executions", executionActivityIdsToMapExplicitly.size());
        for (
            ActivityMigrationMapping activityMapping : activityMigrationMappings) {

            if (activityMapping instanceof ActivityMigrationMapping.OneToOneMapping) {
                String fromActivityId = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getFromActivityId();
                String toActivityId = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getToActivityId();
                String newAssignee = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getWithNewAssignee();
                String newOwner = ((ActivityMigrationMapping.OneToOneMapping) activityMapping).getWithNewOwner();
                String fromCallActivityId = activityMapping.getFromCallActivityId();

                if (activityMapping.isToParentProcess() && !executionActivityIdsToMapExplicitly.contains(fromCallActivityId)) {
                    List<ExecutionEntity> callActivityExecutions = filteredExecutionsByActivityId.get(fromCallActivityId).stream().filter(ExecutionEntity::isActive).collect(Collectors.toList());
                    for (ExecutionEntity callActivityExecution : callActivityExecutions) {
                        ExecutionEntity subProcessInstanceExecution = executionEntityManager.findSubProcessInstanceBySuperExecutionId(callActivityExecution.getId());
                        ChangeActivityStateBuilderImpl subProcessChangeActivityStateBuilder = new ChangeActivityStateBuilderImpl();
                        subProcessChangeActivityStateBuilder.processInstanceId(subProcessInstanceExecution.getId());
                        subProcessChangeActivityStateBuilder.moveActivityIdToParentActivityId(fromActivityId, toActivityId, newAssignee, newOwner);
                        changeActivityStateBuilders.add(subProcessChangeActivityStateBuilder);
                    }
                    
                } else if (executionActivityIdsToMapExplicitly.contains(fromActivityId)) {
                    if (activityMapping.isToCallActivity()) {
                        mainProcessChangeActivityStateBuilder.moveActivityIdToSubProcessInstanceActivityId(fromActivityId, toActivityId,
                                activityMapping.getToCallActivityId(),
                                activityMapping.getCallActivityProcessDefinitionVersion(),
                                newAssignee,
                                newOwner);
                        
                    } else {
                        mainProcessChangeActivityStateBuilder.moveActivityIdTo(fromActivityId, toActivityId, newAssignee, newOwner);
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
                String newOwner = ((ActivityMigrationMapping.ManyToOneMapping) activityMapping).getWithNewOwner();
                if (activityMapping.isToParentProcess() && !executionActivityIdsToMapExplicitly.contains(fromCallActivityId)) {
                    List<ExecutionEntity> callActivityExecutions = filteredExecutionsByActivityId.get(fromCallActivityId).stream().filter(ExecutionEntity::isActive).collect(Collectors.toList());
                    for (ExecutionEntity callActivityExecution : callActivityExecutions) {
                        ExecutionEntity subProcessInstanceExecution = executionEntityManager.findSubProcessInstanceBySuperExecutionId(callActivityExecution.getId());
                        ChangeActivityStateBuilderImpl subProcessChangeActivityStateBuilder = new ChangeActivityStateBuilderImpl();
                        subProcessChangeActivityStateBuilder.processInstanceId(subProcessInstanceExecution.getId());
                        subProcessChangeActivityStateBuilder.moveActivityIdsToParentActivityId(fromActivityIds, toActivityId, newAssignee, newOwner);
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
                        mainProcessChangeActivityStateBuilder.moveActivityIdsToSubProcessInstanceActivityId(fromActivityIds, toActivityId,
                                activityMapping.getToCallActivityId(),
                                activityMapping.getCallActivityProcessDefinitionVersion(),
                                newAssignee,
                                newOwner);
                    } else {
                        mainProcessChangeActivityStateBuilder.moveExecutionsToSingleActivityId(executionIds, toActivityId, newAssignee, newOwner);
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

    protected boolean isSameOrDefaultTenant(String processInstanceTenantId, String processDefinitionKey, 
            String processDefinitionTenantId, ProcessEngineConfigurationImpl processEngineConfiguration) {
        
        if (processInstanceTenantId != null && processDefinitionTenantId != null) {
            boolean tenantIdsEqual = processInstanceTenantId.equals(processDefinitionTenantId);
            if (!tenantIdsEqual && processEngineConfiguration.isFallbackToDefaultTenant() && processEngineConfiguration.getDefaultTenantProvider() != null) {
                return processDefinitionTenantId.equals(processEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(processInstanceTenantId, ScopeTypes.BPMN, processDefinitionKey));
            }
            
            return tenantIdsEqual;
            
        } else if (processInstanceTenantId == null && processDefinitionTenantId == null) {
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
            return resolveProcessDefinition(document.getMigrateToProcessDefinitionKey(), document.getMigrateToProcessDefinitionVersion(), 
                            document.getMigrateToProcessDefinitionTenantId(), commandContext);
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
        return id != null ? "[id:'" + id + "']" : "[key:'" + key + "', version:'" + version + "', tenantId:'" + tenantId + "']";
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
    
    protected boolean hasSameLoopCharacteristics(Activity currentActivity, Activity newActivity) {
    	boolean hasSameMI = false;
    	MultiInstanceLoopCharacteristics currentMIConfig = currentActivity.getLoopCharacteristics();
    	MultiInstanceLoopCharacteristics newMIConfig = newActivity.getLoopCharacteristics();
    	if (currentMIConfig.isSequential() == newMIConfig.isSequential() &&
    			hasSameStringValue(currentMIConfig.getCollectionString(), newMIConfig.getCollectionString()) &&
    			hasSameStringValue(currentMIConfig.getLoopCardinality(), newMIConfig.getLoopCardinality()) &&
    			hasSameStringValue(currentMIConfig.getCompletionCondition(), newMIConfig.getCompletionCondition()) &&
    			hasSameStringValue(currentMIConfig.getElementIndexVariable(), newMIConfig.getElementIndexVariable()) &&
    			hasSameStringValue(currentMIConfig.getElementVariable(), newMIConfig.getElementVariable()) && 
    			hasSameStringValue(currentMIConfig.getInputDataItem(), newMIConfig.getInputDataItem()) &&
    			hasSameStringValue(currentMIConfig.getLoopCardinality(), newMIConfig.getLoopCardinality())) {
    			
    		hasSameMI = true;
    	}
    	
    	return hasSameMI;
    }
    
    protected boolean hasSameSubProcessContent(SubProcess currentSubProcess, SubProcess newSubProcess) {
    	if (currentSubProcess.getFlowElements().size() != newSubProcess.getFlowElements().size()) {
    		return false;
    	}
    	
    	boolean hasSameContent = true;
    	for (FlowElement subElement : currentSubProcess.getFlowElements()) {
			FlowElement newSubElement = newSubProcess.getFlowElement(subElement.getId());
			if (newSubElement == null) {
				hasSameContent = false;
				break;
			}
			
			if (!(subElement.getClass().getName().equals(newSubElement.getClass().getName()))) {
				hasSameContent = false;
				break;
			}
		}
    	
    	return hasSameContent;
    }

    protected void splitMigrationMappingByCallActivitySubProcessScope(ActivityMigrationMapping activityMigrationMapping, HashMap<String, ActivityMigrationMapping> mainProcessActivityMappingByFromActivityId, HashMap<String, HashMap<String, ActivityMigrationMapping>> subProcessActivityMappingsByCallActivityIdAndFromActivityId) {
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

    protected boolean hasSameStringValue(String currentValue, String newValue) {
    	boolean sameValue = false;
    	if ((currentValue == null && newValue == null) ||
    			(currentValue != null && currentValue.equals(newValue))) {
    		
    		sameValue = true;
    	}
    	
    	return sameValue;
    }
}

