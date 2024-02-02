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

package org.flowable.engine.test.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.exceptions.PersistenceException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test that uses a number of threads to start processes and complete tasks concurrently.
 * 
 * @author Frederik Heremans
 */
public class ConcurrentEngineUsageTest extends PluggableFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentEngineUsageTest.class);
    private static final int MAX_RETRIES = 5;

    @Test
    @Deployment
    public void testConcurrentUsage() throws Exception {

        if (!"h2".equals(processEngineConfiguration.getDatabaseType()) && !"db2".equals(processEngineConfiguration.getDatabaseType())) {
            int numberOfThreads = 5;
            int numberOfProcessesPerThread = 5;
            int totalNumberOfTasks = 2 * numberOfThreads * numberOfProcessesPerThread;

            ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(numberOfThreads));

            for (int i = 0; i < numberOfThreads; i++) {
                executor.execute(new ConcurrentProcessRunnerRunnable(numberOfProcessesPerThread, "kermit" + i));
            }

            // Wait for termination or timeout and check if all tasks are
            // complete
            executor.shutdown();
            boolean isEnded = executor.awaitTermination(20000, TimeUnit.MILLISECONDS);
            if (!isEnded) {
                LOGGER.error("Executor was not shut down after timeout, not al tasks have been executed");
                executor.shutdownNow();

            }
            assertThat(executor.getActiveCount()).isZero();

            // Check there are no processes active anymore
            assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                // Check if all processes and tasks are complete
                assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(numberOfProcessesPerThread * numberOfThreads);
                assertThat(historyService.createHistoricTaskInstanceQuery().finished().count()).isEqualTo(totalNumberOfTasks);
            }
        }
    }

    protected void retryStartProcess(String runningUser) {
        int retries = MAX_RETRIES;
        int timeout = 200;
        boolean success = false;
        while (retries > 0 && !success) {
            try {
                runtimeService.startProcessInstanceByKey("concurrentProcess", Collections.singletonMap("assignee", (Object) runningUser));
                success = true;
            } catch (PersistenceException pe) {
                retries = retries - 1;
                LOGGER.debug("Retrying process start - {}", (MAX_RETRIES - retries));
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ignore) {
                }
                timeout = timeout + 200;
            }
        }
        if (!success) {
            LOGGER.debug("Retrying process start FAILED {} times", MAX_RETRIES);
        }
    }

    protected void retryFinishTask(String taskId) {
        int retries = MAX_RETRIES;
        int timeout = 200;
        boolean success = false;
        while (retries > 0 && !success) {
            try {
                taskService.complete(taskId);
                success = true;
            } catch (PersistenceException pe) {
                retries = retries - 1;
                LOGGER.debug("Retrying task completion - {}", (MAX_RETRIES - retries));
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ignore) {
                }
                timeout = timeout + 200;
            }
        }

        if (!success) {
            LOGGER.debug("Retrying task completion FAILED {} times", MAX_RETRIES);
        }
    }

    private class ConcurrentProcessRunnerRunnable implements Runnable {
        private String drivingUser;
        private int numberOfProcesses;

        public ConcurrentProcessRunnerRunnable(int numberOfProcesses, String drivingUser) {
            this.drivingUser = drivingUser;
            this.numberOfProcesses = numberOfProcesses;
        }

        @Override
        public void run() {
            Authentication.setAuthenticatedUserId(drivingUser);

            boolean finishTask = false;
            boolean tasksAvailable = false;

            while (numberOfProcesses > 0 || tasksAvailable) {
                if (numberOfProcesses > 0 && !finishTask) {
                    // Start a new process
                    retryStartProcess(drivingUser);
                    finishTask = true;

                    if (numberOfProcesses == 0) {
                        // Make sure while-loop doesn't stop when processes are
                        // all started
                        tasksAvailable = taskService.createTaskQuery().taskAssignee(drivingUser).count() > 0;
                    }
                    numberOfProcesses = numberOfProcesses - 1;
                } else {
                    // Finish a task
                    List<org.flowable.task.api.Task> taskToComplete = taskService.createTaskQuery().taskAssignee(drivingUser).listPage(0, 1);
                    tasksAvailable = !taskToComplete.isEmpty();
                    if (tasksAvailable) {
                        retryFinishTask(taskToComplete.get(0).getId());
                    }
                    finishTask = false;
                }
            }
        }
    }
}
