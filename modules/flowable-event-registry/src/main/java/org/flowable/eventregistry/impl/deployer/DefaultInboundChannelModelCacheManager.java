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
package org.flowable.eventregistry.impl.deployer;

import java.util.HashSet;
import java.util.Set;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.InboundChannelModelCacheManager;
import org.flowable.eventregistry.model.InboundChannelModel;

public class DefaultInboundChannelModelCacheManager implements InboundChannelModelCacheManager {

    protected Set<String> channelHashKeySet = new HashSet<>();
    
    @Override
    public boolean isChannelModelAlreadyRegistered(InboundChannelModel channelModel, ChannelDefinition channelDefinition) {
        return channelHashKeySet.contains(channelModel.getChannelModelHashKey() + channelDefinition.getTenantId());
    }

    @Override
    public void registerChannelModel(InboundChannelModel channelModel, ChannelDefinition channelDefinition) {
        channelHashKeySet.add(channelModel.getChannelModelHashKey() + channelDefinition.getTenantId());
    }

    @Override
    public void unregisterChannelModel(InboundChannelModel channelModel, ChannelDefinition channelDefinition) {
        channelHashKeySet.remove(channelModel.getChannelModelHashKey() + channelDefinition.getTenantId());
    }

    @Override
    public void cleanChannelModels() {
        channelHashKeySet.clear();
    }
}
