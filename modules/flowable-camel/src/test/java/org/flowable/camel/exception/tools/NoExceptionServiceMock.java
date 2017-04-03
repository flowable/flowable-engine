package org.flowable.camel.exception.tools;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class NoExceptionServiceMock implements JavaDelegate {

    static boolean isCalled;

    @Override
    public void execute(DelegateExecution execution) {
        isCalled = true;
    }

    public static void reset() {
        isCalled = false;
    }

    public static boolean isCalled() {
        return isCalled;
    }

}
