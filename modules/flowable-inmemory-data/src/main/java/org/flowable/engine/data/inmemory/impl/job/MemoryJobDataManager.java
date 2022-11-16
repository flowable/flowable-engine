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
package org.flowable.engine.data.inmemory.impl.job;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.Page;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.data.inmemory.util.QueryUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.JobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.JobDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory {@link JobDataManager} implementation.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryJobDataManager extends AbstractJobMemoryDataManager<JobEntity> implements JobDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryJobDataManager.class);

    public MemoryJobDataManager(MapProvider mapProvider, ProcessEngineConfiguration processEngineConfiguration,
                    JobServiceConfiguration jobServiceConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration, jobServiceConfiguration);
    }

    @Override
    public JobEntity create() {
        return new JobEntityImpl();
    }

    @Override
    public void insert(JobEntity entity) {
        doInsert(entity);
    }

    @Override
    public JobEntity update(JobEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public JobEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(JobEntity entity) {
        doDelete(entity);
    }

    @Override
    public List<JobEntity> findJobsToExecute(List<String> enabledCategories, Page page) {
        String scope = getJobServiceConfiguration().getJobExecutionScope();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsToExecute {} {}", enabledCategories, scope);
        }

        List<JobEntity> r = sortAndPaginate(getData().values().stream().filter(item -> {
            if (item.getLockExpirationTime() != null) {
                return false;
            }

            if (scope == null && item.getScopeType() != null) {
                return false;
            }

            if (scope != null && !SCOPE_ALL.equals(scope) && !scope.equals(item.getScopeType())) {
                return false;
            }

            if (enabledCategories != null && enabledCategories.stream().noneMatch(cat -> cat.equals(item.getCategory()))) {
                return false;
            }

            // Matched
            return true;
        }).collect(Collectors.toList()), JobComparator.getDefault(), page == null ? -1 : page.getFirstResult(), page == null ? -1 : page.getMaxResults());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsToExecute results {}", r);
        }
        return r;
    }

    @Override
    public List<JobEntity> findExpiredJobs(List<String> enabledCategories, Page page) {
        final String scope = getJobServiceConfiguration().getJobExecutionScope();
        final Date now = getJobServiceConfiguration().getClock().getCurrentTime();
        final Date maxTimeout;

        if (getJobServiceConfiguration().isAsyncHistoryExecutorMessageQueueMode()) {
            maxTimeout = new Date(now.getTime() - getJobServiceConfiguration().getAsyncExecutorResetExpiredJobsMaxTimeout());
        } else {
            maxTimeout = null;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findExpiredJobs {} {} {} {}", enabledCategories, scope, now, maxTimeout);
        }

        List<JobEntity> r = sortAndPaginate(getData().values().stream().filter(item -> {
            if (scope == null && item.getScopeType() != null) {
                return false;
            }

            if (scope != null && !SCOPE_ALL.equals(scope) && !scope.equals(item.getScopeType())) {
                return false;
            }

            if (enabledCategories != null && enabledCategories.stream().noneMatch(cat -> cat.equals(item.getCategory()))) {
                return false;
            }

            // Expired if expiration time is not null and it is before 'now'
            if (item.getLockExpirationTime() != null && item.getLockExpirationTime().before(now)) {
                return true;
            }

            // Expired if maxTimeout specified and lock time is null and create
            // time is before maxTimeout
            if (maxTimeout != null && item.getLockExpirationTime() == null && item.getCreateTime().before(maxTimeout)) {
                return true;
            }

            return false;
        }).collect(Collectors.toList()), JobComparator.getDefault(), page == null ? -1 : page.getFirstResult(), page == null ? -1 : page.getMaxResults());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findExpiredJobs results {}", r);
        }
        return r;
    }

    @Override
    public List<JobEntity> findJobsByExecutionId(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByExecutionId {}", executionId);
        }
        return getData().values().stream().filter(item -> item.getExecutionId() != null && item.getExecutionId().equals(executionId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<JobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByProcessInstanceId {}", processInstanceId);
        }
        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId))
                        .collect(Collectors.toList());
    }

    @Override
    public JobEntity findJobByCorrelationId(String correlationId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobByCorrelationId {}", correlationId);
        }
        return getData().values().stream().filter(item -> item.getCorrelationId() != null && item.getCorrelationId().equals(correlationId)).findFirst()
                        .orElse(null);
    }

    @Override
    public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobCountByQueryCriteria {}", jobQuery);
        }
        return findJobsByQueryCriteria(jobQuery).size();
    }

    @Override
    public List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByQueryCriteria {}", jobQuery);
        }
        if (jobQuery == null) {
            return Collections.emptyList();
        }

        return sortAndLimitJobs(getData().values().stream().filter(item -> {
            return filterJob(item, jobQuery);
        }).collect(Collectors.toList()), jobQuery);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        internalUpdateJobTenantIdForDeployment(deploymentId, newTenantId);
    }

    @Override
    public void bulkUpdateJobLockWithoutRevisionCheck(List<JobEntity> jobEntities, String lockOwner, Date lockExpirationTime) {
        if (jobEntities == null || jobEntities.isEmpty()) {
            return;
        }
        getData().values().stream().filter(item -> jobEntities.stream().anyMatch(target -> target.getId().equals(item.getId()))).forEach(item -> {
            item.setLockOwner(lockOwner);
            item.setLockExpirationTime(lockExpirationTime);
        });
    }

    @Override
    public void resetExpiredJob(String jobId) {
        final Date now = getJobServiceConfiguration().getClock().getCurrentTime();

        JobEntity item = findById(jobId);
        if (item == null) {
            return;
        }
        item.setLockOwner(null);
        item.setLockExpirationTime(null);
        item.setCreateTime(now);
    }

    @Override
    public void deleteJobsByExecutionId(String executionId) {
        doDeleteJobsByExecutionId(executionId);
    }

    private boolean filterJob(JobEntity item, JobQueryImpl query) {
        Boolean retVal = null; // Used to keep track of true/false return for
                               // queries

        if (query.getId() != null) {
            retVal = QueryUtil.matchReturn(query.getId().equals(item.getId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessInstanceId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessInstanceId().equals(item.getProcessInstanceId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutProcessInstanceId()) {
            retVal = QueryUtil.matchReturn(query.getProcessInstanceId() == null, false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getExecutionId() != null) {
            retVal = QueryUtil.matchReturn(query.getExecutionId().equals(item.getExecutionId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getHandlerType() != null) {
            retVal = QueryUtil.matchReturn(query.getHandlerType().equals(item.getJobHandlerType()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getHandlerTypes() != null) {
            Collection<String> handlerTypes = query.getHandlerTypes();
            retVal = QueryUtil.matchReturn(handlerTypes.stream().anyMatch(ht -> ht.equals(item.getJobHandlerType())), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionId().equals(item.getProcessDefinitionId()), false);
            if (retVal != null) {
                return retVal;
            }
        }
        if (query.getCategory() != null) {
            retVal = QueryUtil.matchReturn(query.getCategory().equals(item.getCategory()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCategoryLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getCategoryLike(), item.getCategory()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getElementId() != null) {
            retVal = QueryUtil.matchReturn(query.getElementId().equals(item.getElementId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getElementName() != null) {
            retVal = QueryUtil.matchReturn(query.getElementName().equals(item.getElementName()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeId() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeId().equals(item.getScopeId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutScopeId()) {
            retVal = QueryUtil.matchReturn(item.getScopeId() == null, false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getSubScopeId() != null) {
            retVal = QueryUtil.matchReturn(query.getSubScopeId().equals(item.getSubScopeId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeType() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeType().equals(item.getScopeType()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeDefinitionId().equals(item.getScopeDefinitionId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCorrelationId() != null) {
            retVal = QueryUtil.matchReturn(query.getCorrelationId().equals(item.getCorrelationId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isOnlyTimers()) {
            retVal = QueryUtil.matchReturn(item.getJobType() != null && item.getJobType().equals("timer"), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isOnlyMessages()) {
            retVal = QueryUtil.matchReturn(item.getJobType() != null && item.getJobType().equals("message"), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDuedateHigherThan() != null) {
            retVal = QueryUtil.matchReturn(item.getDuedate() == null ? false : item.getDuedate().after(query.getDuedateHigherThan()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDuedateHigherThanOrEqual() != null) {
            retVal = QueryUtil.matchReturn(item.getDuedate() == null ? false
                            : (item.getDuedate().equals(query.getDuedateHigherThanOrEqual()) || item.getDuedate().after(query.getDuedateHigherThanOrEqual())),
                            false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDuedateLowerThan() != null) {
            retVal = QueryUtil.matchReturn(item.getDuedate() == null ? false : item.getDuedate().before(query.getDuedateLowerThan()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDuedateLowerThanOrEqual() != null) {
            retVal = QueryUtil.matchReturn(item.getDuedate() == null ? false
                            : (item.getDuedate().equals(query.getDuedateLowerThanOrEqual()) || item.getDuedate().before(query.getDuedateLowerThanOrEqual())),
                            false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithException()) {
            retVal = QueryUtil.matchReturn(item.getExceptionMessage() != null || item.getExceptionByteArrayRef() != null, false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getExceptionMessage() != null) {
            retVal = QueryUtil.matchReturn(query.getExceptionMessage().equals(item.getExceptionMessage()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getLockOwner() != null) {
            retVal = QueryUtil.matchReturn(query.getLockOwner().equals(item.getLockOwner()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isOnlyLocked()) {
            retVal = QueryUtil.matchReturn(item.getLockExpirationTime() != null, false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isOnlyUnlocked()) {
            retVal = QueryUtil.matchReturn(item.getLockExpirationTime() == null, false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantId() != null) {
            retVal = QueryUtil.matchReturn(query.getTenantId().equals(item.getTenantId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantIdLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getTenantIdLike(), item.getTenantId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutTenantId()) {
            retVal = QueryUtil.matchReturn(StringUtils.isEmpty(item.getTenantId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutScopeType()) {
            retVal = QueryUtil.matchReturn(item.getScopeType() == null, false);
            if (retVal != null) {
                return retVal;
            }
        }

        return true;
    }
}
