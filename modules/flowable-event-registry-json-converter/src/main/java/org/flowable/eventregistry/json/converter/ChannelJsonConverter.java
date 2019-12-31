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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.flowable.eventregistry.model.JmsOutboundChannelModel;
import org.flowable.eventregistry.model.KafkaInboundChannelModel;
import org.flowable.eventregistry.model.KafkaOutboundChannelModel;
import org.flowable.eventregistry.model.RabbitInboundChannelModel;
import org.flowable.eventregistry.model.RabbitOutboundChannelModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class ChannelJsonConverter {

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected List<ChannelValidator> validators = new ArrayList<>();

    public ChannelJsonConverter() {
        addValidator(new OutboundChannelModelValidator());
        addValidator(new InboundChannelModelValidator());
    }

    public ChannelJsonConverter(Collection<ChannelValidator> validators) {
        this.validators = new ArrayList<>(validators);
    }

    public ChannelModel convertToChannelModel(String modelJson) {
        try {
            JsonNode channelNode = objectMapper.readTree(modelJson);
            Class<? extends ChannelModel> channelClass = determineChannelModelClass(channelNode);

            ChannelModel channelModel = objectMapper.convertValue(channelNode, channelClass);

            validateChannel(channelModel);

            return channelModel;
        } catch (FlowableEventJsonException e) {
            throw e;
        } catch (Exception e) {
            throw new FlowableEventJsonException("Error reading channel json", e);
        }
    }

    protected Class<? extends ChannelModel> determineChannelModelClass(JsonNode channelNode) {
        String channelType = channelNode.path("channelType").asText(null);
        String type = channelNode.path("type").asText(null);

        Class<? extends ChannelModel> channelClass;
        if ("outbound".equals(channelType)) {
            if ("jms".equals(type)) {
                channelClass = JmsOutboundChannelModel.class;
            } else if ("rabbit".equals(type)) {
                channelClass = RabbitOutboundChannelModel.class;
            } else if ("kafka".equals(type)) {
                channelClass = KafkaOutboundChannelModel.class;
            } else {
                throw new FlowableEventJsonException("Not supported outbound channel model type was found " + type);
            }
        } else {
            if ("jms".equals(type)) {
                channelClass = JmsInboundChannelModel.class;
            } else if ("rabbit".equals(type)) {
                channelClass = RabbitInboundChannelModel.class;
            } else if ("kafka".equals(type)) {
                channelClass = KafkaInboundChannelModel.class;
            } else {
                throw new FlowableEventJsonException("Not supported inbound channel model type was found " + type);
            }
        }
        return channelClass;
    }

    protected void validateChannel(ChannelModel channelModel) {
        for (ChannelValidator validator : validators) {
            validator.validateChannel(channelModel);
        }
    }

    public String convertToJson(ChannelModel definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (Exception e) {
            throw new FlowableEventJsonException("Error writing channel json", e);
        }
    }

    public List<ChannelValidator> getValidators() {
        return validators;
    }

    public void addValidator(ChannelValidator validator) {
        validators.add(validator);
    }

    public void setValidators(List<ChannelValidator> validators) {
        this.validators = validators;
    }
}