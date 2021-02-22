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
 * This class converts {@link org.flowable.bpmn.model.ServiceTask} to json representation for modeler
 *
 * @author martin.grofcik
 */
public class ShellTaskJsonConverter extends BaseBpmnJsonConverter {
    public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_SHELL, ShellTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_SHELL;
    }

    @Override
    protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement,
        BpmnJsonConverterContext converterContext) {
        // done in service task
    }

    @Override
    protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap,
        BpmnJsonConverterContext converterContext) {
        ServiceTask task = new ServiceTask();
        task.setType("shell");
        addField("command", PROPERTY_SHELLTASK_COMMAND, elementNode, task);
        addField("arg1", PROPERTY_SHELLTASK_ARG1, elementNode, task);
        addField("arg2", PROPERTY_SHELLTASK_ARG2, elementNode, task);
        addField("arg3", PROPERTY_SHELLTASK_ARG3, elementNode, task);
        addField("arg4", PROPERTY_SHELLTASK_ARG4, elementNode, task);
        addField("arg5", PROPERTY_SHELLTASK_ARG5, elementNode, task);
        addField("wait", PROPERTY_SHELLTASK_WAIT, elementNode, task);
        addField("cleanEnv", PROPERTY_SHELLTASK_CLEAN_ENV, elementNode, task);
        addField("errorCodeVariable", PROPERTY_SHELLTASK_ERROR_CODE_VARIABLE, elementNode, task);
        addField("errorRedirect", PROPERTY_SHELLTASK_ERROR_REDIRECT, elementNode, task);
        addField("outputVariable", PROPERTY_SHELLTASK_OUTPUT_VARIABLE, elementNode, task);
        addField("directory", PROPERTY_SHELLTASK_DIRECTORY, elementNode, task);
        return task;
    }

}
