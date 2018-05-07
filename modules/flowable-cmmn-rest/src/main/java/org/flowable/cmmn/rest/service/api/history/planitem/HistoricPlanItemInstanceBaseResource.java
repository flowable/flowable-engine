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

package org.flowable.cmmn.rest.service.api.history.planitem;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.engine.impl.history.HistoricPlanItemInstanceQueryProperty;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Tijs Rademakers
 * @author Dennis Federico
 */
public abstract class HistoricPlanItemInstanceBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("createdTime", HistoricPlanItemInstanceQueryProperty.CREATED_TIME);
        allowedSortProperties.put("endedTime", HistoricPlanItemInstanceQueryProperty.ENDED_TIME);
        allowedSortProperties.put("name", HistoricPlanItemInstanceQueryProperty.NAME);
    }

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnHistoryService historyService;

    protected DataResponse<HistoricPlanItemInstanceResponse> getQueryResponse(HistoricPlanItemInstanceQueryRequest queryRequest, Map<String, String> allRequestParams) {
        HistoricPlanItemInstanceQuery query = historyService.createHistoricPlanItemInstanceQuery();

        Optional.ofNullable(queryRequest.getPlanItemInstanceId()).ifPresent(query::planItemInstanceId);
        Optional.ofNullable(queryRequest.getPlanItemInstanceName()).ifPresent(query::planItemInstanceName);
        Optional.ofNullable(queryRequest.getPlanItemInstanceState()).ifPresent(query::planItemInstanceState);
        Optional.ofNullable(queryRequest.getCaseDefinitionId()).ifPresent(query::planItemInstanceCaseDefinitionId);
        Optional.ofNullable(queryRequest.getCaseInstanceId()).ifPresent(query::planItemInstanceCaseInstanceId);
        Optional.ofNullable(queryRequest.getStageInstanceId()).ifPresent(query::planItemInstanceStageInstanceId);
        Optional.ofNullable(queryRequest.getElementId()).ifPresent(query::planItemInstanceElementId);
        Optional.ofNullable(queryRequest.getPlanItemDefinitionId()).ifPresent(query::planItemInstanceDefinitionId);
        Optional.ofNullable(queryRequest.getPlanItemDefinitionType()).ifPresent(query::planItemInstanceDefinitionType);
        Optional.ofNullable(queryRequest.getStartUserId()).ifPresent(query::planItemInstanceStartUserId);
        Optional.ofNullable(queryRequest.getReferenceId()).ifPresent(query::planItemInstanceReferenceId);
        Optional.ofNullable(queryRequest.getReferenceType()).ifPresent(query::planItemInstanceReferenceType);
        Optional.ofNullable(queryRequest.getTenantId()).ifPresent(query::planItemInstanceTenantId);
        Optional.ofNullable(queryRequest.getWithoutTenantId()).ifPresent(withoutTenant -> {
                    if (withoutTenant) {
                        query.planItemInstanceWithoutTenantId();
                    }
                }
        );
        Optional.ofNullable(queryRequest.getCreatedBefore()).ifPresent(query::createdBefore);
        Optional.ofNullable(queryRequest.getCreatedAfter()).ifPresent(query::createdAfter);
        Optional.ofNullable(queryRequest.getLastAvailableBefore()).ifPresent(query::lastAvailableBefore);
        Optional.ofNullable(queryRequest.getLastAvailableAfter()).ifPresent(query::lastAvailableAfter);
        Optional.ofNullable(queryRequest.getLastEnabledBefore()).ifPresent(query::lastEnabledBefore);
        Optional.ofNullable(queryRequest.getLastEnabledAfter()).ifPresent(query::lastEnabledAfter);
        Optional.ofNullable(queryRequest.getLastDisabledBefore()).ifPresent(query::lastDisabledBefore);
        Optional.ofNullable(queryRequest.getLastDisabledAfter()).ifPresent(query::lastDisabledAfter);
        Optional.ofNullable(queryRequest.getLastStartedBefore()).ifPresent(query::lastStartedBefore);
        Optional.ofNullable(queryRequest.getLastStartedAfter()).ifPresent(query::lastStartedAfter);
        Optional.ofNullable(queryRequest.getLastSuspendedBefore()).ifPresent(query::lastSuspendedBefore);
        Optional.ofNullable(queryRequest.getLastSuspendedAfter()).ifPresent(query::lastSuspendedAfter);
        Optional.ofNullable(queryRequest.getCompletedBefore()).ifPresent(query::completedBefore);
        Optional.ofNullable(queryRequest.getCompletedAfter()).ifPresent(query::completedAfter);
        Optional.ofNullable(queryRequest.getOccurredBefore()).ifPresent(query::occurredBefore);
        Optional.ofNullable(queryRequest.getOccurredAfter()).ifPresent(query::occurredAfter);
        Optional.ofNullable(queryRequest.getTerminatedBefore()).ifPresent(query::terminatedBefore);
        Optional.ofNullable(queryRequest.getTerminatedAfter()).ifPresent(query::terminatedAfter);
        Optional.ofNullable(queryRequest.getExitBefore()).ifPresent(query::exitBefore);
        Optional.ofNullable(queryRequest.getExitAfter()).ifPresent(query::exitAfter);
        Optional.ofNullable(queryRequest.getEndedBefore()).ifPresent(query::endedBefore);
        Optional.ofNullable(queryRequest.getEndedAfter()).ifPresent(query::endedAfter);

        return new HistoricPlanItemInstancePaginateList(restResponseFactory).paginateList(allRequestParams, queryRequest, query, "createdTime", allowedSortProperties);
    }

}
