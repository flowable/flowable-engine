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
package org.flowable.engine.impl.persistence.entity;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.engine.runtime.ProcessMigrationBatch;

/**
 * @author Dennis Federico
 */
public class ProcessMigrationBatchEntityImpl extends AbstractEntity implements ProcessMigrationBatchEntity, Serializable {

    private static final long serialVersionUID = 1L;

    //TODO WIP - Use an ENUM instead?
    public static final String VALIDATION_TYPE = "processMigrationValidation";
    public static final String MIGRATION_TYPE = "processMigration";
    protected static final String MIGRATION_DOCUMENT_JSON_LABEL = "migrationDocumentJson";
    protected static final String BATCH_RESULT_LABEL = "batchResult";
    protected String parentBatchId;
    protected String batchType;
    protected String processInstanceId;
    protected Date createTime;
    protected Date completeTime;
    //TODO WIP - Use the Id (String) instead?
    protected ByteArrayRef paramDataRefId;
    protected ByteArrayRef resultDataRefId;
    protected List<ProcessMigrationBatch> batchChildren;

    @Override
    public String getIdPrefix() {
        return BpmnEngineEntityConstants.BPMN_ENGINE_ID_PREFIX;
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("resultDataRefId", resultDataRefId);
        persistentState.put("completeTime", completeTime);
        return persistentState;
    }

    @Override
    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    @Override
    public String getParentBatchId() {
        return parentBatchId;
    }

    public void setParentBatchId(String parentBatchId) {
        this.parentBatchId = parentBatchId;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date time) {
        this.createTime = time;
    }

    @Override
    public Date getCompleteTime() {
        if (completeTime != null) {
            return completeTime;
        }

        if (batchChildren != null && !batchChildren.isEmpty()) {
            long maxDate = Long.MIN_VALUE;
            for (ProcessMigrationBatch child : batchChildren) {
                if (!child.isCompleted()) {
                    return null;
                }
                maxDate = Long.max(maxDate, child.getCompleteTime().getTime());
            }
            return new Date(maxDate);
        }
        return null;
    }

    @Override
    public boolean isCompleted() {
        if (completeTime != null) {
            return true;
        }

        if (batchChildren != null && !batchChildren.isEmpty()) {
            return batchChildren.stream().allMatch(ProcessMigrationBatch::isCompleted);
        }
        return false;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public ByteArrayRef getParamDataRefId() {
        return paramDataRefId;
    }

    public void setParamDataRefId(ByteArrayRef paramDataRefId) {
        this.paramDataRefId = paramDataRefId;
    }

    public ByteArrayRef getResultDataRefId() {
        return resultDataRefId;
    }

    @Override
    public String getMigrationDocumentJson() {
        if (paramDataRefId != null) {
            byte[] bytes = paramDataRefId.getBytes();
            if (bytes != null) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public String getResult() {
        if (resultDataRefId != null && resultDataRefId.getEntity() != null) {
            byte[] bytes = resultDataRefId.getEntity().getBytes();
            if (bytes != null) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public void setMigrationDocumentJson(String migrationDocumentJson) {
        this.paramDataRefId = setByteArrayRef(this.paramDataRefId, MIGRATION_DOCUMENT_JSON_LABEL, migrationDocumentJson);
    }

    @Override
    public void completeWithResult(Date completeTime, String result) {
        this.completeTime = completeTime;
        this.resultDataRefId = setByteArrayRef(this.resultDataRefId, BATCH_RESULT_LABEL, result);
    }

    @Override
    public void complete(Date completeTime) {
        this.completeTime = completeTime;
    }

    @Override
    public List<ProcessMigrationBatch> getBatchChildren() {
        return batchChildren;
    }

    public void addBatchChild(ProcessMigrationBatchEntity child) {
        if (batchChildren == null) {
            batchChildren = new ArrayList<>();
        }
        batchChildren.add(child);
    }

    private static ByteArrayRef setByteArrayRef(ByteArrayRef byteArrayRef, String name, String value) {
        if (byteArrayRef == null) {
            byteArrayRef = new ByteArrayRef();
        }
        byte[] bytes = null;
        if (value != null) {
            bytes = value.getBytes(StandardCharsets.UTF_8);
        }
        byteArrayRef.setValue(name, bytes);
        return byteArrayRef;
    }

}

