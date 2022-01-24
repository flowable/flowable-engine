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
package org.flowable.engine.impl.history.async;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.convertToBase64;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.AbstractHistoryManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public abstract class AbstractAsyncHistoryManager extends AbstractHistoryManager {

    public AbstractAsyncHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    protected void addCommonProcessInstanceFields(ExecutionEntity processInstance, ObjectNode data) {
        putIfNotNull(data, HistoryJsonConstants.ID, processInstance.getId());
        putIfNotNull(data, HistoryJsonConstants.REVISION, processInstance.getRevision());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getProcessInstanceId());
        putIfNotNull(data, HistoryJsonConstants.NAME, processInstance.getName());
        putIfNotNull(data, HistoryJsonConstants.BUSINESS_KEY, processInstance.getBusinessKey());
        putIfNotNull(data, HistoryJsonConstants.BUSINESS_STATUS, processInstance.getBusinessStatus());
        putIfNotNull(data, HistoryJsonConstants.DEPLOYMENT_ID, processInstance.getDeploymentId());
        putIfNotNull(data, HistoryJsonConstants.START_TIME, processInstance.getStartTime());
        putIfNotNull(data, HistoryJsonConstants.START_USER_ID, processInstance.getStartUserId());
        putIfNotNull(data, HistoryJsonConstants.START_ACTIVITY_ID, processInstance.getStartActivityId());
        putIfNotNull(data, HistoryJsonConstants.SUPER_PROCESS_INSTANCE_ID, processInstance.getSuperExecution() != null ? processInstance.getSuperExecution().getProcessInstanceId() : null);
        putIfNotNull(data, HistoryJsonConstants.CALLBACK_ID, processInstance.getCallbackId());
        putIfNotNull(data, HistoryJsonConstants.CALLBACK_TYPE, processInstance.getCallbackType());
        putIfNotNull(data, HistoryJsonConstants.REFERENCE_ID, processInstance.getReferenceId());
        putIfNotNull(data, HistoryJsonConstants.REFERENCE_TYPE, processInstance.getReferenceType());
        putIfNotNull(data, HistoryJsonConstants.PROPAGATED_STAGE_INSTANCE_ID, processInstance.getPropagatedStageInstanceId());
        putIfNotNull(data, HistoryJsonConstants.TENANT_ID, processInstance.getTenantId());

        addProcessDefinitionFields(data, processInstance.getProcessDefinitionId());
    }

    protected void addProcessDefinitionFields(ObjectNode data, String processDefinitionId) {
        if (processDefinitionId != null) {
            ProcessDefinition processDefinition = processEngineConfiguration.getDeploymentManager().findDeployedProcessDefinitionById(processDefinitionId);
            addProcessDefinitionFields(data, processDefinition);
        }
    }

    protected void addProcessDefinitionFields(ObjectNode data, ProcessDefinition processDefinition) {
        if (processDefinition != null) {
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinition.getId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_KEY, processDefinition.getKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_NAME, processDefinition.getName());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_VERSION, processDefinition.getVersion());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_CATEGORY, processDefinition.getCategory());
            putIfNotNull(data, HistoryJsonConstants.DEPLOYMENT_ID, processDefinition.getDeploymentId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITIN_DERIVED_FROM, processDefinition.getDerivedFrom());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITIN_DERIVED_FROM_ROOT, processDefinition.getDerivedFromRoot());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITIN_DERIVED_VERSION, processDefinition.getDerivedVersion());
        }
    }

    protected void addCommonTaskFields(TaskEntity task, ExecutionEntity execution, ObjectNode data) {
        putIfNotNull(data, HistoryJsonConstants.ID, task.getId());
        putIfNotNull(data, HistoryJsonConstants.REVISION, task.getRevision());
        putIfNotNull(data, HistoryJsonConstants.NAME, task.getName());
        putIfNotNull(data, HistoryJsonConstants.PARENT_TASK_ID, task.getParentTaskId());
        putIfNotNull(data, HistoryJsonConstants.DESCRIPTION, task.getDescription());
        putIfNotNull(data, HistoryJsonConstants.OWNER, task.getOwner());
        putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, task.getAssignee());
        putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, task.getCreateTime());
        putIfNotNull(data, HistoryJsonConstants.TASK_DEFINITION_KEY, task.getTaskDefinitionKey());
        putIfNotNull(data, HistoryJsonConstants.TASK_DEFINITION_ID, task.getTaskDefinitionId());
        putIfNotNull(data, HistoryJsonConstants.FORM_KEY, task.getFormKey());
        putIfNotNull(data, HistoryJsonConstants.PRIORITY, task.getPriority());
        putIfNotNull(data, HistoryJsonConstants.DUE_DATE, task.getDueDate());
        putIfNotNull(data, HistoryJsonConstants.CATEGORY, task.getCategory());
        putIfNotNull(data, HistoryJsonConstants.CLAIM_TIME, task.getClaimTime());
        putIfNotNull(data, HistoryJsonConstants.TENANT_ID, task.getTenantId());

        if (execution != null) {
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, execution.getId());

            addProcessDefinitionFields(data, execution.getProcessDefinitionId());

        } else if (task.getProcessDefinitionId() != null) {
            addProcessDefinitionFields(data, task.getProcessDefinitionId());

        }
    }

    protected void addCommonVariableFields(VariableInstanceEntity variable, ObjectNode data) {
        putIfNotNull(data, HistoryJsonConstants.ID, variable.getId());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, variable.getProcessInstanceId());
        putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, variable.getExecutionId());
        putIfNotNull(data, HistoryJsonConstants.TASK_ID, variable.getTaskId());
        putIfNotNull(data, HistoryJsonConstants.REVISION, variable.getRevision());
        putIfNotNull(data, HistoryJsonConstants.NAME, variable.getName());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_ID, variable.getScopeId());
        putIfNotNull(data, HistoryJsonConstants.SUB_SCOPE_ID, variable.getSubScopeId());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_TYPE, variable.getScopeType());

        putIfNotNull(data, HistoryJsonConstants.VARIABLE_TYPE, variable.getType().getTypeName());
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE, variable.getTextValue());
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE2, variable.getTextValue2());
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE, variable.getDoubleValue());
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_LONG_VALUE, variable.getLongValue());
        if (variable.getByteArrayRef() != null) {
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_BYTES_VALUE, convertToBase64(variable));
        }

        if (variable.getExecutionId() != null) {
            addProcessDefinitionFields(data, variable.getProcessDefinitionId());
        }
    }

    protected void addCommonIdentityLinkFields(IdentityLinkEntity identityLink, ObjectNode data) {
        putIfNotNull(data, HistoryJsonConstants.ID, identityLink.getId());
        putIfNotNull(data, HistoryJsonConstants.GROUP_ID, identityLink.getGroupId());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, identityLink.getProcessDefinitionId());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, identityLink.getProcessInstanceId());
        putIfNotNull(data, HistoryJsonConstants.TASK_ID, identityLink.getTaskId());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_DEFINITION_ID, identityLink.getScopeDefinitionId());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_ID, identityLink.getScopeId());
        putIfNotNull(data, HistoryJsonConstants.SUB_SCOPE_ID, identityLink.getSubScopeId());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_TYPE, identityLink.getScopeType());
        putIfNotNull(data, HistoryJsonConstants.IDENTITY_LINK_TYPE, identityLink.getType());
        putIfNotNull(data, HistoryJsonConstants.USER_ID, identityLink.getUserId());
    }

    protected void addCommonEntityLinkFields(EntityLinkEntity entityLink, ObjectNode data) {
        putIfNotNull(data, HistoryJsonConstants.ID, entityLink.getId());
        putIfNotNull(data, HistoryJsonConstants.ENTITY_LINK_TYPE, entityLink.getLinkType());
        putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, entityLink.getCreateTime());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_ID, entityLink.getScopeId());
        putIfNotNull(data, HistoryJsonConstants.SUB_SCOPE_ID, entityLink.getSubScopeId());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_TYPE, entityLink.getScopeType());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_DEFINITION_ID, entityLink.getScopeDefinitionId());
        putIfNotNull(data, HistoryJsonConstants.PARENT_ELEMENT_ID, entityLink.getParentElementId());
        putIfNotNull(data, HistoryJsonConstants.REF_SCOPE_ID, entityLink.getReferenceScopeId());
        putIfNotNull(data, HistoryJsonConstants.REF_SCOPE_TYPE, entityLink.getReferenceScopeType());
        putIfNotNull(data, HistoryJsonConstants.REF_SCOPE_DEFINITION_ID, entityLink.getReferenceScopeDefinitionId());
        putIfNotNull(data, HistoryJsonConstants.ROOT_SCOPE_ID, entityLink.getRootScopeId());
        putIfNotNull(data, HistoryJsonConstants.ROOT_SCOPE_TYPE, entityLink.getRootScopeType());
        putIfNotNull(data, HistoryJsonConstants.HIERARCHY_TYPE, entityLink.getHierarchyType());
    }

    protected void addCommonActivityInstanceFields(ActivityInstance activityInstance, ObjectNode data) {
        putIfNotNull(data, HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID, activityInstance.getId());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, activityInstance.getProcessDefinitionId());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, activityInstance.getProcessInstanceId());
        putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, activityInstance.getExecutionId());
        putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityInstance.getActivityId());
        putIfNotNull(data, HistoryJsonConstants.ACTIVITY_NAME, activityInstance.getActivityName());
        putIfNotNull(data, HistoryJsonConstants.ACTIVITY_TYPE, activityInstance.getActivityType());
        if (activityInstance.getTransactionOrder() != null) {
            putIfNotNull(data, HistoryJsonConstants.TRANSACTION_ORDER, activityInstance.getTransactionOrder());
        }
        putIfNotNull(data, HistoryJsonConstants.TENANT_ID, activityInstance.getTenantId());
    }

    protected void addHistoricTaskLogEntryFields(HistoricTaskLogEntryBuilder taskLogEntryBuilder, ObjectNode data) {
        putIfNotNull(data, HistoryJsonConstants.LOG_ENTRY_DATA, taskLogEntryBuilder.getData());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, taskLogEntryBuilder.getProcessInstanceId());
        putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, taskLogEntryBuilder.getExecutionId());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, taskLogEntryBuilder.getProcessDefinitionId());
        putIfNotNull(data, HistoryJsonConstants.TASK_ID, taskLogEntryBuilder.getTaskId());
        putIfNotNull(data, HistoryJsonConstants.TENANT_ID, taskLogEntryBuilder.getTenantId());
        putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, taskLogEntryBuilder.getTimeStamp());
        putIfNotNull(data, HistoryJsonConstants.USER_ID, taskLogEntryBuilder.getUserId());
        putIfNotNull(data, HistoryJsonConstants.LOG_ENTRY_TYPE, taskLogEntryBuilder.getType());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_ID, taskLogEntryBuilder.getScopeId());
        putIfNotNull(data, HistoryJsonConstants.SUB_SCOPE_ID, taskLogEntryBuilder.getSubScopeId());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_TYPE, taskLogEntryBuilder.getScopeType());
        putIfNotNull(data, HistoryJsonConstants.SCOPE_DEFINITION_ID, taskLogEntryBuilder.getScopeDefinitionId());
    }
}
