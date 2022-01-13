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
     * Returns whether the process engine configuration has enabled to check
     * for a history level setting on the process definition that is different
     * from the global history level.
     */
    boolean isEnableProcessDefinitionHistoryLevel();

    /**
     * Returns whether any history should be stored for the given process definition
     * (i.e. the history level is different from {@link HistoryLevel#NONE}.
     */
    boolean isHistoryEnabled(String processDefinitionId);

    /**
     * Returns whether the history level is at least the given level.
     * If process definitions have more specific settings (see {@link #isEnableProcessDefinitionHistoryLevel()}),
     * the level will be checked against that before checking the engine configuration.
     */
    boolean isHistoryLevelAtLeast(HistoryLevel level, String processDefinitionId);

    /**
     * Returns whether history is enabled for the provided process instance.
     */
    boolean isHistoryEnabledForProcessInstance(String processDefinitionId, ExecutionEntity processInstanceExecution);

    /**
     * Returns whether history is enabled for the provided activity.
     */
    boolean isHistoryEnabledForActivity(String processDefinitionId, String activityId);

    /**
     * Returns whether history is enabled for the provided user task.
     */
    boolean isHistoryEnabledForUserTask(String processDefinitionId, TaskEntity taskEntity);

    /**
     * Returns whether history is enabled for the provided variable instance.
     */
    boolean isHistoryEnabledForVariableInstance(String processDefinitionId, VariableInstanceEntity variableInstanceEntity);

    /**
     * Returns whether history is enabled for the provided identity link.
     */
    boolean isHistoryEnabledForIdentityLink(String processDefinitionId, IdentityLinkEntity identityLink);

    /**
     * Returns whether history is enabled for the provided entity link.
     */
    boolean isHistoryEnabledForEntityLink(String processDefinitionId, EntityLinkEntity entityLink);

}
