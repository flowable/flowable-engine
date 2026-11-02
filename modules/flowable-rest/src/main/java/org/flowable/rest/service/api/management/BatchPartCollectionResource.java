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

import java.util.List;
import java.util.Map;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.ManagementService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Api(tags = { "Batch parts" }, authorizations = { @Authorization(value = "basicAuth") })
public class BatchPartCollectionResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ManagementService managementService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List batch parts", tags = { "Batches" }, nickname = "listBatchesPart")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "status", dataType = "string", value = "Only return batch parts for the given status", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested batch parts were returned."),
            @ApiResponse(code = 400, message = "Indicates an illegal value has been used in a url query parameter. Status description contains additional details about the error.")
    })
    @GetMapping(value = "/management/batches/{batchId}/batch-parts", produces = "application/json")
    public List<BatchPartResponse> getBatches(@PathVariable String batchId,
                    @ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        
        Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
        if (batch == null) {
            throw new FlowableObjectNotFoundException("No batch found for id " + batchId);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessBatchPartInfoOfBatch(batch);
        }
        
        List<BatchPart> batchParts = null;
        if (allRequestParams.containsKey("status")) {
            batchParts = managementService.findBatchPartsByBatchIdAndStatus(batchId, allRequestParams.get("status"));
        } else {
            batchParts = managementService.findBatchPartsByBatchId(batchId);
        }

        return restResponseFactory.createBatchPartResponse(batchParts);
    }
}
