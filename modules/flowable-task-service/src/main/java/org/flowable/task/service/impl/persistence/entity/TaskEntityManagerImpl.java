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

package org.flowable.task.service.impl.persistence.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.event.impl.FlowableTaskEventBuilder;
import org.flowable.task.service.impl.BaseHistoricTaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TaskEntityManagerImpl extends AbstractTaskServiceEntityManager<TaskEntity, TaskDataManager> implements TaskEntityManager {
    
    public TaskEntityManagerImpl(TaskServiceConfiguration taskServiceConfiguration, TaskDataManager taskDataManager) {
        super(taskServiceConfiguration, taskDataManager);
    }

    @Override
    public TaskEntity create() {
        TaskEntity taskEntity = super.create();
        taskEntity.setCreateTime(getClock().getCurrentTime());
        if (serviceConfiguration.isEnableTaskRelationshipCounts()) {
            ((CountingTaskEntity) taskEntity).setCountEnabled(true);
        }
        return taskEntity;
    }

    @Override
    public TaskEntity createTask(TaskBuilder taskBuilder) {
        // create and insert task
        TaskEntity taskEntity = create();
        taskEntity.setId(taskBuilder.getId());
        taskEntity.setName(taskBuilder.getName());
        taskEntity.setDescription(taskBuilder.getDescription());
        taskEntity.setPriority(taskBuilder.getPriority());
        taskEntity.setOwner(taskBuilder.getOwner());
        taskEntity.setAssignee(taskBuilder.getAssignee());
        taskEntity.setDueDate(taskBuilder.getDueDate());
        taskEntity.setCategory(taskBuilder.getCategory());
        taskEntity.setParentTaskId(taskBuilder.getParentTaskId());
        taskEntity.setTenantId(taskBuilder.getTenantId());
        taskEntity.setFormKey(taskBuilder.getFormKey());
        taskEntity.setTaskDefinitionId(taskBuilder.getTaskDefinitionId());
        taskEntity.setTaskDefinitionKey(taskBuilder.getTaskDefinitionKey());
        taskEntity.setScopeId(taskBuilder.getScopeId());
        taskEntity.setScopeType(taskBuilder.getScopeType());
        insert(taskEntity);

        TaskEntity enrichedTaskEntity = serviceConfiguration.getTaskPostProcessor().enrich(taskEntity);
        update(enrichedTaskEntity, false);
        taskBuilder.getIdentityLinks().forEach(
                identityLink -> {
                    if (identityLink.getGroupId() != null) {
                        enrichedTaskEntity.addGroupIdentityLink(identityLink.getGroupId(), identityLink.getType());
                    } else if (identityLink.getUserId() != null) {
                        enrichedTaskEntity.addUserIdentityLink(identityLink.getUserId(), identityLink.getType());
                    }
                }
        );

        if (getEventDispatcher() != null && getEventDispatcher().isEnabled() && taskEntity.getAssignee() != null) {
            getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity),
                    serviceConfiguration.getEngineName());
        }

        serviceConfiguration.getInternalHistoryTaskManager().recordTaskCreated(taskEntity);

        return enrichedTaskEntity;
    }

    @Override
    public void insert(TaskEntity taskEntity, boolean fireCreatedEvent) {
        super.insert(taskEntity, fireCreatedEvent);
        if (fireCreatedEvent) {
            logTaskCreatedEvent(taskEntity);
        }
    }

    @Override
    public TaskEntity update(TaskEntity taskEntity, boolean fireUpdateEvents) {
        if (fireUpdateEvents) {
            logTaskUpdateEvents(taskEntity);
        }
        return super.update(taskEntity, fireUpdateEvents);
    }

    @Override
    public void changeTaskAssignee(TaskEntity taskEntity, String assignee) {
        if ((taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee))
                || (taskEntity.getAssignee() == null && assignee != null)) {

            taskEntity.setAssignee(assignee);
            
            if (taskEntity.getId() != null) {
                serviceConfiguration.getInternalHistoryTaskManager().recordTaskInfoChange(taskEntity, getClock().getCurrentTime());
                update(taskEntity);
            }
        }
    }

    @Override
    public void changeTaskOwner(TaskEntity taskEntity, String owner) {
        if ((taskEntity.getOwner() != null && !taskEntity.getOwner().equals(owner))
                || (taskEntity.getOwner() == null && owner != null)) {
            
            taskEntity.setOwner(owner);

            if (taskEntity.getId() != null) {
                serviceConfiguration.getInternalHistoryTaskManager().recordTaskInfoChange(taskEntity, getClock().getCurrentTime());
                update(taskEntity);
            }
        }
    }

    @Override
    public List<TaskEntity> findTasksByExecutionId(String executionId) {
        return dataManager.findTasksByExecutionId(executionId);
    }

    @Override
    public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
        return dataManager.findTasksByProcessInstanceId(processInstanceId);
    }
    
    @Override
    public List<TaskEntity> findTasksByScopeIdAndScopeType(String scopeId, String scopeType) {
        return dataManager.findTasksByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public List<TaskEntity> findTasksBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        return dataManager.findTasksBySubScopeIdAndScopeType(subScopeId, scopeType);
    }

    @Override
    public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery) {
        return dataManager.findTasksByQueryCriteria(taskQuery);
    }

    @Override
    public List<Task> findTasksWithRelatedEntitiesByQueryCriteria(TaskQueryImpl taskQuery) {
        return dataManager.findTasksWithRelatedEntitiesByQueryCriteria(taskQuery);
    }

    @Override
    public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
        return dataManager.findTaskCountByQueryCriteria(taskQuery);
    }

    @Override
    public List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findTasksByNativeQuery(parameterMap);
    }

    @Override
    public long findTaskCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findTaskCountByNativeQuery(parameterMap);
    }

    @Override
    public List<Task> findTasksByParentTaskId(String parentTaskId) {
        return dataManager.findTasksByParentTaskId(parentTaskId);
    }

    @Override
    public void updateTaskTenantIdForDeployment(String deploymentId, String newTenantId) {
        dataManager.updateTaskTenantIdForDeployment(deploymentId, newTenantId);
    }
    
    @Override
    public void updateAllTaskRelatedEntityCountFlags(boolean configProperty) {
        dataManager.updateAllTaskRelatedEntityCountFlags(configProperty);
    }
    
    @Override
    public void deleteTasksByExecutionId(String executionId) {
        dataManager.deleteTasksByExecutionId(executionId);
    }

    protected void logAssigneeChanged(TaskEntity taskEntity, String previousAssignee, String newAssignee) {
        if (serviceConfiguration.isEnableHistoricTaskLogging()) {
            ObjectNode dataNode = serviceConfiguration.getObjectMapper().createObjectNode();
            dataNode.put("newAssigneeId", newAssignee);
            dataNode.put("previousAssigneeId", previousAssignee);
            recordHistoryUserTaskLog(HistoricTaskLogEntryType.USER_TASK_ASSIGNEE_CHANGED, taskEntity, dataNode);
        }
    }

    protected void logOwnerChanged(TaskEntity taskEntity, String previousOwner, String newOwner) {
        if (serviceConfiguration.isEnableHistoricTaskLogging()) {
            ObjectNode dataNode = serviceConfiguration.getObjectMapper().createObjectNode();
            dataNode.put("newOwnerId", newOwner);
            dataNode.put("previousOwnerId", previousOwner);
            recordHistoryUserTaskLog(HistoricTaskLogEntryType.USER_TASK_OWNER_CHANGED, taskEntity, dataNode);
        }
    }

    protected void logPriorityChanged(TaskEntity taskEntity, Integer previousPriority, int newPriority) {
        if (serviceConfiguration.isEnableHistoricTaskLogging()) {
            ObjectNode dataNode = serviceConfiguration.getObjectMapper().createObjectNode();
            dataNode.put("newPriority", newPriority);
            dataNode.put("previousPriority", previousPriority);
            recordHistoryUserTaskLog(HistoricTaskLogEntryType.USER_TASK_PRIORITY_CHANGED, taskEntity, dataNode);
        }
    }

    protected void logDueDateChanged(TaskEntity taskEntity, Date previousDueDate, Date newDueDate) {
        if (serviceConfiguration.isEnableHistoricTaskLogging()) {
            ObjectNode dataNode = serviceConfiguration.getObjectMapper().createObjectNode();
            dataNode.put("newDueDate", newDueDate != null ? newDueDate.getTime() : null);
            dataNode.put("previousDueDate", previousDueDate != null ? previousDueDate.getTime() : null);
            recordHistoryUserTaskLog(HistoricTaskLogEntryType.USER_TASK_DUEDATE_CHANGED, taskEntity, dataNode);
        }
    }

    protected void logNameChanged(TaskEntity taskEntity, String previousName, String newName) {
        if (serviceConfiguration.isEnableHistoricTaskLogging()) {
            ObjectNode dataNode = serviceConfiguration.getObjectMapper().createObjectNode();
            dataNode.put("newName", newName);
            dataNode.put("previousName", previousName);
            recordHistoryUserTaskLog(HistoricTaskLogEntryType.USER_TASK_NAME_CHANGED, taskEntity, dataNode);
        }
    }

    protected void logTaskCreatedEvent(TaskInfo task) {
        if (serviceConfiguration.isEnableHistoricTaskLogging()) {
            HistoricTaskLogEntryBuilder taskLogEntryBuilder = createHistoricTaskLogEntryBuilder(task, HistoricTaskLogEntryType.USER_TASK_CREATED);
            taskLogEntryBuilder.timeStamp(task.getCreateTime());
            serviceConfiguration.getInternalHistoryTaskManager().recordHistoryUserTaskLog(taskLogEntryBuilder);
        }
    }

    protected HistoricTaskLogEntryBuilder createHistoricTaskLogEntryBuilder(TaskInfo task, HistoricTaskLogEntryType userTaskCreated) {
        HistoricTaskLogEntryBuilder taskLogEntryBuilder = new BaseHistoricTaskLogEntryBuilderImpl(task);
        taskLogEntryBuilder.timeStamp(serviceConfiguration.getClock().getCurrentTime());
        taskLogEntryBuilder.userId(Authentication.getAuthenticatedUserId());
        taskLogEntryBuilder.type(userTaskCreated.name());
        return taskLogEntryBuilder;
    }

    protected void logTaskUpdateEvents(TaskEntity task) {
        if (wasPersisted(task)) {
            if (!Objects.equals(task.getAssignee(), getOriginalState(task, "assignee"))) {
                logAssigneeChanged(task, (String) getOriginalState(task, "assignee"), task.getAssignee());
            }
            if (!Objects.equals(task.getOwner(), getOriginalState(task, "owner"))) {
                if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                    getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_OWNER_CHANGED, task),
                            serviceConfiguration.getEngineName());
                }

                logOwnerChanged(task, (String) getOriginalState(task, "owner"), task.getOwner());
            }
            if (!Objects.equals(task.getPriority(), getOriginalState(task, "priority"))) {
                if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                    getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_PRIORITY_CHANGED, task),
                            serviceConfiguration.getEngineName());
                }
                logPriorityChanged(task, (Integer) getOriginalState(task, "priority"), task.getPriority());
            }
            if (!Objects.equals(task.getDueDate(), getOriginalState(task, "dueDate"))) {
                if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                    getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_DUEDATE_CHANGED, task),
                            serviceConfiguration.getEngineName());
                }
                logDueDateChanged(task, (Date) getOriginalState(task, "dueDate"), task.getDueDate());
            }
            if (!Objects.equals(task.getName(), getOriginalState(task, "name"))) {
                if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                    getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_NAME_CHANGED, task),
                            serviceConfiguration.getEngineName());
                }
                logNameChanged(task, (String) getOriginalState(task, "name"), task.getName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected boolean wasPersisted(TaskEntity task) {
        if (task.getOriginalPersistentState() != null && ((Map<String, Object>) task.getOriginalPersistentState()).size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    protected Object getOriginalState(TaskEntity task, String stateKey) {
        if (task.getOriginalPersistentState() != null) {
            return ((Map<String, Object>) task.getOriginalPersistentState()).get(stateKey);
        }
        return null;
    }

    protected void recordHistoryUserTaskLog(HistoricTaskLogEntryType logEntryType, TaskInfo task, ObjectNode dataNode) {
        HistoricTaskLogEntryBuilder taskLogEntryBuilder = createHistoricTaskLogEntryBuilder(task, logEntryType);
        taskLogEntryBuilder.data(dataNode.toString());
        serviceConfiguration.getInternalHistoryTaskManager().recordHistoryUserTaskLog(taskLogEntryBuilder);
    }

}
