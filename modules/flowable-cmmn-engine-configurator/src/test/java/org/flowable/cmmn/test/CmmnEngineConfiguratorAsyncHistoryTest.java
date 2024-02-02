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

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.HistoryJobService;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnEngineConfiguratorAsyncHistoryTest {

    private ProcessEngine processEngine;
    private CmmnEngine cmmnEngine;

    @Before
    public void setup() {
        processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.async.history.cfg.xml").buildProcessEngine();
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

        cmmnEngine.close();
        processEngine.close();
    }

    @Test
    public void testSharedAsyncHistoryExecutor() {
        // The async history executor should be the same instance
        AsyncExecutor processEngineAsyncExecutor = processEngine.getProcessEngineConfiguration().getAsyncHistoryExecutor();
        AsyncExecutor cmmnEngineAsyncExecutor = cmmnEngine.getCmmnEngineConfiguration().getAsyncHistoryExecutor();
        assertThat(processEngineAsyncExecutor).isNotNull();
        assertThat(cmmnEngineAsyncExecutor).isSameAs(processEngineAsyncExecutor);

        AsyncTaskExecutor processEngineAsyncHistoryTaskExecutor = processEngine.getProcessEngineConfiguration().getAsyncHistoryTaskExecutor();
        AsyncTaskExecutor cmmnEngineAsyncHistoryTaskExecutor = cmmnEngine.getCmmnEngineConfiguration().getAsyncHistoryTaskExecutor();
        assertThat(processEngineAsyncHistoryTaskExecutor).isNotNull();
        assertThat(cmmnEngineAsyncHistoryTaskExecutor).isSameAs(processEngineAsyncHistoryTaskExecutor);

        // Running them together should have moved the job execution scope to 'all' (from process which is null)
        assertThat(processEngine.getProcessEngineConfiguration().getAsyncHistoryExecutor().getJobServiceConfiguration().getHistoryJobExecutionScope())
                .isEqualTo(JobServiceConfiguration.JOB_EXECUTION_SCOPE_ALL);

        // 1 job handlers / engine
        assertThat(processEngineAsyncExecutor.getJobServiceConfiguration().getHistoryJobHandlers())
                .containsOnlyKeys("bpmn-test-history-job-handler", "cmmn-test-history-job-handler");

        processEngine.getManagementService()
                .executeCommand(commandContext -> {
                    HistoryJobService historyJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                            .getJobServiceConfiguration()
                            .getHistoryJobService();

                    HistoryJobEntity historyJob = historyJobService.createHistoryJob();
                    historyJob.setScopeType(JobServiceConfiguration.JOB_EXECUTION_SCOPE_ALL);
                    historyJob.setJobHandlerType("bpmn-test-history-job-handler");
                    historyJob.setRetries(3);
                    historyJob.setCreateTime(commandContext.getClock().getCurrentTime());
                    historyJobService.scheduleHistoryJob(historyJob);
                    return null;
                });

        cmmnEngine.getCmmnEngineConfiguration()
                .getCommandExecutor()
                .execute(commandContext -> {
                    HistoryJobService historyJobService = org.flowable.cmmn.engine.impl.util.CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                            .getJobServiceConfiguration()
                            .getHistoryJobService();

                    HistoryJobEntity historyJob = historyJobService.createHistoryJob();
                    historyJob.setScopeType(JobServiceConfiguration.JOB_EXECUTION_SCOPE_ALL);
                    historyJob.setJobHandlerType("cmmn-test-history-job-handler");
                    historyJob.setRetries(3);
                    historyJob.setCreateTime(commandContext.getClock().getCurrentTime());
                    historyJobService.scheduleHistoryJob(historyJob);
                    return null;
                });

        assertThat(cmmnEngine.getCmmnManagementService().createHistoryJobQuery().count()).isEqualTo(2);
        assertThat(processEngine.getManagementService().createHistoryJobQuery().count()).isEqualTo(2);

        // Starting the async history executor should process all of these
        CmmnJobTestHelper.waitForAsyncHistoryExecutorToProcessAllJobs(cmmnEngine.getCmmnEngineConfiguration(), 10000L, 200L, true);

        assertThat(cmmnEngine.getCmmnManagementService().createHistoryJobQuery().count()).isZero();
        assertThat(processEngine.getManagementService().createHistoryJobQuery().count()).isZero();
    }
}
