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
package org.flowable.eventregistry.api.definition;

import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventSerializer;

/**
 * @author Joram Barrez
 */
public interface OutboundChannelDefinitionBuilder {

    OutboundChannelDefinitionBuilder key(String key);

    OutboundEventProcessingPipelineBuilder channelAdapter(OutboundEventChannelAdapter outboundEventChannelAdapter);

    OutboundChannelDefinition register();


    interface OutboundEventProcessingPipelineBuilder {

        OutboundChannelDefinitionBuilder jsonSerializer();

        OutboundChannelDefinitionBuilder serializer(OutboundEventSerializer serializer);

        OutboundChannelDefinitionBuilder eventProcessingPipeline(OutboundEventProcessingPipeline outboundEventProcessingPipeline);

        OutboundEventProcessingPipeline build();

    }

}
