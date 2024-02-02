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
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * An abstract command with various common methods for the creation and modification of process start event subscriptions.
 *
 * @author Micha Kiener
 */
public abstract class AbstractProcessStartEventSubscriptionCmd {

    protected String generateCorrelationConfiguration(String eventDefinitionKey, String tenantId, Map<String, Object> correlationParameterValues, CommandContext commandContext) {
        EventModel eventModel = getEventModel(eventDefinitionKey, tenantId, commandContext);
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
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
        ProcessDefinition processDefinition = null;
        if (processDefinitionKey != null && (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId))) {
            processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);

            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("No process definition found for key '" + processDefinitionKey + "'", ProcessDefinition.class);
            }

        } else if (processDefinitionKey != null && tenantId != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {

            processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);

            if (processDefinition == null) {
                ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
                if (processEngineConfiguration.isFallbackToDefaultTenant()) {
                    String defaultTenant = processEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.BPMN, processDefinitionKey);
                    if (StringUtils.isNotEmpty(defaultTenant)) {
                        processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, defaultTenant);
                        
                    } else {
                        processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);
                    }
                    
                    if (processDefinition == null) {
                        throw new FlowableObjectNotFoundException("No process definition found for key '" + processDefinitionKey +
                            "'. Fallback to default tenant was also applied.", ProcessDefinition.class);
                    }
                    
                } else {
                    throw new FlowableObjectNotFoundException("Process definition with key '" + processDefinitionKey +
                        "' and tenantId '"+ tenantId +"' was not found", ProcessDefinition.class);
                }
            }

        }
        
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

    protected EventModel getEventModel(String eventDefinitionKey, String tenantId, CommandContext commandContext) {
        EventModel eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(eventDefinitionKey, tenantId);
        if (eventModel == null) {
            throw new FlowableIllegalArgumentException("Could not find event model with key '" + eventDefinitionKey + "'.");
        }
        return eventModel;
    }

    protected EventSubscriptionService getEventSubscriptionService(CommandContext commandContext) {
        return CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
    }
}
