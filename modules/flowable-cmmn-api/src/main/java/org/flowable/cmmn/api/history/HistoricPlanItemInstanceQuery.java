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
package org.flowable.cmmn.api.history;

import org.flowable.common.engine.api.query.Query;

import java.util.Date;

/**
 * @author Dennis Federico
 */
public interface HistoricPlanItemInstanceQuery extends Query<HistoricPlanItemInstanceQuery, HistoricPlanItemInstance> {

    HistoricPlanItemInstanceQuery planItemInstanceId(String planItemInstanceId);
    HistoricPlanItemInstanceQuery planItemInstanceName(String planItemInstanceName);
    HistoricPlanItemInstanceQuery planItemInstanceState(String state);
    HistoricPlanItemInstanceQuery planItemInstanceCaseDefinitionId(String caseDefinitionId);
    HistoricPlanItemInstanceQuery planItemInstanceCaseInstanceId(String caseInstanceId);
    HistoricPlanItemInstanceQuery planItemInstanceStageInstanceId(String stageInstanceId);
    HistoricPlanItemInstanceQuery planItemInstanceElementId(String elementId);
    HistoricPlanItemInstanceQuery planItemInstanceDefinitionId(String planItemDefinitionId);
    HistoricPlanItemInstanceQuery planItemInstanceDefinitionType(String planItemDefinitionType);
    HistoricPlanItemInstanceQuery planItemInstanceStartUserId(String startUserId);
    HistoricPlanItemInstanceQuery planItemInstanceReferenceId(String referenceId);
    HistoricPlanItemInstanceQuery planItemInstanceReferenceType(String referenceType);
    HistoricPlanItemInstanceQuery planItemInstanceTenantId(String tenantId);
    HistoricPlanItemInstanceQuery planItemInstanceWithoutTenantId();
    HistoricPlanItemInstanceQuery planItemInstanceTenantIdLike(String tenantIdLike);
    HistoricPlanItemInstanceQuery createdBefore(Date createdBefore);
    HistoricPlanItemInstanceQuery createdAfter(Date createdAfter);
    HistoricPlanItemInstanceQuery lastAvailableBefore(Date availableBefore);
    HistoricPlanItemInstanceQuery lastAvailableAfter(Date availableAfter);
    HistoricPlanItemInstanceQuery lastEnabledBefore(Date enabledBefore);
    HistoricPlanItemInstanceQuery lastEnabledAfter(Date enabledAfter);
    HistoricPlanItemInstanceQuery lastDisabledBefore(Date disabledBefore);
    HistoricPlanItemInstanceQuery lastDisabledAfter(Date disabledAfter);
    HistoricPlanItemInstanceQuery lastStartedBefore(Date activatedBefore);
    HistoricPlanItemInstanceQuery lastStartedAfter(Date activatedAfter);
    HistoricPlanItemInstanceQuery lastSuspendedBefore(Date suspendedBefore);
    HistoricPlanItemInstanceQuery lastSuspendedAfter(Date suspendedAfter);
    HistoricPlanItemInstanceQuery completedBefore(Date completedBefore);
    HistoricPlanItemInstanceQuery completedAfter(Date completedAfter);
    HistoricPlanItemInstanceQuery occurredBefore(Date occurredBefore);
    HistoricPlanItemInstanceQuery occurredAfter(Date occurredAfter);
    HistoricPlanItemInstanceQuery terminatedBefore(Date terminatedBefore);
    HistoricPlanItemInstanceQuery terminatedAfter(Date terminatedAfter);
    HistoricPlanItemInstanceQuery exitBefore(Date exitBefore);
    HistoricPlanItemInstanceQuery exitAfter(Date exitAfter);
    HistoricPlanItemInstanceQuery endedBefore(Date startedBefore);
    HistoricPlanItemInstanceQuery endedAfter(Date startedAfter);
    HistoricPlanItemInstanceQuery orderByCreatedTime();
    HistoricPlanItemInstanceQuery orderByEndedTime();
    HistoricPlanItemInstanceQuery orderByName();
}
