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
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EventSubscriptionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * An {@link FlowableEventListener} that throws a signal event when an event is dispatched to it.
 * 
 * @author Frederik Heremans
 * 
 */
public class SignalThrowingEventListener extends BaseDelegateEventListener {

    protected String signalName;
    protected boolean processInstanceScope = true;

    @Override
    public void onEvent(FlowableEvent event) {
        if (isValidEvent(event) && event instanceof FlowableEngineEvent engineEvent) {

            if (engineEvent.getProcessInstanceId() == null && processInstanceScope) {
                throw new FlowableIllegalArgumentException("Cannot throw process-instance scoped signal, since the dispatched event is not part of an ongoing process instance");
            }

            CommandContext commandContext = Context.getCommandContext();
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            List<SignalEventSubscriptionEntity> subscriptionEntities = null;
            if (processInstanceScope) {
                subscriptionEntities = eventSubscriptionService.findSignalEventSubscriptionsByProcessInstanceAndEventName(engineEvent.getProcessInstanceId(), signalName);
            } else {
                String tenantId = null;
                if (engineEvent.getProcessDefinitionId() != null) {
                    ProcessDefinition processDefinition = processEngineConfiguration.getDeploymentManager()
                            .findDeployedProcessDefinitionById(engineEvent.getProcessDefinitionId());
                    tenantId = processDefinition.getTenantId();
                }
                subscriptionEntities = eventSubscriptionService.findSignalEventSubscriptionsByEventName(signalName, tenantId);
            }

            for (SignalEventSubscriptionEntity signalEventSubscriptionEntity : subscriptionEntities) {
                EventSubscriptionUtil.eventReceived(signalEventSubscriptionEntity, null, false);
            }
        }
    }

    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }

    public void setProcessInstanceScope(boolean processInstanceScope) {
        this.processInstanceScope = processInstanceScope;
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }
}
