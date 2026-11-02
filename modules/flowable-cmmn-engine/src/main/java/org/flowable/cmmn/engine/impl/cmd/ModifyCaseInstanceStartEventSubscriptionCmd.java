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
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceStartEventSubscriptionModificationBuilderImpl;
import org.flowable.cmmn.model.Case;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * This command either modifies event subscriptions with a case start event and optional correlation parameter values.
 *
 * @author Micha Kiener
 */
public class ModifyCaseInstanceStartEventSubscriptionCmd extends AbstractCaseStartEventSubscriptionCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected final CaseInstanceStartEventSubscriptionModificationBuilderImpl builder;

    public ModifyCaseInstanceStartEventSubscriptionCmd(CaseInstanceStartEventSubscriptionModificationBuilderImpl builder) {
        this.builder = builder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        CaseDefinition newCaseDefinition;
        if (builder.hasNewCaseDefinitionId()) {
            newCaseDefinition = getCaseDefinitionById(builder.getNewCaseDefinitionId(), commandContext);
        } else {
            // no explicit case definition provided, so use latest one
            CaseDefinition caseDefinition = getCaseDefinitionById(builder.getCaseDefinitionId(), commandContext);
            newCaseDefinition = getLatestCaseDefinitionByKey(caseDefinition.getKey(), caseDefinition.getTenantId(), commandContext);
        }

        if (newCaseDefinition == null) {
            throw new FlowableIllegalArgumentException("Cannot find case definition with id " + (builder.hasNewCaseDefinitionId() ?
                builder.getNewCaseDefinitionId() :
                builder.getCaseDefinitionId()));
        }

        Case caze = getCase(newCaseDefinition.getId(), commandContext);

        String eventDefinitionKey = caze.getStartEventType();
        String startCorrelationConfiguration = getStartCorrelationConfiguration(newCaseDefinition.getId(), commandContext);

        if (eventDefinitionKey != null && Objects.equals(startCorrelationConfiguration, CmmnXmlConstants.START_EVENT_CORRELATION_MANUAL)) {
            String correlationKey = null;

            if (builder.hasCorrelationParameterValues()) {
                correlationKey = generateCorrelationConfiguration(eventDefinitionKey, builder.getTenantId(),
                        builder.getCorrelationParameterValues(), commandContext);
            }

            getEventSubscriptionService(commandContext).updateEventSubscriptionScopeDefinitionId(builder.getCaseDefinitionId(), newCaseDefinition.getId(),
                eventDefinitionKey, null, correlationKey);
        }

        return null;
    }
}
