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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

/**
 * @author Dennis Federico
 */
public class ProcessMigrationBatchPartEntityImpl extends AbstractEntity implements ProcessMigrationBatchPartEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String BATCH_RESULT_LABEL = "batchPartResult";

    protected String batchType;
    protected String parentBatchId;
    protected String processInstanceId;
    protected String sourceProcessDefinitionId;
    protected String targetProcessDefinitionId;
    protected Date createTime;
    protected Date completeTime;
    protected ByteArrayRef resultDataRefId;

    @Override
    public String getIdPrefix() {
        return BpmnEngineEntityConstants.BPMN_ENGINE_ID_PREFIX;
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("completeTime", completeTime);
        persistentState.put("resultDataRefId", resultDataRefId);
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
        return completeTime;
    }

    public void setCompleteTime(Date time) {
        this.completeTime = time;
    }

    @Override
    public boolean isCompleted() {
        return completeTime != null;
    }

    @Override
    public String getSourceProcessDefinitionId() {
        return sourceProcessDefinitionId;
    }

    public void setSourceProcessDefinitionId(String sourceProcessDefinitionId) {
        this.sourceProcessDefinitionId = sourceProcessDefinitionId;
    }

    @Override
    public String getTargetProcessDefinitionId() {
        return targetProcessDefinitionId;
    }

    public void setTargetProcessDefinitionId(String targetProcessDefinitionId) {
        this.targetProcessDefinitionId = targetProcessDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public ByteArrayRef getResultDataRefId() {
        return resultDataRefId;
    }

    public void setResultDataRefId(ByteArrayRef resultDataRefId) {
        this.resultDataRefId = resultDataRefId;
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
    public void complete(Date completeTime, String result) {
        this.completeTime = completeTime;
        this.resultDataRefId = setByteArrayRef(this.resultDataRefId, BATCH_RESULT_LABEL, result);
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
