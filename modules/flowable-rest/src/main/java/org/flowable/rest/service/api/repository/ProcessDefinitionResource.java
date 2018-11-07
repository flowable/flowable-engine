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

package org.flowable.rest.service.api.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Process Definitions" }, description = "Manage Process Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class ProcessDefinitionResource extends BaseProcessDefinitionResource {

    @ApiOperation(value = "Get a process definition", tags = { "Process Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the process-definitions are returned"),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}", produces = "application/json")
    public ProcessDefinitionResponse getProcessDefinition(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId, HttpServletRequest request) {
        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);

        return restResponseFactory.createProcessDefinitionResponse(processDefinition);
    }

    // FIXME Unique endpoint but with multiple actions
    @ApiOperation(value = "Execute actions for a process definition", tags = { "Process Definitions" },
            notes = "Execute actions for a process definition (Update category, Suspend or Activate)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates action has been executed for the specified process. (category altered, activate or suspend)"),
            @ApiResponse(code = 400, message = "Indicates no category was defined in the request body."),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found."),
            @ApiResponse(code = 409, message = "Indicates the requested process definition is already suspended or active.")
    })
    @PutMapping(value = "/repository/process-definitions/{processDefinitionId}", produces = "application/json")
    public ProcessDefinitionResponse executeProcessDefinitionAction(
            @ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId,
            @ApiParam(required = true) @RequestBody ProcessDefinitionActionRequest actionRequest,
            HttpServletRequest request) {

        if (actionRequest == null) {
            throw new FlowableIllegalArgumentException("No action found in request body.");
        }

        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);

        if (actionRequest.getCategory() != null) {
            // Update of category required
            repositoryService.setProcessDefinitionCategory(processDefinition.getId(), actionRequest.getCategory());

            // No need to re-fetch the ProcessDefinition entity, just update
            // category in response
            ProcessDefinitionResponse response = restResponseFactory.createProcessDefinitionResponse(processDefinition);
            response.setCategory(actionRequest.getCategory());
            return response;

        } else {
            // Actual action
            if (actionRequest.getAction() != null) {
                if (ProcessDefinitionActionRequest.ACTION_SUSPEND.equals(actionRequest.getAction())) {
                    return suspendProcessDefinition(processDefinition, actionRequest.isIncludeProcessInstances(), actionRequest.getDate());

                } else if (ProcessDefinitionActionRequest.ACTION_ACTIVATE.equals(actionRequest.getAction())) {
                    return activateProcessDefinition(processDefinition, actionRequest.isIncludeProcessInstances(), actionRequest.getDate());
                }
            }

            throw new FlowableIllegalArgumentException("Invalid action: '" + actionRequest.getAction() + "'.");
        }
    }

    protected ProcessDefinitionResponse activateProcessDefinition(ProcessDefinition processDefinition, boolean suspendInstances, Date date) {

        if (!repositoryService.isProcessDefinitionSuspended(processDefinition.getId())) {
            throw new FlowableConflictException("Process definition with id '" + processDefinition.getId() + " ' is already active");
        }
        repositoryService.activateProcessDefinitionById(processDefinition.getId(), suspendInstances, date);

        ProcessDefinitionResponse response = restResponseFactory.createProcessDefinitionResponse(processDefinition);

        // No need to re-fetch the ProcessDefinition, just alter the suspended
        // state of the result-object
        response.setSuspended(false);
        return response;
    }

    protected ProcessDefinitionResponse suspendProcessDefinition(ProcessDefinition processDefinition, boolean suspendInstances, Date date) {

        if (repositoryService.isProcessDefinitionSuspended(processDefinition.getId())) {
            throw new FlowableConflictException("Process definition with id '" + processDefinition.getId() + " ' is already suspended");
        }
        repositoryService.suspendProcessDefinitionById(processDefinition.getId(), suspendInstances, date);

        ProcessDefinitionResponse response = restResponseFactory.createProcessDefinitionResponse(processDefinition);

        // No need to re-fetch the ProcessDefinition, just alter the suspended
        // state of the result-object
        response.setSuspended(true);
        return response;
    }

}
