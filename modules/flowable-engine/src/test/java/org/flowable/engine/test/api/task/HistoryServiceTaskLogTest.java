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
package org.flowable.engine.test.api.task;

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
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskLogEntry;
import org.flowable.task.api.TaskLogEntryBuilder;
import org.flowable.task.api.TaskLogEntryQuery;
import org.flowable.task.service.impl.persistence.entity.TaskLogEntryEntity;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
@FlowableTest
@ConfigurationResource("flowable.usertask-log.cfg.xml")
public class HistoryServiceTaskLogTest {

    protected Task task;

    @AfterEach
    public void deleteTasks(TaskService taskService, HistoryService historyService) {
        if (task != null) {
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    protected void deleteTaskWithLogEntries(TaskService taskService, String taskId) {
        taskService.createTaskLogEntryQuery().taskId(taskId).list().
            forEach(
                logEntry -> taskService.deleteTaskLogEntry(logEntry.getLogNumber())
            );
        taskService.deleteTask(taskId);
    }

    @Test
    public void createTaskEvent(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();

        List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(1);

        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_CREATED");
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(TaskLogEntry::getTimeStamp).isEqualTo(task.getCreateTime());
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(TaskLogEntry::getUserId).isNull();
    }

    @Test
    public void createTaskEventAsAuthenticatedUser(TaskService taskService) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();

            List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(
                taskLogsByTaskInstanceId
            ).size().isEqualTo(1);

            assertThat(taskLogsByTaskInstanceId.get(0)).
                extracting(TaskLogEntry::getUserId).isEqualTo("testUser");
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void queryForNonExistingTaskLogEntries(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();

        List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.createTaskLogEntryQuery().taskId("NON-EXISTING-TASK-ID").list();

        assertThat(
            taskLogsByTaskInstanceId
        ).isEmpty();
    }

    @Test
    public void queryForNullTaskLogEntries_returnsAll(TaskService taskService) {
        Task taskA = taskService.createTaskBuilder().
            create();
        Task taskB = taskService.createTaskBuilder().
            create();
        Task taskC = taskService.createTaskBuilder().
            create();

        try {
            List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.createTaskLogEntryQuery().taskId(null).list();

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
    public void deleteTaskEventLogEntry(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(1);

        taskService.deleteTaskLogEntry(taskLogsByTaskInstanceId.get(0).getLogNumber());

        taskLogsByTaskInstanceId = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).isEmpty();
    }

    @Test
    public void deleteNonExistingTaskEventLogEntry(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();
        // non existing log entry delete should be successful
        taskService.deleteTaskLogEntry(Long.MIN_VALUE);

        assertThat(taskService.createTaskLogEntryQuery().taskId(task.getId()).list().size()).isEqualTo(1);
    }

    @Test
    public void taskAssigneeEvent(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.setAssignee(task.getId(), "newAssignee");
        List<TaskLogEntry> taskLogEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1)).
            extracting(assigneeTaskLogEntry -> new String(assigneeTaskLogEntry.getData())).
            isEqualTo("{\"newAssigneeId\":\"newAssignee\",\"previousAssigneeId\":\"initialAssignee\"}");
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void changeAssigneeTaskEventAsAuthenticatedUser(TaskService taskService) {
        assertThatAuthenticatedUserIsSet(taskService,
            taskId -> taskService.setAssignee(taskId, "newAssignee")
        );
    }

    @Test
    public void taskOwnerEvent(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.setOwner(task.getId(), "newOwner");
        List<TaskLogEntry> taskLogEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1)).
            extracting(assigneeTaskLogEntry -> new String(assigneeTaskLogEntry.getData())).
            isEqualTo("{\"previousOwnerId\":null,\"newOwnerId\":\"newOwner\"}");
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_OWNER_CHANGED");
    }

    @Test
    public void changeOwnerTaskEventAsAuthenticatedUser(TaskService taskService) {
        assertThatAuthenticatedUserIsSet(taskService,
            taskId -> taskService.setOwner(taskId, "newOwner")
        );
    }

    @Test
    public void claimTaskEvent(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();

        taskService.claim(task.getId(), "testUser");

        List<TaskLogEntry> taskLogEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1)).
            extracting(assigneeTaskLogEntry -> new String(assigneeTaskLogEntry.getData())).
            isEqualTo("{\"newAssigneeId\":\"testUser\",\"previousAssigneeId\":null}");
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void unClaimTaskEvent(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.unclaim(task.getId());

        List<TaskLogEntry> taskLogEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1)).
            extracting(assigneeTaskLogEntry -> new String(assigneeTaskLogEntry.getData())).
            isEqualTo("{\"newAssigneeId\":null,\"previousAssigneeId\":\"initialAssignee\"}");
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void changePriority(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();

        taskService.setPriority(task.getId(), Integer.MAX_VALUE);
        List<TaskLogEntry> taskLogEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1)).
            extracting(assigneeTaskLogEntry -> new String(assigneeTaskLogEntry.getData())).
            isEqualTo("{\"newPriority\":2147483647,\"previousPriority\":50}");
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_PRIORITY_CHANGED");
    }

    @Test
    public void changePriorityEventAsAuthenticatedUser(TaskService taskService) {
        assertThatAuthenticatedUserIsSet(taskService,
            taskId ->  taskService.setPriority(taskId, Integer.MAX_VALUE)
        );
    }

    protected void assertThatAuthenticatedUserIsSet(TaskService taskService, Consumer<String> functionToAssert) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            functionToAssert.accept(task.getId());

            List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(
                taskLogsByTaskInstanceId
            ).size().isEqualTo(2);

            assertThat(taskLogsByTaskInstanceId.get(1)).
                extracting(TaskLogEntry::getUserId).isEqualTo("testUser");
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void changeDueDate(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();

        taskService.setDueDate(task.getId(), new Date(0));
        List<TaskLogEntry> taskLogEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1)).
            extracting(assigneeTaskLogEntry -> new String(assigneeTaskLogEntry.getData())).
            isEqualTo("{\"newDueDate\":0,\"previousDueDate\":null}");
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_DUEDATE_CHANGED");
    }

    @Test
    public void saveTask(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();

        task.setName("newTaskName");
        task.setAssignee("newAssignee");
        task.setOwner("newOwner");
        task.setPriority(Integer.MAX_VALUE);
        task.setDueDate(new Date(0));
        taskService.saveTask(task);

        List<TaskLogEntry> taskLogEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(6);
    }

    @Test
    public void changeDueDateEventAsAuthenticatedUser(TaskService taskService) {
        assertThatAuthenticatedUserIsSet(taskService,
            taskId ->  taskService.setDueDate(taskId, new Date(0))
        );
    }

    @Test
    public void createCustomTaskEventLog(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();
        TaskLogEntryBuilder taskLogEntryBuilder = taskService.createTaskLogEntryBuilder(task);
        taskLogEntryBuilder.
            timeStamp(new Date(0)).
            userId("testUser").
            type("customType").
            data("testData".getBytes()).
            add();

        List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        TaskLogEntry taskLogEntry = logEntries.get(1);
        assertThat(taskLogEntry.getLogNumber()).isNotNull();
        assertThat(taskLogEntry.getUserId()).isEqualTo("testUser");
        assertThat(taskLogEntry.getTaskId()).isEqualTo(task.getId());
        assertThat(taskLogEntry.getType()).isEqualTo("customType");
        assertThat(taskLogEntry.getTimeStamp()).isEqualTo(new Date(0));
        assertThat(taskLogEntry.getData()).isEqualTo("testData".getBytes());
        taskService.deleteTaskLogEntry(logEntries.get(0).getLogNumber());
    }

    @Test
    public void createCustomTaskEventLog_taskIdIsEnoughToCreateTaskLogEntry(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();

        TaskLogEntryBuilder taskLogEntryBuilder = taskService.createTaskLogEntryBuilder(task);
        taskLogEntryBuilder.
            add();
        List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        TaskLogEntry taskLogEntry = logEntries.get(1);
        assertThat(taskLogEntry.getLogNumber()).isNotNull();
        assertThat(taskLogEntry.getUserId()).isNull();
        assertThat(taskLogEntry.getTaskId()).isEqualTo(task.getId());
        assertThat(taskLogEntry.getType()).isNull();
        assertThat(taskLogEntry.getTimeStamp()).isNotNull();
        assertThat(taskLogEntry.getData()).isNull();
    }

    @Test
    public void createCustomTaskEventLog_withoutTimeStamp_addsDefault(TaskService taskService) {
        task = taskService.createTaskBuilder().
            create();

        TaskLogEntryBuilder taskLogEntryBuilder = taskService.createTaskLogEntryBuilder(task);

        taskLogEntryBuilder.
            userId("testUser").
            type("customType").
            data("testData".getBytes()).
            add();

        List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        TaskLogEntry taskLogEntry = logEntries.get(1);
        assertThat(taskLogEntry.getLogNumber()).isNotNull();
        assertThat(taskLogEntry.getTimeStamp()).isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logSuspensionStateEvents(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        try {
            runtimeService.suspendProcessInstanceById(processInstance.getId());
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertNotNull(task);

            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_SUSPENSIONSTATE_CHANGED")
            ;

            runtimeService.activateProcessInstanceById(processInstance.getId());

            logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_SUSPENSIONSTATE_CHANGED")
            ;
        } finally {
            taskService.createTaskLogEntryQuery().taskId(
                taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId()
            ).list().
                forEach(
                    logEntry -> taskService.deleteTaskLogEntry(logEntry.getLogNumber())
                );
            runtimeService.deleteProcessInstance(processInstance.getId(), "clean up");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logProcessTaskEvents(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        try {
            taskService.setAssignee(task.getId(), "newAssignee");
            taskService.setOwner(task.getId(), "newOwner");
            taskService.complete(task.getId());

            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(4);
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_CREATED")
            ;
            assertThat(logEntries.get(1)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_ASSIGNEE_CHANGED")
            ;
            assertThat(logEntries.get(2)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_OWNER_CHANGED")
            ;
            assertThat(logEntries.get(3)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("TASK_COMPLETED")
            ;
        } finally {
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddCandidateUser(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        try {
            assertNotNull(processInstance);
            assertNotNull(task);

            taskService.addCandidateUser(task.getId(), "newCandidateUser");

            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
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
    public void logAddParticipantUser(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        try {
            assertNotNull(processInstance);
            assertNotNull(task);

            taskService.addUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.PARTICIPANT);

            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
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
    public void logAddCandidateGroup(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        try {

            taskService.addCandidateGroup(task.getId(), "newCandidateGroup");

            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
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
    public void logAddGroup(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        try {

            taskService.addGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.PARTICIPANT);

            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
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
    public void logDeleteCandidateGroup(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        taskService.addCandidateGroup(task.getId(), "newCandidateGroup");
        try {

            taskService.deleteCandidateGroup(task.getId(), "newCandidateGroup");

            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
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
    public void logDeleteCandidateUser(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(processInstance);
        assertNotNull(task);
        taskService.addCandidateUser(task.getId(), "newCandidateUser");

        try {
            taskService.deleteCandidateUser(task.getId(), "newCandidateUser");
            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
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
    public void logIdentityLinkEventsForProcessIdentityLinks(RuntimeService runtimeService, TaskService taskService) {
        runtimeService.startProcessInstanceByKey("customIdentityLink");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskInvolvedUser("kermit").list();
        assertThat(tasks).size().isEqualTo(1);
        task = tasks.get(0);
        List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
        // create, identityLinkAdded, identityLinkAdded
        assertThat(logEntries).size().isEqualTo(3);

        assertThat(logEntries.get(1)).
            extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
        assertThat(new String(logEntries.get(1).getData())).contains(
            "\"type\":\"businessAdministrator\"",
            "\"userId\":\"kermit\""
        );
        assertThat(logEntries.get(2)).
            extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
        assertThat(new String(logEntries.get(2).getData())).contains(
            "\"type\":\"businessAdministrator\"",
            "\"groupId\":\"management\""
        );

        taskService.complete(tasks.get(0).getId());
        logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
        // + completed event. Do not expect identity link removed events
        assertThat(logEntries).size().isEqualTo(4);
        assertThat(logEntries.get(3)).
            extracting(TaskLogEntry::getType).isEqualTo("TASK_COMPLETED");
    }

    @Test
    public void queryForTaskLogEntriesByTasKId(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();

        try {
            List<TaskLogEntry> logEntries = taskService.createTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries.size()).isEqualTo(1);
            assertThat(logEntries.get(0)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());

            assertThat(
                taskService.createTaskLogEntryQuery().taskId(task.getId()).count()
            ).isEqualTo(1l);
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByUserId(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().userId("testUser"),
            taskService.createTaskLogEntryQuery().userId("testUser")
        );
    }

    protected void assertThatTaskLogIsFetched(TaskService taskService, TaskLogEntryBuilder taskLogEntryBuilder, TaskLogEntryQuery taskLogEntryQuery) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();
        taskLogEntryBuilder.taskId(task.getId()).add();
        taskLogEntryBuilder.taskId(task.getId()).add();
        taskLogEntryBuilder.taskId(task.getId()).add();

        try {
            List<TaskLogEntry> logEntries = taskLogEntryQuery.list();
            assertThat(logEntries.size()).isEqualTo(3);
            assertThat(logEntries).extracting(TaskLogEntry::getTaskId).containsExactly(task.getId(), task.getId(), task.getId());

            assertThat(
                taskLogEntryQuery.count()
            ).isEqualTo(3l);

            List<TaskLogEntry> pagedLogEntries = taskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByType(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().type("testType"),
            taskService.createTaskLogEntryQuery().type("testType")
        );
    }

    @Test
    public void queryForTaskLogEntriesByProcessInstanceId(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().processInstanceId("testProcess"),
            taskService.createTaskLogEntryQuery().processInstanceId("testProcess")
        );
    }

    @Test
    public void queryForTaskLogEntriesByScopeId(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().scopeId("testScopeId"),
            taskService.createTaskLogEntryQuery().scopeId("testScopeId")
        );
    }

    @Test
    public void queryForTaskLogEntriesBySubScopeId(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().subScopeId("testSubScopeId"),
            taskService.createTaskLogEntryQuery().subScopeId("testSubScopeId")
        );
    }

    @Test
    public void queryForTaskLogEntriesByScopeType(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().scopeType("testScopeType"),
            taskService.createTaskLogEntryQuery().scopeType("testScopeType")
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromTimeStamp(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().timeStamp(new Date(Long.MAX_VALUE / 2)),
            taskService.createTaskLogEntryQuery().from(new Date(Long.MAX_VALUE / 2 - 1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromIncludedTimeStamp(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().timeStamp(new Date(Long.MAX_VALUE / 2)),
            taskService.createTaskLogEntryQuery().from(new Date(Long.MAX_VALUE / 2))
        );
    }

    @Test
    public void queryForTaskLogEntriesByToTimeStamp(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().timeStamp(new Date(0)),
            taskService.createTaskLogEntryQuery().to(new Date(1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByToIncludedTimeStamp(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().timeStamp(new Date(0)),
            taskService.createTaskLogEntryQuery().to(new Date(1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromToTimeStamp(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().timeStamp(new Date(0)),
            taskService.createTaskLogEntryQuery().from(new Date(-1)).to(new Date(1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByTenantId(TaskService taskService) {
        assertThatTaskLogIsFetched(taskService,
            taskService.createTaskLogEntryBuilder().timeStamp(new Date(0)),
            taskService.createTaskLogEntryQuery().from(new Date(-1)).to(new Date(1))
        );
    }

    @Test
    public void queryForTaskLogEntriesByLogNumber(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();
        TaskLogEntryBuilder taskLogEntryBuilder = taskService.createTaskLogEntryBuilder();
        taskLogEntryBuilder.taskId(task.getId()).add();
        taskLogEntryBuilder.taskId(task.getId()).add();
        taskLogEntryBuilder.taskId(task.getId()).add();

        List<TaskLogEntry> allLogEntries = taskService.createTaskLogEntryQuery().list();

        try {
            TaskLogEntryQuery taskLogEntryQuery = taskService.createTaskLogEntryQuery().
                fromLogNumber(allLogEntries.get(1).getLogNumber()).
                toLogNumber(allLogEntries.get(allLogEntries.size() - 2).getLogNumber());
            List<TaskLogEntry> logEntries = taskLogEntryQuery.
                list();
            assertThat(logEntries.size()).isEqualTo(3);
            assertThat(logEntries).extracting(TaskLogEntry::getTaskId).containsExactly(anotherTask.getId(), task.getId(), task.getId());

            assertThat(
                taskLogEntryQuery.count()
            ).isEqualTo(3l);

            List<TaskLogEntry> pagedLogEntries = taskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByNativeQuery(TaskService taskService, ManagementService managementService) {
        assertEquals("FLW_HI_TSK_LOG", managementService.getTableName(TaskLogEntryEntity.class));
        assertEquals("FLW_HI_TSK_LOG", managementService.getTableName(TaskLogEntry.class));
        TaskLogEntryBuilder taskLogEntryBuilder = taskService.createTaskLogEntryBuilder();
        taskLogEntryBuilder.taskId("1").add();
        taskLogEntryBuilder.taskId("2").add();
        taskLogEntryBuilder.taskId("3").add();

        try {
            assertEquals(3,
                taskService.createNativeTaskLogEntryQuery().sql("SELECT * FROM " + managementService.getTableName(TaskLogEntry.class)).list().size());
            assertEquals(3,
                taskService.createNativeTaskLogEntryQuery().sql("SELECT count(*) FROM " + managementService.getTableName(TaskLogEntry.class)).count());

            assertEquals(1, taskService.createNativeTaskLogEntryQuery().parameter("taskId", "1").
                sql("SELECT count(*) FROM " + managementService.getTableName(TaskLogEntry.class) + " WHERE TASK_ID_ = #{taskId}").list().size());
            assertEquals(1, taskService.createNativeTaskLogEntryQuery().parameter("taskId", "1").
                sql("SELECT count(*) FROM " + managementService.getTableName(TaskLogEntry.class) + " WHERE TASK_ID_ = #{taskId}").count());
        } finally {
            taskService.createTaskLogEntryQuery().list().
                forEach(
                    logEntry -> taskService.deleteTaskLogEntry(logEntry.getLogNumber())
                );
        }
    }
}
