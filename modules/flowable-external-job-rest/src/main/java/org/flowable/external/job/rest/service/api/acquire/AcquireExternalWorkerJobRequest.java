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
@ApiModel(description = "Request that is used for acquiring external worker jobs")
public class AcquireExternalWorkerJobRequest {

    @ApiModelProperty(value = "Acquire jobs with the given topic", example = "order", required = true)
    protected String topic;

    @ApiModelProperty(
            value = "The acquired jobs will be locked with this lock duration. ISO-8601 duration format PnDTnHnMn.nS with days considered to be exactly 24 hours.",
            example = "PT10M", dataType = "string", required = true)
    protected Duration lockDuration;

    @ApiModelProperty(value = "The number of tasks that should be acquired. Default is 1.", example = "1")
    protected int numberOfTasks = 1;

    @ApiModelProperty(value = "The number of retries if an optimistic lock exception occurs during acquiring. Default is 5", example = "10")
    protected int numberOfRetries = 5;

    @ApiModelProperty(value = "The id of the external worker that would be used for locking the job", example = "orderWorker1", required = true)
    protected String workerId;

    @ApiModelProperty(value = "Only acquire jobs with the given scope type", example = "cmmn")
    protected String scopeType;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }

    public int getNumberOfTasks() {
        return numberOfTasks;
    }

    public void setNumberOfTasks(int numberOfTasks) {
        this.numberOfTasks = numberOfTasks;
    }

    public int getNumberOfRetries() {
        return numberOfRetries;
    }

    public void setNumberOfRetries(int numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
}
