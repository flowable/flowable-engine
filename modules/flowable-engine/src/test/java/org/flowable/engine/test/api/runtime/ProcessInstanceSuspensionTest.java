/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class ProcessInstanceSuspensionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceActiveByDefault() {

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        runtimeService.startProcessInstanceByKey(processDefinition.getKey());

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.isSuspended()).isFalse();

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testSuspendActivateProcessInstance() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        runtimeService.startProcessInstanceByKey(processDefinition.getKey());

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.isSuspended()).isFalse();

        // suspend
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.isSuspended()).isTrue();

        // activate
        runtimeService.activateProcessInstanceById(processInstance.getId());
        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.isSuspended()).isFalse();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testCannotActivateActiveProcessInstance() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        runtimeService.startProcessInstanceByKey(processDefinition.getKey());

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.isSuspended()).isFalse();

        assertThatThrownBy(() -> runtimeService.activateProcessInstanceById(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testCannotSuspendSuspendedProcessInstance() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        runtimeService.startProcessInstanceByKey(processDefinition.getKey());

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.isSuspended()).isFalse();

        runtimeService.suspendProcessInstanceById(processInstance.getId());

        assertThatThrownBy(() -> runtimeService.suspendProcessInstanceById(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcessWithMultipleNestedSubProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testQueryForActiveAndSuspendedProcessInstances() {
        runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
        assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(5);
        assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isZero();

        ProcessInstance piToSuspend = runtimeService.createProcessInstanceQuery().processDefinitionKey("nestedSubProcessQueryTest").singleResult();
        runtimeService.suspendProcessInstanceById(piToSuspend.getId());

        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5);
        assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(4);
        assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(1);

        assertThat(runtimeService.createProcessInstanceQuery().suspended().singleResult().getId()).isEqualTo(piToSuspend.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTaskSuspendedAfterProcessInstanceSuspension() {

        // Start Process Instance
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        runtimeService.startProcessInstanceByKey(processDefinition.getKey());
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

        // Suspense process instance
        runtimeService.suspendProcessInstanceById(processInstance.getId());

        // Assert that the task is now also suspended
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (org.flowable.task.api.Task task : tasks) {
            assertThat(task.isSuspended()).isTrue();
        }

        // Activate process instance again
        runtimeService.activateProcessInstanceById(processInstance.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (org.flowable.task.api.Task task : tasks) {
            assertThat(task.isSuspended()).isFalse();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testTaskQueryAfterProcessInstanceSuspend() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();

        // Suspend
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().suspended().count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().active().count()).isZero();

        // Activate
        runtimeService.activateProcessInstanceById(processInstance.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().suspended().count()).isZero();
        assertThat(taskService.createTaskQuery().active().count()).isEqualTo(1);

        // Completing should end the process instance
        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testChildExecutionsSuspendedAfterProcessInstanceSuspend() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testChildExecutionsSuspended");
        runtimeService.suspendProcessInstanceById(processInstance.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        for (Execution execution : executions) {
            assertThat(execution.isSuspended()).isTrue();
        }

        // Activate again
        runtimeService.activateProcessInstanceById(processInstance.getId());
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        for (Execution execution : executions) {
            assertThat(execution.isSuspended()).isFalse();
        }

        // Finish process
        while (taskService.createTaskQuery().count() > 0) {
            for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
                taskService.complete(task.getId());
            }
        }
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSubmitTaskFormAfterProcessInstanceSuspend() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        runtimeService.suspendProcessInstanceById(processInstance.getId());

        assertThatThrownBy(() -> formService
                .submitTaskFormData(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId(), new HashMap<>()))
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceOperationsFailAfterSuspend() {

        // Suspend process instance
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        runtimeService.suspendProcessInstanceById(processInstance.getId());

        assertThatThrownBy(() -> runtimeService.messageEventReceived("someMessage", processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.messageEventReceived("someMessage", processInstance.getId(), new HashMap<>()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.removeVariable(processInstance.getId(), "someVariable"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.removeVariableLocal(processInstance.getId(), "someVariable"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.removeVariables(processInstance.getId(), Arrays.asList("one", "two", "three")))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.removeVariablesLocal(processInstance.getId(), Arrays.asList("one", "two", "three")))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "someVariable", "someValue"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.setVariableLocal(processInstance.getId(), "someVariable", "someValue"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.setVariables(processInstance.getId(), new HashMap<>()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.setVariablesLocal(processInstance.getId(), new HashMap<>()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.trigger(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.trigger(processInstance.getId(), new HashMap<>()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.signalEventReceived("someSignal", processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");

        assertThatThrownBy(() -> runtimeService.signalEventReceived("someSignal", processInstance.getId(), new HashMap<>()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is suspended");
    }

    @Test
    @Deployment
    public void testSignalEventReceivedAfterProcessInstanceSuspended() {

        final String signal = "Some Signal";

        // Test if process instance can be completed using the signal
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
        runtimeService.signalEventReceived(signal);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        // Now test when suspending the process instance: the process instance
        // shouldn't be continued
        processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        runtimeService.signalEventReceived(signal);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        runtimeService.signalEventReceived(signal, new HashMap<>());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        // Activate and try again
        runtimeService.activateProcessInstanceById(processInstance.getId());
        runtimeService.signalEventReceived(signal);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/ProcessInstanceSuspensionTest.testSignalEventReceivedAfterProcessInstanceSuspended.bpmn20.xml")
    public void testSignalEventReceivedAfterMultipleProcessInstancesSuspended() {

        final String signal = "Some Signal";

        // Test if process instance can be completed using the signal
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
        runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
        runtimeService.signalEventReceived(signal);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        // Now test when suspending the process instance: the process instance
        // shouldn't be continued
        processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        processInstance = runtimeService.startProcessInstanceByKey("signalSuspendedProcessInstance");
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        runtimeService.signalEventReceived(signal);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

        runtimeService.signalEventReceived(signal, new HashMap<>());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

        // Activate and try again
        runtimeService.activateProcessInstanceById(processInstance.getId());
        runtimeService.signalEventReceived(signal);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testTaskOperationsFailAfterProcessInstanceSuspend() {

        // Start a new process instance with one task
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        final org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Suspend the process instance
        runtimeService.suspendProcessInstanceById(processInstance.getId());

        // Yeah, the following is pretty long and boring ... but I didn't have the patience
        // to create separate tests for each of them.

        // Completing the task should fail
        assertThatThrownBy(() -> taskService.complete(task.getId()))
                .as("It is not allowed to complete a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Claiming the task should fail
        assertThatThrownBy(() -> taskService.claim(task.getId(), "jos"))
                .as("It is not allowed to claim a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Setting variable on the task should fail
        assertThatThrownBy(() -> taskService.setVariable(task.getId(), "someVar", "someValue"))
                .as("It is not allowed to set a variable on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Setting variable on the task should fail
        assertThatThrownBy(() -> taskService.setVariableLocal(task.getId(), "someVar", "someValue"))
                .as("It is not allowed to set a variable on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Setting variables on the task should fail
        assertThatThrownBy(() -> {
            HashMap<String, String> variables = new HashMap<>();
            variables.put("varOne", "one");
            variables.put("varTwo", "two");
            taskService.setVariables(task.getId(), variables);
        })
                .as("It is not allowed to set variables on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Setting variables on the task should fail
        assertThatThrownBy(() -> {
            HashMap<String, String> variables = new HashMap<>();
            variables.put("varOne", "one");
            variables.put("varTwo", "two");
            taskService.setVariablesLocal(task.getId(), variables);
        })
                .isExactlyInstanceOf(FlowableException.class);

        // Removing variable on the task should fail
        assertThatThrownBy(() -> taskService.removeVariable(task.getId(), "someVar"))
                .as("It is not allowed to remove a variable on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Removing variable on the task should fail
        assertThatThrownBy(() -> taskService.removeVariableLocal(task.getId(), "someVar"))
                .as("It is not allowed to remove a variable on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Removing variables on the task should fail
        assertThatThrownBy(() -> taskService.removeVariables(task.getId(), Arrays.asList("one", "two")))
                .as("It is not allowed to remove a variable on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Removing variables on the task should fail
        assertThatThrownBy(() -> taskService.removeVariablesLocal(task.getId(), Arrays.asList("one", "two")))
                .as("It is not allowed to remove a variable on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Adding candidate groups on the task should fail
        assertThatThrownBy(() -> taskService.addCandidateGroup(task.getId(), "blahGroup"))
                .as("It is not allowed to add a candidate group on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Adding candidate users on the task should fail
        assertThatThrownBy(() -> taskService.addCandidateUser(task.getId(), "blahUser"))
                .as("It is not allowed to add a candidate user on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Adding candidate users on the task should fail
        assertThatThrownBy(() -> taskService.addGroupIdentityLink(task.getId(), "blahGroup", IdentityLinkType.CANDIDATE))
                .as("It is not allowed to add a candidate user on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Adding an identity link on the task should fail
        assertThatThrownBy(() -> taskService.addUserIdentityLink(task.getId(), "blahUser", IdentityLinkType.OWNER))
                .as("It is not allowed to add an identityLink on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Adding a comment on the task should fail
        assertThatThrownBy(() -> taskService.addComment(task.getId(), processInstance.getId(), "test comment"))
                .as("It is not allowed to add a comment on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Adding an attachment on the task should fail
        assertThatThrownBy(() -> taskService.createAttachment("text", task.getId(), processInstance.getId(), "testName", "testDescription", "http://test.com"))
                .as("It is not allowed to add an attachment on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Set an assignee on the task should fail
        assertThatThrownBy(() -> taskService.setAssignee(task.getId(), "mispiggy"))
                .as("It is not allowed to set an assignee on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Set an owner on the task should fail
        assertThatThrownBy(() -> taskService.setOwner(task.getId(), "kermit"))
                .as("It is not allowed to set an owner on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);

        // Set priority on the task should fail
        assertThatThrownBy(() -> taskService.setPriority(task.getId(), 99))
                .as("It is not allowed to set a priority on a task of a suspended process instance")
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment
    public void testJobNotExecutedAfterProcessInstanceSuspend() {

        Date now = new Date();
        processEngineConfiguration.getClock().setCurrentTime(now);

        // Suspending the process instance should also stop the execution of jobs for that process instance
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        Job job = managementService.createTimerJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getCorrelationId()).isNotNull();
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        String correlationId = job.getCorrelationId();
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        assertThat(managementService.createSuspendedJobQuery().count()).isEqualTo(1);

        // The jobs should not be executed now
        processEngineConfiguration.getClock().setCurrentTime(new Date(now.getTime() + (60 * 60 * 1000))); // Timer is set to fire on 5 minutes
        job = managementService.createTimerJobQuery().executable().singleResult();
        assertThat(job).isNull();

        assertThat(managementService.createSuspendedJobQuery().count()).isEqualTo(1);
        Job suspendedJob = managementService.createSuspendedJobQuery().correlationId(correlationId).singleResult();
        assertThat(suspendedJob).isNotNull();
        assertThat(suspendedJob.getCorrelationId()).isEqualTo(correlationId);

        // Activation of the process instance should now allow for job execution
        runtimeService.activateProcessInstanceById(processInstance.getId());
        waitForJobExecutorToProcessAllJobs(1000L, 100L);
        assertThat(managementService.createJobQuery().count()).isZero();
        assertThat(managementService.createTimerJobQuery().count()).isZero();
        assertThat(managementService.createSuspendedJobQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/ProcessInstanceSuspensionTest.testJobNotExecutedAfterProcessInstanceSuspend.bpmn20.xml")
    public void testJobActivationAfterProcessInstanceSuspend() {

        Date now = new Date();
        processEngineConfiguration.getClock().setCurrentTime(now);

        // Suspending the process instance should also stop the execution of jobs for that process instance
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        assertThat(managementService.createSuspendedJobQuery().count()).isEqualTo(1);

        Job job = managementService.createTimerJobQuery().executable().singleResult();
        assertThat(job).isNull();

        Job suspendedJob = managementService.createSuspendedJobQuery().singleResult();
        assertThat(suspendedJob).isNotNull();

        // Activation of the suspended job instance should throw exception because parent is suspended
        assertThatThrownBy(() -> managementService.moveSuspendedJobToExecutableJob(suspendedJob.getId()))
                .as("FlowableIllegalArgumentException expected. Cannot activate job with suspended parent")
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("Can not activate job " + suspendedJob.getId() + ". Parent is suspended.");
    }

}
