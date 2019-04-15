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
package org.flowable.bpmn.converter.export;

import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;

/**
 * @author Zheng Ji
 */
public class ErrorDefinitionExport implements BpmnXMLConstants {

    public static void writeErrors(BpmnModel model, XMLStreamWriter xtw) throws Exception {
        Map<String, String> errors = model.getErrors();
        for (Map.Entry<String, String> errorEntry : errors.entrySet()) {
            String errorId = errorEntry.getKey();
            String errorCode = errorEntry.getValue();
            xtw.writeStartElement(ELEMENT_ERROR);
            xtw.writeAttribute(ATTRIBUTE_ID, errorId);
            xtw.writeAttribute(ATTRIBUTE_ERROR_CODE, errorCode);
            xtw.writeEndElement();
        }
    }
}
