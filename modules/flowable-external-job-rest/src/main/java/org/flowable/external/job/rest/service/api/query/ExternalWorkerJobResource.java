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
package org.flowable.external.job.rest.service.api.query;

import org.flowable.external.job.rest.service.api.ExternalJobRestResponseFactory;
import org.flowable.external.job.rest.service.api.ExternalWorkerJobBaseResource;
import org.flowable.job.api.ExternalWorkerJob;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Filip Hrisafov
 */
@RestController
@Api(tags = { "Info and Query" })
public class ExternalWorkerJobResource extends ExternalWorkerJobBaseResource {

    protected final ExternalJobRestResponseFactory restResponseFactory;

    public ExternalWorkerJobResource(ExternalJobRestResponseFactory restResponseFactory) {
        this.restResponseFactory = restResponseFactory;
    }

    @ApiOperation(value = "Get a single external worker job", tags = { "Info and Query" })
    @ApiResponses({
            @ApiResponse(code = 200, message = "Indicates the requested job was returned."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights access the job."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
    })
    @GetMapping(value = "/jobs/{jobId}", produces = "application/json")
    public ExternalWorkerJobResponse getExternalWorkerJob(@PathVariable String jobId) {
        ExternalWorkerJob job = getExternalWorkerJobById(jobId);

        return restResponseFactory.createExternalWorkerJobResponse(job);
    }
}
