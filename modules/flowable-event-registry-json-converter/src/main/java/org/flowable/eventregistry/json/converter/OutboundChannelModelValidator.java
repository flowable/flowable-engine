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
package org.flowable.eventregistry.json.converter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;

/**
 * @author Filip Hrisafov
 */
public class OutboundChannelModelValidator implements ChannelValidator {

    protected Set<String> supportedSerializers;

    public OutboundChannelModelValidator() {
        supportedSerializers = new HashSet<>();
        supportedSerializers.add("json");
        supportedSerializers.add("xml");
    }

    public OutboundChannelModelValidator(Collection<String> supportedSerializers) {
        this.supportedSerializers = new HashSet<>(supportedSerializers);
    }

    @Override
    public void validateChannel(ChannelModel channelModel) {
        if (channelModel instanceof OutboundChannelModel) {
            OutboundChannelModel outboundChannelModel = (OutboundChannelModel) channelModel;

            validateChannel(outboundChannelModel);
        }
    }

    protected void validateChannel(OutboundChannelModel outboundChannelModel) {
        String serializerType = outboundChannelModel.getSerializerType();
        if (!supportedSerializers.contains(serializerType)) {
            throw new FlowableEventJsonException(
                "The serializer type is not supported " + serializerType + " for the channel model with key " + outboundChannelModel.getKey());
        }
    }
}
