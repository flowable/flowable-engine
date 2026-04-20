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

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationDocumentConverter;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.rest.service.api.FormHandlerRestApiInterceptor;
import org.flowable.rest.service.api.FormModelResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@Api(tags = { "Process Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class ProcessDefinitionResource extends BaseProcessDefinitionResource {
    
    @Autowired
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    
    @Autowired
    protected ProcessMigrationService processMigrationService;
    
    @Autowired(required=false)
    protected FormHandlerRestApiInterceptor formHandlerRestApiInterceptor;

    @ApiOperation(value = "Get a process definition", tags = { "Process Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the process-definitions are returned"),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}", produces = "application/json")
    public ProcessDefinitionResponse getProcessDefinition(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {
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
            @ApiParam(required = true) @RequestBody ProcessDefinitionActionRequest actionRequest) {

        if (actionRequest == null) {
            throw new FlowableIllegalArgumentException("No action found in request body.");
        }

        ProcessDefinition processDefinition = getProcessDefinitionFromRequestWithoutAccessCheck(processDefinitionId);

        if (restApiInterceptor != null) {
            restApiInterceptor.executeProcessDefinitionAction(processDefinition, actionRequest);
        }

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
    
    @ApiOperation(value = "Get a process definition start form", tags = { "Process Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the process definition form is returned"),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @GetMapping(value = "/repository/process-definitions/{processDefinitionId}/start-form", produces = "application/json")
    public String getProcessDefinitionStartForm(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {
        FormEngineConfigurationApi formEngineConfiguration = (FormEngineConfigurationApi) processEngineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
        if (formEngineConfiguration == null) {
            return null;
        }
        FormRepositoryService formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        if (formRepositoryService == null) {
            return null;
        }
        
        ProcessDefinition processDefinition = getProcessDefinitionFromRequest(processDefinitionId);
        FormInfo formInfo = getStartForm(formRepositoryService, processDefinition);
        if (formHandlerRestApiInterceptor != null) {
            return formHandlerRestApiInterceptor.convertStartFormInfo(formInfo, processDefinition);
        } else {
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            return restResponseFactory.getFormModelString(new FormModelResponse(formInfo, formModel));
        }
    }
    
    @ApiOperation(value = "Migrate all instances of process definition", tags = { "Process Definitions" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates process instances were found and migration was executed."),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @PostMapping(value = "/repository/process-definitions/{processDefinitionId}/migrate", produces = "application/json")
    public void migrateInstancesOfProcessDefinition(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId,
            @RequestBody String migrationDocumentJson) {
        
        ProcessDefinition processDefinition = getProcessDefinitionFromRequestWithoutAccessCheck(processDefinitionId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.migrateInstancesOfProcessDefinition(processDefinition, migrationDocumentJson);
        }

        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentConverter.convertFromJson(migrationDocumentJson);
        processMigrationService.migrateProcessInstancesOfProcessDefinition(processDefinitionId, migrationDocument);
    }
    
    @ApiOperation(value = "Batch migrate all instances of process definition", tags = { "Process Definitions" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates process instances were found and batch migration was started."),
            @ApiResponse(code = 404, message = "Indicates the requested process definition was not found.")
    })
    @PostMapping(value = "/repository/process-definitions/{processDefinitionId}/batch-migrate", produces = "application/json")
    public void batchMigrateInstancesOfProcessDefinition(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId,
            @RequestBody String migrationDocumentJson) {
        
        ProcessDefinition processDefinition = getProcessDefinitionFromRequestWithoutAccessCheck(processDefinitionId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.migrateInstancesOfProcessDefinition(processDefinition, migrationDocumentJson);
        }

        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentConverter.convertFromJson(migrationDocumentJson);
        processMigrationService.batchMigrateProcessInstancesOfProcessDefinition(processDefinitionId, migrationDocument);
    }
    
    protected FormInfo getStartForm(FormRepositoryService formRepositoryService, ProcessDefinition processDefinition) {
        FormInfo formInfo = null;
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process process = bpmnModel.getProcessById(processDefinition.getKey());
        FlowElement startElement = process.getInitialFlowElement();
        if (startElement instanceof StartEvent) {
            StartEvent startEvent = (StartEvent) startElement;
            if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
                if (startEvent.isSameDeployment()) {
                    Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(processDefinition.getDeploymentId()).singleResult();
                    formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(startEvent.getFormKey(),
                    		deployment.getParentDeploymentId(), processDefinition.getTenantId(), processEngineConfiguration.isFallbackToDefaultTenant());
                    
                } else {
                    formInfo = formRepositoryService.getFormModelByKey(startEvent.getFormKey(), processDefinition.getTenantId(),
                            processEngineConfiguration.isFallbackToDefaultTenant());
                }
            }
        }

        if (formInfo == null) {
            // Definition found, but no form attached
            throw new FlowableObjectNotFoundException("Process definition does not have a form defined: " + processDefinition.getId());
        }
        
        return formInfo;
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
