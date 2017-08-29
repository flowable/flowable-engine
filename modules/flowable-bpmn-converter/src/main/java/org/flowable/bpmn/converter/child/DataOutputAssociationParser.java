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
package org.flowable.bpmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DataAssociation;

/**
 * @author Tijs Rademakers
 */
public class DataOutputAssociationParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_OUTPUT_ASSOCIATION;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {

        if (!(parentElement instanceof Activity))
            return;

        DataAssociation dataAssociation = new DataAssociation();
        BpmnXMLUtil.addXMLLocation(dataAssociation, xtr);
        DataAssociationParser.parseDataAssociation(dataAssociation, getElementName(), xtr);

        ((Activity) parentElement).getDataOutputAssociations().add(dataAssociation);
    }
}
