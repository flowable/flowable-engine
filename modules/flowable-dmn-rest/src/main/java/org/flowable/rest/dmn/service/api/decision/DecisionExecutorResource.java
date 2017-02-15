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
package org.flowable.rest.dmn.service.api.decision;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.rest.variable.EngineRestVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Decision Executor" }, description = "Execute Decision Table", authorizations = { @Authorization(value = "basicAuth") })
public class DecisionExecutorResource extends BaseDecisionExecutorResource {

    @ApiOperation(value = "Execute a Decision", tags = { "Decision Executor" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the Decision has been executed")
    })
    @RequestMapping(value = "/dmn-rule/decision-executor", method = RequestMethod.POST, produces = "application/json")
    public ExecuteDecisionResponse executeDecision(@ApiParam("request") @RequestBody ExecuteDecisionRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {

        if (request.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("Decision key is required.");
        }

        Map<String, Object> inputVariables = null;
        if (request.getInputVariables() != null) {
            inputVariables = new HashMap<String, Object>();
            for (EngineRestVariable variable : request.getInputVariables()) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                inputVariables.put(variable.getName(), dmnRestResponseFactory.getVariableValue(variable));
            }
        }

        try {
            RuleEngineExecutionResult executionResult = executeDecisionByKeyAndTenantId(request.getDecisionKey(), request.getTenantId(), inputVariables);

            response.setStatus(HttpStatus.CREATED.value());

            return dmnRestResponseFactory.createExecuteDecisionResponse(executionResult);

            // TODO: add audit trail info
        } catch (FlowableObjectNotFoundException aonfe) {
            throw new FlowableIllegalArgumentException(aonfe.getMessage(), aonfe);
        }
    }
}
