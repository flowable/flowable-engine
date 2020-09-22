package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.complete;
import static org.flowable.engine.assertions.BpmnAwareTests.processDefinitionQuery;
import static org.flowable.engine.assertions.BpmnAwareTests.runtimeService;
import static org.flowable.engine.assertions.BpmnAwareTests.task;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class ProcessDefinitionAssertTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessDefinitionAssertTest.bpmn20.xml")
    public void oneStartedSuccess() {
        ProcessDefinition processDefinition = processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
        runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        assertThat(processDefinition).hasActiveInstances(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessDefinitionAssertTest.bpmn20.xml")
    public void oneStartedFailure() {
        ProcessDefinition processDefinition = processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
        runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        assertThatThrownBy(() -> assertThat(processDefinition).hasActiveInstances(0))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processDefinition).hasActiveInstances(2))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessDefinitionAssertTest.bpmn20.xml")
    public void twoStartedSuccess() {
        ProcessDefinition processDefinition = processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
        runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        assertThat(processDefinition).hasActiveInstances(2);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessDefinitionAssertTest.bpmn20.xml")
    public void twoStartedFailure() {
        ProcessDefinition processDefinition = processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
        runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        assertThatThrownBy(() -> assertThat(processDefinition).hasActiveInstances(0))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processDefinition).hasActiveInstances(1))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processDefinition).hasActiveInstances(3))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessDefinitionAssertTest.bpmn20.xml")
    public void twoStartedOneEndedSuccess() {
        ProcessDefinition processDefinition = processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        complete(task(processInstance));
        assertThat(processDefinition).hasActiveInstances(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessDefinitionAssertTest.bpmn20.xml")
    public void twoStartedOneEndedFailure() {
        ProcessDefinition processDefinition = processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        runtimeService().startProcessInstanceByKey("ProcessDefinitionAssert-hasActiveInstances");
        complete(task(processInstance));
        assertThatThrownBy(() -> assertThat(processDefinition).hasActiveInstances(0))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processDefinition).hasActiveInstances(2))
                .isInstanceOf(AssertionError.class);
    }
}
