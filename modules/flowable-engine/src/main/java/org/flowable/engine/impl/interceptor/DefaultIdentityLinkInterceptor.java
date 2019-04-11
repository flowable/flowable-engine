package org.flowable.engine.impl.interceptor;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.IdentityLinkUtil;
import org.flowable.engine.interceptor.IdentityLinkInterceptor;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class DefaultIdentityLinkInterceptor implements IdentityLinkInterceptor {

    @Override
    public void handleCompleteTask(TaskEntity task) {
        if (Authentication.getAuthenticatedUserId() != null && task.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceEntity = CommandContextUtil.getExecutionEntityManager().findById(task.getProcessInstanceId());
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstanceEntity,
                    Authentication.getAuthenticatedUserId(), null, IdentityLinkType.PARTICIPANT);
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
    public void handleCreateProcessInstance(ExecutionEntity processInstanceExecution) {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        if (authenticatedUserId != null) {
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstanceExecution, authenticatedUserId, null, IdentityLinkType.STARTER);
        }
    }
    
    @Override
    public void handleCreateSubProcessInstance(ExecutionEntity subProcessInstanceExecution, ExecutionEntity superExecution) {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        if (authenticatedUserId != null) {
            IdentityLinkUtil.createProcessInstanceIdentityLink(subProcessInstanceExecution, authenticatedUserId, null, IdentityLinkType.STARTER);
        }
    }
    
    protected void addUserIdentityLinkToParent(Task task, String userId) {
        if (userId != null && task.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceEntity = CommandContextUtil.getExecutionEntityManager().findById(task.getProcessInstanceId());
            for (IdentityLinkEntity identityLink : processInstanceEntity.getIdentityLinks()) {
                if (identityLink.isUser() && identityLink.getUserId().equals(userId) && IdentityLinkType.PARTICIPANT.equals(identityLink.getType())) {
                    return;
                }
            }
            
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstanceEntity, userId, null, IdentityLinkType.PARTICIPANT);
        }
    }
}