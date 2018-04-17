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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;

/**
 * @author Tom Baeyens
 */
public class JobExecutorTest extends JobExecutorTestCase {

    public void testBasicJobExecutorOperation() throws Exception {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                JobServiceConfiguration jobServiceConfiguration = (JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
                JobManager jobManager = jobServiceConfiguration.getJobManager();
                jobManager.execute(createTweetMessage("message-one"));
                jobManager.execute(createTweetMessage("message-two"));
                jobManager.execute(createTweetMessage("message-three"));
                jobManager.execute(createTweetMessage("message-four"));

                TimerJobEntityManager timerJobManager = jobServiceConfiguration.getTimerJobEntityManager();
                timerJobManager.insert(createTweetTimer("timer-one", new Date()));
                timerJobManager.insert(createTweetTimer("timer-one", new Date()));
                timerJobManager.insert(createTweetTimer("timer-two", new Date()));
                return null;
            }
        });

        GregorianCalendar currentCal = new GregorianCalendar();
        currentCal.add(Calendar.MINUTE, 1);
        processEngineConfiguration.getClock().setCurrentTime(currentCal.getTime());

        waitForJobExecutorToProcessAllJobs(8000L, 200L);

        Set<String> messages = new HashSet<>(tweetHandler.getMessages());
        Set<String> expectedMessages = new HashSet<>();
        expectedMessages.add("message-one");
        expectedMessages.add("message-two");
        expectedMessages.add("message-three");
        expectedMessages.add("message-four");
        expectedMessages.add("timer-one");
        expectedMessages.add("timer-two");

        assertEquals(new TreeSet<>(expectedMessages), new TreeSet<>(messages));
    }
}
