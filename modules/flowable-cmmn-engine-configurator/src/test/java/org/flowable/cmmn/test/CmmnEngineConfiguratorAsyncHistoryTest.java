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

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
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
        // Execute history jobs for the delete deployments
        processEngine.getManagementService().createHistoryJobQuery().list()
                .forEach(historyJob -> processEngine.getManagementService().executeHistoryJob(historyJob.getId()));

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

        // 2 job handlers / engine
        assertThat(processEngineAsyncExecutor.getJobServiceConfiguration().getHistoryJobHandlers()).hasSize(4);

        // Deploy and start test processes/cases
        // Trigger one plan item instance to start the process
        processEngine.getRepositoryService().createDeployment().addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").deploy();
        cmmnEngine.getCmmnRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessNonBlocking.cmmn").deploy();
        cmmnEngine.getCmmnRuntimeService().createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        cmmnEngine.getCmmnRuntimeService().triggerPlanItemInstance(
                cmmnEngine.getCmmnRuntimeService().createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult().getId());

        // As async history is enabled, there should be  no historical entries yet, but there should be history jobs
        assertThat(cmmnEngine.getCmmnHistoryService().createHistoricCaseInstanceQuery().count()).isZero();
        assertThat(processEngine.getHistoryService().createHistoricProcessInstanceQuery().count()).isZero();

        // 3 history jobs expected:
        // - one for the case instance start
        // - one for the plan item instance trigger
        // - one for the process instance start
        assertThat(cmmnEngine.getCmmnManagementService().createHistoryJobQuery().count()).isEqualTo(3);
        assertThat(processEngine.getManagementService().createHistoryJobQuery().count()).isEqualTo(3);

        // Expected 2 jobs originating from the cmmn engine and 1 for the process engine
        int cmmnHistoryJobs = 0;
        int bpmnHistoryJobs = 0;
        for (HistoryJob historyJob : cmmnEngine.getCmmnManagementService().createHistoryJobQuery().list()) {
            if (historyJob.getJobHandlerType().equals(CmmnAsyncHistoryConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY)
                    || historyJob.getJobHandlerType().equals(CmmnAsyncHistoryConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED)) {
                cmmnHistoryJobs++;
            } else if (historyJob.getJobHandlerType().equals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY)
                    || historyJob.getJobHandlerType().equals(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED)) {
                bpmnHistoryJobs++;
            }

            // Execution scope should be all (see the CmmnEngineConfigurator)
            assertThat(historyJob.getScopeType()).isEqualTo(JobServiceConfiguration.JOB_EXECUTION_SCOPE_ALL);
        }
        assertThat(bpmnHistoryJobs).isEqualTo(1);
        assertThat(cmmnHistoryJobs).isEqualTo(2);

        // Starting the async history executor should process all of these
        CmmnJobTestHelper.waitForAsyncHistoryExecutorToProcessAllJobs(cmmnEngine.getCmmnEngineConfiguration(), 10000L, 200L, true);

        assertThat(cmmnEngine.getCmmnHistoryService().createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(processEngine.getHistoryService().createHistoricProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnEngine.getCmmnManagementService().createHistoryJobQuery().count()).isZero();
        assertThat(processEngine.getManagementService().createHistoryJobQuery().count()).isZero();
    }

    @Test
    public void testProcessAsyncHistoryNotChanged() {
        // This test validates that the shared async history executor does not intervene when running a process regularly
        processEngine.getRepositoryService().createDeployment().addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").deploy();
        processEngine.getRuntimeService().startProcessInstanceByKey("oneTask");
        assertThat(processEngine.getManagementService().createHistoryJobQuery().count()).isEqualTo(1);

        HistoryJob historyJob = processEngine.getManagementService().createHistoryJobQuery().singleResult();
        assertThat(historyJob.getJobHandlerType()).isEqualTo(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY);
        processEngine.getManagementService().executeHistoryJob(historyJob.getId());
    }

}
