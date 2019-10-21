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

import org.flowable.common.engine.api.eventregistry.InboundEventProcessingPipeline;
import org.flowable.common.engine.api.eventregistry.definition.ChannelDefinition;
import org.flowable.common.engine.api.eventregistry.InboundEventChannelAdapter;
import org.flowable.common.engine.api.eventregistry.InboundEventKeyDetector;
import org.flowable.common.engine.api.eventregistry.definition.InboundChannelDefinition;

/**
 * @author Joram Barrez
 */
public class InboundChannelDefinitionImpl implements InboundChannelDefinition {

    protected String key;
    protected InboundEventChannelAdapter inboundEventChannelAdapter;
    protected InboundEventProcessingPipeline inboundEventProcessingPipeline;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public InboundEventProcessingPipeline getInboundEventProcessingPipeline() {
        return inboundEventProcessingPipeline;
    }

    public void setInboundEventProcessingPipeline(InboundEventProcessingPipeline inboundEventProcessingPipeline) {
        this.inboundEventProcessingPipeline = inboundEventProcessingPipeline;
    }

    @Override
    public InboundEventChannelAdapter getInboundEventChannelAdapter() {
        return inboundEventChannelAdapter;
    }

    public void setInboundEventChannelAdapter(InboundEventChannelAdapter inboundEventChannelAdapter) {
        this.inboundEventChannelAdapter = inboundEventChannelAdapter;
    }

}
