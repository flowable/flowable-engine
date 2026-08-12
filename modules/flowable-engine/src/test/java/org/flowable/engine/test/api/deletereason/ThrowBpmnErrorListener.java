package org.flowable.engine.test.api.deletereason;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;

/** Raises a BpmnError from a start execution listener. */
public class ThrowBpmnErrorListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        throw new BpmnError("someError", "raised from a start execution listener");
    }

}

/** Never reached; the listener throws before the behaviour runs. */
class NoopDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        // no-op
    }

}
