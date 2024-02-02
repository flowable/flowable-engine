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

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Escalation;

public class EscalationDefinitionExport implements BpmnXMLConstants {

    public static void writeEscalations(BpmnModel model, XMLStreamWriter xtw) throws Exception {

        for (Escalation escalation : model.getEscalations()) {
            xtw.writeStartElement(ELEMENT_ESCALATION);
            xtw.writeAttribute(ATTRIBUTE_ID, escalation.getId());
            if (StringUtils.isNotEmpty(escalation.getName())) {
                xtw.writeAttribute(ATTRIBUTE_NAME, escalation.getName());
            }
            xtw.writeAttribute(ATTRIBUTE_ESCALATION_CODE, escalation.getEscalationCode());
            xtw.writeEndElement();
        }
    }
}
