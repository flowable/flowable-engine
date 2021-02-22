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
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.DelegateExpressionOutboundChannelModel;

/**
 * @author Filip Hrisafov
 */
public class DelegateExpressionOutboundChannelModelProcessor implements ChannelModelProcessor {

    protected HasExpressionManagerEngineConfiguration engineConfiguration;

    public DelegateExpressionOutboundChannelModelProcessor(HasExpressionManagerEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
    }

    @Override
    public boolean canProcess(ChannelModel channelModel) {
        return channelModel instanceof DelegateExpressionOutboundChannelModel;
    }

    @Override
    public void registerChannelModel(ChannelModel channelModel, String tenantId, EventRegistry eventRegistry, EventRepositoryService eventRepositoryService,
        boolean fallbackToDefaultTenant) {
        if (channelModel instanceof DelegateExpressionOutboundChannelModel) {
            registerChannelModel((DelegateExpressionOutboundChannelModel) channelModel);
        }
    }

    protected void registerChannelModel(DelegateExpressionOutboundChannelModel channelModel) {
        String delegateExpression = channelModel.getAdapterDelegateExpression();
        if (StringUtils.isNotEmpty(delegateExpression)) {
            Object channelAdapter = engineConfiguration.getExpressionManager()
                .createExpression(delegateExpression)
                .getValue(new VariableContainerWrapper(Collections.emptyMap()));
            if (!(channelAdapter instanceof OutboundEventChannelAdapter)) {
                throw new FlowableException(
                    "DelegateExpression outbound channel model with key " + channelModel.getKey() + " resolved channel adapter delegate expression to "
                        + channelAdapter + " which is not of type " + OutboundEventChannelAdapter.class);
            }
            channelModel.setOutboundEventChannelAdapter(channelAdapter);
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
