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
 * @author Joram Barrez
 */
public class EventSubscriptionsByScopeDefinitionIdAndTypeMatcher extends CachedEntityMatcherAdapter<EventSubscriptionEntity> {

    @Override
    public boolean isRetained(EventSubscriptionEntity eventSubscriptionEntity, Object parameter) {
        Map<String, String> params = (Map<String, String>) parameter;

        String scopeDefinitionId = params.get("scopeDefinitionId");
        String scopeType = params.get("scopeType");

        return Objects.equals(scopeDefinitionId, eventSubscriptionEntity.getScopeDefinitionId())
            && Objects.equals(scopeType, eventSubscriptionEntity.getScopeType());
    }

}