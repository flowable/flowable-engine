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

package org.flowable.cmmn.rest.service.api.runtime.caze;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.rest.api.DataResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
@Api(tags = { "Case Instances" }, description = "Manage Case Instances", authorizations = { @Authorization(value = "basicAuth") })
public class CaseInstanceQueryResource extends BaseCaseInstanceResource {

    @ApiOperation(value = "Query case instances", tags = {"Case Instances", "Query" },
            notes = "The request body can contain all possible filters that can be used in the List case instances URL query. On top of these, it’s possible to provide an array of variables to include in the query, with their format described here.\n"
                    + "\n" + "The general paging and sorting query-parameters can be used for this URL.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the case instances are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format . The status-message contains additional information.")
    })
    @PostMapping(value = "/cmmn-query/case-instances", produces = "application/json")
    public DataResponse<CaseInstanceResponse> queryCaseInstances(@RequestBody CaseInstanceQueryRequest queryRequest, @ApiParam(hidden = true) 
            @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {

        return getQueryResponse(queryRequest, allRequestParams);
    }
}
