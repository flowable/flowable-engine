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
import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christopher Welsch
 */
public class BulkMoveDeadLetterJobToHistoryJobCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkMoveDeadLetterJobToHistoryJobCmd.class);

    protected JobServiceConfiguration jobServiceConfiguration;

    protected List<String> deadLetterJobIds;
    protected int retries;

    public BulkMoveDeadLetterJobToHistoryJobCmd(List<String> deadLetterJobIds, int retries, JobServiceConfiguration jobServiceConfiguration) {
        this.deadLetterJobIds = deadLetterJobIds;
        this.retries = retries;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (deadLetterJobIds == null) {
            throw new FlowableIllegalArgumentException("deadLetterJobIds are null");
        }
        DeadLetterJobQueryImpl query = new DeadLetterJobQueryImpl(commandContext, jobServiceConfiguration);
        query.jobIds(deadLetterJobIds);
        List<Job> deadLetterJobs = jobServiceConfiguration.getDeadLetterJobEntityManager().findJobsByQueryCriteria(query);
        for (Job job : deadLetterJobs) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Moving deadletter job to history job table {}", job.getId());
            }
            jobServiceConfiguration.getJobManager().moveDeadLetterJobToHistoryJob((DeadLetterJobEntity) job, retries);
        }
        return null;
    }

}
