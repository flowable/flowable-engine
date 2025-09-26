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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.variable.VariableAggregator;
import org.flowable.engine.delegate.variable.VariableAggregatorContext;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.variable.BpmnAggregatedVariableType;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.api.variables.VariablesTest;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.JsonType;
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
                    + "{ userId: null },"
                    + "{ userId: null },"
                    + "{ userId: null }"
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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                            + "{ userId: 'userTwo' },"
                            + "{ userId: 'userThree' }"
                            + "]");
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

    }

    @Test
    @Deployment
    public void testParallelMultiInstanceUserTaskWithoutCreateOverview() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        VariableInstance reviews = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviews).isNull();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(3);

        taskService.setAssignee(tasks.get(0).getId(), "userOne");
        taskService.setAssignee(tasks.get(1).getId(), "userTwo");
        taskService.setAssignee(tasks.get(2).getId(), "userThree");

        reviews = runtimeService.getVariableInstance(processInstance.getId(), "reviews");

        assertThat(reviews).isNull();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables);

        reviews = runtimeService.getVariableInstance(processInstance.getId(), "reviews");

        assertThat(reviews).isNull();

        assertVariablesNotVisibleForProcessInstance(processInstance);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }

        variables.put("approved", true);
        variables.put("description", "description task 1");
        taskService.complete(tasks.get(1).getId(), variables);

        variables.put("approved", false);
        variables.put("description", "description task 2");
        taskService.complete(tasks.get(2).getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        reviews = runtimeService.getVariableInstance(processInstance.getId(), "reviews");

        assertThat(reviews).isNotNull();
        assertThat(reviews.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
        assertThatJson(reviews.getValue())
            .isEqualTo(
                "["
                + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                + "]");

        assertNoAggregatedVariables();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }
    }

    @Test
    @Deployment
    public void testParallelMultiInstanceUserTaskStoreAsTransient() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        VariableInstance reviews = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviews).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(3);

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

        reviews = runtimeService.getVariableInstance(processInstance.getId(), "reviews");

        assertThat(reviews).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "firstDescription")).isEqualTo("description task 0");

        assertNoAggregatedVariables();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();

            HistoricVariableInstance historicFirstDescription = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("firstDescription")
                    .singleResult();
            assertThat(historicFirstDescription).isNotNull();
            assertThat(historicFirstDescription.getValue()).isEqualTo("description task 0");
        }
    }

    @Test
    @Deployment
    public void testParallelMultiInstanceUserTaskVariableTypes() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .variable("nrOfLoops", 4)
                .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        Map<String, Object> variables = new HashMap<>();
        variables.put("description", "Description for task with LocalDate");
        variables.put("score", 10);
        variables.put("passed", true);
        variables.put("location", "Springfield");
        variables.put("startTime", LocalDate.of(2020, Month.DECEMBER, 8));
        taskService.complete(tasks.get(0).getId(), variables);

        variables = new HashMap<>();
        variables.put("description", "Description for task with LocalDateTime");
        variables.put("score", 60.55);
        variables.put("passed", false);
        variables.put("location", null);
        variables.put("startTime", LocalDate.of(2020, Month.DECEMBER, 8).atTime(10, 20, 30));
        taskService.complete(tasks.get(1).getId(), variables);

        variables = new HashMap<>();
        variables.put("description", "Description for task with Instant");
        variables.put("score", (short) 100);
        variables.put("passed", true);
        variables.put("location", "Zurich");
        variables.put("startTime", Instant.parse("2020-12-08T08:20:45.585Z"));
        taskService.complete(tasks.get(2).getId(), variables);

        variables = new HashMap<>();
        variables.put("description", "Description for task with Date");
        variables.put("score", 1234L);
        variables.put("passed", true);
        variables.put("location", "Test Valley");
        variables.put("startTime", Instant.parse("2020-12-12T12:24:15.155Z"));
        taskService.complete(tasks.get(3).getId(), variables);

        assertNoAggregatedVariables();
        assertThatJson(runtimeService.getVariable(processInstance.getId(), "results"))
                .isEqualTo("["
                        + "  {"
                        + "    description: 'Description for task with LocalDate',"
                        + "    score: 10,"
                        + "    passed: true,"
                        + "    location: 'Springfield',"
                        + "    startTime: '2020-12-08'"
                        + "  },"
                        + "  {"
                        + "    description: 'Description for task with LocalDateTime',"
                        + "    score: 60.55,"
                        + "    passed: false,"
                        + "    location: null,"
                        + "    startTime: '2020-12-08T10:20:30'"
                        + "  },"
                        + "  {"
                        + "    description: 'Description for task with Instant',"
                        + "    score: 100,"
                        + "    passed: true,"
                        + "    location: 'Zurich',"
                        + "    startTime: '2020-12-08T08:20:45.585Z'"
                        + "  },"
                        + "  {"
                        + "    description: 'Description for task with Date',"
                        + "    score: 1234,"
                        + "    passed: true,"
                        + "    location: 'Test Valley',"
                        + "    startTime: '2020-12-12T12:24:15.155Z'"
                        + "  }"
                        + "]");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicResults = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("results")
                    .singleResult();
            assertThat(historicResults).isNotNull();
            assertThatJson(historicResults.getValue())
                    .isEqualTo("["
                            + "  {"
                            + "    description: 'Description for task with LocalDate',"
                            + "    score: 10,"
                            + "    passed: true,"
                            + "    location: 'Springfield',"
                            + "    startTime: '2020-12-08'"
                            + "  },"
                            + "  {"
                            + "    description: 'Description for task with LocalDateTime',"
                            + "    score: 60.55,"
                            + "    passed: false,"
                            + "    location: null,"
                            + "    startTime: '2020-12-08T10:20:30'"
                            + "  },"
                            + "  {"
                            + "    description: 'Description for task with Instant',"
                            + "    score: 100,"
                            + "    passed: true,"
                            + "    location: 'Zurich',"
                            + "    startTime: '2020-12-08T08:20:45.585Z'"
                            + "  },"
                            + "  {"
                            + "    description: 'Description for task with Date',"
                            + "    score: 1234,"
                            + "    passed: true,"
                            + "    location: 'Test Valley',"
                            + "    startTime: '2020-12-12T12:24:15.155Z'"
                            + "  }"
                            + "]");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTaskVariableTypes.bpmn20.xml")
    public void testParallelMultiInstanceUserTaskUnsupportedVariableTypes() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .variable("nrOfLoops", 2)
                .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(2);

        Map<String, Object> variables = new HashMap<>();
        variables.put("description", "Description for task with LocalDate");
        variables.put("score", 10);
        variables.put("passed", true);
        variables.put("location", "Springfield");
        variables.put("startTime", new VariablesTest.TestSerializableVariable(19));

        assertThatThrownBy(() -> taskService.complete(tasks.get(0).getId(), variables))
            .isExactlyInstanceOf(FlowableException.class)
            .hasMessageContaining("Cannot aggregate variable: ")
            .hasMessageContaining("startTime");
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
                    + "{ userId: null },"
                    + "{ userId: null },"
                    + "{ userId: null }"
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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                            + "{ userId: 'userTwo' },"
                            + "{ userId: 'userThree' }"
                            + "]");
        }

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

        runtimeService.messageEventReceived("Abort", eventSubscription.getExecutionId());

        Task afterBoundary = taskService.createTaskQuery().singleResult();
        assertThat(afterBoundary).isNotNull();
        assertThat(afterBoundary.getTaskDefinitionKey()).isEqualTo("afterBoundary");

        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTaskWithBoundaryEvent.bpmn20.xml")
    public void testParallelMultiInstanceUserTaskWithBoundaryEventCancelAndPayload() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                    + "{ userId: null },"
                    + "{ userId: null },"
                    + "{ userId: null }"
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

        runtimeService.messageEventReceived("Abort", eventSubscription.getExecutionId(), Collections.singletonMap("abortReason", "test"));

        Task afterBoundary = taskService.createTaskQuery().singleResult();
        assertThat(afterBoundary).isNotNull();
        assertThat(afterBoundary.getTaskDefinitionKey()).isEqualTo("afterBoundary");

        assertThat(runtimeService.getVariableInstance(processInstance.getId(), "reviews")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "abortReason")).isEqualTo("test");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }
    }

    @Test
    @Deployment
    public void testParallelMultiInstanceUserTaskWithTimerBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                    + "{ userId: null },"
                    + "{ userId: null },"
                    + "{ userId: null }"
                    + "]");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables()
                    .singleResult();

            assertThat(historicProcessInstance.getProcessVariables())
                    .hasEntrySatisfying("reviews", variable -> {
                        assertThat(variable).isInstanceOf(ArrayNode.class);
                        assertThatJson(variable)
                                .isEqualTo(
                                        "["
                                                + "{ userId: null },"
                                                + "{ userId: null },"
                                                + "{ userId: null }"
                                                + "]");
                    });
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables()
                    .singleResult();

            assertThat(historicProcessInstance.getProcessVariables())
                    .hasEntrySatisfying("reviews", variable -> {
                        assertThat(variable).isInstanceOf(ArrayNode.class);
                        assertThatJson(variable)
                                .isEqualTo(
                                        "["
                                                + "{ userId: 'userOne' },"
                                                + "{ userId: 'userTwo' },"
                                                + "{ userId: 'userThree' }"
                                                + "]");
                    });
        }

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        Job job = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(job.getId());

        Task afterBoundary = taskService.createTaskQuery().singleResult();
        assertThat(afterBoundary).isNotNull();
        assertThat(afterBoundary.getTaskDefinitionKey()).isEqualTo("afterBoundary");

        assertThat(runtimeService.getVariableInstance(processInstance.getId(), "reviews")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables()
                    .singleResult();

            assertThat(historicProcessInstance.getProcessVariables())
                    .doesNotContainKey("reviews");
        }
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
                        + "{ userId: null },"
                        + "{ userId: null },"
                        + "{ userId: null }"
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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTask.bpmn20.xml")
    public void testParallelMultiInstanceUserTaskWithExpressionModifyingOverviewVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .variable("nrOfLoops", 3)
                .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ userId: null },"
                        + "{ userId: null },"
                        + "{ userId: null }"
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
                .isEqualTo("["
                        + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                        + "{ userId: 'userTwo' },"
                        + "{ userId: 'userThree' }"
                        + "]");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                            + "{ userId: 'userTwo' },"
                            + "{ userId: 'userThree' }"
                            + "]");
        }

        managementService.executeCommand(commandContext -> {
            ExecutionEntity execution = processEngineConfiguration.getExecutionEntityManager().findById(processInstance.getId());
            processEngineConfiguration.getExpressionManager()
                    .createExpression("${reviews}")
                    .setValue("customReviews", execution);

            return null;
        });

        VariableInstance reviewsInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");

        assertThat(reviewsInstance.getTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        assertThatJson(reviewsInstance.getValue())
                .isEqualTo("["
                        + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                        + "{ userId: 'userTwo' },"
                        + "{ userId: 'userThree' }"
                        + "]");

        assertThatThrownBy(() -> {
            managementService.executeCommand(commandContext -> {
                ExecutionEntity execution = processEngineConfiguration.getExecutionEntityManager().findById(processInstance.getId());
                processEngineConfiguration.getExpressionManager()
                        .createExpression("${reviews[0].userId}")
                        .setValue("testUser", execution);
                return null;
            });
        }).hasMessageStartingWith("Error while evaluating expression: ${reviews[0].userId} with ProcessInstance[")
            .hasMessageContaining(" - definition 'myProcess:1:");

        reviewsInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");

        assertThat(reviewsInstance.getTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        assertThatJson(reviewsInstance.getValue())
                .isEqualTo("["
                        + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                        + "{ userId: 'userTwo' },"
                        + "{ userId: 'userThree' }"
                        + "]");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                            + "{ userId: 'userTwo' },"
                            + "{ userId: 'userThree' }"
                            + "]");
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTaskWithBoundaryEvent.bpmn20.xml")
    public void testParallelMultiInstanceUserTaskWithProcessDeletion() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .variable("nrOfLoops", 3)
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: null },"
                            + "{ userId: null },"
                            + "{ userId: null }"
                            + "]");
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne', approved : true, description : 'description task 0' },"
                            + "{ userId: 'userTwo' },"
                            + "{ userId: 'userThree' }"
                            + "]");
        }

        runtimeService.deleteProcessInstance(processInstance.getId(), "for testing");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }
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
                    + "{ userId: null }"
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
                + "{ userId: null }"
                + "]");

        assertVariablesNotVisibleForProcessInstance(processInstance);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        }

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
                + "{ userId: null }"
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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

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
                    + "{ userId: null }"
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
                + "{ userId: null }"
                + "]");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        }

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

        runtimeService.messageEventReceived("Abort", eventSubscription.getExecutionId());

        assertVariablesNotVisibleForProcessInstance(processInstance);

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "reviews");
        assertThat(reviewsVarInstance).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testSequentialMultiInstanceUserTaskWithBoundaryEvent.bpmn20.xml")
    public void testSequentialMultiInstanceUserTaskWithBoundaryEventCancelAndPayload() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 4)
            .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
            .isEqualTo("["
                    + "{ userId: null }"
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
                + "{ userId: null }"
                + "]");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        }

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

        runtimeService.messageEventReceived("Abort", eventSubscription.getExecutionId(), Collections.singletonMap("abortReason", "test"));

        assertVariablesNotVisibleForProcessInstance(processInstance);

        assertNoAggregatedVariables();
        assertThat(runtimeService.getVariableInstance(processInstance.getId(), "reviews")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "abortReason")).isEqualTo("test");
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
                    + "{ userId: null }"
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
                + "{ userId: null }"
                + "]");

        assertVariablesNotVisibleForProcessInstance(processInstance);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        }

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
                + "{ userId: null }"
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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

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

        List<VariableInstanceEntity> variableInstanceEntities = managementService.executeCommand(new Command<>() {

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

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

                if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                    HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                            .processInstanceId(processInstance.getId())
                            .variableName("reviews")
                            .singleResult();
                    assertThat(historicReviews).isNotNull();
                    assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
                }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }
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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

    }

    @Test
    @Deployment
    public void testParallelSubProcessWithParallelUserTaskWithCustomAggregator() {

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
        }

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

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
        }

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
        public Object aggregateSingleVariable(DelegateExecution execution, VariableAggregatorContext context) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();

            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (VariableAggregationDefinition.Variable variable : context.getDefinition().getDefinitions()) {
                Object sourceVariable = execution.getVariable(variable.getSource());
                if (sourceVariable instanceof ArrayNode sourceArrayNode) {
                    for (int i = 0; i < sourceArrayNode.size(); i++) {
                        JsonNode node = arrayNode.get(i);
                        JsonNode sourceNode = sourceArrayNode.get(i);
                        if (node == null) {
                            arrayNode.add(sourceNode.deepCopy());
                        } else if (node.isObject()) {
                            ObjectNode objectNode = (ObjectNode) node;
                            for (Map.Entry<String, JsonNode> propertyEntry : sourceNode.properties()) {
                                String propertyName = propertyEntry.getKey();
                                JsonNode value = propertyEntry.getValue();
                                objectNode.set(propertyName, value);
                            }
                        }
                    }
                }
            }

            return arrayNode;
        }

        @Override
        public Object aggregateMultiVariables(DelegateExecution execution, List<? extends VariableInstance> instances, VariableAggregatorContext context) {
            ArrayNode arrayNode = CommandContextUtil.getProcessEngineConfiguration().getObjectMapper().createArrayNode();
            for (VariableInstance instance : instances) {
                arrayNode.addAll((ArrayNode) instance.getValue());
            }

            return arrayNode;
        }
    }

}
