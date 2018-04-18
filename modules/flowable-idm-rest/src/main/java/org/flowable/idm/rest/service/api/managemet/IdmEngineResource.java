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
package org.flowable.idm.rest.service.api.managemet;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.EngineInfo;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngines;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Joram Barrez
 */
@RestController
@Api(tags = { "Engine" }, description = "Manage IDM Engine", authorizations = { @Authorization(value = "basicAuth") })
public class IdmEngineResource {

    @ApiOperation(value = "Get IDM engine info", tags = { "Engine" }, notes = "Returns a read-only view of the engine that is used in this REST-service.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the engine info is returned."),
    })
    @GetMapping(value = "/idm-management/engine", produces = "application/json")
    public IdmEngineInfoResponse getEngineInfo() {
        IdmEngineInfoResponse response = new IdmEngineInfoResponse();

        try {
            IdmEngine idmEngine = IdmEngines.getDefaultIdmEngine();
            EngineInfo idmEngineInfo = IdmEngines.getIdmEngineInfo(idmEngine.getName());

            if (idmEngineInfo != null) {
                response.setName(idmEngineInfo.getName());
                response.setResourceUrl(idmEngineInfo.getResourceUrl());
                response.setException(idmEngineInfo.getException());
            } else {
                response.setName(idmEngine.getName());
            }
        } catch (Exception e) {
            throw new FlowableException("Error retrieving idm engine info", e);
        }

        response.setVersion(IdmEngine.VERSION);

        return response;
    }
}
