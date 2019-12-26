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

public class ChannelModel {

    protected String key;
    protected String category;
    protected String name;
    protected String description;
    
    // inbound or outbound
    protected String channelType;
    
    // jms, rabbitmq, kafka etc
    protected String type;
    
    protected String destination;
    
    // inbound channel
    protected String selector;
    protected String deserializerType;
    protected ChannelEventKeyDetection channelEventKeyDetection;
    
    // outbound channel
    protected String serializerType;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getDeserializerType() {
        return deserializerType;
    }

    public void setDeserializerType(String deserializerType) {
        this.deserializerType = deserializerType;
    }

    public ChannelEventKeyDetection getChannelEventKeyDetection() {
        return channelEventKeyDetection;
    }

    public void setChannelEventKeyDetection(ChannelEventKeyDetection channelEventKeyDetection) {
        this.channelEventKeyDetection = channelEventKeyDetection;
    }

    public String getSerializerType() {
        return serializerType;
    }

    public void setSerializerType(String serializerType) {
        this.serializerType = serializerType;
    }
}
