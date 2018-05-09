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

package org.flowable.cmmn.rest.service.api.history.variable;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.rest.api.DataResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "History" }, description = "Manage History", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricVariableInstanceQueryResource extends HistoricVariableInstanceBaseResource {

    @ApiOperation(value = "Query for historic variable instances", tags = { "History", "Query" },
            notes = "All supported JSON parameter fields allowed are exactly the same as the parameters found for getting a collection of historic process instances,"
            + " but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri’s that are too long. "
            + "On top of that, the query allows for filtering based on process variables. "
            + "The variables property is a JSON-array containing objects with the format as described here.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the tasks are returned"),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information.") })
    @PostMapping(value = "/cmmn-query/historic-variable-instances", produces = "application/json")
    public DataResponse<HistoricVariableInstanceResponse> queryVariableInstances(@RequestBody HistoricVariableInstanceQueryRequest queryRequest, @ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams,
            HttpServletRequest request) {

        return getQueryResponse(queryRequest, allRequestParams);
    }
}
