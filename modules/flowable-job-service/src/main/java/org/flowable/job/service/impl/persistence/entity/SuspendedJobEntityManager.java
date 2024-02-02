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

import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.JobQueryImpl;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;

/**
 * @author Tijs Rademakers
 */
public interface SuspendedJobEntityManager extends EntityManager<SuspendedJobEntity> {

    /**
     * Find the suspended job with the given correlation id.
     */
    SuspendedJobEntity findJobByCorrelationId(String correlationId);

    /**
     * Returns all {@link SuspendedJobEntity} instances related to an execution id.
     */
    List<SuspendedJobEntity> findJobsByExecutionId(String id);

    /**
     * Returns all {@link SuspendedJobEntity} instances related to an execution id.
     */
    List<SuspendedJobEntity> findJobsByProcessInstanceId(String id);

    /**
     * Executes a {@link JobQueryImpl} and returns the matching {@link SuspendedJobEntity} instances.
     */
    List<Job> findJobsByQueryCriteria(SuspendedJobQueryImpl jobQuery);

    /**
     * Same as {@link #findJobsByQueryCriteria(SuspendedJobQueryImpl)}, but only returns a count and not the instances itself.
     */
    long findJobCountByQueryCriteria(SuspendedJobQueryImpl jobQuery);

    /**
     * Changes the tenantId for all jobs related to a given deployment id.
     */
    void updateJobTenantIdForDeployment(String deploymentId, String newTenantId);
    
}
