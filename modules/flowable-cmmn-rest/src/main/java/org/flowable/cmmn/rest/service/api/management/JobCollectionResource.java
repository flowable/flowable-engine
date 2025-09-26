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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.rest.service.api.BulkMoveDeadLetterActionRequest;
import org.flowable.cmmn.rest.service.api.CmmnRestApiInterceptor;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@Api(tags = { "Jobs" }, authorizations = { @Authorization(value = "basicAuth") })
public class JobCollectionResource {

    private static final String MOVE_ACTION = "move";
    private static final String MOVE_TO_HISTORY_JOB_ACTION = "moveToHistoryJob";

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnManagementService managementService;
    
    @Autowired(required=false)
    protected CmmnRestApiInterceptor restApiInterceptor;

    @Autowired
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    // Fixme documentation & real parameters
    @ApiOperation(value = "List jobs", tags = { "Jobs" }, nickname = "listJobs")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return job with the given id", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "Only return jobs part of a case with the given id", paramType = "query"),
            @ApiImplicitParam(name = "withoutScopeId", dataType = "boolean", value = "If true, only returns jobs without a scope id set. If false, the withoutScopeId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "planItemInstanceId", dataType = "string", value = "Only return jobs part of a plan item instance with the given id", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "Only return jobs with the given case definition id", paramType = "query"),
            @ApiImplicitParam(name = "scopeDefinitionId", dataType = "string", value = "Only return jobs with the given scope definition id", paramType = "query"),
            @ApiImplicitParam(name = "scopeType", dataType = "string", value = "Only return jobs with the given scope type", paramType = "query"),
            @ApiImplicitParam(name = "elementId", dataType = "string", value = "Only return jobs with the given element id", paramType = "query"),
            @ApiImplicitParam(name = "elementName", dataType = "string", value = "Only return jobs with the given element name", paramType = "query"),
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
            @ApiImplicitParam(name = "withoutProcessInstanceId", dataType = "boolean", value = "If true, only returns jobs without a process instance id set. If false, the withoutProcessInstanceId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,dueDate,executionId,processInstanceId,retries,tenantId", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested jobs were returned."),
            @ApiResponse(code = 400, message = "Indicates an illegal value has been used in a url query parameter or the both 'messagesOnly' and 'timersOnly' are used as parameters. Status description contains additional details about the error.")
    })
    @GetMapping(value = "/cmmn-management/jobs", produces = "application/json")
    public DataResponse<JobResponse> getJobs(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        JobQuery query = managementService.createJobQuery();

        if (allRequestParams.containsKey("id")) {
            query.jobId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("caseInstanceId")) {
            query.scopeId(allRequestParams.get("caseInstanceId"));
            query.scopeType(ScopeTypes.CMMN);
        }
        if (allRequestParams.containsKey("withoutScopeId") && Boolean.parseBoolean(allRequestParams.get("withoutScopeId"))) {
            query.withoutScopeId();
        }
        if (allRequestParams.containsKey("planItemInstanceId")) {
            query.subScopeId(allRequestParams.get("planItemInstanceId"));
            query.scopeType(ScopeTypes.CMMN);
        }
        if (allRequestParams.containsKey("caseDefinitionId")) {
            query.caseDefinitionId(allRequestParams.get("caseDefinitionId"));
        }
        if (allRequestParams.containsKey("scopeDefinitionId")) {
            query.scopeDefinitionId(allRequestParams.get("scopeDefinitionId"));
            query.scopeType(ScopeTypes.CMMN);
        }
        if (allRequestParams.containsKey("elementId")) {
            query.elementId(allRequestParams.get("elementId"));
        }
        if (allRequestParams.containsKey("elementName")) {
            query.elementName(allRequestParams.get("elementName"));
        }
        if (allRequestParams.containsKey("timersOnly")) {
            if (allRequestParams.containsKey("messagesOnly")) {
                throw new FlowableIllegalArgumentException("Only one of 'timersOnly' or 'messagesOnly' can be provided.");
            }
            if (Boolean.parseBoolean(allRequestParams.get("timersOnly"))) {
                query.timers();
            }
        }
        if (allRequestParams.containsKey("messagesOnly") && Boolean.parseBoolean(allRequestParams.get("messagesOnly"))) {
            query.messages();
        }
        if (allRequestParams.containsKey("dueBefore")) {
            query.duedateLowerThan(RequestUtil.getDate(allRequestParams, "dueBefore"));
        }
        if (allRequestParams.containsKey("dueAfter")) {
            query.duedateHigherThan(RequestUtil.getDate(allRequestParams, "dueAfter"));
        }
        if (allRequestParams.containsKey("withException") && Boolean.parseBoolean(allRequestParams.get("withException"))) {
            query.withException();
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
        if (allRequestParams.containsKey("withoutTenantId") && Boolean.parseBoolean(allRequestParams.get("withoutTenantId"))) {
            query.jobWithoutTenantId();
        }
        if (allRequestParams.containsKey("locked") && Boolean.parseBoolean(allRequestParams.get("locked"))) {
            query.locked();
        }
        if (allRequestParams.containsKey("unlocked") && Boolean.parseBoolean(allRequestParams.get("unlocked"))) {
            query.unlocked();
        }
        if (allRequestParams.containsKey("scopeType")) {
            query.scopeType(allRequestParams.get("scopeType"));
        }
        if (allRequestParams.containsKey("withoutProcessInstanceId") && Boolean.parseBoolean(allRequestParams.get("withoutProcessInstanceId"))) {
            query.withoutProcessInstanceId();
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessJobInfoWithQuery(query);
        }

        return paginateList(allRequestParams, query, "id", JobQueryProperties.PROPERTIES, restResponseFactory::createJobResponseList);
    }

    @ApiOperation(value = "Move a bulk of deadletter jobs. Accepts 'move' and 'moveToHistoryJob' as action.", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the dead letter jobs where moved. Response-body is intentionally empty."),
            @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @PostMapping("/cmmn-management/deadletter-jobs")
    public void executeDeadLetterJobAction(@RequestBody BulkMoveDeadLetterActionRequest actionRequest) {
        if (actionRequest == null || !(MOVE_ACTION.equals(actionRequest.getAction()) || MOVE_TO_HISTORY_JOB_ACTION.equals(actionRequest.getAction()))) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'move' or 'moveToHistoryJob' is supported.");
        }

        Collection<String> jobIds = actionRequest.getJobIds();
        long existingJobIdCount = managementService.createDeadLetterJobQuery().jobIds(jobIds).count();
        if (jobIds.size() != existingJobIdCount) {
            List<Job> foundJobs = managementService.createDeadLetterJobQuery().jobIds(jobIds).list();
            for (Job job : foundJobs) {
                jobIds.remove(job.getId());
            }
            throw new FlowableObjectNotFoundException(
                    "Could not find a dead letter job(s) with id(s) {" + jobIds.stream().collect(Collectors.joining(",")) + "}", Job.class);
        }
        if (MOVE_ACTION.equals(actionRequest.getAction())) {
            if (restApiInterceptor != null) {
                restApiInterceptor.bulkMoveDeadLetterJobs(actionRequest.getJobIds(), MOVE_ACTION);
            }
            managementService.bulkMoveDeadLetterJobs(actionRequest.getJobIds(), cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        } else if (MOVE_TO_HISTORY_JOB_ACTION.equals(actionRequest.getAction())) {
            if (restApiInterceptor != null) {
                restApiInterceptor.bulkMoveDeadLetterJobs(actionRequest.getJobIds(), MOVE_TO_HISTORY_JOB_ACTION);
            }
            managementService.bulkMoveDeadLetterJobsToHistoryJobs(actionRequest.getJobIds(), cmmnEngineConfiguration.getAsyncHistoryExecutorNumberOfRetries());
        }
    }
}
