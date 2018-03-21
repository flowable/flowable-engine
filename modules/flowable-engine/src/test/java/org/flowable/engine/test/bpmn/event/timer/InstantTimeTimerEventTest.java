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
package org.flowable.engine.test.bpmn.event.timer;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author martin.grofcik
 */
public class InstantTimeTimerEventTest extends ResourceFlowableTestCase {

    public InstantTimeTimerEventTest() {
        super("org/flowable/engine/test/bpmn/event/timer/InstantTimeTimerEventTest.cfg.xml");
    }

    @Deployment
    public void testExpressionStartTimerEvent() throws Exception {
        TimerJobQuery jobQuery = managementService.createTimerJobQuery();
        assertEquals(1, jobQuery.count());

        waitForJobExecutorToProcessAllJobs(5000L, 200L);

        jobQuery = managementService.createTimerJobQuery();
        assertEquals(0, jobQuery.count());
    }

    @Deployment
    public void testVariableExpressionBoundaryTimerEvent() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("duration", Instant.ofEpochSecond(100));

        processEngineConfiguration.getClock().setCurrentTime(new Date(0));
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnBoundaryTimer", variables);

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertEquals(1, jobs.size());

        processEngineConfiguration.getClock().setCurrentTime(new Date(200*1000));
        waitForJobExecutorToProcessAllJobs(10000L, 25L);
        assertEquals(0L, jobQuery.count());
        
        assertProcessEnded(pi.getId());
        processEngineConfiguration.getClock().reset();
    }

    @Deployment
    public void testBeanExpressionBoundaryTimerEvent() {
        processEngineConfiguration.getClock().setCurrentTime(new Date(0));
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnBoundaryTimer");

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertEquals(1, jobs.size());

        processEngineConfiguration.getClock().setCurrentTime(new Date(200*1000));
        waitForJobExecutorToProcessAllJobs(10000L, 25L);
        assertEquals(0L, jobQuery.count());
        
        assertProcessEnded(pi.getId());
        processEngineConfiguration.getClock().reset();
    }

}
