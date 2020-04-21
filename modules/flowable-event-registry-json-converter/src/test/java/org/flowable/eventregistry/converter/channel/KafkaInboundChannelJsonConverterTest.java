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
import org.flowable.eventregistry.model.KafkaInboundChannelModel;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class KafkaInboundChannelJsonConverterTest extends AbstractChannelConverterTest {

    @Test
    void convertJsonToModel() {
        ChannelModel channelModel = readJson("org/flowable/eventregistry/converter/channel/simpleKafkaInboundChannel.json");

        validateModel(channelModel);
    }

    @Test
    void convertModelToJson() {
        ChannelModel channelModel = readJson("org/flowable/eventregistry/converter/channel/simpleKafkaInboundChannel.json");
        ChannelModel parsedChannel = exportAndReadChannel(channelModel);

        validateModel(parsedChannel);
    }

    protected void validateModel(ChannelModel channelModel) {
        assertThat(channelModel)
            .isInstanceOfSatisfying(KafkaInboundChannelModel.class, model -> {
                assertThat(model.getKey()).isEqualTo("kafkaChannel");
                assertThat(model.getCategory()).isEqualTo("test");
                assertThat(model.getName()).isEqualTo("Test channel");
                assertThat(model.getDescription()).isEqualTo("Test Kafka channel");

                assertThat(model.getChannelType()).isEqualTo("inbound");
                assertThat(model.getType()).isEqualTo("kafka");

                assertThat(model.getDeserializerType()).isEqualTo("json");

                ChannelEventKeyDetection eventKeyDetection = model.getChannelEventKeyDetection();
                assertThat(eventKeyDetection).isNotNull();
                assertThat(eventKeyDetection.getFixedValue()).isNull();
                assertThat(eventKeyDetection.getJsonField()).isEqualTo("eventKey");
                assertThat(eventKeyDetection.getJsonPointerExpression()).isNull();
                assertThat(eventKeyDetection.getXmlXPathExpression()).isNull();

                assertThat(model.getTopics()).containsExactlyInAnyOrder("customer", "test-customer");
                assertThat(model.getTopicPattern()).isEqualTo("*customer");
                assertThat(model.getClientIdPrefix()).isEqualTo("customer-");
                assertThat(model.getConcurrency()).isEqualTo("2");
                assertThat(model.getCustomProperties()).hasSize(1);
                assertThat(model.getCustomProperties().get(0).getName()).isEqualTo("connections.max.idle.ms");
                assertThat(model.getCustomProperties().get(0).getValue()).isEqualTo("10000");
            });
    }

    @Test
    void convertSimpleModelToJson() {
        ChannelModel channelModel = readJson("org/flowable/eventregistry/converter/channel/simpleKafkaInboundChannel2.json");
        assertThat(channelModel)
            .isInstanceOfSatisfying(KafkaInboundChannelModel.class, model -> {
                assertThat(model.getTopics()).containsExactly("customer");
            });

    }

}
