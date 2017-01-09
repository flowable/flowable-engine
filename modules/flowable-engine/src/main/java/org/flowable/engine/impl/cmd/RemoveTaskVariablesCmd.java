package org.flowable.engine.impl.cmd;

import java.util.Collection;

import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.util.Flowable5Util;

/**
 * @author roman.smirnov
 * @author Joram Barrez
 */
public class RemoveTaskVariablesCmd extends NeedsActiveTaskCmd<Void> {

  private static final long serialVersionUID = 1L;

  private final Collection<String> variableNames;
  private final boolean isLocal;

  public RemoveTaskVariablesCmd(String taskId, Collection<String> variableNames, boolean isLocal) {
    super(taskId);
    this.variableNames = variableNames;
    this.isLocal = isLocal;
  }

  protected Void execute(CommandContext commandContext, TaskEntity task) {

    if (task.getProcessDefinitionId() != null && Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
      Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler(); 
      compatibilityHandler.removeTaskVariables(taskId, variableNames, isLocal);
      return null;
    }
    
    if (isLocal) {
      task.removeVariablesLocal(variableNames);
    } else {
      task.removeVariables(variableNames);
    }

    task.forceUpdate();
    return null;
  }

  @Override
  protected String getSuspendedTaskException() {
    return "Cannot remove variables from a suspended task.";
  }

}
