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
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Joram Barrez
 */
public class RepetitionVariableAggregationTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSequentialRepeatingUserTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingTask")
            .variable("otherVariable", "Hello World")
            .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setVariablesLocal(task.getId(), variables);
        cmmnTaskService.setAssignee(task.getId(), "userOne");
        cmmnTaskService.complete(task.getId());

        assertVariablesNotVisible(caseInstance);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.setVariablesLocal(task.getId(), variables);
        cmmnTaskService.setAssignee(task.getId(), "userTwo");
        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.setVariablesLocal(task.getId(), variables);
        cmmnTaskService.setAssignee(task.getId(), "userThree");
        cmmnTaskService.complete(task.getId());

        assertVariablesNotVisible(caseInstance);

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ userId: 'userOne', approved : false, description : 'description task 0' },"
                    + "{ userId: 'userTwo', approved : true, description : 'description task 1' },"
                    + "{ userId: 'userThree', approved : false, description : 'description task 2' }"
                    + "]]");

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

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task B").list();
        assertThat(tasks).hasSize(4);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 3");
        cmmnTaskService.setVariablesLocal(tasks.get(3).getId(), variables);
        cmmnTaskService.setAssignee(tasks.get(3).getId(), "userThree");
        cmmnTaskService.complete(tasks.get(3).getId());

        assertVariablesNotVisible(caseInstance);

        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.setVariablesLocal(tasks.get(1).getId(), variables);
        cmmnTaskService.setAssignee(tasks.get(1).getId(), "userOne");
        cmmnTaskService.complete(tasks.get(1).getId());

        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.setVariablesLocal(tasks.get(2).getId(), variables);
        cmmnTaskService.setAssignee(tasks.get(2).getId(), "userTwo");
        cmmnTaskService.complete(tasks.get(2).getId());

        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setVariablesLocal(tasks.get(0).getId(), variables);
        cmmnTaskService.setAssignee(tasks.get(0).getId(), "userZero");
        cmmnTaskService.complete(tasks.get(0).getId());

        assertVariablesNotVisible(caseInstance);

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(
                "["
                    + "{ userId: 'userZero', approved : false, description : 'description task 0' },"
                    + "{ userId: 'userOne', approved : true, description : 'description task 1' },"
                    + "{ userId: 'userTwo', approved : false, description : 'description task 2' },"
                    + "{ userId: 'userThree', approved : true, description : 'description task 3' }"
                    + "]]");

    }

    @Test
    @CmmnDeployment
    public void testSequentialRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingStage")
            .variable("otherVariable", "Hello World")
            .start();

        for (int i = 0; i < 3; i++) {

            // User task 'task one':  sets approved variable
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task A").singleResult();

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            cmmnTaskService.setVariablesLocal(task.getId(), variables);
            cmmnTaskService.complete(task.getId());

            // User task 'task two': sets description variable
            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task B").singleResult();

            variables = new HashMap<>();
            variables.put("description", "description task " + i);
            cmmnTaskService.setVariablesLocal(task.getId(), variables);
            cmmnTaskService.complete(task.getId());

            // User task 'task three': updates description and adds score
            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task C").singleResult();

            variables = new HashMap<>();
            variables.put("myScore", i + 100);
            //            variables.put("description", "updated description task " + i);
            cmmnTaskService.setVariablesLocal(task.getId(), variables);
            cmmnTaskService.complete(task.getId());
        }

        Task remainingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(remainingTask.getName()).isEqualTo("A");

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");
        assertThatJson(reviews)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(
                "["
                    + "{ score: 100, approved : true, description : 'description task 0' },"
                    + "{ score: 101, approved : false, description : 'description task 1' },"
                    + "{ score: 102, approved : true, description : 'description task 2' }"
                    + "]]");

    }

    @Test
    @CmmnDeployment
    public void testParallelRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingStage")
            .variable("myCollection", Arrays.asList("one", "two", "three", "four"))
            .variable("otherVariable", "Hello World")
            .start();

        // User task 'Stage task A':  sets approved variable
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task A").list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", i % 2 == 0);
            cmmnTaskService.setVariablesLocal(task.getId(), variables);
            cmmnTaskService.complete(task.getId(), variables);
        }

        // User task 'Stage task B': sets description variable
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task B").list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("description", "description task " + i);
            cmmnTaskService.setVariablesLocal(task.getId(), variables);
            cmmnTaskService.complete(task.getId(), variables);
        }

        // User task 'task three': updates description and adds score
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Stage task C").list();
        assertThat(tasks).hasSize(4);

        for (int i = 0; i < tasks.size();  i++) {
            Task task = tasks.get(i);

            Map<String, Object> variables = new HashMap<>();
            variables.put("myScore", i + 10);
            cmmnTaskService.setVariablesLocal(task.getId(), variables);
            cmmnTaskService.complete(task.getId(), variables);
        }

        Task taskAfterMi = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskAfterMi.getName()).isEqualTo("A");

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(
                "["
                    + "{ score: 10, approved : true, description : 'description task 0' },"
                    + "{ score: 11, approved : false, description : 'description task 1' },"
                    + "{ score: 12, approved : true, description : 'description task 2' },"
                    + "{ score: 13, approved : false, description : 'description task 3' }"
                    + "]]");

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

}
