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

package org.activiti.engine.impl.bpmn.behavior;

import java.util.List;

import org.activiti.engine.impl.bpmn.parser.EventSubscriptionDeclaration;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Daniel Meyer
 */
public class IntermediateThrowSignalEventActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = -2961893934810190972L;

    protected final boolean processInstanceScope;
    protected final EventSubscriptionDeclaration signalDefinition;

    public IntermediateThrowSignalEventActivityBehavior(ThrowEvent throwEvent, Signal signal, EventSubscriptionDeclaration signalDefinition) {
        this.processInstanceScope = Signal.SCOPE_PROCESS_INSTANCE.equals(signal.getScope());
        this.signalDefinition = signalDefinition;
    }

    @Override
    public void execute(DelegateExecution execution) {

        CommandContext commandContext = Context.getCommandContext();

        List<SignalEventSubscriptionEntity> subscriptionEntities = null;
        if (processInstanceScope) {
            subscriptionEntities = commandContext
                    .getEventSubscriptionEntityManager()
                    .findSignalEventSubscriptionsByProcessInstanceAndEventName(execution.getProcessInstanceId(), signalDefinition.getEventName());
        } else {
            subscriptionEntities = commandContext
                    .getEventSubscriptionEntityManager()
                    .findSignalEventSubscriptionsByEventName(signalDefinition.getEventName(), execution.getTenantId());
        }

        for (SignalEventSubscriptionEntity signalEventSubscriptionEntity : subscriptionEntities) {
            ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(signalEventSubscriptionEntity.getProcessDefinitionId());
            if (Flowable5Util.isVersion5Tag(processDefinition.getEngineVersion())) {
                signalEventSubscriptionEntity.eventReceived(null, signalDefinition.isAsync());
                
            } else {
                org.flowable.engine.ProcessEngineConfiguration flowable6ProcessEngineConfiguration = commandContext.getProcessEngineConfiguration()
                        .getFlowable5CompatibilityHandler()
                        .getFlowable6ProcessEngineConfiguration();
                
                org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl flowable6ProcessEngineConfigurationImpl = (org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl) flowable6ProcessEngineConfiguration;
                EventSubscriptionEntityManager eventSubScriptionEntityManager = flowable6ProcessEngineConfigurationImpl.getEventSubscriptionEntityManager();
                EventSubscriptionEntity flowable6EventSubscription = eventSubScriptionEntityManager.findById(signalEventSubscriptionEntity.getId());
                eventSubScriptionEntityManager.eventReceived(flowable6EventSubscription, null, signalDefinition.isAsync());
            }
        }

        ActivityExecution activityExecution = (ActivityExecution) execution;
        if (activityExecution.getActivity() != null) { // don't continue if process has already finished
            leave(activityExecution);
        }
    }

}
