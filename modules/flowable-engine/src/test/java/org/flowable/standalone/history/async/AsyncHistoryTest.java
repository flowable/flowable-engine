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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

public class AsyncHistoryTest extends CustomConfigurationFlowableTestCase {

    @Override
    protected String getEngineName() {
        return "asyncHistoryTest";
    }
    
    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        // Enable it, but don't start the executor automatically, it will be started in the tests themselves.
        processEngineConfiguration.setAsyncHistoryEnabled(true);
        processEngineConfiguration.setAsyncHistoryJsonGroupingEnabled(true);
        processEngineConfiguration.setAsyncHistoryJsonGroupingThreshold(1);
        processEngineConfiguration.setAsyncFailedJobWaitTime(100);
        processEngineConfiguration.setDefaultFailedJobWaitTime(100);
        processEngineConfiguration.setAsyncHistoryExecutorNumberOfRetries(10);
        processEngineConfiguration.setAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime(100);
        processEngineConfiguration.setAsyncExecutorActivate(false);
    }

    @Override
    protected void tearDown() throws Exception {

        for (Job job : managementService.createJobQuery().list()) {
            if (job.getJobHandlerType().equals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY)
                    || job.getJobHandlerType().equals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED)) {
                managementService.deleteJob(job.getId());
            }
        }

        super.tearDown();
    }

    public void testOneTaskProcess() {
        deployOneTaskTestProcess();
        for (int i = 0; i < 10; i++) { // Run this multiple times, as order of jobs processing can be different each run
            String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
            taskService.complete(taskService.createTaskQuery().singleResult().getId());

            List<HistoryJob> jobs = managementService.createHistoryJobQuery().list();
            
            int expectedNrOfJobs = 11;
            if ( processEngineConfiguration.isAsyncHistoryJsonGroupingEnabled() && 
                    expectedNrOfJobs > processEngineConfiguration.getAsyncHistoryJsonGroupingThreshold()) {
                expectedNrOfJobs = 2; // 1 job  for start, 1 for complete
            }
            
            assertEquals(expectedNrOfJobs, jobs.size());
            for (HistoryJob job : jobs) {
                if (processEngineConfiguration.isAsyncHistoryJsonGzipCompressionEnabled()) {
                    assertEquals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED, job.getJobHandlerType());
                } else {
                    assertEquals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY, job.getJobHandlerType());
                }
                assertNotNull(((HistoryJobEntity) job).getAdvancedJobHandlerConfigurationByteArrayRef());
            }

            waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);

            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertNotNull(historicProcessInstance);
            assertNotNull(historicProcessInstance.getEndTime());

            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertNotNull(historicTaskInstance.getName());
            assertNotNull(historicTaskInstance.getExecutionId());
            assertNotNull(historicTaskInstance.getProcessInstanceId());
            assertNotNull(historicTaskInstance.getProcessDefinitionId());
            assertNotNull(historicTaskInstance.getTaskDefinitionKey());
            assertNotNull(historicTaskInstance.getStartTime());
            assertNotNull(historicTaskInstance.getEndTime());
            assertNotNull(historicTaskInstance.getDurationInMillis());

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
            assertEquals(3, historicActivityInstances.size());
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertNotNull(historicActivityInstance.getActivityId());
                assertNotNull(historicActivityInstance.getActivityName());
                assertNotNull(historicActivityInstance.getActivityType());
                assertNotNull(historicActivityInstance.getProcessDefinitionId());
                assertNotNull(historicActivityInstance.getProcessInstanceId());
                assertNotNull(historicActivityInstance.getExecutionId());
                assertNotNull(historicActivityInstance.getDurationInMillis());
                assertNotNull(historicActivityInstance.getStartTime());
                assertNotNull(historicActivityInstance.getEndTime());
            }

            for (String activityId : Arrays.asList("start", "theTask", "theEnd")) {
                assertNotNull(historyService.createHistoricActivityInstanceQuery()
                                .activityId(activityId).processInstanceId(processInstanceId).list());
            }
        }
    }

    @Deployment
    public void testSimpleStraightThroughProcess() {
        String processInstanceId = runtimeService
                        .startProcessInstanceByKey("testSimpleStraightThroughProcess", CollectionUtil.singletonMap("counter", 0)).getId();

        final List<HistoryJob> jobs = managementService.createHistoryJobQuery().list();
        assertTrue(jobs.size() > 0);

        waitForHistoryJobExecutorToProcessAllJobs(50000L, 100L);
        assertNull(managementService.createHistoryJobQuery().singleResult());

        // 1002 -> (start, 1) + (end, 1) + (gateway, 1000), + (service task, 1000)
        assertEquals(2002, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).count());
    }

    public void testTaskAssigneeChange() {
        Task task = startOneTaskprocess();

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                        .activityId("theTask").singleResult();
        assertEquals("kermit", historicActivityInstance.getAssignee());

        task = taskService.createTaskQuery().singleResult();
        taskService.setAssignee(task.getId(), "johnDoe");
        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
        assertEquals("johnDoe", historicActivityInstance.getAssignee());

        finishOneTaskProcess(task);
    }

    public void testTaskAssigneeChangeToNull() {
        Task task = startOneTaskprocess();

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                        .activityId("theTask").singleResult();
        assertEquals("kermit", historicActivityInstance.getAssignee());

        task = taskService.createTaskQuery().singleResult();
        taskService.setAssignee(task.getId(), null);
        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
        assertNull(historicActivityInstance.getAssignee());

        finishOneTaskProcess(task);
    }

    public void testClaimTask() {
        Task task = startOneTaskprocess();
        taskService.setAssignee(task.getId(), null);

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getClaimTime());

        taskService.claim(historicTaskInstance.getId(), "johnDoe");
        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertNotNull(historicTaskInstance.getClaimTime());
        assertNotNull(historicTaskInstance.getAssignee());

        finishOneTaskProcess(task);
    }

    public void testSetTaskOwner() {
        Task task = startOneTaskprocess();
        assertNull(task.getOwner());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getOwner());

        taskService.setOwner(task.getId(), "johnDoe");

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("johnDoe", historicTaskInstance.getOwner());

        taskService.setOwner(task.getId(), null);
        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertNull(historicTaskInstance.getOwner());

        finishOneTaskProcess(task);
    }

    public void testSetTaskName() {
        Task task = startOneTaskprocess();
        assertEquals("The Task", task.getName());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("The Task", historicTaskInstance.getName());

        task.setName("new name");
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("new name", historicTaskInstance.getName());

        finishOneTaskProcess(task);
    }

    public void testSetTaskDescription() {
        Task task = startOneTaskprocess();
        assertNull(task.getDescription());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getDescription());

        task.setDescription("test description");
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNotNull(historicTaskInstance.getDescription());

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        task.setDescription(null);
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertNull(historicTaskInstance.getDescription());

        finishOneTaskProcess(task);
    }

    public void testSetTaskDueDate() {
        Task task = startOneTaskprocess();
        assertNull(task.getDueDate());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getDueDate());

        taskService.setDueDate(task.getId(), new Date());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNotNull(historicTaskInstance.getDueDate());

        taskService.setDueDate(task.getId(), null);
        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertNull(historicTaskInstance.getDueDate());

        finishOneTaskProcess(task);
    }

    public void testSetTaskPriority() {
        Task task = startOneTaskprocess();
        assertEquals(Task.DEFAULT_PRIORITY, task.getPriority());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals(Task.DEFAULT_PRIORITY, historicTaskInstance.getPriority());

        taskService.setPriority(task.getId(), 1);

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals(1, historicTaskInstance.getPriority());

        finishOneTaskProcess(task);
    }

    public void testSetTaskCategory() {
        Task task = startOneTaskprocess();
        assertNull(task.getCategory());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getCategory());

        task.setCategory("test category");
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("test category", historicTaskInstance.getCategory());

        finishOneTaskProcess(task);
    }

    public void testSetTaskFormKey() {
        Task task = startOneTaskprocess();
        assertNull(task.getFormKey());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getFormKey());
        task.setFormKey("test form key");
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("test form key", historicTaskInstance.getFormKey());

        finishOneTaskProcess(task);
    }

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
        assertEquals(1, taskService.getSubTasks(parentTask1.getId()).size());
        assertEquals(0, taskService.getSubTasks(parentTask2.getId()).size());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);

        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(childTask.getId()).singleResult();
        assertEquals(parentTask1.getId(), historicTaskInstance.getParentTaskId());

        childTask = taskService.createTaskQuery().taskId(childTask.getId()).singleResult();
        childTask.setParentTaskId(parentTask2.getId());
        taskService.saveTask(childTask);
        assertEquals(0, taskService.getSubTasks(parentTask1.getId()).size());
        assertEquals(1, taskService.getSubTasks(parentTask2.getId()).size());

        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);

        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(childTask.getId()).singleResult();
        assertEquals(parentTask2.getId(), historicTaskInstance.getParentTaskId());

        taskService.deleteTask(parentTask1.getId(), true);
        taskService.deleteTask(parentTask2.getId(), true);
    }

    protected Task startOneTaskprocess() {
        deployOneTaskTestProcess();
        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        return task;
    }

    protected void finishOneTaskProcess(Task task) {
        taskService.complete(task.getId());
        waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        assertNull(managementService.createHistoryJobQuery().singleResult());
    }

}
