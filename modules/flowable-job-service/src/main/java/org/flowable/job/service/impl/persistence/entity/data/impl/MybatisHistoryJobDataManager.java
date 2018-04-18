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

import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.db.ListQueryParameterObject;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.service.impl.HistoryJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.HistoryJobDataManager;
import org.flowable.job.service.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class MybatisHistoryJobDataManager extends AbstractDataManager<HistoryJobEntity> implements HistoryJobDataManager {

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
        // Needed for db2/sqlserver (see limitBetween in mssql.properties), otherwise ordering will be incorrect
        ListQueryParameterObject params = new ListQueryParameterObject();
        params.setFirstResult(page.getFirstResult());
        params.setMaxResults(page.getMaxResults());
        params.setOrderByColumns("CREATE_TIME_ ASC");
        return getDbSqlSession().selectList("selectHistoryJobsToExecute", params);
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
        Map<String, Object> params = new HashMap<>();
        Date now = CommandContextUtil.getJobServiceConfiguration().getClock().getCurrentTime();
        params.put("now", now);
        Date maxTimeout = new Date(now.getTime() - CommandContextUtil.getJobServiceConfiguration().getAsyncExecutorResetExpiredJobsMaxTimeout());
        params.put("maxTimeout", maxTimeout);
        return getDbSqlSession().selectList("selectExpiredHistoryJobs", params, page);
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
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateHistoryJobTenantIdForDeployment", params);
    }

    @Override
    public void resetExpiredJob(String jobId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", jobId);
        getDbSqlSession().update("resetExpiredHistoryJob", params);
    }

}
