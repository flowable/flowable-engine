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

/**
 * {@link EntityManager} responsible for the {@link JobEntity} class.
 *
 * @author Joram Barrez
 */
public interface JobEntityManager extends EntityManager<JobEntity>, JobInfoEntityManager<JobEntity> {

    /**
     * Insert the {@link JobEntity}, similar to {@link #insert(JobEntity)}, but returns a boolean in case the insert did not go through. This could happen if the execution related to the
     * {@link JobEntity} has been removed.
     */
    boolean insertJobEntity(JobEntity jobEntity);

    /**
     * Executes a {@link JobQueryImpl} and returns the matching {@link JobEntity} instances.
     */
    List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery);

    /**
     * Same as {@link #findJobsByQueryCriteria(JobQueryImpl)}, but only returns a count and not the instances itself.
     */
    long findJobCountByQueryCriteria(JobQueryImpl jobQuery);
    
}
