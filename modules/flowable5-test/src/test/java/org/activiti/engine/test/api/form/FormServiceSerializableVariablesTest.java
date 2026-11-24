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
package org.activiti.engine.test.api.form;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * Form property tests that write a property of a Java bean ({@link Address}) process variable through a form property expression. The Flowable 5
 * engine only detects changes of such beans when they are stored as serializable variables, so these tests require serializable variables to be
 * enabled.
 *
 * @author Joram Barrez
 * @author Frederik Heremans
 */
public class FormServiceSerializableVariablesTest extends ResourceFlowableTestCase {

    public FormServiceSerializableVariablesTest() {
        super("org/activiti/engine/test/serializable-variables.flowable.cfg.xml");
    }

    @Deployment(resources = "org/activiti/engine/test/api/form/FormServiceTest.testFormPropertyHandling.bpmn20.xml")
    public void testFormPropertyHandling() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("room", "5b"); // default
        properties.put("speaker", "Mike"); // variable name mapping
        properties.put("duration", "45"); // type conversion
        properties.put("free", "true"); // type conversion
        properties.put("double", "45.5"); // type conversion

        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        String processInstanceId = formService.submitStartFormData(procDefId, properties).getId();

        Map<String, Object> expectedVariables = new HashMap<String, Object>();
        expectedVariables.put("room", "5b");
        expectedVariables.put("SpeakerName", "Mike");
        expectedVariables.put("duration", 45L);
        expectedVariables.put("free", Boolean.TRUE);
        expectedVariables.put("double", 45.5d);

        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        assertEquals(expectedVariables, variables);

        Address address = new Address();
        address.setStreet("broadway");
        runtimeService.setVariable(processInstanceId, "address", address);

        runtimeService.trigger(runtimeService.createExecutionQuery().processInstanceId(processInstanceId).singleResult().getId());

        String taskId = taskService.createTaskQuery().singleResult().getId();
        TaskFormData taskFormData = formService.getTaskFormData(taskId);

        List<FormProperty> formProperties = taskFormData.getFormProperties();
        FormProperty propertyRoom = formProperties.get(0);
        assertEquals("room", propertyRoom.getId());
        assertEquals("5b", propertyRoom.getValue());

        FormProperty propertyDuration = formProperties.get(1);
        assertEquals("duration", propertyDuration.getId());
        assertEquals("45", propertyDuration.getValue());

        FormProperty propertySpeaker = formProperties.get(2);
        assertEquals("speaker", propertySpeaker.getId());
        assertEquals("Mike", propertySpeaker.getValue());

        FormProperty propertyStreet = formProperties.get(3);
        assertEquals("street", propertyStreet.getId());
        assertEquals("broadway", propertyStreet.getValue());

        FormProperty propertyFree = formProperties.get(4);
        assertEquals("free", propertyFree.getId());
        assertEquals("true", propertyFree.getValue());

        FormProperty propertyDouble = formProperties.get(5);
        assertEquals("double", propertyDouble.getId());
        assertEquals("45.5", propertyDouble.getValue());

        assertEquals(6, formProperties.size());

        try {
            formService.submitTaskFormData(taskId, new HashMap<String, String>());
            fail("expected exception about required form property 'street'");
        } catch (FlowableException e) {
            // OK
        }

        try {
            properties = new HashMap<String, String>();
            properties.put("speaker", "its not allowed to update speaker!");
            formService.submitTaskFormData(taskId, properties);
            fail("expected exception about a non writable form property 'speaker'");
        } catch (FlowableException e) {
            // OK
        }

        properties = new HashMap<String, String>();
        properties.put("street", "rubensstraat");
        formService.submitTaskFormData(taskId, properties);

        expectedVariables = new HashMap<String, Object>();
        expectedVariables.put("room", "5b");
        expectedVariables.put("SpeakerName", "Mike");
        expectedVariables.put("duration", 45L);
        expectedVariables.put("free", Boolean.TRUE);
        expectedVariables.put("double", 45.5d);

        variables = runtimeService.getVariables(processInstanceId);
        address = (Address) variables.remove("address");
        assertEquals("rubensstraat", address.getStreet());
        assertEquals(expectedVariables, variables);
    }

    @Deployment(resources = "org/activiti/engine/test/api/form/FormServiceTest.testFormPropertyExpression.bpmn20.xml")
    public void testFormPropertyExpression() {
        Map<String, Object> varMap = new HashMap<String, Object>();
        varMap.put("speaker", "Mike"); // variable name mapping
        Address address = new Address();
        varMap.put("address", address);

        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefId, varMap);

        String taskId = taskService.createTaskQuery().singleResult().getId();
        TaskFormData taskFormData = formService.getTaskFormData(taskId);

        List<FormProperty> formProperties = taskFormData.getFormProperties();
        FormProperty propertySpeaker = formProperties.get(0);
        assertEquals("speaker", propertySpeaker.getId());
        assertEquals("Mike", propertySpeaker.getValue());

        assertEquals(2, formProperties.size());

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("street", "Broadway");
        formService.submitTaskFormData(taskId, properties);

        address = (Address) runtimeService.getVariable(processInstance.getId(), "address");
        assertEquals("Broadway", address.getStreet());
    }

}
