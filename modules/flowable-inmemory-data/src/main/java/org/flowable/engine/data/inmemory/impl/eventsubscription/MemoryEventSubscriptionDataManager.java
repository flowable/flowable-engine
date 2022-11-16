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
package org.flowable.engine.data.inmemory.impl.eventsubscription;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.data.inmemory.AbstractMemoryDataManager;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.data.inmemory.util.QueryUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.GenericEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.GenericEventSubscriptionEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.data.EventSubscriptionDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in-memory event subscription data manager.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryEventSubscriptionDataManager extends AbstractMemoryDataManager<EventSubscriptionEntity> implements EventSubscriptionDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryEventSubscriptionDataManager.class);
    private final ProcessEngineConfigurationImpl processEngineConfiguration;

    public MemoryEventSubscriptionDataManager(MapProvider mapProvider, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration.getEventSubscriptionServiceConfiguration().getIdGenerator());
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public EventSubscriptionEntity create() {
        throw new UnsupportedOperationException("Only allowed to create one of the subclasses of EventSubscriptionEntity");
    }

    @Override
    public CompensateEventSubscriptionEntity createCompensateEventSubscription() {
        return new CompensateEventSubscriptionEntityImpl(processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }

    @Override
    public MessageEventSubscriptionEntity createMessageEventSubscription() {
        return new MessageEventSubscriptionEntityImpl(processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }

    @Override
    public SignalEventSubscriptionEntity createSignalEventSubscription() {
        return new SignalEventSubscriptionEntityImpl(processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }

    @Override
    public GenericEventSubscriptionEntity createGenericEventSubscriptionEntity() {
        return new GenericEventSubscriptionEntityImpl(processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }

    @Override
    public void insert(EventSubscriptionEntity entity) {
        doInsert(entity);
    }

    @Override
    public EventSubscriptionEntity update(EventSubscriptionEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public void updateEventSubscriptionTenantId(String oldTenantId, String newTenantId) {
        getData().values().stream().filter(item -> {
            if (item.getTenantId() == null && oldTenantId == null) {
                return true;
            }
            if (item.getTenantId() != null && item.getTenantId().equals(oldTenantId)) {
                return true;
            }
            return false;
        }).forEach(item -> item.setTenantId(newTenantId));
    }

    @Override
    public EventSubscriptionEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public List<MessageEventSubscriptionEntity> findMessageEventSubscriptionsByProcessInstanceAndEventName(String processInstanceId, String eventName) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findMessageEventSubscriptionsByProcessInstanceAndEventName {} {}", processInstanceId, eventName);
        }

        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId)
                        && item.getEventName() != null && item.getEventName().equals(eventName) && item instanceof MessageEventSubscriptionEntity)
                        .map(item -> (MessageEventSubscriptionEntity) item).collect(Collectors.toList());
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByEventName(String eventName, String tenantId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findSignalEventSubscriptionsByEventName {} {}", eventName, tenantId);
        }

        ExecutionDataManager executionDataManager = processEngineConfiguration.getExecutionDataManager();
        return getData().values().stream().filter(item -> {
            boolean match = item.getEventName() != null && item.getEventName().equals(eventName)
                            && (tenantId != null && tenantId.equals(item.getTenantId()) || tenantId == null && StringUtils.isEmpty(item.getTenantId()))
                            && item instanceof SignalEventSubscriptionEntity;
            if (!match) {
                return false;
            }
            // See selectSignalEventSubscriptionsByEventName in
            // org.flowable.eventsubscription.service.db.entity.mapping.EventSubscription.xml
            if (item.getExecutionId() == null) {
                return true;
            }
            ExecutionEntity execution = executionDataManager.findById(item.getExecutionId());
            if (execution == null) {
                return false;
            }
            return execution.getSuspensionState() == 1;
        }).map(item -> (SignalEventSubscriptionEntity) item).collect(Collectors.toList());
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByProcessInstanceAndEventName(String processInstanceId, String eventName) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findSignalEventSubscriptionsByProcessInstanceAndEventName {} {}", processInstanceId, eventName);
        }

        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId)
                        && item.getEventName() != null && item.getEventName().equals(eventName) && item instanceof SignalEventSubscriptionEntity)
                        .map(item -> (SignalEventSubscriptionEntity) item).collect(Collectors.toList());
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByScopeAndEventName(String scopeId, String scopeType, String eventName) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findSignalEventSubscriptionsByScopeAndEventName {} {} {}", scopeId, scopeType, eventName);
        }

        return getData().values().stream()
                        .filter(item -> item.getEventName() != null && item.getEventName().equals(eventName) && item.getScopeId() != null
                                        && item.getScopeId().equals(scopeId) && item.getScopeType() != null && item.getScopeType().equals(scopeType)
                                        && item instanceof SignalEventSubscriptionEntity)
                        .map(item -> (SignalEventSubscriptionEntity) item).collect(Collectors.toList());
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByNameAndExecution(String eventName, String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findSignalEventSubscriptionsByNameAndExecution {} {}", eventName, executionId);
        }

        return getData().values().stream()
                        .filter(item -> item.getEventName() != null && item.getEventName().equals(eventName) && item.getExecutionId() != null
                                        && item.getExecutionId().equals(executionId) && item instanceof SignalEventSubscriptionEntity)
                        .map(item -> (SignalEventSubscriptionEntity) item).collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecutionAndType(String executionId, String type) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByExecutionAndType {} {}", executionId, type);
        }

        return getData().values().stream().filter(item -> item.getExecutionId() != null && item.getExecutionId().equals(executionId)
                        && item.getEventType() != null && item.getEventType().equals(type)).collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndType(String processInstanceId, String type) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByProcessInstanceAndType {} {}", processInstanceId, type);
        }

        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId)
                        && item.getEventType() != null && item.getEventType().equals(type)).collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndActivityId(String processInstanceId, String activityId, String type) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByProcessInstanceAndActivityId {} {} {}", processInstanceId, activityId, type);
        }

        return getData().values().stream()
                        .filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId)
                                        && item.getActivityId() != null && item.getActivityId().equals(activityId) && item.getEventType() != null
                                        && item.getEventType().equals(type))
                        .collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecution(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByExecution {}", executionId);
        }
        return getData().values().stream().filter(item -> item.getExecutionId() != null && item.getExecutionId().equals(executionId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsBySubScopeId(String subScopeId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsBySubScopeId {}", subScopeId);
        }

        return getData().values().stream().filter(item -> item.getSubScopeId() != null && item.getSubScopeId().equals(subScopeId)).collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByTypeAndProcessDefinitionId(String type, String processDefinitionId, String tenantId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByTypeAndProcessDefinitionId {} {} {}", type, processDefinitionId, tenantId);
        }

        return getData().values().stream().filter(item ->
        // See selectEventSubscriptionsByTypeAndProcessDefinitionId in
        // org.flowable.eventsubscription.service.db.entity.mapping.EventSubscription.xml
        item.getExecutionId() == null && item.getProcessInstanceId() == null && item.getEventType() != null && item.getEventType().equals(type)
                        && item.getProcessDefinitionId() != null && item.getProcessDefinitionId().equals(processDefinitionId)
                        && (tenantId != null && tenantId.equals(item.getTenantId()) || tenantId == null && StringUtils.isEmpty(item.getTenantId())))
                        .collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByScopeIdAndType(String scopeId, String type) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByScopeIdAndType {} {}", scopeId, type);
        }

        return getData().values().stream().filter(item -> item.getScopeId() != null && item.getScopeId().equals(scopeId) && item.getEventType() != null
                        && item.getEventType().equals(type)).collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByName(String type, String eventName, String tenantId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByName {} {} {}", type, eventName, tenantId);
        }

        return getData().values().stream().filter(item -> item.getEventName() != null && item.getEventName().equals(eventName) && item.getEventType() != null
                        && item.getEventType().equals(type)
                        && (tenantId != null && tenantId.equals(item.getTenantId()) || tenantId == null && StringUtils.isEmpty(item.getTenantId())))
                        .collect(Collectors.toList());
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByNameAndExecution(String type, String eventName, String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByNameAndExecution {} {} {}", type, eventName, executionId);
        }
        return getData().values().stream()
                        .filter(item -> item.getExecutionId() != null && item.getExecutionId().equals(executionId) && item.getEventType() != null
                                        && item.getEventType().equals(type) && item.getEventName() != null && item.getEventName().equals(eventName))
                        .collect(Collectors.toList());
    }

    @Override
    public MessageEventSubscriptionEntity findMessageStartEventSubscriptionByName(String messageName, String tenantId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findMessageStartEventSubscriptionByName {} {}", messageName, tenantId);
        }

        return getData().values().stream().filter(item ->
        // See selectMessageStartEventSubscriptionByName in
        // org.flowable.eventsubscription.service.db.entity.mapping.EventSubscription.xml
        item.getExecutionId() == null && item.getEventName() != null && item.getEventName().equals(messageName) && item.getExecutionId() == null
                        && item instanceof MessageEventSubscriptionEntity).map(item -> (MessageEventSubscriptionEntity) item).findFirst().orElse(null);
    }

    @Override
    public long findEventSubscriptionCountByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionCountByQueryCriteria {}", eventSubscriptionQueryImpl);
        }

        return findEventSubscriptionsByQueryCriteria(eventSubscriptionQueryImpl).size();
    }

    @Override
    public List<EventSubscription> findEventSubscriptionsByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findEventSubscriptionsByQueryCriteria {}", eventSubscriptionQueryImpl);
        }
        if (eventSubscriptionQueryImpl == null) {
            return Collections.emptyList();
        }

        return sortAndLimit(getData().values().stream().filter(item -> {
            boolean matchesQuery = filterEventSubscription(item, eventSubscriptionQueryImpl, false);
            if (!matchesQuery) {
                return false;
            }
            if (eventSubscriptionQueryImpl.getOrQueryObjects() != null && !eventSubscriptionQueryImpl.getOrQueryObjects().isEmpty()) {
                // Nested OR query objects (in reality only one can exist)
                if (eventSubscriptionQueryImpl.getOrQueryObjects().stream().noneMatch(nestedQuery -> filterEventSubscription(item, nestedQuery, true))) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList()), eventSubscriptionQueryImpl);
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(EventSubscriptionEntity entity) {
        doDelete(entity);
    }

    @Override
    public void deleteEventSubscriptionsForProcessDefinition(String processDefinitionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteEventSubscriptionsForProcessDefinition {}", processDefinitionId);
        }

        getData().entrySet().removeIf(entry -> {
            EventSubscriptionEntity item = entry.getValue();
            if (item.getExecutionId() != null) {
                return false;
            }
            if (item.getProcessInstanceId() != null) {
                return false;
            }
            if (item.getProcessDefinitionId() == null) {
                return false;
            }
            return item.getProcessDefinitionId().equals(processDefinitionId);
        });
    }

    @Override
    public void deleteEventSubscriptionsByExecutionId(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteEventSubscriptionsByExecutionId {}", executionId);
        }
        getData().entrySet().removeIf(entry -> entry.getValue().getExecutionId() != null && entry.getValue().getExecutionId().equals(executionId));
    }

    @Override
    public void deleteEventSubscriptionsForScopeIdAndType(String scopeId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteEventSubscriptionsForScopeIdAndType {} {}", scopeId, scopeType);
        }
        getData().entrySet().removeIf(entry -> entry.getValue().getScopeId() != null && entry.getValue().getScopeId().equals(scopeId)
                        && entry.getValue().getScopeType() != null && entry.getValue().getScopeType().equals(scopeType));
    }

    @Override
    public void deleteEventSubscriptionsForScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteEventSubscriptionsForScopeDefinitionIdAndType {} {}", scopeDefinitionId, scopeType);
        }
        getData().entrySet()
                        .removeIf(entry -> entry.getValue().getScopeDefinitionId() != null && entry.getValue().getScopeDefinitionId().equals(scopeDefinitionId)
                                        && entry.getValue().getScopeType() != null && entry.getValue().getScopeType().equals(scopeType));
    }

    @Override
    public void deleteEventSubscriptionsForScopeDefinitionIdAndTypeAndNullScopeId(String scopeDefinitionId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteEventSubscriptionsForScopeDefinitionIdAndTypeAndNullScopeId {} {}", scopeDefinitionId, scopeType);
        }
        getData().entrySet()
                        .removeIf(entry -> entry.getValue().getScopeId() == null && entry.getValue().getScopeDefinitionId() != null
                                        && entry.getValue().getScopeDefinitionId().equals(scopeDefinitionId) && entry.getValue().getScopeType() != null
                                        && entry.getValue().getScopeType().equals(scopeType));
    }

    @Override
    public boolean updateEventSubscriptionLockTime(String eventSubscriptionId, Date lockDate, String lockOwner, Date currentTime) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("updateEventSubscriptionLockTime {} {} {} {}", eventSubscriptionId, lockDate, lockOwner, currentTime);
        }

        EventSubscriptionEntity entity = findById(eventSubscriptionId);
        if (entity == null) {
            return false;
        }
        if (entity.getLockTime() == null || entity.getLockTime().before(lockDate)) {
            entity.setLockTime(lockDate);
            entity.setLockOwner(lockOwner);
            update(entity);
            return true;
        }
        return false;

    }

    @Override
    public void clearEventSubscriptionLockTime(String eventSubscriptionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("clearEventSubscriptionLockTime {}", eventSubscriptionId);
        }

        EventSubscriptionEntity entity = findById(eventSubscriptionId);
        if (entity == null) {
            return;
        }
        entity.setLockTime(null);
        entity.setLockOwner(null);
        update(entity);

    }

    private boolean filterEventSubscription(EventSubscriptionEntity item, EventSubscriptionQueryImpl query, boolean isOrQuery) {
        Boolean retVal = null; // Used to keep track of true/false return for
                               // and/or queries

        if (query.getId() != null) {
            retVal = QueryUtil.matchReturn(query.getId().equals(item.getId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getEventType() != null) {
            retVal = QueryUtil.matchReturn(query.getEventType().equals(item.getEventType()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getEventName() != null) {
            retVal = QueryUtil.matchReturn(query.getEventType().equals(item.getEventType()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getExecutionId() != null) {
            retVal = QueryUtil.matchReturn(query.getExecutionId().equals(item.getExecutionId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessInstanceId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessInstanceId().equals(item.getProcessInstanceId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutProcessInstanceId()) {
            retVal = QueryUtil.matchReturn(item.getProcessInstanceId() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionId().equals(item.getProcessDefinitionId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutProcessDefinitionId()) {
            retVal = QueryUtil.matchReturn(item.getProcessDefinitionId() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getActivityId() != null) {
            retVal = QueryUtil.matchReturn(query.getActivityId().equals(item.getActivityId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getSubScopeId() != null) {
            retVal = QueryUtil.matchReturn(query.getSubScopeId().equals(item.getSubScopeId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeId() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeId().equals(item.getScopeId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutScopeId()) {
            retVal = QueryUtil.matchReturn(item.getScopeId() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeDefinitionId().equals(item.getScopeDefinitionId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutScopeDefinitionId()) {
            retVal = QueryUtil.matchReturn(item.getScopeDefinitionId() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeType() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeType().equals(item.getScopeType()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCreatedBefore() != null) {
            if (item.getCreated() == null) {
                return false;
            }
            retVal = QueryUtil.matchReturn(item.getCreated().before(query.getCreatedBefore()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCreatedAfter() != null) {
            if (item.getCreated() == null) {
                return false;
            }
            retVal = QueryUtil.matchReturn(item.getCreated().after(query.getCreatedAfter()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantId() != null) {
            retVal = QueryUtil.matchReturn(query.getTenantId().equals(item.getTenantId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutTenantId()) {
            retVal = QueryUtil.matchReturn(item.getTenantId() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantIds() != null && !query.getTenantIds().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getTenantIds().stream().anyMatch(pid -> pid.equals(item.getTenantId())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getConfiguration() != null) {
            retVal = QueryUtil.matchReturn(query.getConfiguration().equals(item.getConfiguration()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutConfiguration()) {
            retVal = QueryUtil.matchReturn(item.getConfiguration() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getConfigurations() != null && !query.getConfigurations().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getConfigurations().stream().anyMatch(pid -> pid.equals(item.getConfiguration())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        // Query allows a *single* or, so if none of the filters matched (that
        // is, none returned true
        // for an orQuery), we can safely return false here as there cannot be
        // other orQueries to
        // process.
        return isOrQuery ? false : true;
    }

    private List<EventSubscription> sortAndLimit(List<EventSubscription> collect, EventSubscriptionQueryImpl query) {

        if (collect == null || collect.isEmpty()) {
            return collect;
        }

        return sortAndPaginate(collect, EventSubscriptionComparator.resolve(query.getOrderBy()), query.getFirstResult(), query.getMaxResults());
    }
}
