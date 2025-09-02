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

package org.flowable.rest.service.api.history;

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
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.HistoricProcessInstanceQueryProperty;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.QueryVariable;
import org.flowable.rest.service.api.engine.variable.QueryVariable.QueryVariableOperation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class HistoricProcessInstanceBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("processInstanceId", HistoricProcessInstanceQueryProperty.PROCESS_INSTANCE_ID_);
        allowedSortProperties.put("processDefinitionId", HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_ID);
        allowedSortProperties.put("businessKey", HistoricProcessInstanceQueryProperty.BUSINESS_KEY);
        allowedSortProperties.put("startTime", HistoricProcessInstanceQueryProperty.START_TIME);
        allowedSortProperties.put("endTime", HistoricProcessInstanceQueryProperty.END_TIME);
        allowedSortProperties.put("duration", HistoricProcessInstanceQueryProperty.DURATION);
        allowedSortProperties.put("tenantId", HistoricProcessInstanceQueryProperty.TENANT_ID);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;
    
    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected DataResponse<HistoricProcessInstanceResponse> getQueryResponse(HistoricProcessInstanceQueryRequest queryRequest, Map<String, String> allRequestParams) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

        // Populate query based on request
        if (queryRequest.getProcessInstanceId() != null) {
            query.processInstanceId(queryRequest.getProcessInstanceId());
        }
        if (queryRequest.getProcessInstanceIds() != null && !queryRequest.getProcessInstanceIds().isEmpty()) {
            query.processInstanceIds(new HashSet<>(queryRequest.getProcessInstanceIds()));
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
            query.processDefinitionKeyIn(queryRequest.getProcessDefinitionKeys());
        }
        if (queryRequest.getExcludeProcessDefinitionKeys() != null) {
            query.excludeProcessDefinitionKeys(queryRequest.getExcludeProcessDefinitionKeys());
        }
        if (queryRequest.getProcessDefinitionKeyIn() != null) {
            query.processDefinitionKeyIn(queryRequest.getProcessDefinitionKeyIn());
        }
        if (queryRequest.getProcessDefinitionKeyNotIn() != null) {
            query.processDefinitionKeyNotIn(queryRequest.getProcessDefinitionKeyNotIn());
        }
        if (queryRequest.getProcessDefinitionId() != null) {
            query.processDefinitionId(queryRequest.getProcessDefinitionId());
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
        if (queryRequest.getProcessDefinitionVersion() != null) {
            query.processDefinitionVersion(queryRequest.getProcessDefinitionVersion());
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
        if (queryRequest.getActiveActivityId() != null) {
            query.activeActivityId(queryRequest.getActiveActivityId());
        }
        if (queryRequest.getActiveActivityIds() != null) {
            query.activeActivityIds(queryRequest.getActiveActivityIds());
        }
        if (queryRequest.getInvolvedUser() != null) {
            query.involvedUser(queryRequest.getInvolvedUser());
        }
        if (queryRequest.getSuperProcessInstanceId() != null) {
            query.superProcessInstanceId(queryRequest.getSuperProcessInstanceId());
        }
        if (queryRequest.getExcludeSubprocesses() != null) {
            query.excludeSubprocesses(queryRequest.getExcludeSubprocesses());
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
        if (queryRequest.getFinishedBy() != null) {
            query.finishedBy(queryRequest.getFinishedBy());
        }
        if (queryRequest.getState() != null) {
            query.state(queryRequest.getState());
        }
        if (queryRequest.getFinished() != null) {
            if (queryRequest.getFinished()) {
                query.finished();
            } else {
                query.unfinished();
            }
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

        if (queryRequest.getCallbackIds() != null && !queryRequest.getCallbackIds().isEmpty()) {
            query.processInstanceCallbackIds(queryRequest.getCallbackIds());
        }

        if (queryRequest.getCallbackType() != null) {
            query.processInstanceCallbackType(queryRequest.getCallbackType());
        }
        if (Boolean.TRUE.equals(queryRequest.getWithoutCallbackId())) {
            query.withoutProcessInstanceCallbackId();
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
            restApiInterceptor.accessHistoryProcessInfoWithQuery(query, queryRequest);
        }

        DataResponse<HistoricProcessInstanceResponse> responseList = paginateList(allRequestParams, queryRequest, query, "processInstanceId", allowedSortProperties,
                restResponseFactory::createHistoricProcessInstanceResponseList);
        
        Set<String> processDefinitionIds = new HashSet<>();
        List<HistoricProcessInstanceResponse> processInstanceList = responseList.getData();
        for (HistoricProcessInstanceResponse processInstanceResponse : processInstanceList) {
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
            
            for (HistoricProcessInstanceResponse processInstanceResponse : processInstanceList) {
                if (processDefinitionMap.containsKey(processInstanceResponse.getProcessDefinitionId())) {
                    ProcessDefinition processDefinition = processDefinitionMap.get(processInstanceResponse.getProcessDefinitionId());
                    processInstanceResponse.setProcessDefinitionName(processDefinition.getName());
                    processInstanceResponse.setProcessDefinitionDescription(processDefinition.getDescription());
                }
            }
        }
        
        return responseList;
    }
    
    protected HistoricProcessInstance getHistoricProcessInstanceFromRequest(String processInstanceId) {
        HistoricProcessInstance processInstance = getHistoricProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);

        if (restApiInterceptor != null) {
            restApiInterceptor.accessHistoryProcessInfoById(processInstance);
        }
        
        return processInstance;
    }

    protected HistoricProcessInstance getHistoricProcessInstanceFromRequestWithoutAccessCheck(String processInstanceId) {
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a process instance with id '" + processInstanceId + "'.", HistoricProcessInstance.class);
        }

        return processInstance;
    }

    protected void addVariables(HistoricProcessInstanceQuery processInstanceQuery, List<QueryVariable> variables) {
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
                    throw new FlowableIllegalArgumentException("Only string variable values are supported for like, but was: "
                            + actualValue.getClass().getName());
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
}
