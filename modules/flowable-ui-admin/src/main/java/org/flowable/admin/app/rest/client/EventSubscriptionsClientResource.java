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
package org.flowable.admin.app.rest.client;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.admin.domain.EndpointType;
import org.flowable.admin.domain.ServerConfig;
import org.flowable.admin.service.engine.EventSubscriptionService;
import org.flowable.admin.service.engine.exception.FlowableServiceException;
import org.flowable.app.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
public class EventSubscriptionsClientResource extends AbstractClientResource {

  private static final Logger logger = LoggerFactory.getLogger(EventSubscriptionsClientResource.class);

  @Autowired
  protected EventSubscriptionService eventSubscriptionService;

  /**
   * GET /rest/admin/event-subscriptions -> Get a list of event subscriptions.
   */
  @RequestMapping(value = "/rest/admin/event-subscriptions", method = RequestMethod.GET, produces = "application/json")
  public JsonNode listEventSubscriptions(HttpServletRequest request) {
    logger.debug("REST request to get a list of event subscriptions");
    ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
    Map<String, String[]> parameterMap = getRequestParametersWithoutServerId(request);

    try {
      return eventSubscriptionService.listEventSubscriptions(serverConfig, parameterMap);
    } catch (FlowableServiceException e) {
      logger.error("Error getting event subscriptions");
      throw new BadRequestException(e.getMessage());
    }
  }
}
