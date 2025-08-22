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

package org.flowable.engine.impl.test;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Saeid Mirzaei
 */

// This helper class helps sharing the same code for jobExecutor test helpers,
// between Junit3 and junit 4 test support classes
public class JobTestHelper {

    public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfiguration processEngineConfiguration,
            ManagementService managementService, long maxMillisToWait, long intervalMillis) {
        waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, maxMillisToWait, intervalMillis, true);
    }

    public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService,
            long maxMillisToWait, long intervalMillis, boolean shutdownExecutorWhenFinished) {
        internalWaitForJobs(processEngineConfiguration, managementService, JobTestHelper::areJobsAvailable,
            maxMillisToWait, intervalMillis, shutdownExecutorWhenFinished);
    }

    public static void waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(ProcessEngineConfiguration processEngineConfiguration,
            ManagementService managementService, long maxMillisToWait, long intervalMillis) {
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(processEngineConfiguration, managementService,
            maxMillisToWait, intervalMillis, true);
    }

    public static void waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(ProcessEngineConfiguration processEngineConfiguration,
            ManagementService managementService, long maxMillisToWait, long intervalMillis, boolean shutdownExecutorWhenFinished) {
        internalWaitForJobs(processEngineConfiguration, managementService, JobTestHelper::areJobsOrExecutableTimersAvailable,
            maxMillisToWait, intervalMillis, shutdownExecutorWhenFinished);
    }

    public static void waitForJobExecutorToProcessAllJobsAndTimerJobs(ProcessEngineConfiguration processEngineConfiguration,
            ManagementService managementService, long maxMillisToWait, long intervalMillis) {
        waitForJobExecutorToProcessAllJobsAndTimerJobs(processEngineConfiguration, managementService, maxMillisToWait, intervalMillis, true);
    }

    public static void waitForJobExecutorToProcessAllJobsAndTimerJobs(ProcessEngineConfiguration processEngineConfiguration,
            ManagementService managementService, long maxMillisToWait, long intervalMillis, boolean shutdownExecutorWhenFinished) {
        internalWaitForJobs(processEngineConfiguration, managementService, JobTestHelper::areJobsOrTimersAvailable,
            maxMillisToWait, intervalMillis, shutdownExecutorWhenFinished);
    }

    public static void waitForJobExecutorOnCondition(ProcessEngineConfiguration processEngineConfiguration,
            long maxMillisToWait, long intervalMillis, Callable<Boolean> condition) {
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        asyncExecutor.start();

        try {
            Timer timer = new Timer();
            InterruptTask task = new InterruptTask(Thread.currentThread());
            timer.schedule(task, maxMillisToWait);
            boolean conditionIsViolated = true;
            try {
                while (conditionIsViolated) {
                    Thread.sleep(intervalMillis);
                    conditionIsViolated = !condition.call();
                }
            } catch (InterruptedException e) {
                // ignore
            } catch (Exception e) {
                throw new FlowableException("Exception while waiting on condition: " + e.getMessage(), e);
            } finally {
                timer.cancel();
            }

            if (conditionIsViolated) {
                throw new FlowableException("time limit of " + maxMillisToWait + " was exceeded");
            }

        } finally {
            asyncExecutor.shutdown();
        }
    }

    public static void executeJobExecutorForTime(ProcessEngineConfiguration processEngineConfiguration, long maxMillisToWait, long intervalMillis) {
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
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

    public static boolean areJobsAvailable(ManagementService managementService) {
        return !managementService.createJobQuery().list().isEmpty();
    }

    public static boolean areJobsOrExecutableTimersAvailable(ManagementService managementService) {
        // We have to check in one transaction because it can happen that a timer moves to an async job after the async job is checked
        return managementService.executeCommand(commandContext -> {
            boolean emptyJobs = managementService.createJobQuery().list().isEmpty();
            if (emptyJobs) {
                return !managementService.createTimerJobQuery().executable().list().isEmpty();
            } else {
                return true;
            }
        });
    }

    /**
     * Returns true when there are any entries for the jobs or timers (unlike {@link #areJobsOrExecutableTimersAvailable(ManagementService)},
     * which only take in account executable timers).
     */
    public static boolean areJobsOrTimersAvailable(ManagementService managementService) {
        // We have to check in one transaction because it can happen that a timer moves to an async job after the async job is checked
        return managementService.executeCommand(commandContext -> {
            boolean emptyJobs = managementService.createJobQuery().count() == 0L;
            if (emptyJobs) {
                return !(managementService.createTimerJobQuery().count() == 0L);
            } else {
                return true;
            }
        });
    }

    protected static void internalWaitForJobs(ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService,
        Predicate<ManagementService> jobsAvailablePredicate, long maxMillisToWait, long intervalMillis, boolean shutdownExecutorWhenFinished) {
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        asyncExecutor.start();
        processEngineConfiguration.setAsyncExecutorActivate(true);

        try {
            Timer timer = new Timer();
            InterruptTask task = new InterruptTask(Thread.currentThread());
            timer.schedule(task, maxMillisToWait);
            boolean areJobsAvailable = true;
            try {
                while (areJobsAvailable && !task.isTimeLimitExceeded()) {
                    Thread.sleep(intervalMillis);
                    try {
                        areJobsAvailable = jobsAvailablePredicate.test(managementService);
                    } catch (Throwable t) {
                        // Ignore, possible that exception occurs due to locking/updating of table on MSSQL when
                        // isolation level doesn't allow READ of the table
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                timer.cancel();
            }
            if (areJobsAvailable) {
                throw new FlowableException("time limit of " + maxMillisToWait + " was exceeded");
            }

        } finally {
            if (shutdownExecutorWhenFinished) {
                processEngineConfiguration.setAsyncExecutorActivate(false);
                asyncExecutor.shutdown();
            }
        }
    }

    private static class InterruptTask extends TimerTask {

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
