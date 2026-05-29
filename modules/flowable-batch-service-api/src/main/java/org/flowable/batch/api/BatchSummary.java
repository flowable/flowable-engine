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
package org.flowable.batch.api;

import java.util.Date;

public class BatchSummary {

    protected String batchId;
    protected String batchType;
    protected Date createTime;
    protected String status;
    protected Date completeTime;
    protected String tenantId;
    protected long totalBatchParts;
    protected long completedBatchParts;
    protected long successBatchParts;
    protected long failedBatchParts;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public long getTotalBatchParts() {
        return totalBatchParts;
    }

    public void setTotalBatchParts(long totalBatchParts) {
        this.totalBatchParts = totalBatchParts;
    }

    public long getCompletedBatchParts() {
        return completedBatchParts;
    }

    public void setCompletedBatchParts(long completedBatchParts) {
        this.completedBatchParts = completedBatchParts;
    }

    public long getSuccessBatchParts() {
        return successBatchParts;
    }

    public void setSuccessBatchParts(long successBatchParts) {
        this.successBatchParts = successBatchParts;
    }

    public long getFailedBatchParts() {
        return failedBatchParts;
    }

    public void setFailedBatchParts(long failedBatchParts) {
        this.failedBatchParts = failedBatchParts;
    }
}
