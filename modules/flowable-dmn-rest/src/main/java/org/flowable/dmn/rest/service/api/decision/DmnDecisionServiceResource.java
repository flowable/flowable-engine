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
package org.flowable.dmn.rest.service.api.decision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.variable.EngineRestVariable;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.dmn.rest.service.api.DmnRestApiInterceptor;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "DMN Decision Service" }, description = "Execute DMN Decision Service or Decision Table", authorizations = {
        @Authorization(value = "basicAuth") })
public class DmnDecisionServiceResource {

    @Autowired
    protected DmnRestResponseFactory dmnRestResponseFactory;

    @Autowired
    protected DmnDecisionService dmnDecisionService;

    @Autowired(required = false)
    protected DmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "Execute a single decision or a decision service depending on the provided decision key", tags = { "DMN Decision Service" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision or decision service has been executed")
    })
    @PostMapping(value = "/dmn-decision/execute", produces = "application/json")
    public DmnDecisionServiceResponse execute(@ApiParam("request") @RequestBody DmnRuleServiceRequest request, HttpServletRequest httpRequest,
            HttpServletResponse response) {
        if (request.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("Decision key is required.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.executeDecision(request);
        }

        Map<String, Object> inputVariables = composeInputVariables(request.getInputVariables());

        try {
            ExecuteDecisionBuilder decisionBuilder = dmnDecisionService.createExecuteDecisionBuilder();
            decisionBuilder.decisionKey(request.getDecisionKey()).variables(inputVariables);

            if (StringUtils.isNotEmpty(request.getParentDeploymentId())) {
                decisionBuilder.parentDeploymentId(request.getParentDeploymentId());
            }

            if (StringUtils.isNotEmpty(request.getTenantId())) {
                decisionBuilder.tenantId(request.getTenantId());
            }

            DecisionExecutionAuditContainer executionResult = decisionBuilder.executeWithAuditTrail();

            response.setStatus(HttpStatus.CREATED.value());

            return dmnRestResponseFactory.createDmnDecisionServiceResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a single decision or a decision service depending on the provided decision key", tags = { "DMN Decision Service" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision or decision service has been executed"),
            @ApiResponse(code = 500, message = "Indicates the decision or decision service returned multiple results")
    })
    @PostMapping(value = "/dmn-decision/execute/single-result", produces = "application/json")
    public DmnDecisionServiceSingleResponse executeWithSingleResult(@ApiParam("request") @RequestBody DmnRuleServiceRequest request,
            HttpServletRequest httpRequest, HttpServletResponse response) {
        if (request.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("Decision key is required.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.executeDecision(request);
        }

        Map<String, Object> inputVariables = composeInputVariables(request.getInputVariables());

        try {
            ExecuteDecisionBuilder decisionBuilder = dmnDecisionService.createExecuteDecisionBuilder();
            decisionBuilder.decisionKey(request.getDecisionKey()).variables(inputVariables);

            if (StringUtils.isNotEmpty(request.getParentDeploymentId())) {
                decisionBuilder.parentDeploymentId(request.getParentDeploymentId());
            }

            if (StringUtils.isNotEmpty(request.getTenantId())) {
                decisionBuilder.tenantId(request.getTenantId());
            }

            Map<String, Object> executionResult = decisionBuilder.executeWithSingleResult();

            response.setStatus(HttpStatus.CREATED.value());

            return dmnRestResponseFactory.createDmnDecisionServiceSingleResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a single decision", tags = { "DMN Decision Service" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision has been executed")
    })
    @PostMapping(value = "/dmn-decision/execute-decision", produces = "application/json")
    public DmnDecisionServiceResponse executeDecision(@ApiParam("request") @RequestBody DmnRuleServiceRequest request, HttpServletRequest httpRequest,
            HttpServletResponse response) {
        if (request.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("Decision key is required.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.executeDecision(request);
        }

        Map<String, Object> inputVariables = composeInputVariables(request.getInputVariables());

        try {
            ExecuteDecisionBuilder decisionBuilder = dmnDecisionService.createExecuteDecisionBuilder();
            decisionBuilder.decisionKey(request.getDecisionKey()).variables(inputVariables);

            if (StringUtils.isNotEmpty(request.getParentDeploymentId())) {
                decisionBuilder.parentDeploymentId(request.getParentDeploymentId());
            }

            if (StringUtils.isNotEmpty(request.getTenantId())) {
                decisionBuilder.tenantId(request.getTenantId());
            }

            List<Map<String, Object>> executionResult = decisionBuilder.executeDecision();

            response.setStatus(HttpStatus.CREATED.value());

            return dmnRestResponseFactory.createDmnDecisionServiceResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a decision service", tags = { "DMN Decision Service" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision service has been executed")
    })
    @PostMapping(value = "/dmn-decision/execute-decision-service", produces = "application/json")
    public DmnDecisionServiceResponse executeDecisionService(@ApiParam("request") @RequestBody DmnRuleServiceRequest request, HttpServletRequest httpRequest,
            HttpServletResponse response) {
        if (request.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("Decision key is required.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.executeDecision(request);
        }

        Map<String, Object> inputVariables = composeInputVariables(request.getInputVariables());

        try {
            ExecuteDecisionBuilder decisionBuilder = dmnDecisionService.createExecuteDecisionBuilder();
            decisionBuilder.decisionKey(request.getDecisionKey()).variables(inputVariables);

            if (StringUtils.isNotEmpty(request.getParentDeploymentId())) {
                decisionBuilder.parentDeploymentId(request.getParentDeploymentId());
            }

            if (StringUtils.isNotEmpty(request.getTenantId())) {
                decisionBuilder.tenantId(request.getTenantId());
            }

            Map<String, List<Map<String, Object>>> executionResult = decisionBuilder.executeDecisionService();

            response.setStatus(HttpStatus.CREATED.value());

            return dmnRestResponseFactory.createDmnDecisionServiceResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a single decision", tags = { "DMN Decision Service" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision or decision service has been executed"),
            @ApiResponse(code = 500, message = "Indicates the decision returned multiple results")
    })
    @PostMapping(value = "/dmn-decision/execute-decision/single-result", produces = "application/json")
    public DmnDecisionServiceSingleResponse executeDecisionWithSingleResult(@ApiParam("request") @RequestBody DmnRuleServiceRequest request,
            HttpServletRequest httpRequest, HttpServletResponse response) {
        if (request.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("Decision key is required.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.executeDecision(request);
        }

        Map<String, Object> inputVariables = composeInputVariables(request.getInputVariables());

        try {
            ExecuteDecisionBuilder decisionBuilder = dmnDecisionService.createExecuteDecisionBuilder();
            decisionBuilder.decisionKey(request.getDecisionKey()).variables(inputVariables);

            if (StringUtils.isNotEmpty(request.getParentDeploymentId())) {
                decisionBuilder.parentDeploymentId(request.getParentDeploymentId());
            }

            if (StringUtils.isNotEmpty(request.getTenantId())) {
                decisionBuilder.tenantId(request.getTenantId());
            }

            Map<String, Object> executionResult = decisionBuilder.executeDecisionWithSingleResult();

            response.setStatus(HttpStatus.CREATED.value());

            return dmnRestResponseFactory.createDmnDecisionServiceSingleResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a decision service", tags = { "DMN Decision Service" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision service has been executed"),
            @ApiResponse(code = 500, message = "Indicates the decision service returned multiple results")
    })
    @PostMapping(value = "/dmn-decision/execute-decision-service/single-result", produces = "application/json")
    public DmnDecisionServiceSingleResponse executeDecisionServiceWithSingleResult(@ApiParam("request") @RequestBody DmnRuleServiceRequest request,
            HttpServletRequest httpRequest, HttpServletResponse response) {
        if (request.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("Decision key is required.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.executeDecision(request);
        }

        Map<String, Object> inputVariables = composeInputVariables(request.getInputVariables());

        try {
            ExecuteDecisionBuilder decisionBuilder = dmnDecisionService.createExecuteDecisionBuilder();
            decisionBuilder.decisionKey(request.getDecisionKey()).variables(inputVariables);

            if (StringUtils.isNotEmpty(request.getParentDeploymentId())) {
                decisionBuilder.parentDeploymentId(request.getParentDeploymentId());
            }

            if (StringUtils.isNotEmpty(request.getTenantId())) {
                decisionBuilder.tenantId(request.getTenantId());
            }

            Map<String, Object> executionResult = decisionBuilder.executeDecisionServiceWithSingleResult();

            response.setStatus(HttpStatus.CREATED.value());

            return dmnRestResponseFactory.createDmnDecisionServiceSingleResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    protected Map<String, Object> composeInputVariables(List<EngineRestVariable> restVariables) {
        Map<String, Object> inputVariables = null;
        if (restVariables != null) {
            inputVariables = new HashMap<>();
            for (EngineRestVariable variable : restVariables) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                inputVariables.put(variable.getName(), dmnRestResponseFactory.getVariableValue(variable));
            }
        }
        return inputVariables;
    }
}