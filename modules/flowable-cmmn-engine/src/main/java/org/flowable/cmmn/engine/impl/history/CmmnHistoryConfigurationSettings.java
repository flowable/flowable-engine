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
package org.flowable.cmmn.engine.impl.history;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface CmmnHistoryConfigurationSettings {

    /**
     * Returns whether history is enabled on the case engine configuration.
     */
    boolean isHistoryEnabled();

    /**
     * Returns whether the case engine configuration has enabled to check
     * for a history level setting on the case definition that is different
     * from the global history level.
     */
    boolean isEnableCaseDefinitionHistoryLevel();

    /**
     * Returns whether any history should be stored for the given case definition
     * (i.e. the history level is different from {@link HistoryLevel#NONE}.
     */
    boolean isHistoryEnabled(String caseDefinitionId);

    /**
     * Returns whether the history level is at least the given level.
     * If case definitions have more specific settings (see {@link #isEnableCaseDefinitionHistoryLevel()}),
     * the level will be checked against that before checking the engine configuration.
     */
    boolean isHistoryLevelAtLeast(HistoryLevel level, String caseDefinitionId);

    /**
     * Returns whether history is enabled for the provided case instance.
     */
    boolean isHistoryEnabledForCaseInstance(String caseDefinitionId, CaseInstanceEntity caseInstanceEntity);

    /**
     * Returns whether history is enabled for the provided activity.
     */
    boolean isHistoryEnabledForActivity(String caseDefinitionId, String activityId);

    /**
     * Returns whether history is enabled for the provided user task.
     */
    boolean isHistoryEnabledForUserTask(String caseDefinitionId, TaskEntity taskEntity);

    /**
     * Returns whether history is enabled for the provided variable instance.
     */
    boolean isHistoryEnabledForVariableInstance(String caseDefinitionId, VariableInstanceEntity variableInstanceEntity);

    /**
     * Returns whether history is enabled for the provided identity link.
     */
    boolean isHistoryEnabledForIdentityLink(String caseDefinitionId, IdentityLinkEntity identityLinkEntity);

    /**
     * Returns whether history is enabled for the provided entity link.
     */
    boolean isHistoryEnabledForEntityLink(String caseDefinitionId, EntityLinkEntity entityLink);

}
