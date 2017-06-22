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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.impl.HistoryJobQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoryJobDataManager;
import org.flowable.engine.runtime.HistoryJob;

/**
 * @author Tijs Rademakers
 */
public class MybatisHistoryJobDataManager extends AbstractDataManager<HistoryJobEntity> implements HistoryJobDataManager {

    public MybatisHistoryJobDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends HistoryJobEntity> getManagedEntityClass() {
        return HistoryJobEntityImpl.class;
    }

    @Override
    public HistoryJobEntity create() {
        return new HistoryJobEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoryJobEntity> findJobsToExecute(Page page) {
        return getDbSqlSession().selectList("selectHistoryJobsToExecute", null, page);
    }

    @Override
    public List<HistoryJobEntity> findJobsByExecutionId(final String executionId) {
        return getDbSqlSession().selectList("selectHistoryJobsByExecutionId", executionId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoryJobEntity> findJobsByProcessInstanceId(final String processInstanceId) {
        return getDbSqlSession().selectList("selectHistoryJobsByProcessInstanceId", processInstanceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoryJobEntity> findExpiredJobs(Page page) {
        Date now = getClock().getCurrentTime();
        return getDbSqlSession().selectList("selectExpiredHistoryJobs", now, page);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoryJob> findHistoryJobsByQueryCriteria(HistoryJobQueryImpl jobQuery) {
        final String query = "selectHistoryJobByQueryCriteria";
        return getDbSqlSession().selectList(query, jobQuery);
    }

    @Override
    public long findHistoryJobCountByQueryCriteria(HistoryJobQueryImpl jobQuery) {
        return (Long) getDbSqlSession().selectOne("selectHistoryJobCountByQueryCriteria", jobQuery);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateHistoryJobTenantIdForDeployment", params);
    }

    @Override
    public void resetExpiredJob(String jobId) {
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("id", jobId);
        getDbSqlSession().update("resetExpiredHistoryJob", params);
    }

}
