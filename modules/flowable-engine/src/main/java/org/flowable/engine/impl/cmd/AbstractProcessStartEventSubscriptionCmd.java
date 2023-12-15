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
package org.flowable.engine.impl.cmd;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * An abstract command with various common methods for the creation and modification of process start event subscriptions.
 *
 * @author Micha Kiener
 */
public abstract class AbstractProcessStartEventSubscriptionCmd {

    protected String generateCorrelationConfiguration(String eventDefinitionKey, Map<String, Object> correlationParameterValues, CommandContext commandContext) {
        EventModel eventModel = getEventModel(eventDefinitionKey, commandContext);
        Map<String, Object> correlationParameters = new HashMap<>();
        for (Map.Entry<String, Object> correlationValue : correlationParameterValues.entrySet()) {
            // make sure the correlation parameter value is based on a valid, defined correlation parameter within the event model
            checkEventModelCorrelationParameter(eventModel, correlationValue.getKey());
            correlationParameters.put(correlationValue.getKey(), correlationValue.getValue());
        }

        return CommandContextUtil.getEventRegistry().generateKey(correlationParameters);
    }

    protected void checkEventModelCorrelationParameter(EventModel eventModel, String correlationParameterName) {
        Collection<EventPayload> correlationParameters = eventModel.getCorrelationParameters();
        for (EventPayload correlationParameter : correlationParameters) {
            if (correlationParameter.getName().equals(correlationParameterName)) {
                return;
            }
        }
        throw new FlowableIllegalArgumentException("There is no correlation parameter with name '" + correlationParameterName + "' defined in event model "
            + "with key '" + eventModel.getKey() + "'. You can only subscribe for an event with a combination of valid correlation parameters.");
    }

    protected ProcessDefinition getLatestProcessDefinitionByKey(String processDefinitionKey, String tenantId, CommandContext commandContext) {
        RepositoryService repositoryService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getRepositoryService();

        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(processDefinitionKey)
            .latestVersion();

        if (StringUtils.isNotBlank(tenantId)) {
            query.processDefinitionTenantId(tenantId);
        }

        ProcessDefinition processDefinition = query.singleResult();

        if (processDefinition == null) {
            throw new FlowableIllegalArgumentException("No deployed process definition found for key '" + processDefinitionKey + "'.");
        }
        return processDefinition;
    }

    protected ProcessDefinition getProcessDefinitionById(String processDefinitionId, CommandContext commandContext) {
        RepositoryService repositoryService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getRepositoryService();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionId(processDefinitionId)
            .singleResult();

        if (processDefinition == null) {
            throw new FlowableIllegalArgumentException("No deployed process definition found for id '" + processDefinitionId + "'.");
        }
        return processDefinition;
    }

    protected Process getProcess(String processDefinitionId, CommandContext commandContext) {
        RepositoryService repositoryService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getRepositoryService();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        return bpmnModel.getMainProcess();
    }

    protected EventModel getEventModel(String eventDefinitionKey, CommandContext commandContext) {
        EventModel eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(eventDefinitionKey);
        if (eventModel == null) {
            throw new FlowableIllegalArgumentException("Could not find event model with key '" + eventDefinitionKey + "'.");
        }
        return eventModel;
    }

    protected EventSubscriptionService getEventSubscriptionService(CommandContext commandContext) {
        return CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
    }
}
