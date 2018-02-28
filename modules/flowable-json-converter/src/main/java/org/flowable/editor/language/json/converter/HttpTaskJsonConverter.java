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
import org.flowable.bpmn.model.HttpServiceTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class HttpTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_HTTP, HttpTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_HTTP;
    }

    @Override
    protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement) {
        // done in service task
    }

    @Override
    protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
        HttpServiceTask task = new HttpServiceTask();
        task.setType("http");
        addField("requestMethod", PROPERTY_HTTPTASK_REQ_METHOD, "GET", elementNode, task);
        addField("requestUrl", PROPERTY_HTTPTASK_REQ_URL, elementNode, task);
        addField("requestHeaders", PROPERTY_HTTPTASK_REQ_HEADERS, elementNode, task);
        addField("requestBody", PROPERTY_HTTPTASK_REQ_BODY, elementNode, task);
        addField("requestTimeout", PROPERTY_HTTPTASK_REQ_TIMEOUT, elementNode, task);
        addField("disallowRedirects", PROPERTY_HTTPTASK_REQ_DISALLOW_REDIRECTS, elementNode, task);
        addField("failStatusCodes", PROPERTY_HTTPTASK_REQ_FAIL_STATUS_CODES, elementNode, task);
        addField("handleStatusCodes", PROPERTY_HTTPTASK_REQ_HANDLE_STATUS_CODES, elementNode, task);
        addField("responseVariableName", PROPERTY_HTTPTASK_RESPONSE_VARIABLE_NAME, elementNode, task);
        addField("ignoreException", PROPERTY_HTTPTASK_REQ_IGNORE_EXCEPTION, elementNode, task);
        addField("saveRequestVariables", PROPERTY_HTTPTASK_SAVE_REQUEST_VARIABLES, elementNode, task);
        addField("saveResponseParameters", PROPERTY_HTTPTASK_SAVE_RESPONSE_PARAMETERS, elementNode, task);
        addField("resultVariablePrefix", PROPERTY_HTTPTASK_RESULT_VARIABLE_PREFIX, elementNode, task);
        addField("saveResponseParametersTransient", PROPERTY_HTTPTASK_SAVE_RESPONSE_TRANSIENT, elementNode, task);
        addField("saveResponseVariableAsJson", PROPERTY_HTTPTASK_SAVE_RESPONSE_AS_JSON, elementNode, task);
        return task;
    }
}
