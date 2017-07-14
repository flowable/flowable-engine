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

import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntityImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Job;

/**
 * @author Tom Baeyens
 */
public class JobExecutorCmdExceptionTest extends PluggableFlowableTestCase {

    protected TweetExceptionHandler tweetExceptionHandler = new TweetExceptionHandler();

    private CommandExecutor commandExecutor;

    public void setUp() throws Exception {
        processEngineConfiguration.getJobHandlers().put(tweetExceptionHandler.getType(), tweetExceptionHandler);
        this.commandExecutor = processEngineConfiguration.getCommandExecutor();
    }

    public void tearDown() throws Exception {
        processEngineConfiguration.getJobHandlers().remove(tweetExceptionHandler.getType());
    }

    public void testJobCommandsWith2Exceptions() {
        commandExecutor.execute(new Command<String>() {

            public String execute(CommandContext commandContext) {
                JobEntity message = createTweetExceptionMessage();
                CommandContextUtil.getJobManager(commandContext).scheduleAsyncJob(message);
                return message.getId();
            }
        });

        Job job = managementService.createJobQuery().singleResult();
        assertEquals(3, job.getRetries());

        try {
            managementService.executeJob(job.getId());
            fail("exception expected");
        } catch (Exception e) {
            // exception expected;
        }

        job = managementService.createTimerJobQuery().singleResult();
        assertEquals(2, job.getRetries());

        try {
            managementService.moveTimerToExecutableJob(job.getId());
            managementService.executeJob(job.getId());
            fail("exception expected");
        } catch (Exception e) {
            // exception expected;
        }

        job = managementService.createTimerJobQuery().singleResult();
        assertEquals(1, job.getRetries());

        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
    }

    public void testJobCommandsWith3Exceptions() {
        tweetExceptionHandler.setExceptionsRemaining(3);

        commandExecutor.execute(new Command<String>() {

            public String execute(CommandContext commandContext) {
                JobEntity message = createTweetExceptionMessage();
                CommandContextUtil.getJobManager(commandContext).scheduleAsyncJob(message);
                return message.getId();
            }
        });

        Job job = managementService.createJobQuery().singleResult();
        assertEquals(3, job.getRetries());

        try {
            managementService.executeJob(job.getId());
            fail("exception expected");
        } catch (Exception e) {
            // exception expected;
        }

        job = managementService.createTimerJobQuery().singleResult();
        assertEquals(2, job.getRetries());

        try {
            managementService.moveTimerToExecutableJob(job.getId());
            managementService.executeJob(job.getId());
            fail("exception expected");
        } catch (Exception e) {
            // exception expected;
        }

        job = managementService.createTimerJobQuery().singleResult();
        assertEquals(1, job.getRetries());

        try {
            managementService.moveTimerToExecutableJob(job.getId());
            managementService.executeJob(job.getId());
            fail("exception expected");
        } catch (Exception e) {
            // exception expected;
        }

        job = managementService.createDeadLetterJobQuery().singleResult();
        assertNotNull(job);

        managementService.deleteDeadLetterJob(job.getId());
    }

    protected JobEntity createTweetExceptionMessage() {
        JobEntity message = new JobEntityImpl();
        message.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        message.setRetries(3);
        message.setJobHandlerType("tweet-exception");
        return message;
    }
}
