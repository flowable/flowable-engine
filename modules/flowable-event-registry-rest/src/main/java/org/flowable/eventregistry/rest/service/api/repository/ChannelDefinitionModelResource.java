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

package org.flowable.eventregistry.rest.service.api.repository;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.model.ChannelModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Api(tags = { "Channel Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class ChannelDefinitionModelResource extends BaseEventDefinitionResource {

    @ApiOperation(value = "Get a channel definition JSON model", tags = { "Channel Definitions" }, nickname = "getChannelModelResource")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the event definition was found and the model is returned. The response contains the full event definition model."),
            @ApiResponse(code = 404, message = "Indicates the requested event definition was not found.")
    })
    @GetMapping(value = "/event-registry-repository/channel-definitions/{channelDefinitionId}/model", produces = "application/json")
    public ChannelModel getModelResource(@ApiParam(name = "channelDefinitionId") @PathVariable String channelDefinitionId) {
        ChannelDefinition channelDefinition = getChannelDefinitionFromRequest(channelDefinitionId);
        return repositoryService.getChannelModelById(channelDefinition.getId());
    }

}
