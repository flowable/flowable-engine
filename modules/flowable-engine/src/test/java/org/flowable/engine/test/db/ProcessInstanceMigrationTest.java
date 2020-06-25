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

package org.flowable.engine.test.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cmd.SetProcessDefinitionVersionCmd;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Falko Menge
 */
public class ProcessInstanceMigrationTest extends PluggableFlowableTestCase {

    private static final String TEST_PROCESS_WITH_PARALLEL_GATEWAY = "org/flowable/examples/bpmn/gateway/ParallelGatewayTest.testForkJoin.bpmn20.xml";
    private static final String TEST_PROCESS = "org/flowable/engine/test/db/ProcessInstanceMigrationTest.testSetProcessDefinitionVersion.bpmn20.xml";
    private static final String TEST_PROCESS_ACTIVITY_MISSING = "org/flowable/engine/test/db/ProcessInstanceMigrationTest.testSetProcessDefinitionVersionActivityMissing.bpmn20.xml";

    private static final String TEST_PROCESS_CALL_ACTIVITY = "org/flowable/engine/test/db/ProcessInstanceMigrationTest.withCallActivity.bpmn20.xml";
    private static final String TEST_PROCESS_USER_TASK_V1 = "org/flowable/engine/test/db/ProcessInstanceMigrationTest.testSetProcessDefinitionVersionWithTask.bpmn20.xml";
    private static final String TEST_PROCESS_USER_TASK_V2 = "org/flowable/engine/test/db/ProcessInstanceMigrationTest.testSetProcessDefinitionVersionWithTaskV2.bpmn20.xml";
    private static final String TEST_PROCESS_NESTED_SUB_EXECUTIONS = "org/flowable/engine/test/db/ProcessInstanceMigrationTest.testSetProcessDefinitionVersionSubExecutionsNested.bpmn20.xml";

    @Test
    public void testSetProcessDefinitionVersionEmptyArguments() {
        assertThatThrownBy(() -> new SetProcessDefinitionVersionCmd(null, 23))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The process instance id is mandatory, but 'null' has been provided.");

        assertThatThrownBy(() -> new SetProcessDefinitionVersionCmd("", 23))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The process instance id is mandatory, but '' has been provided.");

        assertThatThrownBy(() -> new SetProcessDefinitionVersionCmd("42", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The process definition version is mandatory, but 'null' has been provided.");

        assertThatThrownBy(() -> new SetProcessDefinitionVersionCmd("42", -1))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The process definition version must be positive, but '-1' has been provided.");
    }

    @Test
    public void testSetProcessDefinitionVersionNonExistingPI() {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        assertThatThrownBy(() -> commandExecutor.execute(new SetProcessDefinitionVersionCmd("42", 23)))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No process instance found for id = '42'.");
    }

    @Test
    @Deployment(resources = { TEST_PROCESS_WITH_PARALLEL_GATEWAY })
    public void testSetProcessDefinitionVersionPIIsSubExecution() {
        // start process instance
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("forkJoin");

        Execution execution = runtimeService.createExecutionQuery().activityId("receivePayment").singleResult();
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        SetProcessDefinitionVersionCmd command = new SetProcessDefinitionVersionCmd(execution.getId(), 1);
        assertThatThrownBy(() -> commandExecutor.execute(command))
                .isInstanceOf(FlowableException.class)
                .hasMessage("A process instance id is required, but the provided id '" + execution.getId() + "' points to a child execution of process instance '" + pi.getId() + "'. Please invoke the "
                        + command.getClass().getSimpleName() + " with a root execution id.");
    }

    @Test
    @Deployment(resources = { TEST_PROCESS })
    public void testSetProcessDefinitionVersionNonExistingPD() {
        // start process instance
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("receiveTask");

        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        assertThatThrownBy(() -> commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 23)))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("no processes deployed with key = 'receiveTask' and version = '23'");
    }

    @Test
    @Deployment(resources = { TEST_PROCESS })
    public void testSetProcessDefinitionVersionActivityMissing() {
        // start process instance
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("receiveTask");

        // check that receive task has been reached
        Execution execution = runtimeService.createExecutionQuery().activityId("waitState1").singleResult();
        assertThat(execution).isNotNull();

        // deploy new version of the process definition
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addClasspathResource(TEST_PROCESS_ACTIVITY_MISSING).deploy();
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

        // migrate process instance to new process definition version
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        SetProcessDefinitionVersionCmd setProcessDefinitionVersionCmd = new SetProcessDefinitionVersionCmd(pi.getId(), 2);
        assertThatThrownBy(() -> commandExecutor.execute(setProcessDefinitionVersionCmd))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("The new process definition (key = 'receiveTask') does not contain the current activity (id = 'waitState1') of the process instance (id = '");

        // undeploy "manually" deployed process definition
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

    @Test
    @Deployment
    public void testSetProcessDefinitionVersion() {
        // start process instance
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("receiveTask");

        // check that receive task has been reached
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).activityId("waitState1").singleResult();
        assertThat(execution).isNotNull();

        // deploy new version of the process definition
        repositoryService.createDeployment().addClasspathResource(TEST_PROCESS).deploy();
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

        // migrate process instance to new process definition version
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

        // signal process instance
        runtimeService.trigger(execution.getId());

        // check that the instance now uses the new process definition version
        ProcessDefinition newProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(2).singleResult();
        pi = runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(pi.getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
        
        // check history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicPI = historyService.createHistoricProcessInstanceQuery().processInstanceId(pi.getId()).singleResult();
            assertThat(historicPI.getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());

            List<HistoricActivityInstance> historicActivities = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(pi.getId())
                    .unfinished()
                    .list();
            assertThat(historicActivities)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsExactly(newProcessDefinition.getId());
        }

        deleteDeployments();
    }

    @Test
    @Deployment(resources = { TEST_PROCESS_WITH_PARALLEL_GATEWAY })
    public void testSetProcessDefinitionVersionSubExecutions() {
        // start process instance
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("forkJoin");

        // check that the user tasks have been reached
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        // deploy new version of the process definition
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addClasspathResource(TEST_PROCESS_WITH_PARALLEL_GATEWAY).deploy();
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

        // migrate process instance to new process definition version
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

        // check that all executions of the instance now use the new process
        // definition version
        ProcessDefinition newProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(2).singleResult();
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).list();
        for (Execution execution : executions) {
            assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
        }

        // undeploy "manually" deployed process definition
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

    @Test
    @Deployment(resources = { TEST_PROCESS_CALL_ACTIVITY })
    public void testSetProcessDefinitionVersionWithCallActivity() {
        // start process instance
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("parentProcess");

        // check that receive task has been reached
        Execution execution = runtimeService.createExecutionQuery().activityId("waitState1").processDefinitionKey("childProcess").singleResult();
        assertThat(execution).isNotNull();

        // deploy new version of the process definition
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addClasspathResource(TEST_PROCESS_CALL_ACTIVITY).deploy();
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey("parentProcess").count()).isEqualTo(2);

        // migrate process instance to new process definition version
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

        // signal process instance
        runtimeService.trigger(execution.getId());

        // should be finished now
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count()).isZero();

        // undeploy "manually" deployed process definition
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

    @Test
    @Deployment(resources = { TEST_PROCESS_USER_TASK_V1 })
    public void testSetProcessDefinitionVersionWithWithTask() {
        try {
            // start process instance
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("userTask");

            // check that user task has been reached
            assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).count()).isEqualTo(1);

            // deploy new version of the process definition
            repositoryService.createDeployment().addClasspathResource(TEST_PROCESS_USER_TASK_V2).deploy();
            assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey("userTask").count()).isEqualTo(2);

            ProcessDefinition newProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("userTask").processDefinitionVersion(2).singleResult();

            // migrate process instance to new process definition version
            processEngineConfiguration.getCommandExecutor().execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

            // check UserTask
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
            assertThat(task.getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
            assertThat(formService.getTaskFormData(task.getId()).getFormKey()).isEqualTo("testFormKey");

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(pi.getId()).singleResult();
                assertThat(historicTask.getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
                assertThat(formService.getTaskFormData(historicTask.getId()).getFormKey()).isEqualTo("testFormKey");
            }

            // continue
            taskService.complete(task.getId());

            assertProcessEnded(pi.getId());

            deleteDeployments();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    @Deployment(resources = { TEST_PROCESS_NESTED_SUB_EXECUTIONS })
    public void testSetProcessDefinitionVersionSubExecutionsNested() {
        // start process instance
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("forkJoinNested");

        // check that the user tasks have been reached
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        // deploy new version of the process definition
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addClasspathResource(TEST_PROCESS_NESTED_SUB_EXECUTIONS).deploy();
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

        // migrate process instance to new process definition version
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

        // check that all executions of the instance now use the new process
        // definition version
        ProcessDefinition newProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(2).singleResult();
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).list();
        for (Execution execution : executions) {
            assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
        }

        // undeploy "manually" deployed process definition
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

}
