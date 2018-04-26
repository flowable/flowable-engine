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

package org.flowable.cmmn.rest.service.api.history.milestone;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * @author Tijs Rademakers
 * @author DennisFederico
 */
@RestController
@Api(tags = {"History Milestone"}, description = "Manage History Milestone Instances", authorizations = {@Authorization(value = "basicAuth")})
public class HistoricMilestoneInstanceCollectionResource extends HistoricMilestoneInstanceBaseResource {

    private static Map<String, BiConsumer<HistoricMilestoneInstanceQueryRequest, String>> mapping = new HashMap<>();
    private static BiConsumer<HistoricMilestoneInstanceQueryRequest, String> voidConsumer = (o1, o2) -> { };

    static {
        mapping.put("milestoneId", HistoricMilestoneInstanceQueryRequest::setId);
        mapping.put("milestoneName", HistoricMilestoneInstanceQueryRequest::setName);
        mapping.put("caseInstanceId", HistoricMilestoneInstanceQueryRequest::setCaseInstanceId);
        mapping.put("caseDefinitionId", HistoricMilestoneInstanceQueryRequest::setCaseDefinitionId);
        mapping.put("reachedBefore", (h, s) -> h.setReachedBefore(RequestUtil.parseLongDate(s)));
        mapping.put("reachedAfter", (h, s) -> h.setReachedAfter(RequestUtil.parseLongDate(s)));
    }

    @ApiOperation(value = "List of historic milestone instances", tags = {"History Milestone"}, nickname = "listHistoricMilestoneInstances")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "milestoneId", dataType = "string", value = "An id of the historic milestone instance.", paramType = "query"),
            @ApiImplicitParam(name = "milestoneName", dataType = "string", value = "The name of the historic milestone instance", paramType = "query"),
            @ApiImplicitParam(name = "caseInstanceId", dataType = "string", value = "The id of the case instance containing the milestone.", paramType = "query"),
            @ApiImplicitParam(name = "caseDefinitionId", dataType = "string", value = "The id of the definition of the case where the milestone is defined.", paramType = "query"),
            @ApiImplicitParam(name = "reachedBefore", dataType = "string", value = "Return only historic milestone instances that were reached before this date.", paramType = "query"),
            @ApiImplicitParam(name = "reachedAfter", dataType = "string", value = "Return only historic milestone instances that were reached after this date.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that historic milestone instances could be queried."),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.")})
    @GetMapping(value = "/cmmn-history/historic-milestone-instances", produces = "application/json")
    public DataResponse<HistoricMilestoneInstanceResponse> getHistoricMilestoneInstances(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        HistoricMilestoneInstanceQueryRequest queryRequest = new HistoricMilestoneInstanceQueryRequest();
        allRequestParams.forEach((key, value) -> Optional.ofNullable(value).ifPresent(v -> mapping.getOrDefault(key, voidConsumer).accept(queryRequest, v)));
        return getQueryResponse(queryRequest, allRequestParams);
    }
}
