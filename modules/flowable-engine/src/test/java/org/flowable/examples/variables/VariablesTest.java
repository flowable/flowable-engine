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
package org.flowable.examples.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tom Baeyens
 */
public class VariablesTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testBasicVariableOperations() {
        processEngineConfiguration.getVariableTypes().addType(CustomVariableType.instance);

        Date now = new Date();
        List<String> serializable = new ArrayList<>();
        serializable.add("one");
        serializable.add("two");
        serializable.add("three");
        byte[] bytes1 = "somebytes1".getBytes();
        byte[] bytes2 = "somebytes2".getBytes();

        // 2000 characters * 2 bytes = 4000 bytes
        StringBuilder long2000StringBuilder = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            long2000StringBuilder.append("z");
        }

        // 2001 characters * 2 bytes = 4002 bytes
        StringBuilder long2001StringBuilder = new StringBuilder();

        for (int i = 0; i < 2000; i++) {
            long2001StringBuilder.append("a");
        }
        long2001StringBuilder.append("a");

        // 4002 characters
        StringBuilder long4001StringBuilder = new StringBuilder();

        for (int i = 0; i < 4000; i++) {
            long4001StringBuilder.append("a");
        }
        long4001StringBuilder.append("a");

        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("longString2000chars", long2000StringBuilder.toString());
        variables.put("stringVar", "coca-cola");
        variables.put("dateVar", now);
        variables.put("nullVar", null);
        variables.put("serializableVar", serializable);
        variables.put("bytesVar", bytes1);
        variables.put("customVar1", new CustomType(bytes2));
        variables.put("customVar2", null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess", variables);

        variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables)
                .containsOnly(
                        entry("longVar", 928374L),
                        entry("shortVar", (short) 123),
                        entry("integerVar", 1234),
                        entry("stringVar", "coca-cola"),
                        entry("longString2000chars", long2000StringBuilder.toString()),
                        entry("dateVar", now),
                        entry("nullVar", null),
                        entry("serializableVar", serializable),
                        entry("bytesVar", bytes1),
                        entry("customVar1", new CustomType(bytes2)),
                        entry("customVar2", null)
                );

        // Set all existing variables values to null
        runtimeService.setVariable(processInstance.getId(), "longVar", null);
        runtimeService.setVariable(processInstance.getId(), "shortVar", null);
        runtimeService.setVariable(processInstance.getId(), "integerVar", null);
        runtimeService.setVariable(processInstance.getId(), "stringVar", null);
        runtimeService.setVariable(processInstance.getId(), "longString2000chars", null);
        runtimeService.setVariable(processInstance.getId(), "longString4000chars", null);
        runtimeService.setVariable(processInstance.getId(), "dateVar", null);
        runtimeService.setVariable(processInstance.getId(), "nullVar", null);
        runtimeService.setVariable(processInstance.getId(), "serializableVar", null);
        runtimeService.setVariable(processInstance.getId(), "bytesVar", null);
        runtimeService.setVariable(processInstance.getId(), "customVar1", null);
        runtimeService.setVariable(processInstance.getId(), "customVar2", null);

        variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables)
                .containsOnly(
                        entry("longVar", null),
                        entry("shortVar", null),
                        entry("integerVar", null),
                        entry("stringVar", null),
                        entry("longString2000chars", null),
                        entry("longString4000chars", null),
                        entry("dateVar", null),
                        entry("nullVar", null),
                        entry("serializableVar", null),
                        entry("bytesVar", null),
                        entry("customVar1", null),
                        entry("customVar2", null)
                );

        // Update existing variable values again, and add a new variable
        runtimeService.setVariable(processInstance.getId(), "new var", "hi");
        runtimeService.setVariable(processInstance.getId(), "longVar", 9987L);
        runtimeService.setVariable(processInstance.getId(), "shortVar", (short) 456);
        runtimeService.setVariable(processInstance.getId(), "integerVar", 4567);
        runtimeService.setVariable(processInstance.getId(), "stringVar", "colgate");
        runtimeService.setVariable(processInstance.getId(), "longString2000chars", long2001StringBuilder.toString());
        runtimeService.setVariable(processInstance.getId(), "longString4000chars", long4001StringBuilder.toString());
        runtimeService.setVariable(processInstance.getId(), "dateVar", now);
        runtimeService.setVariable(processInstance.getId(), "serializableVar", serializable);
        runtimeService.setVariable(processInstance.getId(), "bytesVar", bytes1);
        runtimeService.setVariable(processInstance.getId(), "customVar1", new CustomType(bytes2));
        runtimeService.setVariable(processInstance.getId(), "customVar2", new CustomType(bytes1));

        variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables)
                .containsOnly(
                        entry("new var", "hi"),
                        entry("longVar", 9987L),
                        entry("shortVar", (short) 456),
                        entry("integerVar", 4567),
                        entry("stringVar", "colgate"),
                        entry("longString2000chars", long2001StringBuilder.toString()),
                        entry("longString4000chars", long4001StringBuilder.toString()),
                        entry("dateVar", now),
                        entry("nullVar", null),
                        entry("serializableVar", serializable),
                        entry("bytesVar", bytes1),
                        entry("customVar1", new CustomType(bytes2)),
                        entry("customVar2", new CustomType(bytes1))
                );

        Collection<String> varFilter = new ArrayList<>(2);
        varFilter.add("stringVar");
        varFilter.add("integerVar");

        Map<String, Object> filteredVariables = runtimeService.getVariables(processInstance.getId(), varFilter);
        assertThat(filteredVariables)
                .containsOnlyKeys("stringVar", "integerVar");

        // Try setting the value of the variable that was initially created with value 'null'
        runtimeService.setVariable(processInstance.getId(), "nullVar", "a value");
        Object newValue = runtimeService.getVariable(processInstance.getId(), "nullVar");
        assertThat(newValue).isEqualTo("a value");

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());
    }

    @Test
    @Deployment
    public void testLocalizeVariables() {
        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "coca-cola");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("localizeVariables", variables);

        Map<String, VariableInstance> variableInstances = runtimeService.getVariableInstances(processInstance.getId());
        assertThat(variableInstances.get("stringVar"))
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "coca-cola");

        List<String> variableNames = new ArrayList<>();
        variableNames.add("stringVar");

        // getVariablesInstances via names
        variableInstances = runtimeService.getVariableInstances(processInstance.getId(), variableNames);
        assertThat(variableInstances.get("stringVar"))
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "coca-cola");

        // getVariableInstancesLocal via names
        variableInstances = runtimeService.getVariableInstancesLocal(processInstance.getId(), variableNames);
        assertThat(variableInstances.get("stringVar"))
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "coca-cola");

        // getVariableInstance
        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "stringVar");
        assertThat(variableInstance)
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "coca-cola");

        // getVariableInstanceLocal
        variableInstance = runtimeService.getVariableInstanceLocal(processInstance.getId(), "stringVar");
        assertThat(variableInstance)
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "coca-cola");

        // Verify TaskService behavior
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        variableInstances = taskService.getVariableInstances(task.getId());
        assertThat(variableInstances).hasSize(2);
        assertThat(variableInstances.get("stringVar"))
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "coca-cola");
        assertThat(variableInstances.get("intVar"))
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("intVar", null);

        variableNames = new ArrayList<>();
        variableNames.add("stringVar");

        // getVariablesInstances via names
        variableInstances = taskService.getVariableInstances(task.getId(), variableNames);
        assertThat(variableInstances.get("stringVar"))
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "coca-cola");

        taskService.setVariableLocal(task.getId(), "stringVar", "pepsi-cola");

        // getVariableInstancesLocal via names
        variableInstances = taskService.getVariableInstancesLocal(task.getId(), variableNames);
        assertThat(variableInstances.get("stringVar"))
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "pepsi-cola");

        // getVariableInstance
        variableInstance = taskService.getVariableInstance(task.getId(), "stringVar");
        assertThat(variableInstance)
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "pepsi-cola");

        // getVariableInstanceLocal
        variableInstance = taskService.getVariableInstanceLocal(task.getId(), "stringVar");
        assertThat(variableInstance)
                .extracting(VariableInstance::getName, VariableInstance::getValue)
                .containsExactly("stringVar", "pepsi-cola");
    }

    @Test
    @Deployment
    public void testLocalizeDataObjects() {
        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "coca-cola");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("localizeVariables", variables);
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processInstance.getProcessDefinitionId());
        dynamicBpmnService.changeLocalizationName("en-US", "stringVarId", "stringVar 'en-US' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-US", "stringVarId", "stringVar 'en-US' Description", infoNode);
        dynamicBpmnService.changeLocalizationName("en-AU", "stringVarId", "stringVar 'en-AU' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-AU", "stringVarId", "stringVar 'en-AU' Description", infoNode);
        dynamicBpmnService.changeLocalizationName("en", "stringVarId", "stringVar 'en' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en", "stringVarId", "stringVar 'en' Description", infoNode);

        dynamicBpmnService.changeLocalizationName("en-US", "intVarId", "intVar 'en-US' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-US", "intVarId", "intVar 'en-US' Description", infoNode);
        dynamicBpmnService.changeLocalizationName("en-AU", "intVarId", "intVar 'en-AU' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-AU", "intVarId", "intVar 'en-AU' Description", infoNode);
        dynamicBpmnService.changeLocalizationName("en", "intVarId", "intVar 'en' Name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en", "intVarId", "intVar 'en' Description", infoNode);

        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        Map<String, DataObject> dataObjects = runtimeService.getDataObjects(processInstance.getId(), "es", false);
        assertThat(dataObjects).hasSize(1);
        DataObject dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "it", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObjects
        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "en-US", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "en-AU", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "en-GB", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), "en-GB", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        List<String> variableNames = new ArrayList<>();
        variableNames.add("stringVar");

        // getDataObjects via names

        // no locale/default
        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "es", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "it", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "en-US", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "en-AU", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "en-GB", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjects(processInstance.getId(), variableNames, "en-GB", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObjectsLocal
        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "es", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "it", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "en-US", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "en-AU", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "en-GB", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "en-GB", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), "ja-JA", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObjectsLocal via names

        // no locale/default
        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "es", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "it", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "en-US", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "en-AU", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "en-GB", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(processInstance.getId(), variableNames, "en-GB", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObject
        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "es", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "it", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-GB", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-US", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-AU", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-GB", true);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(processInstance.getId(), "stringVar", "en-GB", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObjectLocal
        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "es", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "it", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "en-US", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "en-AU", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "en-GB", true);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(processInstance.getId(), "stringVar", "en-GB", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        Execution subprocess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subprocess1").singleResult();

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "es", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObjects
        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "es", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'es' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'es' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "it", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'it' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'it' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getName()).isEqualTo("stringVar");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "en-US", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-US' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-US' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "en-AU", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-AU' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-AU' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "en-GB", true);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), "en-GB", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        // getDataObjects via names (from subprocess)

        variableNames.add("intVar");
        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "es", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'es' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'es' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "it", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'it' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'it' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "en-US", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-US' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-US' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "en-AU", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-AU' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-AU' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "en-GB", true);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = runtimeService.getDataObjects(subprocess.getId(), variableNames, "en-GB", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getValue()).isNull();
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        // getDataObjectsLocal
        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "es", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "it", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "en-US", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "en-AU", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "en-GB", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "en-GB", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), "ja-JA", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObjectsLocal via names
        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "es", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "it", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "en-US", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "en-AU", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "en-GB", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = runtimeService.getDataObjectsLocal(subprocess.getId(), variableNames, "en-GB", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObject (in subprocess)
        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "es", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "it", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-GB", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-US", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-AU", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-GB", true);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObject(subprocess.getId(), "intVar", "en-GB", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObjectLocal (in subprocess)
        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "es", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "it", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "en-US", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "en-AU", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "en-GB", true);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = runtimeService.getDataObjectLocal(subprocess.getId(), "intVar", "en-GB", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getValue()).isNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();

        // Verify TaskService behavior
        dataObjects = taskService.getDataObjects(task.getId());
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        // getDataObjects
        dataObjects = taskService.getDataObjects(task.getId());
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = taskService.getDataObjects(task.getId(), "es", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'es' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'es' Name");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = taskService.getDataObjects(task.getId(), "it", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'it' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'it' Name");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = taskService.getDataObjects(task.getId(), "en-US", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-US' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-US' Name");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = taskService.getDataObjects(task.getId(), "en-AU", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en-AU' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en-AU' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = taskService.getDataObjects(task.getId(), "en-GB", true);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'en' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar 'en' Name");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        dataObjects = taskService.getDataObjects(task.getId(), "en-GB", false);
        assertThat(dataObjects).hasSize(2);
        dataObject = dataObjects.get("intVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("intVarId");
        assertThat(dataObject.getDescription()).isEqualTo("intVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(subprocess.getId());
        assertThat(dataObject.getId()).isNotNull();
        assertThat(dataObject.getLocalizedName()).isEqualTo("intVar");
        assertThat(dataObject.getName()).isEqualTo("intVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("int");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");

        variableNames = new ArrayList<>();
        variableNames.add("stringVar");

        // getDataObjects via names

        // no locale/default
        dataObjects = taskService.getDataObjects(task.getId(), variableNames);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "es", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "it", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "en-US", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "en-AU", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "en-GB", true);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObjects = taskService.getDataObjects(task.getId(), variableNames, "en-GB", false);
        assertThat(dataObjects).hasSize(1);
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        // getDataObject
        dataObject = taskService.getDataObject(task.getId(), "stringVar");
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "es", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'es' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'es' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "it", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'it' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'it' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-GB", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-US", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-US' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-US' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-AU", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en-AU' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en-AU' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-GB", true);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar 'en' Name");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'en' Description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();

        dataObject = taskService.getDataObject(task.getId(), "stringVar", "en-GB", false);
        assertThat(dataObject).isNotNull();
        assertThat(dataObject.getName()).isEqualTo("stringVar");
        assertThat(dataObject.getValue()).isEqualTo("coca-cola");
        assertThat(dataObject.getLocalizedName()).isEqualTo("stringVar");
        assertThat(dataObject.getDescription()).isEqualTo("stringVar 'default' description");
        assertThat(dataObject.getDataObjectDefinitionKey()).isEqualTo("stringVarId");
        assertThat(dataObject.getType()).isEqualTo("string");
        dataObject = dataObjects.get("stringVar");
        assertThat(dataObject.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(dataObject.getId()).isNotNull();
    }

    // Test case for ACT-1839
    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testChangeTypeSerializable.bpmn20.xml" })
    public void testChangeTypeSerializable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variable-type-change-test");
        assertThat(processInstance).isNotNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Activiti is awesome!");
        SomeSerializable myVar = (SomeSerializable) runtimeService.getVariable(processInstance.getId(), "myVar");
        assertThat(myVar.getValue()).isEqualTo("someValue");
    }

    public String getVariableInstanceId(String executionId, String name) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId).variableName(name).singleResult();
        return variable.getId();
    }

    // test case for ACT-1082
    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testBasicVariableOperations.bpmn20.xml" })
    public void testChangeVariableType() {

        Date now = new Date();
        List<String> serializable = new ArrayList<>();
        serializable.add("one");
        serializable.add("two");
        serializable.add("three");
        byte[] bytes = "somebytes".getBytes();

        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "coca-cola");
        variables.put("dateVar", now);
        variables.put("nullVar", null);
        variables.put("serializableVar", serializable);
        variables.put("bytesVar", bytes);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess", variables);

        variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables)
                .containsOnly(
                        entry("longVar", 928374L),
                        entry("shortVar", (short) 123),
                        entry("integerVar", 1234),
                        entry("stringVar", "coca-cola"),
                        entry("dateVar", now),
                        entry("nullVar", null),
                        entry("serializableVar", serializable),
                        entry("bytesVar", bytes)
                );

        // check if the id of the variable is the same or not

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            String oldSerializableVarId = getVariableInstanceId(processInstance.getId(), "serializableVar");
            String oldLongVar = getVariableInstanceId(processInstance.getId(), "longVar");

            // Change type of serializableVar from serializable to Short
            Map<String, Object> newVariables = new HashMap<>();
            newVariables.put("serializableVar", (short) 222);
            runtimeService.setVariables(processInstance.getId(), newVariables);
            variables = runtimeService.getVariables(processInstance.getId());
            assertThat(variables)
                    .containsEntry("serializableVar", (short) 222);

            String newSerializableVarId = getVariableInstanceId(processInstance.getId(), "serializableVar");

            assertThat(newSerializableVarId).isEqualTo(oldSerializableVarId);

            // Change type of a longVar from Long to Short
            newVariables = new HashMap<>();
            newVariables.put("longVar", (short) 123);
            runtimeService.setVariables(processInstance.getId(), newVariables);
            variables = runtimeService.getVariables(processInstance.getId());
            assertThat(variables)
                    .containsEntry("longVar", (short) 123);

            String newLongVar = getVariableInstanceId(processInstance.getId(), "longVar");
            assertThat(newLongVar).isEqualTo(oldLongVar);
        }
    }

    // test case for ACT-1428
    @Test
    @Deployment
    public void testNullVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Map<String, String> variables = new HashMap<>();
        variables.put("testProperty", "434");

        formService.submitTaskFormData(task.getId(), variables);
        String resultVar = (String) runtimeService.getVariable(processInstance.getId(), "testProperty");

        assertThat(resultVar).isEqualTo("434");

        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        // If no variable is given, no variable should be set and script test should throw exception
        processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
        String processId = processInstance.getId();
        task = taskService.createTaskQuery().processInstanceId(processId).singleResult();
        String taskId = task.getId();
        try {
            assertThatThrownBy(() -> formService.submitTaskFormData(taskId, new HashMap<>()))
                    .isExactlyInstanceOf(FlowableException.class);
        } finally {
            runtimeService.deleteProcessInstance(processId, "intentional exception in script task");
        }

        // No we put null property, This should be put into the variable. We do not expect exceptions
        processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String finalTaskId = task.getId();
        Map<String, String> finalVariables = new HashMap<>();
        finalVariables.put("testProperty", null);
        assertThatCode(() -> formService.submitTaskFormData(finalTaskId, finalVariables))
                .doesNotThrowAnyException();
        resultVar = (String) runtimeService.getVariable(processInstance.getId(), "testProperty");

        assertThat(resultVar).isNull();

        runtimeService.deleteProcessInstance(processInstance.getId(), "intentional exception in script task");
    }

    /**
     * Test added to validate UUID variable type + querying (ACT-1665)
     */
    @Test
    @Deployment
    public void testUUIDVariableAndQuery() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        // Check UUID variable type query on task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        UUID randomUUID = UUID.randomUUID();
        taskService.setVariableLocal(task.getId(), "conversationId", randomUUID);

        org.flowable.task.api.Task resultingTask = taskService.createTaskQuery().taskVariableValueEquals("conversationId", randomUUID).singleResult();
        assertThat(resultingTask).isNotNull();
        assertThat(resultingTask.getId()).isEqualTo(task.getId());

        randomUUID = UUID.randomUUID();

        // Check UUID variable type query on process
        runtimeService.setVariable(processInstance.getId(), "uuidVar", randomUUID);
        ProcessInstance result = runtimeService.createProcessInstanceQuery().variableValueEquals("uuidVar", randomUUID).singleResult();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testBasicVariableOperations.bpmn20.xml" })
    public void testAccessToProcessInstanceIdWhenSettingVariable() {
        addVariableTypeIfNotExists(CustomAccessProcessInstanceVariableType.INSTANCE);

        CustomAccessProcessType customVar = new CustomAccessProcessType();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("taskAssigneeProcess")
            .variable("customVar", customVar)
            .start();

        assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isEqualTo(processInstance.getId());

        assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isEqualTo(processInstance.getId());

        assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isNull();

        assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isNull();

        assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isNull();

        assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isNull();

        customVar = runtimeService.getVariable(processInstance.getId(), "customVar", CustomAccessProcessType.class);

        assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isEqualTo(processInstance.getId());

        assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isEqualTo(processInstance.getId());

        assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isNull();

        assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isNull();

        assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isNull();

        assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testBasicVariableOperations.bpmn20.xml" })
    public void testAccessToTaskIdWhenSettingLocalVariableOnTask() {
        addVariableTypeIfNotExists(CustomAccessProcessInstanceVariableType.INSTANCE);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("taskAssigneeProcess")
            .start();

        Task task = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .singleResult();

        assertThat(task).isNotNull();

        CustomAccessProcessType customVar = new CustomAccessProcessType();
        taskService.setVariableLocal(task.getId(), "customTaskVar", customVar);

        assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isEqualTo(processInstance.getId());

        assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isEqualTo(task.getExecutionId());

        assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isEqualTo(task.getId());

        assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isNull();

        assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isNull();

        assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isNull();

        customVar = taskService.getVariableLocal(task.getId(), "customTaskVar", CustomAccessProcessType.class);

        assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isEqualTo(processInstance.getId());

        assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isEqualTo(task.getExecutionId());

        assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isEqualTo(task.getId());

        assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isNull();

        assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isNull();

        assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testBasicVariableOperations.bpmn20.xml" })
    public void testAccessToExecutionIdWhenSettingLocalVariableOnExecution() {
        addVariableTypeIfNotExists(CustomAccessProcessInstanceVariableType.INSTANCE);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("taskAssigneeProcess")
            .start();

        Execution execution = runtimeService.createExecutionQuery()
            .activityId("theTask")
            .singleResult();

        assertThat(execution).isNotNull();

        CustomAccessProcessType customVar = new CustomAccessProcessType();
        runtimeService.setVariableLocal(execution.getId(), "customExecutionVar", customVar);

        assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isEqualTo(processInstance.getId());

        assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isEqualTo(execution.getId());

        assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isNull();

        assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isNull();

        assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isNull();

        assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isNull();

        customVar = runtimeService.getVariableLocal(execution.getId(), "customExecutionVar", CustomAccessProcessType.class);

        assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isEqualTo(processInstance.getId());

        assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isEqualTo(execution.getId());

        assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isNull();

        assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isNull();

        assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isNull();

        assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testBasicVariableOperations.bpmn20.xml" })
    public void testImmutableEmptyCollectionVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("taskAssigneeProcess")
                .variable("listVar", Collections.emptyList())
                .variable("setVar", Collections.emptySet())
                .start();

        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "listVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("emptyCollection");
        assertThat(variableInstance.getValue()).asList().isEmpty();

        variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "setVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("emptyCollection");
        assertThat(variableInstance.getValue())
                .isInstanceOfSatisfying(Set.class, set -> assertThat(set).isEmpty());
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/variables/VariablesTest.testBasicVariableOperations.bpmn20.xml" })
    public void testEmptyCollectionVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("taskAssigneeProcess")
                .variable("listVar", new ArrayList<>())
                .variable("setVar", new HashSet<>())
                .start();

        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "listVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("serializable");
        assertThat(variableInstance.getValue()).asList().isEmpty();

        variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "setVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("serializable");
        assertThat(variableInstance.getValue())
                .isInstanceOfSatisfying(Set.class, set -> assertThat(set).isEmpty());
    }

    protected void addVariableTypeIfNotExists(VariableType variableType) {
        // We can't remove the VariableType after every test since it would cause the test
        // to fail due to not being able to get the variable value during deleting
        if (processEngineConfiguration.getVariableTypes().getTypeIndex(variableType) == -1) {
            processEngineConfiguration.getVariableTypes().addType(variableType);
        }
    }

    static class CustomAccessProcessType {

        protected String processInstanceId;
        protected String executionId;
        protected String taskId;
        protected String scopeId;
        protected String subScopeId;
        protected String scopeType;

        public String getProcessInstanceId() {
            return processInstanceId;
        }

        public void setProcessInstanceId(String processInstanceId) {
            this.processInstanceId = processInstanceId;
        }

        public String getExecutionId() {
            return executionId;
        }

        public void setExecutionId(String executionId) {
            this.executionId = executionId;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getScopeId() {
            return scopeId;
        }

        public void setScopeId(String scopeId) {
            this.scopeId = scopeId;
        }

        public String getSubScopeId() {
            return subScopeId;
        }

        public void setSubScopeId(String subScopeId) {
            this.subScopeId = subScopeId;
        }

        public String getScopeType() {
            return scopeType;
        }

        public void setScopeType(String scopeType) {
            this.scopeType = scopeType;
        }
    }
    static class CustomAccessProcessInstanceVariableType implements  VariableType {

        static final CustomAccessProcessInstanceVariableType INSTANCE = new CustomAccessProcessInstanceVariableType();

        @Override
        public String getTypeName() {
            return "CustomAccessProcessInstanceVariableType";
        }

        @Override
        public boolean isCachable() {
            return true;
        }

        @Override
        public boolean isAbleToStore(Object value) {
            return value instanceof CustomAccessProcessType;
        }

        @Override
        public void setValue(Object value, ValueFields valueFields) {
            CustomAccessProcessType customValue = (CustomAccessProcessType) value;

            customValue.setProcessInstanceId(valueFields.getProcessInstanceId());
            customValue.setExecutionId(valueFields.getExecutionId());
            customValue.setTaskId(valueFields.getTaskId());
            customValue.setScopeId(valueFields.getScopeId());
            customValue.setSubScopeId(valueFields.getSubScopeId());
            customValue.setScopeType(valueFields.getScopeType());

            String textValue = new StringJoiner(",")
                .add(customValue.getProcessInstanceId())
                .add(customValue.getExecutionId())
                .add(customValue.getTaskId())
                .add(customValue.getScopeId())
                .add(customValue.getSubScopeId())
                .add(customValue.getScopeType())
                .toString();
            valueFields.setTextValue(textValue);
        }

        @Override
        public Object getValue(ValueFields valueFields) {
            String textValue = valueFields.getTextValue();
            String[] values = textValue.split(",");

            CustomAccessProcessType customValue = new CustomAccessProcessType();
            customValue.setProcessInstanceId(valueAt(values, 0));
            customValue.setExecutionId(valueAt(values, 1));
            customValue.setTaskId(valueAt(values, 2));
            customValue.setScopeId(valueAt(values, 3));
            customValue.setSubScopeId(valueAt(values, 4));
            customValue.setScopeType(valueAt(values, 5));

            return customValue;
        }

        protected String valueAt(String[] array, int index) {
            if (array.length > index) {
                return getValue(array[index]);
            }

            return null;
        }
        protected String getValue(String value) {
            return "null".equals(value) ? null : value;
        }
    }

}

class CustomType {
    private byte[] value;

    public CustomType(byte[] value) {
        if (value == null) {
            throw new NullPointerException();
        }
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        CustomType other = (CustomType) obj;
        return Arrays.equals(value, other.value);
    }

}

/**
 * A custom variable type for testing byte array value handling.
 *
 * @author Marcus Klimstra (CGI)
 */
class CustomVariableType implements VariableType {
    public static final CustomVariableType instance = new CustomVariableType();

    @Override
    public String getTypeName() {
        return "CustomVariableType";
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return value == null || value instanceof CustomType;
    }

    @Override
    public void setValue(Object o, ValueFields valueFields) {
        // ensure calling setBytes multiple times no longer causes any problems
        valueFields.setBytes(new byte[] { 1, 2, 3 });
        valueFields.setBytes(null);
        valueFields.setBytes(new byte[] { 4, 5, 6 });

        byte[] value = (o == null ? null : ((CustomType) o).getValue());
        valueFields.setBytes(value);
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        byte[] bytes = valueFields.getBytes();
        return bytes == null ? null : new CustomType(bytes);
    }

}
