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
package org.activiti.engine.impl.cmd;

import java.util.Map;

import org.activiti.engine.compatibility.Activiti5CompatibilityHandler;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.util.Activiti5Util;
import org.activiti.engine.impl.util.TaskHelper;

/**
 * @author Joram Barrez
 */
public class CompleteTaskCmd extends NeedsActiveTaskCmd<Void> {

  private static final long serialVersionUID = 1L;
  protected Map<String, Object> variables;
  protected Map<String, Object> transientVariables;
  protected boolean localScope;

  public CompleteTaskCmd(String taskId, Map<String, Object> variables) {
    super(taskId);
    this.variables = variables;
  }

  public CompleteTaskCmd(String taskId, Map<String, Object> variables, boolean localScope) {
    this(taskId, variables);
    this.localScope = localScope;
  }
  
  public CompleteTaskCmd(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
    this(taskId, variables);
    this.transientVariables = transientVariables;
  }

  protected Void execute(CommandContext commandContext, TaskEntity task) {
    // Backwards compatibility
    if (task.getProcessDefinitionId() != null) {
      if (Activiti5Util.isActiviti5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
        Activiti5CompatibilityHandler activiti5CompatibilityHandler = Activiti5Util.getActiviti5CompatibilityHandler(); 
        
        if (transientVariables == null) {
            activiti5CompatibilityHandler.completeTask(task, variables, localScope);
        } else {
            activiti5CompatibilityHandler.completeTask(task, variables, transientVariables);
        }
        return null;
      }
    }
    
    TaskHelper.completeTask(task, variables, transientVariables, localScope, commandContext);
    return null;
  }

  @Override
  protected String getSuspendedTaskException() {
    return "Cannot complete a suspended task";
  }

}
