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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.InternalJobCompatibilityManager;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */

public class DeleteJobCmd implements Command<Object>, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteJobCmd.class);
    private static final long serialVersionUID = 1L;

    protected String jobId;

    public DeleteJobCmd(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        JobEntity jobToDelete = getJobToDelete(commandContext);

        JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getJobServiceConfiguration(commandContext);
        InternalJobCompatibilityManager internalJobCompatibilityManager = jobServiceConfiguration.getInternalJobCompatibilityManager(); 
        if (internalJobCompatibilityManager != null && internalJobCompatibilityManager.isFlowable5Job(jobToDelete)) {
            internalJobCompatibilityManager.deleteV5Job(jobToDelete.getId());
            return null;
        }

        sendCancelEvent(jobToDelete);

        CommandContextUtil.getJobEntityManager(commandContext).delete(jobToDelete);
        return null;
    }

    protected void sendCancelEvent(JobEntity jobToDelete) {
        if (CommandContextUtil.getJobServiceConfiguration().getEventDispatcher().isEnabled()) {
            CommandContextUtil.getJobServiceConfiguration().getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, jobToDelete));
        }
    }

    protected JobEntity getJobToDelete(CommandContext commandContext) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("jobId is null");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleting job {}", jobId);
        }

        JobEntity job = CommandContextUtil.getJobEntityManager(commandContext).findById(jobId);
        if (job == null) {
            throw new FlowableObjectNotFoundException("No job found with id '" + jobId + "'", Job.class);
        }

        // We need to check if the job was locked, ie acquired by the job acquisition thread
        // This happens if the the job was already acquired, but not yet executed.
        // In that case, we can't allow to delete the job.
        if (job.getLockOwner() != null) {
            throw new FlowableException("Cannot delete job when the job is being executed. Try again later.");
        }
        return job;
    }

}
