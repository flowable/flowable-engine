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

package org.flowable.rest.service.api.management;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.EngineInfo;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Engine" }, description = "Manage Engine", authorizations = { @Authorization(value = "basicAuth") })
public class ProcessEngineResource {

    @Autowired
    @Qualifier("processEngine")
    protected ProcessEngine engine;

    @ApiOperation(value = "Get engine info", tags = { "Engine" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the engine info is returned."),
    })
    @GetMapping(value = "/management/engine", produces = "application/json")
    public ProcessEngineInfoResponse getEngineInfo() {
        ProcessEngineInfoResponse response = new ProcessEngineInfoResponse();

        try {
            EngineInfo engineInfo = ProcessEngines.getProcessEngineInfo(engine.getName());
            if (engineInfo != null) {
                response.setName(engineInfo.getName());
                response.setResourceUrl(engineInfo.getResourceUrl());
                response.setException(engineInfo.getException());
            } else {
                // Revert to using process-engine directly
                response.setName(engine.getName());
            }
        } catch (Exception e) {
            throw new FlowableException("Error retrieving process info", e);
        }

        response.setVersion(ProcessEngine.VERSION);
        return response;
    }
}