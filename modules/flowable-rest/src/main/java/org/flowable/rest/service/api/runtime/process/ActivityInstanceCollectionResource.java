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

import java.util.Map;

import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
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
@Api(tags = { "Activity Instances" }, authorizations = { @Authorization(value = "basicAuth") })
@RestController
public class ActivityInstanceCollectionResource extends ActivityInstanceBaseResource {

    @ApiOperation(value = "List activity instances", tags = { "History" }, nickname = "listActivityInstances")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that activity instances could be queried."),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.") })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "activityId", dataType = "string", value = "An id of the activity instance.", paramType = "query"),
            @ApiImplicitParam(name = "activityInstanceId", dataType = "string", value = "An id of the activity instance.", paramType = "query"),
            @ApiImplicitParam(name = "activityName", dataType = "string", value = "The name of the activity instance.", paramType = "query"),
            @ApiImplicitParam(name = "activityType", dataType = "string", value = "The element type of the activity instance.", paramType = "query"),
            @ApiImplicitParam(name = "executionId", dataType = "string", value = "The execution id of the activity instance.", paramType = "query"),
            @ApiImplicitParam(name = "finished", dataType = "boolean", value = "Indication if the activity instance is finished.", paramType = "query"),
            @ApiImplicitParam(name = "taskAssignee", dataType = "string", value = "The assignee of the activity instance.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceId", dataType = "string", value = "The process instance id of the activity instance.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceIds", dataType = "string", value = "The process instance ids of the activity instances.", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionId", dataType = "string", value = "The process definition id of the activity instance.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return instances with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return instances with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns instances without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "The field to sort by. Defaults to 'startTime'.", allowableValues = "activityId,activityName,activityType,duration,endTime,executionId,activityInstanceId,processDefinitionId,processInstanceId,startTime,tenantId,", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),
    })
    @GetMapping(value = "/runtime/activity-instances", produces = "application/json")
    public DataResponse<ActivityInstanceResponse> getActivityInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        ActivityInstanceQueryRequest query = new ActivityInstanceQueryRequest();

        // Populate query based on request
        if (allRequestParams.get("activityId") != null) {
            query.setActivityId(allRequestParams.get("activityId"));
        }

        if (allRequestParams.get("activityInstanceId") != null) {
            query.setActivityInstanceId(allRequestParams.get("activityInstanceId"));
        }

        if (allRequestParams.get("activityName") != null) {
            query.setActivityName(allRequestParams.get("activityName"));
        }

        if (allRequestParams.get("activityType") != null) {
            query.setActivityType(allRequestParams.get("activityType"));
        }

        if (allRequestParams.get("executionId") != null) {
            query.setExecutionId(allRequestParams.get("executionId"));
        }

        if (allRequestParams.get("finished") != null) {
            query.setFinished(Boolean.valueOf(allRequestParams.get("finished")));
        }

        if (allRequestParams.get("taskAssignee") != null) {
            query.setTaskAssignee(allRequestParams.get("taskAssignee"));
        }

        if (allRequestParams.get("taskCompletedBy") != null) {
            query.setTaskCompletedBy(allRequestParams.get("taskCompletedBy"));
        }

        if (allRequestParams.get("processInstanceId") != null) {
            query.setProcessInstanceId(allRequestParams.get("processInstanceId"));
        }

        if (allRequestParams.get("processInstanceIds") != null) {
            query.setProcessInstanceIds(RequestUtil.parseToSet(allRequestParams.get("processInstanceIds")));
        }

        if (allRequestParams.get("processDefinitionId") != null) {
            query.setProcessDefinitionId(allRequestParams.get("processDefinitionId"));
        }

        if (allRequestParams.get("tenantId") != null) {
            query.setTenantId(allRequestParams.get("tenantId"));
        }

        if (allRequestParams.get("tenantIdLike") != null) {
            query.setTenantIdLike(allRequestParams.get("tenantIdLike"));
        }

        if (allRequestParams.get("withoutTenantId") != null) {
            query.setWithoutTenantId(Boolean.valueOf(allRequestParams.get("withoutTenantId")));
        }

        return getQueryResponse(query, allRequestParams);
    }
}
