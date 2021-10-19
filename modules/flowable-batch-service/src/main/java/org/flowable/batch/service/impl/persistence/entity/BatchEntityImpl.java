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

import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;

public class BatchEntityImpl extends AbstractBatchServiceEntity implements BatchEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String BATCH_DOCUMENT_JSON_LABEL = "batchDocumentJson";

    protected String batchType;
    protected Date createTime;
    protected Date completeTime;
    protected String batchSearchKey;
    protected String batchSearchKey2;
    protected String status;
    protected ByteArrayRef batchDocRefId;
    protected String tenantId;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("batchType", batchType);
        persistentState.put("createTime", createTime);
        persistentState.put("completeTime", completeTime);
        persistentState.put("batchSearchKey", batchSearchKey);
        persistentState.put("batchSearchKey2", batchSearchKey2);
        persistentState.put("status", status);
        persistentState.put("tenantId", tenantId);
        
        if (batchDocRefId != null) {
            persistentState.put("batchDocRefId", batchDocRefId);
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
    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
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
    public ByteArrayRef getBatchDocRefId() {
        return batchDocRefId;
    }

    public void setBatchDocRefId(ByteArrayRef batchDocRefId) {
        this.batchDocRefId = batchDocRefId;
    }

    @Override
    public String getBatchDocumentJson(String engineType) {
        if (batchDocRefId != null) {
            byte[] bytes = batchDocRefId.getBytes(engineType);
            if (bytes != null) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public void setBatchDocumentJson(String batchDocumentJson, String engineType) {
        this.batchDocRefId = setByteArrayRef(this.batchDocRefId, BATCH_DOCUMENT_JSON_LABEL, batchDocumentJson, engineType);
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    protected static ByteArrayRef setByteArrayRef(ByteArrayRef byteArrayRef, String name, String value, String engineType) {
        if (byteArrayRef == null) {
            byteArrayRef = new ByteArrayRef();
        }
        byte[] bytes = null;
        if (value != null) {
            bytes = value.getBytes(StandardCharsets.UTF_8);
        }
        byteArrayRef.setValue(name, bytes, engineType);
        return byteArrayRef;
    }

}

