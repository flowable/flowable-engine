package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.complete;
import static org.flowable.engine.assertions.BpmnAwareTests.runtimeService;
import static org.flowable.engine.assertions.BpmnAwareTests.taskQuery;
import static org.flowable.engine.assertions.BpmnAwareTests.taskService;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class TaskHasCandidateUserTest extends PluggableFlowableTestCase {

    private static final String CANDIDATE_USER = "candidateUser";
    private static final String ASSIGNEE = "assignee";

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void preDefinedSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void preDefinedFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        complete(taskQuery().singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void predefinedRemovedFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        taskService().deleteCandidateUser(taskQuery().singleResult().getId(), CANDIDATE_USER);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void preDefinedOtherFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        taskService().deleteCandidateUser(taskQuery().singleResult().getId(), CANDIDATE_USER);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateUser("otherCandidateUser"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void explicitelySetSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        complete(taskQuery().singleResult());
        taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
        assertThat(processInstance).task().hasCandidateUser("explicitCandidateUserId");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void explicitelySetFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        complete(taskQuery().singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void explicitelySetRemovedFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        complete(taskQuery().singleResult());
        taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
        taskService().deleteCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateUser("explicitCandidateUserId"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void explicitelySetOtherFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        complete(taskQuery().singleResult());
        taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateUser("otherCandidateUser"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void moreThanOneSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
        assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER);
        assertThat(processInstance).task().hasCandidateUser("explicitCandidateUserId");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void moreThanOneFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateUser("otherCandidateUser"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void nullFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateUser(null))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void nonExistingTaskFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        Task task = taskQuery().singleResult();
        complete(task);
        assertThatThrownBy(() -> assertThat(task).hasCandidateUser(CANDIDATE_USER))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateUserTest.bpmn20.xml")
    public void assignedFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateUser");
        Task task = taskQuery().singleResult();
        taskService().setAssignee(task.getId(), ASSIGNEE);
        assertThatThrownBy(() -> assertThat(task).hasCandidateUser(CANDIDATE_USER))
                .isInstanceOf(AssertionError.class);
    }
}
