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

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.json.jackson2.Jackson2VariableJsonMapper;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.variable.BpmnAggregatedVariableType;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.JsonType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class MultiInstanceVariableAggregationJackson2Test extends ResourceFlowableTestCase {

    public MultiInstanceVariableAggregationJackson2Test() {
        super("flowable.cfg.xml", "multiInstanceVariableAggregationJackson2");
    }

    @Override
    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setVariableJsonMapper(
                new Jackson2VariableJsonMapper(new ObjectMapper()));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTask.bpmn20.xml")
    public void testParallelMultiInstanceUserTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .variable("nrOfLoops", 3)
                .start();

        ArrayNode reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: null },
                          { userId: null },
                          { userId: null }
                        ] """);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(3);

        taskService.setAssignee(tasks.get(0).getId(), "userOne");
        taskService.setAssignee(tasks.get(1).getId(), "userTwo");
        taskService.setAssignee(tasks.get(2).getId(), "userThree");

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: 'userOne' },
                          { userId: 'userTwo' },
                          { userId: 'userThree' }
                        ]
                        """);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: 'userOne', approved : true, description : 'description task 0' },
                          { userId: 'userTwo' },
                          { userId: 'userThree' }
                        ]
                        """);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicReviews = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(BpmnAggregatedVariableType.TYPE_NAME);
            Object historicValue = historicReviews.getValue();
            assertThat(historicValue).isInstanceOf(ArrayNode.class);
            assertThatJson(historicValue)
                    .isEqualTo("""
                            [
                              { userId: 'userOne', approved : true, description : 'description task 0' },
                              { userId: 'userTwo' },
                              { userId: 'userThree' }
                            ]
                            """);
        }

        variables.put("approved", true);
        variables.put("description", "description task 1");
        taskService.complete(tasks.get(1).getId(), variables);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: 'userOne', approved : true, description : 'description task 0' },
                          { userId: 'userTwo', approved : true, description : 'description task 1' },
                          { userId: 'userThree' }
                        ]
                        """);

        variables.put("approved", false);
        variables.put("description", "description task 2");
        taskService.complete(tasks.get(2).getId(), variables);

        assertVariablesNotVisibleForProcessInstance(processInstance);

        reviews = runtimeService.getVariable(processInstance.getId(), "reviews", ArrayNode.class);

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: 'userOne', approved : true, description : 'description task 0' },
                          { userId: 'userTwo', approved : true, description : 'description task 1' },
                          { userId: 'userThree', approved : false, description : 'description task 2' }
                        ]
                        """);

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
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTaskWithoutCreateOverview.bpmn20.xml")
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
                .isEqualTo("""
                        [
                          { userId: 'userOne', approved : true, description : 'description task 0' },
                          { userId: 'userTwo', approved : true, description : 'description task 1' },
                          { userId: 'userThree', approved : false, description : 'description task 2' }
                        ]
                        """);

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
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceVariableAggregationTest.testParallelMultiInstanceUserTaskVariableTypes.bpmn20.xml")
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
                .isEqualTo("""
                        [
                          {
                            description: 'Description for task with LocalDate',
                            score: 10,
                            passed: true,
                            location: 'Springfield',
                            startTime: '2020-12-08'
                          },
                          {
                            description: 'Description for task with LocalDateTime',
                            score: 60.55,
                            passed: false,
                            location: null,
                            startTime: '2020-12-08T10:20:30'
                          },
                          {
                            description: 'Description for task with Instant',
                            score: 100,
                            passed: true,
                            location: 'Zurich',
                            startTime: '2020-12-08T08:20:45.585Z'
                          },
                          {
                            description: 'Description for task with Date',
                            score: 1234,
                            passed: true,
                            location: 'Test Valley',
                            startTime: '2020-12-12T12:24:15.155Z'
                          }
                        ]
                        """);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicResults = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("results")
                    .singleResult();
            assertThat(historicResults).isNotNull();
            Object historicValue = historicResults.getValue();
            assertThat(historicValue).isInstanceOf(ArrayNode.class);
            assertThatJson(historicValue)
                    .isEqualTo("""
                            [
                              {
                                description: 'Description for task with LocalDate',
                                score: 10,
                                passed: true,
                                location: 'Springfield',
                                startTime: '2020-12-08'
                              },
                              {
                                description: 'Description for task with LocalDateTime',
                                score: 60.55,
                                passed: false,
                                location: null,
                                startTime: '2020-12-08T10:20:30'
                              },
                              {
                                description: 'Description for task with Instant',
                                score: 100,
                                passed: true,
                                location: 'Zurich',
                                startTime: '2020-12-08T08:20:45.585Z'
                              },
                              {
                                description: 'Description for task with Date',
                                score: 1234,
                                passed: true,
                                location: 'Test Valley',
                                startTime: '2020-12-12T12:24:15.155Z'
                              }
                            ]
                            """);
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

}
