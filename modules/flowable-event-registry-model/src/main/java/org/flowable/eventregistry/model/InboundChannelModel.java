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
package org.flowable.eventregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Joram Barrez
 */
public class InboundChannelModel extends ChannelModel {

    protected String deserializerType;

    protected String deserializerDelegateExpression;
    protected String payloadExtractorDelegateExpression;
    protected String eventTransformerDelegateExpression;
    protected String pipelineDelegateExpression;
    protected ChannelEventKeyDetection channelEventKeyDetection;
    protected ChannelEventTenantIdDetection channelEventTenantIdDetection;

    @JsonIgnore
    protected Object inboundEventChannelAdapter;

    @JsonIgnore
    protected Object inboundEventProcessingPipeline;

    public InboundChannelModel() {
        setChannelType("inbound");
    }

    public String getDeserializerType() {
        return deserializerType;
    }

    public void setDeserializerType(String deserializerType) {
        this.deserializerType = deserializerType;
    }

    public String getDeserializerDelegateExpression() {
        return deserializerDelegateExpression;
    }

    public void setDeserializerDelegateExpression(String deserializerDelegateExpression) {
        this.deserializerDelegateExpression = deserializerDelegateExpression;
    }

    public String getPayloadExtractorDelegateExpression() {
        return payloadExtractorDelegateExpression;
    }

    public void setPayloadExtractorDelegateExpression(String payloadExtractorDelegateExpression) {
        this.payloadExtractorDelegateExpression = payloadExtractorDelegateExpression;
    }

    public String getEventTransformerDelegateExpression() {
        return eventTransformerDelegateExpression;
    }

    public void setEventTransformerDelegateExpression(String eventTransformerDelegateExpression) {
        this.eventTransformerDelegateExpression = eventTransformerDelegateExpression;
    }

    public String getPipelineDelegateExpression() {
        return pipelineDelegateExpression;
    }

    public void setPipelineDelegateExpression(String pipelineDelegateExpression) {
        this.pipelineDelegateExpression = pipelineDelegateExpression;
    }

    public ChannelEventKeyDetection getChannelEventKeyDetection() {
        return channelEventKeyDetection;
    }

    public void setChannelEventKeyDetection(ChannelEventKeyDetection channelEventKeyDetection) {
        this.channelEventKeyDetection = channelEventKeyDetection;
    }

    public ChannelEventTenantIdDetection getChannelEventTenantIdDetection() {
        return channelEventTenantIdDetection;
    }

    public void setChannelEventTenantIdDetection(ChannelEventTenantIdDetection channelEventTenantIdDetection) {
        this.channelEventTenantIdDetection = channelEventTenantIdDetection;
    }

    public Object getInboundEventProcessingPipeline() {
        return inboundEventProcessingPipeline;
    }

    public void setInboundEventProcessingPipeline(Object inboundEventProcessingPipeline) {
        this.inboundEventProcessingPipeline = inboundEventProcessingPipeline;
    }

    public Object getInboundEventChannelAdapter() {
        return inboundEventChannelAdapter;
    }

    public void setInboundEventChannelAdapter(Object inboundEventChannelAdapter) {
        this.inboundEventChannelAdapter = inboundEventChannelAdapter;
    }

}
