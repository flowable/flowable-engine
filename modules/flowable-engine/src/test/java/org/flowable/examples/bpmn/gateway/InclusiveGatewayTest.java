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
package org.flowable.examples.bpmn.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * Example of using the exclusive gateway.
 * 
 * @author Joram Barrez
 * @author Tom Van Buskirk
 */
public class InclusiveGatewayTest extends PluggableFlowableTestCase {

    private static final String TASK1_NAME = "Send e-mail for more information";
    private static final String TASK2_NAME = "Check account balance";
    private static final String TASK3_NAME = "Call customer";

    /**
     * The test process has an OR gateway where, the 'input' variable is used to select the expected outgoing sequence flow.
     */
    @Test
    @Deployment
    public void testDecisionFunctionality() {

        Map<String, Object> variables = new HashMap<>();

        // Test with input == 1
        variables.put("input", 1);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGateway", variables);
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(TASK1_NAME, TASK2_NAME, TASK3_NAME);

        // Test with input == 2
        variables.put("input", 2);
        pi = runtimeService.startProcessInstanceByKey("inclusiveGateway", variables);
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(TASK2_NAME, TASK3_NAME);

        // Test with input == 3
        variables.put("input", 3);
        pi = runtimeService.startProcessInstanceByKey("inclusiveGateway", variables);
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly(TASK3_NAME);

        // Test with input == 4
        variables.put("input", 4);
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveGateway", variables))
                .as("Exception is expected since no outgoing sequence flow matches")
                .isInstanceOf(FlowableException.class);
    }

}
