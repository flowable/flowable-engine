package org.flowable.camel.util;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * ServiceTask to start with.
 * 
 * @author stefan.schulze@accelsis.biz
 * 
 */
public class DummyJavaDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        // dummy
    }

}
