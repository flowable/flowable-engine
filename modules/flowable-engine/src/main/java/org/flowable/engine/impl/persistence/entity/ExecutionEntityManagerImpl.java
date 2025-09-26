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

package org.flowable.engine.impl.persistence.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmmn.CaseInstanceService;
import org.flowable.engine.impl.delegate.InterruptibleActivityBehaviour;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.engine.impl.util.BpmnLoggingSessionUtil;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.EventUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.identitylink.service.IdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher.IdentityLinksByProcessInstanceMatcher;
import org.flowable.job.service.JobService;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ExecutionEntityManagerImpl
    extends AbstractProcessEngineEntityManager<ExecutionEntity, ExecutionDataManager>
    implements ExecutionEntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionEntityManagerImpl.class);

    public ExecutionEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ExecutionDataManager executionDataManager) {
        super(processEngineConfiguration, executionDataManager);
    }

    // Overriding the default delete methods to set the 'isDeleted' flag

    @Override
    public void delete(ExecutionEntity entity) {
        delete(entity, true);
    }

    @Override
    public void delete(ExecutionEntity entity, boolean fireDeleteEvent) {
        super.delete(entity, fireDeleteEvent);
        entity.setDeleted(true);
    }

    // FIND METHODS

    @Override
    public ExecutionEntity findSubProcessInstanceBySuperExecutionId(String superExecutionId) {
        return dataManager.findSubProcessInstanceBySuperExecutionId(superExecutionId);
    }

    @Override
    public List<ExecutionEntity> findChildExecutionsByParentExecutionId(String parentExecutionId) {
        return dataManager.findChildExecutionsByParentExecutionId(parentExecutionId);
    }

    @Override
    public List<ExecutionEntity> findChildExecutionsByProcessInstanceId(String processInstanceId) {
        return dataManager.findChildExecutionsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(final String parentExecutionId, final Collection<String> activityIds) {
        return dataManager.findExecutionsByParentExecutionAndActivityIds(parentExecutionId, activityIds);
    }

    @Override
    public long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery) {
        return dataManager.findExecutionCountByQueryCriteria(executionQuery);
    }

    @Override
    public List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery) {
        return dataManager.findExecutionsByQueryCriteria(executionQuery);
    }

    @Override
    public long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return dataManager.findProcessInstanceCountByQueryCriteria(executionQuery);
    }

    @Override
    public List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return dataManager.findProcessInstanceByQueryCriteria(executionQuery);
    }

    @Override
    public ExecutionEntity findByRootProcessInstanceId(String rootProcessInstanceId) {
        List<ExecutionEntity> executions = dataManager.findExecutionsByRootProcessInstanceId(rootProcessInstanceId);
        return processExecutionTree(rootProcessInstanceId, executions);

    }

    /**
     * Processes a collection of {@link ExecutionEntity} instances, which form on execution tree. All the executions share the same rootProcessInstanceId (which is provided). The return value will be
     * the root {@link ExecutionEntity} instance, with all child {@link ExecutionEntity} instances populated and set using the {@link ExecutionEntity} instances from the provided collections
     */
    protected ExecutionEntity processExecutionTree(String rootProcessInstanceId, List<ExecutionEntity> executions) {
        ExecutionEntity rootExecution = null;

        // Collect executions
        Map<String, ExecutionEntity> executionMap = new HashMap<>(executions.size());
        for (ExecutionEntity executionEntity : executions) {
            if (executionEntity.getId().equals(rootProcessInstanceId)) {
                rootExecution = executionEntity;
            }
            executionMap.put(executionEntity.getId(), executionEntity);
        }

        // Set relationships
        for (ExecutionEntity executionEntity : executions) {

            // Root process instance relationship
            if (executionEntity.getRootProcessInstanceId() != null) {
                executionEntity.setRootProcessInstance(executionMap.get(executionEntity.getRootProcessInstanceId()));
            }

            // Process instance relationship
            if (executionEntity.getProcessInstanceId() != null) {
                executionEntity.setProcessInstance(executionMap.get(executionEntity.getProcessInstanceId()));
            }

            // Parent - child relationship
            if (executionEntity.getParentId() != null) {
                ExecutionEntity parentExecutionEntity = executionMap.get(executionEntity.getParentId());
                executionEntity.setParent(parentExecutionEntity);
                parentExecutionEntity.addChildExecution(executionEntity);
            }

            // Super - sub execution relationship
            if (executionEntity.getSuperExecution() != null) {
                ExecutionEntity superExecutionEntity = executionMap.get(executionEntity.getSuperExecutionId());
                executionEntity.setSuperExecution(superExecutionEntity);
                superExecutionEntity.setSubProcessInstance(executionEntity);
            }

        }
        return rootExecution;
    }

    @Override
    public List<ProcessInstance> findProcessInstanceAndVariablesByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return dataManager.findProcessInstanceAndVariablesByQueryCriteria(executionQuery);
    }

    @Override
    public Collection<ExecutionEntity> findInactiveExecutionsByProcessInstanceId(final String processInstanceId) {
        return dataManager.findInactiveExecutionsByProcessInstanceId(processInstanceId);
    }

    @Override
    public Collection<ExecutionEntity> findInactiveExecutionsByActivityIdAndProcessInstanceId(final String activityId, final String processInstanceId) {
        return dataManager.findInactiveExecutionsByActivityIdAndProcessInstanceId(activityId, processInstanceId);
    }

    @Override
    public List<Execution> findExecutionsByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findExecutionsByNativeQuery(parameterMap);
    }

    @Override
    public List<ProcessInstance> findProcessInstanceByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findProcessInstanceByNativeQuery(parameterMap);
    }

    @Override
    public long findExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findExecutionCountByNativeQuery(parameterMap);
    }
    @Override
    public long countActiveExecutionsByParentId(String parentId) {
        return dataManager.countActiveExecutionsByParentId(parentId);
    }

    // CREATE METHODS

    @Override
    public ExecutionEntity createProcessInstanceExecution(ProcessDefinition processDefinition, String predefinedProcessInstanceId, String businessKey,
            String businessStatus, String processInstanceName, String callbackId, String callbackType, String referenceId, String referenceType,
            String propagatedStageInstanceId, String tenantId, String initiatorVariableName, String startActivityId) {

        ExecutionEntity processInstanceExecution = dataManager.create();

        if (CountingEntityUtil.isExecutionRelatedEntityCountEnabledGlobally()) {
            ((CountingExecutionEntity) processInstanceExecution).setCountEnabled(true);
        }
        
        if (predefinedProcessInstanceId != null) {
            processInstanceExecution.setId(predefinedProcessInstanceId);
        }

        processInstanceExecution.setProcessDefinitionId(processDefinition.getId());
        processInstanceExecution.setProcessDefinitionKey(processDefinition.getKey());
        processInstanceExecution.setProcessDefinitionName(processDefinition.getName());
        processInstanceExecution.setProcessDefinitionVersion(processDefinition.getVersion());
        processInstanceExecution.setProcessDefinitionCategory(processDefinition.getCategory());
        processInstanceExecution.setDeploymentId(processDefinition.getDeploymentId());
        processInstanceExecution.setBusinessKey(businessKey);
        processInstanceExecution.setBusinessStatus(businessStatus);
        processInstanceExecution.setName(processInstanceName);
        
        // Callbacks
        if (callbackId != null) {
            processInstanceExecution.setCallbackId(callbackId);
        }
        if (callbackType != null) {
            processInstanceExecution.setCallbackType(callbackType);
        }
        if (referenceId != null) {
            processInstanceExecution.setReferenceId(referenceId);
        }
        if (referenceType != null) {
            processInstanceExecution.setReferenceType(referenceType);
        }
        if (propagatedStageInstanceId != null) {
            processInstanceExecution.setPropagatedStageInstanceId(propagatedStageInstanceId);
        }
        
        processInstanceExecution.setScope(true); // process instance is always a scope for all child executions

        // Inherit tenant id (if any)
        if (tenantId != null) {
            processInstanceExecution.setTenantId(tenantId);
        }

        String authenticatedUserId = Authentication.getAuthenticatedUserId();

        processInstanceExecution.setStartActivityId(startActivityId);
        processInstanceExecution.setStartTime(CommandContextUtil.getProcessEngineConfiguration().getClock().getCurrentTime());
        processInstanceExecution.setStartUserId(authenticatedUserId);

        // Store in database
        insert(processInstanceExecution, false);

        if (initiatorVariableName != null) {
            processInstanceExecution.setVariable(initiatorVariableName, authenticatedUserId);
        }

        // Need to be after insert, cause we need the id
        processInstanceExecution.setProcessInstanceId(processInstanceExecution.getId());
        processInstanceExecution.setRootProcessInstanceId(processInstanceExecution.getId());
        
        if (engineConfiguration.getIdentityLinkInterceptor() != null) {
            engineConfiguration.getIdentityLinkInterceptor().handleCreateProcessInstance(processInstanceExecution);
        }

        // Fire events
        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, processInstanceExecution),
                    engineConfiguration.getEngineCfgKey());
        }

        return processInstanceExecution;
    }

    /**
     * Creates a new execution. properties processDefinition, processInstance and activity will be initialized.
     */
    @Override
    public ExecutionEntity createChildExecution(ExecutionEntity parentExecutionEntity) {
        ExecutionEntity childExecution = dataManager.create();
        inheritCommonProperties(parentExecutionEntity, childExecution);
        childExecution.setParent(parentExecutionEntity);
        childExecution.setProcessDefinitionId(parentExecutionEntity.getProcessDefinitionId());
        childExecution.setProcessDefinitionKey(parentExecutionEntity.getProcessDefinitionKey());
        childExecution.setProcessInstanceId(parentExecutionEntity.getProcessInstanceId() != null
                ? parentExecutionEntity.getProcessInstanceId() : parentExecutionEntity.getId());
        childExecution.setScope(false);

        // manage the bidirectional parent-child relation
        parentExecutionEntity.addChildExecution(childExecution);

        // Insert the child execution
        insert(childExecution, false);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Child execution {} created with parent {}", childExecution, parentExecutionEntity.getId());
        }

        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, childExecution),
                    engineConfiguration.getEngineCfgKey());
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, childExecution),
                    engineConfiguration.getEngineCfgKey());
        }

        return childExecution;
    }

    @Override
    public ExecutionEntity createSubprocessInstance(ProcessDefinition processDefinition, ExecutionEntity superExecutionEntity,
                                                    String businessKey, String activityId) {

        ExecutionEntity subProcessInstance = dataManager.create();
        inheritCommonProperties(superExecutionEntity, subProcessInstance);
        subProcessInstance.setProcessDefinitionId(processDefinition.getId());
        subProcessInstance.setProcessDefinitionKey(processDefinition.getKey());
        subProcessInstance.setProcessDefinitionName(processDefinition.getName());
        subProcessInstance.setSuperExecution(superExecutionEntity);
        subProcessInstance.setRootProcessInstanceId(superExecutionEntity.getRootProcessInstanceId());
        subProcessInstance.setScope(true); // process instance is always a scope for all child executions

        String authenticatedUserId = Authentication.getAuthenticatedUserId();

        subProcessInstance.setStartActivityId(activityId);
        subProcessInstance.setStartUserId(authenticatedUserId);
        subProcessInstance.setBusinessKey(businessKey);

        // Store in database
        insert(subProcessInstance, false);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Child execution {} created with super execution {}", subProcessInstance, superExecutionEntity.getId());
        }

        subProcessInstance.setProcessInstanceId(subProcessInstance.getId());
        superExecutionEntity.setSubProcessInstance(subProcessInstance);
        
        if (engineConfiguration.getIdentityLinkInterceptor() != null) {
            engineConfiguration.getIdentityLinkInterceptor().handleCreateSubProcessInstance(subProcessInstance, superExecutionEntity);
        }

        engineConfiguration.getProcessInstanceHelper().processAvailableEventSubProcesses(subProcessInstance,
            ProcessDefinitionUtil.getProcess(processDefinition.getId()),CommandContextUtil.getCommandContext());

        FlowableEventDispatcher flowableEventDispatcher = engineConfiguration.getEventDispatcher();
        if (flowableEventDispatcher != null && flowableEventDispatcher.isEnabled()) {
            flowableEventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, subProcessInstance),
                    engineConfiguration.getEngineCfgKey());
        }

        return subProcessInstance;
    }

    protected void inheritCommonProperties(ExecutionEntity parentExecutionEntity, ExecutionEntity childExecution) {

        // Inherits the 'count' feature from the parent.
        // If the parent was not 'counting', we can't make the child 'counting' again.
        if (parentExecutionEntity instanceof CountingExecutionEntity countingParentExecutionEntity) {
            ((CountingExecutionEntity) childExecution).setCountEnabled(countingParentExecutionEntity.isCountEnabled());
        }

        // inherit the stage instance id, if present
        childExecution.setPropagatedStageInstanceId(parentExecutionEntity.getPropagatedStageInstanceId());

        childExecution.setRootProcessInstanceId(parentExecutionEntity.getRootProcessInstanceId());
        childExecution.setActive(true);
        childExecution.setStartTime(getClock().getCurrentTime());

        if (parentExecutionEntity.getTenantId() != null) {
            childExecution.setTenantId(parentExecutionEntity.getTenantId());
        }

    }

    // UPDATE METHODS

    @Override
    public void updateExecutionTenantIdForDeployment(String deploymentId, String newTenantId) {
        dataManager.updateExecutionTenantIdForDeployment(deploymentId, newTenantId);
    }

    // DELETE METHODS

    @Override
    public void deleteProcessInstancesByProcessDefinition(String processDefinitionId, String deleteReason, boolean cascade) {
        List<String> processInstanceIds = dataManager.findProcessInstanceIdsByProcessDefinitionId(processDefinitionId);

        for (String processInstanceId : processInstanceIds) {
            deleteProcessInstanceCascade(findById(processInstanceId), ProcessInstanceState.CANCELLED, deleteReason, cascade, true);
        }

        if (cascade) {
            getHistoryManager().recordDeleteHistoricProcessInstancesByProcessDefinitionId(processDefinitionId);
        }
    }
    
    @Override
    public void deleteProcessInstance(String processInstanceId, String deleteReason, boolean cascade) {
        deleteProcessInstance(processInstanceId, deleteReason, cascade, false);
    }
    
    @Override
    public void deleteProcessInstance(String processInstanceId, String deleteReason, boolean cascade, boolean directDeleteInDatabase) {
        ExecutionEntity processInstanceExecution = findById(processInstanceId);

        if (engineConfiguration.getEndProcessInstanceInterceptor() != null) {
            engineConfiguration.getEndProcessInstanceInterceptor().beforeEndProcessInstance(processInstanceExecution, ProcessInstanceState.CANCELLED);
        }

        if (processInstanceExecution == null) {
            throw new FlowableObjectNotFoundException("No process instance found for id '" + processInstanceId + "'", ProcessInstance.class);
        }

        deleteProcessInstanceCascade(processInstanceExecution, ProcessInstanceState.CANCELLED, deleteReason, cascade, directDeleteInDatabase);
        
        // Special care needed for a process instance of a call activity: the parent process instance needs to be triggered for completion
        // This can't be added to the deleteProcessInstanceCascade method, as this will also trigger all child and/or multi-instance
        // process instances for child call activities, which shouldn't happen.
        if (processInstanceExecution.getSuperExecutionId() != null) {
            ExecutionEntity superExecution = processInstanceExecution.getSuperExecution();
            if (superExecution != null
                    && superExecution.getCurrentFlowElement() instanceof FlowNode flowNode
                    && flowNode.getBehavior() instanceof SubProcessActivityBehavior subProcessActivityBehavior) {
                try {
                    subProcessActivityBehavior.completing(superExecution, processInstanceExecution);
                    superExecution.setSubProcessInstance(null);
                    subProcessActivityBehavior.completed(superExecution);
                } catch (Exception e) {
                    throw new FlowableException("Could not complete parent process instance for call activity with process instance execution " 
                                + processInstanceExecution, e);
                }
            }
        }

        if (engineConfiguration.getEndProcessInstanceInterceptor() != null) {
            engineConfiguration.getEndProcessInstanceInterceptor().afterEndProcessInstance(processInstanceId, ProcessInstanceState.CANCELLED);
        }
    }

    protected void deleteProcessInstanceCascade(ExecutionEntity execution, String state, String deleteReason, boolean deleteHistory, boolean directDeleteInDatabase) {

        // fill default reason if none provided
        if (deleteReason == null) {
            deleteReason = DeleteReason.PROCESS_INSTANCE_DELETED;
        }
        getActivityInstanceEntityManager().deleteActivityInstancesByProcessInstanceId(execution.getId());

        List<ExecutionEntity> childExecutions = collectChildren(execution.getProcessInstance());
        for (ExecutionEntity subExecutionEntity : childExecutions) {
            // We are treating the deletion of a process instance similar to an interruption
            // The reason for that is that there might be variables that have to be persisted differently
            // i.e. nrOfCompletedInstances / nrOfActiveInstance or variable aggregations
            FlowElement subExecutionFlowElement = subExecutionEntity.getCurrentFlowElement();
            if (subExecutionFlowElement instanceof FlowNode) {
                Object behavior = ((FlowNode) subExecutionFlowElement).getBehavior();
                if (behavior instanceof InterruptibleActivityBehaviour) {
                    ((InterruptibleActivityBehaviour) behavior).interrupted(subExecutionEntity);
                }
            }
            if (subExecutionEntity.isMultiInstanceRoot()) {
                for (ExecutionEntity miExecutionEntity : subExecutionEntity.getExecutions()) {
                    if (miExecutionEntity.getSubProcessInstance() != null) {
                        deleteProcessInstanceCascade(miExecutionEntity.getSubProcessInstance(), state, deleteReason, deleteHistory, directDeleteInDatabase);

                        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                            FlowElement callActivityElement = miExecutionEntity.getCurrentFlowElement();
                            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createActivityCancelledEvent(callActivityElement.getId(),
                                    callActivityElement.getName(), miExecutionEntity.getId(), miExecutionEntity.getProcessInstanceId(),
                                    miExecutionEntity.getProcessDefinitionId(), "callActivity", deleteReason), engineConfiguration.getEngineCfgKey());
                        }
                    }
                }

            } else if (subExecutionEntity.getSubProcessInstance() != null) {
                deleteProcessInstanceCascade(subExecutionEntity.getSubProcessInstance(), state, deleteReason, deleteHistory, directDeleteInDatabase);

                if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                    FlowElement callActivityElement = subExecutionEntity.getCurrentFlowElement();
                    getEventDispatcher().dispatchEvent(FlowableEventBuilder.createActivityCancelledEvent(callActivityElement.getId(),
                            callActivityElement.getName(), subExecutionEntity.getId(), subExecutionEntity.getProcessInstanceId(),
                            subExecutionEntity.getProcessDefinitionId(), "callActivity", deleteReason), engineConfiguration.getEngineCfgKey());
                }
            }
        }

        TaskHelper.deleteTasksByProcessInstanceId(execution.getId(), deleteReason, deleteHistory);

        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createCancelledEvent(execution.getProcessInstanceId(),
                    execution.getProcessInstanceId(), execution.getProcessDefinitionId(), deleteReason), engineConfiguration.getEngineCfgKey());
        }

        // delete the execution BEFORE we delete the history, otherwise we will
        // produce orphan HistoricVariableInstance instances

        ExecutionEntity processInstanceExecutionEntity = execution.getProcessInstance();
        if (processInstanceExecutionEntity == null) {
            return;
        }

        for (int i = childExecutions.size() - 1; i >= 0; i--) {
            ExecutionEntity childExecutionEntity = childExecutions.get(i);
            deleteExecutionAndRelatedData(childExecutionEntity, deleteReason, deleteHistory, directDeleteInDatabase);
        }

        deleteExecutionAndRelatedData(execution, deleteReason, deleteHistory, directDeleteInDatabase, true, null);

        if (deleteHistory) {
            getHistoryManager().recordProcessInstanceDeleted(execution.getId(), execution.getProcessDefinitionId(), execution.getTenantId());
        }

        getHistoryManager().recordProcessInstanceEnd(processInstanceExecutionEntity, state, deleteReason, null, getClock().getCurrentTime());
        processInstanceExecutionEntity.setDeleted(true);
    }

    @Override
    public void deleteExecutionAndRelatedData(ExecutionEntity executionEntity, String deleteReason, boolean deleteHistory, 
            boolean directDeleteInDatabase, boolean cancel, FlowElement cancelActivity) {
        
        if (!deleteHistory && executionEntity.isActive()
                && executionEntity.getCurrentFlowElement() != null
                && !executionEntity.isMultiInstanceRoot()) {
            
            CommandContextUtil.getActivityInstanceEntityManager().recordActivityEnd(executionEntity, deleteReason);
        }
        
        deleteRelatedDataForExecution(executionEntity, deleteReason, directDeleteInDatabase);
        delete(executionEntity);

        if (cancel && !executionEntity.isProcessInstanceType()) {
            dispatchActivityCancelled(executionEntity, cancelActivity != null ? cancelActivity : executionEntity.getCurrentFlowElement());
        }
        
        if (executionEntity.isProcessInstanceType() && executionEntity.getCallbackId() != null) {
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessInstanceHelper(commandContext);
            if (cancel) {
                processInstanceHelper.callCaseInstanceStateChangeCallbacks(commandContext, executionEntity,
                        ProcessInstanceState.RUNNING, ProcessInstanceState.CANCELLED);
            } else {
                processInstanceHelper.callCaseInstanceStateChangeCallbacks(commandContext, executionEntity,
                        ProcessInstanceState.RUNNING, ProcessInstanceState.COMPLETED);
            }
        }
    }
    
    @Override
    public void deleteExecutionAndRelatedData(ExecutionEntity executionEntity, String deleteReason, boolean deleteHistory) {
        deleteExecutionAndRelatedData(executionEntity, deleteReason, deleteHistory, false, false, null);
    }

    @Override
    public void deleteExecutionAndRelatedData(ExecutionEntity executionEntity, String deleteReason, boolean deleteHistory, boolean directDeleteInDatabase) {
        deleteExecutionAndRelatedData(executionEntity, deleteReason, deleteHistory, directDeleteInDatabase, false, null);
    }

    @Override
    public void deleteProcessInstanceExecutionEntity(String processInstanceId, String currentFlowElementId, String deleteReason,
            boolean cascade, boolean cancel, boolean fireEvents) {

        ExecutionEntity processInstanceEntity = findById(processInstanceId);
        String state = cancel ? ProcessInstanceState.CANCELLED : ProcessInstanceState.COMPLETED;

        if (processInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No process instance found for id '" + processInstanceId + "'", ProcessInstance.class);
        }

        if (processInstanceEntity.isDeleted()) {
            return;
        }

        // Call activities
        for (ExecutionEntity subExecutionEntity : processInstanceEntity.getExecutions()) {
            if (subExecutionEntity.getSubProcessInstance() != null && !subExecutionEntity.isEnded()) {
                deleteProcessInstanceCascade(subExecutionEntity.getSubProcessInstance(), state, deleteReason, cascade, false);

                if (getEventDispatcher() != null && getEventDispatcher().isEnabled() && fireEvents) {
                    FlowElement callActivityElement = subExecutionEntity.getCurrentFlowElement();
                    getEventDispatcher().dispatchEvent(FlowableEventBuilder.createActivityCancelledEvent(callActivityElement.getId(),
                            callActivityElement.getName(), subExecutionEntity.getId(), processInstanceId, subExecutionEntity.getProcessDefinitionId(),
                            "callActivity", deleteReason), engineConfiguration.getEngineCfgKey());
                }
            }
        }

        // delete event scope executions
        for (ExecutionEntity childExecution : processInstanceEntity.getExecutions()) {
            if (childExecution.isEventScope()) {
                deleteExecutionAndRelatedData(childExecution, null, cascade, false);
            }
        }

        deleteChildExecutions(processInstanceEntity, deleteReason, cancel);
        deleteExecutionAndRelatedData(processInstanceEntity, deleteReason, cascade, false);

        if (getEventDispatcher() != null && getEventDispatcher().isEnabled() && fireEvents) {
            if (!cancel) {
                getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_COMPLETED, processInstanceEntity),
                        engineConfiguration.getEngineCfgKey());
            } else {
                getEventDispatcher().dispatchEvent(FlowableEventBuilder.createCancelledEvent(processInstanceEntity.getId(),
                        processInstanceEntity.getId(), processInstanceEntity.getProcessDefinitionId(), deleteReason),
                        engineConfiguration.getEngineCfgKey());
            }
        }
        
        if (engineConfiguration.isLoggingSessionEnabled()) {
            BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_PROCESS_COMPLETED, "Completed process instance with id " + processInstanceEntity.getId(), processInstanceEntity);
        }

        getHistoryManager().recordProcessInstanceEnd(processInstanceEntity, state, deleteReason, currentFlowElementId, getClock().getCurrentTime());
        processInstanceEntity.setDeleted(true);
    }

    @Override
    public void deleteChildExecutions(ExecutionEntity executionEntity, String deleteReason, boolean cancel) {
        deleteChildExecutions(executionEntity, null, null, deleteReason, cancel, null);
    }

    @Override
    public void deleteChildExecutions(ExecutionEntity executionEntity, Collection<String> executionIdsNotToDelete,
            Collection<String> executionIdsNotToSendCancelledEventFor, String deleteReason, 
            boolean cancel, FlowElement cancelActivity) {

        // The children of an execution for a tree. For correct deletions (taking care of foreign keys between child-parent)
        // the leafs of this tree must be deleted first before the parents elements.

        List<ExecutionEntity> childExecutions = collectChildren(executionEntity, executionIdsNotToDelete);
        for (int i = childExecutions.size() - 1; i >= 0; i--) {
            ExecutionEntity childExecutionEntity = childExecutions.get(i);
            if (!childExecutionEntity.isEnded()) {
                if (executionIdsNotToDelete == null || !executionIdsNotToDelete.contains(childExecutionEntity.getId())) {

                    if (childExecutionEntity.isProcessInstanceType()) {
                        deleteProcessInstanceExecutionEntity(childExecutionEntity.getId(),
                                cancelActivity != null ? cancelActivity.getId() : null, deleteReason, true, cancel, true);

                    } else {
                        if (cancel && (childExecutionEntity.isActive() || childExecutionEntity.isMultiInstanceRoot()) 
                                && (executionIdsNotToSendCancelledEventFor == null || !executionIdsNotToSendCancelledEventFor.contains(childExecutionEntity.getId()))) {
                            dispatchExecutionCancelled(childExecutionEntity,
                                    cancelActivity != null ? cancelActivity : childExecutionEntity.getCurrentFlowElement());
                        }
                    }
                    deleteExecutionAndRelatedData(childExecutionEntity, deleteReason, false, false);
                }

            }
        }
    }

    @Override
    public List<ExecutionEntity> collectChildren(ExecutionEntity executionEntity) {
        return collectChildren(executionEntity, null);
    }

    protected List<ExecutionEntity> collectChildren(ExecutionEntity executionEntity, Collection<String> executionIdsToExclude) {
        List<ExecutionEntity> childExecutions = new ArrayList<>();
        collectChildren(executionEntity, childExecutions, executionIdsToExclude != null ? executionIdsToExclude : Collections.emptyList());
        return childExecutions;
    }

    @SuppressWarnings("unchecked")
    protected void collectChildren(ExecutionEntity executionEntity, List<ExecutionEntity> collectedChildExecution, Collection<String> executionIdsToExclude) {
        List<ExecutionEntity> childExecutions = (List<ExecutionEntity>) executionEntity.getExecutions();
        if (childExecutions != null && childExecutions.size() > 0) {

            // Have a fixed ordering of child executions (important for the order in which events are sent)
            childExecutions.sort(ExecutionEntity.EXECUTION_ENTITY_START_TIME_ASC_COMPARATOR);

            for (ExecutionEntity childExecution : childExecutions) {
                if (!executionIdsToExclude.contains(childExecution.getId())) {
                    if (!childExecution.isDeleted()) {
                        collectedChildExecution.add(childExecution);
                    }

                    collectChildren(childExecution, collectedChildExecution, executionIdsToExclude);
                }
            }
        }

        ExecutionEntity subProcessInstance = executionEntity.getSubProcessInstance();
        if (subProcessInstance != null && !executionIdsToExclude.contains(subProcessInstance.getId())) {
            if (!subProcessInstance.isDeleted()) {
                collectedChildExecution.add(subProcessInstance);
            }

            collectChildren(subProcessInstance, collectedChildExecution, executionIdsToExclude);
        }
    }

    protected void dispatchExecutionCancelled(ExecutionEntity execution, FlowElement cancelActivity) {

        ExecutionEntityManager executionEntityManager = engineConfiguration.getExecutionEntityManager();

        // subprocesses
        for (ExecutionEntity subExecution : executionEntityManager.findChildExecutionsByParentExecutionId(execution.getId())) {
            dispatchExecutionCancelled(subExecution, cancelActivity);
        }

        // call activities
        ExecutionEntity subProcessInstance = engineConfiguration.getExecutionEntityManager().findSubProcessInstanceBySuperExecutionId(execution.getId());
        if (subProcessInstance != null) {
            dispatchExecutionCancelled(subProcessInstance, cancelActivity);
        }

        // activity with message/signal boundary events
        FlowElement currentFlowElement = execution.getCurrentFlowElement();
        if (currentFlowElement instanceof FlowNode) {

            if (execution.isMultiInstanceRoot()) {
                dispatchMultiInstanceActivityCancelled(execution, cancelActivity);
            }
            else {
                dispatchActivityCancelled(execution, cancelActivity);
            }
        }
    }

    protected void dispatchActivityCancelled(ExecutionEntity execution, FlowElement cancelActivity) {
        FlowableEventDispatcher eventDispatcher = engineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
              eventDispatcher.dispatchEvent(
                      FlowableEventBuilder.createActivityCancelledEvent(execution.getCurrentFlowElement().getId(),
                              execution.getCurrentFlowElement().getName(), execution.getId(), execution.getProcessInstanceId(),
                              execution.getProcessDefinitionId(), getActivityType(execution.getCurrentFlowElement()), cancelActivity),
                      engineConfiguration.getEngineCfgKey());
        }


    }

    protected void dispatchMultiInstanceActivityCancelled(ExecutionEntity execution, FlowElement cancelActivity) {
        FlowableEventDispatcher eventDispatcher = engineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(
                FlowableEventBuilder.createMultiInstanceActivityCancelledEvent(execution.getCurrentFlowElement().getId(),
                    execution.getCurrentFlowElement().getName(), execution.getId(), execution.getProcessInstanceId(),
                    execution.getProcessDefinitionId(), getActivityType(execution.getCurrentFlowElement()), cancelActivity),
                engineConfiguration.getEngineCfgKey());
        }
    }

    protected String getActivityType(FlowElement flowNode) {
        String elementType = flowNode.getClass().getSimpleName();
        elementType = elementType.substring(0, 1).toLowerCase() + elementType.substring(1);
        return elementType;
    }

    @Override
    public ExecutionEntity findFirstScope(ExecutionEntity executionEntity) {
        ExecutionEntity currentExecutionEntity = executionEntity;
        while (currentExecutionEntity != null) {
            if (currentExecutionEntity.isScope()) {
                return currentExecutionEntity;
            }

            ExecutionEntity parentExecutionEntity = currentExecutionEntity.getParent();
            if (parentExecutionEntity == null) {
                parentExecutionEntity = currentExecutionEntity.getSuperExecution();
            }
            currentExecutionEntity = parentExecutionEntity;
        }
        return null;
    }

    @Override
    public ExecutionEntity findFirstMultiInstanceRoot(ExecutionEntity executionEntity) {
        ExecutionEntity currentExecutionEntity = executionEntity;
        while (currentExecutionEntity != null) {
            if (currentExecutionEntity.isMultiInstanceRoot()) {
                return currentExecutionEntity;
            }

            ExecutionEntity parentExecutionEntity = currentExecutionEntity.getParent();
            if (parentExecutionEntity == null) {
                parentExecutionEntity = currentExecutionEntity.getSuperExecution();
            }
            currentExecutionEntity = parentExecutionEntity;
        }
        return null;
    }
    
    protected CachedEntityMatcher<IdentityLinkEntity> identityLinkByProcessInstanceMatcher = new IdentityLinksByProcessInstanceMatcher();
    
    @Override
    public void deleteRelatedDataForExecution(ExecutionEntity executionEntity, String deleteReason, boolean directDeleteInDatabase) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ending and deleting execution {} Reason: {}", executionEntity, deleteReason);
        }
        // To start, deactivate the current incoming execution
        executionEntity.setEnded(true);
        executionEntity.setActive(false);
        
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        boolean enableExecutionRelationshipCounts = CountingEntityUtil.isExecutionRelatedEntityCountEnabled(executionEntity);
        
        // If event dispatching is disabled, related entities can be deleted in bulk. Otherwise, they need to be fetched
        // and events need to be sent for each of them separately (the bulk delete still happens).
        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher();
        boolean eventDispatcherEnabled = eventDispatcher != null && eventDispatcher.isEnabled();
        
        deleteIdentityLinks(executionEntity, commandContext, eventDispatcherEnabled);
        deleteEntityLinks(executionEntity, commandContext, eventDispatcherEnabled);
        deleteVariables(executionEntity, commandContext, enableExecutionRelationshipCounts, eventDispatcherEnabled);
        deleteUserTasks(executionEntity, deleteReason, commandContext, enableExecutionRelationshipCounts, eventDispatcherEnabled);
        deleteJobs(executionEntity, commandContext, enableExecutionRelationshipCounts, eventDispatcherEnabled);
        deleteEventSubScriptions(executionEntity, enableExecutionRelationshipCounts, eventDispatcherEnabled);
        deleteActivityInstances(executionEntity, commandContext);
        deleteSubCases(executionEntity, directDeleteInDatabase, commandContext);
    }

    protected void deleteSubCases(ExecutionEntity executionEntity, boolean directDeleteInDatabase, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        CaseInstanceService caseInstanceService = processEngineConfiguration.getCaseInstanceService();
        if (caseInstanceService != null) {
            if (executionEntity.getReferenceId() != null && ReferenceTypes.EXECUTION_CHILD_CASE.equals(executionEntity.getReferenceType())) {
                if (directDeleteInDatabase) {
                    caseInstanceService.deleteCaseInstanceWithoutAgenda(executionEntity.getReferenceId());
                } else {
                    caseInstanceService.deleteCaseInstance(executionEntity.getReferenceId());
                }
                
            } else if (executionEntity.getCurrentFlowElement() instanceof CaseServiceTask) {
                // backwards compatibility in case there is no reference
                // (cases created before the double reference in the execution) was added
                caseInstanceService.deleteCaseInstancesForExecutionId(executionEntity.getId());
            }
        }
    }

    protected void deleteActivityInstances(ExecutionEntity executionEntity, CommandContext commandContext) {
        if (executionEntity.isProcessInstanceType()) {
            engineConfiguration.getActivityInstanceEntityManager().deleteActivityInstancesByProcessInstanceId(executionEntity.getId());
        }
    }

    protected void deleteIdentityLinks(ExecutionEntity executionEntity, CommandContext commandContext, boolean eventDispatcherEnabled) {
        if (executionEntity.isProcessInstanceType()) {
            IdentityLinkService identityLinkService = engineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService();
            boolean deleteIdentityLinks = true;
            if (eventDispatcherEnabled) {
                Collection<IdentityLinkEntity> identityLinks = identityLinkService.findIdentityLinksByProcessInstanceId(executionEntity.getId());
                for (IdentityLinkEntity identityLink : identityLinks) {
                    fireEntityDeletedEvent(identityLink);
                }
                deleteIdentityLinks = !identityLinks.isEmpty();
            }
            
            if (deleteIdentityLinks) {
                identityLinkService.deleteIdentityLinksByProcessInstanceId(executionEntity.getId());
            }
        }
    }
    
    protected void deleteEntityLinks(ExecutionEntity executionEntity, CommandContext commandContext, boolean eventDispatcherEnabled) {
        // Entity links are deleted by a root instance only.
        // (A callback id is set for a child process instance of a case instance.
        // A super execution id is set for a child process instance of a process instance)
        // Can't simply check for callBackId being null however, as other usages of callbackType still need to be cleaned up
        if (engineConfiguration.isEnableEntityLinks() && executionEntity.isProcessInstanceType() && isRootProcessInstance(executionEntity)) {

            EntityLinkService entityLinkService = engineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService();
            boolean deleteEntityLinks = true;
            if (eventDispatcherEnabled) {
                List<EntityLink> entityLinks = entityLinkService.findEntityLinksByRootScopeIdAndRootType(executionEntity.getId(), ScopeTypes.BPMN);
                for (EntityLink entityLink : entityLinks) {
                    fireEntityDeletedEvent((EntityLinkEntity) entityLink);
                }
                deleteEntityLinks = !entityLinks.isEmpty();
            }
            
            if (deleteEntityLinks) {
                entityLinkService.deleteEntityLinksByRootScopeIdAndType(executionEntity.getId(), ScopeTypes.BPMN);
            }
        }

    }

    protected boolean isRootProcessInstance(ExecutionEntity executionEntity) {
        // An execution is a root process instance when it doesn't have a super execution and,
        // it has no callback or it is not a child of a case instance
        return executionEntity.getSuperExecutionId() == null
            && (executionEntity.getCallbackId() == null || !CallbackTypes.PLAN_ITEM_CHILD_PROCESS.equals(executionEntity.getCallbackType()));
    }

    protected void deleteVariables(ExecutionEntity executionEntity, CommandContext commandContext, boolean enableExecutionRelationshipCounts, boolean eventDispatcherEnabled) {
        if (!enableExecutionRelationshipCounts ||
                (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getVariableCount() > 0)) {
            
            Collection<VariableInstance> executionVariables = executionEntity.getVariableInstancesLocal().values();
            if (!executionVariables.isEmpty()) {
                
                List<ByteArrayRef> variableByteArrayRefs = new ArrayList<>();
                for (VariableInstance variableInstance : executionVariables) {
                    if (variableInstance instanceof VariableInstanceEntity variableInstanceEntity) {

                        if (variableInstanceEntity.getByteArrayRef() != null && variableInstanceEntity.getByteArrayRef().getId() != null) {
                            variableByteArrayRefs.add(variableInstanceEntity.getByteArrayRef());
                        }
                        
                        if (eventDispatcherEnabled) {
                            FlowableEventDispatcher eventDispatcher = engineConfiguration.getEventDispatcher();
                            if (eventDispatcher != null) {
                                eventDispatcher.dispatchEvent(EventUtil.createVariableDeleteEvent(variableInstanceEntity),
                                        engineConfiguration.getEngineCfgKey());
                                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, variableInstance),
                                        engineConfiguration.getEngineCfgKey());
                            }
                        }
                    }
                }
                
                // First byte arrays that reference variable, then variables in bulk
                for (ByteArrayRef variableByteArrayRef : variableByteArrayRefs) {
                    getByteArrayEntityManager().deleteByteArrayById(variableByteArrayRef.getId());
                }
                
                engineConfiguration.getVariableServiceConfiguration().getVariableService().deleteVariablesByExecutionId(executionEntity.getId());
            }

            List<VariableInstanceEntity> variableInstances = engineConfiguration.getVariableServiceConfiguration().getVariableService()
                    .createInternalVariableInstanceQuery()
                    .subScopeId(executionEntity.getId())
                    .scopeTypes(engineConfiguration.getDependentScopeTypes())
                    .list();
            boolean deleteVariableInstances = !variableInstances.isEmpty();

            for (VariableInstanceEntity variableInstance : variableInstances) {
                if (eventDispatcherEnabled) {
                    FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher(commandContext);
                    if (eventDispatcher != null) {
                        eventDispatcher.dispatchEvent(EventUtil.createVariableDeleteEvent(variableInstance),
                                engineConfiguration.getEngineCfgKey());
                        eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, variableInstance),
                                engineConfiguration.getEngineCfgKey());
                    }
                }

                if (variableInstance.getByteArrayRef() != null && variableInstance.getByteArrayRef().getId() != null) {
                    variableInstance.getByteArrayRef().delete(engineConfiguration.getEngineCfgKey());
                }
            }

            if (deleteVariableInstances) {
                engineConfiguration.getVariableServiceConfiguration().getVariableInstanceEntityManager()
                        .deleteBySubScopeIdAndScopeTypes(executionEntity.getId(), engineConfiguration.getDependentScopeTypes());
            }

        }

    }

    protected void deleteUserTasks(ExecutionEntity executionEntity, String deleteReason, CommandContext commandContext, 
            boolean enableExecutionRelationshipCounts, boolean eventDispatcherEnabled) {
        if (!enableExecutionRelationshipCounts ||
                (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getTaskCount() > 0)) {
            TaskHelper.deleteTasksForExecution(executionEntity, 
                    engineConfiguration.getTaskServiceConfiguration().getTaskService().findTasksByExecutionId(executionEntity.getId()), deleteReason);
        }
    }
    
    protected void deleteJobs(ExecutionEntity executionEntity, CommandContext commandContext, boolean enableExecutionRelationshipCounts, boolean eventDispatcherEnabled) {
        
        // Jobs have byte array references that don't store the execution id. 
        // This means a bulk delete is not done for jobs. Generally there aren't many jobs / execution either.
        
        if (!enableExecutionRelationshipCounts
                || (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getTimerJobCount() > 0)) {
            engineConfiguration.getJobServiceConfiguration().getTimerJobService().deleteTimerJobsByExecutionId(executionEntity.getId());
        }

        JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
        if (!enableExecutionRelationshipCounts
                || (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getJobCount() > 0)) {
            jobService.deleteJobsByExecutionId(executionEntity.getId());
        }

        if (!enableExecutionRelationshipCounts
                || (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getSuspendedJobCount() > 0)) {
            jobService.deleteSuspendedJobsByExecutionId(executionEntity.getId());
        }

        if (!enableExecutionRelationshipCounts
                || (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getDeadLetterJobCount() > 0)) {
            jobService.deleteDeadLetterJobsByExecutionId(executionEntity.getId());
        }

        if (!enableExecutionRelationshipCounts || ((CountingExecutionEntity) executionEntity).getExternalWorkerJobCount() > 0) {
            Collection<ExternalWorkerJobEntity> externalWorkerJobsForExecution = jobService.findExternalWorkerJobsByExecutionId(executionEntity.getId());

            ExternalWorkerJobEntityManager externalWorkerJobEntityManager = engineConfiguration.getJobServiceConfiguration()
                    .getExternalWorkerJobEntityManager();
            IdentityLinkService identityLinkService = engineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService();
            for (ExternalWorkerJobEntity job : externalWorkerJobsForExecution) {
                externalWorkerJobEntityManager.delete(job);
                identityLinkService.deleteIdentityLinksByScopeIdAndType(job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER);
                if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                    getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, job),
                            engineConfiguration.getEngineCfgKey());
                }
            }
        }
    }

    protected void deleteEventSubScriptions(ExecutionEntity executionEntity, boolean enableExecutionRelationshipCounts, boolean eventDispatcherEnabled) {
        if (!enableExecutionRelationshipCounts
                || (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getEventSubscriptionCount() > 0)) {
            
            EventSubscriptionService eventSubscriptionService = engineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            
            boolean deleteEventSubscriptions = true;
            if (eventDispatcherEnabled) {
                List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsByExecution(executionEntity.getId());
                for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                    
                    fireEntityDeletedEvent(eventSubscription);
                    if (MessageEventSubscriptionEntity.EVENT_TYPE.equals(eventSubscription.getEventType())) {
                        getEventDispatcher().dispatchEvent(FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED,
                                eventSubscription.getActivityId(), eventSubscription.getEventName(), null, eventSubscription.getExecutionId(),
                                eventSubscription.getProcessInstanceId(), eventSubscription.getProcessDefinitionId()),
                                engineConfiguration.getEngineCfgKey());
                    }
                }
                
                deleteEventSubscriptions = !eventSubscriptions.isEmpty();
            }
            
            if (deleteEventSubscriptions) {
                eventSubscriptionService.deleteEventSubscriptionsByExecutionId(executionEntity.getId());
            }
        }
    }

    // OTHER METHODS

    @Override
    public void updateProcessInstanceLockTime(String processInstanceId, String lockOwner, Date lockTime) {
        Date expirationTime = getClock().getCurrentTime();

        dataManager.updateProcessInstanceLockTime(processInstanceId, lockTime, lockOwner, expirationTime);
    }

    @Override
    public void clearProcessInstanceLockTime(String processInstanceId) {
        dataManager.clearProcessInstanceLockTime(processInstanceId);
    }

    @Override
    public void clearAllProcessInstanceLockTimes(String lockOwner) {
        dataManager.clearAllProcessInstanceLockTimes(lockOwner);
    }
    
    @Override
    public String updateProcessInstanceBusinessKey(ExecutionEntity executionEntity, String businessKey) {
        if (executionEntity.isProcessInstanceType() && businessKey != null) {
            executionEntity.setBusinessKey(businessKey);
            getHistoryManager().updateProcessBusinessKeyInHistory(executionEntity);

            if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, executionEntity),
                        engineConfiguration.getEngineCfgKey());
            }

            return businessKey;
        }
        return null;
    }
    
    @Override
    public String updateProcessInstanceBusinessStatus(ExecutionEntity executionEntity, String businessStatus) {
        if (executionEntity.isProcessInstanceType() && businessStatus != null) {
            executionEntity.setBusinessStatus(businessStatus);
            getHistoryManager().updateProcessBusinessStatusInHistory(executionEntity);

            if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, executionEntity),
                        engineConfiguration.getEngineCfgKey());
            }

            return businessStatus;
        }
        return null;
    }

    protected HistoryManager getHistoryManager() {
        return engineConfiguration.getHistoryManager();
    }

    protected AsyncExecutor getAsyncExecutor() {
        return engineConfiguration.getAsyncExecutor();
    }

    protected ByteArrayEntityManager getByteArrayEntityManager() {
        return engineConfiguration.getByteArrayEntityManager();
    }

    protected ActivityInstanceEntityManager getActivityInstanceEntityManager() {
        return engineConfiguration.getActivityInstanceEntityManager();
    }

}
