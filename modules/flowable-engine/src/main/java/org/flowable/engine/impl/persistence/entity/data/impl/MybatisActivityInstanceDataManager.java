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
package org.flowable.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.engine.impl.ActivityInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.ActivityInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ActivityInstanceMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.UnfinishedActivityInstanceMatcher;
import org.flowable.engine.runtime.ActivityInstance;

/**
 * @author martin.grofcik
 */
public class MybatisActivityInstanceDataManager extends AbstractProcessDataManager<ActivityInstanceEntity> implements ActivityInstanceDataManager {

    protected CachedEntityMatcher<ActivityInstanceEntity> unfinishedActivityInstanceMatcher = new UnfinishedActivityInstanceMatcher();
    protected CachedEntityMatcher<ActivityInstanceEntity> activityInstanceMatcher = new ActivityInstanceMatcher();
    protected CachedEntityMatcher<ActivityInstanceEntity> activitiesByProcessInstanceIdMatcher = new ActivityByProcessInstanceIdMatcher();

    public MybatisActivityInstanceDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends ActivityInstanceEntity> getManagedEntityClass() {
        return ActivityInstanceEntityImpl.class;
    }

    @Override
    public ActivityInstanceEntity create() {
        return new ActivityInstanceEntityImpl();
    }

    @Override
    public List<ActivityInstanceEntity> findUnfinishedActivityInstancesByExecutionAndActivityId(final String executionId, final String activityId) {
        Map<String, Object> params = new HashMap<>();
        params.put("executionId", executionId);
        params.put("activityId", activityId);
        return getList("selectUnfinishedActivityInstanceExecutionIdAndActivityId", params, unfinishedActivityInstanceMatcher, true);
    }
    
    @Override
    public List<ActivityInstanceEntity> findActivityInstancesByExecutionIdAndActivityId(final String executionId, final String activityId) {
        Map<String, Object> params = new HashMap<>();
        params.put("executionId", executionId);
        params.put("activityId", activityId);
        return getList("selectActivityInstanceExecutionIdAndActivityId", params, activityInstanceMatcher, true);
    }

    @Override
    public void deleteActivityInstancesByProcessInstanceId(String processInstanceId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        deleteCachedEntities(dbSqlSession, activitiesByProcessInstanceIdMatcher, processInstanceId);
        if (!isEntityInserted(dbSqlSession, "execution", processInstanceId)) {
            dbSqlSession.delete("deleteActivityInstancesByProcessInstanceId", processInstanceId, ActivityInstanceEntityImpl.class);
        }
    }

    @Override
    public long findActivityInstanceCountByQueryCriteria(ActivityInstanceQueryImpl activityInstanceQuery) {
        return (Long) getDbSqlSession().selectOne("selectActivityInstanceCountByQueryCriteria", activityInstanceQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ActivityInstance> findActivityInstancesByQueryCriteria(ActivityInstanceQueryImpl activityInstanceQuery) {
        return getDbSqlSession().selectList("selectActivityInstancesByQueryCriteria", activityInstanceQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ActivityInstance> findActivityInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectActivityInstanceByNativeQuery", parameterMap);
    }

    @Override
    public long findActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectActivityInstanceCountByNativeQuery", parameterMap);
    }

}
