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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
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
public class TaskSubTaskCollectionResource extends TaskBaseResource {

    @ApiOperation(value = "List of sub tasks for a task", nickname="listTaskSubtasks", tags = { "Tasks" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the  sub tasks are returned"),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @GetMapping(value = "/cmmn-runtime/tasks/{taskId}/subtasks", produces = "application/json")
    public List<TaskResponse> getSubTasks(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletRequest request) {
        Task task = getTaskFromRequest(taskId);
        return restResponseFactory.createTaskResponseList(taskService.getSubTasks(task.getId()));
    }
}
