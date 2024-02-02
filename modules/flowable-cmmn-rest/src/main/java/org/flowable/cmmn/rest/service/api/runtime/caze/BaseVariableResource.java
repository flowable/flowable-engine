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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.common.rest.exception.FlowableContentNotSupportedException;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class BaseVariableResource extends BaseCaseInstanceResource implements InitializingBean{

    @Autowired
    protected ObjectMapper objectMapper;

    
    @Autowired
    protected Environment env;
    
    protected boolean isSerializableVariableAllowed;

    @Override
    public void afterPropertiesSet() {
        isSerializableVariableAllowed = env.getProperty("rest.variables.allow.serializable", Boolean.class, true);
    }

    protected PlanItemInstance getPlanItemInstanceFromRequest(String planItemInstanceId) {
        PlanItemInstance planItemInstance = runtimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstanceId).singleResult();
        if (planItemInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a plan item instance with id '" + planItemInstanceId + "'.");
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessPlanItemInstanceInfoById(planItemInstance);
        }

        return planItemInstance;
    }

    public RestVariable getVariableFromRequest(CaseInstance caseInstance, String variableName, boolean includeBinary) {

        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a case instance", CaseInstance.class);
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessCaseInstanceVariable(caseInstance, variableName);
        }

        return getVariableFromRequestWithoutAccessCheck(caseInstance.getId(), variableName, CmmnRestResponseFactory.VARIABLE_CASE, includeBinary);
    }

    public RestVariable getVariableFromRequest(PlanItemInstance planItemInstance, String variableName, boolean includeBinary) {
        if (planItemInstance == null) {
            throw new FlowableObjectNotFoundException("Could not find a plan item instance", CaseInstance.class);
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessPlanItemInstanceVariable(planItemInstance, variableName);
        }
        return getVariableFromRequestWithoutAccessCheck(planItemInstance.getId(), variableName, CmmnRestResponseFactory.VARIABLE_PLAN_ITEM, includeBinary);
    }

    protected RestVariable getVariableFromRequestWithoutAccessCheck(String instanceId, String variableName, int variableType, boolean includeBinary) {
        Object value = null;

        if (variableType == CmmnRestResponseFactory.VARIABLE_PLAN_ITEM) {
            value = runtimeService.getLocalVariable(instanceId, variableName);
        } else if (variableType == CmmnRestResponseFactory.VARIABLE_CASE) {
            value = runtimeService.getVariable(instanceId, variableName);
        } else {
            throw new FlowableIllegalArgumentException("Unknown variable type " + variableType);
        }

        if (value == null) {
            if (variableType == CmmnRestResponseFactory.VARIABLE_PLAN_ITEM) {
                throw new FlowableObjectNotFoundException(
                        "Plan item instance '" + instanceId + "' doesn't have a variable with name: '" + variableName + "'.",
                        VariableInstance.class);
            } else {
                throw new FlowableObjectNotFoundException(
                        "Case instance '" + instanceId + "' doesn't have a variable with name: '" + variableName + "'.",
                        VariableInstance.class);
            }
        }

        //we use null for the scope, because the extraction from request does not require the scope
        return constructRestVariable(variableName, value, instanceId, variableType, includeBinary, null);
    }

    protected byte[] getVariableDataByteArray(CaseInstance caseInstance, String variableName, HttpServletResponse response) {
        RestVariable variable = getVariableFromRequest(caseInstance, variableName, true);
        return restVariableDataToRestResponse(variable, response);
    }

    protected byte[] getVariableDataByteArray(PlanItemInstance planItemInstance, String variableName, HttpServletResponse response) {
        RestVariable variable = getVariableFromRequest(planItemInstance, variableName, true);
        return restVariableDataToRestResponse(variable, response);
    }

    protected byte[] restVariableDataToRestResponse(RestVariable variable, HttpServletResponse response) {
        byte[] result = null;
        try {
            if (CmmnRestResponseFactory.BYTE_ARRAY_VARIABLE_TYPE.equals(variable.getType())) {
                result = (byte[]) variable.getValue();
                response.setContentType("application/octet-stream");

            } else if (CmmnRestResponseFactory.SERIALIZABLE_VARIABLE_TYPE.equals(variable.getType())) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ObjectOutputStream outputStream = new ObjectOutputStream(buffer);
                outputStream.writeObject(variable.getValue());
                outputStream.close();
                result = buffer.toByteArray();
                response.setContentType("application/x-java-serialized-object");

            } else {
                throw new FlowableObjectNotFoundException("The variable does not have a binary data stream.", null);
            }
        } catch (IOException ioe) {
            throw new FlowableException("Error getting variable " + variable.getName(), ioe);
        }
        return result;
    }

    protected RestVariable constructRestVariable(String variableName, Object value, String caseInstanceId, int variableType, boolean includeBinary,
            RestVariableScope scope) {
        return restResponseFactory.createRestVariable(variableName, value, scope, caseInstanceId, variableType, includeBinary);
    }

    protected List<RestVariable> processCaseVariables(CaseInstance caseInstance) {

        // Check if it's a valid execution to get the variables for
        List<RestVariable> variables = addVariables(caseInstance);

        // Get unique variables from map
        List<RestVariable> result = new ArrayList<>(variables);
        return result;
    }

    protected Object createVariable(CaseInstance caseInstance, HttpServletRequest request, HttpServletResponse response) {
        return createVariable(caseInstance.getId(), CmmnRestResponseFactory.VARIABLE_CASE, request, response, RestVariableScope.GLOBAL,
                createVariableInterceptor(caseInstance));
    }

    protected Object createVariable(PlanItemInstance planItemInstance, HttpServletRequest request, HttpServletResponse response) {
        return createVariable(planItemInstance.getId(), CmmnRestResponseFactory.VARIABLE_PLAN_ITEM, request, response, RestVariableScope.LOCAL,
                createVariableInterceptor(planItemInstance));
    }

    protected Object createVariable(String instanceId, int variableType, HttpServletRequest request, HttpServletResponse response, RestVariableScope scope,
            VariableInterceptor variableInterceptor) {
        Object result = null;
        if (request instanceof MultipartHttpServletRequest) {
            result = setBinaryVariable((MultipartHttpServletRequest) request, instanceId, variableType, true, scope, variableInterceptor);
        } else {

            List<RestVariable> inputVariables = new ArrayList<>();
            List<RestVariable> resultVariables = new ArrayList<>();
            result = resultVariables;

            try {
                @SuppressWarnings("unchecked")
                List<Object> variableObjects = (List<Object>) objectMapper.readValue(request.getInputStream(), List.class);
                for (Object restObject : variableObjects) {
                    RestVariable restVariable = objectMapper.convertValue(restObject, RestVariable.class);
                    inputVariables.add(restVariable);
                }
                
            } catch (Exception e) {
                throw new FlowableIllegalArgumentException("Failed to serialize to a RestVariable instance", e);
            }

            if (inputVariables == null || inputVariables.size() == 0) {
                throw new FlowableIllegalArgumentException("Request didn't contain a list of variables to create.");
            }

            Map<String, Object> variablesToSet = new HashMap<>();
            for (RestVariable var : inputVariables) {
                if (var.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required");
                }

                Object actualVariableValue = restResponseFactory.getVariableValue(var);
                variablesToSet.put(var.getName(), actualVariableValue);
            }

            if (!variablesToSet.isEmpty()) {
                variableInterceptor.createVariables(variablesToSet);
                Map<String, Object> setVariables;
                if (variableType == CmmnRestResponseFactory.VARIABLE_PLAN_ITEM || scope == RestVariableScope.LOCAL) {
                    runtimeService.setLocalVariables(instanceId, variablesToSet);
                    setVariables = runtimeService.getLocalVariables(instanceId, variablesToSet.keySet());
                } else {
                    runtimeService.setVariables(instanceId, variablesToSet);
                    setVariables = runtimeService.getVariables(instanceId, variablesToSet.keySet());
                }

                for (RestVariable inputVariable : inputVariables) {
                    String variableName = inputVariable.getName();
                    Object variableValue = setVariables.get(variableName);
                    resultVariables.add(restResponseFactory.createRestVariable(variableName, variableValue, scope, instanceId, variableType, false));
                }
            }
        }
        response.setStatus(HttpStatus.CREATED.value());
        return result;
    }
    
    protected List<RestVariable> addVariables(CaseInstance caseInstance) {
        Map<String, Object> rawVariables = runtimeService.getVariables(caseInstance.getId());
        if (restApiInterceptor != null) {
            rawVariables = restApiInterceptor.accessCaseInstanceVariables(caseInstance, rawVariables);
        }
        return restResponseFactory.createRestVariables(rawVariables, caseInstance.getId(), CmmnRestResponseFactory.VARIABLE_CASE);
    }
    
    public void deleteAllVariables(CaseInstance caseInstance) {
        Collection<String> currentVariables = runtimeService.getVariables(caseInstance.getId()).keySet();
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteCaseInstanceVariables(caseInstance, currentVariables);
        }
        runtimeService.removeVariables(caseInstance.getId(), currentVariables);
    }
    
    protected RestVariable setSimpleVariable(RestVariable restVariable, String instanceId, boolean isNew, RestVariableScope scope, int variableType, VariableInterceptor variableInterceptor) {
        if (restVariable.getName() == null) {
            throw new FlowableIllegalArgumentException("Variable name is required");
        }

        Object actualVariableValue = restResponseFactory.getVariableValue(restVariable);

        setVariable(instanceId, restVariable.getName(), actualVariableValue, scope, isNew, variableInterceptor);

        RestVariable variable = getVariableFromRequestWithoutAccessCheck(instanceId, restVariable.getName(), variableType, false);
        // We are setting the scope because the fetched variable does not have it
        variable.setVariableScope(scope);
        return variable;
    }

    protected RestVariable setBinaryVariable(MultipartHttpServletRequest request, String instanceId, int responseVariableType, boolean isNew,
            RestVariableScope scope, VariableInterceptor variableInterceptor) {

        // Validate input and set defaults
        if (request.getFileMap().size() == 0) {
            throw new FlowableIllegalArgumentException("No file content was found in request body.");
        }

        // Get first file in the map, ignore possible other files
        MultipartFile file = request.getFile(request.getFileMap().keySet().iterator().next());

        if (file == null) {
            throw new FlowableIllegalArgumentException("No file content was found in request body.");
        }

        String variableScope = null;
        String variableName = null;
        String variableType = null;

        Map<String, String[]> paramMap = request.getParameterMap();
        for (String parameterName : paramMap.keySet()) {

            if (paramMap.get(parameterName).length > 0) {

                if ("scope".equalsIgnoreCase(parameterName)) {
                    variableScope = paramMap.get(parameterName)[0];

                } else if ("name".equalsIgnoreCase(parameterName)) {
                    variableName = paramMap.get(parameterName)[0];

                } else if ("type".equalsIgnoreCase(parameterName)) {
                    variableType = paramMap.get(parameterName)[0];
                }
            }
        }

        try {

            // Validate input and set defaults
            if (variableName == null) {
                throw new FlowableIllegalArgumentException("No variable name was found in request body.");
            }

            if (variableType != null) {
                if (!CmmnRestResponseFactory.BYTE_ARRAY_VARIABLE_TYPE.equals(variableType) && !CmmnRestResponseFactory.SERIALIZABLE_VARIABLE_TYPE.equals(variableType)) {
                    throw new FlowableIllegalArgumentException("Only 'binary' and 'serializable' are supported as variable type.");
                }
            } else {
                variableType = CmmnRestResponseFactory.BYTE_ARRAY_VARIABLE_TYPE;
            }

            if (variableScope != null) {
                scope = RestVariable.getScopeFromString(variableScope);
            }

            if (variableType.equals(CmmnRestResponseFactory.BYTE_ARRAY_VARIABLE_TYPE)) {
                // Use raw bytes as variable value
                byte[] variableBytes = IOUtils.toByteArray(file.getInputStream());
                setVariable(instanceId, variableName, variableBytes, scope, isNew, variableInterceptor);

            } else if (isSerializableVariableAllowed) {
                // Try deserializing the object
                ObjectInputStream stream = new ObjectInputStream(file.getInputStream());
                Object value = stream.readObject();
                setVariable(instanceId, variableName, value, scope, isNew, variableInterceptor);
                stream.close();
            } else {
                throw new FlowableContentNotSupportedException("Serialized objects are not allowed");
            }

            RestVariable restVariable = getVariableFromRequestWithoutAccessCheck(instanceId, variableName, responseVariableType, false);
            // We are setting the scope because the fetched variable does not have it
            restVariable.setVariableScope(scope);
            return restVariable;
        } catch (IOException ioe) {
            throw new FlowableIllegalArgumentException("Could not process multipart content", ioe);
        } catch (ClassNotFoundException ioe) {
            throw new FlowableContentNotSupportedException(
                    "The provided body contains a serialized object for which the class was not found: " + ioe.getMessage());
        }
    }

    protected void setVariable(String instanceId, String name, Object value, RestVariableScope scope, boolean isNew, VariableInterceptor variableInterceptor) {
        if (isNew) {
            variableInterceptor.createVariables(Collections.singletonMap(name, value));
        } else {
            variableInterceptor.updateVariables(Collections.singletonMap(name, value));
        }

        if (RestVariableScope.LOCAL == scope) {
            //the guard is only added here, because this whole block is new
            if (isNew && runtimeService.hasLocalVariable(instanceId, name)) {
                throw new FlowableConflictException("Local variable '" + name + "' is already present on plan item instance '" + instanceId + "'.");
            }
            runtimeService.setLocalVariable(instanceId, name, value);
        } else {
            runtimeService.setVariable(instanceId, name, value);
        }
    }

    protected VariableInterceptor createVariableInterceptor(PlanItemInstance planItemInstance) {
        if (restApiInterceptor != null) {
            return new VariableInterceptor() {

                @Override
                public void createVariables(Map<String, Object> variables) {
                    restApiInterceptor.createPlanItemInstanceVariables(planItemInstance, variables);
                }

                @Override
                public void updateVariables(Map<String, Object> variables) {
                    restApiInterceptor.updatePlanItemInstanceVariables(planItemInstance, variables);
                }
            };
        }

        return NoopVariableInterceptor.INSTANCE;
    }

    protected VariableInterceptor createVariableInterceptor(CaseInstance caseInstance) {
        if (restApiInterceptor != null) {
            return new VariableInterceptor() {

                @Override
                public void createVariables(Map<String, Object> variables) {
                    restApiInterceptor.createCaseInstanceVariables(caseInstance, variables);
                }

                @Override
                public void updateVariables(Map<String, Object> variables) {
                    restApiInterceptor.updateCaseInstanceVariables(caseInstance, variables);
                }
            };
        }

        return NoopVariableInterceptor.INSTANCE;
    }

    protected interface VariableInterceptor {

        void createVariables(Map<String, Object> variables);

        void updateVariables(Map<String, Object> variables);
    }

    protected static class NoopVariableInterceptor implements VariableInterceptor {

        static final VariableInterceptor INSTANCE = new NoopVariableInterceptor();

        @Override
        public void createVariables(Map<String, Object> variables) {
            // Nothing to do
        }

        @Override
        public void updateVariables(Map<String, Object> variables) {
            // Nothing to do
        }
    }


}
