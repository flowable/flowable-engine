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

package org.flowable.cmmn.rest.service.api.management;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.Map;

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.rest.service.api.CmmnRestApiInterceptor;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.job.api.HistoryJobQuery;
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
 * @author Joram Barrez
 */
@RestController
@Api(tags = { "Jobs" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoryJobCollectionResource {

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnManagementService managementService;
    
    @Autowired(required=false)
    protected CmmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List history jobs", tags = { "Jobs" }, nickname = "listDeadLetterJobs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return the job with the given id", paramType = "query"),
            @ApiImplicitParam(name = "withException", dataType = "boolean", value = "If true, only return jobs for which an exception occurred while executing it. If false, this parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "exceptionMessage", dataType = "string", value = "Only return jobs with the given exception message", paramType = "query"),
            @ApiImplicitParam(name = "scopeType", dataType = "string", value = "Only return jobs with the given scope type", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return jobs with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return jobs with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns jobs without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "lockOwner", dataType = "string", value = "If true, only return jobs which are owned by the given lockOwner.", paramType = "query"),
            @ApiImplicitParam(name = "locked", dataType = "boolean", value = "If true, only return jobs which are locked.  If false, this parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "unlocked", dataType = "boolean", value = "If true, only return jobs which are unlocked. If false, this parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,retries,tenantId", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested jobs were returned."),
            @ApiResponse(code = 400, message = "Indicates an illegal value has been used in a url query parameter. Status description contains additional details about the error.")
    })
    @GetMapping(value = "/cmmn-management/history-jobs", produces = "application/json")
    public DataResponse<HistoryJobResponse> getHistoryJobs(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        HistoryJobQuery query = managementService.createHistoryJobQuery();

        if (allRequestParams.containsKey("id")) {
            query.jobId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("scopeType")) {
            query.jobId(allRequestParams.get("scopeType"));
        }
        if (allRequestParams.containsKey("withException")) {
            if (Boolean.parseBoolean(allRequestParams.get("withException"))) {
                query.withException();
            }
        }
        if (allRequestParams.containsKey("exceptionMessage")) {
            query.exceptionMessage(allRequestParams.get("exceptionMessage"));
        }
        if (allRequestParams.containsKey("lockOwner")) {
            query.lockOwner(allRequestParams.get("lockOwner"));
        }
        if (allRequestParams.containsKey("locked")) {
            if (Boolean.parseBoolean(allRequestParams.get("locked"))) {
                query.locked();
            }
        }
        if (allRequestParams.containsKey("unlocked")) {
            if (Boolean.parseBoolean(allRequestParams.get("unlocked"))) {
                query.unlocked();
            }
        }
        if (allRequestParams.containsKey("tenantId")) {
            query.jobTenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            query.jobTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        if (allRequestParams.containsKey("withoutTenantId")) {
            if (Boolean.parseBoolean(allRequestParams.get("withoutTenantId"))) {
                query.jobWithoutTenantId();
            }
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessHistoryJobInfoWithQuery(query);
        }

        return paginateList(allRequestParams, query, "id", HistoryJobQueryProperties.PROPERTIES, restResponseFactory::createHistoryJobResponseList);
    }
}
