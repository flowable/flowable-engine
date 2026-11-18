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

package org.activiti.engine.impl.test;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import org.activiti.engine.test.ActivitiRule;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Saeid Mirzaei
 */

// This helper class helps sharing the same code for jobExecutor test helpers, between Junit3 and junit 4 test support classes
public class JobTestHelper {

    public static void waitForJobExecutorToProcessAllJobs(ActivitiRule activitiRule, long maxMillisToWait, long intervalMillis) {
        waitForJobExecutorToProcessAllJobs(activitiRule.getProcessEngine().getProcessEngineConfiguration(),
                activitiRule.getManagementService(), maxMillisToWait, intervalMillis);
    }

    public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfiguration processEngineConfiguration,
            ManagementService managementService, long maxMillisToWait, long intervalMillis) {

        waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, maxMillisToWait, intervalMillis, true);
    }

    public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfiguration processEngineConfiguration,
            ManagementService managementService, long maxMillisToWait, long intervalMillis, boolean shutdownExecutorWhenFinished) {

        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        asyncExecutor.start();

        try {
            Timer timer = new Timer();
            InterruptTask task = new InterruptTask(Thread.currentThread());
            timer.schedule(task, maxMillisToWait);
            boolean areJobsAvailable = true;
            try {
                while (areJobsAvailable && !task.isTimeLimitExceeded()) {
                    Thread.sleep(intervalMillis);
                    try {
                        // A single check can miss a job when it is moved between the timer and async job tables while the check runs.
                        // Any job that is still in progress after such a miss is seen by a second check, so only stop when both are empty.
                        areJobsAvailable = areJobsAvailable(managementService) || areJobsAvailable(managementService);
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
                asyncExecutor.shutdown();
            }
        }
    }

    public static void waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService, long maxMillisToWait, long intervalMillis) {
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(processEngineConfiguration, managementService, maxMillisToWait, intervalMillis, true);
    }

    public static void waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(ProcessEngineConfiguration processEngineConfiguration, ManagementService managementService, long maxMillisToWait, long intervalMillis,
            boolean shutdownExecutorWhenFinished) {

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
                        // A single check can miss a job when it is moved between the timer and async job tables while the check runs.
                        // Any job that is still in progress after such a miss is seen by a second check, so only stop when both are empty.
                        areJobsAvailable = areJobsOrExecutableTimersAvailable(managementService) || areJobsOrExecutableTimersAvailable(managementService);
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

    public static void waitForJobExecutorOnCondition(ActivitiRule activitiRule, long maxMillisToWait, long intervalMillis, Callable<Boolean> condition) {
        waitForJobExecutorOnCondition(activitiRule.getProcessEngine().getProcessEngineConfiguration(), maxMillisToWait, intervalMillis, condition);
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

    public static void executeJobExecutorForTime(ActivitiRule activitiRule, long maxMillisToWait, long intervalMillis) {
        executeJobExecutorForTime(activitiRule.getProcessEngine().getProcessEngineConfiguration(), maxMillisToWait, intervalMillis);
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

    public static boolean areJobsAvailable(ActivitiRule activitiRule) {
        return areJobsAvailable(activitiRule.getManagementService());
    }

    public static boolean areJobsAvailable(ManagementService managementService) {
        // The timers are checked before and after the jobs, since a timer can be moved to an async job (or an async job can create a timer)
        // between the queries
        return !managementService.createTimerJobQuery().executable().list().isEmpty()
                || !managementService.createJobQuery().list().isEmpty()
                || !managementService.createTimerJobQuery().executable().list().isEmpty();
    }

    public static boolean areJobsAvailable(org.activiti.engine.ManagementService managementService) {
        return !managementService.createJobQuery().list().isEmpty();
    }

    public static boolean areJobsOrExecutableTimersAvailable(ManagementService managementService) {
        // The timers are checked before and after the jobs, since a timer can be moved to an async job (or an async job can create a timer)
        // between the queries
        return !managementService.createTimerJobQuery().executable().list().isEmpty()
                || !managementService.createJobQuery().list().isEmpty()
                || !managementService.createTimerJobQuery().executable().list().isEmpty();
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

        public void run() {
            timeLimitExceeded = true;
            thread.interrupt();
        }
    }
}
