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

import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinition;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinitionBuilder;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.InboundEventChannelAdapter;
import org.flowable.common.engine.api.eventregistry.InboundEventKeyDetector;
import org.flowable.common.engine.impl.eventregistry.JsonFieldBasedInboundEventKeyDetector;
import org.flowable.common.engine.impl.eventregistry.JsonPathBasedInboundEventKeyDetector;

/**
 * @author Joram Barrez
 */
public class ChannelDefinitionBuilderImpl implements ChannelDefinitionBuilder {

    protected EventRegistry eventRegistry;

    protected String key;
    protected InboundEventChannelAdapter inboundEventChannelAdapter;
    protected InboundEventKeyDetector inboundEventKeyDetector;

    public ChannelDefinitionBuilderImpl(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public ChannelDefinitionBuilder key(String key) {
        this.key = key;
        return this;
    }

    @Override
    public ChannelDefinitionBuilder inboundAdapter(InboundEventChannelAdapter inboundEventChannelAdapter) {
        this.inboundEventChannelAdapter = inboundEventChannelAdapter;
        return this;
    }

    @Override
    public InboundEventDefinitionKeyDetectorBuilder inboundEventDefinitionKeyDetector() {
        return new InboundEventDefinitionKeyDetectorBuilderImpl(this);
    }

    @Override
    public ChannelDefinition register() {
        ChannelDefinitionImpl channelDefinition = new ChannelDefinitionImpl();
        channelDefinition.setKey(key);
        channelDefinition.setInboundEventChannelAdapter(inboundEventChannelAdapter);
        channelDefinition.setInboundEventKeyDetector(inboundEventKeyDetector);

        eventRegistry.registerChannelDefinition(channelDefinition);

        return channelDefinition;
    }


    public static class InboundEventDefinitionKeyDetectorBuilderImpl implements InboundEventDefinitionKeyDetectorBuilder {

        protected ChannelDefinitionBuilderImpl channelDefinitionBuilder;

        public InboundEventDefinitionKeyDetectorBuilderImpl(ChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            this.channelDefinitionBuilder = channelDefinitionBuilder;
        }

        @Override
        public ChannelDefinitionBuilder mapFromJsonField(String field) {
            channelDefinitionBuilder.inboundEventKeyDetector = new JsonFieldBasedInboundEventKeyDetector(field);
            return channelDefinitionBuilder;
        }

        @Override
        public ChannelDefinitionBuilder mapFromJsonPathExpression(String jsonPathExpression) {
            channelDefinitionBuilder.inboundEventKeyDetector = new JsonPathBasedInboundEventKeyDetector(jsonPathExpression);
            return channelDefinitionBuilder;
        }

        @Override
        public ChannelDefinitionBuilder custom(InboundEventKeyDetector inboundEventKeyDetector) {
            channelDefinitionBuilder.inboundEventKeyDetector = inboundEventKeyDetector;
            return channelDefinitionBuilder;
        }

    }


}
