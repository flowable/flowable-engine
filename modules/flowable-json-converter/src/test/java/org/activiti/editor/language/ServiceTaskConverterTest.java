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
package org.activiti.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.ServiceTask;
import org.junit.Test;

public class ServiceTaskConverterTest extends AbstractConverterTest {

  @Test
  public void connvertJsonToModel() throws Exception {
    BpmnModel bpmnModel = readJsonFile();
    validateModel(bpmnModel);
  }
  
  @Test 
  public void doubleConversionValidation() throws Exception {
    BpmnModel bpmnModel = readJsonFile();
    bpmnModel = convertToJsonAndBack(bpmnModel);
    validateModel(bpmnModel);
  }
  
  protected String getResource() {
    return "test.servicetaskmodel.json";
  }
  
  private void validateModel(BpmnModel model) {
    FlowElement flowElement = model.getMainProcess().getFlowElement("servicetask");
    assertNotNull(flowElement);
    assertTrue(flowElement instanceof ServiceTask);
    assertEquals("servicetask", flowElement.getId());
    ServiceTask serviceTask = (ServiceTask) flowElement;
    assertEquals("servicetask", serviceTask.getId());
    assertEquals("Service task", serviceTask.getName());
    
    List<FieldExtension> fields = serviceTask.getFieldExtensions();
    assertEquals(2, fields.size());
    FieldExtension field = (FieldExtension) fields.get(0);
    assertEquals("testField", field.getFieldName());
    assertEquals("test", field.getStringValue());
    field = (FieldExtension) fields.get(1);
    assertEquals("testField2", field.getFieldName());
    assertEquals("${test}", field.getExpression());
    
    List<ActivitiListener> listeners = serviceTask.getExecutionListeners();
    assertEquals(3, listeners.size());
    ActivitiListener listener = (ActivitiListener) listeners.get(0);
    assertTrue(ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(listener.getImplementationType()));
    assertEquals("org.test.TestClass", listener.getImplementation());
    assertEquals("start", listener.getEvent());
    listener = (ActivitiListener) listeners.get(1);
    assertTrue(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(listener.getImplementationType()));
    assertEquals("${testExpression}", listener.getImplementation());
    assertEquals("end", listener.getEvent());
    listener = (ActivitiListener) listeners.get(2);
    assertTrue(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(listener.getImplementationType()));
    assertEquals("${delegateExpression}", listener.getImplementation());
    assertEquals("start", listener.getEvent());
  }
}
