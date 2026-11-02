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

import jakarta.servlet.http.HttpServletResponse;

import org.flowable.batch.api.BatchPart;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
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

@RestController
@Api(tags = { "Batch parts" }, authorizations = { @Authorization(value = "basicAuth") })
public class BatchPartResource extends BatchPartBaseResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    
    @ApiOperation(value = "Get a single batch part", tags = { "Batch parts" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the batch part exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested batch part does not exist.")
    })
    @GetMapping(value = "/management/batch-parts/{batchPartId}", produces = "application/json")
    public BatchPartResponse getBatchPart(@ApiParam(name = "batchPartId") @PathVariable String batchPartId) {
        BatchPart batchPart = getBatchPartById(batchPartId);
        return restResponseFactory.createBatchPartResponse(batchPart);
    }
    
    @ApiOperation(value = "Get the batch part document", tags = { "Batches" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested batch part was found and the batch part document has been returned. The response contains the raw batch part document and always has a Content-type of application/json."),
            @ApiResponse(code = 404, message = "Indicates the requested batch part was not found or the job does not have a batch part document. Status-description contains additional information about the error.")
    })
    @GetMapping("/management/batch-parts/{batchPartId}/batch-part-document")
    public String getBatchPartDocument(@ApiParam(name = "batchPartId") @PathVariable String batchPartId, HttpServletResponse response) {
        BatchPart batchPart = getBatchPartById(batchPartId);

        String batchPartDocument = managementService.getBatchPartDocument(batchPartId);

        if (batchPartDocument == null) {
            throw new FlowableObjectNotFoundException("Batch part with id '" + batchPart.getId() + "' does not have a batch part document.", String.class);
        }

        response.setContentType("application/json");
        return batchPartDocument;
    }
}
