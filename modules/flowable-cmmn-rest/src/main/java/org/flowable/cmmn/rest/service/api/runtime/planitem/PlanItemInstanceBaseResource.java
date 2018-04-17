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

package org.flowable.cmmn.rest.service.api.runtime.planitem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryProperty;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable.QueryVariableOperation;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class PlanItemInstanceBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("name", PlanItemInstanceQueryProperty.NAME);
        allowedSortProperties.put("startTime", PlanItemInstanceQueryProperty.START_TIME);
    }

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnRuntimeService runtimeService;

    protected DataResponse<PlanItemInstanceResponse> getQueryResponse(PlanItemInstanceQueryRequest queryRequest, Map<String, String> requestParams, String serverRootUrl) {

        PlanItemInstanceQuery query = runtimeService.createPlanItemInstanceQuery();

        // Populate query based on request
        if (queryRequest.getId() != null) {
            query.planItemInstanceId(queryRequest.getId());
        }
        if (queryRequest.getCaseInstanceId() != null) {
            query.caseInstanceId(queryRequest.getCaseInstanceId());
        }
        if (queryRequest.getCaseDefinitionId() != null) {
            query.caseDefinitionId(queryRequest.getCaseDefinitionId());
        }
        if (queryRequest.getStageInstanceId() != null) {
            query.stageInstanceId(queryRequest.getStageInstanceId());
        }
        if (queryRequest.getPlanItemDefinitionId() != null) {
            query.planItemDefinitionId(queryRequest.getPlanItemDefinitionId());
        }
        if (queryRequest.getPlanItemDefinitionType() != null) {
            query.planItemDefinitionType(queryRequest.getPlanItemDefinitionType());
        }
        if (queryRequest.getName() != null) {
            query.planItemInstanceName(queryRequest.getName());
        }
        if (queryRequest.getElementId() != null) {
            query.planItemInstanceElementId(queryRequest.getElementId());
        }
        if (queryRequest.getState() != null) {
            query.planItemInstanceState(queryRequest.getState());
        }
        if (queryRequest.getReferenceId() != null) {
            query.planItemInstanceReferenceId(queryRequest.getReferenceId());
        }
        if (queryRequest.getReferenceType() != null) {
            query.planItemInstanceReferenceType(queryRequest.getReferenceType());
        }
        if (queryRequest.getStartedBefore() != null) {
            query.planItemInstanceStartedBefore(queryRequest.getStartedBefore());
        }
        if (queryRequest.getStartedAfter() != null) {
            query.planItemInstanceStartedAfter(queryRequest.getStartedAfter());
        }
        if (queryRequest.getStartUserId() != null) {
            query.planItemInstanceStartUserId(queryRequest.getStartUserId());
        }

        if (queryRequest.getVariables() != null) {
            addVariables(query, queryRequest.getVariables(), false);
        }

        if (queryRequest.getCaseInstanceVariables() != null) {
            addVariables(query, queryRequest.getCaseInstanceVariables(), true);
        }

        if (queryRequest.getTenantId() != null) {
            query.planItemInstanceTenantId(queryRequest.getTenantId());
        }

        if (Boolean.TRUE.equals(queryRequest.getWithoutTenantId())) {
            query.planItemInstanceWithoutTenantId();
        }

        return new PlanItemInstancePaginateList(restResponseFactory).paginateList(requestParams, queryRequest, query, "startTime", allowedSortProperties);
    }

    protected void addVariables(PlanItemInstanceQuery planItemInstanceQuery, List<QueryVariable> variables, boolean isCase) {
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
                    if (isCase) {
                        planItemInstanceQuery.caseVariableValueEquals(actualValue);
                    } else {
                        planItemInstanceQuery.variableValueEquals(actualValue);
                    }
                } else {
                    if (isCase) {
                        planItemInstanceQuery.caseVariableValueEquals(variable.getName(), actualValue);
                    } else {
                        planItemInstanceQuery.variableValueEquals(variable.getName(), actualValue);
                    }
                }
                break;

            case EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    if (isCase) {
                        planItemInstanceQuery.caseVariableValueEqualsIgnoreCase(variable.getName(), (String) actualValue);
                    } else {
                        planItemInstanceQuery.variableValueEqualsIgnoreCase(variable.getName(), (String) actualValue);
                    }
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case NOT_EQUALS:
                if (isCase) {
                    planItemInstanceQuery.caseVariableValueNotEquals(variable.getName(), actualValue);
                } else {
                    planItemInstanceQuery.variableValueNotEquals(variable.getName(), actualValue);
                }
                break;

            case NOT_EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    if (isCase) {
                        planItemInstanceQuery.caseVariableValueNotEqualsIgnoreCase(variable.getName(), (String) actualValue);
                    } else {
                        planItemInstanceQuery.variableValueNotEqualsIgnoreCase(variable.getName(), (String) actualValue);
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

    protected PlanItemInstance getPlanItemInstanceFromRequest(String planItemInstanceId) {
        PlanItemInstance planItemInstance = runtimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstanceId).singleResult();
        if (planItemInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find an plan item instance with id '" + planItemInstance + "'.", PlanItemInstance.class);
        }
        return planItemInstance;
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
