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
package org.flowable.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.persistence.entity.EntityManager;
import org.flowable.engine.impl.HistoryJobQueryImpl;
import org.flowable.engine.impl.asyncexecutor.AcquireTimerJobsRunnable;
import org.flowable.engine.impl.cmd.AcquireJobsCmd;
import org.flowable.engine.runtime.Job;

/**
 * {@link EntityManager} responsible for the {@link HistoryJobEntity} class.
 * 
 * @author Tijs Rademakers
 */
public interface HistoryJobEntityManager extends EntityManager<HistoryJobEntity> {

    /**
     * Insert the {@link HistoryJobEntity}, similar to {@link #insert(HistoryJobEntity)}, but returns a boolean in case the insert did not go through. This could happen if the execution related to the
     * {@link HistoryJobEntity} has been removed.
     */
    void insertHistoryJobEntity(HistoryJobEntity HistoryJobEntity);

    /**
     * Returns {@link HistoryJobEntity} that are eligible to be executed.
     * 
     * For example used by the default {@link AcquireJobsCmd} command used by the default {@link AcquireTimerJobsRunnable} implementation to get async jobs that can be executed.
     */
    List<HistoryJobEntity> findHistoryJobsToExecute(Page page);

    /**
     * Returns all {@link HistoryJobEntity} instances related to on {@link ExecutionEntity}.
     */
    List<HistoryJobEntity> findHistoryJobsByExecutionId(String executionId);

    /**
     * Returns all {@link HistoryJobEntity} instances related to one process instance {@link ExecutionEntity}.
     */
    List<HistoryJobEntity> findHistoryJobsByProcessInstanceId(String processInstanceId);

    /**
     * Returns all {@link HistoryJobEntity} instance which are expired, which means that the lock time of the {@link HistoryJobEntity} is past a certain configurable date and is deemed to be in error.
     */
    List<HistoryJobEntity> findExpiredHistoryJobs(Page page);

    /**
     * Executes a {@link HistoryJobQueryImpl} and returns the matching {@link HistoryJobEntity} instances.
     */
    List<Job> findHistoryJobsByQueryCriteria(HistoryJobQueryImpl jobQuery, Page page);

    /**
     * Same as {@link #findHistoryJobsByQueryCriteria(HistoryJobQueryImpl, Page)}, but only returns a count and not the instances itself.
     */
    long findHistoryJobCountByQueryCriteria(HistoryJobQueryImpl jobQuery);

    /**
     * Resets an expired job. These are jobs that were locked, but not completed. Resetting these will make them available for being picked up by other executors.
     */
    void resetExpiredHistoryJob(String jobId);

    /**
     * Changes the tenantId for all jobs related to a given {@link DeploymentEntity}.
     */
    void updateHistoryJobTenantIdForDeployment(String deploymentId, String newTenantId);

}
