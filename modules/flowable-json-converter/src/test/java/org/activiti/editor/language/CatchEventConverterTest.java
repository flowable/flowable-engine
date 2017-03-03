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
import static org.junit.Assert.assertTrue;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EventDefinition;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.MessageEventDefinition;
import org.activiti.bpmn.model.SignalEventDefinition;
import org.activiti.bpmn.model.TimerEventDefinition;
import org.junit.Test;


public class CatchEventConverterTest extends AbstractConverterTest {

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
    return "test.catcheventmodel.json";
  }
  
  private void validateModel(BpmnModel model) {
    
    FlowElement timerElement = model.getMainProcess().getFlowElement("timer_evt");
    EventDefinition timerEvent = extractEventDefinition(timerElement);
    assertTrue(timerEvent instanceof TimerEventDefinition);
    TimerEventDefinition ted = (TimerEventDefinition)timerEvent;
    assertEquals("PT5M", ted.getTimeDuration());
    
    FlowElement signalElement = model.getMainProcess().getFlowElement("signal_evt");
    EventDefinition signalEvent = extractEventDefinition(signalElement);
    assertTrue(signalEvent instanceof SignalEventDefinition);
    SignalEventDefinition sed = (SignalEventDefinition)signalEvent;
    assertEquals("signal_ref", sed.getSignalRef());
    
    FlowElement messageElement = model.getMainProcess().getFlowElement("message_evt");
    EventDefinition messageEvent = extractEventDefinition(messageElement);
    assertTrue(messageEvent instanceof MessageEventDefinition);
    MessageEventDefinition med = (MessageEventDefinition)messageEvent;
    assertEquals("message_ref", med.getMessageRef());
    
  }
  
}
