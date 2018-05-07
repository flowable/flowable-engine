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

package org.flowable.cmmn.rest.service.api.history.milestone;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.history.HistoricMilestoneInstanceQuery;
import org.flowable.cmmn.engine.impl.runtime.MilestoneInstanceQueryProperty;
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
public abstract class HistoricMilestoneInstanceBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("milestoneName", MilestoneInstanceQueryProperty.MILESTONE_NAME);
        allowedSortProperties.put("timestamp", MilestoneInstanceQueryProperty.MILESTONE_TIMESTAMP);
    }

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnHistoryService historyService;

    protected DataResponse<HistoricMilestoneInstanceResponse> getQueryResponse(HistoricMilestoneInstanceQueryRequest queryRequest, Map<String, String> allRequestParams) {
        HistoricMilestoneInstanceQuery query = historyService.createHistoricMilestoneInstanceQuery();

        Optional.ofNullable(queryRequest.getId()).ifPresent(query::milestoneInstanceId);
        Optional.ofNullable(queryRequest.getName()).ifPresent(query::milestoneInstanceName);
        Optional.ofNullable(queryRequest.getCaseInstanceId()).ifPresent(query::milestoneInstanceCaseInstanceId);
        Optional.ofNullable(queryRequest.getCaseDefinitionId()).ifPresent(query::milestoneInstanceCaseInstanceId);
        Optional.ofNullable(queryRequest.getReachedBefore()).ifPresent(query::milestoneInstanceReachedBefore);
        Optional.ofNullable(queryRequest.getReachedAfter()).ifPresent(query::milestoneInstanceReachedAfter);

        return new HistoricMilestoneInstancePaginateList(restResponseFactory).paginateList(allRequestParams, queryRequest, query, "timestamp", allowedSortProperties);
    }

}
