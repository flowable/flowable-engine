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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.DefaultVariableInstanceValueModifier;
import org.flowable.variable.service.impl.VariableInstanceValueModifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This test checks the basic functionality of the {@link VariableInstanceValueModifier} implementation.
 * It ensures that the modifier is called when setting a variable and that it can be replaced with a custom implementation.
 * Additional tests are available in <code>VariableInstanceValueModifierBpmnTest</code>, as BPMN shares the same implementation of the VariableService.
 *
 * @author Arthur Hupka-Merle
 */
public class VariableInstanceValueModifierCmmnTest extends FlowableCmmnTestCase {

    VariableInstanceValueModifier originalModifier;

    @Before
    public void setUp() {
        originalModifier = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableInstanceValueModifier();
    }

    @After
    public void tearDown() {
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceValueModifier(originalModifier);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/one-human-task-model.cmmn" })
    public void testCompleteTaskWithExceptionInPostSetVariable() {
        DefaultVariableInstanceValueModifier modifier = new DefaultVariableInstanceValueModifier(cmmnEngineConfiguration.getVariableServiceConfiguration()) {

            @Override
            protected void setOrUpdateValue(VariableInstance variableInstance, Object value, String tenantId) {
                if (variableInstance.getName().equals("orderId")) {
                    if (((Number) value).longValue() < 0) {
                        throw new FlowableIllegalArgumentException("Invalid type: value should be larger than zero");
                    }
                }
                super.setOrUpdateValue(variableInstance, value, tenantId);
            }
        };
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceValueModifier(modifier);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", 1L);
        variables.put("OtherVariable", "Hello World");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThatThrownBy(() -> {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            variables.put("orderId", -1L);
            variables.put("OtherVariable", "Hello World update");
            cmmnTaskService.complete(task.getId(), variables);
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/one-human-task-model.cmmn" })
    public void testTransientVariables() {
        DefaultVariableInstanceValueModifier modifier = new DefaultVariableInstanceValueModifier(cmmnEngineConfiguration.getVariableServiceConfiguration()) {

            @Override
            protected void setOrUpdateValue(VariableInstance variableInstance, Object value, String tenantId) {
                if (variableInstance.getName().equals("orderId")) {
                    if (((Number) value).longValue() < 0) {
                        throw new FlowableIllegalArgumentException("Invalid type: value should be larger than zero");
                    }
                }
            }
        };
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceValueModifier(modifier);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", -1L);
        variables.put("OtherVariable", "Hello World");

        assertThatThrownBy(() -> {
            cmmnRuntimeService.createCaseInstanceBuilder().transientVariables(variables).caseDefinitionKey("oneTaskCase").start();
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");
    }
}
