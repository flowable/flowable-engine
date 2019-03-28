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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.form.api.FormInfo;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.rest.service.api.FormHandlerRestApiInterceptor;
import org.flowable.rest.service.api.FormModelResponse;
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
    
    @Autowired
    protected TaskService taskService;
    
    @Autowired(required=false)
    protected FormHandlerRestApiInterceptor formHandlerRestApiInterceptor;
    
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
    
    @ApiOperation(value = "Get a historic task instance form", tags = { "History Task" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the task form is returned"),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @GetMapping(value = "/history/historic-task-instances/{taskId}/form", produces = "application/json")
    public String getTaskForm(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletRequest request) {
        HistoricTaskInstance task = getHistoricTaskInstanceFromRequest(taskId);
        if (StringUtils.isEmpty(task.getFormKey())) {
            throw new FlowableIllegalArgumentException("Task has no form defined");
        }
        
        FormInfo formInfo = taskService.getTaskFormModel(task.getId());
        if (formHandlerRestApiInterceptor != null) {
            return formHandlerRestApiInterceptor.convertHistoricTaskFormInfo(formInfo, task);
        } else {
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            return restResponseFactory.getFormModelString(new FormModelResponse(formInfo, formModel));
        }
    }
}
