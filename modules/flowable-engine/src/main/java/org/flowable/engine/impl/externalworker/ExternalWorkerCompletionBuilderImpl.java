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
package org.flowable.engine.impl.externalworker;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cmd.ExternalWorkerJobBpmnErrorCmd;
import org.flowable.engine.impl.cmd.ExternalWorkerJobCompleteCmd;
import org.flowable.engine.impl.cmd.ExternalWorkerJobFailCmd;
import org.flowable.engine.runtime.ExternalWorkerCompletionBuilder;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerCompletionBuilderImpl implements ExternalWorkerCompletionBuilder {

    protected final CommandExecutor commandExecutor;
    protected final String externalJobId;
    protected final String workerId;

    protected Map<String, Object> variables;
    protected String errorMessage;
    protected String errorDetails;

    public ExternalWorkerCompletionBuilderImpl(CommandExecutor commandExecutor, String externalJobId, String workerId) {
        this.commandExecutor = commandExecutor;
        this.externalJobId = externalJobId;
        this.workerId = workerId;
    }

    @Override
    public ExternalWorkerCompletionBuilder variables(Map<String, Object> variables) {
        if (this.variables == null) {
            this.variables = new LinkedHashMap<>();
        }
        this.variables.putAll(variables);
        return this;
    }

    @Override
    public ExternalWorkerCompletionBuilder variable(String name, Object value) {
        if (this.variables == null) {
            this.variables = new LinkedHashMap<>();
        }
        this.variables.put(name, value);
        return this;
    }

    @Override
    public ExternalWorkerCompletionBuilder errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    @Override
    public ExternalWorkerCompletionBuilder errorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
        return this;
    }

    @Override
    public void complete() {
        commandExecutor.execute(new ExternalWorkerJobCompleteCmd(externalJobId, workerId, variables));
    }

    @Override
    public void failure(int retries, Duration retryTimeout) {
        commandExecutor.execute(new ExternalWorkerJobFailCmd(externalJobId, workerId, retries, retryTimeout, errorMessage, errorDetails));
    }

    @Override
    public void bpmnError(String errorCode) {
        commandExecutor.execute(new ExternalWorkerJobBpmnErrorCmd(externalJobId, workerId, variables, errorCode));
    }
}
