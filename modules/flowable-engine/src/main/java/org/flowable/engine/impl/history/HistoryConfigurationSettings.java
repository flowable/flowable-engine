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
package org.flowable.engine.impl.history;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface HistoryConfigurationSettings {

    /**
     * Returns whether history is enabled on the process engine configuration.
     */
    boolean isHistoryEnabled();

    /**
     * Returns whether any history should be stored for the given process definition
     * (i.e. the history level is different from {@link HistoryLevel#NONE}.
     */
    boolean isHistoryEnabled(String processDefinitionId);

    /**
     * Returns whether the history level is at least the given level.
     * If process definitions have more specific settings the level will be checked against that before checking the engine configuration.
     */
    boolean isHistoryLevelAtLeast(HistoryLevel level, String processDefinitionId);

    /**
     * Returns whether history is enabled for the provided process instance.
     */
    boolean isHistoryEnabledForProcessInstance(ExecutionEntity processInstanceExecution);

    /**
     * Returns whether history is enabled for the provided activity.
     */
    boolean isHistoryEnabledForActivity(ActivityInstance activityInstance);

    /**
     * Returns whether history is enabled for the provided activity.
     * This method has an extra activityInstance parameter, for legacy reasons and should only be used in those exceptional situations.
     */
    boolean isHistoryEnabledForActivity(String processDefinitionId, String activityId);

    /**
     * Returns whether history is enabled for the provided user task.
     */
    boolean isHistoryEnabledForUserTask(TaskEntity taskEntity);

    /**
     * Returns whether history is enabled for the provided user task.
     * This method has an extra executionEntity parameter, for legacy reasons and should only be used in those exceptional situations.
     */
    boolean isHistoryEnabledForUserTask(ExecutionEntity executionEntity, TaskEntity taskEntity);

    /**
     * Returns whether history is enabled for the provided variable instance.
     */
    boolean isHistoryEnabledForVariableInstance(VariableInstanceEntity variableInstanceEntity);

    /**
     * Returns whether history is enabled for the provided variable instance.
     * This method has an extra processDefinitionId parameter, for legacy reasons and should only be used in those exceptional situations.
     */
    boolean isHistoryEnabledForVariableInstance(String processDefinitionId, VariableInstanceEntity variableInstanceEntity);

    /**
     * Returns whether history is enabled for the provided identity link.
     */
    boolean isHistoryEnabledForIdentityLink(IdentityLinkEntity identityLink);

    /**
     * Returns whether history is enabled for the provided entity link.
     */
    boolean isHistoryEnabledForEntityLink(EntityLinkEntity entityLink);

}
