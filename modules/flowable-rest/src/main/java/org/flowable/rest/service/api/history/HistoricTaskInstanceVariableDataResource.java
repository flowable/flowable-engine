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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.*;

import org.flowable.engine.HistoryService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.history.HistoricTaskInstanceQuery;
import org.flowable.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.engine.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.rest.service.api.engine.variable.RestVariable.RestVariableScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "History" }, description = "Manage History")
public class HistoricTaskInstanceVariableDataResource {

  @Autowired
  protected RestResponseFactory restResponseFactory;

  @Autowired
  protected HistoryService historyService;

  @RequestMapping(value = "/history/historic-task-instances/{taskId}/variables/{variableName}/data", method = RequestMethod.GET)
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Indicates the task instance was found and the requested variable data is returned."),
          @ApiResponse(code = 404, message = "Indicates the requested task instance was not found or the process instance doesn’t have a variable with the given name or the variable doesn’t have a binary stream available. Status message provides additional information.")})
  @ApiOperation(value = "Get the binary data for a historic task instance variable", tags = {"History"}, nickname = "getHistoricTaskInstanceVariableData",
          notes = "The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.")
  @ResponseBody
  public byte[] getVariableData(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName, @RequestParam(value = "scope", required = false) String scope,
      HttpServletRequest request, HttpServletResponse response) {

    try {
      byte[] result = null;
      RestVariable variable = getVariableFromRequest(true, taskId, variableName, scope, request);
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

  public RestVariable getVariableFromRequest(boolean includeBinary, String taskId, String variableName, String scope, HttpServletRequest request) {
    RestVariableScope variableScope = RestVariable.getScopeFromString(scope);
    HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery().taskId(taskId);

    if (variableScope != null) {
      if (variableScope == RestVariableScope.GLOBAL) {
        taskQuery.includeProcessVariables();
      } else {
        taskQuery.includeTaskLocalVariables();
      }
    } else {
      taskQuery.includeTaskLocalVariables().includeProcessVariables();
    }

    HistoricTaskInstance taskObject = taskQuery.singleResult();

    if (taskObject == null) {
      throw new FlowableObjectNotFoundException("Historic task instance '" + taskId + "' couldn't be found.", HistoricTaskInstanceEntity.class);
    }

    Object value = null;
    if (variableScope != null) {
      if (variableScope == RestVariableScope.GLOBAL) {
        value = taskObject.getProcessVariables().get(variableName);
      } else {
        value = taskObject.getTaskLocalVariables().get(variableName);
      }
    } else {
      // look for local task variables first
      if (taskObject.getTaskLocalVariables().containsKey(variableName)) {
        value = taskObject.getTaskLocalVariables().get(variableName);
      } else {
        value = taskObject.getProcessVariables().get(variableName);
      }
    }

    if (value == null) {
      throw new FlowableObjectNotFoundException("Historic task instance '" + taskId + "' variable value for " + variableName + " couldn't be found.", VariableInstanceEntity.class);
    } else {
      return restResponseFactory.createRestVariable(variableName, value, null, taskId, RestResponseFactory.VARIABLE_HISTORY_TASK, includeBinary);
    }
  }
}
