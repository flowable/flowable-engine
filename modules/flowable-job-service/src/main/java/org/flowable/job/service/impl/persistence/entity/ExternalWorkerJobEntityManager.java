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
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.service.impl.ExternalWorkerJobAcquireBuilderImpl;
import org.flowable.job.service.impl.ExternalWorkerJobQueryImpl;

/**
 * {@link EntityManager} responsible for the {@link ExternalWorkerJobEntity} class.
 *
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJobEntityManager extends EntityManager<ExternalWorkerJobEntity>, JobInfoEntityManager<ExternalWorkerJobEntity> {

    /**
     * Insert the {@link ExternalWorkerJobEntity}, similar to insert(ExternalWorkerJobEntity), but returns a boolean in case the insert did not go through. This could happen if the execution related to the
     * {@link ExternalWorkerJobEntity} has been removed.
     */
    boolean insertExternalWorkerJobEntity(ExternalWorkerJobEntity externalWorkerJobEntity);

    /**
     * Find the external worker job by the given correlationId
     */
    ExternalWorkerJobEntity findJobByCorrelationId(String correlationId);

    /**
     * Returns all {@link ExternalWorkerJobEntity} for the given scope and subscope.
     */
    List<ExternalWorkerJobEntity> findJobsByScopeIdAndSubScopeId(String scopeId, String subScopeId);

    /**
     * Executes a {@link ExternalWorkerJobQueryImpl} and returns the matching {@link ExternalWorkerJobEntity} instances.
     * @return
     */
    List<ExternalWorkerJob> findJobsByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery);

    /**
     * Same as {@link #findJobsByQueryCriteria(ExternalWorkerJobQueryImpl)}, but only returns a count and not the instances itself.
     */
    long findJobCountByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery);

    List<ExternalWorkerJobEntity> findExternalJobsToExecute(ExternalWorkerJobAcquireBuilderImpl builder, int numberOfJobs);
}
