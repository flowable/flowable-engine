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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.api.event.TestVariableEventListener;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.event.FlowableVariableEvent;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.DefaultVariableInstanceValueModifier;
import org.flowable.variable.service.impl.VariableInstanceValueModifier;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Arthur Hupka-Merle
 */
public class VariableInstanceValueModifierBpmnTest extends PluggableFlowableTestCase {

    VariableInstanceValueModifier originalModifier;

    VariableType customType;

    TestVariableEventListener eventListener;

    @BeforeEach
    public void setUp() {
        originalModifier = processEngineConfiguration.getVariableServiceConfiguration().getVariableInstanceValueModifier();
        eventListener = new TestVariableEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(eventListener);
    }

    @AfterEach
    public void tearDown() {
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceValueModifier(originalModifier);
        if (customType != null) {
            processEngineConfiguration.getVariableTypes().removeType(customType);
        }
        if (eventListener != null) {
            eventListener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(eventListener);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testCompleteTaskWithExceptionInPostSetVariable() {
        processEngineConfiguration.getVariableServiceConfiguration()
                .setVariableInstanceValueModifier(new TestOrderIdValidatingValueModifier(processEngineConfiguration.getVariableServiceConfiguration()));

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

    /**
     * Tests changing of the variable type for a variable.
     * First the variable is set as a string, then it is changed to an integral number, which is stored
     * as 'long' (and not as 'integer' as the default would be).
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testChangeVariableType() {
        DefaultVariableInstanceValueModifier variableValueModifier = new DefaultVariableInstanceValueModifier(
                processEngineConfiguration.getVariableServiceConfiguration()) {

            @Override
            public void setVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
                if (isIntegralNumber(value)) {
                    super.setVariableValue(variableInstance, Long.parseLong(value.toString()), tenantId);
                } else {
                    super.setVariableValue(variableInstance, value, tenantId);
                }
            }

            @Override
            public void updateVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
                if (isIntegralNumber(value)) {
                    super.updateVariableValue(variableInstance, Long.parseLong(value.toString()), tenantId);
                } else {
                    super.updateVariableValue(variableInstance, value, tenantId);
                }
            }

            boolean isIntegralNumber(Object value) {
                return StringUtils.isNumeric(Objects.toString(value));
            }
        };
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceValueModifier(variableValueModifier);

        Map<String, Object> variables = new HashMap<>();
        // First variable is a string
        variables.put("orderId", "ABC");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess", variables);
        VariableInstance orderIdInstance = runtimeService.getVariableInstance(processInstance.getId(), "orderId");
        // type should be string
        assertThat(orderIdInstance.getTypeName()).isEqualTo("string");
        assertThat(orderIdInstance.getValue()).isEqualTo("ABC");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables()
                    .singleResult();
            assertThat(historicProcessInstance).isNotNull();
            assertThat(historicProcessInstance.getProcessVariables())
                    .contains(entry("orderId", "ABC"));
        }

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        // now we change it to be an integral number
        variables.put("orderId", 1);
        taskService.complete(task.getId(), variables);

        VariableInstanceEntity orderIdInstanceTask = (VariableInstanceEntity) runtimeService.getVariableInstance(processInstance.getId(), "orderId");
        // and expect it to be stored as long, because the variable value modifier forced it to be a long
        assertThat(orderIdInstanceTask.getTypeName()).isEqualTo("long");
        assertThat(orderIdInstanceTask.getValue()).isEqualTo(1L);
        assertThat(eventListener.getEventsReceived())
                .filteredOn(event -> event instanceof FlowableVariableEvent)
                .map(FlowableVariableEvent.class::cast)
                .extracting(FlowableVariableEvent::getVariableName, FlowableVariableEvent::getVariableValue)
                .containsExactly(tuple("orderId", "ABC"), tuple("orderId", 1L));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables()
                    .singleResult();
            assertThat(historicProcessInstance).isNotNull();
            assertThat(historicProcessInstance.getProcessVariables())
                    .contains(entry("orderId", 1L));
        }

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testWrapVariableWithCustomType() {
        VariableTypes variableTypes = processEngineConfiguration.getVariableServiceConfiguration().getVariableTypes();
        customType = new WrappedIntegerCustomType();
        variableTypes.addTypeBefore(customType, "integer");
        try {
            processEngineConfiguration.getVariableServiceConfiguration()
                    .setVariableInstanceValueModifier(new WrappedIntegerValueModifier(processEngineConfiguration.getVariableServiceConfiguration()));

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

            processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables()
                    .singleResult();

            wrappedIntegerProcessVariable = processInstance.getProcessVariables().get("wrappedInteger");
            assertThat(wrappedIntegerProcessVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
            });

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName("wrappedInteger")
                        .singleResult();
                assertThat(historicVariableInstance).isNotNull();
                assertThat(historicVariableInstance.getValue())
                        .isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
                        });
                assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo("wrappedIntegerType");
                assertThat(historicVariableInstance.getMetaInfo()).isEqualTo("1001meta");

                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .includeProcessVariables()
                        .singleResult();

                wrappedIntegerProcessVariable = historicProcessInstance.getProcessVariables().get("wrappedInteger");
                assertThat(wrappedIntegerProcessVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                    assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                    assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
                });

                HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .includeProcessVariables()
                        .singleResult();

                wrappedIntegerProcessVariable = historicTask.getProcessVariables().get("wrappedInteger");
                assertThat(wrappedIntegerProcessVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                    assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                    assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
                });
            }

            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables()
                    .singleResult();

            wrappedIntegerProcessVariable = task.getProcessVariables().get("wrappedInteger");
            assertThat(wrappedIntegerProcessVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
            });

            variables.put("wrappedInteger", 1002);
            taskService.complete(task.getId(), variables);

            VariableInstanceEntity wrappedIntegerTaskUpdate = (VariableInstanceEntity) runtimeService.getVariableInstance(processInstance.getId(),
                    "wrappedInteger");
            assertThat(wrappedIntegerTaskUpdate.getValue()).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                assertThat(wrappedIntegerValue.value).isEqualTo(1002);
                assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1002meta");
            });

            assertThat(eventListener.getEventsReceived())
                    .filteredOn(event -> event instanceof FlowableVariableEvent)
                    .map(FlowableVariableEvent.class::cast)
                    .extracting(FlowableVariableEvent::getVariableName,
                            e -> e.getVariableType().getTypeName(),
                            FlowableVariableEvent::getVariableValue,
                            e -> e.getType().name())
                    .containsExactly(
                            tuple("simpleInteger", "integer", 1000, "VARIABLE_CREATED"),
                            tuple("wrappedInteger", "wrappedIntegerType", wrappedInteger.getValue(), "VARIABLE_CREATED"),
                            tuple("simpleInteger", "integer", 1000, "VARIABLE_UPDATED"),  // task completion triggers 'update' of this variable
                            tuple("wrappedInteger", "wrappedIntegerType", wrappedIntegerTaskUpdate.getValue(), "VARIABLE_UPDATED"));

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName("wrappedInteger")
                        .singleResult();
                assertThat(historicVariableInstance).isNotNull();
                assertThat(historicVariableInstance.getValue())
                        .isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                            assertThat(wrappedIntegerValue.value).isEqualTo(1002);
                            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1002meta");
                        });
                assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo("wrappedIntegerType");
                assertThat(historicVariableInstance.getMetaInfo()).isEqualTo("1002meta");
            }
        } finally {
            // We have to delete here. Otherwise the annotation deployment delete operation will fail, due to the missing custom type
            deleteDeployments();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testWrapVariableWithCustomTypeChangeToSimpleType() {
        VariableTypes variableTypes = processEngineConfiguration.getVariableServiceConfiguration().getVariableTypes();
        customType = new WrappedIntegerCustomType();
        try {
            variableTypes.addTypeBefore(customType, "integer");
            processEngineConfiguration.getVariableServiceConfiguration()
                    .setVariableInstanceValueModifier(new WrappedIntegerValueModifier(processEngineConfiguration.getVariableServiceConfiguration()));
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

            // Use variable query API to check if the variable can be retrieved by value correctly
            VariableInstanceEntity variableInstanceResult = (VariableInstanceEntity) runtimeService.createVariableInstanceQuery()
                    .variableValueEquals("wrappedInteger", new WrappedIntegerValue(1001, null)).singleResult();
            assertThat(variableInstanceResult.getType()).isInstanceOf(WrappedIntegerCustomType.class);
            assertThat(variableInstanceResult.getTypeName()).isEqualTo(WrappedIntegerCustomType.TYPE_NAME);
            assertThat(variableInstanceResult.getValue()).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
            });

            Object wrappedIntegerProcessVariable = processInstance.getProcessVariables().get("wrappedInteger");
            assertThat(wrappedIntegerProcessVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
            });

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName("wrappedInteger")
                        .singleResult();
                assertThat(historicVariableInstance).isNotNull();
                assertThat(historicVariableInstance.getValue())
                        .isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
                        });
                assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo("wrappedIntegerType");
                assertThat(historicVariableInstance.getMetaInfo()).isEqualTo("1001meta");
            }

            // Change back to simple integer type by setting value to 1000
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            variables.put("wrappedInteger", 1000);
            taskService.complete(task.getId(), variables);

            VariableInstanceEntity wrappedIntegerTaskUpdate = (VariableInstanceEntity) runtimeService.getVariableInstance(processInstance.getId(),
                    "wrappedInteger");
            assertThat(wrappedIntegerTaskUpdate.getValue()).isInstanceOf(Integer.class).isEqualTo(1000);
            assertThat(wrappedIntegerTaskUpdate.getMetaInfo()).isNull();
            assertThat(wrappedIntegerTaskUpdate.getType()).isInstanceOf(IntegerType.class);
            assertThat(wrappedIntegerTaskUpdate.getTypeName()).isEqualTo(IntegerType.TYPE_NAME);

            assertThat(eventListener.getEventsReceived())
                    .filteredOn(event -> event instanceof FlowableVariableEvent)
                    .map(FlowableVariableEvent.class::cast)
                    .extracting(FlowableVariableEvent::getVariableName,
                            e -> e.getVariableType().getTypeName(),
                            FlowableVariableEvent::getVariableValue,
                            e -> e.getType().name())
                    .containsExactly(
                            tuple("wrappedInteger", "wrappedIntegerType", wrappedInteger.getValue(), "VARIABLE_CREATED"),
                            tuple("wrappedInteger", "integer", 1000, "VARIABLE_UPDATED"));

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName("wrappedInteger")
                        .singleResult();
                assertThat(historicVariableInstance).isNotNull();
                assertThat(historicVariableInstance.getValue()).isEqualTo(1000);
                assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo(IntegerType.TYPE_NAME);
                assertThat(historicVariableInstance.getMetaInfo()).isNull();
            }

        } finally {
            // We have to delete here. Otherwise the annotation deployment delete operation will fail, due to the missing custom type
            deleteDeployments();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" }, tenantId = "myTenant")
    public void testCompleteTaskWithExceptionInPostSetVariableWithTenantId() {
        TestOrderIdValidatingValueModifier valueModifier = new TestOrderIdValidatingValueModifier(processEngineConfiguration.getVariableServiceConfiguration());
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceValueModifier(valueModifier);

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", 1L);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKeyAndTenantId("oneTaskProcess", variables, "myTenant");

        assertThat(valueModifier.setVariableTenantIds).containsExactly("myTenant");

        assertThatThrownBy(() -> {
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            variables.put("orderId", -1L);
            taskService.complete(task.getId(), variables);
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");

        assertThat(valueModifier.setVariableTenantIds).containsExactly("myTenant");
        assertThat(valueModifier.updateVariableTenantIds).containsExactly("myTenant");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testTransientVariables() {
        processEngineConfiguration.getVariableServiceConfiguration()
                .setVariableInstanceValueModifier(new TestOrderIdValidatingValueModifier(processEngineConfiguration.getVariableServiceConfiguration()));

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
        List<Object> preSetValueCalls = new ArrayList<>();
        DefaultVariableInstanceValueModifier modifier = new DefaultVariableInstanceValueModifier(processEngineConfiguration.getVariableServiceConfiguration()) {

            @Override
            public void setVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
                preSetValueCalls.add(value);
                assertThat(variableInstance.getProcessInstanceId()).isNotNull();
                assertThat(variableInstance.getProcessDefinitionId()).isNotNull();
                Pair<Object, String> valueAndMeta = determineValueAndMetaInfo(value);
                super.setVariableValue(variableInstance, valueAndMeta.getLeft(), tenantId);
                variableInstance.setMetaInfo(valueAndMeta.getRight());
            }

            @Override
            public void updateVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
                preSetValueCalls.add(value);
                assertThat(variableInstance.getProcessInstanceId()).isNotNull();
                assertThat(variableInstance.getProcessDefinitionId()).isNotNull();
                Pair<Object, String> valueAndMeta = determineValueAndMetaInfo(value);
                super.updateVariableValue(variableInstance, valueAndMeta.getLeft(), tenantId);
                variableInstance.setMetaInfo(valueAndMeta.getRight());
            }

            Pair<Object, String> determineValueAndMetaInfo(Object value) {
                if (value instanceof String) {
                    VariableMeta variableMeta = new VariableMeta();
                    variableMeta.byteLength = String.valueOf(((String) value).getBytes().length);
                    String metaInfo;
                    try {
                        metaInfo = objectMapper.writeValueAsString(variableMeta);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    return Pair.of(value + "Enhanced", metaInfo);
                }

                return Pair.of(value, null);
            }

        };
        processEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceValueModifier(modifier);

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


        // Use variable query API to check if the variable can be retrieved by value correctly
        VariableInstanceEntity variableInstanceResult = (VariableInstanceEntity) runtimeService.createVariableInstanceQuery()
                .variableValueLike("myEnhancedStringVariable", "myValue1Enh%").singleResult();
        assertThat(variableInstanceResult.getType()).isInstanceOf(StringType.class);
        assertThat(variableInstanceResult.getTypeName()).isEqualTo("string");
        assertThat(variableInstanceResult.getValue()).isEqualTo("myValue1Enhanced");


        assertThat(processVariables.get("myIntVariable")).isInstanceOf(Integer.class);
        Object intVariableValue = runtimeService.getVariable(processInstance.getId(), "myIntVariable");
        assertThat(intVariableValue).isEqualTo(1);

        VariableInstance intVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "myIntVariable");
        assertThat(intVariableInstance.getMetaInfo()).isNull();

        assertThat(preSetValueCalls).containsExactlyInAnyOrder("myValue1", 1);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("myEnhancedStringVariable")
                    .singleResult();
            assertThat(historicVariableInstance).isNotNull();
            assertThat(historicVariableInstance.getValue()).isEqualTo("myValue1Enhanced");
            assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo("string");
            assertThat(historicVariableInstance.getMetaInfo()).isEqualTo("{\"byteLength\":\"8\"}");

            historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("myIntVariable")
                    .singleResult();
            assertThat(historicVariableInstance).isNotNull();
            assertThat(historicVariableInstance.getValue()).isEqualTo(1);
            assertThat(historicVariableInstance.getMetaInfo()).isNull();
        }
    }

    static class VariableMeta {

        @JsonProperty("byteLength")
        public String byteLength;

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

        @Override
        public Object getValue(ValueFields valueFields) {
            if (valueFields instanceof VariableInstance) {
                return new WrappedIntegerValue(valueFields.getLongValue(), ((VariableInstance) valueFields).getMetaInfo());
            } else if (valueFields instanceof HistoricVariableInstance) {
                return new WrappedIntegerValue(valueFields.getLongValue(), ((HistoricVariableInstance) valueFields).getMetaInfo());
            }
            return super.getValue(valueFields);
        }

        @Override
        public boolean isAbleToStore(Object value) {
            if (value == null) {
                return true;
            }
            return value instanceof WrappedIntegerValue;
        }
    }

    static class WrappedIntegerValue {

        public String metaInfo;
        public Integer value;

        public WrappedIntegerValue(Number value, String metaInfo) {
            this.metaInfo = metaInfo;
            this.value = value.intValue();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            WrappedIntegerValue that = (WrappedIntegerValue) o;
            return Objects.equals(metaInfo, that.metaInfo) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(metaInfo, value);
        }
    }

    static class TestOrderIdValidatingValueModifier extends DefaultVariableInstanceValueModifier {

        final Collection<String> setVariableTenantIds = new ArrayList<>();
        final Collection<String> updateVariableTenantIds = new ArrayList<>();

        TestOrderIdValidatingValueModifier(VariableServiceConfiguration serviceConfiguration) {
            super(serviceConfiguration);
        }

        @Override
        public void setVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
            setVariableTenantIds.add(tenantId);
            validateValue(variableInstance, value);
            super.setVariableValue(variableInstance, value, tenantId);
        }

        @Override
        public void updateVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
            updateVariableTenantIds.add(tenantId);
            validateValue(variableInstance, value);
            super.updateVariableValue(variableInstance, value, tenantId);
        }

        protected void validateValue(VariableInstance variableInstance, Object value) {
            if (variableInstance.getName().equals("orderId")) {
                if (((Number) value).longValue() < 0) {
                    throw new FlowableIllegalArgumentException("Invalid type: value should be larger than zero");
                }
            }
        }
    }

    static class WrappedIntegerValueModifier extends DefaultVariableInstanceValueModifier {

        WrappedIntegerValueModifier(VariableServiceConfiguration serviceConfiguration) {
            super(serviceConfiguration);
        }


        @Override
        public void setVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
            Pair<Object, String> valueAndMeta = determineValueAndMeta(value);
            super.setVariableValue(variableInstance, valueAndMeta.getLeft(), tenantId);
            variableInstance.setMetaInfo(valueAndMeta.getRight());
        }

        @Override
        public void updateVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
            Pair<Object, String> valueAndMeta = determineValueAndMeta(value);
            super.updateVariableValue(variableInstance, valueAndMeta.getLeft(), tenantId);
            variableInstance.setMetaInfo(valueAndMeta.getRight());
        }

        Pair<Object, String> determineValueAndMeta(Object value) {
            if (value instanceof Integer && ((Integer) value) > 1000) {
                String metaInfo = value + "meta";
                return Pair.of(new WrappedIntegerValue((Number) value, metaInfo), metaInfo);
            }
            return Pair.of(value, null);
        }
    }

}
