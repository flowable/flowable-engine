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

import org.flowable.engine.common.api.query.Query;

import java.util.Date;

/**
 * @author Dennis Federico
 */
public interface HistoricPlanItemInstanceQuery extends Query<HistoricPlanItemInstanceQuery, HistoricPlanItemInstance> {

    HistoricPlanItemInstanceQuery withId(String planItemInstanceId);
    HistoricPlanItemInstanceQuery withName(String planItemInstanceName);
    HistoricPlanItemInstanceQuery withState(String state);
    HistoricPlanItemInstanceQuery withCaseDefinitionId(String caseDefinitionId);
    HistoricPlanItemInstanceQuery withCaseInstanceId(String caseInstanceId);
    HistoricPlanItemInstanceQuery withStageInstanceId(String stageInstanceId);
    HistoricPlanItemInstanceQuery withElementId(String elementId);
    HistoricPlanItemInstanceQuery withDefinitionId(String planItemDefinitionId);
    HistoricPlanItemInstanceQuery withDefinitionType(String planItemDefinitionType);
    HistoricPlanItemInstanceQuery withStartUserId(String startUserId);
    HistoricPlanItemInstanceQuery withReferenceId(String referenceId);
    HistoricPlanItemInstanceQuery withReferenceType(String referenceType);
    HistoricPlanItemInstanceQuery withTenantId(String tenantId);
    HistoricPlanItemInstanceQuery withoutTenantId();
    HistoricPlanItemInstanceQuery startedBefore(Date startedBefore);
    HistoricPlanItemInstanceQuery startedAfter(Date startedAfter);
    HistoricPlanItemInstanceQuery endedBefore(Date beforeTime);
    HistoricPlanItemInstanceQuery endedAfter(Date afterTime);
    HistoricPlanItemInstanceQuery includeInstanceVariables();
    HistoricPlanItemInstanceQuery orderByStartTime();
    HistoricPlanItemInstanceQuery orderByName();
}
