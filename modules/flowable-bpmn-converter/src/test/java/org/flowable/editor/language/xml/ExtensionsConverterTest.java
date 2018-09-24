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
package org.flowable.editor.language.xml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class ExtensionsConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "extensions.bpmn20.xml";
    }
    
    @Test
    public void convertXMLToModel() throws Exception {
        
        // Check that reconverting doesn't duplicate extension elements
        BpmnModel bpmnModel = readXMLFile();
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement("theTask");
        List<ExtensionElement> extensionElements = flowElement.getExtensionElements().get("test");
        assertEquals(1, extensionElements.size());
        
        // Reconvert to xml and back to bpmn model
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        byte[] xmlBytes = bpmnXMLConverter.convertToXML(bpmnModel);
        bpmnModel = readXMLFile(new ByteArrayInputStream(xmlBytes));
        
        extensionElements = bpmnModel.getMainProcess().getFlowElement("theTask").getExtensionElements().get("test");
        assertEquals(1, extensionElements.size());
    }

}
