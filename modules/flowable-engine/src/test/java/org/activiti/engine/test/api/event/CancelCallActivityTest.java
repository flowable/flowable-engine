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
package org.activiti.engine.test.api.event;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.event.ActivitiActivityCancelledEvent;
import org.activiti.engine.delegate.event.ActivitiActivityEvent;
import org.activiti.engine.delegate.event.ActivitiCancelledEvent;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiProcessCancelledEventImpl;
import org.activiti.engine.event.EventLogEntry;
import org.activiti.engine.impl.event.logger.EventLogger;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;

public class CancelCallActivityTest extends PluggableActivitiTestCase {

  private CallActivityEventListener listener;

  protected EventLogger databaseEventLogger;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Database event logger setup
    databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(),
        processEngineConfiguration.getObjectMapper());
    runtimeService.addEventListener(databaseEventLogger);
  }

  @Override
  protected void tearDown() throws Exception {

    if (listener != null) {
      listener.clearEventsReceived();
      processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
    }

    // Remove entries
    for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
      managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
    }

    // Database event logger teardown
    runtimeService.removeEventListener(databaseEventLogger);

    super.tearDown();
  }

  @Override
  protected void initializeServices() {
    super.initializeServices();

    listener = new CallActivityEventListener();
    processEngineConfiguration.getEventDispatcher().addEventListener(listener);
  }

  @Deployment(resources = {
      "org/activiti/engine/test/api/event/CancelCallActivityTest.testActivityMessageBoundaryEventsOnCallActivity.bpmn20.xml",
      "org/activiti/engine/test/api/event/CancelCallActivityTest.testActivityMessageBoundaryEventsCalledActivity.bpmn20.xml" })
  public void testCancelCallActivity() throws Exception {

    CallActivityEventListener mylistener = new CallActivityEventListener();
    processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageOnCallActivity");
    assertNotNull(processInstance);

    Execution executionWithMessage = runtimeService.createExecutionQuery().activityId("cancelBoundaryEvent")
        .singleResult();
    assertNotNull(executionWithMessage);

    runtimeService.messageEventReceived("cancel", executionWithMessage.getId());

    ActivitiEntityEvent entityEvent = (ActivitiEntityEvent) mylistener.getEventsReceived().get(0);
    assertEquals(ActivitiEventType.ENTITY_CREATED, entityEvent.getType());
    ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();
    // this is process so parent null
    assertNull(executionEntity.getParentId());
    String processExecutionId = executionEntity.getId();

    // this is callActivity
    entityEvent = (ActivitiEntityEvent) mylistener.getEventsReceived().get(1);
    assertEquals(ActivitiEventType.ENTITY_CREATED, entityEvent.getType());
    executionEntity = (ExecutionEntity) entityEvent.getEntity();
    assertNotNull(executionEntity.getParentId());
    assertEquals(processExecutionId, executionEntity.getParentId());

    ActivitiEvent activitiEvent = (ActivitiEvent) mylistener.getEventsReceived().get(2);
    assertEquals(ActivitiEventType.PROCESS_STARTED, activitiEvent.getType());

    
    ActivitiActivityEvent activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(3);
    assertEquals(ActivitiEventType.ACTIVITY_STARTED, activityEvent.getType());
    assertEquals("startEvent", activityEvent.getActivityType());
    
    activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(4);
    assertEquals(ActivitiEventType.ACTIVITY_COMPLETED, activityEvent.getType());
    assertEquals("startEvent", activityEvent.getActivityType());
    
    
    entityEvent = (ActivitiEntityEvent) mylistener.getEventsReceived().get(5);
    assertEquals(ActivitiEventType.ENTITY_CREATED, entityEvent.getType());
    executionEntity = (ExecutionEntity) entityEvent.getEntity();
    assertEquals("cancelBoundaryEvent", executionEntity.getActivityId());
    String boundaryExecutionId = executionEntity.getId();
        
    activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(6);
    assertEquals(ActivitiEventType.ACTIVITY_STARTED, activityEvent.getType());
    assertEquals("callActivity1", activityEvent.getActivityId());
    
    // this is external subprocess. Workflow uses the ENTITY_CREATED event to determine when to send our event.
    entityEvent = (ActivitiEntityEvent) mylistener.getEventsReceived().get(7);
    assertEquals(ActivitiEventType.ENTITY_CREATED, entityEvent.getType());
    executionEntity = (ExecutionEntity) entityEvent.getEntity();
    assertNull(executionEntity.getParentId());
    assertEquals(executionEntity.getId(), executionEntity.getProcessInstanceId());
    String externalExecutionId = executionEntity.getId();
    
    
    // this is the task within the external subprocess
    entityEvent = (ActivitiEntityEvent) mylistener.getEventsReceived().get(8);
    assertEquals(ActivitiEventType.ENTITY_CREATED, entityEvent.getType());
    executionEntity = (ExecutionEntity) entityEvent.getEntity();
    assertEquals("calledtask1", executionEntity.getActivityId());

    
    // start event in external subprocess
    activitiEvent = (ActivitiEvent) mylistener.getEventsReceived().get(9);
    assertEquals(ActivitiEventType.PROCESS_STARTED, activitiEvent.getType());   
    
    
    activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(10);
    assertEquals(ActivitiEventType.ACTIVITY_STARTED, activityEvent.getType());
    assertEquals("startEvent", activityEvent.getActivityType());
    assertEquals("startevent2", activityEvent.getActivityId());
    
    // start event in external subprocess
    activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(11);
    assertEquals(ActivitiEventType.ACTIVITY_COMPLETED, activityEvent.getType());
    assertEquals("startEvent", activityEvent.getActivityType());
    assertEquals("startevent2", activityEvent.getActivityId());
    
    // this is user task within external subprocess
    activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(12);
    assertEquals(ActivitiEventType.ACTIVITY_STARTED, activityEvent.getType());
    assertEquals("calledtask1", activityEvent.getActivityId());
    assertEquals("userTask", activityEvent.getActivityType());
    
    
    entityEvent = (ActivitiEntityEvent) mylistener.getEventsReceived().get(13);
    assertEquals(ActivitiEventType.TASK_CREATED, entityEvent.getType());
    TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
    assertEquals("User Task2 in External", taskEntity.getName());
    
    // activityId is the call activity and the execution is the boundary event as we have seen before
    // We get this event in workflow but we ignore the activityType of "callActivity"
    activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(14);
    assertEquals(ActivitiEventType.ACTIVITY_CANCELLED, activityEvent.getType());
    assertEquals("callActivity", activityEvent.getActivityType());
    assertEquals(boundaryExecutionId, activityEvent.getExecutionId());
    

    // This is the problem. We get a PROCESS_COMPLETED event for the external subprocess 
    // here instead of a PROCESS_CANCELLED event. We also don't receive a ACTIVITY_CANCELLED
    // event for the user task in the external subprocess.
    
//    entityEvent = (ActivitiEntityEvent) mylistener.getEventsReceived().get(15);
//    assertEquals(ActivitiEventType.PROCESS_CANCELLED, entityEvent.getType());
//    executionEntity = (ExecutionEntity) entityEvent.getEntity();
//    assertNull(executionEntity.getParentId());
//    assertNotSame(executionEntity.getProcessInstanceId(), executionEntity.getRootProcessInstanceId()); 
//    assertEquals(externalExecutionId, executionEntity.getId());
        
    // We should get this.
    ActivitiActivityCancelledEvent taskCancelledEvent = (ActivitiActivityCancelledEvent) mylistener.getEventsReceived().get(15);
    assertEquals(ActivitiEventType.ACTIVITY_CANCELLED, taskCancelledEvent.getType());
    assertEquals(taskEntity.getName(), taskCancelledEvent.getActivityName()); 
    
    ActivitiCancelledEvent processCancelledEvent = (ActivitiCancelledEvent) mylistener.getEventsReceived().get(16);
    assertEquals(ActivitiEventType.PROCESS_CANCELLED, processCancelledEvent.getType());
    assertEquals(processCancelledEvent.getProcessInstanceId(), processCancelledEvent.getExecutionId());
    
    activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(17);
    assertEquals(ActivitiEventType.ACTIVITY_COMPLETED, activityEvent.getType());
    assertEquals("boundaryEvent", activityEvent.getActivityType());
    assertEquals("cancelBoundaryEvent", activityEvent.getActivityId());
    
    // task in the main definition
    activityEvent = (ActivitiActivityEvent) mylistener.getEventsReceived().get(18);
    assertEquals(ActivitiEventType.ACTIVITY_STARTED, activityEvent.getType());
    assertEquals("task1", activityEvent.getActivityId());
    assertEquals("userTask", activityEvent.getActivityType());
    
    entityEvent = (ActivitiEntityEvent) mylistener.getEventsReceived().get(19);
    assertEquals(ActivitiEventType.TASK_CREATED, entityEvent.getType());
     taskEntity = (TaskEntity) entityEvent.getEntity();
    assertEquals("User Task1", taskEntity.getName());
    
    assertEquals(20, mylistener.getEventsReceived().size());
  }

  class CallActivityEventListener implements ActivitiEventListener {

    private List<ActivitiEvent> eventsReceived;

    public CallActivityEventListener() {
      eventsReceived = new ArrayList<ActivitiEvent>();

    }

    public List<ActivitiEvent> getEventsReceived() {
      return eventsReceived;
    }

    public void clearEventsReceived() {
      eventsReceived.clear();
    }

    @Override
    public void onEvent(ActivitiEvent event) {
      switch (event.getType()) {
      case ENTITY_CREATED:
        ActivitiEntityEvent entityEvent = (ActivitiEntityEvent) event;
        if (entityEvent.getEntity() instanceof ExecutionEntity) {
          eventsReceived.add(event);
        }
        break;
      case ACTIVITY_STARTED:
      case ACTIVITY_COMPLETED:
      case ACTIVITY_CANCELLED:
      case TASK_CREATED:
      case TASK_COMPLETED:
      case PROCESS_STARTED:
      case PROCESS_COMPLETED:
      case PROCESS_CANCELLED:
        eventsReceived.add(event);
        break;
      default:
        break;

      }
    }

    @Override
    public boolean isFailOnException() {
      return false;
    }
  }

}

