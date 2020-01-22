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
package org.flowable.batch.service.impl.persistence.entity;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BatchPartEntityImpl extends AbstractBatchServiceEntity implements BatchPartEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String BATCH_RESULT_LABEL = "batchPartResult";

    protected String batchType;
    protected String batchId;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String batchSearchKey;
    protected String batchSearchKey2;
    protected Date createTime;
    protected Date completeTime;
    protected String status;
    protected BatchByteArrayRef resultDocRefId;
    protected String tenantId;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("batchId", batchId);
        persistentState.put("batchType", batchType);
        persistentState.put("scopeId", scopeId);
        persistentState.put("subScopeId", subScopeId);
        persistentState.put("scopeType", scopeType);
        persistentState.put("createTime", createTime);
        persistentState.put("completeTime", completeTime);
        persistentState.put("batchSearchKey", batchSearchKey);
        persistentState.put("batchSearchKey2", batchSearchKey2);
        persistentState.put("status", status);
        persistentState.put("tenantId", tenantId);
        
        if (resultDocRefId != null) {
            persistentState.put("resultDocRefId", resultDocRefId);
        }

        return persistentState;
    }

    @Override
    public String getBatchType() {
        return batchType;
    }

    @Override
    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    @Override
    public String getBatchId() {
        return batchId;
    }

    @Override
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date time) {
        this.createTime = time;
    }

    @Override
    public Date getCompleteTime() {
        return completeTime;
    }

    @Override
    public void setCompleteTime(Date time) {
        this.completeTime = time;
    }

    @Override
    public boolean isCompleted() {
        return completeTime != null;
    }

    @Override
    public String getScopeId() {
        return scopeId;
    }

    @Override
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getSubScopeId() {
        return subScopeId;
    }

    @Override
    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getBatchSearchKey() {
        return batchSearchKey;
    }

    @Override
    public void setBatchSearchKey(String batchSearchKey) {
        this.batchSearchKey = batchSearchKey;
    }

    @Override
    public String getBatchSearchKey2() {
        return batchSearchKey2;
    }

    @Override
    public void setBatchSearchKey2(String batchSearchKey2) {
        this.batchSearchKey2 = batchSearchKey2;
    }
    
    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public BatchByteArrayRef getResultDocRefId() {
        return resultDocRefId;
    }

    public void setResultDocRefId(BatchByteArrayRef resultDocRefId) {
        this.resultDocRefId = resultDocRefId;
    }

    @Override
    public String getResultDocumentJson() {
        if (resultDocRefId != null && resultDocRefId.getEntity() != null) {
            byte[] bytes = resultDocRefId.getEntity().getBytes();
            if (bytes != null) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public void setResultDocumentJson(String resultDocumentJson) {
        this.resultDocRefId = setByteArrayRef(this.resultDocRefId, BATCH_RESULT_LABEL, resultDocumentJson);
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    private static BatchByteArrayRef setByteArrayRef(BatchByteArrayRef byteArrayRef, String name, String value) {
        if (byteArrayRef == null) {
            byteArrayRef = new BatchByteArrayRef();
        }
        byte[] bytes = null;
        if (value != null) {
            bytes = value.getBytes(StandardCharsets.UTF_8);
        }
        byteArrayRef.setValue(name, bytes);
        return byteArrayRef;
    }

}
