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

package org.flowable.rest.service.api.history;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@Api(tags = { "History Process" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricProcessInstanceIdentityLinkCollectionResource extends HistoricProcessInstanceBaseResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected HistoryService historyService;

    @ApiOperation(value = "List identity links of a historic process instance", nickname="listHistoricProcessInstanceIdentityLinks", tags = { "History Process" }, notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the identity links are returned", response = HistoricIdentityLinkResponse.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Indicates the process instance could not be found..") })
    @GetMapping(value = "/history/historic-process-instances/{processInstanceId}/identitylinks", produces = "application/json")
    public List<HistoricIdentityLinkResponse> getProcessIdentityLinks(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId) {
        HistoricProcessInstance processInstance = getHistoricProcessInstanceFromRequestWithoutAccessCheck(processInstanceId);

        if (restApiInterceptor != null) {
            restApiInterceptor.accessHistoricProcessIdentityLinks(processInstance);
        }
        
        List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());

        if (identityLinks != null) {
            return restResponseFactory.createHistoricIdentityLinkResponseList(identityLinks);
        }

        return new ArrayList<>();
    }
}
