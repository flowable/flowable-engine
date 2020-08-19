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
    public void testSequentialRepeatingUserTaskWithVariableAggregation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("repeatingTask")
            .variable("otherVariable", "Hello World")
            .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setVariablesLocal(task.getId(), variables);
        cmmnTaskService.complete(task.getId());

        assertVariablesNotVisible(caseInstance);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.setVariablesLocal(task.getId(), variables);
        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("My Task").singleResult();
        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.setVariablesLocal(task.getId(), variables);
        cmmnTaskService.complete(task.getId());

        assertVariablesNotVisible(caseInstance);

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
            .isEqualTo(
                "["
                    + "{ approved : false, description : 'description task 0' },"
                    + "{ approved : true, description : 'description task 1' },"
                    + "{ approved : false, description : 'description task 2' }"
                    + "]]");

    }

    @Test
    @CmmnDeployment
    public void testParallelRepeatingUserTaskWithVariableAggregation() {
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
        cmmnTaskService.complete(tasks.get(3).getId());

        assertVariablesNotVisible(caseInstance);

        variables.put("approved", true);
        variables.put("description", "description task 1");
        cmmnTaskService.setVariablesLocal(tasks.get(1).getId(), variables);
        cmmnTaskService.complete(tasks.get(1).getId());

        variables.put("approved", false);
        variables.put("description", "description task 2");
        cmmnTaskService.setVariablesLocal(tasks.get(2).getId(), variables);
        cmmnTaskService.complete(tasks.get(2).getId());

        variables.put("approved", false);
        variables.put("description", "description task 0");
        cmmnTaskService.setVariablesLocal(tasks.get(0).getId(), variables);
        cmmnTaskService.complete(tasks.get(0).getId());

        assertVariablesNotVisible(caseInstance);

        ArrayNode reviews = (ArrayNode) cmmnRuntimeService.getVariable(caseInstance.getId(), "reviews");

        assertThatJson(reviews)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(
                "["
                    + "{ approved : false, description : 'description task 0' },"
                    + "{ approved : true, description : 'description task 1' },"
                    + "{ approved : false, description : 'description task 2' },"
                    + "{ approved : true, description : 'description task 3' }"
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
