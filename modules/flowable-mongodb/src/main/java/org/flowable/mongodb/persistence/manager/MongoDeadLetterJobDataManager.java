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

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.data.DeadLetterJobDataManager;

import com.mongodb.BasicDBObject;

/**
 * @author Joram Barrez
 */
public class MongoDeadLetterJobDataManager extends AbstractMongoDbDataManager<DeadLetterJobEntity> implements DeadLetterJobDataManager {

    public static final String COLLECTION_DEADLETTER_JOBS = "deadLetterJobs";
    
    @Override
    public String getCollection() {
        return COLLECTION_DEADLETTER_JOBS;
    }
    
    @Override
    public DeadLetterJobEntity create() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        return null;
    }

    @Override
    public List<DeadLetterJobEntity> findJobsByExecutionId(String executionId) {
        return Collections.emptyList();
    }

    @Override
    public List<DeadLetterJobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Job> findJobsByQueryCriteria(DeadLetterJobQueryImpl jobQuery) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findJobCountByQueryCriteria(DeadLetterJobQueryImpl jobQuery) {
        return 0;
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        throw new UnsupportedOperationException();
        
    }

}
