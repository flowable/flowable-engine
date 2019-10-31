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
package org.flowable.eventregistry.impl.definition;

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.api.definition.OutboundChannelDefinition;
import org.flowable.eventregistry.api.definition.OutboundChannelDefinitionBuilder;
import org.flowable.eventregistry.impl.pipeline.DefaultOutboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.serialization.EventPayloadToJsonStringSerializer;
import org.flowable.eventregistry.impl.serialization.EventPayloadToXmlStringSerializer;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class OutboundChannelDefinitionBuilderImpl implements OutboundChannelDefinitionBuilder {

    protected EventRegistry eventRegistry;

    protected String key;
    protected OutboundEventChannelAdapter outboundEventChannelAdapter;
    protected OutboundEventProcessingPipelineBuilder outboundEventProcessingPipelineBuilder;

    public OutboundChannelDefinitionBuilderImpl(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public OutboundChannelDefinitionBuilderImpl key(String key) {
        this.key = key;
        return this;
    }
    @Override
    public OutboundEventProcessingPipelineBuilder channelAdapter(OutboundEventChannelAdapter outboundEventChannelAdapter) {
        this.outboundEventChannelAdapter = outboundEventChannelAdapter;
        this.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRegistry, this);
        return this.outboundEventProcessingPipelineBuilder;
    }

    @Override
    public OutboundChannelDefinition register() {
        OutboundChannelDefinitionImpl outboundChannelDefinition = new OutboundChannelDefinitionImpl();
        outboundChannelDefinition.setKey(key);
        outboundChannelDefinition.setOutboundEventChannelAdapter(outboundEventChannelAdapter);

        OutboundEventProcessingPipeline outboundEventProcessingPipeline = this.outboundEventProcessingPipelineBuilder.build();
        outboundChannelDefinition.setOutboundEventProcessingPipeline(outboundEventProcessingPipeline);

        eventRegistry.registerChannelDefinition(outboundChannelDefinition);

        return outboundChannelDefinition;
    }

    public static class OutboundEventProcessingPipelineBuilderImpl implements OutboundEventProcessingPipelineBuilder {

        protected EventRegistry eventRegistry;
        protected OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected OutboundEventSerializer outboundEventSerializer;
        protected OutboundEventProcessingPipeline customOutboundEventProcessingPipeline;

        public OutboundEventProcessingPipelineBuilderImpl(EventRegistry eventRegistry,
            OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder) {
            this.eventRegistry = eventRegistry;
            this.outboundChannelDefinitionBuilder = outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelDefinitionBuilder jsonSerializer() {
            this.outboundEventSerializer = new EventPayloadToJsonStringSerializer();
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelDefinitionBuilder xmlSerializer() {
            this.outboundEventSerializer = new EventPayloadToXmlStringSerializer();
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelDefinitionBuilder serializer(OutboundEventSerializer serializer) {
            this.outboundEventSerializer = serializer;
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelDefinitionBuilder eventProcessingPipeline(OutboundEventProcessingPipeline outboundEventProcessingPipeline) {
            this.customOutboundEventProcessingPipeline = outboundEventProcessingPipeline;
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundEventProcessingPipeline build() {
            if (customOutboundEventProcessingPipeline != null) {
                return customOutboundEventProcessingPipeline;
            } else {
                return new DefaultOutboundEventProcessingPipeline(eventRegistry, outboundEventSerializer);
            }
        }

    }

}
