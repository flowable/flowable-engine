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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.FindExpiredJobsCmd;
import org.flowable.job.service.impl.asyncexecutor.ResetExpiredJobsCmd;
import org.flowable.job.service.impl.asyncexecutor.ResetExpiredJobsRunnable;
import org.flowable.job.service.impl.cmd.AcquireJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class ResetExpiredJobsTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testResetExpiredJobs() {

        // This first test 'mimics' the async executor:
        // first the job will be acquired via the lowlevel API instead of using threads
        // and then they will be reset, using the lowlevel API again.

        Date startOfTestTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startOfTestTime);

        // Starting process instance will make one job ready
        runtimeService.startProcessInstanceByKey("myProcess");
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);

        // Running the 'reset expired' logic should have no effect now
        JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        int expiredJobsPagesSize = processEngineConfiguration.getAsyncExecutorResetExpiredJobsPageSize();
        List<? extends JobInfoEntity> expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager(), jobServiceConfiguration));
        assertThat(expiredJobs).isEmpty();
        assertJobDetails(false);

        // Run the acquire logic. This should lock the job
        managementService.executeCommand(new AcquireJobsCmd(processEngineConfiguration.getAsyncExecutor()));
        assertJobDetails(true);

        // Running the 'reset expired' logic should have no effect, the lock time is not yet passed
        expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager(), jobServiceConfiguration));
        assertThat(expiredJobs).isEmpty();
        assertJobDetails(true);

        // Move clock to past the lock time
        Date newDate = new Date(startOfTestTime.getTime() + processEngineConfiguration.getAsyncExecutor().getAsyncJobLockTimeInMillis() + 10000);
        processEngineConfiguration.getClock().setCurrentTime(newDate);

        // Running the reset logic should now reset the lock
        expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize,  jobServiceConfiguration.getJobEntityManager(), jobServiceConfiguration));
        assertThat(expiredJobs).isNotEmpty();

        List<String> jobIds = new ArrayList<>();
        for (JobInfoEntity jobEntity : expiredJobs) {
            jobIds.add(jobEntity.getId());
        }

        managementService.executeCommand(new ResetExpiredJobsCmd(jobIds, jobServiceConfiguration.getJobEntityManager(), jobServiceConfiguration));
        assertJobDetails(false);

        // And it can be re-acquired
        managementService.executeCommand(new AcquireJobsCmd(processEngineConfiguration.getAsyncExecutor()));
        assertJobDetails(true);

        // Start two new process instances, those jobs should not be locked
        runtimeService.startProcessInstanceByKey("myProcess");
        runtimeService.startProcessInstanceByKey("myProcess");
        assertThat(managementService.createJobQuery().count()).isEqualTo(3);
        assertJobDetails(true);

        List<Job> unlockedJobs = managementService.createJobQuery().unlocked().list();
        assertThat(unlockedJobs).hasSize(2);
        for (Job job : unlockedJobs) {
            JobEntity jobEntity = (JobEntity) job;
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        }
    }
    
    @Test
    @Deployment
    public void testResetExpiredJobTimeout() {
        Date startOfTestTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startOfTestTime);

        runtimeService.startProcessInstanceByKey("myProcess");
        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOf(JobEntity.class);
        
        JobEntity jobEntity = (JobEntity) job;
        assertThat(jobEntity.getLockOwner()).isNull();
        assertThat(jobEntity.getLockExpirationTime()).isNull();
        
        int expiredJobsPagesSize = processEngineConfiguration.getAsyncExecutorResetExpiredJobsPageSize();
        JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        List<? extends JobInfoEntity> expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager(), jobServiceConfiguration));
        assertThat(expiredJobs).isEmpty();

        // Mimic job locking
        managementService.executeCommand(commandContext -> {
            JobEntity j = CommandContextUtil.getJobService(commandContext).findJobById(jobEntity.getId());
            j.setLockOwner(processEngineConfiguration.getAsyncExecutor().getLockOwner());
            j.setLockExpirationTime(new Date());
            return null;
        });

        // Move time to current time + 1 second. This should make the job expried
        processEngineConfiguration.getClock().setCurrentTime(new Date(new Date().getTime() + 1000));
      
        expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager(), jobServiceConfiguration));
        assertThat(expiredJobs).extracting(JobInfoEntity::getId).containsExactly(job.getId());

        List<String> jobIds = new ArrayList<>();
        for (JobInfoEntity j : expiredJobs) {
            jobIds.add(j.getId());
        }
        managementService.executeCommand(new ResetExpiredJobsCmd(jobIds, jobServiceConfiguration.getJobEntityManager(), jobServiceConfiguration));
        assertThat(managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager(), jobServiceConfiguration))).isEmpty();
        
        assertThat(managementService.createJobQuery().jobId(job.getId()).singleResult()).isNull();
        assertThat(managementService.createJobQuery().singleResult()).isNotNull();
    }

    @Test
    public void testResetRunnableContinuesUntilNoMoreToReset() {
        int nrOfJobsToCreate = processEngineConfiguration.getAsyncExecutorResetExpiredJobsPageSize() * 3;

        for (int i = 0; i < nrOfJobsToCreate; i++) {
            managementService.executeCommand(new Command<Void>() {

                @Override
                public Void execute(CommandContext commandContext) {
                    JobEntityManager jobEntityManager = processEngineConfiguration.getJobServiceConfiguration().getJobEntityManager();
                    JobEntity jobEntity = jobEntityManager.create();

                    jobEntity.setJobType("type");

                    // Set the time way back, so it's definitely expired
                    jobEntity.setLockExpirationTime(Date.from(Instant.now().minus(100, ChronoUnit.DAYS)));
                    jobEntity.setLockOwner("claimed");

                    jobEntityManager.insert(jobEntity);
                    return null;
                }
            });
        }
        assertThat(managementService.createJobQuery().count()).isEqualTo(nrOfJobsToCreate);

        // Running the reset expired runnable should trigger them all
        ResetExpiredJobsRunnable resetExpiredJobsRunnable = new ResetExpiredJobsRunnable("test-reset-expired",
            processEngineConfiguration.getAsyncExecutor(), processEngineConfiguration.getJobServiceConfiguration().getJobEntityManager());
        resetExpiredJobsRunnable.resetJobs();

        List<Job> jobs = managementService.createJobQuery().list();
        for (Job job : jobs) {
            JobEntityImpl jobEntity = (JobEntityImpl) job;
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();

            managementService.deleteJob(job.getId());
        }
    }

    protected void assertJobDetails(boolean locked) {
        JobQuery jobQuery = managementService.createJobQuery();

        if (locked) {
            jobQuery.locked();
        }

        Job job = jobQuery.singleResult();
        assertThat(job).isInstanceOf(JobEntity.class);
        JobEntity jobEntity = (JobEntity) job;

        if (locked) {
            assertThat(jobEntity.getLockOwner()).isNotNull();
            assertThat(jobEntity.getLockExpirationTime()).isNotNull();
        } else {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        }
    }

}
