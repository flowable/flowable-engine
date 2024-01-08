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
package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;
import java.util.Objects;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceStartEventSubscriptionBuilderImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * This command creates and registers a new case start event subscription based on the provided builder information.
 *
 * @author Micha Kiener
 */
public class RegisterCaseInstanceStartEventSubscriptionCmd extends AbstractCaseStartEventSubscriptionCmd implements Command<EventSubscription>,
    Serializable {

    private static final long serialVersionUID = 1L;

    protected final CaseInstanceStartEventSubscriptionBuilderImpl builder;

    public RegisterCaseInstanceStartEventSubscriptionCmd(CaseInstanceStartEventSubscriptionBuilderImpl builder) {
        this.builder = builder;
    }

    @Override
    public EventSubscription execute(CommandContext commandContext) {
        CaseDefinition caseDefinition = getLatestCaseDefinitionByKey(builder.getCaseDefinitionKey(), builder.getTenantId(), commandContext);
        Case caze = getCase(caseDefinition.getId(), commandContext);

        EventSubscription eventSubscription = null;
        String eventDefinitionKey = caze.getStartEventType();
        String startCorrelationConfiguration = getStartCorrelationConfiguration(caseDefinition.getId(), commandContext);

        if (eventDefinitionKey != null && Objects.equals(startCorrelationConfiguration, CmmnXmlConstants.START_EVENT_CORRELATION_MANUAL)) {
            String correlationKey = generateCorrelationConfiguration(eventDefinitionKey, builder.getTenantId(), builder.getCorrelationParameterValues(), commandContext);

            eventSubscription = insertEventRegistryEvent(eventDefinitionKey, builder.isDoNotUpdateToLatestVersionAutomatically(), caseDefinition,
                correlationKey, commandContext);
        }

        if (eventSubscription == null) {
            throw new FlowableIllegalArgumentException("The case definition with id '" + caseDefinition.getId()
                + "' does not have an event-registry based start event with a manual subscription behavior.");
        }

        return eventSubscription;
    }

    protected EventSubscription insertEventRegistryEvent(String eventDefinitionKey, boolean doNotUpdateToLatestVersionAutomatically,
        CaseDefinition caseDefinition, String correlationKey, CommandContext commandContext) {
        
        CmmnEngineConfiguration caseEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        EventSubscriptionService eventSubscriptionService = caseEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        EventSubscriptionBuilder eventSubscriptionBuilder = eventSubscriptionService.createEventSubscriptionBuilder()
                .eventType(eventDefinitionKey)
                .scopeDefinitionId(caseDefinition.getId())
                .scopeType(ScopeTypes.CMMN)
                .configuration(correlationKey);

        if (caseDefinition.getTenantId() != null) {
            eventSubscriptionBuilder.tenantId(caseDefinition.getTenantId());
        }

        // if we need to update the case definition to the latest version upon new deployment, also set the definition key, not just the case definition id
        if (!doNotUpdateToLatestVersionAutomatically) {
            eventSubscriptionBuilder.scopeDefinitionKey(caseDefinition.getKey());
        }

        return eventSubscriptionBuilder.create();
    }
}
