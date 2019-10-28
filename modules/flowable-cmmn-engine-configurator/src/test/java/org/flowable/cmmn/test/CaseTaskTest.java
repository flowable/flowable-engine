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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Valentin Zickner
 */
public class CaseTaskTest extends AbstractProcessEngineIntegrationTest {
    
    @Test
    @CmmnDeployment
    public void testCaseTask() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcess.bpmn20.xml")
                .deploy();
        
        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask");
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());
            
            processEngineTaskService.complete(processTasks.get(0).getId());
            
            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                            .processInstanceId(processInstance.getId())
                            .singleResult();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                            .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                            .singleResult();
            
            assertNotNull(caseInstance);
            
            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertEquals(1, caseTasks.size());
            
            cmmnTaskService.complete(caseTasks.get(0).getId());
            
            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNull(dbCaseInstance);
            
            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());
            
            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
            
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
    public void testCaseTaskWithCaseDefinitionKeyExpression() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithCaseDefinitionKeyExpression.bpmn20.xml")
            .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                .processDefinitionKey("caseTask")
                .variable("caseKeyVar", "oneHumanTaskCase")
                .start();
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngineTaskService.complete(processTasks.get(0).getId());

            String caseInstanceId = processEngineRuntimeService.getVariable(processInstance.getId(), "myCaseInstanceId", String.class);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .singleResult();

            assertThat(caseInstance).isNotNull();

            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(caseTasks).hasSize(1);

            cmmnTaskService.complete(caseTasks.get(0).getId());

            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(dbCaseInstance).isNull();

            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(0);

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseTaskWithParameters() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithParameters.bpmn20.xml")
                .deploy();
        
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("testVar", "test");
            variables.put("testNumVar", 43);
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask", variables);
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());
            
            processEngineTaskService.complete(processTasks.get(0).getId());
            
            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                            .processInstanceId(processInstance.getId())
                            .singleResult();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                            .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                            .singleResult();
            
            assertNotNull(caseInstance);
            
            assertEquals("test", cmmnRuntimeService.getVariable(caseInstance.getId(), "caseTestVar"));
            assertEquals(43, cmmnRuntimeService.getVariable(caseInstance.getId(), "caseTestNumVar"));
            
            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertEquals(1, caseTasks.size());
            
            cmmnRuntimeService.setVariable(caseInstance.getId(), "caseResult", "someResult");
            
            cmmnTaskService.complete(caseTasks.get(0).getId());
            
            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNull(dbCaseInstance);
            
            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());
            
            assertEquals("someResult", processEngineRuntimeService.getVariable(processInstance.getId(), "processResult"));
            
            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
            
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTaskWithParameters.cmmn")
    public void testCaseTaskWithCaseNameAndBusinessKey() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithCaseNameAndBusinessKey.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                .processDefinitionKey("caseTask")
                .variable("caseName", "Test")
                .variable("caseBusinessKey", "case-test-business")
                .start();
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());

            processEngineTaskService.complete(processTasks.get(0).getId());

            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                            .processInstanceId(processInstance.getId())
                            .singleResult();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                            .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                            .singleResult();

            assertThat(caseInstance).isNotNull();
            assertThat(caseInstance.getName()).isEqualTo("Custom Test Name");
            assertThat(caseInstance.getBusinessKey()).isEqualTo("case-test-business");

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn")
    public void testDeleteCaseTaskShouldNotBePossible() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/caseTaskProcess.bpmn20.xml")
            .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask");
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngineTaskService.complete(processTasks.get(0).getId());

            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                .processInstanceId(processInstance.getId())
                .singleResult();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                .singleResult();

            assertThat(caseInstance).isNotNull();

            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(caseTasks).hasSize(1);

            assertThatThrownBy(() -> processEngineTaskService.deleteTask(caseTasks.get(0).getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("The task cannot be deleted")
                .hasMessageContaining("running case");


            cmmnTaskService.complete(caseTasks.get(0).getId());

            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNull(dbCaseInstance);

            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
    
    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/oneProcessTask.cmmn", "org/flowable/cmmn/test/oneServiceTask.cmmn"})
    public void testCaseHierarchyResolvement() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/oneCaseTaskProcess.bpmn20.xml")
            .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneProcessTaskCase").start();
            assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
            HistoricVariableInstance variableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery()
                            .variableName("linkCount")
                            .singleResult();
            assertEquals(2, variableInstance.getValue());

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
    
    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/oneProcessTask.cmmn", "org/flowable/cmmn/test/oneServiceTask.cmmn"})
    public void testCaseHierarchyResolvementWithUserTask() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/userTaskAndCaseTaskProcess.bpmn20.xml")
            .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneProcessTaskCase").start();
            Task task = processEngineTaskService.createTaskQuery().taskDefinitionKey("userTask").singleResult();
            processEngineTaskService.complete(task.getId());
            
            assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
            HistoricVariableInstance variableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery()
                            .variableName("linkCount")
                            .singleResult();
            assertEquals(2, variableInstance.getValue());

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn"})
    public void testCaseTaskIdVariableName() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/caseTaskProcess.bpmn20.xml")
            .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask");
            processEngineTaskService.complete(processEngineTaskService.createTaskQuery().singleResult().getId());
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertEquals(caseInstance.getId(), processEngineRuntimeService.getVariable(processInstance.getId(), "myCaseInstanceId"));

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn"})
    public void testCaseTaskIdVariableNameExpression() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithIdNameExpressions.bpmn20.xml")
            .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                .processDefinitionKey("caseTask")
                .variable("counter", 1)
                .start();
            processEngineTaskService.complete(processEngineTaskService.createTaskQuery().singleResult().getId());
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertEquals(caseInstance.getId(), processEngineRuntimeService.getVariable(processInstance.getId(), "myVariable-1"));

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn"})
    public void testCaseTaskWithTerminateEndEvent() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTerminateEndEventOnDifferentExecution.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateByEndEvent");
            Task task = processEngineTaskService.createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .singleResult();
            Assumptions.assumeThat(task).isNotNull();
            Assumptions.assumeThat(task.getTaskDefinitionKey()).isEqualTo("formTask1");

            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assumptions.assumeThat(numberOfActiveCaseInstances).isEqualTo(1);

            // Act
            processEngineTaskService.complete(task.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assertions.assertThat(numberOfActiveCaseInstancesAfterCompletion).isEqualTo(0);
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn"})
    public void testCaseTaskWithTwoCaseTasksWithTerminateEndEvent() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTwoParallelCasesAndWithTerminateEndEventOnDifferentExecution.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateByEndEvent");
            Task task = processEngineTaskService.createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .singleResult();
            Assumptions.assumeThat(task).isNotNull();
            Assumptions.assumeThat(task.getTaskDefinitionKey()).isEqualTo("formTask1");

            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assumptions.assumeThat(numberOfActiveCaseInstances).isEqualTo(2);

            // Act
            processEngineTaskService.complete(task.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assertions.assertThat(numberOfActiveCaseInstancesAfterCompletion).isEqualTo(0);
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn"})
    public void testCaseTaskWithTerminatingSignalBoundaryEvent() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTerminatingSignalBoundaryEvent.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateBySignalTestCase");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assumptions.assumeThat(numberOfActiveCaseInstances).isEqualTo(1);

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assertions.assertThat(numberOfActiveCaseInstancesAfterCompletion).isEqualTo(0);
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn"})
    public void testCaseTasksInSubProcessWithTerminatingSignalBoundaryEvent() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTasksInSubProcessWithTerminatingSignalBoundaryEvent.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateTwoCasesWithinSubprocessBySignalEvent");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assumptions.assumeThat(numberOfActiveCaseInstances).isEqualTo(2);

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assertions.assertThat(numberOfActiveCaseInstancesAfterCompletion).isEqualTo(0);
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn"})
    public void testCaseTasksWithTerminatingSignalBoundaryEventOnOnlyOneOfTwoCaseTask() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTerminatingSignalBoundaryEventOnOnlyOneOfTwoCaseTask.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateOneOfTwoCasesBySignalTestCase");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assumptions.assumeThat(numberOfActiveCaseInstances).isEqualTo(2);

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            Assertions.assertThat(numberOfActiveCaseInstancesAfterCompletion).isEqualTo(1);
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .singleResult();
            cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

}
