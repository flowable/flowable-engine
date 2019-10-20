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

package org.flowable.eventsubscription.service.impl.persistence.entity;

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.Signal;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.EventSubscriptionServiceConfiguration;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.data.EventSubscriptionDataManager;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class EventSubscriptionEntityManagerImpl extends AbstractEntityManager<EventSubscriptionEntity> implements EventSubscriptionEntityManager {

    protected EventSubscriptionDataManager eventSubscriptionDataManager;

    public EventSubscriptionEntityManagerImpl(EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration, 
                    EventSubscriptionDataManager eventSubscriptionDataManager) {
        
        super(eventSubscriptionServiceConfiguration);
        this.eventSubscriptionDataManager = eventSubscriptionDataManager;
    }

    @Override
    protected DataManager<EventSubscriptionEntity> getDataManager() {
        return eventSubscriptionDataManager;
    }

    @Override
    public CompensateEventSubscriptionEntity createCompensateEventSubscription() {
        return eventSubscriptionDataManager.createCompensateEventSubscription();
    }

    @Override
    public MessageEventSubscriptionEntity createMessageEventSubscription() {
        return eventSubscriptionDataManager.createMessageEventSubscription();
    }

    @Override
    public SignalEventSubscriptionEntity createSignalEventSubscription() {
        return eventSubscriptionDataManager.createSignalEventSubscription();
    }

    @Override
    public GenericEventSubscriptionEntity createGenericEventSubscription() {
        return eventSubscriptionDataManager.createGenericEventSubscriptionEntity();
    }

    public EventSubscription createEventSubscription(EventSubscriptionBuilder eventSubscriptionBuilder) {
        if (SignalEventSubscriptionEntity.EVENT_TYPE.equals(eventSubscriptionBuilder.getEventType())) {
            return insertSignalEvent(eventSubscriptionBuilder);
            
        } else if (MessageEventSubscriptionEntity.EVENT_TYPE.equals(eventSubscriptionBuilder.getEventType())) {
            return insertMessageEvent(eventSubscriptionBuilder);
            
        } else if (CompensateEventSubscriptionEntity.EVENT_TYPE.equals(eventSubscriptionBuilder.getEventType())) {
            return insertCompensationEvent(eventSubscriptionBuilder);
        
        } else {
            return insertGenericEvent(eventSubscriptionBuilder);
        }
    }

    @Override
    public List<CompensateEventSubscriptionEntity> findCompensateEventSubscriptionsByExecutionId(String executionId) {
        return findCompensateEventSubscriptionsByExecutionIdAndActivityId(executionId, null);
    }

    @Override
    public List<CompensateEventSubscriptionEntity> findCompensateEventSubscriptionsByExecutionIdAndActivityId(String executionId, String activityId) {
        List<EventSubscriptionEntity> eventSubscriptions = findEventSubscriptionsByExecutionAndType(executionId, "compensate");
        List<CompensateEventSubscriptionEntity> result = new ArrayList<>();
        for (EventSubscriptionEntity eventSubscriptionEntity : eventSubscriptions) {
            if (eventSubscriptionEntity instanceof CompensateEventSubscriptionEntity) {
                if (activityId == null || activityId.equals(eventSubscriptionEntity.getActivityId())) {
                    result.add((CompensateEventSubscriptionEntity) eventSubscriptionEntity);
                }
            }
        }
        return result;
    }

    @Override
    public List<CompensateEventSubscriptionEntity> findCompensateEventSubscriptionsByProcessInstanceIdAndActivityId(String processInstanceId, String activityId) {
        List<EventSubscriptionEntity> eventSubscriptions = findEventSubscriptionsByProcessInstanceAndActivityId(processInstanceId, activityId, "compensate");
        List<CompensateEventSubscriptionEntity> result = new ArrayList<>();
        for (EventSubscriptionEntity eventSubscriptionEntity : eventSubscriptions) {
            result.add((CompensateEventSubscriptionEntity) eventSubscriptionEntity);
        }
        return result;
    }

    @Override
    public long findEventSubscriptionCountByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
        return eventSubscriptionDataManager.findEventSubscriptionCountByQueryCriteria(eventSubscriptionQueryImpl);
    }

    @Override
    public List<EventSubscription> findEventSubscriptionsByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
        return eventSubscriptionDataManager.findEventSubscriptionsByQueryCriteria(eventSubscriptionQueryImpl);
    }

    @Override
    public List<MessageEventSubscriptionEntity> findMessageEventSubscriptionsByProcessInstanceAndEventName(String processInstanceId, String eventName) {
        return eventSubscriptionDataManager.findMessageEventSubscriptionsByProcessInstanceAndEventName(processInstanceId, eventName);
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByEventName(String eventName, String tenantId) {
        return eventSubscriptionDataManager.findSignalEventSubscriptionsByEventName(eventName, tenantId);
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByProcessInstanceAndEventName(String processInstanceId, String eventName) {
        return eventSubscriptionDataManager.findSignalEventSubscriptionsByProcessInstanceAndEventName(processInstanceId, eventName);
    }
    
    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByScopeAndEventName(String scopeId, String scopeType, String eventName) {
        return eventSubscriptionDataManager.findSignalEventSubscriptionsByScopeAndEventName(scopeId, scopeType, eventName);
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByNameAndExecution(String name, String executionId) {
        return eventSubscriptionDataManager.findSignalEventSubscriptionsByNameAndExecution(name, executionId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecutionAndType(final String executionId, final String type) {
        return eventSubscriptionDataManager.findEventSubscriptionsByExecutionAndType(executionId, type);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndActivityId(String processInstanceId, String activityId, String type) {
        return eventSubscriptionDataManager.findEventSubscriptionsByProcessInstanceAndActivityId(processInstanceId, activityId, type);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecution(final String executionId) {
        return eventSubscriptionDataManager.findEventSubscriptionsByExecution(executionId);
    }
    
    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsBySubScopeId(final String subScopeId) {
        return eventSubscriptionDataManager.findEventSubscriptionsBySubScopeId(subScopeId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByTypeAndProcessDefinitionId(String type, String processDefinitionId, String tenantId) {
        return eventSubscriptionDataManager.findEventSubscriptionsByTypeAndProcessDefinitionId(type, processDefinitionId, tenantId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByName(String type, String eventName, String tenantId) {
        return eventSubscriptionDataManager.findEventSubscriptionsByName(type, eventName, tenantId);
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByNameAndExecution(String type, String eventName, String executionId) {
        return eventSubscriptionDataManager.findEventSubscriptionsByNameAndExecution(type, eventName, executionId);
    }

    @Override
    public MessageEventSubscriptionEntity findMessageStartEventSubscriptionByName(String messageName, String tenantId) {
        return eventSubscriptionDataManager.findMessageStartEventSubscriptionByName(messageName, tenantId);
    }

    @Override
    public void updateEventSubscriptionTenantId(String oldTenantId, String newTenantId) {
        eventSubscriptionDataManager.updateEventSubscriptionTenantId(oldTenantId, newTenantId);
    }

    @Override
    public void deleteEventSubscriptionsForProcessDefinition(String processDefinitionId) {
        eventSubscriptionDataManager.deleteEventSubscriptionsForProcessDefinition(processDefinitionId);
    }

    @Override
    public void deleteEventSubscriptionsByExecutionId(String executionId) {
        eventSubscriptionDataManager.deleteEventSubscriptionsByExecutionId(executionId);
    }
    
    @Override
    public void deleteEventSubscriptionsForScopeIdAndType(String scopeId, String scopeType) {
        eventSubscriptionDataManager.deleteEventSubscriptionsForScopeIdAndType(scopeId, scopeType);
    }

    @Override
    public void deleteEventSubscriptionsForScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        eventSubscriptionDataManager.deleteEventSubscriptionsForScopeDefinitionIdAndType(scopeDefinitionId, scopeType);
    }

    protected SignalEventSubscriptionEntity insertSignalEvent(EventSubscriptionBuilder eventSubscriptionBuilder) {
        SignalEventSubscriptionEntity subscriptionEntity = createSignalEventSubscription();
        subscriptionEntity.setExecutionId(eventSubscriptionBuilder.getExecutionId());
        subscriptionEntity.setProcessInstanceId(eventSubscriptionBuilder.getProcessInstanceId());
        Signal signal = eventSubscriptionBuilder.getSignal();
        if (signal != null) {
            subscriptionEntity.setEventName(signal.getName());
            if (signal.getScope() != null) {
                subscriptionEntity.setConfiguration(signal.getScope());
            }
        } else {
            subscriptionEntity.setEventName(eventSubscriptionBuilder.getEventName());
        }

        subscriptionEntity.setActivityId(eventSubscriptionBuilder.getActivityId());
        subscriptionEntity.setProcessDefinitionId(eventSubscriptionBuilder.getProcessDefinitionId());
        subscriptionEntity.setSubScopeId(eventSubscriptionBuilder.getSubScopeId());
        subscriptionEntity.setScopeId(eventSubscriptionBuilder.getScopeId());
        subscriptionEntity.setScopeDefinitionId(eventSubscriptionBuilder.getScopeDefinitionId());
        subscriptionEntity.setScopeType(eventSubscriptionBuilder.getScopeType());
        
        if (eventSubscriptionBuilder.getTenantId() != null) {
            subscriptionEntity.setTenantId(eventSubscriptionBuilder.getTenantId());
        }
        
        if (subscriptionEntity.getConfiguration() == null) {
            subscriptionEntity.setConfiguration(eventSubscriptionBuilder.getConfiguration());
        }

        insert(subscriptionEntity);
        
        return subscriptionEntity;
    }
    
    protected MessageEventSubscriptionEntity insertMessageEvent(EventSubscriptionBuilder eventSubscriptionBuilder) {
        
        MessageEventSubscriptionEntity subscriptionEntity = createMessageEventSubscription();
        subscriptionEntity.setExecutionId(eventSubscriptionBuilder.getExecutionId());
        subscriptionEntity.setProcessInstanceId(eventSubscriptionBuilder.getProcessInstanceId());
        subscriptionEntity.setEventName(eventSubscriptionBuilder.getEventName());

        subscriptionEntity.setActivityId(eventSubscriptionBuilder.getActivityId());
        subscriptionEntity.setProcessDefinitionId(eventSubscriptionBuilder.getProcessDefinitionId());
        if (eventSubscriptionBuilder.getTenantId() != null) {
            subscriptionEntity.setTenantId(eventSubscriptionBuilder.getTenantId());
        }

        subscriptionEntity.setConfiguration(eventSubscriptionBuilder.getConfiguration());

        insert(subscriptionEntity);
        
        return subscriptionEntity;
    }
    
    protected CompensateEventSubscriptionEntity insertCompensationEvent(EventSubscriptionBuilder eventSubscriptionBuilder) {
        
        CompensateEventSubscriptionEntity eventSubscription = createCompensateEventSubscription();
        eventSubscription.setExecutionId(eventSubscriptionBuilder.getExecutionId());
        eventSubscription.setProcessInstanceId(eventSubscriptionBuilder.getProcessInstanceId());
        eventSubscription.setActivityId(eventSubscriptionBuilder.getActivityId());
        if (eventSubscriptionBuilder.getTenantId() != null) {
            eventSubscription.setTenantId(eventSubscriptionBuilder.getTenantId());
        }

        eventSubscription.setConfiguration(eventSubscriptionBuilder.getConfiguration());

        insert(eventSubscription);
        return eventSubscription;
    }

    protected GenericEventSubscriptionEntity insertGenericEvent(EventSubscriptionBuilder eventSubscriptionBuilder) {
        GenericEventSubscriptionEntity eventSubscription = createGenericEventSubscription();
        eventSubscription.setEventType(eventSubscriptionBuilder.getEventType());
        eventSubscription.setExecutionId(eventSubscriptionBuilder.getExecutionId());
        eventSubscription.setProcessInstanceId(eventSubscriptionBuilder.getProcessInstanceId());
        eventSubscription.setActivityId(eventSubscriptionBuilder.getActivityId());
        eventSubscription.setProcessDefinitionId(eventSubscriptionBuilder.getProcessDefinitionId());
        eventSubscription.setSubScopeId(eventSubscriptionBuilder.getSubScopeId());
        eventSubscription.setScopeId(eventSubscriptionBuilder.getScopeId());
        eventSubscription.setScopeDefinitionId(eventSubscriptionBuilder.getScopeDefinitionId());
        eventSubscription.setScopeType(eventSubscriptionBuilder.getScopeType());

        if (eventSubscriptionBuilder.getTenantId() != null) {
            eventSubscription.setTenantId(eventSubscriptionBuilder.getTenantId());
        }

        eventSubscription.setConfiguration(eventSubscriptionBuilder.getConfiguration());

        insert(eventSubscription);

        return eventSubscription;
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

    public EventSubscriptionDataManager getEventSubscriptionDataManager() {
        return eventSubscriptionDataManager;
    }

    public void setEventSubscriptionDataManager(EventSubscriptionDataManager eventSubscriptionDataManager) {
        this.eventSubscriptionDataManager = eventSubscriptionDataManager;
    }

}
