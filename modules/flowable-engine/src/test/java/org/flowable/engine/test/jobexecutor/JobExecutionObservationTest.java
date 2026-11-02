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

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.flowable.common.engine.impl.scripting.FlowableScriptEvaluationException;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.JobInfo;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.job.service.impl.asyncexecutor.AbstractAsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.JobExecutionObservation;
import org.flowable.job.service.impl.asyncexecutor.JobExecutionObservationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class JobExecutionObservationTest extends PluggableFlowableTestCase {

    protected TestJobExecutionObservationProvider testObservationProvider = new TestJobExecutionObservationProvider();

    @BeforeEach
    void setUp() {
        ((AbstractAsyncExecutor) processEngineConfiguration.getAsyncExecutor()).setJobExecutionObservationProvider(testObservationProvider);
    }

    @AfterEach
    void tearDown() {
        ((AbstractAsyncExecutor) processEngineConfiguration.getAsyncExecutor()).setJobExecutionObservationProvider(JobExecutionObservationProvider.NOOP);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/jobexecutor/AsyncExecutorTest.testRegularAsyncExecution.bpmn20.xml" })
    public void regularTimerExecution() {
        runtimeService.startProcessInstanceByKey("asyncExecutor");
        assertThat(managementService.createTimerJobQuery().count()).isOne();

        assertThat(testObservationProvider.createdObservations).isEmpty();

        // Timer is set for 5 minutes, so move clock 10 minutes
        processEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)));

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000L, 500L);

        assertThat(testObservationProvider.createdObservations)
                .singleElement()
                .satisfies(observation -> {
                    assertThat(observation.jobInfo).isNotNull();
                    assertThat(observation.started).isEqualTo(1);
                    assertThat(observation.stopped).isEqualTo(1);
                    assertThat(observation.lockErrors).isEmpty();
                    assertThat(observation.executionErrors).isEmpty();
                    assertThat(observation.scopes)
                            .extracting(scope -> scope.name)
                            .containsExactly("lock", "execution");
                    assertThat(observation.scopes)
                            .allSatisfy(scope -> assertThat(scope.closed).isTrue());
                });
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/jobexecutor/asyncNonExclusiveServiceTask.bpmn20.xml" })
    public void regularNonExclusive() {
        runtimeService.startProcessInstanceByKey("asyncTask");
        assertThat(managementService.createJobQuery().count()).isOne();

        assertThat(testObservationProvider.createdObservations).isEmpty();

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000L, 500L);

        assertThat(testObservationProvider.createdObservations)
                .singleElement()
                .satisfies(observation -> {
                    assertThat(observation.jobInfo).isNotNull();
                    assertThat(observation.started).isEqualTo(1);
                    assertThat(observation.stopped).isEqualTo(1);
                    assertThat(observation.lockErrors).isEmpty();
                    assertThat(observation.executionErrors).isEmpty();
                    assertThat(observation.scopes)
                            .extracting(scope -> scope.name)
                            .containsExactly("execution");
                    assertThat(observation.scopes)
                            .allSatisfy(scope -> assertThat(scope.closed).isTrue());
                });
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
    public void withExecutionException() {
        TimerJobQuery query = managementService.createTimerJobQuery().withException();
        assertThat(query.count()).isZero();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // Timer is set for 4 hours, so move clock 5 hours
        processEngineConfiguration.getClock().setCurrentTime(new Date(new Date().getTime() + 5 * 60 * 60 * 1000));

        // The execution is waiting in the first usertask. This contains a
        // boundary timer event which we will execute manual for testing purposes.
        JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, 7000L, 100L, new Callable<>() {
            @Override
            public Boolean call() throws Exception {
                return managementService.createTimerJobQuery().withException().count() == 1;
            }
        });

        query = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).withException();
        assertThat(query.count()).isEqualTo(1);

        assertThat(testObservationProvider.createdObservations)
                .singleElement()
                .satisfies(observation -> {
                    assertThat(observation.jobInfo).isNotNull();
                    assertThat(observation.started).isEqualTo(1);
                    assertThat(observation.stopped).isEqualTo(1);
                    assertThat(observation.lockErrors).isEmpty();
                    assertThat(observation.executionErrors)
                            .singleElement(as(InstanceOfAssertFactories.throwable(Throwable.class)))
                            .isInstanceOf(FlowableScriptEvaluationException.class);
                    assertThat(observation.scopes)
                            .extracting(scope -> scope.name)
                            .containsExactly("lock", "execution");
                    assertThat(observation.scopes)
                            .allSatisfy(scope -> assertThat(scope.closed).isTrue());
                });

    }

    static class TestJobExecutionObservationProvider implements JobExecutionObservationProvider {

        protected final List<TestJobExecutionObservation> createdObservations = new ArrayList<>();

        @Override
        public JobExecutionObservation create(JobInfo job) {
            TestJobExecutionObservation observation = new TestJobExecutionObservation(job);
            createdObservations.add(observation);
            return observation;
        }
    }

    static class TestJobExecutionObservation implements JobExecutionObservation {

        protected final JobInfo jobInfo;
        protected int started = 0;
        protected int stopped = 0;
        protected final List<JobExecutionObservationScope> scopes = new ArrayList<>();
        protected final List<Throwable> lockErrors = new ArrayList<>();
        protected final List<Throwable> executionErrors = new ArrayList<>();

        TestJobExecutionObservation(JobInfo jobInfo) {
            this.jobInfo = jobInfo;
        }

        @Override
        public void start() {
            started++;
        }

        @Override
        public void stop() {
            stopped++;
        }

        @Override
        public Scope lockScope() {
            JobExecutionObservationScope scope = new JobExecutionObservationScope("lock");
            scopes.add(scope);
            return scope;
        }

        @Override
        public void lockError(Throwable lockException) {
            lockErrors.add(lockException);
        }

        @Override
        public Scope executionScope() {
            JobExecutionObservationScope scope = new JobExecutionObservationScope("execution");
            scopes.add(scope);
            return scope;
        }

        @Override
        public void executionError(Throwable exception) {
            executionErrors.add(exception);
        }
    }

    static class JobExecutionObservationScope implements JobExecutionObservation.Scope {

        protected final String name;
        protected boolean closed;

        JobExecutionObservationScope(String name) {
            this.name = name;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

}
