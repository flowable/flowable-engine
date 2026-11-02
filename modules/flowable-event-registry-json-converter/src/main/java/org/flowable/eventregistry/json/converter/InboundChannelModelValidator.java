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

import org.apache.commons.lang3.StringUtils;
import org.flowable.eventregistry.model.ChannelEventKeyDetection;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;

/**
 * @author Filip Hrisafov
 */
public class InboundChannelModelValidator implements ChannelValidator {

    @Override
    public void validateChannel(ChannelModel channelModel) {
        if (channelModel instanceof InboundChannelModel inboundChannelModel) {

            validateChannel(inboundChannelModel);
        }
    }

    protected void validateChannel(InboundChannelModel inboundChannelModel) {
        if (StringUtils.isEmpty(inboundChannelModel.getPipelineDelegateExpression())) {
            // Deserializer is only needed if there is no pipeline delegate expression
            validateDeserializer(inboundChannelModel);
        }
    }

    protected void validateDeserializer(InboundChannelModel inboundChannelModel) {
        String deserializerType = inboundChannelModel.getDeserializerType();
        ChannelEventKeyDetection channelEventKeyDetection = inboundChannelModel.getChannelEventKeyDetection();
        if ("json".equalsIgnoreCase(deserializerType)) {
            if (channelEventKeyDetection == null) {
                throw new FlowableEventJsonException("A channel key detection value is required for the channel model with key " + inboundChannelModel.getKey());
            }

            if (StringUtils.isEmpty(channelEventKeyDetection.getFixedValue()) &&
                StringUtils.isEmpty(channelEventKeyDetection.getJsonField()) &&
                StringUtils.isEmpty(channelEventKeyDetection.getJsonPointerExpression()) &&
                StringUtils.isEmpty(channelEventKeyDetection.getDelegateExpression())) {
                throw new FlowableEventJsonException(
                    "The channel json key detection value was not found for the channel model with key " + inboundChannelModel.getKey()
                        + ". One of fixedValue, jsonField, jsonPointerExpression, delegateExpression should be set.");
            }

        } else if ("xml".equalsIgnoreCase(deserializerType)) {
            if (channelEventKeyDetection == null) {
                throw new FlowableEventJsonException("A channel key detection value is required for the channel model with key " + inboundChannelModel.getKey());
            }

            if (StringUtils.isEmpty(channelEventKeyDetection.getFixedValue()) &&
                StringUtils.isEmpty(channelEventKeyDetection.getXmlXPathExpression()) &&
                StringUtils.isEmpty(channelEventKeyDetection.getDelegateExpression())) {
                throw new FlowableEventJsonException(
                    "The channel xml key detection value was not found for the channel model with key " + inboundChannelModel.getKey()
                        + ". One of fixedValue, xmlPathExpression, delegateExpression should be set.");
            }

        } else if ("expression".equalsIgnoreCase(deserializerType)) {
            if (StringUtils.isEmpty(inboundChannelModel.getDeserializerDelegateExpression())) {
                throw new FlowableEventJsonException(
                    "The channel deserializer delegate expression was not set for the channel model with key " + inboundChannelModel);
            }
        } else if (deserializerType != null) {
            throw new FlowableEventJsonException(
                "The deserializer type is not supported " + deserializerType + " for the channel model with key " + inboundChannelModel.getKey());
        }
    }
}
