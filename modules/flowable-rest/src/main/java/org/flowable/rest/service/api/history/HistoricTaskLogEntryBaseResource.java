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

package org.flowable.rest.service.api.history;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.engine.HistoryService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
import org.flowable.task.service.impl.HistoricTaskLogEntryQueryProperty;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luis Belloch
 */
public class HistoricTaskLogEntryBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("logNumber", HistoricTaskLogEntryQueryProperty.LOG_NUMBER);
        allowedSortProperties.put("timeStamp", HistoricTaskLogEntryQueryProperty.TIME_STAMP);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;

    @Autowired(required = false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected DataResponse<HistoricTaskLogEntryResponse> getQueryResponse(HistoricTaskLogEntryQueryRequest request, Map<String, String> allRequestParams) {
        HistoricTaskLogEntryQuery query = this.historyService.createHistoricTaskLogEntryQuery();

        if (request.getTaskId() != null) {
            query.taskId(allRequestParams.get("taskId"));
        }

        if (request.getType() != null) {
            query.type(allRequestParams.get("type"));
        }

        if (request.getUserId() != null) {
            query.userId(allRequestParams.get("userId"));
        }

        if (request.getProcessInstanceId() != null) {
            query.processInstanceId(allRequestParams.get("processInstanceId"));
        }

        if (request.getProcessDefinitionId() != null) {
            query.processDefinitionId(allRequestParams.get("processDefinitionId"));
        }

        if (request.getScopeId() != null) {
            query.scopeId(allRequestParams.get("scopeId"));
        }

        if (request.getScopeDefinitionId() != null) {
            query.scopeDefinitionId(allRequestParams.get("scopeDefinitionId"));
        }

        if (request.getSubScopeId() != null) {
            query.subScopeId(allRequestParams.get("subScopeId"));
        }

        if (request.getScopeType() != null) {
            query.scopeType(allRequestParams.get("scopeType"));
        }

        if (request.getFrom() != null) {
            query.from(RequestUtil.getDate(allRequestParams, "from"));
        }

        if (request.getTo() != null) {
            query.to(RequestUtil.getDate(allRequestParams, "to"));
        }

        if (request.getTenantId() != null) {
            query.tenantId(allRequestParams.get("tenantId"));
        }

        if (request.getFromLogNumber() != null) {
            query.fromLogNumber(Long.parseLong(allRequestParams.get("fromLogNumber")));
        }

        if (request.getToLogNumber() != null) {
            query.toLogNumber(Long.parseLong(allRequestParams.get("toLogNumber")));
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessHistoricTaskLogWithQuery(query, request);
        }

        return paginateList(allRequestParams, query, "logNumber", allowedSortProperties, restResponseFactory::createHistoricTaskLogEntryResponseList);
    }
}

