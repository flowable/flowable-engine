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
import org.flowable.engine.runtime.ProcessMigrationBatchPart;

/**
 * @author Dennis Federico
 */
public class ProcessMigrationBatchEntityManagerImpl extends AbstractEntityManager<ProcessMigrationBatchEntity> implements ProcessMigrationBatchEntityManager {

    protected ProcessMigrationBatchDataManager processMigrationBatchDataManager;

    public ProcessMigrationBatchEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ProcessMigrationBatchDataManager processMigrationBatchDataManager) {
        super(processEngineConfiguration);
        this.processMigrationBatchDataManager = processMigrationBatchDataManager;
    }

    @Override
    protected DataManager<ProcessMigrationBatchEntity> getDataManager() {
        return processMigrationBatchDataManager;
    }

    @Override
    public ProcessMigrationBatchEntity insertBatchForProcessMigration(ProcessInstanceMigrationDocument processInstanceMigrationDocument, String sourceProcDefId, String targetProcDefId) {
        ProcessMigrationBatchEntity processMigrationBatchEntity = prepareProcessMigrationBatch(processInstanceMigrationDocument, sourceProcDefId, targetProcDefId, ProcessMigrationBatchEntityImpl.MIGRATION_TYPE);
        insert(processMigrationBatchEntity);
        return processMigrationBatchEntity;
    }

    @Override
    public ProcessMigrationBatchEntity insertBatchForProcessMigrationValidation(ProcessInstanceMigrationDocument processInstanceMigrationDocument, String sourceProcDefId, String targetProcDefId) {
        ProcessMigrationBatchEntity processMigrationBatchEntity = prepareProcessMigrationBatch(processInstanceMigrationDocument, sourceProcDefId, targetProcDefId, ProcessMigrationBatchEntityImpl.VALIDATION_TYPE);
        insert(processMigrationBatchEntity);
        return processMigrationBatchEntity;
    }

    protected ProcessMigrationBatchEntity prepareProcessMigrationBatch(ProcessInstanceMigrationDocument processInstanceMigrationDocument, String sourceProcDefId, String targetProcDefId, String processMigrationBatchType) {
        ProcessMigrationBatchEntityImpl processMigrationBatchEntity = (ProcessMigrationBatchEntityImpl) processMigrationBatchDataManager.create();
        processMigrationBatchEntity.setBatchType(processMigrationBatchType);
        processMigrationBatchEntity.setSourceProcessDefinitionId(sourceProcDefId);
        processMigrationBatchEntity.setTargetProcessDefinitionId(targetProcDefId);
        processMigrationBatchEntity.setCreateTime(getClock().getCurrentTime());
        processMigrationBatchEntity.setMigrationDocumentJson(processInstanceMigrationDocument.asJsonString());
        return processMigrationBatchEntity;
    }

    @Override
    public void deleteParentBatchAndPartsAndResources(String parentBatchId) {
        ProcessMigrationBatchPartEntityManager processMigrationBatchPartEntityManager = processEngineConfiguration.getProcessMigrationBatchPartEntityManager();
        ProcessMigrationBatchEntity parentBatch = processMigrationBatchDataManager.findById(parentBatchId);
        if (parentBatch.getBatchParts() != null) {
            for (ProcessMigrationBatchPart child : parentBatch.getBatchParts()) {
                processMigrationBatchPartEntityManager.deleteBatchPartEntityAndResources((ProcessMigrationBatchPartEntity) child);
            }
        }

        ByteArrayRef migrationDocRefId = ((ProcessMigrationBatchEntityImpl) parentBatch).getMigrationDocRefId();

        if (migrationDocRefId != null && migrationDocRefId.getId() != null) {
            migrationDocRefId.delete();
        }

        delete(parentBatch);
    }
}