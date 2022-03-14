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
package org.flowable.eventregistry.impl.header;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.eventregistry.api.InboundEventHeaderInstanceExtractor;
import org.flowable.eventregistry.api.runtime.EventHeaderInstance;
import org.flowable.eventregistry.impl.runtime.EventHeaderInstanceImpl;
import org.flowable.eventregistry.model.EventModel;

public class DefaultInboundEventHeaderInstanceExtractor implements InboundEventHeaderInstanceExtractor {

    @Override
    public Collection<EventHeaderInstance> extractHeaderInstances(EventModel eventModel, Map<String, Object> contextInfo) {
        if (contextInfo == null) {
            return null;
        }
        
        return eventModel.getHeaders().stream()
                .filter(headerDefinition -> contextInfo.containsKey(headerDefinition.getName()))
                .map(headerDefinition -> new EventHeaderInstanceImpl(headerDefinition, contextInfo.get(headerDefinition.getName())))
                .collect(Collectors.toList());
    }
}
