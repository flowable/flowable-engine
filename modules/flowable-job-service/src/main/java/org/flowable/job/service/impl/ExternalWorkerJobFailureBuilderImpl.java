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
package org.flowable.job.service.impl;

import java.time.Duration;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.api.ExternalWorkerJobFailureBuilder;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.cmd.ExternalWorkerJobFailCmd;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobFailureBuilderImpl implements ExternalWorkerJobFailureBuilder {

    protected final String externalJobId;
    protected final String workerId;
    protected final CommandExecutor commandExecutor;
    protected final JobServiceConfiguration jobServiceConfiguration;

    protected String errorMessage;
    protected String errorDetails;
    protected int retries = -1;
    protected Duration retryTimeout;

    public ExternalWorkerJobFailureBuilderImpl(String externalJobId, String workerId, CommandExecutor commandExecutor,
            JobServiceConfiguration jobServiceConfiguration) {
        
        this.externalJobId = externalJobId;
        this.workerId = workerId;
        this.commandExecutor = commandExecutor;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public ExternalWorkerJobFailureBuilder errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    @Override
    public ExternalWorkerJobFailureBuilder errorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
        return this;
    }

    @Override
    public ExternalWorkerJobFailureBuilder retries(int retries) {
        this.retries = retries;
        return this;
    }

    @Override
    public ExternalWorkerJobFailureBuilder retryTimeout(Duration retryTimeout) {
        this.retryTimeout = retryTimeout;
        return this;
    }

    @Override
    public void fail() {
        commandExecutor.execute(new ExternalWorkerJobFailCmd(externalJobId, workerId, retries, retryTimeout, 
                errorMessage, errorDetails, jobServiceConfiguration));
    }
}
