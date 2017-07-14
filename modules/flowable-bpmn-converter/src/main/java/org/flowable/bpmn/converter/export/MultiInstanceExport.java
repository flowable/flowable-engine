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

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;

public class MultiInstanceExport implements BpmnXMLConstants {

    public static void writeMultiInstance(Activity activity, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        if (activity.getLoopCharacteristics() != null) {
            MultiInstanceLoopCharacteristics multiInstanceObject = activity.getLoopCharacteristics();
            if (StringUtils.isNotEmpty(multiInstanceObject.getLoopCardinality()) || StringUtils.isNotEmpty(multiInstanceObject.getInputDataItem())
                    || StringUtils.isNotEmpty(multiInstanceObject.getCompletionCondition())) {

                xtw.writeStartElement(ELEMENT_MULTIINSTANCE);
                BpmnXMLUtil.writeDefaultAttribute(ATTRIBUTE_MULTIINSTANCE_SEQUENTIAL, String.valueOf(multiInstanceObject.isSequential()).toLowerCase(), xtw);
                if (StringUtils.isNotEmpty(multiInstanceObject.getInputDataItem())) {
                    BpmnXMLUtil.writeQualifiedAttribute(ATTRIBUTE_MULTIINSTANCE_COLLECTION, multiInstanceObject.getInputDataItem(), xtw);
                }
                if (StringUtils.isNotEmpty(multiInstanceObject.getElementVariable())) {
                    BpmnXMLUtil.writeQualifiedAttribute(ATTRIBUTE_MULTIINSTANCE_VARIABLE, multiInstanceObject.getElementVariable(), xtw);
                }
                if (StringUtils.isNotEmpty(multiInstanceObject.getLoopCardinality())) {
                    xtw.writeStartElement(ELEMENT_MULTIINSTANCE_CARDINALITY);
                    xtw.writeCharacters(multiInstanceObject.getLoopCardinality());
                    xtw.writeEndElement();
                }
                if (StringUtils.isNotEmpty(multiInstanceObject.getCompletionCondition())) {
                    xtw.writeStartElement(ELEMENT_MULTIINSTANCE_CONDITION);
                    xtw.writeCharacters(multiInstanceObject.getCompletionCondition());
                    xtw.writeEndElement();
                }

                // check for extension elements
                Map<String, List<ExtensionElement>> extensions = multiInstanceObject.getExtensionElements();
                if (!extensions.isEmpty()) {
                	BpmnXMLUtil.writeExtensionElements(multiInstanceObject, false, model.getNamespaces(), xtw);
                }
                xtw.writeEndElement();
            }
        }
    }
}
