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
import static org.flowable.editor.language.xml.util.XmlTestUtils.exportAndReadXMLFile;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.junit.jupiter.api.Test;

class ExporterAndVersionTest {

    @Test
    public void convertModelToXML() {
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId("myProcessWithExporter");
        bpmnModel.addProcess(process);
        bpmnModel.setExporter("Flowable");
        bpmnModel.setExporterVersion("latest");
        
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);

        assertThat(parsedModel.getExporter()).isEqualTo("Flowable");
        assertThat(parsedModel.getExporterVersion()).isEqualTo("latest");
    }
}
