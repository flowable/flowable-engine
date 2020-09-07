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

package org.flowable.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.data.CommentDataManager;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.Event;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class CommentEntityManagerImpl
    extends AbstractProcessEngineEntityManager<CommentEntity, CommentDataManager>
    implements CommentEntityManager {

    public CommentEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, CommentDataManager commentDataManager) {
        super(processEngineConfiguration, commentDataManager);
    }

    @Override
    public void insert(CommentEntity commentEntity) {
        checkHistoryEnabled();

        insert(commentEntity, false);

        Comment comment = (Comment) commentEntity;
        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            // Forced to fetch the process-instance to associate the right
            // process definition
            String processDefinitionId = null;
            String processInstanceId = comment.getProcessInstanceId();
            if (comment.getProcessInstanceId() != null) {
                ExecutionEntity process = getExecutionEntityManager().findById(comment.getProcessInstanceId());
                if (process != null) {
                    processDefinitionId = process.getProcessDefinitionId();
                }
            }
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, 
                    commentEntity, processInstanceId, processInstanceId, processDefinitionId),
                    engineConfiguration.getEngineCfgKey());
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, 
                    commentEntity, processInstanceId, processInstanceId, processDefinitionId),
                    engineConfiguration.getEngineCfgKey());
        }
    }
    
    @Override
    public CommentEntity update(CommentEntity commentEntity) {
        checkHistoryEnabled();

        CommentEntity updatedCommentEntity = update(commentEntity, false);

        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            // Forced to fetch the process-instance to associate the right
            // process definition
            String processDefinitionId = null;
            String processInstanceId = updatedCommentEntity.getProcessInstanceId();
            if (updatedCommentEntity.getProcessInstanceId() != null) {
                ExecutionEntity process = getExecutionEntityManager().findById(updatedCommentEntity.getProcessInstanceId());
                if (process != null) {
                    processDefinitionId = process.getProcessDefinitionId();
                }
            }
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, 
                    commentEntity, processInstanceId, processInstanceId, processDefinitionId),
                    engineConfiguration.getEngineCfgKey());
        }
        
        return updatedCommentEntity;
    }

    @Override
    public List<Comment> findCommentsByTaskId(String taskId) {
        checkHistoryEnabled();
        return dataManager.findCommentsByTaskId(taskId);
    }

    @Override
    public List<Comment> findCommentsByTaskIdAndType(String taskId, String type) {
        checkHistoryEnabled();
        return dataManager.findCommentsByTaskIdAndType(taskId, type);
    }

    @Override
    public List<Comment> findCommentsByType(String type) {
        checkHistoryEnabled();
        return dataManager.findCommentsByType(type);
    }

    @Override
    public List<Event> findEventsByTaskId(String taskId) {
        checkHistoryEnabled();
        return dataManager.findEventsByTaskId(taskId);
    }

    @Override
    public List<Event> findEventsByProcessInstanceId(String processInstanceId) {
        checkHistoryEnabled();
        return dataManager.findEventsByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteCommentsByTaskId(String taskId) {
        checkHistoryEnabled();
        dataManager.deleteCommentsByTaskId(taskId);
    }

    @Override
    public void deleteCommentsByProcessInstanceId(String processInstanceId) {
        checkHistoryEnabled();
        dataManager.deleteCommentsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<Comment> findCommentsByProcessInstanceId(String processInstanceId) {
        checkHistoryEnabled();
        return dataManager.findCommentsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<Comment> findCommentsByProcessInstanceId(String processInstanceId, String type) {
        checkHistoryEnabled();
        return dataManager.findCommentsByProcessInstanceId(processInstanceId, type);
    }

    @Override
    public Comment findComment(String commentId) {
        return dataManager.findComment(commentId);
    }

    @Override
    public Event findEvent(String commentId) {
        return dataManager.findEvent(commentId);
    }

    @Override
    public void delete(CommentEntity commentEntity) {
        checkHistoryEnabled();

        delete(commentEntity, false);

        Comment comment = (Comment) commentEntity;
        if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
            // Forced to fetch the process-instance to associate the right
            // process definition
            String processDefinitionId = null;
            String processInstanceId = comment.getProcessInstanceId();
            if (comment.getProcessInstanceId() != null) {
                ExecutionEntity process = getExecutionEntityManager().findById(comment.getProcessInstanceId());
                if (process != null) {
                    processDefinitionId = process.getProcessDefinitionId();
                }
            }
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, 
                    commentEntity, processInstanceId, processInstanceId, processDefinitionId),
                    engineConfiguration.getEngineCfgKey());
        }
    }

    protected void checkHistoryEnabled() {
        if (!getHistoryManager().isHistoryEnabled()) {
            throw new FlowableException("In order to use comments, history should be enabled");
        }
    }

    protected ExecutionEntityManager getExecutionEntityManager() {
        return engineConfiguration.getExecutionEntityManager();
    }

    protected HistoryManager getHistoryManager() {
        return engineConfiguration.getHistoryManager();
    }

}
