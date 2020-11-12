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
package org.flowable.eventsubscription.service;

import java.util.List;

import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * Service which provides access to eventsubscriptions.
 * 
 * @author Tijs Rademakers
 */
public interface EventSubscriptionService {
    
    EventSubscriptionEntity findById(String eventSubscriptionId);
    
    List<EventSubscriptionEntity> findEventSubscriptionsByName(String type, String eventName, String tenantId);
    
    List<EventSubscriptionEntity> findEventSubscriptionsByExecution(String executionId);
    
    List<EventSubscriptionEntity> findEventSubscriptionsByNameAndExecution(String type, String eventName, String executionId);
    
    List<EventSubscriptionEntity> findEventSubscriptionsBySubScopeId(String subScopeId);
    
    List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndActivityId(String processInstanceId, String activityId, String type);
    
    List<EventSubscriptionEntity> findEventSubscriptionsByTypeAndProcessDefinitionId(String type, String processDefinitionId, String tenantId);
    
    List<EventSubscriptionEntity> findEventSubscriptionsByExecutionAndType(String executionId, String type);
    
    List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByProcessInstanceAndEventName(String processInstanceId, String eventName);
    
    List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByScopeAndEventName(String scopeId, String scopeType, String eventName);
    
    List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByEventName(String eventName, String tenantId);
    
    List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByNameAndExecution(String eventName, String executionId);
    
    List<MessageEventSubscriptionEntity> findMessageEventSubscriptionsByProcessInstanceAndEventName(String processInstanceId, String eventName);
    
    MessageEventSubscriptionEntity findMessageStartEventSubscriptionByName(String eventName, String tenantId);
    
    List<CompensateEventSubscriptionEntity> findCompensateEventSubscriptionsByExecutionId(String executionId);
    
    List<CompensateEventSubscriptionEntity> findCompensateEventSubscriptionsByProcessInstanceIdAndActivityId(String processInstanceId, String activityId);
    
    List<EventSubscription> findEventSubscriptionsByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQuery);
    
    SignalEventSubscriptionEntity createSignalEventSubscription();
    
    MessageEventSubscriptionEntity createMessageEventSubscription();
    
    EventSubscriptionBuilder createEventSubscriptionBuilder();
    
    void insertEventSubscription(EventSubscriptionEntity eventSubscription);
    
    void updateEventSubscriptionTenantId(String oldTenantId, String newTenantId);
    
    void updateEventSubscription(EventSubscriptionEntity eventSubscription);
    
    void deleteEventSubscription(EventSubscriptionEntity eventSubscription);
    
    void deleteEventSubscriptionsByExecutionId(String executionId);
    
    void deleteEventSubscriptionsForScopeIdAndType(String scopeId, String scopeType);
    
    void deleteEventSubscriptionsForProcessDefinition(String processDefinitionId);

    void deleteEventSubscriptionsForScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType);

    void deleteEventSubscriptionsForScopeDefinitionIdAndTypeAndNullScopeId(String scopeDefinitionId, String scopeType);
    
}
