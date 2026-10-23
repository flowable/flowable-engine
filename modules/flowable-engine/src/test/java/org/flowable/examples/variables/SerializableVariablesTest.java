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
package org.flowable.examples.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;
import org.junit.jupiter.api.Test;

/**
 * Variable tests that rely on serializable variables, which are disabled by default.
 */
public class SerializableVariablesTest extends CustomConfigurationFlowableTestCase {

    public SerializableVariablesTest() {
        super(SerializableVariablesTest.class.getSimpleName());
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setSerializableVariableTypeMode(SerializableVariableTypeMode.ENABLED);
    }

    // Test case for ACT-1839
    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testChangeTypeSerializable.bpmn20.xml" })
    public void testChangeTypeSerializable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variable-type-change-test");
        assertThat(processInstance).isNotNull();
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Activiti is awesome!");
        SomeSerializable myVar = (SomeSerializable) runtimeService.getVariable(processInstance.getId(), "myVar");
        assertThat(myVar.getValue()).isEqualTo("someValue");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testBasicVariableOperations.bpmn20.xml" })
    public void testEmptyCollectionVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("taskAssigneeProcess")
                .variable("listVar", new ArrayList<>())
                .variable("setVar", new HashSet<>())
                .start();

        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "listVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("serializable");
        assertThat(variableInstance.getValue()).asInstanceOf(LIST).isEmpty();

        variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "setVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("serializable");
        assertThat(variableInstance.getValue())
                .isInstanceOfSatisfying(Set.class, set -> assertThat(set).isEmpty());
    }

}
