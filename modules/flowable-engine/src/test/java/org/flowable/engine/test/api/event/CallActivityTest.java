package org.flowable.engine.test.api.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class CallActivityTest extends PluggableFlowableTestCase {

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
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivity.bpmn20.xml" })
    public void testCallActivityCalledHasNoneEndEvent() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivity");
        assertNotNull(processInstance);

        // no task should be active in parent process
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(task);

        // only active task should be the one defined in the external subprocess
        task = taskService.createTaskQuery().active().singleResult();
        assertNotNull(task);
        assertEquals("User Task2 in External", task.getName());

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertNotNull(subprocessInstance);

        assertEquals("Default name", runtimeService.getVariable(processInstance.getId(), "Name"));
        assertEquals("Default name", runtimeService.getVariable(subprocessInstance.getId(), "FullName"));

        // set the variable in the subprocess to validate that the new value is returned from callActivity
        runtimeService.setVariable(subprocessInstance.getId(), "FullName", "Mary Smith");
        assertEquals("Default name", runtimeService.getVariable(processInstance.getId(), "Name"));
        assertEquals("Mary Smith", runtimeService.getVariable(subprocessInstance.getId(), "FullName"));

        // complete user task so that external subprocess will flow to terminate end
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("User Task1", task.getName());

        // validate that the variable was copied back when Call Activity finished
        assertEquals("Mary Smith", runtimeService.getVariable(processInstance.getId(), "Name"));

        // complete user task so that parent process will terminate normally
        taskService.complete(task.getId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();

        // this is the root process so parent null
        assertNull(executionEntity.getParentId());
        String processExecutionId = executionEntity.getId();

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertNotNull(executionEntity.getParentId());
        assertEquals(processExecutionId, executionEntity.getParentId());

        FlowableEvent flowableEvent = mylistener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(3);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(4);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(5);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("callActivity1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(6);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertNull(executionEntity.getParentId());
        assertEquals(executionEntity.getId(), executionEntity.getProcessInstanceId());

        // user task within the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(7);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals("calledtask1", executionEntity.getActivityId());

        // external subprocess
        flowableEvent = mylistener.getEventsReceived().get(8);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());

        // start event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(9);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(10);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());
        assertEquals("startevent2", activityEvent.getActivityId());

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(11);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(12);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task2 in External", taskEntity.getName());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(13);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task2 in External", taskEntity.getName());

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(14);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        // None event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(15);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("noneevent2", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(16);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("noneevent2", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        // the external subprocess
        entityEvent = (FlowableEntityEvent)mylistener.getEventsReceived().get(17);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED, entityEvent.getType());

        // callActivity
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(18);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("callActivity", activityEvent.getActivityType());

        // user task within parent process
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(19);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(20);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1", taskEntity.getName());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(21);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(22);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(23);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("noneevent1", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(24);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("noneevent1", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        // the parent process
        entityEvent = (FlowableEntityEvent)mylistener.getEventsReceived().get(25);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED, entityEvent.getType());

        assertEquals(26, mylistener.getEventsReceived().size());
    }
    
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivity.bpmn20.xml" })
    public void testDeleteParentWhenCallActivityCalledHasNoneEndEvent() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivity");
        assertNotNull(processInstance);

        // no task should be active in parent process
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(task);

        // only active task should be the one defined in the external subprocess
        task = taskService.createTaskQuery().active().singleResult();
        assertNotNull(task);
        assertEquals("User Task2 in External", task.getName());

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertNotNull(subprocessInstance);

        assertEquals("Default name", runtimeService.getVariable(processInstance.getId(), "Name"));
        assertEquals("Default name", runtimeService.getVariable(subprocessInstance.getId(), "FullName"));
        
        runtimeService.deleteProcessInstance(processInstance.getId(), null);  

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();

        // this is the root process so parent null
        assertNull(executionEntity.getParentId());
        String processExecutionId = executionEntity.getId();

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertNotNull(executionEntity.getParentId());
        assertEquals(processExecutionId, executionEntity.getParentId());

        FlowableEvent flowableEvent = mylistener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(3);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(4);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(5);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("callActivity1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(6);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertNull(executionEntity.getParentId());
        assertEquals(executionEntity.getId(), executionEntity.getProcessInstanceId());

        // user task within the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(7);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals("calledtask1", executionEntity.getActivityId());

        // external subprocess
        flowableEvent = mylistener.getEventsReceived().get(8);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());

        // start event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(9);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(10);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());
        assertEquals("startevent2", activityEvent.getActivityId());

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(11);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(12);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task2 in External", taskEntity.getName());

        // user task within external subprocess cancelled
        FlowableActivityCancelledEvent activityCancelledEvent = (FlowableActivityCancelledEvent) mylistener.getEventsReceived().get(13);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityCancelledEvent.getType());       
        assertEquals("User Task2 in External", activityCancelledEvent.getActivityName());
        assertEquals("userTask", activityCancelledEvent.getActivityType());
   
        // external subprocess cancelled
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) mylistener.getEventsReceived().get(14);
        assertEquals(FlowableEngineEventType.PROCESS_CANCELLED, processCancelledEvent.getType());       
        assertEquals(subprocessInstance.getId(), processCancelledEvent.getProcessInstanceId());
        
        // expecting cancelled event for Call Activity
        activityCancelledEvent = (FlowableActivityCancelledEvent) mylistener.getEventsReceived().get(15);
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, activityCancelledEvent.getType());       
        assertEquals("callActivity", activityCancelledEvent.getActivityType());
        
        // parent process cancelled
        processCancelledEvent = (FlowableCancelledEvent) mylistener.getEventsReceived().get(16);
        assertEquals(FlowableEngineEventType.PROCESS_CANCELLED, processCancelledEvent.getType());       
        assertEquals(processInstance.getId(), processCancelledEvent.getProcessInstanceId());

        assertEquals(17, mylistener.getEventsReceived().size());
    }

    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityTerminateEnd.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivityTerminateEnd.bpmn20.xml" })
    public void  testCallActivityCalledHasTerminateEndEvent() throws Exception {

        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivityTerminateEnd");
        assertNotNull(processInstance);

        // no task should be active in parent process
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(task);

        // only active task should be the one defined in the external subprocess
        task = taskService.createTaskQuery().active().singleResult();
        assertNotNull(task);
        assertEquals("User Task2 in External with Terminate End Event", task.getName());

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertNotNull(subprocessInstance);

        assertEquals("Default name", runtimeService.getVariable(processInstance.getId(), "Name"));
        assertEquals("Default name", runtimeService.getVariable(subprocessInstance.getId(), "FullName"));

        // set the variable in the subprocess to validate that the new value is returned from callActivity
        runtimeService.setVariable(subprocessInstance.getId(), "FullName", "Mary Smith");
        assertEquals("Default name", runtimeService.getVariable(processInstance.getId(), "Name"));
        assertEquals("Mary Smith", runtimeService.getVariable(subprocessInstance.getId(), "FullName"));

        // complete user task so that external subprocess will flow to terminate end
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("User Task1 in Parent", task.getName());

        // validate that the variable was copied back when Call Activity finished
        assertEquals("Mary Smith", runtimeService.getVariable(processInstance.getId(), "Name"));

        // complete user task so that parent process will terminate normally
        taskService.complete(task.getId());

        FlowableEntityEvent entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        ExecutionEntity executionEntity = (ExecutionEntity) entityEvent.getEntity();

        // this is the root process so parent null
        assertNull(executionEntity.getParentId());
        String processExecutionId = executionEntity.getId();

        int idx=1;
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertNotNull(executionEntity.getParentId());
        assertEquals(processExecutionId, executionEntity.getParentId());

        FlowableEvent flowableEvent = mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("callActivityId1", activityEvent.getActivityId());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertNull(executionEntity.getParentId());
        assertEquals(executionEntity.getId(), executionEntity.getProcessInstanceId());

        // user task within the external subprocess
        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, entityEvent.getType());
        executionEntity = (ExecutionEntity) entityEvent.getEntity();
        assertEquals("calledtask1", executionEntity.getActivityId());

        // external subprocess
        flowableEvent = mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_STARTED, flowableEvent.getType());
        executionEntity = (ExecutionEntity) ((FlowableProcessStartedEvent) flowableEvent).getEntity();
        assertEquals(subprocessInstance.getId(), executionEntity.getParentId());

        // start event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());
        assertEquals("startevent2", activityEvent.getActivityId());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("startEvent", activityEvent.getActivityType());
        assertEquals("startevent2", activityEvent.getActivityId());

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task2 in External with Terminate End Event", taskEntity.getName());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task2 in External with Terminate End Event", taskEntity.getName());

        // user task within external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("calledtask1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        // None event in external subprocess
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("terminateEnd2", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        // PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT instead of PROCESS_COMPLETED
        // because external subprocess defined with terminate end event
        entityEvent = (FlowableEntityEvent)mylistener.getEventsReceived().get(idx++);
        assertEquals(subprocessInstance.getId(), ((FlowableEngineEntityEvent)entityEvent).getExecutionId());
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, entityEvent.getType());

        //the external subprocess (callActivity)
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("callActivity", activityEvent.getActivityType());

        // user task within parent process
        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_CREATED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1 in Parent", taskEntity.getName());

        entityEvent = (FlowableEntityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.TASK_COMPLETED, entityEvent.getType());
        taskEntity = (TaskEntity) entityEvent.getEntity();
        assertEquals("User Task1 in Parent", taskEntity.getName());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("task1", activityEvent.getActivityId());
        assertEquals("userTask", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, activityEvent.getType());
        assertEquals("noneevent1", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        activityEvent = (FlowableActivityEvent) mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.ACTIVITY_COMPLETED, activityEvent.getType());
        assertEquals("noneevent1", activityEvent.getActivityId());
        assertEquals("endEvent", activityEvent.getActivityType());

        // the parent process
        entityEvent = (FlowableEntityEvent)mylistener.getEventsReceived().get(idx++);
        assertEquals(FlowableEngineEventType.PROCESS_COMPLETED, entityEvent.getType());

        assertEquals(idx, mylistener.getEventsReceived().size());
    }
    
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivity.bpmn20.xml" })
    public void testDeleteParentProcessWithCallActivity() throws Exception {
        
        CallActivityEventListener mylistener = new CallActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(mylistener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivity");
        assertNotNull(processInstance);

        // no task should be active in parent process
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(task);

        // only active task should be the one defined in the external subprocess
        task = taskService.createTaskQuery().active().singleResult();
        assertNotNull(task);
        assertEquals("User Task2 in External", task.getName());

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertNotNull(subprocessInstance);
        
        runtimeService.deleteProcessInstance(processInstance.getId(), null);
        
        List<FlowableEvent> entityEvents = mylistener.getEventsReceived();
        int lastIndex = entityEvents.size() - 1;
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, entityEvents.get(lastIndex - 3).getType());
        assertEquals(FlowableEngineEventType.PROCESS_CANCELLED, entityEvents.get(lastIndex - 2).getType());
        FlowableCancelledEvent subProcessCancelledEvent = (FlowableCancelledEvent) entityEvents.get(lastIndex - 2);
        assertEquals(subprocessInstance.getId(), subProcessCancelledEvent.getProcessInstanceId());
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, entityEvents.get(lastIndex - 1).getType());
        assertEquals(FlowableEngineEventType.PROCESS_CANCELLED, entityEvents.get(lastIndex).getType());
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) entityEvents.get(lastIndex);
        assertEquals(processInstance.getId(), processCancelledEvent.getProcessInstanceId());
        
        System.out.println("the end");
    }
    
    @Deployment(resources = {
            "org/flowable/engine/test/api/event/CallActivityTest.testCallActivityProcessInstanceName.bpmn20.xml",
            "org/flowable/engine/test/api/event/CallActivityTest.testCalledActivity.bpmn20.xml"
    })
    public void testCallActivityProcessInstanceName() {
        runtimeService.startProcessInstanceByKey("testCallActivityProcessInstanceName", 
                CollectionUtil.singletonMap("theCollection", Arrays.asList("A", "B", "C", "D")));
        
        List<ProcessInstance> childProcessInstances = runtimeService.createProcessInstanceQuery().list().stream()
            .filter(processInstance -> (processInstance.getSuperExecutionId() != null))
            .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
            .collect(Collectors.toList());
        
        assertEquals(4, childProcessInstances.size());
        assertEquals("Process instance A", childProcessInstances.get(0).getName());
        assertEquals("Process instance B", childProcessInstances.get(1).getName());
        assertEquals("Process instance C", childProcessInstances.get(2).getName());
        assertEquals("Process instance D", childProcessInstances.get(3).getName());
    }

    class CallActivityEventListener extends AbstractFlowableEngineEventListener {

        private List<FlowableEvent> eventsReceived;

        public CallActivityEventListener() {
            eventsReceived = new ArrayList<>();

        }

        public List<FlowableEvent> getEventsReceived() {
            return eventsReceived;
        }

        public void clearEventsReceived() {
            eventsReceived.clear();
        }

        @Override
        public void onEvent(FlowableEvent event) {
            FlowableEngineEventType engineEventType = (FlowableEngineEventType) event.getType();
            switch (engineEventType) {
            case ENTITY_CREATED:
                FlowableEntityEvent entityEvent = (FlowableEntityEvent) event;
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
            case PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT:
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
