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
package org.flowable.common.engine.impl.eventregistry.pipeline;

import org.flowable.common.engine.api.eventregistry.InboundEventDeserializer;
import org.flowable.common.engine.api.eventregistry.InboundEventKeyDetector;
import org.flowable.common.engine.api.eventregistry.InboundEventPayloadExtractor;
import org.flowable.common.engine.api.eventregistry.InboundEventProcessingPipeline;
import org.flowable.common.engine.api.eventregistry.InboundEventTransformer;

/**
 * @author Joram Barrez
 */
public class DefaultEventProcessingPipeline implements InboundEventProcessingPipeline {

    protected InboundEventDeserializer inboundEventDeserializer;
    protected InboundEventKeyDetector inboundEventKeyDetector;
    protected InboundEventPayloadExtractor inboundEventPayloadExtractor;
    protected InboundEventTransformer inboundEventTransformer;

    public DefaultEventProcessingPipeline(InboundEventDeserializer inboundEventDeserializer,
            InboundEventKeyDetector inboundEventKeyDetector, InboundEventPayloadExtractor inboundEventPayloadExtractor,
            InboundEventTransformer inboundEventTransformer) {
        this.inboundEventDeserializer = inboundEventDeserializer;
        this.inboundEventKeyDetector = inboundEventKeyDetector;
        this.inboundEventPayloadExtractor = inboundEventPayloadExtractor;
        this.inboundEventTransformer = inboundEventTransformer;
    }

    @Override
    public InboundEventDeserializer getDeserializer() {
        return inboundEventDeserializer;
    }

    @Override
    public InboundEventKeyDetector getInboundKeyDetector() {
        return inboundEventKeyDetector;
    }

    @Override
    public InboundEventPayloadExtractor getPayloadExtractor() {
        return inboundEventPayloadExtractor;
    }

    @Override
    public InboundEventTransformer getTransformer() {
        return inboundEventTransformer;
    }

}
