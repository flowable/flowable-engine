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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tom Baeyens
 * @author Tijs Rademakers
 * @author Falko Menge
 * @author Bernd Ruecker
 * @author Joram Barrez
 */
public class HistoricProcessInstanceQueryImpl extends AbstractVariableQueryImpl<HistoricProcessInstanceQuery, HistoricProcessInstance> implements HistoricProcessInstanceQuery {

    private static final long serialVersionUID = 1L;
    
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String businessKey;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected boolean finished;
    protected boolean unfinished;
    protected boolean deleted;
    protected boolean notDeleted;
    protected String startedBy;
    protected String superProcessInstanceId;
    protected boolean excludeSubprocesses;
    protected List<String> processDefinitionKeyIn;
    protected List<String> processKeyNotIn;
    protected Date startedBefore;
    protected Date startedAfter;
    protected Date finishedBefore;
    protected Date finishedAfter;
    protected String processDefinitionKey;
    protected String processDefinitionCategory;
    protected String processDefinitionName;
    protected Integer processDefinitionVersion;
    protected Set<String> processInstanceIds;
    protected String involvedUser;
    protected boolean includeProcessVariables;
    protected Integer processInstanceVariablesLimit;
    protected boolean withJobException;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String callbackId;
    protected String callbackType;
    protected String locale;
    protected boolean withLocalizationFallback;
    protected List<HistoricProcessInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected HistoricProcessInstanceQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    public HistoricProcessInstanceQueryImpl() {
    }

    public HistoricProcessInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public HistoricProcessInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public HistoricProcessInstanceQueryImpl processInstanceId(String processInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceId = processInstanceId;
        } else {
            this.processInstanceId = processInstanceId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceIds(Set<String> processInstanceIds) {
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
    public HistoricProcessInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionKey(String processDefinitionKey) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKey = processDefinitionKey;
        } else {
            this.processDefinitionKey = processDefinitionKey;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionKeyIn(List<String> processDefinitionKeys) {
        if (inOrStatement) {
            currentOrQueryObject.processDefinitionKeyIn = processDefinitionKeys;
        } else {
            this.processDefinitionKeyIn = processDefinitionKeys;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionCategory(String processDefinitionCategory) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategory = processDefinitionCategory;
        } else {
            this.processDefinitionCategory = processDefinitionCategory;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionName(String processDefinitionName) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionName = processDefinitionName;
        } else {
            this.processDefinitionName = processDefinitionName;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionVersion(Integer processDefinitionVersion) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionVersion = processDefinitionVersion;
        } else {
            this.processDefinitionVersion = processDefinitionVersion;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceBusinessKey(String businessKey) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessKey = businessKey;
        } else {
            this.businessKey = businessKey;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery deploymentId(String deploymentId) {
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentId = deploymentId;
        } else {
            this.deploymentId = deploymentId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery deploymentIdIn(List<String> deploymentIds) {
        if (inOrStatement) {
            currentOrQueryObject.deploymentIds = deploymentIds;
        } else {
            this.deploymentIds = deploymentIds;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery finished() {
        if (inOrStatement) {
            this.currentOrQueryObject.finished = true;
        } else {
            this.finished = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery unfinished() {
        if (inOrStatement) {
            this.currentOrQueryObject.unfinished = true;
        } else {
            this.unfinished = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery deleted() {
        if (inOrStatement) {
            this.currentOrQueryObject.deleted = true;
        } else {
            this.deleted = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery notDeleted() {
        if (inOrStatement) {
            this.currentOrQueryObject.notDeleted = true;
        } else {
            this.notDeleted = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery startedBy(String startedBy) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedBy = startedBy;
        } else {
            this.startedBy = startedBy;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionKeyNotIn(List<String> processDefinitionKeys) {
        if (inOrStatement) {
            this.currentOrQueryObject.processKeyNotIn = processDefinitionKeys;
        } else {
            this.processKeyNotIn = processDefinitionKeys;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery startedAfter(Date startedAfter) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedAfter = startedAfter;
        } else {
            this.startedAfter = startedAfter;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery startedBefore(Date startedBefore) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedBefore = startedBefore;
        } else {
            this.startedBefore = startedBefore;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery finishedAfter(Date finishedAfter) {
        if (inOrStatement) {
            this.currentOrQueryObject.finishedAfter = finishedAfter;
        } else {
            this.finishedAfter = finishedAfter;
            this.finished = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery finishedBefore(Date finishedBefore) {
        if (inOrStatement) {
            this.currentOrQueryObject.finishedBefore = finishedBefore;
        } else {
            this.finishedBefore = finishedBefore;
            this.finished = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery superProcessInstanceId(String superProcessInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.superProcessInstanceId = superProcessInstanceId;
        } else {
            this.superProcessInstanceId = superProcessInstanceId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery excludeSubprocesses(boolean excludeSubprocesses) {
        if (inOrStatement) {
            this.currentOrQueryObject.excludeSubprocesses = excludeSubprocesses;
        } else {
            this.excludeSubprocesses = excludeSubprocesses;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery involvedUser(String involvedUser) {
        if (inOrStatement) {
            this.currentOrQueryObject.involvedUser = involvedUser;
        } else {
            this.involvedUser = involvedUser;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery includeProcessVariables() {
        this.includeProcessVariables = true;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery limitProcessInstanceVariables(Integer processInstanceVariablesLimit) {
        this.processInstanceVariablesLimit = processInstanceVariablesLimit;
        return this;
    }

    public Integer getProcessInstanceVariablesLimit() {
        return processInstanceVariablesLimit;
    }

    @Override
    public HistoricProcessInstanceQuery withJobException() {
        this.withJobException = true;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceTenantId(String tenantId) {
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
    public HistoricProcessInstanceQuery processInstanceTenantIdLike(String tenantIdLike) {
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
    public HistoricProcessInstanceQuery processInstanceWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceName(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.name = name;
        } else {
            this.name = name;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceNameLike(String nameLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.nameLike = nameLike;
        } else {
            this.nameLike = nameLike;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        } else {
            this.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processInstanceCallbackId(String callbackId) {
        if (inOrStatement) {
            currentOrQueryObject.callbackId = callbackId;
        } else {
            this.callbackId = callbackId;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processInstanceCallbackType(String callbackType) {
        if (inOrStatement) {
            currentOrQueryObject.callbackType = callbackType;
        } else {
            this.callbackType = callbackType;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableValue, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery variableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
            return this;
        } else {
            return variableValueGreaterThan(name, value, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
            return this;
        } else {
            return variableValueLessThan(name, value, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, false);
            return this;
        } else {
            return variableValueLike(name, value, false);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, false);
            return this;
        } else {
            return variableExists(name, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery variableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, false);
            return this;
        } else {
            return variableNotExists(name, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery withLocalizationFallback() {
        withLocalizationFallback = true;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        currentOrQueryObject = new HistoricProcessInstanceQueryImpl();
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceBusinessKey() {
        return orderBy(HistoricProcessInstanceQueryProperty.BUSINESS_KEY);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceDuration() {
        return orderBy(HistoricProcessInstanceQueryProperty.DURATION);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceStartTime() {
        return orderBy(HistoricProcessInstanceQueryProperty.START_TIME);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceEndTime() {
        return orderBy(HistoricProcessInstanceQueryProperty.END_TIME);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessDefinitionId() {
        return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_ID);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceId() {
        return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_INSTANCE_ID_);
    }

    @Override
    public HistoricProcessInstanceQuery orderByTenantId() {
        return orderBy(HistoricProcessInstanceQueryProperty.TENANT_ID);
    }

    public String getMssqlOrDB2OrderBy() {
        String specialOrderBy = super.getOrderByColumns();
        if (specialOrderBy != null && specialOrderBy.length() > 0) {
            specialOrderBy = specialOrderBy.replace("RES.", "TEMPRES_");
            specialOrderBy = specialOrderBy.replace("VAR.", "TEMPVAR_");
        }
        return specialOrderBy;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();
        return CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext).findHistoricProcessInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<HistoricProcessInstance> executeList(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();
        List<HistoricProcessInstance> results = null;
        if (includeProcessVariables) {
            results = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext).findHistoricProcessInstancesAndVariablesByQueryCriteria(this);
        } else {
            results = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext).findHistoricProcessInstancesByQueryCriteria(this);
        }

        if (CommandContextUtil.getProcessEngineConfiguration().getPerformanceSettings().isEnableLocalization()) {
            for (HistoricProcessInstance processInstance : results) {
                localize(processInstance, commandContext);
            }
        }

        return results;
    }

    protected void localize(HistoricProcessInstance processInstance, CommandContext commandContext) {
        HistoricProcessInstanceEntity processInstanceEntity = (HistoricProcessInstanceEntity) processInstance;
        processInstanceEntity.setLocalizedName(null);
        processInstanceEntity.setLocalizedDescription(null);

        if (locale != null && processInstance.getProcessDefinitionId() != null) {
            ProcessDefinition processDefinition = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDeploymentManager().findDeployedProcessDefinitionById(processInstanceEntity.getProcessDefinitionId());
            ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, processDefinition.getKey(),
                    processInstanceEntity.getProcessDefinitionId(), withLocalizationFallback);

            if (languageNode != null) {
                JsonNode languageNameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                if (languageNameNode != null && !languageNameNode.isNull()) {
                    processInstanceEntity.setLocalizedName(languageNameNode.asText());
                }

                JsonNode languageDescriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                if (languageDescriptionNode != null && !languageDescriptionNode.isNull()) {
                    processInstanceEntity.setLocalizedDescription(languageDescriptionNode.asText());
                }
            }
        }
    }

    @Override
    protected void ensureVariablesInitialized() {
        super.ensureVariablesInitialized();

        for (HistoricProcessInstanceQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }

    @Override
    protected void checkQueryOk() {
        super.checkQueryOk();

        if (includeProcessVariables) {
            this.orderBy(HistoricProcessInstanceQueryProperty.INCLUDED_VARIABLE_TIME).asc();
        }
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public boolean isOpen() {
        return unfinished;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public List<String> getProcessDefinitionKeyIn() {
        return processDefinitionKeyIn;
    }

    public String getProcessDefinitionIdLike() {
        return processDefinitionKey + ":%:%";
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public Set<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public boolean isExcludeSubprocesses() {
        return excludeSubprocesses;
    }

    public List<String> getProcessKeyNotIn() {
        return processKeyNotIn;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public Date getFinishedAfter() {
        return finishedAfter;
    }

    public Date getFinishedBefore() {
        return finishedBefore;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public List<String> getDeploymentIds() {
        return deploymentIds;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isUnfinished() {
        return unfinished;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isNotDeleted() {
        return notDeleted;
    }

    public boolean isIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public boolean isWithException() {
        return withJobException;
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

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }
    
    public String getCallbackId() {
        return callbackId;
    }

    public String getCallbackType() {
        return callbackType;
    }

    public List<HistoricProcessInstanceQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }
}
