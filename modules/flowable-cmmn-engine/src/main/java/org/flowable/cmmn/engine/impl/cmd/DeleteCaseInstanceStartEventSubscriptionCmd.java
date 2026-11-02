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

import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceStartEventSubscriptionDeletionBuilderImpl;
import org.flowable.cmmn.model.Case;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * This command deletes event subscriptions with a case start event and optional correlation parameter values.
 *
 * @author Micha Kiener
 */
public class DeleteCaseInstanceStartEventSubscriptionCmd extends AbstractCaseStartEventSubscriptionCmd implements Command<Void>, Serializable {
    private static final long serialVersionUID = 1L;

    protected final CaseInstanceStartEventSubscriptionDeletionBuilderImpl builder;

    public DeleteCaseInstanceStartEventSubscriptionCmd(CaseInstanceStartEventSubscriptionDeletionBuilderImpl builder) {
        this.builder = builder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        Case caze = getCase(builder.getCaseDefinitionId(), commandContext);
        String eventDefinitionKey = caze.getStartEventType();
        String startCorrelationConfiguration = getStartCorrelationConfiguration(builder.getCaseDefinitionId(), commandContext);

        if (eventDefinitionKey != null && Objects.equals(startCorrelationConfiguration, CmmnXmlConstants.START_EVENT_CORRELATION_MANUAL)) {
            String correlationKey = null;

            if (builder.hasCorrelationParameterValues()) {
                correlationKey = generateCorrelationConfiguration(eventDefinitionKey, builder.getTenantId(),
                        builder.getCorrelationParameterValues(), commandContext);
            }

            getEventSubscriptionService(commandContext).deleteEventSubscriptionsForScopeDefinitionAndScopeStartEvent(builder.getCaseDefinitionId(),
                eventDefinitionKey, correlationKey);
        }

        return null;
    }
}
