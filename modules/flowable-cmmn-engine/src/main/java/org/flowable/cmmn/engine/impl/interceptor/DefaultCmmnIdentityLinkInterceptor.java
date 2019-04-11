package org.flowable.cmmn.engine.impl.interceptor;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.cmmn.engine.interceptor.CmmnIdentityLinkInterceptor;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class DefaultCmmnIdentityLinkInterceptor implements CmmnIdentityLinkInterceptor {

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
            IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstance, authenticatedUserId, null, IdentityLinkType.STARTER);
        }
    }
    
    protected void addUserIdentityLinkToParent(Task task, String userId) {
        if (userId != null && ScopeTypes.CMMN.equals(task.getScopeType()) && StringUtils.isNotEmpty(task.getScopeId())) {
            CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager().findById(task.getScopeId());
            if (caseInstanceEntity != null) {
                List<IdentityLinkEntity> identityLinks = CommandContextUtil.getIdentityLinkService()
                    .findIdentityLinksByScopeIdAndType(caseInstanceEntity.getId(), ScopeTypes.CMMN);
                for (IdentityLinkEntity identityLink : identityLinks) {
                    if (identityLink.isUser() && identityLink.getUserId().equals(userId) && IdentityLinkType.PARTICIPANT.equals(identityLink.getType())) {
                        return;
                    }
                }

                IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstanceEntity, userId, null, IdentityLinkType.PARTICIPANT);
            }
        }
    }
}