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
import java.util.Objects;

import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * A matcher for event subscriptions with a process start event and specific process definition id and optional configuration (correlation parameter values).
 *
 * @author Micha Kiener
 */
public class EventSubscriptionsByProcessDefinitionIdAndProcessStartEventMatcher extends CachedEntityMatcherAdapter<EventSubscriptionEntity> {

    @Override
    public boolean isRetained(EventSubscriptionEntity eventSubscriptionEntity, Object parameter) {
        Map<String, String> params = (Map<String, String>) parameter;

        String processDefinitionId = params.get("processDefinitionId");
        String eventType = params.get("eventType");
        String activityId = params.get("activityId");
        String configuration = params.get("configuration");

        return Objects.equals(processDefinitionId, eventSubscriptionEntity.getProcessDefinitionId())
            && Objects.equals(eventType, eventSubscriptionEntity.getEventType())
            && Objects.equals(activityId, eventSubscriptionEntity.getActivityId())
            && (configuration == null || Objects.equals(configuration, eventSubscriptionEntity.getConfiguration()));
    }

}
