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
package org.flowable.common.engine.impl.eventbus;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.eventbus.FlowableEventBus;
import org.flowable.common.engine.api.eventbus.FlowableEventBusItem;
import org.flowable.common.engine.api.eventbus.FlowableEventPublisher;

public class FlowableEventPublisherImpl implements FlowableEventPublisher {

    protected FlowableEventBus flowableEventBus;
    protected boolean enabled = true;

    public FlowableEventPublisherImpl(FlowableEventBus flowableEventBus) {
        this.flowableEventBus = flowableEventBus;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void publishEvent(FlowableEventBusItem event) {
        if (!enabled) {
            return;
        }

        if (event == null) {
            throw new FlowableIllegalArgumentException("Event cannot be null.");
        }

        if (event.getType() == null) {
            throw new FlowableIllegalArgumentException("Event type cannot be null.");
        }
        
        flowableEventBus.sendEvent(event);
    }

}
