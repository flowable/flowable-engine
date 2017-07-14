package org.flowable.examples.bpmn.tasklistener;

import org.flowable.engine.delegate.DelegateTask;
import org.flowable.engine.delegate.TaskListener;

import java.util.LinkedList;
import java.util.List;

public class VariablesListener implements TaskListener {

    public static List<String> messages = new LinkedList<>();

    public void notify(DelegateTask delegateTask) {
        messages.add(String.format("%s: %s", delegateTask.getEventName(), delegateTask.getVariables()));
    }
}
