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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.CompensateEventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.EventSubscriptionDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.EventSubscriptionsByExecutionAndTypeMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.EventSubscriptionsByExecutionIdMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.EventSubscriptionsByNameMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.EventSubscriptionsByProcInstTypeAndActivityMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.MessageEventSubscriptionsByProcInstAndEventNameMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.SignalEventSubscriptionByEventNameMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.SignalEventSubscriptionByNameAndExecutionMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.SignalEventSubscriptionByProcInstAndEventNameMatcher;
import org.flowable.engine.runtime.EventSubscription;

/**
 * @author Joram Barrez
 */
public class MybatisEventSubscriptionDataManager extends AbstractProcessDataManager<EventSubscriptionEntity> implements EventSubscriptionDataManager {

    private static List<Class<? extends EventSubscriptionEntity>> ENTITY_SUBCLASSES = new ArrayList<>();

    static {
        ENTITY_SUBCLASSES.add(MessageEventSubscriptionEntityImpl.class);
        ENTITY_SUBCLASSES.add(SignalEventSubscriptionEntityImpl.class);
        ENTITY_SUBCLASSES.add(CompensateEventSubscriptionEntityImpl.class);
    }

    protected CachedEntityMatcher<EventSubscriptionEntity> eventSubscriptionsByNameMatcher = new EventSubscriptionsByNameMatcher();

    protected CachedEntityMatcher<EventSubscriptionEntity> eventSubscriptionsByExecutionIdMatcher = new EventSubscriptionsByExecutionIdMatcher();

    protected CachedEntityMatcher<EventSubscriptionEntity> eventSubscriptionsByProcInstTypeAndActivityMatcher = new EventSubscriptionsByProcInstTypeAndActivityMatcher();

    protected CachedEntityMatcher<EventSubscriptionEntity> eventSubscriptionsByExecutionAndTypeMatcher = new EventSubscriptionsByExecutionAndTypeMatcher();

    protected CachedEntityMatcher<EventSubscriptionEntity> signalEventSubscriptionByNameAndExecutionMatcher = new SignalEventSubscriptionByNameAndExecutionMatcher();

    protected CachedEntityMatcher<EventSubscriptionEntity> signalEventSubscriptionByProcInstAndEventNameMatcher = new SignalEventSubscriptionByProcInstAndEventNameMatcher();

    protected CachedEntityMatcher<EventSubscriptionEntity> signalEventSubscriptionByEventNameMatcher = new SignalEventSubscriptionByEventNameMatcher();

    protected CachedEntityMatcher<EventSubscriptionEntity> messageEventSubscriptionsByProcInstAndEventNameMatcher = new MessageEventSubscriptionsByProcInstAndEventNameMatcher();

    public MybatisEventSubscriptionDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends EventSubscriptionEntity> getManagedEntityClass() {
        return EventSubscriptionEntityImpl.class;
    }

    @Override
    public List<Class<? extends EventSubscriptionEntity>> getManagedEntitySubClasses() {
        return ENTITY_SUBCLASSES;
    }

    @Override
    public EventSubscriptionEntity create() {
        // only allowed to create subclasses
        throw new UnsupportedOperationException();
    }

    @Override
    public CompensateEventSubscriptionEntity createCompensateEventSubscription() {
        return new CompensateEventSubscriptionEntityImpl();
    }

    @Override
    public MessageEventSubscriptionEntity createMessageEventSubscription() {
        return new MessageEventSubscriptionEntityImpl();
    }

    @Override
    public SignalEventSubscriptionEntity createSignalEventSubscription() {
        return new SignalEventSubscriptionEntityImpl();
    }

    @Override
    public long findEventSubscriptionCountByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
        final String query = "selectEventSubscriptionCountByQueryCriteria";
        return (Long) getDbSqlSession().selectOne(query, eventSubscriptionQueryImpl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EventSubscription> findEventSubscriptionsByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
        final String query = "selectEventSubscriptionByQueryCriteria";
        return getDbSqlSession().selectList(query, eventSubscriptionQueryImpl);
    }

    @Override
    public List<MessageEventSubscriptionEntity> findMessageEventSubscriptionsByProcessInstanceAndEventName(final String processInstanceId, final String eventName) {
        Map<String, String> params = new HashMap<>();
        params.put("processInstanceId", processInstanceId);
        params.put("eventName", eventName);
        return toMessageEventSubscriptionEntityList(getList("selectMessageEventSubscriptionsByProcessInstanceAndEventName",
                params, messageEventSubscriptionsByProcInstAndEventNameMatcher, true));
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByEventName(final String eventName, final String tenantId) {
        final String query = "selectSignalEventSubscriptionsByEventName";

        final Map<String, String> params = new HashMap<>();
        params.put("eventName", eventName);
        if (tenantId != null && !tenantId.equals(ProcessEngineConfiguration.NO_TENANT_ID)) {
            params.put("tenantId", tenantId);
        }

        List<EventSubscriptionEntity> result = getList(query, params, signalEventSubscriptionByEventNameMatcher, true);
        return toSignalEventSubscriptionEntityList(result);
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByProcessInstanceAndEventName(final String processInstanceId, final String eventName) {
        final String query = "selectSignalEventSubscriptionsByProcessInstanceAndEventName";
        Map<String, String> params = new HashMap<>();
        params.put("processInstanceId", processInstanceId);
        params.put("eventName", eventName);
        return toSignalEventSubscriptionEntityList(getList(query, params, signalEventSubscriptionByProcInstAndEventNameMatcher, true));
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByNameAndExecution(final String name, final String executionId) {
        Map<String, String> params = new HashMap<>();
        params.put("executionId", executionId);
        params.put("eventName", name);
        return toSignalEventSubscriptionEntityList(getList("selectSignalEventSubscriptionsByNameAndExecution", params, signalEventSubscriptionByNameAndExecutionMatcher, true));
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecutionAndType(final String executionId, final String type) {
        Map<String, String> params = new HashMap<>();
        params.put("executionId", executionId);
        params.put("eventType", type);
        return getList("selectEventSubscriptionsByExecutionAndType", params, eventSubscriptionsByExecutionAndTypeMatcher, true);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndActivityId(final String processInstanceId, final String activityId, final String type) {
        Map<String, String> params = new HashMap<>();
        params.put("processInstanceId", processInstanceId);
        params.put("eventType", type);
        params.put("activityId", activityId);
        return getList("selectEventSubscriptionsByProcessInstanceTypeAndActivity", params, eventSubscriptionsByProcInstTypeAndActivityMatcher, true);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecution(final String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        
        // If the execution has been inserted in the same command execution as this query, there can't be any in the database 
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            return getListFromCache(eventSubscriptionsByExecutionIdMatcher, executionId);
        }
        
        return getList(dbSqlSession, "selectEventSubscriptionsByExecution", executionId, eventSubscriptionsByExecutionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EventSubscriptionEntity> findEventSubscriptionsByTypeAndProcessDefinitionId(String type, String processDefinitionId, String tenantId) {
        final String query = "selectEventSubscriptionsByTypeAndProcessDefinitionId";
        Map<String, String> params = new HashMap<>();
        if (type != null) {
            params.put("eventType", type);
        }
        params.put("processDefinitionId", processDefinitionId);
        if (tenantId != null && !tenantId.equals(ProcessEngineConfiguration.NO_TENANT_ID)) {
            params.put("tenantId", tenantId);
        }
        return getDbSqlSession().selectList(query, params);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByName(final String type, final String eventName, final String tenantId) {

        Map<String, String> params = new HashMap<>();
        params.put("eventType", type);
        params.put("eventName", eventName);
        if (tenantId != null && !tenantId.equals(ProcessEngineConfiguration.NO_TENANT_ID)) {
            params.put("tenantId", tenantId);
        }

        return getList("selectEventSubscriptionsByName", params, eventSubscriptionsByNameMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EventSubscriptionEntity> findEventSubscriptionsByNameAndExecution(String type, String eventName, String executionId) {
        final String query = "selectEventSubscriptionsByNameAndExecution";
        Map<String, String> params = new HashMap<>();
        params.put("eventType", type);
        params.put("eventName", eventName);
        params.put("executionId", executionId);
        return getDbSqlSession().selectList(query, params);
    }

    @Override
    public MessageEventSubscriptionEntity findMessageStartEventSubscriptionByName(String messageName, String tenantId) {
        Map<String, String> params = new HashMap<>();
        params.put("eventName", messageName);
        if (tenantId != null && !tenantId.equals(ProcessEngineConfiguration.NO_TENANT_ID)) {
            params.put("tenantId", tenantId);
        }
        MessageEventSubscriptionEntity entity = (MessageEventSubscriptionEntity) getDbSqlSession().selectOne("selectMessageStartEventSubscriptionByName", params);
        return entity;
    }

    @Override
    public void updateEventSubscriptionTenantId(String oldTenantId, String newTenantId) {
        Map<String, String> params = new HashMap<>();
        params.put("oldTenantId", oldTenantId);
        params.put("newTenantId", newTenantId);
        getDbSqlSession().update("updateTenantIdOfEventSubscriptions", params);
    }

    @Override
    public void deleteEventSubscriptionsForProcessDefinition(String processDefinitionId) {
        getDbSqlSession().delete("deleteEventSubscriptionsForProcessDefinition", processDefinitionId, EventSubscriptionEntityImpl.class);
    }
    
    @Override
    public void deleteEventSubscriptionsByExecutionId(String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            deleteCachedEntities(dbSqlSession, eventSubscriptionsByExecutionIdMatcher, executionId);
        } else {
            bulkDelete("deleteEventSubscriptionsByExecutionId", eventSubscriptionsByExecutionIdMatcher, executionId);
        }
    }

    protected List<SignalEventSubscriptionEntity> toSignalEventSubscriptionEntityList(List<EventSubscriptionEntity> result) {
        List<SignalEventSubscriptionEntity> signalEventSubscriptionEntities = new ArrayList<>(result.size());
        for (EventSubscriptionEntity eventSubscriptionEntity : result) {
            signalEventSubscriptionEntities.add((SignalEventSubscriptionEntity) eventSubscriptionEntity);
        }
        return signalEventSubscriptionEntities;
    }

    protected List<MessageEventSubscriptionEntity> toMessageEventSubscriptionEntityList(List<EventSubscriptionEntity> result) {
        List<MessageEventSubscriptionEntity> messageEventSubscriptionEntities = new ArrayList<>(result.size());
        for (EventSubscriptionEntity eventSubscriptionEntity : result) {
            messageEventSubscriptionEntities.add((MessageEventSubscriptionEntity) eventSubscriptionEntity);
        }
        return messageEventSubscriptionEntities;
    }

}
