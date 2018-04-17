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
package org.flowable.engine.impl.agenda;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * Destroys a scope (for example a subprocess): this means that all child executions, tasks, jobs, variables, etc within that scope are deleted.
 * 
 * The typical example is an interrupting boundary event that is on the boundary of a subprocess and is triggered. At that point, everything within the subprocess would need to be destroyed.
 * 
 * @author Joram Barrez
 */
public class DestroyScopeOperation extends AbstractOperation {

    public DestroyScopeOperation(CommandContext commandContext, ExecutionEntity execution) {
        super(commandContext, execution);
    }

    @Override
    public void run() {

        // Find the actual scope that needs to be destroyed.
        // This could be the incoming execution, or the first parent execution where isScope = true

        // Find parent scope execution
        ExecutionEntity scopeExecution = execution.isScope() ? execution : findFirstParentScopeExecution(execution);

        if (scopeExecution == null) {
            throw new FlowableException("Programmatic error: no parent scope execution found for boundary event");
        }

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        // Delete all child executions
        executionEntityManager.deleteChildExecutions(scopeExecution, execution.getDeleteReason(), true);
        executionEntityManager.deleteExecutionAndRelatedData(scopeExecution, execution.getDeleteReason(), true, null);

        if (scopeExecution.isActive()) {
            CommandContextUtil.getHistoryManager(commandContext).recordActivityEnd(scopeExecution, scopeExecution.getDeleteReason());
        }
        executionEntityManager.delete(scopeExecution);
    }

}
