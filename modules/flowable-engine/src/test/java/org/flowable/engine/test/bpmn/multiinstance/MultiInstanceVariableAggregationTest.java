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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Joram Barrez
 */
public class MultiInstanceVariableAggregationTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testParallelMultiInstanceUserTaskWithVariableAggregation() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 3)
            .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables, true);

        assertVariablesNotVisibleForAnyExecution(processInstance);

        variables.put("approved", true);
        variables.put("description", "description task 1");
        taskService.complete(tasks.get(1).getId(), variables, true);

        variables.put("approved", false);
        variables.put("description", "description task 2");
        taskService.complete(tasks.get(2).getId(), variables, true);

        assertVariablesNotVisibleForAnyExecution(processInstance);

        ArrayNode reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(
                "["
                + "{ approved : true, description : 'description task 0' },"
                + "{ approved : true, description : 'description task 1' },"
                + "{ approved : false, description : 'description task 2' }"
                + "]]");
    }

    @Test
    @Deployment
    public void testSequentialMultiInstanceUserTaskWithVariableAggregation() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", 4)
            .start();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("description", "a");
        taskService.complete(task.getId(), variables, true);

        assertVariablesNotVisibleForAnyExecution(processInstance);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("approved", false);
        variables.put("description", "b");
        taskService.complete(task.getId(), variables, true);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("approved", true);
        variables.put("description", "c");
        taskService.complete(task.getId(), variables, true);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("approved", true);
        variables.put("description", "d");
        taskService.complete(task.getId(), variables, true);

        assertVariablesNotVisibleForAnyExecution(processInstance);

        ArrayNode reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");

        assertThatJson(reviews)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(
                "["
                    + "{ approved : false, description : 'a' },"
                    + "{ approved : false, description : 'b' },"
                    + "{ approved : true, description : 'c' },"
                    + "{ approved : true, description : 'd' }"
                    + "]]");
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
        taskService.complete(tasks.get(0).getId(), variables, true);

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

    protected void assertVariablesNotVisibleForAnyExecution(ProcessInstance processInstance) {

        assertThat(runtimeService.getVariable(processInstance.getId(), "approved")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "description")).isNull();

        // Gathered variables shouldn't be visible for any execution
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        for (Execution execution : executions) {
            assertThat(runtimeService.getVariable(execution.getId(), "approved")).isNull();
            assertThat(runtimeService.getVariable(execution.getId(), "description")).isNull();
        }
    }

}
