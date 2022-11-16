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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.data.inmemory.util.QueryUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.SuspendedJobDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory {@link SuspendedJobDataManager} implementation.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemorySuspendedJobDataManager extends AbstractJobMemoryDataManager<SuspendedJobEntity> implements SuspendedJobDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemorySuspendedJobDataManager.class);

    public MemorySuspendedJobDataManager(MapProvider mapProvider, ProcessEngineConfiguration processEngineConfiguration,
                    JobServiceConfiguration jobServiceConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration, jobServiceConfiguration);
    }

    @Override
    public SuspendedJobEntity create() {
        return new SuspendedJobEntityImpl();
    }

    @Override
    public void insert(SuspendedJobEntity entity) {
        doInsert(entity);
    }

    @Override
    public SuspendedJobEntity update(SuspendedJobEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public SuspendedJobEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(SuspendedJobEntity entity) {
        doDelete(entity);
    }

    @Override
    public List<SuspendedJobEntity> findJobsByExecutionId(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByExecutionId {}", executionId);
        }
        return getData().values().stream().filter(item -> item.getExecutionId() != null && item.getExecutionId().equals(executionId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<SuspendedJobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobsByProcessInstanceId {}", processInstanceId);
        }
        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId))
                        .collect(Collectors.toList());
    }

    @Override
    public SuspendedJobEntity findJobByCorrelationId(String correlationId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobByCorrelationId {}", correlationId);
        }
        return getData().values().stream().filter(item -> item.getCorrelationId() != null && item.getCorrelationId().equals(correlationId)).findFirst()
                        .orElse(null);
    }

    @Override
    public long findJobCountByQueryCriteria(SuspendedJobQueryImpl jobQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findJobCountByQueryCriteria {}", jobQuery);
        }
        return findJobsByQueryCriteria(jobQuery).size();
    }

    @Override
    public List<Job> findJobsByQueryCriteria(SuspendedJobQueryImpl jobQuery) {
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

    private boolean filterJob(SuspendedJobEntity item, SuspendedJobQueryImpl query) {
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

        if (query.isRetriesLeft()) {
            retVal = QueryUtil.matchReturn(item.getRetries() > 0, false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isNoRetriesLeft()) {
            retVal = QueryUtil.matchReturn(item.getRetries() <= 0, false);
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
        if (query.isOnlyExternalWorkers()) {
            retVal = QueryUtil.matchReturn(item.getJobType() != null && item.getJobType().equals("exdternalWorker"), false);
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
