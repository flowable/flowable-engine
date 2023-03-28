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
package org.flowable.eventregistry.spring.kafka;

import org.flowable.eventregistry.api.OutboundEvent;
import org.flowable.eventregistry.impl.runtime.ReadOnlyEventInstanceVariableContainer;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventregistry.model.KafkaOutboundChannelModel;

/**
 * @author Roman Saratz
 */
public class ExpressionKafkaMessageKeyProvider implements KafkaMessageKeyProvider {

    protected final KafkaOutboundChannelModel channelModel;

    public ExpressionKafkaMessageKeyProvider(KafkaOutboundChannelModel channelModel) {
        this.channelModel = channelModel;
    }

    @Override
    public String determineMessageKey(OutboundEvent<?> eventInstance) {
        return CommandContextUtil.getEventRegistryConfiguration().getExpressionManager().createExpression(channelModel.getRecordKey())
                .getValue(new ReadOnlyEventInstanceVariableContainer(eventInstance.getEventInstance())).toString();
    }
}
