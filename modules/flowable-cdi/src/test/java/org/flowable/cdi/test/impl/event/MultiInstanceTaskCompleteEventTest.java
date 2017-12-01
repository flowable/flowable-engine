package org.flowable.cdi.test.impl.event;

import static org.junit.Assert.assertEquals;

import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.Test;

public class MultiInstanceTaskCompleteEventTest extends CdiFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/cdi/test/impl/event/MultiInstanceTaskCompleteEventTest.process1.bpmn20.xml.bpmn" })
    public void testReceiveAll() {

        TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
        listenerBean.reset();

        assertEquals(0, listenerBean.getCreateTask1());
        assertEquals(0, listenerBean.getAssignTask1());
        assertEquals(0, listenerBean.getCompleteTask1());

        // start the process
        runtimeService.startProcessInstanceByKey("process1");

        Task task = taskService.createTaskQuery().singleResult();

        taskService.claim(task.getId(), "auser");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        // assert
        assertEquals(2, listenerBean.getCreateTask1());
        assertEquals(1, listenerBean.getAssignTask1());
        assertEquals(2, listenerBean.getCompleteTask1());

    }
}
