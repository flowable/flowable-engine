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

package org.flowable.rest.service.api.management;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.engine.ManagementService;
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
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Database tables" }, description = "Manage Database tables", authorizations = { @Authorization(value = "basicAuth") })
public class TableColumnsResource {

    @Autowired
    protected ManagementService managementService;

    @ApiOperation(value = "Get column info for a single table", tags = { "Database tables" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the table exists and the table column info is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested table does not exist.")
    })
    @GetMapping(value = "/management/tables/{tableName}/columns", produces = "application/json")
    public TableMetaData getTableMetaData(@ApiParam(name = "tableName") @PathVariable String tableName) {
        TableMetaData response = managementService.getTableMetaData(tableName);

        if (response == null) {
            throw new FlowableObjectNotFoundException("Could not find a table with name '" + tableName + "'.", String.class);
        }
        return response;
    }
}
