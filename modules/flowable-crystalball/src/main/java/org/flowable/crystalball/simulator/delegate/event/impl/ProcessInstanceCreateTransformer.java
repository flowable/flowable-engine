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
package org.flowable.crystalball.simulator.delegate.event.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author martin.grofcik
 */
public class ProcessInstanceCreateTransformer extends Flowable2SimulationEventFunction {

    public static final String PROCESS_INSTANCE_ID = "processInstanceId";
    private final String processDefinitionIdKey;
    private final String businessKey;
    private final String variablesKey;

    public ProcessInstanceCreateTransformer(String simulationEventType, String processDefinitionIdKey, String businessKey, String variablesKey) {
        super(simulationEventType);
        this.processDefinitionIdKey = processDefinitionIdKey;
        this.businessKey = businessKey;
        this.variablesKey = variablesKey;
    }

    @Override
    public SimulationEvent apply(FlowableEvent event) {
        if (FlowableEngineEventType.ENTITY_INITIALIZED == event.getType() && (event instanceof FlowableEntityEvent) && ((FlowableEntityEvent) event).getEntity() instanceof ProcessInstance
                && ((ExecutionEntity) ((FlowableEntityEvent) event).getEntity()).isProcessInstanceType()) {

            ProcessInstance processInstance = (ProcessInstance) ((FlowableEntityEvent) event).getEntity();
            ExecutionEntity executionEntity = (ExecutionEntity) ((FlowableEntityEvent) event).getEntity();

            Map<String, Object> simEventProperties = new HashMap<>();
            simEventProperties.put(processDefinitionIdKey, processInstance.getProcessDefinitionId());
            simEventProperties.put(businessKey, processInstance.getBusinessKey());
            simEventProperties.put(variablesKey, executionEntity.getVariables());
            simEventProperties.put(PROCESS_INSTANCE_ID, executionEntity.getProcessInstanceId());

            return new SimulationEvent.Builder(simulationEventType).simulationTime(CommandContextUtil.getProcessEngineConfiguration().getClock().getCurrentTime().getTime()).properties(simEventProperties).priority(2).build();
        }
        return null;
    }
}
