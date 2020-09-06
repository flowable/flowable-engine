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
package org.flowable.task.service.impl.util;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.event.impl.FlowableVariableEventBuilder;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tijs Rademakers
 */
public class CountingTaskUtil {

    public static void handleDeleteVariableInstanceEntityCount(VariableInstanceEntity variableInstance, boolean fireDeleteEvent,
            TaskServiceConfiguration taskServiceConfiguration) {
        
        if (variableInstance.getTaskId() != null && isTaskRelatedEntityCountEnabledGlobally(taskServiceConfiguration)) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskServiceConfiguration.getTaskEntityManager().findById(variableInstance.getTaskId());
            if (isTaskRelatedEntityCountEnabled(countingTaskEntity, taskServiceConfiguration)) {
                countingTaskEntity.setVariableCount(countingTaskEntity.getVariableCount() - 1);
            }
        }

        FlowableEventDispatcher eventDispatcher = taskServiceConfiguration.getEventDispatcher();
        if (fireDeleteEvent && eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableVariableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, variableInstance),
                    taskServiceConfiguration.getEngineName());

            eventDispatcher.dispatchEvent(FlowableVariableEventBuilder.createVariableEvent(FlowableEngineEventType.VARIABLE_DELETED,
                    variableInstance, null, variableInstance.getType()), taskServiceConfiguration.getEngineName());
        }
    }

    public static void handleInsertVariableInstanceEntityCount(VariableInstanceEntity variableInstance, TaskServiceConfiguration taskServiceConfiguration) {
        if (variableInstance.getTaskId() != null && isTaskRelatedEntityCountEnabledGlobally(taskServiceConfiguration)) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskServiceConfiguration.getTaskEntityManager().findById(variableInstance.getTaskId());
            if (isTaskRelatedEntityCountEnabled(countingTaskEntity, taskServiceConfiguration)) {
                countingTaskEntity.setVariableCount(countingTaskEntity.getVariableCount() + 1);
            }
        }
    }

    /**
     * Check if the Task Relationship Count performance improvement is enabled.
     */
    public static boolean isTaskRelatedEntityCountEnabledGlobally(TaskServiceConfiguration taskServiceConfiguration) {
        if (taskServiceConfiguration == null) {
            return false;
        }
        
        return taskServiceConfiguration.isEnableTaskRelationshipCounts();
    }

    public static boolean isTaskRelatedEntityCountEnabled(TaskEntity taskEntity, TaskServiceConfiguration taskServiceConfiguration) {
        if (taskEntity instanceof CountingTaskEntity) {
            return isTaskRelatedEntityCountEnabled((CountingTaskEntity) taskEntity, taskServiceConfiguration);
        }
        return false;
    }

    /**
     * Similar functionality with <b>ExecutionRelatedEntityCount</b>, but on the TaskEntity level.
     */
    public static boolean isTaskRelatedEntityCountEnabled(CountingTaskEntity taskEntity, TaskServiceConfiguration taskServiceConfiguration) {
        return isTaskRelatedEntityCountEnabledGlobally(taskServiceConfiguration) && taskEntity.isCountEnabled();
    }
}
