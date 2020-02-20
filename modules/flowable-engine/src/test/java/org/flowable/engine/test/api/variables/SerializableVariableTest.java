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
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class SerializableVariableTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testUpdateSerializableInServiceTask() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("myVar", new TestSerializableVariable(1));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testUpdateSerializableInServiceTask", vars);

        // There is a task here, such the VariableInstanceEntityImpl is inserter first, and updated later
        // (instead of being inserted/updated in the same Tx)
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        TestSerializableVariable testSerializableVariable = (TestSerializableVariable) runtimeService.getVariable(processInstance.getId(), "myVar");
        assertEquals(2, testSerializableVariable.getNumber());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            testSerializableVariable = (TestSerializableVariable) managementService.executeCommand(commandContext -> historyService.createHistoricVariableInstanceQuery().variableName("myVar").singleResult().getValue());
            assertEquals(2, testSerializableVariable.getNumber());
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetSerializableValueFromValueInstance() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("var", new TestSerializableVariable(10))
                .start();

        assertThat(runtimeService.getVariable(processInstance.getId(), "var", TestSerializableVariable.class))
                .extracting(TestSerializableVariable::getNumber)
                .isEqualTo(10);

        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "var");
        assertThat(variableInstance).isNotNull();

        assertThat(variableInstance.getValue())
                .asInstanceOf(type(TestSerializableVariable.class))
                .extracting(TestSerializableVariable::getNumber)
                .isEqualTo(10);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId())
                    .variableName("var")
                    .singleResult();

            assertThat(historicVariableInstance.getValue())
                    .asInstanceOf(type(TestSerializableVariable.class))
                    .extracting(TestSerializableVariable::getNumber)
                    .isEqualTo(10);
        }
    }

    public static class TestUpdateSerializableVariableDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            TestSerializableVariable var = (TestSerializableVariable) execution.getVariable("myVar");
            var.setNumber(2);
        }

    }

    public static class TestSerializableVariable implements Serializable {

        private static final long serialVersionUID = 1L;
        private int number;

        public TestSerializableVariable(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

    }

}
