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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.rest.service.api.CmmnRestApiInterceptor;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.job.api.JobQuery;
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
@Api(tags = { "Jobs" }, description = "Manage Jobs", authorizations = { @Authorization(value = "basicAuth") })
public class JobCollectionResource {

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnManagementService managementService;
    
    @Autowired(required=false)
    protected CmmnRestApiInterceptor restApiInterceptor;

    // Fixme documentation & real parameters
    @ApiOperation(value = "List jobs", tags = { "Jobs" }, nickname = "listJobs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return job with the given id", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "Only return jobs part of a case with the given id", paramType = "query"),
            @ApiImplicitParam(name = "planItemInstanceId", dataType = "string", value = "Only return jobs part of a plan item instance with the given id", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "Only return jobs with the given process definition id", paramType = "query"),
            @ApiImplicitParam(name = "timersOnly", dataType = "boolean", value = "If true, only return jobs which are timers. If false, this parameter is ignored. Cannot be used together with 'messagesOnly'.", paramType = "query"),
            @ApiImplicitParam(name = "messagesOnly", dataType = "boolean", value = "If true, only return jobs which are messages. If false, this parameter is ignored. Cannot be used together with 'timersOnly'", paramType = "query"),
            @ApiImplicitParam(name = "withException", dataType = "boolean", value = "If true, only return jobs for which an exception occurred while executing it. If false, this parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "dueBefore", dataType = "string", format="date-time", value = "Only return jobs which are due to be executed before the given date. Jobs without duedate are never returned using this parameter.", paramType = "query"),
            @ApiImplicitParam(name = "dueAfter", dataType = "string", format="date-time", value = "Only return jobs which are due to be executed after the given date. Jobs without duedate are never returned using this parameter.", paramType = "query"),
            @ApiImplicitParam(name = "exceptionMessage", dataType = "string", value = "Only return jobs with the given exception message", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return jobs with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return jobs with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns jobs without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "locked", dataType = "boolean", value = "If true, only return jobs which are locked.  If false, this parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "unlocked", dataType = "boolean", value = "If true, only return jobs which are unlocked. If false, this parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,dueDate,executionId,processInstanceId,retries,tenantId", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested jobs were returned."),
            @ApiResponse(code = 400, message = "Indicates an illegal value has been used in a url query parameter or the both 'messagesOnly' and 'timersOnly' are used as parameters. Status description contains additional details about the error.")
    })
    @GetMapping(value = "/cmmn-management/jobs", produces = "application/json")
    public DataResponse<JobResponse> getJobs(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        JobQuery query = managementService.createJobQuery();

        if (allRequestParams.containsKey("id")) {
            query.jobId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("caseInstanceId")) {
            query.scopeId(allRequestParams.get("caseInstanceId"));
            query.scopeType(ScopeTypes.CMMN);
        }
        if (allRequestParams.containsKey("planItemInstanceId")) {
            query.subScopeId(allRequestParams.get("planItemInstanceId"));
            query.scopeType(ScopeTypes.CMMN);
        }
        if (allRequestParams.containsKey("caseDefinitionId")) {
            query.scopeDefinitionId(allRequestParams.get("caseDefinitionId"));
            query.scopeType(ScopeTypes.CMMN);
        }
        if (allRequestParams.containsKey("timersOnly")) {
            if (allRequestParams.containsKey("messagesOnly")) {
                throw new FlowableIllegalArgumentException("Only one of 'timersOnly' or 'messagesOnly' can be provided.");
            }
            if (Boolean.valueOf(allRequestParams.get("timersOnly"))) {
                query.timers();
            }
        }
        if (allRequestParams.containsKey("messagesOnly")) {
            if (Boolean.valueOf(allRequestParams.get("messagesOnly"))) {
                query.messages();
            }
        }
        if (allRequestParams.containsKey("dueBefore")) {
            query.duedateLowerThan(RequestUtil.getDate(allRequestParams, "dueBefore"));
        }
        if (allRequestParams.containsKey("dueAfter")) {
            query.duedateHigherThan(RequestUtil.getDate(allRequestParams, "dueAfter"));
        }
        if (allRequestParams.containsKey("withException")) {
            if (Boolean.valueOf(allRequestParams.get("withException"))) {
                query.withException();
            }
        }
        if (allRequestParams.containsKey("exceptionMessage")) {
            query.exceptionMessage(allRequestParams.get("exceptionMessage"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            query.jobTenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            query.jobTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        if (allRequestParams.containsKey("withoutTenantId")) {
            if (Boolean.valueOf(allRequestParams.get("withoutTenantId"))) {
                query.jobWithoutTenantId();
            }
        }
        if (allRequestParams.containsKey("locked")) {
            if (Boolean.valueOf(allRequestParams.get("locked"))) {
                query.locked();
            }
        }
        if (allRequestParams.containsKey("unlocked")) {
            if (Boolean.valueOf(allRequestParams.get("unlocked"))) {
                query.unlocked();
            }
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessJobInfoWithQuery(query);
        }

        return new JobPaginateList(restResponseFactory).paginateList(allRequestParams, query, "id", JobQueryProperties.PROPERTIES);
    }
}
