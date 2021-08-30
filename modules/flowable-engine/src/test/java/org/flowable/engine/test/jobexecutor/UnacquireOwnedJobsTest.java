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

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.job.service.impl.cmd.AcquireJobsCmd;
import org.flowable.job.service.impl.cmd.UnacquireOwnedJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class UnacquireOwnedJobsTest extends PluggableFlowableTestCase {
    // This test case tests that jobs unlocked by the AsyncExecutor are correctly released

    private boolean asyncExecutorActivated;

    /*
        Need to disable the async executor during this test, as otherwise jobs will be picked up
        which will make it impossible to test the lock releasing logic.
     */

    @BeforeEach
    void disableAsyncExecutorIfNeeded() {
        asyncExecutorActivated = processEngineConfiguration.getAsyncExecutor().isActive();

        if (asyncExecutorActivated) {
            processEngineConfiguration.getAsyncExecutor().shutdown();
        }
    }

    @AfterEach
    void enabledAsyncExecutorIfNeeded() {
        if (asyncExecutorActivated) {
            processEngineConfiguration.getAsyncExecutor().start();
        }
    }

    @Test
    @Deployment
    void testUnlockOwnJobs() {

        // Tests the command, not the execution of the logic on close.
        // But this way, the SQL gets tested on all databases

        // Each process will have one async job
        int nrOfProcessInstances = 5;
        for (int i = 0; i < nrOfProcessInstances; i++) {
            runtimeService.startProcessInstanceByKey("myProcess");
        }

        assertThat(managementService.createJobQuery().list())
                .hasSize(5)
                .extracting(job -> ((JobInfoEntity) job).getLockOwner())
                .containsOnlyNulls();

        DefaultAsyncJobExecutor asyncExecutor = (DefaultAsyncJobExecutor) processEngineConfiguration.getAsyncExecutor();


        // Acquire jobs (mimic the async executor behavior)
        List<JobInfoEntity> acquiredJobs = new ArrayList<>();
        while (acquiredJobs.size() < 5) {
            List<? extends JobInfoEntity> jobs = processEngineConfiguration.getCommandExecutor()
                    .execute(new AcquireJobsCmd(asyncExecutor));
            acquiredJobs.addAll(jobs);
        }

        // Validate lock owner and time set after acquiring
        assertThat(acquiredJobs).hasSize(5);

        for (JobInfoEntity acquiredJob : acquiredJobs) {

            // After locking, the lockowner should be set
            assertThat(acquiredJob.getLockOwner()).isEqualTo(asyncExecutor.getLockOwner());
            assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        }

        processEngineConfiguration.getCommandExecutor()
                .execute(new UnacquireOwnedJobsCmd(asyncExecutor.getLockOwner(), asyncExecutor.getTenantId(), asyncExecutor.getJobServiceConfiguration()));

        assertThat(managementService.createJobQuery().list())
                .hasSize(5)
                .extracting(job -> ((JobInfoEntity) job).getLockOwner())
                .containsOnlyNulls();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/jobexecutor/UnacquireOwnedJobsTest.testUnlockOwnJobs.bpmn20.xml")
    void testUnlockOwnJobsDifferentTenantsUsingSpecificTenant() {

        // Tests the command, not the execution of the logic on close.
        // But this way, the SQL gets tested on all databases

        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .overrideProcessDefinitionTenantId("tenant1")
                .start();
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .overrideProcessDefinitionTenantId("tenant1")
                .start();
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .overrideProcessDefinitionTenantId("tenant2")
                .start();
        runtimeService.startProcessInstanceByKey("myProcess");
        runtimeService.startProcessInstanceByKey("myProcess");

        assertThat(managementService.createJobQuery().list())
                .hasSize(5)
                .extracting(job -> ((JobInfoEntity) job).getLockOwner())
                .containsOnlyNulls();

        DefaultAsyncJobExecutor asyncExecutor = (DefaultAsyncJobExecutor) processEngineConfiguration.getAsyncExecutor();


        // Acquire jobs (mimic the async executor behavior)
        List<JobInfoEntity> acquiredJobs = new ArrayList<>();
        while (acquiredJobs.size() < 5) {
            List<? extends JobInfoEntity> jobs = processEngineConfiguration.getCommandExecutor()
                    .execute(new AcquireJobsCmd(asyncExecutor));
            acquiredJobs.addAll(jobs);
        }

        // Validate lock owner and time set after acquiring
        assertThat(acquiredJobs).hasSize(5);

        String lockOwner = asyncExecutor.getLockOwner();
        for (JobInfoEntity acquiredJob : acquiredJobs) {

            // After locking, the lockowner should be set
            assertThat(acquiredJob.getLockOwner()).isEqualTo(lockOwner);
            assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        }

        // Unacquiring should only release the jobs from the defined tenant
        processEngineConfiguration.getCommandExecutor()
                .execute(new UnacquireOwnedJobsCmd(lockOwner, "tenant1", asyncExecutor.getJobServiceConfiguration()));

        assertThat(managementService.createJobQuery().list())
                .hasSize(5)
                .extracting(job -> StringUtils.defaultIfEmpty(job.getTenantId(), ""), job -> ((JobInfoEntity) job).getLockOwner())
                .containsExactlyInAnyOrder(
                        tuple("", lockOwner),
                        tuple("", lockOwner),
                        tuple("tenant2", lockOwner),
                        tuple("tenant1", null),
                        tuple("tenant1", null)
                );

        // Explicitly using the empty tenant should release only the jobs without a tenant id
        processEngineConfiguration.getCommandExecutor()
                .execute(new UnacquireOwnedJobsCmd(lockOwner, "", asyncExecutor.getJobServiceConfiguration()));

        assertThat(managementService.createJobQuery().list())
                .hasSize(5)
                .extracting(job -> StringUtils.defaultIfEmpty(job.getTenantId(), ""), job -> ((JobInfoEntity) job).getLockOwner())
                .containsExactlyInAnyOrder(
                        tuple("", null),
                        tuple("", null),
                        tuple("tenant2", lockOwner),
                        tuple("tenant1", null),
                        tuple("tenant1", null)
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/jobexecutor/UnacquireOwnedJobsTest.testUnlockOwnJobs.bpmn20.xml")
    void testUnlockOwnJobsDifferentTenantsUsingNullTenant() {

        // Tests the command, not the execution of the logic on close.
        // But this way, the SQL gets tested on all databases

        // Each process will have one async job
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .overrideProcessDefinitionTenantId("tenant1")
                .start();
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .overrideProcessDefinitionTenantId("tenant1")
                .start();
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .overrideProcessDefinitionTenantId("tenant2")
                .start();
        runtimeService.startProcessInstanceByKey("myProcess");
        runtimeService.startProcessInstanceByKey("myProcess");

        assertThat(managementService.createJobQuery().list())
                .hasSize(5)
                .extracting(job -> ((JobInfoEntity) job).getLockOwner())
                .containsOnlyNulls();

        DefaultAsyncJobExecutor asyncExecutor = (DefaultAsyncJobExecutor) processEngineConfiguration.getAsyncExecutor();


        // Acquire jobs (mimic the async executor behavior)
        List<JobInfoEntity> acquiredJobs = new ArrayList<>();
        while (acquiredJobs.size() < 5) {
            List<? extends JobInfoEntity> jobs = processEngineConfiguration.getCommandExecutor()
                    .execute(new AcquireJobsCmd(asyncExecutor));
            acquiredJobs.addAll(jobs);
        }

        // Validate lock owner and time set after acquiring
        assertThat(acquiredJobs).hasSize(5);

        String lockOwner = asyncExecutor.getLockOwner();
        for (JobInfoEntity acquiredJob : acquiredJobs) {

            // After locking, the lockowner should be set
            assertThat(acquiredJob.getLockOwner()).isEqualTo(lockOwner);
            assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        }

        // Unacquiring using the null tenant should release all jobs
        processEngineConfiguration.getCommandExecutor()
                .execute(new UnacquireOwnedJobsCmd(lockOwner, null, asyncExecutor.getJobServiceConfiguration()));

        assertThat(managementService.createJobQuery().list())
                .hasSize(5)
                .extracting(job -> StringUtils.defaultIfEmpty(job.getTenantId(), ""), job -> ((JobInfoEntity) job).getLockOwner())
                .containsExactlyInAnyOrder(
                        tuple("", null),
                        tuple("", null),
                        tuple("tenant2", null),
                        tuple("tenant1", null),
                        tuple("tenant1", null)
                );
    }

}
