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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.SingleCachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.ExternalWorkerJobAcquireBuilderImpl;
import org.flowable.job.service.impl.ExternalWorkerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.ExternalWorkerJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.cachematcher.ExternalWorkerJobsByExecutionIdMatcher;
import org.flowable.job.service.impl.persistence.entity.data.impl.cachematcher.ExternalWorkerJobsByScopeIdAndSubScopeIdMatcher;
import org.flowable.job.service.impl.persistence.entity.data.impl.cachematcher.JobByCorrelationIdMatcher;

/**
 * @author Filip Hrisafov
 */
public class MybatisExternalWorkerJobDataManager extends AbstractDataManager<ExternalWorkerJobEntity> implements ExternalWorkerJobDataManager {

    protected JobServiceConfiguration jobServiceConfiguration;

    protected CachedEntityMatcher<ExternalWorkerJobEntity> jobsByExecutionIdMatcher = new ExternalWorkerJobsByExecutionIdMatcher();
    protected CachedEntityMatcher<ExternalWorkerJobEntity> externalWorkerJobsByScopeIdAndSubScopeIdMatcher = new ExternalWorkerJobsByScopeIdAndSubScopeIdMatcher();
    protected SingleCachedEntityMatcher<ExternalWorkerJobEntity> externalWorkerJobByCorrelationIdMatcher = new JobByCorrelationIdMatcher<>();

    public MybatisExternalWorkerJobDataManager(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Class<? extends ExternalWorkerJobEntity> getManagedEntityClass() {
        return ExternalWorkerJobEntityImpl.class;
    }

    @Override
    public ExternalWorkerJobEntity create() {
        return new ExternalWorkerJobEntityImpl();
    }

    @Override
    public ExternalWorkerJobEntity findJobByCorrelationId(String correlationId) {
        return getEntity("selectExternalWorkerJobByCorrelationId", correlationId, externalWorkerJobByCorrelationIdMatcher, true);
    }

    @Override
    public List<ExternalWorkerJobEntity> findJobsToExecute(List<String> enabledCategories, Page page) {
        throw new FlowableException("Use dedicated method for finding external worker jobs to execute");
    }

    @Override
    public List<ExternalWorkerJobEntity> findJobsByExecutionId(final String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();

        // If the execution has been inserted in the same command execution as this query, there can't be any in the database 
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            return getListFromCache(jobsByExecutionIdMatcher, executionId);
        }

        return getList(dbSqlSession, "selectExternalWorkerJobsByExecutionId", executionId, jobsByExecutionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalWorkerJobEntity> findJobsByProcessInstanceId(final String processInstanceId) {
        return getDbSqlSession().selectList("selectExternalWorkerJobsByProcessInstanceId", processInstanceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalWorkerJobEntity> findExpiredJobs(List<String> enabledCategories, Page page) {
        Map<String, Object> params = new HashMap<>();
        params.put("jobExecutionScope", jobServiceConfiguration.getJobExecutionScope());
        Date now = jobServiceConfiguration.getClock().getCurrentTime();
        params.put("now", now);
        if (enabledCategories != null && enabledCategories.size() > 0) {
            params.put("enabledCategories", enabledCategories);
        }
        return getDbSqlSession().selectList("selectExpiredExternalWorkerJobs", params, page);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalWorkerJob> findJobsByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery) {
        final String query = "selectExternalWorkerJobByQueryCriteria";
        return getDbSqlSession().selectList(query, jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery) {
        return (Long) getDbSqlSession().selectOne("selectExternalWorkerJobCountByQueryCriteria", jobQuery);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateExternalWorkerJobTenantIdForDeployment", params);
    }

    @Override
    public void bulkUpdateJobLockWithoutRevisionCheck(List<ExternalWorkerJobEntity> externalWorkerJobs, String lockOwner, Date lockExpirationTime) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("lockOwner", lockOwner);
        params.put("lockExpirationTime", lockExpirationTime);

        bulkUpdateEntities("updateExternalWorkerJobLocks", params, "externalWorkerJobs", externalWorkerJobs);
    }

    @Override
    public void resetExpiredJob(String jobId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", jobId);
        params.put("now", jobServiceConfiguration.getClock().getCurrentTime());
        getDbSqlSession().update("resetExpiredExternalWorkerJob", params);
    }

    @Override
    public void deleteJobsByExecutionId(String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            deleteCachedEntities(dbSqlSession, jobsByExecutionIdMatcher, executionId);
        } else {
            bulkDelete("deleteExternalWorkerJobsByExecutionId", jobsByExecutionIdMatcher, executionId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalWorkerJobEntity> findExternalJobsToExecute(ExternalWorkerJobAcquireBuilderImpl builder, int numberOfJobs) {
        return getDbSqlSession().selectList("selectExternalWorkerJobsToExecute", builder, new Page(0, numberOfJobs));
    }

    @Override
    public List<ExternalWorkerJobEntity> findJobsByScopeIdAndSubScopeId(String scopeId, String subScopeId) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("scopeId", scopeId);
        paramMap.put("subScopeId", subScopeId);
        return getList(getDbSqlSession(), "selectExternalWorkerJobsByScopeIdAndSubScopeId", paramMap, externalWorkerJobsByScopeIdAndSubScopeIdMatcher, true);
    }
    
    @Override
    protected IdGenerator getIdGenerator() {
        return jobServiceConfiguration.getIdGenerator();
    }
}
