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

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Transaction;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.editor.language.json.model.ModelInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class SubProcessJsonConverter extends BaseBpmnJsonConverter implements FormAwareConverter, FormKeyAwareConverter,
        DecisionTableAwareConverter, DecisionTableKeyAwareConverter {

    protected Map<String, String> formMap;
    protected Map<String, ModelInfo> formKeyMap;
    protected Map<String, String> decisionTableMap;
    protected Map<String, ModelInfo> decisionTableKeyMap;

    public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_SUB_PROCESS, SubProcessJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_COLLAPSED_SUB_PROCESS, SubProcessJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(SubProcess.class, SubProcessJsonConverter.class);
        convertersToJsonMap.put(Transaction.class, SubProcessJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        //see http://forum.flowable.org/t/collapsed-subprocess-navigation-in-the-web-based-bpmn-modeler/138/19
        GraphicInfo graphicInfo = model.getGraphicInfo(baseElement.getId());
        Boolean isExpanded = graphicInfo.getExpanded();
        if (isExpanded != null && isExpanded == false) {
            return STENCIL_COLLAPSED_SUB_PROCESS;
        } else {
            return STENCIL_SUB_PROCESS;
        }
    }

    @Override
    protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement) {
        SubProcess subProcess = (SubProcess) baseElement;

        propertiesNode.put("activitytype", getStencilId(baseElement));
        GraphicInfo gi = model.getGraphicInfo(baseElement.getId());
        Boolean isExpanded = gi.getExpanded();
        
        ArrayNode subProcessShapesArrayNode = objectMapper.createArrayNode();
        GraphicInfo graphicInfo = model.getGraphicInfo(subProcess.getId());
        
        if (isExpanded != null && isExpanded == false) {
            processor.processFlowElements(subProcess, model, subProcessShapesArrayNode, formKeyMap, decisionTableKeyMap, 0, 0);
        } else {
            processor.processFlowElements(subProcess, model, subProcessShapesArrayNode, formKeyMap,
                    decisionTableKeyMap, graphicInfo.getX(), graphicInfo.getY());
        }
        
        flowElementNode.set("childShapes", subProcessShapesArrayNode);

        if (subProcess instanceof Transaction) {
            propertiesNode.put("istransaction", true);
        }

        BpmnJsonConverterUtil.convertDataPropertiesToJson(subProcess.getDataObjects(), propertiesNode);
    }

    @Override
    protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
        SubProcess subProcess = null;
        if (getPropertyValueAsBoolean("istransaction", elementNode)) {
            subProcess = new Transaction();

        } else {
            subProcess = new SubProcess();
        }

        JsonNode childShapesArray = elementNode.get(EDITOR_CHILD_SHAPES);
        processor.processJsonElements(childShapesArray, modelNode, subProcess, shapeMap, formMap, decisionTableMap, model);

        JsonNode processDataPropertiesNode = elementNode.get(EDITOR_SHAPE_PROPERTIES).get(PROPERTY_DATA_PROPERTIES);
        if (processDataPropertiesNode != null) {
            List<ValuedDataObject> dataObjects = BpmnJsonConverterUtil.convertJsonToDataProperties(processDataPropertiesNode, subProcess);
            subProcess.setDataObjects(dataObjects);
            subProcess.getFlowElements().addAll(dataObjects);
        }
        
        //store correct convertion info...
        if (STENCIL_COLLAPSED_SUB_PROCESS.equals(BpmnJsonConverterUtil.getStencilId(elementNode))) {
            GraphicInfo graphicInfo = model.getGraphicInfo(BpmnJsonConverterUtil.getElementId(elementNode));
            graphicInfo.setExpanded(false); //default is null!
        }

        return subProcess;
    }

    @Override
    public void setFormMap(Map<String, String> formMap) {
        this.formMap = formMap;
    }

    @Override
    public void setFormKeyMap(Map<String, ModelInfo> formKeyMap) {
        this.formKeyMap = formKeyMap;
    }

    @Override
    public void setDecisionTableMap(Map<String, String> decisionTableMap) {
        this.decisionTableMap = decisionTableMap;
    }

    @Override
    public void setDecisionTableKeyMap(Map<String, ModelInfo> decisionTableKeyMap) {
        this.decisionTableKeyMap = decisionTableKeyMap;
    }
}
