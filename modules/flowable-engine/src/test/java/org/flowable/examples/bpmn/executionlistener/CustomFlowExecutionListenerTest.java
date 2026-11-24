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
package org.flowable.examples.bpmn.executionlistener;

import static org.assertj.core.api.Assertions.assertThat;


import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

/**
 * @author Tijs Rademakers
 */
public class CustomFlowExecutionListenerTest extends ResourceFlowableTestCase {

    public CustomFlowExecutionListenerTest() {
        super("org/flowable/examples/bpmn/executionlistener/custom.flow.parse.handler.flowable.cfg.xml");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/CustomFlowExecutionListenerTest.bpmn20.xml" })
    public void testScriptExecutionListener() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("scriptExecutionListenerProcess")
                .transientVariable("customFlowBean", new CustomFlowBean())
                .start();
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().variableName("flow1_activiti_conditions").singleResult();
        assertThat(variable).isNotNull();
        assertThat(variable.getVariableName()).isEqualTo("flow1_activiti_conditions");
        ArrayNode conditions = (ArrayNode) variable.getValue();
        assertThat(conditions)
                .extracting(JsonNode::asString)
                .containsExactly("hello", "world");
    }
}
