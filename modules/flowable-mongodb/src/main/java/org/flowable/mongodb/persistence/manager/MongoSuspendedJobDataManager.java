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
package org.flowable.mongodb.persistence.manager;

import java.util.Collections;
import java.util.List;

import org.flowable.job.api.Job;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.data.SuspendedJobDataManager;

/**
 * @author Joram Barrez
 */
public class MongoSuspendedJobDataManager extends AbstractMongoDbDataManager implements SuspendedJobDataManager {

    public static final String COLLECTION_SUSPENDED_JOBS = "suspendedJobs";
    
    @Override
    public SuspendedJobEntity create() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SuspendedJobEntity findById(String entityId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(SuspendedJobEntity entity) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public SuspendedJobEntity update(SuspendedJobEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void delete(SuspendedJobEntity entity) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public List<SuspendedJobEntity> findJobsByExecutionId(String executionId) {
        return Collections.emptyList();
    }

    @Override
    public List<SuspendedJobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Job> findJobsByQueryCriteria(SuspendedJobQueryImpl jobQuery) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findJobCountByQueryCriteria(SuspendedJobQueryImpl jobQuery) {
        return 0;
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        throw new UnsupportedOperationException();
        
    }

}
