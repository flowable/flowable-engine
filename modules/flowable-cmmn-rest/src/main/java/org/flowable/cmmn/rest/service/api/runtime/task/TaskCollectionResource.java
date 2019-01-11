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

package org.flowable.cmmn.rest.service.api.runtime.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@Api(tags = { "Tasks" }, description = "Manage Tasks", authorizations = { @Authorization(value = "basicAuth") })
public class TaskCollectionResource extends TaskBaseResource {

    @ApiOperation(value = "List of tasks", nickname="listTasks", tags = { "Tasks" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return models with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return tasks with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "description", dataType = "string", value = "Only return tasks with the given description.", paramType = "query"),
            @ApiImplicitParam(name = "priority", dataType = "string", value = "Only return tasks with the given priority.", paramType = "query"),
            @ApiImplicitParam(name = "minimumPriority", dataType = "string", value = "Only return tasks with a priority greater than the given value.", paramType = "query"),
            @ApiImplicitParam(name = "maximumPriority", dataType = "string", value = "Only return tasks with a priority lower than the given value.", paramType = "query"),
            @ApiImplicitParam(name = "assignee", dataType = "string", value = "Only return tasks assigned to the given user.", paramType = "query"),
            @ApiImplicitParam(name = "assigneeLike", dataType = "string", value = "Only return tasks assigned with an assignee like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "owner", dataType = "string", value = "Only return tasks owned by the given user.", paramType = "query"),
            @ApiImplicitParam(name = "ownerLike", dataType = "string", value = "Only return tasks assigned with an owner like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "unassigned", dataType = "string", value = "Only return tasks that are not assigned to anyone. If false is passed, the value is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "delegationState", dataType = "string", value = "Only return tasks that have the given delegation state. Possible values are pending and resolved.", paramType = "query"),
            @ApiImplicitParam(name = "candidateUser", dataType = "string", value = "Only return tasks that can be claimed by the given user. This includes both tasks where the user is an explicit candidate for and task that are claimable by a group that the user is a member of.", paramType = "query"),
            @ApiImplicitParam(name = "candidateGroup", dataType = "string", value = "Only return tasks that can be claimed by a user in the given group.", paramType = "query"),
            @ApiImplicitParam(name = "candidateGroups", dataType = "string", value = "Only return tasks that can be claimed by a user in the given groups. Values split by comma.", paramType = "query"),
            @ApiImplicitParam(name = "involvedUser", dataType = "string", value = "Only return tasks in which the given user is involved.", paramType = "query"),
            @ApiImplicitParam(name = "taskDefinitionKey", dataType = "string", value = "Only return tasks with the given task definition id.", paramType = "query"),
            @ApiImplicitParam(name = "taskDefinitionKeyLike", dataType = "string", value = "Only return tasks with a given task definition id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "Only return tasks which are part of the case instance with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceIdWithChildren", dataType = "string", value = "Only return tasks which are part of the case instance and its children with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "Only return tasks which are part of a case instance which has a case definition with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "createdOn", dataType = "string",format = "date-time", value = "Only return tasks which are created on the given date.", paramType = "query"),
            @ApiImplicitParam(name = "createdBefore", dataType = "string",format = "date-time", value = "Only return tasks which are created before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "createdAfter", dataType = "string",format = "date-time", value = "Only return tasks which are created after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "dueOn", dataType = "string",format = "date-time", value = "Only return tasks which are due on the given date.", paramType = "query"),
            @ApiImplicitParam(name = "dueBefore", dataType = "string", format = "date-time", value = "Only return tasks which are due before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "dueAfter", dataType = "string", format = "date-time", value = "Only return tasks which are due after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "withoutDueDate", dataType = "boolean", value = "Only return tasks which do not have a due date. The property is ignored if the value is false.", paramType = "query"),
            @ApiImplicitParam(name = "excludeSubTasks", dataType = "boolean", value = "Only return tasks that are not a subtask of another task.", paramType = "query"),
            @ApiImplicitParam(name = "active", dataType = "boolean", value = "If true, only return tasks that are not suspended (either part of a process that is not suspended or not part of a process at all). If false, only tasks that are part of suspended process instances are returned.", paramType = "query"),
            @ApiImplicitParam(name = "includeTaskLocalVariables", dataType = "boolean", value = "Indication to include task local variables in the result.", paramType = "query"),
            @ApiImplicitParam(name = "includeProcessVariables", dataType = "boolean", value = "Indication to include process variables in the result.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return tasks with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return tasks with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns tasks without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "candidateOrAssigned", dataType = "string", value = "Select tasks that has been claimed or assigned to user or waiting to claim by user (candidate user or groups).", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string", value = "Select tasks with the given category. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).\n", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the tasks are returned"),
            @ApiResponse(code = 404, message = "Indicates a parameter was passed in the wrong format or that delegationState has an invalid value (other than pending and resolved). The status-message contains additional information.")
    })
    @GetMapping(value = "/cmmn-runtime/tasks", produces = "application/json")
    public DataResponse<TaskResponse> getTasks(@ApiParam(hidden = true) @RequestParam Map<String, String> requestParams, HttpServletRequest httpRequest) {
        // Create a Task query request
        TaskQueryRequest request = new TaskQueryRequest();

        // Populate filter-parameters
        if (requestParams.containsKey("name")) {
            request.setName(requestParams.get("name"));
        }

        if (requestParams.containsKey("nameLike")) {
            request.setNameLike(requestParams.get("nameLike"));
        }

        if (requestParams.containsKey("description")) {
            request.setDescription(requestParams.get("description"));
        }

        if (requestParams.containsKey("descriptionLike")) {
            request.setDescriptionLike(requestParams.get("descriptionLike"));
        }

        if (requestParams.containsKey("priority")) {
            request.setPriority(Integer.valueOf(requestParams.get("priority")));
        }

        if (requestParams.containsKey("minimumPriority")) {
            request.setMinimumPriority(Integer.valueOf(requestParams.get("minimumPriority")));
        }

        if (requestParams.containsKey("maximumPriority")) {
            request.setMaximumPriority(Integer.valueOf(requestParams.get("maximumPriority")));
        }

        if (requestParams.containsKey("assignee")) {
            request.setAssignee(requestParams.get("assignee"));
        }

        if (requestParams.containsKey("assigneeLike")) {
            request.setAssigneeLike(requestParams.get("assigneeLike"));
        }

        if (requestParams.containsKey("owner")) {
            request.setOwner(requestParams.get("owner"));
        }

        if (requestParams.containsKey("ownerLike")) {
            request.setOwnerLike(requestParams.get("ownerLike"));
        }

        if (requestParams.containsKey("unassigned")) {
            request.setUnassigned(Boolean.valueOf(requestParams.get("unassigned")));
        }

        if (requestParams.containsKey("delegationState")) {
            request.setDelegationState(requestParams.get("delegationState"));
        }

        if (requestParams.containsKey("candidateUser")) {
            request.setCandidateUser(requestParams.get("candidateUser"));
        }

        if (requestParams.containsKey("involvedUser")) {
            request.setInvolvedUser(requestParams.get("involvedUser"));
        }

        if (requestParams.containsKey("candidateGroup")) {
            request.setCandidateGroup(requestParams.get("candidateGroup"));
        }

        if (requestParams.containsKey("candidateGroups")) {
            String[] candidateGroups = requestParams.get("candidateGroups").split(",");
            List<String> groups = new ArrayList<>(candidateGroups.length);
            Collections.addAll(groups, candidateGroups);
            request.setCandidateGroupIn(groups);
        }

        if (requestParams.containsKey("caseDefinitionId")) {
            request.setCaseDefinitionId(requestParams.get("caseDefinitionId"));
        }

        if (requestParams.containsKey("caseInstanceId")) {
            request.setCaseInstanceId(requestParams.get("caseInstanceId"));
        }
        
        if (requestParams.containsKey("caseInstanceIdWithChildren")) {
            request.setCaseInstanceIdWithChildren(requestParams.get("caseInstanceIdWithChildren"));
        }

        if (requestParams.containsKey("createdOn")) {
            request.setCreatedOn(RequestUtil.getDate(requestParams, "createdOn"));
        }

        if (requestParams.containsKey("createdBefore")) {
            request.setCreatedBefore(RequestUtil.getDate(requestParams, "createdBefore"));
        }

        if (requestParams.containsKey("createdAfter")) {
            request.setCreatedAfter(RequestUtil.getDate(requestParams, "createdAfter"));
        }

        if (requestParams.containsKey("excludeSubTasks")) {
            request.setExcludeSubTasks(Boolean.valueOf(requestParams.get("excludeSubTasks")));
        }

        if (requestParams.containsKey("taskDefinitionKey")) {
            request.setTaskDefinitionKey(requestParams.get("taskDefinitionKey"));
        }

        if (requestParams.containsKey("taskDefinitionKeyLike")) {
            request.setTaskDefinitionKeyLike(requestParams.get("taskDefinitionKeyLike"));
        }

        if (requestParams.containsKey("dueDate")) {
            request.setDueDate(RequestUtil.getDate(requestParams, "dueDate"));
        }

        if (requestParams.containsKey("dueBefore")) {
            request.setDueBefore(RequestUtil.getDate(requestParams, "dueBefore"));
        }

        if (requestParams.containsKey("dueAfter")) {
            request.setDueAfter(RequestUtil.getDate(requestParams, "dueAfter"));
        }

        if (requestParams.containsKey("active")) {
            request.setActive(Boolean.valueOf(requestParams.get("active")));
        }

        if (requestParams.containsKey("includeTaskLocalVariables")) {
            request.setIncludeTaskLocalVariables(Boolean.valueOf(requestParams.get("includeTaskLocalVariables")));
        }

        if (requestParams.containsKey("tenantId")) {
            request.setTenantId(requestParams.get("tenantId"));
        }

        if (requestParams.containsKey("tenantIdLike")) {
            request.setTenantIdLike(requestParams.get("tenantIdLike"));
        }

        if (requestParams.containsKey("withoutTenantId") && Boolean.valueOf(requestParams.get("withoutTenantId"))) {
            request.setWithoutTenantId(Boolean.TRUE);
        }

        if (requestParams.containsKey("candidateOrAssigned")) {
            request.setCandidateOrAssigned(requestParams.get("candidateOrAssigned"));
        }

        if (requestParams.containsKey("category")) {
            request.setCategory(requestParams.get("category"));
        }

        return getTasksFromQueryRequest(request, requestParams);
    }

    @ApiOperation(value = "Create Task", tags = { "Tasks" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates request was successful and the tasks are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format or that delegationState has an invalid value (other than pending and resolved). The status-message contains additional information.")
    })
    @PostMapping(value = "/cmmn-runtime/tasks", produces = "application/json")
    public TaskResponse createTask(@RequestBody TaskRequest taskRequest, HttpServletRequest request, HttpServletResponse response) {
        Task task = taskService.newTask();

        // Populate the task properties based on the request
        populateTaskFromRequest(task, taskRequest);
        if (taskRequest.isTenantIdSet()) {
            ((TaskEntity) task).setTenantId(taskRequest.getTenantId());
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.createTask(task, taskRequest);
        }
        
        taskService.saveTask(task);

        response.setStatus(HttpStatus.CREATED.value());
        return restResponseFactory.createTaskResponse(task);
    }
}
