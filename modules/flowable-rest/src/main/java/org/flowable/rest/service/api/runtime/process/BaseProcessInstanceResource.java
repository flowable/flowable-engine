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

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.ProcessInstanceQueryProperty;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.QueryVariable;
import org.flowable.rest.service.api.engine.variable.QueryVariable.QueryVariableOperation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Frederik Heremans
 */
public class BaseProcessInstanceResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("processDefinitionId", ProcessInstanceQueryProperty.PROCESS_DEFINITION_ID);
        allowedSortProperties.put("processDefinitionKey", ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY);
        allowedSortProperties.put("id", ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID);
        allowedSortProperties.put("startTime", ProcessInstanceQueryProperty.PROCESS_START_TIME);
        allowedSortProperties.put("tenantId", ProcessInstanceQueryProperty.TENANT_ID);
        allowedSortProperties.put("businessKey", ProcessInstanceQueryProperty.BUSINESS_KEY);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RuntimeService runtimeService;
    
    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected DataResponse<ProcessInstanceResponse> getQueryResponse(ProcessInstanceQueryRequest queryRequest, Map<String, String> requestParams) {

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

        // Populate query based on request
        if (queryRequest.getProcessInstanceId() != null) {
            query.processInstanceId(queryRequest.getProcessInstanceId());
        }
        if (queryRequest.getProcessInstanceIds() != null) {
            query.processInstanceIds(queryRequest.getProcessInstanceIds());
        }
        if (queryRequest.getProcessInstanceName() != null) {
            query.processInstanceName(queryRequest.getProcessInstanceName());
        }
        if (queryRequest.getProcessInstanceNameLike() != null) {
            query.processInstanceNameLike(queryRequest.getProcessInstanceNameLike());
        }
        if (queryRequest.getProcessInstanceNameLikeIgnoreCase() != null) {
            query.processInstanceNameLikeIgnoreCase(queryRequest.getProcessInstanceNameLikeIgnoreCase());
        }
        if (queryRequest.getProcessDefinitionName() != null) {
            query.processDefinitionName(queryRequest.getProcessDefinitionName());
        }
        if (queryRequest.getProcessDefinitionNameLike() != null) {
            query.processDefinitionNameLike(queryRequest.getProcessDefinitionNameLike());
        }
        if (queryRequest.getProcessDefinitionNameLikeIgnoreCase() != null) {
            query.processDefinitionNameLikeIgnoreCase(queryRequest.getProcessDefinitionNameLikeIgnoreCase());
        }
        if (queryRequest.getProcessDefinitionKey() != null) {
            query.processDefinitionKey(queryRequest.getProcessDefinitionKey());
        }
        if (queryRequest.getProcessDefinitionKeyLike() != null) {
            query.processDefinitionKeyLike(queryRequest.getProcessDefinitionKeyLike());
        }
        if (queryRequest.getProcessDefinitionKeyLikeIgnoreCase() != null) {
            query.processDefinitionKeyLikeIgnoreCase(queryRequest.getProcessDefinitionKeyLikeIgnoreCase());
        }
        if (queryRequest.getProcessDefinitionKeys() != null) {
            query.processDefinitionKeys(queryRequest.getProcessDefinitionKeys());
        }
        if (queryRequest.getExcludeProcessDefinitionKeys() != null) {
            query.excludeProcessDefinitionKeys(queryRequest.getExcludeProcessDefinitionKeys());
        }
        if (queryRequest.getProcessDefinitionId() != null) {
            query.processDefinitionId(queryRequest.getProcessDefinitionId());
        }
        if (queryRequest.getProcessDefinitionIds() != null) {
            query.processDefinitionIds(queryRequest.getProcessDefinitionIds());
        }
        if (queryRequest.getProcessDefinitionCategory() != null) {
            query.processDefinitionCategory(queryRequest.getProcessDefinitionCategory());
        }
        if (queryRequest.getProcessDefinitionCategoryLike() != null) {
            query.processDefinitionCategoryLike(queryRequest.getProcessDefinitionCategoryLike());
        }
        if (queryRequest.getProcessDefinitionCategoryLikeIgnoreCase() != null) {
            query.processDefinitionCategoryLikeIgnoreCase(queryRequest.getProcessDefinitionCategoryLikeIgnoreCase());
        }
        if (queryRequest.getProcessDefinitionVersion() != null) {
            query.processDefinitionVersion(queryRequest.getProcessDefinitionVersion());
        }
        if (queryRequest.getProcessDefinitionEngineVersion() != null) {
            query.processDefinitionEngineVersion(queryRequest.getProcessDefinitionEngineVersion());
        }
        if (queryRequest.getRootScopeId() != null) {
            query.processInstanceRootScopeId(queryRequest.getRootScopeId());
        }
        if (queryRequest.getParentScopeId() != null) {
            query.processInstanceParentScopeId(queryRequest.getParentScopeId());
        }
        if (queryRequest.getDeploymentId() != null) {
            query.deploymentId(queryRequest.getDeploymentId());
        }
        if (queryRequest.getDeploymentIdIn() != null) {
            query.deploymentIdIn(queryRequest.getDeploymentIdIn());
        }
        if (queryRequest.getProcessBusinessKey() != null) {
            query.processInstanceBusinessKey(queryRequest.getProcessBusinessKey());
        }
        if (queryRequest.getProcessBusinessKeyLike() != null) {
            query.processInstanceBusinessKeyLike(queryRequest.getProcessBusinessKeyLike());
        }
        if (queryRequest.getProcessBusinessKeyLikeIgnoreCase() != null) {
            query.processInstanceBusinessKeyLikeIgnoreCase(queryRequest.getProcessBusinessKeyLikeIgnoreCase());
        }
        if (queryRequest.getProcessBusinessStatus() != null) {
            query.processInstanceBusinessStatus(queryRequest.getProcessBusinessStatus());
        }
        if (queryRequest.getProcessBusinessStatusLike() != null) {
            query.processInstanceBusinessStatusLike(queryRequest.getProcessBusinessStatusLike());
        }
        if (queryRequest.getProcessBusinessStatusLikeIgnoreCase() != null) {
            query.processInstanceBusinessStatusLikeIgnoreCase(queryRequest.getProcessBusinessStatusLikeIgnoreCase());
        }
        if (queryRequest.getStartedBy() != null) {
            query.startedBy(queryRequest.getStartedBy());
        }
        if (queryRequest.getStartedBefore() != null) {
            query.startedBefore(queryRequest.getStartedBefore());
        }
        if (queryRequest.getStartedAfter() != null) {
            query.startedAfter(queryRequest.getStartedAfter());
        }
        if (queryRequest.getActiveActivityId() != null) {
            query.activeActivityId(queryRequest.getActiveActivityId());
        }
        if (queryRequest.getActiveActivityIds() != null) {
            query.activeActivityIds(queryRequest.getActiveActivityIds());
        }
        if (queryRequest.getInvolvedUser() != null) {
            query.involvedUser(queryRequest.getInvolvedUser());
        }
        if (queryRequest.getSuspended() != null) {
            if (queryRequest.getSuspended()) {
                query.suspended();
            } else {
                query.active();
            }
        }
        if (queryRequest.getSubProcessInstanceId() != null) {
            query.subProcessInstanceId(queryRequest.getSubProcessInstanceId());
        }
        if (queryRequest.getSuperProcessInstanceId() != null) {
            query.superProcessInstanceId(queryRequest.getSuperProcessInstanceId());
        }
        if (queryRequest.getExcludeSubprocesses() != null) {
            query.excludeSubprocesses(queryRequest.getExcludeSubprocesses());
        }
        if (queryRequest.getIncludeProcessVariables() != null) {
            if (queryRequest.getIncludeProcessVariables()) {
                query.includeProcessVariables();
            }
        }
        if (queryRequest.getIncludeProcessVariablesNames() != null) {
            query.includeProcessVariables(queryRequest.getIncludeProcessVariablesNames());
        }
        if (queryRequest.getVariables() != null) {
            addVariables(query, queryRequest.getVariables());
        }
        
        if (queryRequest.getCallbackId() != null) {
            query.processInstanceCallbackId(queryRequest.getCallbackId());
        }

        if(queryRequest.getCallbackIds() != null && !queryRequest.getCallbackIds().isEmpty()) {
            query.processInstanceCallbackIds(queryRequest.getCallbackIds());
        }

        if (queryRequest.getCallbackType() != null) {
            query.processInstanceCallbackType(queryRequest.getCallbackType());
        }
        
        if (queryRequest.getParentCaseInstanceId() != null) {
            query.parentCaseInstanceId(queryRequest.getParentCaseInstanceId());
        }

        if (queryRequest.getTenantId() != null) {
            query.processInstanceTenantId(queryRequest.getTenantId());
        }
        if (queryRequest.getTenantIdLike() != null) {
            query.processInstanceTenantIdLike(queryRequest.getTenantIdLike());
        }
        if (queryRequest.getTenantIdLikeIgnoreCase() != null) {
            query.processInstanceTenantIdLikeIgnoreCase(queryRequest.getTenantIdLikeIgnoreCase());
        }

        if (Boolean.TRUE.equals(queryRequest.getWithoutTenantId())) {
            query.processInstanceWithoutTenantId();
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessProcessInstanceInfoWithQuery(query, queryRequest);
        }

        DataResponse<ProcessInstanceResponse> responseList = paginateList(requestParams, queryRequest, query, "id", allowedSortProperties, restResponseFactory::createProcessInstanceResponseList);
        
        Set<String> processDefinitionIds = new HashSet<>();
        List<ProcessInstanceResponse> processInstanceList = responseList.getData();
        for (ProcessInstanceResponse processInstanceResponse : processInstanceList) {
            if (!processDefinitionIds.contains(processInstanceResponse.getProcessDefinitionId())) {
                processDefinitionIds.add(processInstanceResponse.getProcessDefinitionId());
            }
        }
        
        if (processDefinitionIds.size() > 0) {
            List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery().processDefinitionIds(processDefinitionIds).list();
            Map<String, ProcessDefinition> processDefinitionMap = new HashMap<>();
            for (ProcessDefinition processDefinition : processDefinitionList) {
                processDefinitionMap.put(processDefinition.getId(), processDefinition);
            }
            
            for (ProcessInstanceResponse processInstanceResponse : processInstanceList) {
                if (processDefinitionMap.containsKey(processInstanceResponse.getProcessDefinitionId())) {
                    ProcessDefinition processDefinition = processDefinitionMap.get(processInstanceResponse.getProcessDefinitionId());
                    processInstanceResponse.setProcessDefinitionName(processDefinition.getName());
                    processInstanceResponse.setProcessDefinitionDescription(processDefinition.getDescription());
                }
            }
        }
        
        return responseList;
    }

    protected void addVariables(ProcessInstanceQuery processInstanceQuery, List<QueryVariable> variables) {
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
                    processInstanceQuery.variableValueEquals(actualValue);
                } else {
                    processInstanceQuery.variableValueEquals(variable.getName(), actualValue);
                }
                break;

            case EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    processInstanceQuery.variableValueEqualsIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case NOT_EQUALS:
                processInstanceQuery.variableValueNotEquals(variable.getName(), actualValue);
                break;

            case NOT_EQUALS_IGNORE_CASE:
                if (actualValue instanceof String) {
                    processInstanceQuery.variableValueNotEqualsIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported when ignoring casing, but was: " + actualValue.getClass().getName());
                }
                break;

            case LIKE:
                if (actualValue instanceof String) {
                    processInstanceQuery.variableValueLike(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported for like, but was: " + actualValue.getClass().getName());
                }
                break;

            case LIKE_IGNORE_CASE:
                if (actualValue instanceof String) {
                    processInstanceQuery.variableValueLikeIgnoreCase(variable.getName(), (String) actualValue);
                } else {
                    throw new FlowableIllegalArgumentException("Only string variable values are supported for like, but was: " + actualValue.getClass().getName());
                }
                break;

            case GREATER_THAN:
                processInstanceQuery.variableValueGreaterThan(variable.getName(), actualValue);
                break;

            case GREATER_THAN_OR_EQUALS:
                processInstanceQuery.variableValueGreaterThanOrEqual(variable.getName(), actualValue);
                break;

            case LESS_THAN:
                processInstanceQuery.variableValueLessThan(variable.getName(), actualValue);
                break;

            case LESS_THAN_OR_EQUALS:
                processInstanceQuery.variableValueLessThanOrEqual(variable.getName(), actualValue);
                break;

            default:
                throw new FlowableIllegalArgumentException("Unsupported variable query operation: " + variable.getVariableOperation());
            }
        }
    }

    /**
     * Returns the {@link ProcessInstance} that is requested and calls the access interceptor.
     * Throws the right exceptions when bad request was made or instance was not found.
     */
    protected ProcessInstance getProcessInstanceFromRequest(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);

        if (restApiInterceptor != null) {
            restApiInterceptor.accessProcessInstanceInfoById(processInstance);
        }

        return processInstance;
    }

    /**
     * Returns the {@link ProcessInstance} that is requested without calling the access interceptor
     * Throws the right exceptions when bad request was made or instance was not found.
     */
    protected ProcessInstance getProcessInstanceFromRequestWithoutAccessCheck(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a process instance with id '" + processInstanceId + "'.");
        }
        return processInstance;
    }
}
