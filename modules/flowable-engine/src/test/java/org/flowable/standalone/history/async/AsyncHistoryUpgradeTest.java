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
import java.nio.charset.StandardCharsets;
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
        processEngineConfiguration.setAsyncHistoryJsonGroupingThreshold(1);
        processEngineConfiguration.setAsyncHistoryJsonGzipCompressionEnabled(false);
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
        assertThat(jobs.size()).isPositive();

        removeRuntimeActivityInstances(processInstanceId);
        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(70000L, 200L);
        assertThat(managementService.createHistoryJobQuery().singleResult()).isNull();

        // 203 -> (start, 1) + -->(1) + (service task, 50) + -->(50) + (gateway, 50), + <--(49) + -->(1) + (end, 1)
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).count()).isEqualTo(203);
    }

    @Test
    public void testTaskAssigneeChange() {
        Task task = startOneTaskprocess();

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                .activityId("theTask").singleResult();
        assertThat(historicActivityInstance.getAssignee()).isEqualTo("kermit");
        assertThat(runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).count())
                .as("RuntimeActivityInstances are not created back from historicActivityInstances when they are asynchronously inserted into data store")
                .isZero();

        task = taskService.createTaskQuery().singleResult();

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.setAssignee(task.getId(), "johnDoe");
        waitForHistoryJobExecutorToProcessAllJobs(7000L, 100L);
        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
        assertThat(historicActivityInstance.getAssignee()).isEqualTo("johnDoe");
        assertRuntimeActivityInstanceIsSame(historicActivityInstance);

        finishOneTaskProcess(task);
    }

    @Test
    public void testClaimTask() {
        Task task = startOneTaskprocess();

        downgradeHistoryJobConfigurations();
        waitForHistoryJobExecutorToProcessAllJobs(20000L, 100L);

        taskService.setAssignee(task.getId(), null);
        downgradeHistoryJobConfigurations();
        waitForHistoryJobExecutorToProcessAllJobs(20000L, 100L);

        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getClaimTime()).isNull();
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().processInstanceId(task.getProcessInstanceId())
                .activityId("theTask").singleResult();
        assertThat(historicActivityInstance).extracting(HistoricActivityInstance::getTaskId).isEqualTo(task.getId());

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.claim(historicTaskInstance.getId(), "johnDoe");

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(20000L, 100L);
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

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.setOwner(task.getId(), "johnDoe");

        downgradeHistoryJobConfigurations();

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

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        task.setName("new name");
        taskService.saveTask(task);

        downgradeHistoryJobConfigurations();

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

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        task.setDescription("test description");
        taskService.saveTask(task);

        downgradeHistoryJobConfigurations();

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

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.setDueDate(task.getId(), new Date());

        downgradeHistoryJobConfigurations();

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

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        taskService.setPriority(task.getId(), 1);

        downgradeHistoryJobConfigurations();

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

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        task.setCategory("test category");
        taskService.saveTask(task);

        downgradeHistoryJobConfigurations();

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

        removeRuntimeActivityInstances(task.getProcessInstanceId());

        task.setFormKey("test form key");
        taskService.saveTask(task);

        downgradeHistoryJobConfigurations();

        waitForHistoryJobExecutorToProcessAllJobs(10000L, 100L);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getFormKey()).isEqualTo("test form key");

        finishOneTaskProcess(task);
    }

    protected void downgradeHistoryJobConfigurations() {
        managementService.executeCommand(commandContext -> {
            try {
                HistoryJob historyJob = managementService.createHistoryJobQuery().singleResult();
                List<Map<String, Object>> configurations = processEngineConfiguration.getObjectMapper().readValue(
                        new String(((HistoryJobEntity) historyJob).getAdvancedJobHandlerConfiguration().getBytes(), StandardCharsets.UTF_8),
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
                processEngineConfiguration.getJobServiceConfiguration().getHistoryJobEntityManager().update((HistoryJobEntity) historyJob);
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
        waitForHistoryJobExecutorToProcessAllJobs(10_000L, 100L);
        assertThat(managementService.createHistoryJobQuery().singleResult()).isNull();
    }

    protected void assertRuntimeActivityInstanceIsSame(HistoricActivityInstance historicActivityInstance) {
        assertActivityInstancesAreSame(historicActivityInstance,
                runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());
    }

}
