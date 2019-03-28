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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.impl.HistoricDetailQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.HistoricDetailDataManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricDetailEntityManagerImpl extends AbstractEntityManager<HistoricDetailEntity> implements HistoricDetailEntityManager {

    protected HistoricDetailDataManager historicDetailDataManager;

    public HistoricDetailEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, HistoricDetailDataManager historicDetailDataManager) {
        super(processEngineConfiguration);
        this.historicDetailDataManager = historicDetailDataManager;
    }

    @Override
    protected DataManager<HistoricDetailEntity> getDataManager() {
        return historicDetailDataManager;
    }

    @Override
    public HistoricFormPropertyEntity insertHistoricFormPropertyEntity(ExecutionEntity execution,
        String propertyId, String propertyValue, String taskId, Date createTime) {

        HistoricFormPropertyEntity historicFormPropertyEntity = historicDetailDataManager.createHistoricFormProperty();
        historicFormPropertyEntity.setProcessInstanceId(execution.getProcessInstanceId());
        historicFormPropertyEntity.setExecutionId(execution.getId());
        historicFormPropertyEntity.setTaskId(taskId);
        historicFormPropertyEntity.setPropertyId(propertyId);
        historicFormPropertyEntity.setPropertyValue(propertyValue);
        historicFormPropertyEntity.setTime(createTime);

        ActivityInstanceEntity activityInstance = getActivityInstanceEntityManager().findUnfinishedActivityInstance(execution);
        String activityInstanceId;
        if (activityInstance != null) {
            activityInstanceId = activityInstance.getId();
        } else {
            throw new FlowableException("ActivityInstance not found for execution "+execution.getId());
        }
        historicFormPropertyEntity.setActivityInstanceId(activityInstanceId);

        insert(historicFormPropertyEntity);
        return historicFormPropertyEntity;
    }

    @Override
    public HistoricDetailVariableInstanceUpdateEntity copyAndInsertHistoricDetailVariableInstanceUpdateEntity(VariableInstanceEntity variableInstance,
        Date createTime) {
        HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = historicDetailDataManager.createHistoricDetailVariableInstanceUpdate();
        historicVariableUpdate.setProcessInstanceId(variableInstance.getProcessInstanceId());
        historicVariableUpdate.setExecutionId(variableInstance.getExecutionId());
        historicVariableUpdate.setTaskId(variableInstance.getTaskId());
        historicVariableUpdate.setTime(createTime);
        historicVariableUpdate.setRevision(variableInstance.getRevision());
        historicVariableUpdate.setName(variableInstance.getName());
        historicVariableUpdate.setVariableType(variableInstance.getType());
        historicVariableUpdate.setTextValue(variableInstance.getTextValue());
        historicVariableUpdate.setTextValue2(variableInstance.getTextValue2());
        historicVariableUpdate.setDoubleValue(variableInstance.getDoubleValue());
        historicVariableUpdate.setLongValue(variableInstance.getLongValue());

        if (variableInstance.getBytes() != null) {
            historicVariableUpdate.setBytes(variableInstance.getBytes());
        }

        insert(historicVariableUpdate);
        return historicVariableUpdate;
    }

    @Override
    public void delete(HistoricDetailEntity entity, boolean fireDeleteEvent) {
        super.delete(entity, fireDeleteEvent);

        if (entity instanceof HistoricDetailVariableInstanceUpdateEntity) {
            HistoricDetailVariableInstanceUpdateEntity historicDetailVariableInstanceUpdateEntity = ((HistoricDetailVariableInstanceUpdateEntity) entity);
            if (historicDetailVariableInstanceUpdateEntity.getByteArrayRef() != null) {
                historicDetailVariableInstanceUpdateEntity.getByteArrayRef().delete();
            }
        }
    }

    @Override
    public void deleteHistoricDetailsByProcessInstanceId(String historicProcessInstanceId) {
        if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            List<HistoricDetailEntity> historicDetails = historicDetailDataManager.findHistoricDetailsByProcessInstanceId(historicProcessInstanceId);

            for (HistoricDetailEntity historicDetail : historicDetails) {
                delete(historicDetail);
            }
        }
    }

    @Override
    public long findHistoricDetailCountByQueryCriteria(HistoricDetailQueryImpl historicVariableUpdateQuery) {
        return historicDetailDataManager.findHistoricDetailCountByQueryCriteria(historicVariableUpdateQuery);
    }

    @Override
    public List<HistoricDetail> findHistoricDetailsByQueryCriteria(HistoricDetailQueryImpl historicVariableUpdateQuery) {
        return historicDetailDataManager.findHistoricDetailsByQueryCriteria(historicVariableUpdateQuery);
    }

    @Override
    public void deleteHistoricDetailsByTaskId(String taskId) {
        if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.FULL)) {
            List<HistoricDetailEntity> details = historicDetailDataManager.findHistoricDetailsByTaskId(taskId);
            for (HistoricDetail detail : details) {
                delete((HistoricDetailEntity) detail);
            }
        }
    }

    @Override
    public List<HistoricDetail> findHistoricDetailsByNativeQuery(Map<String, Object> parameterMap) {
        return historicDetailDataManager.findHistoricDetailsByNativeQuery(parameterMap);
    }

    @Override
    public long findHistoricDetailCountByNativeQuery(Map<String, Object> parameterMap) {
        return historicDetailDataManager.findHistoricDetailCountByNativeQuery(parameterMap);
    }

    public HistoricDetailDataManager getHistoricDetailDataManager() {
        return historicDetailDataManager;
    }

    public void setHistoricDetailDataManager(HistoricDetailDataManager historicDetailDataManager) {
        this.historicDetailDataManager = historicDetailDataManager;
    }

}
