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
package org.flowable.eventsubscription.service.impl.persistence.entity.data;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.GenericEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * @author Joram Barrez
 */
public interface EventSubscriptionDataManager extends DataManager<EventSubscriptionEntity> {

    MessageEventSubscriptionEntity createMessageEventSubscription();

    SignalEventSubscriptionEntity createSignalEventSubscription();

    CompensateEventSubscriptionEntity createCompensateEventSubscription();

    GenericEventSubscriptionEntity createGenericEventSubscriptionEntity();

    long findEventSubscriptionCountByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl);

    List<EventSubscription> findEventSubscriptionsByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl);

    List<MessageEventSubscriptionEntity> findMessageEventSubscriptionsByProcessInstanceAndEventName(final String processInstanceId, final String eventName);

    List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByEventName(final String eventName, final String tenantId);

    List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByProcessInstanceAndEventName(final String processInstanceId, final String eventName);
    
    List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByScopeAndEventName(final String scopeId, final String scopeType, final String eventName);

    List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByNameAndExecution(final String name, final String executionId);

    List<EventSubscriptionEntity> findEventSubscriptionsByExecutionAndType(final String executionId, final String type);
    
    List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndType(final String processInstanceId, final String type);

    List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndActivityId(final String processInstanceId, final String activityId, final String type);

    List<EventSubscriptionEntity> findEventSubscriptionsByExecution(final String executionId);
    
    List<EventSubscriptionEntity> findEventSubscriptionsBySubScopeId(final String subScopeId);

    List<EventSubscriptionEntity> findEventSubscriptionsByTypeAndProcessDefinitionId(String type, String processDefinitionId, String tenantId);
    
    List<EventSubscriptionEntity> findEventSubscriptionsByScopeIdAndType(final String scopeId, final String type);

    List<EventSubscriptionEntity> findEventSubscriptionsByName(final String type, final String eventName, final String tenantId);

    List<EventSubscriptionEntity> findEventSubscriptionsByNameAndExecution(String type, String eventName, String executionId);

    MessageEventSubscriptionEntity findMessageStartEventSubscriptionByName(String messageName, String tenantId);

    void updateEventSubscriptionTenantId(String oldTenantId, String newTenantId);

    void deleteEventSubscriptionsForProcessDefinition(String processDefinitionId);
    
    void deleteEventSubscriptionsByExecutionId(String executionId);
    
    void deleteEventSubscriptionsForScopeIdAndType(String scopeId, String scopeType);

    void deleteEventSubscriptionsForScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType);

    void deleteEventSubscriptionsForScopeDefinitionIdAndTypeAndNullScopeId(String scopeDefinitionId, String scopeType);

}
