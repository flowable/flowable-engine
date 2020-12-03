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

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryProperty;
import org.flowable.cmmn.rest.service.api.CmmnRestApiInterceptor;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable.QueryVariableOperation;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class BaseCaseInstanceResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("caseDefinitionId", CaseInstanceQueryProperty.CASE_DEFINITION_ID);
        allowedSortProperties.put("caseDefinitionKey", CaseInstanceQueryProperty.CASE_DEFINITION_KEY);
        allowedSortProperties.put("id", CaseInstanceQueryProperty.CASE_INSTANCE_ID);
        allowedSortProperties.put("startTime", CaseInstanceQueryProperty.CASE_START_TIME);
        allowedSortProperties.put("tenantId", CaseInstanceQueryProperty.TENANT_ID);
    }

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnRuntimeService runtimeService;

    @Autowired
    protected CmmnRepositoryService repositoryService;

    @Autowired(required=false)
    protected CmmnRestApiInterceptor restApiInterceptor;

    protected DataResponse<CaseInstanceResponse> getQueryResponse(CaseInstanceQueryRequest queryRequest, Map<String, String> requestParams) {

        CaseInstanceQuery query = runtimeService.createCaseInstanceQuery();

        // Populate query based on request
        if (queryRequest.getCaseInstanceId() != null) {
            query.caseInstanceId(queryRequest.getCaseInstanceId());
        }
        if (queryRequest.getCaseDefinitionKey() != null) {
            query.caseDefinitionKey(queryRequest.getCaseDefinitionKey());
        }
        if (queryRequest.getCaseDefinitionId() != null) {
            query.caseDefinitionId(queryRequest.getCaseDefinitionId());
        }
        if (queryRequest.getCaseDefinitionCategory() != null) {
            query.caseDefinitionCategory(queryRequest.getCaseDefinitionCategory());
        }
        if (queryRequest.getCaseBusinessKey() != null) {
            query.caseInstanceBusinessKey(queryRequest.getCaseBusinessKey());
        }
        if (queryRequest.getInvolvedUser() != null) {
            query.involvedUser(queryRequest.getInvolvedUser());
        }
        if (queryRequest.getCaseInstanceParentId() != null) {
            query.caseInstanceParentId(queryRequest.getCaseInstanceParentId());
        }
        if (queryRequest.getIncludeCaseVariables() != null) {
            if (queryRequest.getIncludeCaseVariables()) {
                query.includeCaseVariables();
            }
        }
        if (queryRequest.getVariables() != null) {
            addVariables(query, queryRequest.getVariables());
        }
        
        if (queryRequest.getActivePlanItemDefinitionId() != null) {
            query.activePlanItemDefinitionId(queryRequest.getActivePlanItemDefinitionId());
        }
        
        if (queryRequest.getActivePlanItemDefinitionIds() != null) {
            query.activePlanItemDefinitionIds(queryRequest.getActivePlanItemDefinitionIds());
        }

        if (queryRequest.getTenantId() != null) {
            query.caseInstanceTenantId(queryRequest.getTenantId());
        }

        if (queryRequest.getTenantIdLike() != null) {
            query.caseInstanceTenantIdLike(queryRequest.getTenantIdLike());
        }

        if (Boolean.TRUE.equals(queryRequest.getWithoutTenantId())) {
            query.caseInstanceWithoutTenantId();
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessCaseInstanceInfoWithQuery(query, queryRequest);
        }

        DataResponse<CaseInstanceResponse> responseList = paginateList(requestParams, queryRequest, query, "id", allowedSortProperties, restResponseFactory::createCaseInstanceResponseList);

        Set<String> caseDefinitionIds = new HashSet<>();
        List<CaseInstanceResponse> caseInstanceList = responseList.getData();
        for (CaseInstanceResponse caseInstanceResponse : caseInstanceList) {
            if (!caseDefinitionIds.contains(caseInstanceResponse.getCaseDefinitionId())) {
                caseDefinitionIds.add(caseInstanceResponse.getCaseDefinitionId());
            }
        }

        if (caseDefinitionIds.size() > 0) {
            List<CaseDefinition> caseDefinitionList = repositoryService.createCaseDefinitionQuery().caseDefinitionIds(caseDefinitionIds).list();
            Map<String, CaseDefinition> caseDefinitionMap = new HashMap<>();
            for (CaseDefinition caseDefinition : caseDefinitionList) {
                caseDefinitionMap.put(caseDefinition.getId(), caseDefinition);
            }

            for (CaseInstanceResponse caseInstanceResponse : caseInstanceList) {
                if (caseDefinitionMap.containsKey(caseInstanceResponse.getCaseDefinitionId())) {
                    CaseDefinition caseDefinition = caseDefinitionMap.get(caseInstanceResponse.getCaseDefinitionId());
                    caseInstanceResponse.setCaseDefinitionName(caseDefinition.getName());
                    caseInstanceResponse.setCaseDefinitionDescription(caseDefinition.getDescription());
                }
            }
        }

        return responseList;
    }

    protected CaseInstance getCaseInstanceFromRequest(String caseInstanceId) {
        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();
        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a case instance with id '" + caseInstanceId + "'.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessCaseInstanceInfoById(caseInstance);
        }

        return caseInstance;
    }

    protected void addVariables(CaseInstanceQuery caseInstanceQuery, List<QueryVariable> variables) {
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

            case NOT_EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    caseInstanceQuery.variableValueNotEqualsIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
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
                    throw new FlowableIllegalArgumentException("Only string variable values are supported for like, but was: " + actualValue.getClass().getName());
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
