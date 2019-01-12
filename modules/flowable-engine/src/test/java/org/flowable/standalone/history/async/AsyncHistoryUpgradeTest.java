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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Upgrade from previous version should be able to run currently inserted history jobs
 */
public class AsyncHistoryUpgradeTest extends CustomConfigurationFlowableTestCase {

    private static final Collection<String> ADD_ACTIVITY_ID_CONFIGURATIONS = Arrays.asList(
        HistoryJsonConstants.TYPE_TASK_CREATED, HistoryJsonConstants.TYPE_TASK_ENDED, HistoryJsonConstants.TYPE_TASK_PROPERTY_CHANGED
    );

    public AsyncHistoryUpgradeTest() {
        super("asyncHistoryTest");
    }
    
    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        // Enable it, but don't start the executor automatically, it will be started in the tests themselves.
        processEngineConfiguration.setAsyncHistoryEnabled(true);
        processEngineConfiguration.setAsyncHistoryJsonGroupingEnabled(true);
        processEngineConfiguration.setAsyncHistoryJsonGzipCompressionEnabled(false);
        processEngineConfiguration.setAsyncHistoryJsonGroupingThreshold(1);
        processEngineConfiguration.setAsyncFailedJobWaitTime(100);
        processEngineConfiguration.setDefaultFailedJobWaitTime(100);
        processEngineConfiguration.setAsyncHistoryExecutorNumberOfRetries(10);
        processEngineConfiguration.setAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime(100);
        processEngineConfiguration.setAsyncExecutorActivate(false);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        // The tests are doing deployments, which trigger async history. Therefore, we need to invoke them manually and then wait for the jobs to finish
        // so there can be clean data in the DB
        for (String autoDeletedDeploymentId : deploymentIdsForAutoCleanup) {
            repositoryService.deleteDeployment(autoDeletedDeploymentId, true);
        }
        deploymentIdsForAutoCleanup.clear();
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 100);
        for (Job job : managementService.createJobQuery().list()) {
            if (job.getJobHandlerType().equals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY)
                    || job.getJobHandlerType().equals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED)) {
                managementService.deleteJob(job.getId());
            }
        }
    }

    @Test
    @Deployment(resources = "org/flowable/standalone/history/async/AsyncHistoryTest.testSimpleStraightThroughProcess.bpmn")
    public void testSimpleStraightThroughProcess() {
        final String processInstanceId = runtimeService
                        .startProcessInstanceByKey("testSimpleStraightThroughProcess", CollectionUtil.singletonMap("counter", 0)).getId();

        final List<HistoryJob> jobs = managementService.createHistoryJobQuery().list();
        assertTrue(jobs.size() > 0);

        removeRuntimeActivityInstances(processInstanceId);
        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(70000L, 200L);
        assertNull(managementService.createHistoryJobQuery().singleResult());

        // 203 -> (start, 1) + -->(1) + (service task, 50) + -->(50) + (gateway, 50), + <--(49) + -->(1) + (end, 1)
        assertEquals(203, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).count());
    }

    @Test
    public void testTaskAssigneeChange() {
        Task task = startOneTaskprocess();

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
            .activityId("theTask").singleResult();
        assertEquals("kermit", historicActivityInstance.getAssignee());
        assertThat(runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).count()).
            as("RuntimeActivityInstances are not created back from historicActivityInstances when they are asynchronously inserted into data store").
            isEqualTo(0);

        task = taskService.createTaskQuery().singleResult();

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.setAssignee(task.getId(), "johnDoe");
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
        assertEquals("johnDoe", historicActivityInstance.getAssignee());
        assertRuntimeActivityInstanceIsSame(historicActivityInstance);

        finishOneTaskProcess(task);
    }

    @Test
    public void testClaimTask() {
        Task task = startOneTaskprocess();

        downgradeHistoryJobConfigurations();

        taskService.setAssignee(task.getId(), null);

        waitForHistoryJobExecutorToProcessAllJobs(14000L, 200L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getClaimTime());
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().processInstanceId(task.getProcessInstanceId())
            .activityId("theTask").singleResult();
        assertThat(historicActivityInstance).extracting(HistoricActivityInstance::getTaskId).isEqualTo(task.getId());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.claim(historicTaskInstance.getId(), "johnDoe");

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertNotNull(historicTaskInstance.getClaimTime());
        assertNotNull(historicTaskInstance.getAssignee());

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskOwner() {
        Task task = startOneTaskprocess();
        assertNull(task.getOwner());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getOwner());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.setOwner(task.getId(), "johnDoe");

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("johnDoe", historicTaskInstance.getOwner());

        taskService.setOwner(task.getId(), null);
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertNull(historicTaskInstance.getOwner());

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskName() {
        Task task = startOneTaskprocess();
        assertEquals("The Task", task.getName());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("The Task", historicTaskInstance.getName());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        task.setName("new name");
        taskService.saveTask(task);

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("new name", historicTaskInstance.getName());

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskDescription() {
        Task task = startOneTaskprocess();
        assertNull(task.getDescription());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getDescription());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        task.setDescription("test description");
        taskService.saveTask(task);

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNotNull(historicTaskInstance.getDescription());

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        task.setDescription(null);
        taskService.saveTask(task);

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertNull(historicTaskInstance.getDescription());

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskDueDate() {
        Task task = startOneTaskprocess();
        assertNull(task.getDueDate());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getDueDate());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.setDueDate(task.getId(), new Date());

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNotNull(historicTaskInstance.getDueDate());

        taskService.setDueDate(task.getId(), null);
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
        assertNull(historicTaskInstance.getDueDate());

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskPriority() {
        Task task = startOneTaskprocess();
        assertEquals(Task.DEFAULT_PRIORITY, task.getPriority());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals(Task.DEFAULT_PRIORITY, historicTaskInstance.getPriority());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.setPriority(task.getId(), 1);

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals(1, historicTaskInstance.getPriority());

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskCategory() {
        Task task = startOneTaskprocess();
        assertNull(task.getCategory());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getCategory());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        task.setCategory("test category");
        taskService.saveTask(task);

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("test category", historicTaskInstance.getCategory());

        finishOneTaskProcess(task);
    }

    @Test
    public void testSetTaskFormKey() {
        Task task = startOneTaskprocess();
        assertNull(task.getFormKey());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertNull(historicTaskInstance.getFormKey());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        task.setFormKey("test form key");
        taskService.saveTask(task);

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertEquals("test form key", historicTaskInstance.getFormKey());

        finishOneTaskProcess(task);
    }

    protected void downgradeHistoryJobConfigurations() {
        managementService.executeCommand(commandContext -> {
            try {
                HistoryJob historyJob = managementService.createHistoryJobQuery().singleResult();
                List<Map<String, Object>> configurations = processEngineConfiguration.getObjectMapper().readValue(
                    new String(((HistoryJobEntity) historyJob).getAdvancedJobHandlerConfiguration().getBytes()),
                    List.class
                );

                List<Map<String, Object>> downgradedConfigurations = configurations.stream().
                    map(configuration -> {
                            Map<String, Object> data = (Map<String, Object>) configuration.get("data");
                            String type = (String) configuration.get("type");
                            data.remove(HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID);
                            if (ADD_ACTIVITY_ID_CONFIGURATIONS.contains(type)) {
                                data.put(HistoryJsonConstants.ACTIVITY_ID, "theTask");
                            }
                            HashMap<String, Object> newConfiguration = new HashMap<>();
                            newConfiguration.put("type", configuration.get("type"));
                            newConfiguration.put("data", data);
                            return newConfiguration;
                    }).
                    collect(Collectors.toList());

                ((HistoryJobEntity) historyJob).setAdvancedJobHandlerConfiguration(
                    processEngineConfiguration.getObjectMapper().writeValueAsString(downgradedConfigurations)
                );
                org.flowable.job.service.impl.util.CommandContextUtil.getHistoryJobEntityManager(commandContext).update((HistoryJobEntity) historyJob);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return null;
        });
    }

    protected void removeRuntimeActivityInstances(String processInstanceId) {
        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).deleteActivityInstancesByProcessInstanceId(processInstanceId);
            return null;
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
        assertNull(managementService.createHistoryJobQuery().singleResult());
    }

    protected void assertRuntimeActivityInstanceIsSame(HistoricActivityInstance historicActivityInstance) {
        assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());
    }

}
