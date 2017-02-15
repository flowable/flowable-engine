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
package org.flowable.rest.dmn.service.api;

import java.util.ArrayList;
import java.util.List;

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.rest.dmn.service.api.decision.ExecuteDecisionResponse;
import org.flowable.rest.dmn.service.api.repository.DecisionTableResponse;
import org.flowable.rest.dmn.service.api.repository.DmnDeploymentResponse;
import org.flowable.rest.variable.BooleanRestVariableConverter;
import org.flowable.rest.variable.DateRestVariableConverter;
import org.flowable.rest.variable.DoubleRestVariableConverter;
import org.flowable.rest.variable.EngineRestVariable;
import org.flowable.rest.variable.IntegerRestVariableConverter;
import org.flowable.rest.variable.LongRestVariableConverter;
import org.flowable.rest.variable.RestVariableConverter;
import org.flowable.rest.variable.ShortRestVariableConverter;
import org.flowable.rest.variable.StringRestVariableConverter;

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

    public DecisionTableResponse createDecisionTableResponse(DmnDecisionTable decisionTable) {
        return createDecisionTableResponse(decisionTable, createUrlBuilder());
    }

    public DecisionTableResponse createDecisionTableResponse(DmnDecisionTable decisionTable, DmnRestUrlBuilder urlBuilder) {
        DecisionTableResponse response = new DecisionTableResponse(decisionTable);
        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_DECISION_TABLE, decisionTable.getId()));

        return response;
    }

    public List<DecisionTableResponse> createDecisionTableResponseList(List<DmnDecisionTable> decisionTables) {
        DmnRestUrlBuilder urlBuilder = createUrlBuilder();
        List<DecisionTableResponse> responseList = new ArrayList<>();
        for (DmnDecisionTable instance : decisionTables) {
            responseList.add(createDecisionTableResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public List<DmnDeploymentResponse> createDmnDeploymentResponseList(List<DmnDeployment> deployments) {
        DmnRestUrlBuilder urlBuilder = createUrlBuilder();
        List<DmnDeploymentResponse> responseList = new ArrayList<>();
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

    public ExecuteDecisionResponse createExecuteDecisionResponse(RuleEngineExecutionResult executionResult) {
        return createExecuteDecisionResponse(executionResult, createUrlBuilder());
    }

    public ExecuteDecisionResponse createExecuteDecisionResponse(RuleEngineExecutionResult executionResult, DmnRestUrlBuilder urlBuilder) {
        ExecuteDecisionResponse response = new ExecuteDecisionResponse();

        if (executionResult.getResultVariables() != null) {
            for (String name : executionResult.getResultVariables().keySet()) {
                response.addResultVariable(createRestVariable(name, executionResult.getResultVariables().get(name), false));
            }
        }

        response.setUrl(urlBuilder.buildUrl(DmnRestUrls.URL_DECISION_EXECUTOR));

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
    }

    protected DmnRestUrlBuilder createUrlBuilder() {
        return DmnRestUrlBuilder.fromCurrentRequest();
    }
}
