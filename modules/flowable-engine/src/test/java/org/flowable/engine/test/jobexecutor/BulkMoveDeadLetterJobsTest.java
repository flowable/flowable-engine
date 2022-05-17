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
package org.flowable.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the bulk move of timer jobs, as this uses a where with in() clause that is limited
 * in number of elements (due to limitations of certain databases).
 * If this test passes on all db's we know that the in() clause limitation works on all supported db's.
 *
 * @author Christopher Welsch
 */
public class BulkMoveDeadLetterJobsTest extends JobExecutorTestCase {

    private static final int NR_OF_TIMER_JOBS = AbstractDataManager.MAX_ENTRIES_IN_CLAUSE + (AbstractDataManager.MAX_ENTRIES_IN_CLAUSE / 2);

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super.configureConfiguration(processEngineConfiguration);

        // Make sure more timer jobs are fetched in one go than possible in the in() clause, so the logic to split is used.
        processEngineConfiguration.getAsyncExecutorConfiguration().setMaxTimerJobsPerAcquisition(NR_OF_TIMER_JOBS);
        if (processEngineConfiguration.getAsyncExecutor() != null) {
            processEngineConfiguration.getAsyncExecutor().setMaxTimerJobsPerAcquisition(NR_OF_TIMER_JOBS);
        }
    }

    @AfterEach
    public void cleanup() {
        for (Job timerJob : processEngineConfiguration.getManagementService().createTimerJobQuery().list()) {
            processEngineConfiguration.getManagementService().deleteTimerJob(timerJob.getId());
        }
        for (Job job : processEngineConfiguration.getManagementService().createJobQuery().list()) {
            processEngineConfiguration.getManagementService().deleteJob(job.getId());
        }
    }

    @Test
    @Deployment
    public void testBulkMoveDeadLetterJobs() {
        // process throws no exception. Service task passes at the first time.
        ProcessInstance instance1 = runtimeService.createProcessInstanceBuilder().variable("fail", true).processDefinitionKey("failedServiceTask").start();
        ProcessInstance instance2 = runtimeService.createProcessInstanceBuilder().variable("fail", true).processDefinitionKey("failedServiceTask").start();

        waitForJobExecutorToProcessAllJobs(1000, 200);

        Job jobOfInstance1 = managementService.createTimerJobQuery().processInstanceId(instance1.getId()).singleResult();
        managementService.setTimerJobRetries(jobOfInstance1.getId(), 0);
        managementService.moveJobToDeadLetterJob(jobOfInstance1.getId());

        Job jobOfInstance2 = managementService.createTimerJobQuery().processInstanceId(instance2.getId()).singleResult();
        managementService.setTimerJobRetries(jobOfInstance2.getId(), 0);
        managementService.moveJobToDeadLetterJob(jobOfInstance2.getId());

        executeJobExecutorForTime(1000, 500);

        List<Job> deadLetterJobs = managementService.createDeadLetterJobQuery().list();
        assertThat(deadLetterJobs.size()).isEqualTo(2);

        runtimeService.setVariable(instance1.getId(), "fail", false);
        runtimeService.setVariable(instance2.getId(), "fail", false);

        executeJobExecutorForTime(1000, 500);

        List<String> jobIds = new ArrayList<>(2);
        jobIds.add(jobOfInstance1.getId());
        jobIds.add(jobOfInstance2.getId());

        managementService.bulkMoveDeadLetterJobs(jobIds, 0);

        executeJobExecutorForTime(1000, 500);

        deadLetterJobs = managementService.createDeadLetterJobQuery().list();
        assertThat(deadLetterJobs.size()).isEqualTo(0);

    }
}
