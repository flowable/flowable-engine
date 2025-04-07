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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceQueryImpl extends AbstractVariableQueryImpl<PlanItemInstanceQuery, PlanItemInstance> implements PlanItemInstanceQuery,
        CacheAwareQuery<PlanItemInstanceEntity> {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    protected String caseDefinitionId;
    protected String derivedCaseDefinitionId;
    protected String caseInstanceId;
    protected Set<String> caseInstanceIds;
    private List<List<String>> safeCaseInstanceIds;
    protected String stageInstanceId;
    protected String planItemInstanceId;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected List<String> planItemDefinitionTypes;
    protected String name;
    protected String state;
    protected Date createdBefore;
    protected Date createdAfter;
    protected Date lastAvailableBefore;
    protected Date lastAvailableAfter;
    protected Date lastUnavailableBefore;
    protected Date lastUnavailableAfter;
    protected Date lastEnabledBefore;
    protected Date lastEnabledAfter;
    protected Date lastDisabledBefore;
    protected Date lastDisabledAfter;
    protected Date lastStartedBefore;
    protected Date lastStartedAfter;
    protected Date lastSuspendedBefore;
    protected Date lastSuspendedAfter;
    protected Date completedBefore;
    protected Date completedAfter;
    protected Date terminatedBefore;
    protected Date terminatedAfter;
    protected Date occurredBefore;
    protected Date occurredAfter;
    protected Date exitBefore;
    protected Date exitAfter;
    protected Date endedBefore;
    protected Date endedAfter;
    protected boolean ended;
    protected boolean includeEnded;
    protected String startUserId;
    protected String assignee;
    protected String completedBy;
    protected String referenceId;
    protected String referenceType;
    protected boolean completable;
    protected boolean onlyStages;
    protected String entryCriterionId;
    protected String exitCriterionId;
    protected String formKey;
    protected String extraValue;
    protected String involvedUser;
    protected Collection<String> involvedGroups;
    private List<List<String>> safeInvolvedGroups;
    protected String tenantId;
    protected boolean withoutTenantId;
    protected String locale;
    protected boolean withLocalizationFallback;
    protected boolean includeLocalVariables;

    protected List<PlanItemInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected PlanItemInstanceQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    public PlanItemInstanceQueryImpl() {
        
    }
    
    public PlanItemInstanceQueryImpl(CommandContext commandContext, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandContext, cmmnEngineConfiguration.getVariableServiceConfiguration());
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    public PlanItemInstanceQueryImpl(CommandExecutor commandExecutor, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandExecutor, cmmnEngineConfiguration.getVariableServiceConfiguration());
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public PlanItemInstanceQuery caseDefinitionId(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Case definition id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionId = caseDefinitionId;
        } else {
            this.caseDefinitionId = caseDefinitionId;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery derivedCaseDefinitionId(String derivedCaseDefinitionId) {
        if (derivedCaseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Derived case definition id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.derivedCaseDefinitionId = derivedCaseDefinitionId;
        } else {
            this.derivedCaseDefinitionId = derivedCaseDefinitionId;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseInstanceId(String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("Case instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseInstanceId = caseInstanceId;
        } else {
            this.caseInstanceId = caseInstanceId;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseInstanceIds(Set<String> caseInstanceIds) {
        if (caseInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Set of case instance ids is null");
        }
        if (caseInstanceIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of case instance ids is empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.caseInstanceIds = caseInstanceIds;
        } else {
            this.caseInstanceIds = caseInstanceIds;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery stageInstanceId(String stageInstanceId) {
        if (stageInstanceId == null) {
            throw new FlowableIllegalArgumentException("Stage instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.stageInstanceId = stageInstanceId;
        } else {
            this.stageInstanceId = stageInstanceId;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceId(String planItemInstanceId) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("Plan Item instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.planItemInstanceId = planItemInstanceId;
        } else {
            this.planItemInstanceId = planItemInstanceId;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceElementId(String elementId) {
        if (elementId == null) {
            throw new FlowableIllegalArgumentException("Element id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.elementId = elementId;
        } else {
            this.elementId = elementId;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemDefinitionId(String planItemDefinitionId) {
        if (planItemDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Plan item definition id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.planItemDefinitionId = planItemDefinitionId;
        } else {
            this.planItemDefinitionId = planItemDefinitionId;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemDefinitionType(String planItemDefinitionType) {
        if (planItemDefinitionType == null) {
            throw new FlowableIllegalArgumentException("Plan item definition type is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.planItemDefinitionType = planItemDefinitionType;
        } else {
            this.planItemDefinitionType = planItemDefinitionType;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemDefinitionTypes(List<String> planItemDefinitionTypes) {
        if (planItemDefinitionTypes == null) {
            throw new FlowableIllegalArgumentException("Plan item definition types is null");
        }
        if (planItemDefinitionTypes.isEmpty()) {
            throw new FlowableIllegalArgumentException("Plan item definition types is empty");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.planItemDefinitionTypes = planItemDefinitionTypes;
        } else {
            this.planItemDefinitionTypes = planItemDefinitionTypes;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("Name is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.name = name;
        } else {
            this.name = name;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceState(String state) {
        if (state == null) {
            throw new FlowableIllegalArgumentException("State is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.state = state;
        } else {
            this.state = state;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateWaitingForRepetition() {
        return planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateActive() {
        return planItemInstanceState(PlanItemInstanceState.ACTIVE);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateEnabled() {
        return planItemInstanceState(PlanItemInstanceState.ENABLED);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateDisabled() {
        return planItemInstanceState(PlanItemInstanceState.DISABLED);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateAsyncActive() {
        return planItemInstanceState(PlanItemInstanceState.ASYNC_ACTIVE);
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStateAsyncActiveLeave() {
        return planItemInstanceState(PlanItemInstanceState.ASYNC_ACTIVE_LEAVE);
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStateAvailable() {
        return planItemInstanceState(PlanItemInstanceState.AVAILABLE);
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStateUnavailable() {
        return planItemInstanceState(PlanItemInstanceState.UNAVAILABLE);
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStateCompleted() {
        return planItemInstanceState(PlanItemInstanceState.COMPLETED);
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceStateTerminated() {
        return planItemInstanceState(PlanItemInstanceState.TERMINATED);
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCreatedBefore(Date createdBefore) {
        if (createdBefore == null) {
            throw new FlowableIllegalArgumentException("createdBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.createdBefore = createdBefore;
        } else {
            this.createdBefore = createdBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCreatedAfter(Date createdAfter) {
        if (createdAfter == null) {
            throw new FlowableIllegalArgumentException("createdAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.createdAfter = createdAfter;
        } else {
            this.createdAfter = createdAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastAvailableBefore(Date availableBefore) {
        if (availableBefore == null) {
            throw new FlowableIllegalArgumentException("availableBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastAvailableBefore = availableBefore;
        } else {
            this.lastAvailableBefore = availableBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastAvailableAfter(Date availableAfter) {
        if (availableAfter == null) {
            throw new FlowableIllegalArgumentException("availableAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastAvailableAfter = availableAfter;
        } else {
            this.lastAvailableAfter = availableAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastUnavailableBefore(Date unavailableBefore) {
        if (unavailableBefore == null) {
            throw new FlowableIllegalArgumentException("unavailableBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastUnavailableBefore = unavailableBefore;
        } else {
            this.lastUnavailableBefore = unavailableBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastUnavailableAfter(Date unavailableAfter) {
        if (unavailableAfter == null) {
            throw new FlowableIllegalArgumentException("unavailableAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastUnavailableAfter = unavailableAfter;
        } else {
            this.lastUnavailableAfter = unavailableAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastEnabledBefore(Date enabledBefore) {
        if (enabledBefore == null) {
            throw new FlowableIllegalArgumentException("enabledBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastEnabledBefore = enabledBefore;
        } else {
            this.lastEnabledBefore = enabledBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastEnabledAfter(Date enabledAfter) {
        if (enabledAfter == null) {
            throw new FlowableIllegalArgumentException("enabledAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastEnabledAfter = enabledAfter;
        } else {
            this.lastEnabledAfter = enabledAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastDisabledBefore(Date disabledBefore) {
        if (disabledBefore == null) {
            throw new FlowableIllegalArgumentException("disabledBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastDisabledBefore = disabledBefore;
        } else {
            this.lastDisabledBefore = disabledBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastDisabledAfter(Date disabledAfter) {
        if (disabledAfter == null) {
            throw new FlowableIllegalArgumentException("disabledAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastDisabledAfter = disabledAfter;
        } else {
            this.lastDisabledAfter = disabledAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastStartedBefore(Date startedBefore) {
        if (startedBefore == null) {
            throw new FlowableIllegalArgumentException("activatedBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastStartedBefore = startedBefore;
        } else {
            this.lastStartedBefore = startedBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastStartedAfter(Date startedAfter) {
        if (startedAfter == null) {
            throw new FlowableIllegalArgumentException("startedAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastStartedAfter = startedAfter;
        } else {
            this.lastStartedAfter = startedAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastSuspendedBefore(Date suspendedBefore) {
        if (suspendedBefore == null) {
            throw new FlowableIllegalArgumentException("suspendedBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastSuspendedBefore = suspendedBefore;
        } else {
            this.lastSuspendedBefore = suspendedBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceLastSuspendedAfter(Date suspendedAfter) {
        if (suspendedAfter == null) {
            throw new FlowableIllegalArgumentException("suspendedAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastSuspendedAfter = suspendedAfter;
        } else {
            this.lastSuspendedAfter = suspendedAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCompletedBefore(Date completedBefore) {
        if (completedBefore == null) {
            throw new FlowableIllegalArgumentException("completedBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.completedBefore = completedBefore;
        } else {
            this.completedBefore = completedBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCompletedAfter(Date completedAfter) {
        if (completedAfter == null) {
            throw new FlowableIllegalArgumentException("completedAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.completedAfter = completedAfter;
        } else {
            this.completedAfter = completedAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceOccurredBefore(Date occurredBefore) {
        if (occurredBefore == null) {
            throw new FlowableIllegalArgumentException("occurredBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.occurredBefore = occurredBefore;
        } else {
            this.occurredBefore = occurredBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceOccurredAfter(Date occurredAfter) {
        if (occurredAfter == null) {
            throw new FlowableIllegalArgumentException("occurredAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.occurredAfter = occurredAfter;
        } else {
            this.occurredAfter = occurredAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceTerminatedBefore(Date terminatedBefore) {
        if (terminatedBefore == null) {
            throw new FlowableIllegalArgumentException("terminatedBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.terminatedBefore = terminatedBefore;
        } else {
            this.terminatedBefore = terminatedBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceTerminatedAfter(Date terminatedAfter) {
        if (terminatedAfter == null) {
            throw new FlowableIllegalArgumentException("terminatedAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.terminatedAfter = terminatedAfter;
        } else {
            this.terminatedAfter = terminatedAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceExitBefore(Date exitBefore) {
        if (exitBefore == null) {
            throw new FlowableIllegalArgumentException("exitBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.exitBefore = exitBefore;
        } else {
            this.exitBefore = exitBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceExitAfter(Date exitAfter) {
        if (exitAfter == null) {
            throw new FlowableIllegalArgumentException("exitAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.exitAfter = exitAfter;
        } else {
            this.exitAfter = exitAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceEndedBefore(Date endedBefore) {
        if (endedBefore == null) {
            throw new FlowableIllegalArgumentException("endedBefore is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.endedBefore = endedBefore;
        } else {
            this.endedBefore = endedBefore;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceEndedAfter(Date endedAfter) {
        if (endedAfter == null) {
            throw new FlowableIllegalArgumentException("endedAfter is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.endedAfter = endedAfter;
        } else {
            this.endedAfter = endedAfter;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery ended() {
        if (inOrStatement) {
            this.currentOrQueryObject.ended = true;
            includeEnded = true;
        } else {
            this.ended = true;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery includeEnded() {
        if (inOrStatement) {
            throw new FlowableIllegalArgumentException("includeEnded is not allowed within an or query");
        } else {
            this.includeEnded = true;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceStartUserId(String startUserId) {
        if (startUserId == null) {
            throw new FlowableIllegalArgumentException("Start user id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.startUserId = startUserId;
        } else {
            this.startUserId = startUserId;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceAssignee(String assignee) {
        if (assignee == null) {
            throw new FlowableIllegalArgumentException("assignee is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.assignee = assignee;
        } else {
            this.assignee = assignee;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceCompletedBy(String completedBy) {
        if (completedBy == null) {
            throw new FlowableIllegalArgumentException("completedBy is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.completedBy = completedBy;
        } else {
            this.completedBy = completedBy;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceReferenceId(String referenceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.referenceId = referenceId;
        } else {
            this.referenceId = referenceId;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceReferenceType(String referenceType) {
        if (inOrStatement) {
            this.currentOrQueryObject.referenceType = referenceType;
        } else {
            this.referenceType = referenceType;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceCompletable() {
        if (inOrStatement) {
            this.currentOrQueryObject.completable = true;
        } else {
            this.completable = true;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery onlyStages() {
        if (inOrStatement) {
            this.currentOrQueryObject.onlyStages = true;
        } else {
            this.onlyStages = true;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceEntryCriterionId(String entryCriterionId) {
        if (entryCriterionId == null) {
            throw new FlowableIllegalArgumentException("EntryCriterionId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.entryCriterionId = entryCriterionId;
        } else {
            this.entryCriterionId = entryCriterionId;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceExitCriterionId(String exitCriterionId) {
        if (exitCriterionId == null) {
            throw new FlowableIllegalArgumentException("ExitCriterionId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.exitCriterionId = exitCriterionId;
        } else {
            this.exitCriterionId = exitCriterionId;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceFormKey(String formKey) {
        if (formKey == null) {
            throw new FlowableIllegalArgumentException("formKey is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.formKey = formKey;
        } else {
            this.formKey = formKey;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceExtraValue(String extraValue) {
        if (extraValue == null) {
            throw new FlowableIllegalArgumentException("extraValue is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.extraValue = extraValue;
        } else {
            this.extraValue = extraValue;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery involvedUser(String involvedUser) {
        if (involvedUser == null) {
            throw new FlowableIllegalArgumentException("involvedUser is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedUser = involvedUser;
        } else {
            this.involvedUser = involvedUser;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery involvedGroups(Collection<String> involvedGroups) {
        if (involvedGroups == null) {
            throw new FlowableIllegalArgumentException("involvedGroups is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedGroups = involvedGroups;
        } else {
            this.involvedGroups = involvedGroups;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery planItemInstanceTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }
    
    @Override
    public PlanItemInstanceQuery planItemInstanceWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueEquals(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueEquals(name, value);
        } else {
            super.variableValueEquals(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueEquals(Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueEquals(value);
        } else {
            super.variableValueEquals(value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueEqualsIgnoreCase(name, value);
        } else {
            super.variableValueEqualsIgnoreCase(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value);
        } else {
            super.variableValueNotEqualsIgnoreCase(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueNotEquals(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueNotEquals(name, value);
        } else {
            super.variableValueNotEquals(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueLikeIgnoreCase(name, value);
        } else {
            super.variableValueLikeIgnoreCase(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueLike(String name, String value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueLike(name, value);
        } else {
            super.variableValueLike(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueLessThanOrEqual(name, value);
        } else {
            super.variableValueLessThanOrEqual(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueLessThan(name, value);
        } else {
            super.variableValueLessThan(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueGreaterThanOrEqual(name, value);
        } else {

            super.variableValueGreaterThanOrEqual(name, value);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueGreaterThan(name, value);
        } else {
            super.variableValueGreaterThan(name, value);
        }
        return this;
    }
    @Override
    public PlanItemInstanceQuery variableExists(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableExists(name);
        } else {
            super.variableExists(name);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery variableNotExists(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableNotExists(name);
        } else {
            super.variableNotExists(name);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueEquals(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueEquals(name, value, false);
        } else {
            variableValueEquals(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueEquals(Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueEquals(value, false);
        } else {
            variableValueEquals(value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
        } else {
            variableValueEqualsIgnoreCase(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueNotEquals(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueNotEquals(name, value, false);
        } else {
            variableValueNotEquals(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
        } else {
            variableValueNotEqualsIgnoreCase(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueGreaterThan(name, value, false);
        } else {
            variableValueGreaterThan(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
        } else {
            variableValueGreaterThanOrEqual(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueLessThan(name, value, false);
        } else {
            variableValueLessThan(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
        } else {
            variableValueLessThanOrEqual(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueLike(String name, String value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueLike(name, value, false);
        } else {
            variableValueLike(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
        } else {
            variableValueLikeIgnoreCase(name, value, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableExists(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableExists(name, false);
        } else {
            variableExists(name, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery caseVariableNotExists(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.variableNotExists(name, false);
        } else {
            variableNotExists(name, false);
        }
        return this;
    }

    @Override
    public PlanItemInstanceQuery or() {
        if (inOrStatement) {
            throw new FlowableIllegalArgumentException("The query is already in an or statement");
        }

        inOrStatement = true;
        if (commandContext != null) {
            currentOrQueryObject = new PlanItemInstanceQueryImpl(commandContext, cmmnEngineConfiguration);
        } else {
            currentOrQueryObject = new PlanItemInstanceQueryImpl(commandExecutor, cmmnEngineConfiguration);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public PlanItemInstanceQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableIllegalArgumentException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    @Override
    public PlanItemInstanceQuery includeLocalVariables() {
        this.includeLocalVariables = true;
        return this;
    }

    @Override
    public PlanItemInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public PlanItemInstanceQuery withLocalizationFallback() {
        this.withLocalizationFallback = true;
        return this;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        return cmmnEngineConfiguration.getPlanItemInstanceEntityManager().countByCriteria(this);
    }

    @Override
    public List<PlanItemInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        List<PlanItemInstance> planItems = null;
        if (includeLocalVariables) {
            planItems = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findWithVariablesByCriteria(this);

        } else {
            planItems = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findByCriteria(this);
        }

        if (cmmnEngineConfiguration.getPlanItemLocalizationManager() != null) {
            for (PlanItemInstance planItemInstance : planItems) {
                cmmnEngineConfiguration.getPlanItemLocalizationManager().localize(planItemInstance, locale, withLocalizationFallback);
            }
        }

        return planItems;
    }
    
    @Override
    public PlanItemInstanceQuery orderByCreateTime() {
        this.orderProperty = PlanItemInstanceQueryProperty.CREATE_TIME;
        return this;
    }

    @Override
    public PlanItemInstanceQuery orderByEndTime() {
        this.orderProperty = PlanItemInstanceQueryProperty.END_TIME;
        return this;
    }

    @Override
    public PlanItemInstanceQuery orderByName() {
        this.orderProperty = PlanItemInstanceQueryProperty.NAME;
        return this;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    public String getDerivedCaseDefinitionId() {
        return derivedCaseDefinitionId;
    }
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    public String getStageInstanceId() {
        return stageInstanceId;
    }
    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }
    @Override
    public String getId() {
        return planItemInstanceId;
    }
    public String getElementId() {
        return elementId;
    }
    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }
    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }
    public List<String> getPlanItemDefinitionTypes() {
        return planItemDefinitionTypes;
    }
    public String getName() {
        return name;
    }
    public String getState() {
        return state;
    }
    public Date getCreatedBefore() {
        return createdBefore;
    }
    public Date getCreatedAfter() {
        return createdAfter;
    }
    public Date getLastAvailableBefore() {
        return lastAvailableBefore;
    }
    public Date getLastAvailableAfter() {
        return lastAvailableAfter;
    }
    public Date getLastUnavailableBefore() {
        return lastUnavailableBefore;
    }
    public Date getLastUnavailableAfter() {
        return lastUnavailableAfter;
    }
    public Date getLastEnabledBefore() {
        return lastEnabledBefore;
    }
    public Date getLastEnabledAfter() {
        return lastEnabledAfter;
    }
    public Date getLastDisabledBefore() {
        return lastDisabledBefore;
    }
    public Date getLastDisabledAfter() {
        return lastDisabledAfter;
    }
    public Date getLastStartedBefore() {
        return lastStartedBefore;
    }
    public Date getLastStartedAfter() {
        return lastStartedAfter;
    }
    public Date getLastSuspendedBefore() {
        return lastSuspendedBefore;
    }
    public Date getLastSuspendedAfter() {
        return lastSuspendedAfter;
    }
    public Date getCompletedBefore() {
        return completedBefore;
    }
    public Date getCompletedAfter() {
        return completedAfter;
    }
    public Date getTerminatedBefore() {
        return terminatedBefore;
    }
    public Date getTerminatedAfter() {
        return terminatedAfter;
    }
    public Date getOccurredBefore() {
        return occurredBefore;
    }
    public Date getOccurredAfter() {
        return occurredAfter;
    }
    public Date getExitBefore() {
        return exitBefore;
    }
    public Date getExitAfter() {
        return exitAfter;
    }
    public Date getEndedBefore() {
        return endedBefore;
    }
    public Date getEndedAfter() {
        return endedAfter;
    }
    public boolean isEnded() {
        return ended;
    }
    public boolean isIncludeEnded() {
        return includeEnded;
    }
    public String getStartUserId() {
        return startUserId;
    }
    public String getAssignee() {
        return assignee;
    }
    public String getCompletedBy() {
        return completedBy;
    }
    public String getReferenceId() {
        return referenceId;
    }
    public String getReferenceType() {
        return referenceType;
    }
    public boolean isCompletable() {
        return completable;
    }
    public boolean isOnlyStages() {
        return onlyStages;
    }
    public String getEntryCriterionId() {
        return entryCriterionId;
    }
    public String getExitCriterionId() {
        return exitCriterionId;
    }
    public String getFormKey() {
        return formKey;
    }
    public String getExtraValue() {
        return extraValue;
    }
    public String getInvolvedUser() {
        return involvedUser;
    }
    public Collection<String> getInvolvedGroups() {
        return involvedGroups;
    }
    public String getTenantId() {
        return tenantId;
    }
    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public boolean isIncludeLocalVariables() {
        return includeLocalVariables;
    }
    public List<List<String>> getSafeInvolvedGroups() {
        return safeInvolvedGroups;
    }

    public void setSafeInvolvedGroups(List<List<String>> safeInvolvedGroups) {
        this.safeInvolvedGroups = safeInvolvedGroups;
    }

    @Override
    protected void ensureVariablesInitialized() {
        super.ensureVariablesInitialized();
        for (PlanItemInstanceQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }

    public List<PlanItemInstanceQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

    public List<List<String>> getSafeCaseInstanceIds() {
        return safeCaseInstanceIds;
    }

    public void setSafeCaseInstanceIds(List<List<String>> safeProcessInstanceIds) {
        this.safeCaseInstanceIds = safeProcessInstanceIds;
    }

    public Collection<String> getCaseInstanceIds() {
        return caseInstanceIds;
    }
}
