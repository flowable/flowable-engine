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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.FlowableTest;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskLogEntry;
import org.flowable.task.api.TaskLogEntryBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
@FlowableTest
@ConfigurationResource("flowable.usertask-log.cfg.xml")
public class TaskServiceEventTest {

    protected Task task;

    @AfterEach
    public void deleteTasks(TaskService taskService, HistoryService historyService) {
        if (task != null) {
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    protected void deleteTaskWithLogEntries(TaskService taskService, String taskId) {
        taskService.getTaskLogEntriesByTaskInstanceId(taskId).
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

        List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());
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

            List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());
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

        List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.getTaskLogEntriesByTaskInstanceId("NON-EXISTING-TASK-ID");

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
            List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.getTaskLogEntriesByTaskInstanceId(null);

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
        List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());
        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(1);

        taskService.deleteTaskLogEntry(taskLogsByTaskInstanceId.get(0).getLogNumber());

        taskLogsByTaskInstanceId = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());
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

        assertThat(taskService.getTaskLogEntriesByTaskInstanceId(task.getId())).size().isEqualTo(1);
    }

    @Test
    public void taskAssigneeEvent(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.setAssignee(task.getId(), "newAssignee");
        List<TaskLogEntry> taskLogEntries = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());

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
        List<TaskLogEntry> taskLogEntries = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());

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

        List<TaskLogEntry> taskLogEntries = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());
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

        List<TaskLogEntry> taskLogEntries = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());
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
        List<TaskLogEntry> taskLogEntries = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());

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

            List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());
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
        List<TaskLogEntry> taskLogEntries = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());

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

        task.setAssignee("newAssignee");
        task.setOwner("newOwner");
        task.setPriority(Integer.MAX_VALUE);
        task.setDueDate(new Date(0));
        taskService.saveTask(task);

        List<TaskLogEntry> taskLogEntries = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());
        assertThat(taskLogEntries).size().isEqualTo(5);
    }

    @Test
    public void changeDueDateEventAsAuthenticatedUser(TaskService taskService) {
        assertThatAuthenticatedUserIsSet(taskService,
            taskId ->  taskService.setDueDate(taskId, new Date(0))
        );
    }

    @Test
    public void createCustomTaskEventLog(TaskService taskService) {
        TaskLogEntryBuilder taskLogEntryBuilder = taskService.createTaskLogEntryBuilder();
        taskLogEntryBuilder.
            taskId("testTaskId").
            timeStamp(new Date(0)).
            userId("testUser").
            type("customType").
            data("testData".getBytes()).
            add();

        List<TaskLogEntry> logEntries = taskService.getTaskLogEntriesByTaskInstanceId("testTaskId");

        MatcherAssert.assertThat(logEntries.size(), is(1));
        TaskLogEntry taskLogEntry = logEntries.get(0);
        assertThat(taskLogEntry.getLogNumber()).isNotNull();
        assertThat(taskLogEntry.getUserId()).isEqualTo("testUser");
        assertThat(taskLogEntry.getTaskId()).isEqualTo("testTaskId");
        assertThat(taskLogEntry.getType()).isEqualTo("customType");
        assertThat(taskLogEntry.getTimeStamp()).isEqualTo(new Date(0));
        assertThat(taskLogEntry.getData()).isEqualTo("testData".getBytes());
        taskService.deleteTaskLogEntry(logEntries.get(0).getLogNumber());
    }

    @Test
    public void createCustomTaskEventLog_taskIdIsEnoughToCreateTaskLogEntry(TaskService taskService) {
        TaskLogEntryBuilder taskLogEntryBuilder = taskService.createTaskLogEntryBuilder();
        taskLogEntryBuilder.
            taskId("testTaskId").
            add();
            List<TaskLogEntry> logEntries = taskService.getTaskLogEntriesByTaskInstanceId("testTaskId");

            MatcherAssert.assertThat(logEntries.size(), is(1));
            TaskLogEntry taskLogEntry = logEntries.get(0);
            assertThat(taskLogEntry.getLogNumber()).isNotNull();
            assertThat(taskLogEntry.getUserId()).isNull();
            assertThat(taskLogEntry.getTaskId()).isEqualTo("testTaskId");
            assertThat(taskLogEntry.getType()).isNull();
            assertThat(taskLogEntry.getTimeStamp()).isNotNull();
            assertThat(taskLogEntry.getData()).isNull();
            taskService.deleteTaskLogEntry(logEntries.get(0).getLogNumber());
    }

    @Test
    public void createCustomTaskEventLog_withoutTaskId_throwsException(TaskService taskService) {
        TaskLogEntryBuilder taskLogEntryBuilder = taskService.createTaskLogEntryBuilder();

        assertThatThrownBy(
            () -> taskLogEntryBuilder.
            timeStamp(new Date(0)).
            userId("testUser").
            type("customType").
            data("testData".getBytes()).
            add()
        ).
            hasMessage("Empty taskId is not allowed for TaskLogEntry").
            isInstanceOf(FlowableException.class);
    }

}
