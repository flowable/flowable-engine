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

package org.flowable.cmmn.rest.service.api.history.task;

import java.util.Arrays;
import java.util.HashSet;
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
@RestController
@Api(tags = { "History Task" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricTaskInstanceCollectionResource extends HistoricTaskInstanceBaseResource {

    @ApiOperation(value = "List historic task instances", tags = { "History Task" }, nickname = "listHistoricTaskInstances")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "taskId", dataType = "string", value = "An id of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "The case instance id of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "caseInstanceIdWithChildren", dataType = "string", value = "Selects the historic task instance of a case instance and its children.", paramType = "query"),
        @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "The case definition id of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "propagatedStageInstanceId", dataType = "string", value = "Only return tasks which have the given id as propagated stage instance id", paramType = "query"),
        @ApiImplicitParam(name = "withoutScopeId", dataType = "boolean", value = "If true, only returns historic task instances without a scope id set. If false, the withoutScopeId parameter is ignored.", paramType = "query"),
        @ApiImplicitParam(name = "taskDefinitionKey", dataType = "string", value = "The task definition key for tasks part of a process", paramType = "query"),
        @ApiImplicitParam(name = "taskName", dataType = "string", value = "The task name of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskNameLike", dataType = "string", value = "The task name with like operator for the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskNameLikeIgnoreCase", dataType = "string", value = "The task name with like operator for the historic task instance ignoring case.", paramType = "query"),
        @ApiImplicitParam(name = "taskDescription", dataType = "string", value = "The task description of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskDescriptionLike", dataType = "string", value = "The task description with like operator for the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskCategory", dataType = "string", value = "Select tasks with the given category. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).", paramType = "query"),
        @ApiImplicitParam(name = "taskCategoryIn", dataType = "string", value = "Select tasks with the given categories. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).", paramType = "query"),
        @ApiImplicitParam(name = "taskCategoryNotIn", dataType = "string", value = "Select tasks not assigned to the given categories. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).", paramType = "query"),
        @ApiImplicitParam(name = "taskWithoutCategory", dataType = "string", value = "Select tasks with no category assigned. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).", paramType = "query"),
        @ApiImplicitParam(name = "taskDeleteReason", dataType = "string", value = "The task delete reason of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskDeleteReasonLike", dataType = "string", value = "The task delete reason with like operator for the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskAssignee", dataType = "string", value = "The assignee of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskAssigneeLike", dataType = "string", value = "The assignee with like operator for the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskOwner", dataType = "string", value = "The owner of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskOwnerLike", dataType = "string", value = "The owner with like operator for the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "taskInvolvedUser", dataType = "string", value = "An involved user of the historic task instance", paramType = "query"),
        @ApiImplicitParam(name = "taskCandidateGroup", dataType = "string", value = "Only return tasks that can be claimed by a user in the given group.", paramType = "query"),
        @ApiImplicitParam(name = "taskIgnoreAssignee", dataType = "boolean", value = "Allows to select a task (typically in combination with a candidateGroup) and ignore the assignee (as claimed tasks will not be returned when using candidateGroup)"),
        @ApiImplicitParam(name = "taskPriority", dataType = "string", value = "The priority of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "finished", dataType = "boolean", value = "Indication if the historic task instance is finished.", paramType = "query"),
        @ApiImplicitParam(name = "processFinished", dataType = "boolean", value = "Indication if the process instance of the historic task instance is finished.", paramType = "query"),
        @ApiImplicitParam(name = "parentTaskId", dataType = "string", value = "An optional parent task id of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "dueDate", dataType = "string", format="date-time", value = "Return only historic task instances that have a due date equal this date.", paramType = "query"),
        @ApiImplicitParam(name = "dueDateAfter", dataType = "string", format="date-time", value = "Return only historic task instances that have a due date after this date.", paramType = "query"),
        @ApiImplicitParam(name = "dueDateBefore", dataType = "string", format="date-time", value = "Return only historic task instances that have a due date before this date.", paramType = "query"),
        @ApiImplicitParam(name = "withoutDueDate", dataType = "boolean", value = "Return only historic task instances that have no due-date. When false is provided as value, this parameter is ignored.", paramType = "query"),
        @ApiImplicitParam(name = "taskCompletedOn", dataType = "string", format="date-time", value = "Return only historic task instances that have been completed on this date.", paramType = "query"),
        @ApiImplicitParam(name = "taskCompletedAfter", dataType = "string", format="date-time", value = "Return only historic task instances that have been completed after this date.", paramType = "query"),
        @ApiImplicitParam(name = "taskCompletedBefore", dataType = "string", format="date-time", value = "Return only historic task instances that have been completed before this date.", paramType = "query"),
        @ApiImplicitParam(name = "taskCreatedOn", dataType = "string", format="date-time", value = "Return only historic task instances that were created on this date.", paramType = "query"),
        @ApiImplicitParam(name = "taskCreatedBefore", dataType = "string", format="date-time", value = "Return only historic task instances that were created before this date.", paramType = "query"),
        @ApiImplicitParam(name = "taskCreatedAfter", dataType = "string", format="date-time", value = "Return only historic task instances that were created after this date.", paramType = "query"),
        @ApiImplicitParam(name = "includeTaskLocalVariables", dataType = "boolean", value = "An indication if the historic task instance local variables should be returned as well.", paramType = "query"),
        @ApiImplicitParam(name = "includeProcessVariables", dataType = "boolean", value = "Indication to include historic process variables in the result.", paramType = "query"),
        @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return historic task instances with the given tenantId.", paramType = "query"),
        @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return historic task instances with a tenantId like the given value.", paramType = "query"),
        @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns historic task instances without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
        @ApiImplicitParam(name = "withoutProcessInstanceId", dataType = "boolean", value = "If true, only returns historic task instances without a process instance id set. If false, the withoutProcessInstanceId parameter is ignored.", paramType = "query"),
        @ApiImplicitParam(name = "planItemInstanceId", dataType = "string", value = "The plan item instance instance id of the historic task instance.", paramType = "query"),
        @ApiImplicitParam(name = "rootScopeId", dataType = "string", value = "Only return case instances which have the given root scope id (that can be a process or case instance ID).", paramType = "query"),
        @ApiImplicitParam(name = "parentScopeId", dataType = "string", value = "Only return case instances which have the given parent scope id (that can be a process or case instance ID).", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that historic task instances could be queried."),
            @ApiResponse(code = 404, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.") })
    @GetMapping(value = "/cmmn-history/historic-task-instances", produces = "application/json")
    public DataResponse<HistoricTaskInstanceResponse> getHistoricProcessInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        // Populate query based on request
        HistoricTaskInstanceQueryRequest queryRequest = new HistoricTaskInstanceQueryRequest();

        if (allRequestParams.get("taskId") != null) {
            queryRequest.setTaskId(allRequestParams.get("taskId"));
        }

        if (allRequestParams.get("caseInstanceId") != null) {
            queryRequest.setCaseInstanceId(allRequestParams.get("caseInstanceId"));
        }
        
        if (allRequestParams.get("caseInstanceIdWithChildren") != null) {
            queryRequest.setCaseInstanceIdWithChildren(allRequestParams.get("caseInstanceIdWithChildren"));
        }

        if (allRequestParams.get("caseDefinitionId") != null) {
            queryRequest.setCaseDefinitionId(allRequestParams.get("caseDefinitionId"));
        }

        if (allRequestParams.get("propagatedStageInstanceId") != null) {
            queryRequest.setPropagatedStageInstanceId(allRequestParams.get("propagatedStageInstanceId"));
        }
        
        if (allRequestParams.get("withoutScopeId") != null) {
            queryRequest.setWithoutScopeId(Boolean.valueOf(allRequestParams.get("withoutScopeId")));
        }

        if (allRequestParams.get("taskName") != null) {
            queryRequest.setTaskName(allRequestParams.get("taskName"));
        }

        if (allRequestParams.get("taskNameLike") != null) {
            queryRequest.setTaskNameLike(allRequestParams.get("taskNameLike"));
        }
        
        if (allRequestParams.get("taskNameLikeIgnoreCase") != null) {
            queryRequest.setTaskNameLikeIgnoreCase(allRequestParams.get("taskNameLikeIgnoreCase"));
        }

        if (allRequestParams.get("taskDescription") != null) {
            queryRequest.setTaskDescription(allRequestParams.get("taskDescription"));
        }

        if (allRequestParams.get("taskDescriptionLike") != null) {
            queryRequest.setTaskDescriptionLike(allRequestParams.get("taskDescriptionLike"));
        }

        if (allRequestParams.get("taskDefinitionKey") != null) {
            queryRequest.setTaskDefinitionKey(allRequestParams.get("taskDefinitionKey"));
        }

        if (allRequestParams.containsKey("taskCategory")) {
            queryRequest.setTaskCategory(allRequestParams.get("taskCategory"));
        }

        if (allRequestParams.containsKey("taskCategoryIn")) {
            queryRequest.setTaskCategoryIn(Arrays.asList(allRequestParams.get("taskCategoryIn").split(",")));
        }

        if (allRequestParams.containsKey("taskCategoryNotIn")) {
            queryRequest.setTaskCategoryNotIn(Arrays.asList(allRequestParams.get("taskCategoryNotIn").split(",")));
        }

        if (allRequestParams.containsKey("taskWithoutCategory") && Boolean.parseBoolean(allRequestParams.get("taskWithoutCategory"))) {
            queryRequest.setTaskWithoutCategory(Boolean.TRUE);
        }

        if (allRequestParams.get("taskDeleteReason") != null) {
            queryRequest.setTaskDeleteReason(allRequestParams.get("taskDeleteReason"));
        }

        if (allRequestParams.get("taskDeleteReasonLike") != null) {
            queryRequest.setTaskDeleteReasonLike(allRequestParams.get("taskDeleteReasonLike"));
        }

        if (allRequestParams.get("taskAssignee") != null) {
            queryRequest.setTaskAssignee(allRequestParams.get("taskAssignee"));
        }

        if (allRequestParams.get("taskAssigneeLike") != null) {
            queryRequest.setTaskAssigneeLike(allRequestParams.get("taskAssigneeLike"));
        }

        if (allRequestParams.get("taskOwner") != null) {
            queryRequest.setTaskOwner(allRequestParams.get("taskOwner"));
        }

        if (allRequestParams.get("taskOwnerLike") != null) {
            queryRequest.setTaskOwnerLike(allRequestParams.get("taskOwnerLike"));
        }

        if (allRequestParams.get("taskInvolvedUser") != null) {
            queryRequest.setTaskInvolvedUser(allRequestParams.get("taskInvolvedUser"));
        }

        if (allRequestParams.get("taskPriority") != null) {
            queryRequest.setTaskPriority(Integer.valueOf(allRequestParams.get("taskPriority")));
        }

        if (allRequestParams.get("taskMinPriority") != null) {
            queryRequest.setTaskMinPriority(Integer.valueOf(allRequestParams.get("taskMinPriority")));
        }

        if (allRequestParams.get("taskMaxPriority") != null) {
            queryRequest.setTaskMaxPriority(Integer.valueOf(allRequestParams.get("taskMaxPriority")));
        }

        if (allRequestParams.get("finished") != null) {
            queryRequest.setFinished(Boolean.valueOf(allRequestParams.get("finished")));
        }

        if (allRequestParams.get("processFinished") != null) {
            queryRequest.setProcessFinished(Boolean.valueOf(allRequestParams.get("processFinished")));
        }

        if (allRequestParams.get("parentTaskId") != null) {
            queryRequest.setParentTaskId(allRequestParams.get("parentTaskId"));
        }

        if (allRequestParams.get("dueDate") != null) {
            queryRequest.setDueDate(RequestUtil.getDate(allRequestParams, "dueDate"));
        }

        if (allRequestParams.get("dueDateAfter") != null) {
            queryRequest.setDueDateAfter(RequestUtil.getDate(allRequestParams, "dueDateAfter"));
        }

        if (allRequestParams.get("dueDateBefore") != null) {
            queryRequest.setDueDateBefore(RequestUtil.getDate(allRequestParams, "dueDateBefore"));
        }

        if (allRequestParams.get("taskCreatedOn") != null) {
            queryRequest.setTaskCreatedOn(RequestUtil.getDate(allRequestParams, "taskCreatedOn"));
        }

        if (allRequestParams.get("taskCreatedBefore") != null) {
            queryRequest.setTaskCreatedBefore(RequestUtil.getDate(allRequestParams, "taskCreatedBefore"));
        }

        if (allRequestParams.get("taskCreatedAfter") != null) {
            queryRequest.setTaskCreatedAfter(RequestUtil.getDate(allRequestParams, "taskCreatedAfter"));
        }

        if (allRequestParams.get("taskCompletedOn") != null) {
            queryRequest.setTaskCompletedOn(RequestUtil.getDate(allRequestParams, "taskCompletedOn"));
        }

        if (allRequestParams.get("taskCompletedBefore") != null) {
            queryRequest.setTaskCompletedBefore(RequestUtil.getDate(allRequestParams, "taskCompletedBefore"));
        }

        if (allRequestParams.get("taskCompletedAfter") != null) {
            queryRequest.setTaskCompletedAfter(RequestUtil.getDate(allRequestParams, "taskCompletedAfter"));
        }

        if (allRequestParams.get("includeTaskLocalVariables") != null) {
            queryRequest.setIncludeTaskLocalVariables(Boolean.valueOf(allRequestParams.get("includeTaskLocalVariables")));
        }

        if (allRequestParams.containsKey("includeProcessVariables")) {
            queryRequest.setIncludeProcessVariables(Boolean.valueOf(allRequestParams.get("includeProcessVariables")));
        }

        if (allRequestParams.get("tenantId") != null) {
            queryRequest.setTenantId(allRequestParams.get("tenantId"));
        }

        if (allRequestParams.get("tenantIdLike") != null) {
            queryRequest.setTenantIdLike(allRequestParams.get("tenantIdLike"));
        }

        if (allRequestParams.get("scopeId") != null) {
            queryRequest.setScopeId(allRequestParams.get("scopeId"));
        }
        
        if (allRequestParams.get("scopeIds") != null) {
            queryRequest.setScopeIds(RequestUtil.parseToSet(allRequestParams.get("scopeIds")));
        }

        if (allRequestParams.get("withoutTenantId") != null) {
            queryRequest.setWithoutTenantId(Boolean.valueOf(allRequestParams.get("withoutTenantId")));
        }
        
        if (allRequestParams.get("withoutProcessInstanceId") != null) {
            queryRequest.setWithoutProcessInstanceId(Boolean.valueOf(allRequestParams.get("withoutProcessInstanceId")));
        }

        if (allRequestParams.get("taskCandidateGroup") != null) {
            queryRequest.setTaskCandidateGroup(allRequestParams.get("taskCandidateGroup"));
        }

        if (allRequestParams.containsKey("ignoreTaskAssignee") && Boolean.parseBoolean(allRequestParams.get("ignoreTaskAssignee"))) {
            queryRequest.setIgnoreTaskAssignee(true);
        }

        if (allRequestParams.get("planItemInstanceId") != null) {
            queryRequest.setPlanItemInstanceId(allRequestParams.get("planItemInstanceId"));
        }

        if (allRequestParams.containsKey("rootScopeId")) {
            queryRequest.setRootScopeId(allRequestParams.get("rootScopeId"));
        }

        if (allRequestParams.containsKey("parentScopeId")) {
            queryRequest.setParentScopeId(allRequestParams.get("parentScopeId"));
        }

        return getQueryResponse(queryRequest, allRequestParams);
    }
}
