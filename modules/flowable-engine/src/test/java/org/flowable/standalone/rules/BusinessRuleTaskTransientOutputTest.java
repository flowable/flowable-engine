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
package org.flowable.standalone.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class BusinessRuleTaskTransientOutputTest extends ResourceFlowableTestCase {

    public BusinessRuleTaskTransientOutputTest() {
        super("org/flowable/standalone/rules/rules.flowable.cfg.xml");
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/rules/rulesTransientOutputTestProcess.bpmn20.xml", "org/flowable/standalone/rules/simpleRule1.drl" })
    public void testRuleOutputIsTransientWhenSerializableVariablesAreDisabled() {
        Order order = new Order();
        order.setItemCount(2);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("rulesTransientOutput")
                .transientVariable("order", order)
                .start();

        assertThat(order.isValid()).isTrue();

        // The rule output is available until the next wait state, but it is not persisted
        assertThat(runtimeService.getVariable(processInstance.getId(), "rulesOutputSize")).isEqualTo(1);
        assertThat(runtimeService.hasVariable(processInstance.getId(), "rulesOutput")).isFalse();
    }
}
