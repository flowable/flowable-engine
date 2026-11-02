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
package org.flowable.cmmn.engine.impl.deployer;

import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.engine.impl.util.CmmnCorrelationUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * Built-in case-start lifecycle handler for the BPMN-spec-extension event-registry case start. Owns
 * the deploy-time event-subscription registration that historically lived inline in
 * {@link CmmnDeployer#updateEventSubscriptions} and
 * {@code CmmnDeploymentEntityManagerImpl.restoreEventRegistryStartEvent}.
 */
public class EventRegistryCaseDefinitionStartLifecycleHandler implements CaseDefinitionStartLifecycleHandler {

    protected final String eventType;
    protected final boolean manualCorrelation;

    public EventRegistryCaseDefinitionStartLifecycleHandler(String eventType, boolean manualCorrelation) {
        this.eventType = eventType;
        this.manualCorrelation = manualCorrelation;
    }

    @Override
    public void deploy(CaseDefinitionStartDeployContext context) {
        // Manual-correlation mode: subscriptions are added explicitly by the application at runtime,
        // not at deploy time.
        if (manualCorrelation) {
            return;
        }

        CaseDefinitionEntity caseDefinition = context.getCaseDefinition();
        Case caseModel = context.getCaseModel();
        EventSubscriptionService eventSubscriptionService = context.getEventSubscriptionService();

        EventSubscriptionBuilder builder = eventSubscriptionService.createEventSubscriptionBuilder()
                .eventType(eventType)
                .configuration(CmmnCorrelationUtil.getCorrelationKey(CmmnXmlConstants.ELEMENT_EVENT_CORRELATION_PARAMETER,
                        context.getCommandContext(), caseModel))
                .scopeDefinitionId(caseDefinition.getId())
                .scopeType(ScopeTypes.CMMN);

        if (caseDefinition.getTenantId() != null) {
            builder.tenantId(caseDefinition.getTenantId());
        }

        builder.create();
    }

    @Override
    public void undeploy(CaseDefinitionStartUndeployContext context) {
        if (manualCorrelation) {
            // dynamic mode: keep existing subscriptions but re-point them to the new case definition
            // instead of deleting.
            CaseDefinitionEntity previousCaseDefinition = context.getPreviousCaseDefinition();
            CaseDefinitionEntity newCaseDefinition = context.getNewCaseDefinition();
            context.getEventSubscriptionService().updateEventSubscriptionScopeDefinitionId(
                    previousCaseDefinition.getId(), newCaseDefinition.getId(),
                    eventType, newCaseDefinition.getKey(), null);
        } else {
            context.registerObsoleteEventSubscriptionType(eventType);
        }
    }

    public String getEventType() {
        return eventType;
    }

    public boolean isManualCorrelation() {
        return manualCorrelation;
    }
}
