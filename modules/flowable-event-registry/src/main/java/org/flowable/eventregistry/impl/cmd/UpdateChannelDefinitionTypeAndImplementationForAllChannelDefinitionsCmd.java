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

import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntityManager;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventregistry.model.ChannelModel;

/**
 * @author Filip Hrisafov
 */
public class UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd implements Command<Void> {

    @Override
    public Void execute(CommandContext commandContext) {
        EventRegistryEngineConfiguration configuration = CommandContextUtil.getEventRegistryConfiguration(commandContext);
        EventRepositoryService repositoryService = configuration.getEventRepositoryService();
        List<ChannelDefinition> channelDefinitions = repositoryService.createChannelDefinitionQuery().list();
        ChannelDefinitionEntityManager entityManager = configuration.getChannelDefinitionEntityManager();
        for (ChannelDefinition channelDefinition : channelDefinitions) {
            ChannelModel model = repositoryService.getChannelModelById(channelDefinition.getId());
            entityManager.updateChannelDefinitionTypeAndImplementation(channelDefinition.getId(), model.getChannelType(), model.getType());
        }

        return null;
    }
}
