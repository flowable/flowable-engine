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

package org.flowable.cmmn.rest.service.api.runtime.caze;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.rest.service.api.RestActionRequest;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Case Instances" }, description = "Manage Case Instances", authorizations = { @Authorization(value = "basicAuth") })
public class CaseInstanceResource extends BaseCaseInstanceResource {

    @ApiOperation(value = "Get a case instance", tags = { "Case Instances" }, nickname = "getCaseInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the case instance was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found.")
    })
    @GetMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}", produces = "application/json")
    public CaseInstanceResponse getCaseInstance(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, HttpServletRequest request) {
        return restResponseFactory.createCaseInstanceResponse(getCaseInstanceFromRequest(caseInstanceId));
    }
    
    @ApiOperation(value = "Execute an action on a case instance", tags = { "Plan Item Instances" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the case instance was found and the action is performed."),
            @ApiResponse(code = 204, message = "Indicates the case was found, the action was performed and the action caused the case instance to end."),
            @ApiResponse(code = 400, message = "Indicates an illegal action was requested, required parameters are missing in the request body or illegal variables are passed in. Status description contains additional information about the error."),
            @ApiResponse(code = 404, message = "Indicates the case instance was not found.")
    })
    @PutMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}", produces = "application/json")
    public CaseInstanceResponse performCaseInstanceAction(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, 
                    @RequestBody RestActionRequest actionRequest, HttpServletRequest request, HttpServletResponse response) {

        CaseInstance caseInstance = getCaseInstanceFromRequest(caseInstanceId);

        if (RestActionRequest.EVALUATE_CRITERIA.equals(actionRequest.getAction())) {
            runtimeService.evaluateCriteria(caseInstance.getId());
            
        } else {
            throw new FlowableIllegalArgumentException("Invalid action: '" + actionRequest.getAction() + "'.");
        }

        // Re-fetch the execution, could have changed due to action or even completed
        caseInstance = runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        if (caseInstance == null) {
            // Execution is finished, return empty body to inform user
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return null;
        } else {
            return restResponseFactory.createCaseInstanceResponse(caseInstance);
        }
    }

    @ApiOperation(value = "Delete a case instance", tags = { "Case Instances" }, nickname = "deleteCaseInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the case instance was found and deleted. Response body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found.")
    })
    @DeleteMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}")
    public void deleteProcessInstance(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, @RequestParam(value = "deleteReason", required = false) String deleteReason, HttpServletResponse response) {

        CaseInstance caseInstance = getCaseInstanceFromRequest(caseInstanceId);

        runtimeService.terminateCaseInstance(caseInstance.getId());
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
