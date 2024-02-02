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
package org.flowable.eventregistry.impl.cmd;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.eventregistry.impl.persistence.deploy.ChannelDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SetChannelDefinitionCategoryCmd implements Command<Void> {

    protected String channelDefinitionId;
    protected String category;

    public SetChannelDefinitionCategoryCmd(String channelDefinitionId, String category) {
        this.channelDefinitionId = channelDefinitionId;
        this.category = category;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        if (channelDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Channel definition id is null");
        }

        ChannelDefinitionEntity channelDefinition = CommandContextUtil.getChannelDefinitionEntityManager(commandContext).findById(channelDefinitionId);

        if (channelDefinition == null) {
            throw new FlowableObjectNotFoundException("No channel definition found for id = '" + channelDefinitionId + "'");
        }

        // Update category
        channelDefinition.setCategory(category);

        // Remove channel from cache, it will be refetch later
        DeploymentCache<ChannelDefinitionCacheEntry> channelDefinitionCache = CommandContextUtil.getEventRegistryConfiguration().getChannelDefinitionCache();
        if (channelDefinitionCache != null) {
            channelDefinitionCache.remove(channelDefinitionId);
        }

        CommandContextUtil.getChannelDefinitionEntityManager(commandContext).update(channelDefinition);

        return null;
    }

}
