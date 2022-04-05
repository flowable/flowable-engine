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

import java.util.Date;
import java.util.List;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.AbstractManager;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.task.Event;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public abstract class AbstractHistoryManager extends AbstractManager implements HistoryManager {

    public AbstractHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }
    
    protected HistoryConfigurationSettings getHistoryConfigurationSettings() {
        return processEngineConfiguration.getHistoryConfigurationSettings();
    }
    
    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        return isHistoryLevelAtLeast(level, null);
    }

    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level, String processDefinitionId) {
        return getHistoryConfigurationSettings().isHistoryLevelAtLeast(level, processDefinitionId);
    }

    @Override
    public boolean isHistoryEnabled() {
        return getHistoryConfigurationSettings().isHistoryEnabled();
    }
    
    @Override
    public boolean isHistoryEnabled(String processDefinitionId) {
        return getHistoryConfigurationSettings().isHistoryEnabled(processDefinitionId);
    }

    @Override
    public void createIdentityLinkComment(TaskEntity taskEntity, String userId, String groupId, String type, boolean create) {
        createIdentityLinkComment(taskEntity, userId, groupId, type, create, false);
    }

    @Override
    public void createUserIdentityLinkComment(TaskEntity taskEntity, String userId, String type, boolean create) {
        createIdentityLinkComment(taskEntity, userId, null, type, create, false);
    }

    @Override
    public void createGroupIdentityLinkComment(TaskEntity taskEntity, String groupId, String type, boolean create) {
        createIdentityLinkComment(taskEntity, null, groupId, type, create, false);
    }

    @Override
    public void createUserIdentityLinkComment(TaskEntity taskEntity, String userId, String type, boolean create, boolean forceNullUserId) {
        createIdentityLinkComment(taskEntity, userId, null, type, create, forceNullUserId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.flowable.engine.impl.history.HistoryManagerInterface# createIdentityLinkComment(java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, boolean)
     */
    @Override
    public void createIdentityLinkComment(TaskEntity taskEntity, String userId, String groupId, String type, boolean create, boolean forceNullUserId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
            String authenticatedUserId = Authentication.getAuthenticatedUserId();
            CommentEntity comment = getCommentEntityManager().create();
            comment.setUserId(authenticatedUserId);
            comment.setType(CommentEntity.TYPE_EVENT);
            comment.setTime(getClock().getCurrentTime());
            comment.setTaskId(taskEntity.getId());
            if (userId != null || forceNullUserId) {
                if (create && !forceNullUserId) {
                    comment.setAction(Event.ACTION_ADD_USER_LINK);
                } else {
                    comment.setAction(Event.ACTION_DELETE_USER_LINK);
                }
                comment.setMessage(new String[] { userId, type });
            } else {
                if (create) {
                    comment.setAction(Event.ACTION_ADD_GROUP_LINK);
                } else {
                    comment.setAction(Event.ACTION_DELETE_GROUP_LINK);
                }
                comment.setMessage(new String[] { groupId, type });
            }

            getCommentEntityManager().insert(comment);
        }
    }

    @Override
    public void createProcessInstanceIdentityLinkComment(ExecutionEntity processInstance, String userId, String groupId, String type, boolean create) {
        createProcessInstanceIdentityLinkComment(processInstance, userId, groupId, type, create, false);
    }

    @Override
    public void createProcessInstanceIdentityLinkComment(ExecutionEntity processInstance, String userId, String groupId, String type, boolean create, boolean forceNullUserId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processInstance.getProcessDefinitionId())) {
            String authenticatedUserId = Authentication.getAuthenticatedUserId();
            CommentEntity comment = getCommentEntityManager().create();
            comment.setUserId(authenticatedUserId);
            comment.setType(CommentEntity.TYPE_EVENT);
            comment.setTime(getClock().getCurrentTime());
            comment.setProcessInstanceId(processInstance.getId());
            if (userId != null || forceNullUserId) {
                if (create && !forceNullUserId) {
                    comment.setAction(Event.ACTION_ADD_USER_LINK);
                } else {
                    comment.setAction(Event.ACTION_DELETE_USER_LINK);
                }
                comment.setMessage(new String[] { userId, type });
            } else {
                if (create) {
                    comment.setAction(Event.ACTION_ADD_GROUP_LINK);
                } else {
                    comment.setAction(Event.ACTION_DELETE_GROUP_LINK);
                }
                comment.setMessage(new String[] { groupId, type });
            }
            getCommentEntityManager().insert(comment);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.flowable.engine.impl.history.HistoryManagerInterface# createAttachmentComment(java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void createAttachmentComment(TaskEntity task, ExecutionEntity processInstance, String attachmentName, boolean create) {
        String processDefinitionId = null;
        if (processInstance != null) {
            processDefinitionId = processInstance.getProcessDefinitionId();
        } else if (task != null) {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryEnabled(processDefinitionId)) {
            String userId = Authentication.getAuthenticatedUserId();
            CommentEntity comment = getCommentEntityManager().create();
            comment.setUserId(userId);
            comment.setType(CommentEntity.TYPE_EVENT);
            comment.setTime(getClock().getCurrentTime());
            if (task != null) {
                comment.setTaskId(task.getId());
            }
            if (processInstance != null) {
                comment.setProcessInstanceId(processInstance.getId());
            }
            if (create) {
                comment.setAction(Event.ACTION_ADD_ATTACHMENT);
            } else {
                comment.setAction(Event.ACTION_DELETE_ATTACHMENT);
            }
            comment.setMessage(attachmentName);
            getCommentEntityManager().insert(comment);
        }
    }

    @Override
    public void updateActivity(ExecutionEntity childExecution, String oldActivityId, FlowElement newFlowElement, TaskEntity task, Date updateTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager = CommandContextUtil.getHistoricActivityInstanceEntityManager();
            List<HistoricActivityInstanceEntity> historicActivityInstances = historicActivityInstanceEntityManager.findHistoricActivityInstancesByExecutionAndActivityId(childExecution.getId(), oldActivityId);
            for (HistoricActivityInstanceEntity historicActivityInstance : historicActivityInstances) {
                historicActivityInstance.setProcessDefinitionId(childExecution.getProcessDefinitionId());
                historicActivityInstance.setActivityId(childExecution.getActivityId());
                historicActivityInstance.setActivityName(newFlowElement.getName());
            }
        }

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            HistoricTaskService historicTaskService = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
            historicTaskService.recordTaskInfoChange(task, updateTime, processEngineConfiguration);
        }
    }

    protected HistoricActivityInstanceEntity getHistoricActivityInstanceFromCache(String executionId, String activityId, boolean endTimeMustBeNull) {
        List<HistoricActivityInstanceEntity> cachedHistoricActivityInstances = getEntityCache().findInCache(HistoricActivityInstanceEntity.class);
        for (HistoricActivityInstanceEntity cachedHistoricActivityInstance : cachedHistoricActivityInstances) {
            if (activityId != null
                            && activityId.equals(cachedHistoricActivityInstance.getActivityId())
                            && (!endTimeMustBeNull || cachedHistoricActivityInstance.getEndTime() == null)) {
                if (executionId.equals(cachedHistoricActivityInstance.getExecutionId())) {
                    return cachedHistoricActivityInstance;
                }
            }
        }

        return null;
    }

    @Override
    public HistoricActivityInstanceEntity findHistoricActivityInstance(ExecutionEntity execution, boolean endTimeMustBeNull) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, execution.getProcessDefinitionId())) {
            String activityId = getActivityIdForExecution(execution);
            return activityId != null ? findHistoricActivityInstance(execution, activityId, endTimeMustBeNull) : null;
        }
        
        return null;
    }

    protected String getActivityIdForExecution(ExecutionEntity execution) {
        String activityId = null;
        if (execution.getCurrentFlowElement() instanceof FlowNode) {
            activityId = execution.getCurrentFlowElement().getId();
        } else if (execution.getCurrentFlowElement() instanceof SequenceFlow
                        && execution.getCurrentFlowableListener() == null) { // while executing sequence flow listeners, we don't want historic activities
            activityId = ((SequenceFlow) execution.getCurrentFlowElement()).getSourceFlowElement().getId();
        }
        return activityId;
    }

    protected HistoricActivityInstanceEntity findHistoricActivityInstance(ExecutionEntity execution, String activityId, boolean endTimeMustBeNull) {

        // No use looking for the HistoricActivityInstance when no activityId is provided.
        if (activityId == null) {
            return null;
        }

        String executionId = execution.getId();

        // Check the cache
        HistoricActivityInstanceEntity historicActivityInstanceEntityFromCache = getHistoricActivityInstanceFromCache(executionId, activityId, endTimeMustBeNull);
        if (historicActivityInstanceEntityFromCache != null) {
            return historicActivityInstanceEntityFromCache;
        }

        // If the execution was freshly created, there is no need to check the database,
        // there can never be an entry for a historic activity instance with this execution id.
        if (!execution.isInserted() && !execution.isProcessInstanceType()) {

            // Check the database
            List<HistoricActivityInstanceEntity> historicActivityInstances = getHistoricActivityInstanceEntityManager()
                            .findUnfinishedHistoricActivityInstancesByExecutionAndActivityId(executionId, activityId);

            if (historicActivityInstances.size() > 0 && (!endTimeMustBeNull || historicActivityInstances.get(0).getEndTime() == null)) {
                return historicActivityInstances.get(0);
            }

        }

        return null;
    }

    protected String parseActivityType(FlowElement element) {
        String elementType = element.getClass().getSimpleName();
        elementType = elementType.substring(0, 1).toLowerCase() + elementType.substring(1);
        return elementType;
    }

    protected EntityCache getEntityCache() {
        return getSession(EntityCache.class);
    }

    protected String getProcessDefinitionId(VariableInstanceEntity variable, ExecutionEntity sourceActivityExecution) {
        String processDefinitionId = null;
        if (sourceActivityExecution != null) {
            processDefinitionId = sourceActivityExecution.getProcessDefinitionId();
        } else if (variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = processEngineConfiguration.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            if (processInstanceExecution != null) {
                processDefinitionId = processInstanceExecution.getProcessDefinitionId();
            }
        } else if (variable.getTaskId() != null) {
            TaskEntity taskEntity = processEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(variable.getTaskId());
            if (taskEntity != null) {
                processDefinitionId = taskEntity.getProcessDefinitionId();
            }
        }
        return processDefinitionId;
    }

}
