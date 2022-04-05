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
import java.util.HashMap;
import java.util.Map;

import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;

public abstract class BaseMapPayloadExtractor<T> implements InboundEventPayloadExtractor<T> {

    protected Map<String, Object> convertHeaderValues(FlowableEventInfo<T> eventInfo, EventModel eventModel) {
        Map<String, Object> filteredHeaders = new HashMap<>();
        if (eventInfo.getHeaders() != null) {
            Map<String, Object> headers = eventInfo.getHeaders();
            for (String headerName : headers.keySet()) {
                EventPayload eventHeaderDef = eventModel.getPayload(headerName);
                if (eventHeaderDef != null && eventHeaderDef.isHeader()) {
                    Object headerValueObject = headers.get(headerName);
                    if (headerValueObject instanceof byte[]) {
                        byte[] headerValue = (byte[]) headers.get(headerName);
                        convertBytesHeaderValue(headerName, headerValue, filteredHeaders, eventHeaderDef);
                    } else {
                        filteredHeaders.put(headerName, headerValueObject);
                    }
                }
            }
            
            eventInfo.setHeaders(filteredHeaders);
        }
        
        return filteredHeaders;
    }
    
    protected void convertBytesHeaderValue(String headerName, byte[] headerValue, Map<String, Object> filteredHeaders, EventPayload eventHeaderDef) {
        if (EventPayloadTypes.STRING.equals(eventHeaderDef.getType())) {
            filteredHeaders.put(headerName, convertBytesToString(headerValue));

        } else if (EventPayloadTypes.DOUBLE.equals(eventHeaderDef.getType())) {
            filteredHeaders.put(headerName, Double.valueOf(convertBytesToString(headerValue)));

        } else if (EventPayloadTypes.INTEGER.equals(eventHeaderDef.getType())) {
            filteredHeaders.put(headerName, Integer.valueOf(convertBytesToString(headerValue)));

        } else if (EventPayloadTypes.LONG.equals(eventHeaderDef.getType())) {
            filteredHeaders.put(headerName, Long.valueOf(convertBytesToString(headerValue)));

        } else if (EventPayloadTypes.BOOLEAN.equals(eventHeaderDef.getType())) {
            filteredHeaders.put(headerName, Boolean.valueOf(convertBytesToString(headerValue))); 
            
        } else {
            filteredHeaders.put(headerName, headerValue);
        }
    }
    
    protected String convertBytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
