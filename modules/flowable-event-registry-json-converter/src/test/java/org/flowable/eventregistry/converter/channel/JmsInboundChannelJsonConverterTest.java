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
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class JmsInboundChannelJsonConverterTest extends AbstractChannelConverterTest {

    @Test
    void convertJsonToModel() {
        ChannelModel channelModel = readJson("org/flowable/eventregistry/converter/channel/simpleJmsInboundChannel.json");

        validateModel(channelModel);
    }

    @Test
    void convertModelToJson() {
        ChannelModel channelModel = readJson("org/flowable/eventregistry/converter/channel/simpleJmsInboundChannel.json");
        ChannelModel parsedChannel = exportAndReadChannel(channelModel);

        validateModel(parsedChannel);
    }

    protected void validateModel(ChannelModel channelModel) {
        assertThat(channelModel)
            .isInstanceOfSatisfying(JmsInboundChannelModel.class, model -> {
                assertThat(model.getKey()).isEqualTo("jmsChannel");
                assertThat(model.getCategory()).isEqualTo("test");
                assertThat(model.getName()).isEqualTo("Test channel");
                assertThat(model.getDescription()).isEqualTo("Test JMS channel");

                assertThat(model.getChannelType()).isEqualTo("inbound");
                assertThat(model.getType()).isEqualTo("jms");

                assertThat(model.getDestination()).isEqualTo("customer");
                assertThat(model.getDeserializerType()).isEqualTo("json");

                ChannelEventKeyDetection eventKeyDetection = model.getChannelEventKeyDetection();
                assertThat(eventKeyDetection).isNotNull();
                assertThat(eventKeyDetection.getFixedValue()).isNull();
                assertThat(eventKeyDetection.getJsonField()).isEqualTo("eventKey");
                assertThat(eventKeyDetection.getJsonPointerExpression()).isNull();
                assertThat(eventKeyDetection.getXmlXPathExpression()).isNull();

                assertThat(model.getSelector()).isEqualTo("name = 'kermit'");
                assertThat(model.getSubscription()).isEqualTo("testSubscription");
                assertThat(model.getConcurrency()).isEqualTo("3");
            });
    }

}
