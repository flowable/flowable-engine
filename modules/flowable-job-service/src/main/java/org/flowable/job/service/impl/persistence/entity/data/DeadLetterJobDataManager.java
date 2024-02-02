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
package org.flowable.job.service.impl.persistence.entity.data;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;

/**
 * @author Tijs Rademakers
 */
public interface DeadLetterJobDataManager extends DataManager<DeadLetterJobEntity> {

    DeadLetterJobEntity findJobByCorrelationId(String correlationId);

    List<DeadLetterJobEntity> findJobsByExecutionId(String executionId);
    
    List<DeadLetterJobEntity> findJobsByProcessInstanceId(String processInstanceId);

    List<Job> findJobsByQueryCriteria(DeadLetterJobQueryImpl jobQuery);

    long findJobCountByQueryCriteria(DeadLetterJobQueryImpl jobQuery);

    void updateJobTenantIdForDeployment(String deploymentId, String newTenantId);
    
}
