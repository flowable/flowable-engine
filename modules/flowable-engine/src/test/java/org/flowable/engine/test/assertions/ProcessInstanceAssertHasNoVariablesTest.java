package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.complete;
import static org.flowable.engine.assertions.BpmnAwareTests.runtimeService;
import static org.flowable.engine.assertions.BpmnAwareTests.task;
import static org.flowable.engine.assertions.BpmnAwareTests.withVariables;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class ProcessInstanceAssertHasNoVariablesTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNoVariablesTest.bpmn20.xml")
    public void noneSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNoVariables");
        assertThat(processInstance).hasNoVariables();
        complete(task(processInstance));
        assertThat(processInstance).hasNoVariables();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNoVariablesTest.bpmn20.xml")
    public void oneFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNoVariables",
                withVariables("aVariable", "aValue"));
        assertThatThrownBy(() -> assertThat(processInstance).hasNoVariables())
                .isInstanceOf(AssertionError.class);
        complete(task(processInstance));
        assertThatThrownBy(() -> assertThat(processInstance).hasNoVariables())
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasNoVariablesTest.bpmn20.xml")
    public void twoFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasNoVariables",
                withVariables("firstVariable", "firstValue", "secondVariable", "secondValue"));
        assertThatThrownBy(() -> assertThat(processInstance).hasNoVariables())
                .isInstanceOf(AssertionError.class);
        complete(task(processInstance));
        assertThatThrownBy(() -> assertThat(processInstance).hasNoVariables())
                .isInstanceOf(AssertionError.class);
    }
}
