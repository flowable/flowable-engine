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
package org.flowable.content.rest.service.api.management;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.EngineInfo;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngines;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Engine" }, description = "Manage Content Engine", authorizations = { @Authorization(value = "basicAuth") })
public class ContentEngineResource {

    @ApiOperation(value = "Get Content engine info", tags = { "Engine" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the engine info is returned."),
    })
    @GetMapping(value = "/content-management/engine", produces = "application/json")
    public ContentEngineInfoResponse getEngineInfo() {
        ContentEngineInfoResponse response = new ContentEngineInfoResponse();

        try {
            ContentEngine contentEngine = ContentEngines.getDefaultContentEngine();
            EngineInfo contentEngineInfo = ContentEngines.getContentEngineInfo(contentEngine.getName());
            if (contentEngineInfo != null) {
                response.setName(contentEngineInfo.getName());
                response.setResourceUrl(contentEngineInfo.getResourceUrl());
                response.setException(contentEngineInfo.getException());

            } else {
                response.setName(contentEngine.getName());
            }

        } catch (Exception e) {
            throw new FlowableException("Error retrieving content engine info", e);
        }

        response.setVersion(ContentEngine.VERSION);

        return response;
    }
}
