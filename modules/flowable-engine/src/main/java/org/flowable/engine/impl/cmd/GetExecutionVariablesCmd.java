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
import java.util.Collection;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.runtime.Execution;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class GetExecutionVariablesCmd implements Command<Map<String, Object>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String executionId;
    protected Collection<String> variableNames;
    protected boolean isLocal;

    public GetExecutionVariablesCmd(String executionId, Collection<String> variableNames, boolean isLocal) {
        this.executionId = executionId;
        this.variableNames = variableNames;
        this.isLocal = isLocal;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {

        // Verify existence of execution
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("executionId is null");
        }

        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);

        if (execution == null) {
            throw new FlowableObjectNotFoundException("execution " + executionId + " doesn't exist", Execution.class);
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            return compatibilityHandler.getExecutionVariables(executionId, variableNames, isLocal);
        }

        if (variableNames == null || variableNames.isEmpty()) {

            // Fetch all

            if (isLocal) {
                return execution.getVariablesLocal();
            } else {
                return execution.getVariables();
            }

        } else {

            // Fetch specific collection of variables
            if (isLocal) {
                return execution.getVariablesLocal(variableNames, false);
            } else {
                return execution.getVariables(variableNames, false);
            }

        }

    }
}
