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
package org.flowable.cmmn.engine.impl.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.cmmn.engine.interceptor.CmmnIdentityLinkInterceptor;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class DefaultCmmnIdentityLinkInterceptor implements CmmnIdentityLinkInterceptor {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public DefaultCmmnIdentityLinkInterceptor(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public void handleCompleteTask(TaskEntity task) {
        String userId = Authentication.getAuthenticatedUserId();
        if (StringUtils.isNotEmpty(userId)) {
            addUserIdentityLinkToParent(task, userId);
        }
    }
    
    @Override
    public void handleAddIdentityLinkToTask(TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity) {
        addUserIdentityLinkToParent(taskEntity, identityLinkEntity.getUserId());
    }
    
    @Override
    public void handleAddAssigneeIdentityLinkToTask(TaskEntity taskEntity, String assignee) {
        addUserIdentityLinkToParent(taskEntity, assignee);
    }
    
    @Override
    public void handleAddOwnerIdentityLinkToTask(TaskEntity taskEntity, String owner) {
        addUserIdentityLinkToParent(taskEntity, owner);
    }

    @Override
    public void handleCreateCaseInstance(CaseInstanceEntity caseInstance) {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        if (authenticatedUserId != null) {
            IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstance, authenticatedUserId, null, IdentityLinkType.STARTER, cmmnEngineConfiguration);
        }
    }

    @Override
    public void handleReactivateCaseInstance(CaseInstanceEntity caseInstance) {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        List<IdentityLinkEntity> identityLinks = createCaseIdentityLinksFromHistoricCaseInstance(caseInstance.getId());

        if (authenticatedUserId != null) {
            for (IdentityLinkEntity identityLink : identityLinks) {
                if (identityLink.isUser() && identityLink.getUserId().equals(authenticatedUserId) && IdentityLinkType.REACTIVATOR.equals(identityLink.getType())) {
                    return;
                }
            }

            IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstance, authenticatedUserId, null, IdentityLinkType.REACTIVATOR, cmmnEngineConfiguration);
        }
    }

    protected List<IdentityLinkEntity> createCaseIdentityLinksFromHistoricCaseInstance(String caseInstanceId) {
        List<HistoricIdentityLinkEntity> historicIdentityLinks = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                .getHistoricIdentityLinkService()
                .findHistoricIdentityLinksByScopeIdAndScopeType(caseInstanceId, ScopeTypes.CMMN);

        List<IdentityLinkEntity> identityLinkEntities = new ArrayList<>(historicIdentityLinks.size());
        for (HistoricIdentityLinkEntity historicIdentityLink : historicIdentityLinks) {
            IdentityLinkEntity identityLink = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                    .createIdentityLinkFromHistoricIdentityLink(historicIdentityLink);
            cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService().insertIdentityLink(identityLink);
            identityLinkEntities.add(identityLink);
        }
        return identityLinkEntities;
    }

    protected void addUserIdentityLinkToParent(Task task, String userId) {
        if (userId != null && ScopeTypes.CMMN.equals(task.getScopeType()) && StringUtils.isNotEmpty(task.getScopeId())) {
            CaseInstanceEntity caseInstanceEntity = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(task.getScopeId());
            if (caseInstanceEntity != null) {
                List<IdentityLinkEntity> identityLinks = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                    .findIdentityLinksByScopeIdAndType(caseInstanceEntity.getId(), ScopeTypes.CMMN);
                for (IdentityLinkEntity identityLink : identityLinks) {
                    if (identityLink.isUser() && identityLink.getUserId().equals(userId) && IdentityLinkType.PARTICIPANT.equals(identityLink.getType())) {
                        return;
                    }
                }

                IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstanceEntity, userId, null, IdentityLinkType.PARTICIPANT, cmmnEngineConfiguration);
            }
        }
    }
}
