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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class BaseCmmnHistoryManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    protected CmmnHistoryConfigurationSettings historyConfigurationSettings;
    
    public BaseCmmnHistoryManager(CmmnEngineConfiguration cmmnEngineConfiguration, CmmnHistoryConfigurationSettings cmmnHistoryConfigurationSettings) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.historyConfigurationSettings = cmmnHistoryConfigurationSettings;
    }

    protected boolean isHistoryLevelAtLeast(HistoryLevel level) {
        return isHistoryLevelAtLeast(level, null);
    }

    protected boolean isEnableCaseDefinitionHistoryLevel() {
        return historyConfigurationSettings.isEnableCaseDefinitionHistoryLevel();
    }

    protected boolean isHistoryLevelAtLeast(HistoryLevel level, String caseDefinitionId) {
        return historyConfigurationSettings.isHistoryLevelAtLeast(level, caseDefinitionId);
    }
    
    protected boolean isHistoryEnabled(String caseDefinitionId) {
        return historyConfigurationSettings.isHistoryEnabled(caseDefinitionId);
    }
    
    protected String getCaseDefinitionId(IdentityLinkEntity identityLink) {
        String caseDefinitionId = null;
        if (identityLink.getScopeDefinitionId() != null) {
            return identityLink.getScopeDefinitionId();
            
        } else if (identityLink.getScopeId() != null) {
            CaseInstanceEntity caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(identityLink.getScopeId());
            if (caseInstance != null) {
                caseDefinitionId = caseInstance.getCaseDefinitionId();
            }
        } else if (identityLink.getTaskId() != null) {
            TaskEntity task = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(identityLink.getTaskId());
            if (task != null) {
                caseDefinitionId = task.getScopeDefinitionId();
            }
        }
        return caseDefinitionId;
    }
    
    protected String getCaseDefinitionId(EntityLinkEntity entityLink) {
        String caseDefinitionId = null;
        if (ScopeTypes.CMMN.equals(entityLink.getScopeType()) && entityLink.getScopeId() != null) {
            CaseInstanceEntity caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(entityLink.getScopeId());
            if (caseInstance != null) {
                caseDefinitionId = caseInstance.getCaseDefinitionId();
            }

        } else if (ScopeTypes.TASK.equals(entityLink.getScopeType()) && entityLink.getScopeId() != null) {
            TaskEntity task = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(entityLink.getScopeId());
            if (task != null) {
                caseDefinitionId = task.getScopeDefinitionId();
            }
        }
        return caseDefinitionId;
    }
    
    protected boolean hasTaskHistoryLevel(String caseDefinitionId, TaskEntity taskEntity) {
        return historyConfigurationSettings.isHistoryEnabledForUserTask(caseDefinitionId, taskEntity);
    }
    
    protected boolean hasActivityHistoryLevel(String caseDefinitionId, String activityId) {
        return historyConfigurationSettings.isHistoryEnabledForActivity(caseDefinitionId, activityId);
    }
    
}
