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

import org.flowable.eventregistry.model.ChannelEventKeyDetection;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.DelegateExpressionInboundChannelModel;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class DelegateExpressionInboundChannelJsonConverterTest extends AbstractChannelConverterTest {

    @Test
    void convertJsonToModel() {
        ChannelModel channelModel = readJson("org/flowable/eventregistry/converter/channel/simpleDelegateExpressionInboundChannel.json");

        validateModel(channelModel);
    }

    @Test
    void convertModelToJson() {
        ChannelModel channelModel = readJson("org/flowable/eventregistry/converter/channel/simpleDelegateExpressionInboundChannel.json");
        ChannelModel parsedChannel = exportAndReadChannel(channelModel);

        validateModel(parsedChannel);
    }

    protected void validateModel(ChannelModel channelModel) {
        assertThat(channelModel)
            .isInstanceOfSatisfying(DelegateExpressionInboundChannelModel.class, model -> {
                assertThat(model.getKey()).isEqualTo("expressionChannel");
                assertThat(model.getCategory()).isEqualTo("test");
                assertThat(model.getName()).isEqualTo("Test channel");
                assertThat(model.getDescription()).isEqualTo("Test Expression channel");

                assertThat(model.getChannelType()).isEqualTo("inbound");
                assertThat(model.getType()).isEqualTo("expression");

                assertThat(model.getAdapterDelegateExpression()).isEqualTo("${channelAdapter}");
                assertThat(model.getDeserializerType()).isEqualTo("expression");

                assertThat(model.getDeserializerDelegateExpression()).isEqualTo("${deserializer}");
                assertThat(model.getPayloadExtractorDelegateExpression()).isEqualTo("${payloadExtractor}");
                assertThat(model.getEventTransformerDelegateExpression()).isEqualTo("${eventTransformer}");

                ChannelEventKeyDetection eventKeyDetection = model.getChannelEventKeyDetection();
                assertThat(eventKeyDetection).isNotNull();
                assertThat(eventKeyDetection.getFixedValue()).isNull();
                assertThat(eventKeyDetection.getJsonField()).isNull();
                assertThat(eventKeyDetection.getJsonPointerExpression()).isNull();
                assertThat(eventKeyDetection.getXmlXPathExpression()).isNull();
                assertThat(eventKeyDetection.getDelegateExpression()).isEqualTo("${keyDetector}");
            });
    }

}
