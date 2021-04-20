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
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionData;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * Operation that triggers conditional events for which the condition evaluate to true and continues the process, leaving that activity.
 * 
 * @author Tijs Rademakers
 */
public class EvaluateVariableListenerEventsOperation extends AbstractOperation {
    
    protected String processDefinitionId;
    protected String processInstanceId;

    public EvaluateVariableListenerEventsOperation(CommandContext commandContext, String processDefinitionId, String processInstanceId) {
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
        
        for (String variableName : variableSessionData.keySet()) {
            
            if (!bpmnModel.containsVariableListenerForVariableName(variableName)) {
                continue;
            }
            
            List<VariableListenerSessionData> variableListenerDataList = variableSessionData.get(variableName);
            Iterator<VariableListenerSessionData> itVariableListener = variableListenerDataList.iterator();
            while (itVariableListener.hasNext()) {
                VariableListenerSessionData variableListenerData = itVariableListener.next();
               
                if (!processInstanceId.equals(variableListenerData.getScopeId())) {
                    continue;
                }
                
                List<EventSubscriptionEntity> eventSubscriptionEntities = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionEntityManager()
                        .findEventSubscriptionsByProcessInstanceAndType(processInstanceId, "variable");
                for (EventSubscriptionEntity eventSubscription : eventSubscriptionEntities) {
                    if (eventSubscription.isDeleted() || !variableName.equals(eventSubscription.getEventName())) {
                        continue;
                    }
                    
                    String executionId = eventSubscription.getExecutionId();
                    ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
                    FlowNode currentFlowElement = (FlowNode) execution.getCurrentFlowElement();

                    if (currentFlowElement == null) {
                        throw new FlowableException("Error while sending signal for event subscription '" + eventSubscription.getId() + "': " + "no activity associated with event subscription");
                    }
                    
                    if (!(currentFlowElement instanceof Event)) {
                        throw new FlowableException("Unexpected activity type listening to event subscription '" + eventSubscription.getId() + "': " + currentFlowElement.getId() + " " + currentFlowElement.getClass().getName());
                    }
                    
                    Event event = (Event) currentFlowElement;
                    EventDefinition eventDefinition = event.getEventDefinitions().iterator().next();
                    
                    if (!(eventDefinition instanceof VariableListenerEventDefinition)) {
                        throw new FlowableException("Unexpected event definition for event subscription '" + eventSubscription.getId() + "': " + eventDefinition.getClass().getName());
                    }
                    
                    VariableListenerEventDefinition variableListenerEventDefinition = (VariableListenerEventDefinition) eventDefinition;
                    if (StringUtils.isEmpty(variableListenerEventDefinition.getVariableChangeType()) || variableListenerEventDefinition.getVariableChangeType().equals(variableListenerData.getChangeType()) ||
                            VariableListenerEventDefinition.CHANGE_TYPE_ALL.equals(variableListenerEventDefinition.getVariableChangeType()) ||
                            (VariableListenerEventDefinition.CHANGE_TYPE_UPDATE_CREATE.equals(variableListenerEventDefinition.getVariableChangeType()) &&
                                    (VariableListenerEventDefinition.CHANGE_TYPE_CREATE.equals(variableListenerData.getChangeType()) || VariableListenerEventDefinition.CHANGE_TYPE_UPDATE.equals(variableListenerData.getChangeType())))) {
                        
                        itVariableListener.remove();
                        CommandContextUtil.getAgenda().planTriggerExecutionOperation(execution);
                    }
                }
            }               
        }
    }
}
