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

package org.flowable.rest.service.api.runtime.process;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.impl.dynamic.DynamicEmbeddedSubProcessBuilder;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
@Api(tags = { "Process Instances" }, description = "Manage Process Instances", authorizations = { @Authorization(value = "basicAuth") })
public class ProcessInstanceResource extends BaseProcessInstanceResource {
    
    @Autowired
    protected DynamicBpmnService dynamicBpmnService;

    @ApiOperation(value = "Get a process instance", tags = { "Process Instances" }, nickname = "getProcessInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @GetMapping(value = "/runtime/process-instances/{processInstanceId}", produces = "application/json")
    public ProcessInstanceResponse getProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, HttpServletRequest request) {
        return restResponseFactory.createProcessInstanceResponse(getProcessInstanceFromRequest(processInstanceId));
    }

    @ApiOperation(value = "Delete a process instance", tags = { "Process Instances" }, nickname = "deleteProcessInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the process instance was found and deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @DeleteMapping(value = "/runtime/process-instances/{processInstanceId}")
    public void deleteProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, @RequestParam(value = "deleteReason", required = false) String deleteReason, HttpServletResponse response) {

        ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);

        runtimeService.deleteProcessInstance(processInstance.getId(), deleteReason);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @ApiOperation(value = "Activate or suspend a process instance", tags = { "Process Instances" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and action was executed."),
            @ApiResponse(code = 400, message = "\t\n" + "Indicates an invalid action was supplied."),
            @ApiResponse(code = 409, message = "Indicates the requested process instance action cannot be executed since the process-instance is already activated/suspended."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @PutMapping(value = "/runtime/process-instances/{processInstanceId}", produces = "application/json")
    public ProcessInstanceResponse performProcessInstanceAction(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, @RequestBody ProcessInstanceActionRequest actionRequest, HttpServletRequest request) {

        ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);

        if (ProcessInstanceActionRequest.ACTION_ACTIVATE.equals(actionRequest.getAction())) {
            return activateProcessInstance(processInstance);

        } else if (ProcessInstanceActionRequest.ACTION_SUSPEND.equals(actionRequest.getAction())) {
            return suspendProcessInstance(processInstance);
        }
        throw new FlowableIllegalArgumentException("Invalid action: '" + actionRequest.getAction() + "'.");
    }

    @ApiOperation(value = "Change the state a process instance", tags = { "Process Instances" },
            notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and change state activity was executed."),
            @ApiResponse(code = 409, message = "Indicates the requested process instance action cannot be executed since the process-instance is already activated/suspended."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @PostMapping(value = "/runtime/process-instances/{processInstanceId}/change-state", produces = "application/json")
    public void changeActivityState(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId,
            @RequestBody ExecutionChangeActivityStateRequest activityStateRequest, HttpServletRequest request) {

        if (activityStateRequest.getCancelActivityIds() != null && activityStateRequest.getCancelActivityIds().size() == 1) {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveSingleActivityIdToActivityIds(activityStateRequest.getCancelActivityIds().get(0), activityStateRequest.getStartActivityIds())
                .changeState();
        
        } else if (activityStateRequest.getStartActivityIds() != null && activityStateRequest.getStartActivityIds().size() == 1) {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdsToSingleActivityId(activityStateRequest.getCancelActivityIds(), activityStateRequest.getStartActivityIds().get(0))
                .changeState();
        }
        
    }
    
    @ApiOperation(value = "Inject activity in a process instance", tags = { "Process Instances" },
            notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was updated and the activity injection was executed."),
            @ApiResponse(code = 409, message = "Indicates the requested process instance action cannot be executed since the process-instance is already activated/suspended."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @PostMapping(value = "/runtime/process-instances/{processInstanceId}/inject", produces = "application/json")
    public void injectActivityInProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId,
            @RequestBody InjectActivityRequest injectActivityRequest, HttpServletRequest request) {

        if ("task".equalsIgnoreCase(injectActivityRequest.getInjectionType())) {
            DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
            taskBuilder.id(injectActivityRequest.getId())
                .name(injectActivityRequest.getName())
                .assignee(injectActivityRequest.getAssignee());
            
            if (injectActivityRequest.getTaskId() != null) {
                dynamicBpmnService.injectParallelUserTask(injectActivityRequest.getTaskId(), taskBuilder);
            } else {
                dynamicBpmnService.injectUserTaskInProcessInstance(processInstanceId, taskBuilder);
            }
        
        } else if ("subprocess".equalsIgnoreCase(injectActivityRequest.getInjectionType())) {
            if (StringUtils.isEmpty(injectActivityRequest.getProcessDefinitionId())) {
                throw new FlowableIllegalArgumentException("processDefinitionId is required");
            }
            DynamicEmbeddedSubProcessBuilder subProcessBuilder = new DynamicEmbeddedSubProcessBuilder();
            subProcessBuilder.id(injectActivityRequest.getId())
                .processDefinitionId(injectActivityRequest.getProcessDefinitionId());
            
            if (injectActivityRequest.getTaskId() != null) {
                dynamicBpmnService.injectParallelEmbeddedSubProcess(injectActivityRequest.getTaskId(), subProcessBuilder);
            } else {
                dynamicBpmnService.injectEmbeddedSubProcessInProcessInstance(processInstanceId, subProcessBuilder);
            }
        
        } else {
            throw new FlowableIllegalArgumentException("injection type is not supported " + injectActivityRequest.getInjectionType());
        }
    }

    protected ProcessInstanceResponse activateProcessInstance(ProcessInstance processInstance) {
        if (!processInstance.isSuspended()) {
            throw new FlowableConflictException("Process instance with id '" + processInstance.getId() + "' is already active.");
        }
        runtimeService.activateProcessInstanceById(processInstance.getId());

        ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(processInstance);

        // No need to re-fetch the instance, just alter the suspended state of the result-object
        response.setSuspended(false);
        return response;
    }

    protected ProcessInstanceResponse suspendProcessInstance(ProcessInstance processInstance) {
        if (processInstance.isSuspended()) {
            throw new FlowableConflictException("Process instance with id '" + processInstance.getId() + "' is already suspended.");
        }
        runtimeService.suspendProcessInstanceById(processInstance.getId());

        ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(processInstance);

        // No need to re-fetch the instance, just alter the suspended state of the result-object
        response.setSuspended(true);
        return response;
    }
}
