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

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests specifically for the {@link AsyncExecutor}.
 * 
 * @author Joram Barrez
 */
public class AsyncExecutorTest {

    @Test
    public void testRegularAsyncExecution() {

        ProcessEngine processEngine = null;

        try {
            // Deploy
            processEngine = createProcessEngine(true);
            setClockToCurrentTime(processEngine);
            deploy(processEngine, "AsyncExecutorTest.testRegularAsyncExecution.bpmn20.xml");

            // Start process instance. Wait for all jobs to be done
            processEngine.getRuntimeService().startProcessInstanceByKey("asyncExecutor");

            // Move clock 3 minutes. Nothing should happen
            addSecondsToCurrentTime(processEngine, 180L);
            ProcessEngine processEngine1 = processEngine;
            assertThatThrownBy(() -> waitForAllJobsBeingExecuted(processEngine1, 500L))
                    .isExactlyInstanceOf(FlowableException.class);
            assertThat(processEngine.getTaskService().createTaskQuery().taskName("The Task").count()).isEqualTo(1);
            assertThat(processEngine.getTaskService().createTaskQuery().taskName("Task after timer").count()).isZero();
            assertThat(processEngine.getManagementService().createTimerJobQuery().count()).isEqualTo(1);
            assertThat(getAsyncExecutorJobCount(processEngine)).isZero();

            // Move clock 3 minutes and 1 second. Triggers the timer
            addSecondsToCurrentTime(processEngine, 181);
            waitForAllJobsBeingExecuted(processEngine);

            // Verify if all is as expected
            assertThat(processEngine.getTaskService().createTaskQuery().taskName("The Task").count()).isZero();
            assertThat(processEngine.getTaskService().createTaskQuery().taskName("Task after timer").count()).isEqualTo(1);
            assertThat(processEngine.getManagementService().createTimerJobQuery().count()).isZero();
            assertThat(processEngine.getManagementService().createJobQuery().count()).isZero();

            assertThat(getAsyncExecutorJobCount(processEngine)).isEqualTo(1);
        } finally {

            // Clean up
            if (processEngine != null) {
                cleanup(processEngine);
            }
        }
    }

    @Test
    public void testAsyncExecutorDisabledOnOneEngine() {

        ProcessEngine firstProcessEngine = null;
        ProcessEngine secondProcessEngine = null;

        try {

            // Deploy on one engine, where the async executor is disabled
            firstProcessEngine = createProcessEngine(false);
            Date now = setClockToCurrentTime(firstProcessEngine);
            deploy(firstProcessEngine, "AsyncExecutorTest.testRegularAsyncExecution.bpmn20.xml");

            // Start process instance on first engine
            firstProcessEngine.getRuntimeService().startProcessInstanceByKey("asyncExecutor");

            // Move clock 5 minutes and 1 second. Triggers the timer normally,
            // but not now since async execution is disabled
            addSecondsToCurrentTime(firstProcessEngine, 301); // 301 = 5m01s
            assertThat(firstProcessEngine.getTaskService().createTaskQuery().taskName("The Task").count()).isEqualTo(1);
            assertThat(firstProcessEngine.getTaskService().createTaskQuery().taskName("Task after timer").count()).isZero();
            assertThat(firstProcessEngine.getManagementService().createTimerJobQuery().count()).isEqualTo(1);

            // Create second engine, with async executor enabled. Same time as
            // the first engine to start, then add 301 seconds
            secondProcessEngine = createProcessEngine(true, null, now);
            addSecondsToCurrentTime(secondProcessEngine, 361);
            waitForAllJobsBeingExecuted(secondProcessEngine);

            // Verify if all is as expected
            assertThat(firstProcessEngine.getTaskService().createTaskQuery().taskName("The Task").count()).isZero();
            assertThat(firstProcessEngine.getTaskService().createTaskQuery().taskName("Task after timer").count()).isEqualTo(1);
            assertThat(firstProcessEngine.getManagementService().createTimerJobQuery().count()).isZero();
            assertThat(firstProcessEngine.getManagementService().createJobQuery().count()).isZero();

            assertThat(getAsyncExecutorJobCount(firstProcessEngine)).isZero();
            assertThat(getAsyncExecutorJobCount(secondProcessEngine)).isEqualTo(1);

        } finally {

            // Clean up
            cleanup(firstProcessEngine);
            cleanup(secondProcessEngine);

        }

    }

    @Test
    public void testAsyncScriptExecution() {

        ProcessEngine processEngine = null;

        try {

            // Deploy
            processEngine = createProcessEngine(true);
            setClockToCurrentTime(processEngine);
            deploy(processEngine, "AsyncExecutorTest.testAsyncScriptExecution.bpmn20.xml");

            // Start process instance. Wait for all jobs to be done
            ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("asyncScript");
            waitForAllJobsBeingExecuted(processEngine);

            // Verify if all is as expected
            assertThat(processEngine.getManagementService().createJobQuery().count()).isZero();
            assertThat(processEngine.getManagementService().createTimerJobQuery().count()).isZero();
            assertThat(processEngine.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
            assertThat(processEngine.getTaskService().createTaskQuery().taskName("Task after script").count()).isEqualTo(1);

            assertThat(getAsyncExecutorJobCount(processEngine)).isEqualTo(1);

        } finally {

            // Clean up
            cleanup(processEngine);
        }
    }

    @Test
    public void testAsyncFailingScript() {

        ProcessEngine processEngine = null;

        try {

            // Deploy
            processEngine = createProcessEngine(true);
            processEngine.getProcessEngineConfiguration().getClock().reset();
            deploy(processEngine, "AsyncExecutorTest.testAsyncFailingScript.bpmn20.xml");

            // There is a back off mechanism for the retry, so need a bit of
            // time. But to be sure, we make the wait time small
            processEngine.getProcessEngineConfiguration().setAsyncFailedJobWaitTime(1);
            processEngine.getProcessEngineConfiguration().setDefaultFailedJobWaitTime(1);

            // Start process instance. Wait for all jobs to be done.
            processEngine.getRuntimeService().startProcessInstanceByKey("asyncScript");

            final ProcessEngine processEngineCopy = processEngine;
            JobTestHelper.waitForJobExecutorOnCondition(processEngine.getProcessEngineConfiguration(), 10000L, 1000L, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    long timerJobCount = processEngineCopy.getManagementService().createTimerJobQuery().count();
                    if (timerJobCount == 0) {
                        return processEngineCopy.getManagementService().createJobQuery().count() == 0;
                    } else {
                        return false;
                    }
                }
            });

            // Verify if all is as expected
            assertThat(processEngine.getTaskService().createTaskQuery().taskName("Task after script").count()).isZero();
            assertThat(processEngine.getManagementService().createJobQuery().count()).isZero();
            assertThat(processEngine.getManagementService().createDeadLetterJobQuery().count()).isEqualTo(1);

            assertThat(getAsyncExecutorJobCount(processEngine)).isEqualTo(3);

        } finally {

            // Clean up
            cleanup(processEngine);
        }
    }
    
    @Test
    public void testAsyncFailingScriptWithCategory() {
        ProcessEngine processEngine = null;

        try {

            // Deploy
            processEngine = createProcessEngine(true, "myCategory", null);
            processEngine.getProcessEngineConfiguration().getClock().reset();
            deploy(processEngine, "AsyncExecutorTest.testAsyncFailingScriptWithCategory.bpmn20.xml");

            // There is a back off mechanism for the retry, so need a bit of time. But to be sure, we make the wait time small
            processEngine.getProcessEngineConfiguration().setAsyncFailedJobWaitTime(1);
            processEngine.getProcessEngineConfiguration().setDefaultFailedJobWaitTime(1);

            // Start process instance. Wait for all jobs to be done.
            processEngine.getRuntimeService().startProcessInstanceByKey("asyncScript");

            final ProcessEngine processEngineCopy = processEngine;
            JobTestHelper.waitForJobExecutorOnCondition(processEngine.getProcessEngineConfiguration(), 10000L, 1000L, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    long timerJobCount = processEngineCopy.getManagementService().createTimerJobQuery().count();
                    if (timerJobCount == 0) {
                        return processEngineCopy.getManagementService().createJobQuery().count() == 0;
                    } else {
                        return false;
                    }
                }
            });

            // Verify if all is as expected
            assertThat(processEngine.getTaskService().createTaskQuery().taskName("Task after script").count()).isZero();
            assertThat(processEngine.getManagementService().createJobQuery().count()).isZero();
            assertThat(processEngine.getManagementService().createDeadLetterJobQuery().count()).isEqualTo(1);
            Job job = processEngine.getManagementService().createDeadLetterJobQuery().singleResult();
            assertThat(job.getCategory()).isEqualTo("myCategory");

        } finally {
            // Clean up
            cleanup(processEngine);
        }
    }

    @Test
    public void testJobRejectionOnQueueFull() {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
        processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-AsyncExecutorTest;DB_CLOSE_DELAY=1000");
        processEngineConfiguration.setDatabaseSchemaUpdate("true");
        processEngineConfiguration.setAsyncExecutorActivate(true);

        // Important for this test
        processEngineConfiguration.setAsyncExecutorCorePoolSize(1);
        processEngineConfiguration.setAsyncExecutorMaxPoolSize(1);
        processEngineConfiguration.setAsyncExecutorThreadPoolQueueSize(1);

        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

        processEngine.getProcessEngineConfiguration().getEventDispatcher().addEventListener(new TestRejectionEventListener(), FlowableEngineEventType.JOB_REJECTED);

        // 3 starts:
        // 1) 1 thread available -> 1 thread blocked
        // 2) no thread available -> take 1 spot in queue
        // 3) no thread + queue spot available -> rejected
        int nrOfProcesses = 3;

        try {
            deploy(processEngine, "AsyncExecutorTest.testAsyncJobRejection.bpmn20.xml");

            assertThat(TestRejectionEventListener.COUNTER.get()).isEqualTo(0);
            for (int i = 0; i < nrOfProcesses; i++) {
                processEngine.getRuntimeService().startProcessInstanceByKey("testRejection");
            }
            assertThat(TestRejectionEventListener.COUNTER.get()).isEqualTo(1);

        } catch(Exception e)  {
            Assert.fail("Unexpected exception: " + e.getMessage());
            throw e;
        } finally {

            TestBlockingJavaDelegate.SEMAPHORE.release(nrOfProcesses);

            // 2 blocked jobs should be processed and end the process instance
            // other job should have been changed to a timer job
            await().atMost(Duration.of(10, SECONDS)).until(() -> processEngine.getRuntimeService().createProcessInstanceQuery().count() == 1);
            assertThat(TestRejectionEventListener.COUNTER.get()).isEqualTo(1);

            if (processEngine != null) {
                cleanup(processEngine);
            }
        }
    }

    // Helpers ////////////////////////////////////////////////////////

    private ProcessEngine createProcessEngine(boolean enableAsyncExecutor) {
        return createProcessEngine(enableAsyncExecutor, null, null);
    }

    private ProcessEngine createProcessEngine(boolean enableAsyncExecutor, String enabledJobCategory, Date time) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
        processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-AsyncExecutorTest;DB_CLOSE_DELAY=1000");
        processEngineConfiguration.setDatabaseSchemaUpdate("true");

        if (enableAsyncExecutor) {
            processEngineConfiguration.setAsyncExecutorActivate(true);

            CountingAsyncExecutor countingAsyncExecutor = new CountingAsyncExecutor();
            countingAsyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(50); // To avoid waiting too long when a retry happens
            countingAsyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(50);
            processEngineConfiguration.setAsyncExecutor(countingAsyncExecutor);
        }
        
        if (enabledJobCategory != null) {
            processEngineConfiguration.addEnabledJobCategory(enabledJobCategory);
        }

        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

        if (time != null) {
            processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(time);
        }

        return processEngine;
    }

    private Date setClockToCurrentTime(ProcessEngine processEngine) {
        Date date = new Date();
        processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(date);
        return date;
    }

    private void addSecondsToCurrentTime(ProcessEngine processEngine, long nrOfSeconds) {
        Date currentTime = processEngine.getProcessEngineConfiguration().getClock().getCurrentTime();
        processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(new Date(currentTime.getTime() + (nrOfSeconds * 1000L)));
    }

    private void cleanup(ProcessEngine processEngine) {
        for (org.flowable.engine.repository.Deployment deployment : processEngine.getRepositoryService().createDeploymentQuery().list()) {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
        processEngine.close();
    }

    private String deploy(ProcessEngine processEngine, String resource) {
        return processEngine.getRepositoryService().createDeployment().addClasspathResource("org/flowable/engine/test/jobexecutor/" + resource).deploy().getId();
    }

    private void waitForAllJobsBeingExecuted(ProcessEngine processEngine) {
        waitForAllJobsBeingExecuted(processEngine, 10000L);
    }

    private void waitForAllJobsBeingExecuted(ProcessEngine processEngine, long maxWaitTime) {
        JobTestHelper.waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(processEngine.getProcessEngineConfiguration(), processEngine.getManagementService(), maxWaitTime, 1000L, false);
    }

    private int getAsyncExecutorJobCount(ProcessEngine processEngine) {
        AsyncExecutor asyncExecutor = processEngine.getProcessEngineConfiguration().getAsyncExecutor();
        if (asyncExecutor instanceof CountingAsyncExecutor) {
            return ((CountingAsyncExecutor) asyncExecutor).getCounter().get();
        }
        return 0;
    }

    static class CountingAsyncExecutor extends DefaultAsyncJobExecutor {

        private static final Logger LOGGER = LoggerFactory.getLogger(CountingAsyncExecutor.class);

        private AtomicInteger counter = new AtomicInteger(0);

        @Override
        public boolean executeAsyncJob(JobInfo job) {
            LOGGER.info("About to execute job {}", job.getId());
            counter.incrementAndGet();
            boolean success = super.executeAsyncJob(job);
            LOGGER.info("Handed off job {} to async executor (retries={})", job.getId(), job.getRetries());
            return success;
        }

        public AtomicInteger getCounter() {
            return counter;
        }

        public void setCounter(AtomicInteger counter) {
            this.counter = counter;
        }

    }

    public static final class TestBlockingJavaDelegate implements JavaDelegate {

        public static Semaphore SEMAPHORE = new Semaphore(0);

        @Override
        public void execute(DelegateExecution execution) {
            try {
                SEMAPHORE.acquire();
            } catch (InterruptedException e) {
                throw new FlowableException("Couldn't acquire semaphore", e);
            }
        }
    }

    public static final class TestRejectionEventListener implements FlowableEventListener {

        public static AtomicInteger COUNTER = new AtomicInteger(0);

        @Override
        public void onEvent(FlowableEvent event) {
            COUNTER.incrementAndGet();
        }
        @Override
        public boolean isFailOnException() {
            return false;
        }
        @Override
        public boolean isFireOnTransactionLifecycleEvent() {
            return false;
        }
        @Override
        public String getOnTransaction() {
            return null;
        }
    }

}
