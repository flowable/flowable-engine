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
package org.flowable.engine.data.inmemory.impl.execution;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.data.inmemory.AbstractMemoryDataManager;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.data.inmemory.util.QueryUtil;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.IdentityLinkQueryObject;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.service.EventSubscriptionServiceConfiguration;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory {@link ExecutionDataManager} implementation.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryExecutionDataManager extends AbstractMemoryDataManager<ExecutionEntity> implements ExecutionDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryExecutionDataManager.class);

    private final ProcessEngineConfiguration processEngineConfiguration;

    private final VariableServiceConfiguration variableServiceConfiguration;

    private final EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration;

    private final IdentityLinkServiceConfiguration identityLinkServiceConfiguration;

    private final JobServiceConfiguration jobServiceConfiguration;

    public MemoryExecutionDataManager(MapProvider mapProvider, ProcessEngineConfiguration processEngineConfiguration,
                    VariableServiceConfiguration variableServiceConfiguration, EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration,
                    IdentityLinkServiceConfiguration identityLinkServiceConfiguration, JobServiceConfiguration jobServiceConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration.getIdGenerator());
        this.processEngineConfiguration = processEngineConfiguration;
        this.variableServiceConfiguration = variableServiceConfiguration;
        this.eventSubscriptionServiceConfiguration = eventSubscriptionServiceConfiguration;
        this.identityLinkServiceConfiguration = identityLinkServiceConfiguration;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    protected Clock getClock() {
        return processEngineConfiguration.getClock();
    }

    @Override
    public ExecutionEntity create() {
        return MemoryExecutionEntityImpl.createWithEmptyRelationshipCollections();
    }

    @Override
    public ExecutionEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public void insert(ExecutionEntity entity) {
        doInsert(entity);
    }

    @Override
    public ExecutionEntity update(ExecutionEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(ExecutionEntity entity) {
        doDelete(entity);
    }

    @Override
    public ExecutionEntity findSubProcessInstanceBySuperExecutionId(String superExecutionId) {
        return findSubProcessInstancesBySuperExecutionId(superExecutionId).stream().findFirst().orElse(null);
    }

    protected List<ExecutionEntity> findSubProcessInstancesBySuperExecutionId(String superExecutionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findSubProcessInstancesBySuperExecutionId {}", superExecutionId);
        }
        if (superExecutionId == null) {
            return Collections.emptyList();
        }
        return getData().values().stream().filter(item -> superExecutionId.equals(item.getSuperExecutionId())).collect(Collectors.toList());
    }

    protected List<ExecutionEntity> findProcessInstancesBySuperExecutionId(String superExecutionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findProcessInstancesBySuperExecutionId {}", superExecutionId);
        }
        if (superExecutionId == null) {
            return Collections.emptyList();
        }
        return getData().values().stream().filter(item -> superExecutionId.equals(item.getProcessInstanceId())).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionEntity> findChildExecutionsByParentExecutionId(String parentExecutionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findChildExecutionsByParentExecutionId {}", parentExecutionId);
        }
        if (parentExecutionId == null) {
            return Collections.emptyList();
        }
        return getData().values().stream().filter(item -> parentExecutionId.equals(item.getParentId())).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionEntity> findChildExecutionsByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findChildExecutionsByProcessInstanceId {}", processInstanceId);
        }
        if (processInstanceId == null) {
            return Collections.emptyList();
        }
        return getData().values().stream().filter(item -> processInstanceId.equals(item.getProcessInstanceId())).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(String parentExecutionId, Collection<String> activityIds) {
        if (parentExecutionId == null || activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findExecutionsByParentExecutionAndActivityIds {} {}", parentExecutionId, activityIds);
        }

        return getData().values().stream().filter(
                        item -> parentExecutionId.equals(item.getParentId()) && activityIds.stream().anyMatch(actId -> actId.equals(item.getActivityId())))
                        .collect(Collectors.toList());
    }

    @Override
    public long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery) {
        return findExecutionsByQueryCriteria(executionQuery).size();
    }

    @Override
    public List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findExecutionsByQueryCriteria {}", executionQuery);
        }
        if (executionQuery == null) {
            return Collections.emptyList();
        }
        final CombinedExecutionQueryImpl query = new CombinedExecutionQueryImpl(executionQuery);

        return sortAndLimit(getData().values().stream().filter(item -> {

            boolean matchesQuery = filterExecution(item, query);
            if (!matchesQuery) {
                return false;
            }
            List<CombinedExecutionQueryImpl> orQueryObjects = query.getOrQueryObjects();
            if (orQueryObjects != null && !orQueryObjects.isEmpty()) {
                // Nested OR query objects
                if (orQueryObjects.stream().noneMatch(nestedQuery -> filterExecution(item, nestedQuery))) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList()), query);
    }

    @Override
    public long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return findProcessInstanceByQueryCriteria(executionQuery).size();
    }

    @Override
    public List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findProcessInstanceByQueryCriteria {}", executionQuery);
        }

        if (executionQuery == null) {
            return Collections.emptyList();
        }
        final CombinedExecutionQueryImpl query = new CombinedExecutionQueryImpl(executionQuery);

        return sortAndLimit(getData().values().stream().filter(item -> {
            boolean matchesQuery = filterExecution(item, query);
            if (!matchesQuery) {
                return false;
            }
            List<CombinedExecutionQueryImpl> orQueryObjects = query.getOrQueryObjects();
            if (orQueryObjects != null && !orQueryObjects.isEmpty()) {
                // Nested OR query objects
                if (orQueryObjects.stream().noneMatch(nestedQuery -> filterExecution(item, nestedQuery))) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList()), query);
    }

    @Override
    public List<ExecutionEntity> findExecutionsByRootProcessInstanceId(String rootProcessInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findExecutionsByRootProcessInstanceId {}", rootProcessInstanceId);
        }
        if (rootProcessInstanceId == null) {
            return Collections.emptyList();
        }

        return getData().values().stream().filter(item -> rootProcessInstanceId.equals(item.getRootProcessInstanceId())).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionEntity> findExecutionsByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findExecutionsByProcessInstanceId {}", processInstanceId);
        }
        if (processInstanceId == null) {
            return Collections.emptyList();
        }

        return getData().values().stream().filter(item -> processInstanceId.equals(item.getProcessInstanceId())).collect(Collectors.toList());
    }

    @Override
    public List<ProcessInstance> findProcessInstanceAndVariablesByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findProcessInstanceAndVariablesByQueryCriteria {}", executionQuery);
        }

        List<ProcessInstance> items = findProcessInstanceByQueryCriteria(executionQuery);
        items.stream().forEach(item -> {
            if (item instanceof ExecutionEntityImpl) {
                ((ExecutionEntityImpl) item).setQueryVariables(variableServiceConfiguration.getVariableService().createInternalVariableInstanceQuery()
                                .processInstanceId(item.getProcessInstanceId()).list());
            } else {
                throw new IllegalStateException("This ExecutionDataManager implementation supports only ExecutionEntityImpl items");
            }
        });
        return items;
    }

    @Override
    public Collection<ExecutionEntity> findInactiveExecutionsByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findInactiveExecutionsByProcessInstanceId {}", processInstanceId);
        }
        if (processInstanceId == null) {
            return Collections.emptyList();
        }

        return getData().values().stream().filter(item -> processInstanceId.equals(item.getProcessInstanceId()) && !item.isActive())
                        .collect(Collectors.toList());
    }

    @Override
    public Collection<ExecutionEntity> findInactiveExecutionsByActivityIdAndProcessInstanceId(String activityId, String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findInactiveExecutionsByActivityIdAndProcessInstanceId {} {}", activityId, processInstanceId);
        }

        if (processInstanceId == null || activityId == null) {
            return Collections.emptyList();
        }

        return getData().values().stream()
                        .filter(item -> processInstanceId.equals(item.getProcessInstanceId()) && activityId.equals(item.getActivityId()) && !item.isActive())
                        .collect(Collectors.toList());
    }

    @Override
    public List<String> findProcessInstanceIdsByProcessDefinitionId(String processDefinitionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findProcessInstanceIdsByProcessDefinitionId {}", processDefinitionId);
        }

        if (processDefinitionId == null) {
            return Collections.emptyList();
        }

        return getData().values().stream().filter(item -> processDefinitionId.equals(item.getProcessDefinitionId())).map(item -> item.getProcessInstanceId())
                        .distinct().collect(Collectors.toList());
    }

    @Override
    public long countActiveExecutionsByParentId(String parentId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("countActiveExecutionsByParentId {}", parentId);
        }

        if (parentId == null) {
            return 0;
        }

        return getData().values().stream().filter(item -> parentId.equals(item.getParentId())).count();
    }

    @Override
    public void updateExecutionTenantIdForDeployment(String deploymentId, String newTenantId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("updateExecutionTenantIdForDeployment {} {}", deploymentId, newTenantId);
        }

        List<ProcessDefinition> definitions = processEngineConfiguration.getRepositoryService().createProcessDefinitionQuery().deploymentId(deploymentId)
                        .list();

        getData().values().stream().filter(item -> definitions.stream().anyMatch(def -> def.getId().equals(item.getProcessDefinitionId()))).forEach(item -> {
            // update directly in the cached entity
            item.setTenantId(newTenantId);
        });
    }

    @Override
    public void updateAllExecutionRelatedEntityCountFlags(boolean newValue) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("updateAllExecutionRelatedEntityCountFlags {}", newValue);
        }
        getData().values().stream().forEach(val -> {
            if (!(val instanceof ExecutionEntityImpl)) {
                throw new IllegalStateException("This Execution Data Manager implementation supports only ExecutionEntityImpl items");
            }
            // replace directly in the cached entity
            ((ExecutionEntityImpl) val).setIsCountEnabled(newValue);
        });
    }

    @Override
    public void updateProcessInstanceLockTime(String processInstanceId, Date lockDate, String lockOwner, Date expirationTime) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("updateProcessInstanceLockTime {} {} {} {}", processInstanceId, lockDate, lockOwner, expirationTime);
        }
        List<ExecutionEntity> entities = findExecutionsByProcessInstanceId(processInstanceId);
        if (entities.isEmpty()) {
            return;
        }

        entities.stream().forEach(entity -> {
            if (entity.getLockTime() == null || entity.getLockTime().before(expirationTime)) {
                entity.setLockTime(expirationTime);
                entity.setLockOwner(lockOwner);
                update(entity);
            }
        });
    }

    @Override
    public void clearProcessInstanceLockTime(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("clearProcessInstanceLockTime {}", processInstanceId);
        }
        List<ExecutionEntity> entities = findExecutionsByProcessInstanceId(processInstanceId);
        if (entities.isEmpty()) {
            return;
        }

        entities.stream().forEach(entity -> {
            entity.setLockTime(null);
            entity.setLockOwner(null);
            update(entity);
        });
    }

    @Override
    public void clearAllProcessInstanceLockTimes(String lockOwner) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("clearAllProcessInstanceLockTimes {}", lockOwner);
        }
        if (lockOwner == null) {
            return;
        }

        List<ExecutionEntity> entities = getData().values().stream().filter(item -> lockOwner.equals(item.getLockOwner())).collect(Collectors.toList());

        if (entities.isEmpty()) {
            return;
        }

        entities.stream().forEach(entity -> {
            entity.setLockTime(null);
            entity.setLockOwner(null);
            update(entity);
        });
    }

    @Override
    public List<Execution> findExecutionsByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this ExecutionDataManager implementation!");
    }

    @Override
    public List<ProcessInstance> findProcessInstanceByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this ExecutionDataManager implementation!");
    }

    @Override
    public long findExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this ExecutionDataManager implementation!");
    }

    private boolean filterExecution(ExecutionEntity entity, CombinedExecutionQueryImpl query) {
        Boolean retVal = null; // Used to keep track of true/false return for
                               // and/or queries

        // Supported Filters, these are kept in somewhat "least expensive, most
        // common -> most expensive, least common" order
        if (query.isProcessInstancesOnly() && entity.getParentId() != null) {
            retVal = QueryUtil.matchReturn(false, query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isOnlyChildExecutions() && entity.getParentId() == null) {
            retVal = QueryUtil.matchReturn(false, query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isOnlyProcessInstanceExecutions() && entity.getParentId() != null) {
            retVal = QueryUtil.matchReturn(false, query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isActive() && !entity.isActive()) {
            retVal = QueryUtil.matchReturn(false, query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getStartedBefore() != null) {
            retVal = QueryUtil.matchReturn(entity.getStartTime() == null ? false : entity.getStartTime().before(query.getStartedBefore()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getStartedAfter() != null) {
            retVal = QueryUtil.matchReturn(entity.getStartTime() == null ? false : entity.getStartTime().after(query.getStartedAfter()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getStartedBy() != null && !query.getStartedBy().equals(entity.getStartUserId())) {
            return false;
        }

        if (query.isOnlySubProcessExecutions()) {
            // not supported in OR query
            if (!entity.isScope()) {
                return false;
            }
            if ((entity.getParentId() == null || entity.getSuperExecutionId() == null)) {
                return false;
            }
        }

        if (query.getParentId() != null) {
            retVal = QueryUtil.matchReturn(query.getParentId().equals(entity.getParentId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getExecutionId() != null) {
            retVal = QueryUtil.matchReturn(query.getExecutionId().equals(entity.getId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessInstanceId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessInstanceId().equals(entity.getProcessInstanceId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessInstanceIds() != null && !query.getProcessInstanceIds().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getProcessInstanceIds().stream().anyMatch(pid -> pid.equals(entity.getProcessInstanceId())),
                            query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getRootProcessInstanceId() != null) {
            retVal = QueryUtil.matchReturn(query.getRootProcessInstanceId().equals(entity.getRootProcessInstanceId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getActivityId() != null) {
            retVal = QueryUtil.matchReturn((query.getActivityId().equals(entity.getActivityId())), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDeploymentId() != null) {
            retVal = QueryUtil.matchReturn(query.getDeploymentId().equals(entity.getDeploymentId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDeploymentIds() != null && !query.getDeploymentIds().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getDeploymentIds().stream().anyMatch(dip -> dip.equals(entity.getDeploymentId())), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isExcludeSubprocesses()) {
            retVal = QueryUtil.matchReturn(entity.getSuperExecutionId() == null, query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCallbackId() != null) {
            retVal = QueryUtil.matchReturn(query.getCallbackId().equals(entity.getCallbackId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCallbackType() != null) {
            retVal = QueryUtil.matchReturn(query.getCallbackType().equals(entity.getCallbackType()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getReferenceId() != null) {
            retVal = QueryUtil.matchReturn(query.getReferenceId().equals(entity.getReferenceId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getReferenceType() != null) {
            retVal = QueryUtil.matchReturn(query.getReferenceType().equals(entity.getReferenceType()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantId() != null) {
            retVal = QueryUtil.matchReturn(query.getTenantId().equals(entity.getTenantId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantIdLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getTenantIdLike(), entity.getTenantId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutTenantId()) {
            retVal = QueryUtil.matchReturn((entity.getTenantId() == null || entity.getTenantId().isEmpty()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getName() != null) {
            retVal = QueryUtil.matchReturn(query.getName().equals(entity.getName()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getNameLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getNameLike(), entity.getName()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getNameLikeIgnoreCase() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLikeCaseInsensitive(query.getNameLikeIgnoreCase(), entity.getName()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getSuspensionState() != null) {
            retVal = QueryUtil.matchReturn(query.getSuspensionState().getStateCode() == entity.getSuspensionState(), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getSuperProcessInstanceId() != null) {
            // Find processes with super process id
            List<ExecutionEntity> executions = findProcessInstancesBySuperExecutionId(query.getSuperProcessInstanceId());
            retVal = QueryUtil.matchReturn(executions.stream().anyMatch(superExec -> query.getSuperProcessInstanceId().equals(superExec.getId())),
                            query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        retVal = QueryUtil.matchReturn(filterExecutionProcessDefinition(entity, query), query.isOrQuery());
        if (retVal != null) {
            return retVal;
        }

        retVal = QueryUtil.matchReturn(filterExecutionBusinessInfo(entity, query), query.isOrQuery());
        if (retVal != null) {
            return retVal;
        }

        if (query.getSubProcessInstanceId() != null) {
            /*
             * from Execution.xml ibatis mapper: and RES.ID_ = (select
             * PROC_INST_ID_ from ${prefix}ACT_RU_EXECUTION where ID_ = (select
             * SUPER_EXEC_ from ${prefix}ACT_RU_EXECUTION where ID_ =
             * #{subProcessInstanceId}) )
             */

            // The sub execution, if any
            ExecutionEntity execution = findById(query.getSubProcessInstanceId());
            if (execution == null) {
                if (!query.isOrQuery()) {
                    // 'or' query can continue
                    return false;
                }
            } else {
                // The sub process instance for the sub execution, if any
                ExecutionEntity subProcess = findSubProcessInstanceBySuperExecutionId(execution.getSuperExecutionId());
                if (subProcess == null) {
                    if (!query.isOrQuery()) {
                        // 'or' query can continue
                        return false;
                    }
                } else {
                    retVal = QueryUtil.matchReturn(entity.getId().equals(subProcess.getProcessInstanceId()), query.isOrQuery());
                    if (retVal != null) {
                        return retVal;
                    }
                }
            }
        }

        if (query.getActiveActivityId() != null || (query.getActiveActivityIds() != null && !query.getActiveActivityIds().isEmpty())) {
            Set<String> ids = query.getActiveActivityIds();
            if (ids == null) {
                ids = new HashSet<>();
            }
            if (ids.isEmpty()) {
                ids.add(query.getActiveActivityId());
            }
            retVal = QueryUtil.matchReturn(ids.stream().anyMatch(activityId -> {
                return processEngineConfiguration.getRuntimeService().createActivityInstanceQuery().processInstanceId(entity.getId()).unfinished()
                                .activityId(activityId).count() > 0;
            }), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getEventSubscriptions() != null && !query.getEventSubscriptions().isEmpty()) {
            List<EventSubscriptionEntity> subscriptionEntities = eventSubscriptionServiceConfiguration.getEventSubscriptionService()
                            .findEventSubscriptionsByExecution(entity.getId());
            retVal = QueryUtil.matchReturn(
                            query.getEventSubscriptions().stream()
                                            .anyMatch(requested -> subscriptionEntities.stream()
                                                            .anyMatch(actual -> QueryUtil.nullSafeEquals(requested.getEventName(), actual.getEventName())
                                                                            && QueryUtil.nullSafeEquals(requested.getEventType(), actual.getEventType()))),
                            query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isProcessInstancesOnly() && query.isWithJobException()) {
            List<TimerJobEntity> jobs = jobServiceConfiguration.getTimerJobEntityManager().findJobsByProcessInstanceId(entity.getProcessInstanceId());
            retVal = QueryUtil.matchReturn(jobs.stream().anyMatch(
                            job -> job.getExceptionMessage() != null || job.getExceptionByteArrayRef() != null || job.getExceptionStacktrace() != null),
                            query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }

        }

        retVal = QueryUtil.matchReturn(filterExecutionIdentityLinks(entity, query), query.isOrQuery());
        if (retVal != null) {
            return retVal;
        }

        retVal = QueryUtil.matchReturn(filterExecutionVariables(entity, query), query.isOrQuery());
        if (retVal != null) {
            return retVal;
        }

        // ExecutionQuery and ProcessInstanceQuery allow a *single* or, so if
        // none of the filters matches (that is, none returned true for an
        // orQuery), we can safely return false here as there cannot be other
        // orQueries to process.
        return query.isOrQuery() ? false : true;
    }

    private Boolean filterExecutionProcessDefinition(ExecutionEntity entity, CombinedExecutionQueryImpl query) {

        Boolean retVal = null;
        if (query.getProcessDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionId().equals(entity.getProcessDefinitionId()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionIds() != null && !query.getProcessDefinitionIds().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionIds().stream().anyMatch(pid -> pid.equals(entity.getProcessDefinitionId())),
                            query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionKey() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionKey().equals(entity.getProcessDefinitionKey()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionKeys() != null && !query.getProcessDefinitionKeys().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionKeys().stream().anyMatch(pdk -> pdk.equals(entity.getProcessDefinitionKey())),
                            query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionName() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionName().equals(entity.getProcessDefinitionName()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionVersion() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionVersion().equals(entity.getProcessDefinitionVersion()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionCategory() != null || query.getProcessDefinitionEngineVersion() != null) {
            // this hits database
            ProcessDefinition pd = processEngineConfiguration.getRepositoryService().getProcessDefinition(entity.getProcessDefinitionId());
            if (query.getProcessDefinitionCategory() != null) {
                retVal = QueryUtil.matchReturn(pd != null && query.getProcessDefinitionCategory().equals(pd.getCategory()), query.isOrQuery());

                if (retVal != null) {
                    return retVal;
                }
            }

            if (query.getProcessDefinitionEngineVersion() != null) {
                retVal = QueryUtil.matchReturn(pd != null && query.getProcessDefinitionEngineVersion().equals(pd.getEngineVersion()), query.isOrQuery());

                if (retVal != null) {
                    return retVal;
                }
            }
        }
        return retVal;
    }

    private Boolean filterExecutionBusinessInfo(ExecutionEntity entity, CombinedExecutionQueryImpl query) {
        Boolean retVal = null;

        if (query.getBusinessKey() != null || query.getBusinessKeyLike() != null) {
            if (query.isIncludeChildExecutionsWithBusinessKeyQuery()) {
                boolean childsMatched = findChildExecutionsByParentExecutionId(entity.getId()).stream().anyMatch(child -> {
                    if (query.getBusinessKey() != null && query.getBusinessKey().equals(entity.getBusinessKey())) {
                        return true;
                    }

                    if (query.getBusinessKeyLike() != null && QueryUtil.queryLike(query.getBusinessKeyLike(), entity.getBusinessKey())) {
                        return true;
                    }
                    return false;
                });
                retVal = QueryUtil.matchReturn(childsMatched, query.isOrQuery());
                if (retVal != null) {
                    return retVal;
                }

            } else {
                if (query.getBusinessKey() != null) {
                    retVal = QueryUtil.matchReturn(query.getBusinessKey().equals(entity.getBusinessKey()), query.isOrQuery());
                    if (retVal != null) {
                        return retVal;
                    }
                }

                if (query.getBusinessKeyLike() != null) {
                    retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getBusinessKeyLike(), entity.getBusinessKey()), query.isOrQuery());
                    if (retVal != null) {
                        return retVal;
                    }
                }
            }
        }

        if (query.getBusinessStatus() != null) {
            retVal = QueryUtil.matchReturn(query.getBusinessStatus().equals(entity.getBusinessStatus()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getBusinessStatusLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getBusinessStatusLike(), entity.getBusinessStatus()), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        return retVal;
    }

    private Boolean filterExecutionIdentityLinks(ExecutionEntity entity, CombinedExecutionQueryImpl query) {

        Boolean retVal = null;

        if (query.getInvolvedUser() != null || query.getInvolvedUserIdentityLink() != null) {

            IdentityLinkQueryObject identityQuery = query.getInvolvedUserIdentityLink();
            final String userId = identityQuery == null ? query.getInvolvedUser() : identityQuery.getUserId();
            final String type = identityQuery == null ? null : identityQuery.getType();

            retVal = QueryUtil.matchReturn(identityLinkServiceConfiguration.getIdentityLinkService().findIdentityLinksByProcessInstanceId(entity.getId())
                            .stream().anyMatch(il -> {
                                if (QueryUtil.nullSafeEquals(userId, il.getUserId())) {
                                    return false;
                                }
                                if (QueryUtil.nullSafeEquals(type, il.getType())) {
                                    return false;
                                }
                                return true;
                            }), query.isOrQuery());

            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getInvolvedGroups() != null && !query.getInvolvedGroups().isEmpty()) {

            retVal = QueryUtil.matchReturn(identityLinkServiceConfiguration.getIdentityLinkService().findIdentityLinksByProcessInstanceId(entity.getId())
                            .stream().anyMatch(il -> query.getInvolvedGroups().stream().anyMatch(ig -> ig.equals(il.getGroupId()))), query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getInvolvedGroupIdentityLink() != null) {
            retVal = QueryUtil.matchReturn(identityLinkServiceConfiguration.getIdentityLinkService().findIdentityLinksByProcessInstanceId(entity.getId())
                            .stream().anyMatch(il -> query.getInvolvedGroupIdentityLink().getGroupId().equals(il.getGroupId())
                                            && query.getInvolvedGroupIdentityLink().getType().equals(il.getType())),
                            query.isOrQuery());
            if (retVal != null) {
                return retVal;
            }
        }

        return retVal;
    }

    private Boolean filterExecutionVariables(ExecutionEntity entity, CombinedExecutionQueryImpl query) {
        Boolean retVal = null;
        if (query.getQueryVariableValues() == null || query.getQueryVariableValues().isEmpty()) {
            // No variable filters
            return retVal;
        }

        List<VariableInstanceEntity> executionVariables = variableServiceConfiguration.getVariableInstanceEntityManager().createInternalVariableInstanceQuery()
                        .executionId(entity.getId()).list();
        List<VariableInstanceEntity> processVariables = entity.getId().equals(entity.getProcessInstanceId()) ? executionVariables
                        : variableServiceConfiguration.getVariableService().findVariableInstancesByExecutionId(entity.getProcessInstanceId());
        return QueryUtil.filterVariables(query.getQueryVariableValues(), executionVariables, Collections.emptyList(), processVariables, query.isOrQuery());
    }

    private <N extends Execution> List<N> sortAndLimit(List<N> collect, CombinedExecutionQueryImpl query) {
        if (collect == null || collect.isEmpty()) {
            return collect;
        }
        return sortAndPaginate(collect, ExecutionComparator.resolve(query.getOrderBy()), query.getFirstResult(), query.getMaxResults());
    }
}
