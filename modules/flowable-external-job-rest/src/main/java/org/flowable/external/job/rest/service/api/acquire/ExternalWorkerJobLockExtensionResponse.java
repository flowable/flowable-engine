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

import java.util.Date;

import org.flowable.common.rest.util.DateToStringSerializer;

import tools.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Response returned after extending an external-worker job lock")
public class ExternalWorkerJobLockExtensionResponse {

    @ApiModelProperty(value = "The external-worker job id", required = true)
    protected final String jobId;

    @ApiModelProperty(value = "The worker that owns the renewed lock", required = true)
    protected final String workerId;

    @ApiModelProperty(value = "The new lock expiration time", required = true)
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected final Date lockExpirationTime;

    public ExternalWorkerJobLockExtensionResponse(String jobId, String workerId, Date lockExpirationTime) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.lockExpirationTime = lockExpirationTime;
    }

    public String getJobId() {
        return jobId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public Date getLockExpirationTime() {
        return lockExpirationTime;
    }
}
