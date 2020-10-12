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
package org.flowable.cmmn.api.runtime;

import java.util.Date;

/**
 * @author Joram Barrez
 */
public interface PlanItemInstance {
    
    String getId();
    String getName();
    String getState();
    String getCaseDefinitionId();
    String getDerivedCaseDefinitionId();
    String getCaseInstanceId();
    String getStageInstanceId();
    boolean isStage();
    String getElementId();
    String getPlanItemDefinitionId();
    String getPlanItemDefinitionType();
    /**
     * @deprecated use {@link #getCreateTime()} instead
     */
    @Deprecated()
    default Date getStartTime() {
        return getCreateTime();
    }
    Date getCreateTime();
    Date getLastAvailableTime();
    Date getLastUnavailableTime();
    Date getLastEnabledTime();
    Date getLastDisabledTime();
    Date getLastStartedTime();
    Date getLastSuspendedTime();
    Date getCompletedTime();
    Date getOccurredTime();
    Date getTerminatedTime();
    Date getExitTime();
    Date getEndedTime();
    String getStartUserId();
    String getReferenceId();
    String getReferenceType();
    boolean isCompletable();
    String getEntryCriterionId();
    String getExitCriterionId();
    String getFormKey();
    String getExtraValue();
    String getTenantId();
    

    /** Sets an optional localized name for the plan item */
    void setLocalizedName(String localizedName);
}
