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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionData;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.ReadOnlyDelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ReadOnlyDelegateExecutionImpl;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.util.BpmnLoggingSessionUtil;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Falko Menge
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */

public class ExecutionEntityImpl extends AbstractBpmnEngineVariableScopeEntity implements ExecutionEntity, CountingExecutionEntity {

    private static final long serialVersionUID = 1L;

    // current position /////////////////////////////////////////////////////////

    protected FlowElement currentFlowElement;
    protected FlowableListener currentListener; // Only set when executing an execution listener
    
    protected FlowElement originatingCurrentFlowElement;

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
    protected boolean isSubProcessInstanceInitialized;
    protected ExecutionEntityImpl subProcessInstance;

    /** The tenant identifier (if any) */
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
    protected String name;
    protected String description;
    protected String localizedName;
    protected String localizedDescription;

    protected Date lockTime;
    protected String lockOwner;

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
    protected int externalWorkerJobCount;
    protected int variableCount;
    protected int identityLinkCount;
    
    /**
     * Persisted reference to the processDefinition.
     * 
     * @see #processDefinitionId
     * @see #setProcessDefinitionId(String)
     * @see #getProcessDefinitionId()
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
     * Persisted reference to the process definition version.
     */
    protected Integer processDefinitionVersion;

    /**
     * Persisted reference to the process definition category.
     */
    protected String processDefinitionCategory;

    /**
     * Persisted reference to the deployment id.
     */
    protected String deploymentId;

    /**
     * Persisted reference to the current position in the diagram within the {@link #processDefinitionId}.
     *
     * @see #activityId
     * @see #setActivityId(String)
     * @see #getActivityId()
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
     * Persisted reference to the business status.
     */
    protected String businessStatus;

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
     * @see #setSuperExecution(ExecutionEntity)
     */
    protected String superExecutionId;

    protected String rootProcessInstanceId;
    protected ExecutionEntityImpl rootProcessInstance;

    protected boolean forcedUpdate;

    protected List<VariableInstanceEntity> queryVariables;
    
    // Callback
    protected String callbackId;
    protected String callbackType;

    // Reference
    protected String referenceId;
    protected String referenceType;

    /**
     * The optional stage instance id, if this execution runs in the context of a CMMN case and has a parent stage it belongs to.
     */
    protected String propagatedStageInstanceId;

    public ExecutionEntityImpl() {

    }


    /**
     * Static factory method: to be used when a new execution is created for the very first time/ Calling this will make sure no extra db fetches are needed later on, as all collections will be
     * populated with empty collections. If they would be null, it would trigger a database fetch for those relationship entities.
     */
    public static ExecutionEntityImpl createWithEmptyRelationshipCollections() {
        ExecutionEntityImpl execution = new ExecutionEntityImpl();
        execution.executions = new ArrayList<>(1);
        execution.variableInstances = new HashMap<>(1);
        execution.eventSubscriptions = new ArrayList<>(1);
        execution.identityLinks = new ArrayList<>(1);
        return execution;
    }

    // persistent state /////////////////////////////////////////////////////////

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("processDefinitionId", this.processDefinitionId);
        persistentState.put("businessKey", this.businessKey);
        persistentState.put("businessStatus", this.businessStatus);
        persistentState.put("activityId", this.activityId);
        persistentState.put("isActive", this.isActive);
        persistentState.put("isConcurrent", this.isConcurrent);
        persistentState.put("isScope", this.isScope);
        persistentState.put("isEventScope", this.isEventScope);
        persistentState.put("parentId", parentId);
        persistentState.put("name", name);
        persistentState.put("lockTime", lockTime);
        persistentState.put("lockOwner", lockOwner);
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
        persistentState.put("externalWorkerJobCount", externalWorkerJobCount);
        persistentState.put("variableCount", variableCount);
        persistentState.put("identityLinkCount", identityLinkCount);
        persistentState.put("callbackId", callbackId);
        persistentState.put("callbackType", callbackType);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("propagatedStageInstanceId", propagatedStageInstanceId);
        return persistentState;
    }

    @Override
    public ReadOnlyDelegateExecution snapshotReadOnly() {
        return new ReadOnlyDelegateExecutionImpl(this);
    }

    // The current flow element, will be filled during operation execution

    @Override
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

    @Override
    public void setCurrentFlowElement(FlowElement currentFlowElement) {
        this.currentFlowElement = currentFlowElement;
        if (currentFlowElement != null) {
            this.activityId = currentFlowElement.getId();
            this.activityName = currentFlowElement.getName();
        } else {
            this.activityId = null;
            this.activityName = null;
        }
    }

    @Override
    public FlowableListener getCurrentFlowableListener() {
        return currentListener;
    }

    @Override
    public void setCurrentFlowableListener(FlowableListener currentListener) {
        this.currentListener = currentListener;
    }
    
    @Override
    public FlowElement getOriginatingCurrentFlowElement() {
        return originatingCurrentFlowElement;
    }
    
    @Override
    public void setOriginatingCurrentFlowElement(FlowElement flowElement) {
        this.originatingCurrentFlowElement = flowElement;
    }

    // executions ///////////////////////////////////////////////////////////////

    /** ensures initialization and returns the non-null executions list */
    @Override
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

    @Override
    public String getBusinessKey() {
        return businessKey;
    }

    @Override
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public String getBusinessStatus() {
        return businessStatus;
    }

    @Override
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    @Override
    public String getProcessInstanceBusinessKey() {
        return getProcessInstance().getBusinessKey();
    }
    
    @Override
    public String getProcessInstanceBusinessStatus() {
        return getProcessInstance().getBusinessStatus();
    }

    // process definition ///////////////////////////////////////////////////////

    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getProcessDefinitionKey() {
        if (StringUtils.isEmpty(processDefinitionKey) && StringUtils.isNotEmpty(processDefinitionId)) {
            resolveProcessDefinitionInfo();
        }
        return processDefinitionKey;
    }

    @Override
    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    @Override
    public String getProcessDefinitionName() {
        // The process definition name can be null, therefore we can't use an is empty check on it
        // as it will lead to evaluating the information every time we try to get the name, even though it is null
        // The process definition key can never be empty, therefore we use it to check if process definition information has been resolved
        if (StringUtils.isEmpty(processDefinitionKey) && StringUtils.isNotEmpty(processDefinitionId)) {
            resolveProcessDefinitionInfo();
        }
        return processDefinitionName;
    }

    @Override
    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    @Override
    public Integer getProcessDefinitionVersion() {
        if (processDefinitionVersion == null && StringUtils.isNotEmpty(processDefinitionId)) {
            resolveProcessDefinitionInfo();
        }
        return processDefinitionVersion;
    }

    @Override
    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    @Override
    public String getProcessDefinitionCategory() {
        if (StringUtils.isEmpty(processDefinitionCategory) && StringUtils.isNotEmpty(processDefinitionId)) {
            resolveProcessDefinitionInfo();
        }
        return processDefinitionCategory;
    }

    @Override
    public void setProcessDefinitionCategory(String processDefinitionCategory) {
        this.processDefinitionCategory = processDefinitionCategory;
    }

    @Override
    public String getDeploymentId() {
        if (StringUtils.isEmpty(deploymentId) && StringUtils.isNotEmpty(processDefinitionId)) {
            resolveProcessDefinitionInfo();
        }
        return deploymentId;
    }

    @Override
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    // process instance /////////////////////////////////////////////////////////

    /** ensures initialization and returns the process instance. */
    @Override
    public ExecutionEntityImpl getProcessInstance() {
        ensureProcessInstanceInitialized();
        return processInstance;
    }

    protected void ensureProcessInstanceInitialized() {
        if ((processInstance == null) && (processInstanceId != null)) {
            processInstance = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findById(processInstanceId);
        }
    }

    @Override
    public void setProcessInstance(ExecutionEntity processInstance) {
        this.processInstance = (ExecutionEntityImpl) processInstance;
        if (processInstance != null) {
            this.processInstanceId = this.processInstance.getId();
        }
    }

    @Override
    public boolean isProcessInstanceType() {
        return parentId == null;
    }

    // parent ///////////////////////////////////////////////////////////////////

    /** ensures initialization and returns the parent */
    @Override
    public ExecutionEntityImpl getParent() {
        ensureParentInitialized();
        return parent;
    }

    protected void ensureParentInitialized() {
        if (parent == null && parentId != null) {
            parent = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findById(parentId);
        }
    }

    @Override
    public void setParent(ExecutionEntity parent) {
        this.parent = (ExecutionEntityImpl) parent;

        if (parent != null) {
            this.parentId = parent.getId();
        } else {
            this.parentId = null;
        }
    }

    // super- and subprocess executions /////////////////////////////////////////

    @Override
    public String getSuperExecutionId() {
        return superExecutionId;
    }
    
    public void setSuperExecutionId(String superExecutionId) {
        this.superExecutionId = superExecutionId;
    }

    @Override
    public ExecutionEntityImpl getSuperExecution() {
        ensureSuperExecutionInitialized();
        return superExecution;
    }

    @Override
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

    @Override
    public ExecutionEntityImpl getSubProcessInstance() {
        ensureSubProcessInstanceInitialized();
        return subProcessInstance;
    }

    @Override
    public void setSubProcessInstance(ExecutionEntity subProcessInstance) {
        this.subProcessInstance = (ExecutionEntityImpl) subProcessInstance;
    }

    protected void ensureSubProcessInstanceInitialized() {
        if (!isSubProcessInstanceInitialized && activityId != null && subProcessInstance == null) {
            isSubProcessInstanceInitialized = true;
            FlowElement flowElement = getCurrentFlowElement();
            if (flowElement != null && flowElement instanceof CallActivity) {
                subProcessInstance = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findSubProcessInstanceBySuperExecutionId(id);
            }
        }
    }

    @Override
    public ExecutionEntity getRootProcessInstance() {
        ensureRootProcessInstanceInitialized();
        return rootProcessInstance;
    }

    protected void ensureRootProcessInstanceInitialized() {
        if (rootProcessInstance == null && rootProcessInstanceId != null) {
            rootProcessInstance = (ExecutionEntityImpl) CommandContextUtil.getExecutionEntityManager().findById(rootProcessInstanceId);
        }
    }

    @Override
    public void setRootProcessInstance(ExecutionEntity rootProcessInstance) {
        this.rootProcessInstance = (ExecutionEntityImpl) rootProcessInstance;

        if (rootProcessInstance != null) {
            this.rootProcessInstanceId = rootProcessInstance.getId();
        } else {
            this.rootProcessInstanceId = null;
        }
    }

    @Override
    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    @Override
    public void setRootProcessInstanceId(String rootProcessInstanceId) {
        this.rootProcessInstanceId = rootProcessInstanceId;
    }

    // scopes ///////////////////////////////////////////////////////////////////

    @Override
    public boolean isScope() {
        return isScope;
    }

    public boolean getIsScope() {
        return isScope;
    }

    @Override
    public void setScope(boolean isScope) {
        this.isScope = isScope;
    }

    public void setIsScope(boolean isScope) {
        this.isScope = isScope;
    }

    @Override
    public void forceUpdate() {
        this.forcedUpdate = true;
    }

    // VariableScopeImpl methods //////////////////////////////////////////////////////////////////

    @Override
    protected void initializeVariableInstanceBackPointer(VariableInstance variableInstance) {
        if (processInstanceId != null) {
            variableInstance.setProcessInstanceId(processInstanceId);
        } else {
            variableInstance.setProcessInstanceId(id);
        }
        variableInstance.setExecutionId(id);
        variableInstance.setProcessDefinitionId(processDefinitionId);
    }

    @Override
    protected boolean storeVariableLocal(String variableName) {
        if (super.storeVariableLocal(variableName)) {
            return true;
        }

        ExecutionEntityImpl parent = getParent();
        if (parent != null && parent.isMultiInstanceRoot()) {

            if (getCurrentFlowElement() instanceof BoundaryEvent) {
                // Executions for boundary events should not store variables locally
                return false;
            }

            // If the parent is a multi instance root then the variable should be stored in this execution
            // the multi instance behaviour will collect this variables once it is done
            // For backwards compatibility we store the variable locally only if the loop characteristics has aggregations
            FlowElement parentFlowElement = parent.getCurrentFlowElement();
            if (parentFlowElement instanceof Activity) {
                MultiInstanceLoopCharacteristics loopCharacteristics = ((Activity) parentFlowElement).getLoopCharacteristics();
                return loopCharacteristics != null && loopCharacteristics.getAggregations() != null;
            }
        }

        return false;
    }

    @Override
    protected void addLoggingSessionInfo(ObjectNode loggingNode) {
        BpmnLoggingSessionUtil.fillLoggingData(loggingNode, this);
    }

    @Override
    protected Collection<VariableInstanceEntity> loadVariableInstances() {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        return processEngineConfiguration.getVariableServiceConfiguration().getVariableService().findVariableInstancesByExecutionId(id);
    }

    @Override
    protected VariableScopeImpl getParentVariableScope() {
        return getParent();
    }
    
    @Override
    public void setVariable(String variableName, Object value, boolean fetchAllVariables) {
        setVariable(variableName, value, this, fetchAllVariables);
    }
    
    @Override
    public void setVariable(String variableName, Object value, ExecutionEntity sourceExecution, boolean fetchAllVariables) {

        if (fetchAllVariables) {

            // If it's in the cache, it's more recent
            if (usedVariablesCache.containsKey(variableName)) {
                updateVariableInstance(usedVariablesCache.get(variableName), value, sourceExecution);
            }

            // If the variable exists on this scope, replace it
            if (storeVariableLocal(variableName)) {
                setVariableLocal(variableName, value, sourceExecution, true);
                return;
            }

            // Otherwise, go up the hierarchy (we're trying to put it as high as possible)
            VariableScopeImpl parentVariableScope = getParentVariableScope();
            if (parentVariableScope != null) {
                FlowElement localFlowElement = getCurrentFlowElement();
                if (localFlowElement != null) {
                    ((ExecutionEntity) parentVariableScope).setOriginatingCurrentFlowElement(localFlowElement);
                }
                
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
                }
                usedVariablesCache.put(variableName, variable);

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

        }
        return null;
    }
    
    @Override
    protected VariableInstanceEntity createVariableInstance(String variableName, Object value) {
        return createVariableInstance(variableName, value, this);
    }
    
    protected VariableInstanceEntity createVariableInstance(String variableName, Object value, ExecutionEntity sourceExecution) {
        VariableInstanceEntity variableInstance = super.createVariableInstance(variableName, value);
        
        CountingEntityUtil.handleInsertVariableInstanceEntityCount(variableInstance);
        
        VariableListenerSession variableListenerSession = Context.getCommandContext().getSession(VariableListenerSession.class);
        variableListenerSession.addVariableData(variableInstance.getName(), VariableListenerSessionData.VARIABLE_CREATE, 
                variableInstance.getProcessInstanceId(), ScopeTypes.BPMN, variableInstance.getProcessDefinitionId());
        
        Clock clock = CommandContextUtil.getProcessEngineConfiguration().getClock();
        // Record historic variable
        CommandContextUtil.getHistoryManager().recordVariableCreate(variableInstance, clock.getCurrentTime());

        // Record historic detail
        CommandContextUtil.getHistoryManager().recordHistoricDetailVariableCreate(variableInstance, sourceExecution, true,
            getRelatedActivityInstanceId(sourceExecution), clock.getCurrentTime());

        return variableInstance;
    }
    
    protected void createVariableLocal(String variableName, Object value, ExecutionEntity sourceActivityExecution) {
        ensureVariableInstancesInitialized();

        if (variableInstances.containsKey(variableName)) {
            throw new FlowableException("variable '" + variableName + "' already exists. Use setVariableLocal if you want to overwrite the value for " + this);
        }

        createVariableInstance(variableName, value, sourceActivityExecution);
    }
    
    @Override
    protected void updateVariableInstance(VariableInstanceEntity variableInstance, Object value) {
        updateVariableInstance(variableInstance, value, this);
    }

    protected void updateVariableInstance(VariableInstanceEntity variableInstance, Object value, ExecutionEntity sourceExecution) {
        super.updateVariableInstance(variableInstance, value);

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        VariableListenerSession variableListenerSession = Context.getCommandContext().getSession(VariableListenerSession.class);
        variableListenerSession.addVariableData(variableInstance.getName(), VariableListenerSessionData.VARIABLE_UPDATE, 
                variableInstance.getProcessInstanceId(), ScopeTypes.BPMN, variableInstance.getProcessDefinitionId());
        
        Clock clock = processEngineConfiguration.getClock();
        CommandContextUtil.getHistoryManager().recordHistoricDetailVariableCreate(variableInstance, sourceExecution, true,
            getRelatedActivityInstanceId(sourceExecution), clock.getCurrentTime());

        CommandContextUtil.getHistoryManager().recordVariableUpdate(variableInstance, clock.getCurrentTime());
    }

    @Override
    protected void deleteVariableInstanceForExplicitUserCall(VariableInstanceEntity variableInstance) {
        super.deleteVariableInstanceForExplicitUserCall(variableInstance);
        
        CountingEntityUtil.handleDeleteVariableInstanceEntityCount(variableInstance, true);
        
        Clock clock = CommandContextUtil.getProcessEngineConfiguration().getClock();
        // Record historic variable deletion
        CommandContextUtil.getHistoryManager().recordVariableRemoved(variableInstance);

        // Record historic detail
        CommandContextUtil.getHistoryManager().recordHistoricDetailVariableCreate(variableInstance, this, true,
            getRelatedActivityInstanceId(this), clock.getCurrentTime());
    }
    
    @Override
    protected boolean isPropagateToHistoricVariable() {
        return false;
    }

    @Override
    protected VariableInstanceEntity getSpecificVariable(String variableName) {

        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context for " + this);
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        return processEngineConfiguration.getVariableServiceConfiguration().getVariableService()
                .createInternalVariableInstanceQuery()
                .executionId(id)
                .withoutTaskId()
                .name(variableName)
                .singleResult();
    }

    @Override
    protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            throw new FlowableException("lazy loading outside command context for " + this);
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        return processEngineConfiguration.getVariableServiceConfiguration().getVariableService()
                .createInternalVariableInstanceQuery()
                .executionId(id)
                .withoutTaskId()
                .names(variableNames)
                .list();
    }

    // event subscription support //////////////////////////////////////////////

    @Override
    public List<EventSubscriptionEntity> getEventSubscriptions() {
        ensureEventSubscriptionsInitialized();
        return eventSubscriptions;
    }

    protected void ensureEventSubscriptionsInitialized() {
        if (eventSubscriptions == null) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            eventSubscriptions = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                    .findEventSubscriptionsByExecution(id);
        }
    }

    // identity links ///////////////////////////////////////////////////////////

    @Override
    public List<IdentityLinkEntity> getIdentityLinks() {
        ensureIdentityLinksInitialized();
        return identityLinks;
    }

    protected void ensureIdentityLinksInitialized() {
        if (identityLinks == null) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            identityLinks = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                    .findIdentityLinksByProcessInstanceId(id);
        }
    }

    // getters and setters //////////////////////////////////////////////////////

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public String getActivityId() {
        return activityId;
    }
    
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    @Override
    public boolean isConcurrent() {
        return isConcurrent;
    }
    
    public boolean getIsConcurrent() {
        return isConcurrent;
    }

    @Override
    public void setConcurrent(boolean isConcurrent) {
        this.isConcurrent = isConcurrent;
    }

    public void setIsConcurrent(boolean isConcurrent) {
        this.isConcurrent = isConcurrent;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    public boolean getIsActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public void inactivate() {
        this.isActive = false;
    }

    @Override
    public boolean isEnded() {
        return isEnded;
    }

    public boolean getIsEnded() {
        return isEnded;
    }

    @Override
    public void setEnded(boolean isEnded) {
        this.isEnded = isEnded;
    }

    public void setIsEnded(boolean isEnded) {
        this.isEnded = isEnded;
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    @Override
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public String getDeleteReason() {
        return deleteReason;
    }

    @Override
    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    @Override
    public int getSuspensionState() {
        return suspensionState;
    }

    @Override
    public void setSuspensionState(int suspensionState) {
        this.suspensionState = suspensionState;
    }

    @Override
    public boolean isSuspended() {
        return suspensionState == SuspensionState.SUSPENDED.getStateCode();
    }

    @Override
    public boolean isEventScope() {
        return isEventScope;
    }

    public boolean getIsEventScope() {
        return isEventScope;
    }

    @Override
    public void setEventScope(boolean isEventScope) {
        this.isEventScope = isEventScope;
    }

    public void setIsEventScope(boolean isEventScope) {
        this.isEventScope = isEventScope;
    }

    @Override
    public boolean isMultiInstanceRoot() {
        return isMultiInstanceRoot;
    }

    public boolean getIsMultiInstanceRoot() {
        return isMultiInstanceRoot;
    }

    @Override
    public void setMultiInstanceRoot(boolean isMultiInstanceRoot) {
        this.isMultiInstanceRoot = isMultiInstanceRoot;
    }

    public void setIsMultiInstanceRoot(boolean isMultiInstanceRoot) {
        this.isMultiInstanceRoot = isMultiInstanceRoot;
    }

    @Override
    public boolean isCountEnabled() {
        return isCountEnabled;
    }

    public boolean getIsCountEnabled() {
        return isCountEnabled;
    }

    @Override
    public void setCountEnabled(boolean isCountEnabled) {
        this.isCountEnabled = isCountEnabled;
    }

    public void setIsCountEnabled(boolean isCountEnabled) {
        this.isCountEnabled = isCountEnabled;
    }

    @Override
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

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        if (localizedDescription != null && localizedDescription.length() > 0) {
            return localizedDescription;
        } else {
            return description;
        }
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    @Override
    public String getLocalizedDescription() {
        return localizedDescription;
    }

    @Override
    public void setLocalizedDescription(String localizedDescription) {
        this.localizedDescription = localizedDescription;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Date getLockTime() {
        return lockTime;
    }

    @Override
    public void setLockTime(Date lockTime) {
        this.lockTime = lockTime;
    }

    @Override
    public String getLockOwner() {
        return lockOwner;
    }

    @Override
    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    @Override
    public Map<String, Object> getProcessVariables() {
        Map<String, Object> variables = new HashMap<>();

        if (queryVariables != null) {
            for (VariableInstanceEntity variableInstance : queryVariables) {
                if (variableInstance.getId() != null && variableInstance.getTaskId() == null) {
                    variables.put(variableInstance.getName(), variableInstance.getValue());
                }
            }
        }

        // The variables from the cache have precedence
        if (variableInstances != null) {
            for (String variableName : variableInstances.keySet()) {
                variables.put(variableName, variableInstances.get(variableName).getValue());
            }
        }


        return variables;
    }

    @Override
    public List<VariableInstanceEntity> getQueryVariables() {
        if (queryVariables == null && Context.getCommandContext() != null) {
            queryVariables = new VariableInitializingList();
        }
        return queryVariables;
    }

    public void setQueryVariables(List<VariableInstanceEntity> queryVariables) {
        this.queryVariables = queryVariables;
    }

    public String getActivityName() {
        return activityName;
    }

    @Override
    public String getCurrentActivityName() {
        return activityName;
    }

    @Override
    public void setCurrentActivityName(String activityName) {
        this.activityName = activityName;
    }
    @Override
    public String getStartActivityId() {
        return startActivityId;
    }

    @Override
    public void setStartActivityId(String startActivityId) {
        this.startActivityId = startActivityId;
    }

    @Override
    public String getStartUserId() {
        return startUserId;
    }

    @Override
    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public int getEventSubscriptionCount() {
        return eventSubscriptionCount;
    }

    @Override
    public void setEventSubscriptionCount(int eventSubscriptionCount) {
        this.eventSubscriptionCount = eventSubscriptionCount;
    }

    @Override
    public int getTaskCount() {
        return taskCount;
    }

    @Override
    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    @Override
    public int getJobCount() {
        return jobCount;
    }

    @Override
    public void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    @Override
    public int getTimerJobCount() {
        return timerJobCount;
    }

    @Override
    public void setTimerJobCount(int timerJobCount) {
        this.timerJobCount = timerJobCount;
    }

    @Override
    public int getSuspendedJobCount() {
        return suspendedJobCount;
    }

    @Override
    public void setSuspendedJobCount(int suspendedJobCount) {
        this.suspendedJobCount = suspendedJobCount;
    }

    @Override
    public int getDeadLetterJobCount() {
        return deadLetterJobCount;
    }

    @Override
    public void setDeadLetterJobCount(int deadLetterJobCount) {
        this.deadLetterJobCount = deadLetterJobCount;
    }

    @Override
    public int getExternalWorkerJobCount() {
        return externalWorkerJobCount;
    }

    @Override
    public void setExternalWorkerJobCount(int externalWorkerJobCount) {
        this.externalWorkerJobCount = externalWorkerJobCount;
    }

    @Override
    public int getVariableCount() {
        return variableCount;
    }

    @Override
    public void setVariableCount(int variableCount) {
        this.variableCount = variableCount;
    }

    @Override
    public int getIdentityLinkCount() {
        return identityLinkCount;
    }

    @Override
    public void setIdentityLinkCount(int identityLinkCount) {
        this.identityLinkCount = identityLinkCount;
    }
    
    @Override
    public String getCallbackId() {
        return callbackId;
    }

    @Override
    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    @Override
    public String getCallbackType() {
        return callbackType;
    }

    @Override
    public void setCallbackType(String callbackType) {
        this.callbackType = callbackType;
    }

    @Override
    public String getReferenceId() {
        return referenceId;
    }

    @Override
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public String getReferenceType() {
        return referenceType;
    }

    @Override
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public void setPropagatedStageInstanceId(String propagatedStageInstanceId) {
        this.propagatedStageInstanceId = propagatedStageInstanceId;
    }

    @Override
    public String getPropagatedStageInstanceId() {
        return propagatedStageInstanceId;
    }

    protected String getRelatedActivityInstanceId(ExecutionEntity sourceExecution) {
        String activityInstanceId = null;
        if (CommandContextUtil.getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.FULL)) {
            ActivityInstanceEntity unfinishedActivityInstance = CommandContextUtil.getActivityInstanceEntityManager()
                .findUnfinishedActivityInstance(sourceExecution);
            if (unfinishedActivityInstance != null) {
                activityInstanceId = unfinishedActivityInstance.getId();
            }
        }
        return activityInstanceId;
    }

    protected void resolveProcessDefinitionInfo() {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration == null) {
            // We are outside of a command context so do not try to resolve anything
            return;
        }
        ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId, false, processEngineConfiguration);
        if (processDefinition == null) {
            throw new FlowableException("Cannot get process definition for id " + processDefinitionId + " for " + this);
        }

        this.processDefinitionKey = processDefinition.getKey();
        this.processDefinitionName = processDefinition.getName();
        this.processDefinitionVersion = processDefinition.getVersion();
        this.processDefinitionCategory = processDefinition.getCategory();
        this.deploymentId = processDefinition.getDeploymentId();
    }

    // toString /////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        StringBuilder strb;
        if (isProcessInstanceType()) {
            strb = new StringBuilder("ProcessInstance[" + getId() + "] - definition '" + getProcessDefinitionId() + "'");
        } else {
            strb = new StringBuilder();
            if (isScope) {
                strb.append("Scoped execution[ id '").append(getId());
            } else if (isMultiInstanceRoot) {
                strb.append("Multi instance root execution[ id '").append(getId());
            } else {
                strb.append("Execution[ id '").append(getId());
            }
            strb.append("' ]");
            strb.append(" - definition '").append(getProcessDefinitionId()).append("'");
            
            if (activityId != null) {
                strb.append(" - activity '").append(activityId).append("'");
            }
            if (parentId != null) {
                strb.append(" - parent '").append(parentId).append("'");
            }
        }

        if (StringUtils.isNotEmpty(tenantId)) {
            strb.append(" - tenantId '").append(tenantId).append("'");
        }
        return strb.toString();
    }


    @Override
    protected VariableServiceConfiguration getVariableServiceConfiguration() {
        return CommandContextUtil.getProcessEngineConfiguration().getVariableServiceConfiguration();
    }

}
