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
package org.flowable.engine.test.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Daniel Meyer
 */
public class ProcessInstanceSuspensionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/db/oneJobProcess.bpmn20.xml" })
    public void testJobsNotVisibleToAcquisitionIfInstanceSuspended() {

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(pd.getKey());

        // now there is one job:
        Job job = managementService.createTimerJobQuery().singleResult();
        assertThat(job).isNotNull();

        makeSureJobDue(job);

        // the acquirejobs command sees the job:
        List<TimerJobEntity> acquiredJobs = executeAcquireJobsCommand();
        assertThat(acquiredJobs).hasSize(1);

        // suspend the process instance:
        runtimeService.suspendProcessInstanceById(pi.getId());

        // now, the acquirejobs command does not see the job:
        acquiredJobs = executeAcquireJobsCommand();
        assertThat(acquiredJobs).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/db/oneJobProcess.bpmn20.xml" })
    public void testJobsNotVisibleToAcquisitionIfDefinitionSuspended() {

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();
        runtimeService.startProcessInstanceByKey(pd.getKey());

        // now there is one job:
        Job job = managementService.createTimerJobQuery().singleResult();
        assertThat(job).isNotNull();

        makeSureJobDue(job);

        // the acquire jobs command sees the job:
        List<TimerJobEntity> acquiredJobs = executeAcquireJobsCommand();
        assertThat(acquiredJobs).hasSize(1);

        // suspend the process instance:
        repositoryService.suspendProcessDefinitionById(pd.getId(), true, null);

        // now, the acquire jobs command does not see the job:
        acquiredJobs = executeAcquireJobsCommand();
        assertThat(acquiredJobs).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/db/oneJobProcess.bpmn20.xml" })
    public void testJobsVisibleToAcquisitionIfDefinitionSuspendedWithoutProcessInstances() {

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();
        runtimeService.startProcessInstanceByKey(pd.getKey());

        // now there is one job:
        Job job = managementService.createTimerJobQuery().singleResult();
        assertThat(job).isNotNull();

        makeSureJobDue(job);

        // the acquire jobs command sees the job:
        List<TimerJobEntity> acquiredJobs = executeAcquireJobsCommand();
        assertThat(acquiredJobs).hasSize(1);

        // suspend the process instance:
        repositoryService.suspendProcessDefinitionById(pd.getId());

        // the acquire jobs command still sees the job, because the process instances are not suspended:
        acquiredJobs = executeAcquireJobsCommand();
        assertThat(acquiredJobs).hasSize(1);
    }

    @Test
    @Deployment
    public void testSuspendedProcessTimerExecution() throws Exception {
        // Process with boundary timer-event that fires in 1 hour
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("suspendProcess");
        assertThat(procInst).isNotNull();
        assertThat(managementService.createTimerJobQuery().processInstanceId(procInst.getId()).count()).isEqualTo(1);

        // Roll time ahead to be sure timer is due to fire
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());

        // Check if timer is eligible to be executed, when process in not yet suspended
        final JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        List<TimerJobEntity> jobs = commandExecutor.execute(new Command<List<TimerJobEntity>>() {

            @Override
            public List<TimerJobEntity> execute(CommandContext commandContext) {
                return jobServiceConfiguration.getTimerJobEntityManager().findJobsToExecute(null, new Page(0, 1));
            }

        });
        assertThat(jobs).hasSize(1);

        // Suspend process instance
        runtimeService.suspendProcessInstanceById(procInst.getId());

        // Check if the timer is NOT acquired, even though the duedate is reached
        jobs = commandExecutor.execute(new Command<List<TimerJobEntity>>() {

            @Override
            public List<TimerJobEntity> execute(CommandContext commandContext) {
                return jobServiceConfiguration.getTimerJobEntityManager().findJobsToExecute(null, new Page(0, 1));
            }
        });

        assertThat(jobs).isEmpty();
    }

    protected void makeSureJobDue(final Job job) {
        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                Date currentTime = processEngineConfiguration.getClock().getCurrentTime();
                TimerJobService timerJobService = processEngineConfiguration.getJobServiceConfiguration().getTimerJobService();
                timerJobService.findTimerJobById(job.getId()).setDuedate(new Date(currentTime.getTime() - 10000));
                return null;
            }

        });
    }

    protected List<TimerJobEntity> executeAcquireJobsCommand() {
        return processEngineConfiguration.getCommandExecutor().execute(new Command<List<TimerJobEntity>>() {
            @Override
            public List<TimerJobEntity> execute(CommandContext commandContext) {
                JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
                return jobServiceConfiguration.getTimerJobEntityManager().findJobsToExecute(null, new Page(0, 1));
            }

        });
    }

}
