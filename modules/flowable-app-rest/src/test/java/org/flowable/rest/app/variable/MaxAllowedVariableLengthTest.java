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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.BYTE_ARRAY;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.RandomStringUtils;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.spring.impl.test.FlowableCmmnSpringExtension;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest
@ExtendWith({
        FlowableSpringExtension.class,
        FlowableCmmnSpringExtension.class,
})
public class MaxAllowedVariableLengthTest {

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

            assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(5001)))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the longString value exceeds the maximum allowed length of 5000 characters. Current length: 5001, for variable: stringVar in scope bpmn with id "
                                    + processInstance.getId());

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> taskService.setVariableLocal(task.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(5001)))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the longString value exceeds the maximum allowed length of 5000 characters. Current length: 5001, for variable: stringVar in scope task with id "
                                    + task.getId());

            assertThat(runtimeService.getVariable(processInstance.getId(), "stringVar")).isEqualTo("Test value");
        }

        @Test
        @Deployment(resources = "oneTaskProcess.bpmn20.xml")
        void jsonVariable() {
            ObjectNode customer = objectMapper.createObjectNode();
            customer.put("name", "John Doe");
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneTaskProcess")
                    .variable("jsonVar", customer)
                    .start();

            customer.put("lastName", RandomStringUtils.insecure().nextAlphanumeric(5001));
            assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "jsonVar", customer))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the json value exceeds the maximum allowed length of 5000 characters. Current length: 5034, for variable: jsonVar in scope bpmn with id "
                                    + processInstance.getId());

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> taskService.setVariableLocal(task.getId(), "jsonVar", customer))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the json value exceeds the maximum allowed length of 5000 characters. Current length: 5034, for variable: jsonVar in scope task with id "
                                    + task.getId());

            assertThatJson(runtimeService.getVariable(processInstance.getId(), "jsonVar"))
                    .isEqualTo("{ name:  'John Doe' }");
        }

        @Test
        @Deployment(resources = "oneTaskProcess.bpmn20.xml")
        void bytesVariable() {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneTaskProcess")
                    .variable("bytesVar", "Test".getBytes(StandardCharsets.UTF_8))
                    .start();

            byte[] largeBytes = RandomStringUtils.insecure().nextAlphanumeric(5001).getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "bytesVar", largeBytes))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the bytes value exceeds the maximum allowed length of 5000 characters. Current length: 5001, for variable: bytesVar in scope bpmn with id "
                                    + processInstance.getId());

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> taskService.setVariableLocal(task.getId(), "bytesVar", largeBytes))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the bytes value exceeds the maximum allowed length of 5000 characters. Current length: 5001, for variable: bytesVar in scope task with id "
                                    + task.getId());

            assertThat(runtimeService.getVariable(processInstance.getId(), "bytesVar", byte[].class))
                    .asString(StandardCharsets.UTF_8)
                    .isEqualTo("Test");
        }

        @Test
        @Deployment(resources = "oneTaskProcess.bpmn20.xml")
        void serializableVariable() {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneTaskProcess")
                    .variable("serializableVar", new Customer("Test"))
                    .start();

            Customer largeCustomer = new Customer(RandomStringUtils.insecure().nextAlphanumeric(5001));
            assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "serializableVar", largeCustomer))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessageStartingWith("The length of the serializable value exceeds the maximum allowed length of 5000 characters. Current length: ")
                    .hasMessageEndingWith(", for variable: serializableVar in scope bpmn with id " + processInstance.getId());

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> taskService.setVariableLocal(task.getId(), "serializableVar", largeCustomer))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessageStartingWith("The length of the serializable value exceeds the maximum allowed length of 5000 characters. Current length: ")
                    .hasMessageEndingWith(", for variable: serializableVar in scope task with id " + task.getId());

            assertThat(runtimeService.getVariable(processInstance.getId(), "serializableVar"))
                    .isInstanceOfSatisfying(Customer.class, customer -> assertThat(customer.name).isEqualTo("Test"));
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

            assertThatThrownBy(() -> cmmnRuntimeService.setVariable(caseInstance.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(5001)))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the longString value exceeds the maximum allowed length of 5000 characters. Current length: 5001, for variable: stringVar in scope cmmn with id "
                                    + caseInstance.getId());

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> cmmnTaskService.setVariableLocal(task.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(5001)))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the longString value exceeds the maximum allowed length of 5000 characters. Current length: 5001, for variable: stringVar in scope task with id "
                                    + task.getId());

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("Test value");
        }

        @Test
        @CmmnDeployment(resources = "oneHumanTaskCase.cmmn")
        void jsonVariable() {
            ObjectNode customer = objectMapper.createObjectNode();
            customer.put("name", "John Doe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .variable("jsonVar", customer)
                    .start();

            customer.put("lastName", RandomStringUtils.insecure().nextAlphanumeric(5001));
            assertThatThrownBy(() -> cmmnRuntimeService.setVariable(caseInstance.getId(), "jsonVar", customer))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the json value exceeds the maximum allowed length of 5000 characters. Current length: 5034, for variable: jsonVar in scope cmmn with id "
                                    + caseInstance.getId());

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> cmmnTaskService.setVariableLocal(task.getId(), "jsonVar", customer))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the json value exceeds the maximum allowed length of 5000 characters. Current length: 5034, for variable: jsonVar in scope task with id "
                                    + task.getId());

            assertThatJson(cmmnRuntimeService.getVariable(caseInstance.getId(), "jsonVar"))
                    .isEqualTo("{ name:  'John Doe' }");
        }

        @Test
        @CmmnDeployment(resources = "oneHumanTaskCase.cmmn")
        void bytesVariable() {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .variable("bytesVar", "Test".getBytes(StandardCharsets.UTF_8))
                    .start();

            byte[] largeBytes = RandomStringUtils.insecure().nextAlphanumeric(5001).getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> cmmnRuntimeService.setVariable(caseInstance.getId(), "bytesVar", largeBytes))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the bytes value exceeds the maximum allowed length of 5000 characters. Current length: 5001, for variable: bytesVar in scope cmmn with id "
                                    + caseInstance.getId());

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> cmmnTaskService.setVariableLocal(task.getId(), "bytesVar", largeBytes))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage(
                            "The length of the bytes value exceeds the maximum allowed length of 5000 characters. Current length: 5001, for variable: bytesVar in scope task with id "
                                    + task.getId());

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "bytesVar"))
                    .asInstanceOf(BYTE_ARRAY)
                    .asString(StandardCharsets.UTF_8)
                    .isEqualTo("Test");
        }

        @Test
        @CmmnDeployment(resources = "oneHumanTaskCase.cmmn")
        void serializableVariable() {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .variable("serializableVar", new Customer("Test"))
                    .start();

            Customer largeCustomer = new Customer(RandomStringUtils.insecure().nextAlphanumeric(5001));
            assertThatThrownBy(() -> cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", largeCustomer))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessageStartingWith("The length of the serializable value exceeds the maximum allowed length of 5000 characters. Current length: ")
                    .hasMessageEndingWith(", for variable: serializableVar in scope cmmn with id " + caseInstance.getId());

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThatThrownBy(() -> cmmnTaskService.setVariableLocal(task.getId(), "serializableVar", largeCustomer))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessageStartingWith("The length of the serializable value exceeds the maximum allowed length of 5000 characters. Current length: ")
                    .hasMessageEndingWith(", for variable: serializableVar in scope task with id " + task.getId());

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "serializableVar"))
                    .isInstanceOfSatisfying(Customer.class, customer -> assertThat(customer.name).isEqualTo("Test"));
        }
    }

    @TestConfiguration
    static class CustomConfiguration {

        @Bean
        public EngineConfigurationConfigurer<SpringAppEngineConfiguration> customAppEngineConfigurationConfigurer() {
            return appEngineConfiguration -> {
                appEngineConfiguration.setMaxAllowedLengthVariableType(5000);
            };
        }
    }

    protected static class Customer implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String name;

        protected Customer(String name) {
            this.name = name;
        }
    }
}
