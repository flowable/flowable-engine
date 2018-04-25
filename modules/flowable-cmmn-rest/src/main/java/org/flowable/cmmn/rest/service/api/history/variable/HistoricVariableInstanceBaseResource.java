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

package org.flowable.cmmn.rest.service.api.history.variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.history.HistoricVariableInstanceQuery;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.variable.service.impl.HistoricVariableInstanceQueryProperty;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class HistoricVariableInstanceBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("caseInstanceId", HistoricVariableInstanceQueryProperty.SCOPE_ID);
        allowedSortProperties.put("variableName", HistoricVariableInstanceQueryProperty.VARIABLE_NAME);
    }

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnHistoryService historyService;

    protected DataResponse<HistoricVariableInstanceResponse> getQueryResponse(HistoricVariableInstanceQueryRequest queryRequest, Map<String, String> allRequestParams) {
        HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

        // Populate query based on request
        if (queryRequest.getExcludeTaskVariables() != null) {
            if (queryRequest.getExcludeTaskVariables()) {
                query.excludeTaskVariables();
            }
        }

        if (queryRequest.getTaskId() != null) {
            query.taskId(queryRequest.getTaskId());
        }

        if (queryRequest.getPlanItemInstanceId() != null) {
            query.planItemInstanceId(queryRequest.getPlanItemInstanceId());
        }

        if (queryRequest.getCaseInstanceId() != null) {
            query.caseInstanceId(queryRequest.getCaseInstanceId());
        }

        if (queryRequest.getVariableName() != null) {
            query.variableName(queryRequest.getVariableName());
        }

        if (queryRequest.getVariableNameLike() != null) {
            query.variableNameLike(queryRequest.getVariableNameLike());

        }

        if (queryRequest.getVariables() != null) {
            addVariables(query, queryRequest.getVariables());
        }

        return new HistoricVariableInstancePaginateList(restResponseFactory).paginateList(allRequestParams, query, "variableName", allowedSortProperties);
    }

    protected void addVariables(HistoricVariableInstanceQuery variableInstanceQuery, List<QueryVariable> variables) {
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
            if (nameLess) {
                throw new FlowableIllegalArgumentException("Value-only query (without a variable-name) is not supported");
            }

            switch (variable.getVariableOperation()) {

            case EQUALS:
                variableInstanceQuery.variableValueEquals(variable.getName(), actualValue);
                break;

            default:
                throw new FlowableIllegalArgumentException("Unsupported variable query operation: " + variable.getVariableOperation());
            }
        }
    }
}
