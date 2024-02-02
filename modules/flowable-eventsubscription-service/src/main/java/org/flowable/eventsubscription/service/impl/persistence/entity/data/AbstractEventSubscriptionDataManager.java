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

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.eventsubscription.service.EventSubscriptionServiceConfiguration;

/**
 * @author Joram Barrez
 */
public abstract class AbstractEventSubscriptionDataManager<EntityImpl extends Entity> extends AbstractDataManager<EntityImpl> {
    
    protected EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration;

    public AbstractEventSubscriptionDataManager(EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration) {
        this.eventSubscriptionServiceConfiguration = eventSubscriptionServiceConfiguration;
    }

    public EventSubscriptionServiceConfiguration getEventSubscriptionServiceConfiguration() {
        return eventSubscriptionServiceConfiguration;
    }

    public void setEventSubscriptionServiceConfiguration(EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration) {
        this.eventSubscriptionServiceConfiguration = eventSubscriptionServiceConfiguration;
    }
}
