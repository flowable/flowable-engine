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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.db.SuspensionState;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Falko Menge
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */

public class ExecutionEntityImpl extends VariableScopeImpl implements ExecutionEntity, CountingExecutionEntity {

    private static final long serialVersionUID = 1L;

    // current position /////////////////////////////////////////////////////////

    protected FlowElement currentFlowElement;
    protected FlowableListener currentListener; // Only set when executing an execution listener

    /**
     * the process instance. this is the root of the execution tree. the processInstance of a process instance is a self reference.
     */
    protected ExecutionEntityImpl processInstance;

    /** the parent execution */
    protected ExecutionEntityImpl parent;

    /** nested executions representing scopes or concurrent paths */
    protected List<ExecutionEntityImpl> executions;

    /** super execution, not-null if this execution is part of a subprocess */
    protected ExecutionEntityImpl superExecution;

    /** reference to a subprocessinstance, not-null if currently subprocess is started from this execution */
    protected ExecutionEntityImpl subProcessInstance;

    /** The tenant identifier (if any) */
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
    protected String name;
    protected String description;
    protected String localizedName;
    protected String localizedDescription;

    protected Date lockTime;

    // state/type of execution //////////////////////////////////////////////////

    protected boolean isActive = true;
    protected boolean isScope = true;
    protected boolean isConcurrent;
    protected boolean isEnded;
    protected boolean isEventScope;
    protected boolean isMultiInstanceRoot;
    protected boolean isCountEnabled;

    // events ///////////////////////////////////////////////////////////////////

    // TODO: still needed in v6?

    protected String eventName;

    // associated entities /////////////////////////////////////////////////////

    // (we cache associated entities here to minimize db queries)
    protected List<EventSubscriptionEntity> eventSubscriptions;
    protected List<JobEntity> jobs;
    protected List<TimerJobEntity> timerJobs;
    protected List<TaskEntity> tasks;
    protected List<IdentityLinkEntity> identityLinks;

    // cascade deletion ////////////////////////////////////////////////////////

    protected String deleteReason;

    protected int suspensionState = SuspensionState.ACTIVE.getStateCode();

    protected String startActivityId;
    protected String startUserId;
    protected Date startTime;

    // CountingExecutionEntity
    protected int eventSubscriptionCount;
    protected int taskCount;
    protected int jobCount;
    protected int timerJobCount;
    protected int suspendedJobCount;
    protected int deadLetterJobCount;
    protected int variableCount;
    protected int identityLinkCount;

    /**
     * Persisted reference to the processDefinition.
     * 
     * @see #processDefinition
     * @see #setProcessDefinition(ProcessDefinitionImpl)
     * @see #getProcessDefinition()
     */
    protected String processDefinitionId;

    /**
     * Persisted reference to the process definition key.
     */
    protected String processDefinitionKey;

    /**
     * Persisted reference to the process definition name.
     */
    protected String processDefinitionName;

    /**
     * persisted reference to the process definition version.
     */
    protected Integer processDefinitionVersion;

    /**
     * Persisted reference to the deployment id.
     */
    protected String deploymentId;

    /**
     * Persisted reference to the current position in the diagram within the {@link #processDefinition}.
     * 
     * @see #activity
     * @see #setActivity(ActivityImpl)
     * @see #getActivity()
     */
    protected String activityId;

    /**
     * The name of the current activity position
     */
    protected String activityName;

    /**
     * Persisted reference to the process instance.
     * 
     * @see #getProcessInstance()
     */
    protected String processInstanceId;

    /**
     * Persisted reference to the business key.
     */
    protected String businessKey;

    /**
     * Persisted reference to the parent of this execution.
     * 
     * @see #getParent()
     * @see #setParentId(String)
     */
    protected String parentId;

    /**
     * Persisted reference to the super execution of this execution
     * 
     * @see #getSuperExecution()
     * @see #setSuperExecution(ExecutionEntityImpl)
     */
    protected String superExecutionId;

    protected String rootProcessInstanceId;
    protected ExecutionEntityImpl rootProcessInstance;

    protected boolean forcedUpdate;

    protected List<VariableInstanceEntity> queryVariables;

    protected boolean isDeleted; // TODO: should be in entity superclass probably

    public ExecutionEntityImpl() {

    }

    /**
     * Static factory method: to be used when a new execution is created for the very first time/ Calling this will make sure no extra db fetches are needed later on, as all collections will be
     * populated with empty collections. If they would be null, it would trigger a database fetch for those relationship entities.
     */
    public static ExecutionEntityImpl createWithEmptyRelationshipCollections() {
        ExecutionEntityImpl execution = new ExecutionEntityImpl();
        execution.executions = new ArrayList<>(1);
        execution.tasks = new ArrayList<>(1);
        execution.variableInstances = new HashMap<>(1);
        execution.jobs = new ArrayList<>(1);
        execution.timerJobs = new ArrayList<>(1);
        execution.eventSubscriptions = new ArrayList<>(1);
        execution.identityLinks = new ArrayList<>(1);
        return execution;
    }

    // persistent state /////////////////////////////////////////////////////////

    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("processDefinitionId", this.processDefinitionId);
        persistentState.put("businessKey", this.businessKey);
        persistentState.put("activityId", this.activityId);
        persistentState.put("isActive", this.isActive);
        persistentState.put("isConcurrent", this.isConcurrent);
        persistentState.put("isScope", this.isScope);
        persistentState.put("isEventScope", this.isEventScope);
        persistentState.put("parentId", parentId);
        persistentState.put("name", name);
        persistentState.put("lockTime", lockTime);
        persistentState.put("superExecution", this.superExecutionId);
        persistentState.put("rootProcessInstanceId", this.rootProcessInstanceId);
        persistentState.put("isMultiInstanceRoot", this.isMultiInstanceRoot);
        if (forcedUpdate) {
            persistentState.put("forcedUpdate", Boolean.TRUE);
        }
        persistentState.put("suspensionState", this.suspensionState);
        persistentState.put("startActivityId", this.startActivityId);
        persistentState.put("startTime", this.startTime);
        persistentState.put("startUserId", this.startUserId);
        persistentState.put("isCountEnabled", this.isCountEnabled);
        persistentState.put("eventSubscriptionCount", eventSubscriptionCount);
        persistentState.put("taskCount", taskCount);
        persistentState.put("jobCount", jobCount);
        persistentState.put("timerJobCount", timerJobCount);
        persistentState.put("suspendedJobCount", suspendedJobCount);
        persistentState.put("deadLetterJobCount", deadLetterJobCount);
        persistentState.put("variableCount", variableCount);
        persistentState.put("identityLinkCount", identityLinkCount);
        return persistentState;
    }

    // The current flow element, will be filled during operation execution

    public FlowElement getCurrentFlowElement() {
        if (currentFlowElement == null) {
            String processDefinitionId = getProcessDefinitionId();
            if (processDefinitionId != null) {
                org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
                currentFlowElement = process.getFlowElement(getCurrentActivityId(), true);
            }
        }
        return currentFlowElement;
    }

    public void setCurrentFlowElement(FlowElement currentFlowElement) {
        this.currentFlowElement = currentFlowElement;
        if (currentFlowElement != null) {
            this.activityId = currentFlowElement.getId();
        } else {
            this.activityId = null;
        }
    }

    public FlowableListener getCurrentFlowableListener() {
        return currentListener;
    }

    public void setCurrentFlowableListener(FlowableListener currentListener) {
        this.currentListener = currentListener;
    }

    // executions ///////////////////////////////////////////////////////////////

    /** ensures initialization and returns the non-null executions list */
    public List<ExecutionEntityImpl> getExecutions() {
        ensureExecutionsInitialized();
        return executions;
    }

    @Override
    public void addChildExecution(ExecutionEntity executionEntity) {
        ensureExecutionsInitialized();
        executions.remove(executionEntity);
        executions.add((ExecutionEntityImpl) executionEntity);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void ensureExecutionsInitialized() {
        if (executions == null) {
            this.executions = (List) CommandContextUtil.getExecutionEntityManager().findChildExecutionsByParentExecutionId(id);
        }
    }

    // business key ////////////////////////////////////////////////////////////

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getProcessInstanceBusinessKey() {
        return getProcessInstance().getBusinessKey();
    }

    // process definition ///////////////////////////////////////////////////////

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    // process instance /////////////////////////////////////////////////////////

    /** ensures initialization and returns the process instance. */
    public ExecutionEntityImpl getProcessInstance() {
        ensureProcessInstanceInitialized();
        return processInstance;
    }

    protected void ensureProcessInstanceInitialized() {
        if ((processInstance == null) && (processInstanceId != null)) {
            processInstance = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findById(processInstanceId);
        }
    }

    public void setProcessInstance(ExecutionEntity processInstance) {
        this.processInstance = (ExecutionEntityImpl) processInstance;
        if (processInstance != null) {
            this.processInstanceId = this.processInstance.getId();
        }
    }

    public boolean isProcessInstanceType() {
        return parentId == null;
    }

    // parent ///////////////////////////////////////////////////////////////////

    /** ensures initialization and returns the parent */
    public ExecutionEntityImpl getParent() {
        ensureParentInitialized();
        return parent;
    }

    protected void ensureParentInitialized() {
        if (parent == null && parentId != null) {
            parent = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findById(parentId);
        }
    }

    public void setParent(ExecutionEntity parent) {
        this.parent = (ExecutionEntityImpl) parent;

        if (parent != null) {
            this.parentId = parent.getId();
        } else {
            this.parentId = null;
        }
    }

    // super- and subprocess executions /////////////////////////////////////////

    public String getSuperExecutionId() {
        return superExecutionId;
    }

    public ExecutionEntityImpl getSuperExecution() {
        ensureSuperExecutionInitialized();
        return superExecution;
    }

    public void setSuperExecution(ExecutionEntity superExecution) {
        this.superExecution = (ExecutionEntityImpl) superExecution;
        if (superExecution != null) {
            superExecution.setSubProcessInstance(null);
        }

        if (superExecution != null) {
            this.superExecutionId = ((ExecutionEntityImpl) superExecution).getId();
        } else {
            this.superExecutionId = null;
        }
    }

    protected void ensureSuperExecutionInitialized() {
        if (superExecution == null && superExecutionId != null) {
            superExecution = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findById(superExecutionId);
        }
    }

    public ExecutionEntityImpl getSubProcessInstance() {
        ensureSubProcessInstanceInitialized();
        return subProcessInstance;
    }

    public void setSubProcessInstance(ExecutionEntity subProcessInstance) {
        this.subProcessInstance = (ExecutionEntityImpl) subProcessInstance;
    }

    protected void ensureSubProcessInstanceInitialized() {
        if (subProcessInstance == null) {
            subProcessInstance = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findSubProcessInstanceBySuperExecutionId(id);
        }
    }

    public ExecutionEntity getRootProcessInstance() {
        ensureRootProcessInstanceInitialized();
        return rootProcessInstance;
    }

    protected void ensureRootProcessInstanceInitialized() {
        if (rootProcessInstanceId == null) {
            rootProcessInstance = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findById(rootProcessInstanceId);
        }
    }

    public void setRootProcessInstance(ExecutionEntity rootProcessInstance) {
        this.rootProcessInstance = (ExecutionEntityImpl) rootProcessInstance;

        if (rootProcessInstance != null) {
            this.rootProcessInstanceId = rootProcessInstance.getId();
        } else {
            this.rootProcessInstanceId = null;
        }
    }

    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    public void setRootProcessInstanceId(String rootProcessInstanceId) {
        this.rootProcessInstanceId = rootProcessInstanceId;
    }

    // scopes ///////////////////////////////////////////////////////////////////

    public boolean isScope() {
        return isScope;
    }

    public void setScope(boolean isScope) {
        this.isScope = isScope;
    }

    public void forceUpdate() {
        this.forcedUpdate = true;
    }

    // VariableScopeImpl methods //////////////////////////////////////////////////////////////////

    // TODO: this should ideally move to another place
    @Override
    protected void initializeVariableInstanceBackPointer(VariableInstanceEntity variableInstance) {
        if (processInstanceId != null) {
            variableInstance.setProcessInstanceId(processInstanceId);
        } else {
            variableInstance.setProcessInstanceId(id);
        }
        variableInstance.setExecutionId(id);
        variableInstance.setProcessDefinitionId(processDefinitionId);
    }

    @Override
    protected Collection<VariableInstanceEntity> loadVariableInstances() {
        return CommandContextUtil.getVariableService().findVariableInstancesByExecutionId(id);
    }

    @Override
    protected VariableScopeImpl getParentVariableScope() {
        return getParent();
    }
    
    @Override
    public void setVariable(String variableName, Object value, boolean fetchAllVariables) {
        setVariable(variableName, value, this, fetchAllVariables);
    }
    
    public void setVariable(String variableName, Object value, ExecutionEntity sourceExecution, boolean fetchAllVariables) {

        if (fetchAllVariables) {

            // If it's in the cache, it's more recent
            if (usedVariablesCache.containsKey(variableName)) {
                updateVariableInstance(usedVariablesCache.get(variableName), value, sourceExecution);
            }

            // If the variable exists on this scope, replace it
            if (hasVariableLocal(variableName)) {
                setVariableLocal(variableName, value, sourceExecution, true);
                return;
            }

            // Otherwise, go up the hierarchy (we're trying to put it as high as possible)
            VariableScopeImpl parentVariableScope = getParentVariableScope();
            if (parentVariableScope != null) {
                if (sourceExecution == null) {
                    parentVariableScope.setVariable(variableName, value);
                } else {
                    ((ExecutionEntity) parentVariableScope).setVariable(variableName, value, sourceExecution, true);
                }
                return;
            }

            // We're as high as possible and the variable doesn't exist yet, so we're creating it
            if (sourceExecution != null) {
                createVariableLocal(variableName, value, sourceExecution);
            } else {
                createVariableLocal(variableName, value);
            }

        } else {

            // Check local cache first
            if (usedVariablesCache.containsKey(variableName)) {

                updateVariableInstance(usedVariablesCache.get(variableName), value, sourceExecution);

            } else if (variableInstances != null && variableInstances.containsKey(variableName)) {

                updateVariableInstance(variableInstances.get(variableName), value, sourceExecution);

            } else {

                // Not in local cache, check if defined on this scope
                // Create it if it doesn't exist yet
                VariableInstanceEntity variable = getSpecificVariable(variableName);
                if (variable != null) {
                    updateVariableInstance(variable, value, sourceExecution);
                    usedVariablesCache.put(variableName, variable);
                } else {

                    VariableScopeImpl parent = getParentVariableScope();
                    if (parent != null) {
                        if (sourceExecution == null) {
                            parent.setVariable(variableName, value, fetchAllVariables);
                        } else {
                            ((ExecutionEntity) parent).setVariable(variableName, value, sourceExecution, fetchAllVariables);
                        }
                        return;
                    }

                    variable = createVariableInstance(variableName, value, sourceExecution);
                    usedVariablesCache.put(variableName, variable);
                }

            }

        }

    }
    
    @Override
    public Object setVariableLocal(String variableName, Object value, boolean fetchAllVariables) {
        return setVariableLocal(variableName, value, this, fetchAllVariables);
    }

    @Override
    public Object setVariableLocal(String variableName, Object value, ExecutionEntity sourceExecution, boolean fetchAllVariables) {
        if (fetchAllVariables) {

            // If it's in the cache, it's more recent
            if (usedVariablesCache.containsKey(variableName)) {
                updateVariableInstance(usedVariablesCache.get(variableName), value, sourceExecution);
            }

            ensureVariableInstancesInitialized();

            VariableInstanceEntity variableInstance = variableInstances.get(variableName);
            if (variableInstance == null) {
                variableInstance = usedVariablesCache.get(variableName);
            }

            if (variableInstance == null) {
                createVariableLocal(variableName, value, sourceExecution);
            } else {
                updateVariableInstance(variableInstance, value, sourceExecution);
            }

            return null;

        } else {

            if (usedVariablesCache.containsKey(variableName)) {
                updateVariableInstance(usedVariablesCache.get(variableName), value, sourceExecution);
            } else if (variableInstances != null && variableInstances.containsKey(variableName)) {
                updateVariableInstance(variableInstances.get(variableName), value, sourceExecution);
            } else {

                VariableInstanceEntity variable = getSpecificVariable(variableName);
                if (variable != null) {
                    updateVariableInstance(variable, value, sourceExecution);
                } else {
                    variable = createVariableInstance(variableName, value, sourceExecution);
                }
                usedVariablesCache.put(variableName, variable);

            }

            return null;

        }
    }
    
    @Override
    protected VariableInstanceEntity createVariableInstance(String variableName, Object value) {
        return createVariableInstance(variableName, value, this);
    }
    
    protected VariableInstanceEntity createVariableInstance(String variableName, Object value, ExecutionEntity sourceExecution) {
        VariableInstanceEntity variableInstance = super.createVariableInstance(variableName, value);
        
        CountingEntityUtil.handleInsertVariableInstanceEntityCount(variableInstance);
        
        // Record historic variable
        CommandContextUtil.getHistoryManager().recordVariableCreate(variableInstance);

        // Record historic detail
        CommandContextUtil.getHistoryManager().recordHistoricDetailVariableCreate(variableInstance, sourceExecution, true);

        return variableInstance;
    }
    
    protected void createVariableLocal(String variableName, Object value, ExecutionEntity sourceActivityExecution) {
        ensureVariableInstancesInitialized();

        if (variableInstances.containsKey(variableName)) {
            throw new FlowableException("variable '" + variableName + "' already exists. Use setVariableLocal if you want to overwrite the value");
        }

        createVariableInstance(variableName, value, sourceActivityExecution);
    }
    
    @Override
    protected void updateVariableInstance(VariableInstanceEntity variableInstance, Object value) {
        updateVariableInstance(variableInstance, value, this);
    }

    protected void updateVariableInstance(VariableInstanceEntity variableInstance, Object value, ExecutionEntity sourceExecution) {
        super.updateVariableInstance(variableInstance, value);
        
        CommandContextUtil.getHistoryManager().recordHistoricDetailVariableCreate(variableInstance, sourceExecution, true);

        CommandContextUtil.getHistoryManager().recordVariableUpdate(variableInstance);
    }
    
    @Override
    protected void deleteVariableInstanceForExplicitUserCall(VariableInstanceEntity variableInstance) {
        super.deleteVariableInstanceForExplicitUserCall(variableInstance);
        
        CountingEntityUtil.handleDeleteVariableInstanceEntityCount(variableInstance, true);
        
        // Record historic variable deletion
        CommandContextUtil.getHistoryManager().recordVariableRemoved(variableInstance);

        // Record historic detail
        CommandContextUtil.getHistoryManager().recordHistoricDetailVariableCreate(variableInstance, this, true);
    }
    
    @Override
    protected String variableScopeType() {
        return "execution";
    }

    @Override
    protected VariableInstanceEntity getSpecificVariable(String variableName) {

        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context");
        }
        VariableInstanceEntity variableInstance = CommandContextUtil.getVariableService().findVariableInstanceByExecutionAndName(id, variableName);

        return variableInstance;
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context");
        }
        return CommandContextUtil.getVariableService().findVariableInstancesByExecutionAndNames(id, variableNames);
    }

    // event subscription support //////////////////////////////////////////////

    public List<EventSubscriptionEntity> getEventSubscriptions() {
        ensureEventSubscriptionsInitialized();
        return eventSubscriptions;
    }

    protected void ensureEventSubscriptionsInitialized() {
        if (eventSubscriptions == null) {
            eventSubscriptions = CommandContextUtil.getEventSubscriptionEntityManager().findEventSubscriptionsByExecution(id);
        }
    }

    // referenced job entities //////////////////////////////////////////////////

    public List<JobEntity> getJobs() {
        ensureJobsInitialized();
        return jobs;
    }

    protected void ensureJobsInitialized() {
        if (jobs == null) {
            jobs = CommandContextUtil.getJobService().findJobsByExecutionId(id);
        }
    }

    public List<TimerJobEntity> getTimerJobs() {
        ensureTimerJobsInitialized();
        return timerJobs;
    }

    protected void ensureTimerJobsInitialized() {
        if (timerJobs == null) {
            timerJobs = CommandContextUtil.getTimerJobService().findTimerJobsByExecutionId(id);
        }
    }

    // referenced task entities ///////////////////////////////////////////////////

    protected void ensureTasksInitialized() {
        if (tasks == null) {
            tasks = CommandContextUtil.getTaskService().findTasksByExecutionId(id);
        }
    }

    public List<TaskEntity> getTasks() {
        ensureTasksInitialized();
        return tasks;
    }

    // identity links ///////////////////////////////////////////////////////////

    public List<IdentityLinkEntity> getIdentityLinks() {
        ensureIdentityLinksInitialized();
        return identityLinks;
    }

    protected void ensureIdentityLinksInitialized() {
        if (identityLinks == null) {
            identityLinks = CommandContextUtil.getIdentityLinkService().findIdentityLinksByProcessInstanceId(id);
        }
    }

    // getters and setters //////////////////////////////////////////////////////

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getActivityId() {
        return activityId;
    }

    public boolean isConcurrent() {
        return isConcurrent;
    }

    public void setConcurrent(boolean isConcurrent) {
        this.isConcurrent = isConcurrent;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void inactivate() {
        this.isActive = false;
    }

    public boolean isEnded() {
        return isEnded;
    }

    public void setEnded(boolean isEnded) {
        this.isEnded = isEnded;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public int getSuspensionState() {
        return suspensionState;
    }

    public void setSuspensionState(int suspensionState) {
        this.suspensionState = suspensionState;
    }

    public boolean isSuspended() {
        return suspensionState == SuspensionState.SUSPENDED.getStateCode();
    }

    public boolean isEventScope() {
        return isEventScope;
    }

    public void setEventScope(boolean isEventScope) {
        this.isEventScope = isEventScope;
    }

    @Override
    public boolean isMultiInstanceRoot() {
        return isMultiInstanceRoot;
    }

    @Override
    public void setMultiInstanceRoot(boolean isMultiInstanceRoot) {
        this.isMultiInstanceRoot = isMultiInstanceRoot;
    }

    @Override
    public boolean isCountEnabled() {
        return isCountEnabled;
    }

    @Override
    public void setCountEnabled(boolean isCountEnabled) {
        this.isCountEnabled = isCountEnabled;
    }

    public String getCurrentActivityId() {
        return activityId;
    }

    @Override
    public String getName() {
        if (localizedName != null && localizedName.length() > 0) {
            return localizedName;
        } else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        if (localizedDescription != null && localizedDescription.length() > 0) {
            return localizedDescription;
        } else {
            return description;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    public String getLocalizedDescription() {
        return localizedDescription;
    }

    public void setLocalizedDescription(String localizedDescription) {
        this.localizedDescription = localizedDescription;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Date getLockTime() {
        return lockTime;
    }

    public void setLockTime(Date lockTime) {
        this.lockTime = lockTime;
    }

    public Map<String, Object> getProcessVariables() {
        Map<String, Object> variables = new HashMap<>();
        if (queryVariables != null) {
            for (VariableInstanceEntity variableInstance : queryVariables) {
                if (variableInstance.getId() != null && variableInstance.getTaskId() == null) {
                    variables.put(variableInstance.getName(), variableInstance.getValue());
                }
            }
        }
        return variables;
    }

    public List<VariableInstanceEntity> getQueryVariables() {
        if (queryVariables == null && Context.getCommandContext() != null) {
            queryVariables = new VariableInitializingList();
        }
        return queryVariables;
    }

    public void setQueryVariables(List<VariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getStartActivityId() {
        return startActivityId;
    }

    public void setStartActivityId(String startActivityId) {
        this.startActivityId = startActivityId;
    }

    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public int getEventSubscriptionCount() {
        return eventSubscriptionCount;
    }

    public void setEventSubscriptionCount(int eventSubscriptionCount) {
        this.eventSubscriptionCount = eventSubscriptionCount;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public int getJobCount() {
        return jobCount;
    }

    public void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    public int getTimerJobCount() {
        return timerJobCount;
    }

    public void setTimerJobCount(int timerJobCount) {
        this.timerJobCount = timerJobCount;
    }

    public int getSuspendedJobCount() {
        return suspendedJobCount;
    }

    public void setSuspendedJobCount(int suspendedJobCount) {
        this.suspendedJobCount = suspendedJobCount;
    }

    public int getDeadLetterJobCount() {
        return deadLetterJobCount;
    }

    public void setDeadLetterJobCount(int deadLetterJobCount) {
        this.deadLetterJobCount = deadLetterJobCount;
    }

    public int getVariableCount() {
        return variableCount;
    }

    public void setVariableCount(int variableCount) {
        this.variableCount = variableCount;
    }

    public int getIdentityLinkCount() {
        return identityLinkCount;
    }

    public void setIdentityLinkCount(int identityLinkCount) {
        this.identityLinkCount = identityLinkCount;
    }

    // toString /////////////////////////////////////////////////////////////////

    public String toString() {
        if (isProcessInstanceType()) {
            return "ProcessInstance[" + getId() + "]";
        } else {
            StringBuilder strb = new StringBuilder();
            if (isScope) {
                strb.append("Scoped execution[ id '").append(getId()).append("' ]");
            } else if (isMultiInstanceRoot) {
                strb.append("Multi instance root execution[ id '").append(getId()).append("' ]");
            } else {
                strb.append("Execution[ id '").append(getId()).append("' ]");
            }
            if (activityId != null) {
                strb.append(" - activity '").append(activityId);
            }
            if (parentId != null) {
                strb.append(" - parent '").append(parentId).append("'");
            }
            return strb.toString();
        }
    }

}
