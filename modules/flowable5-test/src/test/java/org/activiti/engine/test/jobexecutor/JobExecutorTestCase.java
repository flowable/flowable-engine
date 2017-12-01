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

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.TimerJobEntity;
import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.job.api.Job;

/**
 * @author Tom Baeyens
 */
public abstract class JobExecutorTestCase extends PluggableFlowableTestCase {

    protected TweetHandler tweetHandler = new TweetHandler();

    public void setUp() throws Exception {
        ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler().getRawProcessConfiguration();
        activiti5ProcessEngineConfig.getJobHandlers().put(tweetHandler.getType(), tweetHandler);
    }

    public void tearDown() throws Exception {
        ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler().getRawProcessConfiguration();
        activiti5ProcessEngineConfig.getJobHandlers().remove(tweetHandler.getType());
    }

    protected JobEntity createTweetMessage(String msg) {
        JobEntity message = new JobEntity();
        message.setJobType(Job.JOB_TYPE_MESSAGE);
        message.setRevision(1);
        message.setJobHandlerType("tweet");
        message.setJobHandlerConfiguration(msg);
        return message;
    }

    protected TimerJobEntity createTweetTimer(String msg, Date duedate) {
        TimerJobEntity timer = new TimerJobEntity();
        timer.setJobType(Job.JOB_TYPE_TIMER);
        timer.setRevision(1);
        timer.setJobHandlerType("tweet");
        timer.setJobHandlerConfiguration(msg);
        timer.setDuedate(duedate);
        return timer;
    }

}
