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
package org.flowable.mongodb.persistence.manager;

import java.util.Collections;
import java.util.List;

import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.data.EventSubscriptionDataManager;
import org.flowable.engine.runtime.EventSubscription;

/**
 * @author Joram Barrez
 */
public class MongoDbEventSubscriptionDataManager extends AbstractMongoDbDataManager implements EventSubscriptionDataManager {
    
    public static final String COLLECTION_EVENT_SUBSCRIPTION = "eventSubscriptions";

    @Override
    public EventSubscriptionEntity create() {
        return null;
    }

    @Override
    public EventSubscriptionEntity findById(String entityId) {
        return null;
    }

    @Override
    public void insert(EventSubscriptionEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventSubscriptionEntity update(EventSubscriptionEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void delete(EventSubscriptionEntity entity) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public MessageEventSubscriptionEntity createMessageEventSubscription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SignalEventSubscriptionEntity createSignalEventSubscription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompensateEventSubscriptionEntity createCompensateEventSubscription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findEventSubscriptionCountByQueryCriteria(EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EventSubscription> findEventSubscriptionsByQueryCriteria(
            EventSubscriptionQueryImpl eventSubscriptionQueryImpl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<MessageEventSubscriptionEntity> findMessageEventSubscriptionsByProcessInstanceAndEventName(
            String processInstanceId, String eventName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByEventName(String eventName, String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByProcessInstanceAndEventName(
            String processInstanceId, String eventName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SignalEventSubscriptionEntity> findSignalEventSubscriptionsByNameAndExecution(String name, String executionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecutionAndType(String executionId, String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByProcessInstanceAndActivityId(String processInstanceId, String activityId, String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByExecution(String executionId) {
        return Collections.emptyList();
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByTypeAndProcessDefinitionId(String type,
            String processDefinitionId, String tenantId) {
        // TODO 
        return Collections.emptyList();
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByName(String type, String eventName, String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EventSubscriptionEntity> findEventSubscriptionsByNameAndExecution(String type, String eventName, String executionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageEventSubscriptionEntity findMessageStartEventSubscriptionByName(String messageName, String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEventSubscriptionTenantId(String oldTenantId, String newTenantId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteEventSubscriptionsForProcessDefinition(String processDefinitionId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteEventSubscriptionsByExecutionId(String executionId) {
        throw new UnsupportedOperationException();        
    }

}
