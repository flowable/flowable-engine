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

import java.util.Set;

import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;

/**
 * Context passed to {@link ProcessLevelStartEventActivityBehavior#undeploy} when a process definition
 * is being superseded by a new version. Carries both the previous (now-superseded) process definition
 * and the new one — most behaviors only need the previous process definition, but the EventRegistry "manual" re-point
 * branch updates subscriptions to point at {@link #getNewProcessDefinition()}.
 * <p>
 * Behaviors register their obsolete event subscription / timer job handler types via
 * {@link #registerObsoleteEventSubscriptionType(String)} and
 * {@link #registerObsoleteTimerJobHandlerType(String)}. The deployer issues one mass-delete per
 * unique registered type after the undeploy iteration — fewer DB round-trips than per-start-event
 * deletes, and tighter than the historical fixed Message+Signal+EventRegistry sweep that always
 * ran regardless of which types the previous process definition actually used.
 */
public class ProcessLevelStartEventUndeployContext {

    protected final ProcessDefinitionEntity previousProcessDefinition;
    protected final ProcessDefinitionEntity newProcessDefinition;
    protected final StartEvent startEvent;
    protected final ProcessEngineConfigurationImpl processEngineConfiguration;
    protected final CommandContext commandContext;
    protected final Set<String> obsoleteEventSubscriptionTypes;
    protected final Set<String> obsoleteTimerJobHandlerTypes;

    public ProcessLevelStartEventUndeployContext(ProcessDefinitionEntity previousProcessDefinition, ProcessDefinitionEntity newProcessDefinition,
            StartEvent startEvent,
            ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext,
            Set<String> obsoleteEventSubscriptionTypes, Set<String> obsoleteTimerJobHandlerTypes) {
        this.previousProcessDefinition = previousProcessDefinition;
        this.newProcessDefinition = newProcessDefinition;
        this.startEvent = startEvent;
        this.processEngineConfiguration = processEngineConfiguration;
        this.commandContext = commandContext;
        this.obsoleteEventSubscriptionTypes = obsoleteEventSubscriptionTypes;
        this.obsoleteTimerJobHandlerTypes = obsoleteTimerJobHandlerTypes;
    }

    public ProcessDefinitionEntity getPreviousProcessDefinition() {
        return previousProcessDefinition;
    }

    public ProcessDefinitionEntity getNewProcessDefinition() {
        return newProcessDefinition;
    }

    public StartEvent getStartEvent() {
        return startEvent;
    }

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }

    /**
     * Registers an event subscription handler type that the deployer should mass-delete for the
     * previous process definition after the undeploy pass. Multiple behaviors registering the same
     * type result in a single DB sweep.
     */
    public void registerObsoleteEventSubscriptionType(String eventHandlerType) {
        obsoleteEventSubscriptionTypes.add(eventHandlerType);
    }

    /**
     * Registers a timer job handler type that the deployer should mass-cancel for the previous
     * process definition after the undeploy pass. Multiple behaviors registering the same type
     * result in a single DB sweep.
     */
    public void registerObsoleteTimerJobHandlerType(String timerJobHandlerType) {
        obsoleteTimerJobHandlerTypes.add(timerJobHandlerType);
    }
}
