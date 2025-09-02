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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Dennis Federico
 * @author Joram Barrez
 */
public interface HistoricPlanItemInstanceQuery extends Query<HistoricPlanItemInstanceQuery, HistoricPlanItemInstance> {

    HistoricPlanItemInstanceQuery planItemInstanceId(String planItemInstanceId);
    HistoricPlanItemInstanceQuery planItemInstanceName(String planItemInstanceName);
    HistoricPlanItemInstanceQuery planItemInstanceState(String state);
    HistoricPlanItemInstanceQuery planItemInstanceCaseDefinitionId(String caseDefinitionId);
    HistoricPlanItemInstanceQuery planItemInstanceDerivedCaseDefinitionId(String derivedCaseDefinitionId);
    HistoricPlanItemInstanceQuery planItemInstanceCaseInstanceId(String caseInstanceId);

    HistoricPlanItemInstanceQuery planItemInstanceCaseInstanceIds(Set<String> caseInstanceIds);
    HistoricPlanItemInstanceQuery planItemInstanceStageInstanceId(String stageInstanceId);
    HistoricPlanItemInstanceQuery planItemInstanceElementId(String elementId);
    HistoricPlanItemInstanceQuery planItemInstanceDefinitionId(String planItemDefinitionId);
    HistoricPlanItemInstanceQuery planItemInstanceDefinitionType(String planItemDefinitionType);
    HistoricPlanItemInstanceQuery planItemInstanceDefinitionTypes(List<String> planItemDefinitionTypes);
    HistoricPlanItemInstanceQuery planItemInstanceStartUserId(String startUserId);
    HistoricPlanItemInstanceQuery planItemInstanceAssignee(String assignee);
    HistoricPlanItemInstanceQuery planItemInstanceCompletedBy(String completedBy);
    HistoricPlanItemInstanceQuery planItemInstanceReferenceId(String referenceId);
    HistoricPlanItemInstanceQuery planItemInstanceReferenceType(String referenceType);
    HistoricPlanItemInstanceQuery planItemInstanceEntryCriterionId(String entryCriterionId);
    HistoricPlanItemInstanceQuery planItemInstanceExitCriterionId(String exitCriterionId);
    HistoricPlanItemInstanceQuery planItemInstanceFormKey(String formKey);
    HistoricPlanItemInstanceQuery planItemInstanceExtraValue(String extraValue);
    HistoricPlanItemInstanceQuery involvedUser(String involvedUser);
    HistoricPlanItemInstanceQuery involvedGroups(Collection<String> involvedGroups);
    HistoricPlanItemInstanceQuery onlyStages();
    HistoricPlanItemInstanceQuery planItemInstanceTenantId(String tenantId);
    HistoricPlanItemInstanceQuery planItemInstanceWithoutTenantId();
    HistoricPlanItemInstanceQuery planItemInstanceTenantIdLike(String tenantIdLike);
    HistoricPlanItemInstanceQuery createdBefore(Date createdBefore);
    HistoricPlanItemInstanceQuery createdAfter(Date createdAfter);
    HistoricPlanItemInstanceQuery lastAvailableBefore(Date availableBefore);
    HistoricPlanItemInstanceQuery lastAvailableAfter(Date availableAfter);
    HistoricPlanItemInstanceQuery lastUnavailableBefore(Date unavailableBefore);
    HistoricPlanItemInstanceQuery lastUnavailableAfter(Date unavailableAfter);
    HistoricPlanItemInstanceQuery lastEnabledBefore(Date enabledBefore);
    HistoricPlanItemInstanceQuery lastEnabledAfter(Date enabledAfter);
    HistoricPlanItemInstanceQuery lastDisabledBefore(Date disabledBefore);
    HistoricPlanItemInstanceQuery lastDisabledAfter(Date disabledAfter);
    HistoricPlanItemInstanceQuery lastStartedBefore(Date startedBefore);
    HistoricPlanItemInstanceQuery lastStartedAfter(Date startedAfter);
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
    HistoricPlanItemInstanceQuery endedBefore(Date endedBefore);
    HistoricPlanItemInstanceQuery endedAfter(Date endedAfter);
    HistoricPlanItemInstanceQuery ended();
    HistoricPlanItemInstanceQuery notEnded();

    /**
     * Localize plan item name to specified locale.
     */
    HistoricPlanItemInstanceQuery locale(String locale);

    /**
     * Instruct localization to fallback to more general locales including the default locale of the JVM if the specified locale is not found.
     */
    HistoricPlanItemInstanceQuery withLocalizationFallback();

    HistoricPlanItemInstanceQuery includeLocalVariables();

    HistoricPlanItemInstanceQuery orderByCreateTime();
    HistoricPlanItemInstanceQuery orderByEndedTime();
    HistoricPlanItemInstanceQuery orderByLastAvailableTime();
    HistoricPlanItemInstanceQuery orderByLastEnabledTime();
    HistoricPlanItemInstanceQuery orderByLastDisabledTime();
    HistoricPlanItemInstanceQuery orderByLastStartedTime();
    HistoricPlanItemInstanceQuery orderByLastSuspendedTime();
    HistoricPlanItemInstanceQuery orderByLastUpdatedTime();
    HistoricPlanItemInstanceQuery orderByCompletedTime();
    HistoricPlanItemInstanceQuery orderByOccurredTime();
    HistoricPlanItemInstanceQuery orderByTerminatedTime();
    HistoricPlanItemInstanceQuery orderByExitTime();
    HistoricPlanItemInstanceQuery orderByName();
}
