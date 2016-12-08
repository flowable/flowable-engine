package org.activiti5.engine.impl.history.handler;

import org.activiti5.engine.impl.context.Context;
import org.activiti5.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.delegate.DelegateTask;
import org.flowable.engine.delegate.TaskListener;


/**
 * Called when a task is created for a user-task activity. Allows recoring task-id in
 * historic activity.
 * 
 * @author Frederik Heremans
 */
public class UserTaskIdHandler implements TaskListener {

  public void notify(DelegateTask task) {
    Context.getCommandContext().getHistoryManager()
      .recordTaskId((TaskEntity) task);
  }
  
}
