package org.flowable.crystalball.simulator.impl.playback;

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.SimulationEventHandler;
import org.flowable.crystalball.simulator.SimulationRunContext;
import org.flowable.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * complete user task handler for playback purposes
 * 
 * @author martin.grofcik
 */
public class PlaybackUserTaskCompleteEventHandler implements SimulationEventHandler {

  private static Logger log = LoggerFactory.getLogger(PlaybackUserTaskCompleteEventHandler.class);

  @Override
  public void handle(SimulationEvent event) {
    String taskId = (String) event.getProperty("taskId");
    Task task = SimulationRunContext.getTaskService().createTaskQuery().taskId(taskId).singleResult();
    String assignee = task.getAssignee();

    @SuppressWarnings("unchecked")
    Map<String, Object> variables = (Map<String, Object>) event.getProperty("variables");

    SimulationRunContext.getTaskService().complete(taskId, variables);
    log.debug("completed {}, {}, {}, {}", task, task.getName(), assignee, variables);
  }

  @Override
  public void init() {

  }
}
