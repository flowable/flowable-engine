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
package org.flowable.job.service.impl;

import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;

/**
 * @author Tijs Rademakers
 */
public class TimerJobServiceImpl extends ServiceImpl implements TimerJobService {

    public TimerJobServiceImpl(JobServiceConfiguration jobServiceConfiguration) {
        super(jobServiceConfiguration);
    }
    
    @Override
    public TimerJobEntity findTimerJobById(String jobId) {
        return getTimerJobEntityManager().findById(jobId);
    }

    @Override
    public List<TimerJobEntity> findTimerJobsByExecutionId(String executionId) {
        return getTimerJobEntityManager().findJobsByExecutionId(executionId);
    }

    @Override
    public List<TimerJobEntity> findTimerJobsByProcessInstanceId(String processInstanceId) {
        return getTimerJobEntityManager().findJobsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String type, String processDefinitionId) {
        return getTimerJobEntityManager().findJobsByTypeAndProcessDefinitionId(type, processDefinitionId);
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String type, String processDefinitionKey) {
        return getTimerJobEntityManager().findJobsByTypeAndProcessDefinitionKeyNoTenantId(type, processDefinitionKey);
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String type, String processDefinitionKey, String tenantId) {
        return getTimerJobEntityManager().findJobsByTypeAndProcessDefinitionKeyAndTenantId(type, processDefinitionKey, tenantId);
    }
    
    @Override
    public void scheduleTimerJob(TimerJobEntity timerJob) {
        getJobManager().scheduleTimerJob(timerJob);
    }
    
    @Override
    public AbstractRuntimeJobEntity moveJobToTimerJob(JobEntity job) {
        return getJobManager().moveJobToTimerJob(job);
    }
    
    @Override
    public TimerJobEntity createTimerJob() {
        return getTimerJobEntityManager().create();
    }

    @Override
    public void insertTimerJob(TimerJobEntity timerJob) {
        getTimerJobEntityManager().insert(timerJob);
    }

    @Override
    public void deleteTimerJob(TimerJobEntity timerJob) {
        getTimerJobEntityManager().delete(timerJob);
    }

    @Override
    public void deleteTimerJobsByExecutionId(String executionId) {
        TimerJobEntityManager timerJobEntityManager = getTimerJobEntityManager();
        Collection<TimerJobEntity> timerJobsForExecution = timerJobEntityManager.findJobsByExecutionId(executionId);
        for (TimerJobEntity job : timerJobsForExecution) {
            timerJobEntityManager.delete(job);
            if (getEventDispatcher().isEnabled()) {
                getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, job));
            }
        }
    }
}
