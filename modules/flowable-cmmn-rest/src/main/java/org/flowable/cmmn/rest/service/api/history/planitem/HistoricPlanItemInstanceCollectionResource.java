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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * @author Tijs Rademakers
 * @author DennisFederico
 */
@RestController
@Api(tags = {"History PlanItem"}, description = "Manage History Plan Item Instances", authorizations = {@Authorization(value = "basicAuth")})
public class HistoricPlanItemInstanceCollectionResource extends HistoricPlanItemInstanceBaseResource {

    private static Map<String, BiConsumer<HistoricPlanItemInstanceQueryRequest, String>> mapping = new HashMap<>();
    private static BiConsumer<HistoricPlanItemInstanceQueryRequest, String> voidConsumer = (o1, o2) -> {
    };

    static {
        mapping.put("planItemInstanceId", HistoricPlanItemInstanceQueryRequest::setPlanItemInstanceId);
        mapping.put("planItemInstanceName", HistoricPlanItemInstanceQueryRequest::setPlanItemInstanceName);
        mapping.put("planItemInstanceState", HistoricPlanItemInstanceQueryRequest::setPlanItemInstanceState);
        mapping.put("caseDefinitionId", HistoricPlanItemInstanceQueryRequest::setCaseDefinitionId);
        mapping.put("caseInstanceId", HistoricPlanItemInstanceQueryRequest::setCaseInstanceId);
        mapping.put("stageInstanceId", HistoricPlanItemInstanceQueryRequest::setStageInstanceId);
        mapping.put("elementId", HistoricPlanItemInstanceQueryRequest::setElementId);
        mapping.put("planItemDefinitionId", HistoricPlanItemInstanceQueryRequest::setPlanItemDefinitionId);
        mapping.put("planItemDefinitionType", HistoricPlanItemInstanceQueryRequest::setPlanItemDefinitionType);
        mapping.put("createdBefore", (h, s) -> h.setCreatedBefore(RequestUtil.parseLongDate(s)));
        mapping.put("createdAfter", (h, s) -> h.setCreatedAfter(RequestUtil.parseLongDate(s)));
        mapping.put("lastAvailableBefore", (h, s) -> h.setLastAvailableBefore(RequestUtil.parseLongDate(s)));
        mapping.put("lastAvailableAfter", (h, s) -> h.setLastAvailableAfter(RequestUtil.parseLongDate(s)));
        mapping.put("lastEnabledBefore", (h, s) -> h.setLastEnabledBefore(RequestUtil.parseLongDate(s)));
        mapping.put("lastEnabledAfter", (h, s) -> h.setLastEnabledAfter(RequestUtil.parseLongDate(s)));
        mapping.put("lastDisabledBefore", (h, s) -> h.setLastDisabledBefore(RequestUtil.parseLongDate(s)));
        mapping.put("lastDisabledAfter", (h, s) -> h.setLastDisabledAfter(RequestUtil.parseLongDate(s)));
        mapping.put("lastStartedBefore", (h, s) -> h.setLastStartedBefore(RequestUtil.parseLongDate(s)));
        mapping.put("lastStartedAfter", (h, s) -> h.setLastStartedAfter(RequestUtil.parseLongDate(s)));
        mapping.put("lastSuspendedBefore", (h, s) -> h.setLastSuspendedBefore(RequestUtil.parseLongDate(s)));
        mapping.put("lastSuspendedAfter", (h, s) -> h.setLastSuspendedAfter(RequestUtil.parseLongDate(s)));
        mapping.put("completedBefore", (h, s) -> h.setCompletedBefore(RequestUtil.parseLongDate(s)));
        mapping.put("completedAfter", (h, s) -> h.setCompletedAfter(RequestUtil.parseLongDate(s)));
        mapping.put("terminatedBefore", (h, s) -> h.setTerminatedBefore(RequestUtil.parseLongDate(s)));
        mapping.put("terminatedAfter", (h, s) -> h.setTerminatedAfter(RequestUtil.parseLongDate(s)));
        mapping.put("occurredBefore", (h, s) -> h.setOccurredBefore(RequestUtil.parseLongDate(s)));
        mapping.put("occurredAfter", (h, s) -> h.setOccurredAfter(RequestUtil.parseLongDate(s)));
        mapping.put("exitBefore", (h, s) -> h.setExitBefore(RequestUtil.parseLongDate(s)));
        mapping.put("exitAfter", (h, s) -> h.setExitAfter(RequestUtil.parseLongDate(s)));
        mapping.put("endedBefore", (h, s) -> h.setEndedBefore(RequestUtil.parseLongDate(s)));
        mapping.put("endedAfter", (h, s) -> h.setEndedAfter(RequestUtil.parseLongDate(s)));
        mapping.put("startUserId", HistoricPlanItemInstanceQueryRequest::setStartUserId);
        mapping.put("referenceId", HistoricPlanItemInstanceQueryRequest::setReferenceId);
        mapping.put("referenceType", HistoricPlanItemInstanceQueryRequest::setReferenceType);
        mapping.put("tenantId", HistoricPlanItemInstanceQueryRequest::setTenantId);
        mapping.put("withoutTenantId", (h, s) -> h.setWithoutTenantId(Boolean.valueOf(s)));
    }

    @ApiOperation(value = "List of historic plan item instances", tags = {"History PlanItem"}, nickname = "listHistoricPlanItemInstances")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "planItemInstanceId", dataType = "string", value = "The id of the historic planItem instance.", paramType = "query"),
            @ApiImplicitParam(name = "planItemInstanceName", dataType = "string", value = "The name of the historic planItem instance.", paramType = "query"),
            @ApiImplicitParam(name = "planItemInstanceState", dataType = "string", value = "The state of the historic planItem instance.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "The id of the definition of the case were the historic planItem instance is defined.", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "The id of the case instance were the historic planItem instance existed.", paramType = "query"),
            @ApiImplicitParam(name = "stageInstanceId", dataType = "string", value = "The id of the stage were the historic planItem instance was contained.", paramType = "query"),
            @ApiImplicitParam(name = "elementId", dataType = "string", value = "The id of the planItem model of the historic planItem instance.", paramType = "query"),
            @ApiImplicitParam(name = "planItemDefinitionId", dataType = "string", value = "The id of the planItem model definition of the historic planItem instance.", paramType = "query"),
            @ApiImplicitParam(name = "planItemDefinitionType", dataType = "string", value = "The type of planItem of the historic planItem instance.", paramType = "query"),
            @ApiImplicitParam(name = "createdBefore", dataType = "date-time", value = "Return only historic planItem instances that were created before this date.", paramType = "query"),
            @ApiImplicitParam(name = "createdAfter", dataType = "date-time", value = "Return only historic planItem instances that were created after this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastAvailableBefore", dataType = "date-time", value = "Return only historic planItem instances that were last in available before this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastAvailableAfter", dataType = "date-time", value = "Return only historic planItem instances that were last in available state after this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastEnabledBefore", dataType = "date-time", value = "Return only historic planItem instances that were last in enabled state before this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastEnabledAfter", dataType = "date-time", value = "Return only historic planItem instances that were last in enabled state after this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastDisabledBefore", dataType = "date-time", value = "Return only historic planItem instances that were last in disable state before this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastDisabledAfter", dataType = "date-time", value = "Return only historic planItem instances that were last in disabled state after this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastStartedBefore", dataType = "date-time", value = "Return only historic planItem instances that were last in active state before this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastStartedAfter", dataType = "date-time", value = "Return only historic planItem instances that were last in active state after this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastSuspendedBefore", dataType = "date-time", value = "Return only historic planItem instances that were last in suspended state before this date.", paramType = "query"),
            @ApiImplicitParam(name = "lastSuspendedAfter", dataType = "date-time", value = "Return only historic planItem instances that were last in suspended state after this date.", paramType = "query"),
            @ApiImplicitParam(name = "completedBefore", dataType = "date-time", value = "Return only historic planItem instances that were completed before this date.", paramType = "query"),
            @ApiImplicitParam(name = "completedAfter", dataType = "date-time", value = "Return only historic planItem instances that were completed after this date.", paramType = "query"),
            @ApiImplicitParam(name = "terminatedBefore", dataType = "date-time", value = "Return only historic planItem instances that were terminated before this date.", paramType = "query"),
            @ApiImplicitParam(name = "terminatedAfter", dataType = "date-time", value = "Return only historic planItem instances that were terminated after this date.", paramType = "query"),
            @ApiImplicitParam(name = "occurredBefore", dataType = "date-time", value = "Return only historic planItem instances that occurred before this date.", paramType = "query"),
            @ApiImplicitParam(name = "occurredAfter", dataType = "date-time", value = "Return only historic planItem instances that occurred after after this date.", paramType = "query"),
            @ApiImplicitParam(name = "exitBefore", dataType = "date-time", value = "Return only historic planItem instances that exit before this date.", paramType = "query"),
            @ApiImplicitParam(name = "exitAfter", dataType = "date-time", format = "date-time", value = "Return only historic planItem instances that exit after this date.", paramType = "query"),
            @ApiImplicitParam(name = "endedBefore", dataType = "date-time", format = "date-time", value = "Return only historic planItem instances that ended before this date.", paramType = "query"),
            @ApiImplicitParam(name = "endedAfter", dataType = "date-time", format = "date-time", value = "Return only historic planItem instances that ended after this date.", paramType = "query"),
            @ApiImplicitParam(name = "startUserId", dataType = "string", format = "date-time", value = "Return only historic planItem instances that were started by this user.", paramType = "query"),
            @ApiImplicitParam(name = "referenceId", dataType = "string", value = "The id of process that was referenced by this historic planItem instance.", paramType = "query"),
            @ApiImplicitParam(name = "referenceType", dataType = "string", value = "The type of reference to the process referenced by this historic planItem instance.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return instances with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns instances without a tenantId set. If false, the withoutTenantId parameter is ignored.\n", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that historic planItem instances could be queried."),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.")})
    @GetMapping(value = "/cmmn-history/historic-planitem-instances", produces = "application/json")
    public DataResponse<HistoricPlanItemInstanceResponse> getHistoricPlanItemInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        // Populate query based on request
        HistoricPlanItemInstanceQueryRequest queryRequest = new HistoricPlanItemInstanceQueryRequest();
        allRequestParams.forEach((key, value) -> Optional.ofNullable(value).ifPresent(v -> mapping.getOrDefault(key, voidConsumer).accept(queryRequest, v)));
        return getQueryResponse(queryRequest, allRequestParams);
    }
}
