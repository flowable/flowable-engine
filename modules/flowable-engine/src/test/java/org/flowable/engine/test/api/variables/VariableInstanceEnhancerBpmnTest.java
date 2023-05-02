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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.DefaultVariableInstanceEnhancer;
import org.flowable.variable.service.impl.VariableInstanceEnhancer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Arthur Hupka-Merle
 */
public class VariableInstanceEnhancerBpmnTest extends PluggableFlowableTestCase {

    VariableInstanceEnhancer originalEnhancer;

    @BeforeEach
    public void setUp() {
        originalEnhancer = processEngineConfiguration.getVariableServiceConfiguration().getVariableInstanceEnhancer();
    }

    @AfterEach
    public void tearDown() {
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(originalEnhancer);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
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
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", 1L);
        variables.put("OtherVariable", "Hello World");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThatThrownBy(() -> {
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            variables.put("orderId", -1L);
            variables.put("OtherVariable", "Hello World update");
            taskService.complete(task.getId(), variables);
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" }, tenantId = "myTenant")
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
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", 1L);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKeyAndTenantId("oneTaskProcess", variables, "myTenant");

        assertThatThrownBy(() -> {
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            variables.put("orderId", -1L);
            taskService.complete(task.getId(), variables);
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");

        assertThat(preSetVariableTenantId).containsExactly("myTenant", "myTenant");
        assertThat(postSetVariableTenantId).containsExactly("myTenant", "myTenant");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
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
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", -1L);
        variables.put("OtherVariable", "Hello World");

        assertThatThrownBy(() -> {
            runtimeService.createProcessInstanceBuilder().transientVariables(variables).processDefinitionKey("oneTaskProcess").start();
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testEnhanceVariableWithMetaInfo() {

        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        List<Object> preSetValueCalls = new LinkedList<>();
        List<Object> postSetValueCalls = new LinkedList<>();
        DefaultVariableInstanceEnhancer enhancer = new DefaultVariableInstanceEnhancer() {

            @Override
            public Object preSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue) {
                preSetValueCalls.add(originalValue);
                assertThat(variableInstance.getProcessInstanceId()).isNotNull();
                assertThat(variableInstance.getProcessDefinitionId()).isNotNull();
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
                assertThat(variableInstance.getProcessInstanceId()).isNotNull();
                assertThat(variableInstance.getProcessDefinitionId()).isNotNull();
                if (originalValue instanceof String) {
                    assertThat(originalValue).isNotSameAs(variableValue);
                    assertThat(originalValue).isEqualTo("myValue1");
                    assertThat(variableValue).isEqualTo("myValue1Enhanced");
                }
            }
        };
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("myEnhancedStringVariable", "myValue1")
                .variable("myIntVariable", 1)
                .start();

        Map<String, Object> processVariables = processInstance.getProcessVariables();
        assertThat(processVariables.get("myEnhancedStringVariable")).isInstanceOf(String.class);
        Object stringVariableValue = runtimeService.getVariable(processInstance.getId(), "myEnhancedStringVariable");
        assertThat(stringVariableValue).isEqualTo("myValue1Enhanced");
        VariableInstance stringVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "myEnhancedStringVariable");
        assertThat(stringVariableInstance.getMetaInfo()).isEqualTo("{\"byteLength\":\"8\"}");

        assertThat(processVariables.get("myIntVariable")).isInstanceOf(Integer.class);
        Object intVariableValue = runtimeService.getVariable(processInstance.getId(), "myIntVariable");
        assertThat(intVariableValue).isEqualTo(1);

        VariableInstance intVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "myIntVariable");
        assertThat(intVariableInstance.getMetaInfo()).isNull();

        assertThat(preSetValueCalls).containsExactlyInAnyOrder("myValue1", 1);
        assertThat(postSetValueCalls).containsExactlyInAnyOrder("myValue1Enhanced", 1);
    }

    static class VariableMeta {

        @JsonProperty("byteLength")
        public String byteLength;

    }

}
