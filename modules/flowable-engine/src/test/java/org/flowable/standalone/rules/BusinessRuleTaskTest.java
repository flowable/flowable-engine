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

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class BusinessRuleTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testBusinessRuleTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("customRule");
        assertThat(runtimeService.getVariable(processInstance.getId(), "test")).isEqualTo("test2");

        assertThat(CustomBusinessRuleTask.ruleInputVariables).hasSize(1);
        assertThat(CustomBusinessRuleTask.ruleInputVariables.get(0).getExpressionText()).isEqualTo("order");

        assertThat(CustomBusinessRuleTask.ruleIds).hasSize(2);
        assertThat(CustomBusinessRuleTask.ruleIds.get(0).getExpressionText()).isEqualTo("rule1");
        assertThat(CustomBusinessRuleTask.ruleIds.get(1).getExpressionText()).isEqualTo("rule2");

        assertThat(CustomBusinessRuleTask.exclude).isTrue();
        assertThat(CustomBusinessRuleTask.resultVariableName).isEqualTo("rulesOutput");

        runtimeService.trigger(runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .onlyChildExecutions()
                .singleResult()
                .getId());
        assertProcessEnded(processInstance.getId());
    }
}
