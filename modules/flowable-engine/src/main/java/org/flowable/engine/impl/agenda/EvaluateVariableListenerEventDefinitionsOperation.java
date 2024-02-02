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
package org.flowable.engine.impl.agenda;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionData;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Operation that triggers conditional events for which the condition evaluate to true and continues the process, leaving that activity.
 * 
 * @author Tijs Rademakers
 */
public class EvaluateVariableListenerEventDefinitionsOperation extends AbstractOperation {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateVariableListenerEventDefinitionsOperation.class);
    
    protected String processDefinitionId;
    protected String processInstanceId;

    public EvaluateVariableListenerEventDefinitionsOperation(CommandContext commandContext, String processDefinitionId, String processInstanceId) {
        super(commandContext, null);
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId; 
    }

    @Override
    public void run() {
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);
        if (!bpmnModel.hasVariableListeners()) {
            return;
        }
        
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        VariableListenerSession variableListenerSession = commandContext.getSession(VariableListenerSession.class);
        Map<String, List<VariableListenerSessionData>> variableSessionData = variableListenerSession.getVariableData();
        if (variableSessionData == null || variableSessionData.isEmpty()) {
            return;
        }
        
        List<EventSubscriptionEntity> eventSubscriptionEntities = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionEntityManager()
                .findEventSubscriptionsByProcessInstanceAndType(processInstanceId, "variable");
        
        for (EventSubscriptionEntity eventSubscription : eventSubscriptionEntities) {
            
            if (eventSubscription.isDeleted() || !variableSessionData.containsKey(eventSubscription.getEventName())) {
                continue;
            }
            
            if (!bpmnModel.containsVariableListenerForVariableName(eventSubscription.getEventName())) {
                continue;
            }
            
            List<VariableListenerSessionData> variableListenerDataList = variableSessionData.get(eventSubscription.getEventName());
            Iterator<VariableListenerSessionData> itVariableListener = variableListenerDataList.iterator();
            while (itVariableListener.hasNext()) {
                VariableListenerSessionData variableListenerData = itVariableListener.next();
               
                if (!processInstanceId.equals(variableListenerData.getScopeId())) {
                    continue;
                }
                
                String executionId = eventSubscription.getExecutionId();
                ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
                
                String configuration = eventSubscription.getConfiguration();
                String changeTypeValue = VariableListenerEventDefinition.CHANGE_TYPE_ALL;
                if (StringUtils.isNotEmpty(configuration)) {
                    try {
                        JsonNode configNode = processEngineConfiguration.getObjectMapper().readTree(configuration);
                        if (configNode.has(VariableListenerEventDefinition.CHANGE_TYPE_PROPERTY) && !configNode.get(VariableListenerEventDefinition.CHANGE_TYPE_PROPERTY).isNull()) {
                            changeTypeValue = configNode.get(VariableListenerEventDefinition.CHANGE_TYPE_PROPERTY).asText();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error reading variable listener configuration value for {}", eventSubscription.getActivityId(), e);
                    }
                }
                
                if (changeTypeValue.equals(variableListenerData.getChangeType()) ||
                        VariableListenerEventDefinition.CHANGE_TYPE_ALL.equals(changeTypeValue) || (VariableListenerEventDefinition.CHANGE_TYPE_UPDATE_CREATE.equals(changeTypeValue) &&
                                (VariableListenerEventDefinition.CHANGE_TYPE_CREATE.equals(variableListenerData.getChangeType()) || VariableListenerEventDefinition.CHANGE_TYPE_UPDATE.equals(variableListenerData.getChangeType())))) {
                    
                    itVariableListener.remove();
                    CommandContextUtil.getAgenda().planTriggerExecutionOperation(execution);
                }
            }               
        }
    }
}
