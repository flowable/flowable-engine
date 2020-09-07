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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CmmnExternalWorkerTransitionBuilder;
import org.flowable.cmmn.engine.impl.cmd.ExternalWorkerJobCompleteCmd;
import org.flowable.cmmn.engine.impl.cmd.ExternalWorkerJobTerminateCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Filip Hrisafov
 */
public class CmmnExternalWorkerTransitionBuilderImpl implements CmmnExternalWorkerTransitionBuilder {

    protected final CommandExecutor commandExecutor;
    protected final String externalJobId;
    protected final String workerId;

    protected Map<String, Object> variables;

    public CmmnExternalWorkerTransitionBuilderImpl(CommandExecutor commandExecutor, String externalJobId, String workerId) {
        this.commandExecutor = commandExecutor;
        this.externalJobId = externalJobId;
        this.workerId = workerId;
    }

    @Override
    public CmmnExternalWorkerTransitionBuilder variables(Map<String, Object> variables) {
        if (this.variables == null) {
            this.variables = new LinkedHashMap<>();
        }
        this.variables.putAll(variables);
        return this;
    }

    @Override
    public CmmnExternalWorkerTransitionBuilder variable(String name, Object value) {
        if (this.variables == null) {
            this.variables = new LinkedHashMap<>();
        }
        this.variables.put(name, value);
        return this;
    }

    @Override
    public void complete() {
        commandExecutor.execute(new ExternalWorkerJobCompleteCmd(externalJobId, workerId, variables));
    }

    @Override
    public void terminate() {
        commandExecutor.execute(new ExternalWorkerJobTerminateCmd(externalJobId, workerId, variables));
    }
}
