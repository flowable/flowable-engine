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
package org.flowable.eventsubscription.service.impl;

import java.util.List;

import org.flowable.common.engine.impl.service.CommonServiceImpl;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.EventSubscriptionServiceConfiguration;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * @author Tijs Rademakers
 */
public class EventSubscriptionServiceImpl extends CommonServiceImpl<EventSubscriptionServiceConfiguration> implements EventSubscriptionService {

    public EventSubscriptionServiceImpl(EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration) {
        super(eventSubscriptionServiceConfiguration);
    }

    @Override
    public EventSubscriptionEntity findById(String eventSubscriptionId) {
        return getEventSubscriptionEntityManager().findById(eventSubscriptionId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByName(String type, String eventName, String tenantId) {
        return getEventSubscriptionEntityManager().findEventSubscriptionsByName(type, eventName, tenantId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecution(String executionId) {
        return getEventSubscriptionEntityManager().findEventSubscriptionsByExecution(executionId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByNameAndExecution(String type, String eventName, String executionId) {
        return getEventSubscriptionEntityManager().findEventSubscriptionsByNameAndExecution(type, eventName, executionId);
    }
    
    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsBySubScopeId(String subScopeId) {
        return getEventSubscriptionEntityManager().findEventSubscriptionsBySubScopeId(subScopeId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndActivityId(String processInstanceId, String activityId, String type) {
        return getEventSubscriptionEntityManager().findEventSubscriptionsByProcessInstanceAndActivityId(processInstanceId, activityId, type);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByTypeAndProcessDefinitionId(String type, String processDefinitionId, String tenantId) {
        return getEventSubscriptionEntityManager().findEventSubscriptionsByTypeAndProcessDefinitionId(type, processDefinitionId, tenantId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecutionAndType(String executionId, String type) {
        return getEventSubscriptionEntityManager().findEventSubscriptionsByExecutionAndType(executionId, type);
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByProcessInstanceAndEventName(String processInstanceId, String eventName) {
        return getEventSubscriptionEntityManager().findSignalEventSubscriptionsByProcessInstanceAndEventName(processInstanceId, eventName);
    }
    
    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByScopeAndEventName(String scopeId, String scopeType, String eventName) {
        return getEventSubscriptionEntityManager().findSignalEventSubscriptionsByScopeAndEventName(scopeId, scopeType, eventName);
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByEventName(String eventName, String tenantId) {
        return getEventSubscriptionEntityManager().findSignalEventSubscriptionsByEventName(eventName, tenantId);
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByNameAndExecution(String eventName, String executionId) {
        return getEventSubscriptionEntityManager().findSignalEventSubscriptionsByNameAndExecution(eventName, executionId);
    }

    @Override
    public List<MessageEventSubscriptionEntity> findMessageEventSubscriptionsByProcessInstanceAndEventName(String processInstanceId, String eventName) {
        return getEventSubscriptionEntityManager().findMessageEventSubscriptionsByProcessInstanceAndEventName(processInstanceId, eventName);
    }

    @Override
    public MessageEventSubscriptionEntity findMessageStartEventSubscriptionByName(String eventName, String tenantId) {
        return getEventSubscriptionEntityManager().findMessageStartEventSubscriptionByName(eventName, tenantId);
    }

    @Override
    public List<CompensateEventSubscriptionEntity> findCompensateEventSubscriptionsByExecutionId(String executionId) {
        return getEventSubscriptionEntityManager().findCompensateEventSubscriptionsByExecutionId(executionId);
    }

    @Override
    public List<CompensateEventSubscriptionEntity> findCompensateEventSubscriptionsByProcessInstanceIdAndActivityId(String processInstanceId, String activityId) {
        return getEventSubscriptionEntityManager().findCompensateEventSubscriptionsByProcessInstanceIdAndActivityId(processInstanceId, activityId);
    }

    @Override
    public List<EventSubscription> findEventSubscriptionsByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQuery) {
        return getEventSubscriptionEntityManager().findEventSubscriptionsByQueryCriteria(eventSubscriptionQuery);
    }

    @Override
    public SignalEventSubscriptionEntity createSignalEventSubscription() {
        return getEventSubscriptionEntityManager().createSignalEventSubscription();
    }

    @Override
    public MessageEventSubscriptionEntity createMessageEventSubscription() {
        return getEventSubscriptionEntityManager().createMessageEventSubscription();
    }

    @Override
    public EventSubscriptionBuilder createEventSubscriptionBuilder() {
        return new EventSubscriptionBuilderImpl(this);
    }

    @Override
    public void insertEventSubscription(EventSubscriptionEntity eventSubscription) {
        getEventSubscriptionEntityManager().insert(eventSubscription);
    }

    @Override
    public void updateEventSubscriptionTenantId(String oldTenantId, String newTenantId) {
        getEventSubscriptionEntityManager().updateEventSubscriptionTenantId(oldTenantId, newTenantId);
    }

    @Override
    public void updateEventSubscription(EventSubscriptionEntity eventSubscription) {
        getEventSubscriptionEntityManager().update(eventSubscription);
    }

    @Override
    public void deleteEventSubscription(EventSubscriptionEntity eventSubscription) {
        getEventSubscriptionEntityManager().delete(eventSubscription);
    }

    @Override
    public void deleteEventSubscriptionsByExecutionId(String executionId) {
        getEventSubscriptionEntityManager().deleteEventSubscriptionsByExecutionId(executionId);
    }
    
    @Override
    public void deleteEventSubscriptionsForScopeIdAndType(String scopeId, String scopeType) {
        getEventSubscriptionEntityManager().deleteEventSubscriptionsForScopeIdAndType(scopeId, scopeType);
    }

    @Override
    public void deleteEventSubscriptionsForProcessDefinition(String processDefinitionId) {
        getEventSubscriptionEntityManager().deleteEventSubscriptionsForProcessDefinition(processDefinitionId);
    }

    @Override
    public void deleteEventSubscriptionsForScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        getEventSubscriptionEntityManager().deleteEventSubscriptionsForScopeDefinitionIdAndType(scopeDefinitionId, scopeType);
    }

    @Override
    public void deleteEventSubscriptionsForScopeDefinitionIdAndTypeAndNullScopeId(String scopeDefinitionId, String scopeType) {
        getEventSubscriptionEntityManager().deleteEventSubscriptionsForScopeDefinitionIdAndTypeAndNullScopeId(scopeDefinitionId, scopeType);
    }

    public EventSubscription createEventSubscription(EventSubscriptionBuilder builder) {
        return getEventSubscriptionEntityManager().createEventSubscription(builder);
    }

    public EventSubscriptionEntityManager getEventSubscriptionEntityManager() {
        return configuration.getEventSubscriptionEntityManager();
    }
}
