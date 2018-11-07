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
package org.flowable.engine.impl.util;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.variable.api.event.FlowableVariableEvent;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class EventUtil {
    
    public static FlowableVariableEvent createVariableDeleteEvent(VariableInstanceEntity variableInstance) {

        String processDefinitionId = null;
        if (variableInstance.getProcessInstanceId() != null) {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager().findById(variableInstance.getProcessInstanceId());
            if (executionEntity != null) {
                processDefinitionId = executionEntity.getProcessDefinitionId();
            }
        }

        return FlowableEventBuilder.createVariableEvent(FlowableEngineEventType.VARIABLE_DELETED,
                variableInstance.getName(),
                null,
                variableInstance.getType(),
                variableInstance.getTaskId(),
                variableInstance.getExecutionId(),
                variableInstance.getProcessInstanceId(),
                processDefinitionId);
    }

}
