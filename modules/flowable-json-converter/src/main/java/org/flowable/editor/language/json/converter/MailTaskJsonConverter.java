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
package org.flowable.editor.language.json.converter;

import java.util.Map;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class MailTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_MAIL, MailTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
        // will be handled by ServiceTaskJsonConverter
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_MAIL;
    }

    @Override
    protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement,
        BpmnJsonConverterContext converterContext) {
        // will be handled by ServiceTaskJsonConverter
    }

    @Override
    protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap,
        BpmnJsonConverterContext converterContext) {
        ServiceTask task = new ServiceTask();
        task.setType(ServiceTask.MAIL_TASK);
        addField(PROPERTY_MAILTASK_HEADERS, elementNode, task);
        addField(PROPERTY_MAILTASK_TO, elementNode, task);
        addField(PROPERTY_MAILTASK_FROM, elementNode, task);
        addField(PROPERTY_MAILTASK_SUBJECT, elementNode, task);
        addField(PROPERTY_MAILTASK_CC, elementNode, task);
        addField(PROPERTY_MAILTASK_BCC, elementNode, task);
        addField(PROPERTY_MAILTASK_TEXT, elementNode, task);
        addField(PROPERTY_MAILTASK_HTML, elementNode, task);
        addField("htmlVar", PROPERTY_MAILTASK_HTML_VAR, elementNode, task);
        addField("textVar", PROPERTY_MAILTASK_TEXT_VAR, elementNode, task);
        addField(PROPERTY_MAILTASK_CHARSET, elementNode, task);

        return task;
    }
}
