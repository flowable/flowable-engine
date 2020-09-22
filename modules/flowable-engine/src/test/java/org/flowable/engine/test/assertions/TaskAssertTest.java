package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.claim;
import static org.flowable.engine.assertions.BpmnAwareTests.complete;
import static org.flowable.engine.assertions.BpmnAwareTests.taskQuery;
import static org.flowable.engine.assertions.BpmnAwareTests.taskService;

import java.util.Date;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class TaskAssertTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void taskAssertions() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskId");

        assertThat(processInstance).task().hasId(taskQuery().singleResult().getId());
        assertThat(processInstance).task().isNotAssigned();

        // claim
        claim(taskQuery().singleResult(), "fozzie");
        assertThat(processInstance).task().isAssignedTo("fozzie");
        assertThat(processInstance).task().isNotAssignedTo("kermit");

        assertThatThrownBy(() -> assertThat(processInstance).task().isNotAssigned())
                .isInstanceOf(AssertionError.class);

        // DefinitionKey
        assertThat(processInstance).task().hasDefinitionKey("task1");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasDefinitionKey("somethingElse"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasDefinitionKey(null))
                .isInstanceOf(AssertionError.class);

        // Name
        assertThat(processInstance).task().hasName("Task");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasName("someOtherName"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasName(null))
                .isInstanceOf(AssertionError.class);

        // Description
        assertThat(processInstance).task().hasDescription(null);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasDescription("somethingElse"))
                .isInstanceOf(AssertionError.class);

        assertThatThrownBy(() -> assertThat(processInstance).task().isAssignedTo(null))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("to be assigned to user 'null'");

        // DueDate
        assertThatThrownBy(() -> assertThat(processInstance).task().hasDueDate(new Date()))
                .isInstanceOf(AssertionError.class);
        Date dueDate = new Date();
        Task task = taskQuery().singleResult();
        task.setDueDate(dueDate);
        taskService().saveTask(task);
        assertThat(processInstance).task().hasDueDate(dueDate);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasDueDate(new Date()))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasDueDate(null))
                .isInstanceOf(AssertionError.class);

        // FormKey
        assertThat(processInstance).task().hasFormKey("formKey");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasFormKey("otherFormKey"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasFormKey(null))
                .isInstanceOf(AssertionError.class);

        // complete task
        complete(taskQuery().singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).task().isAssignedTo("fozzie"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskAssertTest.taskAssertions.bpmn20.xml")
    public void hasNameFailure() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskId");

        assertThatThrownBy(() -> assertThat(processInstance).task().hasName("otherName"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskAssertTest.taskAssertions.bpmn20.xml")
    public void hasNameNull() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskId");

        assertThatThrownBy(() -> assertThat(processInstance).task().hasName(null))
                .isInstanceOf(AssertionError.class);
    }

}
