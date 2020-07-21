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
package org.flowable.eventregistry.converter.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.eventregistry.json.converter.FlowableEventJsonException;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class InvalidChannelModelJsonConverterTest extends AbstractChannelConverterTest {

    @Test
    void unsupportedOutboundType() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/unsupportedOutboundTypeChannel.json");
        assertThatThrownBy(() -> channelConverter.convertToChannelModel(modelJson))
            .isInstanceOf(FlowableEventJsonException.class)
            .hasMessage("Not supported outbound channel model type was found custom");
    }

    @Test
    void unsupportedInboundType() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/unsupportedInboundTypeChannel.json");
        assertThatThrownBy(() -> channelConverter.convertToChannelModel(modelJson))
            .isInstanceOf(FlowableEventJsonException.class)
            .hasMessage("Not supported inbound channel model type was found custom");
    }

    @Test
    void jsonInboundChannelWithoutKeyDetection() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/jsonInboundChannelWithoutKeyDetection.json");
        assertThatThrownBy(() -> channelConverter.convertToChannelModel(modelJson))
            .isInstanceOf(FlowableEventJsonException.class)
            .hasMessage("A channel key detection value is required for the channel model with key jsonInboundWithoutKey");
    }

    @Test
    void jsonInboundChannelWithEmptyKeyDetection() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/jsonInboundChannelWithEmptyKeyDetection.json");
        assertThatThrownBy(() -> channelConverter.convertToChannelModel(modelJson))
            .isInstanceOf(FlowableEventJsonException.class)
            .hasMessage("The channel json key detection value was not found for the channel model with key jsonInboundWithEmptyKeyDetection."
                + " One of fixedValue, jsonField, jsonPointerExpression, delegateExpression should be set.");
    }

    @Test
    void xmlInboundChannelWithoutKeyDetection() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/xmlInboundChannelWithoutKeyDetection.json");
        assertThatThrownBy(() -> channelConverter.convertToChannelModel(modelJson))
            .isInstanceOf(FlowableEventJsonException.class)
            .hasMessage("A channel key detection value is required for the channel model with key xmlInboundWithoutKey");
    }

    @Test
    void xmlInboundChannelWithEmptyKeyDetection() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/xmlInboundChannelWithEmptyKeyDetection.json");
        assertThatThrownBy(() -> channelConverter.convertToChannelModel(modelJson))
            .isInstanceOf(FlowableEventJsonException.class)
            .hasMessage("The channel xml key detection value was not found for the channel model with key xmlInboundWithEmptyKeyDetection."
                + " One of fixedValue, xmlPathExpression, delegateExpression should be set.");
    }

    @Test
    void inboundChannelWithUnsupportedDeserializer() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/inboundChannelWithUnsupportedDeserializer.json");
        assertThatThrownBy(() -> channelConverter.convertToChannelModel(modelJson))
            .isInstanceOf(FlowableEventJsonException.class)
            .hasMessage("The deserializer type is not supported yml for the channel model with key ymlInbound");
    }

    @Test
    void inboundChannelWithNullDeserializer() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/inboundChannelWithNullDeserializer.json");
        ChannelModel model = channelConverter.convertToChannelModel(modelJson);
        assertThat(model).isInstanceOf(InboundChannelModel.class);
        InboundChannelModel inboundChannelModel = (InboundChannelModel) model;
        assertThat(inboundChannelModel.getDeserializerType()).isNull();
    }

    @Test
    void outboundChannelWithUnsupportedSerializer() {
        String modelJson = readJsonToString("org/flowable/eventregistry/converter/channel/outboundChannelWithUnsupportedSerializer.json");
        assertThatThrownBy(() -> channelConverter.convertToChannelModel(modelJson))
            .isInstanceOf(FlowableEventJsonException.class)
            .hasMessage("The serializer type is not supported yml for the channel model with key ymlOutbound");
    }

}
