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
package org.activiti.engine.test.jobexecutor;

import java.util.Date;
import java.util.List;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cmd.ExecuteAsyncJobCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsCmd;

/**
 * @author Tom Baeyens
 */
public class JobExecutorCmdHappyTest extends JobExecutorTestCase {

    public void testJobCommandsWithMessage() {
        ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler().getRawProcessConfiguration();
        CommandExecutor commandExecutor = activiti5ProcessEngineConfig.getCommandExecutor();

        String jobId = commandExecutor.execute(new Command<String>() {

            public String execute(CommandContext commandContext) {
                JobEntity message = createTweetMessage("i'm coding a test");
                commandContext.getJobEntityManager().send(message);
                return message.getId();
            }
        });

        Job job = managementService.createJobQuery().singleResult();
        assertNotNull(job);
        assertEquals(jobId, job.getId());

        assertEquals(0, tweetHandler.getMessages().size());

        activiti5ProcessEngineConfig.getManagementService().executeJob(job.getId());

        assertEquals("i'm coding a test", tweetHandler.getMessages().get(0));
        assertEquals(1, tweetHandler.getMessages().size());
    }

    static final long SOME_TIME = 928374923546L;
    static final long SECOND = 1000;

    public void testJobCommandsWithTimer() {
        ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler().getRawProcessConfiguration();

        // clock gets automatically reset in LogTestCase.runTest
        Clock clock = processEngineConfiguration.getClock();
        clock.setCurrentTime(new Date(SOME_TIME));
        processEngineConfiguration.setClock(clock);

        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        CommandExecutor commandExecutor = (CommandExecutor) processEngineConfiguration.getFlowable5CompatibilityHandler().getRawCommandExecutor();

        String jobId = commandExecutor.execute(new Command<String>() {

            public String execute(CommandContext commandContext) {
                TimerJobEntity timer = createTweetTimer("i'm coding a test", new Date(SOME_TIME + (10 * SECOND)));
                commandContext.getJobEntityManager().schedule(timer);
                return timer.getId();
            }
        });

        List<org.flowable.job.service.impl.persistence.entity.TimerJobEntity> acquiredJobs = processEngineConfiguration.getCommandExecutor().execute(new AcquireTimerJobsCmd(asyncExecutor));
        assertEquals(0, acquiredJobs.size());

        clock.setCurrentTime(new Date(SOME_TIME + (20 * SECOND)));
        processEngineConfiguration.setClock(clock);

        acquiredJobs = processEngineConfiguration.getCommandExecutor().execute(new AcquireTimerJobsCmd(asyncExecutor));
        assertEquals(1, acquiredJobs.size());

        Job job = acquiredJobs.iterator().next();

        assertEquals(jobId, job.getId());

        assertEquals(0, tweetHandler.getMessages().size());

        managementService.moveTimerToExecutableJob(jobId);
        JobEntity jobEntity = (JobEntity) activiti5ProcessEngineConfig.getManagementService().createJobQuery().singleResult();
        activiti5ProcessEngineConfig.getCommandExecutor().execute(new ExecuteAsyncJobCmd(jobEntity));

        assertEquals("i'm coding a test", tweetHandler.getMessages().get(0));
        assertEquals(1, tweetHandler.getMessages().size());

        processEngineConfiguration.resetClock();
    }
}