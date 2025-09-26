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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemVariableAggregator;
import org.flowable.cmmn.api.delegate.PlanItemVariableAggregatorContext;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.variable.CmmnAggregatedVariableType;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.JsonType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class RepetitionVariableAggregationTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSequentialRepeatingUserTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingTask")
            .variable("nrOfLoops", 3)
            .variable("otherVariable", "Hello World")
            .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ userId: null }"
                        + "]");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();

        assertThat(cmmnTaskService.getVariable(task.getId(), "repetitionCounter")).isEqualTo(1);
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setAssignee(task.getId(), "userOne");

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ userId: 'userOne' }"
                        + "]");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne' }"
                            + "]");
        }

        cmmnTaskService.complete(task.getId(), variables);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                        + "{ userId: null }"
                        + "]");

        assertVariablesNotVisible(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                            + "{ userId: null }"
                            + "]");
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
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                    + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                    + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                    + "]]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                            + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                            + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                            + "]");
        }

    }
    
    @Test
    @CmmnDeployment
    public void testSequentialRepeatingUserTaskIgnoreCounter() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingTask")
            .variable("nrOfLoops", 3)
            .variable("otherVariable", "Hello World")
            .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ userId: null }"
                        + "]");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();

        assertThat(cmmnTaskService.getVariable(task.getId(), "repetitionCounter")).isEqualTo(1); 
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setAssignee(task.getId(), "userOne");

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ userId: 'userOne' }"
                        + "]");

        cmmnTaskService.complete(task.getId(), variables);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                        + "{ userId: null }"
                        + "]");

        assertVariablesNotVisible(caseInstance);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        assertThat(cmmnTaskService.getVariable(task.getId(), "repetitionCounter")).isEqualTo(2);
        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.setAssignee(task.getId(), "userTwo");
        cmmnTaskService.complete(task.getId(), variables);
    }

    @Test
    @CmmnDeployment
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
        assertThatJson(reviews.getValue())
            .isEqualTo("["
                    + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                    + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                    + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                    + "]");

        assertNoAggregatedVariables();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                            + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                            + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                            + "]");
        }

    }

    @Test
    @CmmnDeployment
    public void testSequentialRepeatingUserTaskStoreAsTransient() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingTask")
            .variable("nrOfLoops", 3)
            .variable("otherVariable", "Hello World")
            .start();

        VariableInstance reviews = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviews).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.complete(task.getId(), variables);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.complete(task.getId(), variables);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.complete(task.getId(), variables);

        assertVariablesNotVisible(caseInstance);

        reviews = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviews).isNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "firstDescription")).isEqualTo("description task 0");

        assertNoAggregatedVariables();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();

            HistoricVariableInstance historicFirstDescription = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("firstDescription")
                    .singleResult();
            assertThat(historicFirstDescription).isNotNull();
            assertThat(historicFirstDescription.getValue()).isEqualTo("description task 0");
        }

    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testSequentialRepeatingCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn",
    })
    public void testSequentialRepeatingCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .variable("nrOfLoops", 3)
                .variable("otherVariable", "Hello World")
                .start();

        VariableInstance reviews = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");

        assertThat(reviews).isNull();

        Task task = cmmnTaskService.createTaskQuery().taskName("Sub task").singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");

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

        task = cmmnTaskService.createTaskQuery().taskName("Sub task").singleResult();
        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.complete(task.getId(), variables);

        task = cmmnTaskService.createTaskQuery().taskName("Sub task").singleResult();
        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.complete(task.getId(), variables);

        assertVariablesNotVisible(caseInstance);

        reviews = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");

        assertThat(reviews).isNotNull();
        assertThat(reviews.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
        assertThatJson(reviews.getValue())
                .isEqualTo("["
                        + "{ approved: false, description: 'description task 0' },"
                        + "{ approved: true, description: 'description task 1' },"
                        + "{ approved: false, description: 'description task 2' }"
                        + "]");

        assertNoAggregatedVariables();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ approved: false, description: 'description task 0' },"
                            + "{ approved: true, description: 'description task 1' },"
                            + "{ approved: false, description: 'description task 2' }"
                            + "]");
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testStageWithSequentialRepeatingUserTask.cmmn")
    public void testTerminateStateWithSequentialRepeatingUserTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingTask")
            .variable("nrOfLoops", 3)
            .variable("otherVariable", "Hello World")
            .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ }"
                        + "]");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task A").singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");

        cmmnTaskService.complete(task.getId(), variables);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ approved : false, description : 'description task 0' },"
                        + "{ }"
                        + "]");

        assertVariablesNotVisible(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ approved : false, description : 'description task 0' },"
                            + "{ }"
                            + "]");
        }

        PlanItemInstance stageInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .onlyStages()
                .singleResult();
        assertThat(stageInstance).isNotNull();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(stageInstance.getId())
                .variable("reason", "terminate")
                .terminate();

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task A").singleResult();
        assertThat(task).isNull();

        assertVariablesNotVisible(caseInstance);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThat(reviews).isNull();

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }

    }

    @Test
    @CmmnDeployment
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
                .isEqualTo("["
                        + "{ userId: null },"
                        + "{ userId: null },"
                        + "{ userId: null },"
                        + "{ userId: null }"
                        + "]");

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
                .isEqualTo("["
                        + "{ userId: null },"
                        + "{ userId: null },"
                        + "{ userId: null },"
                        + "{ userId: 'userThree' }"
                        + "]");

        cmmnTaskService.complete(tasks.get(3).getId(), variables);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ userId: null },"
                        + "{ userId: null },"
                        + "{ userId: null },"
                        + "{ userId: 'userThree', approved : true, description : 'description task 3' }"
                        + "]");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: null },"
                            + "{ userId: null },"
                            + "{ userId: null },"
                            + "{ userId: 'userThree', approved : true, description : 'description task 3' }"
                            + "]");
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
                "["
                    + "{ userId: 'userZero', approved : false, description : 'description task 0' },"
                    + "{ userId: 'userOne', approved : true, description : 'description task 1' },"
                    + "{ userId: 'userTwo', approved : false, description : 'description task 2' },"
                    + "{ userId: 'userThree', approved : true, description : 'description task 3' }"
                    + "]]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ userId: 'userZero', approved : false, description : 'description task 0' },"
                            + "{ userId: 'userOne', approved : true, description : 'description task 1' },"
                            + "{ userId: 'userTwo', approved : false, description : 'description task 2' },"
                            + "{ userId: 'userThree', approved : true, description : 'description task 3' }"
                            + "]");
        }

    }

    @Test
    @CmmnDeployment
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
        assertThatJson(cmmnRuntimeService.getVariable(caseInstance.getId(), "results"))
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

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .variableName("results")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThatJson(historicReviews.getValue())
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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testParallelRepeatingUserTaskVariableTypes.cmmn")
    public void testParallelRepeatingUserTaskVariableTypesUnsupportedVariableTypes() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .variable("otherVariable", "Hello World")
                .variable("myCollection", Arrays.asList("a", "b"))
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task A").singleResult();
        cmmnTaskService.complete(task.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task B")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(2);

        Map<String, Object> variables = new HashMap<>();
        variables.put("description", "Description for task with LocalDate");
        variables.put("score", 10);
        variables.put("passed", true);
        variables.put("location", "Springfield");
        variables.put("startTime", new TestSerializableVariable());

        assertThatThrownBy(() -> cmmnTaskService.complete(tasks.get(0).getId(), variables))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("Cannot aggregate variable: ")
                .hasMessageContaining("startTime");
    }


    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testStageWithParallelRepeatingUserTask.cmmn")
    public void testTerminateStageWithParallelRepeatingUserTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingTask")
            .variable("otherVariable", "Hello World")
            .variable("myCollection", Arrays.asList("a", "b", "c"))
            .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ task: 1 },"
                        + "{ task: 2 },"
                        + "{ task: 3 }"
                        + "]");

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId()).taskName("Stage task A")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(3);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 3");

        cmmnTaskService.complete(tasks.get(2).getId(), variables);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ task: 1 },"
                        + "{ task: 2 },"
                        + "{ task: 3, approved : true, description : 'description task 3' }"
                        + "]");

        assertVariablesNotVisible(caseInstance);

        PlanItemInstance stageInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .onlyStages()
                .singleResult();
        assertThat(stageInstance).isNotNull();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(stageInstance.getId())
                .variable("reason", "terminate")
                .terminate();

        tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId()).taskName("Stage task A")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).isEmpty();

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThat(reviews).isNull();

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNull();
        }

    }

    @Test
    @CmmnDeployment
    public void testSequentialRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingStage")
            .variable("otherVariable", "Hello World")
            .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");
        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ }"
                        + "]");

        for (int i = 0; i < 3; i++) {

            // User task 'task one':  sets approved variable
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task A").singleResult();

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            cmmnTaskService.complete(task.getId(), variables);

            // User task 'task two': sets description variable
            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task B").singleResult();

            variables = new HashMap<>();
            variables.put("description", "description task " + i);
            cmmnTaskService.complete(task.getId(), variables);

            if (i == 0) {
                reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");
                assertThatJson(reviews)
                        .isEqualTo("["
                                + "{ approved : true, description : 'description task 0' }"
                                + "]");

                if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
                    HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                            .variableName("reviews")
                            .singleResult();
                    assertThat(historicReviews).isNotNull();
                    assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
                    assertThatJson(historicReviews.getValue())
                            .isEqualTo("["
                                    + "{ approved : true, description : 'description task 0' }"
                                    + "]");
                }

            } else if (i == 1) {
                reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");
                assertThatJson(reviews)
                        .isEqualTo("["
                                + "{ score: 100, approved : true, description : 'description task 0' },"
                                + "{ approved : false, description : 'description task 1' }"
                                + "]");

                if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
                    HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                            .variableName("reviews")
                            .singleResult();
                    assertThat(historicReviews).isNotNull();
                    assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
                    assertThatJson(historicReviews.getValue())
                            .isEqualTo("["
                                    + "{ score: 100, approved : true, description : 'description task 0' },"
                                    + "{ approved : false, description : 'description task 1' }"
                                    + "]");
                }

            }

            // User task 'task three': updates description and adds score
            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task C").singleResult();

            variables = new HashMap<>();
            variables.put("myScore", i + 100);
            //            variables.put("description", "updated description task " + i);
            cmmnTaskService.complete(task.getId(), variables);
        }

        Task remainingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(remainingTask.getName()).isEqualTo("A");

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");
        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ score: 100, approved : true, description : 'description task 0' },"
                    + "{ score: 101, approved : false, description : 'description task 1' },"
                    + "{ score: 102, approved : true, description : 'description task 2' }"
                    + "]]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ score: 100, approved : true, description : 'description task 0' },"
                            + "{ score: 101, approved : false, description : 'description task 1' },"
                            + "{ score: 102, approved : true, description : 'description task 2' }"
                            + "]");
        }

    }

    @Test
    @CmmnDeployment
    public void testParallelRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingStage")
            .variable("myCollection", Arrays.asList("one", "two", "three", "four"))
            .variable("otherVariable", "Hello World")
            .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ },"
                        + "{ },"
                        + "{ },"
                        + "{ }"
                        + "]");

        // User task 'Stage task A':  sets approved variable
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task A")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            cmmnTaskService.complete(task.getId(), variables);
        }

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ approved : true },"
                        + "{ approved : false },"
                        + "{ approved : true },"
                        + "{ approved : false }"
                        + "]");

        // User task 'Stage task B': sets description variable
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task B")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("description", "description task " + i);
            cmmnTaskService.complete(task.getId(), variables);
        }

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ approved : true, description : 'description task 0' },"
                        + "{ approved : false, description : 'description task 1' },"
                        + "{ approved : true, description : 'description task 2' },"
                        + "{ approved : false, description : 'description task 3' }"
                        + "]");

        // User task 'task three': updates description and adds score
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task C")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ approved : true, description : 'description task 0' },"
                            + "{ approved : false, description : 'description task 1' },"
                            + "{ approved : true, description : 'description task 2' },"
                            + "{ approved : false, description : 'description task 3' }"
                            + "]");
        }

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("myScore", i + 10);
            cmmnTaskService.complete(task.getId(), variables);
        }

        Task taskAfterMi = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskAfterMi.getName()).isEqualTo("A");

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ score: 10, approved : true, description : 'description task 0' },"
                    + "{ score: 11, approved : false, description : 'description task 1' },"
                    + "{ score: 12, approved : true, description : 'description task 2' },"
                    + "{ score: 13, approved : false, description : 'description task 3' }"
                    + "]]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "{ score: 10, approved : true, description : 'description task 0' },"
                            + "{ score: 11, approved : false, description : 'description task 1' },"
                            + "{ score: 12, approved : true, description : 'description task 2' },"
                            + "{ score: 13, approved : false, description : 'description task 3' }"
                            + "]");
        }

    }

    @Test
    @CmmnDeployment
    public void testParallelRepeatingStageWithParallelRepeatingUserTask() {

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingStage")
                .variable("myCollection", Arrays.asList("one", "two"))
                .variable("otherVariable", "Hello World")
                .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "  {"
                        + "    first: [{ task: 11 }, { task: 12 }],"
                        + "    second: [{ score: 0 }, { score: 0 }]"
                        + "  },"
                        + "  {"
                        + "    first: [{ task: 21 }, { task: 22 }],"
                        + "    second: [{ score: 0 }, { score: 0 }]"
                        + "  }"
                        + "]");

        // User task 'task one': sets approved and description variable
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Stage task A")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            variables.put("description", "description task " + i);
            cmmnTaskService.complete(task.getId(), variables);
        }

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "  {"
                        + "    first: ["
                        + "      { task: 11, approved : true, description : 'description task 0' },"
                        + "      { task: 12, approved : false, description : 'description task 1'}"
                        + "    ],"
                        + "    second: ["
                        + "      { score: 0 },"
                        + "      { score: 0 }"
                        + "    ]"
                        + "  },"
                        + "  {"
                        + "    first: ["
                        + "      { task: 21, approved : true, description : 'description task 2' },"
                        + "      { task: 22, approved : false, description : 'description task 3' }"
                        + "    ],"
                        + "    second: ["
                        + "      { score: 0 },"
                        + "      { score: 0 }"
                        + "    ]"
                        + "  }"
                        + "]");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(CmmnAggregatedVariableType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "  {"
                            + "    first: ["
                            + "      { task: 11, approved : true, description : 'description task 0' },"
                            + "      { task: 12, approved : false, description : 'description task 1'}"
                            + "    ],"
                            + "    second: ["
                            + "      { score: 0 },"
                            + "      { score: 0 }"
                            + "    ]"
                            + "  },"
                            + "  {"
                            + "    first: ["
                            + "      { task: 21, approved : true, description : 'description task 2' },"
                            + "      { task: 22, approved : false, description : 'description task 3' }"
                            + "    ],"
                            + "    second: ["
                            + "      { score: 0 },"
                            + "      { score: 0 }"
                            + "    ]"
                            + "  }"
                            + "]");
        }

        assertVariablesNotVisible(caseInstance);

        // User task 'task two': sets myScore variable
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Stage task B")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("myScore", i + 10);
            cmmnTaskService.complete(task.getId(), variables);
        }

        assertVariablesNotVisible(caseInstance);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "  {"
                        + "    first: ["
                        + "      { task: 11, approved : true, description : 'description task 0' },"
                        + "      { task: 12, approved : false, description : 'description task 1'}"
                        + "    ],"
                        + "    second: ["
                        + "      { score: 20 },"
                        + "      { score: 22 }"
                        + "    ]"
                        + "  },"
                        + "  {"
                        + "    first: ["
                        + "      { task: 21, approved : true, description : 'description task 2' },"
                        + "      { task: 22, approved : false, description : 'description task 3' }"
                        + "    ],"
                        + "    second: ["
                        + "      { score: 24 },"
                        + "      { score: 26 }"
                        + "    ]"
                        + "  }"
                        + "]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicReviews = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("reviews")
                    .singleResult();
            assertThat(historicReviews).isNotNull();
            assertThat(historicReviews.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);
            assertThatJson(historicReviews.getValue())
                    .isEqualTo("["
                            + "  {"
                            + "    first: ["
                            + "      { task: 11, approved : true, description : 'description task 0' },"
                            + "      { task: 12, approved : false, description : 'description task 1'}"
                            + "    ],"
                            + "    second: ["
                            + "      { score: 20 },"
                            + "      { score: 22 }"
                            + "    ]"
                            + "  },"
                            + "  {"
                            + "    first: ["
                            + "      { task: 21, approved : true, description : 'description task 2' },"
                            + "      { task: 22, approved : false, description : 'description task 3' }"
                            + "    ],"
                            + "    second: ["
                            + "      { score: 24 },"
                            + "      { score: 26 }"
                            + "    ]"
                            + "  }"
                            + "]");
        }
    }

    @Test
    @CmmnDeployment
    public void testParallelRepeatingStageWithParallelRepeatingUserTaskWithCustomAggregator() {

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingStage")
                .variable("myCollection", Arrays.asList("one", "two"))
                .variable("otherVariable", "Hello World")
                .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ task: 11, score: 0 },"
                        + "{ task: 12, score: 0 },"
                        + "{ task: 21, score: 0 },"
                        + "{ task: 22, score: 0 }"
                        + "]");

        // User task 'task one': sets approved and description variable
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Stage task A")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            variables.put("description", "description task " + i);
            cmmnTaskService.complete(task.getId(), variables);
        }

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ task: 11, approved : true, description : 'description task 0', score: 0 },"
                        + "{ task: 12, approved : false, description : 'description task 1', score: 0 },"
                        + "{ task: 21, approved : true, description : 'description task 2', score: 0 },"
                        + "{ task: 22, approved : false, description : 'description task 3', score: 0 }"
                        + "]");

        assertVariablesNotVisible(caseInstance);

        // User task 'task two': sets myScore variable
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Stage task B")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("myScore", i + 10);
            cmmnTaskService.complete(task.getId(), variables);
        }

        assertVariablesNotVisible(caseInstance);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ task: 11, approved : true, description : 'description task 0', score: 20 },"
                        + "{ task: 12, approved : false, description : 'description task 1', score: 22 },"
                        + "{ task: 21, approved : true, description : 'description task 2', score: 24 },"
                        + "{ task: 22, approved : false, description : 'description task 3', score: 26 }"
                        + "]");

        assertNoAggregatedVariables();
        VariableInstance reviewsVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "reviews");
        assertThat(reviewsVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
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

    public static class CustomVariableAggregator implements PlanItemVariableAggregator {

        @Override
        public Object aggregateSingleVariable(DelegatePlanItemInstance planItemInstance, PlanItemVariableAggregatorContext context) {
            ArrayNode arrayNode = CommandContextUtil.getCmmnEngineConfiguration().getObjectMapper().createArrayNode();
            for (VariableAggregationDefinition.Variable variable : context.getDefinition().getDefinitions()) {
                Object sourceVariable = planItemInstance.getVariable(variable.getSource());
                if (sourceVariable instanceof ArrayNode sourceArrayNode) {
                    for (int i = 0; i < sourceArrayNode.size(); i++) {
                        JsonNode node = arrayNode.get(i);
                        JsonNode sourceNode = sourceArrayNode.get(i);
                        if (node == null) {
                            arrayNode.add(sourceNode.deepCopy());
                        } else if (node.isObject()) {
                            ObjectNode objectNode = (ObjectNode) node;
                            for (Map.Entry<String, JsonNode> property : sourceNode.properties()) {
                                String propertyName = property.getKey();
                                JsonNode value = property.getValue();
                                objectNode.set(propertyName, value);
                            }
                        }
                    }
                }
            }

            return arrayNode;
        }

        @Override
        public Object aggregateMultiVariables(DelegatePlanItemInstance planItemInstance, List<? extends VariableInstance> instances, PlanItemVariableAggregatorContext context) {
            ArrayNode arrayNode = CommandContextUtil.getCmmnEngineConfiguration().getObjectMapper().createArrayNode();
            for (VariableInstance instance : instances) {
                arrayNode.addAll((ArrayNode) instance.getValue());
            }

            return arrayNode;
        }
    }

    public static class TestSerializableVariable implements Serializable {

        private static final long serialVersionUID = 1L;

        private String someField;

        public String getSomeField() {
            return someField;
        }

        public void setSomeField(String someField) {
            this.someField = someField;
        }
    }

}
