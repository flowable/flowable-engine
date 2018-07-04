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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.ExecutionQueryProperty;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.QueryVariable;
import org.flowable.rest.service.api.engine.variable.QueryVariable.QueryVariableOperation;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Frederik Heremans
 */
public class ExecutionBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("processDefinitionId", ExecutionQueryProperty.PROCESS_DEFINITION_ID);
        allowedSortProperties.put("processDefinitionKey", ExecutionQueryProperty.PROCESS_DEFINITION_KEY);
        allowedSortProperties.put("processInstanceId", ExecutionQueryProperty.PROCESS_INSTANCE_ID);
        allowedSortProperties.put("tenantId", ExecutionQueryProperty.TENANT_ID);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RuntimeService runtimeService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected DataResponse<ExecutionResponse> getQueryResponse(ExecutionQueryRequest queryRequest, Map<String, String> requestParams, String serverRootUrl) {

        ExecutionQuery query = runtimeService.createExecutionQuery();

        // Populate query based on request
        if (queryRequest.getId() != null) {
            query.executionId(queryRequest.getId());
        }
        if (queryRequest.getProcessInstanceId() != null) {
            query.processInstanceId(queryRequest.getProcessInstanceId());
        }
        if (queryRequest.getProcessDefinitionKey() != null) {
            query.processDefinitionKey(queryRequest.getProcessDefinitionKey());
        }
        if (queryRequest.getProcessDefinitionId() != null) {
            query.processDefinitionId(queryRequest.getProcessDefinitionId());
        }
        if (queryRequest.getProcessBusinessKey() != null) {
            query.processInstanceBusinessKey(queryRequest.getProcessBusinessKey());
        }
        if (queryRequest.getActivityId() != null) {
            query.activityId(queryRequest.getActivityId());
        }
        if (queryRequest.getParentId() != null) {
            query.parentId(queryRequest.getParentId());
        }
        if (queryRequest.getMessageEventSubscriptionName() != null) {
            query.messageEventSubscriptionName(queryRequest.getMessageEventSubscriptionName());
        }
        if (queryRequest.getSignalEventSubscriptionName() != null) {
            query.signalEventSubscriptionName(queryRequest.getSignalEventSubscriptionName());
        }

        if (queryRequest.getVariables() != null) {
            addVariables(query, queryRequest.getVariables(), false);
        }

        if (queryRequest.getProcessInstanceVariables() != null) {
            addVariables(query, queryRequest.getProcessInstanceVariables(), true);
        }

        if (queryRequest.getTenantId() != null) {
            query.executionTenantId(queryRequest.getTenantId());
        }

        if (queryRequest.getTenantIdLike() != null) {
            query.executionTenantIdLike(queryRequest.getTenantIdLike());
        }

        if (Boolean.TRUE.equals(queryRequest.getWithoutTenantId())) {
            query.executionWithoutTenantId();
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessExecutionInfoWithQuery(query);
        }

        return new ExecutionPaginateList(restResponseFactory).paginateList(requestParams, queryRequest, query, "processInstanceId", allowedSortProperties);
    }

    protected void addVariables(ExecutionQuery processInstanceQuery, List<QueryVariable> variables, boolean process) {
        for (QueryVariable variable : variables) {
            if (variable.getVariableOperation() == null) {
                throw new FlowableIllegalArgumentException("Variable operation is missing for variable: " + variable.getName());
            }
            if (variable.getValue() == null) {
                throw new FlowableIllegalArgumentException("Variable value is missing for variable: " + variable.getName());
            }

            boolean nameLess = variable.getName() == null;

            Object actualValue = restResponseFactory.getVariableValue(variable);

            // A value-only query is only possible using equals-operator
            if (nameLess && variable.getVariableOperation() != QueryVariableOperation.EQUALS) {
                throw new FlowableIllegalArgumentException("Value-only query (without a variable-name) is only supported when using 'equals' operation.");
            }

            switch (variable.getVariableOperation()) {

            case EQUALS:
                if (nameLess) {
                    if (process) {
                        processInstanceQuery.processVariableValueEquals(actualValue);
                    } else {
                        processInstanceQuery.variableValueEquals(actualValue);
                    }
                } else {
                    if (process) {
                        processInstanceQuery.processVariableValueEquals(variable.getName(), actualValue);
                    } else {
                        processInstanceQuery.variableValueEquals(variable.getName(), actualValue);
                    }
                }
                break;

            case EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    if (process) {
                        processInstanceQuery.processVariableValueEqualsIgnoreCase(variable.getName(), (String) actualValue);
                    } else {
                        processInstanceQuery.variableValueEqualsIgnoreCase(variable.getName(), (String) actualValue);
                    }
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case NOT_EQUALS:
                if (process) {
                    processInstanceQuery.processVariableValueNotEquals(variable.getName(), actualValue);
                } else {
                    processInstanceQuery.variableValueNotEquals(variable.getName(), actualValue);
                }
                break;

            case NOT_EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    if (process) {
                        processInstanceQuery.processVariableValueNotEqualsIgnoreCase(variable.getName(), (String) actualValue);
                    } else {
                        processInstanceQuery.variableValueNotEqualsIgnoreCase(variable.getName(), (String) actualValue);
                    }
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;
            default:
                throw new FlowableIllegalArgumentException("Unsupported variable query operation: " + variable.getVariableOperation());
            }
        }
    }

    protected Execution getExecutionFromRequest(String executionId) {
        Execution execution = runtimeService.createExecutionQuery().executionId(executionId).singleResult();
        if (execution == null) {
            throw new FlowableObjectNotFoundException("Could not find an execution with id '" + executionId + "'.", Execution.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessExecutionInfoById(execution);
        }
        
        return execution;
    }

    protected Map<String, Object> getVariablesToSet(List<RestVariable> restVariables) {
        Map<String, Object> variablesToSet = new HashMap<>();
        for (RestVariable var : restVariables) {
            if (var.getName() == null) {
                throw new FlowableIllegalArgumentException("Variable name is required");
            }

            Object actualVariableValue = restResponseFactory.getVariableValue(var);

            variablesToSet.put(var.getName(), actualVariableValue);
        }
        return variablesToSet;
    }
}
