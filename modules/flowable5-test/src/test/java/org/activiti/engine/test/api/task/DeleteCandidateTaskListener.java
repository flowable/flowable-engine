
package org.activiti.engine.test.api.task;

import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

public class DeleteCandidateTaskListener implements TaskListener {
    public void notify(DelegateTask delegateTask) {
        ((TaskEntity) delegateTask).deleteCandidateUser("admin");
    }
}
