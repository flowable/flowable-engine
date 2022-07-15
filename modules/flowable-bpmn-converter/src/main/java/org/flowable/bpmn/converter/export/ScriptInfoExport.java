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

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.ScriptInfo;

/**
 * @author Arthur Hupka-Merle
 */
public class ScriptInfoExport implements BpmnXMLConstants {

    public static void writeScriptInfo(XMLStreamWriter xtw, ScriptInfo scriptInfo) throws Exception {
        if (scriptInfo == null) {
            return;
        }
        xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, BpmnXMLConstants.ELEMENT_SCRIPT, FLOWABLE_EXTENSIONS_NAMESPACE);
        if (scriptInfo.getLanguage() != null) {
            BpmnXMLUtil.writeDefaultAttribute(BpmnXMLConstants.ATTRIBUTE_SCRIPT_LANGUAGE, scriptInfo.getLanguage(), xtw);
        }
        if (scriptInfo.getResultVariable() != null) {
            BpmnXMLUtil.writeDefaultAttribute(BpmnXMLConstants.ATTRIBUTE_SCRIPT_RESULTVARIABLE, scriptInfo.getResultVariable(), xtw);
        }
        if (scriptInfo.getScript() != null) {
            xtw.writeCData(scriptInfo.getScript());
        }
        xtw.writeEndElement();
    }
}
