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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
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
        String encoding = configuration.getXmlEncoding();
        Charset encodingCharset = encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();
        for (ChannelDefinition channelDefinition : channelDefinitions) {
            // We are explicitly not using EventRepositoryService#getChannelModelById.
            // When the repository service is used, then it will trigger a deployment of the channel.
            // However, a channel should not be deployed that early during the update
            ChannelModel model;
            try (InputStream stream = repositoryService.getResourceAsStream(channelDefinition.getDeploymentId(), channelDefinition.getResourceName())) {
                model = configuration.getChannelJsonConverter().convertToChannelModel(IOUtils.toString(stream, encodingCharset));
            } catch (IOException e) {
                throw new FlowableException("Failed to close resource", e);
            }
            entityManager.updateChannelDefinitionTypeAndImplementation(channelDefinition.getId(), model.getChannelType(), model.getType());
        }

        return null;
    }
}
