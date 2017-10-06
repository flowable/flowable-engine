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
import static org.junit.Assert.assertNotNull;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Message;
import org.junit.Test;

public class MessageConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    private void validateModel(BpmnModel model) {
        Message message = model.getMessage("writeReport");
        assertNotNull(message);
        assertEquals("Examples:writeReportItem", message.getItemRef());
        assertEquals("newWriteReport", message.getName());
        assertEquals("writeReport", message.getId());

        Message message2 = model.getMessage("writeReport2");
        assertNotNull(message2);
        assertEquals("http://foo.bar.com/Examples:writeReportItem2", message2.getItemRef());
        assertEquals("newWriteReport2", message2.getName());
        assertEquals("writeReport2", message2.getId());
    }

    @Override
    protected String getResource() {
        return "message.bpmn";
    }
}
