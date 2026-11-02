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

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.rest.service.api.RestResponseFactory;
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
@Api(tags = { "History Process" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricProcessInstanceResource extends HistoricProcessInstanceBaseResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;

    @ApiOperation(value = "Get a historic process instance", tags = { "History Process" }, nickname = "getHistoricProcessInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that the historic process instances could be found."),
            @ApiResponse(code = 404, message = "Indicates that the historic process instances could not be found.") })
    @GetMapping(value = "/history/historic-process-instances/{processInstanceId}", produces = "application/json")
    public HistoricProcessInstanceResponse getProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId) {
        HistoricProcessInstanceResponse processInstanceResponse = restResponseFactory.createHistoricProcessInstanceResponse(getHistoricProcessInstanceFromRequest(processInstanceId));
        
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstanceResponse.getProcessDefinitionId()).singleResult();
        
        if (processDefinition != null) {
            processInstanceResponse.setProcessDefinitionName(processDefinition.getName());
            processInstanceResponse.setProcessDefinitionDescription(processDefinition.getDescription());
        }
        
        return processInstanceResponse;
    }

    @ApiOperation(value = " Delete a historic process instance", tags = { "History Process" }, nickname = "deleteHistoricProcessInstance", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates that the historic process instance was deleted."),
            @ApiResponse(code = 404, message = "Indicates that the historic process instance could not be found.") })
    @DeleteMapping(value = "/history/historic-process-instances/{processInstanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId) {
        HistoricProcessInstance processInstance = getHistoricProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteHistoricProcess(processInstance);
        }
        
        historyService.deleteHistoricProcessInstance(processInstanceId);
    }
}
