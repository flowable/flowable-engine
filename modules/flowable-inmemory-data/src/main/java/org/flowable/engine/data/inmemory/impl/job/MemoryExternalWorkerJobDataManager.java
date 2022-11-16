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
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.data.inmemory.util.QueryUtil;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.ExternalWorkerJobAcquireBuilderImpl;
import org.flowable.job.service.impl.ExternalWorkerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.ExternalWorkerJobDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory {@link ExternalWorkerJobDataManager} implementation.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryExternalWorkerJobDataManager extends AbstractJobMemoryDataManager<ExternalWorkerJobEntity> implements ExternalWorkerJobDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryExternalWorkerJobDataManager.class);

    private IdentityLinkServiceConfiguration identityLinkServiceConfiguration;

    public MemoryExternalWorkerJobDataManager(MapProvider mapProvider, ProcessEngineConfiguration processEngineConfiguration,
                    JobServiceConfiguration jobServiceConfiguration, IdentityLinkServiceConfiguration identityLinkServiceConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration, jobServiceConfiguration);
        this.identityLinkServiceConfiguration = identityLinkServiceConfiguration;
    }

    @Override
    public ExternalWorkerJobEntity create() {
        return new ExternalWorkerJobEntityImpl();
    }

    @Override
    public void insert(ExternalWorkerJobEntity entity) {
        doInsert(entity);
    }

    @Override
    public ExternalWorkerJobEntity update(ExternalWorkerJobEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public ExternalWorkerJobEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(ExternalWorkerJobEntity entity) {
        doDelete(entity);
    }

    @Override
    public List<ExternalWorkerJobEntity> findJobsByExecutionId(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByExecutionId {}", executionId);
        }
        return getData().values().stream().filter(item -> item.getExecutionId() != null && item.getExecutionId().equals(executionId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<ExternalWorkerJobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByProcessInstanceId {}", processInstanceId);
        }
        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId))
                        .collect(Collectors.toList());
    }

    @Override
    public ExternalWorkerJobEntity findJobByCorrelationId(String correlationId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobByCorrelationId {}", correlationId);
        }
        return getData().values().stream().filter(item -> item.getCorrelationId() != null && item.getCorrelationId().equals(correlationId)).findFirst()
                        .orElse(null);
    }

    @Override
    public long findJobCountByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobCountByQueryCriteria {}", jobQuery);
        }
        return findJobsByQueryCriteria(jobQuery).size();
    }

    @Override
    public List<ExternalWorkerJob> findJobsByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByQueryCriteria {}", jobQuery);
        }
        if (jobQuery == null) {
            return Collections.emptyList();
        }

        return sortAndLimitExternalWorkerJob(getData().values().stream().filter(item -> {
            return filterJob(item, jobQuery);
        }).collect(Collectors.toList()), jobQuery);
    }

    @Override
    public List<ExternalWorkerJobEntity> findJobsToExecute(List<String> enabledCategories, Page page) {
        String scope = getJobServiceConfiguration().getJobExecutionScope();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsToExecute {} {}", enabledCategories, scope);
        }
        final Date now = getJobServiceConfiguration().getClock().getCurrentTime();
        List<ExternalWorkerJobEntity> r = sortAndPaginate(getData().values().stream().filter(item -> {
            if (item.getLockOwner() != null) {
                return false;
            }
            if (item.getDuedate() == null || item.getDuedate().after(now)) {
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
    public List<ExternalWorkerJobEntity> findExternalJobsToExecute(ExternalWorkerJobAcquireBuilderImpl builder, int numberOfJobs) {
        return sortAndPaginate(getData().values().stream().filter(item -> {
            return filterJobToExecute(item, builder);
        }).collect(Collectors.toList()), JobComparator.getDefault(), 0, numberOfJobs);

    }

    @Override
    public List<ExternalWorkerJobEntity> findExpiredJobs(List<String> enabledCategories, Page page) {
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

        List<ExternalWorkerJobEntity> r = sortAndPaginate(getData().values().stream().filter(item -> {
            if (scope == null && item.getScopeType() != null) {
                return false;
            }

            if (scope != null && !SCOPE_ALL.equals(scope) && !scope.equals(item.getScopeType())) {
                return false;
            }

            if (enabledCategories != null && enabledCategories.stream().noneMatch(cat -> cat.equals(item.getCategory()))) {
                return false;
            }

            // Expired if expiration time is not null and it is before 'now
            if (item.getLockExpirationTime() != null && item.getLockExpirationTime().before(now)) {
                return true;
            }

            // Expired if maxTimeout specified and lock time is null and create
            // time is
            // before maxTimeout
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
    public void bulkUpdateJobLockWithoutRevisionCheck(List<ExternalWorkerJobEntity> jobEntities, String lockOwner, Date lockExpirationTime) {
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

        ExternalWorkerJobEntity item = findById(jobId);
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

    @Override
    public List<ExternalWorkerJobEntity> findJobsByScopeIdAndSubScopeId(String scopeId, String subScopeId) {
        return doFindJobsByScopeIdAndSubScopeId(scopeId, subScopeId);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        internalUpdateJobTenantIdForDeployment(deploymentId, newTenantId);
    }

    private boolean filterJobToExecute(ExternalWorkerJobEntity item, ExternalWorkerJobAcquireBuilderImpl builder) {

        Boolean retVal = null; // Used to keep track of true/false return for
                               // queries

        retVal = QueryUtil.matchReturn(item.getLockExpirationTime() != null, false);
        if (retVal != null) {
            return retVal;
        }

        if (builder.getTopic() != null) {
            retVal = QueryUtil.matchReturn(builder.getTopic().equals(item.getJobHandlerConfiguration()), false);
            if (retVal != null) {
                return retVal;
            }
        }
        if (builder.getScopeType() != null) {
            if (SCOPE_BPMN.equals(builder.getScopeType())) {
                retVal = QueryUtil.matchReturn(item.getProcessInstanceId() != null, false);
            } else {
                retVal = QueryUtil.matchReturn(builder.getScopeType().equals(item.getScopeType()), false);
            }
            if (retVal != null) {
                return retVal;
            }
        }

        if (builder.getTenantId() != null) {
            retVal = QueryUtil.matchReturn(builder.getTenantId().equals(item.getTenantId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (builder.getAuthorizedUser() != null) {
            retVal = QueryUtil.matchReturn(identityLinkServiceConfiguration.getIdentityLinkDataManager().findIdentityLinkByScopeIdScopeTypeUserGroupAndType(
                            item.getCorrelationId(), "externalWorker", builder.getAuthorizedUser(), null, null).isEmpty(), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (builder.getAuthorizedGroups() != null) {
            Collection<String> groups = builder.getAuthorizedGroups();
            retVal = QueryUtil.matchReturn(groups.stream().anyMatch(group -> {
                return !identityLinkServiceConfiguration.getIdentityLinkDataManager()
                                .findIdentityLinkByScopeIdScopeTypeUserGroupAndType(item.getCorrelationId(), "externalWorker", null, group, null).isEmpty();
            }), false);
            if (retVal != null) {
                return retVal;
            }
        }
        return true;
    }

    private boolean filterJob(ExternalWorkerJobEntity item, ExternalWorkerJobQueryImpl query) {
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

        if (query.isOnlyLocked()) {
            retVal = QueryUtil.matchReturn(item.getLockExpirationTime() == null, false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isOnlyUnlocked()) {
            retVal = QueryUtil.matchReturn(item.getLockExpirationTime() != null, false);
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

        if (query.getAuthorizedUser() != null) {
            retVal = QueryUtil.matchReturn(identityLinkServiceConfiguration.getIdentityLinkDataManager().findIdentityLinkByScopeIdScopeTypeUserGroupAndType(
                            item.getCorrelationId(), "externalWorker", query.getAuthorizedUser(), null, null).isEmpty(), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getAuthorizedGroups() != null) {
            Collection<String> groups = query.getAuthorizedGroups();
            retVal = QueryUtil.matchReturn(groups.stream().anyMatch(group -> {
                return !identityLinkServiceConfiguration.getIdentityLinkDataManager()
                                .findIdentityLinkByScopeIdScopeTypeUserGroupAndType(item.getCorrelationId(), "externalWorker", null, group, null).isEmpty();
            }), false);
            if (retVal != null) {
                return retVal;
            }
        }

        return true;
    }

    protected List<ExternalWorkerJob> sortAndLimitExternalWorkerJob(List<ExternalWorkerJob> collect, AbstractQuery< ? , ExternalWorkerJob> query) {
        if (collect == null || collect.isEmpty()) {
            return collect;
        }
        return sortAndPaginate(collect, JobComparator.resolve(query.getOrderBy()), query.getFirstResult(), query.getMaxResults());
    }
}
