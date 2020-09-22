package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.complete;
import static org.flowable.engine.assertions.BpmnAwareTests.runtimeService;
import static org.flowable.engine.assertions.BpmnAwareTests.taskQuery;
import static org.flowable.engine.assertions.BpmnAwareTests.withVariables;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class ProcessInstanceAssertHasPassedInOrderLoopTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedInOrderLoopTest.bpmn20.xml")
    public void severalActivitiesHistoricInstance() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassedInOrder-loop",
                withVariables("exit", false));
        complete(taskQuery().taskDefinitionKey("UserTask_1").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult(), withVariables("exit", true));
        complete(taskQuery().taskDefinitionKey("UserTask_5").singleResult());
        assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_2", "UserTask_5");
        assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_3", "UserTask_4", "UserTask_3", "UserTask_4", "UserTask_5");
        assertThatThrownBy(() -> assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_3", "UserTask_4", "UserTask_3", "UserTask_4", "UserTask_3", "UserTask_4", "UserTask_5"))
                .isInstanceOf(AssertionError.class);
    }
}
