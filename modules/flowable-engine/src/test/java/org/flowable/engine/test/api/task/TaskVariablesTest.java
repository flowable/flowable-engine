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

package org.flowable.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 */
public class TaskVariablesTest extends PluggableFlowableTestCase {

    @Test
    public void testStandaloneTaskVariables() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName("gonzoTask");
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.setVariable(taskId, "instrument", "trumpet");
        assertThat(taskService.getVariable(taskId, "instrument")).isEqualTo("trumpet");

        taskService.deleteTask(taskId, true);
    }

    @Test
    @Deployment
    public void testTaskExecutionVariables() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
        String taskId = taskService.createTaskQuery().singleResult().getId();

        assertThat(runtimeService.getVariables(processInstanceId)).isEmpty();
        assertThat(taskService.getVariables(taskId)).isEmpty();
        assertThat(runtimeService.getVariablesLocal(processInstanceId)).isEmpty();
        assertThat(taskService.getVariablesLocal(taskId)).isEmpty();

        runtimeService.setVariable(processInstanceId, "instrument", "trumpet");

        assertThat(taskService.getVariablesLocal(taskId)).isEmpty();
        assertThat(runtimeService.getVariables(processInstanceId))
                .containsOnly(
                        entry("instrument", "trumpet")
                );
        assertThat(taskService.getVariables(taskId))
                .containsOnly(
                        entry("instrument", "trumpet")
                );
        assertThat(runtimeService.getVariablesLocal(processInstanceId))
                .containsOnly(
                        entry("instrument", "trumpet")
                );

        taskService.setVariable(taskId, "player", "gonzo");
        assertThat(taskService.hasVariable(taskId, "player")).isTrue();
        assertThat(taskService.hasVariableLocal(taskId, "budget")).isFalse();

        assertThat(runtimeService.getVariables(processInstanceId))
                .containsOnly(
                        entry("player", "gonzo"),
                        entry("instrument", "trumpet")
                );
        assertThat(taskService.getVariables(taskId))
                .containsOnly(
                        entry("player", "gonzo"),
                        entry("instrument", "trumpet")
                );
        assertThat(runtimeService.getVariablesLocal(processInstanceId))
                .containsOnly(
                        entry("player", "gonzo"),
                        entry("instrument", "trumpet")
                );

        taskService.setVariableLocal(taskId, "budget", "unlimited");
        assertThat(taskService.hasVariableLocal(taskId, "budget")).isTrue();
        assertThat(taskService.hasVariable(taskId, "budget")).isTrue();

        assertThat(taskService.getVariablesLocal(taskId))
                .containsOnly(
                        entry("budget", "unlimited")
                );
        assertThat(taskService.getVariables(taskId))
                .containsOnly(
                        entry("budget", "unlimited"),
                        entry("player", "gonzo"),
                        entry("instrument", "trumpet")
                );

        assertThat(runtimeService.getVariables(processInstanceId))
                .containsOnly(
                        entry("player", "gonzo"),
                        entry("instrument", "trumpet")
                );
        assertThat(runtimeService.getVariablesLocal(processInstanceId))
                .containsOnly(
                        entry("player", "gonzo"),
                        entry("instrument", "trumpet")
                );
    }

    @Test
    public void testSerializableTaskVariable() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName("MyTask");
        taskService.saveTask(task);

        // Set variable
        Map<String, Object> vars = new HashMap<>();
        MyVariable myVariable = new MyVariable("Hello world");
        vars.put("theVar", myVariable);
        taskService.setVariables(task.getId(), vars);

        // Fetch variable
        MyVariable variable = (MyVariable) taskService.getVariable(task.getId(), "theVar");
        assertThat(variable.getValue()).isEqualTo("Hello world");

        // Cleanup
        taskService.deleteTask(task.getId(), true);
    }

    @Test
    @Deployment
    public void testGetVariablesLocalByTaskIds() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("twoTaskProcess");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoTaskProcess");
        List<org.flowable.task.api.Task> taskList1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).list();
        List<org.flowable.task.api.Task> taskList2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).list();

        // org.flowable.task.service.Task local variables
        for (org.flowable.task.api.Task task : taskList1) {
            if ("usertask1".equals(task.getTaskDefinitionKey())) {
                taskService.setVariableLocal(task.getId(), "taskVar1", "sayHello1");
            } else {
                taskService.setVariableLocal(task.getId(), "taskVar2", "sayHello2");
            }
            // Execution variables
            taskService.setVariable(task.getId(), "executionVar1", "helloWorld1");
        }
        // org.flowable.task.service.Task local variables
        for (org.flowable.task.api.Task task : taskList2) {
            if ("usertask1".equals(task.getTaskDefinitionKey())) {
                taskService.setVariableLocal(task.getId(), "taskVar3", "sayHello3");
            } else {
                taskService.setVariableLocal(task.getId(), "taskVar4", "sayHello4");
            }
            // Execution variables
            taskService.setVariable(task.getId(), "executionVar2", "helloWorld2");
        }

        // only 1 process
        Set<String> taskIds = new HashSet<>();
        taskIds.add(taskList1.get(0).getId());
        taskIds.add(taskList1.get(1).getId());
        List<VariableInstance> variables = taskService.getVariableInstancesLocalByTaskIds(taskIds);
        assertThat(variables).hasSize(2);
        checkVariable(taskList1.get(0).getId(), "taskVar1", "sayHello1", variables);
        checkVariable(taskList1.get(1).getId(), "taskVar2", "sayHello2", variables);

        // 2 process
        taskIds = new HashSet<>();
        taskIds.add(taskList1.get(0).getId());
        taskIds.add(taskList1.get(1).getId());
        taskIds.add(taskList2.get(0).getId());
        taskIds.add(taskList2.get(1).getId());
        variables = taskService.getVariableInstancesLocalByTaskIds(taskIds);
        assertThat(variables).hasSize(4);
        checkVariable(taskList1.get(0).getId(), "taskVar1", "sayHello1", variables);
        checkVariable(taskList1.get(1).getId(), "taskVar2", "sayHello2", variables);
        checkVariable(taskList2.get(0).getId(), "taskVar3", "sayHello3", variables);
        checkVariable(taskList2.get(1).getId(), "taskVar4", "sayHello4", variables);

        // mixture 2 process
        taskIds = new HashSet<>();
        taskIds.add(taskList1.get(0).getId());
        taskIds.add(taskList2.get(1).getId());
        variables = taskService.getVariableInstancesLocalByTaskIds(taskIds);
        assertThat(variables).hasSize(2);
        checkVariable(taskList1.get(0).getId(), "taskVar1", "sayHello1", variables);
        checkVariable(taskList2.get(1).getId(), "taskVar4", "sayHello4", variables);
    }

    private void checkVariable(String taskId, String name, String value, List<VariableInstance> variables) {
        for (VariableInstance variable : variables) {
            if (taskId.equals(variable.getTaskId())) {
                assertThat(variable.getName()).isEqualTo(name);
                assertThat(variable.getValue()).isEqualTo(value);
                return;
            }
        }
        fail("checkVariable failed");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/task/TaskVariablesTest.testTaskExecutionVariables.bpmn20.xml"
    })
    public void testGetVariablesLocalByTaskIdsForSerializableType() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
        String taskId = taskService.createTaskQuery().singleResult().getId();

        StringBuilder sb = new StringBuilder("a");
        for (int i = 0; i < 4001; i++) {
            sb.append("a");
        }
        String serializableTypeVar = sb.toString();

        taskService.setVariableLocal(taskId, "taskVar1", serializableTypeVar);

        // only 1 process
        Set<String> taskIds = new HashSet<>();
        taskIds.add(taskId);
        List<VariableInstance> variables = taskService.getVariableInstancesLocalByTaskIds(taskIds);
        assertThat(variables.get(0).getValue()).isEqualTo(serializableTypeVar);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/runtime/variableScope.bpmn20.xml"
    })
    public void testGetVariablesLocalByTaskIdsForScope() {
        Map<String, Object> processVars = new HashMap<>();
        processVars.put("processVar", "processVar");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableScopeProcess", processVars);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        for (Execution execution : executions) {
            if (!processInstance.getId().equals(execution.getId())) {
                runtimeService.setVariableLocal(execution.getId(), "executionVar", "executionVar");
            }
        }

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Set<String> taskIds = new HashSet<>();
        for (org.flowable.task.api.Task task : tasks) {
            taskService.setVariableLocal(task.getId(), "taskVar", "taskVar");
            taskIds.add(task.getId());
        }

        List<VariableInstance> variableInstances = taskService.getVariableInstancesLocalByTaskIds(taskIds);
        assertThat(variableInstances).hasSize(2);
        assertThat(variableInstances.get(0).getName()).isEqualTo("taskVar");
        assertThat(variableInstances.get(0).getValue()).isEqualTo("taskVar");
        assertThat(variableInstances.get(1).getName()).isEqualTo("taskVar");
        assertThat(variableInstances.get(1).getValue()).isEqualTo("taskVar");
    }

    public static class MyVariable implements Serializable {

        private String value;

        public MyVariable(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

}
