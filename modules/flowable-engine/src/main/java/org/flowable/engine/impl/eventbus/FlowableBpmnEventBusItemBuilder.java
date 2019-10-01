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
package org.flowable.engine.impl.eventbus;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.impl.eventregistry.FlowableEventBusEventImpl;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.eventbus.AbstractFlowableEventBusItemBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

public class FlowableBpmnEventBusItemBuilder extends AbstractFlowableEventBusItemBuilder implements FlowableEventBusBpmnConstants {

    public static FlowableEventBusEvent createServiceTaskExceptionEvent(ExecutionEntity execution) {
        FlowableEventBusEvent event = new FlowableEventBusEventImpl(TYPE_SERVICETASK_EXCEPTION,
                        execution.getProcessInstanceId(), ScopeTypes.BPMN, execution.getId());
        event.setScopeDefinitionId(execution.getProcessDefinitionId());
        event.setScopeDefinitionKey(execution.getProcessDefinitionKey());
        Map<String, Object> executionData = createProcessData(execution);
        event.setData(executionData);
        return event;
    }
    
    protected static Map<String, Object> createProcessDataWithVariables(ExecutionEntity execution) {
        Map<String, Object> executionData = createProcessData(execution);
        Map<String, Object> variablesMap = execution.getVariables();
        if (variablesMap != null) {
            executionData.put(VARIABLE_MAP, variablesMap);
        }
        
        return executionData;
    }
    
    protected static Map<String, Object> createProcessData(ExecutionEntity execution) {
        Map<String, Object> executionData = new HashMap<>();
        putIfNotNull(EXECUTION_ID, execution.getId(), executionData);
        putIfNotNull(PROCESS_INSTANCE_ID, execution.getProcessInstanceId(), executionData);
        putIfNotNull(PROCESS_DEFINITION_ID, execution.getProcessDefinitionId(), executionData);
        if (execution.getCurrentFlowElement() != null) {
            putIfNotNull(FLOW_ELEMENT_ID, execution.getCurrentFlowElement().getId(), executionData);
            putIfNotNull(FLOW_ELEMENT_NAME, execution.getCurrentFlowElement().getName(), executionData);
            putIfNotNull(FLOW_ELEMENT_TYPE, execution.getCurrentFlowElement().getClass().getSimpleName(), executionData);
            
        } else {
            putIfNotNull(FLOW_ELEMENT_ID, execution.getActivityId(), executionData);
        }
        
        return executionData;
    }
}
