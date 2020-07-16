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

import java.util.Map;

import org.flowable.common.engine.api.variable.VariableCollectionsContainer;
import org.flowable.common.engine.impl.VariableCollectionsContainerImpl;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class CompleteTaskCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;
    protected VariableCollectionsContainer variableCollectionsContainer;
    protected boolean localScope;

    public CompleteTaskCmd(String taskId, Map<String, Object> variables) {
        super(taskId);

        if (this.variableCollectionsContainer == null) {
            this.variableCollectionsContainer = new VariableCollectionsContainerImpl();
        }

        if (variables != null) {
            variableCollectionsContainer.setVariables(variables);
        }
    }

    public CompleteTaskCmd(String taskId, Map<String, Object> variables, boolean localScope) {
        this(taskId, variables);
        this.localScope = localScope;
    }

    public CompleteTaskCmd(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        this(taskId, variables);
        this.variableCollectionsContainer = new VariableCollectionsContainerImpl();
        this.variableCollectionsContainer.setTransientVariables(transientVariables);
    }

    public CompleteTaskCmd(String taskId, VariableCollectionsContainer variableCollectionsContainer) {
        super(taskId);
        this.variableCollectionsContainer = variableCollectionsContainer;
        this.localScope = variableCollectionsContainer.hasLocalVariables();
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        // Backwards compatibility
        if (task.getProcessDefinitionId() != null) {
            if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();

                if (variableCollectionsContainer.hasAnyTransientVariables()) {
                    compatibilityHandler.completeTask(task, variableCollectionsContainer.getAllVariables(), localScope);
                } else {
                    compatibilityHandler.completeTask(task, variableCollectionsContainer.getAllVariables(), variableCollectionsContainer.getAllTransientVariables());
                }
                return null;
            }
        }

        TaskHelper.completeTask(task, this.variableCollectionsContainer, commandContext);
        return null;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot complete a suspended task";
    }
}
