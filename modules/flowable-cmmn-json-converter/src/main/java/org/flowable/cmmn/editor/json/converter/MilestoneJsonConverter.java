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
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class MilestoneJsonConverter extends BaseCmmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToBpmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_MILESTONE, MilestoneJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(Milestone.class, MilestoneJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_MILESTONE;
    }

    @Override
    protected CaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor, 
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {
        Milestone milestone = new Milestone();
        return milestone;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement, CmmnModel cmmnModel) {
        PlanItem planItem = (PlanItem) baseElement;
        Milestone milestone = (Milestone) planItem.getPlanItemDefinition();
       
        // nothing to do yet
    }
}
