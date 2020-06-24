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
package org.flowable.cmmn.editor.json.converter;

import java.util.Map;

import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.ListenerConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ServiceTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class MailTaskJsonConverter extends BaseCmmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap) {

        fillJsonTypes(convertersToCmmnMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_MAIL, MailTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_MAIL;
    }

    @Override
    protected CaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
        BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper) {

        ServiceTask task = new ServiceTask();

        task.setType(ServiceTask.MAIL_TASK);

        addField("headers", PROPERTY_MAILTASK_HEADERS, elementNode, task);
        addField("to", PROPERTY_MAILTASK_TO, elementNode, task);
        addField("from", PROPERTY_MAILTASK_FROM, elementNode, task);
        addField("subject", PROPERTY_MAILTASK_SUBJECT, elementNode, task);
        addField("cc", PROPERTY_MAILTASK_CC, elementNode, task);
        addField("bcc", PROPERTY_MAILTASK_BCC, elementNode, task);
        addField("text", PROPERTY_MAILTASK_TEXT, elementNode, task);
        addField("textVar", PROPERTY_MAILTASK_TEXT_VAR, elementNode, task);
        addField("html", PROPERTY_MAILTASK_HTML, elementNode, task);
        addField("htmlVar", PROPERTY_MAILTASK_HTML_VAR, elementNode, task);
        addField("charset", PROPERTY_MAILTASK_CHARSET, elementNode, task);

        ListenerConverterUtil.convertJsonToLifeCycleListeners(elementNode, task);

        return task;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement,
        CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {

    }

}
