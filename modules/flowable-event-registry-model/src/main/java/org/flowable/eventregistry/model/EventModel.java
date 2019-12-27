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
package org.flowable.eventregistry.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Joram Barrez
 */
public class EventModel {

    protected String key;
    protected String name;
    protected Collection<String> inboundChannelKeys = new ArrayList<>();
    protected Collection<String> outboundChannelKeys = new ArrayList<>();
    protected Collection<EventCorrelationParameter> correlationParameters = new ArrayList<>();
    protected Collection<EventPayload> payload = new ArrayList<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<String> getInboundChannelKeys() {
        return inboundChannelKeys;
    }

    public void setInboundChannelKeys(Collection<String> inboundChannelKeys) {
        this.inboundChannelKeys = inboundChannelKeys;
    }

    public Collection<String> getOutboundChannelKeys() {
        return outboundChannelKeys;
    }

    public void setOutboundChannelKeys(Collection<String> outboundChannelKeys) {
        this.outboundChannelKeys = outboundChannelKeys;
    }

    public Collection<EventCorrelationParameter> getCorrelationParameters() {
        return correlationParameters;
    }

    public void setCorrelationParameters(Collection<EventCorrelationParameter> correlationParameters) {
        this.correlationParameters = correlationParameters;
    }

    public Collection<EventPayload> getPayload() {
        return payload;
    }

    public void setPayload(Collection<EventPayload> payload) {
        this.payload = payload;
    }

}
