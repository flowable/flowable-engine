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
package org.flowable.eventregistry.impl.payload;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventInfoAwarePayloadExtractor;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;

public class HeadersPayloadExtractor<T> implements InboundEventInfoAwarePayloadExtractor<T> {

    @Override
    public Collection<EventPayloadInstance> extractPayload(EventModel eventModel, FlowableEventInfo<T> event) {
        Collection<EventPayload> headers = eventModel.getHeaders();
        if (headers.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> eventHeaders = event.getHeaders();
        if (eventHeaders == null || eventHeaders.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<EventPayloadInstance> eventPayloadHeaderInstances = new ArrayList<>(headers.size());
        for (EventPayload header : headers) {
            if (eventHeaders.containsKey(header.getName())) {
                Object headerValueObject = eventHeaders.get(header.getName());
                Object headerValue = headerValueObject;
                if (headerValueObject instanceof byte[] headerValueBytes) {
                    headerValue = convertBytesHeaderValue(headerValueBytes, header);
                }
                eventPayloadHeaderInstances.add(new EventPayloadInstanceImpl(header, headerValue));
            }
        }
        return eventPayloadHeaderInstances;
    }

    protected Object convertBytesHeaderValue(byte[] headerValue, EventPayload eventHeaderDef) {
        if (EventPayloadTypes.STRING.equals(eventHeaderDef.getType())) {
            return convertBytesToString(headerValue);

        } else if (EventPayloadTypes.DOUBLE.equals(eventHeaderDef.getType())) {
            return Double.valueOf(convertBytesToString(headerValue));

        } else if (EventPayloadTypes.INTEGER.equals(eventHeaderDef.getType())) {
            return Integer.valueOf(convertBytesToString(headerValue));

        } else if (EventPayloadTypes.LONG.equals(eventHeaderDef.getType())) {
            return Long.valueOf(convertBytesToString(headerValue));

        } else if (EventPayloadTypes.BOOLEAN.equals(eventHeaderDef.getType())) {
            return Boolean.valueOf(convertBytesToString(headerValue));
            
        } else {
            return headerValue;
        }
    }
    
    protected String convertBytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
