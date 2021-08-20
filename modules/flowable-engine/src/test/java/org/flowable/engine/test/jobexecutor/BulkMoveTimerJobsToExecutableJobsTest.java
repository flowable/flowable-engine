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
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.cmd.BulkMoveTimerJobsToExecutableJobsCmd;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the bulk move of timer jobs, as this uses a where with in() clause that is limited
 * in number of elements (due to limitations of certain databases).
 * If this test passes on all db's we know that the in() clause limitation works on all supported db's.
 *
 * @author Joram Barrez
 */
public class BulkMoveTimerJobsToExecutableJobsTest extends JobExecutorTestCase  {

    private static final int NR_OF_TIMER_JOBS = AbstractDataManager.MAX_ENTRIES_IN_CLAUSE + (AbstractDataManager.MAX_ENTRIES_IN_CLAUSE/2);

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
    public void testBulkUpdateAndDelete() {

        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(commandContext -> {

            TimerJobService timerJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                .getJobServiceConfiguration()
                .getTimerJobService();

            Date now = new Date();
            for (int i = 0; i < NR_OF_TIMER_JOBS; i++) {
                TimerJobEntity timer = createTweetTimer("Timer " + i, now);
                timerJobService.scheduleTimerJob(timer);
            }

            return null;
        });

        ManagementService managementService = processEngineConfiguration.getManagementService();
        List<Job> jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs.size()).isEqualTo(NR_OF_TIMER_JOBS);

        // As there are more than AbstractDataManager.MAX_ENTRIES_IN_CLAUSE timer jobs, the logic should split it up into multiple updates
        List<TimerJobEntity> timerJobs = new ArrayList<>();
        for (Job job : jobs) {
            timerJobs.add((TimerJobEntity) job);
            assertThat(((TimerJobEntity) job).getLockOwner()).isNull();
            assertThat(((TimerJobEntity) job).getLockExpirationTime()).isNull();
        }

        // Test bulk update
        String lockOwner = "test";
        commandExecutor.execute(new Command<Object>() {

            @Override
            public Object execute(CommandContext commandContext) {
                processEngineConfiguration.getJobServiceConfiguration().getTimerJobEntityManager().bulkUpdateJobLockWithoutRevisionCheck(timerJobs, lockOwner, new Date());
                return null;
            }
        });

        for (Job job : managementService.createTimerJobQuery().list()) {
            assertThat(((TimerJobEntity) job).getLockOwner()).isEqualTo("test");
            assertThat(((TimerJobEntity) job).getLockExpirationTime()).isNotNull();
        }

        // Test bulk delete
        commandExecutor.execute(new BulkMoveTimerJobsToExecutableJobsCmd(processEngineConfiguration.getJobServiceConfiguration().getJobManager(), timerJobs));
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(0);
        assertThat(managementService.createJobQuery().count()).isEqualTo(NR_OF_TIMER_JOBS);
    }
}
