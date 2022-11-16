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
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory {@link TimerJobDataManager} implementation.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryTimerJobDataManager extends AbstractJobMemoryDataManager<TimerJobEntity> implements TimerJobDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryTimerJobDataManager.class);

    public MemoryTimerJobDataManager(MapProvider mapProvider, ProcessEngineConfiguration processEngineConfiguration,
                    JobServiceConfiguration jobServiceConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration, jobServiceConfiguration);
    }

    @Override
    public TimerJobEntity create() {
        return new TimerJobEntityImpl();
    }

    @Override
    public void insert(TimerJobEntity entity) {
        doInsert(entity);
    }

    @Override
    public TimerJobEntity update(TimerJobEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public TimerJobEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(TimerJobEntity entity) {
        doDelete(entity);
    }

    @Override
    public List<TimerJobEntity> findJobsToExecute(List<String> enabledCategories, Page page) {
        String scope = getJobServiceConfiguration().getJobExecutionScope();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsToExecute {} {}", enabledCategories, scope);
        }
        final Date now = getJobServiceConfiguration().getClock().getCurrentTime();
        List<TimerJobEntity> r = sortAndPaginate(getData().values().stream().filter(item -> {
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
    public List<TimerJobEntity> findExpiredJobs(List<String> enabledCategories, Page page) {
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

        List<TimerJobEntity> r = sortAndPaginate(getData().values().stream().filter(item -> {
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
    public List<TimerJobEntity> findJobsByExecutionId(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByExecutionId {}", executionId);
        }
        return getData().values().stream().filter(item -> item.getExecutionId() != null && item.getExecutionId().equals(executionId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<TimerJobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByProcessInstanceId {}", processInstanceId);
        }
        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId))
                        .collect(Collectors.toList());
    }

    @Override
    public TimerJobEntity findJobByCorrelationId(String correlationId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobByCorrelationId {}", correlationId);
        }
        return getData().values().stream().filter(item -> item.getCorrelationId() != null && item.getCorrelationId().equals(correlationId)).findFirst()
                        .orElse(null);
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByTypeAndProcessDefinitionId {} {}", jobHandlerType, processDefinitionId);
        }
        return getData().values().stream()
                        .filter(item -> item.getJobHandlerType() != null && item.getJobHandlerType().equals(jobHandlerType)
                                        && item.getProcessDefinitionId() != null && item.getProcessDefinitionId().equals(processDefinitionId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByTypeAndProcessDefinitionKeyNoTenantId {} {}", jobHandlerType, processDefinitionKey);
        }

        List<String> procDefs = getProcessEngineConfiguration().getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey)
                        .list().stream().filter(item -> StringUtils.isEmpty(item.getTenantId())).map(item -> item.getId()).collect(Collectors.toList());
        return getData().values().stream()
                        .filter(item -> item.getJobHandlerType() != null && item.getJobHandlerType().equals(jobHandlerType)
                                        && item.getProcessDefinitionId() != null
                                        && procDefs.stream().anyMatch(defId -> item.getProcessDefinitionId().equals(defId)))
                        .collect(Collectors.toList());
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByTypeAndProcessDefinitionKeyAndTenantId {} {} {}", jobHandlerType, processDefinitionKey, tenantId);
        }

        List<String> procDefs = getProcessEngineConfiguration().getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey)
                        .list().stream().filter(item -> item.getTenantId() != null && item.getTenantId().equals(tenantId)).map(item -> item.getId())
                        .collect(Collectors.toList());
        return getData().values().stream()
                        .filter(item -> item.getJobHandlerType() != null && item.getJobHandlerType().equals(jobHandlerType)
                                        && item.getProcessDefinitionId() != null
                                        && procDefs.stream().anyMatch(defId -> item.getProcessDefinitionId().equals(defId)))
                        .collect(Collectors.toList());
    }

    @Override
    public List<TimerJobEntity> findJobsByScopeIdAndSubScopeId(String scopeId, String subScopeId) {
        return doFindJobsByScopeIdAndSubScopeId(scopeId, subScopeId);
    }

    @Override
    public long findJobCountByQueryCriteria(TimerJobQueryImpl jobQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobCountByQueryCriteria {}", jobQuery);
        }
        return findJobsByQueryCriteria(jobQuery).size();
    }

    @Override
    public List<Job> findJobsByQueryCriteria(TimerJobQueryImpl jobQuery) {
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
        List<ProcessDefinition> definitions = getProcessEngineConfiguration().getRepositoryService().createProcessDefinitionQuery().deploymentId(deploymentId)
                        .list();
        getData().values().stream().filter(item -> definitions.stream().anyMatch(def -> def.getId().equals(item.getProcessDefinitionId()))).forEach(item -> {
            item.setTenantId(newTenantId);
        });
    }

    @Override
    public void bulkUpdateJobLockWithoutRevisionCheck(List<TimerJobEntity> jobEntities, String lockOwner, Date lockExpirationTime) {
        if (jobEntities == null || jobEntities.isEmpty()) {
            return;
        }
        getData().values().stream().filter(item -> jobEntities.stream().anyMatch(target -> target.getId().equals(item.getId()))).forEach(item -> {
            item.setLockOwner(lockOwner);
            item.setLockExpirationTime(lockExpirationTime);
        });
    }

    @Override
    public void bulkDeleteWithoutRevision(List<TimerJobEntity> timerJobEntities) {
        getData().entrySet().removeIf(item -> timerJobEntities.stream().anyMatch(target -> target.getId().equals(item.getValue().getId())));
    }

    @Override
    public void resetExpiredJob(String jobId) {
        final Date now = getJobServiceConfiguration().getClock().getCurrentTime();

        TimerJobEntity item = findById(jobId);
        if (item == null) {
            return;
        }
        item.setLockOwner(null);
        item.setLockExpirationTime(null);
        item.setCreateTime(now);
    }

    private boolean filterJob(TimerJobEntity item, TimerJobQueryImpl query) {
        Boolean retVal = null; // Used to keep track of true/false return for
                               // queries
        final Date now = query.getNow();

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

        if (query.isExecutable()) {
            retVal = QueryUtil.matchReturn(item.getDuedate() == null ? false : (item.getDuedate().equals(now) || item.getDuedate().before(now)), false);
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
