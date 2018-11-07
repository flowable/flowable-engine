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
package org.flowable.job.service.impl.cmd;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntityManager;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ExecuteAsyncJobCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteAsyncJobCmd.class);

    protected String jobId;
    protected JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;

    public ExecuteAsyncJobCmd(String jobId) {
        this.jobId = jobId;
    }
    
    public ExecuteAsyncJobCmd(String jobId, JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager) {
        this.jobId = jobId;
        this.jobEntityManager = jobEntityManager;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        
        if (jobEntityManager == null) {
            jobEntityManager = CommandContextUtil.getJobEntityManager(commandContext); // Backwards compatibility
        }

        if (jobId == null) {
            throw new FlowableIllegalArgumentException("jobId is null");
        }

        // We need to refetch the job, as it could have been deleted by another concurrent job
        // For example: an embedded subprocess with a couple of async tasks and a timer on the boundary of the subprocess
        // when the timer fires, all executions and thus also the jobs inside of the embedded subprocess are destroyed.
        // However, the async task jobs could already have been fetched and put in the queue.... while in reality they have been deleted.
        // A refetch is thus needed here to be sure that it exists for this transaction.

        JobInfoEntity job = jobEntityManager.findById(jobId);
        if (job == null) {
            LOGGER.debug("Job does not exist anymore and will not be executed. It has most likely been deleted "
                    + "as part of another concurrent part of the process instance.");
            return null;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing async job {}", job.getId());
        }

        CommandContextUtil.getJobManager(commandContext).execute(job);

        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher(commandContext);
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(
                    FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_EXECUTION_SUCCESS, job));
        }

        return null;
    }
}
