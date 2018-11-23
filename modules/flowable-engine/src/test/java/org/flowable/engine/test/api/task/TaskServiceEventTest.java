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

import java.util.List;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.FlowableTest;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskLogEntry;
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
    public void deleteTasks(TaskService taskService) {
        if (task != null) {
            taskService.getTaskLogEntriesByTaskInstanceId(task.getId()).
                forEach(
                    logEntry -> taskService.deleteTaskLogEntry(logEntry.getLogNumber())
                );
            taskService.deleteTask(task.getId());
        }
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
        taskService.createTaskBuilder().
            create();
        taskService.createTaskBuilder().
            create();
        taskService.createTaskBuilder().
            create();

        List<TaskLogEntry> taskLogsByTaskInstanceId = taskService.getTaskLogEntriesByTaskInstanceId(null);

        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(3L);
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
            isEqualTo("{\"newAssigneeId\":\"newAssignee\"}");
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void changeAssigneeTaskEventAsAuthenticatedUser(TaskService taskService) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            taskService.setAssignee(task.getId(), "newAssignee");

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
    public void taskOwnerEvent(TaskService taskService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.setOwner(task.getId(), "newOwner");
        List<TaskLogEntry> taskLogEntries = taskService.getTaskLogEntriesByTaskInstanceId(task.getId());

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1)).
            extracting(assigneeTaskLogEntry -> new String(assigneeTaskLogEntry.getData())).
            isEqualTo("{\"newOwnerId\":\"newOwner\"}");
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(TaskLogEntry::getType).isEqualTo("USER_TASK_OWNER_CHANGED");
    }

    @Test
    public void changeOwnerTaskEventAsAuthenticatedUser(TaskService taskService) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            taskService.setOwner(task.getId(), "newOwner");

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

}
