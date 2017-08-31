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

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class CriterionJsonConverter extends BaseCmmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap) {
        convertersToCmmnMap.put(STENCIL_ENTRY_CRITERION, CriterionJsonConverter.class);
        convertersToCmmnMap.put(STENCIL_EXIT_CRITERION, CriterionJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(Criterion.class, CriterionJsonConverter.class);
    }

    protected String getStencilId(BaseElement baseElement) {
        Criterion criterion = (Criterion) baseElement;
        
        if (criterion.isEntryCriterion()) {
            return STENCIL_ENTRY_CRITERION;
        } else if (criterion.isExitCriterion()) {
            return STENCIL_EXIT_CRITERION;
        }
        
        return STENCIL_ENTRY_CRITERION;
    }

    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, BaseElement baseElement, CmmnModel cmmnModel) {
        Criterion criterion = (Criterion) baseElement;
        ArrayNode dockersArrayNode = objectMapper.createArrayNode();
        ObjectNode dockNode = objectMapper.createObjectNode();
        GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(criterion.getId());
        GraphicInfo parentGraphicInfo = cmmnModel.getGraphicInfo(criterion.getAttachedToRefId());
        dockNode.put(EDITOR_BOUNDS_X, graphicInfo.getX() - parentGraphicInfo.getX());
        dockNode.put(EDITOR_BOUNDS_Y, graphicInfo.getY() - parentGraphicInfo.getY());
        dockersArrayNode.add(dockNode);
        elementNode.set("dockers", dockersArrayNode);
    }

    protected CaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel) {
        Criterion criterion = new Criterion();
        String stencilId = CmmnJsonConverterUtil.getStencilId(elementNode);
        if (STENCIL_ENTRY_CRITERION.equals(stencilId)) {
            criterion.setEntryCriterion(true);

        } else if (STENCIL_EXIT_CRITERION.equals(stencilId)) {
            criterion.setExitCriterion(true);
        } 
        
        criterion.setAttachedToRefId(lookForAttachedRef(elementNode.get(EDITOR_SHAPE_ID).asText(), modelNode.get(EDITOR_CHILD_SHAPES)));
        
        if (criterion.getAttachedToRefId() != null) {
            String criterionId = CmmnJsonConverterUtil.getElementId(elementNode);
            cmmnModel.addCriterion(criterionId, criterion);
        }
        
        return criterion;
    }

    private String lookForAttachedRef(String criterionId, JsonNode childShapesNode) {
        String attachedRefId = null;

        if (childShapesNode != null) {

            for (JsonNode childNode : childShapesNode) {
                ArrayNode outgoingNode = (ArrayNode) childNode.get("outgoing");
                if (outgoingNode != null && outgoingNode.size() > 0) {
                    for (JsonNode outgoingChildNode : outgoingNode) {
                        JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                        if (resourceNode != null && criterionId.equals(resourceNode.asText())) {
                            attachedRefId = CmmnJsonConverterUtil.getElementId(childNode);
                            break;
                        }
                    }

                    if (attachedRefId != null) {
                        break;
                    }
                }

                attachedRefId = lookForAttachedRef(criterionId, childNode.get(EDITOR_CHILD_SHAPES));

                if (attachedRefId != null) {
                    break;
                }
            }
        }

        return attachedRefId;
    }
}
