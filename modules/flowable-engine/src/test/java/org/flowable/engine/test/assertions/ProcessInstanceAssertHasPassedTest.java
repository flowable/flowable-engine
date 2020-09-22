package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.complete;
import static org.flowable.engine.assertions.BpmnAwareTests.runtimeService;
import static org.flowable.engine.assertions.BpmnAwareTests.taskQuery;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class ProcessInstanceAssertHasPassedTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void onlyActivityRunningInstanceSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        complete(taskQuery().singleResult());
        assertThat(processInstance).hasPassed("UserTask_1");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void onlyActivityRunningInstanceFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        assertThatThrownBy(() -> assertThat(processInstance).hasPassed("UserTask_1"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void parallelActivitiesRunningInstanceSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        complete(taskQuery().singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        assertThat(processInstance).hasPassed("UserTask_1");
        assertThat(processInstance).hasPassed("UserTask_2");
        assertThat(processInstance).hasPassed("UserTask_1", "UserTask_2");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void parallelActivitiesRunningInstanceFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        complete(taskQuery().singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).hasPassed("UserTask_3"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasPassed("UserTask_4"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void severalActivitiesRunningInstanceSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        complete(taskQuery().singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
        assertThat(processInstance).hasPassed("UserTask_1");
        assertThat(processInstance).hasPassed("UserTask_2");
        assertThat(processInstance).hasPassed("UserTask_3");
        assertThat(processInstance).hasPassed("UserTask_1", "UserTask_2", "UserTask_3");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void severalActivitiesRunningInstanceFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        complete(taskQuery().singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).hasPassed("UserTask_4"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void severalActivitiesHistoricInstanceSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        complete(taskQuery().singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
        assertThat(processInstance).hasPassed("UserTask_1");
        assertThat(processInstance).hasPassed("UserTask_2");
        assertThat(processInstance).hasPassed("UserTask_3");
        assertThat(processInstance).hasPassed("UserTask_4");
        assertThat(processInstance).hasPassed("UserTask_1", "UserTask_2", "UserTask_3", "UserTask_4");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void severalActivitiesHistoricInstanceFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        complete(taskQuery().singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).hasPassed("UserTask_5"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasPassedTest.bpmn20.xml")
    public void nullError() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasPassed");
        String[] passed = null;
        assertThatThrownBy(() -> assertThat(processInstance).hasPassedInOrder(passed))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasPassedInOrder("ok", null))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasPassedInOrder(null, "ok"))
                .isInstanceOf(AssertionError.class);
        String[] args = new String[]{};
        assertThatThrownBy(() -> assertThat(processInstance).hasPassedInOrder(args))
                .isInstanceOf(AssertionError.class);
    }
}
