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
package org.flowable.variable.service.impl.eventbus;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.eventbus.FlowableEventBusItem;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.eventbus.AbstractFlowableEventBusItemBuilder;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class FlowableVariableEventBusItemBuilder extends AbstractFlowableEventBusItemBuilder implements FlowableEventBusVariableConstants {

    public static FlowableEventBusItem createVariableCreatedEvent(VariableInstanceEntity variableInstance) {
        FlowableEventBusItem newEvent = createFlowableEventBusItem(TYPE_VARIABLE_CREATED, variableInstance);
        Map<String, Object> variableData = createVariableData(variableInstance);
        newEvent.setData(variableData);
        return newEvent;
    }
    
    public static FlowableEventBusItem createVariableUpdatedEvent(VariableInstanceEntity variableInstance, Object oldVariableValue, String oldVariableType) {
        FlowableEventBusItem updatedEvent = createFlowableEventBusItem(TYPE_VARIABLE_UPDATED, variableInstance);
        Map<String, Object> variableData = createVariableData(variableInstance);
        putIfNotNull(OLD_VARIABLE_TYPE, oldVariableType, variableData);
        variableData.put(OLD_VARIABLE_VALUE, oldVariableValue);
        updatedEvent.setData(variableData);
        
        return updatedEvent;
    }
    
    protected static FlowableEventBusItem createFlowableEventBusItem(String eventType, VariableInstanceEntity variableInstance) {
        FlowableEventBusItem event = null;
        if (variableInstance.getProcessInstanceId() != null) {
            event = new FlowableEventBusItem(eventType, variableInstance.getProcessInstanceId(), ScopeTypes.BPMN, variableInstance.getId());
            event.setScopeDefinitionId(variableInstance.getProcessDefinitionId());
            
        } else if (variableInstance.getScopeId() != null) {
            event = new FlowableEventBusItem(eventType, variableInstance.getScopeId(), variableInstance.getScopeType(), variableInstance.getId());
        } else {
            event = new FlowableEventBusItem();
            event.setType(eventType);
            event.setCorrelationKey(variableInstance.getId());
        }
        
        return event;
    }
    
    protected static Map<String, Object> createVariableData(VariableInstanceEntity variableInstance) {
        Map<String, Object> variableData = new HashMap<>();
        putIfNotNull(VARIABLE_ID, variableInstance.getId(), variableData);
        putIfNotNull(VARIABLE_NAME, variableInstance.getName(), variableData);
        if (variableInstance.getType() != null) {
            variableData.put(VARIABLE_TYPE, variableInstance.getType().getTypeName());
        } else {
            variableData.put(VARIABLE_TYPE, "null");
        }
        
        // not supporting JPA entities
        if (!variableInstance.getType().getTypeName().startsWith("jpa")) {
            variableData.put(VARIABLE_VALUE, variableInstance.getValue());
        }
        
        putIfNotNull(TASK_ID, variableInstance.getTaskId(), variableData);
        addProcessInfo(variableInstance.getExecutionId(), variableInstance.getProcessInstanceId(), variableInstance.getProcessDefinitionId(), variableData);
        addScopeInfo(variableInstance.getScopeId(), variableInstance.getSubScopeId(), variableInstance.getScopeType(), variableData);
        
        return variableData;
    }
}
