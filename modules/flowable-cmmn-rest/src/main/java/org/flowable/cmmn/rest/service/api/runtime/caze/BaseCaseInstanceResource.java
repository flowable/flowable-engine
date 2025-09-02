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
        allowedSortProperties.put("businessKey", CaseInstanceQueryProperty.BUSINESS_KEY);
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
        if (queryRequest.getCaseInstanceIds() != null && !queryRequest.getCaseInstanceIds().isEmpty()) {
            query.caseInstanceIds(queryRequest.getCaseInstanceIds());
        }
        if (queryRequest.getCaseDefinitionKey() != null) {
            query.caseDefinitionKey(queryRequest.getCaseDefinitionKey());
        }
        if (queryRequest.getCaseDefinitionKeyLike() != null) {
            query.caseDefinitionKeyLike(queryRequest.getCaseDefinitionKeyLike());
        }
        if (queryRequest.getCaseDefinitionKeyLikeIgnoreCase() != null) {
            query.caseDefinitionKeyLikeIgnoreCase(queryRequest.getCaseDefinitionKeyLikeIgnoreCase());
        }
        if (queryRequest.getCaseDefinitionKeys() != null && !queryRequest.getCaseDefinitionKeys().isEmpty()) {
            query.caseDefinitionKeys(queryRequest.getCaseDefinitionKeys());
        }
        if (queryRequest.getExcludeCaseDefinitionKeys() != null && !queryRequest.getExcludeCaseDefinitionKeys().isEmpty()) {
            query.excludeCaseDefinitionKeys(queryRequest.getExcludeCaseDefinitionKeys());
        }
        if (queryRequest.getCaseDefinitionId() != null) {
            query.caseDefinitionId(queryRequest.getCaseDefinitionId());
        }
        if (queryRequest.getCaseDefinitionCategory() != null) {
            query.caseDefinitionCategory(queryRequest.getCaseDefinitionCategory());
        }
        if (queryRequest.getCaseDefinitionCategoryLike() != null) {
            query.caseDefinitionCategoryLike(queryRequest.getCaseDefinitionCategoryLike());
        }
        if (queryRequest.getCaseDefinitionCategoryLikeIgnoreCase() != null) {
            query.caseDefinitionCategoryLikeIgnoreCase(queryRequest.getCaseDefinitionCategoryLikeIgnoreCase());
        }
        if (queryRequest.getCaseDefinitionName() != null) {
            query.caseDefinitionName(queryRequest.getCaseDefinitionName());
        }
        if (queryRequest.getCaseDefinitionNameLike() != null) {
            query.caseDefinitionNameLike(queryRequest.getCaseDefinitionNameLike());
        }
        if (queryRequest.getCaseDefinitionNameLikeIgnoreCase() != null) {
            query.caseDefinitionNameLikeIgnoreCase(queryRequest.getCaseDefinitionNameLikeIgnoreCase());
        }
        if (queryRequest.getCaseBusinessKey() != null) {
            query.caseInstanceBusinessKey(queryRequest.getCaseBusinessKey());
        }
        if (queryRequest.getCaseInstanceName() != null) {
            query.caseInstanceName(queryRequest.getCaseInstanceName());
        }
        if (queryRequest.getCaseInstanceNameLike() != null) {
            query.caseInstanceNameLike(queryRequest.getCaseInstanceNameLike());
        }
        if (queryRequest.getCaseInstanceNameLikeIgnoreCase() != null) {
            query.caseInstanceNameLikeIgnoreCase(queryRequest.getCaseInstanceNameLikeIgnoreCase());
        }
        if (queryRequest.getCaseInstanceRootScopeId() != null) {
            query.caseInstanceRootScopeId(queryRequest.getCaseInstanceRootScopeId());
        }
        if (queryRequest.getCaseInstanceParentScopeId() != null) {
            query.caseInstanceParentScopeId(queryRequest.getCaseInstanceParentScopeId());
        }
        if (queryRequest.getCaseInstanceBusinessKey() != null) {
            query.caseInstanceBusinessKey(queryRequest.getCaseInstanceBusinessKey());
        }
        if (queryRequest.getCaseInstanceBusinessKeyLike() != null) {
            query.caseInstanceBusinessKeyLike(queryRequest.getCaseInstanceBusinessKeyLike());
        }
        if (queryRequest.getCaseInstanceBusinessKeyLikeIgnoreCase() != null) {
            query.caseInstanceBusinessKeyLikeIgnoreCase(queryRequest.getCaseInstanceBusinessKeyLikeIgnoreCase());
        }
        if (queryRequest.getCaseInstanceBusinessStatus() != null) {
            query.caseInstanceBusinessStatus(queryRequest.getCaseInstanceBusinessStatus());
        }
        if (queryRequest.getCaseInstanceBusinessStatusLike() != null) {
            query.caseInstanceBusinessStatusLike(queryRequest.getCaseInstanceBusinessStatusLike());
        }
        if (queryRequest.getCaseInstanceBusinessStatusLikeIgnoreCase() != null) {
            query.caseInstanceBusinessStatusLikeIgnoreCase(queryRequest.getCaseInstanceBusinessStatusLikeIgnoreCase());
        }
        if (queryRequest.getInvolvedUser() != null) {
            query.involvedUser(queryRequest.getInvolvedUser());
        }
        if (queryRequest.getCaseInstanceParentId() != null) {
            query.caseInstanceParentId(queryRequest.getCaseInstanceParentId());
        }
        if (queryRequest.getCaseInstanceState() != null) {
            query.caseInstanceState(queryRequest.getCaseInstanceState());
        }
        if (queryRequest.getCaseInstanceStartedBy() != null) {
            query.caseInstanceStartedBy(queryRequest.getCaseInstanceStartedBy());
        }
        if (queryRequest.getCaseInstanceStartedBefore() != null) {
            query.caseInstanceStartedBefore(queryRequest.getCaseInstanceStartedBefore());
        }
        if (queryRequest.getCaseInstanceStartedAfter() != null) {
            query.caseInstanceStartedAfter(queryRequest.getCaseInstanceStartedAfter());
        }
        if (queryRequest.getCaseInstanceCallbackId() != null) {
            query.caseInstanceCallbackId(queryRequest.getCaseInstanceCallbackId());
        }
        if (queryRequest.getCaseInstanceCallbackIds() != null && !queryRequest.getCaseInstanceCallbackIds().isEmpty()) {
            query.caseInstanceCallbackIds(queryRequest.getCaseInstanceCallbackIds());
        }
        if (queryRequest.getCaseInstanceCallbackType() != null) {
            query.caseInstanceCallbackType(queryRequest.getCaseInstanceCallbackType());
        }
        if (queryRequest.getParentCaseInstanceId() != null) {
            query.parentCaseInstanceId(queryRequest.getParentCaseInstanceId());
        }
        if (queryRequest.getCaseInstanceReferenceId() != null) {
            query.caseInstanceReferenceId(queryRequest.getCaseInstanceReferenceId());
        }
        if (queryRequest.getCaseInstanceReferenceType() != null) {
            query.caseInstanceReferenceType(queryRequest.getCaseInstanceReferenceType());
        }
        if (queryRequest.getCaseInstanceLastReactivatedBy() != null) {
            query.caseInstanceLastReactivatedBy(queryRequest.getCaseInstanceLastReactivatedBy());
        }
        if (queryRequest.getCaseInstanceLastReactivatedBefore() != null) {
            query.caseInstanceLastReactivatedBefore(queryRequest.getCaseInstanceLastReactivatedBefore());
        }
        if (queryRequest.getCaseInstanceLastReactivatedAfter() != null) {
            query.caseInstanceLastReactivatedAfter(queryRequest.getCaseInstanceLastReactivatedAfter());
        }
        if (queryRequest.getIncludeCaseVariables() != null) {
            if (queryRequest.getIncludeCaseVariables()) {
                query.includeCaseVariables();
            }
        }
        if (queryRequest.getIncludeCaseVariablesNames() != null) {
            query.includeCaseVariables(queryRequest.getIncludeCaseVariablesNames());
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
        
        if (queryRequest.getTenantIdLikeIgnoreCase() != null) {
            query.caseInstanceTenantIdLikeIgnoreCase(queryRequest.getTenantIdLikeIgnoreCase());
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

    /**
     * Returns the {@link CaseInstance} that is requested and calls the access interceptor.
     * Throws the right exceptions when bad request was made or instance was not found.
     */
    protected CaseInstance getCaseInstanceFromRequest(String caseInstanceId) {
        CaseInstance caseInstance = getCaseInstanceFromRequestWithoutAccessCheck(caseInstanceId);

        if (restApiInterceptor != null) {
            restApiInterceptor.accessCaseInstanceInfoById(caseInstance);
        }

        return caseInstance;
    }

    /**
     * Returns the {@link CaseInstance} that is requested without calling the access interceptor
     * Throws the right exceptions when bad request was made or instance was not found.
     */
    protected CaseInstance getCaseInstanceFromRequestWithoutAccessCheck(String caseInstanceId) {
        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();
        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a case instance with id '" + caseInstanceId + "'.");
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
