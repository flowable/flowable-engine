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
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class OtherToolImportConverterTest {

    @BpmnXmlConverterTest("othertoolimport.bpmn")
    void validateModel(BpmnModel model) {
        org.flowable.bpmn.model.Process process = model.getProcess("_GQ4P0PUQEeK4teimjV5_yg");
        assertThat(process).isNotNull();
        assertThat(process.getId()).isEqualTo("Carpet_Plus");
        assertThat(process.getName()).isEqualTo("Carpet-Plus");
        assertThat(process.isExecutable()).isTrue();
    }
}
