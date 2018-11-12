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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.impl.ActivityInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.ActivityInstanceDataManager;
import org.flowable.engine.runtime.ActivityInstance;

/**
 * @author martin.grofcik
 */
public class ActivityInstanceEntityManagerImpl extends AbstractEntityManager<ActivityInstanceEntity> implements ActivityInstanceEntityManager {

    protected ActivityInstanceDataManager activityInstanceDataManager;

    public ActivityInstanceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ActivityInstanceDataManager activityInstanceDataManager) {
        super(processEngineConfiguration);
        this.activityInstanceDataManager = activityInstanceDataManager;
    }

    @Override
    protected DataManager<ActivityInstanceEntity> getDataManager() {
        return activityInstanceDataManager;
    }

    @Override
    public List<ActivityInstanceEntity> findUnfinishedActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return activityInstanceDataManager.findUnfinishedActivityInstancesByExecutionAndActivityId(executionId, activityId);
    }
    
    @Override
    public List<ActivityInstanceEntity> findActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return activityInstanceDataManager.findActivityInstancesByExecutionIdAndActivityId(executionId, activityId);
    }

    @Override
    public List<ActivityInstanceEntity> findUnfinishedActivityInstancesByProcessInstanceId(String processInstanceId) {
        return activityInstanceDataManager.findUnfinishedActivityInstancesByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteActivityInstancesByProcessInstanceId(String processInstanceId) {
        if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            activityInstanceDataManager.deleteActivityInstancesByProcessInstanceId(processInstanceId);
        }
    }
    @Override
    public void deleteActivityInstancesByProcessDefinitionId(String processDefinitionId) {
        if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            activityInstanceDataManager.deleteActivityInstancesByProcessDefinitionId(processDefinitionId);
        }
    }

    @Override
    public long findActivityInstanceCountByQueryCriteria(ActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return activityInstanceDataManager.findActivityInstanceCountByQueryCriteria(historicActivityInstanceQuery);
    }

    @Override
    public List<ActivityInstance> findActivityInstancesByQueryCriteria(ActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return activityInstanceDataManager.findActivityInstancesByQueryCriteria(historicActivityInstanceQuery);
    }

    @Override
    public List<ActivityInstance> findActivityInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return activityInstanceDataManager.findActivityInstancesByNativeQuery(parameterMap);
    }

    @Override
    public long findActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return activityInstanceDataManager.findActivityInstanceCountByNativeQuery(parameterMap);
    }

    public ActivityInstanceDataManager getActivityInstanceDataManager() {
        return activityInstanceDataManager;
    }

    public void setActivityInstanceDataManager(ActivityInstanceDataManager activityInstanceDataManager) {
        this.activityInstanceDataManager = activityInstanceDataManager;
    }

}
