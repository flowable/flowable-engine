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
package org.flowable.bpmn.converter;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.child.ScriptTextParser;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ScriptTask;

/**
 * @author Tijs Rademakers
 */
public class ScriptTaskXMLConverter extends BaseBpmnXMLConverter {

    protected Map<String, BaseChildElementParser> childParserMap = new HashMap<>();

    public ScriptTaskXMLConverter() {
        ScriptTextParser scriptTextParser = new ScriptTextParser();
        childParserMap.put(scriptTextParser.getElementName(), scriptTextParser);
    }

    @Override
    public Class<? extends BaseElement> getBpmnElementType() {
        return ScriptTask.class;
    }

    @Override
    protected String getXMLElementName() {
        return ELEMENT_TASK_SCRIPT;
    }

    @Override
    protected BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception {
        ScriptTask scriptTask = new ScriptTask();
        BpmnXMLUtil.addXMLLocation(scriptTask, xtr);
        scriptTask.setScriptFormat(xtr.getAttributeValue(null, ATTRIBUTE_TASK_SCRIPT_FORMAT));
        scriptTask.setResultVariable(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SCRIPT_RESULTVARIABLE, xtr));
        if (StringUtils.isEmpty(scriptTask.getResultVariable())) {
            scriptTask.setResultVariable(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_RESULTVARIABLE, xtr));
        }
        String skipExpression = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SCRIPT_SKIP_EXPRESSION, xtr);
        if (StringUtils.isNotEmpty(skipExpression)) {
            scriptTask.setSkipExpression(skipExpression);
        }
        String autoStoreVariables = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SCRIPT_AUTO_STORE_VARIABLE, xtr);
        if (StringUtils.isNotEmpty(autoStoreVariables)) {
            scriptTask.setAutoStoreVariables(Boolean.valueOf(autoStoreVariables));
        }
        parseChildElements(getXMLElementName(), scriptTask, childParserMap, model, xtr);
        return scriptTask;
    }

    @Override
    protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        ScriptTask scriptTask = (ScriptTask) element;
        writeDefaultAttribute(ATTRIBUTE_TASK_SCRIPT_FORMAT, scriptTask.getScriptFormat(), xtw);
        writeQualifiedAttribute(ATTRIBUTE_TASK_SCRIPT_RESULTVARIABLE, scriptTask.getResultVariable(), xtw);
        writeQualifiedAttribute(ATTRIBUTE_TASK_SCRIPT_SKIP_EXPRESSION, scriptTask.getSkipExpression(), xtw);
        writeQualifiedAttribute(ATTRIBUTE_TASK_SCRIPT_AUTO_STORE_VARIABLE, String.valueOf(scriptTask.isAutoStoreVariables()), xtw);
    }

    @Override
    protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        ScriptTask scriptTask = (ScriptTask) element;
        if (StringUtils.isNotEmpty(scriptTask.getScript())) {
            xtw.writeStartElement(ATTRIBUTE_TASK_SCRIPT_TEXT);
            xtw.writeCData(scriptTask.getScript());
            xtw.writeEndElement();
        }
    }
}
