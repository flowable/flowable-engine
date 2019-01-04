package ${package};

import static org.junit.Assert.assertEquals;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.FlowableRule;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.Test;

public class MyUnitTest {

    @Rule
    public FlowableRule flowableRule = new FlowableRule();

    @Test
    @Deployment(resources = {"${packageInPathFormat}/my-process.bpmn20.xml"})
    public void test() {
        ProcessInstance processInstance = flowableRule.getRuntimeService().startProcessInstanceByKey("my-process");
        assertNotNull(processInstance);

        Task task = flowableRule.getTaskService().createTaskQuery().singleResult();
        assertEquals("Flowable is awesome!", task.getName());
    }

}
