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

import java.util.Collections;
import java.util.Map;

import org.flowable.eventregistry.api.InboundEvent;

/**
 * @author Filip Hrisafov
 */
public class DefaultInboundEvent implements InboundEvent {

    protected final Object rawEvent;
    protected final Map<String, Object> headers;

    public DefaultInboundEvent(Object rawEvent) {
        this(rawEvent, Collections.emptyMap());
    }

    public DefaultInboundEvent(Object rawEvent, Map<String, Object> headers) {
        this.rawEvent = rawEvent;
        this.headers = headers;
    }

    @Override
    public Object getRawEvent() {
        return rawEvent;
    }

    @Override
    public Object getBody() {
        return rawEvent;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return headers;
    }
}
