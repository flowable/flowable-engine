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
package org.flowable.job.service.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;

public interface JobInfoEntityManager <T extends JobInfoEntity> extends EntityManager<T> {

    /**
     * Returns {@link JobEntity} that are eligible to be executed.
     * 
     * For example used by the default {@link AcquireJobsCmd} command used by the default AcquireTimerJobsRunnable implementation to get async jobs that can be executed.
     */
    List<T> findJobsToExecute(Page page);

    /**
     * Returns all {@link JobEntity} instances related to on ExecutionEntity.
     */
    List<T> findJobsByExecutionId(String executionId);

    /**
     * Returns all {@link JobEntity} instances related to one process instance ExecutionEntity.
     */
    List<T> findJobsByProcessInstanceId(String processInstanceId);

    /**
     * Returns all {@link JobEntity} instance which are expired, which means that the lock time of the {@link JobEntity} is past a certain configurable date and is deemed to be in error.
     */
    List<T> findExpiredJobs(Page page);

    /**
     * Resets an expired job. These are jobs that were locked, but not completed. Resetting these will make them available for being picked up by other executors.
     */
    void resetExpiredJob(String jobId);

    /**
     * Changes the tenantId for all jobs related to a given DeploymentEntity.
     */
    void updateJobTenantIdForDeployment(String deploymentId, String newTenantId);

}
