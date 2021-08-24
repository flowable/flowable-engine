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
package org.flowable.cmmn.test.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.cmd.ClearCaseInstanceLockTimesCmd;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityImpl;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.cmd.AcquireJobsCmd;
import org.flowable.job.service.impl.cmd.LockExclusiveJobCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

/**
 * @author Joram Barrez
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClearCaseInstanceLocksTest extends FlowableCmmnTestCase {

    private boolean asyncExecutorActivated;

    /*
        Need to disable the async executor during this test, as otherwise jobs will be picked up
        which will make it impossible to test the lock releasing logic.
     */

    @BeforeEach
    public void disableAsyncExecutorIfNeeded() {
        asyncExecutorActivated = cmmnEngineConfiguration.getAsyncExecutor().isActive();

        if (asyncExecutorActivated) {
            cmmnEngineConfiguration.getAsyncExecutor().shutdown();
        }
    }

    @AfterEach
    public void enabledAsyncExecutorIfNeeded() {
        if (asyncExecutorActivated) {
            cmmnEngineConfiguration.getAsyncExecutor().start();
        }
    }

    @Test
    @CmmnDeployment
    public void testClearCaseInstanceCommand() {
        
        // Tests the command, not the execution of the logic on close.
        // But this way, the SQL gets tested on all databases

        // Each case will have one async job
        int nrOfProcessInstances = 5;
        for (int i = 0; i < nrOfProcessInstances; i++) {
            cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        }

        assertThat(cmmnManagementService.createJobQuery().list()).hasSize(5);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
            .extracting(caseInstance -> ((CaseInstanceEntityImpl) caseInstance).getLockOwner(), caseInstance -> ((CaseInstanceEntityImpl) caseInstance).getLockOwner())
            .containsOnly(tuple(null, null));

        // Acquire jobs (mimic the async executor behavior)
        List<JobInfoEntity> acquiredJobs = new ArrayList<>();
        while (acquiredJobs.size() < 5) {
            List<? extends JobInfoEntity> jobs = cmmnEngineConfiguration.getCommandExecutor()
                .execute(new AcquireJobsCmd(cmmnEngineConfiguration.getAsyncExecutor()));
            acquiredJobs.addAll(jobs);
        }

        // Validate lock owner and time set after acquiring
        assertThat(acquiredJobs).hasSize(5);
        for (JobInfoEntity acquiredJob : acquiredJobs) {

            // Mimic the async executor
            cmmnEngineConfiguration.getCommandExecutor().execute(new LockExclusiveJobCmd((Job) acquiredJob, cmmnEngineConfiguration.getJobServiceConfiguration()));

            // After locking, the lockowner should be shared by the job and the process instance
            assertThat(acquiredJob.getLockOwner()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutor().getLockOwner());
            assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

            CaseInstanceEntity caseInstanceEntity = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(((JobEntity) acquiredJob).getScopeId()).singleResult();
            assertThat(acquiredJob.getLockOwner()).isEqualTo(caseInstanceEntity.getLockOwner());
            assertThat(caseInstanceEntity.getLockTime()).isNotNull();

        }

        // Clearing the locks should now remove the lock owner and lock time from all process instances
        cmmnEngineConfiguration.getCommandExecutor().execute(new ClearCaseInstanceLockTimesCmd(cmmnEngineConfiguration.getAsyncExecutor().getLockOwner(), cmmnEngineConfiguration));

        for (CaseInstance caseInstance : cmmnRuntimeService.createCaseInstanceQuery().list()) {
            assertThat(((CaseInstanceEntity) caseInstance).getLockTime()).isNull();
            assertThat(((CaseInstanceEntity) caseInstance).getLockOwner()).isNull();
        }

    }

}
