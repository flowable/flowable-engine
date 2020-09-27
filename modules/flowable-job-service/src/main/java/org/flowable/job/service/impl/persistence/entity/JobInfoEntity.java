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

import java.util.Date;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.job.api.JobInfo;

public interface JobInfoEntity extends JobInfo, AbstractJobEntity, Entity, HasRevision {

    String getLockOwner();

    void setLockOwner(String claimedBy);

    Date getLockExpirationTime();

    void setLockExpirationTime(Date claimedUntil);

    /**
     * Set the scope type for the job.
     * The scope type is the type which is used by the job executor to pick
     * the jobs for executing.
     * <p>
     * For example if the job should be picked up by the CMMN Job executor then it
     * should have the same type as the CMMN job executor.
     * @param scopeType the scope type for the job
     */
    @Override
    void setScopeType(String scopeType);

    @Override
    String getScopeType();
    
}
