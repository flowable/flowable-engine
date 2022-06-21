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
package org.flowable.eventregistry.impl;

import java.util.Map;

import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEvent;
import org.flowable.eventregistry.model.InboundChannelModel;

public class FlowableEventInfoImpl<T> implements FlowableEventInfo<T> {
    
    protected InboundEvent inboundEvent;
    protected T payload;
    protected InboundChannelModel inboundChannel;

    public FlowableEventInfoImpl(InboundEvent inboundEvent, T payload, InboundChannelModel inboundChannel) {
        this.inboundEvent = inboundEvent;
        this.payload = payload;
        this.inboundChannel = inboundChannel;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return inboundEvent.getHeaders();
    }
    
    @Override
    public T getPayload() {
        return payload;
    }

    @Override
    public Object getRawEvent() {
        return inboundEvent.getRawEvent();
    }

    @Override
    public InboundChannelModel getInboundChannel() {
        return inboundChannel;
    }
}
