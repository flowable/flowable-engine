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

import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.event.EventDefinitionExpressionUtil;
import org.flowable.engine.impl.event.SignalEventHandler;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * Process-level signal start event behavior. Owns the deploy-time signal subscription registration
 * for this start event.
 */
public class SignalStartEventActivityBehavior extends FlowNodeActivityBehavior implements ProcessLevelStartEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected SignalEventDefinition signalEventDefinition;
    protected Signal signal;

    public SignalStartEventActivityBehavior(SignalEventDefinition signalEventDefinition, Signal signal) {
        this.signalEventDefinition = signalEventDefinition;
        this.signal = signal;
    }

    @Override
    public void deploy(ProcessLevelStartEventDeployContext context) {
        CommandContext commandContext = context.getCommandContext();
        EventSubscriptionService eventSubscriptionService = context.getEventSubscriptionService();
        ProcessDefinitionEntity processDefinition = context.getProcessDefinition();
        SignalEventSubscriptionEntity subscriptionEntity = eventSubscriptionService.createSignalEventSubscription();

        String signalName = EventDefinitionExpressionUtil.determineSignalName(commandContext, signalEventDefinition, context.getBpmnModel(), processDefinition);
        subscriptionEntity.setEventName(signalName);

        subscriptionEntity.setActivityId(context.getStartEvent().getId());
        subscriptionEntity.setProcessDefinitionId(processDefinition.getId());
        if (processDefinition.getTenantId() != null) {
            subscriptionEntity.setTenantId(processDefinition.getTenantId());
        }

        eventSubscriptionService.insertEventSubscription(subscriptionEntity);
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(subscriptionEntity);
    }

    @Override
    public void undeploy(ProcessLevelStartEventUndeployContext context) {
        context.registerObsoleteEventSubscriptionType(SignalEventHandler.EVENT_HANDLER_TYPE);
    }

    public SignalEventDefinition getSignalEventDefinition() {
        return signalEventDefinition;
    }

    public Signal getSignal() {
        return signal;
    }
}
