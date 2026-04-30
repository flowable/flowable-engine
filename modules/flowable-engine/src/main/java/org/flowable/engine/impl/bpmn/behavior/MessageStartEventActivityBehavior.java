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
package org.flowable.engine.impl.bpmn.behavior;

import java.util.List;

import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.event.EventDefinitionExpressionUtil;
import org.flowable.engine.impl.event.MessageEventHandler;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;

/**
 * Process-level message start event behavior. Owns the deploy-time message subscription registration
 * for this start event.
 */
public class MessageStartEventActivityBehavior extends FlowNodeActivityBehavior implements ProcessLevelStartEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected MessageEventDefinition messageEventDefinition;

    public MessageStartEventActivityBehavior(MessageEventDefinition messageEventDefinition) {
        this.messageEventDefinition = messageEventDefinition;
    }

    @Override
    public void deploy(ProcessLevelStartEventDeployContext context) {
        CommandContext commandContext = context.getCommandContext();
        EventSubscriptionService eventSubscriptionService = context.getEventSubscriptionService();
        ProcessDefinitionEntity processDefinition = context.getProcessDefinition();

        String messageName = EventDefinitionExpressionUtil.determineMessageName(commandContext, messageEventDefinition, processDefinition);

        // Skip the duplicate check when restoring a previous version's start events after the latest
        // deployment was deleted: the just-deleted process definition's subscription is still in the in-session entity
        // cache (the bulk delete in deleteEventSubscriptionsForProcessDefinition is queued, not flushed)
        // and would trip a false-positive conflict.
        if (!context.isRestoringPreviousVersion()) {
            List<EventSubscriptionEntity> subscriptionsForSameMessageName = eventSubscriptionService
                    .findEventSubscriptionsByName(MessageEventHandler.EVENT_HANDLER_TYPE, messageName, processDefinition.getTenantId());

            for (EventSubscriptionEntity eventSubscriptionEntity : subscriptionsForSameMessageName) {
                // throw exception only if there's already a subscription as start event
                if (eventSubscriptionEntity.getProcessInstanceId() == null || eventSubscriptionEntity.getProcessInstanceId().isEmpty()) {
                    // the event subscription has no instance-id, so it's a message start event
                    throw new FlowableException("Cannot deploy process definition '" + processDefinition.getResourceName()
                            + "': there already is a message event subscription for the message with name '" + messageName + "'. For " + eventSubscriptionEntity);
                }
            }
        }

        MessageEventSubscriptionEntity newSubscription = eventSubscriptionService.createMessageEventSubscription();
        newSubscription.setEventName(messageName);
        newSubscription.setActivityId(context.getStartEvent().getId());
        newSubscription.setConfiguration(processDefinition.getId());
        newSubscription.setProcessDefinitionId(processDefinition.getId());

        if (processDefinition.getTenantId() != null) {
            newSubscription.setTenantId(processDefinition.getTenantId());
        }

        eventSubscriptionService.insertEventSubscription(newSubscription);
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(newSubscription);
    }

    @Override
    public void undeploy(ProcessLevelStartEventUndeployContext context) {
        context.registerObsoleteEventSubscriptionType(MessageEventHandler.EVENT_HANDLER_TYPE);
    }

    public MessageEventDefinition getMessageEventDefinition() {
        return messageEventDefinition;
    }
}
