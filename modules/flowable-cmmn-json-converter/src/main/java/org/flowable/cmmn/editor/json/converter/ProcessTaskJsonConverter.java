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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.ListenerConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.ProcessTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        convertersToJsonMap.put(ProcessTask.class, ProcessTaskJsonConverter.class);
    }
    
    @Override
    protected String getStencilId(BaseElement baseElement) {
        return CmmnStencilConstants.STENCIL_TASK_PROCESS;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
            BaseElement baseElement, CmmnModel cmmnModel) {
        
        ProcessTask processTask = (ProcessTask) ((PlanItem) baseElement).getPlanItemDefinition();

        if (processTask.getFallbackToDefaultTenant() != null) {
            propertiesNode.put(PROPERTY_FALLBACK_TO_DEFAULT_TENANT, processTask.getFallbackToDefaultTenant());
        }
        
        ListenerConverterUtil.convertLifecycleListenersToJson(objectMapper, propertiesNode, processTask);

        if (processTask.getInParameters() != null && !processTask.getInParameters().isEmpty()) {
            ObjectNode inParametersNode = propertiesNode.putObject(CmmnStencilConstants.PROPERTY_PROCESS_IN_PARAMETERS);
            ArrayNode inParametersArray = inParametersNode.putArray("inParameters");
            readIOParameters(processTask.getInParameters(), inParametersArray);
        }
        if (processTask.getOutParameters() != null && !processTask.getOutParameters().isEmpty()) {
            ObjectNode outParametersNode = propertiesNode.putObject(CmmnStencilConstants.PROPERTY_PROCESS_OUT_PARAMETERS);
            ArrayNode outParametersArray = outParametersNode.putArray("outParameters");
            readIOParameters(processTask.getOutParameters(), outParametersArray);
        }
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

        JsonNode fallbackToDefaultTenant = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_FALLBACK_TO_DEFAULT_TENANT, elementNode);
        if (fallbackToDefaultTenant != null) {
            task.setFallbackToDefaultTenant(fallbackToDefaultTenant.booleanValue());
        }

        ListenerConverterUtil.convertJsonToLifeCycleListeners(elementNode, task);

        return task;
    }

    protected List<IOParameter> readIOParameters(JsonNode parametersNode) {
        List<IOParameter> ioParameters = new ArrayList<>();
        for (JsonNode paramNode : parametersNode){
            IOParameter ioParameter = new IOParameter();

            if (paramNode.has("source")) {
                ioParameter.setSource(paramNode.get("source").asText());
            }
            if (paramNode.has("sourceExpression")) {
                ioParameter.setSourceExpression(paramNode.get("sourceExpression").asText());
            }
            if (paramNode.has("target")) {
                ioParameter.setTarget(paramNode.get("target").asText());
            }
            if (paramNode.has("targetExpression")) {
                ioParameter.setTargetExpression(paramNode.get("targetExpression").asText());
            }
            ioParameters.add(ioParameter);
        }
        return ioParameters;
    }

    protected void readIOParameters(List<IOParameter> ioParameters, ArrayNode parametersNode) {
        for (IOParameter ioParameter : ioParameters) {

            ObjectNode parameterNode = parametersNode.addObject();

            if (StringUtils.isNotEmpty(ioParameter.getSource())) {
                parameterNode.put("source", ioParameter.getSource());
            }
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                parameterNode.put("sourceExpression", ioParameter.getSourceExpression());
            }
            if (StringUtils.isNotEmpty(ioParameter.getTarget())) {
                parameterNode.put("target", ioParameter.getTarget());
            }
            if (StringUtils.isNotEmpty(ioParameter.getTargetExpression())) {
                parameterNode.put("targetExpression", ioParameter.getTargetExpression());
            }

        }
    }

    @Override
    public void setProcessModelMap(Map<String, String> processModelMap) {
        this.processModelMap = processModelMap;
    }
}
