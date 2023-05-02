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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.DefaultVariableInstanceEnhancer;
import org.flowable.variable.service.impl.VariableInstanceEnhancer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author: Arthur Hupka-Merle
 */
public class VariableInstanceEnhancerCmmnTest extends FlowableCmmnTestCase {

    VariableInstanceEnhancer originalEnhancer;

    @Before
    public void setUp() {
        originalEnhancer = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableInstanceEnhancer();
    }

    @After
    public void tearDown() {
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(originalEnhancer);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/one-human-task-model.cmmn" })
    public void testCompleteTaskWithExceptionInPostSetVariable() {
        DefaultVariableInstanceEnhancer enhancer = new DefaultVariableInstanceEnhancer() {

            @Override
            public void postSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue, Object variableValue) {
                if (variableInstance.getName().equals("orderId")) {
                    if (((Number) originalValue).longValue() < 0) {
                        throw new FlowableIllegalArgumentException("Invalid type: value should be larger than zero");
                    }
                }
            }
        };
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

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
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/one-human-task-model.cmmn" }, tenantId = "myTenant")
    public void testCompleteTaskWithExceptionInPostSetVariableWithTenantId() {
        List<String> preSetVariableTenantId = new LinkedList<>();
        List<String> postSetVariableTenantId = new LinkedList<>();
        DefaultVariableInstanceEnhancer enhancer = new DefaultVariableInstanceEnhancer() {

            @Override
            public Object preSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue) {
                preSetVariableTenantId.add(tenantId);
                return originalValue;
            }

            @Override
            public void postSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue, Object variableValue) {
                postSetVariableTenantId.add(tenantId);
                if (variableInstance.getName().equals("orderId")) {
                    if (((Number) originalValue).longValue() < 0) {
                        throw new FlowableIllegalArgumentException("Invalid type: value should be larger than zero");
                    }
                }
            }
        };
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", 1L);
        CaseInstance caseInstance = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .tenantId("myTenant")
                .start();

        assertThatThrownBy(() -> {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            variables.put("orderId", -1L);
            cmmnTaskService.complete(task.getId(), variables);
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");

        assertThat(preSetVariableTenantId).containsExactly("myTenant", "myTenant");
        assertThat(postSetVariableTenantId).containsExactly("myTenant", "myTenant");
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/one-human-task-model.cmmn" })
    public void testTransientVariables() {
        DefaultVariableInstanceEnhancer enhancer = new DefaultVariableInstanceEnhancer() {

            @Override
            public void postSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue, Object variableValue) {
                if (variableInstance.getName().equals("orderId")) {
                    if (((Number) originalValue).longValue() < 0) {
                        throw new FlowableIllegalArgumentException("Invalid type: value should be larger than zero");
                    }
                }
            }
        };
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", -1L);
        variables.put("OtherVariable", "Hello World");

        assertThatThrownBy(() -> {
            cmmnRuntimeService.createCaseInstanceBuilder().transientVariables(variables).caseDefinitionKey("oneTaskCase").start();
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testEnhanceVariableWithMetaInfo() {
        ObjectMapper objectMapper = cmmnEngineConfiguration.getObjectMapper();
        List<Object> preSetValueCalls = new LinkedList<>();
        List<Object> postSetValueCalls = new LinkedList<>();
        DefaultVariableInstanceEnhancer enhancer = new DefaultVariableInstanceEnhancer() {

            @Override
            public Object preSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue) {
                preSetValueCalls.add(originalValue);
                assertThat(variableInstance.getScopeId()).isNotNull();
                assertThat(variableInstance.getScopeDefinitionId()).isNotNull();
                if (originalValue instanceof String) {
                    VariableMeta variableMeta = new VariableMeta();
                    variableMeta.byteLength = String.valueOf(((String) originalValue).getBytes().length);
                    try {
                        variableInstance.setMetaInfo(objectMapper.writeValueAsString(variableMeta));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    return originalValue + "Enhanced";
                }
                return originalValue;
            }

            @Override
            public void postSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue, Object variableValue) {
                postSetValueCalls.add(variableValue);
                assertThat(variableInstance.getScopeId()).isNotNull();
                assertThat(variableInstance.getScopeDefinitionId()).isNotNull();
                if (originalValue instanceof String) {
                    assertThat(originalValue).isNotSameAs(variableValue);
                    assertThat(originalValue).isEqualTo("myValue1");
                    assertThat(variableValue).isEqualTo("myValue1Enhanced");
                }
            }
        };
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("myEnhancedStringVariable", "myValue1")
                .variable("myIntVariable", 1)
                .start();

        Map<String, Object> processVariables = caseInstance.getCaseVariables();
        assertThat(processVariables.get("myEnhancedStringVariable")).isInstanceOf(String.class);
        Object stringVariableValue = cmmnRuntimeService.getVariable(caseInstance.getId(), "myEnhancedStringVariable");
        assertThat(stringVariableValue).isEqualTo("myValue1Enhanced");
        VariableInstance stringVariableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "myEnhancedStringVariable");
        assertThat(stringVariableInstance.getMetaInfo()).isEqualTo("{\"byteLength\":\"8\"}");

        assertThat(processVariables.get("myIntVariable")).isInstanceOf(Integer.class);
        Object intVariableValue = cmmnRuntimeService.getVariable(caseInstance.getId(), "myIntVariable");
        assertThat(intVariableValue).isEqualTo(1);

        VariableInstance intVariableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "myIntVariable");
        assertThat(intVariableInstance.getMetaInfo()).isNull();

        assertThat(preSetValueCalls).containsExactlyInAnyOrder("myValue1", 1);
        assertThat(postSetValueCalls).containsExactlyInAnyOrder("myValue1Enhanced", 1);
    }

    static class VariableMeta {

        @JsonProperty("byteLength")
        public String byteLength;

    }

}
