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
package org.flowable.eventregistry.impl.persistence.deploy;

import java.io.Serializable;

import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.model.ChannelModel;

/**
 * @author Tijs Rademakers
 */
public class ChannelDefinitionCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    protected ChannelDefinitionEntity channelDefinitionEntity;
    protected ChannelModel channelModel;

    public ChannelDefinitionCacheEntry(ChannelDefinitionEntity channelDefinitionEntity, ChannelModel channelModel) {
        this.channelDefinitionEntity = channelDefinitionEntity;
        this.channelModel = channelModel;
    }

    public ChannelDefinitionEntity getChannelDefinitionEntity() {
        return channelDefinitionEntity;
    }

    public void setChannelDefinitionEntity(ChannelDefinitionEntity channelDefinitionEntity) {
        this.channelDefinitionEntity = channelDefinitionEntity;
    }

    public ChannelModel getChannelModel() {
        return channelModel;
    }

    public void setChannelModel(ChannelModel channelModel) {
        this.channelModel = channelModel;
    }
}
