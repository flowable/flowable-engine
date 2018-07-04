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

package org.flowable.rest.service.api.history;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.engine.HistoryService;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Api(tags = { "History Task" }, description = "Manage History Task Instances", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricTaskInstanceResource extends HistoricTaskInstanceBaseResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;
    
    @ApiOperation(value = "Get a single historic task instance", tags = { "History Task" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that the historic task instances could be found."),
            @ApiResponse(code = 404, message = "Indicates that the historic task instances could not be found.") })
    @GetMapping(value = "/history/historic-task-instances/{taskId}", produces = "application/json")
    public HistoricTaskInstanceResponse getTaskInstance(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletRequest request) {
        return restResponseFactory.createHistoricTaskInstanceResponse(getHistoricTaskInstanceFromRequest(taskId));
    }

    @ApiOperation(value = "Delete a historic task instance", tags = { "History Task" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates that the historic task instance was deleted."),
            @ApiResponse(code = 404, message = "Indicates that the historic task instance could not be found.") })
    @DeleteMapping(value = "/history/historic-task-instances/{taskId}")
    public void deleteTaskInstance(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletResponse response) {
        HistoricTaskInstance task = getHistoricTaskInstanceFromRequest(taskId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteHistoricTask(task);
        }
        
        historyService.deleteHistoricTaskInstance(taskId);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
