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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.EventUtil;
import org.flowable.engine.impl.util.IdentityLinkUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.IdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher.IdentityLinksByProcessInstanceMatcher;
import org.flowable.job.service.JobService;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayRef;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ExecutionEntityManagerImpl extends AbstractEntityManager<ExecutionEntity> implements ExecutionEntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionEntityManagerImpl.class);

    protected ExecutionDataManager executionDataManager;

    public ExecutionEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ExecutionDataManager executionDataManager) {
        super(processEngineConfiguration);
        this.executionDataManager = executionDataManager;
    }

    @Override
    protected DataManager<ExecutionEntity> getDataManager() {
        return executionDataManager;
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
        return executionDataManager.findSubProcessInstanceBySuperExecutionId(superExecutionId);
    }

    @Override
    public List<ExecutionEntity> findChildExecutionsByParentExecutionId(String parentExecutionId) {
        return executionDataManager.findChildExecutionsByParentExecutionId(parentExecutionId);
    }

    @Override
    public List<ExecutionEntity> findChildExecutionsByProcessInstanceId(String processInstanceId) {
        return executionDataManager.findChildExecutionsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(final String parentExecutionId, final Collection<String> activityIds) {
        return executionDataManager.findExecutionsByParentExecutionAndActivityIds(parentExecutionId, activityIds);
    }

    @Override
    public long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery) {
        return executionDataManager.findExecutionCountByQueryCriteria(executionQuery);
    }

    @Override
    public List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery) {
        return executionDataManager.findExecutionsByQueryCriteria(executionQuery);
    }

    @Override
    public long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return executionDataManager.findProcessInstanceCountByQueryCriteria(executionQuery);
    }

    @Override
    public List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return executionDataManager.findProcessInstanceByQueryCriteria(executionQuery);
    }

    @Override
    public ExecutionEntity findByRootProcessInstanceId(String rootProcessInstanceId) {
        List<ExecutionEntity> executions = executionDataManager.findExecutionsByRootProcessInstanceId(rootProcessInstanceId);
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
        return executionDataManager.findProcessInstanceAndVariablesByQueryCriteria(executionQuery);
    }

    @Override
    public Collection<ExecutionEntity> findInactiveExecutionsByProcessInstanceId(final String processInstanceId) {
        return executionDataManager.findInactiveExecutionsByProcessInstanceId(processInstanceId);
    }

    @Override
    public Collection<ExecutionEntity> findInactiveExecutionsByActivityIdAndProcessInstanceId(final String activityId, final String processInstanceId) {
        return executionDataManager.findInactiveExecutionsByActivityIdAndProcessInstanceId(activityId, processInstanceId);
    }

    @Override
    public List<Execution> findExecutionsByNativeQuery(Map<String, Object> parameterMap) {
        return executionDataManager.findExecutionsByNativeQuery(parameterMap);
    }

    @Override
    public List<ProcessInstance> findProcessInstanceByNativeQuery(Map<String, Object> parameterMap) {
        return executionDataManager.findProcessInstanceByNativeQuery(parameterMap);
    }

    @Override
    public long findExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
        return executionDataManager.findExecutionCountByNativeQuery(parameterMap);
    }

    // CREATE METHODS

    @Override
    public ExecutionEntity createProcessInstanceExecution(ProcessDefinition processDefinition, String predefinedProcessInstanceId, 
                    String businessKey, String processInstanceName, String callbackId, String callbackType,
                    String tenantId, String initiatorVariableName, String startActivityId) {

        ExecutionEntity processInstanceExecution = executionDataManager.create();

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
        processInstanceExecution.setDeploymentId(processDefinition.getDeploymentId());
        processInstanceExecution.setBusinessKey(businessKey);
        processInstanceExecution.setName(processInstanceName);
        
        // Callbacks
        if (callbackId != null) {
            processInstanceExecution.setCallbackId(callbackId);
        }
        if (callbackType != null) {
            processInstanceExecution.setCallbackType(callbackType);
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
        if (authenticatedUserId != null) {
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstanceExecution, authenticatedUserId, null, IdentityLinkType.STARTER);
        }

        // Fire events
        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, processInstanceExecution));
        }

        return processInstanceExecution;
    }

    /**
     * Creates a new execution. properties processDefinition, processInstance and activity will be initialized.
     */
    @Override
    public ExecutionEntity createChildExecution(ExecutionEntity parentExecutionEntity) {
        ExecutionEntity childExecution = executionDataManager.create();
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
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, childExecution));
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, childExecution));
        }

        return childExecution;
    }

    @Override
    public ExecutionEntity createSubprocessInstance(ProcessDefinition processDefinition, ExecutionEntity superExecutionEntity,
                                                    String businessKey, String activityId) {

        ExecutionEntity subProcessInstance = executionDataManager.create();
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

        if (authenticatedUserId != null) {
            IdentityLinkUtil.createProcessInstanceIdentityLink(subProcessInstance, authenticatedUserId, null, IdentityLinkType.STARTER);
        }

        FlowableEventDispatcher flowableEventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (flowableEventDispatcher != null && flowableEventDispatcher.isEnabled()) {
            flowableEventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, subProcessInstance));
        }

        return subProcessInstance;
    }

    protected void inheritCommonProperties(ExecutionEntity parentExecutionEntity, ExecutionEntity childExecution) {

        // Inherits the 'count' feature from the parent.
        // If the parent was not 'counting', we can't make the child 'counting' again.
        if (parentExecutionEntity instanceof CountingExecutionEntity) {
            CountingExecutionEntity countingParentExecutionEntity = (CountingExecutionEntity) parentExecutionEntity;
            ((CountingExecutionEntity) childExecution).setCountEnabled(countingParentExecutionEntity.isCountEnabled());
        }

        childExecution.setRootProcessInstanceId(parentExecutionEntity.getRootProcessInstanceId());
        childExecution.setActive(true);
        childExecution.setStartTime(processEngineConfiguration.getClock().getCurrentTime());

        if (parentExecutionEntity.getTenantId() != null) {
            childExecution.setTenantId(parentExecutionEntity.getTenantId());
        }

    }

    // UPDATE METHODS

    @Override
    public void updateExecutionTenantIdForDeployment(String deploymentId, String newTenantId) {
        executionDataManager.updateExecutionTenantIdForDeployment(deploymentId, newTenantId);
    }

    // DELETE METHODS

    @Override
    public void deleteProcessInstancesByProcessDefinition(String processDefinitionId, String deleteReason, boolean cascade) {
        List<String> processInstanceIds = executionDataManager.findProcessInstanceIdsByProcessDefinitionId(processDefinitionId);

        for (String processInstanceId : processInstanceIds) {
            deleteProcessInstanceCascade(findById(processInstanceId), deleteReason, cascade);
        }

        if (cascade) {
            getHistoryManager().recordDeleteHistoricProcessInstancesByProcessDefinitionId(processDefinitionId);
        }
    }

    @Override
    public void deleteProcessInstance(String processInstanceId, String deleteReason, boolean cascade) {
        ExecutionEntity processInstanceExecution = findById(processInstanceId);

        if (processInstanceExecution == null) {
            throw new FlowableObjectNotFoundException("No process instance found for id '" + processInstanceId + "'", ProcessInstance.class);
        }

        deleteProcessInstanceCascade(processInstanceExecution, deleteReason, cascade);
        
        // Special care needed for a process instance of a call activity: the parent process instance needs to be triggered for completion
        // This can't be added to the deleteProcessInstanceCascade method, as this will also trigger all child and/or multi-instance
        // process instances for child call activities, which shouldn't happen.
        if (processInstanceExecution.getSuperExecutionId() != null) {
            ExecutionEntity superExecution = processInstanceExecution.getSuperExecution();
            if (superExecution != null
                    && superExecution.getCurrentFlowElement() instanceof FlowNode
                    && ((FlowNode) superExecution.getCurrentFlowElement()).getBehavior() instanceof SubProcessActivityBehavior) {
                SubProcessActivityBehavior subProcessActivityBehavior = (SubProcessActivityBehavior) ((FlowNode) superExecution.getCurrentFlowElement()).getBehavior();
                try {
                    subProcessActivityBehavior.completing(superExecution, processInstanceExecution);
                    superExecution.setSubProcessInstance(null);
                    subProcessActivityBehavior.completed(superExecution);
                } catch (Exception e) {
                    throw new FlowableException("Could not complete parent process instance for call activity with process instance execution " 
                                + processInstanceExecution.getId(), e);
                }
            }
        }
    }

    protected void deleteProcessInstanceCascade(ExecutionEntity execution, String deleteReason, boolean deleteHistory) {

        // fill default reason if none provided
        if (deleteReason == null) {
            deleteReason = DeleteReason.PROCESS_INSTANCE_DELETED;
        }
        getActivityInstanceEntityManager().deleteActivityInstancesByProcessInstanceId(execution.getId());

        for (ExecutionEntity subExecutionEntity : execution.getExecutions()) {
            if (subExecutionEntity.isMultiInstanceRoot()) {
                for (ExecutionEntity miExecutionEntity : subExecutionEntity.getExecutions()) {
                    if (miExecutionEntity.getSubProcessInstance() != null) {
                        deleteProcessInstanceCascade(miExecutionEntity.getSubProcessInstance(), deleteReason, deleteHistory);

                        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                            FlowElement callActivityElement = miExecutionEntity.getCurrentFlowElement();
                            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createActivityCancelledEvent(callActivityElement.getId(),
                                    callActivityElement.getName(), miExecutionEntity.getId(), miExecutionEntity.getProcessInstanceId(),
                                    miExecutionEntity.getProcessDefinitionId(), "callActivity", deleteReason));
                        }
                    }
                }

            } else if (subExecutionEntity.getSubProcessInstance() != null) {
                deleteProcessInstanceCascade(subExecutionEntity.getSubProcessInstance(), deleteReason, deleteHistory);

                if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                    FlowElement callActivityElement = subExecutionEntity.getCurrentFlowElement();
                    getEventDispatcher().dispatchEvent(FlowableEventBuilder.createActivityCancelledEvent(callActivityElement.getId(),
                            callActivityElement.getName(), subExecutionEntity.getId(), subExecutionEntity.getProcessInstanceId(),
                            subExecutionEntity.getProcessDefinitionId(), "callActivity", deleteReason));
                }
            }
        }

        TaskHelper.deleteTasksByProcessInstanceId(execution.getId(), deleteReason, deleteHistory);

        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createCancelledEvent(execution.getProcessInstanceId(),
                    execution.getProcessInstanceId(), execution.getProcessDefinitionId(), deleteReason));
        }

        // delete the execution BEFORE we delete the history, otherwise we will
        // produce orphan HistoricVariableInstance instances

        ExecutionEntity processInstanceExecutionEntity = execution.getProcessInstance();
        if (processInstanceExecutionEntity == null) {
            return;
        }

        List<ExecutionEntity> childExecutions = collectChildren(execution.getProcessInstance());
        for (int i = childExecutions.size() - 1; i >= 0; i--) {
            ExecutionEntity childExecutionEntity = childExecutions.get(i);
            deleteExecutionAndRelatedData(childExecutionEntity, deleteReason, deleteHistory);
        }

        deleteExecutionAndRelatedData(execution, deleteReason, deleteHistory);

        if (deleteHistory) {
            getHistoryManager().recordProcessInstanceDeleted(execution.getId(), execution.getProcessDefinitionId());
        }

        getHistoryManager().recordProcessInstanceEnd(processInstanceExecutionEntity, deleteReason, null, getClock().getCurrentTime());
        processInstanceExecutionEntity.setDeleted(true);
    }

    @Override
    public void deleteExecutionAndRelatedData(ExecutionEntity executionEntity, String deleteReason, boolean deleteHistory, boolean cancel, FlowElement cancelActivity) {
        if (!deleteHistory && executionEntity.isActive()
                && executionEntity.getCurrentFlowElement() != null
                && !executionEntity.isMultiInstanceRoot()
                && !(executionEntity.getCurrentFlowElement() instanceof BoundaryEvent)) {  // Boundary events will handle the history themselves (see TriggerExecutionOperation for example)
            
            CommandContextUtil.getActivityInstanceEntityManager().recordActivityEnd(executionEntity, deleteReason);
        }
        
        deleteRelatedDataForExecution(executionEntity, deleteReason);
        delete(executionEntity);

        if (cancel) {
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
        deleteExecutionAndRelatedData(executionEntity, deleteReason, deleteHistory, false, null);
    }

    @Override
    public void deleteProcessInstanceExecutionEntity(String processInstanceId,
                                                     String currentFlowElementId, 
                                                     String deleteReason,
                                                     boolean cascade, 
                                                     boolean cancel, 
                                                     boolean fireEvents) {

        ExecutionEntity processInstanceEntity = findById(processInstanceId);

        if (processInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No process instance found for id '" + processInstanceId + "'", ProcessInstance.class);
        }

        if (processInstanceEntity.isDeleted()) {
            return;
        }

        // Call activities
        for (ExecutionEntity subExecutionEntity : processInstanceEntity.getExecutions()) {
            if (subExecutionEntity.getSubProcessInstance() != null && !subExecutionEntity.isEnded()) {
                deleteProcessInstanceCascade(subExecutionEntity.getSubProcessInstance(), deleteReason, cascade);

                if (getEventDispatcher() != null && getEventDispatcher().isEnabled() && fireEvents) {
                    FlowElement callActivityElement = subExecutionEntity.getCurrentFlowElement();
                    getEventDispatcher().dispatchEvent(FlowableEventBuilder.createActivityCancelledEvent(callActivityElement.getId(),
                            callActivityElement.getName(), subExecutionEntity.getId(), processInstanceId, subExecutionEntity.getProcessDefinitionId(),
                            "callActivity", deleteReason));
                }
            }
        }

        // delete event scope executions
        for (ExecutionEntity childExecution : processInstanceEntity.getExecutions()) {
            if (childExecution.isEventScope()) {
                deleteExecutionAndRelatedData(childExecution, null, cascade);
            }
        }

        deleteChildExecutions(processInstanceEntity, deleteReason, cancel);
        deleteExecutionAndRelatedData(processInstanceEntity, deleteReason, cascade);

        if (getEventDispatcher() != null && getEventDispatcher().isEnabled() && fireEvents) {
            if (!cancel) {
                getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_COMPLETED, processInstanceEntity));
            } else {
                getEventDispatcher().dispatchEvent(FlowableEventBuilder.createCancelledEvent(processInstanceEntity.getId(),
                        processInstanceEntity.getId(), processInstanceEntity.getProcessDefinitionId(), deleteReason));
            }
        }

        getHistoryManager().recordProcessInstanceEnd(processInstanceEntity, deleteReason, currentFlowElementId, getClock().getCurrentTime());
        processInstanceEntity.setDeleted(true);
    }

    @Override
    public void deleteChildExecutions(ExecutionEntity executionEntity, String deleteReason, boolean cancel) {
        deleteChildExecutions(executionEntity, null, null, deleteReason, cancel, null);
    }

    @Override
    public void deleteChildExecutions(ExecutionEntity executionEntity, Collection<String> executionIdsNotToDelete,
            Collection<String> executionIdsNotToSendCancelledEventFor, String deleteReason, boolean cancel, FlowElement cancelActivity) {

        // The children of an execution for a tree. For correct deletions (taking care of foreign keys between child-parent)
        // the leafs of this tree must be deleted first before the parents elements.

        List<ExecutionEntity> childExecutions = collectChildren(executionEntity, executionIdsNotToDelete);
        for (int i = childExecutions.size() - 1; i >= 0; i--) {
            ExecutionEntity childExecutionEntity = childExecutions.get(i);
            if (!childExecutionEntity.isEnded()) {
                if (executionIdsNotToDelete == null || (executionIdsNotToDelete != null && !executionIdsNotToDelete.contains(childExecutionEntity.getId()))) {

                    if (childExecutionEntity.isProcessInstanceType()) {
                        deleteProcessInstanceExecutionEntity(childExecutionEntity.getId(),
                                cancelActivity != null ? cancelActivity.getId() : null, deleteReason, true, cancel, true);

                    } else {
                        if (cancel && (childExecutionEntity.isActive() || childExecutionEntity.isMultiInstanceRoot()) 
                                && (executionIdsNotToSendCancelledEventFor == null || !executionIdsNotToSendCancelledEventFor.contains(childExecutionEntity.getId())))
                            dispatchExecutionCancelled(childExecutionEntity, 
                                    cancelActivity != null ? cancelActivity : childExecutionEntity.getCurrentFlowElement());
                    }
                    deleteExecutionAndRelatedData(childExecutionEntity, deleteReason, false);
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
        collectChildren(executionEntity, childExecutions, executionIdsToExclude != null ? executionIdsToExclude : Collections.<String>emptyList());
        return childExecutions;
    }

    @SuppressWarnings("unchecked")
    protected void collectChildren(ExecutionEntity executionEntity, List<ExecutionEntity> collectedChildExecution, Collection<String> executionIdsToExclude) {
        List<ExecutionEntity> childExecutions = (List<ExecutionEntity>) executionEntity.getExecutions();
        if (childExecutions != null && childExecutions.size() > 0) {

            // Have a fixed ordering of child executions (important for the order in which events are sent)
            Collections.sort(childExecutions, ExecutionEntity.EXECUTION_ENTITY_START_TIME_ASC_COMPARATOR);

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

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();

        // subprocesses
        for (ExecutionEntity subExecution : executionEntityManager.findChildExecutionsByParentExecutionId(execution.getId())) {
            dispatchExecutionCancelled(subExecution, cancelActivity);
        }

        // call activities
        ExecutionEntity subProcessInstance = CommandContextUtil.getExecutionEntityManager().findSubProcessInstanceBySuperExecutionId(execution.getId());
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
        CommandContextUtil.getProcessEngineConfiguration()
                .getEventDispatcher()
                .dispatchEvent(
                        FlowableEventBuilder.createActivityCancelledEvent(execution.getCurrentFlowElement().getId(),
                                execution.getCurrentFlowElement().getName(), execution.getId(), execution.getProcessInstanceId(),
                                execution.getProcessDefinitionId(), getActivityType((FlowNode) execution.getCurrentFlowElement()), cancelActivity));
    }

    protected void dispatchMultiInstanceActivityCancelled(ExecutionEntity execution, FlowElement cancelActivity) {
        CommandContextUtil.getProcessEngineConfiguration()
                .getEventDispatcher()
                .dispatchEvent(
                        FlowableEventBuilder.createMultiInstanceActivityCancelledEvent(execution.getCurrentFlowElement().getId(),
                                execution.getCurrentFlowElement().getName(), execution.getId(), execution.getProcessInstanceId(),
                                execution.getProcessDefinitionId(), getActivityType((FlowNode) execution.getCurrentFlowElement()), cancelActivity));
    }

    protected String getActivityType(FlowNode flowNode) {
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
    public void deleteRelatedDataForExecution(ExecutionEntity executionEntity, String deleteReason) {

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
    }

    protected void deleteActivityInstances(ExecutionEntity executionEntity, CommandContext commandContext) {
        if (executionEntity.isProcessInstanceType()) {
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).deleteActivityInstancesByProcessInstanceId(executionEntity.getId());
        }
    }

    protected void deleteIdentityLinks(ExecutionEntity executionEntity, CommandContext commandContext, boolean eventDispatcherEnabled) {
        if (executionEntity.isProcessInstanceType()) {
            IdentityLinkService identityLinkService = CommandContextUtil.getIdentityLinkService(commandContext);
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
        if (processEngineConfiguration.isEnableEntityLinks() && executionEntity.isProcessInstanceType()) {
            EntityLinkService entityLinkService = CommandContextUtil.getEntityLinkService(commandContext);
            boolean deleteEntityLinks = true;
            if (eventDispatcherEnabled) {
                List<EntityLink> entityLinks = entityLinkService.findEntityLinksByScopeIdAndType(
                                executionEntity.getId(), ScopeTypes.BPMN, EntityLinkType.CHILD);
                for (EntityLink entityLink : entityLinks) {
                    fireEntityDeletedEvent((EntityLinkEntity) entityLink);
                }
                deleteEntityLinks = !entityLinks.isEmpty();
            }
            
            if (deleteEntityLinks) {
                entityLinkService.deleteEntityLinksByScopeIdAndType(executionEntity.getId(), ScopeTypes.BPMN);
            }
        }
    }

    protected void deleteVariables(ExecutionEntity executionEntity, CommandContext commandContext, boolean enableExecutionRelationshipCounts, boolean eventDispatcherEnabled) {
        if (!enableExecutionRelationshipCounts ||
                (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getVariableCount() > 0)) {
            
            Collection<VariableInstance> executionVariables = executionEntity.getVariableInstancesLocal().values();
            if (!executionVariables.isEmpty()) {
                
                ArrayList<VariableByteArrayRef> variableByteArrayRefs = new ArrayList<>();
                for (VariableInstance variableInstance : executionVariables) {
                    if (variableInstance instanceof VariableInstanceEntity) {
                        VariableInstanceEntity variableInstanceEntity = (VariableInstanceEntity) variableInstance;
                        
                        if (variableInstanceEntity.getByteArrayRef() != null && variableInstanceEntity.getByteArrayRef().getId() != null) {
                            variableByteArrayRefs.add(variableInstanceEntity.getByteArrayRef());
                        }
                        
                        if (eventDispatcherEnabled) {
                            FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher(commandContext);
                            if (eventDispatcher != null) {
                                eventDispatcher.dispatchEvent(EventUtil.createVariableDeleteEvent(variableInstanceEntity));
                                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, variableInstance));
                            }
                        }
                    }
                }
                
                // First byte arrays that reference variable, then variables in bulk
                for (VariableByteArrayRef variableByteArrayRef : variableByteArrayRefs) {
                    getByteArrayEntityManager().deleteByteArrayById(variableByteArrayRef.getId());
                }
                
                CommandContextUtil.getVariableService(commandContext).deleteVariablesByExecutionId(executionEntity.getId());
            }
        }
    }

    protected void deleteUserTasks(ExecutionEntity executionEntity, String deleteReason, CommandContext commandContext, 
            boolean enableExecutionRelationshipCounts, boolean eventDispatcherEnabled) {
        if (!enableExecutionRelationshipCounts ||
                (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getTaskCount() > 0)) {
            TaskHelper.deleteTasksForExecution(executionEntity, 
                    CommandContextUtil.getTaskService(commandContext).findTasksByExecutionId(executionEntity.getId()), deleteReason);
        }
    }
    
    protected void deleteJobs(ExecutionEntity executionEntity, CommandContext commandContext, boolean enableExecutionRelationshipCounts, boolean eventDispatcherEnabled) {
        
        // Jobs have byte array references that don't store the execution id. 
        // This means a bulk delete is not done for jobs. Generally there aren't many jobs / execution either.
        
        if (!enableExecutionRelationshipCounts
                || (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getTimerJobCount() > 0)) {
            CommandContextUtil.getTimerJobService().deleteTimerJobsByExecutionId(executionEntity.getId());
        }

        JobService jobService = CommandContextUtil.getJobService();
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
    }

    protected void deleteEventSubScriptions(ExecutionEntity executionEntity, boolean enableExecutionRelationshipCounts, boolean eventDispatcherEnabled) {
        if (!enableExecutionRelationshipCounts
                || (enableExecutionRelationshipCounts && ((CountingExecutionEntity) executionEntity).getEventSubscriptionCount() > 0)) {
            
            EventSubscriptionEntityManager eventSubscriptionEntityManager = getEventSubscriptionEntityManager();
            
            boolean deleteEventSubscriptions = true;
            if (eventDispatcherEnabled) {
                List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionEntityManager.findEventSubscriptionsByExecution(executionEntity.getId());
                for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                    
                    fireEntityDeletedEvent(eventSubscription);
                    if (MessageEventSubscriptionEntity.EVENT_TYPE.equals(eventSubscription.getEventType())) {
                        getEventDispatcher().dispatchEvent(FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED,
                                eventSubscription.getActivityId(), eventSubscription.getEventName(), null, eventSubscription.getExecutionId(),
                                eventSubscription.getProcessInstanceId(), eventSubscription.getProcessDefinitionId()));
                    }
                }
                
                deleteEventSubscriptions = !eventSubscriptions.isEmpty();
            }
            
            if (deleteEventSubscriptions) {
                eventSubscriptionEntityManager.deleteEventSubscriptionsByExecutionId(executionEntity.getId());
            }
        }
    }

    // OTHER METHODS

    @Override
    public void updateProcessInstanceLockTime(String processInstanceId) {
        Date expirationTime = getClock().getCurrentTime();
        int lockMillis = getAsyncExecutor().getAsyncJobLockTimeInMillis();

        GregorianCalendar lockCal = new GregorianCalendar();
        lockCal.setTime(expirationTime);
        lockCal.add(Calendar.MILLISECOND, lockMillis);
        Date lockDate = lockCal.getTime();

        executionDataManager.updateProcessInstanceLockTime(processInstanceId, lockDate, expirationTime);
    }

    @Override
    public void clearProcessInstanceLockTime(String processInstanceId) {
        executionDataManager.clearProcessInstanceLockTime(processInstanceId);
    }

    @Override
    public String updateProcessInstanceBusinessKey(ExecutionEntity executionEntity, String businessKey) {
        if (executionEntity.isProcessInstanceType() && businessKey != null) {
            executionEntity.setBusinessKey(businessKey);
            getHistoryManager().updateProcessBusinessKeyInHistory(executionEntity);

            if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, executionEntity));
            }

            return businessKey;
        }
        return null;
    }

    public ExecutionDataManager getExecutionDataManager() {
        return executionDataManager;
    }

    public void setExecutionDataManager(ExecutionDataManager executionDataManager) {
        this.executionDataManager = executionDataManager;
    }

}
