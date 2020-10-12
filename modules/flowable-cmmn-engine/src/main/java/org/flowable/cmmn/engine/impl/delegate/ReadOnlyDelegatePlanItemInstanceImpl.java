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
package org.flowable.cmmn.engine.impl.delegate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.ReadOnlyDelegatePlanItemInstance;
import org.flowable.cmmn.model.PlanItem;

/**
 * @author Filip Hrisafov
 */
public class ReadOnlyDelegatePlanItemInstanceImpl implements ReadOnlyDelegatePlanItemInstance {

    protected final String id;
    protected final String name;
    protected final String state;
    protected final String caseDefinitionId;
    protected final String derivedCaseDefinitionId;
    protected final String caseInstanceId;
    protected final String stageInstanceId;
    protected final boolean stage;
    protected final String elementId;
    protected final String planItemDefinitionId;
    protected final String planItemDefinitionType;
    protected final Date createTime;
    protected final Date lastAvailableTime;
    protected final Date lastUnavailableTime;
    protected final Date lastEnabledTime;
    protected final Date lastDisabledTime;
    protected final Date lastStartedTime;
    protected final Date lastSuspendedTime;
    protected final Date completedTime;
    protected final Date occurredTime;
    protected final Date terminatedTime;
    protected final Date exitTime;
    protected final Date endedTime;
    protected final String startUserId;
    protected final String referenceId;
    protected final String referenceType;
    protected final boolean completable;
    protected final String entryCriterionId;
    protected final String exitCriterionId;
    protected final String formKey;
    protected final String extraValue;
    protected final Map<String, Object> variables;
    protected final String tenantId;
    protected final PlanItem planItem;

    public ReadOnlyDelegatePlanItemInstanceImpl(DelegatePlanItemInstance planItemInstance) {
        this.id = planItemInstance.getId();
        this.name = planItemInstance.getName();
        this.state = planItemInstance.getState();
        this.caseDefinitionId = planItemInstance.getCaseDefinitionId();
        this.derivedCaseDefinitionId = planItemInstance.getDerivedCaseDefinitionId();
        this.caseInstanceId = planItemInstance.getCaseInstanceId();
        this.stageInstanceId = planItemInstance.getStageInstanceId();
        this.stage = planItemInstance.isStage();
        this.elementId = planItemInstance.getElementId();
        this.planItemDefinitionId = planItemInstance.getPlanItemDefinitionId();
        this.planItemDefinitionType = planItemInstance.getPlanItemDefinitionType();
        this.createTime = planItemInstance.getCreateTime();
        this.lastAvailableTime = planItemInstance.getLastAvailableTime();
        this.lastUnavailableTime = planItemInstance.getLastUnavailableTime();
        this.lastEnabledTime = planItemInstance.getLastEnabledTime();
        this.lastDisabledTime = planItemInstance.getLastDisabledTime();
        this.lastStartedTime = planItemInstance.getLastStartedTime();
        this.lastSuspendedTime = planItemInstance.getLastSuspendedTime();
        this.completedTime = planItemInstance.getCompletedTime();
        this.occurredTime = planItemInstance.getOccurredTime();
        this.terminatedTime = planItemInstance.getTerminatedTime();
        this.exitTime = planItemInstance.getExitTime();
        this.endedTime = planItemInstance.getEndedTime();
        this.startUserId = planItemInstance.getStartUserId();
        this.referenceId = planItemInstance.getReferenceId();
        this.referenceType = planItemInstance.getReferenceType();
        this.completable = planItemInstance.isCompletable();
        this.entryCriterionId = planItemInstance.getEntryCriterionId();
        this.exitCriterionId = planItemInstance.getExitCriterionId();
        this.formKey = planItemInstance.getFormKey();
        this.extraValue = planItemInstance.getExtraValue();
        this.variables = new HashMap<>(planItemInstance.getVariables());
        this.tenantId = planItemInstance.getTenantId();
        this.planItem = planItemInstance.getPlanItem();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    @Override
    public String getDerivedCaseDefinitionId() {
        return derivedCaseDefinitionId;
    }

    @Override
    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    @Override
    public String getStageInstanceId() {
        return stageInstanceId;
    }

    @Override
    public boolean isStage() {
        return stage;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    @Override
    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }

    @Override
    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public Date getLastAvailableTime() {
        return lastAvailableTime;
    }

    @Override
    public Date getLastUnavailableTime() {
        return lastUnavailableTime;
    }

    @Override
    public Date getLastEnabledTime() {
        return lastEnabledTime;
    }

    @Override
    public Date getLastDisabledTime() {
        return lastDisabledTime;
    }

    @Override
    public Date getLastStartedTime() {
        return lastStartedTime;
    }

    @Override
    public Date getLastSuspendedTime() {
        return lastSuspendedTime;
    }

    @Override
    public Date getCompletedTime() {
        return completedTime;
    }

    @Override
    public Date getOccurredTime() {
        return occurredTime;
    }

    @Override
    public Date getTerminatedTime() {
        return terminatedTime;
    }

    @Override
    public Date getExitTime() {
        return exitTime;
    }

    @Override
    public Date getEndedTime() {
        return endedTime;
    }

    @Override
    public String getStartUserId() {
        return startUserId;
    }

    @Override
    public String getReferenceId() {
        return referenceId;
    }

    @Override
    public String getReferenceType() {
        return referenceType;
    }

    @Override
    public boolean isCompletable() {
        return completable;
    }

    @Override
    public String getEntryCriterionId() {
        return entryCriterionId;
    }

    @Override
    public String getExitCriterionId() {
        return exitCriterionId;
    }

    @Override
    public String getFormKey() {
        return formKey;
    }

    @Override
    public String getExtraValue() {
        return extraValue;
    }

    @Override
    public boolean hasVariable(String variableName) {
        return variables.containsKey(variableName);
    }

    @Override
    public Object getVariable(String variableName) {
        return variables.get(variableName);
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public PlanItem getPlanItem() {
        return planItem;
    }
}
