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

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Event subscriptions" }, description = "Manage event subscriptions", authorizations = { @Authorization(value = "basicAuth") })
public class EventSubscriptionResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RuntimeService runtimeService;

    @ApiOperation(value = "Get a single event subscription", tags = { "Event subscriptions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the event subscription exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested event subscription does not exist.")
    })
    @GetMapping(value = "/runtime/event-subscriptions/{eventSubscriptionId}", produces = "application/json")
    public EventSubscriptionResponse getEventSubscription(@ApiParam(name = "eventSubscriptionId") @PathVariable String eventSubscriptionId, HttpServletRequest request) {
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().id(eventSubscriptionId).singleResult();

        if (eventSubscription == null) {
            throw new FlowableObjectNotFoundException("Could not find a event subscription with id '" + eventSubscriptionId + "'.", EventSubscription.class);
        }

        return restResponseFactory.createEventSubscriptionResponse(eventSubscription);
    }
}
