package countersign;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskCompleteListener implements TaskListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCompleteListener.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        LOGGER.info("Invoke into taskEnd method and task is "+delegateTask.getVariables()+"assgin:" +delegateTask.getAssignee());
    }
}
