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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class CustomNamespaceAttributeConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "customnamespaceattributemodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
        Process process = model.getMainProcess();
        assertThat(process.getAttributes()).isNotNull();
        List<ExtensionAttribute> attributes = process.getAttributes().get("version");
        // custom:version = "9"
        assertThat(attributes)
                .extracting(ExtensionAttribute::getNamespace, ExtensionAttribute::getNamespacePrefix, ExtensionAttribute::getName, ExtensionAttribute::getValue)
                .containsExactly(tuple("http://custom.org/bpmn", "custom", "version", "9"));

        FlowElement flowElement = model.getMainProcess().getFlowElement("usertask");
        assertThat(flowElement).isNotNull();
        assertThat(flowElement).isInstanceOf(UserTask.class);
        assertThat(flowElement.getId()).isEqualTo("usertask");
        UserTask userTask = (UserTask) flowElement;
        assertThat(userTask.getId()).isEqualTo("usertask");
        assertThat(userTask.getName()).isEqualTo("User Task");

        Map<String, List<ExtensionAttribute>> attributesMap = userTask.getAttributes();
        assertThat(attributesMap).isNotNull();
        assertThat(attributesMap).hasSize(2);

        attributes = attributesMap.get("id");
        assertThat(attributes)
                .extracting(ExtensionAttribute::getName, ExtensionAttribute::getValue, ExtensionAttribute::getNamespacePrefix, ExtensionAttribute::getNamespace)
                .containsExactly(tuple("id", "test", "custom2", "http://custom2.org/bpmn"));

        attributes = attributesMap.get("attr");
        assertThat(attributes)
                .extracting(ExtensionAttribute::getName, ExtensionAttribute::getValue, ExtensionAttribute::getNamespacePrefix, ExtensionAttribute::getNamespace)
                .containsExactly(tuple("attr", "attrValue", "custom2", "http://custom2.org/bpmn"));
    }
}
