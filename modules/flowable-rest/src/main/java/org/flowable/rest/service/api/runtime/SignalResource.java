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

package org.flowable.rest.service.api.runtime;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.RuntimeService;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.engine.variable.RestVariable;
import org.flowable.rest.service.api.runtime.process.SignalEventReceivedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource for notifying the engine a signal event has been received, independent of an execution.
 * 
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Runtime" }, description = "Manage Runtime", authorizations = { @Authorization(value = "basicAuth") })
public class SignalResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RuntimeService runtimeService;

    @ApiOperation(value = "Signal event received", tags = { "Runtime" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicated signal has been processed and no errors occurred."),
            @ApiResponse(code = 202, message = "Indicated signal processing is queued as a job, ready to be executed."),
            @ApiResponse(code = 400, message = "Signal not processed. The signal name is missing or variables are used together with async, which is not allowed. Response body contains additional information about the error.")
    })
    @PostMapping(value = "/runtime/signals")
    public void signalEventReceived(@RequestBody SignalEventReceivedRequest signalRequest, HttpServletResponse response) {
        if (signalRequest.getSignalName() == null) {
            throw new FlowableIllegalArgumentException("signalName is required");
        }

        Map<String, Object> signalVariables = null;
        if (signalRequest.getVariables() != null) {
            signalVariables = new HashMap<>();
            for (RestVariable variable : signalRequest.getVariables()) {
                if (variable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }
                signalVariables.put(variable.getName(), restResponseFactory.getVariableValue(variable));
            }
        }

        if (signalRequest.isAsync()) {
            if (signalVariables != null) {
                throw new FlowableIllegalArgumentException("Async signals cannot take variables as payload");
            }

            if (signalRequest.isCustomTenantSet()) {
                runtimeService.signalEventReceivedAsyncWithTenantId(signalRequest.getSignalName(), signalRequest.getTenantId());
            } else {
                runtimeService.signalEventReceivedAsync(signalRequest.getSignalName());
            }
            response.setStatus(HttpStatus.ACCEPTED.value());
        } else {
            if (signalRequest.isCustomTenantSet()) {
                runtimeService.signalEventReceivedWithTenantId(signalRequest.getSignalName(), signalVariables, signalRequest.getTenantId());
            } else {
                runtimeService.signalEventReceived(signalRequest.getSignalName(), signalVariables);
            }
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }
}
