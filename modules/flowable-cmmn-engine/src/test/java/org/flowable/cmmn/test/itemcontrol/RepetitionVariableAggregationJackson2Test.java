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
package org.flowable.cmmn.test.itemcontrol;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.variable.CmmnAggregatedVariableType;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.EngineConfigurer;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.json.jackson2.Jackson2VariableJsonMapper;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.JsonType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class RepetitionVariableAggregationJackson2Test extends CustomCmmnConfigurationFlowableTestCase {

    @EngineConfigurer
    protected static void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setVariableJsonMapper(new Jackson2VariableJsonMapper(new ObjectMapper()));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testSequentialRepeatingUserTask.cmmn")
    public void testSequentialRepeatingUserTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .variable("nrOfLoops", 3)
                .variable("otherVariable", "Hello World")
                .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: null }
                        ]
                        """);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();

        assertThat(cmmnTaskService.getVariable(task.getId(), "repetitionCounter")).isEqualTo(1);
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setAssignee(task.getId(), "userOne");

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: 'userOne' }
                        ]
                        """);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("""
                            [
                              { userId: 'userOne' }
                            ]
                            """);
        }

        cmmnTaskService.complete(task.getId(), variables);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: 'userOne', approved : false, description : 'description task 0' },
                          { userId: null }
                        ]
                        """);

        assertVariablesNotVisible(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            Object historicValue = historicReviews.getValue();
            assertThat(historicValue).isInstanceOf(JsonNode.class);
            assertThatJson(historicValue)
                    .isEqualTo("""
                            [
                              { userId: 'userOne', approved : false, description : 'description task 0' },
                              { userId: null }
                            ]
                            """);
        }

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        assertThat(cmmnTaskService.getVariable(task.getId(), "repetitionCounter")).isEqualTo(2);
        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.setAssignee(task.getId(), "userTwo");
        cmmnTaskService.complete(task.getId(), variables);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        assertThat(cmmnTaskService.getVariable(task.getId(), "repetitionCounter")).isEqualTo(3);
        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.setAssignee(task.getId(), "userThree");
        cmmnTaskService.complete(task.getId(), variables);

        assertVariablesNotVisible(caseInstance);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: 'userOne', approved : false, description : 'description task 0' },
                          { userId: 'userTwo', approved : true, description : 'description task 1' },
                          { userId: 'userThree', approved : false, description : 'description task 2' }
                        ]
                        """);

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            Object historicValue = historicReviews.getValue();
            assertThat(historicValue).isInstanceOf(JsonNode.class);
            assertThatJson(historicValue)
                    .isEqualTo("""
                            [
                              { userId: 'userOne', approved : false, description : 'description task 0' },
                              { userId: 'userTwo', approved : true, description : 'description task 1' },
                              { userId: 'userThree', approved : false, description : 'description task 2' }
                            ]
                            """);
        }

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testSequentialRepeatingUserTaskWithoutCreateOverview.cmmn")
    public void testSequentialRepeatingUserTaskWithoutCreateOverview() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .variable("nrOfLoops", 3)
                .variable("otherVariable", "Hello World")
                .start();

        VariableInstance reviews = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");

        assertThat(reviews).isNull();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setAssignee(task.getId(), "userOne");

        reviews = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");

        assertThat(reviews).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }

        cmmnTaskService.complete(task.getId(), variables);

        reviews = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");

        assertThat(reviews).isNull();

        assertVariablesNotVisible(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.setAssignee(task.getId(), "userTwo");
        cmmnTaskService.complete(task.getId(), variables);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.setAssignee(task.getId(), "userThree");
        cmmnTaskService.complete(task.getId(), variables);

        assertVariablesNotVisible(caseInstance);

        reviews = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");

        assertThat(reviews).isNotNull();
        assertThat(reviews.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
        Object value = reviews.getValue();
        assertThat(value).isInstanceOf(JsonNode.class);
        assertThatJson(value)
                .isEqualTo("""
                        [
                          { userId: 'userOne', approved : false, description : 'description task 0' },
                          { userId: 'userTwo', approved : true, description : 'description task 1' },
                          { userId: 'userThree', approved : false, description : 'description task 2' }
                        ]
                        """);

        assertNoAggregatedVariables();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            Object historicValue = historicReviews.getValue();
            assertThat(historicValue).isInstanceOf(JsonNode.class);
            assertThatJson(historicValue)
                    .isEqualTo("""
                            [
                              { userId: 'userOne', approved : false, description : 'description task 0' },
                              { userId: 'userTwo', approved : true, description : 'description task 1' },
                              { userId: 'userThree', approved : false, description : 'description task 2' }
                            ]
                            """);
        }

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testParallelRepeatingUserTask.cmmn")
    public void testParallelRepeatingUserTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .variable("otherVariable", "Hello World")
                .variable("myCollection", Arrays.asList("a", "b", "c", "d"))
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task A").singleResult();
        cmmnTaskService.complete(task.getId());

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: null },
                          { userId: null },
                          { userId: null },
                          { userId: null }
                        ]
                        """);

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task B")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 3");
        cmmnTaskService.setAssignee(tasks.get(3).getId(), "userThree");

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: null },
                          { userId: null },
                          { userId: null },
                          { userId: 'userThree' }
                        ]
                        """);

        cmmnTaskService.complete(tasks.get(3).getId(), variables);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("""
                        [
                          { userId: null },
                          { userId: null },
                          { userId: null },
                          { userId: 'userThree', approved : true, description : 'description task 3' }
                        ]
                        """);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            Object historicValue = historicReviews.getValue();
            assertThat(historicValue).isInstanceOf(JsonNode.class);
            assertThatJson(historicValue)
                    .isEqualTo("""
                            [
                              { userId: null },
                              { userId: null },
                              { userId: null },
                              { userId: 'userThree', approved : true, description : 'description task 3' }
                            ]
                            """);
        }

        assertVariablesNotVisible(caseInstance);

        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.setAssignee(tasks.get(1).getId(), "userOne");
        cmmnTaskService.complete(tasks.get(1).getId(), variables);

        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.setAssignee(tasks.get(2).getId(), "userTwo");
        cmmnTaskService.complete(tasks.get(2).getId(), variables);

        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setAssignee(tasks.get(0).getId(), "userZero");
        cmmnTaskService.complete(tasks.get(0).getId(), variables);

        assertVariablesNotVisible(caseInstance);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo(
                        """
                                [
                                  { userId: 'userZero', approved : false, description : 'description task 0' },
                                  { userId: 'userOne', approved : true, description : 'description task 1' },
                                  { userId: 'userTwo', approved : false, description : 'description task 2' },
                                  { userId: 'userThree', approved : true, description : 'description task 3' }
                                ]
                                """);

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            Object historicValue = historicReviews.getValue();
            assertThat(historicValue).isInstanceOf(JsonNode.class);
            assertThatJson(historicValue)
                    .isEqualTo("""
                            [
                              { userId: 'userZero', approved : false, description : 'description task 0' },
                              { userId: 'userOne', approved : true, description : 'description task 1' },
                              { userId: 'userTwo', approved : false, description : 'description task 2' },
                              { userId: 'userThree', approved : true, description : 'description task 3' }
                            ]
                            """);
        }

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testParallelRepeatingUserTaskVariableTypes.cmmn")
    public void testParallelRepeatingUserTaskVariableTypes() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .variable("otherVariable", "Hello World")
                .variable("myCollection", Arrays.asList("a", "b", "c", "d"))
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task A").singleResult();
        cmmnTaskService.complete(task.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task B")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        Map<String, Object> variables = new HashMap<>();
        variables.put("description", "Description for task with LocalDate");
        variables.put("score", 10);
        variables.put("passed", true);
        variables.put("location", "Springfield");
        variables.put("startTime", LocalDate.of(2020, Month.DECEMBER, 8));
        cmmnTaskService.complete(tasks.get(0).getId(), variables);

        variables = new HashMap<>();
        variables.put("description", "Description for task with LocalDateTime");
        variables.put("score", 60.55);
        variables.put("passed", false);
        variables.put("location", null);
        variables.put("startTime", LocalDate.of(2020, Month.DECEMBER, 8).atTime(10, 20, 30));
        cmmnTaskService.complete(tasks.get(1).getId(), variables);

        variables = new HashMap<>();
        variables.put("description", "Description for task with Instant");
        variables.put("score", (short) 100);
        variables.put("passed", true);
        variables.put("location", "Zurich");
        variables.put("startTime", Instant.parse("2020-12-08T08:20:45.585Z"));
        cmmnTaskService.complete(tasks.get(2).getId(), variables);

        variables = new HashMap<>();
        variables.put("description", "Description for task with Date");
        variables.put("score", 1234L);
        variables.put("passed", true);
        variables.put("location", "Test Valley");
        variables.put("startTime", Instant.parse("2020-12-12T12:24:15.155Z"));
        cmmnTaskService.complete(tasks.get(3).getId(), variables);

        assertNoAggregatedVariables();
        Object results = cmmnRuntimeService.getVariable(caseInstance.getId(), "results");
        assertThat(results).isInstanceOf(JsonNode.class);
        assertThatJson(results)
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

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .variableName("results")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            Object historicValue = historicReviews.getValue();
            assertThat(historicValue).isInstanceOf(JsonNode.class);
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

    protected void assertVariablesNotVisible(CaseInstance caseInstance) {

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "otherVariable")).isNotNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "approved")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "description")).isNull();

        // Gathered variables shouldn't be visible for any execution
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "approved")).isNull();
            assertThat(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "description")).isNull();
        }

    }

    protected void assertNoAggregatedVariables() {
        List<VariableInstanceEntity> variableInstanceEntities = cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                        .getVariableServiceConfiguration()
                        .getVariableService()
                        .createInternalVariableInstanceQuery()
                        .scopeType(ScopeTypes.CMMN_VARIABLE_AGGREGATION)
                        .list());
        assertThat(variableInstanceEntities).isEmpty();
    }

}
