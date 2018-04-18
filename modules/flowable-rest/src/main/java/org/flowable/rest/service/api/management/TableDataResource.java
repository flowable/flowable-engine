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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.management.TablePage;
import org.flowable.common.engine.api.management.TablePageQuery;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.ManagementService;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Database tables" }, description = "Manage Database tables", authorizations = { @Authorization(value = "basicAuth") })
public class TableDataResource {

    protected static final Integer DEFAULT_RESULT_SIZE = 10;

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ManagementService managementService;

    @ApiOperation(value = "Get row data for a single table", tags = { "Database tables" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),
            @ApiImplicitParam(name = "orderAscendingColumn", dataType = "string", value = "Name of the column to sort the resulting rows on, ascending.", paramType = "query"),
            @ApiImplicitParam(name = "orderDescendingColumn", dataType = "string", value = "Name of the column to sort the resulting rows on, descending.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the table exists and the table row data is returned"),
            @ApiResponse(code = 404, message = "Indicates the requested table does not exist.")
    })
    @GetMapping(value = "/management/tables/{tableName}/data", produces = "application/json")
    public DataResponse<List<Map<String, Object>>> getTableData(@ApiParam(name = "tableName") @PathVariable String tableName, @ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        // Check if table exists before continuing
        if (managementService.getTableMetaData(tableName) == null) {
            throw new FlowableObjectNotFoundException("Could not find a table with name '" + tableName + "'.", String.class);
        }

        String orderAsc = allRequestParams.get("orderAscendingColumn");
        String orderDesc = allRequestParams.get("orderDescendingColumn");

        if (orderAsc != null && orderDesc != null) {
            throw new FlowableIllegalArgumentException("Only one of 'orderAscendingColumn' or 'orderDescendingColumn' can be supplied.");
        }

        Integer start = null;
        if (allRequestParams.containsKey("start")) {
            start = Integer.valueOf(allRequestParams.get("start"));
        }

        if (start == null) {
            start = 0;
        }

        Integer size = null;
        if (allRequestParams.containsKey("size")) {
            size = Integer.valueOf(allRequestParams.get("size"));
        }

        if (size == null) {
            size = DEFAULT_RESULT_SIZE;
        }

        DataResponse response = new DataResponse();

        TablePageQuery tablePageQuery = managementService.createTablePageQuery().tableName(tableName);

        if (orderAsc != null) {
            tablePageQuery.orderAsc(orderAsc);
            response.setOrder("asc");
            response.setSort(orderAsc);
        }

        if (orderDesc != null) {
            tablePageQuery.orderDesc(orderDesc);
            response.setOrder("desc");
            response.setSort(orderDesc);
        }

        TablePage listPage = tablePageQuery.listPage(start, size);
        response.setSize(((Long) listPage.getSize()).intValue());
        response.setStart(((Long) listPage.getFirstResult()).intValue());
        response.setTotal(listPage.getTotal());
        response.setData(listPage.getRows());

        return response;
    }
}
