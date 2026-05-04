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

import java.util.Set;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.model.Case;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * Context passed to {@link CaseDefinitionStartLifecycleHandler#undeploy} when a case definition is
 * being superseded by a new version. Carries both the previous (now-superseded) case definition and
 * the new one — most handlers only need the previous case definition, but the EventRegistry "manual"
 * re-point branch updates subscriptions to point at {@link #getNewCaseDefinition()}.
 * <p>
 * Handlers register their obsolete event subscription types via
 * {@link #registerObsoleteEventSubscriptionType(String)}. The deployer issues one mass-delete per
 * unique registered type after the undeploy iteration — fewer DB round-trips than per-handler
 * deletes, and tighter than a fixed sweep that always ran regardless of which types the previous
 * case definition actually used.
 */
public class CaseDefinitionStartUndeployContext {

    protected final CaseDefinitionEntity previousCaseDefinition;
    protected final CaseDefinitionEntity newCaseDefinition;
    protected final Case previousCaseModel;
    protected final CmmnEngineConfiguration cmmnEngineConfiguration;
    protected final CommandContext commandContext;
    protected final Set<String> obsoleteEventSubscriptionTypes;

    public CaseDefinitionStartUndeployContext(CaseDefinitionEntity previousCaseDefinition, CaseDefinitionEntity newCaseDefinition,
            Case previousCaseModel, CmmnEngineConfiguration cmmnEngineConfiguration, CommandContext commandContext,
            Set<String> obsoleteEventSubscriptionTypes) {
        this.previousCaseDefinition = previousCaseDefinition;
        this.newCaseDefinition = newCaseDefinition;
        this.previousCaseModel = previousCaseModel;
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.commandContext = commandContext;
        this.obsoleteEventSubscriptionTypes = obsoleteEventSubscriptionTypes;
    }

    public CaseDefinitionEntity getPreviousCaseDefinition() {
        return previousCaseDefinition;
    }

    public CaseDefinitionEntity getNewCaseDefinition() {
        return newCaseDefinition;
    }

    public Case getPreviousCaseModel() {
        return previousCaseModel;
    }

    public CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return cmmnEngineConfiguration;
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }

    public EventSubscriptionService getEventSubscriptionService() {
        return cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
    }

    /**
     * Registers an event subscription event-type that the deployer should mass-delete for the previous
     * case definition (scope-type CMMN, scope-id null) after the undeploy iteration. Multiple handlers
     * registering the same type result in a single DB sweep.
     */
    public void registerObsoleteEventSubscriptionType(String eventType) {
        obsoleteEventSubscriptionTypes.add(eventType);
    }
}
