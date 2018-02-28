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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.deployer.ResourceNameUtil;
import org.flowable.cmmn.rest.service.api.engine.RestIdentityLink;
import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.flowable.cmmn.rest.service.api.history.HistoricCaseInstanceResponse;
import org.flowable.cmmn.rest.service.api.history.HistoricIdentityLinkResponse;
import org.flowable.cmmn.rest.service.api.history.HistoricTaskInstanceResponse;
import org.flowable.cmmn.rest.service.api.history.HistoricVariableInstanceResponse;
import org.flowable.cmmn.rest.service.api.repository.CaseDefinitionResponse;
import org.flowable.cmmn.rest.service.api.repository.CmmnDeploymentResponse;
import org.flowable.cmmn.rest.service.api.repository.DecisionTableResponse;
import org.flowable.cmmn.rest.service.api.repository.DeploymentResourceResponse;
import org.flowable.cmmn.rest.service.api.repository.FormDefinitionResponse;
import org.flowable.cmmn.rest.service.api.runtime.caze.CaseInstanceResponse;
import org.flowable.cmmn.rest.service.api.runtime.task.TaskResponse;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.form.api.FormDefinition;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.rest.application.ContentTypeResolver;
import org.flowable.rest.variable.BooleanRestVariableConverter;
import org.flowable.rest.variable.DateRestVariableConverter;
import org.flowable.rest.variable.DoubleRestVariableConverter;
import org.flowable.rest.variable.IntegerRestVariableConverter;
import org.flowable.rest.variable.LongRestVariableConverter;
import org.flowable.rest.variable.RestVariableConverter;
import org.flowable.rest.variable.ShortRestVariableConverter;
import org.flowable.rest.variable.StringRestVariableConverter;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.type.VariableScopeType;

/**
 * Default implementation of a {@link RestResponseFactory}.
 * 
 * Added a new "createProcessInstanceResponse" method (with a different signature) to conditionally return the process variables that exist within the process instance when the first wait state is
 * encountered (or when the process instance completes). Also added the population of a "completed" flag - within both the original "createProcessInstanceResponse" method and the new one with the
 * different signature - to let the caller know whether the process instance has completed or not.
 * 
 * @author Frederik Heremans
 * @author Ryan Johnston (@rjfsu)
 */
public class RestResponseFactory {

    public static final int VARIABLE_TASK = 1;
    public static final int VARIABLE_EXECUTION = 2;
    public static final int VARIABLE_CASE = 3;
    public static final int VARIABLE_HISTORY_TASK = 4;
    public static final int VARIABLE_HISTORY_CASE = 5;
    public static final int VARIABLE_HISTORY_VARINSTANCE = 6;
    public static final int VARIABLE_HISTORY_DETAIL = 7;

    public static final String BYTE_ARRAY_VARIABLE_TYPE = "binary";
    public static final String SERIALIZABLE_VARIABLE_TYPE = "serializable";

    protected List<RestVariableConverter> variableConverters = new ArrayList<>();

    public RestResponseFactory() {
        initializeVariableConverters();
    }

    public List<TaskResponse> createTaskResponseList(List<Task> tasks) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<TaskResponse> responseList = new ArrayList<>();
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
        response.setUrl(urlBuilder.buildUrl(RestUrls.URL_TASK, task.getId()));

        // Add references to other resources, if needed
        if (response.getParentTaskId() != null) {
            response.setParentTaskUrl(urlBuilder.buildUrl(RestUrls.URL_TASK, response.getParentTaskId()));
        }
        if (response.getCaseDefinitionId() != null) {
            response.setCaseDefinitionUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION, response.getCaseDefinitionId()));
        }
        if (response.getCaseInstanceId() != null) {
            response.setCaseInstanceUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_INSTANCE, response.getCaseInstanceId()));
        }

        if (task.getProcessVariables() != null) {
            Map<String, Object> variableMap = task.getProcessVariables();
            for (String name : variableMap.keySet()) {
                response.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.GLOBAL, task.getId(), VARIABLE_TASK, false, urlBuilder));
            }
        }
        if (task.getTaskLocalVariables() != null) {
            Map<String, Object> variableMap = task.getTaskLocalVariables();
            for (String name : variableMap.keySet()) {
                response.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.LOCAL, task.getId(), VARIABLE_TASK, false, urlBuilder));
            }
        }

        return response;
    }

    public List<CmmnDeploymentResponse> createDeploymentResponseList(List<CmmnDeployment> deployments) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<CmmnDeploymentResponse> responseList = new ArrayList<>();
        for (CmmnDeployment instance : deployments) {
            responseList.add(createDeploymentResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public CmmnDeploymentResponse createDeploymentResponse(CmmnDeployment deployment) {
        return createDeploymentResponse(deployment, createUrlBuilder());
    }

    public CmmnDeploymentResponse createDeploymentResponse(CmmnDeployment deployment, RestUrlBuilder urlBuilder) {
        return new CmmnDeploymentResponse(deployment, urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT, deployment.getId()));
    }

    public List<DeploymentResourceResponse> createDeploymentResourceResponseList(String deploymentId, List<String> resourceList, ContentTypeResolver contentTypeResolver) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        // Add additional metadata to the artifact-strings before returning
        List<DeploymentResourceResponse> responseList = new ArrayList<>();
        for (String resourceId : resourceList) {
            String contentType = null;
            if (resourceId.toLowerCase().endsWith(".cmmn")) {
                contentType = ContentType.TEXT_XML.getMimeType();
            } else {
                contentType = contentTypeResolver.resolveContentType(resourceId);
            }
            responseList.add(createDeploymentResourceResponse(deploymentId, resourceId, contentType, urlBuilder));
        }
        return responseList;
    }

    public DeploymentResourceResponse createDeploymentResourceResponse(String deploymentId, String resourceId, String contentType) {
        return createDeploymentResourceResponse(deploymentId, resourceId, contentType, createUrlBuilder());
    }

    public DeploymentResourceResponse createDeploymentResourceResponse(String deploymentId, String resourceId, String contentType, RestUrlBuilder urlBuilder) {
        // Create URL's
        String resourceUrl = urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, deploymentId, resourceId);
        String resourceContentUrl = urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deploymentId, resourceId);

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
        List<CaseDefinitionResponse> responseList = new ArrayList<>();
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
        response.setUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION, caseDefinition.getId()));
        response.setId(caseDefinition.getId());
        response.setKey(caseDefinition.getKey());
        response.setVersion(caseDefinition.getVersion());
        response.setCategory(caseDefinition.getCategory());
        response.setName(caseDefinition.getName());
        response.setDescription(caseDefinition.getDescription());
        response.setGraphicalNotationDefined(caseDefinition.hasGraphicalNotation());
        response.setTenantId(caseDefinition.getTenantId());

        // Links to other resources
        response.setDeploymentId(caseDefinition.getDeploymentId());
        response.setDeploymentUrl(urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT, caseDefinition.getDeploymentId()));
        response.setResource(urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getResourceName()));
        if (caseDefinition.getDiagramResourceName() != null) {
            response.setDiagramResource(urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getDiagramResourceName()));
        }
        return response;
    }
    
    public List<RestVariable> createRestVariables(Map<String, Object> variables, String id, int variableType) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<RestVariable> result = new ArrayList<>();

        for (Entry<String, Object> pair : variables.entrySet()) {
            result.add(createRestVariable(pair.getKey(), pair.getValue(), null, id, variableType, false, urlBuilder));
        }

        return result;
    }

    public List<RestVariable> createRestVariables(Map<String, Object> variables, String id, int variableType, RestVariableScope scope) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<RestVariable> result = new ArrayList<>();

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
                    restVar.setValueUrl(urlBuilder.buildUrl(RestUrls.URL_TASK_VARIABLE_DATA, id, name));
                } else if (variableType == VARIABLE_CASE) {
                    restVar.setValueUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, id, name));
                } else if (variableType == VARIABLE_HISTORY_TASK) {
                    restVar.setValueUrl(urlBuilder.buildUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, id, name));
                }
            }
        }
        return restVar;
    }

    public RestVariable createBinaryRestVariable(String name, RestVariableScope scope, String type, String taskId, String caseInstanceId) {

        RestUrlBuilder urlBuilder = createUrlBuilder();
        RestVariable restVar = new RestVariable();
        restVar.setVariableScope(scope);
        restVar.setName(name);
        restVar.setType(type);

        if (taskId != null) {
            restVar.setValueUrl(urlBuilder.buildUrl(RestUrls.URL_TASK_VARIABLE_DATA, taskId, name));
        }
        if (caseInstanceId != null) {
            restVar.setValueUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstanceId, name));
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
        List<RestIdentityLink> responseList = new ArrayList<>();
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
            family = RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS;
        } else {
            family = RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS;
        }
        
        if (caseInstanceId != null) {
            result.setUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstanceId, (userId != null ? userId : groupId), type));
        } else if (taskId != null) {
            result.setUrl(urlBuilder.buildUrl(RestUrls.URL_TASK_IDENTITYLINK, taskId, family, (userId != null ? userId : groupId), type));
        } else if (caseDefinitionId != null) {
            result.setUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinitionId, family, (userId != null ? userId : groupId)));
        }
        return result;
    }

    public List<CaseInstanceResponse> createCaseInstanceResponseList(List<CaseInstance> caseInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<CaseInstanceResponse> responseList = new ArrayList<>();
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
        result.setId(caseInstance.getId());
        result.setName(caseInstance.getName());
        result.setCaseDefinitionId(caseInstance.getCaseDefinitionId());
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()));
        result.setUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_INSTANCE, caseInstance.getId()));
        result.setTenantId(caseInstance.getTenantId());

        return result;
    }

    public CaseInstanceResponse createCaseInstanceResponse(CaseInstance caseInstance, boolean returnVariables, Map<String, Object> runtimeVariableMap) {

        RestUrlBuilder urlBuilder = createUrlBuilder();
        CaseInstanceResponse result = new CaseInstanceResponse();
        result.setBusinessKey(caseInstance.getBusinessKey());
        result.setId(caseInstance.getId());
        result.setCaseDefinitionId(caseInstance.getCaseDefinitionId());
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()));
        result.setUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_INSTANCE, caseInstance.getId()));
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

    public List<HistoricCaseInstanceResponse> createHistoricCaseInstanceResponseList(List<HistoricCaseInstance> caseInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricCaseInstanceResponse> responseList = new ArrayList<>();
        for (HistoricCaseInstance instance : caseInstances) {
            responseList.add(createHistoricCaseInstanceResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public HistoricCaseInstanceResponse createHistoricCaseInstanceResponse(HistoricCaseInstance caseInstance) {
        return createHistoricCaseInstanceResponse(caseInstance, createUrlBuilder());
    }

    @SuppressWarnings("deprecation")
    public HistoricCaseInstanceResponse createHistoricCaseInstanceResponse(HistoricCaseInstance caseInstance, RestUrlBuilder urlBuilder) {
        HistoricCaseInstanceResponse result = new HistoricCaseInstanceResponse();
        result.setBusinessKey(caseInstance.getBusinessKey());
        result.setEndTime(caseInstance.getEndTime());
        result.setId(caseInstance.getId());
        result.setCaseDefinitionId(caseInstance.getCaseDefinitionId());
        result.setCaseDefinitionUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()));
        result.setStartTime(caseInstance.getStartTime());
        result.setStartUserId(caseInstance.getStartUserId());
        result.setUrl(urlBuilder.buildUrl(RestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId()));
        result.setTenantId(caseInstance.getTenantId());
        return result;
    }

    public List<HistoricTaskInstanceResponse> createHistoricTaskInstanceResponseList(List<HistoricTaskInstance> taskInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricTaskInstanceResponse> responseList = new ArrayList<>();
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
        result.setTenantId(taskInstance.getTenantId());
        result.setCategory(taskInstance.getCategory());
        if (taskInstance.getScopeDefinitionId() != null && VariableScopeType.CMMN.equals(taskInstance.getScopeType())) {
            result.setCaseDefinitionId(taskInstance.getScopeDefinitionId());
            result.setCaseDefinitionUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION, taskInstance.getScopeDefinitionId()));
        }
        
        if (taskInstance.getScopeId() != null && VariableScopeType.CMMN.equals(taskInstance.getScopeType())) {
            result.setCaseInstanceId(taskInstance.getScopeId());
            result.setCaseInstanceUrl(urlBuilder.buildUrl(RestUrls.URL_HISTORIC_CASE_INSTANCE, taskInstance.getScopeId()));
        }
        result.setStartTime(taskInstance.getStartTime());
        result.setTaskDefinitionKey(taskInstance.getTaskDefinitionKey());
        result.setWorkTimeInMillis(taskInstance.getWorkTimeInMillis());
        result.setUrl(urlBuilder.buildUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, taskInstance.getId()));
        if (taskInstance.getProcessVariables() != null) {
            Map<String, Object> variableMap = taskInstance.getProcessVariables();
            for (String name : variableMap.keySet()) {
                result.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.GLOBAL, taskInstance.getId(), VARIABLE_HISTORY_TASK, false, urlBuilder));
            }
        }
        if (taskInstance.getTaskLocalVariables() != null) {
            Map<String, Object> variableMap = taskInstance.getTaskLocalVariables();
            for (String name : variableMap.keySet()) {
                result.addVariable(createRestVariable(name, variableMap.get(name), RestVariableScope.LOCAL, taskInstance.getId(), VARIABLE_HISTORY_TASK, false, urlBuilder));
            }
        }
        return result;
    }

    public List<HistoricVariableInstanceResponse> createHistoricVariableInstanceResponseList(List<HistoricVariableInstance> variableInstances) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricVariableInstanceResponse> responseList = new ArrayList<>();
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
        if (variableInstance.getScopeId() != null && VariableScopeType.CMMN.equals(variableInstance.getScopeType())) {
            result.setCaseInstanceId(variableInstance.getScopeId());
            result.setCaseInstanceUrl(urlBuilder.buildUrl(RestUrls.URL_HISTORIC_CASE_INSTANCE, variableInstance.getScopeId()));
        }
        result.setTaskId(variableInstance.getTaskId());
        result.setVariable(createRestVariable(variableInstance.getVariableName(), variableInstance.getValue(), null, variableInstance.getId(), VARIABLE_HISTORY_VARINSTANCE, false, urlBuilder));
        return result;
    }

    public List<HistoricIdentityLinkResponse> createHistoricIdentityLinkResponseList(List<HistoricIdentityLink> identityLinks) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<HistoricIdentityLinkResponse> responseList = new ArrayList<>();
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
            result.setTaskUrl(urlBuilder.buildUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, identityLink.getTaskId()));
        }
        
        if (StringUtils.isNotEmpty(identityLink.getScopeId()) && VariableScopeType.CMMN.equals(identityLink.getScopeType())) {
            result.setCaseInstanceId(identityLink.getScopeId());
            result.setCaseInstanceUrl(urlBuilder.buildUrl(RestUrls.URL_HISTORIC_CASE_INSTANCE, identityLink.getScopeId()));
        }
        return result;
    }

    public List<DecisionTableResponse> createDecisionTableResponseList(List<DmnDecisionTable> decisionTables, String processDefinitionId) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<DecisionTableResponse> responseList = new ArrayList<>();
        for (DmnDecisionTable decisionTable : decisionTables) {
            responseList.add(createDecisionTableResponse(decisionTable, processDefinitionId, urlBuilder));
        }
        return responseList;
    }

    public DecisionTableResponse createDecisionTableResponse(DmnDecisionTable decisionTable, String processDefinitionId) {
        return createDecisionTableResponse(decisionTable, processDefinitionId, createUrlBuilder());
    }

    public DecisionTableResponse createDecisionTableResponse(DmnDecisionTable decisionTable, String caseDefinitionId, RestUrlBuilder urlBuilder) {
        DecisionTableResponse decisionTableResponse = new DecisionTableResponse(decisionTable);
        decisionTableResponse.setUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION_DECISION_TABLES_COLLECTION, caseDefinitionId));

        return decisionTableResponse;
    }

    public List<FormDefinitionResponse> createFormDefinitionResponseList(List<FormDefinition> formDefinitions, String processDefinitionId) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<FormDefinitionResponse> responseList = new ArrayList<>();
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
        formDefinitionResponse.setUrl(urlBuilder.buildUrl(RestUrls.URL_CASE_DEFINITION_FORM_DEFINITIONS_COLLECTION, caseDefinitionId));

        return formDefinitionResponse;
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
        variableConverters.add(new BooleanRestVariableConverter());
        variableConverters.add(new DateRestVariableConverter());
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
