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
package org.flowable.common.engine.impl.eventregistry.definition;

import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.InboundEventChannelAdapter;
import org.flowable.common.engine.api.eventregistry.InboundEventDeserializer;
import org.flowable.common.engine.api.eventregistry.InboundEventKeyDetector;
import org.flowable.common.engine.api.eventregistry.InboundEventPayloadExtractor;
import org.flowable.common.engine.api.eventregistry.InboundEventProcessingPipeline;
import org.flowable.common.engine.api.eventregistry.InboundEventTransformer;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinition;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinitionBuilder;
import org.flowable.common.engine.impl.eventregistry.JsonFieldBasedInboundEventKeyDetector;
import org.flowable.common.engine.impl.eventregistry.JsonPathBasedInboundEventKeyDetector;
import org.flowable.common.engine.impl.eventregistry.deserializer.StringToJsonDeserializer;
import org.flowable.common.engine.impl.eventregistry.payload.JsonFieldToMapPayloadExtractor;
import org.flowable.common.engine.impl.eventregistry.pipeline.DefaultEventProcessingPipeline;
import org.flowable.common.engine.impl.eventregistry.transformer.DefaultInboundEventTransformer;

/**
 * @author Joram Barrez
 */
public class ChannelDefinitionBuilderImpl implements ChannelDefinitionBuilder {

    protected EventRegistry eventRegistry;

    protected String key;
    protected InboundEventChannelAdapter inboundEventChannelAdapter;
    protected InboundEventProcessingPipelineBuilder inboundEventProcessingPipelineBuilder;

    public ChannelDefinitionBuilderImpl(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public ChannelDefinitionBuilder key(String key) {
        this.key = key;
        return this;
    }

    @Override
    public InboundEventProcessingPipelineBuilder channelAdapter(InboundEventChannelAdapter inboundEventChannelAdapter) {
        this.inboundEventChannelAdapter = inboundEventChannelAdapter;
        this.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl(this);
        return this.inboundEventProcessingPipelineBuilder;
    }

    @Override
    public ChannelDefinition register() {
        ChannelDefinitionImpl channelDefinition = new ChannelDefinitionImpl();
        channelDefinition.setKey(key);
        channelDefinition.setInboundEventChannelAdapter(inboundEventChannelAdapter);

        InboundEventProcessingPipeline inboundEventProcessingPipeline = inboundEventProcessingPipelineBuilder.build();
        channelDefinition.setInboundEventProcessingPipeline(inboundEventProcessingPipeline);

        eventRegistry.registerChannelDefinition(channelDefinition);

        return channelDefinition;
    }

    public static class InboundEventProcessingPipelineBuilderImpl implements InboundEventProcessingPipelineBuilder {

        protected ChannelDefinitionBuilderImpl channelDefinitionBuilder;

        protected InboundEventProcessingPipeline customInboundEventProcessingPipeline;
        protected InboundEventDeserializer inboundEventDeserializer;
        protected InboundEventKeyDetector inboundEventKeyDetector;
        protected InboundEventPayloadExtractor inboundEventPayloadExtractor;
        protected InboundEventTransformer inboundEventTransformer;

        public InboundEventProcessingPipelineBuilderImpl(ChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            this.channelDefinitionBuilder = channelDefinitionBuilder;
        }

        @Override
        public InboundEventKeyDetectorBuilder deserializeToJson() {
            this.inboundEventDeserializer = new StringToJsonDeserializer();
            return new InboundEventDefinitionKeyDetectorBuilderImpl(this);
        }

        @Override
        public  InboundEventKeyDetectorBuilder customDeserializer(InboundEventDeserializer deserializer) {
            this.inboundEventDeserializer = deserializer;
            return new InboundEventDefinitionKeyDetectorBuilderImpl(this);
        }

        @Override
        public ChannelDefinitionBuilder customEventProcessingPipeline(InboundEventProcessingPipeline inboundEventProcessingPipeline) {
            this.customInboundEventProcessingPipeline = inboundEventProcessingPipeline;
            return channelDefinitionBuilder;
        }

        @Override
        public InboundEventProcessingPipeline build() {
            if (customInboundEventProcessingPipeline != null) {
                return customInboundEventProcessingPipeline;
            } else {
                return new DefaultEventProcessingPipeline(inboundEventDeserializer,
                    inboundEventKeyDetector, inboundEventPayloadExtractor, inboundEventTransformer);
            }
        }

    }

    public static class InboundEventDefinitionKeyDetectorBuilderImpl implements InboundEventKeyDetectorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder;

        public InboundEventDefinitionKeyDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventPayloadExtractorBuilder detectEventKeyUsingJsonField(String field) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new JsonFieldBasedInboundEventKeyDetector(field);
            return new InboundEventPayloadExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadExtractorBuilder detectEventKeyUsingJsonPathExpression(String jsonPathExpression) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new JsonPathBasedInboundEventKeyDetector(jsonPathExpression);
            return new InboundEventPayloadExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadExtractorBuilder detectEventKeyUsingCustomKeyDetector(InboundEventKeyDetector inboundEventKeyDetector) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = inboundEventKeyDetector;
            return new InboundEventPayloadExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventPayloadExtractorBuilderImpl implements InboundEventPayloadExtractorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder;

        public InboundEventPayloadExtractorBuilderImpl(InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTransformerBuilder jsonFieldsMapDirectlyToPayload() {
            this.inboundEventProcessingPipelineBuilder.inboundEventPayloadExtractor = new JsonFieldToMapPayloadExtractor();
            return new InboundEventTransformerBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventTransformerBuilder customPayloadExtractor(InboundEventPayloadExtractor inboundEventPayloadExtractor) {
            this.inboundEventProcessingPipelineBuilder.inboundEventPayloadExtractor = inboundEventPayloadExtractor;
            return new InboundEventTransformerBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventTransformerBuilderImpl implements InboundEventTransformerBuilder {

        protected InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder;

        public InboundEventTransformerBuilderImpl(InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public ChannelDefinitionBuilder customTransformer(InboundEventTransformer inboundEventTransformer) {
            this.inboundEventProcessingPipelineBuilder.inboundEventTransformer = inboundEventTransformer;
            return this.inboundEventProcessingPipelineBuilder.channelDefinitionBuilder;
        }

        @Override
        public ChannelDefinition register() {
            this.inboundEventProcessingPipelineBuilder.inboundEventTransformer = new DefaultInboundEventTransformer();
            return this.inboundEventProcessingPipelineBuilder.channelDefinitionBuilder.register();
        }

    }

}
