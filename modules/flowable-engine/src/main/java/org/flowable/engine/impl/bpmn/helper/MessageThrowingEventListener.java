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
package org.flowable.engine.impl.bpmn.helper;

import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EventSubscriptionUtil;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;

/**
 * An {@link FlowableEventListener} that throws a message event when an event is dispatched to it. Sends the message to the execution the event was fired from. If the execution is not subscribed to a
 * message, the process-instance is checked.
 * 
 * @author Tijs Rademakers
 * 
 */
public class MessageThrowingEventListener extends BaseDelegateEventListener {

    protected String messageName;
    protected Class<?> entityClass;

    @Override
    public void onEvent(FlowableEvent event) {
        if (isValidEvent(event) && event instanceof FlowableEngineEvent engineEvent) {

            if (engineEvent.getProcessInstanceId() == null) {
                throw new FlowableIllegalArgumentException("Cannot throw process-instance scoped message, since the dispatched event is not part of an ongoing process instance");
            }

            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            List<MessageEventSubscriptionEntity> subscriptionEntities = processEngineConfiguration.getEventSubscriptionServiceConfiguration()
                    .getEventSubscriptionService().findMessageEventSubscriptionsByProcessInstanceAndEventName(engineEvent.getProcessInstanceId(), messageName);

            for (EventSubscriptionEntity messageEventSubscriptionEntity : subscriptionEntities) {
                EventSubscriptionUtil.eventReceived(messageEventSubscriptionEntity, null, false);
            }
        }
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }
}
