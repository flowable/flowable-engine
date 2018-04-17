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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.runtime.Execution;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Executions" }, description = "Manage Executions", authorizations = { @Authorization(value = "basicAuth") })
public class ExecutionVariableDataResource extends BaseExecutionVariableResource {

    @ApiOperation(value = "Get the binary data for an execution", tags = { "Executions" }, nickname = "getExecutionVariableData")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the execution was found and the requested variables are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested execution was not found or the task doesn’t have a variable with the given name (in the given scope). Status message provides additional information.")
    })
    @GetMapping(value = "/runtime/executions/{executionId}/variables/{variableName}/data")
    @ResponseBody
    public byte[] getVariableData(@ApiParam(name = "executionId") @PathVariable("executionId") String executionId, @ApiParam(name = "variableName") @PathVariable("variableName") String variableName,
            @RequestParam(value = "scope", required = false) String scope,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            byte[] result = null;

            Execution execution = getExecutionFromRequest(executionId);
            RestVariable variable = getVariableFromRequest(execution, variableName, scope, true);
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
            throw new FlowableException("Error getting variable " + variableName, ioe);
        }
    }
}
