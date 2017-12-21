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
package org.flowable.editor.language;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractConverterTest {

    protected BpmnModel readJsonFile() throws Exception {
        InputStream jsonStream = this.getClass().getClassLoader().getResourceAsStream(getResource());
        JsonNode modelNode = new ObjectMapper().readTree(jsonStream);
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        return bpmnModel;
    }

    protected BpmnModel convertToJsonAndBack(BpmnModel bpmnModel) {
        ObjectNode modelNode = new BpmnJsonConverter().convertToJson(bpmnModel);
        bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        return bpmnModel;
    }

    protected EventDefinition extractEventDefinition(FlowElement flowElement) {
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof Event);
        Event event = (Event) flowElement;
        assertFalse(event.getEventDefinitions().isEmpty());
        return event.getEventDefinitions().get(0);
    }

    protected abstract String getResource();
}
