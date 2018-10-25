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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.ProcessTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Tijs Rademakers
 */
public class ProcessTaskJsonConverter extends BaseCmmnJsonConverter implements ProcessModelAwareConverter {
    
    protected Map<String, String> processModelMap;
    
    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_PROCESS, ProcessTaskJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(CaseTask.class, ProcessTaskJsonConverter.class);
    }
    
    @Override
    protected String getStencilId(BaseElement baseElement) {
        return CmmnStencilConstants.STENCIL_TASK_PROCESS;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
            BaseElement baseElement, CmmnModel cmmnModel) {
        // todo
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
            BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {
        ProcessTask task = new ProcessTask();
        
        JsonNode processModelReferenceNode = CmmnJsonConverterUtil.getProperty(PROPERTY_PROCESS_REFERENCE, elementNode);
        if (processModelReferenceNode != null && processModelReferenceNode.has("id") && !processModelReferenceNode.get("id").isNull()) {

            String processModelId = processModelReferenceNode.get("id").asText();
            if (processModelMap != null) {
                String processModelKey = processModelMap.get(processModelId);
                task.setProcessRef(processModelKey);
            }
        }

        JsonNode processTaskInParametersNode = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_PROCESS_IN_PARAMETERS, elementNode);
        if (processTaskInParametersNode != null && processTaskInParametersNode.has("inParameters") && !processTaskInParametersNode.get("inParameters").isNull()) {
            JsonNode inParametersNode =  processTaskInParametersNode.get("inParameters");
            task.setInParameters(readIOParameters(inParametersNode));
        }


        JsonNode processTaskOutParametersNode = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_PROCESS_OUT_PARAMETERS, elementNode);
        if (processTaskOutParametersNode != null && processTaskOutParametersNode.has("outParameters") && !processTaskOutParametersNode.get("outParameters").isNull()) {
            JsonNode outParametersNode =  processTaskOutParametersNode.get("outParameters");
            task.setOutParameters(readIOParameters(outParametersNode));
        }
        return task;
    }

    private List<IOParameter> readIOParameters(JsonNode parametersNode) {
        List<IOParameter> ioParameters = new ArrayList<>();
        for (JsonNode paramNode : parametersNode){
            IOParameter ioParameter = new IOParameter();
            ioParameter.setSource(paramNode.get("source").asText());
            ioParameter.setSourceExpression(paramNode.get("sourceExpression").asText());
            ioParameter.setTarget(paramNode.get("target").asText());
            ioParameter.setTargetExpression(paramNode.get("targetExpression").asText());
            ioParameters.add(ioParameter);
        }
        return ioParameters;
    }

    @Override
    public void setProcessModelMap(Map<String, String> processModelMap) {
        this.processModelMap = processModelMap;
    }
}
