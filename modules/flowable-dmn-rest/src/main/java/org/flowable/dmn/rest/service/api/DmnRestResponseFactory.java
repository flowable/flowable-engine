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
package org.flowable.dmn.rest.service.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.variable.BooleanRestVariableConverter;
import org.flowable.common.rest.variable.DateRestVariableConverter;
import org.flowable.common.rest.variable.DoubleRestVariableConverter;
import org.flowable.common.rest.variable.EngineRestVariable;
import org.flowable.common.rest.variable.InstantRestVariableConverter;
import org.flowable.common.rest.variable.IntegerRestVariableConverter;
import org.flowable.common.rest.variable.LocalDateRestVariableConverter;
import org.flowable.common.rest.variable.LocalDateTimeRestVariableConverter;
import org.flowable.common.rest.variable.LongRestVariableConverter;
import org.flowable.common.rest.variable.RestVariableConverter;
import org.flowable.common.rest.variable.ShortRestVariableConverter;
import org.flowable.common.rest.variable.StringRestVariableConverter;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.rest.service.api.decision.DmnRuleServiceResponse;
import org.flowable.dmn.rest.service.api.decision.DmnRuleServiceSingleResponse;
import org.flowable.dmn.rest.service.api.history.HistoricDecisionExecutionResponse;
import org.flowable.dmn.rest.service.api.repository.DecisionResponse;
import org.flowable.dmn.rest.service.api.repository.DmnDeploymentResponse;

/**
 *
 * Default implementation of a {@link DmnRestResponseFactory}.
 *
 * @author Yvo Swillens
 */
public class DmnRestResponseFactory {

    public static final String BYTE_ARRAY_VARIABLE_TYPE = "binary";
    public static final String SERIALIZABLE_VARIABLE_TYPE = "serializable";

    protected List<RestVariableConverter> variableConverters = new ArrayList<>();

    public DmnRestResponseFactory() {
        initializeVariableConverters();
    }

    public DecisionResponse createDecisionTableResponse(DmnDecision decision) {
        return createDecisionTableResponse(decision, createUrlBuilder());
    }

    public DecisionResponse createDecisionTableResponse(DmnDecision decision, DmnRestUrlBuilder urlBuilder) {
        DecisionResponse response = new DecisionResponse(decision);
        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_DECISION_TABLE, decision.getId()));

        return response;
    }

    public List<DecisionResponse> createDecisionTableResponseList(List<DmnDecision> decisions) {
        DmnRestUrlBuilder urlBuilder = createUrlBuilder();
        List<DecisionResponse> responseList = new ArrayList<>(decisions.size());
        for (DmnDecision instance : decisions) {
            responseList.add(createDecisionResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public DecisionResponse createDecisionResponse(DmnDecision decision) {
        return createDecisionResponse(decision, createUrlBuilder());
    }

    public DecisionResponse createDecisionResponse(DmnDecision decision, DmnRestUrlBuilder urlBuilder) {
        DecisionResponse response = new DecisionResponse(decision);
        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_DECISION, decision.getId()));

        return response;
    }

    public List<DecisionResponse> createDecisionResponseList(List<DmnDecision> decisions) {
        DmnRestUrlBuilder urlBuilder = createUrlBuilder();
        List<DecisionResponse> responseList = new ArrayList<>(decisions.size());
        for (DmnDecision instance : decisions) {
            responseList.add(createDecisionResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public List<DmnDeploymentResponse> createDmnDeploymentResponseList(List<DmnDeployment> deployments) {
        DmnRestUrlBuilder urlBuilder = createUrlBuilder();
        List<DmnDeploymentResponse> responseList = new ArrayList<>(deployments.size());
        for (DmnDeployment instance : deployments) {
            responseList.add(createDmnDeploymentResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public DmnDeploymentResponse createDmnDeploymentResponse(DmnDeployment deployment) {
        return createDmnDeploymentResponse(deployment, createUrlBuilder());
    }

    public DmnDeploymentResponse createDmnDeploymentResponse(DmnDeployment deployment, DmnRestUrlBuilder urlBuilder) {
        return new DmnDeploymentResponse(deployment, urlBuilder.buildUrl(DmnRestUrls.URL_DEPLOYMENT, deployment.getId()));
    }

    public DmnRuleServiceResponse createDmnRuleServiceResponse(List<Map<String, Object>> executionResults) {
        return createDmnRuleServiceResponse(executionResults, createUrlBuilder());
    }

    public DmnRuleServiceSingleResponse createDmnRuleServiceResponse(Map<String, Object> executionResult) {
        return createDmnRuleServiceResponse(executionResult, createUrlBuilder());
    }

    public DmnRuleServiceResponse createDmnRuleServiceResponse(List<Map<String, Object>> executionResults, DmnRestUrlBuilder urlBuilder) {
        DmnRuleServiceResponse response = new DmnRuleServiceResponse();

        if (executionResults != null && !executionResults.isEmpty()) {
            for (Map<String, Object> executionResult : executionResults) {
                List<EngineRestVariable> ruleResults = new ArrayList<>(executionResult.size());
                for (String name : executionResult.keySet()) {
                    ruleResults.add(createRestVariable(name, executionResult.get(name), false));
                }
                response.addResultVariables(ruleResults);
            }
        }

        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));

        return response;
    }

    public DmnRuleServiceSingleResponse createDmnRuleServiceResponse(Map<String, Object> executionResult, DmnRestUrlBuilder urlBuilder) {
        DmnRuleServiceSingleResponse response = new DmnRuleServiceSingleResponse();

        if (executionResult != null) {
            for (String name : executionResult.keySet()) {
                response.addResultVariable(createRestVariable(name, executionResult.get(name), false));
            }
        }

        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));

        return response;
    }

    public DmnRuleServiceResponse createDmnRuleServiceResponse(DecisionExecutionAuditContainer decisionExecutionAuditContainer) {
        return createDmnRuleServiceResponse(decisionExecutionAuditContainer, createUrlBuilder());
    }

    public DmnRuleServiceResponse createDmnRuleServiceResponse(DecisionServiceExecutionAuditContainer decisionServiceExecutionAuditContainer){
        return createDmnRuleServiceResponse(decisionServiceExecutionAuditContainer, createUrlBuilder());
    }

    public DmnRuleServiceResponse createDmnRuleServiceResponse(DecisionServiceExecutionAuditContainer decisionServiceExecutionAuditContainer, DmnRestUrlBuilder urlBuilder){
        DmnRuleServiceResponse response = new DmnRuleServiceResponse();

        Map<String, List<Map<String, Object>>> executionResults = decisionServiceExecutionAuditContainer.getDecisionServiceResult();

        if (executionResults != null && !executionResults.isEmpty()) {
            for (String decisionName : executionResults.keySet()) {
                for (Map<String, Object> executionResult : executionResults.get(decisionName)) {
                    List<EngineRestVariable> ruleResults = new ArrayList<>(executionResult.size());
                    for (String variableName : executionResult.keySet()) {
                        ruleResults.add(createRestVariable(variableName, executionResult.get(variableName), false));
                    }
                    response.addResultVariables(ruleResults);
                }
            }
        }

        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));

        return response;
    }

    public DmnRuleServiceResponse createDmnRuleServiceResponse(DecisionExecutionAuditContainer decisionExecutionAuditContainer, DmnRestUrlBuilder urlBuilder){
        DmnRuleServiceResponse response = new DmnRuleServiceResponse();

        List<Map<String, Object>> executionResults = decisionExecutionAuditContainer.getDecisionResult();
        if (executionResults != null && !executionResults.isEmpty()) {
            for (Map<String, Object> executionResult : executionResults) {
                List<EngineRestVariable> ruleResults = new ArrayList<>(executionResult.size());
                for (String name : executionResult.keySet()) {
                    ruleResults.add(createRestVariable(name, executionResult.get(name), false));
                }
                response.addResultVariables(ruleResults);
            }
        }

        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));
        return response;
    }
    
    public List<HistoricDecisionExecutionResponse> createHistoricDecisionExecutionResponseList(List<DmnHistoricDecisionExecution> historicDecisionExecutions) {
        DmnRestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricDecisionExecutionResponse> responseList = new ArrayList<>(historicDecisionExecutions.size());
        for (DmnHistoricDecisionExecution execution : historicDecisionExecutions) {
            responseList.add(createHistoryDecisionExecutionResponse(execution, urlBuilder));
        }
        return responseList;
    }
    
    public HistoricDecisionExecutionResponse createHistoryDecisionExecutionResponse(DmnHistoricDecisionExecution execution) {
        return createHistoryDecisionExecutionResponse(execution, createUrlBuilder());
    }
    
    public HistoricDecisionExecutionResponse createHistoryDecisionExecutionResponse(DmnHistoricDecisionExecution execution, DmnRestUrlBuilder urlBuilder) {
        HistoricDecisionExecutionResponse response = new HistoricDecisionExecutionResponse(execution);
        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_HISTORIC_DECISION_EXECUTION, execution.getId()));

        return response;
    }

    public Object getVariableValue(EngineRestVariable restVariable) {
        Object value = null;

        if (restVariable.getType() != null) {
            // Try locating a converter if the type has been specified
            RestVariableConverter converter = null;
            for (RestVariableConverter conv : variableConverters) {
                if (conv.getRestTypeName().equals(restVariable.getType())) {
                    converter = conv;
                    break;
                }
            }
            if (converter == null) {
                throw new FlowableIllegalArgumentException("Variable '" + restVariable.getName() + "' has unsupported type: '" + restVariable.getType() + "'.");
            }
            value = converter.getVariableValue(restVariable);

        } else {
            // Revert to type determined by REST-to-Java mapping when no
            // explicit type has been provided
            value = restVariable.getValue();
        }
        return value;
    }

    public EngineRestVariable createRestVariable(String name, Object value, boolean includeBinaryValue) {
        return createRestVariable(name, value, includeBinaryValue, createUrlBuilder());
    }

    public EngineRestVariable createRestVariable(String name, Object value, boolean includeBinaryValue, DmnRestUrlBuilder urlBuilder) {

        RestVariableConverter converter = null;
        EngineRestVariable restVar = new EngineRestVariable();
        restVar.setName(name);

        if (value != null) {
            // Try converting the value
            for (RestVariableConverter c : variableConverters) {
                if (c.getVariableType().isAssignableFrom(value.getClass())) {
                    converter = c;
                    break;
                }
            }

            if (converter != null) {
                converter.convertVariableValue(value, restVar);
                restVar.setType(converter.getRestTypeName());
            } else {
                // Revert to default conversion, which is the
                // serializable/byte-array form
                if (value instanceof Byte[] || value instanceof byte[]) {
                    restVar.setType(BYTE_ARRAY_VARIABLE_TYPE);
                } else {
                    restVar.setType(SERIALIZABLE_VARIABLE_TYPE);
                }

                if (includeBinaryValue) {
                    restVar.setValue(value);
                }
            }
            // TODO: set url

        }
        return restVar;
    }

    /**
     * Called once when the converters need to be initialized. Override of custom conversion needs to be done between Java and REST.
     */
    protected void initializeVariableConverters() {
        variableConverters.add(new StringRestVariableConverter());
        variableConverters.add(new IntegerRestVariableConverter());
        variableConverters.add(new LongRestVariableConverter());
        variableConverters.add(new ShortRestVariableConverter());
        variableConverters.add(new DoubleRestVariableConverter());
        variableConverters.add(new BooleanRestVariableConverter());
        variableConverters.add(new DateRestVariableConverter());
        variableConverters.add(new InstantRestVariableConverter());
        variableConverters.add(new LocalDateRestVariableConverter());
        variableConverters.add(new LocalDateTimeRestVariableConverter());
    }

    protected DmnRestUrlBuilder createUrlBuilder() {
        return DmnRestUrlBuilder.fromCurrentRequest();
    }
}
