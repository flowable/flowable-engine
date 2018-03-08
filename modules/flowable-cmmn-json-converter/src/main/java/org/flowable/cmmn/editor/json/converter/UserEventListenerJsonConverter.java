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
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;

import java.util.Map;

/**
 * @author Dennis Federico
 */
public class UserEventListenerJsonConverter extends BaseCmmnJsonConverter {


    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement, CmmnModel cmmnModel) {
        //TODO... implement @Dennis Federico
        throw new UnsupportedOperationException("Not implemented yet!!!");
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor, BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverter.CmmnModelIdHelper cmmnModelIdHelper) {
        //TODO... implement @Dennis Federico
        throw new UnsupportedOperationException("Not implemented yet!!!");
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        //TODO... implement @Dennis Federico
        throw new UnsupportedOperationException("Not implemented yet!!!");
    }
}
