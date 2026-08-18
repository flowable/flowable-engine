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

package org.flowable.assertj.process;

import org.assertj.core.groups.Tuple;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author martin.grofcik
 */
@FlowableTest
class HistoricProcessInstanceAssertTest {

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void isFinishedForFinishedProcessInstance(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        ProcessInstance oneTaskProcess = TestUtils.createOneTaskProcess(runtimeService);

        ProcessInstanceAssert assertThatOneTaskProcess = FlowableProcessAssertions.assertThat(oneTaskProcess);
        assertThatOneTaskProcess.inHistory().activities().extracting(HistoricActivityInstance::getActivityId).contains(
                        "theStart", "theStart-theTask", "theTask"
                );

        taskService.complete(taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult().getId());

        assertThatOneTaskProcess.inHistory().isFinished()
            .activities().extracting(HistoricActivityInstance::getActivityId).contains(
                "theStart", "theStart-theTask", "theTask", "theTask-theEnd", "theEnd"
            );

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        FlowableProcessAssertions.assertThat(historicProcessInstance).isFinished()
                .activities().extracting(HistoricActivityInstance::getActivityId).contains(
                        "theStart", "theStart-theTask", "theTask", "theTask-theEnd", "theEnd"
                );
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void variables(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance oneTaskProcess = TestUtils.createOneTaskProcess(runtimeService);

        ProcessInstanceAssert assertThatOneTaskProcess = FlowableProcessAssertions.assertThat(oneTaskProcess);
        assertThatOneTaskProcess.as("No variable exists in the process scope.")
                .inHistory().variables().isEmpty();
        
        runtimeService.setVariable(oneTaskProcess.getId(), "testVariable", "variableValue");

        assertThatOneTaskProcess.as("Variable exists in the process scope, the variable must be present in the history.")
                .inHistory()
                .hasVariable("testVariable")
                .hasVariableWithValue("testVariable", "variableValue")
                .variables().hasSize(1).extracting("name", "value").
                containsExactly(Tuple.tuple("testVariable", "variableValue"));

        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        taskService.complete(task.getId());

        assertThatOneTaskProcess.as("Variable exists in the process scope, the variable must be present in the history.")
                .doesNotExist()
                .inHistory()
                .isFinished()
                .hasVariable("testVariable")
                .hasVariableWithValue("testVariable", "variableValue")
                .variables().hasSize(1).extracting("name", "value").
                containsExactly(Tuple.tuple("testVariable", "variableValue"));
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void hasVariable(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = TestUtils.createOneTaskProcess(runtimeService);

        ProcessInstanceAssert assertThatOneTaskProcess = FlowableProcessAssertions.assertThat(oneTaskProcess);
        assertThatOneTaskProcess.as("No variable exists in the process scope.")
                .inHistory().variables().isEmpty();

        runtimeService.setVariable(oneTaskProcess.getId(), "testVariable", "variableValue");

        assertThatOneTaskProcess.as("Variable exists in the process scope, the variable must be present in the history.")
                .inHistory().variables().hasSize(1).extracting("name", "value").
                containsExactly(Tuple.tuple("testVariable", "variableValue"));
    }

    @Test
    @Deployment(resources = "oneTask.bpmn20.xml")
    void doesNotHaveVariable(RuntimeService runtimeService) {
        ProcessInstance oneTaskProcess = TestUtils.createOneTaskProcess(runtimeService);

        ProcessInstanceAssert assertThatOneTaskProcess = FlowableProcessAssertions.assertThat(oneTaskProcess);
        assertThatOneTaskProcess.as("No variable exists in the process scope.")
                .inHistory().doesNotHaveVariable("nonExistingVariable");

        runtimeService.setVariable(oneTaskProcess.getId(), "testVariable", "variableValue");

        assertThatOneTaskProcess.as("Variable exists in the process scope, the variable must be present in the history.")
                .inHistory().doesNotHaveVariable("nonExistingVariable")
                .hasVariable("testVariable");

        assertThatThrownBy(() -> assertThatOneTaskProcess.inHistory().doesNotHaveVariable("testVariable"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected process instance <oneTaskProcess, "+oneTaskProcess.getId()+"> does not have variable <testVariable> but variable exists in history.");
    }

}