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

import java.util.List;

import org.flowable.common.engine.impl.Page;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;

/**
 * @author Joram Barrez
 */
public class MongoDbTimerJobDataManager extends AbstractMongoDbDataManager implements TimerJobDataManager {

    @Override
    public TimerJobEntity create() {
        return null;
    }

    @Override
    public TimerJobEntity findById(String entityId) {
        return null;
    }

    @Override
    public void insert(TimerJobEntity entity) {
        
    }

    @Override
    public TimerJobEntity update(TimerJobEntity entity) {
        return null;
    }

    @Override
    public void delete(String id) {
        
    }

    @Override
    public void delete(TimerJobEntity entity) {
        
    }

    @Override
    public List<TimerJobEntity> findTimerJobsToExecute(Page page) {
        return null;
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
        return null;
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
        return null;
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
        return null;
    }

    @Override
    public List<TimerJobEntity> findJobsByExecutionId(String executionId) {
        return null;
    }

    @Override
    public List<TimerJobEntity> findJobsByProcessInstanceId(String processInstanceId) {
        return null;
    }

    @Override
    public List<Job> findJobsByQueryCriteria(TimerJobQueryImpl jobQuery) {
        return null;
    }

    @Override
    public long findJobCountByQueryCriteria(TimerJobQueryImpl jobQuery) {
        return 0;
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        
    }

}
