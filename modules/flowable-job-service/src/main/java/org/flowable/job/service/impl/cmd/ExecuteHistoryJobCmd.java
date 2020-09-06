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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.JobNotFoundException;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a {@link HistoryJob} directly (not through the async history executor).
 * 
 * @author Joram Barrez
 */
public class ExecuteHistoryJobCmd implements Command<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteHistoryJobCmd.class);

    protected JobServiceConfiguration jobServiceConfiguration;
    
    protected String historyJobId;

    public ExecuteHistoryJobCmd(String historyJobId, JobServiceConfiguration jobServiceConfiguration) {
        this.historyJobId = historyJobId;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (historyJobId == null) {
            throw new FlowableIllegalArgumentException("historyJobId is null");
        }

        HistoryJobEntity historyJobEntity = jobServiceConfiguration.getHistoryJobEntityManager().findById(historyJobId);
        if (historyJobEntity == null) {
            throw new JobNotFoundException(historyJobId);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing historyJob {}", historyJobEntity.getId());
        }

        try {
            jobServiceConfiguration.getJobManager().execute(historyJobEntity);
        } catch (Throwable exception) {
            // Finally, Throw the exception to indicate the failure
            throw new FlowableException("HistoryJob " + historyJobId + " failed", exception);
        }

        return null;
    }

}
