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

import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public interface HistoryManager {

    /**
     * @return true, if the configured history-level is equal to OR set to a higher value than the given level.
     */
    boolean isHistoryLevelAtLeast(HistoryLevel level);
    
    /**
     * @return true, if the configured process definition history-level is equal to OR set to a higher value than the given level.
     */
    boolean isHistoryLevelAtLeast(HistoryLevel level, String processDefinitionId);

    /**
     * @return true, if history-level is configured to level other than "none".
     */
    boolean isHistoryEnabled();
    
    /**
     * @return true, if process definition history-level is configured to level other than "none".
     */
    boolean isHistoryEnabled(String processDefinitionId);

    /**
     * Record a process-instance ended. Updates the historic process instance if activity history is enabled.
     */
    void recordProcessInstanceEnd(ExecutionEntity processInstance, String deleteReason, String activityId);

    /**
     * Record a process-instance started and record start-event if activity history is enabled.
     */
    void recordProcessInstanceStart(ExecutionEntity processInstance);

    /**
     * Record a process-instance name change.
     */
    void recordProcessInstanceNameChange(ExecutionEntity processInstanceExecution, String newName);

    /**
     * Record a sub-process-instance started and alters the calledProcessinstanceId on the current active activity's historic counterpart. Only effective when activity history is enabled.
     */
    void recordSubProcessInstanceStart(ExecutionEntity parentExecution, ExecutionEntity subProcessInstance);
    
    /**
     * Deletes a historic process instance and all historic data included
     */
    void recordProcessInstanceDeleted(String processInstanceId, String processDefinitionId);
    
    /**
     * Deletes historic process instances for a provided process definition id
     */
    void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId);

    /**
     * Record the start of an activity, if activity history is enabled.
     */
    void recordActivityStart(ExecutionEntity executionEntity);

    /**
     * Record the end of an activity, if activity history is enabled.
     */
    void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason);

    /**
     * Finds the {@link HistoricActivityInstanceEntity} that is active in the given execution.
     */
    HistoricActivityInstanceEntity findActivityInstance(ExecutionEntity execution, boolean createOnNotFound, boolean validateEndTimeNull);

    /**
     * Record a change of the process-definition id of a process instance, if activity history is enabled.
     */
    void recordProcessDefinitionChange(String processInstanceId, String processDefinitionId);

    /**
     * Record the creation of a task, if audit history is enabled.
     */
    void recordTaskCreated(TaskEntity task, ExecutionEntity execution);

    /**
     * Record task as ended, if audit history is enabled.
     */
    void recordTaskEnd(TaskEntity task, ExecutionEntity execution, String deleteReason);

    /**
     * Record task name change, if audit history is enabled.
     */
    void recordTaskInfoChange(TaskEntity taskEntity);

    /**
     * Record a variable has been created, if audit history is enabled.
     */
    void recordVariableCreate(VariableInstanceEntity variable);

    /**
     * Record a variable has been created, if audit history is enabled.
     */
    void recordHistoricDetailVariableCreate(VariableInstanceEntity variable, ExecutionEntity sourceActivityExecution, boolean useActivityId);

    /**
     * Record a variable has been updated, if audit history is enabled.
     */
    void recordVariableUpdate(VariableInstanceEntity variable);

    /**
     * Record a variable has been deleted, if audit history is enabled.
     */
    void recordVariableRemoved(VariableInstanceEntity variable);

    /**
     * Creates a new comment to indicate a new {@link IdentityLink} has been created or deleted, if history is enabled.
     */
    void createIdentityLinkComment(TaskEntity task, String userId, String groupId, String type, boolean create);

    /**
     * Creates a new comment to indicate a new user {@link IdentityLink} has been created or deleted, if history is enabled.
     */
    void createUserIdentityLinkComment(TaskEntity task, String userId, String type, boolean create);

    /**
     * Creates a new comment to indicate a new group {@link IdentityLink} has been created or deleted, if history is enabled.
     */
    void createGroupIdentityLinkComment(TaskEntity task, String groupId, String type, boolean create);

    /**
     * Creates a new comment to indicate a new {@link IdentityLink} has been created or deleted, if history is enabled.
     */
    void createIdentityLinkComment(TaskEntity task, String userId, String groupId, String type, boolean create, boolean forceNullUserId);

    /**
     * Creates a new comment to indicate a new user {@link IdentityLink} has been created or deleted, if history is enabled.
     */
    void createUserIdentityLinkComment(TaskEntity task, String userId, String type, boolean create, boolean forceNullUserId);

    /**
     * Creates a new comment to indicate a new {@link IdentityLink} has been created or deleted, if history is enabled.
     */
    void createProcessInstanceIdentityLinkComment(ExecutionEntity processInstance, String userId, String groupId, String type, boolean create);

    /**
     * Creates a new comment to indicate a new {@link IdentityLink} has been created or deleted, if history is enabled.
     */
    void createProcessInstanceIdentityLinkComment(ExecutionEntity processInstance, String userId, String groupId, String type, boolean create, boolean forceNullUserId);

    /**
     * Creates a new comment to indicate a new attachment has been created or deleted, if history is enabled.
     */
    void createAttachmentComment(TaskEntity task, ExecutionEntity processInstance, String attachmentName, boolean create);

    /**
     * Report form properties submitted, if audit history is enabled.
     */
    void recordFormPropertiesSubmitted(ExecutionEntity processInstance, Map<String, String> properties, String taskId);

    /**
     * Record the creation of a new {@link IdentityLink}, if audit history is enabled.
     */
    void recordIdentityLinkCreated(IdentityLinkEntity identityLink);
    
    /**
     * Record the deletion of a {@link IdentityLink}, if audit history is enabled
     */
    void recordIdentityLinkDeleted(IdentityLinkEntity identityLink);

    void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance);
    
    /**
     * Record the update of a process definition for historic process instance, task, and activity instance, if history is enabled.
     */
    void updateProcessDefinitionIdInHistory(ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance);

}
