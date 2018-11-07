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

import java.util.Date;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Tom Baeyens
 */
public abstract class JobExecutorTestCase extends PluggableFlowableTestCase {

    protected TweetHandler tweetHandler = new TweetHandler();

    @BeforeEach
    public void setUp() throws Exception {
        processEngineConfiguration.addJobHandler(tweetHandler);
    }

    @AfterEach
    public void tearDown() throws Exception {
        processEngineConfiguration.removeJobHandler(tweetHandler.getType());
    }

    protected JobEntity createTweetMessage(String msg) {
        JobEntity message = new JobEntityImpl();
        message.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        message.setJobHandlerType("tweet");
        message.setJobHandlerConfiguration(msg);
        return message;
    }

    protected TimerJobEntity createTweetTimer(String msg, Date duedate) {
        TimerJobEntity timer = new TimerJobEntityImpl();
        timer.setJobType(JobEntity.JOB_TYPE_TIMER);
        timer.setJobHandlerType("tweet");
        timer.setJobHandlerConfiguration(msg);
        timer.setDuedate(duedate);
        return timer;
    }

}
