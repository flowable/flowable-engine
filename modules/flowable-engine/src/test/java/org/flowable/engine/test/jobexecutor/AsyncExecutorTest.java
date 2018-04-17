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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.junit.Assert;
import org.junit.Test;
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
            try {
                waitForAllJobsBeingExecuted(processEngine, 500L);
                Assert.fail();
            } catch (FlowableException e) {
                // Expected
            }
            Assert.assertEquals(1, processEngine.getTaskService().createTaskQuery().taskName("The Task").count());
            Assert.assertEquals(0, processEngine.getTaskService().createTaskQuery().taskName("Task after timer").count());
            Assert.assertEquals(1, processEngine.getManagementService().createTimerJobQuery().count());
            Assert.assertEquals(0, getAsyncExecutorJobCount(processEngine));

            // Move clock 3 minutes and 1 second. Triggers the timer
            addSecondsToCurrentTime(processEngine, 181);
            waitForAllJobsBeingExecuted(processEngine);

            // Verify if all is as expected
            Assert.assertEquals(0, processEngine.getTaskService().createTaskQuery().taskName("The Task").count());
            Assert.assertEquals(1, processEngine.getTaskService().createTaskQuery().taskName("Task after timer").count());
            Assert.assertEquals(0, processEngine.getManagementService().createTimerJobQuery().count());
            Assert.assertEquals(0, processEngine.getManagementService().createJobQuery().count());

            Assert.assertEquals(1, getAsyncExecutorJobCount(processEngine));
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
            Assert.assertEquals(1, firstProcessEngine.getTaskService().createTaskQuery().taskName("The Task").count());
            Assert.assertEquals(0, firstProcessEngine.getTaskService().createTaskQuery().taskName("Task after timer").count());
            Assert.assertEquals(1, firstProcessEngine.getManagementService().createTimerJobQuery().count());

            // Create second engine, with async executor enabled. Same time as
            // the first engine to start, then add 301 seconds
            secondProcessEngine = createProcessEngine(true, now);
            addSecondsToCurrentTime(secondProcessEngine, 361);
            waitForAllJobsBeingExecuted(secondProcessEngine);

            // Verify if all is as expected
            Assert.assertEquals(0, firstProcessEngine.getTaskService().createTaskQuery().taskName("The Task").count());
            Assert.assertEquals(1, firstProcessEngine.getTaskService().createTaskQuery().taskName("Task after timer").count());
            Assert.assertEquals(0, firstProcessEngine.getManagementService().createTimerJobQuery().count());
            Assert.assertEquals(0, firstProcessEngine.getManagementService().createJobQuery().count());

            Assert.assertEquals(0, getAsyncExecutorJobCount(firstProcessEngine));
            Assert.assertEquals(1, getAsyncExecutorJobCount(secondProcessEngine));

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
            Assert.assertEquals(0, processEngine.getManagementService().createJobQuery().count());
            Assert.assertEquals(0, processEngine.getManagementService().createTimerJobQuery().count());
            Assert.assertEquals(1, processEngine.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).count());
            Assert.assertEquals(1, processEngine.getTaskService().createTaskQuery().taskName("Task after script").count());

            Assert.assertEquals(1, getAsyncExecutorJobCount(processEngine));

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
            Assert.assertEquals(0, processEngine.getTaskService().createTaskQuery().taskName("Task after script").count());
            Assert.assertEquals(0, processEngine.getManagementService().createJobQuery().count());
            Assert.assertEquals(1, processEngine.getManagementService().createDeadLetterJobQuery().count());

            Assert.assertEquals(3, getAsyncExecutorJobCount(processEngine));

        } finally {

            // Clean up
            cleanup(processEngine);
        }
    }

    // Helpers ////////////////////////////////////////////////////////

    private ProcessEngine createProcessEngine(boolean enableAsyncExecutor) {
        return createProcessEngine(enableAsyncExecutor, null);
    }

    private ProcessEngine createProcessEngine(boolean enableAsyncExecutor, Date time) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
        processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:activiti-AsyncExecutorTest;DB_CLOSE_DELAY=1000");
        processEngineConfiguration.setDatabaseSchemaUpdate("true");

        if (enableAsyncExecutor) {
            processEngineConfiguration.setAsyncExecutorActivate(true);

            CountingAsyncExecutor countingAsyncExecutor = new CountingAsyncExecutor();
            countingAsyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(50); // To avoid waiting too long when a retry happens
            countingAsyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(50);
            processEngineConfiguration.setAsyncExecutor(countingAsyncExecutor);
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

}
