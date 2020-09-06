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
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tijs Rademakers
 */
public class CountingEntityUtil {

    public static void handleDeleteVariableInstanceEntityCount(VariableInstanceEntity variableInstance, boolean fireDeleteEvent) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        if (variableInstance.getTaskId() != null && isTaskRelatedEntityCountEnabledGlobally()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) processEngineConfiguration.getTaskServiceConfiguration().getTaskService()
                    .getTask(variableInstance.getTaskId());
            if (isTaskRelatedEntityCountEnabled(countingTaskEntity)) {
                countingTaskEntity.setVariableCount(countingTaskEntity.getVariableCount() - 1);
            }
        } else if (variableInstance.getExecutionId() != null && isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity executionEntity = (CountingExecutionEntity) processEngineConfiguration.getExecutionEntityManager(
                    ).findById(variableInstance.getExecutionId());
            if (isExecutionRelatedEntityCountEnabled(executionEntity)) {
                executionEntity.setVariableCount(executionEntity.getVariableCount() - 1);
            }
        }
        
        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (fireDeleteEvent && eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, variableInstance),
                    processEngineConfiguration.getEngineCfgKey());
            eventDispatcher.dispatchEvent(EventUtil.createVariableDeleteEvent(variableInstance), processEngineConfiguration.getEngineCfgKey());
        }
    }
    
    public static void handleInsertVariableInstanceEntityCount(VariableInstanceEntity variableInstance) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        if (variableInstance.getTaskId() != null && isTaskRelatedEntityCountEnabledGlobally()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) processEngineConfiguration.getTaskServiceConfiguration().getTaskService()
                    .getTask(variableInstance.getTaskId());
            if (isTaskRelatedEntityCountEnabled(countingTaskEntity)) {
                countingTaskEntity.setVariableCount(countingTaskEntity.getVariableCount() + 1);
            }
        } else if (variableInstance.getExecutionId() != null && isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity executionEntity = (CountingExecutionEntity) processEngineConfiguration.getExecutionEntityManager().findById(variableInstance.getExecutionId());
            if (isExecutionRelatedEntityCountEnabled(executionEntity)) {
                executionEntity.setVariableCount(executionEntity.getVariableCount() + 1);
            }
        } else if (ScopeTypes.BPMN_DEPENDENT.contains(variableInstance.getScopeType()) && isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity executionEntity = (CountingExecutionEntity) processEngineConfiguration.getExecutionEntityManager().findById(variableInstance.getSubScopeId());
            if (isExecutionRelatedEntityCountEnabled(executionEntity)) {
                executionEntity.setVariableCount(executionEntity.getVariableCount() + 1);
            }
        }
    }
    
    public static void handleInsertEventSubscriptionEntityCount(EventSubscription eventSubscription) {
        if (eventSubscription.getExecutionId() != null && CountingEntityUtil.isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity executionEntity = (CountingExecutionEntity) CommandContextUtil.getExecutionEntityManager().findById(
                            eventSubscription.getExecutionId());

            if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(executionEntity)) {
                executionEntity.setEventSubscriptionCount(executionEntity.getEventSubscriptionCount() + 1);
            }
        }
    }
    
    public static void handleDeleteEventSubscriptionEntityCount(EventSubscription eventSubscription) {
        if (eventSubscription.getExecutionId() != null && CountingEntityUtil.isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity executionEntity = (CountingExecutionEntity) CommandContextUtil.getExecutionEntityManager().findById(
                            eventSubscription.getExecutionId());
            
            if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(executionEntity)) {
                executionEntity.setEventSubscriptionCount(executionEntity.getEventSubscriptionCount() - 1);
            }
        }
    }
    
    /* Execution related entity count methods */

    public static boolean isExecutionRelatedEntityCountEnabledGlobally() {
        return CommandContextUtil.getProcessEngineConfiguration().getPerformanceSettings().isEnableExecutionRelationshipCounts();
    }

    /**
     * Check if the Task Relationship Count performance improvement is enabled.
     */
    public static boolean isTaskRelatedEntityCountEnabledGlobally() {
        return CommandContextUtil.getProcessEngineConfiguration().getPerformanceSettings().isEnableTaskRelationshipCounts();
    }

    public static boolean isExecutionRelatedEntityCountEnabled(ExecutionEntity executionEntity) {
        if (executionEntity.isProcessInstanceType() || executionEntity instanceof CountingExecutionEntity) {
            return isExecutionRelatedEntityCountEnabled((CountingExecutionEntity) executionEntity);
        }
        return false;
    }

    public static boolean isTaskRelatedEntityCountEnabled(TaskEntity taskEntity) {
        if (taskEntity instanceof CountingTaskEntity) {
            return isTaskRelatedEntityCountEnabled((CountingTaskEntity) taskEntity);
        }
        return false;
    }

    /**
     * There are two flags here: a global flag and a flag on the execution entity. 
     * The global flag can be switched on and off between different reboots, however the flag on the executionEntity refers
     * to the state at that particular moment of the last insert/update.
     * 
     * Global flag / ExecutionEntity flag : result
     * 
     * T / T : T (all true, regular mode with flags enabled) 
     * T / F : F (global is true, but execution was of a time when it was disabled, thus treating it as disabled as the counts can't be guessed) 
     * F / T : F (execution was of time when counting was done. But this is overruled by the global flag and thus the queries will o
     * be done) 
     * F / F : F (all disabled)
     * 
     * From this table it is clear that only when both are true, the result should be true, which is the regular AND rule for booleans.
     */
    public static boolean isExecutionRelatedEntityCountEnabled(CountingExecutionEntity executionEntity) {
        return !executionEntity.isProcessInstanceType() && isExecutionRelatedEntityCountEnabledGlobally() && executionEntity.isCountEnabled();
    }

    /**
     * Similar functionality with <b>ExecutionRelatedEntityCount</b>, but on the TaskEntity level.
     */
    public static boolean isTaskRelatedEntityCountEnabled(CountingTaskEntity taskEntity) {
        return isTaskRelatedEntityCountEnabledGlobally() && taskEntity.isCountEnabled();
    }
    
}
