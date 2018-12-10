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
package org.flowable.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntity;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
@FlowableTest
public class HistoryServiceTaskLogTest {

    protected Task task;

    @AfterEach
    public void deleteTasks(TaskService taskService, HistoryService historyService) {
        if (task != null) {
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    protected void deleteTaskWithLogEntries(TaskService taskService, String taskId) {
        taskService.deleteTask(taskId, true);
    }

    @Test
    public void createTaskEvent(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(1);

        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_CREATED");
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getTimeStamp).isEqualTo(task.getCreateTime());
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getUserId).isNull();

        taskService.deleteTask(task.getId());
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isGreaterThan(0l);
    }

    @Test
    public void createTaskEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();

            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(
                taskLogsByTaskInstanceId
            ).size().isEqualTo(1);

            assertThat(taskLogsByTaskInstanceId.get(0)).
                extracting(HistoricTaskLogEntry::getUserId).isEqualTo("testUser");
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void queryForNonExistingTaskLogEntries(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            create();

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId("NON-EXISTING-TASK-ID").list();

        assertThat(
            taskLogsByTaskInstanceId
        ).isEmpty();
    }

    @Test
    public void queryForNullTaskLogEntries_returnsAll(TaskService taskService, HistoryService historyService) {
        Task taskA = taskService.createTaskBuilder().
            create();
        Task taskB = taskService.createTaskBuilder().
            create();
        Task taskC = taskService.createTaskBuilder().
            create();

        try {
            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(null).list();

            assertThat(
                taskLogsByTaskInstanceId
            ).size().isEqualTo(3L);
        } finally {
            deleteTaskWithLogEntries(taskService, taskC.getId());
            deleteTaskWithLogEntries(taskService, taskB.getId());
            deleteTaskWithLogEntries(taskService, taskA.getId());
        }
    }

    @Test
    public void deleteTaskEventLogEntry(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(1);

        historyService.deleteHistoricTaskLogEntry(taskLogsByTaskInstanceId.get(0).getLogNumber());

        taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).isEmpty();
    }

    @Test
    public void deleteNonExistingTaskEventLogEntry(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            create();
        // non existing log entry delete should be successful
        historyService.deleteHistoricTaskLogEntry(Long.MIN_VALUE);

        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list().size()).isEqualTo(1);
    }

    @Test
    public void taskAssigneeEvent(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.setAssignee(task.getId(), "newAssignee");
        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).contains("\"newAssigneeId\":\"newAssignee\"","\"previousAssigneeId\":\"initialAssignee\"");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void changeAssigneeTaskEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService) {
        assertThatAuthenticatedUserIsSet(taskService,
            historyService, taskId -> taskService.setAssignee(taskId, "newAssignee")
        );
    }

    @Test
    public void taskOwnerEvent(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.setOwner(task.getId(), "newOwner");
        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).
            contains("\"previousOwnerId\":null","\"newOwnerId\":\"newOwner\"");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_OWNER_CHANGED");
    }

    @Test
    public void changeOwnerTaskEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService) {
        assertThatAuthenticatedUserIsSet(taskService,
            historyService, taskId -> taskService.setOwner(taskId, "newOwner")
        );
    }

    @Test
    public void claimTaskEvent(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            create();

        taskService.claim(task.getId(), "testUser");

        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).contains("\"newAssigneeId\":\"testUser\"","\"previousAssigneeId\":null");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void unClaimTaskEvent(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.unclaim(task.getId());

        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).
            contains("\"newAssigneeId\":null","\"previousAssigneeId\":\"initialAssignee\"");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void changePriority(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            create();

        taskService.setPriority(task.getId(), Integer.MAX_VALUE);
        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).
            contains("\"newPriority\":2147483647","\"previousPriority\":50}");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_PRIORITY_CHANGED");
    }

    @Test
    public void changePriorityEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService) {
        assertThatAuthenticatedUserIsSet(taskService,
            historyService, taskId ->  taskService.setPriority(taskId, Integer.MAX_VALUE)
        );
    }

    protected void assertThatAuthenticatedUserIsSet(TaskService taskService, HistoryService historyService,
        Consumer<String> functionToAssert) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            functionToAssert.accept(task.getId());

            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(
                taskLogsByTaskInstanceId
            ).size().isEqualTo(2);

            assertThat(taskLogsByTaskInstanceId.get(1)).
                extracting(HistoricTaskLogEntry::getUserId).isEqualTo("testUser");
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void changeDueDate(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            create();

        taskService.setDueDate(task.getId(), new Date(0));
        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).contains("\"newDueDate\":0","\"previousDueDate\":null}");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_DUEDATE_CHANGED");
    }

    @Test
    public void saveTask(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            create();

        task.setName("newTaskName");
        task.setAssignee("newAssignee");
        task.setOwner("newOwner");
        task.setPriority(Integer.MAX_VALUE);
        task.setDueDate(new Date(0));
        taskService.saveTask(task);

        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(6);
    }

    @Test
    public void changeDueDateEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService) {
        assertThatAuthenticatedUserIsSet(taskService,
            historyService, taskId ->  taskService.setDueDate(taskId, new Date(0))
        );
    }

    @Test
    public void createCustomTaskEventLog(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().create();
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.timeStamp(new Date(0));
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.add();

        List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
        assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
        assertThat(historicTaskLogEntry.getUserId()).isEqualTo("testUser");
        assertThat(historicTaskLogEntry.getTaskId()).isEqualTo(task.getId());
        assertThat(historicTaskLogEntry.getType()).isEqualTo("customType");
        assertThat(historicTaskLogEntry.getTimeStamp()).isEqualTo(new Date(0));
        assertThat(historicTaskLogEntry.getData()).isEqualTo("testData");
        historyService.deleteHistoricTaskLogEntry(logEntries.get(0).getLogNumber());
    }

    @Test
    public void createCustomTaskEventLog_taskIdIsEnoughToCreateTaskLogEntry(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.add();
        List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
        assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
        assertThat(historicTaskLogEntry.getUserId()).isNull();
        assertThat(historicTaskLogEntry.getTaskId()).isEqualTo(task.getId());
        assertThat(historicTaskLogEntry.getType()).isNull();
        assertThat(historicTaskLogEntry.getTimeStamp()).isNotNull();
        assertThat(historicTaskLogEntry.getData()).isNull();
    }

    @Test
    public void createCustomTaskEventLog_withoutTimeStamp_addsDefault(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.add();

        List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
        assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
        assertThat(historicTaskLogEntry.getTimeStamp()).isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logSuspensionStateEvents(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        try {
            runtimeService.suspendProcessInstanceById(processInstance.getId());
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertNotNull(task);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_SUSPENSIONSTATE_CHANGED")
            ;

            runtimeService.activateProcessInstanceById(processInstance.getId());

            logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_SUSPENSIONSTATE_CHANGED")
            ;
        } finally {
            historyService.createHistoricTaskLogEntryQuery().taskId(
                taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId()
            ).list().
                forEach(
                    logEntry -> historyService.deleteHistoricTaskLogEntry(logEntry.getLogNumber())
                );
            runtimeService.deleteProcessInstance(processInstance.getId(), "clean up");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logProcessTaskEvents(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        try {
            taskService.setAssignee(task.getId(), "newAssignee");
            taskService.setOwner(task.getId(), "newOwner");
            taskService.complete(task.getId());

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(4);
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_CREATED")
            ;
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId())
            ;
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getExecutionId()).isEqualTo(task.getExecutionId())
            ;
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getProcessInstanceId()).isEqualTo(processInstance.getId())
            ;
            assertThat(logEntries.get(1)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_ASSIGNEE_CHANGED")
            ;
            assertThat(logEntries.get(2)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_OWNER_CHANGED")
            ;
            assertThat(logEntries.get(3)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_COMPLETED")
            ;
        } finally {
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddCandidateUser(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        try {
            assertNotNull(processInstance);
            assertNotNull(task);

            taskService.addCandidateUser(task.getId(), "newCandidateUser");

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(new String(logEntries.get(1).getData())).contains(
                "\"type\":\"candidate\"",
                "\"userId\":\"newCandidateUser\""
            );
        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddParticipantUser(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        try {
            assertNotNull(processInstance);
            assertNotNull(task);

            taskService.addUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.PARTICIPANT);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(new String(logEntries.get(1).getData())).contains(
                "\"type\":\"participant\"",
                "\"userId\":\"newCandidateUser\""
            );
        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddCandidateGroup(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        try {

            taskService.addCandidateGroup(task.getId(), "newCandidateGroup");

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(new String(logEntries.get(1).getData())).contains(
                "\"type\":\"candidate\"",
                "\"groupId\":\"newCandidateGroup\""
            );
        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddGroup(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        try {

            taskService.addGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.PARTICIPANT);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(new String(logEntries.get(1).getData())).contains(
                "\"type\":\"participant\"",
                "\"groupId\":\"newCandidateGroup\""
            );
        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logDeleteCandidateGroup(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        taskService.addCandidateGroup(task.getId(), "newCandidateGroup");
        try {

            taskService.deleteCandidateGroup(task.getId(), "newCandidateGroup");

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
            assertThat(new String(logEntries.get(2).getData())).contains(
                "\"type\":\"candidate\"",
                "\"groupId\":\"newCandidateGroup\""
            );
        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logDeleteCandidateUser(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(processInstance);
        assertNotNull(task);
        taskService.addCandidateUser(task.getId(), "newCandidateUser");

        try {
            taskService.deleteCandidateUser(task.getId(), "newCandidateUser");
            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
            assertThat(new String(logEntries.get(2).getData())).contains(
                "\"type\":\"candidate\"",
                "\"userId\":\"newCandidateUser\""
            );
        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskIdentityLinksTest.testCustomIdentityLink.bpmn20.xml")
    public void logIdentityLinkEventsForProcessIdentityLinks(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        runtimeService.startProcessInstanceByKey("customIdentityLink");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskInvolvedUser("kermit").list();
        assertThat(tasks).size().isEqualTo(1);
        task = tasks.get(0);
        List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        // create, identityLinkAdded, identityLinkAdded
        assertThat(logEntries).size().isEqualTo(3);

        assertThat(logEntries.get(1)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
        assertThat(new String(logEntries.get(1).getData())).contains(
            "\"type\":\"businessAdministrator\"",
            "\"userId\":\"kermit\""
        );
        assertThat(logEntries.get(2)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
        assertThat(new String(logEntries.get(2).getData())).contains(
            "\"type\":\"businessAdministrator\"",
            "\"groupId\":\"management\""
        );

        taskService.complete(tasks.get(0).getId());
        logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        // + completed event. Do not expect identity link removed events
        assertThat(logEntries).size().isEqualTo(4);
        assertThat(logEntries.get(3)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_COMPLETED");
    }

    @Test
    public void queryForTaskLogEntriesByTasKId(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();

        try {
            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries.size()).isEqualTo(1);
            assertThat(logEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());

            assertThat(
                historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()
            ).isEqualTo(1l);
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByUserId(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().userId("testUser"),
            historyService.createHistoricTaskLogEntryQuery().userId("testUser")
        );
    }

    protected void assertThatTaskLogIsFetched(TaskService taskService, HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder, HistoricTaskLogEntryQuery historicTaskLogEntryQuery) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).add();
        historicTaskLogEntryBuilder.taskId(task.getId()).add();
        historicTaskLogEntryBuilder.taskId(task.getId()).add();

        try {
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
            assertThat(logEntries.size()).isEqualTo(3);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(task.getId(), task.getId(), task.getId());

            assertThat(
                historicTaskLogEntryQuery.count()
            ).isEqualTo(3l);

            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByType(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().type("testType"),
            historyService.createHistoricTaskLogEntryQuery().type("testType")
        );
    }

    @Test
    public void queryForTaskLogEntriesByProcessInstanceId(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().processInstanceId("testProcess"),
            historyService.createHistoricTaskLogEntryQuery().processInstanceId("testProcess")
        );
    }

    @Test
    public void queryForTaskLogEntriesByScopeId(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().scopeId("testScopeId"),
            historyService.createHistoricTaskLogEntryQuery().scopeId("testScopeId")
        );
    }

    @Test
    public void queryForTaskLogEntriesBySubScopeId(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().subScopeId("testSubScopeId"),
            historyService.createHistoricTaskLogEntryQuery().subScopeId("testSubScopeId")
        );
    }

    @Test
    public void queryForTaskLogEntriesByScopeType(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().scopeType("testScopeType"),
            historyService.createHistoricTaskLogEntryQuery().scopeType("testScopeType")
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromTimeStamp(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(new Date(Long.MAX_VALUE / 2)),
            historyService.createHistoricTaskLogEntryQuery().from(new Date(Long.MAX_VALUE / 2 - 1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromIncludedTimeStamp(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(new Date(Long.MAX_VALUE / 2)),
            historyService.createHistoricTaskLogEntryQuery().from(new Date(Long.MAX_VALUE / 2))
        );
    }

    @Test
    public void queryForTaskLogEntriesByToTimeStamp(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(new Date(0)),
            historyService.createHistoricTaskLogEntryQuery().to(new Date(1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByToIncludedTimeStamp(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(new Date(0)),
            historyService.createHistoricTaskLogEntryQuery().to(new Date(1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromToTimeStamp(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(new Date(0)),
            historyService.createHistoricTaskLogEntryQuery().from(new Date(-1)).to(new Date(1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByTenantId(TaskService taskService, HistoryService historyService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(new Date(0)),
            historyService.createHistoricTaskLogEntryQuery().from(new Date(-1)).to(new Date(1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByLogNumber(TaskService taskService, HistoryService historyService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId(task.getId()).add();
        historicTaskLogEntryBuilder.taskId(task.getId()).add();
        historicTaskLogEntryBuilder.taskId(task.getId()).add();

        List<HistoricTaskLogEntry> allLogEntries = historyService.createHistoricTaskLogEntryQuery().list();

        try {
            HistoricTaskLogEntryQuery historicTaskLogEntryQuery = historyService.createHistoricTaskLogEntryQuery().
                fromLogNumber(allLogEntries.get(1).getLogNumber()).
                toLogNumber(allLogEntries.get(allLogEntries.size() - 2).getLogNumber());
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.
                list();
            assertThat(logEntries.size()).isEqualTo(3);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(anotherTask.getId(), task.getId(), task.getId());

            assertThat(
                historicTaskLogEntryQuery.count()
            ).isEqualTo(3l);

            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByNativeQuery(TaskService taskService, HistoryService historyService, ManagementService managementService) {
        assertEquals("ACT_HI_TSK_LOG", managementService.getTableName(HistoricTaskLogEntryEntity.class));
        assertEquals("ACT_HI_TSK_LOG", managementService.getTableName(HistoricTaskLogEntry.class));
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId("1").add();
        historicTaskLogEntryBuilder.taskId("2").add();
        historicTaskLogEntryBuilder.taskId("3").add();

        try {
            assertEquals(3,
                historyService.createNativeHistoricTaskLogEntryQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricTaskLogEntry.class)).list().size());
            assertEquals(3,
                historyService.createNativeHistoricTaskLogEntryQuery().sql("SELECT count(*) FROM " + managementService.getTableName(HistoricTaskLogEntry.class)).count());

            assertEquals(1, historyService.createNativeHistoricTaskLogEntryQuery().parameter("taskId", "1").
                sql("SELECT count(*) FROM " + managementService.getTableName(HistoricTaskLogEntry.class) + " WHERE TASK_ID_ = #{taskId}").list().size());
            assertEquals(1, historyService.createNativeHistoricTaskLogEntryQuery().parameter("taskId", "1").
                sql("SELECT count(*) FROM " + managementService.getTableName(HistoricTaskLogEntry.class) + " WHERE TASK_ID_ = #{taskId}").count());
        } finally {
            historyService.createHistoricTaskLogEntryQuery().list().
                forEach(
                    logEntry -> historyService.deleteHistoricTaskLogEntry(logEntry.getLogNumber())
                );
        }
    }

    @Test
    public void queryForTaskLogOrderBy(TaskService taskService, HistoryService historyService, ManagementService managementService) {
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId("1").timeStamp(new Date(0)).add();
        historicTaskLogEntryBuilder.taskId("2").timeStamp(new Date(2)).add();
        historicTaskLogEntryBuilder.taskId("3").timeStamp(new Date(1)).add();

        try {

            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().list();
            assertThat(taskLogEntries).extracting(taskLogEntry -> taskLogEntry.getTaskId()).containsExactly("1", "2", "3");

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().orderByLogNumber().desc().list();
            assertThat(taskLogEntries).extracting(taskLogEntry -> taskLogEntry.getTaskId()).containsExactly("3", "2", "1");

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().orderByTimeStamp().desc().list();
            assertThat(taskLogEntries).extracting(taskLogEntry -> taskLogEntry.getTaskId()).containsExactly("2", "3", "1");

        } finally {
            historyService.createHistoricTaskLogEntryQuery().list().
                forEach(
                    logEntry -> historyService.deleteHistoricTaskLogEntry(logEntry.getLogNumber())
                );
        }
    }

}
