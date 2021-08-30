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
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.HistoryJobService;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the bulk update of async jobs, as this uses a where with in() clause that is limited
 * in number of elements (due to limitations of certain databases).
 *
 * If this test passes on all db's we know that the in() clause limitation works on all supported db's.
 *
 * @author Joram Barrez
 */
public class BulkUpdateJobLockTest extends JobExecutorTestCase  {

    private static final int NR_JOBS = AbstractDataManager.MAX_ENTRIES_IN_CLAUSE + (AbstractDataManager.MAX_ENTRIES_IN_CLAUSE/2);

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super.configureConfiguration(processEngineConfiguration);

        // Make sure more timer jobs are fetched in one go than possible in the in() clause, so the logic to split is used.
        processEngineConfiguration.getAsyncExecutorConfiguration().setMaxAsyncJobsDuePerAcquisition(NR_JOBS);
        if (processEngineConfiguration.getAsyncExecutor() != null) {
            processEngineConfiguration.getAsyncExecutor().setMaxAsyncJobsDuePerAcquisition(NR_JOBS);
        }
    }

    @AfterEach
    public void cleanup() {
        // Need to use low level entity manager, as jobs can't be deleted if they're locked (i.e. not through mgmtService)
        processEngineConfiguration.getCommandExecutor().execute(new Command<Object>() {

            @Override
            public Object execute(CommandContext commandContext) {
                for (Job job : processEngineConfiguration.getManagementService().createJobQuery().list()) {
                    processEngineConfiguration.getJobServiceConfiguration().getJobEntityManager().delete(job.getId());
                }
                for (HistoryJob historyJob : processEngineConfiguration.getManagementService().createHistoryJobQuery().list()) {
                    processEngineConfiguration.getJobServiceConfiguration().getHistoryJobEntityManager().delete(historyJob.getId());
                }
                return null;
            }
        });
    }

    @Test
    public void testAsyncJobBulkUpdate() {

        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(commandContext -> {

            JobService jobService = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                .getJobServiceConfiguration()
                .getJobService();

            Date now = new Date();
            for (int i = 0; i < NR_JOBS; i++) {
                JobEntity job = createTweetMessage("Job " + i);
                jobService.scheduleAsyncJob(job);
            }

            return null;
        });

        ManagementService managementService = processEngineConfiguration.getManagementService();
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs.size()).isEqualTo(NR_JOBS);

        // As there are more than AbstractDataManager.MAX_ENTRIES_IN_CLAUSE timer jobs, the logic should split it up into multiple updates
        List<JobEntity> jobEntities = new ArrayList<>();
        for (Job job : jobs) {
            jobEntities.add((JobEntity) job);
            assertThat(((JobEntity) job).getLockOwner()).isNull();
            assertThat(((JobEntity) job).getLockExpirationTime()).isNull();
        }

        // Test bulk update
        String lockOwner = "test";
        commandExecutor.execute(commandContext -> {
            processEngineConfiguration.getJobServiceConfiguration().getJobEntityManager().bulkUpdateJobLockWithoutRevisionCheck(jobEntities, lockOwner, new Date());
            return null;
        });

        for (Job job : managementService.createJobQuery().list()) {
            assertThat(((JobEntity) job).getLockOwner()).isEqualTo("test");
            assertThat(((JobEntity) job).getLockExpirationTime()).isNotNull();
        }
    }

    @Test
    public void testHistoryJobBulkUpdate() {

        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(commandContext -> {

            HistoryJobService historyJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                .getJobServiceConfiguration()
                .getHistoryJobService();

            Date now = new Date();
            for (int i = 0; i < NR_JOBS; i++) {
                HistoryJobEntity historyJob = historyJobService.createHistoryJob();
                historyJobService.scheduleHistoryJob(historyJob);
            }

            return null;
        });

        ManagementService managementService = processEngineConfiguration.getManagementService();
        List<HistoryJob> jobs = managementService.createHistoryJobQuery().list();
        assertThat(jobs.size()).isEqualTo(NR_JOBS);

        // As there are more than AbstractDataManager.MAX_ENTRIES_IN_CLAUSE timer jobs, the logic should split it up into multiple updates
        List<HistoryJobEntity> historyJobEntities = new ArrayList<>();
        for (HistoryJob job : jobs) {
            historyJobEntities.add((HistoryJobEntity) job);
            assertThat(((HistoryJobEntity) job).getLockOwner()).isNull();
            assertThat(((HistoryJobEntity) job).getLockExpirationTime()).isNull();
        }

        // Test bulk update
        String lockOwner = "test";
        commandExecutor.execute(commandContext -> {
            processEngineConfiguration.getJobServiceConfiguration().getHistoryJobEntityManager().bulkUpdateJobLockWithoutRevisionCheck(historyJobEntities, lockOwner, new Date());
            return null;
        });

        for (Job job : managementService.createJobQuery().list()) {
            assertThat(((HistoryJobEntity) job).getLockOwner()).isEqualTo("test");
            assertThat(((HistoryJobEntity) job).getLockExpirationTime()).isNotNull();
        }
    }
}
