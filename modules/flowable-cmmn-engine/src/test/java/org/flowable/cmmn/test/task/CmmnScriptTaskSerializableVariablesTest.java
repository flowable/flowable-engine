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
package org.flowable.cmmn.test.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.EngineConfigurer;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;
import org.junit.jupiter.api.Test;

/**
 * Tests for script tasks working with Java object (serializable) variables.
 * These need an engine with serializable variables enabled.
 *
 * @author Dennis Federico
 * @author Filip Hrisafov
 */
public class CmmnScriptTaskSerializableVariablesTest extends CustomCmmnConfigurationFlowableTestCase {

    @EngineConfigurer
    protected static void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setSerializableVariableTypeMode(SerializableVariableTypeMode.ENABLED);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnScriptTaskTest.testObjectVariables.cmmn")
    public void testObjectVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", new IntValueHolder(3))
                .variable("b", 7)
                .start();
        assertThat(caseInstance).isNotNull();

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult")).isTrue();
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result).isInstanceOf(Number.class);
        assertThat(((Number) result).intValue()).isEqualTo(12);

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "a")).isTrue();
        Object a = cmmnRuntimeService.getVariable(caseInstance.getId(), "a");
        assertThat(a).isInstanceOf(IntValueHolder.class);
        assertThat(((IntValueHolder) a).getValue()).isEqualTo(5);

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnScriptTaskTest.testReturnJavaObject.cmmn")
    public void testReturnJavaObject() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", new IntValueHolder(3))
                .variable("b", 7)
                .start();
        assertThat(caseInstance).isNotNull();

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult")).isTrue();
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result).isInstanceOf(IntValueHolder.class);
        assertThat(((IntValueHolder) result).getValue()).isEqualTo(10);

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    private void endTestCase() {
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("blockerPlanItem").singleResult();
        assertThat(planItemInstance).isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
    }

    public static class IntValueHolder implements Serializable {

        private int value;

        public IntValueHolder(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "IntValueHolder{" +
                    "value=" + value +
                    '}';
        }
    }
}
