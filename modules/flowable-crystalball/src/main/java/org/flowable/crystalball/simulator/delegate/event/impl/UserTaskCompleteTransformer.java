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
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.Task;

/**
 * @author martin.grofcik
 */
public class UserTaskCompleteTransformer extends Flowable2SimulationEventFunction {

    public static final String PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String TASK_DEFINITION_KEY = "taskDefinitionKey";
    public static final String TASK_VARIABLES = "taskVariables";

    public UserTaskCompleteTransformer(String simulationEventType) {
        super(simulationEventType);
    }

    @Override
    public SimulationEvent apply(FlowableEvent event) {
        if (FlowableEngineEventType.TASK_COMPLETED == event.getType()) {
            Task task = (Task) ((FlowableEntityEvent) event).getEntity();

            Map<String, Object> properties = new HashMap<>();
            properties.put("taskId", task.getId());
            properties.put(TASK_DEFINITION_KEY, task.getTaskDefinitionKey());
            properties.put(PROCESS_INSTANCE_ID, task.getProcessInstanceId());
            properties.put(TASK_VARIABLES, task.getProcessVariables());
            return new SimulationEvent.Builder(this.simulationEventType).simulationTime(CommandContextUtil.getProcessEngineConfiguration().getClock().getCurrentTime().getTime()).properties(properties).build();
        }
        return null;
    }
}
