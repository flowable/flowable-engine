

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.FlowableRule;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertNotNull;


public class MyUnitTest {

    @Rule
    public FlowableRule flowableRule = new FlowableRule();

    @Test
    @Deployment(resources = {"approve-process.bpmn20.xml"})
    public void test() {
        Map vars=new HashMap();
        List<String> countersigns=new ArrayList<>();
        countersigns.add("zjl0");
        countersigns.add("zjl1");
        countersigns.add("zjl3");
        vars.put("countersignAssigneeList",countersigns);
        vars.put("approveResult","pass");
        ProcessInstance processInstance = flowableRule.getRuntimeService().startProcessInstanceByKey
                ("approve-process",vars);
        assertNotNull(processInstance);

        List<Task> tasks = flowableRule.getTaskService().createTaskQuery().list();
        for(Task task:tasks){
            assert  flowableRule.getTaskService().getVariable(task.getId(), "csAssignee") == task.getAssignee();
            flowableRule.getTaskService().setVariableLocal(task.getId(), "csApproveResult", "pass");
            flowableRule.getTaskService().complete(task.getId());
        }
    }




}
