package org.flowable.engine.test.bpmn.event.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableRule;
import org.junit.Rule;
import org.junit.Test;


/**
 * Testcase for error in method 'propagateError' of class
 * 'org.flowable.engine.impl.bpmn.helper.ErrorPropagation'.
 * 
 * @author Fritsche
 */
public class ErrorPropagationTest {

    @Rule
    public FlowableRule flowableRule = new FlowableRule();

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/catchError3.bpmn",
                    "org/flowable/engine/test/bpmn/event/error/catchError4.bpmn", 
                    "org/flowable/engine/test/bpmn/event/error/throwError.bpmn" })
    public void test() {
        ProcessInstance processInstance = flowableRule.getRuntimeService().startProcessInstanceByKey("catchError4");
        assertNotNull(processInstance);

        final org.flowable.task.api.Task task = flowableRule.getTaskService().createTaskQuery().singleResult();

        assertEquals("MyErrorTaskNested", task.getName());
    }
}