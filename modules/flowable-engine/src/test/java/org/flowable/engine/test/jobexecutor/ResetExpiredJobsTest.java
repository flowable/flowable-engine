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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.FindExpiredJobsCmd;
import org.flowable.job.service.impl.asyncexecutor.ResetExpiredJobsCmd;
import org.flowable.job.service.impl.cmd.AcquireJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;

/**
 * @author Joram Barrez
 */
public class ResetExpiredJobsTest extends PluggableFlowableTestCase {

    @Deployment
    public void testResetExpiredJobs() {

        // This first test 'mimics' the async executor:
        // first the job will be acquired via the lowlevel API instead of using threads
        // and then they will be reset, using the lowlevel API again.

        Date startOfTestTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startOfTestTime);

        // Starting process instance will make one job ready
        runtimeService.startProcessInstanceByKey("myProcess");
        assertEquals(1, managementService.createJobQuery().count());

        // Running the 'reset expired' logic should have no effect now
        JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        int expiredJobsPagesSize = processEngineConfiguration.getAsyncExecutorResetExpiredJobsPageSize();
        List<? extends JobInfoEntity> expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager()));
        assertEquals(0, expiredJobs.size());
        assertJobDetails(false);

        // Run the acquire logic. This should lock the job
        managementService.executeCommand(new AcquireJobsCmd(processEngineConfiguration.getAsyncExecutor()));
        assertJobDetails(true);

        // Running the 'reset expired' logic should have no effect, the lock time is not yet passed
        expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager()));
        assertEquals(0, expiredJobs.size());
        assertJobDetails(true);

        // Move clock to past the lock time
        Date newDate = new Date(startOfTestTime.getTime() + processEngineConfiguration.getAsyncExecutor().getAsyncJobLockTimeInMillis() + 10000);
        processEngineConfiguration.getClock().setCurrentTime(newDate);

        // Running the reset logic should now reset the lock
        expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize,  jobServiceConfiguration.getJobEntityManager()));
        assertTrue(expiredJobs.size() > 0);

        List<String> jobIds = new ArrayList<>();
        for (JobInfoEntity jobEntity : expiredJobs) {
            jobIds.add(jobEntity.getId());
        }

        managementService.executeCommand(new ResetExpiredJobsCmd(jobIds, jobServiceConfiguration.getJobEntityManager()));
        assertJobDetails(false);

        // And it can be re-acquired
        managementService.executeCommand(new AcquireJobsCmd(processEngineConfiguration.getAsyncExecutor()));
        assertJobDetails(true);

        // Start two new process instances, those jobs should not be locked
        runtimeService.startProcessInstanceByKey("myProcess");
        runtimeService.startProcessInstanceByKey("myProcess");
        assertEquals(3, managementService.createJobQuery().count());
        assertJobDetails(true);

        List<Job> unlockedJobs = managementService.createJobQuery().unlocked().list();
        assertEquals(2, unlockedJobs.size());
        for (Job job : unlockedJobs) {
            JobEntity jobEntity = (JobEntity) job;
            assertNull(jobEntity.getLockOwner());
            assertNull(jobEntity.getLockExpirationTime());
        }
    }
    
    @Deployment
    public void testResetExpiredJobTimeout() {
        Date startOfTestTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startOfTestTime);

        runtimeService.startProcessInstanceByKey("myProcess");
        Job job = managementService.createJobQuery().singleResult();
        assertNotNull(job);
        assertTrue(job instanceof JobEntity);
        
        JobEntity jobEntity = (JobEntity) job;
        assertNull(jobEntity.getLockOwner());
        assertNull(jobEntity.getLockExpirationTime());
        
        int expiredJobsPagesSize = processEngineConfiguration.getAsyncExecutorResetExpiredJobsPageSize();
        JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        List<? extends JobInfoEntity> expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager()));
        assertEquals(0, expiredJobs.size());
        
        // Move time to timeout + 1 second. This should trigger the max timeout and the job should be reset (unacquired: reinserted as a new job)
        processEngineConfiguration.getClock().setCurrentTime(new Date(startOfTestTime.getTime() + (processEngineConfiguration.getAsyncExecutorResetExpiredJobsMaxTimeout() + 1)));
      
        expiredJobs = managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager()));
        assertEquals(1, expiredJobs.size());
        assertEquals(job.getId(), expiredJobs.get(0).getId());
        assertJobDetails(false);

        List<String> jobIds = new ArrayList<>();
        for (JobInfoEntity j : expiredJobs) {
            jobIds.add(j.getId());
        }
        managementService.executeCommand(new ResetExpiredJobsCmd(jobIds, jobServiceConfiguration.getJobEntityManager()));
        assertEquals(0, managementService.executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getJobEntityManager())).size());
        
        assertNull(managementService.createJobQuery().jobId(job.getId()).singleResult());
        assertNotNull(managementService.createJobQuery().singleResult());
    }

    protected void assertJobDetails(boolean locked) {
        JobQuery jobQuery = managementService.createJobQuery();

        if (locked) {
            jobQuery.locked();
        }

        Job job = jobQuery.singleResult();
        assertTrue(job instanceof JobEntity);
        JobEntity jobEntity = (JobEntity) job;

        if (locked) {
            assertNotNull(jobEntity.getLockOwner());
            assertNotNull(jobEntity.getLockExpirationTime());
        } else {
            assertNull(jobEntity.getLockOwner());
            assertNull(jobEntity.getLockExpirationTime());
        }
    }

}
