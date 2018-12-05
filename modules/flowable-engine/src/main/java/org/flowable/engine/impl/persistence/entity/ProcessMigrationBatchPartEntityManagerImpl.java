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
import org.flowable.engine.impl.persistence.entity.data.ProcessMigrationBatchPartDataManager;

/**
 * @author Dennis Federico
 */
public class ProcessMigrationBatchPartEntityManagerImpl extends AbstractEntityManager<ProcessMigrationBatchPartEntity> implements ProcessMigrationBatchPartEntityManager {

    protected ProcessMigrationBatchPartDataManager processMigrationBatchPartDataManager;

    public ProcessMigrationBatchPartEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ProcessMigrationBatchPartDataManager processMigrationBatchPartDataManager) {
        super(processEngineConfiguration);
        this.processMigrationBatchPartDataManager = processMigrationBatchPartDataManager;
    }

    @Override
    protected DataManager<ProcessMigrationBatchPartEntity> getDataManager() {
        return processMigrationBatchPartDataManager;
    }

    @Override
    public ProcessMigrationBatchPartEntity insertBatchPart(ProcessMigrationBatchEntity parentBatch, String processInstanceId) {
        ProcessMigrationBatchPartEntityImpl childBatchEntity = (ProcessMigrationBatchPartEntityImpl) processMigrationBatchPartDataManager.create();
        childBatchEntity.setParentBatchId(parentBatch.getId());
        childBatchEntity.setBatchType(parentBatch.getBatchType());
        childBatchEntity.setProcessInstanceId(processInstanceId);
        childBatchEntity.setSourceProcessDefinitionId(parentBatch.getSourceProcessDefinitionId());
        childBatchEntity.setTargetProcessDefinitionId(parentBatch.getTargetProcessDefinitionId());
        childBatchEntity.setCreateTime(getClock().getCurrentTime());
        insert(childBatchEntity);
        return childBatchEntity;
    }

    @Override
    public void deleteBatchPartEntityAndResources(ProcessMigrationBatchPartEntity batchPartEntity) {

        ByteArrayRef resultDataRefId = ((ProcessMigrationBatchPartEntityImpl) batchPartEntity).getResultDataRefId();

        if (resultDataRefId != null && resultDataRefId.getId() != null) {
            resultDataRefId.delete();
        }

        delete(batchPartEntity);
    }
}