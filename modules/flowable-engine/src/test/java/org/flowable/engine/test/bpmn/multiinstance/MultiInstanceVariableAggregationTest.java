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
package org.flowable.engine.test.bpmn.multiinstance;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Joram Barrez
 */
public class MultiInstanceVariableAggregationTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testParallelMultiInstanceUserTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(3);

        taskService.setAssignee(tasks.get(0).getId(), "userOne");
        taskService.setAssignee(tasks.get(1).getId(), "userTwo");
        taskService.setAssignee(tasks.get(2).getId(), "userThree");

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        variables.put("approved", true);
        variables.put("description", "description task 1");
        taskService.complete(tasks.get(1).getId(), variables);

        variables.put("approved", false);
        variables.put("description", "description task 2");
        taskService.complete(tasks.get(2).getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        ArrayNode reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                + "]]");

        assertNoAggregatedVariables();
    }

    @Test
    @Deployment
    public void testSequentialMultiInstanceUserTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 4)
            .start();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "a");
        taskService.setAssignee(task.getId(), "userOne");
        taskService.complete(task.getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("approved", false);
        variables.put("description", "b");
        taskService.setAssignee(task.getId(), "userTwo");
        taskService.complete(task.getId(), variables);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("approved", true);
        variables.put("description", "c");
        taskService.setAssignee(task.getId(), "userThree");
        taskService.complete(task.getId(), variables);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("approved", true);
        variables.put("description", "d");
        taskService.setAssignee(task.getId(), "userFour");
        taskService.complete(task.getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        ArrayNode reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : false, description : 'a' },"
                    + "{ userId: 'userTwo', approved : false, description : 'b' },"
                    + "{ userId: 'userThree', approved : true, description : 'c' },"
                    + "{ userId: 'userFour', approved : true, description : 'd' }"
                    + "]]");

        assertNoAggregatedVariables();
    }

    @Test
    @Deployment
    public void testDeleteProcessInstanceBeforeAggregationFinished() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables);

        runtimeService.deleteProcessInstance(processInstance.getId(), null);

        // The aggregated variables should be deleted now too

        List<VariableInstanceEntity> variableInstanceEntities = managementService.executeCommand(new Command<List<VariableInstanceEntity>>() {

            @Override
            public List<VariableInstanceEntity> execute(CommandContext commandContext) {
                return CommandContextUtil.getVariableService().createInternalVariableInstanceQuery().list();
            }
        });
        assertThat(variableInstanceEntities).isEmpty();

    }

    @Test
    @Deployment
    public void testParallelMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 4)
            .start();

        // User task 'task one':  sets approved variable
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByCategory().asc()
                .list();
        assertThat(tasks).extracting(Task::getName).containsOnly("task one");
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            taskService.complete(task.getId(), variables);
        }

        assertVariablesNotVisibleForProcessInstance(processInstance);

        // User task 'task two': sets description variable
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByCategory().asc()
                .list();
        assertThat(tasks).extracting(Task::getName).containsOnly("task two");
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("description", "description task " + i);
            taskService.complete(task.getId(), variables);
        }

        assertVariablesNotVisibleForProcessInstance(processInstance);


        // User task 'task three': updates description and adds score
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByCategory().asc()
                .list();
        assertThat(tasks).extracting(Task::getName).containsOnly("task three");
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("myScore", i + 10);
//            variables.put("description", "updated description task " + i);
            taskService.complete(task.getId(), variables);
        }

        assertVariablesNotVisibleForProcessInstance(processInstance);

        Task taskAfterMi = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterMi.getName()).isEqualTo("Task after Mi");

        ArrayNode reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ score: 10, approved : true, description : 'description task 0' },"
                    + "{ score: 11, approved : false, description : 'description task 1' },"
                    + "{ score: 12, approved : true, description : 'description task 2' },"
                    + "{ score: 13, approved : false, description : 'description task 3' }"
                    + "]]");

        assertNoAggregatedVariables();
    }

    @Test
    @Deployment
    public void testSequentialMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        for (int i = 0; i < 3; i++) {

            // User task 'task one':  sets approved variable
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("task one");

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            taskService.complete(task.getId(), variables);
            assertVariablesNotVisibleForProcessInstance(processInstance);

            // User task 'task two': sets description variable
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("task two");

            variables = new HashMap<>();
            variables.put("description", "description task " + i);
            taskService.complete(task.getId(), variables);
            assertVariablesNotVisibleForProcessInstance(processInstance);

            // User task 'task three': updates description and adds score
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("task three");

            variables = new HashMap<>();
            variables.put("myScore", i + 10);
            //            variables.put("description", "updated description task " + i);
            taskService.complete(task.getId(), variables);
            assertVariablesNotVisibleForProcessInstance(processInstance);
        }

        Task taskAfterMi = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterMi.getName()).isEqualTo("Task after Mi");

        ArrayNode reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ score: 10, approved : true, description : 'description task 0' },"
                    + "{ score: 11, approved : false, description : 'description task 1' },"
                    + "{ score: 12, approved : true, description : 'description task 2' }"
                    + "]]");

        assertNoAggregatedVariables();

    }

    protected void assertVariablesNotVisibleForProcessInstance(ProcessInstance processInstance) {

        assertThat(runtimeService.getVariable(processInstance.getId(), "nrOfLoops")).isNotNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "approved")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "description")).isNull();
    }

    protected void assertNoAggregatedVariables() {
        List<VariableInstanceEntity> variableInstanceEntities = managementService.executeCommand(commandContext -> CommandContextUtil.getVariableService()
                .createInternalVariableInstanceQuery()
                .scopeType(ScopeTypes.BPMN_VARIABLE_AGGREGATION)
                .list());
        assertThat(variableInstanceEntities).isEmpty();

    }

}
