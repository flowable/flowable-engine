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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.model.Case;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * Context passed to {@link CaseDefinitionStartLifecycleHandler#deploy} carrying the freshly-deployed
 * case definition together with its parsed {@link Case} model.
 */
public class CaseDefinitionStartDeployContext {

    protected final CaseDefinitionEntity caseDefinition;
    protected final Case caseModel;
    protected final CmmnEngineConfiguration cmmnEngineConfiguration;
    protected final CommandContext commandContext;
    protected final boolean restoringPreviousVersion;

    public CaseDefinitionStartDeployContext(CaseDefinitionEntity caseDefinition, Case caseModel,
            CmmnEngineConfiguration cmmnEngineConfiguration, CommandContext commandContext) {
        this(caseDefinition, caseModel, cmmnEngineConfiguration, commandContext, false);
    }

    public CaseDefinitionStartDeployContext(CaseDefinitionEntity caseDefinition, Case caseModel,
            CmmnEngineConfiguration cmmnEngineConfiguration, CommandContext commandContext,
            boolean restoringPreviousVersion) {
        this.caseDefinition = caseDefinition;
        this.caseModel = caseModel;
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.commandContext = commandContext;
        this.restoringPreviousVersion = restoringPreviousVersion;
    }

    public CaseDefinitionEntity getCaseDefinition() {
        return caseDefinition;
    }

    public Case getCaseModel() {
        return caseModel;
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
     * {@code true} when this deploy is restoring a previous (earlier-version) case definition's start
     * triggers because the latest version's deployment was just deleted. Behaviors should skip duplicate-
     * subscription validation in this mode — the just-deleted case definition's subscriptions may still
     * be in the in-session entity cache (the bulk delete hasn't been flushed yet) so a re-insert would
     * otherwise trip a false-positive conflict. The fresh-deployment path leaves this {@code false}.
     */
    public boolean isRestoringPreviousVersion() {
        return restoringPreviousVersion;
    }
}
