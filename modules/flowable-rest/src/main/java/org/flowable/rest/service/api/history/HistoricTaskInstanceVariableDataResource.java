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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "History Task" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricTaskInstanceVariableDataResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected ManagementService managementService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "Get the binary data for a historic task instance variable", tags = {"History" }, nickname = "getHistoricTaskInstanceVariableData",
            notes = "The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task instance was found and the requested variable data is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task instance was not found or the process instance does not have a variable with the given name or the variable does not have a binary stream available. Status message provides additional information.") })
    @GetMapping(value = "/history/historic-task-instances/{taskId}/variables/{variableName}/data")
    @ResponseBody
    public byte[] getVariableData(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName, @RequestParam(value = "scope", required = false) String scope,
            HttpServletResponse response) {

        try {
            byte[] result = null;
            RestVariable variable = getVariableFromRequest(true, taskId, variableName, scope);
            if (RestResponseFactory.BYTE_ARRAY_VARIABLE_TYPE.equals(variable.getType())) {
                result = (byte[]) variable.getValue();
                response.setContentType("application/octet-stream");

            } else if (RestResponseFactory.SERIALIZABLE_VARIABLE_TYPE.equals(variable.getType())) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ObjectOutputStream outputStream = new ObjectOutputStream(buffer);
                outputStream.writeObject(variable.getValue());
                outputStream.close();
                result = buffer.toByteArray();
                response.setContentType("application/x-java-serialized-object");

            } else {
                throw new FlowableObjectNotFoundException("The variable does not have a binary data stream.", null);
            }
            return result;

        } catch (IOException ioe) {
            // Re-throw IOException
            throw new FlowableException("Unexpected exception getting variable data", ioe);
        }
    }

    public RestVariable getVariableFromRequest(boolean includeBinary, String taskId, String variableName, String scope) {
        // Only the value of the requested variable is resolved, in the same command as the query
        return managementService.executeCommand(commandContext -> readVariable(includeBinary, taskId, variableName, scope));
    }

    protected RestVariable readVariable(boolean includeBinary, String taskId, String variableName, String scope) {
        RestVariableScope variableScope = RestVariable.getScopeFromString(scope);
        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery().taskId(taskId);

        if (variableScope != null) {
            if (variableScope == RestVariableScope.GLOBAL) {
                taskQuery.includeProcessVariables(true);
            } else {
                taskQuery.includeTaskLocalVariables(true);
            }
        } else {
            taskQuery.includeTaskLocalVariables(true).includeProcessVariables(true);
        }

        HistoricTaskInstance taskObject = taskQuery.singleResult();

        if (taskObject == null) {
            throw new FlowableObjectNotFoundException("Historic task instance '" + taskId + "' could not be found.", HistoricTaskInstanceEntity.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessHistoryTaskInfoById(taskObject);
        }

        HistoricVariableInstance variableInstance = null;
        if (variableScope != null) {
            if (variableScope == RestVariableScope.GLOBAL) {
                variableInstance = getProcessVariable(taskObject, variableName);
            } else {
                variableInstance = getTaskLocalVariable(taskObject, variableName);
            }
        } else {
            // look for local task variables first
            variableInstance = getTaskLocalVariable(taskObject, variableName);
            if (variableInstance == null) {
                variableInstance = getProcessVariable(taskObject, variableName);
            }
        }
        Object value = variableInstance != null ? variableInstance.getValue() : null;

        if (value == null) {
            throw new FlowableObjectNotFoundException("Historic task instance '" + taskId + "' variable value for " + variableName + " could not be found.", VariableInstanceEntity.class);
        } else {
            return restResponseFactory.createRestVariable(variableName, value, null, taskId, RestResponseFactory.VARIABLE_HISTORY_TASK, includeBinary);
        }
    }

    protected HistoricVariableInstance getTaskLocalVariable(HistoricTaskInstance taskObject, String variableName) {
        HistoricVariableInstance variableInstance = null;
        for (HistoricVariableInstanceEntity queryVariable : ((HistoricTaskInstanceEntity) taskObject).getQueryVariables()) {
            if (queryVariable.getId() != null && queryVariable.getTaskId() != null && variableName.equals(queryVariable.getName())) {
                variableInstance = queryVariable;
            }
        }
        return variableInstance;
    }

    protected HistoricVariableInstance getProcessVariable(HistoricTaskInstance taskObject, String variableName) {
        HistoricVariableInstance variableInstance = null;
        for (HistoricVariableInstanceEntity queryVariable : ((HistoricTaskInstanceEntity) taskObject).getQueryVariables()) {
            if (taskObject.getProcessInstanceId() != null && taskObject.getProcessInstanceId().equals(queryVariable.getProcessInstanceId())
                    && queryVariable.getTaskId() == null && variableName.equals(queryVariable.getName())) {
                variableInstance = queryVariable;
            }
        }
        return variableInstance;
    }
}
