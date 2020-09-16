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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.ListenerConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class CaseTaskJsonConverter extends BaseChildTaskCmmnJsonConverter {
    
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
            BaseElement baseElement, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {

        CaseTask caseTask = (CaseTask) ((PlanItem) baseElement).getPlanItemDefinition();

        if (caseTask.getFallbackToDefaultTenant() != null) {
            propertiesNode.put(PROPERTY_FALLBACK_TO_DEFAULT_TENANT, caseTask.getFallbackToDefaultTenant());
        }

        propertiesNode.put(PROPERTY_SAME_DEPLOYMENT, caseTask.isSameDeployment());

        if (StringUtils.isNotEmpty(caseTask.getCaseInstanceIdVariableName())) {
            propertiesNode.put(PROPERTY_ID_VARIABLE_NAME, caseTask.getCaseInstanceIdVariableName());
        }

        String caseRef = caseTask.getCaseRef();
        if (StringUtils.isNotEmpty(caseRef)) {

            ObjectNode caseReferenceNode = objectMapper.createObjectNode();
            caseReferenceNode.put("key", caseRef);
            propertiesNode.set(PROPERTY_CASE_REFERENCE, caseReferenceNode);

            Map<String, String> modelInfo = converterContext.getCaseModelInfoForCaseModelKey(caseRef);
            if (modelInfo != null) {
                caseReferenceNode.put("id", modelInfo.get("id"));
                caseReferenceNode.put("name", modelInfo.get("name"));

            } else {
                converterContext.registerUnresolvedCaseModelReferenceForCaseModel(caseRef, cmmnModel);

            }
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
        if (caseTask.getBusinessKey() != null) {
            propertiesNode.put(PROPERTY_CASE_BUSINESS_KEY, caseTask.getBusinessKey());
        }
        if (caseTask.isInheritBusinessKey()) {
            propertiesNode.put(PROPERTY_CASE_INHERIT_BUSINESS_KEY, caseTask.isInheritBusinessKey());
        }
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
            BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper) {
        
        CaseTask task = new CaseTask();
        
        JsonNode caseModelReferenceNode = CmmnJsonConverterUtil.getProperty(PROPERTY_CASE_REFERENCE, elementNode);
        if (caseModelReferenceNode != null && caseModelReferenceNode.has("id") && !caseModelReferenceNode.get("id").isNull()) {

            String caseModelId = caseModelReferenceNode.get("id").asText();
            String caseModelKey = converterContext.getCaseModelKeyForCaseModelId(caseModelId);

            if (StringUtils.isEmpty(caseModelKey) && caseModelReferenceNode.has("key")) {
                caseModelKey = caseModelReferenceNode.get("key").asText();
            }
            task.setCaseRef(caseModelKey);

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

        JsonNode caseTaskBusinessKey = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_CASE_BUSINESS_KEY, elementNode);
        if (caseTaskBusinessKey != null) {
            task.setBusinessKey(caseTaskBusinessKey.asText());
        }

        JsonNode caseTaskInheritBusinessKey = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_CASE_INHERIT_BUSINESS_KEY, elementNode);
        if (caseTaskInheritBusinessKey != null) {
            task.setInheritBusinessKey(caseTaskInheritBusinessKey.asBoolean());
        }

        boolean fallbackToDefaultTenant = CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_FALLBACK_TO_DEFAULT_TENANT, elementNode, false);
        if (fallbackToDefaultTenant) {
            task.setFallbackToDefaultTenant(true);
        }

        boolean sameDeployment = CmmnJsonConverterUtil.getPropertyValueAsBoolean(CmmnStencilConstants.PROPERTY_SAME_DEPLOYMENT, elementNode, false);
        if (sameDeployment) {
            task.setSameDeployment(true);
        }

        JsonNode idVariableName = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_ID_VARIABLE_NAME, elementNode);
        if (idVariableName != null && idVariableName.isTextual()) {
            task.setCaseInstanceIdVariableName(idVariableName.asText());
        }

        ListenerConverterUtil.convertJsonToLifeCycleListeners(elementNode, task);

        return task;
    }

}
