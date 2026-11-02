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

import java.util.Date;

import org.flowable.common.rest.util.DateToStringSerializer;

import tools.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

public class BatchSummaryResponse {

    protected String id;
    protected String url;
    protected String batchType;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date createTime;
    protected String status;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date completeTime;
    protected String tenantId;
    protected long totalBatchParts;
    protected long completedBatchParts;
    protected long successBatchParts;
    protected long failedBatchParts;

    @ApiModelProperty(example = "8")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/management/batches/8")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "processMigration")
    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    @ApiModelProperty(example = "2020-06-03T22:05:05.474+0000")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @ApiModelProperty(example = "completed")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ApiModelProperty(example = "2020-06-03T22:05:05.474+0000")
    public Date getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "10")
    public long getTotalBatchParts() {
        return totalBatchParts;
    }

    public void setTotalBatchParts(long totalBatchParts) {
        this.totalBatchParts = totalBatchParts;
    }

    @ApiModelProperty(example = "8")
    public long getCompletedBatchParts() {
        return completedBatchParts;
    }

    public void setCompletedBatchParts(long completedBatchParts) {
        this.completedBatchParts = completedBatchParts;
    }

    @ApiModelProperty(example = "7")
    public long getSuccessBatchParts() {
        return successBatchParts;
    }

    public void setSuccessBatchParts(long successBatchParts) {
        this.successBatchParts = successBatchParts;
    }

    @ApiModelProperty(example = "1")
    public long getFailedBatchParts() {
        return failedBatchParts;
    }

    public void setFailedBatchParts(long failedBatchParts) {
        this.failedBatchParts = failedBatchParts;
    }
}
