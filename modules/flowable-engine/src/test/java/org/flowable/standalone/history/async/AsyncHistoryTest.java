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
package org.flowable.standalone.history.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AbstractAsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.ResetExpiredJobsRunnable;
import org.flowable.job.service.impl.history.async.AsyncHistoryDateUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AsyncHistoryTest extends CustomConfigurationFlowableTestCase {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public AsyncHistoryTest() {
        super("asyncHistoryTest");
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        // Enable it, but don't start the executor automatically, it will be started in the tests themselves.
        processEngineConfiguration.setAsyncHistoryEnabled(true);
        processEngineConfiguration.setAsyncHistoryJsonGroupingEnabled(true);
        processEngineConfiguration.setAsyncHistoryJsonGroupingThreshold(1);
        processEngineConfiguration.setAsyncFailedJobWaitTime(100);
        processEngineConfiguration.setDefaultFailedJobWaitTime(100);
        processEngineConfiguration.setAsyncHistoryExecutorNumberOfRetries(100);
        processEngineConfiguration.setAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime(100);
        processEngineConfiguration.setAsyncExecutorActivate(false);
        processEngineConfiguration.setAsyncHistoryExecutorActivate(false);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        // The tests are doing deployments, which trigger async history. Therefore, we need to invoke them manually and then wait for the jobs to finish
        // so there can be clean data in the DB
        for (String autoDeletedDeploymentId : deploymentIdsForAutoCleanup) {
            deleteDeployment(autoDeletedDeploymentId);
        }
        deploymentIdsForAutoCleanup.clear();

        for (Job job : managementService.createJobQuery().list()) {
            if (job.getJobHandlerType().equals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY)
                    || job.getJobHandlerType().equals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED)) {
                managementService.deleteJob(job.getId());
            }
        }
    }

    @Test
    public void testOneTaskProcess() {
        deployOneTaskTestProcess();
        for (int i = 0; i < 10; i++) { // Run this multiple times, as order of jobs processing can be different each run
            String processInstanceId = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneTaskProcess")
                    .name("testName")
                    .businessKey("testBusinessKey")
                    .callbackId("testCallbackId")
                    .callbackType("testCallbackType")
                    .referenceId("testReferenceId")
                    .referenceType("testReferenceType")
                    .start()
                    .getId();
            taskService.complete(taskService.createTaskQuery().singleResult().getId());

            List<HistoryJob> jobs = managementService.createHistoryJobQuery().list();
            for (HistoryJob job : jobs) {
                assertThat(managementService.getHistoryJobHistoryJson(job.getId())).isNotEmpty();
            }

            int expectedNrOfJobs = 11;
            if (processEngineConfiguration.isAsyncHistoryJsonGroupingEnabled() &&
                    expectedNrOfJobs > processEngineConfiguration.getAsyncHistoryJsonGroupingThreshold()) {
                expectedNrOfJobs = 2; // 1 job  for start, 1 for complete
            }

            assertThat(jobs).hasSize(expectedNrOfJobs);
            for (HistoryJob job : jobs) {
                if (processEngineConfiguration.isAsyncHistoryJsonGzipCompressionEnabled()) {
                    assertThat(job.getJobHandlerType()).isEqualTo(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED);
                } else {
                    assertThat(job.getJobHandlerType()).isEqualTo(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY);
                }
                assertThat(((HistoryJobEntity) job).getAdvancedJobHandlerConfigurationByteArrayRef()).isNotNull();
            }

            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceId).count()).isZero();

            waitForHistoryJobExecutorToProcessAllJobs(7000L, 200L);

            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceId).count()).isEqualTo(2);

            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId)
                    .singleResult();
            assertThat(historicProcessInstance).isNotNull();
            assertThat(historicProcessInstance.getEndTime()).isNotNull();

            assertThat(historicProcessInstance.getBusinessKey()).isEqualTo("testBusinessKey");
            assertThat(historicProcessInstance.getReferenceId()).isEqualTo("testReferenceId");
            assertThat(historicProcessInstance.getReferenceType()).isEqualTo("testReferenceType");
            assertThat(historicProcessInstance.getCallbackId()).isEqualTo("testCallbackId");
            assertThat(historicProcessInstance.getCallbackType()).isEqualTo("testCallbackType");

            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertThat(historicTaskInstance.getName()).isNotNull();
            assertThat(historicTaskInstance.getExecutionId()).isNotNull();
            assertThat(historicTaskInstance.getProcessInstanceId()).isNotNull();
            assertThat(historicTaskInstance.getProcessDefinitionId()).isNotNull();
            assertThat(historicTaskInstance.getTaskDefinitionKey()).isNotNull();
            assertThat(historicTaskInstance.getCreateTime()).isNotNull();
            assertThat(historicTaskInstance.getEndTime()).isNotNull();
            assertThat(historicTaskInstance.getDurationInMillis()).isNotNull();

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            assertThat(historicActivityInstances).hasSize(5);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertThat(historicActivityInstance.getActivityId()).isNotNull();
                assertThat(historicActivityInstance.getActivityType()).isNotNull();
                if (!"sequenceFlow".equals(historicActivityInstance.getActivityType())) {
                    assertThat(historicActivityInstance.getActivityName()).isNotNull();
                }
                assertThat(historicActivityInstance.getProcessDefinitionId()).isNotNull();
                assertThat(historicActivityInstance.getProcessInstanceId()).isNotNull();
                assertThat(historicActivityInstance.getExecutionId()).isNotNull();
                assertThat(historicActivityInstance.getDurationInMillis()).isNotNull();
                assertThat(historicActivityInstance.getStartTime()).isNotNull();
                assertThat(historicActivityInstance.getEndTime()).isNotNull();
                assertThat(historicActivityInstance.getTransactionOrder()).isNotNull();
            }

            for (String activityId : Arrays.asList("start", "theTask", "theEnd")) {
                assertThat(historyService.createHistoricActivityInstanceQuery()
                        .activityId(activityId).processInstanceId(processInstanceId).list()).isNotNull();
            }
        }
    }

    @Test
    public void testExecuteThroughManagementService() {
        deployOneTaskTestProcess();

        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

        List<HistoryJob> jobs = managementService.createHistoryJobQuery().list();
        assertThat(jobs).hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).count()).isZero();

        managementService.executeHistoryJob(jobs.get(0).getId());

        jobs = managementService.createHistoryJobQuery().list();
        assertThat(jobs).isEmpty();
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testSimpleStraightThroughProcess() {
        String processInstanceId = runtimeService
                .startProcessInstanceByKey("testSimpleStraightThroughProcess", CollectionUtil.singletonMap("counter", 0)).getId();

        final List<HistoryJob> jobs = managementService.createHistoryJobQuery().list();
        assertThat(jobs.size()).isPositive();

        waitForHistoryJobExecutorToProcessAllJobs(70000L, 200L);
        assertThat(managementService.createHistoryJobQuery().singleResult()).isNull();

        // 203 -> (start, 1) + -->(1) + (service task, 50) + -->(50) + (gateway, 50), + <--(49) + -->(1) + (end, 1)
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).count()).isEqualTo(203);
    }

    @Test
    public void testCreateTaskHistory() {
        Task task = taskService.createTaskBuilder().id("task1").create();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskId("task1").singleResult()).isNull();

        waitForHistoryJobExecutorToProcessAllJobs(70000L, 200L);

        assertThat(historyService.createHistoricTaskInstanceQuery().taskId("task1").singleResult()).isNotNull();
        assertThat(task.getId()).isEqualTo("task1");

        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testTaskAssigneeChange() {
        Task task = startOneTaskprocess();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                .activityId("theTask").singleResult();
        assertThat(historicActivityInstance.getAssignee()).isEqualTo("kermit");

        task = taskService.createTaskQuery().singleResult();
        taskService.setAssignee(task.getId(), "johnDoe");
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
        assertThat(historicActivityInstance.getAssignee()).isEqualTo("johnDoe");

        finishOneTaskProcess(task);
    }

    @Test
    public void testTaskAssigneeChangeToNull() {
        Task task = startOneTaskprocess();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                .activityId("theTask").singleResult();
        assertThat(historicActivityInstance.getAssignee()).isEqualTo("kermit");

        task = taskService.createTaskQuery().singleResult();
        taskService.setAssignee(task.getId(), null);
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
        assertThat(historicActivityInstance.getAssignee()).isNull();

        finishOneTaskProcess(task);
    }

    @Test
    public void testClaimTask() {
        Task task = startOneTaskprocess();
        taskService.setAssignee(task.getId(), null);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getClaimTime()).isNull();

        taskService.claim(historicTaskInstance.getId(), "johnDoe");
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getClaimTime()).isNotNull();
        assertThat(historicTaskInstance.getAssignee()).isNotNull();

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskOwner() {
        Task task = startOneTaskprocess();
        assertThat(task.getOwner()).isNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getOwner()).isNull();

        taskService.setOwner(task.getId(), "johnDoe");

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getOwner()).isEqualTo("johnDoe");

        taskService.setOwner(task.getId(), null);
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getOwner()).isNull();

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskName() {
        Task task = startOneTaskprocess();
        assertThat(task.getName()).isEqualTo("The Task");

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getName()).isEqualTo("The Task");

        task.setName("new name");
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getName()).isEqualTo("new name");

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskDescription() {
        Task task = startOneTaskprocess();
        assertThat(task.getDescription()).isNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getDescription()).isNull();

        task.setDescription("test description");
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getDescription()).isNotNull();

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        task.setDescription(null);
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getDescription()).isNull();

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskDueDate() {
        Task task = startOneTaskprocess();
        assertThat(task.getDueDate()).isNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getDueDate()).isNull();

        taskService.setDueDate(task.getId(), new Date());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getDueDate()).isNotNull();

        taskService.setDueDate(task.getId(), null);
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getDueDate()).isNull();

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskPriority() {
        Task task = startOneTaskprocess();
        assertThat(task.getPriority()).isEqualTo(Task.DEFAULT_PRIORITY);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getPriority()).isEqualTo(Task.DEFAULT_PRIORITY);

        taskService.setPriority(task.getId(), 1);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getPriority()).isEqualTo(1);

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskCategory() {
        Task task = startOneTaskprocess();
        assertThat(task.getCategory()).isNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getCategory()).isNull();

        task.setCategory("test category");
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getCategory()).isEqualTo("test category");

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskFormKey() {
        Task task = startOneTaskprocess();
        assertThat(task.getFormKey()).isNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getFormKey()).isNull();
        task.setFormKey("test form key");
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getFormKey()).isEqualTo("test form key");

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskStartTime() {
        Task task = startOneTaskprocess();

        finishOneTaskProcess(task);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getStartTime()).isNotNull();
        assertThat(historicTaskInstance.getEndTime()).isNotNull();
        assertThat(historicTaskInstance.getStartTime()).isEqualTo(task.getCreateTime());
    }

    @Test
    public void testSetTaskParentId() {
        Task parentTask1 = taskService.newTask();
        parentTask1.setName("Parent task 1");
        taskService.saveTask(parentTask1);

        Task parentTask2 = taskService.newTask();
        parentTask2.setName("Parent task 2");
        taskService.saveTask(parentTask2);

        Task childTask = taskService.newTask();
        childTask.setName("child task");
        childTask.setParentTaskId(parentTask1.getId());
        taskService.saveTask(childTask);
        assertThat(taskService.getSubTasks(parentTask1.getId())).hasSize(1);
        assertThat(taskService.getSubTasks(parentTask2.getId())).isEmpty();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);

        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(childTask.getId()).singleResult();
        assertThat(historicTaskInstance.getParentTaskId()).isEqualTo(parentTask1.getId());

        childTask = taskService.createTaskQuery().taskId(childTask.getId()).singleResult();
        childTask.setParentTaskId(parentTask2.getId());
        taskService.saveTask(childTask);
        assertThat(taskService.getSubTasks(parentTask1.getId())).isEmpty();
        assertThat(taskService.getSubTasks(parentTask2.getId())).hasSize(1);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);

        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(childTask.getId()).singleResult();
        assertThat(historicTaskInstance.getParentTaskId()).isEqualTo(parentTask2.getId());

        taskService.deleteTask(parentTask1.getId(), true);
        taskService.deleteTask(parentTask2.getId(), true);
    }

    @Test
    public void testResetExpiredJobs() {

        // Need to do this to initialize everything properly
        processEngineConfiguration.getAsyncHistoryExecutor().start();
        Runnable runnable = ((AbstractAsyncExecutor) processEngineConfiguration.getAsyncHistoryExecutor()).getResetExpiredJobsRunnable();
        assertThat(runnable).isNotNull();
        processEngineConfiguration.getAsyncHistoryExecutor().shutdown();

        startOneTaskprocess();
        assertThat(managementService.createHistoryJobQuery().count()).isEqualTo(1);

        // Force job to be expired
        managementService.executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                HistoryJob historyJob = managementService.createHistoryJobQuery().singleResult();
                ((HistoryJobEntity) historyJob).setLockOwner("test");
                ((HistoryJobEntity) historyJob).setLockExpirationTime(new Date(Instant.now().minus(100, ChronoUnit.DAYS).toEpochMilli()));
                return null;
            }
        });

        assertThat(((HistoryJobEntity) managementService.createHistoryJobQuery().singleResult()).getLockExpirationTime()).isNotNull();

        // Manually trigger the reset
        ResetExpiredJobsRunnable resetExpiredJobsRunnable = ((ResetExpiredJobsRunnable) runnable);
        resetExpiredJobsRunnable.setInterrupted(false); // the shutdown() above will have interrupted the runnable, hence why it needs to be reset
        resetExpiredJobsRunnable.resetJobs();

        // The lock expiration time should be null now
        assertThat(((HistoryJobEntity) managementService.createHistoryJobQuery().singleResult()).getLockExpirationTime()).isNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
    }

    @Test
    @Deployment(
            resources = {
                    "org/flowable/engine/test/api/runtime/callActivity.bpmn20.xml",
                    "org/flowable/engine/test/api/runtime/calledActivity.bpmn20.xml"
            }
    )
    public void callSubProcess() {
        ProcessInstance pi = this.runtimeService.startProcessInstanceByKey("callActivity");

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricActivityInstance callSubProcessActivityInstance = historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId())
                .activityId("callSubProcess").singleResult();
        assertThat(callSubProcessActivityInstance)
                .extracting(HistoricActivityInstance::getCalledProcessInstanceId)
                .isEqualTo(runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult().getId());
    }

    @Test
    public void createUserTaskLogEntity() {
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = historyService.createHistoricTaskLogEntryBuilder();

        Date todayDate = new Date();
        historicTaskLogEntryBuilder.taskId("1");
        historicTaskLogEntryBuilder.type("testType");
        historicTaskLogEntryBuilder.userId("testUserId");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.processInstanceId("processInstanceId");
        historicTaskLogEntryBuilder.processDefinitionId("testDefinitionId");
        historicTaskLogEntryBuilder.executionId("testExecutionId");
        historicTaskLogEntryBuilder.timeStamp(todayDate);
        historicTaskLogEntryBuilder.tenantId("testTenant");

        historicTaskLogEntryBuilder.create();

        HistoricTaskLogEntry historicTaskLogEntry = null;
        try {
            assertThat(historyService.createHistoricTaskLogEntryQuery().taskId("1").count()).isZero();
            waitForHistoryJobExecutorToProcessAllJobs(7000, 200);
            assertThat(historyService.createHistoricTaskLogEntryQuery().taskId("1").count()).isEqualTo(1);

            historicTaskLogEntry = historyService.createHistoricTaskLogEntryQuery().taskId("1").singleResult();
            assertThat(historicTaskLogEntry.getLogNumber()).isPositive();
            assertThat(historicTaskLogEntry.getTaskId()).isEqualTo("1");
            assertThat(historicTaskLogEntry.getType()).isEqualTo("testType");
            assertThat(historicTaskLogEntry.getUserId()).isEqualTo("testUserId");
            assertThat(historicTaskLogEntry.getProcessInstanceId()).isEqualTo("processInstanceId");
            assertThat(historicTaskLogEntry.getProcessDefinitionId()).isEqualTo("testDefinitionId");
            assertThat(historicTaskLogEntry.getExecutionId()).isEqualTo("testExecutionId");
            assertThat(historicTaskLogEntry.getData()).isEqualTo("testData");
            assertThat(historicTaskLogEntry.getLogNumber()).isPositive();
            assertThat(simpleDateFormat.format(historicTaskLogEntry.getTimeStamp())).isEqualTo(simpleDateFormat.format(todayDate));
            assertThat(historicTaskLogEntry.getTenantId()).isEqualTo("testTenant");

        } finally {
            if (historicTaskLogEntry != null) {
                historyService.deleteHistoricTaskLogEntry(historicTaskLogEntry.getLogNumber());
                waitForHistoryJobExecutorToProcessAllJobs(7000, 200);
            }
        }

    }

    @Test
    public void testAsynchUsertTaskLogEntries() {
        deployOneTaskTestProcess();
        ProcessInstance oneTaskProcess = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        task.setName("newName");
        task.setPriority(0);
        taskService.saveTask(task);
        taskService.setAssignee(task.getId(), "newAssignee");
        taskService.setOwner(task.getId(), "newOwner");
        taskService.setDueDate(task.getId(), new Date());
        taskService.addUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);
        taskService.addGroupIdentityLink(task.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        taskService.deleteUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);
        taskService.deleteGroupIdentityLink(task.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.suspendProcessInstanceById(oneTaskProcess.getId());
        runtimeService.activateProcessInstanceById(oneTaskProcess.getId());
        taskService.complete(task.getId());

        assertThat(historyService.createHistoricTaskLogEntryQuery().count()).isZero();
        assertThat(managementService.createHistoryJobQuery().count()).isEqualTo(12);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 200);

        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isEqualTo(13);
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_CREATED.name()).count())
                .isEqualTo(1);
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_NAME_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(
                historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_PRIORITY_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(
                historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_ASSIGNEE_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_OWNER_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(
                historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_DUEDATE_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_SUSPENSIONSTATE_CHANGED.name())
                .count()).isEqualTo(2);
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED.name())
                .count()).isEqualTo(2);
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_REMOVED.name())
                .count()).isEqualTo(2);
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_COMPLETED.name()).count())
                .isEqualTo(1);
    }

    @Test
    public void testDeleteAsynchUserTaskLogEntries() {
        deployOneTaskTestProcess();
        ProcessInstance oneTaskProcess = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        assertThat(historyService.createHistoricTaskLogEntryQuery().count()).isZero();
        assertThat(managementService.createHistoryJobQuery().count()).isEqualTo(1);
        waitForHistoryJobExecutorToProcessAllJobs(7000, 200);
        List<HistoricTaskLogEntry> historicTaskLogEntries = historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(historicTaskLogEntries).hasSize(1);

        historyService.deleteHistoricTaskLogEntry(historicTaskLogEntries.get(0).getLogNumber());

        assertThat(managementService.createHistoryJobQuery().count()).isEqualTo(1);
        waitForHistoryJobExecutorToProcessAllJobs(7000, 200);
        assertThat(historyService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testInclusiveGatewayEndTimeSet() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testInclusiveGateway");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("Always", "Always");

        for (Task task : tasks) {
            taskService.complete(task.getId());
        }

        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(historicProcessInstance.getEndTime()).isNotNull();

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalWaitOnUserTaskBoundaryEvent.bpmn20.xml")
    public void testSignalWaitOnUserTaskBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signal-wait");
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).signalEventSubscriptionName("waitsig")
                .singleResult();
        assertThat(execution).isNotNull();
        runtimeService.signalEventReceived("waitsig", execution.getId());
        execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).signalEventSubscriptionName("waitsig").singleResult();
        assertThat(execution).isNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Wait2");
    }

    // Test for a bug when using non-interrupting boundary event (any type) with async history
    @Test
    @Deployment(extraResources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testNonInterruptingTimerBoundaryEventOnCallActivityWithOldData() {

        // In earlier versions of Flowable, there used to be a bug that would not generate a historic activity instance when it was expected to,
        // when using a non-interrupting boundary event on a call activity.
        // More precisely: the activity-end for the boundary event was triggered twice (but only one activity-start),
        // which led to failure when using asynchronous history (as no start for the end exists).
        // This unit test mimics this setup and validates that the problem is solved now.
        //
        // Side-note: non-interrupting boundary events such as signals/messages are repeating by default.
        // For timer boundary events, the timer should be made repeating (the child execution is still created if it isn't)

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess");

        // Mimic timer firing
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());

        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        List<HistoricActivityInstance> timerBoundaryEvents = historyService.createHistoricActivityInstanceQuery().activityId("timerBoundaryEvent").list();
        assertThat(timerBoundaryEvents).hasSize(2);

        // Mimic the bug by removing the latest activity instance (runtime and historical) - this was not created before
        HistoricActivityInstance historicActivityInstanceToDelete = timerBoundaryEvents.stream()
            .filter(historicActivityInstance -> historicActivityInstance.getEndTime() == null).findFirst().get();
        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getActivityInstanceEntityManager().delete(historicActivityInstanceToDelete.getId());
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getHistoricActivityInstanceEntityManager().delete(historicActivityInstanceToDelete.getId());
            return null;
        });

        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        // Additionally, an extra activity-end was generated
        // (not anymore, due to recent changes in ActivityInstanceEntityManagerImpl#recordActivityInstanceEnd - hence why it needs to be inserted manually here)
        ObjectNode historyJson = processEngineConfiguration.getObjectMapper().createObjectNode();
        historyJson.put("type", "activity-end");
        ObjectNode dataNode = historyJson.putObject("data");

        // Note the lack of runtimeActivityInstanceId. This is because at that time,
        // there would be no runtime activity and it would fallback to the execution based logic (that's there for backwards compatibility)
        dataNode.put("processDefinitionId", historicActivityInstanceToDelete.getProcessDefinitionId());
        dataNode.put("processInstanceId", historicActivityInstanceToDelete.getProcessInstanceId());
        dataNode.put("executionId", historicActivityInstanceToDelete.getExecutionId());
        dataNode.put("activityId", "timerBoundaryEvent");
        dataNode.put("activityType", "boundaryEvent");
        dataNode.put("transactionOrder", 1);
        dataNode.put("tenantId", "");
        dataNode.put("startTime", AsyncHistoryDateUtil.formatDate(new Date()));
        dataNode.put("__timeStamp", AsyncHistoryDateUtil.formatDate(new Date()));
        addJsonToHistoryJobJson((HistoryJobEntity) managementService.createHistoryJobQuery().singleResult(), historyJson);

        waitForHistoryJobExecutorToProcessAllJobs(1000000, 200);

        timerBoundaryEvents = historyService.createHistoricActivityInstanceQuery().activityId("timerBoundaryEvent").list();
        assertThat(timerBoundaryEvents)
            .extracting(HistoricActivityInstance::getStartTime, HistoricActivityInstance::getEndTime)
            .doesNotContainNull();
    }

    @Test
    public void testHistoryJobFailure() {
        Task task = startOneTaskprocess();

        // Fetch the first history job, and programmatically change the handler type, such that it will guaranteed fail.
        HistoryJob historyJob = managementService.createHistoryJobQuery().singleResult();
        changeTransformerTypeToInvalidType((HistoryJobEntity) historyJob);

        assertThat(managementService.createDeadLetterJobQuery().count()).isEqualTo(0);
        waitForHistoryJobExecutorToProcessAllJobs(20000L, 50L);
        assertThat(managementService.createHistoryJobQuery().count()).isEqualTo(0);

        Job deadLetterJob = managementService.createDeadLetterJobQuery().singleResult();
        assertThat(deadLetterJob.getJobType()).isEqualTo(HistoryJobEntity.HISTORY_JOB_TYPE);
        assertThat(deadLetterJob.getExceptionMessage()).isNotEmpty();

        String deadLetterJobExceptionStacktrace = managementService.getDeadLetterJobExceptionStacktrace(deadLetterJob.getId());
        assertThat(deadLetterJobExceptionStacktrace).isNotEmpty();

        // The history jobs in the deadletter table have no link to the process instance, hence why a manual cleanup is needed.
        runtimeService.deleteProcessInstance(task.getProcessInstanceId(), null);
        managementService.createHistoryJobQuery().list().forEach(j -> managementService.deleteHistoryJob(j.getId()));
        managementService.createDeadLetterJobQuery().list().forEach(j -> managementService.deleteDeadLetterJob(j.getId()));
    }

    @Test
    public void testHistoryJobMissingActivityStart() {
        Task task = startOneTaskprocess();

        // Removing the 'activity start' will fail both the processing of the starting of the process instance and the setting of the assignee
        HistoryJob historyJob = managementService.createHistoryJobQuery().singleResult();
        removeActivityStart((HistoryJobEntity) historyJob, "theTask");

        taskService.complete(task.getId());

        assertThat(managementService.createDeadLetterJobQuery().count()).isEqualTo(0);
        assertThat(historyService.createHistoricProcessInstanceQuery().list()).isEmpty();
        assertThat(historyService.createHistoricTaskInstanceQuery().list()).isEmpty();
        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").list()).isEmpty();
        waitForHistoryJobExecutorToProcessAllJobs(20000L, 50L);
        assertThat(managementService.createDeadLetterJobQuery().count()).isEqualTo(2);
        assertThat(historyService.createHistoricTaskInstanceQuery().list()).hasSize(1);
        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").list()).isEmpty();

        // Even though there are dead letter jobs, only the delta of the jobs needs to be there
        // the parts that didn't fail should still create the historic process instance

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .singleResult();

        assertThat(historicProcessInstance).isNotNull();
        assertThat(historicProcessInstance.getEndTime()).isNotNull();

        List<String> exceptionMessages = managementService.createDeadLetterJobQuery().list().stream().map(job -> job.getExceptionMessage()).collect(Collectors.toList());
        assertThat(exceptionMessages).containsOnly(
            "Failed to process async history json. See suppressed exceptions.",
            "Failed to process async history json. See suppressed exceptions."
        );

        List<String> exceptionStacktraces = managementService.createDeadLetterJobQuery()
                .list()
                .stream()
                .sorted(Comparator.comparing(Job::getCreateTime))
                .map(job -> managementService.getDeadLetterJobExceptionStacktrace(job.getId()))
                .collect(Collectors.toList());

        String stackTrace1 = exceptionStacktraces.get(0);
        if (stackTrace1.contains("[activity-update]")) {
            assertThat(stackTrace1).contains("Job is not applicable for transformer types: [activity-update]");
            
            assertThat(exceptionStacktraces.get(1))
                .contains("Job is not applicable for transformer types: [activity-end]");
            
        } else {
            assertThat(stackTrace1).contains("Job is not applicable for transformer types: [activity-end]");
            
            assertThat(exceptionStacktraces.get(1))
                .contains("Job is not applicable for transformer types: [activity-update]");
        }

        // The history jobs in the deadletter table have no link to the process instance, hence why a manual cleanup is needed.
        managementService.createDeadLetterJobQuery().list().forEach(j -> managementService.deleteDeadLetterJob(j.getId()));
    }

    @Test
    public void testMoveDeadLetterJobBackToHistoryJob() {
        TestDeadletterEventListener testDeadletterEventListener = new TestDeadletterEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(testDeadletterEventListener, FlowableEngineEventType.JOB_MOVED_TO_DEADLETTER);

        Task task = startOneTaskprocess();

        HistoryJob historyJob = managementService.createHistoryJobQuery().singleResult();
        changeTransformerTypeToInvalidType((HistoryJobEntity) historyJob);

        String originalAdvancedConfiguration = getAdvancedJobHandlerConfiguration(historyJob.getId());
        assertThat(originalAdvancedConfiguration).isNotEmpty();
        assertThat(TestDeadletterEventListener.COUNTER.get()).isEqualTo(0);

        waitForHistoryJobExecutorToProcessAllJobs(20000L, 50L);

        assertThat(managementService.createHistoryJobQuery().count()).isEqualTo(0);
        Job deadLetterJob = managementService.createDeadLetterJobQuery().singleResult();
        assertThat(TestDeadletterEventListener.COUNTER.get()).isEqualTo(1);

        managementService.moveDeadLetterJobToHistoryJob(deadLetterJob.getId(), 3);
        assertThat(managementService.createHistoryJobQuery().count()).isEqualTo(1);
        historyJob = managementService.createHistoryJobQuery().singleResult();

        assertThat(historyJob.getCreateTime()).isNotNull();
        assertThat(historyJob.getRetries()).isEqualTo(3);
        assertThat(historyJob.getExceptionMessage()).isNotNull(); // this is consistent with regular jobs
        assertThat(historyJob.getJobHandlerConfiguration()).isNull(); // needs to have been reset

        String newAdvancedConfiguration = getAdvancedJobHandlerConfiguration(historyJob.getId());
        assertThat(originalAdvancedConfiguration).isEqualTo(newAdvancedConfiguration);

        // The history jobs in the deadletter table have no link to the process instance, hence why a manual cleanup is needed.
        runtimeService.deleteProcessInstance(task.getProcessInstanceId(), null);
        managementService.createHistoryJobQuery().list().forEach(j -> managementService.deleteHistoryJob(j.getId()));
        managementService.createDeadLetterJobQuery().list().forEach(j -> managementService.deleteDeadLetterJob(j.getId()));

        assertThat(TestDeadletterEventListener.COUNTER.get()).isEqualTo(1);
        processEngineConfiguration.getEventDispatcher().removeEventListener(testDeadletterEventListener);
    }

    static final class TestDeadletterEventListener implements FlowableEventListener {

        public static AtomicInteger COUNTER = new AtomicInteger(0);

        @Override
        public void onEvent(FlowableEvent event) {
            COUNTER.incrementAndGet();
        }
        @Override
        public boolean isFailOnException() {
            return false;
        }
        @Override
        public boolean isFireOnTransactionLifecycleEvent() {
            return false;
        }
        @Override
        public String getOnTransaction() {
            return null;
        }
    }

    protected void changeTransformerTypeToInvalidType(HistoryJobEntity historyJob) {
        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                try {
                    HistoryJobEntity historyJobEntity = historyJob;

                    ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
                    JsonNode historyJsonNode = objectMapper.readTree(historyJobEntity.getAdvancedJobHandlerConfiguration());

                    for (JsonNode jsonNode : historyJsonNode) {
                        if (jsonNode.has("type")) {
                            ((ObjectNode) jsonNode).put("type", "invalidType");
                        }
                    }

                    historyJobEntity.setAdvancedJobHandlerConfiguration(objectMapper.writeValueAsString(historyJsonNode));
                } catch (JsonProcessingException e) {
                    Assert.fail();
                }
                return null;
            }
        });
    }

    protected void removeActivityStart(HistoryJobEntity historyJob, String activityId) {
        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                try {

                    ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
                    JsonNode historyJsonNode = objectMapper.readTree(historyJob.getAdvancedJobHandlerConfiguration());

                    Iterator<JsonNode> iterator = historyJsonNode.iterator();
                    while (iterator.hasNext()) {
                        JsonNode jsonNode = iterator.next();
                        if (jsonNode.path("data").path("activityId").asText().equals(activityId)) {
                            iterator.remove();
                        }
                    }

                    historyJob.setAdvancedJobHandlerConfiguration(objectMapper.writeValueAsString(historyJsonNode));
                } catch (JsonProcessingException e) {
                    Assert.fail();
                }
                return null;
            }
        });
    }

    protected void addJsonToHistoryJobJson(HistoryJobEntity historyJob, ObjectNode objectNode) {
        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                try {

                    ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
                    JsonNode historyJsonNode = objectMapper.readTree(historyJob.getAdvancedJobHandlerConfiguration());

                    ((ArrayNode) historyJsonNode).add(objectNode);

                    historyJob.setAdvancedJobHandlerConfiguration(objectMapper.writeValueAsString(historyJsonNode));
                } catch (JsonProcessingException e) {
                    Assert.fail();
                }
                return null;
            }
        });
    }

    protected String getAdvancedJobHandlerConfiguration(String historyJobId) {
        return processEngineConfiguration.getCommandExecutor().execute(new Command<String>() {

            @Override
            public String execute(CommandContext commandContext) {
                JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
                HistoryJobEntity job = jobServiceConfiguration.getHistoryJobEntityManager().findById(historyJobId);
                return job.getAdvancedJobHandlerConfiguration();
            }
        });
    }

    protected Task startOneTaskprocess() {
        deployOneTaskTestProcess();
        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        return task;
    }

    protected void finishOneTaskProcess(Task task) {
        taskService.complete(task.getId());
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        assertThat(managementService.createHistoryJobQuery().singleResult()).isNull();
    }

}
