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
import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Request used to extend an active external-worker job lock")
public class ExternalWorkerJobLockExtensionRequest {

    @ApiModelProperty(value = "The id of the external worker that owns the lock", example = "orderWorker1", required = true)
    protected String workerId;

    @ApiModelProperty(
            value = "The renewed lock duration in ISO-8601 duration format.",
            example = "PT5M", dataType = "string", required = true)
    protected Duration lockDuration;

    @ApiModelProperty(
            value = "The current lock expiration time observed by the worker. Used for optimistic concurrency.",
            example = "2026-07-16T17:30:00.000Z", required = true)
    protected Date expectedLockExpirationTime;

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }

    public Date getExpectedLockExpirationTime() {
        return expectedLockExpirationTime;
    }

    public void setExpectedLockExpirationTime(Date expectedLockExpirationTime) {
        this.expectedLockExpirationTime = expectedLockExpirationTime;
    }
}
