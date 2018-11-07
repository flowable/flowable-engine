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
package org.flowable.engine.impl.persistence.entity;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * Helper class for suspension state
 * 
 * @author Tijs Rademakers
 */
public class SuspensionStateUtil {

    public static void setSuspensionState(ProcessDefinitionEntity processDefinitionEntity, SuspensionState state) {
        if (processDefinitionEntity.getSuspensionState() == state.getStateCode()) {
            throw new FlowableException("Cannot set suspension state '" + state + "' for " + processDefinitionEntity + "': already in state '" + state + "'.");
        }
        processDefinitionEntity.setSuspensionState(state.getStateCode());
        dispatchStateChangeEvent(processDefinitionEntity, state);
    }

    public static void setSuspensionState(ExecutionEntity executionEntity, SuspensionState state) {
        if (executionEntity.getSuspensionState() == state.getStateCode()) {
            throw new FlowableException("Cannot set suspension state '" + state + "' for " + executionEntity + "': already in state '" + state + "'.");
        }
        executionEntity.setSuspensionState(state.getStateCode());
        dispatchStateChangeEvent(executionEntity, state);
    }

    public static void setSuspensionState(TaskEntity taskEntity, SuspensionState state) {
        if (taskEntity.getSuspensionState() == state.getStateCode()) {
            throw new FlowableException("Cannot set suspension state '" + state + "' for " + taskEntity + "': already in state '" + state + "'.");
        }
        taskEntity.setSuspensionState(state.getStateCode());
        dispatchStateChangeEvent(taskEntity, state);
    }

    protected static void dispatchStateChangeEvent(Object entity, SuspensionState state) {
        if (Context.getCommandContext() != null && CommandContextUtil.getEventDispatcher().isEnabled()) {
            FlowableEngineEventType eventType = null;
            if (state == SuspensionState.ACTIVE) {
                eventType = FlowableEngineEventType.ENTITY_ACTIVATED;
            } else {
                eventType = FlowableEngineEventType.ENTITY_SUSPENDED;
            }
            CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(eventType, entity));
        }
    }

}
