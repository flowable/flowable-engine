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
package org.flowable.job.service.impl.persistence.entity.data.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.db.AbstractDataManager;
import org.flowable.engine.common.impl.db.DbSqlSession;
import org.flowable.engine.common.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.cachematcher.TimerJobsByExecutionIdMatcher;
import org.flowable.job.service.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Vasile Dirla
 */
public class MybatisTimerJobDataManager extends AbstractDataManager<TimerJobEntity> implements TimerJobDataManager {

    protected CachedEntityMatcher<TimerJobEntity> timerJobsByExecutionIdMatcher = new TimerJobsByExecutionIdMatcher();

    @Override
    public Class<? extends TimerJobEntity> getManagedEntityClass() {
        return TimerJobEntityImpl.class;
    }

    @Override
    public TimerJobEntity create() {
        return new TimerJobEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Job> findJobsByQueryCriteria(TimerJobQueryImpl jobQuery) {
        String query = "selectTimerJobByQueryCriteria";
        return getDbSqlSession().selectList(query, jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(TimerJobQueryImpl jobQuery) {
        return (Long) getDbSqlSession().selectOne("selectTimerJobCountByQueryCriteria", jobQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findTimerJobsToExecute(Page page) {
        Date now = CommandContextUtil.getJobServiceConfiguration().getClock().getCurrentTime();
        return getDbSqlSession().selectList("selectTimerJobsToExecute", now, page);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
        Map<String, String> params = new HashMap<>(2);
        params.put("handlerType", jobHandlerType);
        params.put("processDefinitionId", processDefinitionId);
        return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionId", params);

    }

    @Override
    public List<TimerJobEntity> findJobsByExecutionId(final String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        
        // If the execution has been inserted in the same command execution as this query, there can't be any in the database
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            return getListFromCache(timerJobsByExecutionIdMatcher, executionId);
        }
        
        return getList(dbSqlSession, "selectTimerJobsByExecutionId", executionId, timerJobsByExecutionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsByProcessInstanceId(final String processInstanceId) {
        return getDbSqlSession().selectList("selectTimerJobsByProcessInstanceId", processInstanceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
        Map<String, String> params = new HashMap<>(2);
        params.put("handlerType", jobHandlerType);
        params.put("processDefinitionKey", processDefinitionKey);
        return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionKeyNoTenantId", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
        Map<String, String> params = new HashMap<>(3);
        params.put("handlerType", jobHandlerType);
        params.put("processDefinitionKey", processDefinitionKey);
        params.put("tenantId", tenantId);
        return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionKeyAndTenantId", params);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateTimerJobTenantIdForDeployment", params);
    }
    
}
