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
package org.flowable.eventregistry.spring.rabbit;

import java.util.HashMap;
import java.util.Map;

import org.flowable.eventregistry.api.InboundEventContextExtractor;
import org.flowable.eventregistry.model.EventModel;
import org.springframework.amqp.core.Message;

public class RabbitMessageInboundEventContextExtractor implements InboundEventContextExtractor {
    
    @Override
    public Map<String, Object> extractContextInfo(Object event, EventModel eventModel) {
        Map<String, Object> contextInfo = new HashMap<>();
        
        Message message = (Message) event;
        Map<String, Object> headerMap = message.getMessageProperties().getHeaders();
        for (String headerName : headerMap.keySet()) {
            contextInfo.put(headerName, headerMap.get(headerName));
        }
        
        return contextInfo;
    }

}
