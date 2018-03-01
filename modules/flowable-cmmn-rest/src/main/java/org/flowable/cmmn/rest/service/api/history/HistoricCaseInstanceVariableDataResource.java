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

package org.flowable.cmmn.rest.service.api.history;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.rest.service.api.RestResponseFactory;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Api(tags = { "History Process" }, description = "Manage History Process Instances", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricCaseInstanceVariableDataResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnHistoryService historyService;

    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the case instance was found and the requested variable data is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found or the process instance doesn’t have a variable with the given name or the variable doesn’t have a binary stream available. Status message provides additional information.") })
    @ApiOperation(value = "Get the binary data for a historic case instance variable", tags = {"History Process" }, nickname = "getHistoricCaseInstanceVariableData",
            notes = "The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.")
    @GetMapping(value = "/cmmn-history/historic-case-instances/{caseInstanceId}/variables/{variableName}/data")
    @ResponseBody
    public byte[] getVariableData(@ApiParam(name = "caseInstanceId") @PathVariable("caseInstanceId") String caseInstanceId, 
                    @ApiParam(name = "variableName") @PathVariable("variableName") String variableName, HttpServletRequest request, HttpServletResponse response) {
        try {
            byte[] result = null;
            RestVariable variable = getVariableFromRequest(true, caseInstanceId, variableName, request);
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

    public RestVariable getVariableFromRequest(boolean includeBinary, String caseInstanceId, String variableName, HttpServletRequest request) {

        HistoricCaseInstance caseObject = historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (caseObject == null) {
            throw new FlowableObjectNotFoundException("Historic case instance '" + caseInstanceId + "' couldn't be found.", HistoricCaseInstanceEntity.class);
        }

        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceId).variableName(variableName).singleResult();

        if (variable == null || variable.getValue() == null) {
            throw new FlowableObjectNotFoundException("Historic case instance '" + caseInstanceId + "' variable value for " + variableName + " couldn't be found.", VariableInstanceEntity.class);
        } else {
            return restResponseFactory.createRestVariable(variableName, variable.getValue(), null, caseInstanceId, RestResponseFactory.VARIABLE_HISTORY_CASE, includeBinary);
        }
    }
}
