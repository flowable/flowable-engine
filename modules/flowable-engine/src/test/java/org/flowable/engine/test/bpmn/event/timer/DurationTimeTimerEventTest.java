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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class DurationTimeTimerEventTest extends PluggableFlowableTestCase {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private Map<Object, Object> initialBeans;

    @BeforeEach
    public void setUp() throws Exception {
        initialBeans = processEngineConfiguration.getExpressionManager().getBeans();

        Map<Object, Object> newBeans = new HashMap<>();

        newBeans.put("durationTimeProvider", new DurationTimeProvider());
        processEngineConfiguration.getExpressionManager().setBeans(newBeans);
    }

    @AfterEach
    public void tearDown() throws Exception {
        processEngineConfiguration.getExpressionManager().setBeans(initialBeans);
    }

    @Test
    public void testExpressionStartTimerEvent() {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        processEngineConfiguration.getClock().setCurrentTime(Date.from(yesterday));
        deploymentIdsForAutoCleanup.add(
            repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/DurationTimeTimerEventTest.testExpressionStartTimerEvent.bpmn20.xml")
                .deploy()
                .getId()
        );
        TimerJobQuery jobQuery = managementService.createTimerJobQuery();
        assertThat(jobQuery.count()).isEqualTo(1);
        assertThat(jobQuery.singleResult().getDuedate()).isNotNull();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(yesterday.plus(200, ChronoUnit.SECONDS)));

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 200L);

        jobQuery = managementService.createTimerJobQuery();
        assertThat(jobQuery.count()).isZero();
    }

    @Test
    @Deployment
    public void testVariableExpressionBoundaryTimerEvent() {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("duration", Duration.ofSeconds(100));

        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        processEngineConfiguration.getClock().setCurrentTime(Date.from(yesterday));
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testDurationExpressionOnBoundaryTimer", variables);

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);
        assertThat(simpleDateFormat.format(jobs.get(0).getDuedate())).isEqualTo(simpleDateFormat.format(Date.from(yesterday.plus(100, ChronoUnit.SECONDS))));
        processEngineConfiguration.getClock().setCurrentTime(Date.from(yesterday.plus(200, ChronoUnit.SECONDS)));

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000L, 25L);
        assertThat(jobQuery.count()).isZero();

        assertProcessEnded(pi.getId());
        processEngineConfiguration.getClock().reset();
    }

    @Test
    @Deployment
    public void testBeanExpressionBoundaryTimerEvent() {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        processEngineConfiguration.getClock().setCurrentTime(Date.from(yesterday));
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testDurationExpressionOnBoundaryTimer");

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getDuedate()).isNotNull();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(yesterday.plus(200, ChronoUnit.SECONDS)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000L, 100L);
        assertThat(jobQuery.count()).isZero();

        assertProcessEnded(pi.getId());
        processEngineConfiguration.getClock().reset();
    }

}
