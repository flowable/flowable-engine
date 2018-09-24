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
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */

public class DeleteSuspendedJobCmd implements Command<Object>, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteSuspendedJobCmd.class);
    private static final long serialVersionUID = 1L;

    protected String suspendedJobId;

    public DeleteSuspendedJobCmd(String suspendedJobId) {
        this.suspendedJobId = suspendedJobId;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        SuspendedJobEntity jobToDelete = getJobToDelete(commandContext);

        sendCancelEvent(jobToDelete);

        CommandContextUtil.getSuspendedJobEntityManager(commandContext).delete(jobToDelete);
        return null;
    }

    protected void sendCancelEvent(SuspendedJobEntity jobToDelete) {
        if (CommandContextUtil.getJobServiceConfiguration().getEventDispatcher().isEnabled()) {
            CommandContextUtil.getJobServiceConfiguration().getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, jobToDelete));
        }
    }

    protected SuspendedJobEntity getJobToDelete(CommandContext commandContext) {
        if (suspendedJobId == null) {
            throw new FlowableIllegalArgumentException("jobId is null");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleting job {}", suspendedJobId);
        }

        SuspendedJobEntity job = CommandContextUtil.getSuspendedJobEntityManager(commandContext).findById(suspendedJobId);
        if (job == null) {
            throw new FlowableObjectNotFoundException("No suspended job found with id '" + suspendedJobId + "'", Job.class);
        }

        return job;
    }

}
