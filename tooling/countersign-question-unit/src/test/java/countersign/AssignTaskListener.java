package countersign;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

public class AssignTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        Object nrOfInstancesObj = delegateTask.getVariable("nrOfInstances");
        Object nrOfCompletedInstancesObj = delegateTask.getVariable("nrOfCompletedInstances");
        String nrOfInstances = null == nrOfInstancesObj ? null : nrOfInstancesObj.toString();
        String nrOfCompletedInstances = null == nrOfCompletedInstancesObj ? null : nrOfCompletedInstancesObj.toString();
        delegateTask.setAssignee("zjl" + nrOfCompletedInstances);
    }
}
