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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
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

    protected Map<String, Object> variables;
    protected Map<String, Object> variablesLocal;
    protected Map<String, Object> transientVariables;
    protected Map<String, Object> transientVariablesLocal;

    public CompleteTaskCmd(String taskId, Map<String, Object> variables) {
        super(taskId);
        this.variables = variables;
    }

    public CompleteTaskCmd(String taskId, Map<String, Object> variables, boolean localScope) {
        super(taskId);
        if (localScope) {
            this.variablesLocal = variables;
        } else {
            this.variables = variables;
        }
    }

    public CompleteTaskCmd(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        this(taskId, variables);
        this.transientVariables = transientVariables;
    }

    public CompleteTaskCmd(String taskId, Map<String, Object> variables, Map<String, Object> variablesLocal,
            Map<String, Object> transientVariables, Map<String, Object> transientVariablesLocal) {
        
        super(taskId);
        this.variables = variables;
        this.variablesLocal = variablesLocal;
        this.transientVariables = transientVariables;
        this.transientVariablesLocal = transientVariablesLocal;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        if (StringUtils.isNotEmpty(task.getScopeId()) && ScopeTypes.CMMN.equals(task.getScopeType())) {
            throw new FlowableException("The task instance is created by the cmmn engine and should be completed via the cmmn engine API");
        }
        
        // Backwards compatibility
        if (task.getProcessDefinitionId() != null) {
            if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();

                if (transientVariables == null) {
                    if (variablesLocal != null) {
                        compatibilityHandler.completeTask(task, variablesLocal, true);
                    } else {
                        compatibilityHandler.completeTask(task, variables, false);
                    }
                    
                } else {
                    compatibilityHandler.completeTask(task, variables, transientVariables);
                }
                return null;
            }
        }

        TaskHelper.completeTask(task, variables, variablesLocal, transientVariables, transientVariablesLocal, commandContext);
        return null;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot complete a suspended task";
    }
}
