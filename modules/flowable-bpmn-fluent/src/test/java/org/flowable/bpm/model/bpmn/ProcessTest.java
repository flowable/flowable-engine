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
package org.flowable.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpm.model.bpmn.instance.Process;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.bpmn.util.BpmnModelResource;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Test;

import java.util.Collection;

public class ProcessTest
        extends BpmnModelTest {

    @Test
    @BpmnModelResource
    public void shouldImportProcess() {

        ModelElementInstance modelElementById = bpmnModelInstance.getModelElementById("exampleProcessId");
        assertThat(modelElementById).isNotNull();

        Collection<RootElement> rootElements = bpmnModelInstance.getDefinitions().getRootElements();
        assertThat(rootElements).hasSize(1);
        org.flowable.bpm.model.bpmn.instance.Process process = (Process) rootElements.iterator().next();

        assertThat(process.getId()).isEqualTo("exampleProcessId");
        assertThat(process.getName()).isNull();
        assertThat(process.getProcessType()).isEqualTo(ProcessType.None);
        assertThat(process.isExecutable()).isFalse();
        assertThat(process.isClosed()).isFalse();


    }


}
