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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.service.HistoryJobService;
import org.flowable.job.service.impl.HistoryJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
import org.flowable.task.service.impl.HistoricTaskLogEntryQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
public class HistoryServiceTaskLogTest extends CustomConfigurationFlowableTestCase {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    protected Task task;

    public HistoryServiceTaskLogTest() {
        super(HistoryServiceTaskLogTest.class.getName());
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setEnableHistoricTaskLogging(true);
    }

    @AfterEach
    public void deleteTasks() {
        if (task != null) {
            deleteTaskWithLogEntries(task.getId());
        }
    }

    protected void deleteTaskWithLogEntries(String taskId) {
        taskService.deleteTask(taskId, true);
        managementService.executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                ProcessEngineConfigurationImpl processEngineConfigurationImpl = (ProcessEngineConfigurationImpl) processEngineConfiguration;
                HistoricTaskLogEntryEntityManager historicTaskLogEntryEntityManager = processEngineConfigurationImpl.getTaskServiceConfiguration()
                        .getHistoricTaskLogEntryEntityManager();
                List<HistoricTaskLogEntry> taskLogEntries = historicTaskLogEntryEntityManager
                        .findHistoricTaskLogEntriesByQueryCriteria(new HistoricTaskLogEntryQueryImpl(processEngineConfiguration.getCommandExecutor(), 
                                processEngineConfigurationImpl.getTaskServiceConfiguration()));
                for (HistoricTaskLogEntry historicTaskLogEntry : taskLogEntries) {
                    historicTaskLogEntryEntityManager.deleteHistoricTaskLogEntry(historicTaskLogEntry.getLogNumber());
                }
                
                HistoryJobService historyJobService = processEngineConfigurationImpl.getJobServiceConfiguration().getHistoryJobService();
                List<HistoryJob> jobs = historyJobService.findHistoryJobsByQueryCriteria(new HistoryJobQueryImpl(commandContext, processEngineConfigurationImpl.getJobServiceConfiguration()));
                for (HistoryJob historyJob : jobs) {
                    historyJobService.deleteHistoryJob((HistoryJobEntity) historyJob);
                }

                return null;
            }
        });
    }

    @Test
    public void createTaskEvent() {
        task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();

        try {
            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(taskLogsByTaskInstanceId).hasSize(1);

                assertThat(taskLogsByTaskInstanceId.get(0)).
                        extracting(HistoricTaskLogEntry::getTaskId)
                        .isEqualTo(task.getId());
                assertThat(taskLogsByTaskInstanceId.get(0)).
                        extracting(HistoricTaskLogEntry::getType)
                        .isEqualTo("USER_TASK_CREATED");
                assertThat(taskLogsByTaskInstanceId.get(0)).
                        extracting(HistoricTaskLogEntry::getTimeStamp)
                        .isNotNull();
                assertThat(taskLogsByTaskInstanceId.get(0)).
                        extracting(HistoricTaskLogEntry::getUserId)
                        .isNull();
            }

        } finally {
            taskService.deleteTask(task.getId());
        }
    }

    @Test
    public void createTaskEventAsAuthenticatedUser() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            task = taskService.createTaskBuilder().
                    assignee("testAssignee").
                    create();

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(taskLogsByTaskInstanceId).hasSize(1);

                assertThat(taskLogsByTaskInstanceId.get(0)).
                        extracting(HistoricTaskLogEntry::getUserId)
                        .isEqualTo("testUser");
            }

        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void queryForNonExistingTaskLogEntries() {
        task = taskService.createTaskBuilder().create();

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId("NON-EXISTING-TASK-ID").list();

        assertThat(taskLogsByTaskInstanceId).isEmpty();
    }

    @Test
    public void queryForNullTaskLogEntries_returnsAll() {

        Task taskA = taskService.createTaskBuilder().create();
        Task taskB = taskService.createTaskBuilder().create();
        Task taskC = taskService.createTaskBuilder().create();

        try {
            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(null).list();
                assertThat(taskLogsByTaskInstanceId).hasSize(3);
            }

        } finally {
            deleteTaskWithLogEntries(taskC.getId());
            deleteTaskWithLogEntries(taskB.getId());
            deleteTaskWithLogEntries(taskA.getId());
        }
    }

    @Test
    public void deleteTaskEventLogEntry() {
        task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogsByTaskInstanceId).hasSize(1);

            historyService.deleteHistoricTaskLogEntry(taskLogsByTaskInstanceId.get(0).getLogNumber());

            HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 5000, 200);
            taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogsByTaskInstanceId).isEmpty();
        }
    }

    @Test
    public void deleteNonExistingTaskEventLogEntry() {
        task = taskService.createTaskBuilder().create();

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            // non existing log entry delete should be successful
            historyService.deleteHistoricTaskLogEntry(Long.MIN_VALUE);

            assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list()).hasSize(1);
        }
    }

    @Test
    public void taskAssigneeEvent() {
        task = taskService.createTaskBuilder().
                assignee("initialAssignee").
                create();

        taskService.setAssignee(task.getId(), "newAssignee");

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).hasSize(2);

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_ASSIGNEE_CHANGED").list();
            assertThat(taskLogEntries).hasSize(1);
            assertThat(taskLogEntries.get(0).getData()).contains("\"newAssigneeId\":\"newAssignee\"", "\"previousAssigneeId\":\"initialAssignee\"");
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getUserId).isNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
        }
    }

    @Test
    public void changeAssigneeTaskEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(taskId -> taskService.setAssignee(taskId, "newAssignee"));
    }

    @Test
    public void taskOwnerEvent() {
        task = taskService.createTaskBuilder().
                assignee("initialAssignee").
                create();

        taskService.setOwner(task.getId(), "newOwner");

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).hasSize(2);

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_OWNER_CHANGED").list();
            assertThat(taskLogEntries).hasSize(1);
            assertThat(taskLogEntries.get(0).getData()).
                    contains("\"previousOwnerId\":null", "\"newOwnerId\":\"newOwner\"");
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getUserId).isNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_OWNER_CHANGED");
        }
    }

    @Test
    public void changeOwnerTaskEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(taskId -> taskService.setOwner(taskId, "newOwner"));
    }

    @Test
    public void claimTaskEvent() {
        task = taskService.createTaskBuilder().create();

        taskService.claim(task.getId(), "testUser");

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).hasSize(2);

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_ASSIGNEE_CHANGED").list();
            assertThat(taskLogEntries).hasSize(1);
            assertThat(taskLogEntries.get(0).getData()).contains("\"newAssigneeId\":\"testUser\"", "\"previousAssigneeId\":null");
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getUserId).isNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
        }
    }

    @Test
    public void unclaimTaskEvent() {
        task = taskService.createTaskBuilder().
                assignee("initialAssignee").
                create();

        taskService.unclaim(task.getId());

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).hasSize(2);

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_ASSIGNEE_CHANGED").list();
            assertThat(taskLogEntries).hasSize(1);
            assertThat(taskLogEntries.get(0).getData()).
                    contains("\"newAssigneeId\":null", "\"previousAssigneeId\":\"initialAssignee\"");
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getUserId).isNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
        }
    }

    @Test
    public void changePriority() {
        task = taskService.createTaskBuilder().create();
        taskService.setPriority(task.getId(), Integer.MAX_VALUE);

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).hasSize(2);

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_PRIORITY_CHANGED").list();
            assertThat(taskLogEntries).hasSize(1);
            assertThat(taskLogEntries.get(0).getData()).
                    contains("\"newPriority\":2147483647", "\"previousPriority\":50}");
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getUserId).isNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_PRIORITY_CHANGED");
        }
    }

    @Test
    public void changePriorityEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(taskId -> taskService.setPriority(taskId, Integer.MAX_VALUE));
    }

    protected void assertThatAuthenticatedUserIsSet(Consumer<String> functionToAssert) {

        String previousUserId = Authentication.getAuthenticatedUserId();
        task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();
        Authentication.setAuthenticatedUserId("testUser");

        try {
            functionToAssert.accept(task.getId());

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(taskLogsByTaskInstanceId).hasSize(2);

                taskLogsByTaskInstanceId = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).userId("testUser").list();
                assertThat(taskLogsByTaskInstanceId).hasSize(1);
            }

        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void changeDueDate() {
        task = taskService.createTaskBuilder().create();

        taskService.setDueDate(task.getId(), new Date());

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).hasSize(2);

            taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_DUEDATE_CHANGED").list();
            assertThat(taskLogEntries).hasSize(1);
            assertThat(taskLogEntries.get(0).getData()).contains("\"newDueDate\"", "\"previousDueDate\":null}");
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getUserId).isNull();
            assertThat(taskLogEntries.get(0)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_DUEDATE_CHANGED");
        }
    }

    @Test
    public void saveTask() {
        task = taskService.createTaskBuilder().
                create();

        task.setName("newTaskName");
        task.setAssignee("newAssignee");
        task.setOwner("newOwner");
        task.setPriority(Integer.MAX_VALUE);
        task.setDueDate(new Date());
        taskService.saveTask(task);

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).as("The only event is user task created").hasSize(1);
        }
    }

    @Test
    public void changeDueDateEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(taskId -> taskService.setDueDate(taskId, new Date()));
    }

    @Test
    public void createCustomTaskEventLog() {
        task = taskService.createTaskBuilder().create();

        Date todayDate = new Date();
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.timeStamp(todayDate);
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.create();

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).hasSize(2);

            logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("customType").list();
            assertThat(logEntries).hasSize(1);
            HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(0);
            assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
            assertThat(historicTaskLogEntry.getUserId()).isEqualTo("testUser");
            assertThat(historicTaskLogEntry.getTaskId()).isEqualTo(task.getId());
            assertThat(historicTaskLogEntry.getType()).isEqualTo("customType");
            assertThat(simpleDateFormat.format(historicTaskLogEntry.getTimeStamp())).isEqualTo(simpleDateFormat.format(todayDate));
            assertThat(historicTaskLogEntry.getData()).isEqualTo("testData");
            historyService.deleteHistoricTaskLogEntry(historicTaskLogEntry.getLogNumber());
        }
    }

    @Test
    public void createCustomTaskEventLog_taskIdIsEnoughToCreateTaskLogEntry() {
        task = taskService.createTaskBuilder().create();

        waitForHistoryJobExecutorToProcessAllJobs(20000, 200);

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.create();

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(logEntries).hasSize(2);
            HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
            assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
            assertThat(historicTaskLogEntry.getUserId()).isNull();
            assertThat(historicTaskLogEntry.getTaskId()).isEqualTo(task.getId());
            assertThat(historicTaskLogEntry.getType()).isNull();
            assertThat(historicTaskLogEntry.getTimeStamp()).isNotNull();
            assertThat(historicTaskLogEntry.getData()).isNull();
        }
    }

    @Test
    public void createCustomTaskEventLog_withoutTimeStamp_addsDefault() {
        task = taskService.createTaskBuilder().create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.create();

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(logEntries).hasSize(2);
            HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
            assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
            assertThat(historicTaskLogEntry.getTimeStamp()).isNotNull();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logSuspensionStateEvents() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        try {
            runtimeService.suspendProcessInstanceById(processInstance.getId());
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                        .type("USER_TASK_SUSPENSIONSTATE_CHANGED")
                        .list();
                assertThat(logEntries).hasSize(1);

                logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(2);
            }

            runtimeService.activateProcessInstanceById(processInstance.getId());

            HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 10000, 200);
            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).hasSize(3);

            logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                    .type("USER_TASK_SUSPENSIONSTATE_CHANGED")
                    .list();
            assertThat(logEntries).hasSize(2);

        } finally {
            String taskId = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId();
            managementService.executeCommand(commandContext -> {
                processEngineConfiguration.getTaskServiceConfiguration()
                    .getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
                return null;
            });
            runtimeService.deleteProcessInstance(processInstance.getId(), "clean up");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logProcessTaskEvents() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        try {
            taskService.setAssignee(task.getId(), "newAssignee");
            taskService.setOwner(task.getId(), "newOwner");
            taskService.complete(task.getId());

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(4);

                HistoricTaskLogEntry logEntry = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_CREATED").singleResult();
                assertThat(logEntry).isNotNull();
                assertThat(logEntry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
                assertThat(logEntry.getExecutionId()).isEqualTo(task.getExecutionId());
                assertThat(logEntry.getProcessInstanceId()).isEqualTo(processInstance.getId());

                assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_ASSIGNEE_CHANGED").count()).isEqualTo(1);
                assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_OWNER_CHANGED").count()).isEqualTo(1);
                assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_COMPLETED").count()).isEqualTo(1);
            }

        } finally {
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddCandidateUser() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        try {
            assertThat(processInstance).isNotNull();
            assertThat(task).isNotNull();

            taskService.addCandidateUser(task.getId(), "newCandidateUser");

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(2);

                logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                        .type("USER_TASK_IDENTITY_LINK_ADDED")
                        .list();
                assertThat(logEntries).hasSize(1);
                assertThat(logEntries.get(0).getData()).contains(
                        "\"type\":\"candidate\"",
                        "\"userId\":\"newCandidateUser\""
                );
            }

        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddParticipantUser() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        try {
            assertThat(processInstance).isNotNull();
            assertThat(task).isNotNull();

            taskService.addUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.PARTICIPANT);

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(2);

                logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                        .type("USER_TASK_IDENTITY_LINK_ADDED")
                        .list();
                assertThat(logEntries).hasSize(1);
                assertThat(logEntries.get(0).getData()).contains(
                        "\"type\":\"participant\"",
                        "\"userId\":\"newCandidateUser\""
                );
            }

        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddCandidateGroup() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        try {
            taskService.addCandidateGroup(task.getId(), "newCandidateGroup");

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(2);

                logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                        .type("USER_TASK_IDENTITY_LINK_ADDED")
                        .list();
                assertThat(logEntries).hasSize(1);
                assertThat(logEntries.get(0).getData()).contains(
                        "\"type\":\"candidate\"",
                        "\"groupId\":\"newCandidateGroup\""
                );
            }

        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logAddGroup() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        try {

            taskService.addGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.PARTICIPANT);

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(2);

                logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                        .type("USER_TASK_IDENTITY_LINK_ADDED")
                        .list();
                assertThat(logEntries).hasSize(1);
                assertThat(logEntries.get(0).getData()).contains(
                        "\"type\":\"participant\"",
                        "\"groupId\":\"newCandidateGroup\""
                );
            }

        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logDeleteCandidateGroup() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.addCandidateGroup(task.getId(), "newCandidateGroup");
        try {
            taskService.deleteCandidateGroup(task.getId(), "newCandidateGroup");

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(3);

                logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                        .type("USER_TASK_IDENTITY_LINK_REMOVED")
                        .list();
                assertThat(logEntries).hasSize(1);
                assertThat(logEntries.get(0).getData()).contains(
                        "\"type\":\"candidate\"",
                        "\"groupId\":\"newCandidateGroup\""
                );
            }

        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logDeleteCandidateUser() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(task).isNotNull();
        taskService.addCandidateUser(task.getId(), "newCandidateUser");

        try {
            taskService.deleteCandidateUser(task.getId(), "newCandidateUser");

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(3);

                logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                        .type("USER_TASK_IDENTITY_LINK_REMOVED")
                        .list();
                assertThat(logEntries).hasSize(1);
                assertThat(logEntries.get(0).getData()).contains(
                        "\"type\":\"candidate\"",
                        "\"userId\":\"newCandidateUser\""
                );
            }

        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskIdentityLinksTest.testCustomIdentityLink.bpmn20.xml")
    public void logIdentityLinkEventsForProcessIdentityLinks() {

        runtimeService.startProcessInstanceByKey("customIdentityLink");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskInvolvedUser("kermit").list();
        assertThat(tasks).hasSize(1);
        task = tasks.get(0);

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            // create, identityLinkAdded, identityLinkAdded
            assertThat(logEntries).hasSize(3);

            logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                    .type("USER_TASK_IDENTITY_LINK_ADDED")
                    .list();
            assertThat(logEntries).hasSize(2);

            boolean hasKermit = false;
            boolean hasManagement = false;
            String data = logEntries.get(0).getData();
            String data1 = logEntries.get(1).getData();
            if ((data.contains("\"type\":\"businessAdministrator\"") && data.contains("\"userId\":\"kermit\"")) ||
                    (data1.contains("\"type\":\"businessAdministrator\"") && data1.contains("\"userId\":\"kermit\""))) {

                hasKermit = true;
            }

            if ((data.contains("\"type\":\"businessAdministrator\"") && data.contains("\"groupId\":\"management\"")) ||
                    (data1.contains("\"type\":\"businessAdministrator\"") && data1.contains("\"groupId\":\"management\""))) {

                hasManagement = true;
            }

            assertThat(hasKermit).isTrue();
            assertThat(hasManagement).isTrue();

            taskService.complete(tasks.get(0).getId());

            HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 10000, 200);
            logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            // + completed event. Do not expect identity link removed events
            assertThat(logEntries).hasSize(4);

            logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId())
                    .type("USER_TASK_COMPLETED")
                    .list();
            assertThat(logEntries).hasSize(1);
        }
    }

    @Test
    public void queryForTaskLogEntriesByTasKId() {

        task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();
        Task anotherTask = taskService.createTaskBuilder().create();

        try {
            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(1);
                assertThat(logEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());

                assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isEqualTo(1);
            }

        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByUserId() {

        assertThatTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().userId("testUser"),
                historyService.createHistoricTaskLogEntryQuery().userId("testUser"));
    }

    protected void assertThatTaskLogIsFetched(HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder,
            HistoricTaskLogEntryQuery historicTaskLogEntryQuery) {

        task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();
        Task anotherTask = taskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        try {
            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
                assertThat(logEntries).hasSize(3);
                assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(task.getId(), task.getId(), task.getId());

                assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3);

                List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
                assertThat(pagedLogEntries).hasSize(1);
                assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
            }

        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
        }
    }
    
    protected void assertThatAllTaskLogIsFetched(HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder,
            HistoricTaskLogEntryQuery historicTaskLogEntryQuery) {

        task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();
        Task anotherTask = taskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        try {
            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
                assertThat(logEntries).hasSize(5);
                assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactlyInAnyOrder(task.getId(), anotherTask.getId(), task.getId(), task.getId(), task.getId());

                assertThat(historicTaskLogEntryQuery.count()).isEqualTo(5);

                List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
                assertThat(pagedLogEntries).hasSize(1);
                assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
            }

        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByType() {

        assertThatTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().type("testType"),
                historyService.createHistoricTaskLogEntryQuery().type("testType"));
    }

    @Test
    public void queryForTaskLogEntriesByProcessInstanceId() {

        assertThatTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().processInstanceId("testProcess"),
                historyService.createHistoricTaskLogEntryQuery().processInstanceId("testProcess"));
    }

    @Test
    public void queryForTaskLogEntriesByScopeId() {

        assertThatTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().scopeId("testScopeId"),
                historyService.createHistoricTaskLogEntryQuery().scopeId("testScopeId"));
    }

    @Test
    public void queryForTaskLogEntriesBySubScopeId() {

        assertThatTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().subScopeId("testSubScopeId"),
                historyService.createHistoricTaskLogEntryQuery().subScopeId("testSubScopeId"));
    }

    @Test
    public void queryForTaskLogEntriesByScopeType() {

        assertThatTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().scopeType("testScopeType"),
                historyService.createHistoricTaskLogEntryQuery().scopeType("testScopeType"));
    }

    @Test
    public void queryForTaskLogEntriesByFromTimeStamp() {

        assertThatAllTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
                historyService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()));
    }

    @Test
    public void queryForTaskLogEntriesByToTimeStamp() {
        
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate());
        HistoricTaskLogEntryQuery historicTaskLogEntryQuery = historyService.createHistoricTaskLogEntryQuery().to(getCompareAfterDate());

        task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();
        Task anotherTask = taskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        try {
            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
                assertThat(logEntries).hasSize(3);
                assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId)
                        .containsExactly(task.getId(), task.getId(), task.getId());

                assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3);

                List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
                assertThat(pagedLogEntries).hasSize(1);
                assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
            }

        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
            taskService.deleteTask(anotherTask.getId(), true);
        }
    }

    @Test
    public void queryForTaskLogEntriesByFromToTimeStamp() {

        assertThatTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
                historyService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()).to(getCompareAfterDate()));
    }

    @Test
    public void queryForTaskLogEntriesByTenantId() {

        assertThatTaskLogIsFetched(historyService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
                historyService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()).to(getCompareAfterDate()));
    }

    @Test
    public void queryForTaskLogEntriesByLogNumber() {

        task = taskService.createTaskBuilder().
                assignee("testAssignee").
                create();
        Task anotherTask = taskService.createTaskBuilder().create();

        try {
            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
                historicTaskLogEntryBuilder.taskId(task.getId()).create();
                historicTaskLogEntryBuilder.taskId(task.getId()).create();
                historicTaskLogEntryBuilder.taskId(task.getId()).create();

                HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 10000, 200);
                List<HistoricTaskLogEntry> allLogEntries = historyService.createHistoricTaskLogEntryQuery().list();

                HistoricTaskLogEntryQuery historicTaskLogEntryQuery = historyService.createHistoricTaskLogEntryQuery().
                        fromLogNumber(allLogEntries.get(1).getLogNumber()).
                        toLogNumber(allLogEntries.get(allLogEntries.size() - 2).getLogNumber());
                List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.
                        list();
                assertThat(logEntries).hasSize(3);
                assertThat(logEntries).extracting(HistoricTaskLogEntry::getLogNumber).containsExactly(
                        allLogEntries.get(1).getLogNumber(), allLogEntries.get(2).getLogNumber(), allLogEntries.get(3).getLogNumber()
                );

                assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3);

                List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
                assertThat(pagedLogEntries).hasSize(1);
                assertThat(pagedLogEntries.get(0).getLogNumber()).isEqualTo(logEntries.get(1).getLogNumber());
            }
        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByNativeQuery() {
        
        assertThat(managementService.getTableName(HistoricTaskLogEntryEntity.class, false)).isEqualTo("ACT_HI_TSK_LOG");
        assertThat(managementService.getTableName(HistoricTaskLogEntry.class, false)).isEqualTo("ACT_HI_TSK_LOG");
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId("1").create();
        historicTaskLogEntryBuilder.taskId("2").create();
        historicTaskLogEntryBuilder.taskId("3").create();

        if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
            try {
                assertThat(historyService.createNativeHistoricTaskLogEntryQuery()
                        .sql("SELECT * FROM " + managementService.getTableName(HistoricTaskLogEntry.class)).list()).hasSize(3);
                assertThat(historyService.createNativeHistoricTaskLogEntryQuery()
                        .sql("SELECT count(*) FROM " + managementService.getTableName(HistoricTaskLogEntry.class)).count()).isEqualTo(3);

                assertThat(historyService.createNativeHistoricTaskLogEntryQuery().parameter("taskId", "1").
                        sql("SELECT count(*) FROM " + managementService.getTableName(HistoricTaskLogEntry.class) + " WHERE TASK_ID_ = #{taskId}").list())
                        .hasSize(1);
                assertThat(historyService.createNativeHistoricTaskLogEntryQuery().parameter("taskId", "1").
                        sql("SELECT count(*) FROM " + managementService.getTableName(HistoricTaskLogEntry.class) + " WHERE TASK_ID_ = #{taskId}").count())
                        .isEqualTo(1);
            } finally {
                deleteTaskWithLogEntries("1");
                deleteTaskWithLogEntries("2");
                deleteTaskWithLogEntries("3");
            }
        }
    }

    @Test
    public void queryForTaskLogOrderBy() {
        
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId("1").timeStamp(getInsertDate()).create();
        waitForHistoryJobExecutorToProcessAllJobs(20000, 200);
        historicTaskLogEntryBuilder.taskId("2").timeStamp(getCompareAfterDate()).create();
        waitForHistoryJobExecutorToProcessAllJobs(20000, 200);
        historicTaskLogEntryBuilder.taskId("3").timeStamp(getCompareBeforeDate()).create();
        waitForHistoryJobExecutorToProcessAllJobs(20000, 200);

        try {

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> taskLogEntries = historyService.createHistoricTaskLogEntryQuery().list();
                assertThat(taskLogEntries).extracting(taskLogEntry -> taskLogEntry.getTaskId()).containsExactlyInAnyOrder("1", "2", "3");

                taskLogEntries = historyService.createHistoricTaskLogEntryQuery().orderByLogNumber().desc().list();
                assertThat(taskLogEntries).extracting(taskLogEntry -> taskLogEntry.getTaskId()).containsExactly("3", "2", "1");

                taskLogEntries = historyService.createHistoricTaskLogEntryQuery().orderByTimeStamp().desc().list();
                assertThat(taskLogEntries).extracting(taskLogEntry -> taskLogEntry.getTaskId()).containsExactly("2", "1", "3");
            }

        } finally {
            deleteTaskWithLogEntries("1");
            deleteTaskWithLogEntries("2");
            deleteTaskWithLogEntries("3");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void logProcessTaskEventsInSameTransaction() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        try {
            managementService.executeCommand(commandContext -> {
                taskService.setAssignee(task.getId(), "newAssignee");
                taskService.setOwner(task.getId(), "newOwner");

                return null;
            });

            if (HistoryTestHelper.isHistoricTaskLoggingEnabled(processEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries)
                        .extracting(HistoricTaskLogEntry::getType)
                        .containsExactlyInAnyOrder(
                                "USER_TASK_CREATED",
                                "USER_TASK_ASSIGNEE_CHANGED",
                                "USER_TASK_ASSIGNEE_CHANGED",
                                "USER_TASK_OWNER_CHANGED"
                        );

                HistoricTaskLogEntry logEntry = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type("USER_TASK_CREATED").singleResult();
                assertThat(logEntry).isNotNull();
                assertThat(logEntry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
                assertThat(logEntry.getExecutionId()).isEqualTo(task.getExecutionId());
                assertThat(logEntry.getProcessInstanceId()).isEqualTo(processInstance.getId());
            }

        } finally {
            taskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }


    protected Date getInsertDate() {
        Calendar cal = new GregorianCalendar(2019, 3, 10);
        return cal.getTime();
    }

    protected Date getCompareBeforeDate() {
        Calendar cal = new GregorianCalendar(2019, 3, 9);
        return cal.getTime();
    }

    protected Date getCompareAfterDate() {
        Calendar cal = new GregorianCalendar(2019, 3, 11);
        return cal.getTime();
    }
}
