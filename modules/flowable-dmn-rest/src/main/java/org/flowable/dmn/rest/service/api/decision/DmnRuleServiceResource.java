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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.variable.EngineRestVariable;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.dmn.rest.service.api.DmnRestApiInterceptor;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@Api(tags = { "DMN Rule Service" }, authorizations = { @Authorization(value = "basicAuth") })
public class DmnRuleServiceResource {

    @Autowired
    protected DmnRestResponseFactory dmnRestResponseFactory;

    @Autowired
    protected DmnDecisionService dmnDecisionService;

    @Autowired(required = false)
    protected DmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "Execute a Decision", tags = { "DMN Rule Service" }, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the Decision has been executed")
    })
    @PostMapping(value = "/dmn-rule/execute", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public DmnRuleServiceResponse execute(@ApiParam("request") @RequestBody DmnRuleServiceRequest request) {
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

            if (request.isDisableHistory()) {
                decisionBuilder.disableHistory();
            }

            DecisionExecutionAuditContainer executionResult = decisionBuilder.executeWithAuditTrail();

            if (executionResult instanceof DecisionServiceExecutionAuditContainer) {
                return dmnRestResponseFactory.createDmnRuleServiceResponse((DecisionServiceExecutionAuditContainer) executionResult);
            } else {
                return dmnRestResponseFactory.createDmnRuleServiceResponse(executionResult);
            }
        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a decision or a decision service expecting a single result", tags = { "DMN Rule Service" }, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision or decision service has been executed"),
            @ApiResponse(code = 500, message = "Indicates the decision or decision service returned multiple results")
    })
    @PostMapping(value = "/dmn-rule/execute/single-result", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public DmnRuleServiceSingleResponse executeWithSingleResult(@ApiParam("request") @RequestBody DmnRuleServiceRequest request) {
        if (request.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("Decision key is required.");
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

            if (request.isDisableHistory()) {
                decisionBuilder.disableHistory();
            }

            Map<String, Object> executionResult = decisionBuilder.executeWithSingleResult();

            return dmnRestResponseFactory.createDmnRuleServiceResponse(executionResult);
        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a decision", tags = { "DMN Decision Service" }, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision has been executed")
    })
    @PostMapping(value = "/dmn-rule/execute-decision", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public DmnRuleServiceResponse executeDecision(@ApiParam("request") @RequestBody DmnRuleServiceRequest request) {
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

            if (request.isDisableHistory()) {
                decisionBuilder.disableHistory();
            }

            DecisionExecutionAuditContainer executionResult = decisionBuilder.executeDecisionWithAuditTrail();

            return dmnRestResponseFactory.createDmnRuleServiceResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a decision service", tags = { "DMN Decision Service" }, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision service has been executed")
    })
    @PostMapping(value = "/dmn-rule/execute-decision-service", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public DmnRuleServiceResponse executeDecisionService(@ApiParam("request") @RequestBody DmnRuleServiceRequest request) {
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

            if (request.isDisableHistory()) {
                decisionBuilder.disableHistory();
            }

            DecisionServiceExecutionAuditContainer executionResult = decisionBuilder.executeDecisionServiceWithAuditTrail();

            return dmnRestResponseFactory.createDmnRuleServiceResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a decision expecting a single result", tags = { "DMN Decision Service" }, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision or decision service has been executed"),
            @ApiResponse(code = 500, message = "Indicates the decision returned multiple results")
    })
    @PostMapping(value = "/dmn-rule/execute-decision/single-result", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public DmnRuleServiceSingleResponse executeDecisionWithSingleResult(@ApiParam("request") @RequestBody DmnRuleServiceRequest request) {
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

            if (request.isDisableHistory()) {
                decisionBuilder.disableHistory();
            }

            Map<String, Object> executionResult = decisionBuilder.executeDecisionWithSingleResult();

            return dmnRestResponseFactory.createDmnRuleServiceResponse(executionResult);

        } catch (FlowableObjectNotFoundException fonfe) {
            throw new FlowableIllegalArgumentException(fonfe.getMessage(), fonfe);
        }
    }

    @ApiOperation(value = "Execute a decision service expecting a single result", tags = { "DMN Decision Service" }, code = 201)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the decision service has been executed"),
            @ApiResponse(code = 500, message = "Indicates the decision service returned multiple results")
    })
    @PostMapping(value = "/dmn-rule/execute-decision-service/single-result", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public DmnRuleServiceSingleResponse executeDecisionServiceWithSingleResult(@ApiParam("request") @RequestBody DmnRuleServiceRequest request) {
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

            if (request.isDisableHistory()) {
                decisionBuilder.disableHistory();
            }

            Map<String, Object> executionResult = decisionBuilder.executeDecisionServiceWithSingleResult();

            return dmnRestResponseFactory.createDmnRuleServiceResponse(executionResult);

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