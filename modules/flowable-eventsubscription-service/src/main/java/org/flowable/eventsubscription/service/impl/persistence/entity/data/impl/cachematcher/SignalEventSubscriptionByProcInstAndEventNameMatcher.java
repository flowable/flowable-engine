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
package org.flowable.eventsubscription.service.impl.persistence.entity.data.impl.cachematcher;

import java.util.Map;

import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * @author Joram Barrez
 */
public class SignalEventSubscriptionByProcInstAndEventNameMatcher extends CachedEntityMatcherAdapter<EventSubscriptionEntity> {

    @Override
    public boolean isRetained(EventSubscriptionEntity eventSubscriptionEntity, Object parameter) {
        Map<String, String> params = (Map<String, String>) parameter;
        String processInstanceId = params.get("processInstanceId");
        String eventName = params.get("eventName");

        return eventSubscriptionEntity.getEventType() != null && eventSubscriptionEntity.getEventType().equals(SignalEventSubscriptionEntity.EVENT_TYPE)
                && eventSubscriptionEntity.getEventName() != null && eventSubscriptionEntity.getEventName().equals(eventName)
                && eventSubscriptionEntity.getProcessInstanceId() != null && eventSubscriptionEntity.getProcessInstanceId().equals(processInstanceId);
    }

}