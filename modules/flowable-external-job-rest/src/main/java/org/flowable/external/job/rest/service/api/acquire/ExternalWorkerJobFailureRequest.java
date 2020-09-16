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
package org.flowable.external.job.rest.service.api.acquire;

import java.time.Duration;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author Filip Hrisafov
 */
@ApiModel(description = "Request that is used for failing external worker jobs")
public class ExternalWorkerJobFailureRequest {

    @ApiModelProperty(value = "The id of the external worker that reports the failure. Must match the id of the worker who has most recently locked the job.", required = true)
    protected String workerId;

    @ApiModelProperty(value = "Error message for the failure", example = "Database not available")
    protected String errorMessage;

    @ApiModelProperty(value = "Details for the failure")
    protected String errorDetails;

    @ApiModelProperty(value = "The new number of retries. If not set it will be reduced by 1. If 0 the job will be moved ot the dead letter table and would no longer be available for acquiring.")
    protected Integer retries;

    @ApiModelProperty(value = "The timeout after which the job should be made available again. ISO-8601 duration format PnDTnHnMn.nS with days considered to be exactly 24 hours.",
            dataType = "string", example = "PT20M")
    protected Duration retryTimeout;

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Duration getRetryTimeout() {
        return retryTimeout;
    }

    public void setRetryTimeout(Duration retryTimeout) {
        this.retryTimeout = retryTimeout;
    }
}
