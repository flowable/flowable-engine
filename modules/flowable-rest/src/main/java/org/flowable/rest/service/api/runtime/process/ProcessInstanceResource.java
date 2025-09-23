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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.impl.dynamic.DynamicEmbeddedSubProcessBuilder;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationDocumentConverter;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Process Instances" }, authorizations = { @Authorization(value = "basicAuth") })
public class ProcessInstanceResource extends BaseProcessInstanceResource {
    
    @Autowired
    protected DynamicBpmnService dynamicBpmnService;

    @Autowired
    protected ProcessMigrationService migrationService;

    @ApiOperation(value = "Get a process instance", tags = { "Process Instances" }, nickname = "getProcessInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @GetMapping(value = "/runtime/process-instances/{processInstanceId}", produces = "application/json")
    public ProcessInstanceResponse getProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId) {
        ProcessInstance processInstance = getProcessInstanceFromRequest(processInstanceId);
        ProcessInstanceResponse processInstanceResponse = restResponseFactory.createProcessInstanceResponse(processInstance);
        
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstanceResponse.getProcessDefinitionId()).singleResult();
        
        if (processDefinition != null) {
            processInstanceResponse.setProcessDefinitionName(processDefinition.getName());
            processInstanceResponse.setProcessDefinitionDescription(processDefinition.getDescription());
        }
        
        if (StringUtils.isNotEmpty(processInstance.getSuperExecutionId())) {
            Execution parentProcessExecution = runtimeService.createExecutionQuery().executionId(processInstance.getSuperExecutionId()).singleResult();
            if (parentProcessExecution != null) {
                processInstanceResponse.setSuperProcessInstanceId(parentProcessExecution.getProcessInstanceId());
            }
        }
        
        return processInstanceResponse;
    }

    @ApiOperation(value = "Delete a process instance", tags = { "Process Instances" }, nickname = "deleteProcessInstance", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the process instance was found and deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @DeleteMapping(value = "/runtime/process-instances/{processInstanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, @RequestParam(value = "deleteReason", required = false) String deleteReason) {
        ProcessInstance processInstance = getProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteProcessInstance(processInstance);
        }

        runtimeService.deleteProcessInstance(processInstance.getId(), deleteReason);
    }

    @ApiOperation(value = "Update process instance properties or execute an action on a process instance (body needs to contain an 'action' property for the latter).", tags = { "Process Instances" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and the update/action was executed."),
            @ApiResponse(code = 400, message = "Indicates a invalid parameters are supplied."),
            @ApiResponse(code = 409, message = "Indicates the requested process instance change cannot be executed since the process-instance is in a wrong status which doesn't accept the change"),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @PutMapping(value = "/runtime/process-instances/{processInstanceId}", produces = "application/json")
    public ProcessInstanceResponse updateProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId,
        @RequestBody ProcessInstanceUpdateRequest updateRequest, HttpServletResponse response) {

        ProcessInstance processInstance = getProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);

        if (restApiInterceptor != null) {
            restApiInterceptor.updateProcessInstance(processInstance, updateRequest);
        }

        if (StringUtils.isNotEmpty(updateRequest.getAction())) {

            if (ProcessInstanceUpdateRequest.ACTION_ACTIVATE.equals(updateRequest.getAction())) {
                return activateProcessInstance(processInstance);

            } else if (ProcessInstanceUpdateRequest.ACTION_SUSPEND.equals(updateRequest.getAction())) {
                return suspendProcessInstance(processInstance);
            }
            throw new FlowableIllegalArgumentException("Invalid action: '" + updateRequest.getAction() + "'.");

        } else { // update

            if (StringUtils.isNotEmpty(updateRequest.getName())) {
                runtimeService.setProcessInstanceName(processInstanceId, updateRequest.getName());
            }
            if (StringUtils.isNotEmpty(updateRequest.getBusinessKey())) {
                runtimeService.updateBusinessKey(processInstanceId, updateRequest.getBusinessKey());
            }

            processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (processInstance == null) {
                response.setStatus(HttpStatus.NO_CONTENT.value());
                return null;
            } else {
                return restResponseFactory.createProcessInstanceResponse(processInstance);
            }

        }

    }

    @ApiOperation(value = "Change the state a process instance", tags = { "Process Instances" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and change state activity was executed."),
            @ApiResponse(code = 409, message = "Indicates the requested process instance action cannot be executed since the process-instance is already activated/suspended."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @PostMapping(value = "/runtime/process-instances/{processInstanceId}/change-state", produces = "application/json")
    public void changeActivityState(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId,
            @RequestBody ExecutionChangeActivityStateRequest activityStateRequest) {
        
        ProcessInstance processInstance = getProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.changeActivityState(processInstance, activityStateRequest);
        }

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
    
    @ApiOperation(value = "Evaluate the conditions of a process instance", tags = { "Process Instances" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and the evaluation of the conditions was executed."),
            @ApiResponse(code = 409, message = "Indicates the requested process instance action cannot be executed since the process-instance is already activated/suspended."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @PostMapping(value = "/runtime/process-instances/{processInstanceId}/evaluate-conditions", produces = "application/json")
    public void evaluateConditions(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId) {
        ProcessInstance processInstance = getProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);
        if (restApiInterceptor != null) {
            restApiInterceptor.evaluateProcessInstanceConditionalEvents(processInstance);
        }
        runtimeService.evaluateConditionalEvents(processInstance.getId());
    }
    
    @ApiOperation(value = "Migrate process instance", tags = { "Process Instances" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the process instance was found and migration was executed."),
            @ApiResponse(code = 409, message = "Indicates the requested process instance action cannot be executed since the process-instance is already activated/suspended."),
            @ApiResponse(code = 404, message = "Indicates the requested process instance was not found.")
    })
    @PostMapping(value = "/runtime/process-instances/{processInstanceId}/migrate", produces = "application/json")
    public void migrateProcessInstance(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId,
            @RequestBody String migrationDocumentJson) {
        
        ProcessInstance processInstance = getProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);
        if (restApiInterceptor != null) {
            restApiInterceptor.migrateProcessInstance(processInstance, migrationDocumentJson);
        }

        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentConverter.convertFromJson(migrationDocumentJson);
        migrationService.migrateProcessInstance(processInstanceId, migrationDocument);
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
            @RequestBody InjectActivityRequest injectActivityRequest) {
        
        ProcessInstance processInstance = getProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.injectActivity(processInstance, injectActivityRequest);
        }

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
