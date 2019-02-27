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

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class CompositeCmmnHistoryManager implements CmmnHistoryManager {

    protected final Collection<CmmnHistoryManager> historyManagers;

    public CompositeCmmnHistoryManager(Collection<CmmnHistoryManager> historyManagers) {
        this.historyManagers = historyManagers;
    }

    @Override
    public void recordCaseInstanceStart(CaseInstanceEntity caseInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordCaseInstanceStart(caseInstanceEntity);
        }

    }

    @Override
    public void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordCaseInstanceEnd(caseInstanceEntity, state);
        }
    }

    @Override
    public void recordUpdateCaseInstanceName(CaseInstanceEntity caseInstanceEntity, String name) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordUpdateCaseInstanceName(caseInstanceEntity, name);
        }
    }

    @Override
    public void recordMilestoneReached(MilestoneInstanceEntity milestoneInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordMilestoneReached(milestoneInstanceEntity);
        }
    }

    @Override
    public void recordHistoricCaseInstanceDeleted(String caseInstanceId) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordHistoricCaseInstanceDeleted(caseInstanceId);
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordIdentityLinkCreated(identityLink);
        }
    }

    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordIdentityLinkDeleted(identityLink);
        }
    }

    @Override
    public void recordEntityLinkCreated(EntityLinkEntity entityLink) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordEntityLinkCreated(entityLink);
        }
    }

    @Override
    public void recordEntityLinkDeleted(EntityLinkEntity entityLink) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordEntityLinkDeleted(entityLink);
        }
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordVariableCreate(variable);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordVariableUpdate(variable);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordVariableRemoved(variable);
        }
    }

    @Override
    public void recordTaskCreated(TaskEntity task) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordTaskCreated(task);
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, String deleteReason) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordTaskEnd(task, deleteReason);
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordTaskInfoChange(taskEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceCreated(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceCreated(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceAvailable(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceEnabled(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceDisabled(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceStarted(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceSuspended(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceCompleted(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceOccurred(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceTerminated(planItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordPlanItemInstanceExit(planItemInstanceEntity);
        }
    }

    @Override
    public void recordHistoricUserTaskLogEntry(HistoricTaskLogEntryBuilder taskLogEntryBuilder) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.recordHistoricUserTaskLogEntry(taskLogEntryBuilder);
        }
    }

    @Override
    public void deleteHistoricUserTaskLogEntry(long logNumber) {
        for (CmmnHistoryManager historyManager : historyManagers) {
            historyManager.deleteHistoricUserTaskLogEntry(logNumber);
        }
    }
}
