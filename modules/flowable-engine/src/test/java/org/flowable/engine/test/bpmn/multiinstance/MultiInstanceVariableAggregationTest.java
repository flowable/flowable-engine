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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.VariableAggregator;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.JsonType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class MultiInstanceVariableAggregationTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testParallelMultiInstanceUserTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                    + "{ },"
                    + "{ },"
                    + "{ }"
                    + "]");

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(3);

        taskService.setAssignee(tasks.get(0).getId(), "userOne");
        taskService.setAssignee(tasks.get(1).getId(), "userTwo");
        taskService.setAssignee(tasks.get(2).getId(), "userThree");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne' },"
                    + "{ userId: 'userTwo' },"
                    + "{ userId: 'userThree' }"
                    + "]");

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                    + "{ userId: 'userTwo' },"
                    + "{ userId: 'userThree' }"
                    + "]");

        assertVariablesNotVisibleForProcessInstance(processInstance);

        variables.put("approved", true);
        variables.put("description", "description task 1");
        taskService.complete(tasks.get(1).getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                    + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                    + "{ userId: 'userThree' }"
                    + "]");

        variables.put("approved", false);
        variables.put("description", "description task 2");
        taskService.complete(tasks.get(2).getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

       reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo(
                "["
                + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                + "]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTaskWithBoundaryEvent.bpmn20.xml")
    public void testParallelMultiInstanceUserTaskWithBoundaryEventCancel() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                    + "{ },"
                    + "{ },"
                    + "{ }"
                    + "]");

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(3);

        taskService.setAssignee(tasks.get(0).getId(), "userOne");
        taskService.setAssignee(tasks.get(1).getId(), "userTwo");
        taskService.setAssignee(tasks.get(2).getId(), "userThree");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne' },"
                    + "{ userId: 'userTwo' },"
                    + "{ userId: 'userThree' }"
                    + "]");

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                    + "{ userId: 'userTwo' },"
                    + "{ userId: 'userThree' }"
                    + "]");

        assertVariablesNotVisibleForProcessInstance(processInstance);

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

        runtimeService.messageEventReceived("Abort", eventSubscription.getExecutionId());

        Task afterBoundary = taskService.createTaskQuery().singleResult();
        assertThat(afterBoundary).isNotNull();
        assertThat(afterBoundary.getTaskDefinitionKey()).isEqualTo("afterBoundary");

        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTaskWithBoundaryEvent.bpmn20.xml")
    public void testParallelMultiInstanceUserTaskWithBoundaryEventComplete() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .variable("nrOfLoops", 3)
                .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ },"
                        + "{ },"
                        + "{ }"
                        + "]");

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(3);

        taskService.setAssignee(tasks.get(0).getId(), "userOne");
        taskService.setAssignee(tasks.get(1).getId(), "userTwo");
        taskService.setAssignee(tasks.get(2).getId(), "userThree");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo(
                        "["
                                + "{ userId: 'userOne' },"
                                + "{ userId: 'userTwo' },"
                                + "{ userId: 'userThree' }"
                                + "]");

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo(
                        "["
                                + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                                + "{ userId: 'userTwo' },"
                                + "{ userId: 'userThree' }"
                                + "]");

        assertVariablesNotVisibleForProcessInstance(processInstance);

        variables.put("approved", true);
        variables.put("description", "description task 1");
        taskService.complete(tasks.get(1).getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo(
                        "["
                                + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                                + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                                + "{ userId: 'userThree' }"
                                + "]");

        variables.put("approved", false);
        variables.put("description", "description task 2");
        taskService.complete(tasks.get(2).getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo(
                        "["
                                + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                                + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                                + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                                + "]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTask.bpmn20.xml")
    public void testParallelMultiInstanceUserTaskWithZeroCardinality() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 0)
            .start();

        Task taskAfterMi = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterMi.getTaskDefinitionKey()).isEqualTo("afterMiTasks");

        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
        assertThatJson(reviewsVarInstance.getValue()).isEqualTo("[]");
    }

    @Test
    @Deployment
    public void testSequentialMultiInstanceUserTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 4)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                    + "{ }"
                    + "]");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "a");
        taskService.setAssignee(task.getId(), "userOne");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                + "{ userId: 'userOne' }"
                + "]");

        taskService.complete(task.getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                + "{ userId: 'userOne', approved : false, description : 'a' },"
                + "{ }"
                + "]");

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

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                + "{ userId: 'userOne', approved : false, description : 'a' },"
                + "{ userId: 'userTwo', approved : false, description : 'b' },"
                + "{ userId: 'userThree', approved : true, description : 'c' },"
                + "{ }"
                + "]");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("approved", true);
        variables.put("description", "d");
        taskService.setAssignee(task.getId(), "userFour");
        taskService.complete(task.getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : false, description : 'a' },"
                    + "{ userId: 'userTwo', approved : false, description : 'b' },"
                    + "{ userId: 'userThree', approved : true, description : 'c' },"
                    + "{ userId: 'userFour', approved : true, description : 'd' }"
                    + "]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testSequentialMultiInstanceUserTaskWithBoundaryEvent.bpmn20.xml")
    public void testSequentialMultiInstanceUserTaskWithBoundaryEventCancel() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 4)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                    + "{ }"
                    + "]");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "a");
        taskService.setAssignee(task.getId(), "userOne");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                + "{ userId: 'userOne' }"
                + "]");

        taskService.complete(task.getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                + "{ userId: 'userOne', approved : false, description : 'a' },"
                + "{ }"
                + "]");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

        runtimeService.messageEventReceived("Abort", eventSubscription.getExecutionId());

        assertVariablesNotVisibleForProcessInstance(processInstance);

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testSequentialMultiInstanceUserTaskWithBoundaryEvent.bpmn20.xml")
    public void testSequentialMultiInstanceUserTaskWithBoundaryEventComplete() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 4)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                    + "{ }"
                    + "]");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "a");
        taskService.setAssignee(task.getId(), "userOne");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                + "{ userId: 'userOne' }"
                + "]");

        taskService.complete(task.getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                + "{ userId: 'userOne', approved : false, description : 'a' },"
                + "{ }"
                + "]");

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

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                + "{ userId: 'userOne', approved : false, description : 'a' },"
                + "{ userId: 'userTwo', approved : false, description : 'b' },"
                + "{ userId: 'userThree', approved : true, description : 'c' },"
                + "{ }"
                + "]");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("approved", true);
        variables.put("description", "d");
        taskService.setAssignee(task.getId(), "userFour");
        taskService.complete(task.getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : false, description : 'a' },"
                    + "{ userId: 'userTwo', approved : false, description : 'b' },"
                    + "{ userId: 'userThree', approved : true, description : 'c' },"
                    + "{ userId: 'userFour', approved : true, description : 'd' }"
                    + "]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testSequentialMultiInstanceUserTask.bpmn20.xml")
    public void testSequentialMultiInstanceUserTaskWithZeroCardinality() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 0)
            .start();

        Task taskAfterMi = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterMi.getTaskDefinitionKey()).isEqualTo("afterMiTasks");

        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
        assertThatJson(reviewsVarInstance.getValue()).isEqualTo("[]");
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

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ },"
                        + "{ },"
                        + "{ },"
                        + "{ }"
                        + "]");

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

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ approved : true },"
                        + "{ approved : false },"
                        + "{ approved : true },"
                        + "{ approved : false }"
                        + "]");

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

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ approved : true, description : 'description task 0' },"
                        + "{ approved : false, description : 'description task 1' },"
                        + "{ approved : true, description : 'description task 2' },"
                        + "{ approved : false, description : 'description task 3' }"
                        + "]");

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

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ score: 10, approved : true, description : 'description task 0' },"
                    + "{ score: 11, approved : false, description : 'description task 1' },"
                    + "{ score: 12, approved : true, description : 'description task 2' },"
                    + "{ score: 13, approved : false, description : 'description task 3' }"
                    + "]]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
    }

    @Test
    @Deployment
    public void testSequentialMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ }"
                        + "]");

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

            if (i == 0) {
                reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

                assertThatJson(reviews)
                        .isEqualTo("["
                                + "{ approved : true, description : 'description task 0' }"
                                + "]");
            } else if (i == 1) {
                reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

                assertThatJson(reviews)
                        .isEqualTo("["
                                + "{ score: 10, approved : true, description : 'description task 0' },"
                                + "{ approved : false, description : 'description task 1' }"
                                + "]");
            }

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

        reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ score: 10, approved : true, description : 'description task 0' },"
                    + "{ score: 11, approved : false, description : 'description task 1' },"
                    + "{ score: 12, approved : true, description : 'description task 2' }"
                    + "]]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

    }

    @Test
    @Deployment
    public void testParallelMultiInstanceSubProcessWithParallelMultiInstanceUserTask() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .variable("nrOfLoops", 2)
                .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ first: [{ task: 0 }, { task: 1 }] },"
                        + "{ first: [{ task: 10}, { task: 11 }] }"
                        + "]");

        // User task 'task one': sets approved and description variable
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByCategory().asc()
                .list();
        assertThat(tasks).extracting(Task::getName).containsOnly("task one");
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            variables.put("description", "description task " + i);
            taskService.complete(task.getId(), variables);
        }

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "  {"
                        + "    first: ["
                        + "      { task: 0, approved : true, description : 'description task 0' },"
                        + "      { task: 1, approved : false, description : 'description task 1'}"
                        + "    ],"
                        + "    second: ["
                        + "      { score: 0 },"
                        + "      { score: 0 }"
                        + "    ]"
                        + "  },"
                        + "  {"
                        + "    first: ["
                        + "      { task: 10, approved : true, description : 'description task 2' },"
                        + "      { task: 11, approved : false, description : 'description task 3' }"
                        + "    ],"
                        + "    second: ["
                        + "      { score: 0 },"
                        + "      { score: 0 }"
                        + "    ]"
                        + "  }"
                        + "]");

        assertVariablesNotVisibleForProcessInstance(processInstance);

        // User task 'task two': sets myScore variable
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByCategory().asc()
                .list();
        assertThat(tasks).extracting(Task::getName).containsOnly("task two");
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("myScore", i + 10);
            taskService.complete(task.getId(), variables);
        }

        assertVariablesNotVisibleForProcessInstance(processInstance);

        Task taskAfterMi = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterMi.getName()).isEqualTo("Task after Mi");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "  {"
                        + "    first: ["
                        + "      { task: 0, approved : true, description : 'description task 0' },"
                        + "      { task: 1, approved : false, description : 'description task 1'}"
                        + "    ],"
                        + "    second: ["
                        + "      { score: 20 },"
                        + "      { score: 22 }"
                        + "    ]"
                        + "  },"
                        + "  {"
                        + "    first: ["
                        + "      { task: 10, approved : true, description : 'description task 2' },"
                        + "      { task: 11, approved : false, description : 'description task 3' }"
                        + "    ],"
                        + "    second: ["
                        + "      { score: 24 },"
                        + "      { score: 26 }"
                        + "    ]"
                        + "  }"
                        + "]");


        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
    }

    @Test
    @Deployment
    public void testParallelMultiInstanceSubProcessWithParallelMultiInstanceUserTaskWithCustomAggregator() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .variable("nrOfLoops", 2)
                .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ task: 0 },"
                        + "{ task: 1 },"
                        + "{ task: 10 },"
                        + "{ task: 11 }"
                        + "]");

        // User task 'task one': sets approved and description variable
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByCategory().asc()
                .list();
        assertThat(tasks).extracting(Task::getName).containsOnly("task one");
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            variables.put("description", "description task " + i);
            taskService.complete(task.getId(), variables);
        }

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ task: 0, approved : true, description : 'description task 0', score: 0 },"
                        + "{ task: 1, approved : false, description : 'description task 1', score: 0 },"
                        + "{ task: 10, approved : true, description : 'description task 2', score: 0 },"
                        + "{ task: 11, approved : false, description : 'description task 3', score: 0 }"
                        + "]");

        assertVariablesNotVisibleForProcessInstance(processInstance);

        // User task 'task two': sets myScore variable
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByCategory().asc()
                .list();
        assertThat(tasks).extracting(Task::getName).containsOnly("task two");
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("myScore", i + 10);
            taskService.complete(task.getId(), variables);
        }

        assertVariablesNotVisibleForProcessInstance(processInstance);

        Task taskAfterMi = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterMi.getName()).isEqualTo("Task after Mi");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ task: 0, approved : true, description : 'description task 0', score: 20 },"
                        + "{ task: 1, approved : false, description : 'description task 1', score: 22 },"
                        + "{ task: 10, approved : true, description : 'description task 2', score: 24 },"
                        + "{ task: 11, approved : false, description : 'description task 3', score: 26 }"
                        + "]");

        assertNoAggregatedVariables();

        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
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

    public static class CustomVariableAggregator implements VariableAggregator {

        @Override
        public Object aggregateSingle(DelegateExecution execution, Context context) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();

            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (VariableAggregationDefinition.Variable variable : context.getDefinition().getDefinitions()) {
                Object sourceVariable = execution.getVariable(variable.getSource());
                if (sourceVariable instanceof ArrayNode) {
                    ArrayNode sourceArrayNode = (ArrayNode) sourceVariable;
                    for (int i = 0; i < sourceArrayNode.size(); i++) {
                        JsonNode node = arrayNode.get(i);
                        JsonNode sourceNode = sourceArrayNode.get(i);
                        if (node == null) {
                            arrayNode.add(sourceNode.deepCopy());
                        } else if (node.isObject()) {
                            ObjectNode objectNode = (ObjectNode) node;
                            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = sourceNode.fields();
                            while (fieldsIterator.hasNext()) {
                                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                                objectNode.set(field.getKey(), field.getValue());
                            }
                        }
                    }
                }
            }

            return arrayNode;
        }

        @Override
        public Object aggregateMulti(DelegateExecution execution, List<? extends VariableInstance> instances, Context context) {
            ArrayNode arrayNode = CommandContextUtil.getProcessEngineConfiguration().getObjectMapper().createArrayNode();
            for (VariableInstance instance : instances) {
                arrayNode.addAll((ArrayNode) instance.getValue());
            }

            return arrayNode;
        }
    }

}
