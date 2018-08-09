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
package org.flowable.mongodb.persistence.mapper;

import org.bson.Document;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.persistence.entity.CompensateEventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntityImpl;
import org.flowable.mongodb.persistence.EntityMapper;

public class EventSubscriptionEntityMapper implements EntityMapper<EventSubscriptionEntityImpl> {

    @Override
    public EventSubscriptionEntityImpl fromDocument(Document document) {
        EventSubscriptionEntityImpl eventEntity = null;
        String eventType = document.getString("eventType");
        if ("signal".equals(eventType)) {
            eventEntity = new SignalEventSubscriptionEntityImpl();
        } else if ("message".equals(eventType)) {
            eventEntity = new MessageEventSubscriptionEntityImpl();
        } else if ("compensate".equals(eventType)) {
            eventEntity = new CompensateEventSubscriptionEntityImpl();
        } else {
            throw new FlowableException("Not supported event type " + eventType);
        }
        
        eventEntity.setId(document.getString("_id"));
        eventEntity.setActivityId(document.getString("activityId"));
        if (document.getString("configuration") != null) {
            eventEntity.setConfiguration(document.getString("configuration"));
        }
        eventEntity.setCreated(document.getDate("created"));
        eventEntity.setEventName(document.getString("eventName"));
        eventEntity.setEventType(eventType);
        eventEntity.setExecutionId(document.getString("executionId"));
        eventEntity.setProcessDefinitionId(document.getString("processDefinitionId"));
        eventEntity.setProcessInstanceId(document.getString("processInstanceId"));
        eventEntity.setRevision(document.getInteger("revision"));
        eventEntity.setTenantId(document.getString("tenantId"));
        
        return eventEntity;
    }
    
    @Override
    public Document toDocument(EventSubscriptionEntityImpl eventEntity) {
        Document eventDocument = new Document();
        eventDocument.append("_id", eventEntity.getId());
        eventDocument.append("activityId", eventEntity.getActivityId());
        eventDocument.append("configuration", eventEntity.getConfiguration());
        eventDocument.append("created", eventEntity.getCreated());
        eventDocument.append("eventName", eventEntity.getEventName());
        eventDocument.append("eventType", eventEntity.getEventType());
        eventDocument.append("executionId", eventEntity.getExecutionId());
        eventDocument.append("processDefinitionId", eventEntity.getProcessDefinitionId());
        eventDocument.append("processInstanceId", eventEntity.getProcessInstanceId());
        eventDocument.append("revision", eventEntity.getRevision());
        eventDocument.append("tenantId", eventEntity.getTenantId());
        
        return eventDocument;
    }

}
