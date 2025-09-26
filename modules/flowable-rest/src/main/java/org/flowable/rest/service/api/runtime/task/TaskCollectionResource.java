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

package org.flowable.rest.service.api.runtime.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
 * @author Christopher Welsch
 */
@RestController
@Api(tags = { "Tasks" }, authorizations = { @Authorization(value = "basicAuth") })
public class TaskCollectionResource extends TaskBaseResource {

    @ApiOperation(value = "List of tasks", nickname="listTasks", tags = { "Tasks" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", dataType = "string", value = "Only return tasks with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return tasks with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return tasks with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLikeIgnoreCase", dataType = "string", value = "Only return tasks with a name like the given name ignoring case.", paramType = "query"),
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
            @ApiImplicitParam(name = "ignoreAssignee", dataType = "boolean", value = "Allows to select a task (typically in combination with candidateGroups or candidateUser) and ignore the assignee (as claimed tasks will not be returned when using candidateGroup or candidateUser)"),
            @ApiImplicitParam(name = "involvedUser", dataType = "string", value = "Only return tasks in which the given user is involved.", paramType = "query"),
            @ApiImplicitParam(name = "taskDefinitionKey", dataType = "string", value = "Only return tasks with the given task definition id.", paramType = "query"),
            @ApiImplicitParam(name = "taskDefinitionKeyLike", dataType = "string", value = "Only return tasks with a given task definition id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "taskDefinitionKeys", dataType = "string", value = "Only return tasks with the given task definition ids.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceId", dataType = "string", value = "Only return tasks which are part of the process instance with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceIdWithChildren", dataType = "string", value = "Only return tasks which are part of the process instance and its children with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "withoutProcessInstanceId", dataType = "boolean", value = "If true, only returns tasks without a process instance id set. If false, the withoutProcessInstanceId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceBusinessKey", dataType = "string", value = "Only return tasks which are part of the process instance with the given business key.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceBusinessKeyLike", dataType = "string", value = "Only return tasks which are part of the process instance which has a business key like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionId", dataType = "string", value = "Only return tasks which are part of a process instance which has a process definition with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionKey", dataType = "string", value = "Only return tasks which are part of a process instance which has a process definition with the given key.", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionKeyLike", dataType = "string", value = "Only return tasks which are part of a process instance which has a process definition with a key like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionName", dataType = "string", value = "Only return tasks which are part of a process instance which has a process definition with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionNameLike", dataType = "string", value = "Only return tasks which are part of a process instance which has a process definition with a name like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "executionId", dataType = "string", value = "Only return tasks which are part of the execution with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "createdOn", dataType = "string",format = "date-time", value = "Only return tasks which are created on the given date.", paramType = "query"),
            @ApiImplicitParam(name = "createdBefore", dataType = "string",format = "date-time", value = "Only return tasks which are created before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "createdAfter", dataType = "string",format = "date-time", value = "Only return tasks which are created after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "dueDate", dataType = "string",format = "date-time", value = "Only return tasks which are due on the given date.", paramType = "query"),
            @ApiImplicitParam(name = "dueBefore", dataType = "string", format = "date-time", value = "Only return tasks which are due before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "dueAfter", dataType = "string", format = "date-time", value = "Only return tasks which are due after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "withoutDueDate", dataType = "boolean", value = "Only return tasks which do not have a due date. The property is ignored if the value is false.", paramType = "query"),
            @ApiImplicitParam(name = "excludeSubTasks", dataType = "boolean", value = "Only return tasks that are not a subtask of another task.", paramType = "query"),
            @ApiImplicitParam(name = "active", dataType = "boolean", value = "If true, only return tasks that are not suspended (either part of a process that is not suspended or not part of a process at all). If false, only tasks that are part of suspended process instances are returned.", paramType = "query"),
            @ApiImplicitParam(name = "includeTaskLocalVariables", dataType = "boolean", value = "Indication to include task local variables in the result.", paramType = "query"),
            @ApiImplicitParam(name = "includeProcessVariables", dataType = "boolean", value = "Indication to include process variables in the result.", paramType = "query"),
            @ApiImplicitParam(name = "scopeDefinitionId", dataType = "string", value = "Only return tasks with the given scopeDefinitionId.", paramType = "query"),
            @ApiImplicitParam(name = "scopeId", dataType = "string", value = "Only return tasks with the given scopeId.", paramType = "query"),
            @ApiImplicitParam(name = "scopeIds", dataType = "string", value = "Only return tasks with one of the given scopeIds.", paramType = "query"),
            @ApiImplicitParam(name = "withoutScopeId", dataType = "boolean", value = "If true, only returns tasks without a scope id set. If false, the withoutScopeId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "scopeType", dataType = "string", value = "Only return tasks with the given scopeType.", paramType = "query"),
            @ApiImplicitParam(name = "propagatedStageInstanceId", dataType = "string", value = "Only return tasks which have the given id as propagated stage instance id", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return tasks with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return tasks with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns tasks without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "candidateOrAssigned", dataType = "string", value = "Select tasks that has been claimed or assigned to user or waiting to claim by user (candidate user or groups).", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string", value = "Select tasks with the given category. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).\n", paramType = "query"),
            @ApiImplicitParam(name = "categoryIn", dataType = "string", value = "Select tasks for the given categories. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).\n", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotIn", dataType = "string", value = "Select tasks which are not assigned to the given categories. Does not return tasks without categories. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).\n", paramType = "query"),
            @ApiImplicitParam(name = "withoutCategory", dataType = "string", value = "Select tasks without a category assigned. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).\n", paramType = "query"),
            @ApiImplicitParam(name = "rootScopeId", dataType = "string", value = "Only return tasks which have the given root scope id (that can be a process or case instance ID).", paramType = "query"),
            @ApiImplicitParam(name = "parentScopeId", dataType = "string", value = "Only return tasks which have the given parent scope id (that can be a process or case instance ID).", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "The field to sort by. Defaults to 'id'.", allowableValues = "id,name,description,dueDate,createTime,priority,executionId,processInstanceId,tenantId,assignee,owner", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the tasks are returned"),
            @ApiResponse(code = 404, message = "Indicates a parameter was passed in the wrong format or that delegationState has an invalid value (other than pending and resolved). The status-message contains additional information.")
    })
    @GetMapping(value = "/runtime/tasks", produces = "application/json")
    public DataResponse<TaskResponse> getTasks(@ApiParam(hidden = true) @RequestParam Map<String, String> requestParams) {
        // Create a Task query request
        TaskQueryRequest request = new TaskQueryRequest();

        // Populate filter-parameters
        if (requestParams.containsKey("taskId")) {
            request.setTaskId(requestParams.get("taskId"));
        }
        if (requestParams.containsKey("name")) {
            request.setName(requestParams.get("name"));
        }

        if (requestParams.containsKey("nameLike")) {
            request.setNameLike(requestParams.get("nameLike"));
        }
        
        if (requestParams.containsKey("nameLikeIgnoreCase")) {
            request.setNameLikeIgnoreCase(requestParams.get("nameLikeIgnoreCase"));
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
            List<String> groups = csvToList("candidateGroups", requestParams);
            request.setCandidateGroupIn(groups);
        }

        if (requestParams.containsKey("ignoreAssignee") && Boolean.valueOf(requestParams.get("ignoreAssignee"))) {
            request.setIgnoreAssignee(true);
        }

        if (requestParams.containsKey("processDefinitionId")) {
            request.setProcessDefinitionId(requestParams.get("processDefinitionId"));
        }

        if (requestParams.containsKey("processDefinitionKey")) {
            request.setProcessDefinitionKey(requestParams.get("processDefinitionKey"));
        }

        if (requestParams.containsKey("processDefinitionKeyLike")) {
            request.setProcessDefinitionKeyLike(requestParams.get("processDefinitionKeyLike"));
        }

        if (requestParams.containsKey("processDefinitionName")) {
            request.setProcessDefinitionName(requestParams.get("processDefinitionName"));
        }

        if (requestParams.containsKey("processDefinitionNameLike")) {
            request.setProcessDefinitionNameLike(requestParams.get("processDefinitionNameLike"));
        }

        if (requestParams.containsKey("processInstanceId")) {
            request.setProcessInstanceId(requestParams.get("processInstanceId"));
        }
        
        if (requestParams.containsKey("processInstanceIdWithChildren")) {
            request.setProcessInstanceIdWithChildren(requestParams.get("processInstanceIdWithChildren"));
        }
        
        if (requestParams.containsKey("withoutProcessInstanceId") && Boolean.valueOf(requestParams.get("withoutProcessInstanceId"))) {
            request.setWithoutProcessInstanceId(Boolean.TRUE);
        }

        if (requestParams.containsKey("processInstanceBusinessKey")) {
            request.setProcessInstanceBusinessKey(requestParams.get("processInstanceBusinessKey"));
        }

        if (requestParams.containsKey("processInstanceBusinessKeyLike")) {
            request.setProcessInstanceBusinessKeyLike(requestParams.get("processInstanceBusinessKeyLike"));
        }

        if (requestParams.containsKey("executionId")) {
            request.setExecutionId(requestParams.get("executionId"));
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

        if (requestParams.containsKey("taskDefinitionKeys")) {
            request.setTaskDefinitionKeys(Arrays.asList(requestParams.get("taskDefinitionKeys").split(",")));
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

        if (requestParams.containsKey("includeProcessVariables")) {
            request.setIncludeProcessVariables(Boolean.valueOf(requestParams.get("includeProcessVariables")));
        }
        
        if (requestParams.containsKey("scopeDefinitionId")) {
            request.setScopeDefinitionId(requestParams.get("scopeDefinitionId"));
        }
        
        if (requestParams.containsKey("scopeId")) {
            request.setScopeId(requestParams.get("scopeId"));
        }

        if (requestParams.containsKey("scopeIds")) {
            request.setScopeIds(RequestUtil.parseToSet(requestParams.get("scopeIds")));
        }
        
        if (requestParams.containsKey("withoutScopeId") && Boolean.valueOf(requestParams.get("withoutScopeId"))) {
            request.setWithoutScopeId(Boolean.TRUE);
        }
        
        if (requestParams.containsKey("scopeType")) {
            request.setScopeType(requestParams.get("scopeType"));
        }

        if (requestParams.containsKey("propagatedStageInstanceId")) {
            request.setPropagatedStageInstanceId(requestParams.get("propagatedStageInstanceId"));
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

        if (requestParams.containsKey("withoutCategory") && Boolean.valueOf(requestParams.get("withoutCategory"))) {
            request.setWithoutCategory(Boolean.TRUE);
        }

        if (requestParams.containsKey("categoryIn")) {
            request.setCategoryIn(csvToList("categoryIn", requestParams));
        }

        if (requestParams.containsKey("categoryNotIn")) {
            request.setCategoryNotIn(csvToList("categoryNotIn", requestParams));
        }

        if (requestParams.containsKey("rootScopeId")) {
            request.setRootScopeId(requestParams.get("rootScopeId"));
        }

        if (requestParams.containsKey("parentScopeId")) {
            request.setParentScopeId(requestParams.get("parentScopeId"));
        }

        return getTasksFromQueryRequest(request, requestParams);
    }

    @ApiOperation(value = "Create Task", tags = { "Tasks" }, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates request was successful and the tasks are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format or that delegationState has an invalid value (other than pending and resolved). The status-message contains additional information.")
    })
    @PostMapping(value = "/runtime/tasks", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@RequestBody TaskRequest taskRequest) {

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

        return restResponseFactory.createTaskResponse(task);
    }

    @ApiOperation(value = "Update Tasks", tags = { "Tasks" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates request was successful and the tasks are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format or that delegationState has an invalid value (other than pending and resolved). The status-message contains additional information.")
    })
    @PutMapping(value = "/runtime/tasks", produces = "application/json")
    public DataResponse<TaskResponse> bulkUpdateTasks(@RequestBody BulkTasksRequest bulkTasksRequest) {

        if (bulkTasksRequest == null) {
            throw new FlowableException("A request body was expected when bulk updating tasks.");
        }
        if (bulkTasksRequest.getTaskIds() == null) {
            throw new FlowableIllegalArgumentException("taskIds can not be null for bulk update tasks requests");
        }

        Collection<Task> taskList = getTasksFromIdList(bulkTasksRequest.getTaskIds());

        if (taskList.size() != bulkTasksRequest.getTaskIds().size()) {
            taskList.stream().forEach(task -> bulkTasksRequest.getTaskIds().remove(task.getId()));
            throw new FlowableObjectNotFoundException(
                    "Could not find task instance with id:" + bulkTasksRequest.getTaskIds().stream().collect(Collectors.joining(",")));
        }

        // Populate the task properties based on the request
        populateTasksFromRequest(taskList, bulkTasksRequest);

        if (restApiInterceptor != null) {
            restApiInterceptor.bulkUpdateTasks(taskList, bulkTasksRequest);
        }

        // Save the task and fetch again, it's possible that an
        // assignment-listener has updated
        // fields after it was saved so we can not use the in-memory task
        taskService.bulkSaveTasks(taskList);

        List<Task> taskResultList = getTasksFromIdList(bulkTasksRequest.getTaskIds());

        DataResponse<TaskResponse> dataResponse = new DataResponse<>();
        dataResponse.setData(restResponseFactory.createTaskResponseList(taskResultList));
        return dataResponse;
    }

    protected List<String> csvToList(String key, Map<String, String> requestParams) {
        String[] candidateGroupsSplit = requestParams.get(key).split(",");
        List<String> groups = new ArrayList<>(candidateGroupsSplit.length);
        Collections.addAll(groups, candidateGroupsSplit);
        return groups;
    }
}
