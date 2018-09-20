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
package org.flowable.engine.test.bpmn.exclusive;

import java.util.Date;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.TimerJobQuery;
import org.junit.jupiter.api.Test;

public class ExclusiveTimerEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testCatchingTimerEvent() throws Exception {

        // Set the clock fixed
        Date startTime = new Date();

        // After process start, there should be 3 timers created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("exclusiveTimers");
        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertEquals(3, jobQuery.count());

        // After setting the clock to time '50minutes and 5 seconds', the timers should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((50 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 500L);

        assertEquals(0, jobQuery.count());
        assertProcessEnded(pi.getProcessInstanceId());
    }
}
