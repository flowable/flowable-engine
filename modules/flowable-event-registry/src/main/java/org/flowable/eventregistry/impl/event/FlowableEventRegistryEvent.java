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
package org.flowable.eventregistry.impl.event;

import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.runtime.EventInstance;

/**
 * @author Joram Barrez
 */
public class FlowableEventRegistryEvent implements EventRegistryEvent {
    
    protected String type;
    protected EventInstance eventInstance;

    public FlowableEventRegistryEvent(EventInstance eventInstance) {
        this.type = eventInstance.getEventKey();
        this.eventInstance = eventInstance;
    }

    public EventInstance getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstance eventInstance) {
        this.eventInstance = eventInstance;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public EventInstance getEventObject() {
        return eventInstance;
    }
}
