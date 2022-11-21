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
package org.flowable.eventregistry.impl.pipeline;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.HasExpressionManagerEngineConfiguration;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.DelegateExpressionInboundChannelModel;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
public class DelegateExpressionInboundChannelModelProcessor implements ChannelModelProcessor {

    protected HasExpressionManagerEngineConfiguration engineConfiguration;
    protected ObjectMapper objectMapper;

    public DelegateExpressionInboundChannelModelProcessor(HasExpressionManagerEngineConfiguration engineConfiguration, ObjectMapper objectMapper) {
        this.engineConfiguration = engineConfiguration;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canProcess(ChannelModel channelModel) {
        return channelModel instanceof DelegateExpressionInboundChannelModel;
    }

    @Override
    public boolean canProcessIfChannelModelAlreadyRegistered(ChannelModel channelModel) {
        return channelModel instanceof DelegateExpressionInboundChannelModel;
    }

    @Override
    public void registerChannelModel(ChannelModel channelModel, String tenantId, EventRegistry eventRegistry, 
            EventRepositoryService eventRepositoryService,
            boolean fallbackToDefaultTenant) {
        
        if (channelModel instanceof DelegateExpressionInboundChannelModel) {
            registerChannelModel((DelegateExpressionInboundChannelModel) channelModel, tenantId, eventRegistry);
        }
    }

    protected void registerChannelModel(DelegateExpressionInboundChannelModel channelModel, String tenantId, EventRegistry eventRegistry) {
        String delegateExpression = channelModel.getAdapterDelegateExpression();
        if (StringUtils.isNotEmpty(delegateExpression)) {
            VariableContainerWrapper variableContainer = new VariableContainerWrapper(Collections.emptyMap());
            variableContainer.setVariable("tenantId", tenantId);
            variableContainer.setTenantId(tenantId);
            Object channelAdapter = engineConfiguration.getExpressionManager()
                .createExpression(delegateExpression)
                .getValue(variableContainer);
            if (!(channelAdapter instanceof InboundEventChannelAdapter)) {
                throw new FlowableException(
                    "DelegateExpression inbound channel model with key " + channelModel.getKey() + " resolved channel adapter delegate expression to "
                        + channelAdapter + " which is not of type " + InboundEventChannelAdapter.class);
            }
            channelModel.setInboundEventChannelAdapter(channelAdapter);
            ((InboundEventChannelAdapter) channelAdapter).setEventRegistry(eventRegistry);
            ((InboundEventChannelAdapter) channelAdapter).setInboundChannelModel(channelModel);
        }
    }

    @Override
    public void unregisterChannelModel(ChannelModel channelModel, String tenantId, EventRepositoryService eventRepositoryService) {
        // Nothing to do
    }

    public HasExpressionManagerEngineConfiguration getEngineConfiguration() {
        return engineConfiguration;
    }

    public void setEngineConfiguration(HasExpressionManagerEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
    }
}
