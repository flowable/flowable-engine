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
package org.flowable.common.engine.api.eventregistry.definition;

import org.flowable.common.engine.api.eventregistry.InboundEventChannelAdapter;
import org.flowable.common.engine.api.eventregistry.InboundEventKeyDetector;

/**
 * @author Joram Barrez
 */
public interface ChannelDefinitionBuilder {

    ChannelDefinitionBuilder key(String key);

    ChannelDefinitionBuilder inboundAdapter(InboundEventChannelAdapter inboundEventChannelAdapter);

    InboundEventDefinitionKeyDetectorBuilder inboundEventDefinitionKeyDetector();

    ChannelDefinition register();

    interface InboundEventDefinitionKeyDetectorBuilder {

        ChannelDefinitionBuilder mapFromJsonField(String field);

        ChannelDefinitionBuilder mapFromJsonPathExpression(String jsonPathExpression);

        ChannelDefinitionBuilder custom(InboundEventKeyDetector inboundEventKeyDetector);

    }

}
