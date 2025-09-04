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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class CompositeHistoryManager implements HistoryManager {

    protected final Collection<HistoryManager> historyManagers;

    public CompositeHistoryManager(Collection<HistoryManager> historyManagers) {
        this.historyManagers = new ArrayList<>(historyManagers);
    }

    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        for (HistoryManager historyManager : historyManagers) {
            if (historyManager.isHistoryLevelAtLeast(level)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level, String processDefinitionId) {
        for (HistoryManager historyManager : historyManagers) {
            if (historyManager.isHistoryLevelAtLeast(level, processDefinitionId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isHistoryEnabled() {
        for (HistoryManager historyManager : historyManagers) {
            if (historyManager.isHistoryEnabled()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isHistoryEnabled(String processDefinitionId) {
        for (HistoryManager historyManager : historyManagers) {
            if (historyManager.isHistoryEnabled(processDefinitionId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void recordProcessInstanceEnd(ExecutionEntity processInstance, String state, String deleteReason, String activityId, Date endTime) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordProcessInstanceEnd(processInstance, state, deleteReason, activityId, endTime);
        }
    }

    @Override
    public void recordProcessInstanceStart(ExecutionEntity processInstance) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordProcessInstanceStart(processInstance);
        }
    }

    @Override
    public void recordProcessInstanceNameChange(ExecutionEntity processInstanceExecution, String newName) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordProcessInstanceNameChange(processInstanceExecution, newName);
        }
    }

    @Override
    public void recordProcessInstanceDeleted(String processInstanceId, String processDefinitionId, String processTenantId) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordProcessInstanceDeleted(processInstanceId, processDefinitionId, processTenantId);
        }
    }

    @Override
    public void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordDeleteHistoricProcessInstancesByProcessDefinitionId(processDefinitionId);
        }
    }

    @Override
    public void recordBulkDeleteProcessInstances(Collection<String> processInstanceIds) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordBulkDeleteProcessInstances(processInstanceIds);
        }
    }

    @Override
    public void recordActivityStart(ActivityInstance activityInstance) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordActivityStart(activityInstance);
        }
    }

    @Override
    public void recordActivityEnd(ActivityInstance activityInstance) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordActivityEnd(activityInstance);
        }
    }

    @Override
    public HistoricActivityInstanceEntity findHistoricActivityInstance(ExecutionEntity execution, boolean validateEndTimeNull) {
        for (HistoryManager historyManager : historyManagers) {
            HistoricActivityInstanceEntity historicActivityInstance = historyManager.findHistoricActivityInstance(execution, validateEndTimeNull);
            if (historicActivityInstance != null) {
                return historicActivityInstance;
            }
        }

        return null;
    }

    @Override
    public void recordProcessDefinitionChange(String processInstanceId, String processDefinitionId) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordProcessDefinitionChange(processInstanceId, processDefinitionId);
        }
    }

    @Override
    public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordTaskCreated(task, execution);
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, ExecutionEntity execution, String userId, String deleteReason, Date endTime) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordTaskEnd(task, execution, userId, deleteReason, endTime);
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity, String activityInstanceId, Date changeTime) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordTaskInfoChange(taskEntity, activityInstanceId, changeTime);
        }
    }

    @Override
    public void recordHistoricTaskDeleted(HistoricTaskInstance task){
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordHistoricTaskDeleted(task);
        }
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable, Date createTime) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordVariableCreate(variable, createTime);
        }
    }

    @Override
    public void recordHistoricDetailVariableCreate(VariableInstanceEntity variable, ExecutionEntity sourceActivityExecution, boolean useActivityId,
        String activityInstanceId, Date createTime) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordHistoricDetailVariableCreate(variable, sourceActivityExecution, useActivityId, activityInstanceId, createTime);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable, Date updateTime) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordVariableUpdate(variable, updateTime);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordVariableRemoved(variable);
        }
    }

    @Override
    public void createIdentityLinkComment(TaskEntity task, String userId, String groupId, String type, boolean create) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createIdentityLinkComment(task, userId, groupId, type, create);
        }
    }

    @Override
    public void createUserIdentityLinkComment(TaskEntity task, String userId, String type, boolean create) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createUserIdentityLinkComment(task, userId, type, create);
        }
    }

    @Override
    public void createGroupIdentityLinkComment(TaskEntity task, String groupId, String type, boolean create) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createGroupIdentityLinkComment(task, groupId, type, create);
        }
    }

    @Override
    public void createIdentityLinkComment(TaskEntity task, String userId, String groupId, String type, boolean create, boolean forceNullUserId) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createIdentityLinkComment(task, userId, groupId, type, create, forceNullUserId);
        }
    }

    @Override
    public void createUserIdentityLinkComment(TaskEntity task, String userId, String type, boolean create, boolean forceNullUserId) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createUserIdentityLinkComment(task, userId, type, create, forceNullUserId);
        }
    }

    @Override
    public void createProcessInstanceIdentityLinkComment(ExecutionEntity processInstance, String userId, String groupId, String type, boolean create) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createProcessInstanceIdentityLinkComment(processInstance, userId, groupId, type, create);
        }
    }

    @Override
    public void createProcessInstanceIdentityLinkComment(ExecutionEntity processInstance, String userId, String groupId, String type, boolean create,
        boolean forceNullUserId) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createProcessInstanceIdentityLinkComment(processInstance, userId, groupId, type, create, forceNullUserId);
        }
    }

    @Override
    public void createAttachmentComment(TaskEntity task, ExecutionEntity processInstance, String attachmentName, boolean create) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createAttachmentComment(task, processInstance, attachmentName, create);
        }
    }

    @Override
    public void recordFormPropertiesSubmitted(ExecutionEntity processInstance, Map<String, String> properties, String taskId, Date createTime) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordFormPropertiesSubmitted(processInstance, properties, taskId, createTime);
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordIdentityLinkCreated(identityLink);
        }
    }

    @Override
    public void recordIdentityLinkCreated(ExecutionEntity processInstance, IdentityLinkEntity identityLink) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordIdentityLinkCreated(processInstance,identityLink);
        }
    }

    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordIdentityLinkDeleted(identityLink);
        }
    }

    @Override
    public void recordEntityLinkCreated(EntityLinkEntity entityLink) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordEntityLinkCreated(entityLink);
        }
    }

    @Override
    public void recordEntityLinkDeleted(EntityLinkEntity entityLink) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordEntityLinkDeleted(entityLink);
        }
    }

    @Override
    public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.updateProcessBusinessKeyInHistory(processInstance);
        }
    }

    @Override
    public void updateProcessBusinessStatusInHistory(ExecutionEntity processInstance) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.updateProcessBusinessStatusInHistory(processInstance);
        }
    }

    @Override
    public void updateProcessDefinitionIdInHistory(ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.updateProcessDefinitionIdInHistory(processDefinitionEntity, processInstance);
        }
    }

    @Override
    public void updateActivity(ExecutionEntity executionEntity, String oldActivityId, FlowElement newFlowElement, TaskEntity task, Date updateTime) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.updateActivity(executionEntity, oldActivityId, newFlowElement, task, updateTime);
        }
    }

    @Override
    public void updateHistoricActivityInstance(ActivityInstance activityInstance) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.updateHistoricActivityInstance(activityInstance);
        }
    }

    @Override
    public void createHistoricActivityInstance(ActivityInstance activityInstance) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.createHistoricActivityInstance(activityInstance);
        }
    }

    @Override
    public void recordHistoricUserTaskLogEntry(HistoricTaskLogEntryBuilder taskLogEntryBuilder) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.recordHistoricUserTaskLogEntry(taskLogEntryBuilder);
        }
    }

    @Override
    public void deleteHistoryUserTaskLog(long logNumber) {
        for (HistoryManager historyManager : historyManagers) {
            historyManager.deleteHistoryUserTaskLog(logNumber);
        }
    }

    public void addHistoryManager(HistoryManager historyManager) {
        historyManagers.add(historyManager);
    }
}
