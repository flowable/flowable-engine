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
package org.activiti.engine.impl.cmd;

import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;

/**
 * Command that dispatches a JOB_CANCELLED event and deletes the job entity.
 */
public class CancelJobCmd extends DeleteJobCmd {

    private static final long serialVersionUID = 1L;

    public CancelJobCmd(String jobId) {
        super(jobId);
    }

    @Override
    public Object execute(CommandContext commandContext) {
        JobEntity jobToDelete = getJobToDelete(commandContext);

        sendCancelEvent(jobToDelete);

        jobToDelete.delete();
        return null;
    }

    private void sendCancelEvent(JobEntity jobToDelete) {
        if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                    ActivitiEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, jobToDelete));
        }
    }

}
