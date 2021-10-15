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
package org.flowable.cmmn.engine.test.impl;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;


/**
 * Helper class for writing unit tests with the async executor.
 * Inspired by the JobTestHelper from the bpmn engine, but adapted for cmmn.
 * 
 * @author Joram Barrez
 */
public class CmmnJobTestHelper {
    
    public static void waitForJobExecutorToProcessAllJobs(final CmmnEngine cmmnEngine, final long maxMillisToWait, 
            final long intervalMillis, final boolean shutdownExecutorWhenFinished) {
        
        waitForJobExecutorToProcessAllJobs(cmmnEngine.getCmmnEngineConfiguration(), maxMillisToWait, intervalMillis, shutdownExecutorWhenFinished);
    }
    
    public static void waitForJobExecutorToProcessAllJobs(final CmmnEngineConfiguration cmmnEngineConfiguration, final long maxMillisToWait, 
            final long intervalMillis, final boolean shutdownExecutorWhenFinished) {
        
        waitForExecutorToProcessAllJobs(cmmnEngineConfiguration.getAsyncExecutor(), new Callable<Boolean>() {

            @Override
            public Boolean call() {
                return cmmnEngineConfiguration.getCmmnManagementService().createJobQuery().count() > 0
                || cmmnEngineConfiguration.getCmmnManagementService().createTimerJobQuery().count() > 0;
            }

        }, maxMillisToWait, intervalMillis, shutdownExecutorWhenFinished);
    }

    public static void waitForJobExecutorToProcessAllAsyncJobs(final CmmnEngineConfiguration cmmnEngineConfiguration, final long maxMillisToWait,
            final long intervalMillis, final boolean shutdownExecutorWhenFinished) {

        waitForExecutorToProcessAllJobs(cmmnEngineConfiguration.getAsyncExecutor(), new Callable<Boolean>() {

            @Override
            public Boolean call() {
                return cmmnEngineConfiguration.getCmmnManagementService().createJobQuery().count() > 0;
            }

        }, maxMillisToWait, intervalMillis, shutdownExecutorWhenFinished);
    }

    public static void waitForJobExecutorOnCondition(CmmnEngineConfiguration cmmnEngineConfiguration, long maxMillisToWait, long intervalMillis, Callable<Boolean> predicate) {
        waitForExecutorToProcessAllJobs(cmmnEngineConfiguration.getAsyncExecutor(), () -> !predicate.call(), maxMillisToWait, intervalMillis, true);
    }
    
    public static void waitForAsyncHistoryExecutorToProcessAllJobs(final CmmnEngineConfiguration cmmnEngineConfiguration, final long maxMillisToWait, 
            final long intervalMillis, final boolean shutdownExecutorWhenFinished) {
        
        waitForExecutorToProcessAllJobs(cmmnEngineConfiguration.getAsyncHistoryExecutor(), new Callable<Boolean>() {
            
            @Override
            public Boolean call() {
                return cmmnEngineConfiguration.getCmmnManagementService().createHistoryJobQuery().count() > 0;
            }
            
        }, maxMillisToWait, intervalMillis, shutdownExecutorWhenFinished);
    }

    public static void executeJobExecutorForTime(CmmnEngineConfiguration cmmnEngineConfiguration, long maxMillisToWait, long intervalMillis) {
        AsyncExecutor asyncExecutor = cmmnEngineConfiguration.getAsyncExecutor();
        asyncExecutor.start();

        try {
            Timer timer = new Timer();
            InterruptTask task = new InterruptTask(Thread.currentThread());
            timer.schedule(task, maxMillisToWait);
            try {
                while (!task.isTimeLimitExceeded()) {
                    Thread.sleep(intervalMillis);
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                timer.cancel();
            }

        } finally {
            asyncExecutor.shutdown();
        }
    }
    
    public static void waitForExecutorToProcessAllJobs(final AsyncExecutor asyncExecutor, Callable<Boolean> callable,
            final long maxMillisToWait, final long intervalMillis, final boolean shutdownExecutorWhenFinished) {

        if (asyncExecutor == null) {
            throw new FlowableException("No async executor set. Check the cmmn engine configuration.");
        }
        
        asyncExecutor.start();

        try {
            Timer timer = new Timer();
            InterruptTask interruptTask = new InterruptTask(Thread.currentThread());
            timer.schedule(interruptTask, maxMillisToWait);
            
            boolean areJobsAvailable = true;
            try {
                while (areJobsAvailable && !interruptTask.isTimeLimitExceeded()) {
                    Thread.sleep(intervalMillis);
                    try {
                        areJobsAvailable = callable.call();
                    } catch (Throwable t) { 
                        // ignore
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                timer.cancel();
            }
            if (areJobsAvailable) {
                throw new FlowableException("Time limit of " + maxMillisToWait + " was exceeded");
            }

        } finally {
            if (shutdownExecutorWhenFinished) {
                asyncExecutor.shutdown();
            }
        }
    }

    public static class InterruptTask extends TimerTask {

        protected boolean timeLimitExceeded;
        protected Thread thread;

        public InterruptTask(Thread thread) {
            this.thread = thread;
        }

        public boolean isTimeLimitExceeded() {
            return timeLimitExceeded;
        }

        @Override
        public void run() {
            timeLimitExceeded = true;
            thread.interrupt();
        }
    }

}
