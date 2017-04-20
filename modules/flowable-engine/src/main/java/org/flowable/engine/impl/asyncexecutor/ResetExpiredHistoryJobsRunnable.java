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
package org.flowable.engine.impl.asyncexecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.flowable.engine.common.api.FlowableOptimisticLockingException;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable that checks the {@link Job} entities periodically for 'expired' jobs.
 * 
 * When a job is executed, it is first locked (lock owner and lock time is set). A job is expired when this lock time is exceeded. This can happen when an executor goes down before completing a task.
 * 
 * This runnable will find such jobs and reset them, so they can be picked up again.
 * 
 * @author Tijs Rademakers
 */
public class ResetExpiredHistoryJobsRunnable implements Runnable {

    private static Logger log = LoggerFactory.getLogger(ResetExpiredHistoryJobsRunnable.class);

    protected final AsyncHistoryExecutor asyncExecutor;

    protected volatile boolean isInterrupted;
    protected final Object MONITOR = new Object();
    protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

    public ResetExpiredHistoryJobsRunnable(AsyncHistoryExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    public synchronized void run() {
        log.info("starting to reset expired history jobs");
        Thread.currentThread().setName("flowable-reset-expired-history-jobs");

        while (!isInterrupted) {

            try {

                List<HistoryJobEntity> expiredJobs = asyncExecutor.getProcessEngineConfiguration().getCommandExecutor()
                        .execute(new FindExpiredHistoryJobsCmd(asyncExecutor.getResetExpiredJobsPageSize()));

                List<String> expiredJobIds = new ArrayList<String>(expiredJobs.size());
                for (HistoryJobEntity expiredJob : expiredJobs) {
                    expiredJobIds.add(expiredJob.getId());
                }

                if (expiredJobIds.size() > 0) {
                    asyncExecutor.getProcessEngineConfiguration().getCommandExecutor().execute(
                            new ResetExpiredHistoryJobsCmd(expiredJobIds));
                }

            } catch (Throwable e) {
                if (e instanceof FlowableOptimisticLockingException) {
                    log.debug("Optimistic lock exception while resetting locked history jobs", e);
                } else {
                    log.error("exception during resetting expired history jobs: {}", e.getMessage(), e);
                }
            }

            // Sleep
            try {

                synchronized (MONITOR) {
                    if (!isInterrupted) {
                        isWaiting.set(true);
                        MONITOR.wait(asyncExecutor.getResetExpiredJobsInterval());
                    }
                }

            } catch (InterruptedException e) {
                if (log.isDebugEnabled()) {
                    log.debug("async reset expired history jobs wait interrupted");
                }
            } finally {
                isWaiting.set(false);
            }

        }

        log.info("stopped resetting expired history jobs");
    }

    public void stop() {
        synchronized (MONITOR) {
            isInterrupted = true;
            if (isWaiting.compareAndSet(true, false)) {
                MONITOR.notifyAll();
            }
        }
    }

}
