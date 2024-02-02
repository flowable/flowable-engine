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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class RulesDeployerTest extends ResourceFlowableTestCase {

    public RulesDeployerTest() {
        super("org/flowable/standalone/rules/rules.flowable.cfg.xml");
    }

    @SuppressWarnings("unchecked")
    @Test
    @Deployment(resources = { "org/flowable/standalone/rules/rulesDeploymentTestProcess.bpmn20.xml", "org/flowable/standalone/rules/simpleRule1.drl" })
    public void testRulesDeployment() {
        Map<String, Object> variableMap = new HashMap<>();
        Order order = new Order();
        order.setItemCount(2);
        variableMap.put("order", order);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("rulesDeployment", variableMap);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionId()).startsWith("rulesDeployment:1");

        runtimeService.getVariable(processInstance.getId(), "order");
        assertThat(order.isValid()).isTrue();

        Collection<Object> ruleOutputList = (Collection<Object>) runtimeService.getVariable(processInstance.getId(), "rulesOutput");
        assertThat(ruleOutputList).hasSize(1);
        order = (Order) ruleOutputList.iterator().next();
        assertThat(order.isValid()).isTrue();
    }
}
