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
package org.flowable.engine.test.bpmn.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.interceptor.CommandInvoker;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.ParallelMultiInstanceActivityCompletionJobHandler;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.job.api.FlowableUnrecoverableJobException;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.cmd.LockExclusiveJobCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class ParallelMultiInstanceAsyncNonExclusiveTest extends CustomConfigurationFlowableTestCase {

    protected CustomCommandInvoker customCommandInvoker;
    protected CustomEventListener customEventListener;
    protected CollectingAsyncRunnableExecutionExceptionHandler executionExceptionHandler;

    public ParallelMultiInstanceAsyncNonExclusiveTest() {
        super("parallelMultiInstanceAsyncNonExclusiveTest");
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        customCommandInvoker = new CustomCommandInvoker();
        processEngineConfiguration.setCommandInvoker(customCommandInvoker);
        processEngineConfiguration.getAsyncExecutorConfiguration().setGlobalAcquireLockEnabled(true);
        executionExceptionHandler = new CollectingAsyncRunnableExecutionExceptionHandler();
        processEngineConfiguration.setCustomAsyncRunnableExecutionExceptionHandlers(Collections.singletonList(executionExceptionHandler));
        customEventListener = new CustomEventListener();
        processEngineConfiguration.setEventListeners(Collections.singletonList(customEventListener));

    }

    @Test
    @Deployment
    public void parallelMultiInstanceNonExclusiveJobs() {
        // This test is trying to cause an optimistic locking exception when using non-exclusive parallel multi instance jobs.
        // This is mimicking the following scenario:
        // 4 async jobs complete in the same time, and thus they create 4 parallel-multi-instance-complete exclusive jobs
        // 3 of those jobs will fail to get the exclusive lock and unacquire their jobs and 1 will get the lock
        // the one that will get the lock will continue to the next step of the process and perform the multi instance cleanup
        // the cleanup of the multi instance should not fail.

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("parallelScriptTask")
                .start();

        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(4);
        assertThat(jobs)
                .extracting(Job::getJobHandlerType)
                .containsOnly(AsyncContinuationJobHandler.TYPE);
        customCommandInvoker.lockExclusiveCounter = new AtomicLong(0L);

        customCommandInvoker.executeLockReleaseLatch = new CountDownLatch(1);
        customEventListener.parallelMultiInstanceCompleteLatch = customCommandInvoker.executeLockReleaseLatch;

        customCommandInvoker.executeAsyncRunnableLatch = new CountDownLatch(4);
        customEventListener.asyncContinuationLatch = new CountDownLatch(4);

        customCommandInvoker.executeLockCountLatch = new CountDownLatch(3);
        customEventListener.parallelMultiInstanceWaitCompleteLatch = customCommandInvoker.executeLockCountLatch;

        waitForJobExecutorToProcessAllJobs(15_000, 200);

        assertThat(executionExceptionHandler.getExceptions()).isEmpty();
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
        assertThat(managementService.createDeadLetterJobQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
    }

    protected static class CustomCommandInvoker extends CommandInvoker {

        protected AtomicLong lockExclusiveCounter = new AtomicLong();
        protected CountDownLatch executeLockCountLatch;
        protected CountDownLatch executeLockReleaseLatch;
        protected CountDownLatch executeAsyncRunnableLatch;

        protected CustomCommandInvoker() {
            super(((commandContext, runnable) -> runnable.run()), null);
        }

        @Override
        public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {
            if (command instanceof LockExclusiveJobCmd) {
                if (lockExclusiveCounter.incrementAndGet() > 1) {
                    // We let the first exclusive to run without waiting
                    // we then wait to complete this transaction until the execute lock exclusive is released
                    try {
                        executeLockCountLatch.countDown();
                        executeLockReleaseLatch.await(4, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
            return super.execute(config, command, commandExecutor);
        }
    }

    protected static class CustomEventListener implements FlowableEventListener {

        protected CountDownLatch asyncContinuationLatch;
        protected CountDownLatch parallelMultiInstanceCompleteLatch;
        protected CountDownLatch parallelMultiInstanceWaitCompleteLatch;

        @Override
        public void onEvent(FlowableEvent event) {
            if (FlowableEngineEventType.JOB_EXECUTION_SUCCESS.equals(event.getType()) && event instanceof FlowableEntityEvent) {
                JobEntity entity = (JobEntity) ((FlowableEntityEvent) event).getEntity();
                String jobHandlerType = entity.getJobHandlerType();
                if (AsyncContinuationJobHandler.TYPE.equals(jobHandlerType)) {
                    // We are going to wait for all the async jobs to complete in the same time
                    asyncContinuationLatch.countDown();
                    try {
                        if (!asyncContinuationLatch.await(4, TimeUnit.SECONDS)) {
                            throw new FlowableUnrecoverableJobException("asyncContinuationLatch did not reach 0");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                } else if (ParallelMultiInstanceActivityCompletionJobHandler.TYPE.equals(jobHandlerType)) {
                    // There will be one multi instance complete job, so we count it down to release the rest of the lock exclusive commands
                    parallelMultiInstanceCompleteLatch.countDown();

                    try {
                        // Wait for the rest of the lock exclusive commands to complete before resuming this transaction
                        if (!parallelMultiInstanceWaitCompleteLatch.await(4, TimeUnit.SECONDS)) {
                            throw new FlowableUnrecoverableJobException("parallelMultiInstanceWaitLatch did not reach 0");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }

            }

        }

        @Override
        public boolean isFailOnException() {
            return true;
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
