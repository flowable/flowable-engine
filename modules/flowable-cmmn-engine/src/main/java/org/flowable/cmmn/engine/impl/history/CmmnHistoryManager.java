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
package org.flowable.cmmn.engine.impl.history;

import java.util.Collection;
import java.util.Date;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface CmmnHistoryManager {

    void recordCaseInstanceStart(CaseInstanceEntity caseInstanceEntity);

    void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state, Date endTime);

    void recordHistoricCaseInstanceReactivated(CaseInstanceEntity caseInstanceEntity);
    
    void recordUpdateCaseInstanceName(CaseInstanceEntity caseInstanceEntity, String name);

    void recordUpdateBusinessKey(CaseInstanceEntity caseInstanceEntity, String businessKey);
    
    void recordUpdateBusinessStatus(CaseInstanceEntity caseInstanceEntity, String businessStatus);

    void recordMilestoneReached(MilestoneInstanceEntity milestoneInstanceEntity);

    void recordHistoricCaseInstanceDeleted(String caseInstanceId, String tenantId);
    
    void recordBulkDeleteHistoricCaseInstances(Collection<String> caseInstanceIds);

    void recordIdentityLinkCreated(IdentityLinkEntity identityLink);

    void recordIdentityLinkCreated(CaseInstanceEntity caseInstance, IdentityLinkEntity identityLink);

    void recordIdentityLinkDeleted(IdentityLinkEntity identityLink);
    
    void recordEntityLinkCreated(EntityLinkEntity entityLink);

    void recordEntityLinkDeleted(EntityLinkEntity entityLink);

    void recordVariableCreate(VariableInstanceEntity variable, Date createTime);

    void recordVariableUpdate(VariableInstanceEntity variable, Date updateTime);

    void recordVariableRemoved(VariableInstanceEntity variable);

    void recordTaskCreated(TaskEntity task);

    void recordTaskEnd(TaskEntity task, String userId, String deleteReason, Date endTime);

    void recordTaskInfoChange(TaskEntity taskEntity, Date changeTime);

    void recordHistoricTaskDeleted(HistoricTaskInstance task);

    void recordPlanItemInstanceCreated(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceReactivated(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceUpdated(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceUnavailable(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity);
    
    void updateCaseDefinitionIdInHistory(CaseDefinition caseDefinition, CaseInstanceEntity caseInstance);

    /**
     * Record historic user task log entry
     * @param taskLogEntryBuilder historic user task log entry description
     */
    void recordHistoricUserTaskLogEntry(HistoricTaskLogEntryBuilder taskLogEntryBuilder);

    /**
     * Delete historic user task log entry
     * @param logNumber log identifier
     */
    void deleteHistoricUserTaskLogEntry(long logNumber);
}
