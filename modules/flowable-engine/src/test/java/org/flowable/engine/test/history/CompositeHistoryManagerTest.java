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
package org.flowable.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.history.CompositeHistoryManager;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.flowable.engine.runtime.ActivityInstance;
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
class CompositeHistoryManagerTest {

    @Mock
    protected HistoryManager historyManager1;

    @Mock
    protected HistoryManager historyManager2;

    protected HistoryManager compositeHistoryManager;

    @BeforeEach
    void setUp() {
        compositeHistoryManager = new CompositeHistoryManager(Collections.singletonList(historyManager1));
        ((CompositeHistoryManager) compositeHistoryManager).addHistoryManager(historyManager2);
    }

    @Test
    void isHistoryLevelAtLeastOnlyLevelNoneSayTrue() {
        assertThat(compositeHistoryManager.isHistoryLevelAtLeast(HistoryLevel.FULL)).isFalse();

        verify(historyManager1).isHistoryLevelAtLeast(HistoryLevel.FULL);
        verify(historyManager2).isHistoryLevelAtLeast(HistoryLevel.FULL);
    }

    @Test
    void isHistoryLevelAtLeastOnlyLevelFirstSaysTrue() {
        when(historyManager1.isHistoryLevelAtLeast(HistoryLevel.FULL)).thenReturn(true);
        assertThat(compositeHistoryManager.isHistoryLevelAtLeast(HistoryLevel.FULL)).isTrue();
    }

    @Test
    void isHistoryLevelAtLeastWithDefinitionIdNoneSayTrue() {

        assertThat(compositeHistoryManager.isHistoryLevelAtLeast(HistoryLevel.AUDIT, "def-1")).isFalse();

        verify(historyManager1).isHistoryLevelAtLeast(HistoryLevel.AUDIT, "def-1");
        verify(historyManager1).isHistoryLevelAtLeast(HistoryLevel.AUDIT, "def-1");
    }

    @Test
    void isHistoryLevelAtLeastWithDefinitionIdLastSaysTrue() {

        when(historyManager2.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, "def-2")).thenReturn(true);
        assertThat(compositeHistoryManager.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, "def-2")).isTrue();

        verify(historyManager1).isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, "def-2");
    }

    @Test
    void isHistoryEnabledNoneSayTrue() {
        assertThat(compositeHistoryManager.isHistoryEnabled()).isFalse();

        verify(historyManager1).isHistoryEnabled();
        verify(historyManager2).isHistoryEnabled();
    }

    @Test
    void isHistoryEnabledLastSayTrue() {
        when(historyManager2.isHistoryEnabled()).thenReturn(true);
        assertThat(compositeHistoryManager.isHistoryEnabled()).isTrue();

        verify(historyManager1).isHistoryEnabled();
    }

    @Test
    void isHistoryEnabledWithDefinitionNoneSayTrue() {
        assertThat(compositeHistoryManager.isHistoryEnabled("def-1")).isFalse();

        verify(historyManager1).isHistoryEnabled("def-1");
        verify(historyManager2).isHistoryEnabled("def-1");
    }

    @Test
    void isHistoryEnabledWithDefinitionFirstSaysTrue() {
        when(historyManager1.isHistoryEnabled("def-2")).thenReturn(true);
        assertThat(compositeHistoryManager.isHistoryEnabled("def-2")).isTrue();
    }

    @Test
    void recordProcessInstanceEnd() {
        ExecutionEntity instance = new ExecutionEntityImpl();
        Date endTime = Date.from(Instant.now().plusSeconds(1));
        compositeHistoryManager.recordProcessInstanceEnd(instance, "state", "reason", "activity-id", endTime);

        verify(historyManager1).recordProcessInstanceEnd(same(instance), eq("state"), eq("reason"), eq("activity-id"), eq(endTime));
        verify(historyManager2).recordProcessInstanceEnd(same(instance), eq("state"), eq("reason"), eq("activity-id"), eq(endTime));
    }

    @Test
    void recordProcessInstanceStart() {
        ExecutionEntity instance = new ExecutionEntityImpl();
        compositeHistoryManager.recordProcessInstanceStart(instance);

        verify(historyManager1).recordProcessInstanceStart(same(instance));
        verify(historyManager2).recordProcessInstanceStart(same(instance));
    }

    @Test
    void recordProcessInstanceNameChange() {
        ExecutionEntity instance = new ExecutionEntityImpl();
        compositeHistoryManager.recordProcessInstanceNameChange(instance, "new name");

        verify(historyManager1).recordProcessInstanceNameChange(same(instance), eq("new name"));
        verify(historyManager2).recordProcessInstanceNameChange(same(instance), eq("new name"));
    }

    @Test
    void recordProcessInstanceDeleted() {
        compositeHistoryManager.recordProcessInstanceDeleted("instance-id", "def-id", "tenant-1");

        verify(historyManager1).recordProcessInstanceDeleted("instance-id", "def-id", "tenant-1");
        verify(historyManager2).recordProcessInstanceDeleted("instance-id", "def-id", "tenant-1");
    }

    @Test
    void recordDeleteHistoricProcessInstancesByProcessDefinitionId() {
        compositeHistoryManager.recordDeleteHistoricProcessInstancesByProcessDefinitionId("def-4");

        verify(historyManager1).recordDeleteHistoricProcessInstancesByProcessDefinitionId("def-4");
        verify(historyManager2).recordDeleteHistoricProcessInstancesByProcessDefinitionId("def-4");
    }

    @Test
    void recordActivityStart() {
        ActivityInstance instance = new ActivityInstanceEntityImpl();
        compositeHistoryManager.recordActivityStart(instance);

        verify(historyManager1).recordActivityStart(same(instance));
        verify(historyManager2).recordActivityStart(same(instance));
    }

    @Test
    void recordActivityEnd() {
        ActivityInstance instance = new ActivityInstanceEntityImpl();
        compositeHistoryManager.recordActivityEnd(instance);

        verify(historyManager1).recordActivityEnd(same(instance));
        verify(historyManager2).recordActivityEnd(same(instance));
    }

    @Test
    void findHistoricActivityInstanceNoneReturn() {
        ExecutionEntity instance = new ExecutionEntityImpl();
        assertThat(compositeHistoryManager.findHistoricActivityInstance(instance, true)).isNull();

        verify(historyManager1).findHistoricActivityInstance(same(instance), eq(true));
        verify(historyManager2).findHistoricActivityInstance(same(instance), eq(true));
    }

    @Test
    void findHistoricActivityInstanceFirstReturns() {
        ExecutionEntity instance = new ExecutionEntityImpl();
        HistoricActivityInstanceEntity historicActivityInstance = new HistoricActivityInstanceEntityImpl();
        when(historyManager1.findHistoricActivityInstance(same(instance), eq(true))).thenReturn(historicActivityInstance);
        assertThat(compositeHistoryManager.findHistoricActivityInstance(instance, true)).isSameAs(historicActivityInstance);
    }

    @Test
    void recordProcessDefinitionChange() {
        compositeHistoryManager.recordProcessDefinitionChange("instance-id", "def-change");

        verify(historyManager1).recordProcessDefinitionChange("instance-id", "def-change");
        verify(historyManager2).recordProcessDefinitionChange("instance-id", "def-change");
    }

    @Test
    void recordTaskCreated() {
        TaskEntity task = new TaskEntityImpl();
        ExecutionEntity instance = new ExecutionEntityImpl();
        compositeHistoryManager.recordTaskCreated(task, instance);

        verify(historyManager1).recordTaskCreated(same(task), same(instance));
        verify(historyManager2).recordTaskCreated(same(task), same(instance));
    }

    @Test
    void recordTaskEnd() {
        TaskEntity task = new TaskEntityImpl();
        ExecutionEntity instance = new ExecutionEntityImpl();
        Date endTime = Date.from(Instant.now().plus(1, ChronoUnit.MILLIS));
        compositeHistoryManager.recordTaskEnd(task, instance, "kermit", "test", endTime);

        verify(historyManager1).recordTaskEnd(same(task), same(instance), eq("kermit"), eq("test"), eq(endTime));
        verify(historyManager2).recordTaskEnd(same(task), same(instance), eq("kermit"), eq("test"), eq(endTime));
    }

    @Test
    void recordTaskInfoChange() {
        TaskEntity task = new TaskEntityImpl();
        Date changeTime = Date.from(Instant.now().plusSeconds(3));
        compositeHistoryManager.recordTaskInfoChange(task, "activity", changeTime);

        verify(historyManager1).recordTaskInfoChange(same(task), eq("activity"), eq(changeTime));
        verify(historyManager2).recordTaskInfoChange(same(task), eq("activity"), eq(changeTime));
    }

    @Test
    void recordVariableCreate() {
        VariableInstanceEntity variable = new VariableInstanceEntityImpl();
        Date createTime = Date.from(Instant.now().minusSeconds(30));
        compositeHistoryManager.recordVariableCreate(variable, createTime);

        verify(historyManager1).recordVariableCreate(same(variable), eq(createTime));
        verify(historyManager2).recordVariableCreate(same(variable), eq(createTime));
    }

    @Test
    void recordHistoricDetailVariableCreate() {
        VariableInstanceEntity variable = new VariableInstanceEntityImpl();
        ExecutionEntity execution = new ExecutionEntityImpl();
        Date createTime = Date.from(Instant.now().plusSeconds(10));
        compositeHistoryManager.recordHistoricDetailVariableCreate(variable, execution, true, "id", createTime);

        verify(historyManager1).recordHistoricDetailVariableCreate(same(variable), same(execution), eq(true), eq("id"), eq(createTime));
        verify(historyManager2).recordHistoricDetailVariableCreate(same(variable), same(execution), eq(true), eq("id"), eq(createTime));
    }

    @Test
    void recordVariableUpdate() {
        VariableInstanceEntity variable = new VariableInstanceEntityImpl();
        Date updateTime = Date.from(Instant.now().minusSeconds(4));
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
    void createIdentityLinkComment() {
        TaskEntity task = new TaskEntityImpl();
        compositeHistoryManager.createIdentityLinkComment(task, "user-1", "group-1", "type-1", true);

        verify(historyManager1).createIdentityLinkComment(same(task), eq("user-1"), eq("group-1"), eq("type-1"), eq(true));
        verify(historyManager2).createIdentityLinkComment(same(task), eq("user-1"), eq("group-1"), eq("type-1"), eq(true));
    }

    @Test
    void createUserIdentityLinkComment() {
        TaskEntity task = new TaskEntityImpl();
        compositeHistoryManager.createUserIdentityLinkComment(task, "user-1", "type-1", true);

        verify(historyManager1).createUserIdentityLinkComment(same(task), eq("user-1"), eq("type-1"), eq(true));
        verify(historyManager2).createUserIdentityLinkComment(same(task), eq("user-1"), eq("type-1"), eq(true));
    }

    @Test
    void createGroupIdentityLinkComment() {
        TaskEntity task = new TaskEntityImpl();
        compositeHistoryManager.createGroupIdentityLinkComment(task, "group-1", "type-1", true);

        verify(historyManager1).createGroupIdentityLinkComment(same(task), eq("group-1"), eq("type-1"), eq(true));
        verify(historyManager2).createGroupIdentityLinkComment(same(task), eq("group-1"), eq("type-1"), eq(true));
    }

    @Test
    void createIdentityLinkCommentWithForceNullUser() {
        TaskEntity task = new TaskEntityImpl();
        compositeHistoryManager.createIdentityLinkComment(task, "user-2", "group-2", "type-2", false, false);

        verify(historyManager1).createIdentityLinkComment(same(task), eq("user-2"), eq("group-2"), eq("type-2"), eq(false), eq(false));
        verify(historyManager2).createIdentityLinkComment(same(task), eq("user-2"), eq("group-2"), eq("type-2"), eq(false), eq(false));
    }

    @Test
    void createUserIdentityLinkCommentWithForceNullUser() {
        TaskEntity task = new TaskEntityImpl();
        compositeHistoryManager.createUserIdentityLinkComment(task, "user-1", "type-1", true, true);

        verify(historyManager1).createUserIdentityLinkComment(same(task), eq("user-1"), eq("type-1"), eq(true), eq(true));
        verify(historyManager2).createUserIdentityLinkComment(same(task), eq("user-1"), eq("type-1"), eq(true), eq(true));
    }

    @Test
    void createProcessInstanceIdentityLinkComment() {
        ExecutionEntity processInstance = new ExecutionEntityImpl();
        compositeHistoryManager.createProcessInstanceIdentityLinkComment(processInstance, "user-1", "group-1", "type-1", true);

        verify(historyManager1).createProcessInstanceIdentityLinkComment(same(processInstance), eq("user-1"), eq("group-1"), eq("type-1"), eq(true));
        verify(historyManager2).createProcessInstanceIdentityLinkComment(same(processInstance), eq("user-1"), eq("group-1"), eq("type-1"), eq(true));
    }

    @Test
    void createProcessInstanceIdentityLinkCommentWithForceNullUser() {
        ExecutionEntity processInstance = new ExecutionEntityImpl();
        compositeHistoryManager.createProcessInstanceIdentityLinkComment(processInstance, "user-2", "group-2", "type-2", false, true);

        verify(historyManager1).createProcessInstanceIdentityLinkComment(same(processInstance), eq("user-2"), eq("group-2"), eq("type-2"), eq(false), eq(true));
        verify(historyManager2).createProcessInstanceIdentityLinkComment(same(processInstance), eq("user-2"), eq("group-2"), eq("type-2"), eq(false), eq(true));
    }

    @Test
    void createAttachmentComment() {
        TaskEntity task = new TaskEntityImpl();
        ExecutionEntity processInstance = new ExecutionEntityImpl();
        compositeHistoryManager.createAttachmentComment(task, processInstance, "name", true);

        verify(historyManager1).createAttachmentComment(same(task), same(processInstance), eq("name"), eq(true));
        verify(historyManager2).createAttachmentComment(same(task), same(processInstance), eq("name"), eq(true));
    }

    @Test
    void recordFormPropertiesSubmitted() {
        ExecutionEntity processInstance = new ExecutionEntityImpl();
        Map<String, String> properties = new HashMap<>();
        properties.put("key", "value");
        Date createTime = Date.from(Instant.now().plusSeconds(3));
        compositeHistoryManager.recordFormPropertiesSubmitted(processInstance, properties, "task-1", createTime);

        verify(historyManager1).recordFormPropertiesSubmitted(same(processInstance), eq(properties), eq("task-1"), eq(createTime));
        verify(historyManager2).recordFormPropertiesSubmitted(same(processInstance), eq(properties), eq("task-1"), eq(createTime));
    }

    @Test
    void recordIdentityLinkCreated() {
        IdentityLinkEntity identityLink = new IdentityLinkEntityImpl();
        compositeHistoryManager.recordIdentityLinkCreated(identityLink);

        verify(historyManager1).recordIdentityLinkCreated(same(identityLink));
        verify(historyManager2).recordIdentityLinkCreated(same(identityLink));
    }

    @Test
    void recordIdentityLinkCreatedWithProcessInstance() {
        ExecutionEntity processInstance = new ExecutionEntityImpl();
        IdentityLinkEntity identityLink = new IdentityLinkEntityImpl();
        compositeHistoryManager.recordIdentityLinkCreated(processInstance, identityLink);

        verify(historyManager1).recordIdentityLinkCreated(same(processInstance), same(identityLink));
        verify(historyManager2).recordIdentityLinkCreated(same(processInstance), same(identityLink));
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
    void updateProcessBusinessKeyInHistory() {
        ExecutionEntity processInstance = new ExecutionEntityImpl();
        compositeHistoryManager.updateProcessBusinessKeyInHistory(processInstance);

        verify(historyManager1).updateProcessBusinessKeyInHistory(same(processInstance));
        verify(historyManager2).updateProcessBusinessKeyInHistory(same(processInstance));
    }

    @Test
    void updateProcessDefinitionIdInHistory() {
        ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntityImpl();
        ExecutionEntity processInstance = new ExecutionEntityImpl();
        compositeHistoryManager.updateProcessDefinitionIdInHistory(processDefinition, processInstance);

        verify(historyManager1).updateProcessDefinitionIdInHistory(same(processDefinition), same(processInstance));
        verify(historyManager2).updateProcessDefinitionIdInHistory(same(processDefinition), same(processInstance));
    }

    @Test
    void updateActivity() {
        ExecutionEntity execution = new ExecutionEntityImpl();
        FlowElement flowElement = new SequenceFlow();
        TaskEntity task = new TaskEntityImpl();
        Date updateTime = Date.from(Instant.now().plusSeconds(9));
        compositeHistoryManager.updateActivity(execution, "old-id", flowElement, task, updateTime);

        verify(historyManager1).updateActivity(same(execution), eq("old-id"), same(flowElement), same(task), eq(updateTime));
        verify(historyManager2).updateActivity(same(execution), eq("old-id"), same(flowElement), same(task), eq(updateTime));
    }

    @Test
    void updateHistoricActivityInstance() {
        ActivityInstanceEntity activityInstance = new ActivityInstanceEntityImpl();
        compositeHistoryManager.updateHistoricActivityInstance(activityInstance);

        verify(historyManager1).updateHistoricActivityInstance(same(activityInstance));
        verify(historyManager2).updateHistoricActivityInstance(same(activityInstance));
    }

    @Test
    void createHistoricActivityInstance() {
        ActivityInstanceEntity activityInstance = new ActivityInstanceEntityImpl();
        compositeHistoryManager.createHistoricActivityInstance(activityInstance);

        verify(historyManager1).createHistoricActivityInstance(same(activityInstance));
        verify(historyManager2).createHistoricActivityInstance(same(activityInstance));
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
        compositeHistoryManager.deleteHistoryUserTaskLog(10L);

        verify(historyManager1).deleteHistoryUserTaskLog(10L);
        verify(historyManager2).deleteHistoryUserTaskLog(10L);
    }
}
