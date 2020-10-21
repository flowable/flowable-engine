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
package org.flowable.job.service.impl.history.async;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.HistoryJobHandler;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractAsyncHistoryJobHandler implements HistoryJobHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected boolean isAsyncHistoryJsonGroupingEnabled;
    protected String jobType;
    
    public AbstractAsyncHistoryJobHandler(String jobType) {
        this.jobType = jobType;
    }
    
    @Override
    public String getType() {
        return jobType;
    }

    @Override
    public void execute(HistoryJobEntity job, String configuration, CommandContext commandContext,
            JobServiceConfiguration jobServiceConfiguration) {
        ObjectMapper objectMapper = commandContext.getObjectMapper();
        if (job.getAdvancedJobHandlerConfigurationByteArrayRef() != null) {

            JsonNode historyNode;
            try {
                byte[] bytes = getJobBytes(job);
                historyNode = objectMapper.readTree(bytes);
            } catch (Exception e) {
                // The transaction will be rolled back and the job retries decremented,
                // which is different from unacquiring the job where the retries are not changed.
                throw new FlowableException("Could not deserialize async history json for job (id=" + job.getId() + ")", e);
            }

            if (isAsyncHistoryJsonGroupingEnabled() && historyNode.isArray()) {
                List<ObjectNode> failedNodes = null;
                Exception exception = null;
                ArrayNode arrayNode = (ArrayNode) historyNode;
                for (JsonNode jsonNode : arrayNode) {
                    try {
                        processHistoryJson(commandContext, job, jsonNode);

                    } catch (Exception ex) {
                        if (failedNodes == null) {
                            failedNodes = new ArrayList<>();
                        }
                        failedNodes.add((ObjectNode) jsonNode);

                        exception = new FlowableException("Failed to process async history json. See suppressed exceptions.");
                        exception.addSuppressed(ex);
                    }
                }

                if (failedNodes != null && !failedNodes.isEmpty()) {
                    AsyncHistorySession historySession = commandContext.getSession(AsyncHistorySession.class);
                    List<HistoryJobEntity> newHistoryJobs = historySession.getAsyncHistoryListener()
                            .historyDataGenerated(jobServiceConfiguration, failedNodes);

                    StringWriter stringWriter = new StringWriter();
                    exception.printStackTrace(new PrintWriter(stringWriter));
                    String exceptionStacktrace = stringWriter.toString();

                    for (HistoryJobEntity historyJob : newHistoryJobs) {
                        historyJob.setExceptionMessage(exception.getMessage());
                        historyJob.setExceptionStacktrace(exceptionStacktrace);
                        if (job.getRetries() == 0) {
                            // If the job has no more retries then we should create a dead letter job out of it
                            DeadLetterJobEntity deadLetterJob = jobServiceConfiguration.getJobManager().createDeadLetterJobFromHistoryJob(historyJob);
                            jobServiceConfiguration.getDeadLetterJobDataManager().insert(deadLetterJob);
                            jobServiceConfiguration.getHistoryJobEntityManager().deleteNoCascade(historyJob); // no cascade -> the bytearray ref is reused for either the new history job or the deadletter job
                        } else {
                            // The historyJob is a new job with new data
                            // However, we still should decrement the retries
                            historyJob.setRetries(job.getRetries() - 1);
                        }
                    }
                }
            } else {
                try {
                    processHistoryJson(commandContext, job, historyNode);

                } catch (AsyncHistoryJobNotApplicableException e) {
                    throw e;

                } catch (Exception e) {

                    if (!(e instanceof FlowableException) || (e instanceof FlowableException && ((FlowableException) e).isLogged())) {
                        logger.warn("Could not execute history job", e);
                    }

                    // The transaction will be rolled back and the job retries decremented,
                    // which is different from unacquiring the job where the retries are not changed.
                    throw new FlowableException("Failed to process async history json for job (id=" + job.getId() + ")", e);
                }

            }
        }
    }

    protected byte[] getJobBytes(HistoryJobEntity job) {
        return job.getAdvancedJobHandlerConfigurationByteArrayRef().getBytes(job.getScopeType());
    }

    protected abstract void processHistoryJson(CommandContext commandContext, HistoryJobEntity job, JsonNode historyNode);

    public boolean isAsyncHistoryJsonGroupingEnabled() {
        return isAsyncHistoryJsonGroupingEnabled;
    }

    public void setAsyncHistoryJsonGroupingEnabled(boolean isAsyncHistoryJsonGroupingEnabled) {
        this.isAsyncHistoryJsonGroupingEnabled = isAsyncHistoryJsonGroupingEnabled;
    }
    
}
