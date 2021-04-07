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
package org.flowable.cmmn.engine.impl.reactivation;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.reactivation.CaseReactivationBuilder;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.cmd.ReactivateHistoricCaseInstanceCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * The case reactivation builder implementation storing reactivation specific information and executing the reactivation command to reactivate a historical
 * case instance.
 *
 * @author Micha Kiener
 */
public class CaseReactivationBuilderImpl implements CaseReactivationBuilder {

    protected final CommandExecutor commandExecutor;
    protected final String caseInstanceId;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;

    public CaseReactivationBuilderImpl(CommandExecutor commandExecutor, String caseInstanceId) {
        this.commandExecutor = commandExecutor;
        this.caseInstanceId = caseInstanceId;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public boolean hasVariables() {
        return variables != null && variables.size() > 0;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public boolean hasTransientVariables() {
        return transientVariables != null && transientVariables.size() > 0;
    }

    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }

    @Override
    public CaseReactivationBuilder variable(String name, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put(name, value);
        return this;
    }

    @Override
    public CaseReactivationBuilder variables(Map<String, Object> variables) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.putAll(variables);
        return this;
    }

    @Override
    public CaseReactivationBuilder transientVariable(String name, Object value) {
        if (transientVariables == null) {
            transientVariables = new HashMap<>();
        }
        transientVariables.put(name, value);
        return this;
    }

    @Override
    public CaseReactivationBuilder transientVariables(Map<String, Object> variables) {
        if (transientVariables == null) {
            transientVariables = new HashMap<>();
        }
        transientVariables.putAll(variables);
        return this;
    }

    @Override
    public CaseInstance reactivate() {
        return commandExecutor.execute(new ReactivateHistoricCaseInstanceCmd(this));
    }
}
