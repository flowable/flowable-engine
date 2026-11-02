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

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CorrelationUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * Process-level event-registry start event behavior. Owns the deploy-time event-registry subscription
 * registration for this start event, including the dynamic-correlation re-point logic when the
 * process definition is superseded.
 */
public class EventRegistryStartEventActivityBehavior extends FlowNodeActivityBehavior implements ProcessLevelStartEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected String eventDefinitionKey;
    protected boolean manualCorrelation;

    public EventRegistryStartEventActivityBehavior(String eventDefinitionKey, boolean manualCorrelation) {
        this.eventDefinitionKey = eventDefinitionKey;
        this.manualCorrelation = manualCorrelation;
    }

    @Override
    public void deploy(ProcessLevelStartEventDeployContext context) {
        // dynamic, manual subscription mode: deploy does not create a subscription — those are added
        // explicitly by the application at runtime.
        if (manualCorrelation) {
            return;
        }

        ProcessDefinitionEntity processDefinition = context.getProcessDefinition();
        EventSubscriptionService eventSubscriptionService = context.getEventSubscriptionService();
        EventSubscriptionBuilder eventSubscriptionBuilder = eventSubscriptionService.createEventSubscriptionBuilder()
                .eventType(eventDefinitionKey)
                .activityId(context.getStartEvent().getId())
                .processDefinitionId(processDefinition.getId())
                .scopeType(ScopeTypes.BPMN)
                .configuration(CorrelationUtil.getCorrelationKey(BpmnXMLConstants.ELEMENT_EVENT_CORRELATION_PARAMETER, context.getCommandContext(), context.getStartEvent(), null));

        if (processDefinition.getTenantId() != null) {
            eventSubscriptionBuilder.tenantId(processDefinition.getTenantId());
        }

        EventSubscription eventSubscription = eventSubscriptionBuilder.create();
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
    }

    @Override
    public void undeploy(ProcessLevelStartEventUndeployContext context) {
        if (manualCorrelation) {
            // dynamic mode: keep existing subscriptions but re-point them to the new process definition instead of deleting.
            ProcessDefinitionEntity previousProcessDefinition = context.getPreviousProcessDefinition();
            ProcessDefinitionEntity newProcessDefinition = context.getNewProcessDefinition();
            context.getProcessEngineConfiguration().getEventSubscriptionServiceConfiguration().getEventSubscriptionService().updateEventSubscriptionProcessDefinitionId(
                    previousProcessDefinition.getId(), newProcessDefinition.getId(),
                    eventDefinitionKey, context.getStartEvent().getId(), newProcessDefinition.getKey(), null);
        } else {
            context.registerObsoleteEventSubscriptionType(eventDefinitionKey);
        }
    }

    public String getEventDefinitionKey() {
        return eventDefinitionKey;
    }

    public boolean isManualCorrelation() {
        return manualCorrelation;
    }
}
