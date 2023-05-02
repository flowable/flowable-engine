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
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.impl.DefaultVariableInstanceEnhancer;
import org.flowable.variable.service.impl.VariableInstanceEnhancer;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.LongType;
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
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testChangeVariableType() {
        VariableTypes variableTypes = processEngineConfiguration.getVariableServiceConfiguration().getVariableTypes();
        DefaultVariableInstanceEnhancer enhancer = new DefaultVariableInstanceEnhancer() {

            @Override
            public VariableType determineVariableType(VariableInstanceEntity variableInstance, Object originalVariableValue, Object variableValue,
                    VariableType selectedType) {
                return variableValue instanceof Long ? variableTypes.getVariableType(LongType.TYPE_NAME) : selectedType;
            }

            @Override
            public Object preSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue) {
                String typeName = variableInstance.getTypeName();
                if ("string".equals(typeName) && originalValue instanceof Number) {
                    return Long.valueOf(originalValue.toString());
                }
                return originalValue;
            }
        };
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", "ABC");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess", variables);
        VariableInstance orderIdInstance = runtimeService.getVariableInstance(processInstance.getId(), "orderId");
        assertThat(orderIdInstance.getTypeName()).isEqualTo("string");
        assertThat(orderIdInstance.getValue()).isEqualTo("ABC");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("orderId", 1);
        taskService.complete(task.getId(), variables);

        VariableInstanceEntity orderIdInstanceTask = (VariableInstanceEntity) runtimeService.getVariableInstance(processInstance.getId(), "orderId");
        assertThat(orderIdInstanceTask.getTypeName()).isEqualTo("long");
        assertThat(orderIdInstanceTask.getValue()).isEqualTo(1L);

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testWrapVariableWithCustomType() {
        VariableTypes variableTypes = processEngineConfiguration.getVariableServiceConfiguration().getVariableTypes();
        variableTypes.addTypeBefore(new WrappedIntegerCustomType(), "integer");
        DefaultVariableInstanceEnhancer enhancer = new DefaultVariableInstanceEnhancer() {

            @Override
            public VariableType determineVariableType(VariableInstanceEntity variableInstance, Object originalVariableValue, Object variableValue,
                    VariableType selectedType) {
                return isSpecialType(originalVariableValue) ?
                        variableTypes.getVariableType(WrappedIntegerCustomType.TYPE_NAME) :
                        variableTypes.getVariableType(IntegerType.TYPE_NAME);
            }

            @Override
            public Object preSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue) {

                // new variable instance
                if (isSpecialType(originalValue)) {
                    String meta = originalValue + "meta";
                    variableInstance.setMetaInfo(meta);
                    return new WrappedIntegerValue((Integer) originalValue, variableInstance.getMetaInfo());
                }
                return originalValue;
            }

            private boolean isSpecialType(Object originalValue) {
                return originalValue instanceof Integer && ((Integer) originalValue) > 1000;
            }
        };

        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("simpleInteger", 1000);
        variables.put("wrappedInteger", 1001);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess", variables);

        VariableInstance simpleInteger = runtimeService.getVariableInstance(processInstance.getId(), "simpleInteger");
        assertThat(simpleInteger.getTypeName()).isEqualTo("integer");
        assertThat(simpleInteger.getValue()).isEqualTo(Integer.parseInt("1000"));

        VariableInstance wrappedInteger = runtimeService.getVariableInstance(processInstance.getId(), "wrappedInteger");
        assertThat(wrappedInteger.getTypeName()).isEqualTo("wrappedIntegerType");
        assertThat(wrappedInteger.getValue()).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
        });
        Object wrappedIntegerProcessVariable = processInstance.getProcessVariables().get("wrappedInteger");
        assertThat(wrappedIntegerProcessVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
        });

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("wrappedInteger", 1002);
        //variables.put("simpleInteger", 1001);
        taskService.complete(task.getId(), variables);

        VariableInstanceEntity wrappedIntegerTaskUpdate = (VariableInstanceEntity) runtimeService.getVariableInstance(processInstance.getId(),
                "wrappedInteger");
        assertThat(wrappedIntegerTaskUpdate.getValue()).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1002);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1002meta");
        });
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testWrapVariableWithCustomTypeChangeToSimpleType() {
        VariableTypes variableTypes = processEngineConfiguration.getVariableServiceConfiguration().getVariableTypes();
        variableTypes.addTypeBefore(new WrappedIntegerCustomType(), "integer");
        DefaultVariableInstanceEnhancer enhancer = new DefaultVariableInstanceEnhancer() {

            @Override
            public VariableType determineVariableType(VariableInstanceEntity variableInstance, Object originalVariableValue, Object enhancedVariableValue,
                    VariableType selectedType) {
                return enhancedVariableValue instanceof WrappedIntegerValue ?
                        variableTypes.getVariableType(WrappedIntegerCustomType.TYPE_NAME) :
                        variableTypes.getVariableType(IntegerType.TYPE_NAME);
            }

            @Override
            public Object preSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue) {

                VariableInstanceEntity instanceEntity = (VariableInstanceEntity) variableInstance;
                // new variable instance
                if (isSpecialType(originalValue)) {
                    String meta = originalValue + "meta";
                    variableInstance.setMetaInfo(meta);
                    return new WrappedIntegerValue((Integer) originalValue, variableInstance.getMetaInfo());
                } else {
                    VariableType defaultIntegerType = variableTypes.getVariableType("integer");
                    instanceEntity.setType(defaultIntegerType);
                    instanceEntity.setTypeName(defaultIntegerType.getTypeName());
                    return originalValue;
                }
            }

            private boolean isSpecialType(Object originalValue) {
                return originalValue instanceof Integer && ((Integer) originalValue) > 1000;
            }
        };

        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceEnhancer(enhancer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("wrappedInteger", 1001);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess", variables);

        VariableInstanceEntity wrappedInteger = (VariableInstanceEntity) runtimeService.getVariableInstance(processInstance.getId(), "wrappedInteger");
        assertThat(wrappedInteger.getType()).isInstanceOf(WrappedIntegerCustomType.class);
        assertThat(wrappedInteger.getTypeName()).isEqualTo(WrappedIntegerCustomType.TYPE_NAME);
        assertThat(wrappedInteger.getValue()).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
        });

        Object wrappedIntegerProcessVariable = processInstance.getProcessVariables().get("wrappedInteger");
        assertThat(wrappedIntegerProcessVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
        });

        // Change back to simple integer type by setting value to 1000
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables.put("wrappedInteger", 1000);
        taskService.complete(task.getId(), variables);

        VariableInstanceEntity wrappedIntegerTaskUpdate = (VariableInstanceEntity) runtimeService.getVariableInstance(processInstance.getId(),
                "wrappedInteger");
        assertThat(wrappedIntegerTaskUpdate.getValue()).isInstanceOf(Integer.class).isEqualTo(1000);
        assertThat(wrappedIntegerTaskUpdate.getType()).isInstanceOf(IntegerType.class);
        assertThat(wrappedIntegerTaskUpdate.getTypeName()).isEqualTo(IntegerType.TYPE_NAME);
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

    static class WrappedIntegerValue {

        public String metaInfo;
        public Integer value;

        public WrappedIntegerValue(Number value, String metaInfo) {
            this.metaInfo = metaInfo;
            this.value = value.intValue();
        }
    }

    public static class WrappedIntegerCustomType extends IntegerType {

        public static final String TYPE_NAME = "wrappedIntegerType";

        @Override
        public String getTypeName() {
            return TYPE_NAME;
        }

        @Override
        public void setValue(Object value, ValueFields valueFields) {
            if (value instanceof WrappedIntegerValue) {
                super.setValue(((WrappedIntegerValue) value).value, valueFields);
            } else {
                super.setValue(value, valueFields);
            }
        }

        public Object getValue(ValueFields valueFields) {
            if (valueFields instanceof VariableInstance) {
                return new WrappedIntegerValue(valueFields.getLongValue(), ((VariableInstance) valueFields).getMetaInfo());
            }
            return super.getValue(valueFields);
        }

        @Override
        public boolean isAbleToStore(Object value) {
            if (value == null) {
                return true;
            }
            return value instanceof WrappedIntegerValue ||
                    Integer.class.isAssignableFrom(value.getClass()) ||
                    int.class.isAssignableFrom(value.getClass());
        }
    }

}
