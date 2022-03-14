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
package org.flowable.eventregistry.spring.jms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Message;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.InboundEventContextExtractor;
import org.flowable.eventregistry.model.EventModel;

public class JmsMessageInboundEventContextExtractor implements InboundEventContextExtractor {
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> extractContextInfo(Object event, EventModel eventModel) {
        Map<String, Object> contextInfo = new HashMap<>();
        
        try {
            Message message = (Message) event;
            Iterator<String> headerNameIterator = message.getPropertyNames().asIterator();
            while (headerNameIterator.hasNext()) {
                String headerName = headerNameIterator.next();
                contextInfo.put(headerName, message.getObjectProperty(headerName));
            }
            
        } catch (Exception e) {
            throw new FlowableException("Exception while getting header properties", e);
        }
        
        return contextInfo;
    }

}
