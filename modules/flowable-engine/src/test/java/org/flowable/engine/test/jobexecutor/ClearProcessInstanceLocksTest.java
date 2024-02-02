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
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.impl.cmd.ClearProcessInstanceLockTimesCmd;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.cmd.AcquireJobsCmd;
import org.flowable.job.service.impl.cmd.LockExclusiveJobCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * @author Joram Barrez
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClearProcessInstanceLocksTest extends PluggableFlowableTestCase {

    private boolean asyncExecutorActivated;

    /*
        Need to disable the async executor during this test, as otherwise jobs will be picked up
        which will make it impossible to test the lock releasing logic.
     */

    @BeforeEach
    public void disableAsyncExecutorIfNeeded() {
        asyncExecutorActivated = processEngineConfiguration.getAsyncExecutor().isActive();

        if (asyncExecutorActivated) {
            processEngineConfiguration.getAsyncExecutor().shutdown();
        }
    }

    @AfterEach
    public void enabledAsyncExecutorIfNeeded() {
        if (asyncExecutorActivated) {
            processEngineConfiguration.getAsyncExecutor().start();
        }
    }

    @Test
    @Deployment
    public void testClearProcessInstanceCommand() {

        // Tests the command, not the execution of the logic on close.
        // But this way, the SQL gets tested on all databases

        // Each process will have one async job
        int nrOfProcessInstances = 5;
        for (int i = 0; i < nrOfProcessInstances; i++) {
            runtimeService.startProcessInstanceByKey("myProcess");
        }

        assertThat(managementService.createJobQuery().list()).hasSize(5);
        assertThat(runtimeService.createProcessInstanceQuery().list())
            .extracting(processInstance -> ((ExecutionEntity) processInstance).getLockOwner(), processInstance -> ((ExecutionEntity) processInstance).getLockOwner())
            .containsOnly(tuple(null, null));

        // Acquire jobs (mimic the async executor behavior)
        List<JobInfoEntity> acquiredJobs = new ArrayList<>();
        while (acquiredJobs.size() < 5) {
            List<? extends JobInfoEntity> jobs = processEngineConfiguration.getCommandExecutor()
                .execute(new AcquireJobsCmd(processEngineConfiguration.getAsyncExecutor()));
            acquiredJobs.addAll(jobs);
        }

        // Validate lock owner and time set after acquiring
        assertThat(acquiredJobs).hasSize(5);
        for (JobInfoEntity acquiredJob : acquiredJobs) {

            // Mimic the async executor
            processEngineConfiguration.getCommandExecutor().execute(new LockExclusiveJobCmd((Job) acquiredJob, processEngineConfiguration.getJobServiceConfiguration()));

            // After locking, the lockowner should be shared by the job and the process instance
            assertThat(acquiredJob.getLockOwner()).isEqualTo(processEngineConfiguration.getAsyncExecutor().getLockOwner());
            assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

            ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(((JobEntity) acquiredJob).getProcessInstanceId()).singleResult();
            assertThat(acquiredJob.getLockOwner()).isEqualTo(executionEntity.getLockOwner());
            assertThat(executionEntity.getLockTime()).isNotNull();

        }

        // Clearing the locks should now remove the lock owner and lock time from all process instances
        processEngineConfiguration.getCommandExecutor().execute(new ClearProcessInstanceLockTimesCmd(processEngineConfiguration.getAsyncExecutor().getLockOwner()));

        for (Execution execution : runtimeService.createExecutionQuery().list()) {
            assertThat(((ExecutionEntity) execution).getLockTime()).isNull();
            assertThat(((ExecutionEntity) execution).getLockOwner()).isNull();
        }

    }

}
