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
package org.flowable.job.service.impl.asyncexecutor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable that checks the {@link Job} entities periodically for 'expired' jobs.
 * 
 * When a job is executed, it is first locked (lock owner and lock time is set).
 * A job is expired when this lock time is exceeded (this can for example happen when an executor goes down before completing a task)
 * 
 * This runnable will find such jobs and reset them, so they can be picked up again.
 * 
 * @author Joram Barrez
 */
public class ResetExpiredJobsRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetExpiredJobsRunnable.class);

    protected final String name;
    protected final AsyncExecutor asyncExecutor;
    protected final Collection<JobInfoEntityManager<? extends JobInfoEntity>> jobInfoEntityManagers;

    protected volatile boolean isInterrupted;
    protected final Object MONITOR = new Object();
    protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

    public ResetExpiredJobsRunnable(String name, AsyncExecutor asyncExecutor,
            JobInfoEntityManager<? extends JobInfoEntity>... jobEntityManagers) {
        this.name = name;
        this.asyncExecutor = asyncExecutor;
        if (jobEntityManagers.length < 1) {
            throw new IllegalArgumentException("there must be at least one job entity manager");
        }
        this.jobInfoEntityManagers = Arrays.asList(jobEntityManagers);
    }

    @Override
    public synchronized void run() {
        LOGGER.info("starting to reset expired jobs for engine {}", getEngineName());
        Thread.currentThread().setName(name);

        while (!isInterrupted) {

            resetJobs();

            // Sleep
            try {

                synchronized (MONITOR) {
                    if (!isInterrupted) {
                        isWaiting.set(true);
                        MONITOR.wait(asyncExecutor.getResetExpiredJobsInterval());
                    }
                }

            } catch (InterruptedException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("async reset expired jobs wait for engine {} interrupted", getEngineName());
                }
            } finally {
                isWaiting.set(false);
            }

        }

        LOGGER.info("stopped resetting expired jobs for engine {}", asyncExecutor.getJobServiceConfiguration().getEngineName());
    }

    /**
     * Resets jobs that were expired. Will continue to reset jobs until no more jobs are returned.
     */
    public void resetJobs() {
        for (JobInfoEntityManager<? extends JobInfoEntity> jobInfoEntityManager : jobInfoEntityManagers) {
            resetJobs(jobInfoEntityManager);
        }
    }

    protected void resetJobs(JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager) {

        boolean hasExpiredJobs = true;
        while (hasExpiredJobs && !isInterrupted) {

            try {
                JobServiceConfiguration jobServiceConfiguration = asyncExecutor.getJobServiceConfiguration();
                List<? extends JobInfoEntity> expiredJobs = jobServiceConfiguration.getCommandExecutor()
                        .execute(new FindExpiredJobsCmd(asyncExecutor.getResetExpiredJobsPageSize(), jobEntityManager, jobServiceConfiguration));

                List<String> expiredJobIds = expiredJobs.stream().map(JobInfoEntity::getId).collect(Collectors.toList());
                if (!expiredJobIds.isEmpty()) {
                    asyncExecutor.getJobServiceConfiguration().getCommandExecutor().execute(
                            new ResetExpiredJobsCmd(expiredJobIds, jobEntityManager, jobServiceConfiguration));

                } else {
                    hasExpiredJobs = false;

                }

            } catch (Throwable e) {

                // If an optimistic locking exception happens, we continue resetting.
                // If another exception happens, we return the method which will trigger a sleep.

                if (e instanceof FlowableOptimisticLockingException) {
                    LOGGER.debug("Optimistic lock exception while resetting locked jobs for engine {}", asyncExecutor.getJobServiceConfiguration().getEngineName(), e);

                } else {
                    LOGGER.warn("exception during resetting expired jobs: {} for engine {}", e.getMessage(),
                                    asyncExecutor.getJobServiceConfiguration().getEngineName(), e);
                    hasExpiredJobs = false; // will stop the loop

                }
            }

        }

    }

    public void stop() {
        synchronized (MONITOR) {
            isInterrupted = true;
            if (isWaiting.compareAndSet(true, false)) {
                MONITOR.notifyAll();
            }
        }
    }

    protected String getEngineName() {
        return asyncExecutor.getJobServiceConfiguration().getEngineName();
    }

    public boolean isInterrupted() {
        return isInterrupted;
    }
    public void setInterrupted(boolean interrupted) {
        isInterrupted = interrupted;
    }
}
