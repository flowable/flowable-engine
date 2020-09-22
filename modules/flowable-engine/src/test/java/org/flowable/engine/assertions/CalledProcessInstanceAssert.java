package org.flowable.engine.assertions;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.runtime.ProcessInstance;

public class CalledProcessInstanceAssert extends ProcessInstanceAssert {

    protected CalledProcessInstanceAssert(ProcessEngine engine, ProcessInstance actual) {
        super(engine, actual, CalledProcessInstanceAssert.class);
    }

    protected static CalledProcessInstanceAssert assertThat(ProcessEngine engine, ProcessInstance actual) {
        return new CalledProcessInstanceAssert(engine, actual);
    }

}
