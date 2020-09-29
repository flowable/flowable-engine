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

import java.util.List;

import org.flowable.engine.runtime.Execution;
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
 * @author Frederik Heremans
 */

@RestController
@Api(tags = { "Executions" }, description = "Manage Executions", authorizations = { @Authorization(value = "basicAuth") })
public class ExecutionActiveActivitiesCollectionResource extends ExecutionBaseResource {

    @ApiOperation(value = "List active activities in an execution", nickname="listExecutionActiveActivities", tags = { "Executions" },
            notes = "Returns all activities which are active in the execution and in all child-executions (and their children, recursively), if any.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the execution was found and activities are returned."),
            @ApiResponse(code = 404, message = "Indicates the execution was not found.")
    })
    @GetMapping(value = "/runtime/executions/{executionId}/activities", produces = "application/json")
    public List<String> getActiveActivities(@ApiParam(name = "executionId") @PathVariable String executionId) {
        Execution execution = getExecutionFromRequest(executionId);
        return runtimeService.getActiveActivityIds(execution.getId());
    }
}
