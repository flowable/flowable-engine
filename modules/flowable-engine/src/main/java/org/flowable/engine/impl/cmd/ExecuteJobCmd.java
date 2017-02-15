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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.engine.JobNotFoundException;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.jobexecutor.FailedJobListener;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ExecuteJobCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ExecuteJobCmd.class);

    protected String jobId;

    public ExecuteJobCmd(String jobId) {
        this.jobId = jobId;
    }

    public Object execute(CommandContext commandContext) {

        if (jobId == null) {
            throw new FlowableIllegalArgumentException("jobId and job is null");
        }

        Job job = commandContext.getJobEntityManager().findById(jobId);

        if (job == null) {
            throw new JobNotFoundException(jobId);
        }

        if (log.isDebugEnabled()) {
            log.debug("Executing job {}", job.getId());
        }

        if (job.getProcessDefinitionId() != null && Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, job.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.executeJob(job);
            return null;
        }

        commandContext.addCloseListener(new FailedJobListener(commandContext.getProcessEngineConfiguration().getCommandExecutor(), job));

        try {
            commandContext.getJobManager().execute(job);
        } catch (Throwable exception) {
            // Finally, Throw the exception to indicate the ExecuteJobCmd failed
            throw new FlowableException("Job " + jobId + " failed", exception);
        }

        return null;
    }

    public String getJobId() {
        return jobId;
    }

}
