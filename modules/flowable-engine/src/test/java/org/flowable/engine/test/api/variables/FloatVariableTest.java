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
package org.flowable.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.FloatType;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class FloatVariableTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testFloatVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("floatVar", 1.1f)
                .variable("maxFloatVar", Float.MAX_VALUE)
                .start();

        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "floatVar");
        assertThat(variableInstance.getTypeName()).isEqualTo(FloatType.TYPE_NAME);
        assertThat(variableInstance.getValue()).isEqualTo(1.1f);

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("floatVar", 1.1f),
                        entry("maxFloatVar", Float.MAX_VALUE)
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("floatVar", 1.1f).list())
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance.getId());
        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("floatVar", 1.0f).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("floatVar", 1.2f).count()).isZero();

        runtimeService.setVariable(processInstance.getId(), "floatVar", 2.5f);
        assertThat(runtimeService.getVariable(processInstance.getId(), "floatVar")).isEqualTo(2.5f);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("floatVar")
                    .singleResult();
            assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo(FloatType.TYPE_NAME);
            assertThat(historicVariableInstance.getValue()).isEqualTo(2.5f);
        }
    }
}
