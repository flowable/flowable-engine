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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.rest.service.api.CmmnFormHandlerRestApiInterceptor;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.cmmn.rest.service.api.FormModelResponse;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.form.api.FormInfo;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "History Task" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricTaskInstanceResource extends HistoricTaskInstanceBaseResource {

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnHistoryService historyService;
    
    @Autowired
    protected CmmnTaskService taskService;
    
    @Autowired(required=false)
    protected CmmnFormHandlerRestApiInterceptor formHandlerRestApiInterceptor;

    @ApiOperation(value = "Get a single historic task instance", tags = { "History Task" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that the historic task instances could be found."),
            @ApiResponse(code = 404, message = "Indicates that the historic task instances could not be found.") })
    @GetMapping(value = "/cmmn-history/historic-task-instances/{taskId}", produces = "application/json")
    public HistoricTaskInstanceResponse getTaskInstance(@ApiParam(name = "taskId") @PathVariable String taskId) {
        return restResponseFactory.createHistoricTaskInstanceResponse(getHistoricTaskInstanceFromRequest(taskId));
    }

    @ApiOperation(value = "Delete a historic task instance", tags = { "History Task" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates that the historic task instance was deleted."),
            @ApiResponse(code = 404, message = "Indicates that the historic task instance could not be found.") })
    @DeleteMapping(value = "/cmmn-history/historic-task-instances/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskInstance(@ApiParam(name = "taskId") @PathVariable String taskId) {
        HistoricTaskInstance task = getHistoricTaskInstanceFromRequestWithoutAccessCheck(taskId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteHistoricTask(task);
        }
        
        historyService.deleteHistoricTaskInstance(taskId);
    }
    
    @ApiOperation(value = "Get a historic task instance form", tags = { "History Task" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the task form is returned"),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @GetMapping(value = "/cmmn-history/historic-task-instances/{taskId}/form", produces = "application/json")
    public String getTaskForm(@ApiParam(name = "taskId") @PathVariable String taskId) {
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
