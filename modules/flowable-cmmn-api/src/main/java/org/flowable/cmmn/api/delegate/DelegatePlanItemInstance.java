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
package org.flowable.cmmn.api.delegate;

import java.util.Date;

import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Joram Barrez
 */
public interface DelegatePlanItemInstance extends PlanItemInstance, VariableScope {

    void setName(String name);
    void setState(String state);
    void setCaseDefinitionId(String caseDefinitionId);
    void setDerivedCaseDefinitionId(String derivedCaseDefinitionId);
    void setCaseInstanceId(String caseInstanceId);
    void setStageInstanceId(String stageInstanceId);
    void setStage(boolean isStage);
    void setElementId(String elementId);
    void setPlanItemDefinitionId(String planItemDefinitionId);
    void setPlanItemDefinitionType(String planItemDefinitionType);
    /**
     * @deprecated use {@link #setCreateTime(Date)} instead
     */
    @Deprecated
    void setStartTime(Date startTime);
    void setCreateTime(Date createTime);
    void setLastAvailableTime(Date availableTime);
    void setLastUnavailableTime(Date availableTime);
    void setLastEnabledTime(Date enabledTime);
    void setLastDisabledTime(Date disabledTime);
    void setLastStartedTime(Date startedTime);
    void setLastSuspendedTime(Date suspendedTime);
    void setCompletedTime(Date completedTime);
    void setOccurredTime(Date occurredTime);
    void setTerminatedTime(Date terminatedTime);
    void setExitTime(Date exitTime);
    void setEndedTime(Date endedTime);
    void setStartUserId(String startUserId);
    void setReferenceId(String referenceId);
    void setReferenceType(String referenceType);
    void setCompletable(boolean completable);
    void setEntryCriterionId(String entryCriterionId);
    void setExitCriterionId(String exitCriterionId);
    void setFormKey(String formKey);
    void setExtraValue(String extraValue);
    void setTenantId(String tenantId);

    PlanItemInstanceLifecycleListener getCurrentLifecycleListener();
    FlowableListener getCurrentFlowableListener();
    void setCurrentLifecycleListener(PlanItemInstanceLifecycleListener lifecycleListener, FlowableListener flowableListener);

    PlanItem getPlanItem();

    default PlanItemDefinition getPlanItemDefinition() {
        PlanItem planItem = getPlanItem();
        if (planItem != null) {
            return planItem.getPlanItemDefinition();
        }

        return null;
    }

    /**
     * Create a snapshot read only delegate plan item instance of this delegate plan item instance.
     *
     * @return a {@link ReadOnlyDelegatePlanItemInstance}
     */
    ReadOnlyDelegatePlanItemInstance snapshotReadOnly();

}
