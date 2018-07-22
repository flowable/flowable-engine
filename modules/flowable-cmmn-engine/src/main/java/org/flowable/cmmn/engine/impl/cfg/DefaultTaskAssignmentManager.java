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
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.InternalTaskAssignmentManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author martin.grofcik
 */
public class DefaultTaskAssignmentManager implements InternalTaskAssignmentManager {
    
    protected String parentIdentityLinkType;

    public DefaultTaskAssignmentManager() {
        this(IdentityLinkType.PARTICIPANT);
    }

    public DefaultTaskAssignmentManager(String parentIdentityLinkType) {
        this.parentIdentityLinkType = parentIdentityLinkType;
    }

    @Override
    public void changeAssignee(Task task, String assignee) {
        TaskHelper.changeTaskAssignee((TaskEntity) task, assignee);
    }

    @Override
    public void changeOwner(Task task, String owner) {
        TaskHelper.changeTaskOwner((TaskEntity) task, owner);
    }

    @Override
    public void addCandidateUser(Task task, IdentityLink identityLink) {
        IdentityLinkUtil.handleTaskIdentityLinkAddition((TaskEntity) task, (IdentityLinkEntity) identityLink);
    }

    @Override
    public void addCandidateUsers(Task task, List<IdentityLink> candidateUsers) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        for (IdentityLink identityLink : candidateUsers) {
            identityLinks.add((IdentityLinkEntity) identityLink);
        }
        IdentityLinkUtil.handleTaskIdentityLinkAdditions((TaskEntity) task, identityLinks);
    }

    @Override
    public void addCandidateGroup(Task task, IdentityLink identityLink) {
        IdentityLinkUtil.handleTaskIdentityLinkAddition((TaskEntity) task, (IdentityLinkEntity) identityLink);
    }

    @Override
    public void addCandidateGroups(Task task, List<IdentityLink> candidateGroups) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        for (IdentityLink identityLink : candidateGroups) {
            identityLinks.add((IdentityLinkEntity) identityLink);
        }
        IdentityLinkUtil.handleTaskIdentityLinkAdditions((TaskEntity) task, identityLinks);
    }

    @Override
    public void addUserIdentityLink(Task task, IdentityLink identityLink) {
        IdentityLinkUtil.handleTaskIdentityLinkAddition((TaskEntity) task, (IdentityLinkEntity) identityLink);
    }

    @Override
    public void addGroupIdentityLink(Task task, IdentityLink identityLink) {
        IdentityLinkUtil.handleTaskIdentityLinkAddition((TaskEntity) task, (IdentityLinkEntity) identityLink);
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
            if (caseInstanceEntity != null) {
                List<IdentityLinkEntity> identityLinks = CommandContextUtil.getIdentityLinkService()
                    .findIdentityLinksByScopeIdAndType(caseInstanceEntity.getId(), ScopeTypes.CMMN);
                for (IdentityLinkEntity identityLink : identityLinks) {
                    if (identityLink.isUser() && identityLink.getUserId().equals(userId) && parentIdentityLinkType.equals(identityLink.getType())) {
                        return;
                    }
                }

                IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstanceEntity, userId, null, parentIdentityLinkType);
            }
        }
    }

}
