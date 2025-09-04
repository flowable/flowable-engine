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
package org.flowable.cmmn.test.history;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.CompositeCmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityImpl;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.BaseHistoricTaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * @author Filip Hrisafov
 */
@MockitoSettings
class CompositeCmmnHistoryManagerTest {

    @Mock
    protected CmmnHistoryManager historyManager1;

    @Mock
    protected CmmnHistoryManager historyManager2;

    protected CmmnHistoryManager compositeHistoryManager;

    @BeforeEach
    void setUp() {
        compositeHistoryManager = new CompositeCmmnHistoryManager(Collections.singletonList(historyManager1));
        ((CompositeCmmnHistoryManager) compositeHistoryManager).addHistoryManager(historyManager2);

    }

    @Test
    void recordCaseInstanceStart() {
        CaseInstanceEntity caseInstance = new CaseInstanceEntityImpl();
        compositeHistoryManager.recordCaseInstanceStart(caseInstance);

        verify(historyManager1).recordCaseInstanceStart(same(caseInstance));
        verify(historyManager2).recordCaseInstanceStart(same(caseInstance));
    }

    @Test
    void recordCaseInstanceEnd() {
        CaseInstanceEntity caseInstance = new CaseInstanceEntityImpl();
        Date endTime = Date.from(Instant.now().minusSeconds(2));
        compositeHistoryManager.recordCaseInstanceEnd(caseInstance, "state", endTime);

        verify(historyManager1).recordCaseInstanceEnd(same(caseInstance), eq("state"), eq(endTime));
        verify(historyManager2).recordCaseInstanceEnd(same(caseInstance), eq("state"), eq(endTime));
    }

    @Test
    void recordUpdateCaseInstanceName() {
        CaseInstanceEntity caseInstance = new CaseInstanceEntityImpl();
        compositeHistoryManager.recordUpdateCaseInstanceName(caseInstance, "name");

        verify(historyManager1).recordUpdateCaseInstanceName(same(caseInstance), eq("name"));
        verify(historyManager2).recordUpdateCaseInstanceName(same(caseInstance), eq("name"));
    }

    @Test
    void recordMilestoneReached() {
        MilestoneInstanceEntity milestoneInstance = new MilestoneInstanceEntityImpl();
        compositeHistoryManager.recordMilestoneReached(milestoneInstance);

        verify(historyManager1).recordMilestoneReached(same(milestoneInstance));
        verify(historyManager2).recordMilestoneReached(same(milestoneInstance));
    }

    @Test
    void recordHistoricCaseInstanceDeleted() {
        compositeHistoryManager.recordHistoricCaseInstanceDeleted("case-id", "tenant-1");

        verify(historyManager1).recordHistoricCaseInstanceDeleted("case-id", "tenant-1");
        verify(historyManager2).recordHistoricCaseInstanceDeleted("case-id", "tenant-1");
    }

    @Test
    void recordIdentityLinkCreated() {
        IdentityLinkEntity identityLink = new IdentityLinkEntityImpl();
        compositeHistoryManager.recordIdentityLinkCreated(identityLink);

        verify(historyManager1).recordIdentityLinkCreated(same(identityLink));
        verify(historyManager2).recordIdentityLinkCreated(same(identityLink));
    }

    @Test
    void recordIdentityLinkCreatedWithCaseInstance() {
        CaseInstanceEntity caseInstance = new CaseInstanceEntityImpl();
        IdentityLinkEntity identityLink = new IdentityLinkEntityImpl();
        compositeHistoryManager.recordIdentityLinkCreated(caseInstance, identityLink);

        verify(historyManager1).recordIdentityLinkCreated(same(caseInstance), same(identityLink));
        verify(historyManager2).recordIdentityLinkCreated(same(caseInstance), same(identityLink));
    }

    @Test
    void recordIdentityLinkDeleted() {
        IdentityLinkEntity identityLink = new IdentityLinkEntityImpl();
        compositeHistoryManager.recordIdentityLinkDeleted(identityLink);

        verify(historyManager1).recordIdentityLinkDeleted(same(identityLink));
        verify(historyManager2).recordIdentityLinkDeleted(same(identityLink));
    }

    @Test
    void recordEntityLinkCreated() {
        EntityLinkEntity entityLink = new EntityLinkEntityImpl();
        compositeHistoryManager.recordEntityLinkCreated(entityLink);

        verify(historyManager1).recordEntityLinkCreated(same(entityLink));
        verify(historyManager2).recordEntityLinkCreated(same(entityLink));
    }

    @Test
    void recordEntityLinkDeleted() {
        EntityLinkEntity entityLink = new EntityLinkEntityImpl();
        compositeHistoryManager.recordEntityLinkDeleted(entityLink);

        verify(historyManager1).recordEntityLinkDeleted(same(entityLink));
        verify(historyManager2).recordEntityLinkDeleted(same(entityLink));
    }

    @Test
    void recordVariableCreate() {
        VariableInstanceEntity variable = new VariableInstanceEntityImpl();
        Date createTime = Date.from(Instant.now().minusSeconds(5));
        compositeHistoryManager.recordVariableCreate(variable, createTime);

        verify(historyManager1).recordVariableCreate(same(variable), eq(createTime));
        verify(historyManager2).recordVariableCreate(same(variable), eq(createTime));
    }

    @Test
    void recordVariableUpdate() {
        VariableInstanceEntity variable = new VariableInstanceEntityImpl();
        Date updateTime = Date.from(Instant.now().plusSeconds(40));
        compositeHistoryManager.recordVariableUpdate(variable, updateTime);

        verify(historyManager1).recordVariableUpdate(same(variable), eq(updateTime));
        verify(historyManager2).recordVariableUpdate(same(variable), eq(updateTime));
    }

    @Test
    void recordVariableRemoved() {
        VariableInstanceEntity variable = new VariableInstanceEntityImpl();
        compositeHistoryManager.recordVariableRemoved(variable);

        verify(historyManager1).recordVariableRemoved(same(variable));
        verify(historyManager2).recordVariableRemoved(same(variable));
    }

    @Test
    void recordTaskCreated() {
        TaskEntity task = new TaskEntityImpl();
        compositeHistoryManager.recordTaskCreated(task);

        verify(historyManager1).recordTaskCreated(same(task));
        verify(historyManager2).recordTaskCreated(same(task));
    }

    @Test
    void recordTaskEnd() {
        TaskEntity task = new TaskEntityImpl();
        Date endTime = Date.from(Instant.now().plusSeconds(3));
        compositeHistoryManager.recordTaskEnd(task, "kermit", "delete reason", endTime);

        verify(historyManager1).recordTaskEnd(same(task), eq("kermit"), eq("delete reason"), eq(endTime));
        verify(historyManager2).recordTaskEnd(same(task), eq("kermit"), eq("delete reason"), eq(endTime));
    }

    @Test
    void recordTaskInfoChange() {
        TaskEntity task = new TaskEntityImpl();
        Date changeTime = Date.from(Instant.now().minusSeconds(8));
        compositeHistoryManager.recordTaskInfoChange(task, changeTime);

        verify(historyManager1).recordTaskInfoChange(same(task), eq(changeTime));
        verify(historyManager2).recordTaskInfoChange(same(task), eq(changeTime));
    }

    @Test
    void recordPlanItemInstanceCreated() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceCreated(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceCreated(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceCreated(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceAvailable() {

        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceAvailable(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceAvailable(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceAvailable(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceEnabled() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceEnabled(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceEnabled(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceEnabled(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceDisabled() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceDisabled(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceDisabled(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceDisabled(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceStarted() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceStarted(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceStarted(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceStarted(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceSuspended() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceSuspended(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceSuspended(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceSuspended(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceCompleted() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceCompleted(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceCompleted(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceCompleted(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceOccurred() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceOccurred(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceOccurred(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceOccurred(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceTerminated() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceTerminated(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceTerminated(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceTerminated(same(planItemInstance));
    }

    @Test
    void recordPlanItemInstanceExit() {
        PlanItemInstanceEntity planItemInstance = new PlanItemInstanceEntityImpl();
        compositeHistoryManager.recordPlanItemInstanceExit(planItemInstance);

        verify(historyManager1).recordPlanItemInstanceExit(same(planItemInstance));
        verify(historyManager2).recordPlanItemInstanceExit(same(planItemInstance));
    }

    @Test
    void recordHistoricUserTaskLogEntry() {
        HistoricTaskLogEntryBuilder taskLogEntryBuilder = new BaseHistoricTaskLogEntryBuilderImpl();
        compositeHistoryManager.recordHistoricUserTaskLogEntry(taskLogEntryBuilder);

        verify(historyManager1).recordHistoricUserTaskLogEntry(same(taskLogEntryBuilder));
        verify(historyManager2).recordHistoricUserTaskLogEntry(same(taskLogEntryBuilder));
    }

    @Test
    void deleteHistoricUserTaskLogEntry() {
        compositeHistoryManager.deleteHistoricUserTaskLogEntry(10L);

        verify(historyManager1).deleteHistoricUserTaskLogEntry(10L);
        verify(historyManager2).deleteHistoricUserTaskLogEntry(10L);
    }
}
