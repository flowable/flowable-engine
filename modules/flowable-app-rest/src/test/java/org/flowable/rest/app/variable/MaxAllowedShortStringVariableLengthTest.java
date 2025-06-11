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
package org.flowable.rest.app.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.RandomStringUtils;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.spring.impl.test.FlowableCmmnSpringExtension;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.variable.MaxAllowedLengthVariableVerifier;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest
@ExtendWith({
        FlowableSpringExtension.class,
        FlowableCmmnSpringExtension.class,
})
public class MaxAllowedShortStringVariableLengthTest {

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    protected CmmnTaskService cmmnTaskService;

    @Autowired
    protected ObjectMapper objectMapper;

    @Nested
    class Bpmn {

        @Test
        @Deployment(resources = "oneTaskProcess.bpmn20.xml")
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
        @Deployment(resources = "oneTaskProcess.bpmn20.xml")
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

    @Nested
    class Cmmn {

        @Test
        @CmmnDeployment(resources = "oneHumanTaskCase.cmmn")
        void stringVariable() {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .variable("stringVar", "Test value")
                    .start();

            assertThatThrownBy(() -> cmmnRuntimeService.setVariable(caseInstance.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(600)))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the string value exceeds the maximum allowed length of 500 characters. Current length: 600, for variable: stringVar in scope cmmn with id "
                                    + caseInstance.getId());

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> cmmnTaskService.setVariableLocal(task.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(600)))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the string value exceeds the maximum allowed length of 500 characters. Current length: 600, for variable: stringVar in scope task with id "
                                    + task.getId());

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("Test value");
        }

        @Test
        @CmmnDeployment(resources = "oneHumanTaskCase.cmmn")
        void longStringVariable() {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .variable("stringVar", "Test value")
                    .start();

            assertThatThrownBy(() -> cmmnRuntimeService.setVariable(caseInstance.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(6000)))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the longString value exceeds the maximum allowed length of 500 characters. Current length: 6000, for variable: stringVar in scope cmmn with id "
                                    + caseInstance.getId());

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> cmmnTaskService.setVariableLocal(task.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(6000)))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the longString value exceeds the maximum allowed length of 500 characters. Current length: 6000, for variable: stringVar in scope task with id "
                                    + task.getId());

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("Test value");
        }

    }

    @TestConfiguration
    static class CustomConfiguration {

        @Bean
        public EngineConfigurationConfigurer<SpringAppEngineConfiguration> customAppEngineConfigurationConfigurer() {
            return appEngineConfiguration -> {
                appEngineConfiguration.setVariableLengthVerifier(new MaxAllowedLengthVariableVerifier(500));
            };
        }
    }

}
