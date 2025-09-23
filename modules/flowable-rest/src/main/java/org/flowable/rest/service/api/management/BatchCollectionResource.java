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

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.Map;

import org.flowable.batch.api.BatchQuery;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.engine.ManagementService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@RestController
@Api(tags = { "Batches" }, authorizations = { @Authorization(value = "basicAuth") })
public class BatchCollectionResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ManagementService managementService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    // Fixme documentation & real parameters
    @ApiOperation(value = "List batches", tags = { "Batches" }, nickname = "listBatches")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return batch with the given id", paramType = "query"),
            @ApiImplicitParam(name = "batchType", dataType = "string", value = "Only return batches for the given type", paramType = "query"),
            @ApiImplicitParam(name = "searchKey", dataType = "string", value = "Only return batches for the given search key", paramType = "query"),
            @ApiImplicitParam(name = "searchKey2", dataType = "string", value = "Only return batches for the given search key2", paramType = "query"),
            @ApiImplicitParam(name = "createTimeBefore", dataType = "string", format="date-time", value = "Only return batches created before the given date", paramType = "query"),
            @ApiImplicitParam(name = "createTimeAfter", dataType = "string", format="date-time", value = "Only batches batches created after the given date", paramType = "query"),
            @ApiImplicitParam(name = "completeTimeBefore", dataType = "string", format="date-time", value = "Only return batches completed before the given date", paramType = "query"),
            @ApiImplicitParam(name = "completeTimeAfter", dataType = "string", format="date-time", value = "Only batches batches completed after the given date", paramType = "query"),
            @ApiImplicitParam(name = "status", dataType = "string", value = "Only return batches for the given status", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return batches for the given tenant id", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return batches like given search key", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns batches without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested batches were returned."),
            @ApiResponse(code = 400, message = "Indicates an illegal value has been used in a url query parameter. Status description contains additional details about the error.")
    })
    @GetMapping(value = "/management/batches", produces = "application/json")
    public DataResponse<BatchResponse> getBatches(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        BatchQuery query = managementService.createBatchQuery();

        if (allRequestParams.containsKey("id")) {
            query.batchId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("batchType")) {
            query.batchType(allRequestParams.get("batchType"));
        }
        if (allRequestParams.containsKey("searchKey")) {
            query.searchKey(allRequestParams.get("searchKey"));
        }
        if (allRequestParams.containsKey("searchKey2")) {
            query.searchKey2(allRequestParams.get("searchKey2"));
        }
        if (allRequestParams.containsKey("createTimeBefore")) {
            query.createTimeLowerThan(RequestUtil.getDate(allRequestParams, "createTimeBefore"));
        }
        if (allRequestParams.containsKey("createTimeAfter")) {
            query.createTimeHigherThan(RequestUtil.getDate(allRequestParams, "createTimeAfter"));
        }
        if (allRequestParams.containsKey("completeTimeBefore")) {
            query.completeTimeLowerThan(RequestUtil.getDate(allRequestParams, "completeTimeBefore"));
        }
        if (allRequestParams.containsKey("completeTimeAfter")) {
            query.completeTimeHigherThan(RequestUtil.getDate(allRequestParams, "completeTimeAfter"));
        }
        if (allRequestParams.containsKey("status")) {
            query.status(allRequestParams.get("status"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            query.tenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            query.tenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        if (allRequestParams.containsKey("withoutTenantId")) {
            if (Boolean.parseBoolean(allRequestParams.get("withoutTenantId"))) {
                query.withoutTenantId();
            }
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessBatchInfoWithQuery(query);
        }

        return paginateList(allRequestParams, query, "id", JobQueryProperties.PROPERTIES, restResponseFactory::createBatchResponse);
    }
}
