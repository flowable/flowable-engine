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

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface CmmnHistoryManager {

    void recordCaseInstanceStart(CaseInstanceEntity caseInstanceEntity);

    void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state);

    void recordMilestoneReached(MilestoneInstanceEntity milestoneInstanceEntity);

    void recordHistoricCaseInstanceDeleted(String caseInstanceId);

    void recordIdentityLinkCreated(IdentityLinkEntity identityLink);

    void recordIdentityLinkDeleted(IdentityLinkEntity identityLink);

    void recordVariableCreate(VariableInstanceEntity variable);

    void recordVariableUpdate(VariableInstanceEntity variable);

    void recordVariableRemoved(VariableInstanceEntity variable);

    void recordTaskCreated(TaskEntity task);

    void recordTaskEnd(TaskEntity task, String deleteReason);

    void recordTaskInfoChange(TaskEntity taskEntity);

    void recordPlanItemInstanceCreated(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity);

    void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity);

}
