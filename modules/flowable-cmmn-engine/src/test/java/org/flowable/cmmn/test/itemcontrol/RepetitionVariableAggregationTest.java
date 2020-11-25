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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

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
            .variable("otherVariable", "Hello World")
            .start();

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ }"
                        + "]");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();

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
                        + "{ }"
                        + "]");

        assertVariablesNotVisible(caseInstance);

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

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                    + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                    + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                    + "]]");

        assertNoAggregatedVariables();

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
                        + "{ },"
                        + "{ },"
                        + "{ },"
                        + "{ }"
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
                        + "{ },"
                        + "{ },"
                        + "{ },"
                        + "{ userId: 'userThree' }"
                        + "]");

        cmmnTaskService.complete(tasks.get(3).getId(), variables);

        reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
                .isEqualTo("["
                        + "{ },"
                        + "{ },"
                        + "{ },"
                        + "{ userId: 'userThree', approved : true, description : 'description task 3' }"
                        + "]");

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
            } else if (i == 1) {
                reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");
                assertThatJson(reviews)
                        .isEqualTo("["
                                + "{ score: 100, approved : true, description : 'description task 0' },"
                                + "{ approved : false, description : 'description task 1' }"
                                + "]");
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
