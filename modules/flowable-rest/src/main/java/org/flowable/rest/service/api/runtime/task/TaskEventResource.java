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

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.task.Event;
import org.flowable.rest.service.api.engine.EventResponse;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Tasks" }, authorizations = { @Authorization(value = "basicAuth") })
public class TaskEventResource extends TaskBaseResource {

    @ApiOperation(value = "Get an event on a task", tags = { "Tasks" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task and event were found and the event is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the tasks does not have an event with the given ID.")
    })
    @GetMapping(value = "/runtime/tasks/{taskId}/events/{eventId}", produces = "application/json")
    public EventResponse getEvent(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "eventId") @PathVariable("eventId") String eventId) {

        HistoricTaskInstance task = getHistoricTaskFromRequest(taskId);

        Event event = taskService.getEvent(eventId);
        if (event == null || !task.getId().equals(event.getTaskId())) {
            throw new FlowableObjectNotFoundException("Task '" + task.getId() + "' does not have an event with id '" + eventId + "'.", Event.class);
        }

        return restResponseFactory.createEventResponse(event);
    }

    @ApiOperation(value = "Delete an event on a task", tags = { "Tasks" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the task was found and the events are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the task does not have the requested event.")
    })
    @DeleteMapping(value = "/runtime/tasks/{taskId}/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "eventId") @PathVariable("eventId") String eventId) {

        // Check if task exists
        Task task = getTaskFromRequestWithoutAccessCheck(taskId);

        Event event = taskService.getEvent(eventId);
        if (event == null || event.getTaskId() == null || !event.getTaskId().equals(task.getId())) {
            throw new FlowableObjectNotFoundException("Task '" + task.getId() + "' does not have an event with id '" + event + "'.", Event.class);
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.deleteTaskEvent(task, event);
        }

        taskService.deleteComment(eventId);
    }
}
