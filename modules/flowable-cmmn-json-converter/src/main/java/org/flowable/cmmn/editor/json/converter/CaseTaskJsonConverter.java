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

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.ListenerConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class CaseTaskJsonConverter extends BaseChildTaskCmmnJsonConverter implements CaseModelAwareConverter {
    
    protected Map<String, String> caseModelMap;
    
    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_CASE, CaseTaskJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(CaseTask.class, CaseTaskJsonConverter.class);
    }
    
    @Override
    protected String getStencilId(BaseElement baseElement) {
        return CmmnStencilConstants.STENCIL_TASK_CASE;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
            BaseElement baseElement, CmmnModel cmmnModel) {
        // todo implement rest of the properties
        CaseTask caseTask = (CaseTask) ((PlanItem) baseElement).getPlanItemDefinition();

        if (caseTask.getFallbackToDefaultTenant() != null) {
            propertiesNode.put(PROPERTY_FALLBACK_TO_DEFAULT_TENANT, caseTask.getFallbackToDefaultTenant());
        }
        ListenerConverterUtil.convertLifecycleListenersToJson(objectMapper, propertiesNode, caseTask);

        if (caseTask.getInParameters() != null && !caseTask.getInParameters().isEmpty()) {
            ObjectNode inParametersNode = propertiesNode.putObject(CmmnStencilConstants.PROPERTY_CASE_IN_PARAMETERS);
            ArrayNode inParametersArray = inParametersNode.putArray("inParameters");
            readIOParameters(caseTask.getInParameters(), inParametersArray);
        }
        if (caseTask.getOutParameters() != null && !caseTask.getOutParameters().isEmpty()) {
            ObjectNode outParametersNode = propertiesNode.putObject(CmmnStencilConstants.PROPERTY_CASE_OUT_PARAMETERS);
            ArrayNode outParametersArray = outParametersNode.putArray("outParameters");
            readIOParameters(caseTask.getOutParameters(), outParametersArray);
        }
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
            BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {
        
        CaseTask task = new CaseTask();
        
        JsonNode caseModelReferenceNode = CmmnJsonConverterUtil.getProperty(PROPERTY_CASE_REFERENCE, elementNode);
        if (caseModelReferenceNode != null && caseModelReferenceNode.has("id") && !caseModelReferenceNode.get("id").isNull()) {

            String caseModelId = caseModelReferenceNode.get("id").asText();
            if (caseModelMap != null) {
                String caseModelKey = caseModelMap.get(caseModelId);
                task.setCaseRef(caseModelKey);
            }
        }

        JsonNode caseTaskInParametersNode = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_CASE_IN_PARAMETERS, elementNode);
        if (caseTaskInParametersNode != null && caseTaskInParametersNode.has("inParameters") && !caseTaskInParametersNode.get("inParameters").isNull()) {
            JsonNode inParametersNode =  caseTaskInParametersNode.get("inParameters");
            task.setInParameters(readIOParameters(inParametersNode));
        }

        JsonNode caseTaskOutParametersNode = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_CASE_OUT_PARAMETERS, elementNode);
        if (caseTaskOutParametersNode != null && caseTaskOutParametersNode.has("outParameters") && !caseTaskOutParametersNode.get("outParameters").isNull()) {
            JsonNode outParametersNode =  caseTaskOutParametersNode.get("outParameters");
            task.setOutParameters(readIOParameters(outParametersNode));
        }

        boolean fallbackToDefaultTenant = CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_FALLBACK_TO_DEFAULT_TENANT, elementNode, false);
        if (fallbackToDefaultTenant) {
            task.setFallbackToDefaultTenant(true);
        }

        ListenerConverterUtil.convertJsonToLifeCycleListeners(elementNode, task);

        return task;
    }

    @Override
    public void setCaseModelMap(Map<String, String> caseModelMap) {
        this.caseModelMap = caseModelMap;
    }

}
