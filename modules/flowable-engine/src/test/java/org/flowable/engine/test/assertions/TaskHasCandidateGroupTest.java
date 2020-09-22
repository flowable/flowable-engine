package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.claim;
import static org.flowable.engine.assertions.BpmnAwareTests.complete;
import static org.flowable.engine.assertions.BpmnAwareTests.runtimeService;
import static org.flowable.engine.assertions.BpmnAwareTests.task;
import static org.flowable.engine.assertions.BpmnAwareTests.taskQuery;
import static org.flowable.engine.assertions.BpmnAwareTests.taskService;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class TaskHasCandidateGroupTest extends PluggableFlowableTestCase {

    private static final String CANDIDATE_GROUP = "candidateGroup";
    private static final String ASSIGNEE = "assignee";

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void preDefinedSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void preDefinedFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        complete(taskQuery().singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void predefinedRemovedFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), CANDIDATE_GROUP);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void preDefinedOtherFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), CANDIDATE_GROUP);
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateGroup("otherCandidateGroup"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void explicitelySetSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        complete(taskQuery().singleResult());
        taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
        assertThat(processInstance).task().hasCandidateGroup("explicitCandidateGroupId");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void explicitelySetFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        complete(taskQuery().singleResult());
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void explicitelySetRemovedFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        complete(taskQuery().singleResult());
        taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
        taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateGroup("explicitCandidateGroupId"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void explicitelySetOtherFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        complete(taskQuery().singleResult());
        taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateGroup("otherCandidateGroup"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void moreThanOneSuccess() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
        assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP);
        assertThat(processInstance).task().hasCandidateGroup("explicitCandidateGroupId");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void moreThanOneFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateGroup("otherCandidateGroup"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void nullFailure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        assertThatThrownBy(() -> assertThat(processInstance).task().hasCandidateGroup(null))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void testHasCandidateGroup_NonExistingTask_Failure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        Task task = taskQuery().singleResult();
        complete(task);
        assertThatThrownBy(() -> assertThat(task).hasCandidateGroup(CANDIDATE_GROUP))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/TaskHasCandidateGroupTest.bpmn20.xml")
    public void testHasCandidateGroup_Assigned_Failure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TaskAssert-hasCandidateGroup");
        claim(task(processInstance), ASSIGNEE);
        assertThatThrownBy(() -> assertThat(task(processInstance)).hasCandidateGroup(CANDIDATE_GROUP))
                .isInstanceOf(AssertionError.class);
    }
}
