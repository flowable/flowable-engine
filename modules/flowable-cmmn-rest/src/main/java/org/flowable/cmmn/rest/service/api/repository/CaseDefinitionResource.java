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

package org.flowable.cmmn.rest.service.api.repository;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.rest.service.api.CmmnFormHandlerRestApiInterceptor;
import org.flowable.cmmn.rest.service.api.FormModelResponse;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.model.SimpleFormModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Case Definitions" }, description = "Manage Case Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class CaseDefinitionResource extends BaseCaseDefinitionResource {
    
    @Autowired
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    @Autowired(required=false)
    protected FormRepositoryService formRepositoryService;
    
    @Autowired(required=false)
    protected CmmnFormHandlerRestApiInterceptor formHandlerRestApiInterceptor;

    @ApiOperation(value = "Get a case definition", tags = { "Case Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the case definitions are returned"),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found.")
    })
    @GetMapping(value = "/cmmn-repository/case-definitions/{caseDefinitionId}", produces = "application/json")
    public CaseDefinitionResponse getCaseDefinition(@ApiParam(name = "caseDefinitionId") @PathVariable String caseDefinitionId, HttpServletRequest request) {
        CaseDefinition caseDefinition = getCaseDefinitionFromRequest(caseDefinitionId);

        return restResponseFactory.createCaseDefinitionResponse(caseDefinition);
    }
    
    @ApiOperation(value = "Execute actions for a case definition", tags = { "Case Definitions" },
            notes = "Execute actions for a case definition (Update category)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates action has been executed for the specified process. (category altered)"),
            @ApiResponse(code = 400, message = "Indicates no category was defined in the request body."),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found.")
    })
    @PutMapping(value = "/cmmn-repository/case-definitions/{caseDefinitionId}", produces = "application/json")
    public CaseDefinitionResponse executeCaseDefinitionAction(
            @ApiParam(name = "caseDefinitionId") @PathVariable String caseDefinitionId,
            @ApiParam(required = true) @RequestBody CaseDefinitionActionRequest actionRequest,
            HttpServletRequest request) {

        if (actionRequest == null) {
            throw new FlowableIllegalArgumentException("No action found in request body.");
        }

        CaseDefinition caseDefinition = getCaseDefinitionFromRequest(caseDefinitionId);

        if (actionRequest.getCategory() != null) {
            // Update of category required
            repositoryService.setCaseDefinitionCategory(caseDefinition.getId(), actionRequest.getCategory());

            // No need to re-fetch the CaseDefinition entity, just update category in response
            CaseDefinitionResponse response = restResponseFactory.createCaseDefinitionResponse(caseDefinition);
            response.setCategory(actionRequest.getCategory());
            return response;
        }
        
        throw new FlowableIllegalArgumentException("Invalid action: '" + actionRequest.getAction() + "'.");
    }

    @ApiOperation(value = "Get a case definition start form", tags = { "Case Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the case definition form is returned"),
            @ApiResponse(code = 404, message = "Indicates the requested case definition was not found.")
    })
    @GetMapping(value = "/cmmn-repository/case-definitions/{caseDefinitionId}/start-form", produces = "application/json")
    public String getProcessDefinitionStartForm(@ApiParam(name = "caseDefinitionId") @PathVariable String caseDefinitionId, HttpServletRequest request) {
        if (formRepositoryService == null) {
            return null;
        }
        
        CaseDefinition caseDefinition = getCaseDefinitionFromRequest(caseDefinitionId);
        FormInfo formInfo = getStartForm(caseDefinition);
        if (formHandlerRestApiInterceptor != null) {
            return formHandlerRestApiInterceptor.convertStartFormInfo(formInfo, caseDefinition);
        } else {
            SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
            return restResponseFactory.getFormModelString(new FormModelResponse(formInfo, formModel));
        }
    }
    
    protected FormInfo getStartForm(CaseDefinition caseDefinition) {
        FormInfo formInfo = null;
        CmmnModel cmmnModel = repositoryService.getCmmnModel(caseDefinition.getId());
        Case caze = cmmnModel.getCaseById(caseDefinition.getKey());
        Stage stage = caze.getPlanModel();
        if (StringUtils.isNotEmpty(stage.getFormKey())) {
            CmmnDeployment deployment = repositoryService.createDeploymentQuery().deploymentId(caseDefinition.getDeploymentId()).singleResult();
            formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(stage.getFormKey(),
                            deployment.getParentDeploymentId(), caseDefinition.getTenantId(), cmmnEngineConfiguration.isFallbackToDefaultTenant());
        }

        if (formInfo == null) {
            // Definition found, but no form attached
            throw new FlowableObjectNotFoundException("Case definition does not have a start form defined: " + caseDefinition.getId());
        }
        
        return formInfo;
    }
}
