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

import java.util.Collection;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.Flowable5Util;

/**
 * @author roman.smirnov
 * @author Joram Barrez
 */
public class RemoveExecutionVariablesCmd extends NeedsActiveExecutionCmd<Void> {

    private static final long serialVersionUID = 1L;

    private Collection<String> variableNames;
    private boolean isLocal;

    public RemoveExecutionVariablesCmd(String executionId, Collection<String> variableNames, boolean isLocal) {
        super(executionId);
        this.variableNames = variableNames;
        this.isLocal = isLocal;
    }

    @Override
    protected Void execute(CommandContext commandContext, ExecutionEntity execution) {
        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.removeExecutionVariables(executionId, variableNames, isLocal);
            return null;
        }

        if (isLocal) {
            execution.removeVariablesLocal(variableNames);
        } else {
            execution.removeVariables(variableNames);
        }

        return null;
    }

    @Override
    protected String getSuspendedExceptionMessage() {
        return "Cannot remove variables because execution '" + executionId + "' is suspended";
    }

}
