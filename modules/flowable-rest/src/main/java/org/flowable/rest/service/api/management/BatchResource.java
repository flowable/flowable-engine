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

import org.flowable.batch.api.Batch;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@RestController
@Api(tags = { "Batches" }, authorizations = { @Authorization(value = "basicAuth") })
public class BatchResource extends BatchBaseResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    
    @ApiOperation(value = "Get a single batch", tags = { "Batches" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the batch exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested batch does not exist.")
    })
    @GetMapping(value = "/management/batches/{batchId}", produces = "application/json")
    public BatchResponse getBatch(@ApiParam(name = "batchId") @PathVariable String batchId) {
        Batch batch = getBatchById(batchId);
        return restResponseFactory.createBatchResponse(batch);
    }
    
    @ApiOperation(value = "Get the batch document", tags = { "Batches" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the requested batch was found and the batch document has been returned. The response contains the raw batch document and always has a Content-type of application/json."),
            @ApiResponse(code = 404, message = "Indicates the requested batch was not found or the job does not have a batch document. Status-description contains additional information about the error.")
    })
    @GetMapping("/management/batches/{batchId}/batch-document")
    public String getBatchDocument(@ApiParam(name = "batchId") @PathVariable String batchId, HttpServletResponse response) {
        Batch batch = getBatchById(batchId);

        String batchDocument = managementService.getBatchDocument(batchId);

        if (batchDocument == null) {
            throw new FlowableObjectNotFoundException("Batch with id '" + batch.getId() + "' does not have a batch document.", String.class);
        }

        response.setContentType("application/json");
        return batchDocument;
    }

    @ApiOperation(value = "Delete a batch", tags = { "Batches" }, nickname = "deleteBatch", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the batch was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested batch was not found.")
    })
    @DeleteMapping("/management/batches/{batchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(@ApiParam(name = "batchId") @PathVariable String batchId) {
        Batch batch = getBatchById(batchId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteBatch(batch);
        }
        
        try {
            managementService.deleteBatch(batchId);
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a batch with id '" + batchId + "'.", Batch.class);
        }
    }
}
