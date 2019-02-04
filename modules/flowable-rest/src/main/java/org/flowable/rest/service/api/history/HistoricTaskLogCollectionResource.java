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
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
import org.flowable.task.service.impl.HistoricTaskLogEntryQueryProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Luis Belloch
 */
@RestController
@Api(tags = { "Historic Task Log Entries" }, description = "Manage Historic Task Log Entries", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricTaskLogCollectionResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("logNumber", HistoricTaskLogEntryQueryProperty.LOG_NUMBER);
        allowedSortProperties.put("timeStamp", HistoricTaskLogEntryQueryProperty.TIME_STAMP);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;

    @ApiOperation(value = "List historic task log entries", tags = { "History Task" }, nickname = "getHistoricTaskLogEntries")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "taskId", dataType = "string", value = "An id of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "type", dataType = "string", value = "The type of the log entry.", paramType = "query"),
        @ApiImplicitParam(name = "userId", dataType = "string", value = "The user who produced the task change.", paramType = "query"),
        @ApiImplicitParam(name = "processInstanceId", dataType = "string", value = "The process instance id of the historic task log entry.", paramType = "query"),
        @ApiImplicitParam(name = "processDefinitionId", dataType = "string", value = "The process definition id of the historic task log entry.", paramType = "query"),
        @ApiImplicitParam(name = "scopeId", dataType = "string", value = "Only return historic task log entries with the given scopeId.", paramType = "query"),
        @ApiImplicitParam(name = "scopeDefinitionId", dataType = "string", value = "Only return historic task log entries with the given scopeDefinitionId.", paramType = "query"),
        @ApiImplicitParam(name = "subScopeId", dataType = "string", value = "Only return historic task log entries with the given subScopeId", paramType = "query"),
        @ApiImplicitParam(name = "scopeType", dataType = "string", value = "Only return historic task log entries with the given scopeType.", paramType = "query"),
        @ApiImplicitParam(name = "from", dataType = "string", format = "date-time", value = "Return task log entries starting from a date.", paramType = "query"),
        @ApiImplicitParam(name = "to", dataType = "string", format = "date-time", value = "Return task log entries up to a date.", paramType = "query"),
        @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return historic task log entries with the given tenantId.", paramType = "query"),
        @ApiImplicitParam(name = "fromLogNumber", dataType = "string", value = "Return task log entries starting from a log number", paramType = "query"),
        @ApiImplicitParam(name = "toLogNumber", dataType = "string", value = "Return task log entries up to specific a log number", paramType = "query"),
    })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Indicates that historic task log entries could be queried."),
        @ApiResponse(code = 404, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.") })
    @GetMapping(value = "/history/historic-task-log-entries", produces = "application/json")
    public DataResponse<HistoricTaskLogEntryResponse> getHistoricTaskLogEntries(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        HistoricTaskLogEntryQuery query = this.historyService.createHistoricTaskLogEntryQuery();

        if (allRequestParams.containsKey("taskId")) {
            query.taskId(allRequestParams.get("taskId"));
        }

        if (allRequestParams.containsKey("type")) {
            query.type(allRequestParams.get("type"));
        }

        if (allRequestParams.containsKey("userId")) {
            query.userId(allRequestParams.get("userId"));
        }

        if (allRequestParams.containsKey("processInstanceId")) {
            query.processInstanceId(allRequestParams.get("processInstanceId"));
        }

        if (allRequestParams.containsKey("processDefinitionId")) {
            query.processDefinitionId(allRequestParams.get("processDefinitionId"));
        }

        if (allRequestParams.containsKey("scopeId")) {
            query.scopeId(allRequestParams.get("scopeId"));
        }

        if (allRequestParams.containsKey("scopeDefinitionId")) {
            query.scopeDefinitionId(allRequestParams.get("scopeDefinitionId"));
        }

        if (allRequestParams.containsKey("subScopeId")) {
            query.subScopeId(allRequestParams.get("subScopeId"));
        }

        if (allRequestParams.containsKey("scopeType")) {
            query.scopeType(allRequestParams.get("scopeType"));
        }

        if (allRequestParams.containsKey("from")) {
            query.from(RequestUtil.getDate(allRequestParams, "from"));
        }

        if (allRequestParams.containsKey("to")) {
            query.to(RequestUtil.getDate(allRequestParams, "to"));
        }

        if (allRequestParams.containsKey("tenantId")) {
            query.tenantId(allRequestParams.get("tenantId"));
        }

        if (allRequestParams.containsKey("fromLogNumber")) {
            query.fromLogNumber(Long.parseLong(allRequestParams.get("fromLogNumber")));
        }

        if (allRequestParams.containsKey("toLogNumber")) {
            query.toLogNumber(Long.parseLong(allRequestParams.get("toLogNumber")));
        }

        return paginateList(allRequestParams, query, "logNumber", allowedSortProperties, restResponseFactory::createHistoricTaskLogEntryResponseList);
    }
}
