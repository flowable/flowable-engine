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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.HasScriptInfo;
import org.flowable.bpmn.model.ScriptInfo;

/**
 * @author Arthur Hupka-Merle
 */
public class ScriptInfoParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_SCRIPT;
    }

    @Override
    public boolean accepts(BaseElement element) {
        return element instanceof HasScriptInfo;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!accepts(parentElement)) {
            return;
        }
        if (xtr.isStartElement() && BpmnXMLConstants.ELEMENT_SCRIPT.equals(xtr.getLocalName())) {
            ScriptInfo script = new ScriptInfo();
            BpmnXMLUtil.addXMLLocation(script, xtr);
            if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_SCRIPT_LANGUAGE))) {
                script.setLanguage(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_SCRIPT_LANGUAGE));
            }
            if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_SCRIPT_RESULTVARIABLE))) {
                script.setResultVariable(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_SCRIPT_RESULTVARIABLE));
            }
            String elementText = xtr.getElementText();

            if (StringUtils.isNotEmpty(elementText)) {
                script.setScript(elementText);
            }
            if (parentElement instanceof HasScriptInfo) {
                ((HasScriptInfo) parentElement).setScriptInfo(script);
            }
        }
    }
}
