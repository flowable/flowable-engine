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
package org.flowable.engine.dynamic;

import org.flowable.bpmn.model.FlowElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Used for {@link FlowElement} currently not supported by the {@link org.flowable.engine.DynamicBpmnService} and elements who are not parsed.
 *
 * Created by Pardo David on 5/12/2016.
 */
public class DefaultPropertiesParser extends BasePropertiesParser {

    @Override
    protected ObjectNode createPropertiesNode(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper objectMapper) {
        return null;
    }

    @Override
    public boolean supports(FlowElement flowElement) {
        return false;
    }
}
