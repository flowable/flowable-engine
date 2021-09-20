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
package org.flowable.cmmn.engine.impl.history.async;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.convertToBase64;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public abstract class AbstractAsyncCmmnHistoryManager implements CmmnHistoryManager {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    public AbstractAsyncCmmnHistoryManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    protected void addCommonCaseInstanceFields(CaseInstanceEntity caseInstanceEntity, ObjectNode data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceEntity.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, caseInstanceEntity.getRevision());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, caseInstanceEntity.getName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STATE, caseInstanceEntity.getState());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_BUSINESS_KEY, caseInstanceEntity.getBusinessKey());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_BUSINESS_STATUS, caseInstanceEntity.getBusinessStatus());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PARENT_ID, caseInstanceEntity.getParentId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, caseInstanceEntity.getCaseDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_USER_ID, caseInstanceEntity.getStartUserId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_TIME, caseInstanceEntity.getStartTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CALLBACK_ID, caseInstanceEntity.getCallbackId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CALLBACK_TYPE, caseInstanceEntity.getCallbackType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REFERENCE_ID, caseInstanceEntity.getReferenceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REFERENCE_TYPE, caseInstanceEntity.getReferenceType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, caseInstanceEntity.getTenantId());

        if (caseInstanceEntity.getCaseDefinitionId() != null) {
            CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(caseInstanceEntity.getCaseDefinitionId());
            addCaseDefinitionFields(data, caseDefinition);
        }
    }

    protected void addCommonHistoricCaseInstanceFields(HistoricCaseInstanceEntity historicCaseInstanceEntity, ObjectNode data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, historicCaseInstanceEntity.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, historicCaseInstanceEntity.getRevision());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, historicCaseInstanceEntity.getName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STATE, historicCaseInstanceEntity.getState());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_BUSINESS_KEY, historicCaseInstanceEntity.getBusinessKey());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_BUSINESS_STATUS, historicCaseInstanceEntity.getBusinessStatus());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PARENT_ID, historicCaseInstanceEntity.getParentId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, historicCaseInstanceEntity.getCaseDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_USER_ID, historicCaseInstanceEntity.getStartUserId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_TIME, historicCaseInstanceEntity.getStartTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, historicCaseInstanceEntity.getTenantId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CALLBACK_ID, historicCaseInstanceEntity.getCallbackId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CALLBACK_TYPE, historicCaseInstanceEntity.getCallbackType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REFERENCE_ID, historicCaseInstanceEntity.getReferenceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REFERENCE_TYPE, historicCaseInstanceEntity.getReferenceType());

        if (historicCaseInstanceEntity.getCaseDefinitionId() != null) {
            addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(historicCaseInstanceEntity.getCaseDefinitionId()));
        }
    }

    protected void addCaseDefinitionFields(ObjectNode data, CaseDefinition caseDefinition) {
        if (caseDefinition != null) {
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, caseDefinition.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_CATEGORY, caseDefinition.getCategory());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_DEPLOYMENT_ID, caseDefinition.getDeploymentId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_DESCRIPTION, caseDefinition.getDescription());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_KEY, caseDefinition.getKey());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_NAME, caseDefinition.getName());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_VERSION, caseDefinition.getVersion());
        }
    }

    protected void addCommonIdentityLinkFields(IdentityLinkEntity identityLink, ObjectNode data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, identityLink.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_GROUP_ID, identityLink.getGroupId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID, identityLink.getScopeDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID, identityLink.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID, identityLink.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, identityLink.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE, identityLink.getScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, identityLink.getScopeDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_ID, identityLink.getTaskId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PROCESS_INSTANCE_ID, identityLink.getProcessInstanceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_IDENTITY_LINK_TYPE, identityLink.getType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_USER_ID, identityLink.getUserId());
    }

    protected void addCommonEntityLinkFields(EntityLinkEntity entityLink, ObjectNode data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, entityLink.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ENTITY_LINK_TYPE, entityLink.getLinkType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, entityLink.getCreateTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID, entityLink.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID, entityLink.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE, entityLink.getScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID, entityLink.getScopeDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PARENT_ELEMENT_ID, entityLink.getParentElementId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REF_SCOPE_ID, entityLink.getReferenceScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REF_SCOPE_TYPE, entityLink.getReferenceScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REF_SCOPE_DEFINITION_ID, entityLink.getReferenceScopeDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ROOT_SCOPE_ID, entityLink.getRootScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ROOT_SCOPE_TYPE, entityLink.getRootScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_HIERARCHY_TYPE, entityLink.getHierarchyType());
    }

    protected void addCommonTaskFields(TaskEntity task, ObjectNode data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, task.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, task.getRevision());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, task.getName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PARENT_TASK_ID, task.getParentTaskId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DESCRIPTION, task.getDescription());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_OWNER, task.getOwner());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ASSIGNEE, task.getAssignee());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, task.getCreateTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_DEFINITION_KEY, task.getTaskDefinitionKey());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_DEFINITION_ID, task.getTaskDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PRIORITY, task.getPriority());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DUE_DATE, task.getDueDate());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CATEGORY, task.getCategory());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_FORM_KEY, task.getFormKey());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, task.getTenantId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CLAIM_TIME, task.getClaimTime());

        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, task.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID, task.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, task.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID, task.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PLAN_ITEM_INSTANCE_ID, task.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE, task.getScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID, task.getScopeDefinitionId());

        if (task.getScopeDefinitionId() != null) {
            addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(task.getScopeDefinitionId()));
        }
    }

    protected void addCommonPlanItemInstanceFields(PlanItemInstanceEntity planItemInstanceEntity, ObjectNode data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, planItemInstanceEntity.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, planItemInstanceEntity.getRevision());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, planItemInstanceEntity.getName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STATE, planItemInstanceEntity.getState());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, planItemInstanceEntity.getCaseDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, planItemInstanceEntity.getCaseInstanceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STAGE_INSTANCE_ID, planItemInstanceEntity.getStageInstanceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_IS_STAGE, planItemInstanceEntity.isStage());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ELEMENT_ID, planItemInstanceEntity.getElementId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PLAN_ITEM_DEFINITION_ID, planItemInstanceEntity.getPlanItemDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PLAN_ITEM_DEFINITION_TYPE, planItemInstanceEntity.getPlanItemDefinitionType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_USER_ID, planItemInstanceEntity.getStartUserId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, planItemInstanceEntity.getCreateTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REFERENCE_ID, planItemInstanceEntity.getReferenceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REFERENCE_TYPE, planItemInstanceEntity.getReferenceType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ENTRY_CRITERION_ID, planItemInstanceEntity.getEntryCriterionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_EXIT_CRITERION_ID, planItemInstanceEntity.getExitCriterionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_EXTRA_VALUE, planItemInstanceEntity.getExtraValue());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, planItemInstanceEntity.getTenantId());

        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_AVAILABLE_TIME, planItemInstanceEntity.getLastAvailableTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UNAVAILABLE_TIME, planItemInstanceEntity.getLastUnavailableTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_ENABLED_TIME, planItemInstanceEntity.getLastEnabledTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_DISABLED_TIME, planItemInstanceEntity.getLastDisabledTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_STARTED_TIME, planItemInstanceEntity.getLastStartedTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_SUSPENDED_TIME, planItemInstanceEntity.getLastSuspendedTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_COMPLETED_TIME, planItemInstanceEntity.getCompletedTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_OCCURRED_TIME, planItemInstanceEntity.getOccurredTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TERMINATED_TIME, planItemInstanceEntity.getTerminatedTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_EXIT_TIME, planItemInstanceEntity.getExitTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_END_TIME, planItemInstanceEntity.getEndedTime());

        if (planItemInstanceEntity.getCaseDefinitionId() != null) {
            addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(planItemInstanceEntity.getCaseDefinitionId()));
        }
    }

    protected void addCommonMilestoneInstanceFields(MilestoneInstanceEntity milestoneInstanceEntity, ObjectNode data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, milestoneInstanceEntity.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, milestoneInstanceEntity.getRevision());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, milestoneInstanceEntity.getName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, milestoneInstanceEntity.getCaseInstanceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, milestoneInstanceEntity.getCaseDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ELEMENT_ID, milestoneInstanceEntity.getElementId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, milestoneInstanceEntity.getTimeStamp());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, milestoneInstanceEntity.getTenantId());

        if (milestoneInstanceEntity.getCaseDefinitionId() != null) {
            addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(milestoneInstanceEntity.getCaseDefinitionId()));
        }
    }

    protected void addCommonVariableFields(VariableInstanceEntity variable, ObjectNode data, Date time) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, variable.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_ID, variable.getTaskId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, variable.getRevision());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, variable.getName());

        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_EXECUTION_ID, variable.getExecutionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PROCESS_INSTANCE_ID, variable.getProcessInstanceId());

        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, variable.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID, variable.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, variable.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID, variable.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PLAN_ITEM_INSTANCE_ID, variable.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE, variable.getScopeType());

        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME, time);

        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_TYPE, variable.getType().getTypeName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_TEXT_VALUE, variable.getTextValue());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_TEXT_VALUE2, variable.getTextValue2());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_DOUBLE_VALUE, variable.getDoubleValue());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_LONG_VALUE, variable.getLongValue());
        if (variable.getByteArrayRef() != null) {
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_BYTES_VALUE, convertToBase64(variable));
        }

        if (variable.getScopeId() != null && ScopeTypes.CMMN.equals(variable.getScopeType())) {
            CaseInstance caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(variable.getScopeId());
            addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(caseInstance.getCaseDefinitionId()));
        }
    }

    protected void addCommonHistoricTaskLogEntryFields(HistoricTaskLogEntryBuilder taskLogEntryBuilder, ObjectNode data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LOG_ENTRY_DATA, taskLogEntryBuilder.getData());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID, taskLogEntryBuilder.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE, taskLogEntryBuilder.getScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID, taskLogEntryBuilder.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID, taskLogEntryBuilder.getScopeDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_ID, taskLogEntryBuilder.getTaskId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, taskLogEntryBuilder.getTenantId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, taskLogEntryBuilder.getTimeStamp());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LOG_ENTRY_TYPE, taskLogEntryBuilder.getType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_USER_ID, taskLogEntryBuilder.getUserId());
    }

    protected CaseDefinition getCaseDefinition(IdentityLinkEntity identityLink) {
        String caseDefinitionId = null;
        if (ScopeTypes.CMMN.equals(identityLink.getScopeType()) && identityLink.getScopeId() != null) {
            CaseInstance caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(identityLink.getScopeId());
            if (caseInstance != null) {
                caseDefinitionId = caseInstance.getCaseDefinitionId();
            }

        } else if (identityLink.getTaskId() != null) {
            TaskEntity task = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(identityLink.getTaskId());
            if (task != null && ScopeTypes.CMMN.equals(task.getScopeType())) {
                caseDefinitionId = task.getScopeDefinitionId();
            }
        
        } else if (ScopeTypes.PLAN_ITEM.equals(identityLink.getScopeType()) && identityLink.getSubScopeId() != null) {
            PlanItemInstanceEntity planItemInstance = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(identityLink.getSubScopeId());
            if (planItemInstance != null) {
                caseDefinitionId = planItemInstance.getCaseDefinitionId();
            }
        }
        
        return CaseDefinitionUtil.getCaseDefinition(caseDefinitionId);
    }
    
    protected Boolean evaluateShowInOverview(PlanItemInstanceEntity planItemInstanceEntity) {
        Boolean showInOverview = null;
        
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
        String includeInStageOverviewValue = null;
        if (planItemInstanceEntity.isStage()) {
            if (planItemDefinition instanceof Stage) {
                Stage stage = (Stage) planItemDefinition;
                includeInStageOverviewValue = stage.getIncludeInStageOverview();
            }
            
        } else if (planItemDefinition instanceof Milestone) {
            Milestone milestone = (Milestone) planItemDefinition;
            includeInStageOverviewValue = milestone.getIncludeInStageOverview();
        }
        
        if (StringUtils.isNotEmpty(includeInStageOverviewValue)) {
            if ("true".equalsIgnoreCase(includeInStageOverviewValue)) {
                showInOverview = true;
                
            } else if ("false".equalsIgnoreCase(includeInStageOverviewValue)) {
                showInOverview = false;
            
            } else {
                Expression stageExpression = cmmnEngineConfiguration.getExpressionManager().createExpression(includeInStageOverviewValue);
                Object stageValueObject = stageExpression.getValue(planItemInstanceEntity);
                if (!(stageValueObject instanceof Boolean)) {
                    throw new FlowableException("Include in stage overview expression does not resolve to a boolean value " + 
                                    includeInStageOverviewValue + ": " + stageValueObject);
                }
                
                showInOverview = (Boolean) stageValueObject;
            }
        }
        
        return showInOverview;
    }
}
