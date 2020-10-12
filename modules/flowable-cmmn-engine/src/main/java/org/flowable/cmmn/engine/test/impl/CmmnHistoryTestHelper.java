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

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnHistoryTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnHistoryTestHelper.class);

    public static boolean isHistoryLevelAtLeast(HistoryLevel historyLevel, CmmnEngineConfiguration cmmnEngineConfiguration) {
        return isHistoryLevelAtLeast(historyLevel, cmmnEngineConfiguration, 20000);
    }

    public static boolean isHistoryLevelAtLeast(HistoryLevel historyLevel, CmmnEngineConfiguration cmmnEngineConfiguration, long time) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(historyLevel)) {

            // When using async history, we need to process all the historic jobs first before the history can be checked
            if (cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
                LOGGER.debug("CMMN engine is configured to use asynchronous history. Processing async history jobs now, before continuing.");
                waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnEngineConfiguration.getCmmnManagementService(), time, 200);
            }

            return true;
        }

        return false;
    }

    public static void waitForJobExecutorToProcessAllHistoryJobs(CmmnEngineConfiguration cmmnEngineConfiguration, CmmnManagementService managementService,
            long maxMillisToWait, long intervalMillis) {
        waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, managementService, maxMillisToWait, intervalMillis, true);
    }

    public static void waitForJobExecutorToProcessAllHistoryJobs(CmmnEngineConfiguration cmmnEngineConfiguration, CmmnManagementService managementService,
            long maxMillisToWait, long intervalMillis, boolean shutdownExecutorWhenFinished) {

        if (cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
            AsyncExecutor asyncHistoryExecutor = cmmnEngineConfiguration.getAsyncHistoryExecutor();

            if (!asyncHistoryExecutor.isActive()) {
                asyncHistoryExecutor.start();
            }

            try {
                Timer timer = new Timer();
                InterruptTask task = new InterruptTask(Thread.currentThread());
                timer.schedule(task, maxMillisToWait);
                boolean areJobsAvailable = true;
                try {
                    while (areJobsAvailable && !task.isTimeLimitExceeded()) {
                        Thread.sleep(intervalMillis);
                        try {
                            areJobsAvailable = areHistoryJobsAvailable(managementService);
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
                    asyncHistoryExecutor.shutdown();
                }
            }
        }
    }

    public static boolean areHistoryJobsAvailable(CmmnManagementService managementService) {
        return !managementService.createHistoryJobQuery().list().isEmpty();
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
