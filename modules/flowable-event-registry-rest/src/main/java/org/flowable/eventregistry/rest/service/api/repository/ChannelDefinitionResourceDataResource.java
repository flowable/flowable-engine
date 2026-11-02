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

import jakarta.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.eventregistry.api.ChannelDefinition;
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
public class ChannelDefinitionResourceDataResource extends BaseDeploymentResourceDataResource {

    @ApiOperation(value = "Get a channel definition resource content", tags = { "Channel Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both channel definition and resource have been found and the resource data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested channel definition was not found or there is no resource with the given id present in the channel definition. The status-description contains additional information.")
    })
    @GetMapping(value = "/event-registry-repository/channel-definitions/{channelDefinitionId}/resourcedata")
    public byte[] getChannelDefinitionResource(@ApiParam(name = "channelDefinitionId") @PathVariable String channelDefinitionId, HttpServletResponse response) {
        ChannelDefinition channelDefinition = getChannelDefinitionFromRequest(channelDefinitionId);
        return getDeploymentResourceData(channelDefinition.getDeploymentId(), channelDefinition.getResourceName(), response);
    }

    /**
     * Returns the {@link ChannelDefinition} that is requested. Throws the right exceptions when bad request was made or definition was not found.
     */
    protected ChannelDefinition getChannelDefinitionFromRequest(String channelDefinitionId) {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery().channelDefinitionId(channelDefinitionId).singleResult();

        if (channelDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find a channel definition with id '" + channelDefinitionId + "'.", ChannelDefinition.class);
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessChannelDefinitionById(channelDefinition);
        }

        return channelDefinition;
    }
}
