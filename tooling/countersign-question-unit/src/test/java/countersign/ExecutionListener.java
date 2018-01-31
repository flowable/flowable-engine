package countersign;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.test.FlowableRule;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExecutionListener implements JavaDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionListener.class);
    @Rule
    public FlowableRule flowableRule = new FlowableRule();

    @Override
    public void execute(DelegateExecution execution) {
        Object nrOfInstancesObj = execution.getVariable("nrOfInstances");
        Object nrOfCompletedInstancesObj = execution.getVariable("nrOfCompletedInstances");
        String nrOfInstances = null == nrOfInstancesObj ? null : nrOfInstancesObj.toString();
        String nrOfCompletedInstances = null == nrOfCompletedInstancesObj ? null : nrOfCompletedInstancesObj.toString();
        LOGGER.debug("Invoke into executionEnd method and execution is" + execution.getVariables());
        //TODO in the logger execute 4 times, 3 child, and the last is parent in activiti 6.* ,in the last i can get
        // nrOfCompletedInstances 3 but in flowable nrOfCompletedInstances is null
        if (nrOfCompletedInstances != null && nrOfInstances == nrOfCompletedInstances) {
            List<HistoricVariableInstance> vars = flowableRule.getHistoryService().createHistoricVariableInstanceQuery()
                    .processInstanceId(execution.getProcessInstanceId())
                    .variableName("csApproveResult")
                    .list();
            Integer passCount = 0;
            Integer notPassCount = 0;
            for (HistoricVariableInstance value : vars) {
                if ("pass".equals(value.getValue().toString())) {
                    passCount++;
                }
                if ("notpass".equals(value.getValue().toString())) {
                    notPassCount++;
                }
            }
            LOGGER.info("Countersign result: pass" + passCount + " not pass:" + notPassCount);
            execution.setVariable("approveResult", passCount > notPassCount ? "pass" : "notpass");
        }
    }
}
