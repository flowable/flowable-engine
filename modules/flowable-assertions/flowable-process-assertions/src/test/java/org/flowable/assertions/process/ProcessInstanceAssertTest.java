package org.flowable.assertions.process;

import org.assertj.core.groups.Tuple;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.assertions.process.TestUtils.createOneTaskProcess;

/**
 * @author martin.grofcik
 */
@FlowableTest
class ProcessInstanceAssertTest {

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void isRunning(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);

        FlowableProcessAssertions.assertThat(oneTaskProcess).isRunning();
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void isRunningForNonRunningProcess(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);
        taskService.complete(taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult().getId());

        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat(oneTaskProcess).isRunning())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected process instance <oneTaskProcess, "+oneTaskProcess.getId()+"> to be running but is not.");
    }

    @Test
    void isRunningForNull() {
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat((ProcessInstance) null).isRunning())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting actual not to be null");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void hasVariable(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);
        runtimeService.setVariable(oneTaskProcess.getId(), "variableName", "variableValue");

        FlowableProcessAssertions.assertThat(oneTaskProcess).hasVariable("variableName");
        FlowableProcessAssertions.assertThat(oneTaskProcess).hasVariable("variableName").isRunning();
        FlowableProcessAssertions.assertThat(oneTaskProcess).isRunning().hasVariable("variableName");
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat(oneTaskProcess).hasVariable("nonExistingVariable"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected process instance <oneTaskProcess, "+oneTaskProcess.getId()+"> has variable <nonExistingVariable> but variable does not exist.");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void hasVariableForNonRunningProcess(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);
        runtimeService.setVariable(oneTaskProcess.getId(), "variableName", "variableValue");
        taskService.complete(taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult().getId());

        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat(oneTaskProcess).hasVariable("variableName"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected process instance <oneTaskProcess, "+oneTaskProcess.getId()+"> has variable <variableName> but variable does not exist.");
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat(oneTaskProcess).hasVariable("nonExistingVariable"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected process instance <oneTaskProcess, "+oneTaskProcess.getId()+"> has variable <nonExistingVariable> but variable does not exist.");
    }

    @Test
    void hasVariableForNull() {
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat((ProcessInstance) null).hasVariable("variableName"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting actual not to be null");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void doesNotHaveVariable(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);
        runtimeService.setVariable(oneTaskProcess.getId(), "variableName", "variableValue");

        FlowableProcessAssertions.assertThat(oneTaskProcess).doesNotHaveVariable("NonExistingVariableName");
        FlowableProcessAssertions.assertThat(oneTaskProcess).doesNotHaveVariable("NonExistingVariableName").isRunning();
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat(oneTaskProcess).doesNotHaveVariable("variableName"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected process instance <oneTaskProcess, "+oneTaskProcess.getId()+"> does not have variable <variableName> but variable exists.");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void doesNotHaveVariableForNonRunningProcess(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);
        runtimeService.setVariable(oneTaskProcess.getId(), "variableName", "variableValue");
        taskService.complete(taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult().getId());

        FlowableProcessAssertions.assertThat(oneTaskProcess).doesNotHaveVariable("variableName");
        FlowableProcessAssertions.assertThat(oneTaskProcess).doesNotHaveVariable("nonExistingVariable");
    }

    @Test
    void doesNotHaveVariableForNull() {
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat((ProcessInstance) null).doesNotHaveVariable("variableName"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting actual not to be null");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void doesNotExistForRunningInstance(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);

        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat(oneTaskProcess).doesNotExist())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected process instance <oneTaskProcess, "+oneTaskProcess.getId()+"> is finished but instance exists in runtime.");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void doesNotExistForNonRunningProcess(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);
        taskService.complete(taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult().getId());

        FlowableProcessAssertions.assertThat(oneTaskProcess).doesNotExist();
    }

    @Test
    void doesNotExistForNull() {
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat((ProcessInstance) null).doesNotExist())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting actual not to be null");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void activitiesForRunningInstance(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);

        FlowableProcessAssertions.assertThat(oneTaskProcess).activities().extracting(ActivityInstance::getActivityId)
                .contains("theStart", "theStart-theTask", "theTask");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void activitiesForNonRunningProcess(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);
        taskService.complete(taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult().getId());

        FlowableProcessAssertions.assertThat(oneTaskProcess).activities().isEmpty();
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void executions(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);

        FlowableProcessAssertions.assertThat(oneTaskProcess).as("Running process has at least 2 active executions." +
                        "(ProcessInstance + Child)")
                .executions().extracting(Execution::getId).contains(oneTaskProcess.getId())
                        .hasSize(2);

        taskService.complete(taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult().getId());

        FlowableProcessAssertions.assertThat(oneTaskProcess).doesNotExist().as("There must be no execution for the finished process.")
                .executions().hasSize(0);
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void variables(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);

        FlowableProcessAssertions.assertThat(oneTaskProcess).variables().isEmpty();
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat(oneTaskProcess).hasVariableWithValue("nonExistingVar", "anyValue"))
                .isInstanceOf(AssertionError.class).hasMessage("Expected process instance <oneTaskProcess, "
                        +oneTaskProcess.getId()+"> has variable <nonExistingVar> but variable does not exist.");

        runtimeService.setVariable(oneTaskProcess.getId(), "testVariable", "initialValue");

        FlowableProcessAssertions.assertThat(oneTaskProcess).variables().extracting("name", "value").contains(Tuple.tuple("testVariable", "initialValue"));
        FlowableProcessAssertions.assertThat(oneTaskProcess).hasVariableWithValue("testVariable", "initialValue");

        runtimeService.setVariable(oneTaskProcess.getId(), "testVariable", "updatedValue");

        FlowableProcessAssertions.assertThat(oneTaskProcess).variables().extracting("name", "value").contains(Tuple.tuple("testVariable", "updatedValue"));
        FlowableProcessAssertions.assertThat(oneTaskProcess).hasVariableWithValue("testVariable", "updatedValue").isRunning();

        runtimeService.setVariable(oneTaskProcess.getId(), "firstVariable", "initialValue");
        FlowableProcessAssertions.assertThat(oneTaskProcess).as("Variables are ordered by names ascending.")
                .variables().extracting("name")
                .containsExactly("firstVariable", "testVariable");

    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void identityLinks(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = createOneTaskProcess(runtimeService);

        FlowableProcessAssertions.assertThat(oneTaskProcess).identityLinks().isEmpty();
        FlowableProcessAssertions.assertThat(oneTaskProcess).inHistory().identityLinks().isEmpty();

        runtimeService.addUserIdentityLink(oneTaskProcess.getId(), "testUser", IdentityLinkType.ASSIGNEE);
        runtimeService.addGroupIdentityLink(oneTaskProcess.getId(), "testGroup", IdentityLinkType.CANDIDATE);

        FlowableProcessAssertions.assertThat(oneTaskProcess).identityLinks().hasSize(2)
                .extracting(IdentityLink::getUserId, IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactlyInAnyOrder(Tuple.tuple("testUser", null, IdentityLinkType.ASSIGNEE),
                        Tuple.tuple(null, "testGroup", IdentityLinkType.CANDIDATE));
        FlowableProcessAssertions.assertThat(oneTaskProcess).inHistory().identityLinks().hasSize(2)
                .extracting(HistoricIdentityLink::getUserId, HistoricIdentityLink::getGroupId, HistoricIdentityLink::getType)
                .containsExactlyInAnyOrder(Tuple.tuple("testUser", null, IdentityLinkType.ASSIGNEE),
                        Tuple.tuple(null, "testGroup", IdentityLinkType.CANDIDATE));

    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void userTasks(RuntimeService runtimeService, ManagementService managementService,
                   TaskService taskService, ProcessEngineConfiguration processEngineConfiguration) {
        ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").startAsync();

        FlowableProcessAssertions.assertThat(oneTaskProcess).as("Process instance is started asynchronously and did not reach userTask")
                .userTasks().isEmpty();
        FlowableProcessAssertions.assertThat(oneTaskProcess).as("Process instance is started asynchronously and did not reach userTask in history")
                .inHistory().userTasks().isEmpty();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 10_000, 500);

        FlowableProcessAssertions.assertThat(oneTaskProcess).as("Async executions executed and userTask reached.")
                .userTasks().hasSize(1).extracting(Task::getTaskDefinitionKey).containsExactly("theTask");
        FlowableProcessAssertions.assertThat(oneTaskProcess).as("Async executions executed and userTask reached in history.")
                .inHistory().userTasks().hasSize(1).extracting(HistoricTaskInstance::getTaskDefinitionKey)
                .containsExactly("theTask");

        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        taskService.complete(task.getId());

        FlowableProcessAssertions.assertThat(oneTaskProcess).as("User tasks are empty for non existing process").doesNotExist()
                .userTasks().isEmpty();
        FlowableProcessAssertions.assertThat(oneTaskProcess).as("The userTask is completed, but must exist in history.")
                .inHistory().isFinished()
                .userTasks().hasSize(1).extracting(HistoricTaskInstance::getTaskDefinitionKey)
                .containsExactly("theTask");
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void subscriptionsWithoutSubscription(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();

        FlowableProcessAssertions.assertThat(oneTaskProcess).as("One task process does not have any subscription")
                .eventSubscription().isEmpty();

    }

    @Test
    @Deployment(resources = "oneTaskWithBoundaryEvent.bpmn20.xml")
    void subscriptions(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcessWithSubscription = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcessWithBoundaryEvent").start();

        FlowableProcessAssertions.assertThat(oneTaskProcessWithSubscription).as("One task process with subscription has exactly one subscription")
                .eventSubscription().hasSize(1).extracting(EventSubscription::getEventName).contains("eventMessage");
    }

    @Test
    void activitiesForNull() {
        assertThatThrownBy(() -> FlowableProcessAssertions.assertThat((ProcessInstance) null).doesNotExist())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expecting actual not to be null");
    }

}