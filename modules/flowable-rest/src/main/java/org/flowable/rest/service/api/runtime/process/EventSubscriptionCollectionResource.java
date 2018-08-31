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

package org.flowable.rest.service.api.runtime.process;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.EventSubscriptionQuery;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
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
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Event subscriptions" }, description = "Query event subscriptions", authorizations = { @Authorization(value = "basicAuth") })
public class EventSubscriptionCollectionResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RuntimeService runtimeService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List of event subscriptions", tags = { "Event subscriptions" }, nickname = "listEventSubscriptions")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return event subscriptions with the given id", paramType = "query"),
            @ApiImplicitParam(name = "eventType", dataType = "string", value = "Only return event subscriptions with the given event type", paramType = "query"),
            @ApiImplicitParam(name = "eventName", dataType = "string", value = "Only return event subscriptions with the given event name", paramType = "query"),
            @ApiImplicitParam(name = "activityId", dataType = "string", value = "Only return event subscriptions with the given activity id", paramType = "query"),
            @ApiImplicitParam(name = "executionId", dataType = "string", value = "Only return event subscriptions with the given execution id", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceId", dataType = "string", value = "Only return event subscriptions part of a process with the given id", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionId", dataType = "string", value = "Only return event subscriptions with the given process definition id", paramType = "query"),
            @ApiImplicitParam(name = "createdBefore", dataType = "string", format="date-time", value = "Only return event subscriptions which are created before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "createdAfter", dataType = "string", format="date-time", value = "Only return event subscriptions which are created after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return event subscriptions with the given tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,created,executionId,processInstanceId,processDefinitionId,tenantId", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested event subscriptions were returned."),
            @ApiResponse(code = 400, message = "Indicates an illegal value has been used in a url query parameter. Status description contains additional details about the error.")
    })
    @GetMapping(value = "/runtime/event-subscriptions", produces = "application/json")
    public DataResponse<EventSubscriptionResponse> getJobs(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

        if (allRequestParams.containsKey("id")) {
            query.id(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("eventType")) {
            query.eventType(allRequestParams.get("eventType"));
        }
        if (allRequestParams.containsKey("eventName")) {
            query.eventName(allRequestParams.get("eventName"));
        }
        if (allRequestParams.containsKey("activityId")) {
            query.activityId(allRequestParams.get("activityId"));
        }
        if (allRequestParams.containsKey("executionId")) {
            query.executionId(allRequestParams.get("executionId"));
        }
        if (allRequestParams.containsKey("processInstanceId")) {
            query.processInstanceId(allRequestParams.get("processInstanceId"));
        }
        if (allRequestParams.containsKey("processDefinitionId")) {
            query.processDefinitionId(allRequestParams.get("processDefinitionId"));
        }
        if (allRequestParams.containsKey("createdBefore")) {
            query.createdBefore(RequestUtil.getDate(allRequestParams, "createdBefore"));
        }
        if (allRequestParams.containsKey("createdAfter")) {
            query.createdAfter(RequestUtil.getDate(allRequestParams, "createdAfter"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            query.tenantId(allRequestParams.get("tenantId"));
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessEventSubscriptionInfoWithQuery(query);
        }

        return paginateList(allRequestParams, query, "id", EventSubscriptionQueryProperties.PROPERTIES,
            restResponseFactory::createEventSubscriptionResponseList);
    }
}
