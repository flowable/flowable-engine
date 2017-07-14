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
import org.flowable.engine.common.impl.cfg.IdGenerator;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.identity.Authentication;
import org.flowable.engine.impl.persistence.AbstractManager;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.task.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHistoryManager extends AbstractManager implements HistoryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHistoryManager.class.getName());

    protected HistoryLevel historyLevel;

    public AbstractHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration, HistoryLevel historyLevel) {
        super(processEngineConfiguration);
        this.historyLevel = historyLevel;
    }

    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current history level: {}, level required: {}", historyLevel, level);
        }
        // Comparing enums actually compares the location of values declared in the enum
        return historyLevel.isAtLeast(level);
    }

    @Override
    public boolean isHistoryEnabled() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current history level: {}", historyLevel);
        }
        return historyLevel != HistoryLevel.NONE;
    }

    @Override
    public void createIdentityLinkComment(String taskId, String userId, String groupId, String type, boolean create) {
        createIdentityLinkComment(taskId, userId, groupId, type, create, false);
    }

    @Override
    public void createUserIdentityLinkComment(String taskId, String userId, String type, boolean create) {
        createIdentityLinkComment(taskId, userId, null, type, create, false);
    }

    @Override
    public void createGroupIdentityLinkComment(String taskId, String groupId, String type, boolean create) {
        createIdentityLinkComment(taskId, null, groupId, type, create, false);
    }

    @Override
    public void createUserIdentityLinkComment(String taskId, String userId, String type, boolean create, boolean forceNullUserId) {
        createIdentityLinkComment(taskId, userId, null, type, create, forceNullUserId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.flowable.engine.impl.history.HistoryManagerInterface# createIdentityLinkComment(java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, boolean)
     */
    @Override
    public void createIdentityLinkComment(String taskId, String userId, String groupId, String type, boolean create, boolean forceNullUserId) {
        if (isHistoryEnabled()) {
            String authenticatedUserId = Authentication.getAuthenticatedUserId();
            CommentEntity comment = getCommentEntityManager().create();
            comment.setUserId(authenticatedUserId);
            comment.setType(CommentEntity.TYPE_EVENT);
            comment.setTime(getClock().getCurrentTime());
            comment.setTaskId(taskId);
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
    public void createProcessInstanceIdentityLinkComment(String processInstanceId, String userId, String groupId, String type, boolean create) {
        createProcessInstanceIdentityLinkComment(processInstanceId, userId, groupId, type, create, false);
    }

    @Override
    public void createProcessInstanceIdentityLinkComment(String processInstanceId, String userId, String groupId, String type, boolean create, boolean forceNullUserId) {
        if (isHistoryEnabled()) {
            String authenticatedUserId = Authentication.getAuthenticatedUserId();
            CommentEntity comment = getCommentEntityManager().create();
            comment.setUserId(authenticatedUserId);
            comment.setType(CommentEntity.TYPE_EVENT);
            comment.setTime(getClock().getCurrentTime());
            comment.setProcessInstanceId(processInstanceId);
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
    public void createAttachmentComment(String taskId, String processInstanceId, String attachmentName, boolean create) {
        if (isHistoryEnabled()) {
            String userId = Authentication.getAuthenticatedUserId();
            CommentEntity comment = getCommentEntityManager().create();
            comment.setUserId(userId);
            comment.setType(CommentEntity.TYPE_EVENT);
            comment.setTime(getClock().getCurrentTime());
            comment.setTaskId(taskId);
            comment.setProcessInstanceId(processInstanceId);
            if (create) {
                comment.setAction(Event.ACTION_ADD_ATTACHMENT);
            } else {
                comment.setAction(Event.ACTION_DELETE_ATTACHMENT);
            }
            comment.setMessage(attachmentName);
            getCommentEntityManager().insert(comment);
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
    public HistoricActivityInstanceEntity findActivityInstance(ExecutionEntity execution, boolean createOnNotFound, boolean endTimeMustBeNull) {
        String activityId = getActivityIdForExecution(execution);
        return activityId != null ? findActivityInstance(execution, activityId, createOnNotFound, endTimeMustBeNull) : null;
    }

    protected String getActivityIdForExecution(ExecutionEntity execution) {
        String activityId = null;
        if (execution.getCurrentFlowElement() instanceof FlowNode) {
            activityId = execution.getCurrentFlowElement().getId();
        } else if (execution.getCurrentFlowElement() instanceof SequenceFlow
                        && execution.getCurrentFlowableListener() == null) { // while executing sequence flow listeners, we don't want historic activities
            activityId = ((SequenceFlow) (execution.getCurrentFlowElement())).getSourceFlowElement().getId();
        }
        return activityId;
    }

    protected HistoricActivityInstanceEntity findActivityInstance(ExecutionEntity execution, String activityId, boolean createOnNotFound, boolean endTimeMustBeNull) {

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

            if (historicActivityInstances.size() > 0) {
                return historicActivityInstances.get(0);
            }

        }

        if (createOnNotFound
                        && ((execution.getCurrentFlowElement() != null && execution.getCurrentFlowElement() instanceof FlowNode) || execution.getCurrentFlowElement() == null)) {
            return createHistoricActivityInstanceEntity(execution);
        }

        return null;
    }

    protected HistoricActivityInstanceEntity createHistoricActivityInstanceEntity(ExecutionEntity execution) {
        IdGenerator idGenerator = getProcessEngineConfiguration().getIdGenerator();

        String processDefinitionId = execution.getProcessDefinitionId();
        String processInstanceId = execution.getProcessInstanceId();

        HistoricActivityInstanceEntity historicActivityInstance = getHistoricActivityInstanceEntityManager().create();
        historicActivityInstance.setId(idGenerator.getNextId());
        historicActivityInstance.setProcessDefinitionId(processDefinitionId);
        historicActivityInstance.setProcessInstanceId(processInstanceId);
        historicActivityInstance.setExecutionId(execution.getId());
        historicActivityInstance.setActivityId(execution.getActivityId());
        if (execution.getCurrentFlowElement() != null) {
            historicActivityInstance.setActivityName(execution.getCurrentFlowElement().getName());
            historicActivityInstance.setActivityType(parseActivityType(execution.getCurrentFlowElement()));
        }
        Date now = getClock().getCurrentTime();
        historicActivityInstance.setStartTime(now);

        if (execution.getTenantId() != null) {
            historicActivityInstance.setTenantId(execution.getTenantId());
        }

        getHistoricActivityInstanceEntityManager().insert(historicActivityInstance);
        return historicActivityInstance;
    }

    protected String parseActivityType(FlowElement element) {
        String elementType = element.getClass().getSimpleName();
        elementType = elementType.substring(0, 1).toLowerCase() + elementType.substring(1);
        return elementType;
    }

    protected EntityCache getEntityCache() {
        return getSession(EntityCache.class);
    }

    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }

    public void setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
    }

}
