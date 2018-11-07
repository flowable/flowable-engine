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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Saeid Mirzaei Test case for ACT-4066
 */

public class StartTimerEventRepeatWithoutEndTest extends PluggableFlowableTestCase {

    protected long counter;
    protected StartEventListener startEventListener;

    class StartEventListener extends AbstractFlowableEngineEventListener {

        public StartEventListener() {
            super(new HashSet<>(Arrays.asList(FlowableEngineEventType.TIMER_FIRED)));
        }

        @Override
        protected void timerFired(FlowableEngineEntityEvent event) {
            counter++;
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }

    }

    @BeforeEach
    protected void setUp() throws Exception {

        startEventListener = new StartEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(startEventListener);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        processEngineConfiguration.getEventDispatcher().removeEventListener(startEventListener);
    }

    @Test
    @Deployment
    public void testStartTimerEventRepeatWithoutN() {
        counter = 0;

        List<Job> initialTimers = managementService.createTimerJobQuery().list();
        assertThat(initialTimers)
            .as("Timers before execution")
            .hasSize(1);
        processEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().plus(35, ChronoUnit.SECONDS)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5500, 500);
        List<Job> newTimers = managementService.createTimerJobQuery().list();
        List<Job> jobs = managementService.createJobQuery().list();

        assertThat(newTimers)
            .as("Timers after execution")
            .hasSize(1);

        assertThat(newTimers.get(0).getId())
            .as("Timer after execution vs before")
            .isNotEqualTo(initialTimers.get(0).getId());
        assertThat(jobs)
            .as("jobs")
            .isEmpty();
    }

}
