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

public class ProcessInstanceAssertHasVariablesTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasVariablesTest.bpmn20.xml")
    public void oneSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasVariables",
                withVariables("aVariable", "aValue"));
        assertThat(processInstance).hasVariables();
        assertThat(processInstance).hasVariables("aVariable");
        complete(task(processInstance));
        assertThat(processInstance).hasVariables();
        assertThat(processInstance).hasVariables("aVariable");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasVariablesTest.bpmn20.xml")
    public void oneFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasVariables",
                withVariables("aVariable", "aValue"));
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("anotherVariable"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("aVariable", "anotherVariable"))
                .isInstanceOf(AssertionError.class);
        complete(task(processInstance));
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("anotherVariable"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("aVariable", "anotherVariable"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasVariablesTest.bpmn20.xml")
    public void twoSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasVariables",
                withVariables("firstVariable", "firstValue", "secondVariable", "secondValue"));
        assertThat(processInstance).hasVariables();
        assertThat(processInstance).hasVariables("firstVariable");
        assertThat(processInstance).hasVariables("secondVariable");
        assertThat(processInstance).hasVariables("firstVariable", "secondVariable");
        assertThat(processInstance).hasVariables("secondVariable", "firstVariable");
        complete(task(processInstance));
        assertThat(processInstance).hasVariables();
        assertThat(processInstance).hasVariables("firstVariable");
        assertThat(processInstance).hasVariables("secondVariable");
        assertThat(processInstance).hasVariables("firstVariable", "secondVariable");
        assertThat(processInstance).hasVariables("secondVariable", "firstVariable");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasVariablesTest.bpmn20.xml")
    public void twoFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasVariables",
                withVariables("firstVariable", "firstValue", "secondVariable", "secondValue"));
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("anotherVariable"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("firstVariable", "anotherVariable"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("secondVariable", "anotherVariable"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("firstVariable", "secondVariable", "anotherVariable"))
                .isInstanceOf(AssertionError.class);
        complete(task(processInstance));
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("anotherVariable"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("firstVariable", "anotherVariable"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("secondVariable", "anotherVariable"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("firstVariable", "secondVariable", "anotherVariable"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/ProcessInstanceAssertHasVariablesTest.bpmn20.xml")
    public void noneFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessInstanceAssert-hasVariables");
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables())
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("aVariable"))
                .isInstanceOf(AssertionError.class);
        complete(task(processInstance));
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables())
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).hasVariables("aVariable"))
                .isInstanceOf(AssertionError.class);
    }

}
