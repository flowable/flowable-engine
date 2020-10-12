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

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobFailCmd extends AbstractExternalWorkerJobCmd {

    protected int retries;
    protected Duration retryTimeout;
    protected String errorMessage;
    protected String errorDetails;

    public ExternalWorkerJobFailCmd(String externalJobId, String workerId, int retries, Duration retryTimeout, String errorMessage, String errorDetails,
            JobServiceConfiguration jobServiceConfiguration) {
        
        super(externalJobId, workerId, jobServiceConfiguration);
        this.retries = retries;
        this.retryTimeout = retryTimeout;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
    }

    @Override
    protected void runJobLogic(ExternalWorkerJobEntity externalWorkerJob, CommandContext commandContext) {

        externalWorkerJob.setExceptionMessage(errorMessage);
        externalWorkerJob.setExceptionStacktrace(errorDetails);

        int newRetries;

        if (retries >= 0) {
            newRetries = retries;
        } else {
            newRetries = externalWorkerJob.getRetries() - 1;
        }

        if (newRetries > 0) {
            externalWorkerJob.setRetries(newRetries);
            externalWorkerJob.setLockOwner(null);
            if (retryTimeout == null) {
                externalWorkerJob.setLockExpirationTime(null);
            } else {
                Instant lockExpirationTime = jobServiceConfiguration.getClock().getCurrentTime().toInstant().plusMillis(retryTimeout.toMillis());
                externalWorkerJob.setLockExpirationTime(Date.from(lockExpirationTime));
            }
        } else {
            jobServiceConfiguration.getJobService().moveJobToDeadLetterJob(externalWorkerJob);
        }
    }
}
