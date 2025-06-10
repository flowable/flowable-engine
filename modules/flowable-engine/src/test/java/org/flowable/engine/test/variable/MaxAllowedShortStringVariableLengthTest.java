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
package org.flowable.engine.test.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.RandomStringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class MaxAllowedShortStringVariableLengthTest extends CustomConfigurationFlowableTestCase {

    public MaxAllowedShortStringVariableLengthTest() {
        super(MaxAllowedShortStringVariableLengthTest.class.getSimpleName());
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        // The max length is usually 2000 / 4000, so we want to test that the maxAllowedLengthVariableType is applied for the short strings as well
        processEngineConfiguration.setMaxAllowedLengthVariableType(500);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void stringVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("stringVar", "Test value")
                .start();

        assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(600)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage(
                        "The length of the string value exceeds the maximum allowed length of 500 characters. Current length: 600, for variable: stringVar in scope bpmn with id "
                                + processInstance.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThatThrownBy(() -> taskService.setVariableLocal(task.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(600)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage(
                        "The length of the string value exceeds the maximum allowed length of 500 characters. Current length: 600, for variable: stringVar in scope task with id "
                                + task.getId());

        assertThat(runtimeService.getVariable(processInstance.getId(), "stringVar")).isEqualTo("Test value");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void longStringVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("stringVar", "Test value")
                .start();

        assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(6000)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage(
                        "The length of the longString value exceeds the maximum allowed length of 500 characters. Current length: 6000, for variable: stringVar in scope bpmn with id "
                                + processInstance.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThatThrownBy(() -> taskService.setVariableLocal(task.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(6000)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage(
                        "The length of the longString value exceeds the maximum allowed length of 500 characters. Current length: 6000, for variable: stringVar in scope task with id "
                                + task.getId());

        assertThat(runtimeService.getVariable(processInstance.getId(), "stringVar")).isEqualTo("Test value");
    }

}
