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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.asyncexecutor.AcquireAsyncJobsDueLifecycleListener;
import org.flowable.job.service.impl.asyncexecutor.AcquireAsyncJobsDueRunnable;
import org.flowable.job.service.impl.asyncexecutor.AcquireJobsRunnableConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * @author Filip Hrisafov
 */
class AcquireAsyncJobsDueLifecycleListenerTest extends JobExecutorTestCase {

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super.configureConfiguration(processEngineConfiguration);
        processEngineConfiguration.getAsyncExecutorConfiguration().setMaxAsyncJobsDuePerAcquisition(1);
        processEngineConfiguration.getAsyncExecutorConfiguration().setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(500));
        if (processEngineConfiguration.getAsyncExecutor() != null) {
            processEngineConfiguration.getAsyncExecutor().setMaxAsyncJobsDuePerAcquisition(1);
            processEngineConfiguration.getAsyncExecutor().setDefaultAsyncJobAcquireWaitTimeInMillis(500);
        }
    }

    @Test
    void lifecycleMethodsAreInvoked() throws InterruptedException {
        Instant now = Instant.now();
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        CountDownLatch waitingLatch = new CountDownLatch(2);
        TestAcquireAsyncJobsDueLifecycleListener listener = new TestAcquireAsyncJobsDueLifecycleListener(waitingLatch);
        AcquireAsyncJobsDueRunnable runnable = new AcquireAsyncJobsDueRunnable("test-acquire-jobs", asyncExecutor,
                processEngineConfiguration.getJobServiceConfiguration().getJobEntityManager(), listener, AcquireJobsRunnableConfiguration.DEFAULT);

        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

        commandExecutor.execute(commandContext -> {

            JobService jobService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration()
                    .getJobService();

            JobEntity job1 = createTweetMessage("Message 1");
            jobService.scheduleAsyncJob(job1);

            JobEntity job2 = createTweetMessage("Message 2");
            jobService.scheduleAsyncJob(job2);

            return null;
        });

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now.plusSeconds(60)));

        assertThat(managementService.createJobQuery().list())
                .hasSize(2)
                .extracting(job -> ((JobEntity) job).getLockOwner())
                .containsOnlyNulls();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(runnable, executorService);

        if (waitingLatch.await(10, TimeUnit.SECONDS)) {
            runnable.stop();
        } else {
            throw new AssertionFailedError("Acquire timer runnable did not run twice");
        }

        assertThat(managementService.createTimerJobQuery().list()).isEmpty();
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs)
                .hasSize(2)
                .extracting(job -> ((JobEntity) job).getLockOwner())
                .doesNotContainNull();

        commandExecutor.execute(commandContext -> {
            JobService jobService = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                    .getJobServiceConfiguration()
                    .getJobService();
            for (Job job : jobs) {
                JobEntity jobEntity = (JobEntity) job;
                jobEntity.setLockExpirationTime(null);
                jobEntity.setLockOwner(null);
                jobService.updateJob(jobEntity);
                jobService.deleteJob(jobEntity);
            }

            return null;
        });

        assertThat(listener.statesByEngine)
                .containsOnlyKeys(ScopeTypes.BPMN);

        assertThat(listener.statesByEngine.get(ScopeTypes.BPMN))
                .extracting(State::getJobsAcquired, State::getMaxTimerJobsPerAcquisition, State::getMillisToWait, State::getRemainingCapacity, State::isAcquireCycleStopped)
                .containsExactly(
                        // newest entries on top
                        tuple(0, 1, 500L, 2048, true),
                        tuple(0, 1, 500L, 2048, true),
                        tuple(1, 1, 0L, 2048, true), // remaining capacity doesn't go down, cause the async executor isn't running
                        tuple(1, 1, 0L, 2048, true) // 0L for the millisToWait -> after acquiring a job, the acquire should immediately try again
                );
    }

    static class TestAcquireAsyncJobsDueLifecycleListener implements AcquireAsyncJobsDueLifecycleListener {

        protected final CountDownLatch waitingLatch;
        protected final Map<String, Deque<State>> statesByEngine = new LinkedHashMap<>();

        TestAcquireAsyncJobsDueLifecycleListener(CountDownLatch waitingLatch) {
            this.waitingLatch = waitingLatch;
        }

        @Override
        public void startAcquiring(String engineName, int remainingCapacity, int maxAsyncJobsDuePerAcquisition) {
            statesByEngine.computeIfAbsent(engineName, key -> new LinkedList<>()).addFirst(new State(remainingCapacity));
        }

        @Override
        public void stopAcquiring(String engineName) {
            State state = statesByEngine.get(engineName).getFirst();
            state.acquireCycleStopped = true;
        }

        @Override
        public void acquiredJobs(String engineName, int jobsAcquired, int maxAsyncJobsDuePerAcquisition) {
            State state = statesByEngine.get(engineName).getFirst();
            state.jobsAcquired += jobsAcquired;
            state.maxTimerJobsPerAcquisition = maxAsyncJobsDuePerAcquisition;
        }

        @Override
        public void rejectedJobs(String engineName, int jobsRejected, int jobsAcquired, int maxAsyncJobsDuePerAcquisition) {
            State state = statesByEngine.get(engineName).getFirst();
            state.jobsRejected += jobsRejected;
        }

        @Override
        public void optimistLockingException(String engineName, int maxAsyncJobsDuePerAcquisition) {

        }

        @Override
        public void startWaiting(String engineName, long millisToWait) {
            State state = statesByEngine.get(engineName).getFirst();
            state.millisToWait = millisToWait;
            waitingLatch.countDown();
        }
    }

    static class State {

        protected int jobsAcquired = 0;
        protected int jobsRejected = 0;
        protected int maxTimerJobsPerAcquisition = 0;
        protected long millisToWait = 0;
        protected int remainingCapacity;
        protected boolean acquireCycleStopped;

        public State(int remainingCapacity) {
            this.remainingCapacity = remainingCapacity;
        }

        public int getJobsAcquired() {
            return jobsAcquired;
        }

        public int getMaxTimerJobsPerAcquisition() {
            return maxTimerJobsPerAcquisition;
        }

        public long getMillisToWait() {
            return millisToWait;
        }

        public int getRemainingCapacity() {
            return remainingCapacity;
        }

        public boolean isAcquireCycleStopped() {
            return acquireCycleStopped;
        }
    }
}
