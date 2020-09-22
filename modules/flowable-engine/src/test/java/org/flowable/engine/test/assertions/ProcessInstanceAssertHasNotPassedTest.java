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

public class ProcessInstanceAssertHasNotPassedTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNotPassedTest.bpmn20.xml")
    public void onlyActivityRunningInstanceSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNotPassed");
        assertThat(processInstance).hasNotPassed("UserTask_1");
        assertThat(processInstance).hasNotPassed("UserTask_2", "UserTask_3", "UserTask_4");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNotPassedTest.bpmn20.xml")
    public void onlyActivityRunningInstanceFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNotPassed");
        complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
        assertThatThrownBy(() -> assertThat(processInstance).hasNotPassed("UserTask_1"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNotPassedTest.bpmn20.xml")
    public void parallelActivitiesRunningInstanceSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNotPassed");
        complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        assertThat(processInstance).hasNotPassed("UserTask_3");
        assertThat(processInstance).hasNotPassed("UserTask_4");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNotPassedTest.bpmn20.xml")
    public void parallelActivitiesRunningInstanceFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNotPassed");
        complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).hasNotPassed("UserTask_2", "UserTask_3", "UserTask_4"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNotPassedTest.bpmn20.xml")
    public void severalActivitiesRunningInstanceSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNotPassed");
        complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
        assertThat(processInstance).hasNotPassed("UserTask_4", "UserTask_5");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNotPassedTest.bpmn20.xml")
    public void severalActivitiesHistoricInstanceSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNotPassed");
        complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
        complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
        assertThat(processInstance).hasNotPassed("UserTask_5");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNotPassedTest.bpmn20.xml")
    public void testHasNotPassed_SeveralActivities_HistoricInstance_Failure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNotPassed");
        complete(taskQuery().singleResult(), withVariables("doUserTask5", true));
        complete(taskQuery().taskDefinitionKey("UserTask_5").singleResult());
        complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).hasNotPassed("UserTask_5"))
                .isInstanceOf(AssertionError.class);
    }
}
