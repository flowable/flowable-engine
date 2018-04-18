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

package org.flowable.engine.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Daniel Meyer
 */
public class ProcessInstanceQueryImpl extends AbstractVariableQueryImpl<ProcessInstanceQuery, ProcessInstance> implements ProcessInstanceQuery, Serializable {

    private static final long serialVersionUID = 1L;
    protected String executionId;
    protected String businessKey;
    protected boolean includeChildExecutionsWithBusinessKeyQuery;
    protected String processDefinitionId;
    protected Set<String> processDefinitionIds;
    protected String processDefinitionCategory;
    protected String processDefinitionName;
    protected Integer processDefinitionVersion;
    protected Set<String> processInstanceIds;
    protected String processDefinitionKey;
    protected Set<String> processDefinitionKeys;
    protected String processDefinitionEngineVersion;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected String superProcessInstanceId;
    protected String subProcessInstanceId;
    protected boolean excludeSubprocesses;
    protected String involvedUser;
    protected SuspensionState suspensionState;
    protected boolean includeProcessVariables;
    protected Integer processInstanceVariablesLimit;
    protected boolean withJobException;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String callbackId;
    protected String callbackType;
    protected String locale;
    protected boolean withLocalizationFallback;

    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    protected List<ProcessInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected ProcessInstanceQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    protected Date startedBefore;
    protected Date startedAfter;
    protected String startedBy;

    // Unused, see dynamic query
    protected String activityId;
    protected List<EventSubscriptionQueryValue> eventSubscriptions;
    protected boolean onlyChildExecutions;
    protected boolean onlyProcessInstanceExecutions;
    protected boolean onlySubProcessExecutions;
    protected String rootProcessInstanceId;

    public ProcessInstanceQueryImpl() {
    }

    public ProcessInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public ProcessInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public ProcessInstanceQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Process instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.executionId = processInstanceId;
        } else {
            this.executionId = processInstanceId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceIds(Set<String> processInstanceIds) {
        if (processInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Set of process instance ids is null");
        }
        if (processInstanceIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of process instance ids is empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceIds = processInstanceIds;
        } else {
            this.processInstanceIds = processInstanceIds;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceBusinessKey(String businessKey) {
        if (businessKey == null) {
            throw new FlowableIllegalArgumentException("Business key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.businessKey = businessKey;
        } else {
            this.businessKey = businessKey;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceBusinessKey(String businessKey, String processDefinitionKey) {
        if (businessKey == null) {
            throw new FlowableIllegalArgumentException("Business key is null");
        }
        if (inOrStatement) {
            throw new FlowableIllegalArgumentException("This method is not supported in an OR statement");
        }

        this.businessKey = businessKey;
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("process instance tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("process instance tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLike = tenantIdLike;
        } else {
            this.tenantIdLike = tenantIdLike;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionCategory(String processDefinitionCategory) {
        if (processDefinitionCategory == null) {
            throw new FlowableIllegalArgumentException("Process definition category is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategory = processDefinitionCategory;
        } else {
            this.processDefinitionCategory = processDefinitionCategory;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionName(String processDefinitionName) {
        if (processDefinitionName == null) {
            throw new FlowableIllegalArgumentException("Process definition name is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionName = processDefinitionName;
        } else {
            this.processDefinitionName = processDefinitionName;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionVersion(Integer processDefinitionVersion) {
        if (processDefinitionVersion == null) {
            throw new FlowableIllegalArgumentException("Process definition version is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionVersion = processDefinitionVersion;
        } else {
            this.processDefinitionVersion = processDefinitionVersion;
        }
        return this;
    }

    @Override
    public ProcessInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Process definition id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionIds(Set<String> processDefinitionIds) {
        if (processDefinitionIds == null) {
            throw new FlowableIllegalArgumentException("Set of process definition ids is null");
        }
        if (processDefinitionIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of process definition ids is empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionIds = processDefinitionIds;
        } else {
            this.processDefinitionIds = processDefinitionIds;
        }
        return this;
    }

    @Override
    public ProcessInstanceQueryImpl processDefinitionKey(String processDefinitionKey) {
        if (processDefinitionKey == null) {
            throw new FlowableIllegalArgumentException("Process definition key is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKey = processDefinitionKey;
        } else {
            this.processDefinitionKey = processDefinitionKey;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionKeys(Set<String> processDefinitionKeys) {
        if (processDefinitionKeys == null) {
            throw new FlowableIllegalArgumentException("Set of process definition keys is null");
        }
        if (processDefinitionKeys.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of process definition keys is empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeys = processDefinitionKeys;
        } else {
            this.processDefinitionKeys = processDefinitionKeys;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionEngineVersion(String processDefinitionEngineVersion) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionEngineVersion = processDefinitionEngineVersion;
        } else {
            this.processDefinitionEngineVersion = processDefinitionEngineVersion;
        }
        return this;
    }

    @Override
    public ProcessInstanceQueryImpl deploymentId(String deploymentId) {
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentId = deploymentId;
        } else {
            this.deploymentId = deploymentId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQueryImpl deploymentIdIn(List<String> deploymentIds) {
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentIds = deploymentIds;
        } else {
            this.deploymentIds = deploymentIds;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery superProcessInstanceId(String superProcessInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.superProcessInstanceId = superProcessInstanceId;
        } else {
            this.superProcessInstanceId = superProcessInstanceId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery subProcessInstanceId(String subProcessInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.subProcessInstanceId = subProcessInstanceId;
        } else {
            this.subProcessInstanceId = subProcessInstanceId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery excludeSubprocesses(boolean excludeSubprocesses) {
        if (inOrStatement) {
            this.currentOrQueryObject.excludeSubprocesses = excludeSubprocesses;
        } else {
            this.excludeSubprocesses = excludeSubprocesses;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery involvedUser(String involvedUser) {
        if (involvedUser == null) {
            throw new FlowableIllegalArgumentException("Involved user is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.involvedUser = involvedUser;
        } else {
            this.involvedUser = involvedUser;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery active() {
        if (inOrStatement) {
            this.currentOrQueryObject.suspensionState = SuspensionState.ACTIVE;
        } else {
            this.suspensionState = SuspensionState.ACTIVE;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery suspended() {
        if (inOrStatement) {
            this.currentOrQueryObject.suspensionState = SuspensionState.SUSPENDED;
        } else {
            this.suspensionState = SuspensionState.SUSPENDED;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery includeProcessVariables() {
        this.includeProcessVariables = true;
        return this;
    }

    @Override
    public ProcessInstanceQuery limitProcessInstanceVariables(Integer processInstanceVariablesLimit) {
        this.processInstanceVariablesLimit = processInstanceVariablesLimit;
        return this;
    }

    public Integer getProcessInstanceVariablesLimit() {
        return processInstanceVariablesLimit;
    }

    @Override
    public ProcessInstanceQuery withJobException() {
        this.withJobException = true;
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceName(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.name = name;
        } else {
            this.name = name;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceNameLike(String nameLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.nameLike = nameLike;
        } else {
            this.nameLike = nameLike;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        } else {
            this.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceCallbackId(String callbackId) {
        if (inOrStatement) {
            this.currentOrQueryObject.callbackId = callbackId;
        } else {
            this.callbackId = callbackId;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceCallbackType(String callbackType) {
        if (inOrStatement) {
            this.currentOrQueryObject.callbackType = callbackType;
        } else {
            this.callbackType = callbackType;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        currentOrQueryObject = new ProcessInstanceQueryImpl();
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public ProcessInstanceQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    @Override
    public ProcessInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableValue, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
            return this;
        } else {
            return variableValueGreaterThan(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
            return this;
        } else {
            return variableValueLessThan(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, false);
            return this;
        } else {
            return variableValueLike(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, false);
        }
    }
    
    @Override
    public ProcessInstanceQuery variableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, false);
            return this;
        } else {
            return variableExists(name, false);
        }
    }
    
    @Override
    public ProcessInstanceQuery variableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, false);
            return this;
        } else {
            return variableNotExists(name, false);
        }
    }

    @Override
    public ProcessInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public ProcessInstanceQuery withLocalizationFallback() {
        withLocalizationFallback = true;
        return this;
    }

    @Override
    public ProcessInstanceQuery startedBefore(Date beforeTime) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedBefore = beforeTime;
        } else {
            this.startedBefore = beforeTime;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery startedAfter(Date afterTime) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedAfter = afterTime;
        } else {
            this.startedAfter = afterTime;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery startedBy(String userId) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedBy = userId;
        } else {
            this.startedBy = userId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery orderByProcessInstanceId() {
        this.orderProperty = ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID;
        return this;
    }

    @Override
    public ProcessInstanceQuery orderByProcessDefinitionId() {
        this.orderProperty = ProcessInstanceQueryProperty.PROCESS_DEFINITION_ID;
        return this;
    }

    @Override
    public ProcessInstanceQuery orderByProcessDefinitionKey() {
        this.orderProperty = ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY;
        return this;
    }

    @Override
    public ProcessInstanceQuery orderByTenantId() {
        this.orderProperty = ProcessInstanceQueryProperty.TENANT_ID;
        return this;
    }

    public String getMssqlOrDB2OrderBy() {
        String specialOrderBy = super.getOrderByColumns();
        if (specialOrderBy != null && specialOrderBy.length() > 0) {
            specialOrderBy = specialOrderBy.replace("RES.", "TEMPRES_");
            specialOrderBy = specialOrderBy.replace("ProcessDefinitionKey", "TEMPP_KEY_");
            specialOrderBy = specialOrderBy.replace("ProcessDefinitionId", "TEMPP_ID_");
        }
        return specialOrderBy;
    }

    // results /////////////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();
        return CommandContextUtil.getExecutionEntityManager(commandContext).findProcessInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<ProcessInstance> executeList(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();
        List<ProcessInstance> processInstances = null;
        if (includeProcessVariables) {
            processInstances = CommandContextUtil.getExecutionEntityManager(commandContext).findProcessInstanceAndVariablesByQueryCriteria(this);
        } else {
            processInstances = CommandContextUtil.getExecutionEntityManager(commandContext).findProcessInstanceByQueryCriteria(this);
        }

        if (CommandContextUtil.getProcessEngineConfiguration().getPerformanceSettings().isEnableLocalization()) {
            for (ProcessInstance processInstance : processInstances) {
                localize(processInstance);
            }
        }

        return processInstances;
    }

    @Override
    protected void ensureVariablesInitialized() {
        super.ensureVariablesInitialized();

        for (ProcessInstanceQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }

    protected void localize(ProcessInstance processInstance) {
        ExecutionEntity processInstanceExecution = (ExecutionEntity) processInstance;
        processInstanceExecution.setLocalizedName(null);
        processInstanceExecution.setLocalizedDescription(null);

        if (locale != null) {
            String processDefinitionId = processInstanceExecution.getProcessDefinitionId();
            if (processDefinitionId != null) {
                ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, processInstanceExecution.getProcessDefinitionKey(), processDefinitionId, withLocalizationFallback);
                if (languageNode != null) {
                    JsonNode languageNameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                    if (languageNameNode != null && !languageNameNode.isNull()) {
                        processInstanceExecution.setLocalizedName(languageNameNode.asText());
                    }

                    JsonNode languageDescriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                    if (languageDescriptionNode != null && !languageDescriptionNode.isNull()) {
                        processInstanceExecution.setLocalizedDescription(languageDescriptionNode.asText());
                    }
                }
            }
        }
    }

    // getters /////////////////////////////////////////////////////////////////

    public boolean getOnlyProcessInstances() {
        return true; // See dynamic query in runtime.mapping.xml
    }

    public String getProcessInstanceId() {
        return executionId;
    }

    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    public Set<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public boolean isIncludeChildExecutionsWithBusinessKeyQuery() {
        return includeChildExecutionsWithBusinessKeyQuery;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public Set<String> getProcessDefinitionIds() {
        return processDefinitionIds;
    }

    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public Set<String> getProcessDefinitionKeys() {
        return processDefinitionKeys;
    }

    public String getProcessDefinitionEngineVersion() {
        return processDefinitionEngineVersion;
    }

    public String getActivityId() {
        return null; // Unused, see dynamic query
    }

    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public String getSubProcessInstanceId() {
        return subProcessInstanceId;
    }

    public boolean isExcludeSubprocesses() {
        return excludeSubprocesses;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public SuspensionState getSuspensionState() {
        return suspensionState;
    }

    public void setSuspensionState(SuspensionState suspensionState) {
        this.suspensionState = suspensionState;
    }

    public List<EventSubscriptionQueryValue> getEventSubscriptions() {
        return eventSubscriptions;
    }

    public void setEventSubscriptions(List<EventSubscriptionQueryValue> eventSubscriptions) {
        this.eventSubscriptions = eventSubscriptions;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNameLike(String nameLike) {
        this.nameLike = nameLike;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public List<String> getDeploymentIds() {
        return deploymentIds;
    }

    public boolean isIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public boolean iswithException() {
        return withJobException;
    }

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }
    
    public String getCallbackId() {
        return callbackId;
    }

    public String getCallbackType() {
        return callbackType;
    }

    public List<ProcessInstanceQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

    /**
     * Methods needed for ibatis because of re-use of query-xml for executions. ExecutionQuery contains a parentId property.
     */

    public String getParentId() {
        return null;
    }

    public boolean isOnlyChildExecutions() {
        return onlyChildExecutions;
    }

    public boolean isOnlyProcessInstanceExecutions() {
        return onlyProcessInstanceExecutions;
    }

    public boolean isOnlySubProcessExecutions() {
        return onlySubProcessExecutions;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public void setStartedBefore(Date startedBefore) {
        this.startedBefore = startedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public void setStartedAfter(Date startedAfter) {
        this.startedAfter = startedAfter;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }
}
