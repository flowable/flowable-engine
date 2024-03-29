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

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class SignalEventHandler extends AbstractEventHandler {

    public static final String EVENT_HANDLER_TYPE = "signal";

    @Override
    public String getEventHandlerType() {
        return EVENT_HANDLER_TYPE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleEvent(EventSubscriptionEntity eventSubscription, Object payload, CommandContext commandContext) {
        if (eventSubscription.getExecutionId() != null) {
            super.handleEvent(eventSubscription, payload, commandContext);

        } else if (eventSubscription.getProcessDefinitionId() != null) {

            // Find initial flow element matching the signal start event
            String processDefinitionId = eventSubscription.getProcessDefinitionId();
            org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
            ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);

            if (processDefinition.isSuspended()) {
                throw new FlowableException("Could not handle signal: process definition with id: " + processDefinitionId + " is suspended for " + eventSubscription);
            }

            // Start process instance via the flow element linked to the event
            FlowElement flowElement = process.getFlowElement(eventSubscription.getActivityId(), true);
            if (flowElement == null) {
                throw new FlowableException("Could not find matching FlowElement for " + eventSubscription);
            }

            ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
            processInstanceHelper.createAndStartProcessInstanceWithInitialFlowElement(processDefinition, null, null, null, flowElement, process,
                    getPayloadAsMap(payload), null, null, null, true);

        } else if (eventSubscription.getScopeId() != null && ScopeTypes.CMMN.equals(eventSubscription.getScopeType())) {
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getCaseInstanceService().handleSignalEvent(eventSubscription, getPayloadAsMap(payload));
        
        } else {
            throw new FlowableException("Invalid signal handling: no execution nor process definition set for " + eventSubscription);
        }
    }

    protected Map<String, Object> getPayloadAsMap(Object payload) {
        Map<String, Object> variables = null;
        if (payload instanceof Map) {
            variables = (Map<String, Object>) payload;
        }
        return variables;
    }

}
