package org.flowable.camel.util;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class FlagJavaDelegate implements JavaDelegate {
    public static boolean flag;

    public static void reset() {
        flag = false;
    }

    @Override
    public void execute(DelegateExecution execution) {
        flag = true;

    }

    public static boolean isFlagSet() {
        return flag;
    }

}
