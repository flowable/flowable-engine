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

import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * @author Tijs Rademakers
 * @author Vasile Dirla
 */
public interface TimerJobDataManager extends DataManager<TimerJobEntity> {

    List<TimerJobEntity> findTimerJobsToExecute(Page page);

    List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId);

    List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey);

    List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId);

    List<TimerJobEntity> findJobsByExecutionId(String executionId);

    List<TimerJobEntity> findJobsByProcessInstanceId(String processInstanceId);

    List<Job> findJobsByQueryCriteria(TimerJobQueryImpl jobQuery);

    long findJobCountByQueryCriteria(TimerJobQueryImpl jobQuery);

    void updateJobTenantIdForDeployment(String deploymentId, String newTenantId);
    
}
