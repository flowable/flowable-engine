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

package org.flowable.engine.impl.event;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Daniel Meyer
 */
public class MessageEventHandler extends AbstractEventHandler {

    public static final String EVENT_HANDLER_TYPE = "message";

    @Override
    public String getEventHandlerType() {
        return EVENT_HANDLER_TYPE;
    }

    @Override
    public void handleEvent(EventSubscriptionEntity eventSubscription, Object payload, CommandContext commandContext) {
        // As stated in the FlowableEventType java-doc, the message-event is
        // thrown before the actual message has been sent
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        if (processEngineConfiguration.getEventDispatcher().isEnabled()) {
            processEngineConfiguration
                    .getEventDispatcher()
                    .dispatchEvent(
                            FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED, eventSubscription.getActivityId(), eventSubscription.getEventName(), payload,
                                    eventSubscription.getExecutionId(), eventSubscription.getProcessInstanceId(), eventSubscription.getExecution().getProcessDefinitionId()));
        }

        super.handleEvent(eventSubscription, payload, commandContext);
    }

}
