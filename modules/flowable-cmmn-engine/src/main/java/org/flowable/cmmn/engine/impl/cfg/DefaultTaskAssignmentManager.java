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

package org.flowable.cmmn.engine.impl.cfg;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.InternalTaskAssignmentManager;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author martin.grofcik
 */
public class DefaultTaskAssignmentManager implements InternalTaskAssignmentManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected final String parentIdentityLinkType;

    public DefaultTaskAssignmentManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this(cmmnEngineConfiguration, IdentityLinkType.PARTICIPANT);
    }

    public DefaultTaskAssignmentManager(CmmnEngineConfiguration cmmnEngineConfiguration, String parentIdentityLinkType) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.parentIdentityLinkType = parentIdentityLinkType;
    }

    @Override
    public void changeAssignee(Task task, String assignee) {
        if ((task.getAssignee() != null && !task.getAssignee().equals(assignee))
            || (task.getAssignee() == null && assignee != null)) {

            CommandContextUtil.getTaskService().changeTaskAssignee((TaskEntity) task, assignee);

            if (task.getId() != null) {
                addUserIdentityLinkToParent(task, task.getAssignee());
            }
        }
    }

    @Override
    public void changeOwner(Task task, String owner) {
        if ((task.getOwner() != null && !task.getOwner().equals(owner))
            || (task.getOwner() == null && owner != null)) {

            CommandContextUtil.getTaskService().changeTaskOwner((TaskEntity) task, owner);

            if (task.getId() != null) {
                addUserIdentityLinkToParent(task, task.getAssignee());
            }
        }
    }

    @Override
    public void addCandidateUser(Task task, IdentityLink identityLink) {
        handleTaskIdentityLinkAddition( task, identityLink);
    }

    @Override
    public void addCandidateUsers(Task task, List<IdentityLink> candidateUsers) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        for (IdentityLink identityLink : candidateUsers) {
            identityLinks.add((IdentityLinkEntity) identityLink);
        }

        handleTaskIdentityLinkAdditions((TaskEntity) task, identityLinks);
    }

    @Override
    public void addCandidateGroup(Task task, IdentityLink identityLink) {
        handleTaskIdentityLinkAddition(task, identityLink);
    }

    @Override
    public void addCandidateGroups(Task task, List<IdentityLink> candidateGroups) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        for (IdentityLink identityLink : candidateGroups) {
            identityLinks.add((IdentityLinkEntity) identityLink);
        }
        handleTaskIdentityLinkAdditions((TaskEntity) task, identityLinks);
    }

    @Override
    public void addUserIdentityLink(Task task, IdentityLink identityLink) {
        handleTaskIdentityLinkAddition(task, identityLink);
    }

    @Override
    public void addGroupIdentityLink(Task task, IdentityLink identityLink) {
        handleTaskIdentityLinkAddition( task, identityLink);
    }

    @Override
    public void deleteUserIdentityLink(Task task, IdentityLink identityLink) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        identityLinks.add((IdentityLinkEntity) identityLink);
        IdentityLinkUtil.handleTaskIdentityLinkDeletions((TaskEntity) task, identityLinks, true);
    }

    @Override
    public void deleteGroupIdentityLink(Task task, IdentityLink identityLink) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        identityLinks.add((IdentityLinkEntity) identityLink);
        IdentityLinkUtil.handleTaskIdentityLinkDeletions((TaskEntity) task, identityLinks, true);
    }

    @Override
    public void addUserIdentityLinkToParent(Task task, String userId) {
        if (userId != null && ScopeTypes.CMMN.equals(task.getScopeType()) && StringUtils.isNotEmpty(task.getScopeId())) {
            CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager().findById(task.getScopeId());
            createCaseInstanceIdentityLink(caseInstanceEntity, userId, null, parentIdentityLinkType);
        }
    }

    protected void handleTaskIdentityLinkAddition(Task task, IdentityLink identityLink) {
        CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkCreated((IdentityLinkEntity) identityLink);

        CountingTaskEntity countingTaskEntity = (CountingTaskEntity) task;
        if (countingTaskEntity.isCountEnabled()) {
            countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() + 1);
        }

        ((TaskEntity) task).getIdentityLinks().add((IdentityLinkEntity) identityLink);
        if (identityLink.getUserId() != null && task.getScopeId() != null && ScopeTypes.CMMN.equals(task.getScopeType())) {
            CaseInstance caseInstance = CommandContextUtil.getCaseInstanceEntityManager().findById(task.getScopeId());
            if (caseInstance != null) {
                List<IdentityLinkEntity> identityLinks = CommandContextUtil.getIdentityLinkService()
                    .findIdentityLinksByScopeIdAndType(task.getScopeId(), ScopeTypes.CMMN);
                for (IdentityLinkEntity identityLink1 : identityLinks) {
                    if (identityLink1.isUser() && identityLink.getUserId().equals(identityLink.getUserId())) {
                        return;
                    }
                }

                createCaseInstanceIdentityLink(caseInstance, identityLink.getUserId(), null, parentIdentityLinkType);
            }
        }
    }

    protected IdentityLinkEntity createCaseInstanceIdentityLink(CaseInstance caseInstance, String userId, String groupId, String type) {
        IdentityLinkEntity identityLinkEntity = CommandContextUtil.getIdentityLinkService().createScopeIdentityLink(
            caseInstance.getCaseDefinitionId(), caseInstance.getId(), ScopeTypes.CMMN, userId, groupId, type);

        CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkCreated(identityLinkEntity);

        return identityLinkEntity;
    }

    protected void handleTaskIdentityLinkAdditions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinkEntities) {
        for (IdentityLinkEntity identityLinkEntity : identityLinkEntities) {
            handleTaskIdentityLinkAddition(taskEntity, identityLinkEntity);
        }
    }

}
