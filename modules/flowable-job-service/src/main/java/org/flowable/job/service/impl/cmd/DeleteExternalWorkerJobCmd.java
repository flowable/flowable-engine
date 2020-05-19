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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;
import org.flowable.job.service.impl.util.CommandContextUtil;

/**
 * @author Filip Hrisafov
 */
public class DeleteExternalWorkerJobCmd implements Command<Object> {

    protected String jobId;

    public DeleteExternalWorkerJobCmd(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("jobId is null");
        }

        ExternalWorkerJobEntityManager jobEntityManager = CommandContextUtil.getExternalWorkerJobEntityManager(commandContext);

        ExternalWorkerJobEntity job = jobEntityManager.findById(jobId);
        if (job == null) {
            throw new FlowableObjectNotFoundException("No external worker job found with id '" + jobId + "'", Job.class);
        }

        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getJobServiceConfiguration().getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher
                    .dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, job));
        }

        jobEntityManager.delete(job);

        return null;
    }
}
