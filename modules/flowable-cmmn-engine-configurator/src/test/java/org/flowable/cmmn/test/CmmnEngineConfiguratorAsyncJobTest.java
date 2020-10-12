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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnEngineConfiguratorAsyncJobTest {

    private ProcessEngine processEngine;
    private CmmnEngine cmmnEngine;

    @Before
    public void setup() {
        processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.async.cfg.xml").buildProcessEngine();
        cmmnEngine = CmmnEngines.getDefaultCmmnEngine();
    }

    @After
    public void cleanup() {
        processEngine.getRepositoryService()
                .createDeploymentQuery()
                .list()
                .forEach(deployment -> processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true));
        cmmnEngine.getCmmnRepositoryService()
                .createDeploymentQuery()
                .list()
                .forEach(deployment -> cmmnEngine.getCmmnRepositoryService().deleteDeployment(deployment.getId(), true));
        // Execute history jobs for the delete deployments
        processEngine.getManagementService().createHistoryJobQuery().list()
                .forEach(historyJob -> processEngine.getManagementService().executeHistoryJob(historyJob.getId()));

        cmmnEngine.close();
        processEngine.close();
    }

    @Test
    public void testSharedAsyncExecutor() throws Exception {
        // The async executor should be the same instance
        AsyncExecutor processEngineAsyncExecutor = processEngine.getProcessEngineConfiguration().getAsyncExecutor();
        AsyncExecutor cmmnEngineAsyncExecutor = cmmnEngine.getCmmnEngineConfiguration().getAsyncExecutor();
        assertThat(processEngineAsyncExecutor).isNotNull();
        assertThat(cmmnEngineAsyncExecutor).isNotNull();

        // Contrary to the asyncHistoryExecutor, the async executors are not shared between the engines (by default)
        assertThat(cmmnEngineAsyncExecutor).isNotSameAs(processEngineAsyncExecutor);

        assertThat(processEngineAsyncExecutor.getJobServiceConfiguration().getJobExecutionScope()).isNull();
        assertThat(cmmnEngineAsyncExecutor.getJobServiceConfiguration().getJobExecutionScope()).isEqualTo(JobServiceConfiguration.JOB_EXECUTION_SCOPE_CMMN);

        // Deploy and start test processes/cases
        // Trigger one plan item instance to start the process
        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CmmnEngineConfiguratorAsyncJobTest.taskAndTimer.bpmn20.xml").deploy();
        cmmnEngine.getCmmnRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CmmnEngineConfiguratorAsyncJobTest.processAndTimer.cmmn.xml").deploy();


        // Starting the case instance starts the process task. The process has an async job at the beginning
        cmmnEngine.getCmmnRuntimeService().createCaseInstanceBuilder().caseDefinitionKey("timerAndProcess").start();

        // One timer job should exist for the timer event listener
        Job timerEventListenerJob = cmmnEngine.getCmmnManagementService().createTimerJobQuery().singleResult();
        assertThat(timerEventListenerJob).isNotNull();

        Job job = processEngine.getManagementService().createJobQuery().singleResult();
        assertThat(job.getScopeType()).isNull();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngine.getProcessEngineConfiguration(), processEngine.getManagementService(), 10000L, 100L);
        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        processEngine.getTaskService().complete(task.getId());

        List<Job> timerJobs = processEngine.getManagementService().createTimerJobQuery().list();
        assertThat(timerJobs).hasSize(2); // There should now be two timers, one for the case and one for the process
        timerJobs.forEach(timerJob -> {
            if (timerJob.getScopeId() != null) { // cmmn
                assertThat(timerJob.getScopeType()).isEqualTo(JobServiceConfiguration.JOB_EXECUTION_SCOPE_CMMN);
            } else {
                assertThat(timerJob.getScopeType()).isNull();
            }

            processEngine.getManagementService().moveTimerToExecutableJob(timerJob.getId());
        });

        // Can't use the JobTestHelper's, so manually starting the executors
        processEngineAsyncExecutor.start();
        cmmnEngineAsyncExecutor.start();

        try {
            long startTime = new Date().getTime();
            while (processEngine.getTaskService().createTaskQuery().count() != 2) { // 2 tasks = stable state
                Thread.sleep(100L);
            }
        } finally {
            processEngineAsyncExecutor.shutdown();
            cmmnEngineAsyncExecutor.shutdown();
        }

        // There should be two user tasks now: one after the timer of the case and one after the timer of the process
        assertThat(processEngine.getTaskService().createTaskQuery().count()).isEqualTo(2);
        assertThat(cmmnEngine.getCmmnTaskService().createTaskQuery().count()).isEqualTo(2);

    }

}
