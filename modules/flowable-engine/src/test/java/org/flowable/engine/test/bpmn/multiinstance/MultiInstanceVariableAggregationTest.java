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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Joram Barrez
 */
public class MultiInstanceVariableAggregationTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testMultiInstanceUserTaskWithVariableAggregation() {
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

        variables.put("approved", true);
        variables.put("description", "description task 1");
        taskService.complete(tasks.get(1).getId(), variables, true);

        variables.put("approved", false);
        variables.put("description", "description task 2");
        taskService.complete(tasks.get(2).getId(), variables, true);

        ArrayNode reviews = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "reviews");
        assertThat(reviews).isNotNull();
    }

}
