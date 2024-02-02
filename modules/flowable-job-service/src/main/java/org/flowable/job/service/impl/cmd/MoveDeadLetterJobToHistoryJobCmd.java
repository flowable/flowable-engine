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
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.JobNotFoundException;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class MoveDeadLetterJobToHistoryJobCmd implements Command<HistoryJob>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveDeadLetterJobToHistoryJobCmd.class);

    protected String deadletterJobId;
    protected int retries;
    protected JobServiceConfiguration jobServiceConfiguration;

    public MoveDeadLetterJobToHistoryJobCmd(String deadletterJobId, int retries, JobServiceConfiguration jobServiceConfiguration) {
        this.deadletterJobId = deadletterJobId;
        this.retries = retries;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public HistoryJob execute(CommandContext commandContext) {

        if (deadletterJobId == null) {
            throw new FlowableIllegalArgumentException("deadletterJobId is null");
        }

        DeadLetterJobEntity deadLetterJobEntity = jobServiceConfiguration.getDeadLetterJobEntityManager().findById(deadletterJobId);
        if (deadLetterJobEntity == null) {
            throw new JobNotFoundException(deadletterJobId);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Moving deadletter job to history job table {}", deadLetterJobEntity.getId());
        }

        return jobServiceConfiguration.getJobManager().moveDeadLetterJobToHistoryJob(deadLetterJobEntity, retries);
    }

    public String getDeadletterJobId() {
        return deadletterJobId;
    }

}
