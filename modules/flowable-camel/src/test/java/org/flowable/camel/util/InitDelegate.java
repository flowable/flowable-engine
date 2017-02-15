package org.flowable.camel.util;

import org.flowable.camel.FlowableProducer;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class InitDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable(FlowableProducer.PROCESS_ID_PROPERTY, execution.getProcessInstanceId());
    }

}
