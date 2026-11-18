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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.FloatType;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class FloatVariableTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testFloatVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("floatVar", 1.1f)
                .variable("maxFloatVar", Float.MAX_VALUE)
                .start();

        VariableInstance variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "floatVar");
        assertThat(variableInstance.getTypeName()).isEqualTo(FloatType.TYPE_NAME);
        assertThat(variableInstance.getValue()).isEqualTo(1.1f);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("floatVar", 1.1f),
                        entry("maxFloatVar", Float.MAX_VALUE)
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("floatVar", 1.1f).list())
                .extracting(CaseInstance::getId)
                .containsExactly(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("floatVar", 1.0f).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("floatVar", 1.2f).count()).isZero();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "floatVar", 2.5f);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "floatVar")).isEqualTo(2.5f);

        HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("floatVar")
                .singleResult();
        assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo(FloatType.TYPE_NAME);
        assertThat(historicVariableInstance.getValue()).isEqualTo(2.5f);
    }
}
