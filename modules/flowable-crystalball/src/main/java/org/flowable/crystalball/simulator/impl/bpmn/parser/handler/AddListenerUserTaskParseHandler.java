package org.flowable.crystalball.simulator.impl.bpmn.parser.handler;

import org.flowable.bpmn.model.ActivitiListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.handler.UserTaskParseHandler;

/**
 * This class changes UserTaskBehavior for simulation purposes.
 * 
 * @author martin.grofcik
 */
public class AddListenerUserTaskParseHandler extends UserTaskParseHandler {

  private final String eventName;
  private final TaskListener taskListener;

  public AddListenerUserTaskParseHandler(String eventName, TaskListener taskListener) {
    this.eventName = eventName;
    this.taskListener = taskListener;
  }

  protected void executeParse(BpmnParse bpmnParse, UserTask userTask) {
    super.executeParse(bpmnParse, userTask);
    
    ActivitiListener listener = new ActivitiListener();
    listener.setEvent(eventName);
    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_INSTANCE);
    listener.setInstance(taskListener);
    userTask.getTaskListeners().add(listener);
    

  }
  
}
