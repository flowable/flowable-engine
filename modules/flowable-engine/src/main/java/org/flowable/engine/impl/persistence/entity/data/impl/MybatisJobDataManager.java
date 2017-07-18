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
import org.flowable.engine.impl.JobQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.CachedEntityMatcher;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractDataManager;
import org.flowable.engine.impl.persistence.entity.data.JobDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.JobsByExecutionIdMatcher;
import org.flowable.engine.runtime.Job;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class MybatisJobDataManager extends AbstractDataManager<JobEntity> implements JobDataManager {

    protected CachedEntityMatcher<JobEntity> jobsByExecutionIdMatcher = new JobsByExecutionIdMatcher();

    public MybatisJobDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends JobEntity> getManagedEntityClass() {
        return JobEntityImpl.class;
    }

    @Override
    public JobEntity create() {
        return new JobEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobEntity> findJobsToExecute(Page page) {
        return getDbSqlSession().selectList("selectJobsToExecute", null, page);
    }

    @Override
    public List<JobEntity> findJobsByExecutionId(final String executionId) {
        return getList("selectJobsByExecutionId", executionId, jobsByExecutionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobEntity> findJobsByProcessInstanceId(final String processInstanceId) {
        return getDbSqlSession().selectList("selectJobsByProcessInstanceId", processInstanceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobEntity> findExpiredJobs(Page page) {
        Map<String, Object> params = new HashMap<>();
        Date now = getClock().getCurrentTime();
        params.put("now", now);
        Date maxTimeout = new Date(now.getTime() - getProcessEngineConfiguration().getAsyncExecutorResetExpiredJobsMaxTimeout());
        params.put("maxTimeout", maxTimeout);
        return getDbSqlSession().selectList("selectExpiredJobs", params, page);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery) {
        final String query = "selectJobByQueryCriteria";
        return getDbSqlSession().selectList(query, jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
        return (Long) getDbSqlSession().selectOne("selectJobCountByQueryCriteria", jobQuery);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateJobTenantIdForDeployment", params);
    }

    @Override
    public void resetExpiredJob(String jobId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", jobId);
        params.put("now", processEngineConfiguration.getClock().getCurrentTime());
        getDbSqlSession().update("resetExpiredJob", params);
    }

}
