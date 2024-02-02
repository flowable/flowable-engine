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

package org.flowable.eventregistry.rest.service.api.runtime;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Data for creating an event instance. "
    + "Only one of eventDefinitionId or eventDefinitionKey can be used in the request body. Same applies to channelDefinitionId/Key")
public class EventInstanceCreateRequest {

    protected String eventDefinitionId;
    protected String eventDefinitionKey;

    protected String channelDefinitionId;
    protected String channelDefinitionKey;

    protected ObjectNode eventPayload;
    protected String tenantId;

    @ApiModelProperty(example = "oneEvent:1:158")
    public String getEventDefinitionId() {
        return eventDefinitionId;
    }

    public void setEventDefinitionId(String eventDefinitionId) {
        this.eventDefinitionId = eventDefinitionId;
    }

    @ApiModelProperty(example = "oneEvent")
    public String getEventDefinitionKey() {
        return eventDefinitionKey;
    }

    public void setEventDefinitionKey(String eventDefinitionKey) {
        this.eventDefinitionKey = eventDefinitionKey;
    }

    @ApiModelProperty(example = "myChannel:1:123")
    public String getChannelDefinitionId() {
        return channelDefinitionId;
    }

    public void setChannelDefinitionId(String channelDefinitionId) {
        this.channelDefinitionId = channelDefinitionId;
    }

    @ApiModelProperty(example = "myChannel")
    public String getChannelDefinitionKey() {
        return channelDefinitionKey;
    }

    public void setChannelDefinitionKey(String channelDefinitionKey) {
        this.channelDefinitionKey = channelDefinitionKey;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "tenant1")
    public String getTenantId() {
        return tenantId;
    }

    public ObjectNode getEventPayload() {
        return eventPayload;
    }

    public void setEventPayload(ObjectNode eventPayload) {
        this.eventPayload = eventPayload;
    }

    @JsonIgnore
    public boolean isTenantSet() {
        return tenantId != null && !StringUtils.isEmpty(tenantId);
    }
}
