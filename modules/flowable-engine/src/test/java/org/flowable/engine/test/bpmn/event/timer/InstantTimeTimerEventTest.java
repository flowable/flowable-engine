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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
public class InstantTimeTimerEventTest extends ResourceFlowableTestCase {

    public InstantTimeTimerEventTest() {
        super("org/flowable/engine/test/bpmn/event/timer/InstantTimeTimerEventTest.cfg.xml");
    }

    @Test
    @Deployment
    public void testExpressionStartTimerEvent() throws Exception {
        TimerJobQuery jobQuery = managementService.createTimerJobQuery();
        assertThat(jobQuery.count()).isEqualTo(1);

        waitForJobExecutorToProcessAllJobs(7000L, 200L);

        jobQuery = managementService.createTimerJobQuery();
        assertThat(jobQuery.count()).isZero();
    }

    @Test
    @Deployment
    public void testVariableExpressionBoundaryTimerEvent() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("duration", Instant.ofEpochSecond(100));

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnBoundaryTimer", variables);

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);

        Calendar nowCal = processEngineConfiguration.getClock().getCurrentCalendar();
        nowCal.add(Calendar.MINUTE, 3);
        processEngineConfiguration.getClock().setCurrentTime(nowCal.getTime());
        
        try {
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000L, 25L);
            assertThat(jobQuery.count()).isZero();
            
            assertProcessEnded(pi.getId());
        } finally {
            processEngineConfiguration.getClock().reset();
        }
    }

    @Test
    @Deployment
    public void testBeanExpressionBoundaryTimerEvent() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnBoundaryTimer");

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);

        Calendar nowCal = processEngineConfiguration.getClock().getCurrentCalendar();
        nowCal.add(Calendar.MINUTE, 3);
        processEngineConfiguration.getClock().setCurrentTime(nowCal.getTime());
        
        try {
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000L, 25L);
            assertThat(jobQuery.count()).isZero();
            
            assertProcessEnded(pi.getId());
            
        } finally {
            processEngineConfiguration.getClock().reset();
        }
    }

}
