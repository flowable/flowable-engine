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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.ScopedVariableContainerHelper;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.util.Map;

/**
 * @author Ievgenii Bespal
 */
public class CompleteWithScopedVariablesTaskCmd extends NeedsActiveTaskCmd<Void> {
    private static final long serialVersionUID = 1L;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    private final ScopedVariableContainerHelper scopedVariableContainerHelper;
    protected boolean localScope;

    public CompleteWithScopedVariablesTaskCmd(String taskId, ScopedVariableContainerHelper scopedVariableContainerHelper) {
        super(taskId);
        this.scopedVariableContainerHelper = scopedVariableContainerHelper;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        // Backwards compatibility
        this.localScope = scopedVariableContainerHelper.hasVariablesLocal();

        if (scopedVariableContainerHelper.hasAnyVariables()) {
            this.variables = scopedVariableContainerHelper.getAllVariables();
        }

        if (scopedVariableContainerHelper.hasAnyTransientVariables()) {
            this.transientVariables = scopedVariableContainerHelper.getAllTransientVariables();
        }

        if (task.getProcessDefinitionId() != null) {
            if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();

                if (transientVariables == null) {
                    compatibilityHandler.completeTask(task, variables, localScope);
                } else {
                    compatibilityHandler.completeTask(task, variables, transientVariables);
                }
                return null;
            }
        }

        TaskHelper.completeTaskWithScopedVariables(task, this.scopedVariableContainerHelper, commandContext);
        return null;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot complete a suspended task";
    }
}
