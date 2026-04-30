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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * Context passed to {@link ProcessLevelStartEventActivityBehavior#deploy} carrying the freshly-deployed
 * process definition together with its parsed model and the start event in scope.
 */
public class ProcessLevelStartEventDeployContext {

    protected final ProcessDefinitionEntity processDefinition;
    protected final Process process;
    protected final BpmnModel bpmnModel;
    protected final StartEvent startEvent;
    protected final ProcessEngineConfigurationImpl processEngineConfiguration;
    protected final CommandContext commandContext;

    public ProcessLevelStartEventDeployContext(ProcessDefinitionEntity processDefinition,
            Process process, BpmnModel bpmnModel, StartEvent startEvent,
            ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext) {
        this.processDefinition = processDefinition;
        this.process = process;
        this.bpmnModel = bpmnModel;
        this.startEvent = startEvent;
        this.processEngineConfiguration = processEngineConfiguration;
        this.commandContext = commandContext;
    }

    public ProcessDefinitionEntity getProcessDefinition() {
        return processDefinition;
    }

    public Process getProcess() {
        return process;
    }

    public BpmnModel getBpmnModel() {
        return bpmnModel;
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

    public EventSubscriptionService getEventSubscriptionService() {
        return processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
    }

}
