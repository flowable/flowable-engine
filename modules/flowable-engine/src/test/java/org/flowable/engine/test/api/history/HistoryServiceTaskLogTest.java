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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.util.CommandContextUtil;
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
    public void deleteTasks(TaskService taskService) {
        if (task != null) {
            deleteTaskWithLogEntries(taskService, task.getId());
            taskService.deleteTask(task.getId());
        }
    }

    protected void deleteTaskWithLogEntries(TaskService taskService, String taskId) {
        taskService.deleteTask(taskId, true);
    }

    @Test
    public void createTaskEvent(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();

        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(1);

        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_CREATED");
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getUserId).isNull();

        taskService.deleteTask(task.getId());

        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isGreaterThan(0L);
    }

    @Test
    public void createTaskEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

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
    public void queryForNonExistingTaskLogEntries(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId("NON-EXISTING-TASK-ID").list();

        assertThat(
            taskLogsByTaskInstanceId
        ).isEmpty();
    }

    @Test
    public void queryForNullTaskLogEntries_returnsAll(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        Task taskA = taskService.createTaskBuilder().create();
        Task taskB = taskService.createTaskBuilder().create();
        Task taskC = taskService.createTaskBuilder().create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        try {
            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(null).list();

            assertThat(taskLogsByTaskInstanceId).size().isEqualTo(3L);
        } finally {
            deleteTaskWithLogEntries(taskService, taskC.getId());
            deleteTaskWithLogEntries(taskService, taskB.getId());
            deleteTaskWithLogEntries(taskService, taskA.getId());
        }
    }

    @Test
    public void deleteTaskEventLogEntry(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(1);

        historyService.deleteHistoricTaskLogEntry(taskLogsByTaskInstanceId.get(0).getLogNumber());
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).isEmpty();
    }

    @Test
    public void deleteNonExistingTaskEventLogEntry(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        // non existing log entry delete should be successful
        historyService.deleteHistoricTaskLogEntry(Long.MIN_VALUE);

        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list().size()).isEqualTo(1);
    }

    @Test
    public void taskAssigneeEvent(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.setAssignee(task.getId(), "newAssignee");
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).contains("\"newAssigneeId\":\"newAssignee\"","\"previousAssigneeId\":\"initialAssignee\"");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void changeAssigneeTaskEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatAuthenticatedUserIsSet(taskService,
            historyService, processEngineConfiguration, managementService, taskId -> taskService.setAssignee(taskId, "newAssignee")
        );
    }

    @Test
    public void taskOwnerEvent(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.setOwner(task.getId(), "newOwner");
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

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
    public void changeOwnerTaskEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatAuthenticatedUserIsSet(taskService,
            historyService, processEngineConfiguration, managementService, taskId -> taskService.setOwner(taskId, "newOwner")
        );
    }

    @Test
    public void claimTaskEvent(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        taskService.claim(task.getId(), "testUser");
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).contains("\"newAssigneeId\":\"testUser\"","\"previousAssigneeId\":null");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void unClaimTaskEvent(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        taskService.unclaim(task.getId());
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

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
    public void changePriority(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            create();

        taskService.setPriority(task.getId(), Integer.MAX_VALUE);
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

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
    public void changePriorityEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatAuthenticatedUserIsSet(taskService,
            historyService, processEngineConfiguration, managementService, taskId ->  taskService.setPriority(taskId, Integer.MAX_VALUE)
        );
    }

    protected void assertThatAuthenticatedUserIsSet(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService, Consumer<String> functionToAssert) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            functionToAssert.accept(task.getId());
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

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
    public void changeDueDate(TaskService taskService, HistoryService historyService, ProcessEngineConfiguration processEngineConfiguration,
        ManagementService managementService) {
        task = taskService.createTaskBuilder().
            create();

        taskService.setDueDate(task.getId(), new Date());
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).contains("\"newDueDate\"","\"previousDueDate\":null}");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_DUEDATE_CHANGED");
    }

    @Test
    public void saveTask(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService mananagementService) {
        task = taskService.createTaskBuilder().
            create();

        task.setName("newTaskName");
        task.setAssignee("newAssignee");
        task.setOwner("newOwner");
        task.setPriority(Integer.MAX_VALUE);
        task.setDueDate(new Date());
        taskService.saveTask(task);

        processAsyncHistoryIfNecessary(processEngineConfiguration, mananagementService);

        List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).as("The only event is user task created").size().isEqualTo(1);
    }

    @Test
    public void changeDueDateEventAsAuthenticatedUser(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatAuthenticatedUserIsSet(taskService,
            historyService, processEngineConfiguration, managementService, taskId ->  taskService.setDueDate(taskId, new Date())
        );
    }

    @Test
    public void createCustomTaskEventLog(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        Date todayDate = new Date();
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.timeStamp(todayDate);
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.create();

        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
        assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
        assertThat(historicTaskLogEntry.getUserId()).isEqualTo("testUser");
        assertThat(historicTaskLogEntry.getTaskId()).isEqualTo(task.getId());
        assertThat(historicTaskLogEntry.getType()).isEqualTo("customType");
        assertThat(historicTaskLogEntry.getTimeStamp()).isEqualTo(todayDate);
        assertThat(historicTaskLogEntry.getData()).isEqualTo("testData");
        historyService.deleteHistoricTaskLogEntry(logEntries.get(0).getLogNumber());
    }

    @Test
    public void createCustomTaskEventLog_taskIdIsEnoughToCreateTaskLogEntry(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

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
    public void createCustomTaskEventLog_withoutTimeStamp_addsDefault(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
        assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
        assertThat(historicTaskLogEntry.getTimeStamp()).isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logSuspensionStateEvents(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        try {
            runtimeService.suspendProcessInstanceById(processInstance.getId());
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertNotNull(task);
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_SUSPENSIONSTATE_CHANGED")
            ;

            runtimeService.activateProcessInstanceById(processInstance.getId());
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_SUSPENSIONSTATE_CHANGED")
            ;
        } finally {
            String taskId = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
            managementService.executeCommand(commandContext -> {
                CommandContextUtil.getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
                return null;
            });
            runtimeService.deleteProcessInstance(processInstance.getId(), "clean up");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logProcessTaskEvents(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        try {
            taskService.setAssignee(task.getId(), "newAssignee");
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);
            taskService.setOwner(task.getId(), "newOwner");
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);
            taskService.complete(task.getId());
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(4);
            assertThat(logEntries.get(0)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_CREATED")
            ;
            assertThat(logEntries.get(0)).
                extracting(HistoricTaskLogEntry::getProcessDefinitionId).isEqualTo(processInstance.getProcessDefinitionId())
            ;
            assertThat(logEntries.get(0)).
                extracting(HistoricTaskLogEntry::getExecutionId).isEqualTo(task.getExecutionId())
            ;
            assertThat(logEntries.get(0)).
                extracting(HistoricTaskLogEntry::getProcessInstanceId).isEqualTo(processInstance.getId())
            ;
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED")
            ;
            assertThat(logEntries.get(2)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_OWNER_CHANGED")
            ;
            assertThat(logEntries.get(3)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_COMPLETED")
            ;
        } finally {
            deleteTaskWithLogEntries(taskService, task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddCandidateUser(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);
        try {
            assertNotNull(processInstance);
            assertNotNull(task);

            taskService.addCandidateUser(task.getId(), "newCandidateUser");
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(logEntries.get(1).getData()).contains(
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
    public void logAddParticipantUser(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);
        try {
            assertNotNull(processInstance);
            assertNotNull(task);

            taskService.addUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.PARTICIPANT);
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(logEntries.get(1).getData()).contains(
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
    public void logAddCandidateGroup(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        try {

            taskService.addCandidateGroup(task.getId(), "newCandidateGroup");
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(logEntries.get(1).getData()).contains(
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
    public void logAddGroup(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);
        try {

            taskService.addGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.PARTICIPANT);
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(logEntries.get(1).getData()).contains(
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
    public void logDeleteCandidateGroup(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        taskService.addCandidateGroup(task.getId(), "newCandidateGroup");
        try {

            taskService.deleteCandidateGroup(task.getId(), "newCandidateGroup");
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
            assertThat(logEntries.get(2).getData()).contains(
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
    public void logDeleteCandidateUser(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(processInstance);
        assertNotNull(task);
        taskService.addCandidateUser(task.getId(), "newCandidateUser");
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        try {
            taskService.deleteCandidateUser(task.getId(), "newCandidateUser");
            processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
            assertThat(logEntries.get(2).getData()).contains(
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
    public void logIdentityLinkEventsForProcessIdentityLinks(RuntimeService runtimeService, TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        runtimeService.startProcessInstanceByKey("customIdentityLink");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskInvolvedUser("kermit").list();
        assertThat(tasks).size().isEqualTo(1);
        task = tasks.get(0);
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        // create, identityLinkAdded, identityLinkAdded
        assertThat(logEntries).size().isEqualTo(3);

        assertThat(logEntries.get(1)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
        assertThat(logEntries.get(1).getData()).contains(
            "\"type\":\"businessAdministrator\"",
            "\"userId\":\"kermit\""
        );
        assertThat(logEntries.get(2)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
        assertThat(logEntries.get(2).getData()).contains(
            "\"type\":\"businessAdministrator\"",
            "\"groupId\":\"management\""
        );

        taskService.complete(tasks.get(0).getId());
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        // + completed event. Do not expect identity link removed events
        assertThat(logEntries).size().isEqualTo(4);
        assertThat(logEntries.get(3)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_COMPLETED");
    }

    @Test
    public void queryForTaskLogEntriesByTasKId(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        try {
            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries.size()).isEqualTo(1);
            assertThat(logEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());

            assertThat(
                historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()
            ).isEqualTo(1L);
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByUserId(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().userId("testUser"),
            historyService.createHistoricTaskLogEntryQuery().userId("testUser"),
            processEngineConfiguration, managementService);
    }

    protected void assertThatTaskLogIsFetched(TaskService taskService, HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder,
        HistoricTaskLogEntryQuery historicTaskLogEntryQuery, ProcessEngineConfiguration processEngineConfiguration,
        ManagementService managementService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        try {
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
            assertThat(logEntries.size()).isEqualTo(3);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(task.getId(), task.getId(), task.getId());

            assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3L);

            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
            
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
            taskService.deleteTask(anotherTask.getId(), true);
        }
    }

    @Test
    public void queryForTaskLogEntriesByType(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().type("testType"),
            historyService.createHistoricTaskLogEntryQuery().type("testType"),
            processEngineConfiguration, managementService);
    }

    @Test
    public void queryForTaskLogEntriesByProcessInstanceId(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().processInstanceId("testProcess"),
            historyService.createHistoricTaskLogEntryQuery().processInstanceId("testProcess"),
            processEngineConfiguration, managementService);
    }

    @Test
    public void queryForTaskLogEntriesByScopeId(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().scopeId("testScopeId"),
            historyService.createHistoricTaskLogEntryQuery().scopeId("testScopeId"),
            processEngineConfiguration, managementService);
    }

    @Test
    public void queryForTaskLogEntriesBySubScopeId(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().subScopeId("testSubScopeId"),
            historyService.createHistoricTaskLogEntryQuery().subScopeId("testSubScopeId"),
            processEngineConfiguration, managementService);
    }

    @Test
    public void queryForTaskLogEntriesByScopeType(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().scopeType("testScopeType"),
            historyService.createHistoricTaskLogEntryQuery().scopeType("testScopeType"),
            processEngineConfiguration, managementService);
    }

    @Test
    public void queryForTaskLogEntriesByFromTimeStamp(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            historyService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()),
            processEngineConfiguration, managementService);
    }

    @Test
    public void queryForTaskLogEntriesByToTimeStamp(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate());
        HistoricTaskLogEntryQuery historicTaskLogEntryQuery = historyService.createHistoricTaskLogEntryQuery().to(getCompareAfterDate());
        
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        try {
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
            assertThat(logEntries.size()).isEqualTo(5);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(task.getId(), anotherTask.getId(), task.getId(), task.getId(), task.getId());
    
            assertThat(historicTaskLogEntryQuery.count()).isEqualTo(5);
    
            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
            taskService.deleteTask(anotherTask.getId(), true);
        }
    }

    @Test
    public void queryForTaskLogEntriesByFromToTimeStamp(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            historyService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()).to(getCompareAfterDate()),
            processEngineConfiguration, managementService);
    }

    @Test
    public void queryForTaskLogEntriesByTenantId(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertThatTaskLogIsFetched(taskService,
            historyService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            historyService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()).to(getCompareAfterDate()),
            processEngineConfiguration, managementService);
    }

    @Test
    public void queryForTaskLogEntriesByLogNumber(TaskService taskService, HistoryService historyService,
         ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        task = taskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = taskService.createTaskBuilder().create();
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

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
            ).isEqualTo(3L);

            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
        } finally {
            deleteTaskWithLogEntries(taskService, anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByNativeQuery(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        assertEquals("ACT_HI_TSK_LOG", managementService.getTableName(HistoricTaskLogEntryEntity.class));
        assertEquals("ACT_HI_TSK_LOG", managementService.getTableName(HistoricTaskLogEntry.class));
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId("1").create();
        historicTaskLogEntryBuilder.taskId("2").create();
        historicTaskLogEntryBuilder.taskId("3").create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

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
            deleteTaskWithLogEntries(taskService, "1");
            deleteTaskWithLogEntries(taskService, "2");
            deleteTaskWithLogEntries(taskService, "3");
        }
    }

    @Test
    public void queryForTaskLogOrderBy(TaskService taskService, HistoryService historyService,
        ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId("1").timeStamp(getInsertDate()).create();
        historicTaskLogEntryBuilder.taskId("2").timeStamp(getCompareAfterDate()).create();
        historicTaskLogEntryBuilder.taskId("3").timeStamp(getCompareBeforeDate()).create();
        processAsyncHistoryIfNecessary(processEngineConfiguration, managementService);

        try {

            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().list();
            assertThat(taskLogEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly("1", "2", "3");

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().orderByLogNumber().desc().list();
            assertThat(taskLogEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly("3", "2", "1");

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().orderByTimeStamp().desc().list();
            assertThat(taskLogEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly("2", "1", "3");

        } finally {
            deleteTaskWithLogEntries(taskService, "1");
            deleteTaskWithLogEntries(taskService, "2");
            deleteTaskWithLogEntries(taskService, "3");
        }
    }

    protected Date getInsertDate() {
        Calendar cal = new GregorianCalendar(2020, Calendar.APRIL, 10);
        return cal.getTime();
    }
    
    protected Date getCompareBeforeDate() {
        Calendar cal = new GregorianCalendar(2020, Calendar.APRIL, 9);
        return cal.getTime();
    }
    
    protected Date getCompareAfterDate() {
        Calendar cal = new GregorianCalendar(2020, Calendar.APRIL, 11);
        return cal.getTime();
    }

    protected void processAsyncHistoryIfNecessary(ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService) {
        if (((ProcessEngineConfigurationImpl) processEngineConfiguration).isAsyncHistoryEnabled()) {
            HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 14000, 200);
        }
    }

}
