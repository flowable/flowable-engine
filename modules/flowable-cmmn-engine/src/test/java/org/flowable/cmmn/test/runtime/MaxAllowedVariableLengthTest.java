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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.BYTE_ARRAY;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.RandomStringUtils;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class MaxAllowedVariableLengthTest extends CustomCmmnConfigurationFlowableTestCase {

    @Override
    protected String getEngineName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setMaxAllowedLengthVariableType(5000);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void stringVariable() {
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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void jsonVariable() {
        ObjectNode customer = cmmnEngineConfiguration.getObjectMapper()
                .createObjectNode();
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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void bytesVariable() {
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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void serializableVariable() {
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

    protected static class Customer implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String name;

        protected Customer(String name) {
            this.name = name;
        }
    }
}
