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

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.service.CommonServiceImpl;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;

/**
 * @author Tijs Rademakers
 */
public class ServiceImpl extends CommonServiceImpl<JobServiceConfiguration> {

    public ServiceImpl() {

    }

    public ServiceImpl(JobServiceConfiguration configuration) {
        super(configuration);
    }
    
    public FlowableEventDispatcher getEventDispatcher() {
        return configuration.getEventDispatcher();
    }
    
    public JobManager getJobManager() {
        return configuration.getJobManager();
    }

    public JobEntityManager getJobEntityManager() {
        return configuration.getJobEntityManager();
    }
    
    public DeadLetterJobEntityManager getDeadLetterJobEntityManager() {
        return configuration.getDeadLetterJobEntityManager();
    }
    
    public SuspendedJobEntityManager getSuspendedJobEntityManager() {
        return configuration.getSuspendedJobEntityManager();
    }
    
    public TimerJobEntityManager getTimerJobEntityManager() {
        return configuration.getTimerJobEntityManager();
    }
    
    public HistoryJobEntityManager getHistoryJobEntityManager() {
        return configuration.getHistoryJobEntityManager();
    }
    
    public CommandExecutor getCommandExecutor() {
        return configuration.getCommandExecutor();
    }
}
