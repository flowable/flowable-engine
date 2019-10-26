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

import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.definition.OutboundChannelDefinition;

/**
 * @author Joram Barrez
 */
public class OutboundChannelDefinitionImpl implements OutboundChannelDefinition {

    protected String key;
    protected OutboundEventChannelAdapter outboundEventChannelAdapter;
    protected OutboundEventProcessingPipeline outboundEventProcessingPipeline;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public OutboundEventChannelAdapter getOutboundEventChannelAdapter() {
        return outboundEventChannelAdapter;
    }

    public void setOutboundEventChannelAdapter(OutboundEventChannelAdapter outboundEventChannelAdapter) {
        this.outboundEventChannelAdapter = outboundEventChannelAdapter;
    }

    @Override
    public OutboundEventProcessingPipeline getOutboundEventProcessingPipeline() {
        return outboundEventProcessingPipeline;
    }

    public void setOutboundEventProcessingPipeline(OutboundEventProcessingPipeline outboundEventProcessingPipeline) {
        this.outboundEventProcessingPipeline = outboundEventProcessingPipeline;
    }

}
