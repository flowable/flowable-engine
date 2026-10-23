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
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.EngineConfigurer;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;
import org.junit.jupiter.api.Test;

/**
 * Tests for serializable variables. These need an engine with serializable variables enabled.
 *
 * @author Joram Barrez
 */
public class SerializableVariablesTest extends CustomCmmnConfigurationFlowableTestCase {

    @EngineConfigurer
    protected static void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setSerializableVariableTypeMode(SerializableVariableTypeMode.ENABLED);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/VariablesTest.testSerializableVariable.cmmn")
    public void testSerializableVariable() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVariable", new MyVariable("Hello World"));
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        MyVariable myVariable = (MyVariable) cmmnRuntimeService.getVariable(caseInstance.getId(), "myVariable");
        assertThat(myVariable.value).isEqualTo("Hello World");

        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                VariableInstance variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "myVariable");
                MyVariable myVariable = (MyVariable) variableInstance.getValue();
                assertThat(myVariable.value).isEqualTo("Hello World");

                return null;
            }
        });
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testIncludeVariablesWithSerializableVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("myVar", new CustomTestVariable("test", 123))
                .start();

        CaseInstance caseInstanceWithVariables = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();

        CustomTestVariable customTestVariable = (CustomTestVariable) caseInstanceWithVariables.getCaseVariables().get("myVar");
        assertThat(customTestVariable.someValue).isEqualTo("test");
        assertThat(customTestVariable.someInt).isEqualTo(123);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testEmptyCollectionVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("listVar", new ArrayList<>())
                .variable("setVar", new HashSet<>())
                .start();

        VariableInstance variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "listVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("serializable");
        assertThat(variableInstance.getValue()).asInstanceOf(LIST).isEmpty();

        variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "setVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("serializable");
        assertThat(variableInstance.getValue())
                .isInstanceOfSatisfying(Set.class, set -> assertThat(set).isEmpty());
    }

    public static class MyVariable implements Serializable {

        private static final long serialVersionUID = 1L;

        private String value;

        public MyVariable(String value) {
            this.value = value;
        }

    }

    public static class CustomTestVariable implements Serializable {

        private String someValue;
        private int someInt;

        public CustomTestVariable(String someValue, int someInt) {
            this.someValue = someValue;
            this.someInt = someInt;
        }
    }

}
