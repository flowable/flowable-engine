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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class TextAnnotationConverterTest {

    @BpmnXmlConverterTest("parsing_error_on_extension_elements.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getFlowElement("_5");
        assertThat(flowElement)
                .isInstanceOfSatisfying(ScriptTask.class, scriptTask -> {
                    assertThat(scriptTask.getId()).isEqualTo("_5");
                    assertThat(scriptTask.getName()).isEqualTo("Send Hello Message");
                });
    }
}
