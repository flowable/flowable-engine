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
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Process;

public class LaneExport implements BpmnXMLConstants {

    public static void writeLanes(Process process, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        if (!process.getLanes().isEmpty()) {
            xtw.writeStartElement(ELEMENT_LANESET);
            xtw.writeAttribute(ATTRIBUTE_ID, "laneSet_" + process.getId());
            for (Lane lane : process.getLanes()) {
                xtw.writeStartElement(ELEMENT_LANE);
                xtw.writeAttribute(ATTRIBUTE_ID, lane.getId());

                if (StringUtils.isNotEmpty(lane.getName())) {
                    xtw.writeAttribute(ATTRIBUTE_NAME, lane.getName());
                }

                boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(lane, false, model.getNamespaces(), xtw);
                if (didWriteExtensionStartElement) {
                    xtw.writeEndElement();
                }

                for (String flowNodeRef : lane.getFlowReferences()) {
                    xtw.writeStartElement(ELEMENT_FLOWNODE_REF);
                    xtw.writeCharacters(flowNodeRef);
                    xtw.writeEndElement();
                }

                xtw.writeEndElement();
            }
            xtw.writeEndElement();
        }
    }
}
