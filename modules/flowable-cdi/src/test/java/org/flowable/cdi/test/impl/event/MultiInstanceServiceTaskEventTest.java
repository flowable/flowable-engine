package org.flowable.cdi.test.impl.event;

import static org.junit.Assert.assertEquals;

import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.Test;

public class MultiInstanceServiceTaskEventTest extends CdiFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/cdi/test/impl/event/MultiInstanceServiceTaskEvent.bpmn20.xml" })
    public void testReceiveAll() {

        TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
        listenerBean.reset();

        assertEquals(0, listenerBean.getStartActivityService1WithLoopCounter());
        assertEquals(0, listenerBean.getEndActivityService1WithLoopCounter());
        assertEquals(0, listenerBean.getEndActivityService1WithoutLoopCounter());

        assertEquals(0, listenerBean.getStartActivityService2WithLoopCounter());
        assertEquals(0, listenerBean.getEndActivityService2WithLoopCounter());
        assertEquals(0, listenerBean.getEndActivityService2WithoutLoopCounter());

        // start the process
        runtimeService.startProcessInstanceByKey("process1");

        // assert
        assertEquals(1, listenerBean.getTakeTransitiont1());
        assertEquals(1, listenerBean.getTakeTransitiont2());
        assertEquals(1, listenerBean.getTakeTransitiont3());

        assertEquals(2, listenerBean.getStartActivityService1WithLoopCounter());
        assertEquals(3, listenerBean.getStartActivityService2WithLoopCounter());

        assertEquals(2, listenerBean.getEndActivityService1WithLoopCounter());
        assertEquals(1, listenerBean.getEndActivityService1WithoutLoopCounter());
        assertEquals(3, listenerBean.getEndActivityService2WithLoopCounter());
        assertEquals(1, listenerBean.getEndActivityService2WithoutLoopCounter());

    }
}
