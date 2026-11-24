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
package org.activiti.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.activiti.engine.impl.variable.SerializableType;
import org.activiti.examples.variables.SomeSerializable;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;

/**
 * Tests the serializable variable type mode of the embedded Flowable 5 engine. By default the serializable variable type is disabled.
 *
 * @author Filip Hrisafov
 */
public class SerializableVariableTypeModeTest extends PluggableFlowableTestCase {

    public void testDefaultModeIsDisabled() {
        VariableType serializableType = getFlowable5VariableTypes().getVariableType(SerializableType.TYPE_NAME);
        assertThat(serializableType).isInstanceOf(SerializableType.class);
        assertThat(((SerializableType) serializableType).getMode()).isEqualTo(SerializableVariableTypeMode.DISABLED);
    }

    @Deployment(resources = "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testDisabledMode() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "otherVar", new SomeSerializable("other")))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Storing the value of variable 'otherVar' of type " + SomeSerializable.class.getName()
                        + " is not allowed, because serializable variables are disabled");
        assertThat(runtimeService.hasVariable(processInstance.getId(), "otherVar")).isFalse();

        // Existing serializable variable, e.g. created before serializable variables were disabled
        withSerializableVariableTypeMode(SerializableVariableTypeMode.ENABLED,
                () -> runtimeService.setVariable(processInstance.getId(), "serializableVar", new SomeSerializable("test")));

        assertThatThrownBy(() -> runtimeService.getVariable(processInstance.getId(), "serializableVar"))
                .hasMessageContaining("Reading the value of serializable variable 'serializableVar' is not allowed, because serializable variables are disabled");

        // Removing the variable and deleting the process instance is possible
        runtimeService.removeVariable(processInstance.getId(), "serializableVar");
        assertThat(runtimeService.hasVariable(processInstance.getId(), "serializableVar")).isFalse();

        withSerializableVariableTypeMode(SerializableVariableTypeMode.ENABLED,
                () -> runtimeService.setVariable(processInstance.getId(), "serializableVar", new SomeSerializable("test")));
        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Deployment(resources = "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testReadOnlyMode() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        withSerializableVariableTypeMode(SerializableVariableTypeMode.ENABLED,
                () -> runtimeService.setVariable(processInstance.getId(), "serializableVar", new SomeSerializable("test")));

        withSerializableVariableTypeMode(SerializableVariableTypeMode.READ_ONLY, () -> {
            assertThat(runtimeService.getVariable(processInstance.getId(), "serializableVar"))
                    .isInstanceOfSatisfying(SomeSerializable.class, value -> assertThat(value.getValue()).isEqualTo("test"));

            assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "serializableVar", new SomeSerializable("updated")))
                    .hasMessageContaining("Storing the value of variable 'serializableVar' of type " + SomeSerializable.class.getName()
                            + " is not allowed");

            assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "otherVar", new SomeSerializable("other")))
                    .hasMessageContaining("Storing the value of variable 'otherVar' of type " + SomeSerializable.class.getName()
                            + " is not allowed");

            assertThat(runtimeService.getVariable(processInstance.getId(), "serializableVar"))
                    .isInstanceOfSatisfying(SomeSerializable.class, value -> assertThat(value.getValue()).isEqualTo("test"));

            // Migrating the existing variable to another type is possible
            runtimeService.setVariable(processInstance.getId(), "serializableVar", "migrated");
            assertThat(runtimeService.getVariable(processInstance.getId(), "serializableVar")).isEqualTo("migrated");
        });
    }

    @Deployment(resources = "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testFloatVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // Float values do not need the serializable variable type
        runtimeService.setVariable(processInstance.getId(), "floatVar", 1.1f);

        assertThat(getFlowable5VariableTypes().findVariableType(1.1f).getTypeName()).isEqualTo("float");
        assertThat(runtimeService.getVariable(processInstance.getId(), "floatVar")).isEqualTo(1.1f);
        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("floatVar", 1.1f).count()).isEqualTo(1);

        runtimeService.setVariable(processInstance.getId(), "floatVar", 2.5f);
        assertThat(runtimeService.getVariable(processInstance.getId(), "floatVar")).isEqualTo(2.5f);
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("floatVar")
                .singleResult().getValue()).isEqualTo(2.5f);
    }

    protected VariableTypes getFlowable5VariableTypes() {
        ProcessEngineConfigurationImpl flowable5ProcessEngineConfiguration = (ProcessEngineConfigurationImpl) processEngineConfiguration
                .getFlowable5CompatibilityHandler().getRawProcessConfiguration();
        return flowable5ProcessEngineConfiguration.getVariableTypes();
    }

    /**
     * Temporarily changes the mode of the serializable variable type of the Flowable 5 engine.
     */
    protected void withSerializableVariableTypeMode(SerializableVariableTypeMode mode, Runnable runnable) {
        VariableTypes variableTypes = getFlowable5VariableTypes();
        VariableType defaultType = variableTypes.getVariableType(SerializableType.TYPE_NAME);
        int typeIndex = variableTypes.getTypeIndex(defaultType);
        SerializableType type = new SerializableType(mode);
        variableTypes.removeType(defaultType);
        variableTypes.addType(type, typeIndex);
        try {
            runnable.run();
        } finally {
            variableTypes.removeType(type);
            variableTypes.addType(defaultType, typeIndex);
        }
    }
}
