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

package org.flowable.cmmn.rest.service.api;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.deployer.ResourceNameUtil;
import org.flowable.cmmn.rest.service.api.engine.RestIdentityLink;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.flowable.cmmn.rest.service.api.history.caze.HistoricCaseInstanceResponse;
import org.flowable.cmmn.rest.service.api.history.milestone.HistoricMilestoneInstanceResponse;
import org.flowable.cmmn.rest.service.api.history.planitem.HistoricPlanItemInstanceResponse;
import org.flowable.cmmn.rest.service.api.history.task.HistoricIdentityLinkResponse;
import org.flowable.cmmn.rest.service.api.history.task.HistoricTaskInstanceResponse;
import org.flowable.cmmn.rest.service.api.history.variable.HistoricVariableInstanceResponse;
import org.flowable.cmmn.rest.service.api.management.HistoryJobResponse;
import org.flowable.cmmn.rest.service.api.management.JobResponse;
import org.flowable.cmmn.rest.service.api.repository.CaseDefinitionResponse;
import org.flowable.cmmn.rest.service.api.repository.CmmnDeploymentResponse;
import org.flowable.cmmn.rest.service.api.repository.DecisionResponse;
import org.flowable.cmmn.rest.service.api.repository.DeploymentResourceResponse;
import org.flowable.cmmn.rest.service.api.repository.FormDefinitionResponse;
import org.flowable.cmmn.rest.service.api.runtime.caze.CaseInstanceResponse;
import org.flowable.cmmn.rest.service.api.runtime.caze.EventSubscriptionResponse;
import org.flowable.cmmn.rest.service.api.runtime.planitem.PlanItemInstanceResponse;
import org.flowable.cmmn.rest.service.api.runtime.task.TaskResponse;
import org.flowable.cmmn.rest.service.api.runtime.variable.VariableInstanceResponse;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.common.rest.util.RestUrlBuilder;
import org.flowable.common.rest.variable.BigDecimalRestVariableConverter;
import org.flowable.common.rest.variable.BigIntegerRestVariableConverter;
import org.flowable.common.rest.variable.BooleanRestVariableConverter;
import org.flowable.common.rest.variable.DateRestVariableConverter;
import org.flowable.common.rest.variable.DoubleRestVariableConverter;
import org.flowable.common.rest.variable.InstantRestVariableConverter;
import org.flowable.common.rest.variable.IntegerRestVariableConverter;
import org.flowable.common.rest.variable.JsonObjectRestVariableConverter;
import org.flowable.common.rest.variable.LocalDateRestVariableConverter;
import org.flowable.common.rest.variable.LocalDateTimeRestVariableConverter;
import org.flowable.common.rest.variable.LongRestVariableConverter;
import org.flowable.common.rest.variable.RestVariableConverter;
import org.flowable.common.rest.variable.ShortRestVariableConverter;
import org.flowable.common.rest.variable.StringRestVariableConverter;
import org.flowable.common.rest.variable.UUIDRestVariableConverter;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.form.api.FormDefinition;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default implementation of a {@link CmmnRestResponseFactory}.
 * <p>
 * Added a new "createProcessInstanceResponse" method (with a different signature) to conditionally return the process variables that exist within the process instance when the first wait state is
 * encountered (or when the process instance completes). Also added the population of a "completed" flag - within both the original "createProcessInstanceResponse" method and the new one with the
 * different signature - to let the caller know whether the process instance has completed or not.
 *
 * @author Frederik Heremans
 * @author Ryan Johnston (@rjfsu)
 */
public class CmmnRestResponseFactory {

    public static final int VARIABLE_TASK = 1;
    public static final int VARIABLE_EXECUTION = 2;
    public static final int VARIABLE_CASE = 3;
    public static final int VARIABLE_VARINSTANCE = 4;
    public static final int VARIABLE_HISTORY_TASK = 5;
    public static final int VARIABLE_HISTORY_CASE = 6;
    public static final int VARIABLE_HISTORY_VARINSTANCE = 7;
    public static final int VARIABLE_PLAN_ITEM = 8;

    public static final String BYTE_ARRAY_VARIABLE_TYPE = "binary";
    public static final String SERIALIZABLE_VARIABLE_TYPE = "serializable";

    protected ObjectMapper objectMapper;
    protected List<RestVariableConverter> variableConverters = new ArrayList<>();

    public CmmnRestResponseFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        initializeVariableConverters();
    }

    public List<TaskResponse> createTaskResponseList(List<Task> tasks) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<TaskResponse> responseList = new ArrayList<>(tasks.size());
        for (Task instance : tasks) {
            responseList.add(createTaskResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public TaskResponse createTaskResponse(Task task) {
        return createTaskResponse(task, createUrlBuilder());
    }

    public TaskResponse createTaskResponse(Task task, RestUrlBuilder urlBuilder) {
        TaskResponse response = new TaskResponse(task);
        response.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_TASK, task.getId()));

        // Add references to other resources, if needed
        if (response.getParentTaskId() != null) {
            response.setParentTaskUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_TASK, response.getParentTaskId()));
        }
        if (response.getCaseDefinitionId() != null) {
            response.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, response.getCaseDefinitionId()));
        }
        if (response.getCaseInstanceId() != null) {
            response.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, response.getCaseInstanceId()));
        }

        Map<String, Object> variableMap = task.getProcessVariables();
        if (variableMap != null) {
            for (String name : variableMap.keySet()) {
                response.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.GLOBAL, task.getId(), VARIABLE_TASK, false, urlBuilder));
            }
        }

        variableMap = task.getTaskLocalVariables();
        if (variableMap != null) {
            for (String name : variableMap.keySet()) {
                response.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.LOCAL, task.getId(), VARIABLE_TASK, false, urlBuilder));
            }
        }

        return response;
    }

    public List<CmmnDeploymentResponse> createDeploymentResponseList(List<CmmnDeployment> deployments) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<CmmnDeploymentResponse> responseList = new ArrayList<>(deployments.size());
        for (CmmnDeployment instance : deployments) {
            responseList.add(createDeploymentResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public CmmnDeploymentResponse createDeploymentResponse(CmmnDeployment deployment) {
        return createDeploymentResponse(deployment, createUrlBuilder());
    }

    public CmmnDeploymentResponse createDeploymentResponse(CmmnDeployment deployment, RestUrlBuilder urlBuilder) {
        return new CmmnDeploymentResponse(deployment, urlBuilder.buildUrl(CmmnRestUrls.URL_DEPLOYMENT, deployment.getId()));
    }

    public List<DeploymentResourceResponse> createDeploymentResourceResponseList(String deploymentId, List<String> resourceList, ContentTypeResolver contentTypeResolver) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        // Add additional metadata to the artifact-strings before returning
        List<DeploymentResourceResponse> responseList = new ArrayList<>(resourceList.size());
        for (String resourceId : resourceList) {
            String contentType = contentTypeResolver.resolveContentType(resourceId);
            responseList.add(createDeploymentResourceResponse(deploymentId, resourceId, contentType, urlBuilder));
        }
        return responseList;
    }

    public DeploymentResourceResponse createDeploymentResourceResponse(String deploymentId, String resourceId, String contentType) {
        return createDeploymentResourceResponse(deploymentId, resourceId, contentType, createUrlBuilder());
    }

    public DeploymentResourceResponse createDeploymentResourceResponse(String deploymentId, String resourceId, String contentType, RestUrlBuilder urlBuilder) {
        // Create URL's
        String resourceUrl = urlBuilder.buildUrl(CmmnRestUrls.URL_DEPLOYMENT_RESOURCE, deploymentId, resourceId);
        String resourceContentUrl = urlBuilder.buildUrl(CmmnRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deploymentId, resourceId);

        // Determine type
        String type = "resource";
        for (String suffix : ResourceNameUtil.CMMN_RESOURCE_SUFFIXES) {
            if (resourceId.endsWith(suffix)) {
                type = "caseDefinition";
                break;
            }
        }
        return new DeploymentResourceResponse(resourceId, resourceUrl, resourceContentUrl, contentType, type);
    }

    public List<CaseDefinitionResponse> createCaseDefinitionResponseList(List<CaseDefinition> caseDefinitions) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<CaseDefinitionResponse> responseList = new ArrayList<>(caseDefinitions.size());
        for (CaseDefinition instance : caseDefinitions) {
            responseList.add(createCaseDefinitionResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public CaseDefinitionResponse createCaseDefinitionResponse(CaseDefinition caseDefinition) {
        return createCaseDefinitionResponse(caseDefinition, createUrlBuilder());
    }

    public CaseDefinitionResponse createCaseDefinitionResponse(CaseDefinition caseDefinition, RestUrlBuilder urlBuilder) {
        CaseDefinitionResponse response = new CaseDefinitionResponse();
        response.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseDefinition.getId()));
        response.setId(caseDefinition.getId());
        response.setKey(caseDefinition.getKey());
        response.setVersion(caseDefinition.getVersion());
        response.setCategory(caseDefinition.getCategory());
        response.setName(caseDefinition.getName());
        response.setDescription(caseDefinition.getDescription());
        response.setGraphicalNotationDefined(caseDefinition.hasGraphicalNotation());
        response.setStartFormDefined(caseDefinition.hasStartFormKey());
        response.setTenantId(caseDefinition.getTenantId());

        // Links to other resources
        response.setDeploymentId(caseDefinition.getDeploymentId());
        response.setDeploymentUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_DEPLOYMENT, caseDefinition.getDeploymentId()));
        response.setResource(urlBuilder.buildUrl(CmmnRestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getResourceName()));
        if (caseDefinition.getDiagramResourceName() != null) {
            response.setDiagramResource(urlBuilder.buildUrl(CmmnRestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getDiagramResourceName()));
        }
        return response;
    }
    
    public String getFormModelString(FormModelResponse formModelResponse) {
        try {
            return objectMapper.writeValueAsString(formModelResponse);
        } catch (Exception e) {
            throw new FlowableException("Error writing form model response", e);
        }
    }

    public List<RestVariable> createRestVariables(Map<String, Object> variables, String id, int variableType) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<RestVariable> result = new ArrayList<>(variables.size());

        for (Entry<String, Object> pair : variables.entrySet()) {
            result.add(createRestVariable(pair.getKey(), pair.getValue(), null, id, variableType, false, urlBuilder));
        }

        return result;
    }

    public List<RestVariable> createRestVariables(Map<String, Object> variables, String id, int variableType, RestVariableScope scope) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<RestVariable> result = new ArrayList<>(variables.size());

        for (Entry<String, Object> pair : variables.entrySet()) {
            result.add(createRestVariable(pair.getKey(), pair.getValue(), scope, id, variableType, false, urlBuilder));
        }

        return result;
    }

    public RestVariable createRestVariable(String name, Object value, RestVariableScope scope, String id, int variableType, boolean includeBinaryValue) {
        return createRestVariable(name, value, scope, id, variableType, includeBinaryValue, createUrlBuilder());
    }

    public RestVariable createRestVariable(String name, Object value, RestVariableScope scope, String id, int variableType, boolean includeBinaryValue, RestUrlBuilder urlBuilder) {

        RestVariableConverter converter = null;
        RestVariable restVar = new RestVariable();
        restVar.setVariableScope(scope);
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

                if (variableType == VARIABLE_TASK) {
                    restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_TASK_VARIABLE_DATA, id, name));
                } else if (variableType == VARIABLE_CASE) {
                    restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, id, name));
                } else if (variableType == VARIABLE_VARINSTANCE) {
                    restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_VARIABLE_INSTANCE_DATA, id, name));
                } else if (variableType == VARIABLE_HISTORY_TASK) {
                    restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, id, name));
                } else if (variableType == VARIABLE_HISTORY_VARINSTANCE) {
                    restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_VARIABLE_INSTANCE_DATA, id, name));
                } else if (variableType == VARIABLE_HISTORY_CASE) {
                    restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_VARIABLE_DATA, id, name));
                } else if (variableType == VARIABLE_PLAN_ITEM) {
                    restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE_DATA, id, name));
                }
            }
        }
        return restVar;
    }

    public RestVariable createBinaryRestVariable(String name, RestVariableScope scope, String type, String instanceId, int responseVariableType) {

        RestUrlBuilder urlBuilder = createUrlBuilder();
        RestVariable restVar = new RestVariable();
        restVar.setVariableScope(scope);
        restVar.setName(name);
        restVar.setType(type);

        if (responseVariableType == VARIABLE_TASK) {
            restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_TASK_VARIABLE_DATA, instanceId, name));
        } else if (responseVariableType == VARIABLE_CASE) {
            restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, instanceId, name));
        } else if (responseVariableType == VARIABLE_PLAN_ITEM) {
            restVar.setValueUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE_DATA, instanceId, name));
        }

        return restVar;
    }

    public Object getVariableValue(RestVariable restVariable) {
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

    public Object getVariableValue(QueryVariable restVariable) {
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

            RestVariable temp = new RestVariable();
            temp.setValue(restVariable.getValue());
            temp.setType(restVariable.getType());
            temp.setName(restVariable.getName());
            value = converter.getVariableValue(temp);

        } else {
            // Revert to type determined by REST-to-Java mapping when no
            // explicit type has been provided
            value = restVariable.getValue();
        }
        return value;
    }

    public List<RestIdentityLink> createRestIdentityLinks(List<IdentityLink> links) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<RestIdentityLink> responseList = new ArrayList<>(links.size());
        for (IdentityLink instance : links) {
            responseList.add(createRestIdentityLink(instance, urlBuilder));
        }
        return responseList;
    }

    public RestIdentityLink createRestIdentityLink(IdentityLink link) {
        return createRestIdentityLink(link, createUrlBuilder());
    }

    public RestIdentityLink createRestIdentityLink(IdentityLink link, RestUrlBuilder urlBuilder) {
        return createRestIdentityLink(link.getType(), link.getUserId(), link.getGroupId(), link.getTaskId(), link.getScopeDefinitionId(), link.getScopeId(), urlBuilder);
    }

    public RestIdentityLink createRestIdentityLink(String type, String userId, String groupId, String taskId, String caseDefinitionId, String caseInstanceId) {
        return createRestIdentityLink(type, userId, groupId, taskId, caseDefinitionId, caseInstanceId, createUrlBuilder());
    }

    public RestIdentityLink createRestIdentityLink(String type, String userId, String groupId, String taskId, String caseDefinitionId, String caseInstanceId, RestUrlBuilder urlBuilder) {
        RestIdentityLink result = new RestIdentityLink();
        result.setUser(userId);
        result.setGroup(groupId);
        result.setType(type);

        String family = null;
        if (userId != null) {
            family = CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS;
        } else {
            family = CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS;
        }

        if (caseInstanceId != null) {
            result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstanceId, (userId != null ? userId : groupId), type));
        } else if (taskId != null) {
            result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_TASK_IDENTITYLINK, taskId, family, (userId != null ? userId : groupId), type));
        } else if (caseDefinitionId != null) {
            result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinitionId, family, (userId != null ? userId : groupId)));
        }
        return result;
    }

    public List<CaseInstanceResponse> createCaseInstanceResponseList(List<CaseInstance> caseInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<CaseInstanceResponse> responseList = new ArrayList<>(caseInstances.size());
        for (CaseInstance instance : caseInstances) {
            responseList.add(createCaseInstanceResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public CaseInstanceResponse createCaseInstanceResponse(CaseInstance caseInstance) {
        return createCaseInstanceResponse(caseInstance, createUrlBuilder());
    }

    public CaseInstanceResponse createCaseInstanceResponse(CaseInstance caseInstance, RestUrlBuilder urlBuilder) {
        CaseInstanceResponse result = new CaseInstanceResponse();
        result.setBusinessKey(caseInstance.getBusinessKey());
        result.setBusinessStatus(caseInstance.getBusinessStatus());
        result.setId(caseInstance.getId());
        result.setName(caseInstance.getName());
        result.setStartTime(caseInstance.getStartTime());
        result.setStartUserId(caseInstance.getStartUserId());
        result.setState(caseInstance.getState());
        result.setCaseDefinitionId(caseInstance.getCaseDefinitionId());
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()));
        result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId()));
        result.setParentId(caseInstance.getParentId());
        result.setCallbackId(caseInstance.getCallbackId());
        result.setCallbackType(caseInstance.getCallbackType());
        result.setReferenceId(caseInstance.getReferenceId());
        result.setReferenceType(caseInstance.getReferenceType());
        result.setTenantId(caseInstance.getTenantId());

        for (String name : caseInstance.getCaseVariables().keySet()) {
            result.addVariable(createRestVariable(name, caseInstance.getCaseVariables().get(name), RestVariableScope.LOCAL, caseInstance.getId(), VARIABLE_CASE, false, urlBuilder));
        }

        return result;
    }

    public CaseInstanceResponse createCaseInstanceResponse(CaseInstance caseInstance, boolean returnVariables, Map<String, Object> runtimeVariableMap) {

        RestUrlBuilder urlBuilder = createUrlBuilder();
        CaseInstanceResponse result = new CaseInstanceResponse();
        result.setBusinessKey(caseInstance.getBusinessKey());
        result.setId(caseInstance.getId());
        result.setName(caseInstance.getName());
        result.setStartTime(caseInstance.getStartTime());
        result.setStartUserId(caseInstance.getStartUserId());
        result.setState(caseInstance.getState());
        result.setCaseDefinitionId(caseInstance.getCaseDefinitionId());
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()));
        result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId()));
        result.setParentId(caseInstance.getParentId());
        result.setCallbackId(caseInstance.getCallbackId());
        result.setCallbackType(caseInstance.getCallbackType());
        result.setReferenceId(caseInstance.getReferenceId());
        result.setReferenceType(caseInstance.getReferenceType());
        result.setTenantId(caseInstance.getTenantId());
        result.setCompleted(false);

        if (returnVariables) {

            if (runtimeVariableMap != null) {
                for (String name : runtimeVariableMap.keySet()) {
                    result.addVariable(createRestVariable(name, runtimeVariableMap.get(name), RestVariableScope.LOCAL, caseInstance.getId(), VARIABLE_CASE, false, urlBuilder));
                }
            }
        }
        // End Added by Ryan Johnston

        return result;
    }

    public List<PlanItemInstanceResponse> createPlanItemInstanceResponseList(List<PlanItemInstance> planItemInstances) {
        List<PlanItemInstanceResponse> responseList = new ArrayList<>(planItemInstances.size());
        for (PlanItemInstance planItemInstance : planItemInstances) {
            responseList.add(createPlanItemInstanceResponse(planItemInstance));
        }
        return responseList;
    }

    public PlanItemInstanceResponse createPlanItemInstanceResponse(PlanItemInstance planItemInstance) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        PlanItemInstanceResponse result = new PlanItemInstanceResponse();
        result.setId(planItemInstance.getId());
        result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE, planItemInstance.getId()));
        result.setName(planItemInstance.getName());
        result.setCaseDefinitionId(planItemInstance.getCaseDefinitionId());
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, planItemInstance.getCaseDefinitionId()));
        if (planItemInstance.getDerivedCaseDefinitionId() != null) {
            result.setDerivedCaseDefinitionId(planItemInstance.getDerivedCaseDefinitionId());
            result.setDerivedCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, planItemInstance.getDerivedCaseDefinitionId()));
        }
        result.setCaseInstanceId(planItemInstance.getCaseInstanceId());
        result.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, planItemInstance.getCaseInstanceId()));
        if (planItemInstance.getStageInstanceId() != null) {
            result.setStageInstanceId(planItemInstance.getStageInstanceId());
            result.setStageInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE, planItemInstance.getStageInstanceId()));
        }
        result.setPlanItemDefinitionId(planItemInstance.getPlanItemDefinitionId());
        result.setPlanItemDefinitionType(planItemInstance.getPlanItemDefinitionType());
        result.setState(planItemInstance.getState());
        result.setElementId(planItemInstance.getElementId());
        result.setReferenceId(planItemInstance.getReferenceId());
        result.setReferenceType(planItemInstance.getReferenceType());
        result.setCreateTime(planItemInstance.getCreateTime());
        result.setLastAvailableTime(planItemInstance.getLastAvailableTime());
        result.setLastEnabledTime(planItemInstance.getLastEnabledTime());
        result.setLastDisabledTime(planItemInstance.getLastDisabledTime());
        result.setLastStartedTime(planItemInstance.getLastStartedTime());
        result.setLastSuspendedTime(planItemInstance.getLastSuspendedTime());
        result.setCompletedTime(planItemInstance.getCompletedTime());
        result.setOccurredTime(planItemInstance.getOccurredTime());
        result.setTerminatedTime(planItemInstance.getTerminatedTime());
        result.setExitTime(planItemInstance.getExitTime());
        result.setEndedTime(planItemInstance.getEndedTime());
        result.setStartUserId(planItemInstance.getStartUserId());
        result.setAssignee(planItemInstance.getAssignee());
        result.setCompletedBy(planItemInstance.getCompletedBy());
        result.setStage(planItemInstance.isStage());
        result.setCompletable(planItemInstance.isCompletable());
        result.setEntryCriterionId(planItemInstance.getEntryCriterionId());
        result.setExitCriterionId(planItemInstance.getExitCriterionId());
        result.setFormKey(planItemInstance.getFormKey());
        result.setExtraValue(planItemInstance.getExtraValue());
        result.setTenantId(planItemInstance.getTenantId());

        Map<String, Object> variableMap = planItemInstance.getPlanItemInstanceLocalVariables();
        if (variableMap != null) {
            for (String name : variableMap.keySet()) {
                result.addLocalVariable((createRestVariable(name, variableMap.get(name), RestVariableScope.LOCAL,
                        planItemInstance.getId(), VARIABLE_PLAN_ITEM, false, urlBuilder)));
            }
        }

        return result;
    }
    
    public List<VariableInstanceResponse> createVariableInstanceResponseList(List<VariableInstance> variableInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<VariableInstanceResponse> responseList = new ArrayList<>(variableInstances.size());
        for (VariableInstance instance : variableInstances) {
            responseList.add(createVariableInstanceResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public VariableInstanceResponse createVariableInstanceResponse(VariableInstance variableInstance) {
        return createVariableInstanceResponse(variableInstance, createUrlBuilder());
    }

    public VariableInstanceResponse createVariableInstanceResponse(VariableInstance variableInstance, RestUrlBuilder urlBuilder) {
        VariableInstanceResponse result = new VariableInstanceResponse();
        result.setId(variableInstance.getId());
        if (variableInstance.getScopeId() != null && ScopeTypes.CMMN.equals(variableInstance.getScopeType())) {
            result.setCaseInstanceId(variableInstance.getScopeId());
            result.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, variableInstance.getScopeId()));
        }
        result.setTaskId(variableInstance.getTaskId());
        result.setPlanItemInstanceId(variableInstance.getSubScopeId());

        RestVariableScope scope;
        if (variableInstance.getSubScopeId() != null && !variableInstance.getSubScopeId().equals(variableInstance.getScopeId()) ||  variableInstance.getTaskId() != null) {
            scope = RestVariableScope.LOCAL;
        } else {
            scope = RestVariableScope.GLOBAL;
        }

        result.setVariable(
                createRestVariable(variableInstance.getName(), variableInstance.getValue(), scope, variableInstance.getId(), VARIABLE_VARINSTANCE, false,
                        urlBuilder));
        return result;
    }
    
    public List<EventSubscriptionResponse> createEventSubscriptionResponseList(List<EventSubscription> eventSubscriptions) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<EventSubscriptionResponse> responseList = new ArrayList<>(eventSubscriptions.size());
        for (EventSubscription instance : eventSubscriptions) {
            responseList.add(createEventSubscriptionResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public EventSubscriptionResponse createEventSubscriptionResponse(EventSubscription eventSubscription) {
        return createEventSubscriptionResponse(eventSubscription, createUrlBuilder());
    }

    public EventSubscriptionResponse createEventSubscriptionResponse(EventSubscription eventSubscription, RestUrlBuilder urlBuilder) {
        EventSubscriptionResponse response = new EventSubscriptionResponse();
        response.setId(eventSubscription.getId());
        response.setCreated(eventSubscription.getCreated());
        response.setEventType(eventSubscription.getEventType());
        response.setEventName(eventSubscription.getEventName());
        response.setActivityId(eventSubscription.getActivityId());
        response.setPlanItemInstanceId(eventSubscription.getSubScopeId());
        response.setCaseDefinitionId(eventSubscription.getScopeDefinitionId());
        response.setCaseInstanceId(eventSubscription.getScopeId());
        response.setExecutionId(eventSubscription.getExecutionId());
        response.setProcessInstanceId(eventSubscription.getProcessInstanceId());
        response.setProcessDefinitionId(eventSubscription.getProcessDefinitionId());
        response.setSubScopeId(eventSubscription.getSubScopeId());
        response.setScopeId(eventSubscription.getScopeId());
        response.setScopeType(eventSubscription.getScopeType());
        response.setScopeDefinitionId(eventSubscription.getScopeDefinitionId());
        response.setConfiguration(eventSubscription.getConfiguration());
        response.setTenantId(eventSubscription.getTenantId());

        response.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_EVENT_SUBSCRIPTION, eventSubscription.getId()));

        if (eventSubscription.getScopeDefinitionId() != null) {
            response.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, eventSubscription.getScopeDefinitionId()));
        }

        if (eventSubscription.getScopeId() != null) {
            response.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, eventSubscription.getScopeId()));
        }

        if (eventSubscription.getSubScopeId() != null) {
            response.setPlanItemInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE, eventSubscription.getSubScopeId()));
        }

        return response;
    }

    public List<HistoricCaseInstanceResponse> createHistoricCaseInstanceResponseList(List<HistoricCaseInstance> caseInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricCaseInstanceResponse> responseList = new ArrayList<>(caseInstances.size());
        for (HistoricCaseInstance instance : caseInstances) {
            responseList.add(createHistoricCaseInstanceResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public HistoricCaseInstanceResponse createHistoricCaseInstanceResponse(HistoricCaseInstance caseInstance) {
        return createHistoricCaseInstanceResponse(caseInstance, createUrlBuilder());
    }

    public HistoricCaseInstanceResponse createHistoricCaseInstanceResponse(HistoricCaseInstance caseInstance, RestUrlBuilder urlBuilder) {
        HistoricCaseInstanceResponse result = new HistoricCaseInstanceResponse();
        result.setBusinessKey(caseInstance.getBusinessKey());
        result.setBusinessStatus(caseInstance.getBusinessStatus());
        result.setName(caseInstance.getName());
        result.setEndTime(caseInstance.getEndTime());
        result.setId(caseInstance.getId());
        result.setCaseDefinitionId(caseInstance.getCaseDefinitionId());
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()));
        result.setStartTime(caseInstance.getStartTime());
        result.setStartUserId(caseInstance.getStartUserId());
        result.setEndUserId(caseInstance.getEndUserId());
        result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId()));
        Map<String, Object> variableMap = caseInstance.getCaseVariables();
        if (variableMap != null) {
            for (String name : variableMap.keySet()) {
                result.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.LOCAL, caseInstance.getId(), VARIABLE_HISTORY_CASE, false, urlBuilder));
            }
        }
        result.setTenantId(caseInstance.getTenantId());
        result.setState(caseInstance.getState());
        result.setReferenceId(caseInstance.getReferenceId());
        result.setReferenceType(caseInstance.getReferenceType());
        result.setCallbackId(caseInstance.getCallbackId());
        result.setCallbackType(caseInstance.getCallbackType());
        return result;
    }

    public List<HistoricTaskInstanceResponse> createHistoricTaskInstanceResponseList(List<HistoricTaskInstance> taskInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricTaskInstanceResponse> responseList = new ArrayList<>(taskInstances.size());
        for (HistoricTaskInstance instance : taskInstances) {
            responseList.add(createHistoricTaskInstanceResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public HistoricTaskInstanceResponse createHistoricTaskInstanceResponse(HistoricTaskInstance taskInstance) {
        return createHistoricTaskInstanceResponse(taskInstance, createUrlBuilder());
    }

    public HistoricTaskInstanceResponse createHistoricTaskInstanceResponse(HistoricTaskInstance taskInstance, RestUrlBuilder urlBuilder) {
        HistoricTaskInstanceResponse result = new HistoricTaskInstanceResponse();
        result.setAssignee(taskInstance.getAssignee());
        result.setClaimTime(taskInstance.getClaimTime());
        result.setDeleteReason(taskInstance.getDeleteReason());
        result.setDescription(taskInstance.getDescription());
        result.setDueDate(taskInstance.getDueDate());
        result.setDurationInMillis(taskInstance.getDurationInMillis());
        result.setEndTime(taskInstance.getEndTime());
        result.setFormKey(taskInstance.getFormKey());
        result.setId(taskInstance.getId());
        result.setName(taskInstance.getName());
        result.setOwner(taskInstance.getOwner());
        result.setParentTaskId(taskInstance.getParentTaskId());
        result.setPriority(taskInstance.getPriority());
        result.setScopeDefinitionId(taskInstance.getScopeDefinitionId());
        result.setScopeId(taskInstance.getScopeId());
        result.setSubScopeId(taskInstance.getSubScopeId());
        result.setScopeType(taskInstance.getScopeType());
        result.setPropagatedStageInstanceId(taskInstance.getPropagatedStageInstanceId());
        result.setExecutionId(taskInstance.getExecutionId());
        result.setProcessInstanceId(taskInstance.getProcessInstanceId());
        result.setProcessDefinitionId(taskInstance.getProcessDefinitionId());
        result.setTenantId(taskInstance.getTenantId());
        result.setCategory(taskInstance.getCategory());
        if (taskInstance.getScopeDefinitionId() != null && ScopeTypes.CMMN.equals(taskInstance.getScopeType())) {
            result.setCaseDefinitionId(taskInstance.getScopeDefinitionId());
            result.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, taskInstance.getScopeDefinitionId()));
        }

        if (taskInstance.getScopeId() != null && ScopeTypes.CMMN.equals(taskInstance.getScopeType())) {
            result.setCaseInstanceId(taskInstance.getScopeId());
            result.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, taskInstance.getScopeId()));
        }
        result.setStartTime(taskInstance.getStartTime());
        result.setTaskDefinitionKey(taskInstance.getTaskDefinitionKey());
        result.setWorkTimeInMillis(taskInstance.getWorkTimeInMillis());
        result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE, taskInstance.getId()));
        Map<String, Object> variableMap = taskInstance.getProcessVariables();
        if (variableMap != null) {
            for (String name : variableMap.keySet()) {
                result.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.GLOBAL, taskInstance.getId(), VARIABLE_HISTORY_TASK, false, urlBuilder));
            }
        }
        variableMap = taskInstance.getTaskLocalVariables();
        if (variableMap != null) {
            for (String name : variableMap.keySet()) {
                result.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.LOCAL, taskInstance.getId(), VARIABLE_HISTORY_TASK, false,
                        urlBuilder));
            }
        }
        return result;
    }

    public List<HistoricVariableInstanceResponse> createHistoricVariableInstanceResponseList(List<HistoricVariableInstance> variableInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricVariableInstanceResponse> responseList = new ArrayList<>(variableInstances.size());
        for (HistoricVariableInstance instance : variableInstances) {
            responseList.add(createHistoricVariableInstanceResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public HistoricVariableInstanceResponse createHistoricVariableInstanceResponse(HistoricVariableInstance variableInstance) {
        return createHistoricVariableInstanceResponse(variableInstance, createUrlBuilder());
    }

    public HistoricVariableInstanceResponse createHistoricVariableInstanceResponse(HistoricVariableInstance variableInstance, RestUrlBuilder urlBuilder) {
        HistoricVariableInstanceResponse result = new HistoricVariableInstanceResponse();
        result.setId(variableInstance.getId());
        if (variableInstance.getScopeId() != null && ScopeTypes.CMMN.equals(variableInstance.getScopeType())) {
            result.setCaseInstanceId(variableInstance.getScopeId());
            result.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, variableInstance.getScopeId()));
        }
        result.setTaskId(variableInstance.getTaskId());
        result.setPlanItemInstanceId(variableInstance.getSubScopeId());

        RestVariableScope scope;
        if (variableInstance.getSubScopeId() != null && !variableInstance.getSubScopeId().equals(variableInstance.getScopeId())
                || variableInstance.getTaskId() != null) {
            scope = RestVariableScope.LOCAL;
        } else {
            scope = RestVariableScope.GLOBAL;
        }

        result.setVariable(createRestVariable(variableInstance.getVariableName(), variableInstance.getValue(), scope, variableInstance.getId(),
                VARIABLE_HISTORY_VARINSTANCE, false, urlBuilder));
        return result;
    }

    public List<HistoricIdentityLinkResponse> createHistoricIdentityLinkResponseList(List<HistoricIdentityLink> identityLinks) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricIdentityLinkResponse> responseList = new ArrayList<>(identityLinks.size());
        for (HistoricIdentityLink instance : identityLinks) {
            responseList.add(createHistoricIdentityLinkResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public HistoricIdentityLinkResponse createHistoricIdentityLinkResponse(HistoricIdentityLink identityLink) {
        return createHistoricIdentityLinkResponse(identityLink, createUrlBuilder());
    }

    public HistoricIdentityLinkResponse createHistoricIdentityLinkResponse(HistoricIdentityLink identityLink, RestUrlBuilder urlBuilder) {
        HistoricIdentityLinkResponse result = new HistoricIdentityLinkResponse();
        result.setType(identityLink.getType());
        result.setUserId(identityLink.getUserId());
        result.setGroupId(identityLink.getGroupId());
        result.setTaskId(identityLink.getTaskId());
        if (StringUtils.isNotEmpty(identityLink.getTaskId())) {
            result.setTaskUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE, identityLink.getTaskId()));
        }

        if (StringUtils.isNotEmpty(identityLink.getScopeId()) && ScopeTypes.CMMN.equals(identityLink.getScopeType())) {
            result.setCaseInstanceId(identityLink.getScopeId());
            result.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, identityLink.getScopeId()));
        }
        return result;
    }

    public List<HistoricMilestoneInstanceResponse> createHistoricMilestoneInstanceResponseList(List<HistoricMilestoneInstance> historicMilestoneInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        return historicMilestoneInstances
                .stream()
                .map(m -> createHistoricMilestoneInstanceResponse(m, urlBuilder))
                .collect(Collectors.toList());
    }

    public HistoricMilestoneInstanceResponse createHistoricMilestoneInstanceResponse(HistoricMilestoneInstance historicMilestoneInstance) {
        return createHistoricMilestoneInstanceResponse(historicMilestoneInstance, createUrlBuilder());
    }

    public HistoricMilestoneInstanceResponse createHistoricMilestoneInstanceResponse(HistoricMilestoneInstance historicMilestoneInstance, RestUrlBuilder urlBuilder) {
        HistoricMilestoneInstanceResponse result = new HistoricMilestoneInstanceResponse();
        result.setId(historicMilestoneInstance.getId());
        result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCE, historicMilestoneInstance.getId()));
        result.setName(historicMilestoneInstance.getName());
        result.setElementId(historicMilestoneInstance.getElementId());
        result.setTimestamp(historicMilestoneInstance.getTimeStamp());
        result.setCaseInstanceId(historicMilestoneInstance.getCaseInstanceId());
        result.setHistoricCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, historicMilestoneInstance.getCaseInstanceId()));
        result.setCaseDefinitionId(historicMilestoneInstance.getCaseDefinitionId());
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, historicMilestoneInstance.getCaseDefinitionId()));
        return result;
    }

    public List<HistoricPlanItemInstanceResponse> createHistoricPlanItemInstanceResponseList(List<HistoricPlanItemInstance> historicPlanItemInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        return historicPlanItemInstances
                .stream()
                .map(m -> createHistoricPlanItemInstanceResponse(m, urlBuilder))
                .collect(Collectors.toList());
    }

    public HistoricPlanItemInstanceResponse createHistoricPlanItemInstanceResponse(HistoricPlanItemInstance historicPlanItemInstance) {
        return createHistoricPlanItemInstanceResponse(historicPlanItemInstance, createUrlBuilder());
    }

    public HistoricPlanItemInstanceResponse createHistoricPlanItemInstanceResponse(HistoricPlanItemInstance historicPlanItemInstance, RestUrlBuilder urlBuilder) {
        HistoricPlanItemInstanceResponse result = new HistoricPlanItemInstanceResponse();
        result.setId(historicPlanItemInstance.getId());
        result.setName(historicPlanItemInstance.getName());
        result.setState(historicPlanItemInstance.getState());
        result.setCaseDefinitionId(historicPlanItemInstance.getCaseDefinitionId());
        result.setDerivedCaseDefinitionId(historicPlanItemInstance.getDerivedCaseDefinitionId());
        result.setCaseInstanceId(historicPlanItemInstance.getCaseInstanceId());
        result.setStageInstanceId(historicPlanItemInstance.getStageInstanceId());
        result.setStage(historicPlanItemInstance.isStage());
        result.setElementId(historicPlanItemInstance.getElementId());
        result.setPlanItemDefinitionId(historicPlanItemInstance.getPlanItemDefinitionId());
        result.setPlanItemDefinitionType(historicPlanItemInstance.getPlanItemDefinitionType());
        result.setCreateTime(historicPlanItemInstance.getCreateTime());
        result.setLastAvailableTime(historicPlanItemInstance.getLastAvailableTime());
        result.setLastEnabledTime(historicPlanItemInstance.getLastEnabledTime());
        result.setLastDisabledTime(historicPlanItemInstance.getLastDisabledTime());
        result.setLastStartedTime(historicPlanItemInstance.getLastStartedTime());
        result.setLastSuspendedTime(historicPlanItemInstance.getLastSuspendedTime());
        result.setCompletedTime(historicPlanItemInstance.getCompletedTime());
        result.setOccurredTime(historicPlanItemInstance.getOccurredTime());
        result.setTerminatedTime(historicPlanItemInstance.getTerminatedTime());
        result.setExitTime(historicPlanItemInstance.getExitTime());
        result.setEndedTime(historicPlanItemInstance.getEndedTime());
        result.setLastUpdatedTime(historicPlanItemInstance.getLastUpdatedTime());
        result.setStartUserId(historicPlanItemInstance.getStartUserId());
        result.setAssignee(historicPlanItemInstance.getAssignee());
        result.setCompletedBy(historicPlanItemInstance.getCompletedBy());
        result.setReferenceId(historicPlanItemInstance.getReferenceId());
        result.setReferenceType(historicPlanItemInstance.getReferenceType());
        result.setEntryCriterionId(historicPlanItemInstance.getEntryCriterionId());
        result.setExitCriterionId(historicPlanItemInstance.getExitCriterionId());
        result.setFormKey(historicPlanItemInstance.getFormKey());
        result.setExtraValue(historicPlanItemInstance.getExtraValue());
        result.setShowInOverview(historicPlanItemInstance.isShowInOverview());
        result.setTenantId(historicPlanItemInstance.getTenantId());
        result.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, historicPlanItemInstance.getId()));
        result.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, historicPlanItemInstance.getCaseInstanceId()));
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, historicPlanItemInstance.getCaseDefinitionId()));
        if (historicPlanItemInstance.getDerivedCaseDefinitionId() != null) {
            result.setDerivedCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, historicPlanItemInstance.getDerivedCaseDefinitionId()));
        }
        if (historicPlanItemInstance.getStageInstanceId() != null) {
            result.setStageInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, historicPlanItemInstance.getStageInstanceId()));
        }

        Map<String, Object> variableMap = historicPlanItemInstance.getPlanItemInstanceLocalVariables();
        if (variableMap != null) {
            for (String name : variableMap.keySet()) {
                result.addLocalVariable((createRestVariable(name, variableMap.get(name), RestVariableScope.LOCAL,
                        historicPlanItemInstance.getId(), VARIABLE_PLAN_ITEM, false, urlBuilder)));
            }
        }

        return result;
    }

    public List<DecisionResponse> createDecisionResponseList(List<DmnDecision> decisions, String processDefinitionId) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<DecisionResponse> responseList = new ArrayList<>(decisions.size());
        for (DmnDecision decision : decisions) {
            responseList.add(createDecisionResponse(decision, processDefinitionId, urlBuilder));
        }
        return responseList;
    }

    public DecisionResponse createDecisionResponse(DmnDecision decision, String processDefinitionId) {
        return createDecisionResponse(decision, processDefinitionId, createUrlBuilder());
    }

    public DecisionResponse createDecisionResponse(DmnDecision decision, String caseDefinitionId, RestUrlBuilder urlBuilder) {
        DecisionResponse decisionResponse = new DecisionResponse(decision);
        decisionResponse.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION_DECISION_COLLECTION, caseDefinitionId));

        return decisionResponse;
    }

    public List<FormDefinitionResponse> createFormDefinitionResponseList(List<FormDefinition> formDefinitions, String processDefinitionId) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<FormDefinitionResponse> responseList = new ArrayList<>(formDefinitions.size());
        for (FormDefinition formDefinition : formDefinitions) {
            responseList.add(createFormDefinitionResponse(formDefinition, processDefinitionId, urlBuilder));
        }
        return responseList;
    }

    public FormDefinitionResponse createFormDefintionResponse(FormDefinition formDefinition, String processDefinitionId) {
        return createFormDefinitionResponse(formDefinition, processDefinitionId, createUrlBuilder());
    }

    public FormDefinitionResponse createFormDefinitionResponse(FormDefinition formDefinition, String caseDefinitionId, RestUrlBuilder urlBuilder) {
        FormDefinitionResponse formDefinitionResponse = new FormDefinitionResponse(formDefinition);
        formDefinitionResponse.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION_FORM_DEFINITIONS_COLLECTION, caseDefinitionId));

        return formDefinitionResponse;
    }
    
    public List<JobResponse> createJobResponseList(List<Job> jobs) {
        return createJobResponseList(jobs, CmmnRestUrls.URL_JOB);
    }

    public List<JobResponse> createTimerJobResponseList(List<Job> jobs) {
        return createJobResponseList(jobs, CmmnRestUrls.URL_TIMER_JOB);
    }

    public List<JobResponse> createSuspendedJobResponseList(List<Job> jobs) {
        return createJobResponseList(jobs, CmmnRestUrls.URL_SUSPENDED_JOB);
    }

    public List<JobResponse> createDeadLetterJobResponseList(List<Job> jobs) {
        return createJobResponseList(jobs, CmmnRestUrls.URL_DEADLETTER_JOB);
    }

    public List<JobResponse> createJobResponseList(List<Job> jobs, String[] urlJobSegments) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<JobResponse> responseList = new ArrayList<>(jobs.size());
        for (Job instance : jobs) {
            responseList.add(createJobResponse(instance, urlBuilder, urlJobSegments));
        }
        return responseList;
    }
    
    public JobResponse createJobResponse(Job job) {
        return createJobResponse(job, createUrlBuilder(), CmmnRestUrls.URL_JOB);
    }
    
    public JobResponse createTimerJobResponse(Job job) {
        return createJobResponse(job, createUrlBuilder(), CmmnRestUrls.URL_TIMER_JOB);
    }

    public JobResponse createSuspendedJobResponse(Job job) {
        return createJobResponse(job, createUrlBuilder(), CmmnRestUrls.URL_SUSPENDED_JOB);
    }

    public JobResponse createDeadLetterJobResponse(Job job) {
        return createJobResponse(job, createUrlBuilder(), CmmnRestUrls.URL_DEADLETTER_JOB);
    }

    public JobResponse createJobResponse(Job job, RestUrlBuilder urlBuilder, String[] urlJobSegments) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setDueDate(job.getDuedate());
        response.setExceptionMessage(job.getExceptionMessage());
        response.setRetries(job.getRetries());
        response.setCreateTime(job.getCreateTime());
        if (job instanceof JobInfoEntity) {
            JobInfoEntity jobInfoEntity = (JobInfoEntity) job;
            response.setLockOwner(jobInfoEntity.getLockOwner());
            response.setLockExpirationTime(jobInfoEntity.getLockExpirationTime());
        }
        response.setTenantId(job.getTenantId());
        response.setElementId(job.getElementId());
        response.setElementName(job.getElementName());
        response.setHandlerType(job.getJobHandlerType());

        response.setUrl(urlBuilder.buildUrl(urlJobSegments, job.getId()));

        if (ScopeTypes.CMMN.equals(job.getScopeType())) {
            if (job.getScopeDefinitionId() != null) {
                response.setCaseDefinitionId(job.getScopeDefinitionId());
                response.setCaseDefinitionUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, job.getScopeDefinitionId()));
            }

            if (job.getScopeId() != null) {
                response.setCaseInstanceId(job.getScopeId());
                response.setCaseInstanceUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, job.getScopeId()));
            }

            if (job.getSubScopeId() != null) {
                response.setPlanItemInstanceId(job.getSubScopeId());
            }
        }

        return response;
    }

    public List<HistoryJobResponse> createHistoryJobResponseList(List<HistoryJob> jobs) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoryJobResponse> responseList = new ArrayList<>(jobs.size());
        for (HistoryJob job : jobs) {
            responseList.add(createHistoryJobResponse(job, urlBuilder));
        }
        return responseList;
    }

    public HistoryJobResponse createHistoryJobResponse(HistoryJob job) {
        return createHistoryJobResponse(job, createUrlBuilder());
    }

    public HistoryJobResponse createHistoryJobResponse(HistoryJob job, RestUrlBuilder urlBuilder) {
        HistoryJobResponse response = new HistoryJobResponse();
        response.setId(job.getId());
        response.setExceptionMessage(job.getExceptionMessage());
        response.setRetries(job.getRetries());
        response.setCreateTime(job.getCreateTime());
        response.setScopeType(job.getScopeType());
        response.setJobHandlerType(job.getJobHandlerType());
        response.setJobHandlerConfiguration(job.getJobHandlerConfiguration());
        response.setCustomValues(job.getCustomValues());
        if (job instanceof HistoryJobEntity) {
            HistoryJobEntity historyJobEntity = (HistoryJobEntity) job;
            response.setLockOwner(historyJobEntity.getLockOwner());
            response.setLockExpirationTime(historyJobEntity.getLockExpirationTime());
        }
        response.setTenantId(job.getTenantId());

        response.setUrl(urlBuilder.buildUrl(CmmnRestUrls.URL_HISTORY_JOB, job.getId()));

        return response;
    }

    /**
     * @return list of {@link RestVariableConverter} which are used by this factory. Additional converters can be added and existing ones replaced ore removed.
     */
    public List<RestVariableConverter> getVariableConverters() {
        return variableConverters;
    }

    /**
     * Called once when the converters need to be initialized. Override of custom conversion needs to be done between java and rest.
     */
    protected void initializeVariableConverters() {
        variableConverters.add(new StringRestVariableConverter());
        variableConverters.add(new IntegerRestVariableConverter());
        variableConverters.add(new LongRestVariableConverter());
        variableConverters.add(new ShortRestVariableConverter());
        variableConverters.add(new DoubleRestVariableConverter());
        variableConverters.add(new BigDecimalRestVariableConverter());
        variableConverters.add(new BigIntegerRestVariableConverter());
        variableConverters.add(new BooleanRestVariableConverter());
        variableConverters.add(new DateRestVariableConverter());
        variableConverters.add(new InstantRestVariableConverter());
        variableConverters.add(new LocalDateRestVariableConverter());
        variableConverters.add(new LocalDateTimeRestVariableConverter());
        variableConverters.add(new UUIDRestVariableConverter());
        variableConverters.add(new JsonObjectRestVariableConverter(objectMapper));
    }

    protected String formatUrl(String serverRootUrl, String[] fragments, Object... arguments) {
        StringBuilder urlBuilder = new StringBuilder(serverRootUrl);
        for (String urlFragment : fragments) {
            urlBuilder.append("/");
            urlBuilder.append(MessageFormat.format(urlFragment, arguments));
        }
        return urlBuilder.toString();
    }

    protected RestUrlBuilder createUrlBuilder() {
        return RestUrlBuilder.fromCurrentRequest();
    }

}
