package org.flowable.spring.test.jobexecutor;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

/**
 * @author Pablo Ganga
 */
public class ForcedRollbackExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution delegateExecution) {
        throw new RuntimeException("Forcing transaction rollback");
    }

}
