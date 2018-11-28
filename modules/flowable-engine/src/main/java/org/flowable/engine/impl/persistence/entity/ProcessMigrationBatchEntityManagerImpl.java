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

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.ProcessMigrationBatchDataManager;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.runtime.ProcessMigrationBatch;

/**
 * @author Dennis Federico
 */
public class ProcessMigrationBatchEntityManagerImpl extends AbstractEntityManager<ProcessMigrationBatchEntity> implements ProcessMigrationBatchEntityManager {

    protected ProcessMigrationBatchDataManager processMigrationBatchDataManager;

    public ProcessMigrationBatchEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ProcessMigrationBatchDataManager processDefinitionInfoDataManager) {
        super(processEngineConfiguration);
        this.processMigrationBatchDataManager = processDefinitionInfoDataManager;
    }

    @Override
    protected DataManager<ProcessMigrationBatchEntity> getDataManager() {
        return processMigrationBatchDataManager;
    }

    @Override
    public ProcessMigrationBatchEntity insertBatchForProcessMigration(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        ProcessMigrationBatchEntity processMigrationBatchEntity = prepareProcessMigrationBatch(processInstanceMigrationDocument, ProcessMigrationBatchEntityImpl.MIGRATION_TYPE);
        insert(processMigrationBatchEntity);
        return processMigrationBatchEntity;
    }

    @Override
    public ProcessMigrationBatchEntity insertBatchForProcessMigrationValidation(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        ProcessMigrationBatchEntity processMigrationBatchEntity = prepareProcessMigrationBatch(processInstanceMigrationDocument, ProcessMigrationBatchEntityImpl.VALIDATION_TYPE);
        insert(processMigrationBatchEntity);
        return processMigrationBatchEntity;
    }

    protected ProcessMigrationBatchEntity prepareProcessMigrationBatch(ProcessInstanceMigrationDocument processInstanceMigrationDocument, String processMigrationBatchType) {
        ProcessMigrationBatchEntityImpl processMigrationBatchEntity = (ProcessMigrationBatchEntityImpl) processMigrationBatchDataManager.create();
        processMigrationBatchEntity.setBatchType(processMigrationBatchType);
        processMigrationBatchEntity.setCreateTime(getClock().getCurrentTime());
        processMigrationBatchEntity.setMigrationDocumentJson(processInstanceMigrationDocument.asJsonString());
        return processMigrationBatchEntity;
    }

    @Override
    public ProcessMigrationBatchEntity insertBatchChild(ProcessMigrationBatchEntity parentBatch, String processInstanceId) {
        ProcessMigrationBatchEntityImpl childBatchEntity = (ProcessMigrationBatchEntityImpl) processMigrationBatchDataManager.create();
        childBatchEntity.setParentBatchId(parentBatch.getId());
        childBatchEntity.setBatchType(parentBatch.getBatchType());
        childBatchEntity.setCreateTime(getClock().getCurrentTime());
        childBatchEntity.setProcessInstanceId(processInstanceId);
        childBatchEntity.setParamDataRefId(((ProcessMigrationBatchEntityImpl) parentBatch).getParamDataRefId());
        insert(childBatchEntity);
        return childBatchEntity;
    }

    @Override
    public void deleteParentBatchAndChildrenAndResources(String parentBatchId) {
        ProcessMigrationBatchEntity parentBatch = processMigrationBatchDataManager.findById(parentBatchId);
        if (parentBatch.getBatchChildren() != null) {
            for (ProcessMigrationBatch child : parentBatch.getBatchChildren()) {
                deleteBatchEntityAndResources((ProcessMigrationBatchEntityImpl) child);
            }
        }

        deleteBatchEntityAndResources((ProcessMigrationBatchEntityImpl) parentBatch);
    }

    protected void deleteBatchEntityAndResources(ProcessMigrationBatchEntityImpl batchEntity) {

        if (batchEntity.getResultDataRefId() != null && batchEntity.getResultDataRefId().getId() != null) {
            ByteArrayEntity entity = batchEntity.getResultDataRefId().getEntity();
            batchEntity.getResultDataRefId().delete();
        }
        if (batchEntity.getParamDataRefId() != null && batchEntity.getParamDataRefId().getId() != null) {
            ByteArrayEntity entity = batchEntity.getParamDataRefId().getEntity();
            batchEntity.getParamDataRefId().delete();
        }

        delete(batchEntity);
    }
}