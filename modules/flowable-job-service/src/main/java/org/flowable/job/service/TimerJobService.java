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
package org.flowable.job.service;

import java.util.List;

import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * Service which provides access to timer jobs.
 * 
 * @author Tijs Rademakers
 */
public interface TimerJobService {
    
    void scheduleTimerJob(TimerJobEntity timerJob);
    
    TimerJobEntity findTimerJobById(String jobId);
    
    List<TimerJobEntity> findTimerJobsByExecutionId(String executionId);
    
    List<TimerJobEntity> findTimerJobsByProcessInstanceId(String processInstanceId);
    
    List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String type, String processDefinitionId);
    
    List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String type, String processDefinitionKey);
    
    List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String type, String processDefinitionKey, String tenantId);
    
    AbstractRuntimeJobEntity moveJobToTimerJob(JobEntity job);
    
    TimerJobEntity createTimerJob();
    
    void insertTimerJob(TimerJobEntity timerJob);
    
    void deleteTimerJob(TimerJobEntity timerJob);
    
    void deleteTimerJobsByExecutionId(String executionId);
}
