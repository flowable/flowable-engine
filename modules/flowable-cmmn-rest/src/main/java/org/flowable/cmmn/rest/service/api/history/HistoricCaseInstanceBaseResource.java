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

package org.flowable.cmmn.rest.service.api.history;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryProperty;
import org.flowable.cmmn.rest.service.api.RestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable.QueryVariableOperation;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.query.QueryProperty;
import org.flowable.rest.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("caseInstanceId", HistoricCaseInstanceQueryProperty.CASE_INSTANCE_ID);
        allowedSortProperties.put("caseDefinitionId", HistoricCaseInstanceQueryProperty.CASE_DEFINITION_ID);
        allowedSortProperties.put("startTime", HistoricCaseInstanceQueryProperty.CASE_START_TIME);
        allowedSortProperties.put("endTime", HistoricCaseInstanceQueryProperty.CASE_END_TIME);
        allowedSortProperties.put("tenantId", HistoricCaseInstanceQueryProperty.TENANT_ID);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnHistoryService historyService;

    protected DataResponse<HistoricCaseInstanceResponse> getQueryResponse(HistoricCaseInstanceQueryRequest queryRequest, Map<String, String> allRequestParams) {
        HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();

        // Populate query based on request
        if (queryRequest.getCaseInstanceId() != null) {
            query.caseInstanceId(queryRequest.getCaseInstanceId());
        }
        if (queryRequest.getCaseInstanceIds() != null && !queryRequest.getCaseInstanceIds().isEmpty()) {
            query.caseInstanceIds(new HashSet<>(queryRequest.getCaseInstanceIds()));
        }
        if (queryRequest.getCaseDefinitionKey() != null) {
            query.caseDefinitionKey(queryRequest.getCaseDefinitionKey());
        }
        if (queryRequest.getCaseDefinitionId() != null) {
            query.caseDefinitionId(queryRequest.getCaseDefinitionId());
        }
        if (queryRequest.getCaseBusinessKey() != null) {
            query.caseInstanceBusinessKey(queryRequest.getCaseBusinessKey());
        }
        if (queryRequest.getFinishedAfter() != null) {
            query.finishedAfter(queryRequest.getFinishedAfter());
        }
        if (queryRequest.getFinishedBefore() != null) {
            query.finishedBefore(queryRequest.getFinishedBefore());
        }
        if (queryRequest.getStartedAfter() != null) {
            query.startedAfter(queryRequest.getStartedAfter());
        }
        if (queryRequest.getStartedBefore() != null) {
            query.startedBefore(queryRequest.getStartedBefore());
        }
        if (queryRequest.getStartedBy() != null) {
            query.startedBy(queryRequest.getStartedBy());
        }
        if (queryRequest.getFinished() != null) {
            if (queryRequest.getFinished()) {
                query.finished();
            } else {
                query.unfinished();
            }
        }
        if (queryRequest.getVariables() != null) {
            addVariables(query, queryRequest.getVariables());
        }
        if (queryRequest.getTenantId() != null) {
            query.caseInstanceTenantId(queryRequest.getTenantId());
        }

        if (Boolean.TRUE.equals(queryRequest.getWithoutTenantId())) {
            query.caseInstanceWithoutTenantId();
        }

        return new HistoricCaseInstancePaginateList(restResponseFactory).paginateList(allRequestParams, queryRequest, query, "caseInstanceId", allowedSortProperties);
    }

    protected void addVariables(HistoricCaseInstanceQuery caseInstanceQuery, List<QueryVariable> variables) {
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
                    caseInstanceQuery.variableValueEquals(actualValue);
                } else {
                    caseInstanceQuery.variableValueEquals(variable.getName(), actualValue);
                }
                break;

            case EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    caseInstanceQuery.variableValueEqualsIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case NOT_EQUALS:
                caseInstanceQuery.variableValueNotEquals(variable.getName(), actualValue);
                break;

            case LIKE:
                if (actualValue instanceof String) {
                    caseInstanceQuery.variableValueLike(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported for like, but was: " + actualValue.getClass().getName());
                }
                break;

            case LIKE_IGNORE_CASE:
                if (actualValue instanceof String) {
                    caseInstanceQuery.variableValueLikeIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported for like, but was: "
                            + actualValue.getClass().getName());
                }
                break;

            case GREATER_THAN:
                caseInstanceQuery.variableValueGreaterThan(variable.getName(), actualValue);
                break;

            case GREATER_THAN_OR_EQUALS:
                caseInstanceQuery.variableValueGreaterThanOrEqual(variable.getName(), actualValue);
                break;

            case LESS_THAN:
                caseInstanceQuery.variableValueLessThan(variable.getName(), actualValue);
                break;

            case LESS_THAN_OR_EQUALS:
                caseInstanceQuery.variableValueLessThanOrEqual(variable.getName(), actualValue);
                break;

            default:
                throw new FlowableIllegalArgumentException("Unsupported variable query operation: " + variable.getVariableOperation());
            }
        }
    }
}
