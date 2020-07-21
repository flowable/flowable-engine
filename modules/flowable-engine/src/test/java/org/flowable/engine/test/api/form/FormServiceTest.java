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

package org.flowable.engine.test.api.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.DeploymentId;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Tom Baeyens
 * @author Falko Menge (camunda)
 */
public class FormServiceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/examples/taskforms/VacationRequest_deprecated_forms.bpmn20.xml", "org/flowable/examples/taskforms/approve.form",
            "org/flowable/examples/taskforms/request.form", "org/flowable/examples/taskforms/adjustRequest.form" })
    public void testGetStartFormByProcessDefinitionId() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        assertThat(processDefinitions).hasSize(1);
        ProcessDefinition processDefinition = processDefinitions.get(0);

        Object startForm = formService.getRenderedStartForm(processDefinition.getId());
        assertThat(startForm).isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetStartFormByProcessDefinitionIdWithoutStartform() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        assertThat(processDefinitions).hasSize(1);
        ProcessDefinition processDefinition = processDefinitions.get(0);

        Object startForm = formService.getRenderedStartForm(processDefinition.getId());
        assertThat(startForm).isNull();
    }

    @Test
    public void testGetStartFormByKeyNullKey() {
        assertThatThrownBy(() -> formService.getRenderedStartForm(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testGetStartFormByIdNullId() {
        assertThatThrownBy(() -> formService.getStartFormKey(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testGetStartFormByIdUnexistingProcessDefinitionId() {
        assertThatThrownBy(() -> formService.getStartFormKey("unexistingId"))
                .hasMessageContaining("no deployed process definition found with id")
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    public void testGetTaskFormNullTaskId() {
        assertThatThrownBy(() -> formService.getRenderedTaskForm(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testGetTaskFormUnexistingTaskId() {
        assertThatThrownBy(() -> formService.getRenderedTaskForm("unexistingtask"))
                .hasMessage("Task 'unexistingtask' not found")
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/form/FormsProcess.bpmn20.xml", "org/flowable/engine/test/api/form/start.form", "org/flowable/engine/test/api/form/task.form" })
    public void testTaskFormPropertyDefaultsAndFormRendering(@DeploymentId String deploymentIdFromDeploymentAnnotation) {
        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        StartFormData startForm = formService.getStartFormData(procDefId);
        assertThat(startForm).isNotNull();
        assertThat(startForm.getDeploymentId()).isEqualTo(deploymentIdFromDeploymentAnnotation);
        assertThat(startForm.getFormKey()).isEqualTo("org/flowable/engine/test/api/form/start.form");
        assertThat(startForm.getFormProperties()).isEqualTo(new ArrayList<FormProperty>());
        assertThat(startForm.getProcessDefinition().getId()).isEqualTo(procDefId);

        Object renderedStartForm = formService.getRenderedStartForm(procDefId);
        assertThat(renderedStartForm).isEqualTo("start form content");

        Map<String, String> properties = new HashMap<>();
        properties.put("room", "5b");
        properties.put("speaker", "Mike");
        String processInstanceId = formService.submitStartFormData(procDefId, properties).getId();

        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        assertThat(variables)
                .containsOnly(
                        entry("room", "5b"),
                        entry("speaker", "Mike")
                );

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        String taskId = task.getId();
        TaskFormData taskForm = formService.getTaskFormData(taskId);
        assertThat(taskForm.getDeploymentId()).isEqualTo(deploymentIdFromDeploymentAnnotation);
        assertThat(taskForm.getFormKey()).isEqualTo("org/flowable/engine/test/api/form/task.form");
        assertThat(taskForm.getFormProperties()).isEqualTo(new ArrayList<FormProperty>());
        assertThat(taskForm.getTask().getId()).isEqualTo(taskId);

        assertThat(formService.getRenderedTaskForm(taskId)).isEqualTo("Mike is speaking in room 5b");

        properties = new HashMap<>();
        properties.put("room", "3f");
        formService.submitTaskFormData(taskId, properties);

        variables = runtimeService.getVariables(processInstanceId);
        assertThat(variables)
                .containsOnly(
                        entry("room", "3f"),
                        entry("speaker", "Mike")
                );
    }

    @Test
    @Deployment
    public void testFormPropertyHandling() {
        Map<String, String> properties = new HashMap<>();
        properties.put("room", "5b"); // default
        properties.put("speaker", "Mike"); // variable name mapping
        properties.put("duration", "45"); // type conversion
        properties.put("free", "true"); // type conversion
        properties.put("double", "45.5"); // type conversion

        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        String processInstanceId = formService.submitStartFormData(procDefId, properties).getId();

        Map<String, Object> expectedVariables = new HashMap<>();
        expectedVariables.put("room", "5b");
        expectedVariables.put("SpeakerName", "Mike");
        expectedVariables.put("duration", 45L);
        expectedVariables.put("free", Boolean.TRUE);
        expectedVariables.put("double", 45.5d);

        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        assertThat(variables).isEqualTo(expectedVariables);

        Address address = new Address();
        address.setStreet("broadway");
        runtimeService.setVariable(processInstanceId, "address", address);

        runtimeService.trigger(runtimeService.createExecutionQuery().processInstanceId(processInstanceId).onlyChildExecutions().singleResult().getId());

        String taskId = taskService.createTaskQuery().singleResult().getId();
        TaskFormData taskFormData = formService.getTaskFormData(taskId);

        List<FormProperty> formProperties = taskFormData.getFormProperties();
        FormProperty propertyRoom = formProperties.get(0);
        assertThat(propertyRoom.getId()).isEqualTo("room");
        assertThat(propertyRoom.getValue()).isEqualTo("5b");

        FormProperty propertyDuration = formProperties.get(1);
        assertThat(propertyDuration.getId()).isEqualTo("duration");
        assertThat(propertyDuration.getValue()).isEqualTo("45");

        FormProperty propertySpeaker = formProperties.get(2);
        assertThat(propertySpeaker.getId()).isEqualTo("speaker");
        assertThat(propertySpeaker.getValue()).isEqualTo("Mike");

        FormProperty propertyStreet = formProperties.get(3);
        assertThat(propertyStreet.getId()).isEqualTo("street");
        assertThat(propertyStreet.getValue()).isEqualTo("broadway");

        FormProperty propertyFree = formProperties.get(4);
        assertThat(propertyFree.getId()).isEqualTo("free");
        assertThat(propertyFree.getValue()).isEqualTo("true");

        FormProperty propertyDouble = formProperties.get(5);
        assertThat(propertyDouble.getId()).isEqualTo("double");
        assertThat(propertyDouble.getValue()).isEqualTo("45.5");

        assertThat(formProperties).hasSize(6);

        assertThatThrownBy(() -> formService.submitTaskFormData(taskId, new HashMap<>()))
                .as("expected exception about required form property 'street'")
                .isInstanceOf(FlowableException.class);

        assertThatThrownBy(() -> {
            Map<String, String> propertiesSpeaker = new HashMap<>();
            propertiesSpeaker.put("speaker", "its not allowed to update speaker!");
            formService.submitTaskFormData(taskId, propertiesSpeaker);
        })
                .as("expected exception about a non writable form property 'speaker'")
                .isInstanceOf(FlowableException.class);

        properties = new HashMap<>();
        properties.put("street", "rubensstraat");
        formService.submitTaskFormData(taskId, properties);

        expectedVariables = new HashMap<>();
        expectedVariables.put("room", "5b");
        expectedVariables.put("SpeakerName", "Mike");
        expectedVariables.put("duration", 45L);
        expectedVariables.put("free", Boolean.TRUE);
        expectedVariables.put("double", 45.5d);

        variables = runtimeService.getVariables(processInstanceId);
        address = (Address) variables.remove("address");
        assertThat(address.getStreet()).isEqualTo("rubensstraat");
        assertThat(variables).isEqualTo(expectedVariables);
    }

    @Test
    @Deployment
    public void testFormPropertyExpression() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("speaker", "Mike"); // variable name mapping
        Address address = new Address();
        varMap.put("address", address);

        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefId, varMap);

        String taskId = taskService.createTaskQuery().singleResult().getId();
        TaskFormData taskFormData = formService.getTaskFormData(taskId);

        List<FormProperty> formProperties = taskFormData.getFormProperties();
        FormProperty propertySpeaker = formProperties.get(0);
        assertThat(propertySpeaker.getId()).isEqualTo("speaker");
        assertThat(propertySpeaker.getValue()).isEqualTo("Mike");

        assertThat(formProperties).hasSize(2);

        Map<String, String> properties = new HashMap<>();
        properties.put("street", "Broadway");
        formService.submitTaskFormData(taskId, properties);

        address = (Address) runtimeService.getVariable(processInstance.getId(), "address");
        assertThat(address.getStreet()).isEqualTo("Broadway");
    }

    @SuppressWarnings("unchecked")
    @Test
    @Deployment
    public void testFormPropertyDetails() {
        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        StartFormData startFormData = formService.getStartFormData(procDefId);
        FormProperty property = startFormData.getFormProperties().get(0);
        assertThat(property.getId()).isEqualTo("speaker");
        assertThat(property.getValue()).isNull();
        assertThat(property.isReadable()).isTrue();
        assertThat(property.isWritable()).isTrue();
        assertThat(property.isRequired()).isFalse();
        assertThat(property.getType().getName()).isEqualTo("string");

        property = startFormData.getFormProperties().get(1);
        assertThat(property.getId()).isEqualTo("start");
        assertThat(property.getValue()).isNull();
        assertThat(property.isReadable()).isTrue();
        assertThat(property.isWritable()).isTrue();
        assertThat(property.isRequired()).isFalse();
        assertThat(property.getType().getName()).isEqualTo("date");
        assertThat(property.getType().getInformation("datePattern")).isEqualTo("dd-MMM-yyyy");

        property = startFormData.getFormProperties().get(2);
        assertThat(property.getId()).isEqualTo("direction");
        assertThat(property.getValue()).isNull();
        assertThat(property.isReadable()).isTrue();
        assertThat(property.isWritable()).isTrue();
        assertThat(property.isRequired()).isFalse();
        assertThat(property.getType().getName()).isEqualTo("enum");
        Map<String, String> values = (Map<String, String>) property.getType().getInformation("values");

        Map<String, String> expectedValues = new LinkedHashMap<>();
        expectedValues.put("left", "Go Left");
        expectedValues.put("right", "Go Right");
        expectedValues.put("up", "Go Up");
        expectedValues.put("down", "Go Down");

        // ACT-1023: check if ordering is retained
        Iterator<Entry<String, String>> expectedValuesIterator = expectedValues.entrySet().iterator();
        for (Entry<String, String> entry : values.entrySet()) {
            Entry<String, String> expectedEntryAtLocation = expectedValuesIterator.next();
            assertThat(entry.getKey()).isEqualTo(expectedEntryAtLocation.getKey());
            assertThat(entry.getValue()).isEqualTo(expectedEntryAtLocation.getValue());
        }
        assertThat(values).isEqualTo(expectedValues);
    }

    @Test
    @Deployment
    public void testInvalidFormKeyReference() {
        assertThatThrownBy(() -> formService.getRenderedStartForm(repositoryService.createProcessDefinitionQuery().singleResult().getId()))
                .hasMessage("Form with formKey 'IDoNotExist' does not exist")
                .isInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment
    public void testSubmitStartFormDataWithBusinessKey() {
        Map<String, String> properties = new HashMap<>();
        properties.put("duration", "45");
        properties.put("speaker", "Mike");
        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

        ProcessInstance processInstance = formService.submitStartFormData(procDefId, "123", properties);
        assertThat(processInstance.getBusinessKey()).isEqualTo("123");

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("123").singleResult().getId()).isEqualTo(processInstance.getId());
    }

    @Test
    public void testGetStartFormKeyEmptyArgument() {
        assertThatThrownBy(() -> formService.getStartFormKey(null))
                .hasMessage("The process definition id is mandatory, but 'null' has been provided.")
                .isInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> formService.getStartFormKey(""))
                .hasMessage("The process definition id is mandatory, but '' has been provided.")
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/form/FormsProcess.bpmn20.xml")
    public void testGetStartFormKey() {
        String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        String expectedFormKey = formService.getStartFormData(processDefinitionId).getFormKey();
        String actualFormKey = formService.getStartFormKey(processDefinitionId);
        assertThat(actualFormKey).isEqualTo(expectedFormKey);
    }

    @Test
    public void testGetTaskFormKeyEmptyArguments() {
        assertThatThrownBy(() -> formService.getTaskFormKey( null, "23"))
                .hasMessage("The process definition id is mandatory, but 'null' has been provided.")
                .isInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> formService.getTaskFormKey( "", "23"))
                .hasMessage("The process definition id is mandatory, but '' has been provided.")
                .isInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> formService.getTaskFormKey( "42", null))
                .hasMessage("The task definition key is mandatory, but 'null' has been provided.")
                .isInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> formService.getTaskFormKey( "42", ""))
                .hasMessage("The task definition key is mandatory, but '' has been provided.")
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/form/FormsProcess.bpmn20.xml")
    public void testGetTaskFormKey() {
        String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        runtimeService.startProcessInstanceById(processDefinitionId);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        String expectedFormKey = formService.getTaskFormData(task.getId()).getFormKey();
        String actualFormKey = formService.getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        assertThat(actualFormKey).isEqualTo(expectedFormKey);
    }

    @Test
    @Deployment
    public void testGetTaskFormKeyWithExpression() {
        runtimeService.startProcessInstanceByKey("FormsProcess", CollectionUtil.singletonMap("dynamicKey", "test"));
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(formService.getTaskFormData(task.getId()).getFormKey()).isEqualTo("test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSubmitTaskFormData() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        assertThat(processDefinitions).hasSize(1);
        ProcessDefinition processDefinition = processDefinitions.get(0);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinition.getKey());
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        Map<String, String> properties = new HashMap<>();
        properties.put("room", "5b");

        formService.submitTaskFormData(task.getId(), properties);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNull();

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSaveFormData() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        assertThat(processDefinitions).hasSize(1);
        ProcessDefinition processDefinition = processDefinitions.get(0);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinition.getKey());
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        String taskId = task.getId();

        Map<String, String> properties = new HashMap<>();
        properties.put("room", "5b");

        Map<String, String> expectedVariables = new HashMap<>();
        expectedVariables.put("room", "5b");

        formService.saveFormData(task.getId(), properties);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getId()).isEqualTo(taskId);

        Map<String, Object> variables = taskService.getVariables(taskId);
        assertThat(variables).isEqualTo(expectedVariables);

    }
}
