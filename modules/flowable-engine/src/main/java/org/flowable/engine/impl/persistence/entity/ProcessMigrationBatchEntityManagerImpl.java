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
        ProcessMigrationBatchEntityImpl processMigrationBatchEntity = (ProcessMigrationBatchEntityImpl) processMigrationBatchDataManager.create();
        processMigrationBatchEntity.setBatchType(ProcessMigrationBatchEntityImpl.MIGRATION_TYPE);
        processMigrationBatchEntity.setCreateTime(getClock().getCurrentTime());
        processMigrationBatchEntity.setMigrationDocumentJson(processInstanceMigrationDocument.asJsonString());
        insert(processMigrationBatchEntity);
        return processMigrationBatchEntity;
    }

    @Override
    public ProcessMigrationBatchEntity insertBatchForProcessMigrationValidation(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        ProcessMigrationBatchEntityImpl processMigrationBatchEntity = (ProcessMigrationBatchEntityImpl) processMigrationBatchDataManager.create();
        processMigrationBatchEntity.setBatchType(ProcessMigrationBatchEntityImpl.VALIDATION_TYPE);
        processMigrationBatchEntity.setCreateTime(getClock().getCurrentTime());
        processMigrationBatchEntity.setMigrationDocumentJson(processInstanceMigrationDocument.asJsonString());
        insert(processMigrationBatchEntity);
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
                ProcessMigrationBatchEntityImpl childImpl = (ProcessMigrationBatchEntityImpl) child;
                if (childImpl.getResultDataRefId() != null && childImpl.getResultDataRefId().getId() != null) {
                    ByteArrayEntity entity = childImpl.getResultDataRefId().getEntity();
                    childImpl.getResultDataRefId().delete();
                }
                delete((ProcessMigrationBatchEntity) child);
            }
        }

        ProcessMigrationBatchEntityImpl parentBatchImpl = (ProcessMigrationBatchEntityImpl) parentBatch;
        if (parentBatchImpl.getResultDataRefId() != null && parentBatchImpl.getResultDataRefId().getId() != null) {
            ByteArrayEntity entity = parentBatchImpl.getResultDataRefId().getEntity();
            parentBatchImpl.getResultDataRefId().delete();
        }
        if (parentBatchImpl.getParamDataRefId() != null && parentBatchImpl.getParamDataRefId().getId() != null) {
            ByteArrayEntity entity = parentBatchImpl.getParamDataRefId().getEntity();
            parentBatchImpl.getParamDataRefId().delete();
        }

        delete(parentBatch);
    }

    //
    //    void deleteBatchAndResultResource(ProcessMigrationBatchEntity batchEntity);
    //
    //    void deleteBatchAndAllResources(ProcessMigrationBatchEntity batchEntity);
    //
    //
    //    @Override
    //    public void deleteBatchAndResultResource(ProcessMigrationBatchEntity batchEntity) {
    //        ProcessMigrationBatchEntityImpl batchEntityImpl = (ProcessMigrationBatchEntityImpl) batchEntity;
    //        getDbSqlSession().delete(batchEntityImpl);
    //        if (batchEntityImpl.getResultDataRefId() != null && batchEntityImpl.getResultDataRefId().getId() != null) {
    //            batchEntityImpl.getResultDataRefId().delete();
    //            //            getDbSqlSession().delete(batchEntityImpl.getResultDataRefId().getEntity());
    //        }
    //    }
    //
    //    @Override
    //    public void deleteBatchAndAllResources(ProcessMigrationBatchEntity batchEntity) {
    //        ProcessMigrationBatchEntityImpl batchEntityImpl = (ProcessMigrationBatchEntityImpl) batchEntity;
    //        getDbSqlSession().delete(batchEntityImpl);
    //        if (batchEntityImpl.getResultDataRefId() != null && batchEntityImpl.getResultDataRefId().getId() != null) {
    //            batchEntityImpl.getResultDataRefId().delete();
    //        }
    //        if (batchEntityImpl.getParamDataRefId() != null && batchEntityImpl.getParamDataRefId().getId() != null) {
    //            batchEntityImpl.getParamDataRefId().delete();
    //        }
    //    }

    //TODO WIP - Update??

    //TODO WIP - Delete by parent id??

    //TODO WIP - Select all parents??

    //TODO WIP - Select by parent id??

    //
    //    @Override
    //    public void deleteProcessDefinitionInfo(String processDefinitionId) {
    //        ProcessDefinitionInfoEntity processDefinitionInfo = findProcessDefinitionInfoByProcessDefinitionId(processDefinitionId);
    //        if (processDefinitionInfo != null) {
    //            delete(processDefinitionInfo);
    //            deleteInfoJson(processDefinitionInfo);
    //        }
    //    }
    //
    //    @Override
    //    public void updateInfoJson(String id, byte[] json) {
    //        ProcessDefinitionInfoEntity processDefinitionInfo = findById(id);
    //        if (processDefinitionInfo != null) {
    //            ByteArrayRef ref = new ByteArrayRef(processDefinitionInfo.getInfoJsonId());
    //            ref.setValue("json", json);
    //
    //            if (processDefinitionInfo.getInfoJsonId() == null) {
    //                processDefinitionInfo.setInfoJsonId(ref.getId());
    //                updateProcessDefinitionInfo(processDefinitionInfo);
    //            }
    //        }
    //    }
    //
    //    @Override
    //    public void deleteInfoJson(ProcessDefinitionInfoEntity processDefinitionInfo) {
    //        if (processDefinitionInfo.getInfoJsonId() != null) {
    //            ByteArrayRef ref = new ByteArrayRef(processDefinitionInfo.getInfoJsonId());
    //            ref.delete();
    //        }
    //    }
    //

    //
    //    @Override
    //    public byte[] findInfoJsonById(String infoJsonId) {
    //        ByteArrayRef ref = new ByteArrayRef(infoJsonId);
    //        return ref.getBytes();
    //    }
    //
}