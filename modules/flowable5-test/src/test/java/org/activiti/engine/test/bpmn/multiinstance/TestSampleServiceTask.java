package org.activiti.engine.test.bpmn.multiinstance;

import org.activiti.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * @author Andreas Karnahl
 */
public class TestSampleServiceTask extends AbstractBpmnActivityBehavior {
    @Override
    public void execute(DelegateExecution execution) {
        ActivityExecution activityExecution = (ActivityExecution) execution;
        System.out.println("###: execution: " + execution.getId() + "; " + execution.getVariable("value") + "; " + getMultiInstanceActivityBehavior());
        leave(activityExecution);
    }
}
