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

package org.flowable.engine.impl.cfg;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.IdentityLinkUtil;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.InternalTaskAssignmentManager;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public class DefaultTaskAssignmentManager implements InternalTaskAssignmentManager {
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected final String identityLinkType;

    public DefaultTaskAssignmentManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this(processEngineConfiguration, IdentityLinkType.PARTICIPANT);
    }

    public DefaultTaskAssignmentManager(ProcessEngineConfigurationImpl processEngineConfiguration, String identityLinkType) {
        this.processEngineConfiguration = processEngineConfiguration;
        this.identityLinkType = identityLinkType;
    }

    @Override
    public void changeAssignee(Task task, String assignee) {
        if ((task.getAssignee() != null && !task.getAssignee().equals(assignee))
            || (task.getAssignee() == null && assignee != null)) {

            CommandContextUtil.getTaskService().changeTaskAssignee((TaskEntity) task, assignee);
            fireAssignmentEvents((TaskEntity) task);

            if (task.getId() != null) {
                CommandContextUtil.getInternalTaskAssignmentManager().addUserIdentityLinkToParent(task, task.getAssignee());
            }
        }
    }
    
    @Override
    public void changeOwner(Task task, String owner) {
        if ((task.getOwner() != null && !task.getOwner().equals(owner))
            || (task.getOwner() == null && owner != null)) {

            CommandContextUtil.getTaskService().changeTaskOwner((TaskEntity) task, owner);

            if (task.getId() != null) {
                addUserIdentityLinkToParent(task, task.getOwner());
            }
        }
    }

    @Override
    public void addCandidateUser(Task task, IdentityLink identityLink) {
        handleTaskIdentityLinkAddition((TaskEntity) task, (IdentityLinkEntity) identityLink, identityLinkType);
    }

    @Override
    public void addCandidateUsers(Task task, List<IdentityLink> candidateUsers) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        for (IdentityLink identityLink : candidateUsers) {
            identityLinks.add((IdentityLinkEntity) identityLink);
        }
        handleTaskIdentityLinkAdditions((TaskEntity) task, identityLinks, identityLinkType);
    }

    @Override
    public void addCandidateGroup(Task task, IdentityLink identityLink) {
        handleTaskIdentityLinkAddition((TaskEntity) task, (IdentityLinkEntity) identityLink, identityLinkType);
    }

    @Override
    public void addCandidateGroups(Task task, List<IdentityLink> candidateGroups) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        for (IdentityLink identityLink : candidateGroups) {
            identityLinks.add((IdentityLinkEntity) identityLink);
        }
        handleTaskIdentityLinkAdditions((TaskEntity) task, identityLinks, identityLinkType);
    }

    @Override
    public void addUserIdentityLink(Task task, IdentityLink identityLink) {
        handleTaskIdentityLinkAddition((TaskEntity) task, (IdentityLinkEntity) identityLink, identityLinkType);
    }

    @Override
    public void addGroupIdentityLink(Task task, IdentityLink identityLink) {
        handleTaskIdentityLinkAddition((TaskEntity) task, (IdentityLinkEntity) identityLink, identityLinkType);
    }

    @Override
    public void deleteUserIdentityLink(Task task, IdentityLink identityLink) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        identityLinks.add((IdentityLinkEntity) identityLink);
        IdentityLinkUtil.handleTaskIdentityLinkDeletions((TaskEntity) task, identityLinks, true, true);
    }

    @Override
    public void deleteGroupIdentityLink(Task task, IdentityLink identityLink) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        identityLinks.add((IdentityLinkEntity) identityLink);
        IdentityLinkUtil.handleTaskIdentityLinkDeletions((TaskEntity) task, identityLinks, true, true);
    }

    @Override
    public void addUserIdentityLinkToParent(Task task, String userId) {
        if (userId != null && task.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceEntity = CommandContextUtil.getExecutionEntityManager().findById(task.getProcessInstanceId());
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstanceEntity,
                userId, null, identityLinkType);
        }
    }

    protected void fireAssignmentEvents(TaskEntity taskEntity) {
        CommandContextUtil.getProcessEngineConfiguration().getListenerNotificationHelper().executeTaskListeners(taskEntity, TaskListener.EVENTNAME_ASSIGNMENT);

        if (CommandContextUtil.getEventDispatcher().isEnabled()) {
            CommandContextUtil.getEventDispatcher().dispatchEvent(
                FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity));
        }
    }

    protected void handleTaskIdentityLinkAddition(TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity, String parentIdentityLinkType) {
        CommandContextUtil.getHistoryManager().recordIdentityLinkCreated(identityLinkEntity);

        if (CountingEntityUtil.isTaskRelatedEntityCountEnabledGlobally()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskEntity;
            if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(countingTaskEntity)) {
                countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() + 1);
            }
        }

        taskEntity.getIdentityLinks().add(identityLinkEntity);
        if (identityLinkEntity.getUserId() != null && taskEntity.getProcessInstanceId() != null) {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getProcessInstanceId());
            for (IdentityLinkEntity identityLink : executionEntity.getIdentityLinks()) {
                if (identityLink.isUser() && identityLink.getUserId().equals(identityLinkEntity.getUserId())) {
                    return;
                }
            }

            IdentityLinkUtil.createProcessInstanceIdentityLink(executionEntity, identityLinkEntity.getUserId(), null, parentIdentityLinkType);
        }
    }

    protected void handleTaskIdentityLinkAdditions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinkEntities, String parentIdentityLinkType) {
        for (IdentityLinkEntity identityLinkEntity : identityLinkEntities) {
            handleTaskIdentityLinkAddition(taskEntity, identityLinkEntity, parentIdentityLinkType);
        }
    }

}
